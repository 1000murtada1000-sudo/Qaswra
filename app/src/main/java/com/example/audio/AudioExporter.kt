package com.example.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import com.example.audio.cache.SmartCacheManager
import com.example.diagnostics.FfmpegKitBridge
import com.example.model.ExportAudioFormat
import com.example.model.PlatformLoudnessTarget
import com.example.model.StereoAudioBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

data class ExportedAudioResult(
    val file: File,
    val extension: String,
    val mimeType: String,
)

private fun computeFlacCrc8(data: ByteArray, len: Int): Int {
    var crc = 0
    for (i in 0 until len) {
        crc = crc xor (data[i].toInt() and 0xff)
        for (b in 0 until 8) {
            crc = if ((crc and 0x80) != 0) ((crc shl 1) xor 0x07) and 0xff else (crc shl 1) and 0xff
        }
    }
    return crc and 0xff
}

private fun computeFlacCrc16(data: ByteArray): Int {
    var crc = 0
    for (byte in data) {
        crc = crc xor ((byte.toInt() and 0xff) shl 8)
        for (b in 0 until 8) {
            crc = if ((crc and 0x8000) != 0) ((crc shl 1) xor 0x8005) and 0xffff else (crc shl 1) and 0xffff
        }
    }
    return crc and 0xffff
}

private fun appendUtf8Number(out: MutableList<Int>, value: Int) {
    if (value < 0x80) {
        out.add(value)
        return
    }
    val bytesNeeded = when {
        value < 0x800 -> 2
        value < 0x10000 -> 3
        value < 0x200000 -> 4
        value < 0x4000000 -> 5
        else -> 6
    }
    val leadMask = (0xff shl (8 - bytesNeeded)) and 0xff
    val firstShift = (bytesNeeded - 1) * 6
    out.add(leadMask or floor(value / 2.0.pow(firstShift)).toInt())
    for (i in (bytesNeeded - 2) downTo 0) {
        val shift = i * 6
        out.add(0x80 or (floor(value / 2.0.pow(shift)).toInt() and 0x3f))
    }
}

private fun buildFlacFrameHeader(
    frameNumber: Int,
    blockSize: Int,
    sampleRate: Int,
): ByteArray {
    val h = mutableListOf(0xff, 0xf8)
    val bsCode = when (blockSize) {
        4096 -> 0x0c
        2048 -> 0x0b
        1024 -> 0x0a
        else -> 0x07
    }
    val srCode = when (sampleRate) {
        44100 -> 0x09
        48000 -> 0x0a
        32000 -> 0x08
        24000 -> 0x06
        22050 -> 0x05
        16000 -> 0x04
        8000 -> 0x03
        else -> 0x00
    }
    h.add((bsCode shl 4) or srCode)
    h.add(0x18) // 2 ch independent | 16-bit
    appendUtf8Number(h, frameNumber)
    if (bsCode == 0x07) {
        val bsMinusOne = clamp(blockSize - 1, 0, 65535)
        h.add((bsMinusOne shr 8) and 0xff)
        h.add(bsMinusOne and 0xff)
    }
    val raw = ByteArray(h.size + 1)
    for (i in h.indices) raw[i] = h[i].toByte()
    raw[h.size] = computeFlacCrc8(raw, h.size).toByte()
    return raw
}

