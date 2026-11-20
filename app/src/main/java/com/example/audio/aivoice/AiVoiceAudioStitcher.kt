package com.example.audio.aivoice

import android.content.Context
import android.net.Uri
import com.example.audio.AudioFileUploader
import com.example.audio.AudioUploadOutcome
import com.example.audio.AudioDecodeOutcome
import com.example.audio.AudioDecoder
import com.example.audio.TARGET_SAMPLE_RATE
import com.example.audio.buildWavHeaderBytes
import com.example.audio.clamp
import com.example.audio.resampleStereo
import com.example.audio.writeStereoWavFile
import com.example.diagnostics.FfmpegKitBridge
import com.example.model.StereoAudioBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class PreparedReferenceAudio(
    val wavFile: File,
    val durationMs: Long,
    val sampleRate: Int,
)

data class WavStitchSegment(
    val wavFile: File,
    val pauseAfterMs: Int = 350,
)

data class StitchedWavResult(
    val outputFile: File,
    val sampleRate: Int,
    val durationMs: Long,
)

object AiVoiceAudioStitcher {

    /**
     * Decodes a user-selected reference audio URI (WAV/MP3/M4A/OGG/FLAC) into a clean
     * 16-bit WAV file ready for zero-shot cloning (trimmed safely to 20s max to keep payload fast).
     */
    suspend fun prepareReferenceWavFromUri(
        context: Context,
        uri: Uri,
        outWavFile: File,
        maxSeconds: Int = 20,
        onProgress: (Float?, String) -> Unit = { _, _ -> },
    ): PreparedReferenceAudio? = withContext(Dispatchers.IO) {
        val upload = AudioFileUploader.copyAudioUriToCacheDetailed(context, uri) { info ->
            onProgress(info.fraction * 0.35f, "Copying reference audio (${info.percent}%)")
        }
        val sourceFile = when (upload) {
            is AudioUploadOutcome.Success -> upload.uploaded.file
            is AudioUploadOutcome.Failure -> throw IllegalStateException(upload.errorMessage)
        }
        try {
            outWavFile.parentFile?.mkdirs()
            onProgress(null, "Converting reference to mono WAV...")
            val seconds = maxSeconds.coerceIn(1, 60)
            val command = "-y -i ${quoteFfmpegPath(sourceFile.absolutePath)} -vn -t $seconds -ac 1 -ar 24000 -c:a pcm_s16le ${quoteFfmpegPath(outWavFile.absolutePath)}"
            val converted = FfmpegKitBridge.execute(
                command = command,
                toolName = "AiVoiceReferenceConversion",
            )
            if (!converted || !outWavFile.isFile || outWavFile.length() <= 44L) {
                throw IllegalStateException("FFmpegKit could not convert the selected reference audio to WAV")
            }
            onProgress(0.9f, "Validating converted reference audio...")
            val decoded = AudioDecoder.decodeFile(outWavFile)
                ?: throw IllegalStateException("The converted reference audio could not be decoded")
            if (decoded.frameCount <= 0) {
                throw IllegalStateException("The converted reference audio is empty")
            }
            onProgress(1f, "Reference audio is ready")
            PreparedReferenceAudio(
                wavFile = outWavFile,
                durationMs = decoded.durationMs.coerceAtMost(seconds * 1000L),
                sampleRate = 24000,
            )
        } finally {
            runCatching { sourceFile.delete() }
        }
    }

    private fun quoteFfmpegPath(path: String): String =
        "'${path.replace("'", "'\\''")}'"

