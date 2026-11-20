package com.example.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
enum class AppLanguage(val tag: String, val nativeName: String, val shortBadge: String, val isRtl: Boolean) {
    AR("ar", "العربية", "عربي", true),
    EN("en", "English", "EN", false),
    TR("tr", "Türkçe", "TR", false),
    FA("fa", "فارسی", "فارسی", true);

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag == tag } ?: AR
    }
}

enum class EffectCategory {
    TIME_SPACE,
    TONE_EQ,
    PITCH_SPEED,
    MODULATION,
    CHARACTER_DYNAMICS,
    NOISE_CLEAN,
}

@Serializable
enum class EffectType(val titleKey: String, val category: EffectCategory, val isProDefault: Boolean) {
    NOISE_REDUCTION("effect_noise_reduction", EffectCategory.NOISE_CLEAN, false),
    EQUALIZER("effect_equalizer", EffectCategory.TONE_EQ, true),
    BASS_BOOST("effect_bass_boost", EffectCategory.TONE_EQ, false),
    TREBLE("effect_treble", EffectCategory.TONE_EQ, false),
    PITCH_SHIFT("effect_pitch_shift", EffectCategory.PITCH_SPEED, false),
    TIME_STRETCH("effect_time_stretch", EffectCategory.PITCH_SPEED, true),
    CHORUS("effect_chorus", EffectCategory.MODULATION, true),
    FLANGER("effect_flanger", EffectCategory.MODULATION, true),
    PHASER("effect_phaser", EffectCategory.MODULATION, true),
    DISTORTION("effect_distortion", EffectCategory.CHARACTER_DYNAMICS, true),
    COMPRESSOR("effect_compressor", EffectCategory.CHARACTER_DYNAMICS, true),
    ECHO_REVERB("effect_echo_reverb", EffectCategory.TIME_SPACE, false),
    LIMITER("effect_limiter", EffectCategory.CHARACTER_DYNAMICS, true),
}

@Immutable
@Serializable
data class EchoReverbParams(
    val echoStrength: Float = 0f,
    val echoDepth: Float = 0f,
    val delayTimeMs: Float = 240f,
    val feedback: Float = 0f,
    val reverbSize: Float = 0f,
    val reverbDecay: Float = 0f,
    val preDelayMs: Float = 0f,
    val wetDryMix: Float = 0f,
    val stereoWidth: Float = 0f,
    val earlyReflections: Float = 0f,
    val lateReflections: Float = 0f,
    val damping: Float = 0f,
    val diffusion: Float = 0f,
)

/** Single immutable source of truth for every Professional Echo control. */
typealias ProEchoState = EchoReverbParams

val EQ_10_FREQUENCIES_HZ: IntArray = intArrayOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

val EQ_10_LABELS: List<String> = listOf(
    "31 Hz", "62 Hz", "125 Hz", "250 Hz", "500 Hz",
    "1 kHz", "2 kHz", "4 kHz", "8 kHz", "16 kHz",
)

@Immutable
data class EqPresetProfile(
    val id: String,
    val titleKey: String,
    val bands10Db: FloatArray,
)

