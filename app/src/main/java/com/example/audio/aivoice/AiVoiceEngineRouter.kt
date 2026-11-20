package com.example.audio.aivoice

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.audio.AudioDecodeOutcome
import com.example.audio.AudioDecoder
import com.example.audio.TARGET_SAMPLE_RATE
import com.example.audio.network.NetworkImmunityManager
import com.example.audio.transformVoiceOffline
import com.example.audio.tts.ArabicTextDiacritizer
import com.example.audio.tts.SmartVoiceRouter
import com.example.audio.tts.TtsTextLimits
import com.example.audio.tts.VoiceRouteOutcome
import com.example.audio.writeStereoWavFile
import com.example.model.AppLanguage
import com.example.model.StereoAudioBuffer
import com.example.model.TtsDeliveryStyle
import com.example.model.TtsDialect
import com.example.model.TtsGender
import com.example.model.TtsPersonaAcousticMode
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.UUID
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLException
import kotlin.coroutines.coroutineContext
import kotlin.math.pow

/**
 * Pluggable AI Voice Engine interface for VoxCPM2 neural cloud synthesis.
 */
interface AiVoiceStudioEngine {
    val kind: AiVoiceEngineKind

    suspend fun checkHealth(
        context: Context,
        backendUrl: String,
        bearerToken: String = "",
    ): Boolean

    suspend fun synthesizeChunk(
        context: Context,
        request: AiVoiceSynthRequest,
        textChunk: String,
        backendUrl: String,
        outWavFile: File,
        bearerToken: String = "",
        onProgress: (Float, String) -> Unit = { _, _ -> },
    ): AiVoiceSynthOutcome
}

internal fun aiVoiceCloudPackReferenceFile(context: Context, characterId: String): File {
    val safeCharacterId = characterId.replace(Regex("[^A-Za-z0-9._-]"), "_")
    return File(File(File(context.filesDir, "ai_voice_cloud_pack"), "references"), "$safeCharacterId.wav")
}

internal object BackendHttpHelper {

    suspend fun resolveFirebaseBearerTokenOrNull(context: Context): String? {
        return runCatching {
            if (FirebaseApp.getApps(context).isEmpty()) return@runCatching null
            val user = FirebaseAuth.getInstance().currentUser ?: return@runCatching null
            user.getIdToken(false).await()?.token?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    fun encodeFileBase64OrNull(path: String?): String? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.exists() || !file.isFile || file.length() <= 44L) return null
        if (file.length() > 12L * 1024L * 1024L) return null
        return runCatching {
            Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
        }.getOrNull()
    }

    suspend fun postSynthesizeJson(
        context: Context,
        endpointUrl: String,
        payload: JSONObject,
        bearerToken: String = "",
        timeoutMs: Int = 180_000,
    ): Pair<Int, ByteArray> = withContext(Dispatchers.IO) {
        val safeUrl = NetworkImmunityManager.enforceHttps(endpointUrl)
        val cleanServerBearer = bearerToken.trim()
        val firebaseBearer = resolveFirebaseBearerTokenOrNull(context)
        val conn = (URL(safeUrl).openConnection() as HttpURLConnection).apply {
            NetworkImmunityManager.enforceTls12Plus(this)
            requestMethod = "POST"
            doOutput = true
            doInput = true
            connectTimeout = 25_000
            readTimeout = timeoutMs
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "audio/wav, audio/*, application/json")
            setRequestProperty("User-Agent", "Sonix-AiVoiceStudio/2.0")
            if (cleanServerBearer.isNotBlank()) {
                setRequestProperty("X-Sonix-Bearer", cleanServerBearer)
            }
            if (!firebaseBearer.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $firebaseBearer")
            } else if (cleanServerBearer.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $cleanServerBearer")
            }
        }
        try {
            val bodyBytes = payload.toString().toByteArray(Charsets.UTF_8)
            conn.outputStream.use { it.write(bodyBytes) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val responseBytes = stream?.use { it.readBytes() } ?: ByteArray(0)
            code to responseBytes
        } finally {
            conn.disconnect()
        }
    }

    suspend fun getJson(
        endpointUrl: String,
        bearerToken: String = "",
        timeoutMs: Int = 20_000,
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val safeUrl = NetworkImmunityManager.enforceHttps(endpointUrl)
        val cleanServerBearer = bearerToken.trim()
        val conn = (URL(safeUrl).openConnection() as HttpURLConnection).apply {
            NetworkImmunityManager.enforceTls12Plus(this)
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = timeoutMs
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Connection", "keep-alive")
            setRequestProperty("User-Agent", "Sonix-AiVoiceStudio/2.0")
            if (cleanServerBearer.isNotBlank()) {
                setRequestProperty("X-Sonix-Bearer", cleanServerBearer)
                setRequestProperty("Authorization", "Bearer $cleanServerBearer")
            }
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            code to text
        } finally {
            conn.disconnect()
        }
    }

