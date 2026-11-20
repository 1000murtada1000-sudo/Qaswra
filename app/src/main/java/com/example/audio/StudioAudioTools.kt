package com.example.audio

import android.content.ContentValues
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import com.aistudio.voicepro.sonixk.RealWaveformExtractor
import com.example.diagnostics.AppDiagnosticLog
import com.example.diagnostics.FfmpegKitBridge
import com.example.model.BassBoostParams
import com.example.model.BatchDspSettings
import com.example.model.BatchFilterPreset
import com.example.model.DspConfig
import com.example.model.EffectType
import com.example.model.ExportAudioFormat
import com.example.model.GlobalProPreset
import com.example.model.GlobalProPresetId
import com.example.model.NoiseReductionParams
import com.example.model.OneTapSocialPreset
import com.example.model.PitchShiftParams
import com.example.model.PlatformLoudnessTarget
import com.example.model.StereoAudioBuffer
import com.example.model.TrebleParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.util.Locale
import java.util.Random
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Adds studio-grade vocal mastering warmth, compression, 8kHz air, and gentle loudness normalization.
 */
fun applyStudioWarmth(
    buffer: StereoAudioBuffer,
    warmthDb: Float = 2.5f,
    presenceDb: Float = 2.2f,
): StereoAudioBuffer = com.example.audio.fx.AudioSkillsPresets.applyStudioVocalMastering(
    buffer = buffer,
    extraWarmthDb = (warmthDb - 2.5f).coerceAtLeast(-2f),
    extraPresenceDb = (presenceDb - 1.5f).coerceAtLeast(-2f),
)

/**
 * Applies a Global Pro Preset with dedicated post-DSP shaping.
 */
suspend fun renderGlobalProPreset(
    buffer: StereoAudioBuffer,
    preset: GlobalProPreset,
): StereoAudioBuffer = withContext(Dispatchers.Default) {
    val base = renderOfflineDsp(buffer, preset.dspConfig)
    if (base.frameCount <= 0) return@withContext base
    if (preset.id == GlobalProPresetId.BROADCAST_RADIO || preset.id == GlobalProPresetId.CRYSTAL_VOCAL) {
        normalizeAudio(base, -1.0f)
    } else {
        normalizeAudio(base, -0.8f)
    }
}

fun reverseAudio(buffer: StereoAudioBuffer): StereoAudioBuffer {
    val n = buffer.frameCount
    if (n <= 1) return buffer
    val outL = FloatArray(n)
    val outR = FloatArray(n)
    for (i in 0 until n) {
        outL[i] = buffer.left[n - 1 - i]
        outR[i] = buffer.right[n - 1 - i]
    }
    val sr = buffer.sampleRate.coerceAtLeast(8000)
    val fadeFrames = minOf(n / 8, (0.003f * sr).roundToInt().coerceAtLeast(16))
    if (fadeFrames >= 4) {
        if (max(abs(outL[0]), abs(outR[0])) > 0.015f) {
            for (i in 0 until fadeFrames) {
                val g = i.toFloat() / fadeFrames
                outL[i] *= g
                outR[i] *= g
            }
        }
        if (max(abs(outL[n - 1]), abs(outR[n - 1])) > 0.015f) {
            for (i in 0 until fadeFrames) {
                val g = i.toFloat() / fadeFrames
                outL[n - 1 - i] *= g
                outR[n - 1 - i] *= g
            }
        }
    }
    return StereoAudioBuffer(outL, outR, sr)
}

enum class SilenceInsertMode {
    NEW_TRACK,
    INSERT_START,
    APPEND_END,
}

data class SilenceGenerationResult(
    val buffer: StereoAudioBuffer? = null,
    val diskFile: File? = null,
    val sampleRate: Int = TARGET_SAMPLE_RATE,
    val channels: Int = 2,
    val durationMs: Long = 0L,
    val usedFfmpeg: Boolean = false,
)

fun generateSilence(
    durationSeconds: Float,
    sampleRate: Int = TARGET_SAMPLE_RATE,
): StereoAudioBuffer {
    val sr = sampleRate.coerceIn(8000, 96000)
    val safeDur = clamp(durationSeconds, 0.1f, 600f)
    val frames = max(sr / 10, (safeDur * sr).roundToInt()).coerceAtMost(sr * 180)
    return StereoAudioBuffer(
        left = FloatArray(frames),
        right = FloatArray(frames),
        sampleRate = sr,
    )
}

/**
 * Generates pure silence via FFmpeg `anullsrc` (or chunked 8KB disk PCM fallback) up to 10 minutes (600s),
 * supporting New Track, Insert at Start, or Append at End across both RAM buffers and disk-backed files.
 */
suspend fun insertSilenceStreaming(
    context: Context,
    currentBuffer: StereoAudioBuffer?,
    currentDiskFile: File? = null,
    durationSeconds: Float,
    mode: SilenceInsertMode = SilenceInsertMode.NEW_TRACK,
    sampleRate: Int = TARGET_SAMPLE_RATE,
    channels: Int = 2,
): SilenceGenerationResult = withContext(Dispatchers.IO) {
    val safeDur = clamp(durationSeconds, 0.1f, 600f)
    val requestedSr = sampleRate.coerceIn(8000, 96000)
    val ch = channels.coerceIn(1, 2)
    val hasExistingBuffer = currentBuffer != null && currentBuffer.frameCount > 0
    val hasExistingDisk = !hasExistingBuffer && currentDiskFile != null && currentDiskFile.exists() && currentDiskFile.length() > 0L
    val effectiveMode = if (!hasExistingBuffer && !hasExistingDisk) SilenceInsertMode.NEW_TRACK else mode
    val effectiveSr = when {
        effectiveMode == SilenceInsertMode.NEW_TRACK -> requestedSr
        hasExistingBuffer -> currentBuffer!!.sampleRate.coerceIn(8000, 96000)
        else -> TARGET_SAMPLE_RATE
    }
    val effectiveCh = if (effectiveMode == SilenceInsertMode.NEW_TRACK) ch else 2
    val chLayout = if (effectiveCh == 1) "mono" else "stereo"

    val workDir = File(context.cacheDir, "silence_stream").apply { mkdirs() }
    val outDir = File(context.cacheDir, "studio_outputs").apply { mkdirs() }
    val id = UUID.randomUUID().toString().take(8)
    val silenceWav = File(workDir, "sil_$id.wav")
    val curWav = File(workDir, "cur_$id.wav")
    val mergedWav = File(workDir, "sil_out_$id.wav")
    var usedFfmpeg = false

    try {
        if (FfmpegKitBridge.isAvailable()) {
            val durStr = String.format(Locale.US, "%.3f", safeDur)
            val cmd = "-y -f lavfi -i \"anullsrc=r=$effectiveSr:cl=$chLayout\" -t $durStr -c:a pcm_s16le \"${silenceWav.absolutePath}\""
            val ok = FfmpegKitBridge.execute(cmd, toolName = "SilenceGeneratorAnullsrc", logNonZeroFailure = false)
            if (ok && silenceWav.exists() && silenceWav.length() > 44L) {
                usedFfmpeg = true
            }
        }
        if (!usedFfmpeg) {
            writeSilenceWavStreaming(silenceWav, safeDur, effectiveSr, numChannels = effectiveCh)
        }

        if (effectiveMode == SilenceInsertMode.NEW_TRACK) {
            val durMs = (safeDur * 1000f).roundToLong().coerceAtLeast(100L)
            if (safeDur > 180f) {
                val persistFile = File(outDir, "silence_${id}.wav")
                silenceWav.copyTo(persistFile, overwrite = true)
                return@withContext SilenceGenerationResult(
                    buffer = null,
                    diskFile = persistFile,
                    sampleRate = effectiveSr,
                    channels = effectiveCh,
                    durationMs = durMs,
                    usedFfmpeg = usedFfmpeg,
                )
            }
            val decoded = readStereoWavFile(silenceWav, preserveSampleRate = true)
                ?: generateSilence(safeDur, effectiveSr)
            return@withContext SilenceGenerationResult(
                buffer = decoded,
                diskFile = null,
                sampleRate = decoded.sampleRate,
                channels = effectiveCh,
                durationMs = decoded.durationMs.coerceAtLeast(durMs),
                usedFfmpeg = usedFfmpeg,
            )
        }

        if (hasExistingDisk) {
            val diskSrc = currentDiskFile!!
            val persistFile = File(outDir, "silence_merged_${id}.m4a")
            val firstInput = if (effectiveMode == SilenceInsertMode.INSERT_START) silenceWav else diskSrc
            val secondInput = if (effectiveMode == SilenceInsertMode.INSERT_START) diskSrc else silenceWav
            val ok = if (FfmpegKitBridge.isAvailable()) {
                val cmd = "-y -i \"${firstInput.absolutePath}\" -i \"${secondInput.absolutePath}\" " +
                    "-filter_complex \"[0:a]aformat=sample_rates=$TARGET_SAMPLE_RATE:channel_layouts=stereo[a0];" +
                    "[1:a]aformat=sample_rates=$TARGET_SAMPLE_RATE:channel_layouts=stereo[a1];" +
                    "[a0][a1]concat=n=2:v=0:a=1[out]\" -map \"[out]\" -c:a aac -b:a 192k \"${persistFile.absolutePath}\""
                FfmpegKitBridge.execute(cmd, toolName = "SilenceDiskConcat", logNonZeroFailure = true)
            } else {
                false
            }
            if (ok && persistFile.exists() && persistFile.length() > 0L) {
                val meta = AudioDecoder.inspectLargeAudioStreaming(persistFile, buckets = 64, context = context) {}
                return@withContext SilenceGenerationResult(
                    buffer = null,
                    diskFile = persistFile,
                    sampleRate = meta?.sampleRate ?: TARGET_SAMPLE_RATE,
                    channels = 2,
                    durationMs = meta?.durationMs ?: 0L,
                    usedFfmpeg = true,
                )
            } else {
                runCatching { persistFile.delete() }
            }
        }

        val srcBuf = currentBuffer!!
        val existingSec = srcBuf.frameCount.toFloat() / effectiveSr.toFloat()
        if (existingSec + safeDur > 180f) {
            writeStereoWavFile(curWav, StereoAudioBuffer(srcBuf.left, srcBuf.right, effectiveSr))
            val firstWav = if (effectiveMode == SilenceInsertMode.INSERT_START) silenceWav else curWav
            val secondWav = if (effectiveMode == SilenceInsertMode.INSERT_START) curWav else silenceWav
            val mergedOk = mergeWavFilesStreaming(firstWav, secondWav, mergedWav, crossfadeMs = 2, sampleRate = effectiveSr)
            if (mergedOk && mergedWav.exists() && mergedWav.length() > 44L) {
                val persistFile = File(outDir, "silence_merged_${id}.wav")
                mergedWav.copyTo(persistFile, overwrite = true)
                return@withContext SilenceGenerationResult(
                    buffer = null,
                    diskFile = persistFile,
                    sampleRate = effectiveSr,
                    channels = 2,
                    durationMs = ((existingSec + safeDur) * 1000f).roundToLong(),
                    usedFfmpeg = usedFfmpeg,
                )
            }
        }

        writeStereoWavFile(curWav, StereoAudioBuffer(srcBuf.left, srcBuf.right, effectiveSr))
        val firstWav = if (effectiveMode == SilenceInsertMode.INSERT_START) silenceWav else curWav
        val secondWav = if (effectiveMode == SilenceInsertMode.INSERT_START) curWav else silenceWav
        val mergedOk = mergeWavFilesStreaming(firstWav, secondWav, mergedWav, crossfadeMs = 2, sampleRate = effectiveSr)
        val mergedBuf = if (mergedOk && mergedWav.exists() && mergedWav.length() > 44L) {
            readStereoWavFile(mergedWav, preserveSampleRate = true)
        } else {
            null
        }
        val finalBuf = mergedBuf ?: run {
            val sil = generateSilence(safeDur, effectiveSr)
            if (effectiveMode == SilenceInsertMode.INSERT_START) {
                mergeClips(sil, srcBuf, crossfadeMs = 2)
            } else {
                mergeClips(srcBuf, sil, crossfadeMs = 2)
            }
        }
        SilenceGenerationResult(
            buffer = finalBuf,
            diskFile = null,
            sampleRate = finalBuf.sampleRate,
            channels = 2,
            durationMs = finalBuf.durationMs,
            usedFfmpeg = usedFfmpeg,
        )
    } finally {
        runCatching { silenceWav.delete() }
        runCatching { curWav.delete() }
        runCatching { mergedWav.delete() }
    }
}

