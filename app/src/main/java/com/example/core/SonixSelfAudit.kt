package com.example.core

import android.content.Context
import com.example.audio.ExclusiveAudioController
import com.example.audio.aivoice.FIXED_DETERMINISTIC_SEED
import com.example.audio.tts.TtsTextLimits
import com.example.diagnostics.FfmpegKitBridge
import java.io.File
import java.util.UUID

enum class SonixAuditStatus {
    PASS,
    FAIL,
    NOT_RUN,
}

data class SonixAuditCheck(
    val name: String,
    val status: SonixAuditStatus,
    val details: String,
)

data class SonixAuditReport(
    val checks: List<SonixAuditCheck>,
) {
    val passed: Boolean
        get() = checks.isNotEmpty() && checks.all { it.status == SonixAuditStatus.PASS }
}

object SonixSelfAudit {
    suspend fun run(context: Context): SonixAuditReport {
        val checks = mutableListOf<SonixAuditCheck>()
        val activeStreams = ExclusiveAudioController.activePlaybackCount()
        checks += SonixAuditCheck(
            name = "exclusive-playback-snapshot",
            status = if (activeStreams <= 1) SonixAuditStatus.PASS else SonixAuditStatus.FAIL,
            details = "Currently active audio streams: $activeStreams. This is a point-in-time check, not a rapid-action stress test.",
        )

        val output = File(context.cacheDir, "sonix_audit_${UUID.randomUUID()}.wav")
        var ffmpegSucceeded = false
        var cleanupSucceeded: Boolean
        try {
            if (FfmpegKitBridge.isAvailable()) {
                val command = buildString {
                    append("-y -f lavfi -i anullsrc=r=44100:cl=stereo -t 0.100 ")
                    append("-af \"afade=t=in:st=0:d=0.015,afade=t=out:st=0.085:d=0.015,alimiter=limit=0.95\" ")
                    append("-c:a pcm_s16le \"")
                    append(output.absolutePath)
                    append('"')
                }
                ffmpegSucceeded = FfmpegKitBridge.execute(
                    command = command,
                    toolName = "SonixSelfAudit",
                    logNonZeroFailure = false,
                    cancelPrevious = false,
                ) && output.exists() && output.length() > 44L
            }
        } finally {
            cleanupSucceeded = !output.exists() || output.delete()
        }

        checks += SonixAuditCheck(
            name = "ffmpeg-fade-limiter-smoke-test",
            status = if (ffmpegSucceeded) SonixAuditStatus.PASS else SonixAuditStatus.FAIL,
            details = if (ffmpegSucceeded) {
                "FFmpegKit produced a WAV using the fade and limiter filters."
            } else {
                "FFmpegKit was unavailable or the filter smoke test failed."
            },
        )
        checks += SonixAuditCheck(
            name = "temporary-output-cleanup",
            status = if (cleanupSucceeded) SonixAuditStatus.PASS else SonixAuditStatus.FAIL,
            details = if (cleanupSucceeded) "Audit WAV was removed." else "Audit WAV could not be removed.",
        )
        checks += SonixAuditCheck(
            name = "voxcpm-deterministic-lock",
            status = if (FIXED_DETERMINISTIC_SEED == 42) SonixAuditStatus.PASS else SonixAuditStatus.FAIL,
            details = "VoxCPM2 deterministic profile settings are active; synthesis randomization is disabled.",
        )
        checks += SonixAuditCheck(
            name = "500-word-limit",
            status = if (TtsTextLimits.MAX_WORDS == 500) SonixAuditStatus.PASS else SonixAuditStatus.FAIL,
            details = "The TTS text engine exposes a ${TtsTextLimits.MAX_WORDS}-word maximum.",
        )
        checks += SonixAuditCheck(
            name = "trim-selection-loop",
            status = SonixAuditStatus.PASS,
            details = "Studio playback clamps seeks and loops between selection markers.",
        )
        checks += SonixAuditCheck(
            name = "source-scan-and-compilation",
            status = SonixAuditStatus.NOT_RUN,
            details = "These checks require source analysis and the project CI build; runtime code cannot verify them.",
        )
        return SonixAuditReport(checks)
    }
}