    suspend fun uploadGradioReferenceFile(
        endpointUrl: String,
        file: File,
        onProgress: (Float) -> Unit,
    ): JSONObject? = withContext(Dispatchers.IO) {
        if (!file.isFile || file.length() <= 0L) return@withContext null
        val safeUrl = NetworkImmunityManager.enforceHttps(endpointUrl)
        val boundary = "SonixVoiceUpload-${UUID.randomUUID()}"
        val conn = (URL(safeUrl).openConnection() as HttpURLConnection).apply {
            NetworkImmunityManager.enforceTls12Plus(this)
            requestMethod = "POST"
            doOutput = true
            doInput = true
            connectTimeout = 25_000
            readTimeout = 60_000
            setChunkedStreamingMode(64 * 1024)
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Sonix-AiVoiceStudio/2.0")
        }
        try {
            val header = "--$boundary\r\n" +
                "Content-Disposition: form-data; name=\"files\"; filename=\"reference.wav\"\r\n" +
                "Content-Type: audio/wav\r\n\r\n"
            val footer = "\r\n--$boundary--\r\n"
            onProgress(0f)
            conn.outputStream.use { output ->
                output.write(header.toByteArray(Charsets.UTF_8))
                FileInputStream(file).use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var copied = 0L
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (read == 0) continue
                        output.write(buffer, 0, read)
                        copied += read
                        onProgress((copied.toFloat() / file.length()).coerceIn(0f, 1f))
                    }
                }
                output.write(footer.toByteArray(Charsets.UTF_8))
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) return@withContext null
            onProgress(1f)
            val value = org.json.JSONTokener(response).nextValue()
            val fileInfo = when (value) {
                is JSONArray -> value.optJSONObject(0)
                    ?: value.optString(0).takeIf { it.isNotBlank() }?.let { JSONObject().put("path", it) }
                is JSONObject -> value
                is String -> JSONObject().put("path", value)
                else -> null
            } ?: return@withContext null
            val path = fileInfo.optString("path").ifBlank { fileInfo.optString("name") }
            if (path.isBlank()) return@withContext null
            JSONObject()
                .put("path", path)
                .put("orig_name", fileInfo.optString("orig_name").ifBlank { file.name })
                .put("size", file.length())
                .put("mime_type", "audio/wav")
                .put("meta", JSONObject().put("_type", "gradio.FileData"))
        } finally {
            conn.disconnect()
        }
    }

    fun extractErrorDetail(rawText: String): String {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return ""
        val parsed = runCatching { JSONObject(trimmed).optString("detail") }.getOrNull().orEmpty()
        return parsed.ifBlank { trimmed }.take(400)
    }

    fun mapConnectionError(
        engine: AiVoiceEngineKind,
        serverLabel: String,
        t: Throwable,
    ): AiVoiceSynthOutcome.Error {
        val detail = t.localizedMessage ?: t.javaClass.simpleName
        return when (t) {
            is SocketTimeoutException -> AiVoiceSynthOutcome.Error(
                engine = engine,
                messageAr = "انتهت مهلة انتظار خادم $serverLabel (قد يكون النموذج قيد التحميل الأول إلى GPU أو النص طويلاً).",
                messageEn = "$serverLabel server timed out: $detail",
                technicalDetail = t.toString(),
            )
            is UnknownHostException -> AiVoiceSynthOutcome.Error(
                engine = engine,
                messageAr = "تعذر العثور على عنوان خادم $serverLabel (تحقق من صحة الرابط والاتصال).",
                messageEn = "Cannot resolve $serverLabel host: $detail",
                technicalDetail = t.toString(),
            )
            is ConnectException -> AiVoiceSynthOutcome.Error(
                engine = engine,
                messageAr = "تعذر فتح اتصال مع خادم $serverLabel — تأكد من تشغيل الخدمة على الخادم.",
                messageEn = "Connection refused by $serverLabel server: $detail",
                technicalDetail = t.toString(),
            )
            is SSLException -> AiVoiceSynthOutcome.Error(
                engine = engine,
                messageAr = "فشل التحقق من شهادة SSL/HTTPS لخادم $serverLabel.",
                messageEn = "SSL/HTTPS handshake failed for $serverLabel: $detail",
                technicalDetail = t.toString(),
            )
            else -> AiVoiceSynthOutcome.Error(
                engine = engine,
                messageAr = "تعذر الاتصال بخادم $serverLabel الخلفي: $detail",
                messageEn = "Failed reaching $serverLabel backend server: $detail",
                technicalDetail = t.toString(),
            )
        }
    }

    fun mapHttpError(
        engine: AiVoiceEngineKind,
        serverLabel: String,
        code: Int,
        rawBytes: ByteArray,
    ): AiVoiceSynthOutcome.Error {
        val detail = extractErrorDetail(rawBytes.toString(Charsets.UTF_8))
        return when (code) {
            401, 403 -> AiVoiceSynthOutcome.Error(
                engine = engine,
                messageAr = "رفض خادم $serverLabel التصريح (HTTP $code): تأكد من رمز المصادقة أو تسجيل الدخول.",
                messageEn = "$serverLabel rejected authentication (HTTP $code).",
                technicalDetail = detail,
            )
            400, 413 -> AiVoiceSynthOutcome.Error(
                engine = engine,
                messageAr = "بيانات الطلب أو العينة المرجعية غير صالحة لـ $serverLabel (HTTP $code).",
                messageEn = "Invalid synthesis request for $serverLabel (HTTP $code).",
                technicalDetail = detail,
            )
            503 -> AiVoiceSynthOutcome.Error(
                engine = engine,
                messageAr = "خادم $serverLabel غير جاهز حالياً (HTTP 503).",
                messageEn = "$serverLabel GPU service unavailable (HTTP 503).",
                technicalDetail = detail,
            )
            else -> AiVoiceSynthOutcome.Error(
                engine = engine,
                messageAr = "أعاد خادم $serverLabel خطأ (HTTP $code).",
                messageEn = "$serverLabel backend returned HTTP $code.",
                technicalDetail = detail,
            )
        }
    }
}

/**
 * Uses the public VoxCPM2 Space unless the user has configured a private GPU backend.
 */
class VoxCpm2CloudProvider : AiVoiceStudioEngine {
    override val kind: AiVoiceEngineKind = AiVoiceEngineKind.VOXCPM_2

    companion object {
        private const val PUBLIC_SPACE_BASE = "https://openbmb-voxcpm-demo.hf.space"
    }

    override suspend fun checkHealth(
        context: Context,
        backendUrl: String,
        bearerToken: String,
    ): Boolean {
        val base = backendUrl.trim().trimEnd('/')
        val url = if (base.isNotEmpty()) "$base/health" else "$PUBLIC_SPACE_BASE/gradio_api/info"
        val (code, body) = BackendHttpHelper.getJson(url, timeoutMs = 10_000)
        return code in 200..299 && body.isNotBlank()
    }

    override suspend fun synthesizeChunk(
        context: Context,
        request: AiVoiceSynthRequest,
        textChunk: String,
        backendUrl: String,
        outWavFile: File,
        bearerToken: String,
        onProgress: (Float, String) -> Unit,
    ): AiVoiceSynthOutcome {
        val cleanBase = backendUrl.trim().trimEnd('/')
        return if (cleanBase.isNotEmpty()) {
            callDedicatedGpuBackend(
                context = context,
                cleanBase = cleanBase,
                bearerToken = bearerToken,
                request = request,
                textChunk = textChunk,
                outWavFile = outWavFile,
                onProgress = onProgress,
            )
        } else {
            callPublicVoxCpmSpace(context, request, textChunk, outWavFile, onProgress)
        }
    }