/**
 * Surround Stereo M/S processor whose sliders start cleanly at 0% (neutral).
 * - midAdjustPct: -60f .. +60f (default 0% -> 1.0x center)
 * - sideBoostPct: 0f .. +120f (default 0% -> 1.0x side)
 * - widthPct: 0f .. 100f (default 0% -> 0ms Haas width)
 */
fun convertToSurroundStereoFromOffsets(
    buffer: StereoAudioBuffer,
    midAdjustPct: Float = 0f,
    sideBoostPct: Float = 0f,
    widthPct: Float = 0f,
): StereoAudioBuffer {
    if (abs(midAdjustPct) < 0.05f && abs(sideBoostPct) < 0.05f && abs(widthPct) < 0.05f) {
        return buffer
    }
    val midLevel = (1f + midAdjustPct / 100f).coerceIn(0.35f, 1.65f)
    val sideLevel = (1f + sideBoostPct / 100f).coerceIn(0.5f, 2.3f)
    val stereoWidth = (widthPct / 100f).coerceIn(0f, 1f)
    return convertToSurroundStereo(buffer, midLevel, sideLevel, stereoWidth)
}

fun convertToSurroundStereo(
    buffer: StereoAudioBuffer,
    midLevel: Float = 1.0f,
    sideLevel: Float = 1.35f,
    stereoWidth: Float = 0.78f,
): StereoAudioBuffer {
    val n = buffer.frameCount
    if (n == 0) return buffer
    val haasSamples = if (stereoWidth <= 0.005f) {
        0
    } else {
        max(1, (0.011f * buffer.sampleRate * clamp(stereoWidth, 0f, 1f)).roundToInt())
    }
    val outL = FloatArray(n)
    val outR = FloatArray(n)

    for (i in 0 until n) {
        val rawL = buffer.left[i]
        val rawR = buffer.right[i]
        val delayedR = if (haasSamples == 0) rawR else buffer.right[max(0, i - haasSamples)]
        val mid = 0.5f * (rawL + rawR) * midLevel
        val naturalSide = 0.5f * (rawL - rawR)
        val haasSide = if (haasSamples == 0) 0f else 0.5f * (rawL - delayedR) * clamp(stereoWidth, 0f, 1f)
        val side = (naturalSide + haasSide * 0.65f) * sideLevel
        outL[i] = clamp(mid + side, -1f, 1f)
        outR[i] = clamp(mid - side, -1f, 1f)
    }
    return normalizeAudio(StereoAudioBuffer(outL, outR, buffer.sampleRate), -1.0f)
}

data class StereoCompatibilityReport(
    val correlation: Float,
    val monoLossDb: Float,
    val isMonoCompatible: Boolean,
)

fun analyzeStereoCompatibility(
    buffer: StereoAudioBuffer,
    maxSampledFrames: Int = 8192,
): StereoCompatibilityReport {
    val n = buffer.frameCount
    if (n <= 0) return StereoCompatibilityReport(1f, 0f, true)
    val step = max(1, n / max(1, maxSampledFrames))
    var sumLr = 0.0
    var sumL2 = 0.0
    var sumR2 = 0.0
    var sumMono2 = 0.0
    var count = 0
    var i = 0
    while (i < n) {
        val l = buffer.left[i].toDouble()
        val r = buffer.right[i].toDouble()
        val m = 0.5 * (l + r)
        sumLr += l * r
        sumL2 += l * l
        sumR2 += r * r
        sumMono2 += m * m
        count++
        i += step
    }
    val denom = sqrt(sumL2 * sumR2)
    val corr = if (denom > 1e-9) (sumLr / denom).toFloat().coerceIn(-1f, 1f) else 1f
    val stereoMeanPow = 0.5 * (sumL2 + sumR2)
    val monoLossDb = if (stereoMeanPow > 1e-10 && sumMono2 > 1e-12) {
        (10.0 * kotlin.math.log10(sumMono2 / stereoMeanPow)).toFloat().coerceIn(-24f, 6f)
    } else {
        0f
    }
    val isMonoOk = corr >= 0.10f && monoLossDb >= -4.5f
    return StereoCompatibilityReport(
        correlation = corr,
        monoLossDb = monoLossDb,
        isMonoCompatible = isMonoOk,
    )
}

fun estimateStereoCompatibilityForOffsets(
    buffer: StereoAudioBuffer?,
    midAdjustPct: Float,
    sideBoostPct: Float,
    widthPct: Float,
): StereoCompatibilityReport {
    val midLevel = (1f + midAdjustPct / 100f).coerceIn(0.35f, 1.65f)
    val sideLevel = (1f + sideBoostPct / 100f).coerceIn(0.5f, 2.3f)
    val stereoWidth = (widthPct / 100f).coerceIn(0f, 1f)
    if (buffer == null || buffer.frameCount < 32) {
        val estR = ((midLevel * midLevel - (sideLevel * stereoWidth * 0.55f).let { it * it }) /
            max(0.01f, midLevel * midLevel + (sideLevel * stereoWidth * 0.55f).let { it * it })).coerceIn(-1f, 1f)
        return StereoCompatibilityReport(estR, if (estR < 0.1f) -5f else -1f, estR >= 0.10f)
    }
    val n = buffer.frameCount
    val sr = buffer.sampleRate.coerceAtLeast(8000)
    val haasSamples = if (stereoWidth <= 0.005f) 0 else max(1, (0.011f * sr * stereoWidth).roundToInt())
    val step = max(1, n / 4096)
    var sumLr = 0.0
    var sumL2 = 0.0
    var sumR2 = 0.0
    var sumMono2 = 0.0
    var idx = 0
    while (idx < n) {
        val rawL = buffer.left[idx]
        val rawR = buffer.right[idx]
        val delayedR = if (haasSamples == 0) rawR else buffer.right[max(0, idx - haasSamples)]
        val mid = 0.5f * (rawL + rawR) * midLevel
        val naturalSide = 0.5f * (rawL - rawR)
        val haasSide = if (haasSamples == 0) 0f else 0.5f * (rawL - delayedR) * stereoWidth
        val side = (naturalSide + haasSide * 0.65f) * sideLevel
        val outL = clamp(mid + side, -1f, 1f).toDouble()
        val outR = clamp(mid - side, -1f, 1f).toDouble()
        val m = 0.5 * (outL + outR)
        sumLr += outL * outR
        sumL2 += outL * outL
        sumR2 += outR * outR
        sumMono2 += m * m
        idx += step
    }
    val denom = sqrt(sumL2 * sumR2)
    val corr = if (denom > 1e-9) (sumLr / denom).toFloat().coerceIn(-1f, 1f) else 1f
    val stereoMeanPow = 0.5 * (sumL2 + sumR2)
    val monoLossDb = if (stereoMeanPow > 1e-10 && sumMono2 > 1e-12) {
        (10.0 * kotlin.math.log10(sumMono2 / stereoMeanPow)).toFloat().coerceIn(-24f, 6f)
    } else {
        0f
    }
    return StereoCompatibilityReport(
        correlation = corr,
        monoLossDb = monoLossDb,
        isMonoCompatible = corr >= 0.10f && monoLossDb >= -4.5f,
    )
}

fun buildSurroundStereoFfmpegFilter(
    midAdjustPct: Float,
    sideBoostPct: Float,
    widthPct: Float,
): Pair<String, String> {
    val midLevel = (1f + midAdjustPct / 100f).coerceIn(0.35f, 1.65f)
    val sideLevel = (1f + sideBoostPct / 100f).coerceIn(0.5f, 2.3f)
    val stereoWidth = (widthPct / 100f).coerceIn(0f, 1f)
    val mFmt = String.format(Locale.US, "%.2f", midLevel)
    val sFmt = String.format(Locale.US, "%.2f", sideLevel)
    val msTools = "stereotools=mlev=$mFmt:slev=$sFmt"
    val primary = if (stereoWidth > 0.02f) {
        val delayMs = String.format(Locale.US, "%.1f", 4f + stereoWidth * 18f)
        "aformat=channel_layouts=stereo,stereowiden=delay=$delayMs:feedback=0.22:crossfeed=0.35:drymix=0.82,$msTools,alimiter=limit=0.95"
    } else {
        "aformat=channel_layouts=stereo,$msTools,alimiter=limit=0.95"
    }
    val fallback = if (stereoWidth > 0.02f) {
        val extra = String.format(Locale.US, "%.2f", 1f + stereoWidth * 0.95f)
        "aformat=channel_layouts=stereo,$msTools,extrastereo=m=$extra,alimiter=limit=0.95"
    } else {
        "aformat=channel_layouts=stereo,$msTools,alimiter=limit=0.95"
    }
    return primary to fallback
}

fun splitAudioByTime(
    buffer: StereoAudioBuffer,
    splitFraction: Float,
): Pair<StereoAudioBuffer, StereoAudioBuffer> {
    val n = buffer.frameCount
    if (n < 4) return buffer to buffer
    val splitIdx = clamp(floor(splitFraction * n).toInt(), 1, n - 1)
    val l1 = buffer.left.copyOfRange(0, splitIdx)
    val r1 = buffer.right.copyOfRange(0, splitIdx)
    val l2 = buffer.left.copyOfRange(splitIdx, n)
    val r2 = buffer.right.copyOfRange(splitIdx, n)
    val sr = buffer.sampleRate.coerceAtLeast(8000)
    val fadeFrames1 = min(l1.size / 4, (0.004f * sr).roundToInt())
    if (fadeFrames1 >= 2) {
        val tailStart = l1.size - fadeFrames1
        for (i in 0 until fadeFrames1) {
            val g = cos((i.toDouble() / (fadeFrames1 - 1)) * PI * 0.5).toFloat()
            l1[tailStart + i] *= g
            r1[tailStart + i] *= g
        }
    }
    val fadeFrames2 = min(l2.size / 4, (0.004f * sr).roundToInt())
    if (fadeFrames2 >= 2) {
        for (i in 0 until fadeFrames2) {
            val g = sin((i.toDouble() / (fadeFrames2 - 1)) * PI * 0.5).toFloat()
            l2[i] *= g
            r2[i] *= g
        }
    }
    val first = StereoAudioBuffer(left = l1, right = r1, sampleRate = buffer.sampleRate)
    val second = StereoAudioBuffer(left = l2, right = r2, sampleRate = buffer.sampleRate)
    return first to second
}

fun splitStereoChannels(buffer: StereoAudioBuffer): Pair<StereoAudioBuffer, StereoAudioBuffer> {
    val leftOnly = StereoAudioBuffer(
        left = buffer.left.copyOf(),
        right = buffer.left.copyOf(),
        sampleRate = buffer.sampleRate,
    )
    val rightOnly = StereoAudioBuffer(
        left = buffer.right.copyOf(),
        right = buffer.right.copyOf(),
        sampleRate = buffer.sampleRate,
    )
    return leftOnly to rightOnly
}

