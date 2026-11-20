package com.example.data

import android.content.Context
import com.example.audio.readStereoWavFile
import com.example.audio.writeStereoWavFile
import com.example.data.local.AppDatabase
import com.example.data.local.CustomPresetEntity
import com.example.data.local.LocalAccountEntity
import com.example.data.local.PreferencesStore
import com.example.data.local.ProjectEntity
import com.example.data.local.RecordingEntity
import com.example.audio.ai.OnlineSoundHistoryItem
import com.example.audio.ai.OnlineSoundItem
import com.example.model.AppLanguage
import com.example.model.AuthSession
import com.example.model.CloudDraft
import com.example.model.DspConfig
import com.example.model.RecordingSourceType
import com.example.model.SavedAssistantChatSession
import com.example.model.StereoAudioBuffer
import com.example.model.StoredCustomPreset
import com.example.model.StudioPreset
import com.example.model.StudioProject
import com.example.model.AudioRecordingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.util.UUID

sealed interface AuthResult {
    data class Success(val session: AuthSession) : AuthResult
    data class Error(val message: String) : AuthResult
}

class StudioRepository(private val context: Context) {
    private val dao = AppDatabase.getInstance(context).studioDao()
    private val prefs = PreferencesStore(context)
    private val audioDir = File(context.filesDir, "studio_audio").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val languageFlow: Flow<AppLanguage> = prefs.languageFlow
    val autoLoopFlow: Flow<Boolean> = prefs.autoLoopFlow
    val authSessionFlow: Flow<AuthSession> = prefs.authSessionFlow
    val defaultSaveFolderFlow: Flow<Pair<String?, String?>> = prefs.defaultSaveFolderFlow
    val savedAssistantChatsFlow: Flow<List<SavedAssistantChatSession>> = prefs.savedAssistantChatsFlow
    val favoriteOnlineSoundsFlow: Flow<List<OnlineSoundItem>> = prefs.favoriteOnlineSoundsFlow
    val onlineListeningHistoryFlow: Flow<List<OnlineSoundHistoryItem>> = prefs.onlineListeningHistoryFlow
    val savedAiVoiceCharactersFlow: Flow<List<com.example.audio.aivoice.AiVoiceCharacter>> = prefs.savedAiVoiceCharactersFlow
    val favoriteAiVoiceCharacterIdsFlow: Flow<Set<String>> = prefs.favoriteAiVoiceCharacterIdsFlow
    val aiVoiceBackendUrlFlow: Flow<String> = prefs.aiVoiceBackendUrlFlow
    val aiVoiceBackendBearerFlow: Flow<String> = prefs.aiVoiceBackendBearerFlow

    val projectsFlow: Flow<List<StudioProject>> = dao.observeProjects().map { list ->
        list.map { entity ->
            val cfg = runCatching {
                json.decodeFromString<DspConfig>(entity.configJson).normalized()
            }.getOrDefault(DspConfig())
            StudioProject(
                id = entity.id,
                name = entity.name,
                durationMs = entity.durationMs,
                sampleRate = entity.sampleRate,
                config = cfg,
                updatedAt = entity.updatedAt,
                hasAudio = entity.audioFileName != null && File(audioDir, entity.audioFileName).exists(),
            )
        }
    }

    val recordingsFlow: Flow<List<AudioRecordingItem>> = dao.observeRecordings().map { list ->
        list.map { entity ->
            val source = runCatching { RecordingSourceType.valueOf(entity.sourceType) }
                .getOrDefault(RecordingSourceType.RECORDING)
            AudioRecordingItem(
                id = entity.id,
                title = entity.title,
                timestamp = entity.timestamp,
                durationMs = entity.durationMs,
                sampleRate = entity.sampleRate,
                sourceType = source,
            )
        }
    }