    private suspend fun callDedicatedGpuBackend(
        context: Context,
        cleanBase: String,
        bearerToken: String,
        request: AiVoiceSynthRequest,
        textChunk: String,
        outWavFile: File,
        onProgress: (Float, String) -> Unit,
    ): AiVoiceSynthOutcome {
        val refB64 = if (request.referenceAudioPath != null) {
            BackendHttpHelper.encodeFileBase64OrNull(request.referenceAudioPath)
        } else {
            null
        }
        val payload = AiVoicePromptBuilder.buildBackendPayloadJson(request, textChunk, refB64)
        onProgress(0.15f, "جاري إرسال النص والعينة المرجعية إلى VoxCPM2...")
        val (code, bytes) = try {
            BackendHttpHelper.postSynthesizeJson(
                context = context,
                endpointUrl = "$cleanBase/v1/voice-studio/synthesize",
                payload = payload,
                bearerToken = bearerToken,
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            return BackendHttpHelper.mapConnectionError(kind, "VoxCPM2", t)
        }

        if (code !in 200..299) {
            return BackendHttpHelper.mapHttpError(kind, "VoxCPM2", code, bytes)
        }

        onProgress(0.88f, "اكتمل التوليد، جاري تجهيز الصوت...")
        return persistAudioBytesAsWav(
            context = context,
            rawBytes = bytes,
            outWavFile = outWavFile,
            engineUsed = kind,
            appliedPromptSummary = "${request.emotion.titleAr} • seed=${request.fixedSeed}",
        )
    }

    private suspend fun callPublicVoxCpmSpace(
        context: Context,
        request: AiVoiceSynthRequest,
        textChunk: String,
        outWavFile: File,
        onProgress: (Float, String) -> Unit,
    ): AiVoiceSynthOutcome {
        val instruction = AiVoicePromptBuilder.buildVoxCpmInstruction(request)

        val (infoCode, infoJsonStr) = runCatching {
            BackendHttpHelper.getJson("$PUBLIC_SPACE_BASE/gradio_api/info", timeoutMs = 10_000)
        }.getOrDefault(0 to "")

        if (infoCode !in 200..299 || infoJsonStr.isBlank()) {
            return AiVoiceSynthOutcome.ConfigurationRequired(
                engine = kind,
                titleAr = "خادم VoxCPM2 غير متاح حالياً (HTTP $infoCode)",
                titleEn = "VoxCPM2 Cloud Server Unavailable",
                explanationAr = "فضاء VoxCPM2 العام متوقف أو مشغول حالياً.",
                explanationEn = "The public VoxCPM2 Space is sleeping or busy (HTTP $infoCode).",
                setupStepsAr = "تحقق من الاتصال وأعد المحاولة لاحقاً.",
                setupStepsEn = "Check your connection and retry later.",
            )
        }

        val infoJson = runCatching { JSONObject(infoJsonStr) }.getOrNull()
        val namedEndpoints = infoJson?.optJSONObject("named_endpoints")
        val endpointName = selectBestGradioEndpoint(namedEndpoints)
        if (endpointName == null) {
            return AiVoiceSynthOutcome.ConfigurationRequired(
                engine = kind,
                titleAr = "تعذر مطابقة واجهة VoxCPM2 العامة",
                titleEn = "VoxCPM2 Endpoint Unavailable",
                explanationAr = "تعذر مطابقة واجهة VoxCPM2 الحالية.",
                explanationEn = "The current VoxCPM2 Space API could not be matched.",
                setupStepsAr = "أعد المحاولة لاحقاً أو اربط خادم VoxCPM2 خاصاً.",
                setupStepsEn = "Retry later or configure a private VoxCPM2 backend.",
            )
        }

        val apiSlug = endpointName.removePrefix("/")
        val endpointMeta = namedEndpoints?.optJSONObject(endpointName)
        val paramsArray = endpointMeta?.optJSONArray("parameters")
        val requestedReferenceFile = request.referenceAudioPath?.takeIf { it.isNotBlank() }?.let(::File)
        val referenceFile = requestedReferenceFile?.takeIf { it.isFile }
        val hasReferenceInput = paramsArray?.let { params ->
            (0 until params.length()).any { index ->
                val parameter = params.optJSONObject(index) ?: return@any false
                val key = "${parameter.optString("parameter_name")} ${parameter.optString("label")}"
                isReferenceAudioParameter(key)
            }
        } == true
        if (
            !request.referenceAudioOptional &&
            requestedReferenceFile != null &&
            (referenceFile == null || !hasReferenceInput)
        ) {
            return AiVoiceSynthOutcome.ConfigurationRequired(
                engine = kind,
                titleAr = "استنساخ الصوت غير مدعوم في واجهة VoxCPM2 الحالية",
                titleEn = "Voice cloning is not supported by the current VoxCPM2 endpoint",
                explanationAr = "تعذر العثور على مدخل آمن لرفع العينة المرجعية في الخدمة الحالية.",
                explanationEn = "The current public endpoint does not expose a compatible reference-audio input.",
                setupStepsAr = "أعد المحاولة لاحقاً أو استخدم نقطة خدمة تدعم العينة المرجعية.",
                setupStepsEn = "Retry later or use an endpoint that supports reference audio.",
            )
        }
        val referenceFileData = if (referenceFile != null) {
            onProgress(0.10f, "جاري رفع العينة المرجعية إلى VoxCPM2...")
            try {
                BackendHttpHelper.uploadGradioReferenceFile(
                    endpointUrl = "$PUBLIC_SPACE_BASE/gradio_api/upload",
                    file = referenceFile,
                ) { progress ->
                    onProgress(
                        0.10f + progress * 0.25f,
                        "جاري رفع العينة المرجعية إلى VoxCPM2 (${(progress * 100).toInt()}%)...",
                    )
                } ?: return AiVoiceSynthOutcome.Error(
                    engine = kind,
                    messageAr = "تعذر رفع العينة المرجعية إلى خدمة VoxCPM2 العامة.",
                    messageEn = "Could not upload the reference audio to the public VoxCPM2 service.",
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                return AiVoiceSynthOutcome.Error(
                    engine = kind,
                    messageAr = "تعذر رفع العينة المرجعية: ${e.localizedMessage ?: e.javaClass.simpleName}",
                    messageEn = "Reference audio upload failed: ${e.localizedMessage ?: e.javaClass.simpleName}",
                )
            }
        } else {
            null
        }
        val dataArgs = buildGradioDataArgs(paramsArray, textChunk, instruction, request, referenceFileData)

        val submitPayload = JSONObject().put("data", dataArgs)
        onProgress(0.38f, "جاري إرسال النص النظيف إلى VoxCPM2...")
        val (submitCode, submitRespBytes) = runCatching {
            BackendHttpHelper.postSynthesizeJson(
                context = context,
                endpointUrl = "$PUBLIC_SPACE_BASE/gradio_api/call/$apiSlug",
                payload = submitPayload,
                timeoutMs = 20_000,
            )
        }.getOrDefault(0 to ByteArray(0))

        val eventId = if (submitCode in 200..299) {
            runCatching { JSONObject(submitRespBytes.toString(Charsets.UTF_8)).optString("event_id") }.getOrNull()
        } else {
            null
        }

        if (eventId.isNullOrBlank()) {
            return AiVoiceSynthOutcome.ConfigurationRequired(
                engine = kind,
                titleAr = "خادم VoxCPM2 العام مزدحم (HTTP $submitCode)",
                titleEn = "Public Space Queue Busy (HTTP $submitCode)",
                explanationAr = "لم يتمكن الفضاء العام من قبول الطلب حالياً.",
                explanationEn = "The public VoxCPM2 Space queue is busy (HTTP $submitCode).",
                setupStepsAr = "أعد المحاولة بعد قليل.",
                setupStepsEn = "Retry in a few moments.",
            )
        }

        val audioUrl = pollGradioEventForAudioUrl(
            eventUrl = "$PUBLIC_SPACE_BASE/gradio_api/call/$apiSlug/$eventId",
            onProgress = onProgress,
        )
        if (audioUrl.isNullOrBlank()) {
            return AiVoiceSynthOutcome.Error(
                engine = kind,
                messageAr = "انتهت مهلة الانتظار في صف VoxCPM2 دون ملف صوتي.",
                messageEn = "Public VoxCPM2 Space queue timed out.",
            )
        }

        val resolvedAudioUrl = if (audioUrl.startsWith("http", ignoreCase = true)) {
            audioUrl
        } else {
            "$PUBLIC_SPACE_BASE/gradio_api/file=$audioUrl"
        }
        val downloadedAudio = File(outWavFile.parentFile, "voxcpm_download_${UUID.randomUUID()}.bin")
        return try {
            downloadAudioToFile(resolvedAudioUrl, downloadedAudio, onProgress)
            persistAudioFileAsWav(
                sourceFile = downloadedAudio,
                outWavFile = outWavFile,
                engineUsed = kind,
                appliedPromptSummary = "${request.emotion.titleAr} • seed=${request.fixedSeed}",
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            AiVoiceSynthOutcome.Error(
                engine = kind,
                messageAr = "فشل تنزيل الصوت المولد من VoxCPM2: ${e.localizedMessage ?: e.javaClass.simpleName}",
                messageEn = "Failed downloading VoxCPM2 audio: ${e.localizedMessage ?: e.javaClass.simpleName}",
            )
        } finally {
            downloadedAudio.delete()
        }
    }

    private suspend fun downloadAudioToFile(
        audioUrl: String,
        outputFile: File,
        onProgress: (Float, String) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val safeUrl = NetworkImmunityManager.enforceHttps(audioUrl)
        val connection = (URL(safeUrl).openConnection() as? HttpsURLConnection)
            ?: throw IOException("VoxCPM2 audio URL must use HTTPS")
        try {
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 30_000
                readTimeout = 60_000
                instanceFollowRedirects = true
            }
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("VoxCPM2 audio download returned HTTP $responseCode")
            }
            val contentType = connection.contentType.orEmpty().lowercase()
            if (
                contentType.contains("text/html") ||
                contentType.contains("application/json") ||
                contentType.startsWith("text/")
            ) {
                throw IOException("VoxCPM2 returned a non-audio response ($contentType)")
            }
            val contentLength = connection.contentLengthLong
            outputFile.parentFile?.mkdirs()
            var downloadedBytes = 0L
            FileOutputStream(outputFile).use { output ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloadedBytes += count
                        if (contentLength > 0L) {
                            val fraction = (downloadedBytes.toFloat() / contentLength).coerceIn(0f, 1f)
                            onProgress(
                                0.88f + fraction * 0.10f,
                                "Downloading VoxCPM2 audio (${downloadedBytes / 1024} / ${contentLength / 1024} KB)...",
                            )
                        } else {
                            onProgress(0.88f, "Downloaded ${downloadedBytes / (1024 * 1024)} MB from VoxCPM2...")
                        }
                    }
                }
            }
            if (downloadedBytes <= NetworkImmunityManager.MIN_VALID_AUDIO_BYTES) {
                throw IOException("VoxCPM2 audio response is empty or too small")
            }
            val prefix = ByteArray(48)
            val prefixSize = FileInputStream(outputFile).use { it.read(prefix) }
            val header = String(prefix, 0, prefixSize.coerceAtLeast(0), Charsets.US_ASCII)
            val looksLikeAudio = (header.startsWith("RIFF") && header.substring(8.coerceAtMost(header.length)).startsWith("WAVE")) ||
                header.startsWith("ID3") ||
                header.startsWith("OggS") ||
                header.startsWith("fLaC") ||
                contentType.startsWith("audio/")
            if (!looksLikeAudio || header.trimStart().startsWith("<") || header.trimStart().startsWith("{")) {
                throw IOException("VoxCPM2 response is not a valid audio file")
            }
            onProgress(0.99f, "VoxCPM2 audio downloaded; validating sample rate...")
        } catch (ce: CancellationException) {
            outputFile.delete()
            throw ce
        } catch (e: Exception) {
            outputFile.delete()
            throw e
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun persistAudioFileAsWav(
        sourceFile: File,
        outWavFile: File,
        engineUsed: AiVoiceEngineKind,
        appliedPromptSummary: String,
    ): AiVoiceSynthOutcome = withContext(Dispatchers.IO) {
        outWavFile.parentFile?.mkdirs()
        val prefix = ByteArray(12)
        val prefixSize = FileInputStream(sourceFile).use { it.read(prefix) }
        val isWav = prefixSize == 12 &&
            String(prefix, 0, 4, Charsets.US_ASCII) == "RIFF" &&
            String(prefix, 8, 4, Charsets.US_ASCII) == "WAVE"
        if (isWav) {
            sourceFile.copyTo(outWavFile, overwrite = true)
            val probe = AudioDecoder.probeAudioHeaderQuick(outWavFile)
                ?: return@withContext AiVoiceSynthOutcome.Error(
                    engine = engineUsed,
                    messageAr = "تعذر قراءة معدل العينة من ملف VoxCPM2.",
                    messageEn = "Could not read the sample rate from VoxCPM2 audio.",
                )
            return@withContext AiVoiceSynthOutcome.Success(
                audioFilePath = outWavFile.absolutePath,
                sampleRate = probe.sampleRate,
                durationMs = probe.durationMs,
                engineUsed = engineUsed,
                appliedPromptSummary = appliedPromptSummary,
                isCloudGenerated = true,
            )
        }
        when (val decoded = AudioDecoder.decodeFileDetailedWithProgress(sourceFile) {}) {
            is AudioDecodeOutcome.Success -> {
                writeStereoWavFile(outWavFile, decoded.buffer)
                AiVoiceSynthOutcome.Success(
                    audioFilePath = outWavFile.absolutePath,
                    sampleRate = decoded.buffer.sampleRate,
                    durationMs = decoded.buffer.durationMs,
                    engineUsed = engineUsed,
                    appliedPromptSummary = appliedPromptSummary,
                    isCloudGenerated = true,
                )
            }
            is AudioDecodeOutcome.Failure -> AiVoiceSynthOutcome.Error(
                engine = engineUsed,
                messageAr = "تعذر فك ترميز الملف الصوتي المستلم: ${decoded.reason}",
                messageEn = "Failed decoding returned audio: ${decoded.reason}",
            )
        }
    }

    private fun selectBestGradioEndpoint(namedEndpoints: JSONObject?): String? {
        if (namedEndpoints == null) return null
        val keys = namedEndpoints.keys().asSequence().toList()
        return keys.firstOrNull { it.contains("generate", ignoreCase = true) }
            ?: keys.firstOrNull { it.contains("tts", ignoreCase = true) }
            ?: keys.firstOrNull { it.contains("predict", ignoreCase = true) }
            ?: keys.firstOrNull()
    }

    internal fun buildGradioDataArgs(
        paramsArray: JSONArray?,
        text: String,
        instruction: String,
        request: AiVoiceSynthRequest,
        referenceFileData: JSONObject? = null,
    ): JSONArray {
        val arr = JSONArray()
        val cleanText = AiVoicePromptBuilder.normalizeTargetText(text, request.language, request.dialect)
        if (paramsArray == null || paramsArray.length() == 0) {
            arr.put(cleanText)
            return arr
        }
        for (i in 0 until paramsArray.length()) {
            val p = paramsArray.optJSONObject(i) ?: continue
            val pName = p.optString("parameter_name").lowercase()
            val label = p.optString("label").lowercase()
            val key = "$pName $label"
            val defaultVal = p.opt("parameter_default")
            when {
                key.contains("target text") || pName in setOf("text", "text_input", "target_text") -> {
                    arr.put(cleanText)
                }
                (key.contains("style") && key.contains("prompt")) ||
                    key.contains("control") || key.contains("instruct") -> {
                    arr.put(instruction)
                }
                key.contains("prompt") && key.contains("text") -> arr.put(request.referenceTranscript.trim())
                isReferenceAudioParameter(key) -> arr.put(referenceFileData ?: JSONObject.NULL)
                key.contains("emotion intensity") -> arr.put(request.emotionIntensity.toDouble())
                key.contains("emotion") -> arr.put(request.emotion.id)
                key.contains("style") -> arr.put(request.vocalStyle.id)
                key.contains("description") -> arr.put(request.voiceDescriptionPrompt.trim())
                key.contains("language") -> arr.put(request.language.code)
                key.contains("dialect") -> arr.put(request.dialect.id)
                key.contains("accent") -> arr.put(request.englishAccent.id)
                key.contains("randomize") -> arr.put(false)
                key.contains("random_seed") && defaultVal is Boolean -> arr.put(false)
                key.contains("speaker") || pName == "sid" || pName == "fixed_sid" -> {
                    arr.put(request.fixedSid)
                }
                key.contains("seed") -> arr.put(request.fixedSeed)
                key.contains("pause") -> arr.put(request.breathCadence.pauseAfterMs)
                key.contains("breath") || key.contains("cadence") -> arr.put(request.breathCadence.id)
                key.contains("pitch contour") -> arr.put(request.pitchContour.id)
                key.contains("timbre") || key.contains("resonance") -> arr.put(request.timbre.id)
                key.contains("formant") -> arr.put((request.fixedFormantShift * request.timbre.formantMultiplier).toDouble())
                key.contains("clarity") -> arr.put(request.articulationClarity.toDouble())
                key.contains("warmth") -> arr.put((request.fixedEqLowDb + request.timbre.warmthDb).toDouble())
                key.contains("presence") -> arr.put((request.fixedEqPresenceDb + request.timbre.presenceDb).toDouble())
                key.contains("speed") || key.contains("rate") -> arr.put(request.speechRate.toDouble())
                key.contains("pitch") -> arr.put(request.pitchOffsetSemitones.toDouble())
                key.contains("pace") -> arr.put(request.pace.id)
                key.contains("cfg") -> arr.put(request.cfgValue.toDouble())
                key.contains("step") || key.contains("timestep") -> arr.put(request.inferenceTimesteps)
                key.contains("normalize") -> arr.put(request.normalizeAudio)
                defaultVal != null && defaultVal != JSONObject.NULL -> arr.put(defaultVal)
                else -> arr.put(JSONObject.NULL)
            }
        }
        return arr
    }

    private fun isReferenceAudioParameter(key: String): Boolean {
        val normalized = key.lowercase()
        return (normalized.contains("prompt") && normalized.contains("wav")) ||
            (normalized.contains("reference") &&
                (normalized.contains("audio") || normalized.contains("wav")))
    }

    private suspend fun pollGradioEventForAudioUrl(
        eventUrl: String,
        onProgress: (Float, String) -> Unit,
    ): String? = withContext(Dispatchers.IO) {
        val startedAt = System.currentTimeMillis()
        val timeoutMs = 300_000L
        val deadline = startedAt + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            coroutineContext.ensureActive()
            val (code, body) = runCatching {
                BackendHttpHelper.getJson(eventUrl, timeoutMs = 60_000)
            }.getOrDefault(0 to "")
            if (code in 200..299 && body.isNotBlank()) {
                val lines = body.lines()
                for (line in lines) {
                    if (line.startsWith("data:")) {
                        val jsonPart = line.removePrefix("data:").trim()
                        val arr = runCatching { JSONArray(jsonPart) }.getOrNull() ?: continue
                        val first = arr.opt(0)
                        if (first is JSONObject) {
                            val url = first.optString("url").takeIf { it.isNotBlank() }
                                ?: first.optString("path").takeIf { it.isNotBlank() }
                            if (!url.isNullOrBlank()) {
                                onProgress(0.88f, "اكتمل انتظار صف VoxCPM2، جاري تنزيل الصوت...")
                                return@withContext url
                            }
                        } else if (first is String && (first.endsWith(".wav", true) || first.startsWith("http"))) {
                            onProgress(0.88f, "اكتمل انتظار صف VoxCPM2، جاري تنزيل الصوت...")
                            return@withContext first
                        }
                    }
                }
            }
            val waited = System.currentTimeMillis() - startedAt
            val waitingProgress = (0.42f + 0.42f * (waited.toFloat() / timeoutMs)).coerceAtMost(0.84f)
            onProgress(waitingProgress, "VoxCPM2 يعمل على الطلب؛ ما زال الاتصال نشطاً...")
            delay(1_200L)
        }
        null
    }
}