private fun encodeFlacToFile(
    buffer: StereoAudioBuffer,
    outputFile: File,
    onProgress: ((Float) -> Unit)?,
) {
    val totalSamples = buffer.frameCount
    val sampleRate = clamp(buffer.sampleRate, 1, 655350)
    val blockSize = 4096
    outputFile.parentFile?.mkdirs()

    BufferedOutputStream(FileOutputStream(outputFile), 8192).use { stream ->
        val intro = ByteArray(42)
        intro[0] = 0x66
        intro[1] = 0x4c
        intro[2] = 0x61
        intro[3] = 0x43
        intro[4] = 0x80.toByte()
        intro[5] = 0x00
        intro[6] = 0x00
        intro[7] = 34

        val minBlock = if (totalSamples in 16 until blockSize) totalSamples else blockSize
        intro[8] = ((minBlock shr 8) and 0xff).toByte()
        intro[9] = (minBlock and 0xff).toByte()
        intro[10] = ((blockSize shr 8) and 0xff).toByte()
        intro[11] = (blockSize and 0xff).toByte()

        val packed = ((sampleRate.toLong() and 0xfffffL) shl 44) or
            (1L shl 41) or
            (15L shl 36) or
            (totalSamples.toLong() and 0xffffffffL)
        for (i in 0 until 8) {
            intro[18 + i] = ((packed ushr ((7 - i) * 8)) and 0xffL).toByte()
        }
        stream.write(intro)

        var offset = 0
        var frameNumber = 0
        while (offset < totalSamples) {
            val count = min(blockSize, totalSamples - offset)
            val header = buildFlacFrameHeader(frameNumber, count, sampleRate)
            val frameData = ByteArray(header.size + 2 + count * 4)
            System.arraycopy(header, 0, frameData, 0, header.size)
            var p = header.size

            frameData[p++] = 0x02
            for (i in 0 until count) {
                val s = (clamp(buffer.left[offset + i], -1f, 1f) * 32767f).roundToInt()
                frameData[p++] = ((s shr 8) and 0xff).toByte()
                frameData[p++] = (s and 0xff).toByte()
            }

            frameData[p++] = 0x02
            for (i in 0 until count) {
                val s = (clamp(buffer.right[offset + i], -1f, 1f) * 32767f).roundToInt()
                frameData[p++] = ((s shr 8) and 0xff).toByte()
                frameData[p++] = (s and 0xff).toByte()
            }

            val crc16 = computeFlacCrc16(frameData)
            stream.write(frameData)
            stream.write((crc16 shr 8) and 0xff)
            stream.write(crc16 and 0xff)

            offset += count
            frameNumber++
            onProgress?.invoke(min(0.99f, offset.toFloat() / maxOf(1, totalSamples)))
        }
        stream.flush()
    }
    onProgress?.invoke(1f)
}

private fun encodeM4aFile(
    buffer: StereoAudioBuffer,
    outputFile: File,
    bitrateKbps: Int = 320,
    onProgress: ((Float) -> Unit)?,
): Boolean {
    val mime = MediaFormat.MIMETYPE_AUDIO_AAC
    val sampleRate = buffer.sampleRate
    val numFrames = buffer.frameCount
    if (numFrames <= 0) return false

    var codec: MediaCodec? = null
    var muxer: MediaMuxer? = null
    var muxerStarted = false

    return try {
        val safeBitrate = bitrateKbps.coerceIn(32, 320) * 1000
        val format = MediaFormat.createAudioFormat(mime, sampleRate, 2).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, safeBitrate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }

        codec = MediaCodec.createEncoderByType(mime)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        val bufferInfo = MediaCodec.BufferInfo()

        var frameOffset = 0
        var inputDone = false
        var outputDone = false
        val timeoutUs = 10_000L

        while (!outputDone) {
            if (!inputDone) {
                val inIdx = codec.dequeueInputBuffer(timeoutUs)
                if (inIdx >= 0) {
                    val inBuf = codec.getInputBuffer(inIdx)
                    if (frameOffset >= numFrames || inBuf == null) {
                        codec.queueInputBuffer(
                            inIdx,
                            0,
                            0,
                            (frameOffset * 1_000_000L) / sampleRate,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                        )
                        inputDone = true
                    } else {
                        inBuf.clear()
                        val maxFramesInBuf = inBuf.remaining() / 4
                        val framesToWrite = minOf(1024, maxFramesInBuf, numFrames - frameOffset)
                        val tmpBytes = ByteArray(framesToWrite * 4)
                        val bb = ByteBuffer.wrap(tmpBytes).order(ByteOrder.LITTLE_ENDIAN)
                        for (i in 0 until framesToWrite) {
                            val l = (clamp(buffer.left[frameOffset + i], -1f, 1f) * 32767f).roundToInt().toShort()
                            val r = (clamp(buffer.right[frameOffset + i], -1f, 1f) * 32767f).roundToInt().toShort()
                            bb.putShort(l)
                            bb.putShort(r)
                        }
                        inBuf.put(tmpBytes)
                        val ptsUs = (frameOffset * 1_000_000L) / sampleRate
                        codec.queueInputBuffer(inIdx, 0, tmpBytes.size, ptsUs, 0)
                        frameOffset += framesToWrite
                        onProgress?.invoke(min(0.95f, frameOffset.toFloat() / numFrames))
                    }
                }
            }

            val outIdx = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
            when {
                outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (!muxerStarted) {
                        trackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                }
                outIdx >= 0 -> {
                    val encodedData = codec.getOutputBuffer(outIdx)
                    if (encodedData != null && bufferInfo.size > 0 &&
                        (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 &&
                        muxerStarted
                    ) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                }
            }
        }
        onProgress?.invoke(1f)
        true
    } catch (_: Throwable) {
        false
    } finally {
        runCatching { codec?.stop() }
        runCatching { codec?.release() }
        if (muxerStarted) runCatching { muxer?.stop() }
        runCatching { muxer?.release() }
    }
}