    val customPresetsFlow: Flow<List<StudioPreset>> = kotlinx.coroutines.flow.combine(
        prefs.customPresetsJsonFlow,
        dao.observeCustomPresets(),
    ) { jsonList, dbList ->
        val fromPrefs = jsonList.map { item ->
            StudioPreset(
                id = item.id,
                name = item.name,
                description = item.description,
                isBuiltIn = false,
                config = item.config.normalized(),
            )
        }

        val prefIds = fromPrefs.map { it.id }.toSet()
        val fromDb = dbList.filter { it.id !in prefIds }.map { entity ->
            val cfg = runCatching {
                json.decodeFromString<DspConfig>(entity.configJson).normalized()
            }.getOrDefault(DspConfig())
            StudioPreset(
                id = entity.id,
                name = entity.name,
                description = entity.description,
                isBuiltIn = false,
                config = cfg,
            )
        }
        fromPrefs + fromDb
    }

    fun localCloudDraftsFlow(userId: String): Flow<List<CloudDraft>> =
        prefs.localCloudDraftsFlow(userId)

    suspend fun saveLocalCloudDraft(userId: String, draft: CloudDraft) =
        prefs.saveLocalCloudDraft(userId, draft)

    suspend fun removeLocalCloudDraft(userId: String, draftId: String) =
        prefs.removeLocalCloudDraft(userId, draftId)

    suspend fun saveLanguage(lang: AppLanguage) = prefs.saveLanguage(lang)
    suspend fun saveAutoLoop(enabled: Boolean) = prefs.saveAutoLoop(enabled)
    suspend fun saveAuthSession(session: AuthSession) = prefs.saveAuthSession(session)
    suspend fun saveDefaultSaveFolder(uriString: String, folderName: String) =
        prefs.saveDefaultSaveFolder(uriString, folderName)
    suspend fun clearDefaultSaveFolder() = prefs.clearDefaultSaveFolder()
    suspend fun saveAiVoiceCharacters(characters: List<com.example.audio.aivoice.AiVoiceCharacter>) =
        prefs.saveAiVoiceCharacters(characters)
    suspend fun saveFavoriteAiVoiceCharacterIds(ids: Set<String>) =
        prefs.saveFavoriteAiVoiceCharacterIds(ids)
    suspend fun saveAiVoiceBackendUrl(url: String) = prefs.saveAiVoiceBackendUrl(url)
    suspend fun saveAiVoiceBackendBearer(token: String) = prefs.saveAiVoiceBackendBearer(token)

    suspend fun upsertAssistantChatSession(session: SavedAssistantChatSession) {
        val current = prefs.savedAssistantChatsFlow.first()
        val updated = (listOf(session) + current.filterNot { it.id == session.id }).take(40)
        prefs.saveAssistantChats(updated)
    }

    suspend fun deleteAssistantChatSession(sessionId: String) {
        val current = prefs.savedAssistantChatsFlow.first()
        prefs.saveAssistantChats(current.filterNot { it.id == sessionId })
    }

    suspend fun toggleFavoriteOnlineSound(item: OnlineSoundItem): Boolean {
        val current = prefs.favoriteOnlineSoundsFlow.first()
        val exists = current.any { it.identifier == item.identifier }
        val updated = if (exists) {
            current.filterNot { it.identifier == item.identifier }
        } else {
            (listOf(item) + current).take(80)
        }
        prefs.saveFavoriteOnlineSounds(updated)
        return !exists
    }

    suspend fun recordOnlineSoundListen(item: OnlineSoundItem) {
        val current = prefs.onlineListeningHistoryFlow.first()
        val entry = OnlineSoundHistoryItem(sound = item, listenedAtMs = System.currentTimeMillis())
        val updated = (listOf(entry) + current.filterNot { it.sound.identifier == item.identifier }).take(60)
        prefs.saveOnlineListeningHistory(updated)
    }

    suspend fun clearOnlineListeningHistory() {
        prefs.saveOnlineListeningHistory(emptyList())
    }

