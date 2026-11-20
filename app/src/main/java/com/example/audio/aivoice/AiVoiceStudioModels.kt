package com.example.audio.aivoice

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

const val FIXED_DETERMINISTIC_SEED: Int = 42

@Serializable
enum class AiVoiceEngineKind(
    val id: String,
    val modelRepoId: String,
    val titleAr: String,
    val titleEn: String,
    val shortTitleAr: String,
    val shortTitleEn: String,
    val badgeAr: String,
    val badgeEn: String,
    val requiresRemoteGpu: Boolean,
) {
    VOXCPM_2(
        id = "voxcpm2",
        modelRepoId = "openbmb/VoxCPM2",
        titleAr = "VoxCPM2 العصبي عبر الإنترنت",
        titleEn = "VoxCPM2 Neural Cloud",
        shortTitleAr = "VoxCPM2",
        shortTitleEn = "VoxCPM2",
        badgeAr = "توليد سحابي • يتطلب الإنترنت",
        badgeEn = "Cloud synthesis • Internet required",
        requiresRemoteGpu = true,
    ),
    ;

    fun supportedEmotions(): List<AiVoiceEmotion> = AiVoiceEmotion.entries

    fun supportsVoiceDesignPrompt(): Boolean = true

    fun supportsEmotionIntensity(): Boolean = true

    fun supportsPitchShiftControl(): Boolean = true

    fun supportsVoiceCloning(): Boolean = true

    fun supportsLanguage(language: AiVoiceLanguage): Boolean = true

    fun supportedLanguages(): List<AiVoiceLanguage> =
        AiVoiceLanguage.entries.filter { supportsLanguage(it) }

    val isUserSelectable: Boolean
        get() = true
}

@Serializable
enum class AiVoiceLanguage(
    val code: String,
    val titleAr: String,
    val titleEn: String,
    val nativeName: String,
    val voxCpmSupported: Boolean = true,
) {
    AR("ar", "العربية", "Arabic", "العربية", voxCpmSupported = true),
    EN("en", "الإنجليزية", "English", "English", voxCpmSupported = true),
    TR("tr", "التركية", "Turkish", "Türkçe", voxCpmSupported = true),
    FA("fa", "الفارسية", "Persian", "فارسی", voxCpmSupported = true),
}

@Serializable
enum class AiEnglishAccent(
    val id: String,
    val titleAr: String,
    val titleEn: String,
) {
    AMERICAN("american", "أمريكية عادية", "American / Standard"),
    BRITISH("british", "بريطانية", "British"),
}

@Serializable
enum class AiArabicDialect(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val promptHintEn: String,
    val promptHintAr: String,
    val localeTag: String,
) {
    MSA(
        id = "msa",
        titleAr = "الفصحى",
        titleEn = "Modern Standard Arabic",
        promptHintEn = "Modern Standard Arabic",
        promptHintAr = "بالعربية الفصحى",
        localeTag = "ar-SA",
    ),
    IRAQI(
        id = "iraqi",
        titleAr = "العراقية",
        titleEn = "Iraqi",
        promptHintEn = "Iraqi Arabic",
        promptHintAr = "باللهجة العراقية",
        localeTag = "ar-IQ",
    ),
    EGYPTIAN(
        id = "egyptian",
        titleAr = "المصرية",
        titleEn = "Egyptian",
        promptHintEn = "Egyptian Arabic",
        promptHintAr = "باللهجة المصرية",
        localeTag = "ar-EG",
    ),
    SAUDI(
        id = "saudi",
        titleAr = "السعودية",
        titleEn = "Saudi",
        promptHintEn = "Saudi Arabic",
        promptHintAr = "باللهجة السعودية",
        localeTag = "ar-SA",
    ),
    SYRIAN(
        id = "syrian",
        titleAr = "السورية",
        titleEn = "Syrian",
        promptHintEn = "Syrian Arabic",
        promptHintAr = "باللهجة السورية",
        localeTag = "ar-SY",
    ),
}