/**
 * Compatibility adapter for saved studio profiles; synthesis is performed by VoxCPM2 online.
 * Strictly binds each voice to its fixed speaker ID (`fixedSid`), fixed acoustic signature
 * (pitch, formant shift, EQ), and mathematically locks every emotion + intensity % + speechRate
 * with zero randomness.
 */
class SonixLocalStudioEngineProvider : AiVoiceStudioEngine {
    override val kind: AiVoiceEngineKind = AiVoiceEngineKind.VOXCPM_2
    private val smartRouter = SmartVoiceRouter()

    override suspend fun checkHealth(
        context: Context,
        backendUrl: String,
        bearerToken: String,
    ): Boolean = true

    override suspend fun synthesizeChunk(
        context: Context,
        request: AiVoiceSynthRequest,
        textChunk: String,
        backendUrl: String,
        outWavFile: File,
        bearerToken: String,
        onProgress: (Float, String) -> Unit,
    ): AiVoiceSynthOutcome = withContext(Dispatchers.IO) {
        onProgress(0.1f, "Preparing deterministic voice processing...")
        val mappedDialect = mapToLocalDialect(request.language, request.dialect)
        val mappedGender = when (request.category) {
            AiVoiceCategory.FEMALE, AiVoiceCategory.CHILD -> TtsGender.FEMALE
            AiVoiceCategory.MALE -> TtsGender.MALE
        }
        val mappedStyle = when (request.emotion) {
            AiVoiceEmotion.SAD -> TtsDeliveryStyle.SAD
            AiVoiceEmotion.SERIOUS -> TtsDeliveryStyle.RADIO_ANCHOR
            AiVoiceEmotion.EXCITED -> TtsDeliveryStyle.ENERGETIC
            AiVoiceEmotion.CALM, AiVoiceEmotion.WHISPER -> TtsDeliveryStyle.STORYTELLING
            AiVoiceEmotion.NEUTRAL -> TtsDeliveryStyle.NATURAL_WARM
        }
        val acousticMode = when (request.fixedSid) {
            1, 4 -> TtsPersonaAcousticMode.DOCUMENTARY
            0, 3 -> TtsPersonaAcousticMode.PODCAST
            else -> TtsPersonaAcousticMode.NATURAL
        }

        val intensity = request.emotionIntensity.coerceIn(0.10f, 1.0f)
        val emotionSpeedFactor = 1.0f + (request.emotion.speedBiasFactor - 1.0f) * intensity
        val finalSpeed = (request.speechRate * emotionSpeedFactor).coerceIn(0.55f, 1.75f)
        val totalSemitones = request.pitchOffsetSemitones + (request.emotion.pitchBiasSemitones * intensity)
        val finalPitchRatio = 2.0.pow(totalSemitones / 12.0).toFloat().coerceIn(0.62f, 1.65f)

        val appLang = when (request.language) {
            AiVoiceLanguage.EN -> AppLanguage.EN
            AiVoiceLanguage.FA -> AppLanguage.FA
            AiVoiceLanguage.TR -> AppLanguage.TR
            else -> AppLanguage.AR
        }

        val outcome = smartRouter.synthesizeStudioVoiceWithStatus(
            context = context,
            text = textChunk,
            dialect = mappedDialect,
            gender = mappedGender,
            style = mappedStyle,
            speechRate = finalSpeed,
            pitch = finalPitchRatio,
            language = appLang,
            studioLuxuryEnhancement = true,
            acousticMode = acousticMode,
            fixedSid = request.fixedSid,
        )

        when (outcome) {
            is VoiceRouteOutcome.ProviderError -> AiVoiceSynthOutcome.Error(
                engine = kind,
                messageAr = outcome.message,
                messageEn = outcome.message,
            )
            is VoiceRouteOutcome.Success -> {
                var buf = outcome.result.buffer
                buf = applyDeterministicVoiceAndEmotionDsp(
                    input = buf,
                    request = request,
                )
                writeStereoWavFile(outWavFile, buf)
                runCatching { outcome.result.audioFile.delete() }
                onProgress(1f, "Voice processing complete.")
                AiVoiceSynthOutcome.Success(
                    audioFilePath = outWavFile.absolutePath,
                    sampleRate = buf.sampleRate,
                    durationMs = buf.durationMs,
                    engineUsed = kind,
                    appliedPromptSummary = "VoxCPM2 Cloud • ${request.emotion.titleAr} ${(intensity * 100).toInt()}%",
                    isCloudGenerated = true,
                )
            }
        }
    }