    suspend fun signInLocalEmail(
        email: String,
        password: String,
        lang: AppLanguage = AppLanguage.AR,
    ): AuthResult {
        val isEn = lang == AppLanguage.EN
        val cleanEmail = email.trim().lowercase()
        if (!Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(cleanEmail)) {
            return AuthResult.Error(
                if (isEn) "Please enter a valid email address."
                else "يرجى إدخال بريد إلكتروني صحيح.",
            )
        }
        if (password.length < 6) {
            return AuthResult.Error(
                if (isEn) "Password must be at least 6 characters."
                else "يرجى إدخال كلمة المرور (6 أحرف فأكثر).",
            )
        }
        return try {
            val account = dao.findAccountByEmail(cleanEmail)
                ?: return AuthResult.Error(
                    if (isEn) "No account found for this email address."
                    else "لا يوجد حساب مسجل بهذا البريد الإلكتروني.",
                )
            if (account.passwordHash != sha256(password)) {
                return AuthResult.Error(
                    if (isEn) "Incorrect email or password."
                    else "البريد الإلكتروني أو كلمة المرور غير صحيحة.",
                )
            }
            val session = AuthSession.SignedIn(
                uid = account.uid,
                displayName = account.displayName,
                email = account.email,
            )
            prefs.saveAuthSession(session)
            AuthResult.Success(session)
        } catch (e: Exception) {
            AuthResult.Error(
                e.localizedMessage
                    ?: if (isEn) "Unable to sign in right now." else "تعذر إتمام تسجيل الدخول حالياً.",
            )
        }
    }

    suspend fun registerLocalEmail(
        displayName: String,
        email: String,
        password: String,
        lang: AppLanguage = AppLanguage.AR,
    ): AuthResult {
        val isEn = lang == AppLanguage.EN
        val cleanEmail = email.trim().lowercase()
        val cleanName = displayName.trim()
            .ifEmpty { cleanEmail.substringBefore('@') }
            .ifEmpty { if (isEn) "Sonix ✦ User" else "مستخدم Sonix ✦" }
        if (!Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(cleanEmail)) {
            return AuthResult.Error(
                if (isEn) "Please enter a valid email address."
                else "يرجى إدخال بريد إلكتروني صحيح.",
            )
        }
        if (password.length < 6) {
            return AuthResult.Error(
                if (isEn) "Password must be at least 6 characters."
                else "كلمة المرور يجب أن تكون 6 أحرف على الأقل.",
            )
        }
        return try {
            if (dao.findAccountByEmail(cleanEmail) != null) {
                return AuthResult.Error(
                    if (isEn) "This email is already registered to another account."
                    else "هذا البريد الإلكتروني مستخدم بالفعل بحساب آخر.",
                )
            }
            val newAccount = LocalAccountEntity(
                uid = "local_${System.currentTimeMillis()}",
                displayName = cleanName,
                email = cleanEmail,
                passwordHash = sha256(password),
            )
            dao.insertAccount(newAccount)
            val session = AuthSession.SignedIn(
                uid = newAccount.uid,
                displayName = newAccount.displayName,
                email = newAccount.email,
            )
            prefs.saveAuthSession(session)
            AuthResult.Success(session)
        } catch (e: Exception) {
            AuthResult.Error(
                e.localizedMessage
                    ?: if (isEn) "Unable to create account right now." else "تعذر إنشاء الحساب حالياً.",
            )
        }
    }

    suspend fun deleteLocalAccount(uid: String): Boolean =
        dao.deleteAccountByUid(uid) > 0

    suspend fun saveProject(
        existingId: Long,
        name: String,
        buffer: StereoAudioBuffer?,
        config: DspConfig,
    ): StudioProject = withContext(Dispatchers.IO) {
        val cleanName = name.trim().ifEmpty { "مشروع صوتي" }
        val existing = if (existingId > 0L) dao.getProjectById(existingId) else null
        val fileName = if (buffer != null) {
            existing?.audioFileName ?: "proj_${UUID.randomUUID()}.wav"
        } else {
            existing?.audioFileName?.let { File(audioDir, it).delete() }
            null
        }

        if (buffer != null && fileName != null) {
            writeStereoWavFile(File(audioDir, fileName), buffer)
        }

        val now = System.currentTimeMillis()
        val durationMs = buffer?.durationMs ?: 0L
        val sampleRate = buffer?.sampleRate ?: 44100
        val normConfig = config.normalized()
        val cfgJson = json.encodeToString(DspConfig.serializer(), normConfig)

        val entity = ProjectEntity(
            id = if (existingId > 0L) existingId else 0L,
            name = cleanName,
            durationMs = durationMs,
            sampleRate = sampleRate,
            configJson = cfgJson,
            updatedAt = now,
            audioFileName = fileName,
        )
        val id = dao.upsertProject(entity)
        StudioProject(
            id = id,
            name = cleanName,
            durationMs = durationMs,
            sampleRate = sampleRate,
            config = normConfig,
            updatedAt = now,
            hasAudio = buffer != null,
        )
    }