val EQ_PRESET_PROFILES: List<EqPresetProfile> = listOf(
    EqPresetProfile("flat", "eq_preset_flat", floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
    EqPresetProfile("vocal_clarity", "eq_preset_vocal", floatArrayOf(-2.5f, -1.5f, -1.0f, -0.5f, 0.5f, 1.5f, 3.0f, 4.0f, 3.2f, 1.5f)),
    EqPresetProfile("podcast_pro", "eq_preset_podcast", floatArrayOf(-3.0f, -1.5f, 1.5f, 0.5f, -0.5f, 1.0f, 2.8f, 3.8f, 2.5f, 1.5f)),
    EqPresetProfile("warm_radio", "eq_preset_warm", floatArrayOf(1.5f, 2.5f, 3.0f, 1.8f, 0.5f, 0.0f, 1.2f, 2.0f, 0.5f, -1.0f)),
    EqPresetProfile("bright_air", "eq_preset_bright", floatArrayOf(-1.0f, 0.0f, 0.5f, 0.5f, 0.5f, 1.0f, 2.5f, 3.5f, 4.5f, 5.0f)),
    EqPresetProfile("deep_bass", "eq_preset_bass", floatArrayOf(5.5f, 4.5f, 2.5f, 0.5f, -0.5f, 0.0f, 1.0f, 2.0f, 1.5f, 0.5f)),
)

@Immutable
@Serializable
data class EqualizerParams(
    val band60HzDb: Float = 0f,
    val band230HzDb: Float = 0f,
    val band910HzDb: Float = 0f,
    val band3600HzDb: Float = 0f,
    val band14000HzDb: Float = 0f,
    val band31HzDb: Float? = null,
    val band62HzDb: Float? = null,
    val band125HzDb: Float? = null,
    val band250HzDb: Float? = null,
    val band500HzDb: Float? = null,
    val band1000HzDb: Float? = null,
    val band2000HzDb: Float? = null,
    val band4000HzDb: Float? = null,
    val band8000HzDb: Float? = null,
    val band16000HzDb: Float? = null,
) {
    private fun legacyInterpolated10Bands(): FloatArray {
        val legacyFreqs = doubleArrayOf(60.0, 230.0, 910.0, 3600.0, 14000.0)
        val legacyGains = floatArrayOf(band60HzDb, band230HzDb, band910HzDb, band3600HzDb, band14000HzDb)
        return FloatArray(10) { idx ->
            val f = EQ_10_FREQUENCIES_HZ[idx].toDouble()
            when {
                f <= legacyFreqs.first() -> legacyGains.first() * 0.85f
                f >= legacyFreqs.last() -> legacyGains.last() * 0.9f
                else -> {
                    var seg = 0
                    while (seg < legacyFreqs.size - 2 && f > legacyFreqs[seg + 1]) seg++
                    val logLow = kotlin.math.ln(legacyFreqs[seg])
                    val logHigh = kotlin.math.ln(legacyFreqs[seg + 1])
                    val t = ((kotlin.math.ln(f) - logLow) / (logHigh - logLow)).toFloat().coerceIn(0f, 1f)
                    legacyGains[seg] + (legacyGains[seg + 1] - legacyGains[seg]) * t
                }
            }.coerceIn(-12f, 12f)
        }
    }

    fun resolved10Bands(): FloatArray {
        val raw = arrayOf(
            band31HzDb, band62HzDb, band125HzDb, band250HzDb, band500HzDb,
            band1000HzDb, band2000HzDb, band4000HzDb, band8000HzDb, band16000HzDb,
        )
        if (raw.all { it == null }) {
            return legacyInterpolated10Bands()
        }
        val legacy = legacyInterpolated10Bands()
        return FloatArray(10) { idx -> (raw[idx] ?: legacy[idx]).coerceIn(-12f, 12f) }
    }

    fun with10Band(index: Int, gainDb: Float): EqualizerParams {
        val b = resolved10Bands()
        if (index in b.indices) {
            b[index] = gainDb.coerceIn(-12f, 12f)
        }
        return from10Bands(b)
    }

    companion object {
        fun from10Bands(bands: FloatArray): EqualizerParams {
            val b = FloatArray(10) { idx -> bands.getOrElse(idx) { 0f }.coerceIn(-12f, 12f) }
            return EqualizerParams(
                band60HzDb = b[1],
                band230HzDb = b[3],
                band910HzDb = b[5],
                band3600HzDb = b[7],
                band14000HzDb = b[9],
                band31HzDb = b[0],
                band62HzDb = b[1],
                band125HzDb = b[2],
                band250HzDb = b[3],
                band500HzDb = b[4],
                band1000HzDb = b[5],
                band2000HzDb = b[6],
                band4000HzDb = b[7],
                band8000HzDb = b[8],
                band16000HzDb = b[9],
            )
        }
    }
}

@Immutable
@Serializable
data class BassBoostParams(
    val gainDb: Float = 0f,
    val frequencyHz: Float = 95f,
)

@Immutable
@Serializable
data class TrebleParams(
    val gainDb: Float = 0f,
    val frequencyHz: Float = 6500f,
)

@Immutable
@Serializable
data class PitchShiftParams(
    val semitones: Float = 0f,
    val wetMix: Float = 1f,
    val preserveFormants: Boolean = true,
)

@Immutable
@Serializable
data class TimeStretchParams(
    val tempoRatio: Float = 1f,
)

@Immutable
@Serializable
data class ChorusParams(
    val rateHz: Float = 1.2f,
    val depthMs: Float = 6.5f,
    val mix: Float = 0f,
)

@Immutable
@Serializable
data class FlangerParams(
    val rateHz: Float = 0.45f,
    val depthMs: Float = 3f,
    val feedback: Float = 0f,
    val mix: Float = 0f,
)

@Immutable
@Serializable
data class PhaserParams(
    val rateHz: Float = 0.6f,
    val depth: Float = 0f,
    val feedback: Float = 0f,
    val mix: Float = 0f,
)

@Serializable
enum class DistortionMode {
    SOFT_CLIP,
    HARD_CLIP,
    OVERDRIVE,
}

@Immutable
@Serializable
data class DistortionParams(
    val drive: Float = 1f,
    val toneHz: Float = 4200f,
    val mix: Float = 0f,
    val mode: DistortionMode = DistortionMode.OVERDRIVE,
)

@Immutable
@Serializable
data class CompressorParams(
    val thresholdDb: Float = 0f,
    val ratio: Float = 1f,
    val attackMs: Float = 12f,
    val releaseMs: Float = 140f,
    val makeupGainDb: Float = 0f,
)

@Immutable
@Serializable
data class LimiterParams(
    val ceilingDb: Float = 0f,
    val releaseMs: Float = 80f,
)

@Immutable
@Serializable
data class NoiseReductionParams(
    val thresholdDb: Float = -70f,
    val reductionDb: Float = 0f,
    val highPassHz: Float = 20f,
    val lowPassHz: Float = 16000f,
    val spectralStrength: Float = 0f,
    val deEssAmount: Float = 0f,
    val clarityBoostDb: Float = 0f,
    val spectralCleanerEnabled: Boolean = false,
)

@Immutable
@Serializable
data class EffectSlot(
    val type: EffectType,
    val enabled: Boolean,
)

val DEFAULT_CHAIN: List<EffectSlot> = listOf(
    EffectSlot(EffectType.NOISE_REDUCTION, false),
    EffectSlot(EffectType.EQUALIZER, false),
    EffectSlot(EffectType.BASS_BOOST, false),
    EffectSlot(EffectType.TREBLE, false),
    EffectSlot(EffectType.PITCH_SHIFT, false),
    EffectSlot(EffectType.TIME_STRETCH, false),
    EffectSlot(EffectType.CHORUS, false),
    EffectSlot(EffectType.FLANGER, false),
    EffectSlot(EffectType.PHASER, false),
    EffectSlot(EffectType.DISTORTION, false),
    EffectSlot(EffectType.COMPRESSOR, false),
    EffectSlot(EffectType.ECHO_REVERB, false),
    EffectSlot(EffectType.LIMITER, false),
)

@Immutable
@Serializable
data class DspConfig(
    val bypassAll: Boolean = false,
    val masterGainDb: Float = 0f,
    val spatialPanHz: Float = 0f,
    val surroundMidPct: Float = 0f,
    val surroundSidePct: Float = 0f,
    val surroundWidthPct: Float = 0f,
    val vibratoHz: Float = 0f,
    val vibratoDepth: Float = 0f,
    val chain: List<EffectSlot> = DEFAULT_CHAIN,
    val echoReverb: EchoReverbParams = EchoReverbParams(),
    val equalizer: EqualizerParams = EqualizerParams(),
    val bassBoost: BassBoostParams = BassBoostParams(),
    val treble: TrebleParams = TrebleParams(),
    val pitchShift: PitchShiftParams = PitchShiftParams(),
    val timeStretch: TimeStretchParams = TimeStretchParams(),
    val chorus: ChorusParams = ChorusParams(),
    val flanger: FlangerParams = FlangerParams(),
    val phaser: PhaserParams = PhaserParams(),
    val distortion: DistortionParams = DistortionParams(),
    val compressor: CompressorParams = CompressorParams(),
    val limiter: LimiterParams = LimiterParams(),
    val noiseReduction: NoiseReductionParams = NoiseReductionParams(),
) {
    fun normalized(): DspConfig {
        val valid = chain.distinctBy { it.type }
        val existing = valid.map { it.type }.toSet()
        val missing = DEFAULT_CHAIN.filter { it.type !in existing }
        return copy(chain = valid + missing)
    }

    fun isEnabled(type: EffectType): Boolean =
        chain.firstOrNull { it.type == type }?.enabled == true

    fun withSlotEnabled(type: EffectType, enabled: Boolean): DspConfig =
        copy(chain = chain.map { if (it.type == type) it.copy(enabled = enabled) else it })

    fun hasLiveSurround(): Boolean =
        kotlin.math.abs(surroundMidPct) > 0.5f ||
            kotlin.math.abs(surroundSidePct) > 0.5f ||
            surroundWidthPct > 0.5f

    fun hasAnyActiveProcessing(): Boolean =
        !bypassAll && (
            chain.any { it.enabled } ||
                kotlin.math.abs(masterGainDb) > 0.05f ||
                spatialPanHz > 0.01f ||
                hasLiveSurround() ||
                (vibratoHz > 0.1f && vibratoDepth > 0.01f)
            )
}

@Serializable
data class StoredCustomPreset(
    val id: Long,
    val name: String,
    val description: String,
    val config: DspConfig,
    val createdAt: Long = System.currentTimeMillis(),
    val cloudDocId: String? = null,
)

@Immutable
data class StudioPreset(
    val id: Long,
    val name: String,
    val description: String,
    val isBuiltIn: Boolean,
    val config: DspConfig,
    val cloudDocId: String? = null,
)

@Immutable
@Serializable
data class CloudDraft(
    val docId: String,
    val name: String,
    val selectedPresetName: String,
    val durationMs: Long,
    val sampleRate: Int,
    val config: DspConfig,
    val updatedAtMs: Long,
)

enum class GlobalProPresetId {
    BROADCAST_RADIO,
    AUTOTUNE_SYNTH,
    EPIC_CINEMATIC,
    SURROUND_8D_CINEMA,
    CRYSTAL_VOCAL,
}

data class GlobalProPreset(
    val id: GlobalProPresetId,
    val titleKey: String,
    val descKey: String,
    val ffmpegFilter: String,
    val dspConfig: DspConfig,
)

class StereoAudioBuffer(
    val left: FloatArray,
    val right: FloatArray,
    val sampleRate: Int = 44100,
) {
    val frameCount: Int get() = minOf(left.size, right.size)
    val durationMs: Long get() = if (sampleRate > 0) (frameCount * 1000L) / sampleRate else 0L
}

@Immutable
data class DeviceAudioItem(
    val id: Long,
    val uriString: String,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val extension: String,
    val mimeType: String,
    val folderName: String,
    val dateAddedSec: Long,
)

enum class EchoPadAxisMode {
    SPACE,
    DELAY,
    WIDTH,
    DAMPING,
}

@Serializable
enum class EchoDetailGroup {
    CORE,
    REFLECTIONS,
    SPACE,
}

@Serializable
enum class AiToolType {
    TRANSCRIBE,
    SMART_MASTER,
    SEARCH,
}

enum class ExportAudioFormat(
    val displayName: String,
    val subtitle: String,
    val extension: String,
    val mimeType: String,
) {
    M4A("M4A", "ترميز استوديو عالي الكفاءة (AAC / M4A Container)", "m4a", "audio/mp4"),
    WAV("WAV", "جودة استوديو غير مضغوطة (PCM 16-bit)", "wav", "audio/wav"),
    FLAC("FLAC", "حفظ نقي بدون فقدان الجودة (Lossless)", "flac", "audio/flac"),
    OGG("OGG", "مقطع قياسي مضغوط (Opus / Audio)", "ogg", "audio/ogg"),
    MP3("MP3", "صيغة قياسية عالية التوافق", "mp3", "audio/mpeg"),
}

@Immutable
data class StudioProject(
    val id: Long,
    val name: String,
    val durationMs: Long,
    val sampleRate: Int,
    val config: DspConfig,
    val updatedAt: Long,
    val hasAudio: Boolean,
)

enum class RecordingSourceType {
    RECORDING,
    IMPORTED,
    TTS,
}

@Immutable
data class AudioRecordingItem(
    val id: Long,
    val title: String,
    val timestamp: Long,
    val durationMs: Long,
    val sampleRate: Int,
    val sourceType: RecordingSourceType,
)

@Immutable
data class VoicePurifyPreset(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val subtitleAr: String,
    val subtitleEn: String,
    val highPassHz: Float,
    val lowPassHz: Float,
    val thresholdDb: Float,
    val reductionDb: Float,
    val spectralStrength: Float,
    val deEssAmount: Float,
    val clarityBoostDb: Float,
    val spectralCleanerEnabled: Boolean,
)

val VOICE_PURIFY_PRESETS: List<VoicePurifyPreset> = listOf(
    VoicePurifyPreset(
        id = "studio_clean",
        titleAr = "نقاء استوديو ناعم",
        titleEn = "Soft Studio Clean",
        subtitleAr = "تنقية متوازنة تحفظ دفء الصوت ووضوح الحروف",
        subtitleEn = "Balanced noise removal preserving natural vocal warmth",
        highPassHz = 85f,
        lowPassHz = 15200f,
        thresholdDb = -46f,
        reductionDb = 18f,
        spectralStrength = 0.62f,
        deEssAmount = 0.25f,
        clarityBoostDb = 2.5f,
        spectralCleanerEnabled = true,
    ),
    VoicePurifyPreset(
        id = "deep_isolate",
        titleAr = "عزل فائق للضوضاء",
        titleEn = "Deep Noise Isolate",
        subtitleAr = "قمع قوي لضجيج المكيف، المروحة، وهواء الخلفية",
        subtitleEn = "Strong attenuation of AC hum, fans, and wind noise",
        highPassHz = 115f,
        lowPassHz = 12800f,
        thresholdDb = -40f,
        reductionDb = 24f,
        spectralStrength = 0.85f,
        deEssAmount = 0.30f,
        clarityBoostDb = 3.2f,
        spectralCleanerEnabled = true,
    ),
    VoicePurifyPreset(
        id = "podcast_crisp",
        titleAr = "بودكاست ووضوح الحوار",
        titleEn = "Podcast Vocal Crisp",
        subtitleAr = "بروز فوري للحروف وحماية مخارج الهاء والخاء",
        subtitleEn = "Forward vocal presence with full consonant clarity",
        highPassHz = 95f,
        lowPassHz = 15800f,
        thresholdDb = -44f,
        reductionDb = 20f,
        spectralStrength = 0.70f,
        deEssAmount = 0.25f,
        clarityBoostDb = 4.5f,
        spectralCleanerEnabled = true,
    ),
    VoicePurifyPreset(
        id = "clean_podcast_mic",
        titleAr = "تنقية المايك والبودكاست (Studio Mic)",
        titleEn = "Clean Podcast Mic Pro",
        subtitleAr = "عزل ذكي مع موازنة وحضور صوتي إذاعي متكامل",
        subtitleEn = "Studio denoise, rumble cleanup & crisp vocal presence EQ",
        highPassHz = 85f,
        lowPassHz = 14800f,
        thresholdDb = -40f,
        reductionDb = 22f,
        spectralStrength = 0.80f,
        deEssAmount = 0.30f,
        clarityBoostDb = 3.0f,
        spectralCleanerEnabled = true,
    ),
    VoicePurifyPreset(
        id = "gentle_denoise",
        titleAr = "تصفية خفيفة (حفظ الجو)",
        titleEn = "Gentle Ambient Safe",
        subtitleAr = "تخفيف لطيف للهسهسة دون المساس بنقاء الحديث",
        subtitleEn = "Subtle hiss taming without altering quiet ambience",
        highPassHz = 65f,
        lowPassHz = 16000f,
        thresholdDb = -54f,
        reductionDb = 12f,
        spectralStrength = 0.40f,
        deEssAmount = 0.15f,
        clarityBoostDb = 1.2f,
        spectralCleanerEnabled = true,
    ),
)

@Immutable
data class VoiceCharacterPreset(
    val id: String,
    val titleKey: String,
    val descKey: String,
    val pitchSemitones: Float,
    val formantShift: Float,
    val warmthDb: Float,
    val presenceDb: Float,
    val bassBoostDb: Float = 0f,
    val bassFreqHz: Float = 75f,
    val vibratoHz: Float = 0f,
    val vibratoDepth: Float = 0f,
    val echoDelayMs: Float = 0f,
    val echoDecay: Float = 0f,
    val dualLayerPitchSemitones: Float = 0f,
    val dualLayerFormantShift: Float = 1.0f,
    val dualLayerMix: Float = 0f,
)

val VOICE_CHARACTER_PRESETS: List<VoiceCharacterPreset> = listOf(
    VoiceCharacterPreset("male_luxury", "voice_char_male_luxury", "voice_char_male_luxury_desc", -3.2f, 0.85f, 4.0f, 2.2f),
    VoiceCharacterPreset("male_calm", "voice_char_male_calm", "voice_char_male_calm_desc", -1.8f, 0.91f, 2.5f, 1.5f),
    VoiceCharacterPreset("female_soft", "voice_char_female_soft", "voice_char_female_soft_desc", 3.4f, 1.15f, 1.2f, 2.0f),
    VoiceCharacterPreset("female_balanced", "voice_char_female_balanced", "voice_char_female_balanced_desc", 2.2f, 1.1f, 0.5f, 3.0f),
    VoiceCharacterPreset("child", "voice_char_child", "voice_char_child_desc", 4.85f, 1.26f, 0.2f, 2.2f),
)

enum class TtsGender {
    MALE,
    FEMALE,
}

enum class TtsEngineMode {
    VOX_NEURAL_ONLINE,
}

enum class TtsPersonaAcousticMode(
    val titleAr: String,
    val titleEn: String,
    val descAr: String,
    val descEn: String,
) {
    DOCUMENTARY(
        titleAr = "معلق وثائقي",
        titleEn = "Documentary Narrator",
        descAr = "تعزيز العمق والهيبة الصوتية (110Hz + ضغط استوديو ناعم)",
        descEn = "Deep cinema authority (110Hz EQ + smooth studio compression)",
    ),
    PODCAST(
        titleAr = "بودكاست احترافي",
        titleEn = "Pro Podcast",
        descAr = "دفء المايكروفون القريب ونقاء الحروف (highpass=70 + compand)",
        descEn = "Intimate mic warmth & crisp clarity (highpass=70 + compand)",
    ),
    HALL_ECHO(
        titleAr = "صدى قاعة",
        titleEn = "Hall Echo",
        descAr = "رحابة قاعة فخمة مع صدى متوازن وتردد استوديو نقي",
        descEn = "Spacious hall ambience with clean studio echo",
    ),
    NATURAL(
        titleAr = "إلقاء طبيعي",
        titleEn = "Natural Delivery",
        descAr = "نقاء الصوت الصافي وتصفية التشويش فقط",
        descEn = "Pure natural timbre with noise cleanup only",
    ),
}

enum class TtsDeliveryStyle(
    val titleKey: String,
    val rateFactor: Float,
    val pitchOffsetHz: Int,
    val warmthDb: Float,
    val ssmlExpressAsStyle: String? = null,
    val ssmlStyleDegree: Float = 1.0f,
    val volumeDeltaPercent: Int = 0,
    val breathPauseMs: Int = 210,
) {
    NATURAL_WARM("tts_style_natural", 1.0f, 0, 2.5f, null, 1.0f, 0, 210),
    ENERGETIC("tts_style_energetic", 1.09f, 8, 1.6f, "cheerful", 1.35f, 4, 150),
    SAD("tts_style_sad", 0.86f, -7, 3.4f, "sad", 1.25f, -6, 360),
    ANGRY("tts_style_angry", 1.12f, 6, 1.2f, "angry", 1.4f, 10, 130),
    DRAMATIC("tts_style_dramatic", 0.91f, -6, 3.8f, "dramatic", 1.35f, 2, 380),
    RADIO_ANCHOR("tts_style_radio", 0.96f, -6, 4.0f, "newscast", 1.1f, 2, 190),
    STORYTELLING("tts_style_story", 0.92f, -2, 3.0f, "narration-relaxed", 1.15f, 0, 290),
}

@Immutable
data class TtsVoicePersona(
    val id: String,
    val gender: TtsGender,
    val titleAr: String,
    val titleEn: String,
    val descAr: String,
    val descEn: String,
    val geminiVoiceName: String,
    val defaultStyle: TtsDeliveryStyle,
    val defaultAcousticMode: TtsPersonaAcousticMode,
    val defaultPitchSemitones: Float,
    val defaultFormantShift: Float,
    val defaultWarmthDb: Float,
    val defaultPresenceDb: Float,
    val geminiDirectionPromptAr: String,
)

val TTS_VOICE_PERSONAS: List<TtsVoicePersona> = listOf(
    // قسم نبرات الرجال
    TtsVoicePersona(
        id = "male_doc_deep",
        gender = TtsGender.MALE,
        titleAr = "رجل وثائقي عميق",
        titleEn = "Deep Documentary Male",
        descAr = "إلقاء وثائقي رصين بطيء مع وقفات درامية مهيبة",
        descEn = "Deep, dignified documentary pacing with dramatic pauses",
        geminiVoiceName = "Charon",
        defaultStyle = TtsDeliveryStyle.DRAMATIC,
        defaultAcousticMode = TtsPersonaAcousticMode.DOCUMENTARY,
        defaultPitchSemitones = -3.4f,
        defaultFormantShift = 0.84f,
        defaultWarmthDb = 4.2f,
        defaultPresenceDb = 2.0f,
        geminiDirectionPromptAr = "تحدث بصوت رجل وثائقي عميق وفخم (Charon): إلقاء وثائقي رصين وبطيء نوعاً ما مع وقفات تنفس درامية مدروسة بين الجمل ونبرة تمنح الهيبة والوقار.",
    ),
    TtsVoicePersona(
        id = "male_podcast_warm",
        gender = TtsGender.MALE,
        titleAr = "مقدم بودكاست دافئ",
        titleEn = "Warm Podcast Host",
        descAr = "حديث عفوي ودود وقريب جداً من المايكروفون",
        descEn = "Conversational, warm, and intimate close-mic presence",
        geminiVoiceName = "Orus",
        defaultStyle = TtsDeliveryStyle.NATURAL_WARM,
        defaultAcousticMode = TtsPersonaAcousticMode.PODCAST,
        defaultPitchSemitones = -1.6f,
        defaultFormantShift = 0.92f,
        defaultWarmthDb = 3.0f,
        defaultPresenceDb = 2.4f,
        geminiDirectionPromptAr = "تحدث كأنك مقدم بودكاست دافئ (Orus): حديث بشري عفوي، ودود، قريب من المايكروفون، مريح للأذن مع تفاعل طبيعي وابتسامة خفيفة في الصوت.",
    ),
    TtsVoicePersona(
        id = "male_promo_dramatic",
        gender = TtsGender.MALE,
        titleAr = "معلق درامي وحماسي",
        titleEn = "Dramatic Promo Voice",
        descAr = "توجيه قوي ومؤثر للإعلانات والبرومو والمقدمات الحماسية",
        descEn = "Powerful, impactful delivery built for trailers and promos",
        geminiVoiceName = "Fenrir",
        defaultStyle = TtsDeliveryStyle.ENERGETIC,
        defaultAcousticMode = TtsPersonaAcousticMode.DOCUMENTARY,
        defaultPitchSemitones = -2.2f,
        defaultFormantShift = 0.89f,
        defaultWarmthDb = 2.8f,
        defaultPresenceDb = 3.2f,
        geminiDirectionPromptAr = "تحدث بصوت معلق درامي وحماسي قوي (Fenrir): أداء جهوري مؤثر وحازم يصلح للإعلانات الاحترافية والبرومو السينمائي مع تشديد مخارج الحروف.",
    ),
    TtsVoicePersona(
        id = "male_modern_energetic",
        gender = TtsGender.MALE,
        titleAr = "شاب حيوي وعصري",
        titleEn = "Modern Energetic Young Man",
        descAr = "إيقاع طبيعي سريع ومشرق للمحتوى الحديث والريلز",
        descEn = "Upbeat, crisp modern rhythm ideal for contemporary content",
        geminiVoiceName = "Puck",
        defaultStyle = TtsDeliveryStyle.ENERGETIC,
        defaultAcousticMode = TtsPersonaAcousticMode.PODCAST,
        defaultPitchSemitones = 0.8f,
        defaultFormantShift = 1.03f,
        defaultWarmthDb = 1.6f,
        defaultPresenceDb = 2.8f,
        geminiDirectionPromptAr = "تحدث بصوت شاب حيوي وعصري (Puck): إيقاع كلامي طبيعي وسريع نسبياً ومشرق يلائم المحتوى الإبداعي والبودكاست الشبابي الحديث دون تصنع.",
    ),
    // قسم نبرات النساء
    TtsVoicePersona(
        id = "female_doc_dignified",
        gender = TtsGender.FEMALE,
        titleAr = "امرأة وثائقية وقورة",
        titleEn = "Dignified Documentary Female",
        descAr = "إلقاء هادئ وعميق للتقارير والوثائقيات الرصينة",
        descEn = "Calm, poised, and deep vocal authority for reports & docs",
        geminiVoiceName = "Zephyr",
        defaultStyle = TtsDeliveryStyle.DRAMATIC,
        defaultAcousticMode = TtsPersonaAcousticMode.DOCUMENTARY,
        defaultPitchSemitones = 1.8f,
        defaultFormantShift = 1.08f,
        defaultWarmthDb = 2.6f,
        defaultPresenceDb = 2.2f,
        geminiDirectionPromptAr = "تحدثي بصوت امرأة وثائقية وقورة وعميقة (Zephyr): إلقاء هادئ، رصين، ومتزن مع وقفات تنفس ناعمة تمنح التقارير والأفلام الوثائقية فخامة ومصداقية.",
    ),
    TtsVoicePersona(
        id = "female_podcast_host",
        gender = TtsGender.FEMALE,
        titleAr = "مقدمة بودكاست حوارية",
        titleEn = "Conversational Podcast Host",
        descAr = "نبرة دافئة وطبيعية قريبة ومريحة للاستماع الطويل",
        descEn = "Warm, natural, and inviting tone for engaging podcasts",
        geminiVoiceName = "Aoede",
        defaultStyle = TtsDeliveryStyle.NATURAL_WARM,
        defaultAcousticMode = TtsPersonaAcousticMode.PODCAST,
        defaultPitchSemitones = 2.5f,
        defaultFormantShift = 1.12f,
        defaultWarmthDb = 2.2f,
        defaultPresenceDb = 2.4f,
        geminiDirectionPromptAr = "تحدثي كمقدمة بودكاست حوارية (Aoede): نبرة دافئة، عفوية، وطبيعية جداً قريبة من المستمع ومريحة للاستماع المستمر بدون تكلف.",
    ),
    TtsVoicePersona(
        id = "female_news_anchor",
        gender = TtsGender.FEMALE,
        titleAr = "مذيعة أخبار رسمية",
        titleEn = "Formal News Anchor",
        descAr = "نطق حازم، رصين وواضح المخارج للنشرات الرسمية",
        descEn = "Firm, authoritative pronunciation with broadcast clarity",
        geminiVoiceName = "Kore",
        defaultStyle = TtsDeliveryStyle.RADIO_ANCHOR,
        defaultAcousticMode = TtsPersonaAcousticMode.NATURAL,
        defaultPitchSemitones = 2.0f,
        defaultFormantShift = 1.10f,
        defaultWarmthDb = 1.2f,
        defaultPresenceDb = 3.0f,
        geminiDirectionPromptAr = "تحدثي بصوت مذيعة أخبار رسمية (Kore): نطق حازم ومضبوط تماماً وواضح مخارج الحروف بأسلوب النشرات الإخبارية الاحترافية الرصينة.",
    ),
    TtsVoicePersona(
        id = "female_story_soft",
        gender = TtsGender.FEMALE,
        titleAr = "راوية قصص ناعمة",
        titleEn = "Soft Storyteller",
        descAr = "نبرة عاطفية هادئة ومؤثرة للخواطر والكتب الصوتية",
        descEn = "Gentle, emotional tone crafted for stories & audiobooks",
        geminiVoiceName = "Leda",
        defaultStyle = TtsDeliveryStyle.STORYTELLING,
        defaultAcousticMode = TtsPersonaAcousticMode.PODCAST,
        defaultPitchSemitones = 3.2f,
        defaultFormantShift = 1.15f,
        defaultWarmthDb = 2.8f,
        defaultPresenceDb = 1.8f,
        geminiDirectionPromptAr = "تحدثي بصوت راوية قصص ناعمة (Leda): نبرة عاطفية حالمة وهادئة مع تنفّس لطيف ومؤثر يشد المستمع في الخواطر والروايات والكتب الصوتية.",
    ),
    // أصوات خاصة
    TtsVoicePersona(
        id = "child_voice",
        gender = TtsGender.MALE,
        titleAr = "طفل لطيف وواضح",
        titleEn = "Clear Child Voice",
        descAr = "نبرة طفل حيوية وصافية للقصص التعليمية والمحتوى الإبداعي",
        descEn = "Bright, natural child vocal profile for stories & creative clips",
        geminiVoiceName = "Puck",
        defaultStyle = TtsDeliveryStyle.ENERGETIC,
        defaultAcousticMode = TtsPersonaAcousticMode.NATURAL,
        defaultPitchSemitones = 4.6f,
        defaultFormantShift = 1.24f,
        defaultWarmthDb = 1.0f,
        defaultPresenceDb = 2.6f,
        geminiDirectionPromptAr = "تحدث بصوت طفل لطيف وطبيعي وواضح تماماً.",
    ),
    TtsVoicePersona(
        id = "male_elderly",
        gender = TtsGender.MALE,
        titleAr = "رجل مسن هادئ",
        titleEn = "Elderly Male Narrator",
        descAr = "صوت رجل كبير السن هادئ ورصين للقصص والتعليق الوثائقي",
        descEn = "A mature elderly male voice with measured, dignified narration.",
        geminiVoiceName = "Charon",
        defaultStyle = TtsDeliveryStyle.DRAMATIC,
        defaultAcousticMode = TtsPersonaAcousticMode.DOCUMENTARY,
        defaultPitchSemitones = -3.2f,
        defaultFormantShift = 0.88f,
        defaultWarmthDb = 3.8f,
        defaultPresenceDb = 1.8f,
        geminiDirectionPromptAr = "تحدث بصوت رجل مسن هادئ ورصين.",
    ),
)

data class TtsDialect(
    val id: String,
    val titleKey: String,
    val localeTag: String,
    val fallbackLocaleTag: String,
    val maleVoiceName: String,
    val femaleVoiceName: String,
    val sampleText: String,
    val defaultPitch: Float,
    val defaultRate: Float,
)

val TTS_DIALECTS: List<TtsDialect> = listOf(
    TtsDialect(
        id = "standard",
        titleKey = "tts_dialect_standard",
        localeTag = "ar-SA",
        fallbackLocaleTag = "ar-SA",
        maleVoiceName = "ar-SA-HamedNeural",
        femaleVoiceName = "ar-SA-ZariyahNeural",
        sampleText = "أهلاً بكم في استوديو Sonix ✦ المتطور، تجربة صوتية احترافية بالذكاء الاصطناعي.",
        defaultPitch = 1.0f,
        defaultRate = 0.96f,
    ),
    TtsDialect(
        id = "iraqi",
        titleKey = "tts_dialect_iraqi",
        localeTag = "ar-IQ",
        fallbackLocaleTag = "ar-SA",
        maleVoiceName = "ar-IQ-BasselNeural",
        femaleVoiceName = "ar-IQ-RanaNeural",
        sampleText = "هلا بيكم باستوديو Sonix ✦، شلونكم اليوم؟ خلينا نسجّل صوت نقي ومرتّب.",
        defaultPitch = 0.98f,
        defaultRate = 0.96f,
    ),
    TtsDialect(
        id = "egyptian",
        titleKey = "tts_dialect_egyptian",
        localeTag = "ar-EG",
        fallbackLocaleTag = "ar-SA",
        maleVoiceName = "ar-EG-ShakirNeural",
        femaleVoiceName = "ar-EG-SalmaNeural",
        sampleText = "أهلاً بيكم في استوديو Sonix ✦، صوتكم هنا بيطلع صافي واحترافي جداً.",
        defaultPitch = 1.0f,
        defaultRate = 0.97f,
    ),
    TtsDialect(
        id = "saudi",
        titleKey = "tts_dialect_saudi",
        localeTag = "ar-SA",
        fallbackLocaleTag = "ar",
        maleVoiceName = "ar-SA-HamedNeural",
        femaleVoiceName = "ar-SA-ZariyahNeural",
        sampleText = "حياكم الله في استوديو Sonix ✦، تجربة صوتية احترافية بجودة عالية جداً.",
        defaultPitch = 1.0f,
        defaultRate = 0.96f,
    ),
    TtsDialect(
        id = "syrian",
        titleKey = "tts_dialect_syrian",
        localeTag = "ar-SY",
        fallbackLocaleTag = "ar-LB",
        maleVoiceName = "ar-SY-LaithNeural",
        femaleVoiceName = "ar-SY-AmanyNeural",
        sampleText = "يا مية أهلاً وسهلاً فيكم في Sonix ✦، نورتوا الاستوديو وصوتكم طالع بيشهي وواضح.",
        defaultPitch = 1.02f,
        defaultRate = 0.96f,
    ),
    TtsDialect(
        id = "en_us",
        titleKey = "tts_dialect_english_us",
        localeTag = "en-US",
        fallbackLocaleTag = "en",
        maleVoiceName = "en-US-AndrewNeural",
        femaleVoiceName = "en-US-AvaNeural",
        sampleText = "Welcome to Sonix ✦ Studio. Crystal clear acoustics ready for your next mix.",
        defaultPitch = 1.0f,
        defaultRate = 1.0f,
    ),
    TtsDialect(
        id = "persian",
        titleKey = "tts_dialect_persian",
        localeTag = "fa-IR",
        fallbackLocaleTag = "fa",
        maleVoiceName = "fa-IR-FaridNeural",
        femaleVoiceName = "fa-IR-DilaraNeural",
        sampleText = "سلام و خوش آمدید به استودیوی Sonix ✦؛ صدای شما اکنون با شفافیت کامل آماده است.",
        defaultPitch = 1.0f,
        defaultRate = 0.95f,
    ),
    TtsDialect(
        id = "turkish",
        titleKey = "tts_dialect_turkish",
        localeTag = "tr-TR",
        fallbackLocaleTag = "tr",
        maleVoiceName = "tr-TR-AhmetNeural",
        femaleVoiceName = "tr-TR-EmelNeural",
        sampleText = "Sonix ✦ Stüdyo'ya hoş geldiniz. Sesiniz şimdi kristal netliğinde işlenmeye hazır.",
        defaultPitch = 1.0f,
        defaultRate = 0.98f,
    ),
)

@Immutable
data class SmartMasterAnalysis(
    val rmsDb: Float,
    val peakDb: Float,
    val crestFactorDb: Float,
    val noiseFloorDb: Float,
    val lowEnergyRatio: Float,
    val midEnergyRatio: Float,
    val highEnergyRatio: Float,
    val detectedProfile: String,
    val appliedActions: List<String>,
    val optimizedConfig: DspConfig,
    val aiDiagnosticNotes: String? = null,
    val recommendedFfmpegFilter: String? = null,
    val ffmpegFilterApplied: Boolean = false,
    val isAiCloudAnalyzed: Boolean = false,
)

@Immutable
data class SearchSourceLink(
    val title: String,
    val uri: String,
    val domain: String,
)

@Immutable
data class AiStreamState(
    val isLoading: Boolean = false,
    val outputText: String = "",
    val errorMessage: String? = null,
    val searchQueries: List<String> = emptyList(),
    val sources: List<SearchSourceLink> = emptyList(),
)

@Immutable
data class TtsStudioUiState(
    val selectedEngine: TtsEngineMode = TtsEngineMode.VOX_NEURAL_ONLINE,
    val activeEngineUsed: TtsEngineMode? = null,
    val isSynthesizingPreview: Boolean = false,
    val isPreviewing: Boolean = false,
    val isGenerating: Boolean = false,
    val usedFallbackEngine: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
)

@Immutable
@Serializable
sealed interface AuthSession {
    @Serializable
    data object SignedOut : AuthSession

    @Serializable
    data object Guest : AuthSession

    @Serializable
    data class SignedIn(
        val uid: String,
        val displayName: String,
        val email: String,
    ) : AuthSession
}

@Immutable
data class PlaybackTelemetry(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val outputLevel: Float = 0f,
    val outputDbfs: Float = -60f,
    val monitoringSpeed: Float = 1.0f,
    val gainReductionDb: Float = 0f,
    val noiseSuppressionDb: Float = 0f,
    val waveformBars: List<Float> = List(48) { 0.06f },
    val spectrumBands: List<Float> = List(32) { 0.03f },
)

@Immutable
data class RecorderTelemetry(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedMs: Long = 0L,
    val micLevel: Float = 0f,
    val micDbfs: Float = -60f,
    val waveformBars: List<Float> = List(48) { 0.05f },
    val spectrumBands: List<Float> = List(32) { 0.03f },
)

@Immutable
@Serializable
data class AudioMarker(
    val id: String,
    val positionFrac: Float,
    val label: String,
    val colorIndex: Int = 0,
)

enum class PlatformLoudnessTarget(val targetLufs: Float?, val titleKey: String) {
    ORIGINAL(null, "export_lufs_original"),
    YOUTUBE_TIKTOK_14(-14f, "export_lufs_social_14"),
    PODCAST_16(-16f, "export_lufs_podcast_16"),
    WHATSAPP_VOICE(-15f, "export_lufs_whatsapp"),
}

enum class VocalSplitMode(val titleKey: String) {
    VOCALS_ONLY("vocal_split_vocals"),
    INSTRUMENTAL_ONLY("vocal_split_instrumental"),
    CUSTOM_BLEND("vocal_split_blend"),
}

enum class OneTapSocialPreset(val titleKey: String, val descKey: String) {
    LUXURY_PODCAST("social_preset_podcast", "social_preset_podcast_desc"),
    DOCUMENTARY_NARRATOR("social_preset_doc", "social_preset_doc_desc"),
    VINTAGE_PHONE("social_preset_phone", "social_preset_phone_desc"),
    GRAND_HALL_REVERB("social_preset_hall", "social_preset_hall_desc"),
    FM_RADIO("social_preset_fm", "social_preset_fm_desc"),
}

enum class SleepTimerOption(val minutes: Int?, val labelKey: String) {
    OFF(null, "sleep_timer_off"),
    MIN_15(15, "sleep_timer_15"),
    MIN_30(30, "sleep_timer_30"),
    MIN_60(60, "sleep_timer_60"),
}

@Serializable
enum class OnlineSearchCategory {
    VOICES_CLIPS,
    SOUND_EFFECTS,
}

@Immutable
@Serializable
data class AssistantChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val recommendedToolId: String? = null,
    val recommendedToolLabel: String? = null,
    val timestampMs: Long = 0L,
)