@Serializable
enum class AiVoiceEmotion(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val voxDesignHintEn: String,
    val pitchBiasSemitones: Float,
    val speedBiasFactor: Float,
) {
    NEUTRAL(
        id = "neutral",
        titleAr = "طبيعي متزن",
        titleEn = "Natural",
        voxDesignHintEn = "natural balanced studio tone",
        pitchBiasSemitones = 0f,
        speedBiasFactor = 1.0f,
    ),
    EXCITED(
        id = "excited",
        titleAr = "حماسي وقوي",
        titleEn = "Excited",
        voxDesignHintEn = "energetic bold enthusiastic tone",
        pitchBiasSemitones = 0.75f,
        speedBiasFactor = 1.08f,
    ),
    CALM(
        id = "calm",
        titleAr = "هادئ ودافئ",
        titleEn = "Calm & Warm",
        voxDesignHintEn = "soothing warm peaceful cadence",
        pitchBiasSemitones = -0.35f,
        speedBiasFactor = 0.93f,
    ),
    SAD(
        id = "sad",
        titleAr = "حزين ومؤثر",
        titleEn = "Sad",
        voxDesignHintEn = "deeply emotional melancholic tone",
        pitchBiasSemitones = -0.75f,
        speedBiasFactor = 0.87f,
    ),
    SERIOUS(
        id = "serious",
        titleAr = "جاد ورسمي",
        titleEn = "Serious",
        voxDesignHintEn = "authoritative formal broadcast tone",
        pitchBiasSemitones = -0.50f,
        speedBiasFactor = 0.96f,
    ),
    WHISPER(
        id = "whisper",
        titleAr = "همس ناعم",
        titleEn = "Whisper",
        voxDesignHintEn = "intimate soft airy close-mic whisper",
        pitchBiasSemitones = 0.20f,
        speedBiasFactor = 0.90f,
    ),
}

@Serializable
enum class AiVocalStyle(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val instructionEn: String,
) {
    NATURAL("natural", "محادثة طبيعية", "Natural Conversation", "natural conversational delivery"),
    WARM("warm", "دافئ", "Warm", "warm, close and reassuring delivery"),
    EXPRESSIVE("expressive", "تعبيري", "Expressive", "expressive delivery with natural dynamic variation"),
    DRAMATIC("dramatic", "درامي", "Dramatic", "dramatic delivery with deliberate emphasis"),
    STORYTELLING("storytelling", "سرد قصصي", "Storytelling", "engaging storytelling with thoughtful phrasing"),
    NEWS_ANCHOR("news_anchor", "مذيع إخباري", "News Anchor", "confident broadcast news-anchor delivery"),
    WHISPERED("whispered", "همس", "Whispered", "soft intimate near-whisper delivery"),
    ENERGETIC("energetic", "حيوي", "Energetic", "bright energetic delivery with clear projection"),
}

@Serializable
enum class AiBreathCadence(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val instructionEn: String,
    val pauseAfterMs: Int,
) {
    NATURAL("natural", "تنفس طبيعي", "Natural", "natural breath groups and conversational pauses", 180),
    MEASURED("measured", "متأنٍ", "Measured", "measured phrasing with relaxed pauses between clauses", 320),
    BRISK("brisk", "إيقاع سريع", "Brisk", "brisk connected phrasing with short pauses", 100),
    DRAMATIC("dramatic", "وقفات درامية", "Dramatic", "deliberate dramatic pauses at phrase boundaries", 520),
}

@Serializable
enum class AiPitchContour(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val instructionEn: String,
    val semitoneBias: Float,
    val vibratoHz: Float,
    val vibratoDepth: Float,
) {
    STEADY("steady", "ثابت", "Steady", "a steady, controlled pitch contour", 0f, 0f, 0f),
    GENTLE_RISE("gentle_rise", "ارتفاع لطيف", "Gentle Rise", "a gentle upward pitch inflection", 0.35f, 0f, 0f),
    GENTLE_FALL("gentle_fall", "انخفاض لطيف", "Gentle Fall", "a gentle downward pitch inflection", -0.35f, 0f, 0f),
    EXPRESSIVE_SWELL("expressive_swell", "منحنى تعبيري", "Expressive Swell", "a subtle expressive pitch contour", 0.1f, 4.8f, 0.045f),
}

@Serializable
enum class AiVoiceTimbre(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val instructionEn: String,
    val formantMultiplier: Float,
    val warmthDb: Float,
    val presenceDb: Float,
) {
    BALANCED("balanced", "متوازن", "Balanced", "balanced natural vocal resonance", 1f, 0f, 0f),
    WARM("warm", "دافئ", "Warm", "warm, rounded vocal resonance", 0.99f, 2f, 0.5f),
    DEEP("deep", "عميق", "Deep", "deep, full-bodied vocal resonance", 0.93f, 3.5f, -0.5f),
    BRIGHT("bright", "مشرق", "Bright", "bright, clear vocal resonance", 1.05f, -0.5f, 2.5f),
    AIRY("airy", "هوائي", "Airy", "light, airy vocal resonance without harshness", 1.03f, -1f, 3f),
}