    suspend fun loadProjectAudioBuffer(projectId: Long): StereoAudioBuffer? = withContext(Dispatchers.IO) {
        val proj = dao.getProjectById(projectId) ?: return@withContext null
        val fileName = proj.audioFileName ?: return@withContext null
        readStereoWavFile(File(audioDir, fileName))
    }

    suspend fun deleteProject(projectId: Long) = withContext(Dispatchers.IO) {
        val proj = dao.getProjectById(projectId)
        proj?.audioFileName?.let { File(audioDir, it).delete() }
        dao.deleteProjectById(projectId)
    }

    suspend fun indexAudioRecording(
        title: String,
        buffer: StereoAudioBuffer,
        sourceType: RecordingSourceType,
    ): AudioRecordingItem = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim().ifEmpty {
            if (sourceType == RecordingSourceType.IMPORTED) "ملف مستورد" else "تسجيل صوتي"
        }
        val fileName = "rec_${UUID.randomUUID()}.wav"
        writeStereoWavFile(File(audioDir, fileName), buffer)

        val now = System.currentTimeMillis()
        val durationMs = buffer.durationMs
        val entity = RecordingEntity(
            title = cleanTitle,
            timestamp = now,
            durationMs = durationMs,
            sampleRate = buffer.sampleRate,
            sourceType = sourceType.name,
            audioFileName = fileName,
        )
        val id = dao.insertRecording(entity)
        AudioRecordingItem(
            id = id,
            title = cleanTitle,
            timestamp = now,
            durationMs = durationMs,
            sampleRate = buffer.sampleRate,
            sourceType = sourceType,
        )
    }

    suspend fun loadRecordingBuffer(id: Long): StereoAudioBuffer? = withContext(Dispatchers.IO) {
        val rec = dao.getRecordingById(id) ?: return@withContext null
        readStereoWavFile(File(audioDir, rec.audioFileName))
    }

    suspend fun getRecordingFile(id: Long): File? = withContext(Dispatchers.IO) {
        val rec = dao.getRecordingById(id) ?: return@withContext null
        val file = File(audioDir, rec.audioFileName)
        if (file.exists() && file.length() > 0L) file else null
    }

    suspend fun deleteRecording(id: Long) = withContext(Dispatchers.IO) {
        val rec = dao.getRecordingById(id)
        rec?.audioFileName?.let { File(audioDir, it).delete() }
        dao.deleteRecordingById(id)
    }

    suspend fun saveCustomPreset(
        name: String,
        description: String,
        config: DspConfig,
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val cleanDesc = description.trim().ifEmpty { "قالب صوتي مخصص" }
        val normalizedConfig = config.normalized()
        val cfgJson = json.encodeToString(DspConfig.serializer(), normalizedConfig)
        val now = System.currentTimeMillis()
        val insertedId = dao.insertCustomPreset(
            CustomPresetEntity(
                name = cleanName,
                description = cleanDesc,
                configJson = cfgJson,
                createdAt = now,
            ),
        )
        val existingList = prefs.customPresetsJsonFlow.first()
        val updated = listOf(
            StoredCustomPreset(
                id = insertedId,
                name = cleanName,
                description = cleanDesc,
                config = normalizedConfig,
                createdAt = now,
            ),
        ) + existingList.filterNot { it.id == insertedId }
        prefs.saveCustomPresetsJson(updated)
    }

    suspend fun deleteCustomPreset(id: Long) {
        dao.deleteCustomPresetById(id)
        val existingList = prefs.customPresetsJsonFlow.first()
        val updated = existingList.filterNot { it.id == id }
        if (updated.size != existingList.size) {
            prefs.saveCustomPresetsJson(updated)
        }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