fun buildAtempoChain(targetTempo: Double): String {
    var rem = targetTempo.coerceIn(0.125, 8.0)
    val stages = mutableListOf<String>()
    while (rem > 2.0 + 1e-4) {
        stages += "atempo=2.0"
        rem /= 2.0
    }
    while (rem < 0.5 - 1e-4) {
        stages += "atempo=0.5"
        rem /= 0.5
    }
    if (abs(rem - 1.0) > 0.002 || stages.isEmpty()) {
        stages += "atempo=${String.format(Locale.US, "%.4f", rem.coerceIn(0.5, 2.0))}"
    }
    return stages.joinToString(",")
}

/**
 * Returns Pair(primaryRubberbandFilter, fallbackSoxrAtempoFilter) for high-precision Pitch & Speed processing.
 */
fun buildPitchAndSpeedFfmpegChains(
    semitones: Float,
    tempoRatio: Float,
    preserveFormants: Boolean = true,
): Pair<String, String> {
    val safeSemi = clamp(semitones, -12f, 12f)
    val safeTempo = clamp(tempoRatio, 0.5f, 2.5f)
    val pitchFactor = 2.0.pow(safeSemi / 12.0).coerceIn(0.5, 2.0)
    val pFmt = String.format(Locale.US, "%.5f", pitchFactor)
    val sFmt = String.format(Locale.US, "%.4f", safeTempo)
    val formantArg = if (preserveFormants) "preserved" else "shifted"

    val rubberbandFilter = "rubberband=pitch=$pFmt:tempo=$sFmt:formant=$formantArg"

    val netTempo = (safeTempo / pitchFactor).coerceIn(0.125, 8.0)
    val atempoChain = buildAtempoChain(netTempo)
    val fallbackFilter = if (abs(safeSemi) >= 0.05f) {
        "aresample=44100,asetrate=44100*$pFmt,aresample=resampler=soxr:precision=28:osr=44100,$atempoChain,highpass=f=45,lowpass=f=18000,alimiter=limit=0.95"
    } else {
        "$atempoChain,highpass=f=45,lowpass=f=18000,alimiter=limit=0.95"
    }
    return rubberbandFilter to fallbackFilter
}

suspend fun applyPitchAndSpeedOffline(
    buffer: StereoAudioBuffer,
    semitones: Float,
    tempoRatio: Float,
    preserveFormants: Boolean = true,
    context: Context? = null,
    onProgress: (Float) -> Unit = {},
): StereoAudioBuffer = withContext(Dispatchers.Default) {
    val safeSemi = clamp(semitones, -12f, 12f)
    val safeTempo = clamp(tempoRatio, 0.5f, 2.5f)
    if (abs(safeSemi) < 0.05f && abs(safeTempo - 1.0f) < 0.015f) {
        onProgress(1f)
        return@withContext buffer
    }

    onProgress(0.15f)
    if (context != null && FfmpegKitBridge.isAvailable()) {
        val (primaryFilter, fallbackFilter) = buildPitchAndSpeedFfmpegChains(safeSemi, safeTempo, preserveFormants)
        val normSrc = normalizeToStudioStereo44100(buffer)
        val workDir = File(context.cacheDir, "stream_ops").apply { mkdirs() }
        val id = UUID.randomUUID().toString().take(8)
        val inWav = File(workDir, "ps_in_$id.wav")
        val outWav = File(workDir, "ps_out_$id.wav")
        try {
            withContext(Dispatchers.IO) { writeStereoWavFile(inWav, normSrc) }
            onProgress(0.35f)
            val cmd1 = "-y -i \"${inWav.absolutePath}\" -af \"$primaryFilter\" -ar 44100 -ac 2 \"${outWav.absolutePath}\""
            var ok = withContext(Dispatchers.IO) {
                FfmpegKitBridge.execute(cmd1, toolName = "PitchAndSpeedRubberband", logNonZeroFailure = false)
            }
            var usedFallback = false
            if (!ok || !outWav.exists() || outWav.length() <= 44L) {
                usedFallback = true
                onProgress(0.55f)
                val cmd2 = "-y -i \"${inWav.absolutePath}\" -af \"$fallbackFilter\" -ar 44100 -ac 2 \"${outWav.absolutePath}\""
                ok = withContext(Dispatchers.IO) {
                    FfmpegKitBridge.execute(cmd2, toolName = "PitchAndSpeedSoxrFallback", logNonZeroFailure = true)
                }
            }
            onProgress(0.85f)
            if (ok && outWav.exists() && outWav.length() > 44L) {
                AudioDecoder.decodeFile(outWav)?.let { decoded ->
                    val finalDecoded = if (usedFallback && preserveFormants && abs(safeSemi) >= 0.35f) {
                        val pitchFactor = 2.0.pow(safeSemi / 12.0).coerceIn(0.5, 2.0)
                        val compFormant = (1.0 / pitchFactor).coerceIn(0.58, 1.72)
                        StereoAudioBuffer(
                            left = shiftFormantsStft(decoded.left, compFormant),
                            right = shiftFormantsStft(decoded.right, compFormant),
                            sampleRate = decoded.sampleRate,
                        )
                    } else {
                        decoded
                    }
                    onProgress(1f)
                    return@withContext finalDecoded
                }
            }
        } finally {
            runCatching { inWav.delete() }
            runCatching { outWav.delete() }
        }
    }

    onProgress(0.55f)
    val kotlinResult = pitchAndSpeedKotlinHq(buffer, safeSemi, safeTempo, preserveFormants)
    onProgress(1f)
    kotlinResult
}

suspend fun applyPitchAndSpeedFileWithFfmpeg(
    context: Context,
    inputFile: File,
    semitones: Float,
    tempoRatio: Float,
    preserveFormants: Boolean = true,
    onProgress: (Float) -> Unit = {},
): File? = withContext(Dispatchers.IO) {
    if (!inputFile.exists() || !FfmpegKitBridge.isAvailable()) return@withContext null
    val (primaryFilter, fallbackFilter) = buildPitchAndSpeedFfmpegChains(semitones, tempoRatio, preserveFormants)
    onProgress(0.25f)
    val primaryOut = processAudioFileWithFfmpeg(
        context = context,
        inputFile = inputFile,
        audioFilter = primaryFilter,
        toolName = "PitchAndSpeedLargeRubberband",
    )
    if (primaryOut != null) {
        onProgress(1f)
        return@withContext primaryOut
    }
    onProgress(0.6f)
    val fallbackOut = processAudioFileWithFfmpeg(
        context = context,
        inputFile = inputFile,
        audioFilter = fallbackFilter,
        toolName = "PitchAndSpeedLargeSoxr",
    )
    if (fallbackOut != null) onProgress(1f)
    fallbackOut
}

enum class MergeStudioMode {
    SEQUENTIAL,
    CROSSFADE,
    OVERLAY,
}

data class MergeStudioParams(
    val mode: MergeStudioMode = MergeStudioMode.SEQUENTIAL,
    val primaryStartOffsetSec: Float = 0.0f,// 0.0 .. 60.0s
    val crossfadeSec: Float = 2.0f,         // 0.1 .. 15.0s
    val overlayFadeInSec: Float = 0.0f,     // 0.0 .. 15.0s
    val overlayFadeOutSec: Float = 0.0f,    // 0.0 .. 15.0s
    val overlayStartOffsetSec: Float = 0.0f,// 0.0 .. 60.0s
    val primaryLevelPct: Float = 0.0f,      // -60% .. +60% (0% = 1.0x)
    val secondaryLevelPct: Float = 0.0f,    // -60% .. +60% (0% = 1.0x overlay/bg or clip2)
    val autoDuckingEnabled: Boolean = false,
    val duckingStrengthPct: Float = 0.0f,   // 0% .. 100%
    val autoLevelingEnabled: Boolean = false,
    val levelingStrengthPct: Float = 0.0f,  // 0% .. 100%
    val echoTransitionEnabled: Boolean = false,
    val echoTransitionPct: Float = 0.0f,    // 0% .. 100%
    val gapWhooshEnabled: Boolean = false,
    val transitionGapSec: Float = 0.0f,     // 0.0 .. 10.0s
    val whooshIntensityPct: Float = 0.0f,   // 0% .. 100%
)

data class MergeTimelineLayout(
    val primaryStartMs: Long,
    val primaryDurationMs: Long,
    val secondaryStartMs: Long,
    val secondaryDurationMs: Long,
    val totalDurationMs: Long,
    val crossfadeActualMs: Long,
)

fun computeMergeTimelineLayout(
    primaryDurMs: Long,
    secondaryDurMs: Long,
    params: MergeStudioParams,
): MergeTimelineLayout {
    val pDur = primaryDurMs.coerceAtLeast(0L)
    val sDur = secondaryDurMs.coerceAtLeast(0L)
    val pStartMs = (params.primaryStartOffsetSec.coerceIn(0f, 60f) * 1000f).roundToLong()

    return when (params.mode) {
        MergeStudioMode.SEQUENTIAL -> {
            val gapMs = (params.transitionGapSec.coerceIn(0f, 10f) * 1000f).roundToLong()
            val sStartMs = pStartMs + pDur + gapMs
            val totalMs = max(1L, sStartMs + sDur)
            MergeTimelineLayout(
                primaryStartMs = pStartMs,
                primaryDurationMs = pDur,
                secondaryStartMs = sStartMs,
                secondaryDurationMs = sDur,
                totalDurationMs = totalMs,
                crossfadeActualMs = 0L,
            )
        }

        MergeStudioMode.CROSSFADE -> {
            val reqXfMs = (params.crossfadeSec.coerceIn(0.1f, 15f) * 1000f).roundToLong()
            val maxXfMs = minOf(
                (pDur * 0.85).roundToLong(),
                (sDur * 0.85).roundToLong(),
            ).coerceAtLeast(0L)
            val actualXfMs = min(reqXfMs, maxXfMs)
            val sStartMs = (pStartMs + pDur - actualXfMs).coerceAtLeast(pStartMs)
            val totalMs = maxOf(1L, pStartMs + pDur, sStartMs + sDur)
            MergeTimelineLayout(
                primaryStartMs = pStartMs,
                primaryDurationMs = pDur,
                secondaryStartMs = sStartMs,
                secondaryDurationMs = sDur,
                totalDurationMs = totalMs,
                crossfadeActualMs = actualXfMs,
            )
        }

        MergeStudioMode.OVERLAY -> {
            val sStartMs = (params.overlayStartOffsetSec.coerceIn(0f, 60f) * 1000f).roundToLong()
            val totalMs = maxOf(1L, pStartMs + pDur, sStartMs + sDur)
            MergeTimelineLayout(
                primaryStartMs = pStartMs,
                primaryDurationMs = pDur,
                secondaryStartMs = sStartMs,
                secondaryDurationMs = sDur,
                totalDurationMs = totalMs,
                crossfadeActualMs = 0L,
            )
        }
    }
}

private fun prependSilenceFrames(buffer: StereoAudioBuffer, delayFrames: Int): StereoAudioBuffer {
    if (delayFrames <= 0) return buffer
    val outL = FloatArray(delayFrames + buffer.frameCount)
    val outR = FloatArray(delayFrames + buffer.frameCount)
    System.arraycopy(buffer.left, 0, outL, delayFrames, buffer.frameCount)
    System.arraycopy(buffer.right, 0, outR, delayFrames, buffer.frameCount)
    return StereoAudioBuffer(outL, outR, buffer.sampleRate)
}

/**
 * Unified Merge & Overlay Studio processor:
 * - Sequential merge (with optional Echo transition & Gap + Whoosh transition)
 * - Smart End-to-Start Crossfade (0.1s..15.0s via FFmpegKit `acrossfade` or smooth Kotlin crossfade)
 * - Free Overlay & Mix with Fade-In, Fade-Out, Start-Offset, Auto-Ducking, & Auto-Leveling (`loudnorm`)
 */