    /**
     * Prepares an existing StereoAudioBuffer (e.g., from current studio recording) as a cloning reference WAV.
     */
    suspend fun prepareReferenceWavFromBuffer(
        buffer: StereoAudioBuffer,
        outWavFile: File,
        maxSeconds: Int = 20,
        onProgress: (Float?, String) -> Unit = { _, _ -> },
    ): PreparedReferenceAudio? = withContext(Dispatchers.IO) {
        if (buffer.frameCount <= 0) return@withContext null
        onProgress(0.1f, "Preparing studio audio...")
        val targetRate = 24000
        val resampled = resampleStereo(buffer, targetRate)
        val maxFrames = (targetRate * maxSeconds).coerceAtMost(resampled.frameCount)
        val clipped = if (resampled.frameCount > maxFrames) {
            StereoAudioBuffer(
                left = resampled.left.copyOf(maxFrames),
                right = resampled.right.copyOf(maxFrames),
                sampleRate = targetRate,
            )
        } else {
            resampled
        }
        onProgress(0.8f, "Writing WAV reference...")
        writeStereoWavFile(outWavFile, clipped)
        onProgress(1f, "Reference audio is ready")
        PreparedReferenceAudio(
            wavFile = outWavFile,
            durationMs = clipped.durationMs,
            sampleRate = targetRate,
        )
    }

    /**
     * Stitches multiple PCM WAV files cleanly into a single studio stereo WAV file at [targetSampleRate],
     * inserting precise [WavStitchSegment.pauseAfterMs] silence between segments.
     * Pure JVM-safe WAV parsing + linear resampling so it runs fast on device and passes JVM tests.
     */
    fun stitchWavSegments(
        segments: List<WavStitchSegment>,
        outFile: File,
        targetSampleRate: Int = TARGET_SAMPLE_RATE,
        normalizeLoudness: Boolean = true,
    ): StitchedWavResult {
        require(segments.isNotEmpty()) { "At least one audio segment is required to stitch." }
        outFile.parentFile?.mkdirs()

        val decodedBuffers = mutableListOf<Triple<StereoAudioBuffer, Int, Float>>()
        var totalFramesLong = 0L

        for ((index, seg) in segments.withIndex()) {
            val buf = readSimplePcmWav(seg.wavFile)
                ?: throw IllegalStateException("Invalid WAV segment: ${seg.wavFile.name}")
            val resampled = if (buf.sampleRate != targetSampleRate) {
                resampleStereo(buf, targetSampleRate)
            } else {
                buf
            }
            val pauseMs = if (index < segments.lastIndex) seg.pauseAfterMs.coerceIn(0, 5000) else 0
            val pauseFrames = ((pauseMs.toLong() * targetSampleRate) / 1000L).toInt()
            val gain = if (normalizeLoudness) calculateLoudnessGain(resampled) else 1f
            decodedBuffers.add(Triple(resampled, pauseFrames, gain))
            totalFramesLong += resampled.frameCount + pauseFrames
        }

        val totalFrames = totalFramesLong.coerceAtMost(Int.MAX_VALUE / 4L).toInt()
        val dataSizeBytes = totalFrames * 4
        val header = buildWavHeaderBytes(dataSizeBytes, targetSampleRate, numChannels = 2, bitsPerSample = 16)
        val chunk = ByteArray(8192)
        val bb = ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN)
        val fadeFrames = min(targetSampleRate / 120, 360) // subtle ~8ms edge anti-click

        BufferedOutputStream(FileOutputStream(outFile), 8192).use { out ->
            out.write(header)
            var bytesInChunk = 0

            fun writeFrame(l: Float, r: Float) {
                if (bytesInChunk + 4 > chunk.size) {
                    out.write(chunk, 0, bytesInChunk)
                    bb.clear()
                    bytesInChunk = 0
                }
                val sl = (clamp(l, -1f, 1f) * 32767f).roundToInt().coerceIn(-32768, 32767).toShort()
                val sr = (clamp(r, -1f, 1f) * 32767f).roundToInt().coerceIn(-32768, 32767).toShort()
                bb.putShort(sl)
                bb.putShort(sr)
                bytesInChunk += 4
            }

            for ((buf, pauseFrames, gain) in decodedBuffers) {
                val n = buf.frameCount
                for (i in 0 until n) {
                    var env = 1f
                    if (n > fadeFrames * 2) {
                        if (i < fadeFrames) env = i.toFloat() / fadeFrames.toFloat()
                        else if (i >= n - fadeFrames) env = (n - 1 - i).toFloat() / fadeFrames.toFloat()
                    }
                    writeFrame(buf.left[i] * env * gain, buf.right[i] * env * gain)
                }
                for (p in 0 until pauseFrames) {
                    writeFrame(0f, 0f)
                }
            }
            if (bytesInChunk > 0) {
                out.write(chunk, 0, bytesInChunk)
            }
            out.flush()
        }

