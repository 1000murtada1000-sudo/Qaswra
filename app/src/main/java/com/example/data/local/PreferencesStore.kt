package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.audio.ai.OnlineSoundHistoryItem
import com.example.audio.ai.OnlineSoundItem
import com.example.audio.aivoice.AiVoiceCharacter
import com.example.model.AppLanguage
import com.example.model.AuthSession
import com.example.model.SavedAssistantChatSession
import com.example.model.StoredCustomPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.studioPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sonix_studio_prefs",
)

class PreferencesStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val presetListSerializer = ListSerializer(StoredCustomPreset.serializer())
    private val chatSessionListSerializer = ListSerializer(SavedAssistantChatSession.serializer())
    private val soundItemListSerializer = ListSerializer(OnlineSoundItem.serializer())
    private val soundHistoryListSerializer = ListSerializer(OnlineSoundHistoryItem.serializer())
    private val aiVoiceCharacterListSerializer = ListSerializer(AiVoiceCharacter.serializer())
    private val aiVoiceFavoriteIdsSerializer = SetSerializer(String.serializer())

    companion object {
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_AUTO_LOOP = booleanPreferencesKey("auto_loop")
        private val KEY_AUTH_SESSION = stringPreferencesKey("auth_session")
        private val KEY_CUSTOM_PRESETS_JSON = stringPreferencesKey("custom_presets_json")
        private val KEY_DEFAULT_SAVE_TREE_URI = stringPreferencesKey("default_save_tree_uri")
        private val KEY_DEFAULT_SAVE_FOLDER_NAME = stringPreferencesKey("default_save_folder_name")
        private val KEY_ASSISTANT_CHATS_JSON = stringPreferencesKey("assistant_chats_json")
        private val KEY_FAVORITE_ONLINE_SOUNDS_JSON = stringPreferencesKey("favorite_online_sounds_json")
        private val KEY_ONLINE_SOUND_HISTORY_JSON = stringPreferencesKey("online_sound_history_json")
        private val KEY_AI_VOICE_CHARACTERS_JSON = stringPreferencesKey("ai_voice_characters_json")
        private val KEY_AI_VOICE_FAVORITE_IDS_JSON = stringPreferencesKey("ai_voice_favorite_ids_json")
        private val KEY_AI_VOICE_BACKEND_URL = stringPreferencesKey("ai_voice_backend_url")
        private val KEY_AI_VOICE_BACKEND_BEARER = stringPreferencesKey("ai_voice_backend_bearer")
    }

    val languageFlow: Flow<AppLanguage> = context.studioPrefsDataStore.data.map { prefs ->
        AppLanguage.fromTag(prefs[KEY_LANGUAGE])
    }

    val autoLoopFlow: Flow<Boolean> = context.studioPrefsDataStore.data.map { prefs ->
        prefs[KEY_AUTO_LOOP] ?: false
    }

    val authSessionFlow: Flow<AuthSession> = context.studioPrefsDataStore.data.map { prefs ->
        val raw = prefs[KEY_AUTH_SESSION] ?: return@map AuthSession.SignedOut
        runCatching { json.decodeFromString<AuthSession>(raw) }.getOrDefault(AuthSession.SignedOut)
    }

    val customPresetsJsonFlow: Flow<List<StoredCustomPreset>> = context.studioPrefsDataStore.data.map { prefs ->
        val raw = prefs[KEY_CUSTOM_PRESETS_JSON] ?: return@map emptyList()
        runCatching { json.decodeFromString(presetListSerializer, raw) }.getOrDefault(emptyList())
    }

    val defaultSaveFolderFlow: Flow<Pair<String?, String?>> = context.studioPrefsDataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_SAVE_TREE_URI] to prefs[KEY_DEFAULT_SAVE_FOLDER_NAME]
    }

    val savedAssistantChatsFlow: Flow<List<SavedAssistantChatSession>> = context.studioPrefsDataStore.data.map { prefs ->
        val raw = prefs[KEY_ASSISTANT_CHATS_JSON] ?: return@map emptyList()
        runCatching { json.decodeFromString(chatSessionListSerializer, raw) }.getOrDefault(emptyList())
    }

    val favoriteOnlineSoundsFlow: Flow<List<OnlineSoundItem>> = context.studioPrefsDataStore.data.map { prefs ->
        val raw = prefs[KEY_FAVORITE_ONLINE_SOUNDS_JSON] ?: return@map emptyList()
        runCatching { json.decodeFromString(soundItemListSerializer, raw) }.getOrDefault(emptyList())
    }

    val onlineListeningHistoryFlow: Flow<List<OnlineSoundHistoryItem>> = context.studioPrefsDataStore.data.map { prefs ->
        val raw = prefs[KEY_ONLINE_SOUND_HISTORY_JSON] ?: return@map emptyList()
        runCatching { json.decodeFromString(soundHistoryListSerializer, raw) }.getOrDefault(emptyList())
    }

    val savedAiVoiceCharactersFlow: Flow<List<AiVoiceCharacter>> = context.studioPrefsDataStore.data.map { prefs ->
        val raw = prefs[KEY_AI_VOICE_CHARACTERS_JSON] ?: return@map emptyList()
        val migrated = raw
            .replace("\"AUDAR_TTS_V1_FLASH\"", "\"VOXCPM_2\"")
            .replace("\"AUDAR_TTS_V1_TURBO\"", "\"VOXCPM_2\"")
            .replace("\"EDGE_NEURAL_FREE\"", "\"VOXCPM_2\"")
            .replace("\"SONIX_LOCAL_STUDIO\"", "\"VOXCPM_2\"")
            .replace("\"ELDERLY\"", "\"MALE\"")
            .replace("\"NARRATOR\"", "\"MALE\"")
            .replace("\"CARTOON\"", "\"MALE\"")
            .replace("\"CUSTOM_DESIGN\"", "\"MALE\"")
            .replace("\"FR\"", "\"EN\"")
            .replace("\"ES\"", "\"EN\"")
            .replace("\"DE\"", "\"EN\"")
            .replace("\"ZH\"", "\"EN\"")
            .replace("\"JA\"", "\"EN\"")
            .replace("\"KO\"", "\"EN\"")
            .replace("\"RU\"", "\"EN\"")
            .replace("\"IT\"", "\"EN\"")
            .replace("\"PT\"", "\"EN\"")
            .replace("\"HI\"", "\"EN\"")
            .replace("\"UR\"", "\"FA\"")
            .replace("\"ID\"", "\"EN\"")
            .replace("\"ANGER\"", "\"SERIOUS\"")
            .replace("\"CRYING\"", "\"SAD\"")
            .replace("\"JOY\"", "\"EXCITED\"")
            .replace("\"LAUGH\"", "\"EXCITED\"")
            .replace("\"SURPRISED\"", "\"EXCITED\"")
            .replace("\"FEAR\"", "\"CALM\"")
        runCatching { json.decodeFromString(aiVoiceCharacterListSerializer, migrated) }.getOrDefault(emptyList())
    }

    val favoriteAiVoiceCharacterIdsFlow: Flow<Set<String>> = context.studioPrefsDataStore.data.map { prefs ->
        val raw = prefs[KEY_AI_VOICE_FAVORITE_IDS_JSON] ?: return@map emptySet()
        runCatching { json.decodeFromString(aiVoiceFavoriteIdsSerializer, raw) }.getOrDefault(emptySet())
    }

    val aiVoiceBackendUrlFlow: Flow<String> = context.studioPrefsDataStore.data.map { prefs ->
        prefs[KEY_AI_VOICE_BACKEND_URL].orEmpty()
    }

    val aiVoiceBackendBearerFlow: Flow<String> = context.studioPrefsDataStore.data.map { prefs ->
        prefs[KEY_AI_VOICE_BACKEND_BEARER].orEmpty()
    }

    suspend fun saveLanguage(lang: AppLanguage) {
        context.studioPrefsDataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = lang.tag
        }
    }

    suspend fun saveAutoLoop(enabled: Boolean) {
        context.studioPrefsDataStore.edit { prefs ->
            prefs[KEY_AUTO_LOOP] = enabled
        }
    }

    suspend fun saveAuthSession(session: AuthSession) {
        context.studioPrefsDataStore.edit { prefs ->
            prefs[KEY_AUTH_SESSION] = json.encodeToString(AuthSession.serializer(), session)
        }
    }

    suspend fun saveCustomPresetsJson(presets: List<StoredCustomPreset>) {
        context.studioPrefsDataStore.edit { prefs ->
            prefs[KEY_CUSTOM_PRESETS_JSON] = json.encodeToString(presetListSerializer, presets)
        }
    }

    suspend fun saveDefaultSaveFolder(uriString: String, folderName: String) {
        context.studioPrefsDataStore.edit { prefs ->
            prefs[KEY_DEFAULT_SAVE_TREE_URI] = uriString
            prefs[KEY_DEFAULT_SAVE_FOLDER_NAME] = folderName
        }
    }

    suspend fun clearDefaultSaveFolder() {
        context.studioPrefsDataStore.edit { prefs ->
            prefs.remove(KEY_DEFAULT_SAVE_TREE_URI)
            prefs.remove(KEY_DEFAULT_SAVE_FOLDER_NAME)
        }
    }

    suspend fun saveAssistantChats(sessions: List<SavedAssistantChatSession>) {
        context.studioPrefsDataStore.edit { prefs ->
            prefs[KEY_ASSISTANT_CHATS_JSON] = json.encodeToString(chatSessionListSerializer, sessions)
        }
    }

    suspend fun saveFavoriteOnlineSounds(sounds: List<OnlineSoundItem>) {
        context.studioPrefsDataStore.edit { prefs ->
            prefs[KEY_FAVORITE_ONLINE_SOUNDS_JSON] = json.encodeToString(soundItemListSerializer, sounds)
        }
    }

    suspend fun saveOnlineListeningHistory(history: List<OnlineSoundHistoryItem>) {
        context.studioPrefsDataStore.edit { prefs ->
            prefs[KEY_ONLINE_SOUND_HISTORY_JSON] = json.encodeToString(soundHistoryListSerializer, history)
        }
    }

    suspend fun saveAiVoiceCharacters(characters: List<AiVoiceCharacter>) {
        context.studioPrefsDataStore.edit { prefs ->
            prefs[KEY_AI_VOICE_CHARACTERS_JSON] = json.encodeToString(aiVoiceCharacterListSerializer, characters)
        }
    }

    suspend fun saveFavoriteAiVoiceCharacterIds(ids: Set<String>) {
        context.studioPrefsDataStore.edit { prefs ->
            prefs[KEY_AI_VOICE_FAVORITE_IDS_JSON] = json.encodeToString(aiVoiceFavoriteIdsSerializer, ids)
        }
    }

    suspend fun saveAiVoiceBackendUrl(url: String) {
        context.studioPrefsDataStore.edit { prefs ->
            prefs[KEY_AI_VOICE_BACKEND_URL] = url.trim()
        }
    }

    suspend fun saveAiVoiceBackendBearer(token: String) {
        context.studioPrefsDataStore.edit { prefs ->
            prefs[KEY_AI_VOICE_BACKEND_BEARER] = token.trim()
        }
    }
}