suspend fun exportRenderedAudioToFile(
    context: Context,
    buffer: StereoAudioBuffer,
    format: ExportAudioFormat,
    baseFileName: String,
    bitrateKbps: Int = 320,
    loudnessTarget: PlatformLoudnessTarget = PlatformLoudnessTarget.ORIGINAL,
    onProgress: ((Float) -> Unit)? = null,
    onFfmpegProgress: ((Float) -> Unit)? = null,
): ExportedAudioResult = withContext(Dispatchers.IO) {
    val cleanBase = baseFileName.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_").ifEmpty { "Sonix_Export" }
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val safeStamp = UUID.randomUUID().toString().take(6)

    // Standardize 44100 Hz stereo and apply platform LUFS target if requested
    var prepared = normalizeToStudioStereo44100(buffer)
    val targetLufs = loudnessTarget.targetLufs
    if (targetLufs != null && prepared.frameCount > 0) {
        prepared = applyPlatformLoudnessNormalization(
            context = context,
            buffer = prepared,
            targetLufs = targetLufs,
            whatsappSpeechShape = loudnessTarget == PlatformLoudnessTarget.WHATSAPP_VOICE,
        )
    }

    val result = when (format) {
        ExportAudioFormat.WAV -> {
            val out = File(exportDir, "${cleanBase}_$safeStamp.wav")
            writeStereoWavFile(out, prepared)
            onProgress?.invoke(1f)
            ExportedAudioResult(out, "wav", "audio/wav")
        }
        ExportAudioFormat.FLAC -> {
            val out = File(exportDir, "${cleanBase}_$safeStamp.flac")
            encodeFlacToFile(prepared, out, onProgress)
            ExportedAudioResult(out, "flac", "audio/flac")
        }
        ExportAudioFormat.MP3 -> {
            val tempWav = File(exportDir, "mp3_src_$safeStamp.wav")
            val mp3File = File(exportDir, "${cleanBase}_$safeStamp.mp3")
            val safeBitrate = bitrateKbps.coerceIn(128, 320)
            val ffmpegMp3Ok = try {
                writeStereoWavFile(tempWav, prepared)
                val cmd = "-y -i \"${tempWav.absolutePath}\" -vn -ar 44100 -ac 2 -c:a libmp3lame -b:a ${safeBitrate}k \"${mp3File.absolutePath}\""
                val totalDurationMs = if (prepared.sampleRate > 0) {
                    prepared.frameCount.toLong() * 1000L / prepared.sampleRate.toLong()
                } else 0L
                val ok = FfmpegKitBridge.execute(
                    cmd,
                    toolName = "ExportMP3",
                    logNonZeroFailure = false,
                    totalDurationMs = totalDurationMs,
                    onProgress = onFfmpegProgress,
                )
                ok && mp3File.exists() && mp3File.length() > 512L
            } catch (_: Throwable) {
                false
            } finally {
                runCatching { tempWav.delete() }
            }

            if (ffmpegMp3Ok) {
                onProgress?.invoke(1f)
                ExportedAudioResult(mp3File, "mp3", "audio/mpeg")
            } else {
                // High-compat AAC M4A fallback when device has no bundled libmp3lame
                val m4aFile = File(exportDir, "${cleanBase}_$safeStamp.m4a")
                if (encodeM4aFile(prepared, m4aFile, safeBitrate, onProgress)) {
                    ExportedAudioResult(m4aFile, "m4a", "audio/mp4")
                } else {
                    val wavFallback = File(exportDir, "${cleanBase}_$safeStamp.wav")
                    writeStereoWavFile(wavFallback, prepared)
                    onProgress?.invoke(1f)
                    ExportedAudioResult(wavFallback, "wav", "audio/wav")
                }
            }
        }
        ExportAudioFormat.M4A -> {
            val m4aFile = File(exportDir, "${cleanBase}_$safeStamp.m4a")
            if (encodeM4aFile(prepared, m4aFile, bitrateKbps, onProgress)) {
                ExportedAudioResult(m4aFile, "m4a", "audio/mp4")
            } else {
                val wavFallback = File(exportDir, "${cleanBase}_$safeStamp.wav")
                writeStereoWavFile(wavFallback, prepared)
                onProgress?.invoke(1f)
                ExportedAudioResult(wavFallback, "wav", "audio/wav")
            }
        }
        ExportAudioFormat.OGG -> {
            val tempWav = File(exportDir, "ogg_src_$safeStamp.wav")
            val oggFile = File(exportDir, "${cleanBase}_$safeStamp.ogg")
            val oggOk = try {
                if (FfmpegKitBridge.isAvailable()) {
                    writeStereoWavFile(tempWav, prepared)
                    val cmds = buildOggFfmpegCommands(tempWav.absolutePath, oggFile.absolutePath, bitrateKbps)
                    var encoded = false
                    for (cmd in cmds) {
                        runCatching { oggFile.delete() }
                        val totalDurationMs = if (prepared.sampleRate > 0) {
                            prepared.frameCount.toLong() * 1000L / prepared.sampleRate.toLong()
                        } else 0L
                        val ok = FfmpegKitBridge.execute(
                            cmd,
                            toolName = "ExportOGG",
                            logNonZeroFailure = false,
                            totalDurationMs = totalDurationMs,
                            onProgress = onFfmpegProgress,
                        )
                        if (ok && oggFile.exists() && oggFile.length() > 512L) {
                            encoded = true
                            break
                        }
                    }
                    encoded
                } else {
                    false
                }
            } catch (_: Throwable) {
                false
            } finally {
                runCatching { tempWav.delete() }
            }

            if (oggOk) {
                onProgress?.invoke(1f)
                ExportedAudioResult(oggFile, "ogg", "audio/ogg")
            } else {
                runCatching { oggFile.delete() }
                val m4aFile = File(exportDir, "${cleanBase}_$safeStamp.m4a")
                if (encodeM4aFile(prepared, m4aFile, bitrateKbps, onProgress)) {
                    ExportedAudioResult(m4aFile, "m4a", "audio/mp4")
                } else {
                    val wavFallback = File(exportDir, "${cleanBase}_$safeStamp.wav")
                    writeStereoWavFile(wavFallback, prepared)
                    onProgress?.invoke(1f)
                    ExportedAudioResult(wavFallback, "wav", "audio/wav")
                }
            }
        }
    }
    SmartCacheManager.pruneManagedBuckets(context)
    result
}