suspend fun executeUnifiedMergeOverlay(
    context: Context,
    primary: StereoAudioBuffer,
    secondary: StereoAudioBuffer,
    params: MergeStudioParams,
): StereoAudioBuffer = withContext(Dispatchers.Default) {
    val normPrimary = normalizeToStudioStereo44100(primary)
    val alignedSec = normalizeToStudioStereo44100(secondary)
    val sr = TARGET_SAMPLE_RATE

    val gain1 = (1f + params.primaryLevelPct / 100f).coerceIn(0.1f, 2.0f)
    val gain2 = (1f + params.secondaryLevelPct / 100f).coerceIn(0.1f, 2.0f)

    var p1 = if (abs(gain1 - 1f) > 0.01f) StereoAudioBuffer(
        FloatArray(normPrimary.frameCount) { (normPrimary.left[it] * gain1).coerceIn(-1.8f, 1.8f) },
        FloatArray(normPrimary.frameCount) { (normPrimary.right[it] * gain1).coerceIn(-1.8f, 1.8f) },
        sr,
    ) else normPrimary

    var p2 = if (abs(gain2 - 1f) > 0.01f) StereoAudioBuffer(
        FloatArray(alignedSec.frameCount) { (alignedSec.left[it] * gain2).coerceIn(-1.8f, 1.8f) },
        FloatArray(alignedSec.frameCount) { (alignedSec.right[it] * gain2).coerceIn(-1.8f, 1.8f) },
        sr,
    ) else alignedSec

    // Service 2: Smart Auto-Leveling (equalize perceived loudness of both clips before combining)
    if (params.autoLevelingEnabled) {
        val blend = clamp(params.levelingStrengthPct / 100f, 0f, 1f).let { if (it < 0.01f) 0.7f else it }
        p1 = applySmartAutoLevel(p1, strength = blend)
        p2 = applySmartAutoLevel(p2, strength = blend)
    }

    // Service 3: Echo & Beat Transition on tail of clip 1
    if (params.echoTransitionEnabled && params.mode != MergeStudioMode.OVERLAY) {
        val mix = clamp(params.echoTransitionPct / 100f, 0f, 1f).let { if (it < 0.01f) 0.55f else it }
        p1 = applyTailEchoTransition(p1, intensity = mix)
    }

    val p1OffsetFrames = (clamp(params.primaryStartOffsetSec, 0f, 60f) * sr).roundToInt()

    val combined = when (params.mode) {
        MergeStudioMode.SEQUENTIAL -> {
            val p1WithStart = prependSilenceFrames(p1, p1OffsetFrames)
            val gapSec = clamp(params.transitionGapSec, 0f, 10f)
            val midGap = when {
                params.gapWhooshEnabled && gapSec > 0.05f -> synthesizeTransitionGapWhoosh(
                    durationSec = gapSec,
                    whooshIntensity = clamp(params.whooshIntensityPct / 100f, 0f, 1f),
                    sampleRate = sr,
                )
                gapSec > 0.05f -> generateSilence(gapSec, sampleRate = sr)
                else -> null
            }
            if (midGap != null) {
                val p1PlusGap = mergeClipsStreamingOnDisk(context, p1WithStart, midGap, crossfadeMs = 20)
                mergeClipsStreamingOnDisk(context, p1PlusGap, p2, crossfadeMs = 20)
            } else {
                mergeClipsStreamingOnDisk(context, p1WithStart, p2, crossfadeMs = 20)
            }
        }

        MergeStudioMode.CROSSFADE -> {
            val p1WithStart = prependSilenceFrames(p1, p1OffsetFrames)
            val xfadeMs = (clamp(params.crossfadeSec, 0.1f, 15.0f) * 1000f).roundToInt()
            if (params.gapWhooshEnabled && params.whooshIntensityPct > 2f) {
                val merged = mergeClipsStreamingOnDisk(
                    context,
                    p1WithStart,
                    p2,
                    crossfadeMs = xfadeMs,
                    maxCrossfadeFraction = 0.85f,
                )
                overlayWhooshAtFrame(
                    target = merged,
                    centerFrame = (p1WithStart.frameCount - (xfadeMs * sr / 2000)).coerceIn(0, max(0, merged.frameCount - 1)),
                    whooshIntensity = clamp(params.whooshIntensityPct / 100f, 0f, 1f),
                )
            } else {
                mergeClipsStreamingOnDisk(
                    context,
                    p1WithStart,
                    p2,
                    crossfadeMs = xfadeMs,
                    maxCrossfadeFraction = 0.85f,
                )
            }
        }

        MergeStudioMode.OVERLAY -> {
            var shapedSec = p2
            if (params.overlayFadeInSec > 0.05f) {
                shapedSec = applyFadeIn(shapedSec, params.overlayFadeInSec)
            }
            if (params.overlayFadeOutSec > 0.05f) {
                shapedSec = applyFadeOut(shapedSec, params.overlayFadeOutSec)
            }
            val offsetFrames = (clamp(params.overlayStartOffsetSec, 0f, 60f) * sr).roundToInt()
            if (params.autoDuckingEnabled) {
                val duckStrength = clamp(params.duckingStrengthPct / 100f, 0f, 1f).let { if (it < 0.01f) 0.7f else it }
                mixWithSidechainAutoDuckingWithOffset(
                    context = context,
                    voice = p1,
                    music = shapedSec,
                    offsetFrames = offsetFrames,
                    musicBaseVolume = (0.45f * gain2).coerceIn(0.05f, 1.35f),
                    duckDepth = duckStrength,
                    voiceOffsetFrames = p1OffsetFrames,
                )
            } else {
                overlayWithOffset(p1, shapedSec, offsetFrames = offsetFrames, primaryOffsetFrames = p1OffsetFrames)
            }
        }
    }

    val leveled = if (params.autoLevelingEnabled) {
        val id = UUID.randomUUID().toString().take(8)
        val dir = File(context.cacheDir, "stream_ops").apply { mkdirs() }
        val inF = File(dir, "ln_in_$id.wav")
        val outF = File(dir, "ln_out_$id.wav")
        try {
            writeStereoWavFile(inF, combined)
            val cmd = "-y -i \"${inF.absolutePath}\" -af \"loudnorm=I=-16:TP=-1.5:LRA=11,alimiter=limit=0.95:attack=5:release=50\" -ar $sr -ac 2 \"${outF.absolutePath}\""
            val ok = FfmpegKitBridge.execute(cmd, toolName = "AutoLeveling", logNonZeroFailure = false)
            if (ok && outF.exists() && outF.length() > 44L) {
                AudioDecoder.decodeFile(outF) ?: normalizeAudio(combined, -1.5f)
            } else {
                normalizeAudio(combined, -1.5f)
            }
        } finally {
            runCatching { inF.delete() }
            runCatching { outF.delete() }
        }
    } else {
        combined
    }

    // Final true-peak soft ceiling (-0.4 dBFS) to guarantee zero harsh digital clipping
    val maxPeak = leveled.left.fold(0f) { acc, v -> max(acc, abs(v)) }
        .let { p -> leveled.right.fold(p) { acc, v -> max(acc, abs(v)) } }
    if (maxPeak > 0.96f) {
        val outL = FloatArray(leveled.frameCount) { idx ->
            val v = leveled.left[idx]
            if (abs(v) <= 0.85f) v else (0.95f * tanh(v.toDouble())).toFloat()
        }
        val outR = FloatArray(leveled.frameCount) { idx ->
            val v = leveled.right[idx]
            if (abs(v) <= 0.85f) v else (0.95f * tanh(v.toDouble())).toFloat()
        }
        StereoAudioBuffer(outL, outR, sr)
    } else {
        leveled
    }
}

private fun applySmartAutoLevel(buffer: StereoAudioBuffer, strength: Float): StereoAudioBuffer {
    val n = buffer.frameCount
    if (n <= 0 || strength <= 0.01f) return buffer
    var sumSq = 0.0
    for (i in 0 until n) {
        val m = 0.5f * (buffer.left[i] + buffer.right[i])
        sumSq += m * m
    }
    val rms = sqrt(sumSq / n).toFloat().coerceAtLeast(1e-4f)
    val targetRms = 0.22f
    val rawGain = (targetRms / rms).coerceIn(0.35f, 3.2f)
    val gain = 1f + (rawGain - 1f) * strength
    val outL = FloatArray(n) { i -> tanh((buffer.left[i] * gain).toDouble()).toFloat() }
    val outR = FloatArray(n) { i -> tanh((buffer.right[i] * gain).toDouble()).toFloat() }
    return StereoAudioBuffer(outL, outR, buffer.sampleRate)
}

private fun applyTailEchoTransition(buffer: StereoAudioBuffer, intensity: Float): StereoAudioBuffer {
    val n = buffer.frameCount
    val sr = buffer.sampleRate.coerceAtLeast(8000)
    if (n < sr / 4 || intensity <= 0.01f) return buffer
    val tailStart = max(0, n - (1.8f * sr).roundToInt())
    val extraTail = (1.1f * sr).roundToInt()
    val outFrames = n + extraTail
    val outL = FloatArray(outFrames)
    val outR = FloatArray(outFrames)
    System.arraycopy(buffer.left, 0, outL, 0, n)
    System.arraycopy(buffer.right, 0, outR, 0, n)

    val taps = listOf(
        (0.24f * sr).roundToInt() to 0.52f * intensity,
        (0.48f * sr).roundToInt() to 0.34f * intensity,
        (0.76f * sr).roundToInt() to 0.20f * intensity,
    )
    for ((delay, gain) in taps) {
        for (i in tailStart until n) {
            val dst = i + delay
            if (dst < outFrames) {
                val env = (i - tailStart).toFloat() / max(1, n - tailStart)
                outL[dst] = clamp(outL[dst] + buffer.left[i] * gain * env, -1f, 1f)
                outR[dst] = clamp(outR[dst] + buffer.right[i] * gain * env, -1f, 1f)
            }
        }
    }
    return StereoAudioBuffer(outL, outR, sr)
}

private fun synthesizeTransitionGapWhoosh(
    durationSec: Float,
    whooshIntensity: Float,
    sampleRate: Int,
): StereoAudioBuffer {
    val frames = max(sampleRate / 10, (clamp(durationSec, 0.05f, 10f) * sampleRate).roundToInt())
    val outL = FloatArray(frames)
    val outR = FloatArray(frames)
    if (whooshIntensity <= 0.02f) return StereoAudioBuffer(outL, outR, sampleRate)

    val rng = Random(42L)
    var lpL = 0f
    var lpR = 0f
    for (i in 0 until frames) {
        val pos = i.toFloat() / max(1, frames - 1)
        val env = sin(pos * PI).toFloat().pow(1.4f)
        val alpha = 0.03f + 0.32f * env
        val nL = rng.nextFloat() * 2f - 1f
        val nR = rng.nextFloat() * 2f - 1f
        lpL += alpha * (nL - lpL)
        lpR += alpha * (nR - lpR)
        val panL = cos(pos * PI * 0.5).toFloat()
        val panR = sin(pos * PI * 0.5).toFloat()
        val scale = 0.28f * whooshIntensity * env
        outL[i] = clamp(lpL * scale * (0.6f + panL * 0.6f), -1f, 1f)
        outR[i] = clamp(lpR * scale * (0.6f + panR * 0.6f), -1f, 1f)
    }
    return StereoAudioBuffer(outL, outR, sampleRate)
}

private fun overlayWhooshAtFrame(
    target: StereoAudioBuffer,
    centerFrame: Int,
    whooshIntensity: Float,
): StereoAudioBuffer {
    val sr = target.sampleRate
    val whoosh = synthesizeTransitionGapWhoosh(1.1f, whooshIntensity, sr)
    val start = max(0, centerFrame - whoosh.frameCount / 2)
    val outL = target.left.copyOf()
    val outR = target.right.copyOf()
    for (i in 0 until whoosh.frameCount) {
        val dst = start + i
        if (dst >= outL.size) break
        outL[dst] = tanh((outL[dst] + whoosh.left[i]).toDouble()).toFloat()
        outR[dst] = tanh((outR[dst] + whoosh.right[i]).toDouble()).toFloat()
    }
    return StereoAudioBuffer(outL, outR, sr)
}