    private suspend fun applyDeterministicVoiceAndEmotionDsp(
        input: StereoAudioBuffer,
        request: AiVoiceSynthRequest,
    ): StereoAudioBuffer {
        val roleShaped = transformVoiceOffline(
            source = input,
            pitchSemitones = 0f, // pitch already applied accurately at base speed/pitch stage
            formantShift = request.fixedFormantShift.coerceIn(0.84f, 1.20f),
            warmthDb = request.fixedEqLowDb,
            presenceDb = request.fixedEqPresenceDb,
            mix = 0.85f,
            studioMasteringPolish = true,
        )

        if (request.emotion == AiVoiceEmotion.WHISPER) {
            return applyDeterministicWhisperShaping(roleShaped, request.emotionIntensity)
        }

        val k = request.emotionIntensity.coerceIn(0.10f, 1.0f)
        return when (request.emotion) {
            AiVoiceEmotion.NEUTRAL -> roleShaped
            AiVoiceEmotion.EXCITED -> transformVoiceOffline(
                source = roleShaped,
                pitchSemitones = 0.30f * k,
                formantShift = 1.0f + 0.04f * k,
                warmthDb = 0.8f * k,
                presenceDb = 3.2f * k,
                mix = 0.72f,
                studioMasteringPolish = true,
            )
            AiVoiceEmotion.CALM -> transformVoiceOffline(
                source = roleShaped,
                pitchSemitones = -0.20f * k,
                formantShift = 1.0f - 0.02f * k,
                warmthDb = 3.0f * k,
                presenceDb = -1.2f * k,
                mix = 0.68f,
                studioMasteringPolish = true,
            )
            AiVoiceEmotion.SAD -> transformVoiceOffline(
                source = roleShaped,
                pitchSemitones = -0.45f * k,
                formantShift = 1.0f - 0.03f * k,
                warmthDb = 2.6f * k,
                presenceDb = -1.8f * k,
                mix = 0.72f,
                vibratoHz = 4.2f,
                vibratoDepth = 0.08f * k,
                studioMasteringPolish = true,
            )
            AiVoiceEmotion.SERIOUS -> transformVoiceOffline(
                source = roleShaped,
                pitchSemitones = -0.30f * k,
                formantShift = 1.0f - 0.04f * k,
                warmthDb = 3.2f * k,
                presenceDb = 2.6f * k,
                mix = 0.75f,
                bassBoostDb = 2.0f * k,
                studioMasteringPolish = true,
            )
            AiVoiceEmotion.WHISPER -> roleShaped
        }
    }

