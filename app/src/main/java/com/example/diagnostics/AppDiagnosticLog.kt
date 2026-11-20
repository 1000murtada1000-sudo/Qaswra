package com.example.diagnostics

import android.content.Context
import android.util.Log
import com.example.model.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Serializable
enum class DiagnosticSeverity {
    RECOVERABLE,
    CRITICAL,
}

@Serializable
data class AppDiagnosticEntry(
    val id: String,
    val severity: DiagnosticSeverity,
    val titleAr: String,
    val titleEn: String,
    val causeAr: String,
    val causeEn: String,
    val toolName: String,
    val errorCode: String,
    val stackTrace: String,
    val timestampMs: Long,
    val occurrenceCount: Int = 1,
    val isRead: Boolean = false,
)

/**
 * Central crash & technical diagnostics registry for Sonix.
 * - Captures recoverable tool/FFmpeg/network/coroutine errors without killing the app.
 * - Captures critical errors and presents a user-consent restart dialog instead of force closing.
 * - Persists logs cleanly in app filesDir so users can inspect & copy stack traces anytime.
 */
object AppDiagnosticLog {
    private const val FILE_NAME = "sonix_diagnostics.json"
    private const val MAX_ENTRIES = 120

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    @Volatile
    private var storageFile: File? = null

    private val lock = Any()

    private val _entries = MutableStateFlow<List<AppDiagnosticEntry>>(emptyList())
    val entries: StateFlow<List<AppDiagnosticEntry>> = _entries.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _criticalRestartPrompt = MutableStateFlow<AppDiagnosticEntry?>(null)
    val criticalRestartPrompt: StateFlow<AppDiagnosticEntry?> = _criticalRestartPrompt.asStateFlow()

    fun init(context: Context) {
        val appCtx = context.applicationContext ?: context
        synchronized(lock) {
            if (storageFile != null) return
            val dir = appCtx.filesDir ?: appCtx.cacheDir
            storageFile = File(dir, FILE_NAME)
            val loaded = readFromDisk()
            _entries.value = loaded
            _unreadCount.value = loaded.count { !it.isRead }
        }
    }

    fun logRecoverable(
        toolName: String,
        titleAr: String,
        titleEn: String,
        causeAr: String,
        causeEn: String,
        errorCode: String = "ERR_RECOVERABLE",
        throwable: Throwable? = null,
        extraTrace: String? = null,
    ): AppDiagnosticEntry {
        val trace = buildTrace(throwable, extraTrace)
        val memoryPressure = isMemoryPressure(errorCode, titleAr, titleEn, causeAr, causeEn, trace, throwable)
        val entry = AppDiagnosticEntry(
            id = UUID.randomUUID().toString(),
            severity = DiagnosticSeverity.RECOVERABLE,
            titleAr = safeTitle(titleAr, memoryPressure, AppLanguage.AR),
            titleEn = safeTitle(titleEn, memoryPressure, AppLanguage.EN),
            causeAr = safeCause(causeAr, memoryPressure, AppLanguage.AR),
            causeEn = safeCause(causeEn, memoryPressure, AppLanguage.EN),
            toolName = toolName.ifBlank { "Studio" },
            errorCode = if (memoryPressure) "OOM_GUARD" else errorCode.ifBlank { throwable?.javaClass?.simpleName ?: "RECOVERABLE" },
            stackTrace = trace,
            timestampMs = System.currentTimeMillis(),
        )
        appendEntry(entry)
        return entry
    }

    fun logCritical(
        toolName: String,
        titleAr: String,
        titleEn: String,
        causeAr: String,
        causeEn: String,
        errorCode: String = "ERR_CRITICAL",
        throwable: Throwable? = null,
        extraTrace: String? = null,
        requestUserRestartConsent: Boolean = true,
    ): AppDiagnosticEntry {
        val trace = buildTrace(throwable, extraTrace)
        val memoryPressure = isMemoryPressure(errorCode, titleAr, titleEn, causeAr, causeEn, trace, throwable)
        val entry = AppDiagnosticEntry(
            id = UUID.randomUUID().toString(),
            severity = DiagnosticSeverity.CRITICAL,
            titleAr = safeTitle(titleAr, memoryPressure, AppLanguage.AR),
            titleEn = safeTitle(titleEn, memoryPressure, AppLanguage.EN),
            causeAr = safeCause(causeAr, memoryPressure, AppLanguage.AR),
            causeEn = safeCause(causeEn, memoryPressure, AppLanguage.EN),
            toolName = toolName.ifBlank { "System" },
            errorCode = if (memoryPressure) "OOM_GUARD" else errorCode.ifBlank { throwable?.javaClass?.simpleName ?: "CRITICAL" },
            stackTrace = trace,
            timestampMs = System.currentTimeMillis(),
        )
        appendEntry(entry)
        if (requestUserRestartConsent) {
            _criticalRestartPrompt.value = entry
        }
        return entry
    }