private fun overlayWithOffset(
    primary: StereoAudioBuffer,
    secondary: StereoAudioBuffer,
    offsetFrames: Int,
    primaryOffsetFrames: Int = 0,
): StereoAudioBuffer {
    val safeP1Offset = max(0, primaryOffsetFrames)
    val safeP2Offset = max(0, offsetFrames)
    val outFrames = maxOf(
        1,
        safeP1Offset + primary.frameCount,
        safeP2Offset + secondary.frameCount,
    )
    val outL = FloatArray(outFrames)
    val outR = FloatArray(outFrames)
    if (primary.frameCount > 0) {
        System.arraycopy(primary.left, 0, outL, safeP1Offset, primary.frameCount)
        System.arraycopy(primary.right, 0, outR, safeP1Offset, primary.frameCount)
    }
    for (i in 0 until secondary.frameCount) {
        val dst = safeP2Offset + i
        outL[dst] = tanh((outL[dst] + secondary.left[i]).toDouble()).toFloat()
        outR[dst] = tanh((outR[dst] + secondary.right[i]).toDouble()).toFloat()
    }
    return StereoAudioBuffer(outL, outR, primary.sampleRate)
}

suspend fun mixWithSidechainAutoDuckingWithOffset(
    context: Context,
    voice: StereoAudioBuffer,
    music: StereoAudioBuffer,
    offsetFrames: Int,
    musicBaseVolume: Float = 0.4f,
    duckDepth: Float = 0.7f,
    voiceOffsetFrames: Int = 0,
): StereoAudioBuffer = withContext(Dispatchers.Default) {
    val sr = voice.sampleRate.coerceAtLeast(8000)
    val safeVoiceOffset = max(0, voiceOffsetFrames)
    val safeOffset = max(0, offsetFrames)
    val totalFrames = maxOf(1, safeVoiceOffset + voice.frameCount, safeOffset + music.frameCount)
    val alignedMusic = if (music.sampleRate == sr) music else resampleStereo(music, sr)

    // Attempt FFmpegKit sidechaincompress when offsets are 0 and FFmpegKit is bundled
    if (safeOffset == 0 && safeVoiceOffset == 0 && FfmpegKitBridge.isAvailable()) {
        val id = UUID.randomUUID().toString().take(8)
        val dir = File(context.cacheDir, "stream_ops").apply { mkdirs() }
        val vWav = File(dir, "sc_v_$id.wav")
        val mWav = File(dir, "sc_m_$id.wav")
        val outWav = File(dir, "sc_out_$id.wav")
        try {
            writeStereoWavFile(vWav, voice)
            writeStereoWavFile(mWav, alignedMusic)
            val ratio = (2.0f + 8.0f * duckDepth).coerceIn(2f, 12f)
            val cmd = "-y -i \"${vWav.absolutePath}\" -i \"${mWav.absolutePath}\" " +
                "-filter_complex \"[1:a]volume=${"%.2f".format(Locale.US, musicBaseVolume)}[bg0];" +
                "[bg0][0:a]sidechaincompress=threshold=0.02:ratio=${"%.1f".format(Locale.US, ratio)}:attack=180:release=900[bg];" +
                "[0:a][bg]amix=inputs=2:duration=longest:normalize=0\" -ar $sr -ac 2 \"${outWav.absolutePath}\""
            val ok = FfmpegKitBridge.execute(cmd, toolName = "AutoDucking", logNonZeroFailure = false)
            if (ok && outWav.exists() && outWav.length() > 44L) {
                AudioDecoder.decodeFile(outWav)?.let { return@withContext it }
            }
        } finally {
            runCatching { vWav.delete() }
            runCatching { mWav.delete() }
            runCatching { outWav.delete() }
        }
    }

    val outL = FloatArray(totalFrames)
    val outR = FloatArray(totalFrames)
    val attackCoeff = kotlin.math.exp(-1.0 / (180f * 0.001f * sr)).toFloat()
    val releaseCoeff = kotlin.math.exp(-1.0 / (900f * 0.001f * sr)).toFloat()
    val safeThresh = 0.02f
    val minGain = (1f - duckDepth * 0.85f).coerceIn(0.12f, 1.0f)

    var env = 0f
    var duckGain = 1.0f
    val gainAttack = kotlin.math.exp(-1.0 / (0.045f * sr)).toFloat()
    val gainRelease = kotlin.math.exp(-1.0 / (0.350f * sr)).toFloat()

    for (i in 0 until totalFrames) {
        if (i % 16384 == 0) yield()
        val vIdx = i - safeVoiceOffset
        val vL = if (vIdx in 0 until voice.frameCount) voice.left[vIdx] else 0f
        val vR = if (vIdx in 0 until voice.frameCount) voice.right[vIdx] else 0f
        val det = max(abs(vL), abs(vR))
        env = if (det > env) attackCoeff * env + (1f - attackCoeff) * det else releaseCoeff * env + (1f - releaseCoeff) * det
        val targetDuck = if (env > safeThresh) {
            val comp = (env / safeThresh).toDouble().pow(1.0 / (2.0 + 6.0 * duckDepth) - 1.0).toFloat()
            clamp(comp, minGain, 1.0f)
        } else {
            1.0f
        }
        duckGain = if (targetDuck < duckGain) {
            gainAttack * duckGain + (1f - gainAttack) * targetDuck
        } else {
            gainRelease * duckGain + (1f - gainRelease) * targetDuck
        }

        val mIdx = i - safeOffset
        val bgL = if (mIdx in 0 until alignedMusic.frameCount) alignedMusic.left[mIdx] * musicBaseVolume * duckGain else 0f
        val bgR = if (mIdx in 0 until alignedMusic.frameCount) alignedMusic.right[mIdx] * musicBaseVolume * duckGain else 0f
        outL[i] = tanh((vL + bgL).toDouble()).toFloat()
        outR[i] = tanh((vR + bgR).toDouble()).toFloat()
    }
    StereoAudioBuffer(outL, outR, sr)
}

/**
 * Smart Background Music Mixer with Sidechain Auto-Ducking.
 */
suspend fun mixWithSidechainAutoDucking(
    voice: StereoAudioBuffer,
    music: StereoAudioBuffer,
    musicBaseVolume: Float = 0.28f,
    voiceGain: Float = 1.0f,
    threshold: Float = 0.02f,
    ratio: Float = 6.0f,
    attackMs: Float = 200f,
    releaseMs: Float = 1000f,
): StereoAudioBuffer = withContext(Dispatchers.Default) {
    val n = voice.frameCount
    if (n <= 0) return@withContext voice
    val sr = voice.sampleRate.coerceAtLeast(8000)

    val alignedMusic = if (music.sampleRate == sr) music else resampleStereo(music, sr)
    val musicFrames = alignedMusic.frameCount
    if (musicFrames <= 0) return@withContext applyGainDb(voice, (20.0 * kotlin.math.log10(max(voiceGain, 0.01f))).toFloat())

    val outL = FloatArray(n)
    val outR = FloatArray(n)

    val attackCoeff = kotlin.math.exp(-1.0 / (max(1f, attackMs) * 0.001f * sr)).toFloat()
    val releaseCoeff = kotlin.math.exp(-1.0 / (max(1f, releaseMs) * 0.001f * sr)).toFloat()
    val safeThresh = max(0.002f, threshold)
    val safeRatio = max(1.0f, ratio)
    val baseBg = clamp(musicBaseVolume, 0f, 1.5f)
    val fadeLoopFrames = min(sr / 4, musicFrames / 8)

    var env = 0f
    var duckGain = 1.0f
    val gainAttackCoeff = kotlin.math.exp(-1.0 / (0.045f * sr)).toFloat()
    val gainReleaseCoeff = kotlin.math.exp(-1.0 / (0.350f * sr)).toFloat()

    for (i in 0 until n) {
        if (i % 16384 == 0) yield()
        val vL = voice.left[i] * voiceGain
        val vR = voice.right[i] * voiceGain
        val detector = max(abs(vL), abs(vR))

        env = if (detector > env) {
            attackCoeff * env + (1f - attackCoeff) * detector
        } else {
            releaseCoeff * env + (1f - releaseCoeff) * detector
        }

        val targetDuck = if (env > safeThresh) {
            val overRatio = env / safeThresh
            val compressed = overRatio.toDouble().pow(1.0 / safeRatio - 1.0).toFloat()
            clamp(compressed, 0.14f, 1.0f)
        } else {
            1.0f
        }

        duckGain = if (targetDuck < duckGain) {
            gainAttackCoeff * duckGain + (1f - gainAttackCoeff) * targetDuck
        } else {
            gainReleaseCoeff * duckGain + (1f - gainReleaseCoeff) * targetDuck
        }

        val mIdx = i % musicFrames
        val loopEnv = if (musicFrames > fadeLoopFrames * 2 && fadeLoopFrames > 0) {
            when {
                mIdx < fadeLoopFrames -> mIdx.toFloat() / fadeLoopFrames
                mIdx > musicFrames - fadeLoopFrames -> (musicFrames - mIdx).toFloat() / fadeLoopFrames
                else -> 1f
            }
        } else {
            1f
        }

        val bgScale = baseBg * duckGain * loopEnv
        val bgL = alignedMusic.left[mIdx] * bgScale
        val bgR = alignedMusic.right[mIdx] * bgScale

        outL[i] = tanh((vL + bgL).toDouble()).toFloat()
        outR[i] = tanh((vR + bgR).toDouble()).toFloat()
    }

    StereoAudioBuffer(outL, outR, sr)
}

sealed interface RingtoneSaveResult {
    data class Saved(val uri: Uri, val appliedAsDefault: Boolean, val needsWriteSettings: Boolean) : RingtoneSaveResult
    data class Error(val message: String) : RingtoneSaveResult
}

suspend fun saveAndSetRingtone(
    context: Context,
    buffer: StereoAudioBuffer,
    title: String,
    setAsDefault: Boolean,
): RingtoneSaveResult = withContext(Dispatchers.IO) {
    val cleanTitle = title.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_").ifEmpty { "Sonix_Ringtone" }
    val exported = exportRenderedAudioToFile(
        context = context,
        buffer = buffer,
        format = com.example.model.ExportAudioFormat.M4A,
        baseFileName = cleanTitle,
        bitrateKbps = 320,
    )

    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "${cleanTitle}.${exported.extension}")
        put(MediaStore.MediaColumns.TITLE, cleanTitle)
        put(MediaStore.MediaColumns.MIME_TYPE, exported.mimeType)
        put(MediaStore.Audio.Media.IS_RINGTONE, 1)
        put(MediaStore.Audio.Media.IS_NOTIFICATION, 0)
        put(MediaStore.Audio.Media.IS_ALARM, 0)
        put(MediaStore.Audio.Media.IS_MUSIC, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_RINGTONES)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }

    val uri = runCatching {
        resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
    }.getOrNull() ?: return@withContext RingtoneSaveResult.Error("تعذر حفظ النغمة في مجلد الرنين")

    return@withContext try {
        resolver.openOutputStream(uri)?.use { out ->
            exported.file.inputStream().use { inp -> inp.copyTo(out) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val finish = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(uri, finish, null, null)
        }
        var applied = false
        var needsPerm = false
        if (setAsDefault) {
            if (Settings.System.canWrite(context)) {
                runCatching {
                    RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, uri)
                    applied = true
                }
            } else {
                needsPerm = true
            }
        }
        RingtoneSaveResult.Saved(uri = uri, appliedAsDefault = applied, needsWriteSettings = needsPerm)
    } catch (t: Throwable) {
        RingtoneSaveResult.Error(t.message ?: "خطأ أثناء كتابة ملف النغمة")
    }
}