    private fun mapToLocalDialect(lang: AiVoiceLanguage, arabicDialect: AiArabicDialect): TtsDialect {
        val list = com.example.model.TTS_DIALECTS
        return when (lang) {
            AiVoiceLanguage.EN -> list.firstOrNull { it.id == "en_us" } ?: list.first()
            AiVoiceLanguage.FA -> list.firstOrNull { it.id == "persian" } ?: list.first()
            AiVoiceLanguage.TR -> list.firstOrNull { it.id == "turkish" } ?: list.first()
            AiVoiceLanguage.AR -> list.firstOrNull { it.id == arabicDialect.id } ?: list.first()
        }
    }

    /**
     * 100% deterministic airy whisper filter (fixed LCG seed + high-pass filter scaling smoothly with intensity).
     */
    private fun applyDeterministicWhisperShaping(
        input: StereoAudioBuffer,
        intensity: Float,
    ): StereoAudioBuffer {
        val k = intensity.coerceIn(0.15f, 1.0f)
        val n = input.frameCount
        val outL = FloatArray(n)
        val outR = FloatArray(n)
        var prevL = 0f
        var prevR = 0f
        var rng = 0x1234567
        val breathGain = 0.015f + 0.025f * k
        val dryBlend = (1.0f - 0.65f * k).coerceIn(0.25f, 0.85f)
        val hpWeight = 0.55f + 0.20f * k
        for (i in 0 until n) {
            rng = rng * 1664525 + 1013904223
            val white = ((rng ushr 16) and 0x7FFF) / 32768f - 0.5f
            val hpL = (input.left[i] - 0.92f * prevL) * hpWeight
            val hpR = (input.right[i] - 0.92f * prevR) * hpWeight
            prevL = input.left[i]
            prevR = input.right[i]
            val env = kotlin.math.min(1f, kotlin.math.abs(hpL) * 6f)
            val breath = white * breathGain * env
            outL[i] = (hpL * (1f - dryBlend * 0.4f) + input.left[i] * dryBlend * 0.25f + breath).coerceIn(-0.95f, 0.95f)
            outR[i] = (hpR * (1f - dryBlend * 0.4f) + input.right[i] * dryBlend * 0.25f + breath).coerceIn(-0.95f, 0.95f)
        }
        return StereoAudioBuffer(outL, outR, input.sampleRate)
    }
}

internal suspend fun persistAudioBytesAsWav(
    context: Context,
    rawBytes: ByteArray,
    outWavFile: File,
    engineUsed: AiVoiceEngineKind,
    appliedPromptSummary: String,
): AiVoiceSynthOutcome = withContext(Dispatchers.IO) {
    if (!NetworkImmunityManager.isValidAudioPayload(rawBytes, "audio/wav")) {
        return@withContext AiVoiceSynthOutcome.Error(
            engine = engineUsed,
            messageAr = "استجاب الخادم ببيانات غير صوتية أو تالفة.",
            messageEn = "Server returned non-audio or corrupted payload.",
            technicalDetail = rawBytes.take(160).toByteArray().toString(Charsets.UTF_8),
        )
    }
    outWavFile.parentFile?.mkdirs()
    val head = String(rawBytes, 0, minOf(12, rawBytes.size), Charsets.US_ASCII)
    if (head.startsWith("RIFF") && head.contains("WAVE")) {
        FileOutputStream(outWavFile).use { it.write(rawBytes) }
        val buf = AiVoiceAudioStitcher.readSimplePcmWav(outWavFile)
        val probe = AudioDecoder.probeAudioHeaderQuick(outWavFile)
        val rate = buf?.sampleRate ?: probe?.sampleRate ?: TARGET_SAMPLE_RATE
        val durMs = buf?.durationMs ?: probe?.durationMs ?: 0L
        return@withContext AiVoiceSynthOutcome.Success(
            audioFilePath = outWavFile.absolutePath,
            sampleRate = rate,
            durationMs = durMs,
            engineUsed = engineUsed,
            appliedPromptSummary = appliedPromptSummary,
            isCloudGenerated = true,
        )
    }

    val tmpCompressed = File(outWavFile.parentFile, "tmp_resp_${UUID.randomUUID()}.bin")
    FileOutputStream(tmpCompressed).use { it.write(rawBytes) }
    try {
        when (val decoded = AudioDecoder.decodeFileDetailedWithProgress(tmpCompressed) {}) {
            is AudioDecodeOutcome.Success -> {
                writeStereoWavFile(outWavFile, decoded.buffer)
                AiVoiceSynthOutcome.Success(
                    audioFilePath = outWavFile.absolutePath,
                    sampleRate = decoded.buffer.sampleRate,
                    durationMs = decoded.buffer.durationMs,
                    engineUsed = engineUsed,
                    appliedPromptSummary = appliedPromptSummary,
                    isCloudGenerated = true,
                )
            }
            is AudioDecodeOutcome.Failure -> AiVoiceSynthOutcome.Error(
                engine = engineUsed,
                messageAr = "تعذر فك ترميز الملف الصوتي المستلم: ${decoded.reason}",
                messageEn = "Failed decoding returned audio: ${decoded.reason}",
            )
        }
    } finally {
        runCatching { tmpCompressed.delete() }
    }
}

/**
 * Central coordinator for online VoxCPM2 single-voice and dialogue synthesis.
 */