@Serializable
enum class AiStudioPreset(
    val id: String,
    val titleAr: String,
    val titleEn: String,
) {
    CUSTOM("custom", "مخصص", "Custom"),
    BROADCAST("broadcast", "إذاعي احترافي", "Broadcast"),
    WARM_PODCAST("warm_podcast", "بودكاست دافئ", "Warm Podcast"),
    CINEMATIC_STORY("cinematic_story", "سرد سينمائي", "Cinematic Story"),
    BRIGHT_PROMO("bright_promo", "إعلان حيوي", "Bright Promo"),
    INTIMATE_WHISPER("intimate_whisper", "همس حميم", "Intimate Whisper"),
}

fun AiVoiceEmotion.defaultVocalStyle(): AiVocalStyle = when (this) {
    AiVoiceEmotion.NEUTRAL -> AiVocalStyle.NATURAL
    AiVoiceEmotion.EXCITED -> AiVocalStyle.ENERGETIC
    AiVoiceEmotion.CALM -> AiVocalStyle.WARM
    AiVoiceEmotion.SAD -> AiVocalStyle.STORYTELLING
    AiVoiceEmotion.SERIOUS -> AiVocalStyle.NEWS_ANCHOR
    AiVoiceEmotion.WHISPER -> AiVocalStyle.WHISPERED
}

@Serializable
enum class AiDeliveryPace(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val speedMultiplier: Float,
    val promptHintEn: String,
) {
    VERY_SLOW("very_slow", "بطيء جداً", "Very Slow", 0.75f, "very slow deliberate pacing"),
    SLOW("slow", "بطيء متأني", "Slow", 0.88f, "measured unhurried pacing"),
    NORMAL("normal", "إيقاع طبيعي", "Normal Pace", 1.0f, "natural conversational cadence"),
    FAST("fast", "سريع وحيوي", "Fast", 1.15f, "brisk energetic rapid cadence"),
    DRAMATIC_PAUSES("dramatic", "متدرج درامي", "Dramatic", 0.92f, "dramatic cinema pacing with crisp pauses");

    companion object {
        fun fromSpeechRate(rate: Float): AiDeliveryPace = when {
            rate <= 0.80f -> VERY_SLOW
            rate <= 0.93f -> SLOW
            rate <= 1.08f -> NORMAL
            else -> FAST
        }
    }
}

@Serializable
enum class AiVoiceCategory(
    val id: String,
    val titleAr: String,
    val titleEn: String,
) {
    MALE("male", "رجال", "Men"),
    FEMALE("female", "نساء", "Women"),
    CHILD("child", "أطفال", "Children"),
    OLDER_ADULT("older_adult", "كبار السن", "Older adults"),
    ;

    fun toGroupFilter(): AiVoiceGroupFilter = when (this) {
        MALE -> AiVoiceGroupFilter.MALE
        FEMALE -> AiVoiceGroupFilter.FEMALE
        CHILD -> AiVoiceGroupFilter.CHILD
        OLDER_ADULT -> AiVoiceGroupFilter.OLDER_ADULT
    }
}

enum class AiVoiceGroupFilter(
    val id: String,
    val titleAr: String,
    val titleEn: String,
) {
    MALE("male", "رجال", "Men"),
    FEMALE("female", "نساء", "Women"),
    CHILD("child", "أطفال", "Children"),
    OLDER_ADULT("older_adult", "كبار السن", "Older adults"),
}

fun countScriptWords(text: String): Int =
    text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }

@Immutable
@Serializable
data class AiVoiceCharacter(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val roleDescriptionAr: String = "",
    val roleDescriptionEn: String = "",
    val category: AiVoiceCategory,
    val language: AiVoiceLanguage = AiVoiceLanguage.AR,
    val dialect: AiArabicDialect = AiArabicDialect.MSA,
    val preferredEngine: AiVoiceEngineKind = AiVoiceEngineKind.VOXCPM_2,
    val voiceDescriptionPrompt: String,
    val defaultEmotion: AiVoiceEmotion = AiVoiceEmotion.NEUTRAL,
    val defaultEmotionIntensity: Float = 0.65f,
    val defaultPace: AiDeliveryPace = AiDeliveryPace.NORMAL,
    val defaultSpeechRate: Float = 1.0f,
    val pitchOffsetSemitones: Float = 0f,
    val fixedSid: Int = 0,
    val fixedSeed: Int = FIXED_DETERMINISTIC_SEED,
    val fixedFormantShift: Float = 1.0f,
    val fixedEqLowDb: Float = 0f,
    val fixedEqPresenceDb: Float = 0f,
    val defaultVocalStyle: AiVocalStyle = AiVocalStyle.NATURAL,
    val defaultBreathCadence: AiBreathCadence = AiBreathCadence.NATURAL,
    val defaultPitchContour: AiPitchContour = AiPitchContour.STEADY,
    val defaultTimbre: AiVoiceTimbre = AiVoiceTimbre.BALANCED,
    val defaultArticulationClarity: Float = 0.55f,
    val referenceAudioPath: String? = null,
    val referenceAudioName: String? = null,
    val referenceTranscript: String = "",
    val isBuiltIn: Boolean = false,
    val createdAtMs: Long = 0L,
) {
    fun displayName(isEnglish: Boolean): String = if (isEnglish && nameEn.isNotBlank()) nameEn else nameAr

    fun displayDescription(isEnglish: Boolean): String {
        val raw = if (isEnglish) roleDescriptionEn else roleDescriptionAr
        return raw.ifBlank { voiceDescriptionPrompt }
    }
}