suspend fun stageLargeFileToCleanAsciiCache(
    context: Context,
    uploadedFile: File,
    prefix: String = "large",
): File = withContext(Dispatchers.IO) {
    val stagedDir = File(context.cacheDir, "staged_inputs").apply { mkdirs() }
    val isAsciiPath = uploadedFile.name.all { ch -> ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9' || ch == '_' || ch == '-' || ch == '.' }
    if (isAsciiPath && uploadedFile.parentFile == stagedDir && uploadedFile.exists() && uploadedFile.length() > 0L) {
        return@withContext uploadedFile
    }
    val cleanExt = uploadedFile.extension.lowercase()
        .replace(Regex("[^a-z0-9]"), "")
        .take(8)
        .ifBlank { "audio" }
    val target = File(stagedDir, "${prefix}_${UUID.randomUUID()}.$cleanExt")
    if (uploadedFile.renameTo(target) && target.exists() && target.length() > 0L) {
        return@withContext target
    }
    uploadedFile.inputStream().buffered(65536).use { inp ->
        java.io.BufferedOutputStream(java.io.FileOutputStream(target), 65536).use { out ->
            inp.copyTo(out, 65536)
            out.flush()
        }
    }
    runCatching { uploadedFile.delete() }
    target
}

suspend fun processAudioFileWithFfmpeg(
    context: Context,
    inputFile: File,
    audioFilter: String,
    toolName: String,
    preInputArgs: String = "",
    postInputArgs: String = "",
): File? = withContext(Dispatchers.IO) {
    if (!inputFile.exists() || !FfmpegKitBridge.isAvailable()) return@withContext null
    val safeIn = com.example.audio.cache.SmartCacheManager.ensureSafeFfmpegInputFile(context, inputFile)
    val stagedDir = File(context.cacheDir, "staged_inputs").apply { mkdirs() }
    val outFile = File(stagedDir, "ff_proc_${UUID.randomUUID()}.m4a")
    val filterPart = if (audioFilter.isNotBlank()) "-af \"$audioFilter\"" else ""
    val cmd = buildString {
        append("-y ")
        if (preInputArgs.isNotBlank()) append("$preInputArgs ")
        append("-i \"${safeIn.absolutePath}\" ")
        if (postInputArgs.isNotBlank()) append("$postInputArgs ")
        if (filterPart.isNotBlank()) append("$filterPart ")
        append("-vn -ar 44100 -ac 2 -c:a aac -b:a 192k \"${outFile.absolutePath}\"")
    }
    val ok = FfmpegKitBridge.execute(cmd, toolName = toolName, logNonZeroFailure = true)
    if (safeIn != inputFile) runCatching { safeIn.delete() }
    if (ok && outFile.exists() && outFile.length() > 512L) {
        outFile
    } else {
        runCatching { outFile.delete() }
        null
    }
}

suspend fun mixAudioFilesWithFfmpeg(
    context: Context,
    primaryFile: File,
    secondaryFile: File,
    primaryGain: Float = 1f,
    secondaryGain: Float = 0.25f,
    autoDucking: Boolean = false,
    mode: MergeStudioMode = MergeStudioMode.OVERLAY,
    crossfadeSec: Float = 2f,
    offsetSec: Float = 0f,
    outFile: File? = null,
    primaryStartOffsetSec: Float = 0f,
    overlayFadeInSec: Float = 0f,
    overlayFadeOutSec: Float = 0f,
    secondaryDurationSec: Float = 0f,
    transitionGapSec: Float = 0f,
    autoLeveling: Boolean = false,
    duckingStrengthPct: Float = 65f,
    echoTransitionEnabled: Boolean = false,
    echoTransitionPct: Float = 0f,
    gapWhooshEnabled: Boolean = false,
    whooshIntensityPct: Float = 0f,
    levelingStrengthPct: Float = 70f,
    primaryDurationSec: Float = 0f,
): File? = withContext(Dispatchers.IO) {
    if (!primaryFile.exists() || !secondaryFile.exists() || !FfmpegKitBridge.isAvailable()) {
        return@withContext null
    }
    val safe1 = com.example.audio.cache.SmartCacheManager.ensureSafeFfmpegInputFile(context, primaryFile)
    val safe2 = com.example.audio.cache.SmartCacheManager.ensureSafeFfmpegInputFile(context, secondaryFile)
    val stagedDir = File(context.cacheDir, "staged_inputs").apply { mkdirs() }
    val target = outFile ?: File(stagedDir, "ff_mix_${UUID.randomUUID()}.m4a")
    val g1 = String.format(Locale.US, "%.2f", primaryGain.coerceIn(0.05f, 2.5f))
    val g2 = String.format(Locale.US, "%.2f", secondaryGain.coerceIn(0.05f, 2.5f))
    val p1DelayMs = (primaryStartOffsetSec.coerceIn(0f, 60f) * 1000f).roundToInt()
    val p1DelayFilter = if (p1DelayMs > 0) ",adelay=${p1DelayMs}|${p1DelayMs}" else ""
    val p2DelayMs = (offsetSec.coerceIn(0f, 60f) * 1000f).roundToInt()
    val p2DelayFilter = if (p2DelayMs > 0) ",adelay=${p2DelayMs}|${p2DelayMs}" else ""

    val probedP1DurSec = if (primaryDurationSec > 0.1f) {
        primaryDurationSec
    } else {
        (RealWaveformExtractor.extractTrueDurationMs(safe1) / 1000f).coerceAtLeast(0f)
    }
    val probedP2DurSec = if (secondaryDurationSec > 0.1f) {
        secondaryDurationSec
    } else {
        (RealWaveformExtractor.extractTrueDurationMs(safe2) / 1000f).coerceAtLeast(0f)
    }

    val fadeInFilter = if (overlayFadeInSec > 0.05f) {
        val safeInDur = if (probedP2DurSec > 0.1f) min(overlayFadeInSec, probedP2DurSec * 0.9f) else overlayFadeInSec
        ",afade=t=in:st=0:d=${String.format(Locale.US, "%.2f", safeInDur.coerceAtLeast(0.05f))}"
    } else {
        ""
    }
    val fadeOutFilter = if (overlayFadeOutSec > 0.05f && probedP2DurSec > 0.1f) {
        val safeOutDur = min(overlayFadeOutSec, probedP2DurSec * 0.9f).coerceAtLeast(0.05f)
        val stOut = (probedP2DurSec - safeOutDur).coerceAtLeast(0f)
        ",afade=t=out:st=${String.format(Locale.US, "%.2f", stOut)}:d=${String.format(Locale.US, "%.2f", safeOutDur)}"
    } else {
        ""
    }

    val echoTailFilter = if (echoTransitionEnabled && mode != MergeStudioMode.OVERLAY) {
        val echoAmt = clamp(echoTransitionPct / 100f, 0f, 1f).let { if (it < 0.01f) 0.55f else it }
        val d1 = String.format(Locale.US, "%.2f", (0.28f * echoAmt).coerceIn(0.05f, 0.55f))
        val d2 = String.format(Locale.US, "%.2f", (0.16f * echoAmt).coerceIn(0.03f, 0.35f))
        ",aecho=0.82:0.85:240|480:${d1}|${d2}"
    } else {
        ""
    }

    var whooshWav: File? = null
    val needsGapWhooshFile = (mode == MergeStudioMode.SEQUENTIAL && gapWhooshEnabled && transitionGapSec > 0.05f && whooshIntensityPct > 2f)
    if (needsGapWhooshFile) {
        val tmpWhoosh = File(stagedDir, "ff_whoosh_${UUID.randomUUID()}.wav")
        val buf = synthesizeTransitionGapWhoosh(
            durationSec = transitionGapSec.coerceIn(0.05f, 10f),
            whooshIntensity = clamp(whooshIntensityPct / 100f, 0f, 1f),
            sampleRate = TARGET_SAMPLE_RATE,
        )
        writeStereoWavFile(tmpWhoosh, buf)
        if (tmpWhoosh.exists() && tmpWhoosh.length() > 44L) {
            whooshWav = tmpWhoosh
        }
    }

    val needsLimiter = !autoLeveling
    val coreOutLabel = if (autoLeveling) "mixraw" else "prelim"
    val coreGraph = when (mode) {
        MergeStudioMode.SEQUENTIAL -> {
            if (whooshWav != null) {
                "[0:a]aresample=44100,aformat=channel_layouts=stereo,volume=$g1$p1DelayFilter$echoTailFilter[a0];" +
                    "[2:a]aresample=44100,aformat=channel_layouts=stereo[w0];" +
                    "[1:a]aresample=44100,aformat=channel_layouts=stereo,volume=$g2[a1];" +
                    "[a0][w0][a1]concat=n=3:v=0:a=1[$coreOutLabel]"
            } else {
                val gapPad = if (transitionGapSec > 0.05f) {
                    ",apad=pad_dur=${String.format(Locale.US, "%.2f", transitionGapSec.coerceIn(0f, 10f))}"
                } else {
                    ""
                }
                "[0:a]aresample=44100,aformat=channel_layouts=stereo,volume=$g1$p1DelayFilter$echoTailFilter$gapPad[a0];" +
                    "[1:a]aresample=44100,aformat=channel_layouts=stereo,volume=$g2[a1];" +
                    "[a0][a1]concat=n=2:v=0:a=1[$coreOutLabel]"
            }
        }
        MergeStudioMode.CROSSFADE -> {
            val maxAllowedXf = if (probedP1DurSec > 0.1f && probedP2DurSec > 0.1f) {
                min(probedP1DurSec, probedP2DurSec) * 0.85f
            } else {
                15f
            }
            val safeXf = min(crossfadeSec.coerceIn(0.1f, 15f), maxAllowedXf).coerceAtLeast(0.05f)
            val xf = String.format(Locale.US, "%.2f", safeXf)
            "[0:a]aresample=44100,aformat=channel_layouts=stereo,volume=$g1$p1DelayFilter$echoTailFilter[a0];" +
                "[1:a]aresample=44100,aformat=channel_layouts=stereo,volume=$g2[a1];" +
                "[a0][a1]acrossfade=d=$xf:c1=tri:c2=tri[$coreOutLabel]"
        }
        MergeStudioMode.OVERLAY -> {
            if (autoDucking) {
                val duckDepth = clamp(duckingStrengthPct / 100f, 0f, 1f).let { if (it < 0.01f) 0.65f else it }
                val ratio = String.format(Locale.US, "%.1f", (2.0f + 8.0f * duckDepth).coerceIn(2f, 12f))
                "[0:a]aresample=44100,aformat=channel_layouts=stereo,volume=$g1$p1DelayFilter,asplit=2[v][sc];" +
                    "[1:a]aresample=44100,aformat=channel_layouts=stereo,volume=$g2$fadeInFilter$fadeOutFilter$p2DelayFilter[m];" +
                    "[m][sc]sidechaincompress=threshold=0.02:ratio=$ratio:attack=150:release=850:knee=4[ducked];" +
                    "[v][ducked]amix=inputs=2:duration=longest:normalize=0[$coreOutLabel]"
            } else {
                "[0:a]aresample=44100,aformat=channel_layouts=stereo,volume=$g1$p1DelayFilter[a0];" +
                    "[1:a]aresample=44100,aformat=channel_layouts=stereo,volume=$g2$fadeInFilter$fadeOutFilter$p2DelayFilter[a1];" +
                    "[a0][a1]amix=inputs=2:duration=longest:normalize=0[$coreOutLabel]"
            }
        }
    }

    val filterComplex = if (autoLeveling) {
        val levelFilter = if (levelingStrengthPct < 45f) {
            "dynaudnorm=f=150:g=15:m=4.0,alimiter=limit=0.95:attack=5:release=50"
        } else {
            "loudnorm=I=-16:TP=-1.5:LRA=11,alimiter=limit=0.95:attack=5:release=50"
        }
        "$coreGraph;[mixraw]$levelFilter[out]"
    } else if (needsLimiter) {
        "$coreGraph;[prelim]alimiter=limit=0.95:attack=5:release=50[out]"
    } else {
        coreGraph
    }

    val isWav = target.extension.equals("wav", ignoreCase = true)
    val codecArgs = if (isWav) "-acodec pcm_s16le -ar 44100 -ac 2" else "-ar 44100 -ac 2 -c:a aac -b:a 192k"
    val extraInput = if (whooshWav != null) " -i \"${whooshWav.absolutePath}\"" else ""
    val cmd = "-y -i \"${safe1.absolutePath}\" -i \"${safe2.absolutePath}\"$extraInput -filter_complex \"$filterComplex\" -map \"[out]\" -vn $codecArgs \"${target.absolutePath}\""
    val ok = FfmpegKitBridge.execute(cmd, toolName = "LargeFileFfmpegMix", logNonZeroFailure = true)
    if (safe1 != primaryFile) runCatching { safe1.delete() }
    if (safe2 != secondaryFile) runCatching { safe2.delete() }
    runCatching { whooshWav?.delete() }
    if (ok && target.exists() && target.length() > 512L) {
        target
    } else {
        runCatching { target.delete() }
        null
    }
}

fun buildFfmpegFilterForDspConfig(config: DspConfig): String {
    val norm = config.normalized()
    if (!norm.hasAnyActiveProcessing()) return ""
    val stages = mutableListOf<String>()
    var needsLimiter = false

    for (slot in norm.chain) {
        if (!slot.enabled) continue
        when (slot.type) {
            EffectType.NOISE_REDUCTION -> {
                val nr = norm.noiseReduction
                val hp = nr.highPassHz.coerceIn(20f, 300f).roundToInt()
                stages += "highpass=f=$hp"
                val nf = (-nr.reductionDb.coerceIn(6f, 40f)).roundToInt()
                stages += "afftdn=nf=$nf"
            }
            EffectType.EQUALIZER -> {
                val bands10 = norm.equalizer.resolved10Bands()
                val freqs = com.example.model.EQ_10_FREQUENCIES_HZ
                for (i in bands10.indices) {
                    val gain = bands10[i]
                    if (abs(gain) >= 0.25f) {
                        val gStr = String.format(Locale.US, "%.2f", clamp(gain, -12f, 12f))
                        val wStr = if (i == 0 || i == 9) "0.85" else "1.41"
                        stages += "equalizer=f=${freqs[i]}:t=q:w=$wStr:g=$gStr"
                        needsLimiter = true
                    }
                }
            }
            EffectType.BASS_BOOST -> {
                val b = norm.bassBoost
                if (abs(b.gainDb) >= 0.25f) {
                    val gStr = String.format(Locale.US, "%.2f", clamp(b.gainDb, -12f, 12f))
                    val fHz = b.frequencyHz.coerceIn(40f, 250f).roundToInt()
                    stages += "bass=g=$gStr:f=$fHz"
                    needsLimiter = true
                }
            }
            EffectType.TREBLE -> {
                val tr = norm.treble
                if (abs(tr.gainDb) >= 0.25f) {
                    val gStr = String.format(Locale.US, "%.2f", clamp(tr.gainDb, -12f, 12f))
                    val fHz = tr.frequencyHz.coerceIn(2000f, 14000f).roundToInt()
                    stages += "treble=g=$gStr:f=$fHz"
                    needsLimiter = true
                }
            }
            EffectType.PITCH_SHIFT -> {
                val semi = norm.pitchShift.semitones
                if (abs(semi) >= 0.1f) {
                    val factor = 2.0.pow(clamp(semi, -12f, 12f) / 12.0).coerceIn(0.5, 2.0)
                    val invTempo = (1.0 / factor).coerceIn(0.5, 2.0)
                    stages += String.format(Locale.US, "asetrate=44100*%.5f,aresample=44100,atempo=%.5f", factor, invTempo)
                    needsLimiter = true
                }
            }
            EffectType.TIME_STRETCH -> {
                val tr = norm.timeStretch.tempoRatio.coerceIn(0.5f, 2.5f)
                if (abs(tr - 1f) >= 0.02f) {
                    stages += buildAtempoChain(tr.toDouble())
                }
            }
            EffectType.COMPRESSOR -> {
                val c = norm.compressor
                if (c.ratio > 1.05f) {
                    val thresh = String.format(Locale.US, "%.1fdB", clamp(c.thresholdDb, -48f, -1f))
                    val ratio = String.format(Locale.US, "%.2f", clamp(c.ratio, 1.1f, 20f))
                    val attack = String.format(Locale.US, "%.1f", clamp(c.attackMs, 1f, 200f))
                    val release = String.format(Locale.US, "%.1f", clamp(c.releaseMs, 20f, 1000f))
                    val makeup = String.format(Locale.US, "%.2f", clamp(c.makeupGainDb, -6f, 15f))
                    stages += "acompressor=threshold=$thresh:ratio=$ratio:attack=$attack:release=$release:makeup=$makeup"
                    needsLimiter = true
                }
            }
            EffectType.ECHO_REVERB -> {
                val e = norm.echoReverb
                if (e.wetDryMix > 0.02f) {
                    val pre = clamp(e.preDelayMs, 0f, 180f).roundToInt().coerceAtLeast(8)
                    val d1 = clamp(e.delayTimeMs, 25f, 950f).roundToInt()
                    val d2 = (d1 * 1.48f).roundToInt().coerceIn(pre + 15, 1400)
                    val d3 = (d2 * 1.38f).roundToInt().coerceIn(d2 + 20, 1950)
                    val earlyDec = clamp(0.18f + e.earlyReflections * 0.36f * e.wetDryMix, 0.06f, 0.58f)
                    val tailDec1 = clamp((0.14f + e.reverbDecay * 0.34f + e.feedback * 0.14f) * e.wetDryMix, 0.05f, 0.52f)
                    val tailDec2 = clamp(tailDec1 * (0.55f + e.lateReflections * 0.25f), 0.03f, 0.42f)
                    val tailDec3 = clamp(tailDec2 * 0.62f, 0.02f, 0.32f)
                    val inGain = String.format(Locale.US, "%.2f", clamp(1f - e.wetDryMix * 0.28f, 0.55f, 0.92f))
                    val outGain = String.format(Locale.US, "%.2f", clamp(0.84f + e.wetDryMix * 0.08f, 0.78f, 0.92f))
                    stages += String.format(
                        Locale.US,
                        "aecho=%s:%s:%d|%d|%d|%d:%.2f|%.2f|%.2f|%.2f",
                        inGain,
                        outGain,
                        pre,
                        d1,
                        d2,
                        d3,
                        earlyDec,
                        tailDec1,
                        tailDec2,
                        tailDec3,
                    )
                    if (e.damping >= 0.45f) {
                        val lpfHz = (13000 - (e.damping * 6500).roundToInt()).coerceIn(3800, 13000)
                        stages += "lowpass=f=$lpfHz"
                    }
                    if (e.stereoWidth >= 0.55f) {
                        val m = String.format(Locale.US, "%.2f", 1.0f + (e.stereoWidth - 0.45f) * 0.9f)
                        stages += "extrastereo=m=$m"
                    }
                    needsLimiter = true
                }
            }
            EffectType.LIMITER -> {
                needsLimiter = true
            }
            EffectType.CHORUS -> {
                val c = norm.chorus
                if (c.mix > 0.02f) {
                    val speed = clamp(c.rateHz, 0.1f, 8f)
                    val depth = clamp(c.depthMs * 0.18f, 0.2f, 4.5f)
                    val outGain = clamp(0.75f + c.mix * 0.20f, 0.7f, 0.95f)
                    stages += String.format(
                        Locale.US,
                        "chorus=0.70:%.2f:40|55:0.40|0.32:%.2f|%.2f:%.2f|%.2f",
                        outGain,
                        speed,
                        (speed * 1.15f).coerceAtMost(8f),
                        depth,
                        (depth * 0.85f).coerceAtLeast(0.2f),
                    )
                    needsLimiter = true
                }
            }
            EffectType.FLANGER -> {
                val f = norm.flanger
                if (f.mix > 0.02f) {
                    val depth = clamp(f.depthMs, 0.5f, 10f)
                    val regen = clamp(f.feedback * 95f, -90f, 90f)
                    val width = clamp(f.mix * 100f, 10f, 100f)
                    val speed = clamp(f.rateHz, 0.1f, 10f)
                    stages += String.format(
                        Locale.US,
                        "flanger=delay=1.5:depth=%.2f:regen=%.1f:width=%.0f:speed=%.2f",
                        depth,
                        regen,
                        width,
                        speed,
                    )
                    needsLimiter = true
                }
            }
            EffectType.PHASER -> {
                val ph = norm.phaser
                if (ph.mix > 0.02f) {
                    val decay = clamp(0.15f + ph.feedback * 0.70f, 0.1f, 0.85f)
                    val speed = clamp(ph.rateHz, 0.1f, 2f)
                    val outGain = clamp(0.78f + ph.mix * 0.16f, 0.7f, 0.94f)
                    stages += String.format(
                        Locale.US,
                        "aphaser=in_gain=0.80:out_gain=%.2f:delay=3.0:decay=%.2f:speed=%.2f:type=t",
                        outGain,
                        decay,
                        speed,
                    )
                    needsLimiter = true
                }
            }
            EffectType.DISTORTION -> {
                val d = norm.distortion
                if (d.mix > 0.02f && d.drive > 1.05f) {
                    val driveDb = clamp((d.drive - 1f) * 1.4f * d.mix, 0.5f, 18f)
                    val ceil = when (d.mode) {
                        com.example.model.DistortionMode.HARD_CLIP -> 0.58f
                        com.example.model.DistortionMode.OVERDRIVE -> 0.68f
                        com.example.model.DistortionMode.SOFT_CLIP -> 0.76f
                    }
                    val toneHz = clamp(d.toneHz, 600f, 18000f).roundToInt()
                    stages += String.format(
                        Locale.US,
                        "volume=%.2fdB,alimiter=limit=%.2f:attack=1:release=15:level=disabled,lowpass=f=%d",
                        driveDb,
                        ceil,
                        toneHz,
                    )
                    needsLimiter = true
                }
            }
        }
    }

    if (norm.spatialPanHz > 0.01f) {
        stages += String.format(Locale.US, "apulsator=hz=%.3f", clamp(norm.spatialPanHz, 0.02f, 4f))
        needsLimiter = true
    }
    if (norm.hasLiveSurround()) {
        val (_, fallbackSurround) = buildSurroundStereoFfmpegFilter(
            midAdjustPct = norm.surroundMidPct,
            sideBoostPct = norm.surroundSidePct,
            widthPct = norm.surroundWidthPct,
        )
        stages += fallbackSurround.removeSuffix(",alimiter=limit=0.95")
        needsLimiter = true
    }
    if (abs(norm.masterGainDb) >= 0.15f) {
        stages += String.format(Locale.US, "volume=%.2fdB", clamp(norm.masterGainDb, -18f, 12f))
        needsLimiter = true
    }
    if (needsLimiter) {
        val ceilingLin = 10.0.pow(clamp(norm.limiter.ceilingDb, -6f, -0.3f) / 20.0).coerceIn(0.45, 0.96)
        stages += String.format(Locale.US, "alimiter=limit=%.3f:attack=5:release=60", ceilingLin)
    }
    return stages.joinToString(",")
}

suspend fun exportSourceFileDirectlyWithFfmpeg(
    context: Context,
    sourceFile: File,
    format: com.example.model.ExportAudioFormat,
    baseFileName: String,
    loudnessTarget: com.example.model.PlatformLoudnessTarget,
    dspConfig: DspConfig = DspConfig(),
    totalDurationMs: Long = 0L,
    onFfmpegProgress: ((Float) -> Unit)? = null,
): ExportedAudioResult? = withContext(Dispatchers.IO) {
    if (!sourceFile.exists() || !FfmpegKitBridge.isAvailable()) return@withContext null
    val safeIn = com.example.audio.cache.SmartCacheManager.ensureSafeFfmpegInputFile(context, sourceFile)
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val cleanBase = baseFileName.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_").ifEmpty { "Sonix_Export" }
    val stamp = UUID.randomUUID().toString().take(6)

    val dspFilter = buildFfmpegFilterForDspConfig(dspConfig)
    val lufs = loudnessTarget.targetLufs
    val fullFilter = listOfNotNull(
        dspFilter.takeIf { it.isNotBlank() },
        if (lufs != null) "loudnorm=I=$lufs:TP=-1.0:LRA=9,alimiter=limit=0.95" else null,
    ).joinToString(",")
    val afArg = if (fullFilter.isNotBlank()) "-af \"$fullFilter\"" else ""

    try {
        if (format == com.example.model.ExportAudioFormat.OGG) {
            val oggOut = File(exportDir, "${cleanBase}_$stamp.ogg")
            val oggCodecs = listOf(
                "-c:a libopus -b:a 192k -ar 48000 -ac 2",
                "-c:a libvorbis -b:a 192k -ar 44100 -ac 2",
            )
            for (codec in oggCodecs) {
                runCatching { oggOut.delete() }
                val oggCmd = "-y -i \"${safeIn.absolutePath}\" -vn $afArg $codec \"${oggOut.absolutePath}\""
                val ok = FfmpegKitBridge.execute(
                    oggCmd,
                    toolName = "LargeFileFfmpegExportOGG",
                    logNonZeroFailure = false,
                    totalDurationMs = totalDurationMs,
                    onProgress = onFfmpegProgress,
                )
                if (ok && oggOut.exists() && oggOut.length() > 512L) {
                    return@withContext ExportedAudioResult(oggOut, "ogg", "audio/ogg")
                }
            }
            runCatching { oggOut.delete() }
        }

        val (ext, mime, codecArgs) = when (format) {
            com.example.model.ExportAudioFormat.WAV -> Triple("wav", "audio/wav", "-acodec pcm_s16le -ar 44100 -ac 2")
            com.example.model.ExportAudioFormat.FLAC -> Triple("flac", "audio/flac", "-c:a flac -ar 44100 -ac 2")
            com.example.model.ExportAudioFormat.MP3 -> Triple("mp3", "audio/mpeg", "-c:a libmp3lame -b:a 320k -ar 44100 -ac 2")
            com.example.model.ExportAudioFormat.M4A,
            com.example.model.ExportAudioFormat.OGG -> Triple("m4a", "audio/mp4", "-c:a aac -b:a 320k -ar 44100 -ac 2")
        }
        val outFile = File(exportDir, "${cleanBase}_$stamp.$ext")
        val cmd = "-y -i \"${safeIn.absolutePath}\" -vn $afArg $codecArgs \"${outFile.absolutePath}\""
        val ok = FfmpegKitBridge.execute(
            cmd,
            toolName = "LargeFileFfmpegExport",
            logNonZeroFailure = true,
            totalDurationMs = totalDurationMs,
            onProgress = onFfmpegProgress,
        )
        if (ok && outFile.exists() && outFile.length() > 512L) {
            ExportedAudioResult(outFile, ext, mime)
        } else {
            runCatching { outFile.delete() }
            null
        }
    } finally {
        if (safeIn != sourceFile) runCatching { safeIn.delete() }
    }
}

private fun batchFilterToFfmpegStages(preset: BatchFilterPreset): String = when (preset) {
    BatchFilterPreset.NONE -> ""
    BatchFilterPreset.VOICE_CLARITY ->
        "highpass=f=85,equalizer=f=240:t=q:w=1.2:g=-2.0,equalizer=f=3200:t=q:w=1.3:g=3.5,acompressor=threshold=-20dB:ratio=3:attack=8:release=110"
    BatchFilterPreset.BASS_WARMTH ->
        "bass=g=5.5:f=95,equalizer=f=220:t=q:w=1.1:g=2.5,acompressor=threshold=-19dB:ratio=2.8:attack=12:release=140"
    BatchFilterPreset.STUDIO_RADIO ->
        "highpass=f=75,acompressor=threshold=-18dB:ratio=4:attack=5:release=60,bass=g=4.5,treble=g=3.0,alimiter=limit=0.95"
    BatchFilterPreset.ECHO_SPACE ->
        "aecho=0.8:0.85:55:0.30,treble=g=2.0,alimiter=limit=0.95"
    BatchFilterPreset.BRIGHT_AIR ->
        "highpass=f=70,equalizer=f=5500:t=q:w=1.1:g=4.0,treble=g=4.0,alimiter=limit=0.95"
    BatchFilterPreset.ACTIVE_STUDIO_DSP -> ""
}

private fun moveExportToPersistentBatchDir(
    context: Context,
    exported: ExportedAudioResult,
    cleanBase: String,
): ExportedAudioResult {
    val batchDir = File(context.filesDir, "batch_exports").apply { mkdirs() }
    val stamp = UUID.randomUUID().toString().take(6)
    val dest = File(batchDir, "${cleanBase}_$stamp.${exported.extension}")
    val moved = exported.file.renameTo(dest) && dest.exists() && dest.length() > 0L
    if (!moved) {
        exported.file.inputStream().buffered(65_536).use { inp ->
            dest.outputStream().buffered(65_536).use { out ->
                inp.copyTo(out, 65_536)
            }
        }
        runCatching { exported.file.delete() }
    }
    return ExportedAudioResult(dest, exported.extension, exported.mimeType)
}

/**
 * Applies the selected batch DSP settings (pitch, speed, filter preset / active studio DSP,
 * loudness normalization, and output format) to one audio source file and writes the result
 * to persistent `filesDir/batch_exports`.
 */
suspend fun processBatchItemWithDsp(
    context: Context,
    sourceFile: File,
    displayBaseName: String,
    settings: BatchDspSettings,
    activeDspConfig: DspConfig,
    onProgress: (Float) -> Unit = {},
): ExportedAudioResult? = withContext(Dispatchers.IO) {
    if (!sourceFile.exists() || sourceFile.length() <= 0L) return@withContext null
    val cleanBase = displayBaseName.trim()
        .substringBeforeLast('.')
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .ifEmpty { "Sonix_Batch" }
    onProgress(0.08f)

    val hasPitchOrSpeed = abs(settings.pitchSemitones) >= 0.05f || abs(settings.speedRatio - 1.0f) >= 0.02f
    val useFfmpegFastPath = settings.filterPreset != BatchFilterPreset.ACTIVE_STUDIO_DSP && FfmpegKitBridge.isAvailable()

    if (useFfmpegFastPath) {
        val filterStages = batchFilterToFfmpegStages(settings.filterPreset)
        val candidates = if (hasPitchOrSpeed) {
            val (rbChain, soxrChain) = buildPitchAndSpeedFfmpegChains(
                semitones = settings.pitchSemitones,
                tempoRatio = settings.speedRatio,
            )
            listOf(rbChain, soxrChain).map { ps ->
                if (filterStages.isNotBlank()) "$ps,$filterStages" else ps
            }
        } else {
            listOf(filterStages)
        }.map { chain ->
            if (settings.normalizeOutput) {
                if (chain.isNotBlank()) "$chain,loudnorm=I=-15:TP=-1.0:LRA=9" else "loudnorm=I=-15:TP=-1.0:LRA=9"
            } else {
                chain
            }
        }

        for ((idx, graph) in candidates.withIndex()) {
            onProgress(if (idx == 0) 0.25f else 0.45f)
            val tempProcessed = if (graph.isNotBlank()) {
                processAudioFileWithFfmpeg(
                    context = context,
                    inputFile = sourceFile,
                    audioFilter = graph,
                    toolName = "BatchQueueFfmpeg",
                )
            } else {
                sourceFile
            }
            if (tempProcessed != null && tempProcessed.exists() && tempProcessed.length() > 512L) {
                onProgress(0.76f)
                val exported = exportSourceFileDirectlyWithFfmpeg(
                    context = context,
                    sourceFile = tempProcessed,
                    format = settings.outputFormat,
                    baseFileName = cleanBase,
                    loudnessTarget = PlatformLoudnessTarget.ORIGINAL,
                )
                if (tempProcessed != sourceFile) runCatching { tempProcessed.delete() }
                if (exported != null) {
                    val persisted = moveExportToPersistentBatchDir(context, exported, cleanBase)
                    onProgress(1f)
                    return@withContext persisted
                }
            } else if (tempProcessed != null && tempProcessed != sourceFile) {
                runCatching { tempProcessed.delete() }
            }
        }
    }

    // Pure-Kotlin offline path (used for Active Studio DSP chain or when FFmpeg is unavailable)
    if (AudioDecoder.shouldStreamOnDisk(sourceFile)) {
        return@withContext null
    }
    val decoded = AudioDecoder.decodeFileWithProgress(sourceFile) { dec ->
        onProgress((0.10f + dec * 0.25f).coerceIn(0.10f, 0.35f))
    } ?: return@withContext null

    if (decoded.frameCount <= 0) return@withContext null

    onProgress(0.40f)
    val filtered = when (settings.filterPreset) {
        BatchFilterPreset.NONE -> decoded
        BatchFilterPreset.ACTIVE_STUDIO_DSP -> withContext(Dispatchers.Default) {
            renderOfflineDsp(decoded, activeDspConfig)
        }
        BatchFilterPreset.VOICE_CLARITY -> purifyBufferOffline(
            source = decoded,
            params = NoiseReductionParams(
                thresholdDb = -46f,
                reductionDb = 12f,
                highPassHz = 85f,
                spectralStrength = 0.4f,
                clarityBoostDb = 2.5f,
                spectralCleanerEnabled = true,
            ),
        )
        BatchFilterPreset.BASS_WARMTH -> withContext(Dispatchers.Default) {
            renderOfflineDsp(
                decoded,
                DspConfig()
                    .withSlotEnabled(EffectType.BASS_BOOST, true)
                    .withSlotEnabled(EffectType.COMPRESSOR, true)
                    .copy(bassBoost = BassBoostParams(gainDb = 6f, frequencyHz = 95f)),
            )
        }
        BatchFilterPreset.STUDIO_RADIO -> applyOneTapSocialPreset(
            context = context,
            buffer = decoded,
            preset = OneTapSocialPreset.FM_RADIO,
        )
        BatchFilterPreset.ECHO_SPACE -> applyOneTapSocialPreset(
            context = context,
            buffer = decoded,
            preset = OneTapSocialPreset.GRAND_HALL_REVERB,
        )
        BatchFilterPreset.BRIGHT_AIR -> withContext(Dispatchers.Default) {
            renderOfflineDsp(
                decoded,
                DspConfig()
                    .withSlotEnabled(EffectType.TREBLE, true)
                    .copy(treble = TrebleParams(gainDb = 4.5f, frequencyHz = 6500f)),
            )
        }
    }

    onProgress(0.58f)
    val pitched = if (hasPitchOrSpeed) {
        applyPitchAndSpeedOffline(
            buffer = filtered,
            semitones = settings.pitchSemitones,
            tempoRatio = settings.speedRatio,
            context = context,
        ) { p ->
            onProgress((0.58f + p * 0.22f).coerceIn(0.58f, 0.80f))
        }
    } else {
        filtered
    }

    val finalBuf = if (settings.normalizeOutput) {
        normalizeAudio(pitched, -1.0f)
    } else {
        pitched
    }

    val exported = exportRenderedAudioToFile(
        context = context,
        buffer = finalBuf,
        format = settings.outputFormat,
        baseFileName = cleanBase,
    ) { ep ->
        onProgress((0.82f + ep * 0.18f).coerceIn(0.82f, 1.0f))
    }
    val persisted = moveExportToPersistentBatchDir(context, exported, cleanBase)
    onProgress(1f)
    persisted
}