    fun showPromptForEntryId(entryId: String) {
        val found = _entries.value.firstOrNull { it.id == entryId }
        if (found != null) {
            _criticalRestartPrompt.value = found
        }
    }

    fun showPrompt(entry: AppDiagnosticEntry) {
        _criticalRestartPrompt.value = entry
    }

    fun dismissCriticalPrompt() {
        _criticalRestartPrompt.value = null
    }

    fun markAllRead() {
        synchronized(lock) {
            val read = _entries.value.map { it.copy(isRead = true) }
            _entries.value = read
            _unreadCount.value = 0
            writeToDisk(read)
        }
    }

    fun markRead(id: String) {
        synchronized(lock) {
            val current = _entries.value
            val updated = current.map { entry ->
                if (entry.id == id) entry.copy(isRead = true) else entry
            }
            if (updated != current) {
                _entries.value = updated
                _unreadCount.value = updated.count { !it.isRead }
                writeToDisk(updated)
            }
        }
    }

    fun clearAll() {
        synchronized(lock) {
            _entries.value = emptyList()
            _unreadCount.value = 0
            runCatching { storageFile?.delete() }
        }
    }

    fun findById(id: String): AppDiagnosticEntry? = _entries.value.firstOrNull { it.id == id }

    fun formatTimestamp(ts: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(ts))
    }

    fun formatFullReport(entry: AppDiagnosticEntry): String {
        return buildString {
            appendLine("Sonix Support Summary")
            appendLine("Tool: Sonix audio processing")
            appendLine("Time: ${formatTimestamp(entry.timestampMs)}")
            appendLine("Occurrences: ${entry.occurrenceCount.coerceAtLeast(1)}")
            appendLine("Summary: ${userFacingTitle(entry, AppLanguage.EN)}")
            appendLine(userFacingCause(entry, AppLanguage.EN))
        }
    }

    fun userFacingTitle(entry: AppDiagnosticEntry, language: AppLanguage): String {
        val candidate = if (language == AppLanguage.EN) entry.titleEn else entry.titleAr
        val memoryPressure = isMemoryPressure(
            entry.errorCode,
            entry.titleAr,
            entry.titleEn,
            entry.causeAr,
            entry.causeEn,
            entry.stackTrace,
            null,
        )
        return safeTitle(candidate, memoryPressure, language)
    }

    fun userFacingCause(entry: AppDiagnosticEntry, language: AppLanguage): String {
        val candidate = if (language == AppLanguage.EN) entry.causeEn else entry.causeAr
        val memoryPressure = isMemoryPressure(
            entry.errorCode,
            entry.titleAr,
            entry.titleEn,
            entry.causeAr,
            entry.causeEn,
            entry.stackTrace,
            null,
        )
        return safeCause(candidate, memoryPressure, language)
    }

    fun userFacingTechnicalMessage(message: String, language: AppLanguage): String {
        val memoryPressure = isMemoryPressure(
            errorCode = "",
            titleAr = "",
            titleEn = "",
            causeAr = message,
            causeEn = message,
            trace = message,
            throwable = null,
        )
        return safeCause(message, memoryPressure, language)
    }

    private fun isMemoryPressure(
        errorCode: String,
        titleAr: String,
        titleEn: String,
        causeAr: String,
        causeEn: String,
        trace: String,
        throwable: Throwable?,
    ): Boolean {
        val details = listOf(errorCode, titleAr, titleEn, causeAr, causeEn, trace, throwable?.toString().orEmpty())
            .joinToString(" ")
        return Regex(
            "(?i)(outofmemoryerror|out of memory|failed to allocate|allocation failed|heap|memory pressure|oom[_ -]?guard)",
        ).containsMatchIn(details)
    }

    private fun safeTitle(title: String, memoryPressure: Boolean, language: AppLanguage): String {
        if (memoryPressure) {
            return if (language == AppLanguage.EN) "Temporary memory pressure" else "ضغط مؤقت في الذاكرة"
        }
        if (isTechnicalDump(title)) {
            return if (language == AppLanguage.EN) "Audio operation interrupted" else "تعذر إكمال العملية الصوتية"
        }
        return title.trim().take(100).ifBlank {
            if (language == AppLanguage.EN) "Audio operation notice" else "تنبيه معالجة صوتية"
        }
    }

    private fun safeCause(cause: String, memoryPressure: Boolean, language: AppLanguage): String {
        if (memoryPressure) {
            return if (language == AppLanguage.EN) {
                "The operation was stopped to protect the app. Try a shorter audio clip."
            } else {
                "تم إيقاف العملية لحماية التطبيق. جرّب مقطعاً أقصر."
            }
        }
        if (isTechnicalDump(cause)) {
            return if (language == AppLanguage.EN) {
                "The operation could not be completed. Please try again."
            } else {
                "تعذر إكمال العملية. يرجى المحاولة مرة أخرى."
            }
        }
        return cause.trim().take(180).ifBlank {
            if (language == AppLanguage.EN) "Please try the operation again." else "يرجى المحاولة مرة أخرى."
        }
    }

    private fun isTechnicalDump(text: String): Boolean {
        return text.length > 240 ||
            text.contains('\n') ||
            Regex("(?i)(outofmemoryerror|failed to allocate|java\\.|kotlin\\.|exception|stack trace|caused by:|ffmpeg.*(failed|error)|\\berror\\b|\\bfailed\\b)")
                .containsMatchIn(text)
    }

    private fun buildTrace(throwable: Throwable?, extraTrace: String?): String {
        val parts = buildList {
            if (throwable != null) {
                add(Log.getStackTraceString(throwable).ifBlank { throwable.toString() })
            }
            if (!extraTrace.isNullOrBlank()) {
                add(extraTrace.trim())
            }
        }
        return parts.joinToString("\n\n").ifBlank { "No stack trace captured." }.take(12_000)
    }

    private fun appendEntry(entry: AppDiagnosticEntry) {
        synchronized(lock) {
            val normCode = entry.errorCode.trim()
            val normTool = entry.toolName.trim()
            val currentList = _entries.value
            val existingIdx = currentList.indexOfFirst {
                it.errorCode.trim().equals(normCode, ignoreCase = true) &&
                    it.toolName.trim().equals(normTool, ignoreCase = true)
            }
            val updated = if (existingIdx >= 0) {
                val existing = currentList[existingIdx]
                val mergedSeverity = if (
                    existing.severity == DiagnosticSeverity.CRITICAL ||
                    entry.severity == DiagnosticSeverity.CRITICAL
                ) {
                    DiagnosticSeverity.CRITICAL
                } else {
                    entry.severity
                }
                val merged = existing.copy(
                    severity = mergedSeverity,
                    titleAr = entry.titleAr.ifBlank { existing.titleAr },
                    titleEn = entry.titleEn.ifBlank { existing.titleEn },
                    causeAr = entry.causeAr.ifBlank { existing.causeAr },
                    causeEn = entry.causeEn.ifBlank { existing.causeEn },
                    stackTrace = entry.stackTrace.ifBlank { existing.stackTrace },
                    timestampMs = maxOf(existing.timestampMs, entry.timestampMs),
                    occurrenceCount = (existing.occurrenceCount.coerceAtLeast(1) + 1).coerceAtMost(9999),
                    isRead = false,
                )
                val withoutOld = currentList.toMutableList().apply { removeAt(existingIdx) }
                (listOf(merged) + withoutOld).take(MAX_ENTRIES)
            } else {
                (listOf(entry.copy(occurrenceCount = entry.occurrenceCount.coerceAtLeast(1))) + currentList)
                    .take(MAX_ENTRIES)
            }
            _entries.value = updated
            _unreadCount.value = updated.count { !it.isRead }.coerceAtMost(999)
            writeToDisk(updated)
        }
    }

    private fun readFromDisk(): List<AppDiagnosticEntry> {
        val file = storageFile ?: return emptyList()
        if (!file.exists()) return emptyList()
        return runCatching {
            val text = file.readText()
            if (text.isBlank()) emptyList() else json.decodeFromString<List<AppDiagnosticEntry>>(text)
        }.getOrDefault(emptyList())
    }

    private fun writeToDisk(list: List<AppDiagnosticEntry>) {
        val file = storageFile ?: return
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(list))
        }
    }
}