@Immutable
@Serializable
data class AiDialogueTurn(
    val id: String,
    val characterId: String,
    val text: String,
    val emotion: AiVoiceEmotion = AiVoiceEmotion.NEUTRAL,
    val emotionIntensity: Float = 0.65f,
    val pace: AiDeliveryPace = AiDeliveryPace.NORMAL,
    val pauseAfterMs: Int = 450,
    val generatedWavPath: String? = null,
)

@Immutable
data class AiVoiceSynthRequest(
    val engine: AiVoiceEngineKind,
    val text: String,
    val language: AiVoiceLanguage,
    val voiceCharacterId: String = "",
    val dialect: AiArabicDialect = AiArabicDialect.MSA,
    val englishAccent: AiEnglishAccent = AiEnglishAccent.AMERICAN,
    val category: AiVoiceCategory,
    val voiceDescriptionPrompt: String,
    val emotion: AiVoiceEmotion,
    val emotionIntensity: Float,
    val pace: AiDeliveryPace = AiDeliveryPace.NORMAL,
    val speechRate: Float = pace.speedMultiplier,
    val pitchOffsetSemitones: Float = 0f,
    val fixedSid: Int = 0,
    val fixedSeed: Int = FIXED_DETERMINISTIC_SEED,
    val fixedFormantShift: Float = 1.0f,
    val fixedEqLowDb: Float = 0f,
    val fixedEqPresenceDb: Float = 0f,
    val vocalStyle: AiVocalStyle = AiVocalStyle.NATURAL,
    val breathCadence: AiBreathCadence = AiBreathCadence.NATURAL,
    val pitchContour: AiPitchContour = AiPitchContour.STEADY,
    val timbre: AiVoiceTimbre = AiVoiceTimbre.BALANCED,
    val articulationClarity: Float = 0.55f,
    val referenceAudioPath: String? = null,
    val referenceTranscript: String = "",
    val cfgValue: Float = 2.0f,
    val inferenceTimesteps: Int = 10,
    val normalizeAudio: Boolean = true,
    val referenceAudioOptional: Boolean = false,
    val acousticProfileId: String = "",
)

sealed interface AiVoiceSynthOutcome {
    data class Success(
        val audioFilePath: String,
        val sampleRate: Int,
        val durationMs: Long,
        val engineUsed: AiVoiceEngineKind,
        val appliedPromptSummary: String,
        val isCloudGenerated: Boolean,
    ) : AiVoiceSynthOutcome

    data class ConfigurationRequired(
        val engine: AiVoiceEngineKind,
        val titleAr: String,
        val titleEn: String,
        val explanationAr: String,
        val explanationEn: String,
        val setupStepsAr: String,
        val setupStepsEn: String,
    ) : AiVoiceSynthOutcome

    data class Error(
        val engine: AiVoiceEngineKind,
        val messageAr: String,
        val messageEn: String,
        val technicalDetail: String? = null,
    ) : AiVoiceSynthOutcome
}

@Immutable
data class AiVoiceBackendHealth(
    val isChecked: Boolean = false,
    val isChecking: Boolean = false,
    val serverReachable: Boolean = false,
    val voxCpm2Ready: Boolean = false,
    val voxcpm2PackageInstalled: Boolean = false,
    val voxcpm2Loaded: Boolean = false,
    val cudaAvailable: Boolean = false,
    val authRequired: Boolean = false,
    val voxcpm2LoadError: String = "",
    val voxCpmPublicSpaceChecked: Boolean = false,
    val voxCpmPublicSpaceAvailable: Boolean = false,
    val statusDetail: String = "",
    val serverGpuInfo: String = "",
)