@Immutable
@Serializable
data class SavedAssistantChatSession(
    val id: String,
    val title: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val messages: List<AssistantChatMessage>,
)

enum class BatchFilterPreset(val titleKey: String) {
    NONE("batch_filter_none"),
    VOICE_CLARITY("batch_filter_clarity"),
    BASS_WARMTH("batch_filter_bass"),
    STUDIO_RADIO("batch_filter_radio"),
    ECHO_SPACE("batch_filter_echo"),
    BRIGHT_AIR("batch_filter_air"),
    ACTIVE_STUDIO_DSP("batch_filter_active_dsp"),
}

enum class BatchItemStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED,
}

@Immutable
data class BatchDspSettings(
    val pitchSemitones: Float = 0f,
    val speedRatio: Float = 1.0f,
    val filterPreset: BatchFilterPreset = BatchFilterPreset.VOICE_CLARITY,
    val normalizeOutput: Boolean = true,
    val outputFormat: ExportAudioFormat = ExportAudioFormat.M4A,
)

@Immutable
data class BatchQueueItem(
    val id: String,
    val displayName: String,
    val sourceUriString: String,
    val localFilePath: String? = null,
    val durationMs: Long = 0L,
    val status: BatchItemStatus = BatchItemStatus.QUEUED,
    val itemProgress: Float = 0f,
    val outputFilePath: String? = null,
    val outputFormat: ExportAudioFormat = ExportAudioFormat.M4A,
    val errorMessage: String? = null,
)

@Immutable
data class BatchQueueUiState(
    val items: List<BatchQueueItem> = emptyList(),
    val settings: BatchDspSettings = BatchDspSettings(),
    val isRunning: Boolean = false,
    val currentIndex: Int = -1,
    val overallProgress: Float = 0f,
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val statusSummary: String? = null,
    val previewingItemId: String? = null,
)