        val durationMs = if (targetSampleRate > 0) (totalFrames * 1000L) / targetSampleRate else 0L
        return StitchedWavResult(
            outputFile = outFile,
            sampleRate = targetSampleRate,
            durationMs = durationMs,
        )
    }

    private fun calculateLoudnessGain(buffer: StereoAudioBuffer): Float {
        val silenceThreshold = 0.01f
        var energy = 0.0
        var activeSamples = 0L
        var peak = 0f
        for (index in 0 until buffer.frameCount) {
            val left = abs(buffer.left[index])
            val right = abs(buffer.right[index])
            peak = max(peak, max(left, right))
            if (left > silenceThreshold) {
                energy += left * left
                activeSamples++
            }
            if (right > silenceThreshold) {
                energy += right * right
                activeSamples++
            }
        }
        if (activeSamples == 0L || peak <= 0f) return 1f

        val rms = sqrt(energy / activeSamples).toFloat()
        val targetRms = 0.16f
        val desiredGain = targetRms / rms
        val peakLimitedGain = 0.94f / peak
        return min(desiredGain, min(3f, peakLimitedGain)).coerceAtLeast(0f)
    }

    /**
     * Reads standard 16-bit PCM WAV (mono or stereo, parsing arbitrary RIFF subchunks cleanly).
     */
    fun readSimplePcmWav(file: File): StereoAudioBuffer? {
        if (!file.exists() || file.length() < 44L) return null
        val bytes = FileInputStream(file).use { BufferedInputStream(it).readBytes() }
        if (bytes.size < 44) return null
        val riff = String(bytes, 0, 4, Charsets.US_ASCII)
        val wave = String(bytes, 8, 4, Charsets.US_ASCII)
        if (riff != "RIFF" || wave != "WAVE") return null

        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var pos = 12
        var channels = 1
        var sampleRate = TARGET_SAMPLE_RATE
        var bitsPerSample = 16
        var dataOffset = -1
        var dataLength = 0

        while (pos + 8 <= bytes.size) {
            val chunkId = String(bytes, pos, 4, Charsets.US_ASCII)
            val chunkSize = bb.getInt(pos + 4).coerceAtLeast(0)
            val payloadStart = pos + 8
            if (chunkId == "fmt " && payloadStart + 16 <= bytes.size) {
                val audioFormat = bb.getShort(payloadStart).toInt() and 0xFFFF
                if (audioFormat != 1 && audioFormat != 3) return null
                channels = (bb.getShort(payloadStart + 2).toInt() and 0xFFFF).coerceAtLeast(1)
                sampleRate = bb.getInt(payloadStart + 4).coerceAtLeast(8000)
                bitsPerSample = (bb.getShort(payloadStart + 14).toInt() and 0xFFFF).coerceAtLeast(8)
            } else if (chunkId == "data") {
                dataOffset = payloadStart
                dataLength = min(chunkSize, max(0, bytes.size - payloadStart))
                break
            }
            pos = payloadStart + chunkSize + (chunkSize and 1)
        }

        if (dataOffset < 0 || dataLength <= 0 || bitsPerSample != 16) return null
        val bytesPerFrame = channels * 2
        val frameCount = dataLength / bytesPerFrame
        if (frameCount <= 0) return null

        val left = FloatArray(frameCount)
        val right = FloatArray(frameCount)
        var peak = 0f
        for (i in 0 until frameCount) {
            val base = dataOffset + i * bytesPerFrame
            val lSample = bb.getShort(base).toFloat() / 32768f
            val rSample = if (channels >= 2) {
                bb.getShort(base + 2).toFloat() / 32768f
            } else {
                lSample
            }
            left[i] = lSample
            right[i] = rSample
            peak = max(peak, max(abs(lSample), abs(rSample)))
        }
        if (peak > 0.99f) {
            val gain = 0.94f / peak
            for (i in 0 until frameCount) {
                left[i] *= gain
                right[i] *= gain
            }
        }
        return StereoAudioBuffer(left = left, right = right, sampleRate = sampleRate)
    }
}