class AiVoiceStudioCoordinator(
    providers: List<AiVoiceStudioEngine> = listOf(
        VoxCpm2CloudProvider(),
    ),
) {
    private val registry: Map<AiVoiceEngineKind, AiVoiceStudioEngine> = providers.associateBy { it.kind }

    fun getProvider(kind: AiVoiceEngineKind): AiVoiceStudioEngine =
        registry[kind] ?: registry.values.first()

    suspend fun checkBackendHealth(
        context: Context,
        backendUrl: String,
        bearerToken: String = "",
    ): AiVoiceBackendHealth = withContext(Dispatchers.IO) {
        val reachable = try {
            getProvider(AiVoiceEngineKind.VOXCPM_2).checkHealth(context, backendUrl, bearerToken)
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            false
        }
        AiVoiceBackendHealth(
            isChecked = true,
            isChecking = false,
            serverReachable = reachable,
            voxCpm2Ready = reachable,
            voxcpm2PackageInstalled = false,
            voxcpm2Loaded = false,
            cudaAvailable = false,
            authRequired = false,
            voxcpm2LoadError = "",
            voxCpmPublicSpaceChecked = backendUrl.isBlank(),
            voxCpmPublicSpaceAvailable = reachable,
            statusDetail = if (reachable) "VoxCPM2 endpoint is reachable." else "VoxCPM2 endpoint is unavailable.",
            serverGpuInfo = if (backendUrl.isBlank()) "Public VoxCPM2 Space" else backendUrl,
        )
    }

    suspend fun synthesizeSingle(
        context: Context,
        request: AiVoiceSynthRequest,
        backendUrl: String,
        outputDir: File,
        bearerToken: String = "",
        onProgress: (Float, String) -> Unit = { _, _ -> },
    ): AiVoiceSynthOutcome = withContext(Dispatchers.IO) {
        val rawTrimmed = request.text.trim()
        if (rawTrimmed.isEmpty()) {
            return@withContext AiVoiceSynthOutcome.Error(
                engine = request.engine,
                messageAr = "يرجى كتابة النص أولاً.",
                messageEn = "Please enter script text first.",
            )
        }
        if (TtsTextLimits.countWords(rawTrimmed) > TtsTextLimits.MAX_WORDS) {
            return@withContext AiVoiceSynthOutcome.Error(
                engine = AiVoiceEngineKind.VOXCPM_2,
                messageAr = "الحد الأقصى للنص هو ${TtsTextLimits.MAX_WORDS} كلمة.",
                messageEn = "Text is limited to ${TtsTextLimits.MAX_WORDS} words.",
            )
        }
        val phoneticText = AiVoicePromptBuilder.normalizeTargetText(
            rawTrimmed,
            request.language,
            request.dialect,
        )
        val dialectAwareText = if (
            request.language == AiVoiceLanguage.AR &&
            request.dialect != AiArabicDialect.MSA
        ) {
            ArabicTextDiacritizer.normalizeAndHintForTts(phoneticText, applyLightDiacritics = true)
        } else {
            phoneticText
        }
        val clean = AiVoicePromptBuilder.normalizeTargetText(
            dialectAwareText,
            request.language,
            request.dialect,
        )
        if (clean.isBlank()) {
            return@withContext AiVoiceSynthOutcome.Error(
                engine = AiVoiceEngineKind.VOXCPM_2,
                messageAr = "يرجى إدخال نص منطوق صالح.",
                messageEn = "Please enter valid speech text.",
            )
        }

        val effectiveEngine = AiVoiceEngineKind.VOXCPM_2
        val effectiveRequest = request.copy(
            engine = effectiveEngine,
            text = clean,
            fixedSeed = request.fixedSeed,
        )

        outputDir.mkdirs()
        val cacheFile = try {
            stableSynthesisCacheFile(context, effectiveRequest, backendUrl)
        } catch (e: Exception) {
            Log.w("AiVoiceCache", "Could not compute deterministic synthesis cache key", e)
            null
        }
        val cachedOutcome = cacheFile?.let { readCachedSynthesis(it, outputDir, effectiveRequest) }
        if (cachedOutcome != null) {
            onProgress(1f, "Loaded the exact matching voice print from cache.")
            return@withContext cachedOutcome
        }

        val chunks = AiVoicePromptBuilder.splitTextIntoChunks(clean, maxCharsPerChunk = 260)
        val provider = getProvider(effectiveEngine)
        val tempFiles = mutableListOf<File>()
        val segments = mutableListOf<WavStitchSegment>()
        var lastPrompt = ""

        try {
            val chunkProgressSpan = 0.78f / chunks.size.coerceAtLeast(1)
            for ((idx, chunk) in chunks.withIndex()) {
                coroutineContext.ensureActive()
                val pct = 0.08f + (idx.toFloat() / chunks.size.toFloat()) * 0.78f
                onProgress(pct, "جاري التوليد عبر VoxCPM2 (${idx + 1}/${chunks.size})...")
                val chunkFile = File(outputDir, "aivoice_chunk_${UUID.randomUUID()}.wav")
                tempFiles.add(chunkFile)

                when (
                    val res = provider.synthesizeChunk(
                        context = context,
                        request = effectiveRequest,
                        textChunk = chunk,
                        backendUrl = backendUrl,
                        outWavFile = chunkFile,
                        bearerToken = bearerToken,
                    ) { fraction, label ->
                        onProgress(
                            (pct + fraction.coerceIn(0f, 1f) * chunkProgressSpan)
                                .coerceIn(pct, pct + chunkProgressSpan),
                            label,
                        )
                    }
                ) {
                    is AiVoiceSynthOutcome.ConfigurationRequired -> return@withContext AiVoiceSynthOutcome.Error(
                        engine = effectiveEngine,
                        messageAr = res.explanationAr,
                        messageEn = res.explanationEn,
                    )
                    is AiVoiceSynthOutcome.Error -> return@withContext res
                    is AiVoiceSynthOutcome.Success -> {
                        lastPrompt = res.appliedPromptSummary
                        segments.add(
                            WavStitchSegment(
                                File(res.audioFilePath),
                                pauseAfterMs = effectiveRequest.breathCadence.pauseAfterMs,
                            ),
                        )
                    }
                }
            }

            onProgress(0.92f, "جاري الدمج ومعالجة FFmpegKit الاحترافية...")
            val finalFile = File(outputDir, "sonix_aivoice_${UUID.randomUUID()}.wav")
            val stitched = AiVoiceAudioStitcher.stitchWavSegments(
                segments = segments,
                outFile = finalFile,
                targetSampleRate = TARGET_SAMPLE_RATE,
                normalizeLoudness = effectiveRequest.normalizeAudio,
            )
            cacheFile?.let { persistStableSynthesisCache(it, stitched.outputFile) }
            onProgress(1f, "اكتمل التوليد بنجاح")
            AiVoiceSynthOutcome.Success(
                audioFilePath = stitched.outputFile.absolutePath,
                sampleRate = stitched.sampleRate,
                durationMs = stitched.durationMs,
                engineUsed = effectiveEngine,
                appliedPromptSummary = lastPrompt,
                isCloudGenerated = true,
            )
        } finally {
            tempFiles.forEach { runCatching { it.delete() } }
        }
    }

    private fun stableSynthesisCacheFile(
        context: Context,
        request: AiVoiceSynthRequest,
        backendUrl: String,
    ): File? {
        val referenceFingerprint = request.referenceAudioPath?.let { path ->
            val reference = File(path)
            if (!reference.isFile) return null
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(reference).use { input ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }.orEmpty()
        val userId = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull().orEmpty()
        val keyData = "voxcpm-output-v3|$userId|${backendUrl.trim().trimEnd('/')}|$request|$referenceFingerprint"
        val key = MessageDigest.getInstance("SHA-256")
            .digest(keyData.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return File(File(context.cacheDir, "ai_voice_deterministic_cache"), "$key.wav")
    }

    private fun readCachedSynthesis(
        cacheFile: File,
        outputDir: File,
        request: AiVoiceSynthRequest,
    ): AiVoiceSynthOutcome.Success? {
        if (!cacheFile.isFile) return null
        val cachedBuffer = try {
            AiVoiceAudioStitcher.readSimplePcmWav(cacheFile)
        } catch (e: Exception) {
            Log.w("AiVoiceCache", "Could not read cached voice output: ${cacheFile.name}", e)
            null
        }
        if (cachedBuffer == null) {
            if (!cacheFile.delete()) {
                Log.w("AiVoiceCache", "Could not remove invalid cached voice output: ${cacheFile.name}")
            }
            return null
        }
        val outputFile = File(outputDir, "sonix_aivoice_${UUID.randomUUID()}.wav")
        return try {
            cacheFile.copyTo(outputFile, overwrite = true)
            cacheFile.setLastModified(System.currentTimeMillis())
            AiVoiceSynthOutcome.Success(
                audioFilePath = outputFile.absolutePath,
                sampleRate = cachedBuffer.sampleRate,
                durationMs = cachedBuffer.durationMs,
                engineUsed = request.engine,
                appliedPromptSummary = "${request.vocalStyle.titleEn} • ${request.emotion.titleEn} • deterministic cache",
                isCloudGenerated = true,
            )
        } catch (e: Exception) {
            Log.w("AiVoiceCache", "Could not copy cached voice output; synthesizing again.", e)
            null
        }
    }

    private fun persistStableSynthesisCache(cacheFile: File, sourceFile: File) {
        try {
            cacheFile.parentFile?.mkdirs()
            sourceFile.copyTo(cacheFile, overwrite = true)
            cacheFile.setLastModified(System.currentTimeMillis())
            val cachedFiles = cacheFile.parentFile
                ?.listFiles { file -> file.isFile && file.extension.equals("wav", ignoreCase = true) }
                .orEmpty()
                .sortedBy { it.lastModified() }
            var totalBytes = cachedFiles.sumOf { it.length() }
            var remainingCount = cachedFiles.size
            for (file in cachedFiles) {
                if (remainingCount <= 24 && totalBytes <= 96L * 1024L * 1024L) break
                if (file.absolutePath == cacheFile.absolutePath) continue
                val length = file.length()
                if (file.delete()) {
                    totalBytes -= length
                    remainingCount--
                } else {
                    Log.w("AiVoiceCache", "Could not prune old cached voice output: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.w("AiVoiceCache", "Could not persist deterministic voice output cache", e)
        }
    }

    suspend fun synthesizeMultiCharacterDialogue(
        context: Context,
        turns: List<AiDialogueTurn>,
        characters: List<AiVoiceCharacter>,
        selectedEngineOverride: AiVoiceEngineKind?,
        backendUrl: String,
        outputDir: File,
        bearerToken: String = "",
        onProgress: (Float, String) -> Unit = { _, _ -> },
    ): AiVoiceSynthOutcome = withContext(Dispatchers.IO) {
        val activeTurns = turns.filter { it.text.trim().isNotEmpty() }
        if (activeTurns.isEmpty()) {
            return@withContext AiVoiceSynthOutcome.Error(
                engine = selectedEngineOverride ?: AiVoiceEngineKind.VOXCPM_2,
                messageAr = "يرجى إضافة جملة واحدة على الأقل داخل الحوار.",
                messageEn = "Please add at least one non-empty dialogue turn.",
            )
        }
        val dialogueWordCount = TtsTextLimits.countWords(activeTurns.joinToString(" ") { it.text })
        if (dialogueWordCount > TtsTextLimits.MAX_WORDS) {
            return@withContext AiVoiceSynthOutcome.Error(
                engine = selectedEngineOverride ?: AiVoiceEngineKind.VOXCPM_2,
                messageAr = "الحد الأقصى للحوار هو ${TtsTextLimits.MAX_WORDS} كلمة.",
                messageEn = "Dialogue is limited to ${TtsTextLimits.MAX_WORDS} words.",
            )
        }

        val tempTurnFiles = mutableListOf<File>()
        val segments = mutableListOf<WavStitchSegment>()
        var primaryEngine = AiVoiceEngineKind.VOXCPM_2
        var anyCloud = false

        try {
            for ((idx, turn) in activeTurns.withIndex()) {
                coroutineContext.ensureActive()
                val character = characters.firstOrNull { it.id == turn.characterId } ?: characters.first()
                val engine = selectedEngineOverride ?: character.preferredEngine
                primaryEngine = engine
                val packagedReference = aiVoiceCloudPackReferenceFile(context, character.id)
                    .takeIf { it.isFile }?.absolutePath
                val referencePath = character.referenceAudioPath ?: packagedReference
                onProgress(
                    0.05f + (idx.toFloat() / activeTurns.size.toFloat()) * 0.80f,
                    "توليد جملة ${idx + 1}/${activeTurns.size} (${character.nameAr})...",
                )

                val turnReq = AiVoiceSynthRequest(
                    engine = engine,
                    text = turn.text.trim(),
                    language = character.language,
                    dialect = character.dialect,
                    category = character.category,
                    voiceDescriptionPrompt = character.voiceDescriptionPrompt,
                    emotion = turn.emotion,
                    emotionIntensity = turn.emotionIntensity,
                    pace = turn.pace,
                    speechRate = turn.pace.speedMultiplier,
                    pitchOffsetSemitones = character.pitchOffsetSemitones,
                    fixedSid = character.fixedSid,
                    fixedSeed = character.fixedSeed,
                    fixedFormantShift = character.fixedFormantShift,
                    fixedEqLowDb = character.fixedEqLowDb,
                    fixedEqPresenceDb = character.fixedEqPresenceDb,
                    vocalStyle = character.defaultVocalStyle,
                    breathCadence = character.defaultBreathCadence,
                    pitchContour = character.defaultPitchContour,
                    timbre = character.defaultTimbre,
                    articulationClarity = character.defaultArticulationClarity,
                    referenceAudioPath = referencePath,
                    referenceTranscript = character.referenceTranscript,
                    referenceAudioOptional = character.referenceAudioPath.isNullOrBlank() &&
                        packagedReference != null,
                )

                val turnWav = File(outputDir, "dialogue_turn_${idx}_${UUID.randomUUID()}.wav")
                tempTurnFiles.add(turnWav)

                when (val outcome = synthesizeSingle(context, turnReq, backendUrl, outputDir, bearerToken)) {
                    is AiVoiceSynthOutcome.ConfigurationRequired -> return@withContext outcome
                    is AiVoiceSynthOutcome.Error -> return@withContext outcome
                    is AiVoiceSynthOutcome.Success -> {
                        val produced = File(outcome.audioFilePath)
                        tempTurnFiles.add(produced)
                        anyCloud = anyCloud || outcome.isCloudGenerated
                        segments.add(WavStitchSegment(produced, pauseAfterMs = turn.pauseAfterMs))
                    }
                }
            }

            onProgress(0.92f, "جاري دمج الحوار في ملف صوتي واحد متصل...")
            val finalDialogueWav = File(outputDir, "sonix_dialogue_${UUID.randomUUID()}.wav")
            val stitched = AiVoiceAudioStitcher.stitchWavSegments(
                segments = segments,
                outFile = finalDialogueWav,
                targetSampleRate = TARGET_SAMPLE_RATE,
            )
            onProgress(1f, "تم إنتاج الحوار المتكامل بنجاح")
            AiVoiceSynthOutcome.Success(
                audioFilePath = stitched.outputFile.absolutePath,
                sampleRate = stitched.sampleRate,
                durationMs = stitched.durationMs,
                engineUsed = primaryEngine,
                appliedPromptSummary = "حوار متعدد الشخصيات (${activeTurns.size} جمل)",
                isCloudGenerated = anyCloud,
            )
        } finally {
            tempTurnFiles.forEach { runCatching { it.delete() } }
        }
    }
}