internal fun buildOggFfmpegCommands(
    inputWavPath: String,
    outputOggPath: String,
    bitrateKbps: Int,
): List<String> {
    val safeBitrate = bitrateKbps.coerceIn(64, 320)
    return listOf(
        "-y -i \"$inputWavPath\" -vn -ar 48000 -ac 2 -c:a libopus -b:a ${safeBitrate}k \"$outputOggPath\"",
        "-y -i \"$inputWavPath\" -vn -ar 44100 -ac 2 -c:a libvorbis -b:a ${safeBitrate}k \"$outputOggPath\"",
    )
}

private suspend fun applyPlatformLoudnessNormalization(
    context: Context,
    buffer: StereoAudioBuffer,
    targetLufs: Float,
    whatsappSpeechShape: Boolean,
): StereoAudioBuffer = withContext(Dispatchers.IO) {
    val workDir = File(context.cacheDir, "stream_ops").apply { mkdirs() }
    val id = UUID.randomUUID().toString().take(8)
    val inWav = File(workDir, "norm_in_$id.wav")
    val outWav = File(workDir, "norm_out_$id.wav")
    try {
        if (FfmpegKitBridge.isAvailable()) {
            writeStereoWavFile(inWav, buffer)
            val speechFilter = if (whatsappSpeechShape) "highpass=f=110,acompressor=threshold=-20dB:ratio=3:attack=8:release=80," else ""
            val cmd = "-y -i \"${inWav.absolutePath}\" -af \"${speechFilter}loudnorm=I=$targetLufs:TP=-1.0:LRA=9\" -ar 44100 -ac 2 \"${outWav.absolutePath}\""
            val ok = FfmpegKitBridge.execute(cmd, toolName = "PlatformLoudnessExport", logNonZeroFailure = false)
            if (ok && outWav.exists() && outWav.length() > 44L) {
                AudioDecoder.decodeFile(outWav)?.let { return@withContext it }
            }
        }
    } finally {
        runCatching { inWav.delete() }
        runCatching { outWav.delete() }
    }
    // Pure Kotlin fallback loudness + true peak ceiling
    val targetPeakDb = when {
        targetLufs >= -14.5f -> -1.0f
        else -> -1.5f
    }
    val leveled = applyAutoCompressorLimiter(context, buffer, strengthPct = if (whatsappSpeechShape) 75f else 55f)
    normalizeAudio(leveled, targetPeakDb)
}