@Immutable
data class AiVoiceStudioUiState(
    val activeTab: Int = 0, // 0=Studio, 1=Advanced (Dialogue & Cloning)
    val selectedEngine: AiVoiceEngineKind = AiVoiceEngineKind.VOXCPM_2,
    val selectedLanguage: AiVoiceLanguage = AiVoiceLanguage.AR,
    val selectedDialect: AiArabicDialect = AiArabicDialect.MSA,
    val selectedEnglishAccent: AiEnglishAccent = AiEnglishAccent.AMERICAN,
    val selectedCategory: AiVoiceCategory = AiVoiceCategory.MALE,
    val selectedCharacterId: String = "male_news_podcast",
    val selectedEmotion: AiVoiceEmotion = AiVoiceEmotion.NEUTRAL,
    val selectedVocalStyle: AiVocalStyle = AiVocalStyle.NEWS_ANCHOR,
    val selectedBreathCadence: AiBreathCadence = AiBreathCadence.NATURAL,
    val selectedPitchContour: AiPitchContour = AiPitchContour.STEADY,
    val selectedTimbre: AiVoiceTimbre = AiVoiceTimbre.BALANCED,
    val articulationClarity: Float = 0.55f,
    val selectedStudioPreset: AiStudioPreset = AiStudioPreset.CUSTOM,
    val emotionIntensity: Float = 0.65f,
    val selectedPace: AiDeliveryPace = AiDeliveryPace.NORMAL,
    val speechRate: Float = 1.0f,
    val pitchOffsetSemitones: Float = -0.8f,
    val fixedSid: Int = 0,
    val fixedSeed: Int = FIXED_DETERMINISTIC_SEED,
    val fixedFormantShift: Float = 0.96f,
    val fixedEqLowDb: Float = 2.0f,
    val fixedEqPresenceDb: Float = 2.4f,
    val voiceDescriptionPrompt: String = "Professional studio news & podcast anchor, clear poised resonant baritone delivery.",
    val scriptText: String = "أهلاً بكم في استوديو الصوت الذكي، حيث تتحول الكلمات إلى أداء احترافي متزن وثابت.",
    val referenceAudioPath: String? = null,
    val referenceAudioName: String? = null,
    val referenceAudioDurationMs: Long = 0L,
    val referenceTranscript: String = "",
    val referenceRightsConfirmed: Boolean = true,
    val referenceImportProgress: Float? = null,
    val referenceImportStatus: String = "",
    val isReferenceImporting: Boolean = false,
    val isSynthesizing: Boolean = false,
    val synthesisProgress: Float = 0f,
    val synthesisStatusLabel: String = "",
    val errorTitle: String? = null,
    val errorDetail: String? = null,
    val setupRequiredGuide: String? = null,
    val lastOutputWavPath: String? = null,
    val lastOutputDurationMs: Long = 0L,
    val lastOutputSampleRate: Int = 44100,
    val lastOutputEngine: AiVoiceEngineKind? = null,
    val lastOutputAppliedPrompt: String? = null,
    val isPlayingOutput: Boolean = false,
    val playbackPositionMs: Long = 0L,
    val savedCharacters: List<AiVoiceCharacter> = emptyList(),
    val favoriteCharacterIds: Set<String> = emptySet(),
    val dialogueTurns: List<AiDialogueTurn> = DEFAULT_DIALOGUE_TURNS,
    val backendServerUrl: String = "",
    val backendBearerToken: String = "",
    val backendHealth: AiVoiceBackendHealth = AiVoiceBackendHealth(),
) {
    val allCharacters: List<AiVoiceCharacter>
        get() = if (savedCharacters.isEmpty()) {
            ALL_BUILT_IN_AI_VOICE_CHARACTERS
        } else {
            savedCharacters + ALL_BUILT_IN_AI_VOICE_CHARACTERS
        }

    fun isCharacterFavorite(characterId: String): Boolean =
        characterId in favoriteCharacterIds || savedCharacters.any { it.id == characterId }
}

val BUILT_IN_AI_VOICE_CHARACTERS: List<AiVoiceCharacter> = listOf(
    // --- Male (رجالي) - 3 fixed professional roles ---
    AiVoiceCharacter(
        id = "male_news_podcast",
        nameAr = "مذيع إخباري وبودكاست",
        nameEn = "News & Podcast Anchor",
        roleDescriptionAr = "صوت واضح، متزن، وإلقاء احترافي",
        roleDescriptionEn = "Clear, balanced, broadcast studio delivery",
        category = AiVoiceCategory.MALE,
        language = AiVoiceLanguage.AR,
        dialect = AiArabicDialect.MSA,
        preferredEngine = AiVoiceEngineKind.VOXCPM_2,
        voiceDescriptionPrompt = "Professional studio news and podcast host, crisp articulation, poised resonant baritone tone.",
        defaultEmotion = AiVoiceEmotion.NEUTRAL,
        defaultEmotionIntensity = 0.65f,
        defaultPace = AiDeliveryPace.NORMAL,
        defaultSpeechRate = 1.0f,
        pitchOffsetSemitones = -0.8f,
        fixedSid = 0,
        fixedSeed = FIXED_DETERMINISTIC_SEED,
        fixedFormantShift = 0.96f,
        fixedEqLowDb = 2.0f,
        fixedEqPresenceDb = 2.4f,
        defaultVocalStyle = AiVocalStyle.NEWS_ANCHOR,
        defaultArticulationClarity = 0.72f,
        isBuiltIn = true,
    ),
    AiVoiceCharacter(
        id = "male_doc_narrator",
        nameAr = "راوي وثائقي سينمائي",
        nameEn = "Documentary Narrator",
        roleDescriptionAr = "نبرة عميقة، هادئة، ومقنعة",
        roleDescriptionEn = "Deep, steady, persuasive cinematic tone",
        category = AiVoiceCategory.MALE,
        language = AiVoiceLanguage.AR,
        dialect = AiArabicDialect.MSA,
        preferredEngine = AiVoiceEngineKind.VOXCPM_2,
        voiceDescriptionPrompt = "Cinematic documentary narrator with rich deep authoritative warmth and unhurried poised pacing.",
        defaultEmotion = AiVoiceEmotion.CALM,
        defaultEmotionIntensity = 0.65f,
        defaultPace = AiDeliveryPace.SLOW,
        defaultSpeechRate = 0.92f,
        pitchOffsetSemitones = -2.4f,
        fixedSid = 1,
        fixedSeed = FIXED_DETERMINISTIC_SEED + 1,
        fixedFormantShift = 0.89f,
        fixedEqLowDb = 4.2f,
        fixedEqPresenceDb = 1.8f,
        defaultVocalStyle = AiVocalStyle.STORYTELLING,
        defaultBreathCadence = AiBreathCadence.MEASURED,
        defaultPitchContour = AiPitchContour.GENTLE_FALL,
        defaultTimbre = AiVoiceTimbre.DEEP,
        defaultArticulationClarity = 0.64f,
        isBuiltIn = true,
    ),
    AiVoiceCharacter(
        id = "male_drama_speaker",
        nameAr = "متحدث درامي وتفاعلي",
        nameEn = "Dramatic Speaker",
        roleDescriptionAr = "أداء تعبيري حيوي ومؤثر",
        roleDescriptionEn = "Vivid expressive theatrical performance",
        category = AiVoiceCategory.MALE,
        language = AiVoiceLanguage.AR,
        dialect = AiArabicDialect.MSA,
        preferredEngine = AiVoiceEngineKind.VOXCPM_2,
        voiceDescriptionPrompt = "Expressive dramatic male voice actor with dynamic projection and clear emotional presence.",
        defaultEmotion = AiVoiceEmotion.EXCITED,
        defaultEmotionIntensity = 0.70f,
        defaultPace = AiDeliveryPace.NORMAL,
        defaultSpeechRate = 1.02f,
        pitchOffsetSemitones = 0.4f,
        fixedSid = 2,
        fixedSeed = FIXED_DETERMINISTIC_SEED + 2,
        fixedFormantShift = 1.02f,
        fixedEqLowDb = 1.4f,
        fixedEqPresenceDb = 3.0f,
        defaultVocalStyle = AiVocalStyle.DRAMATIC,
        defaultBreathCadence = AiBreathCadence.DRAMATIC,
        defaultPitchContour = AiPitchContour.EXPRESSIVE_SWELL,
        defaultArticulationClarity = 0.70f,
        isBuiltIn = true,
    ),

    // --- Female (نسائي) - 3 fixed professional roles ---
    AiVoiceCharacter(
        id = "female_podcast_host",
        nameAr = "مذيعة بودكاست وإلقاء",
        nameEn = "Podcast & Studio Host",
        roleDescriptionAr = "صوت دافئ، واضح، ومريح",
        roleDescriptionEn = "Warm, crisp, welcoming studio delivery",
        category = AiVoiceCategory.FEMALE,
        language = AiVoiceLanguage.AR,
        dialect = AiArabicDialect.MSA,
        preferredEngine = AiVoiceEngineKind.VOXCPM_2,
        voiceDescriptionPrompt = "Warm polished female podcast anchor with inviting studio closeness and crystal clarity.",
        defaultEmotion = AiVoiceEmotion.NEUTRAL,
        defaultEmotionIntensity = 0.65f,
        defaultPace = AiDeliveryPace.NORMAL,
        defaultSpeechRate = 1.0f,
        pitchOffsetSemitones = 0.9f,
        fixedSid = 3,
        fixedSeed = FIXED_DETERMINISTIC_SEED + 3,
        fixedFormantShift = 1.06f,
        fixedEqLowDb = 1.6f,
        fixedEqPresenceDb = 2.6f,
        defaultVocalStyle = AiVocalStyle.WARM,
        defaultTimbre = AiVoiceTimbre.WARM,
        defaultArticulationClarity = 0.72f,
        isBuiltIn = true,
    ),
    AiVoiceCharacter(
        id = "female_story_narrator",
        nameAr = "راوية قصص ووثائقيات",
        nameEn = "Story & Documentary Narrator",
        roleDescriptionAr = "نبرة هادئة، رصينة، وجذابة",
        roleDescriptionEn = "Calm, poised, captivating storytelling tone",
        category = AiVoiceCategory.FEMALE,
        language = AiVoiceLanguage.AR,
        dialect = AiArabicDialect.MSA,
        preferredEngine = AiVoiceEngineKind.VOXCPM_2,
        voiceDescriptionPrompt = "Poised captivating female storyteller with silky calm resonance and thoughtful phrasing.",
        defaultEmotion = AiVoiceEmotion.CALM,
        defaultEmotionIntensity = 0.65f,
        defaultPace = AiDeliveryPace.SLOW,
        defaultSpeechRate = 0.93f,
        pitchOffsetSemitones = -0.3f,
        fixedSid = 4,
        fixedSeed = FIXED_DETERMINISTIC_SEED + 4,
        fixedFormantShift = 0.99f,
        fixedEqLowDb = 2.4f,
        fixedEqPresenceDb = 1.6f,
        defaultVocalStyle = AiVocalStyle.STORYTELLING,
        defaultBreathCadence = AiBreathCadence.MEASURED,
        defaultPitchContour = AiPitchContour.GENTLE_FALL,
        defaultTimbre = AiVoiceTimbre.WARM,
        defaultArticulationClarity = 0.68f,
        isBuiltIn = true,
    ),
    AiVoiceCharacter(
        id = "female_promo_speaker",
        nameAr = "متحدثة إعلانية وتفاعلية",
        nameEn = "Commercial & Dynamic Host",
        roleDescriptionAr = "أداء نشط، مشرق، وواضح",
        roleDescriptionEn = "Bright, energetic, crisp commercial delivery",
        category = AiVoiceCategory.FEMALE,
        language = AiVoiceLanguage.AR,
        dialect = AiArabicDialect.MSA,
        preferredEngine = AiVoiceEngineKind.VOXCPM_2,
        voiceDescriptionPrompt = "Bright lively commercial female presenter with uplifting clarity and crisp articulation.",
        defaultEmotion = AiVoiceEmotion.EXCITED,
        defaultEmotionIntensity = 0.72f,
        defaultPace = AiDeliveryPace.FAST,
        defaultSpeechRate = 1.08f,
        pitchOffsetSemitones = 1.9f,
        fixedSid = 5,
        fixedSeed = FIXED_DETERMINISTIC_SEED + 5,
        fixedFormantShift = 1.11f,
        fixedEqLowDb = 0.6f,
        fixedEqPresenceDb = 3.4f,
        defaultVocalStyle = AiVocalStyle.ENERGETIC,
        defaultBreathCadence = AiBreathCadence.BRISK,
        defaultPitchContour = AiPitchContour.GENTLE_RISE,
        defaultTimbre = AiVoiceTimbre.BRIGHT,
        defaultArticulationClarity = 0.82f,
        isBuiltIn = true,
    ),
    AiVoiceCharacter(
        id = "child_arabic_storyteller",
        nameAr = "طفل راوي قصص",
        nameEn = "Arabic Child Storyteller",
        roleDescriptionAr = "صوت طفل طبيعي وهادئ مناسب للقصص",
        roleDescriptionEn = "Natural, gentle child narration for stories",
        category = AiVoiceCategory.CHILD,
        language = AiVoiceLanguage.AR,
        dialect = AiArabicDialect.MSA,
        voiceDescriptionPrompt = "Natural young child voice with gentle clear storytelling delivery.",
        defaultEmotion = AiVoiceEmotion.CALM,
        defaultEmotionIntensity = 0.55f,
        defaultPace = AiDeliveryPace.SLOW,
        defaultSpeechRate = 0.94f,
        pitchOffsetSemitones = 3.0f,
        fixedSid = 6,
        fixedSeed = FIXED_DETERMINISTIC_SEED + 6,
        fixedFormantShift = 1.10f,
        fixedEqLowDb = 0.3f,
        fixedEqPresenceDb = 1.4f,
        defaultVocalStyle = AiVocalStyle.STORYTELLING,
        defaultBreathCadence = AiBreathCadence.MEASURED,
        defaultTimbre = AiVoiceTimbre.BRIGHT,
        defaultArticulationClarity = 0.70f,
        isBuiltIn = true,
    ),
    AiVoiceCharacter(
        id = "child_english_storyteller",
        nameAr = "طفل متحدث بالإنجليزية",
        nameEn = "English Child Speaker",
        roleDescriptionAr = "صوت طفل طبيعي وواضح باللغة الإنجليزية",
        roleDescriptionEn = "Natural, clear child voice for English",
        category = AiVoiceCategory.CHILD,
        language = AiVoiceLanguage.EN,
        voiceDescriptionPrompt = "Natural young child voice with clear, relaxed English delivery.",
        defaultEmotion = AiVoiceEmotion.NEUTRAL,
        defaultEmotionIntensity = 0.55f,
        defaultPace = AiDeliveryPace.NORMAL,
        defaultSpeechRate = 0.98f,
        pitchOffsetSemitones = 3.2f,
        fixedSid = 7,
        fixedSeed = FIXED_DETERMINISTIC_SEED + 7,
        fixedFormantShift = 1.11f,
        fixedEqLowDb = 0.2f,
        fixedEqPresenceDb = 1.2f,
        defaultVocalStyle = AiVocalStyle.NATURAL,
        defaultTimbre = AiVoiceTimbre.BRIGHT,
        defaultArticulationClarity = 0.68f,
        isBuiltIn = true,
    ),
    AiVoiceCharacter(
        id = "child_turkish_storyteller",
        nameAr = "طفل متحدث بالتركية",
        nameEn = "Turkish Child Speaker",
        roleDescriptionAr = "صوت طفل طبيعي وواضح باللغة التركية",
        roleDescriptionEn = "Natural, clear child voice for Turkish",
        category = AiVoiceCategory.CHILD,
        language = AiVoiceLanguage.TR,
        voiceDescriptionPrompt = "Natural young child voice with clear, warm Turkish delivery.",
        defaultEmotion = AiVoiceEmotion.NEUTRAL,
        defaultEmotionIntensity = 0.55f,
        defaultPace = AiDeliveryPace.NORMAL,
        defaultSpeechRate = 0.98f,
        pitchOffsetSemitones = 3.0f,
        fixedSid = 8,
        fixedSeed = FIXED_DETERMINISTIC_SEED + 8,
        fixedFormantShift = 1.10f,
        fixedEqLowDb = 0.2f,
        fixedEqPresenceDb = 1.2f,
        defaultVocalStyle = AiVocalStyle.NATURAL,
        defaultTimbre = AiVoiceTimbre.BRIGHT,
        defaultArticulationClarity = 0.68f,
        isBuiltIn = true,
    ),
    AiVoiceCharacter(
        id = "child_persian_storyteller",
        nameAr = "طفل متحدث بالفارسية",
        nameEn = "Persian Child Speaker",
        roleDescriptionAr = "صوت طفل طبيعي وواضح باللغة الفارسية",
        roleDescriptionEn = "Natural, clear child voice for Persian",
        category = AiVoiceCategory.CHILD,
        language = AiVoiceLanguage.FA,
        voiceDescriptionPrompt = "Natural young child voice with gentle, clear Persian delivery.",
        defaultEmotion = AiVoiceEmotion.CALM,
        defaultEmotionIntensity = 0.55f,
        defaultPace = AiDeliveryPace.NORMAL,
        defaultSpeechRate = 0.97f,
        pitchOffsetSemitones = 3.1f,
        fixedSid = 9,
        fixedSeed = FIXED_DETERMINISTIC_SEED + 9,
        fixedFormantShift = 1.10f,
        fixedEqLowDb = 0.2f,
        fixedEqPresenceDb = 1.2f,
        defaultVocalStyle = AiVocalStyle.STORYTELLING,
        defaultTimbre = AiVoiceTimbre.BRIGHT,
        defaultArticulationClarity = 0.68f,
        isBuiltIn = true,
    ),
)

val ALL_BUILT_IN_AI_VOICE_CHARACTERS: List<AiVoiceCharacter> =
    BUILT_IN_AI_VOICE_CHARACTERS

val DEFAULT_DIALOGUE_TURNS: List<AiDialogueTurn> = listOf(
    AiDialogueTurn(
        id = "turn_1",
        characterId = "male_news_podcast",
        text = "أهلاً بكم في هذا الحوار المباشر حول جودة الهندسة الصوتية.",
        emotion = AiVoiceEmotion.NEUTRAL,
        emotionIntensity = 0.65f,
        pace = AiDeliveryPace.NORMAL,
        pauseAfterMs = 450,
    ),
    AiDialogueTurn(
        id = "turn_2",
        characterId = "female_podcast_host",
        text = "سعيدة جداً بوجودي معكم اليوم لنستعرض معاً أدق تفاصيل الصوت.",
        emotion = AiVoiceEmotion.CALM,
        emotionIntensity = 0.65f,
        pace = AiDeliveryPace.NORMAL,
        pauseAfterMs = 450,
    ),
)
