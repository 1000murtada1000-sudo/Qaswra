package com.example.audio.aivoice

import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

object AiVoicePromptBuilder {
    private data class AcousticProfileLock(
        val profileId: String,
        val speakerId: Int,
        val seed: Int,
    )

    private val acousticProfileCache = ConcurrentHashMap<String, AcousticProfileLock>()

    fun lockAcousticProfile(request: AiVoiceSynthRequest): AiVoiceSynthRequest {
        if (request.acousticProfileId.isNotBlank()) return request

        val profileKey = listOf(
            request.engine.id,
            request.voiceCharacterId,
            request.fixedSid,
            request.fixedSeed,
            request.category.id,
            request.language.code,
            request.dialect.id,
            request.englishAccent.id,
            request.voiceDescriptionPrompt.trim(),
            request.emotion.id,
            request.emotionIntensity,
            request.pace.id,
            request.speechRate,
            request.pitchOffsetSemitones,
            request.fixedFormantShift,
            request.fixedEqLowDb,
            request.fixedEqPresenceDb,
            request.vocalStyle.id,
            request.breathCadence.id,
            request.pitchContour.id,
            request.timbre.id,
            request.articulationClarity,
            request.referenceAudioPath.orEmpty(),
            request.referenceTranscript.trim(),
        ).joinToString("\u001f")
        val profile = acousticProfileCache.computeIfAbsent(profileKey) {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(profileKey.toByteArray(Charsets.UTF_8))
            val profileId = digest.take(12).joinToString("") { byte ->
                (byte.toInt() and 0xff).toString(16).padStart(2, '0')
            }
            AcousticProfileLock(
                profileId = profileId,
                speakerId = request.fixedSid,
                seed = request.fixedSeed,
            )
        }
        return request.copy(
            fixedSid = profile.speakerId,
            fixedSeed = profile.seed,
            acousticProfileId = profile.profileId,
        )
    }

    fun normalizeTargetText(text: String, language: AiVoiceLanguage, dialect: AiArabicDialect): String {
        val cleanText = text
            .replace(Regex("</?[A-Za-z][A-Za-z0-9:_-]*(?:\\s+[^<>]*?)?\\s*/?>"), " ")
            .trim()
            .replace(Regex("[\\t\\x0B\\f\\r ]+"), " ")
            .replace(Regex("\\n{2,}"), "\n")
        if (language != AiVoiceLanguage.AR) return cleanText
        return when (dialect) {
            AiArabicDialect.MSA -> cleanText
            AiArabicDialect.IRAQI, AiArabicDialect.SAUDI -> cleanText.replace('ق', 'گ')
            AiArabicDialect.EGYPTIAN -> cleanText.replace('ج', 'گ')
            AiArabicDialect.SYRIAN -> cleanText.replace('ق', 'ء')
        }
    }

    /**
     * Builds deterministic Style Conditioning instruction for VoxCPM2.
     */
    fun buildVoxCpmInstruction(request: AiVoiceSynthRequest): String {
        val parts = mutableListOf<String>()

        val profile = request.voiceDescriptionPrompt.lowercase()
        val catHint = when {
            request.category == AiVoiceCategory.CHILD -> "child speaker"
            request.category == AiVoiceCategory.OLDER_ADULT -> "elderly speaker"
            "child" in profile || "طفل" in profile -> "child speaker"
            "elder" in profile || "مسن" in profile -> "elderly male speaker"
            request.category == AiVoiceCategory.MALE -> "male adult speaker"
            else -> "female adult speaker"
        }
        parts.add(catHint)
        parts.add("speaking ${request.language.titleEn}")
        if (request.language == AiVoiceLanguage.AR) {
            parts.add("Arabic dialect: ${request.dialect.promptHintEn}")
        }
        if (request.language == AiVoiceLanguage.EN) {
            parts.add("English accent: ${request.englishAccent.titleEn}")
        }
        parts.add("Natural human voice; apply the requested speech pace during synthesis without post-generation time stretching")

        val intensityLabel = when {
            request.emotionIntensity >= 0.80f -> "strong intense"
            request.emotionIntensity >= 0.45f -> "clear balanced"
            else -> "subtle gentle"
        }

        parts.add("with $intensityLabel ${request.emotion.voxDesignHintEn}")
        parts.add("Vocal style: ${request.vocalStyle.instructionEn}")
        parts.add("Breath and pause cadence: ${request.breathCadence.instructionEn}")
        parts.add("Pitch contour: ${request.pitchContour.instructionEn}")
        parts.add(
            "Natural pitch offset: ${"%.2f".format(java.util.Locale.US, request.pitchOffsetSemitones)} semitones from the character's base voice",
        )
        parts.add("Resonance: ${request.timbre.instructionEn}")
        val clarity = request.articulationClarity.coerceIn(0f, 1f)
        parts.add(
            "Articulation clarity: ${
                when {
                    clarity >= 0.75f -> "crisp and precise"
                    clarity <= 0.30f -> "soft and relaxed"
                    else -> "clear and natural"
                }
            }",
        )
        parts.add("at ${request.pace.promptHintEn} (rate=${"%.2f".format(java.util.Locale.US, request.speechRate)}x)")
        parts.add(
            "Keep the same acoustic voice identity across all chunks and text edits; " +
                "do not change the speaker's age, accent, or timbre.",
        )

        val userPrompt = request.voiceDescriptionPrompt.trim()
        if (userPrompt.isNotEmpty()) {
            parts.add("Role profile: $userPrompt")
        }

        return parts.joinToString(", ")
    }

    /**
     * Builds the deterministic JSON payload expected by the Sonix GPU Inference Backend (`/v1/voice-studio/synthesize`).
     */
    fun buildBackendPayloadJson(
        request: AiVoiceSynthRequest,
        textChunk: String,
        referenceAudioBase64: String? = null,
    ): JSONObject {
        val profileRequest = lockAcousticProfile(request)
        val hasReferenceAudio = !referenceAudioBase64.isNullOrBlank()
        val voxInstruction = if (hasReferenceAudio) "" else buildVoxCpmInstruction(profileRequest)
        val processedText = normalizeTargetText(textChunk, profileRequest.language, profileRequest.dialect)

        return JSONObject().apply {
            put("engine", profileRequest.engine.id)
            put("model_repo", profileRequest.engine.modelRepoId)
            put("text", processedText)
            put("raw_text", processedText)
            put("language", profileRequest.language.code)
            put("dialect", profileRequest.dialect.id)
            put("dialect_hint", profileRequest.dialect.localeTag)
            put("english_accent", profileRequest.englishAccent.id)
            put("category", profileRequest.category.id)
            put("voice_mode", if (hasReferenceAudio) "clone" else "design")
            put("voice_description", if (hasReferenceAudio) "" else profileRequest.voiceDescriptionPrompt.trim())
            put("style_instruction", voxInstruction)
            put("emotion", profileRequest.emotion.id)
            put("emotion_intensity", profileRequest.emotionIntensity.toDouble())
            put("pace", profileRequest.pace.id)
            put("speaking_rate", profileRequest.speechRate.toDouble())
            put("pitch_semitones", profileRequest.pitchOffsetSemitones.toDouble())
            put("seed", profileRequest.fixedSeed)
            put("randomize_seed", false)
            put("fixed_sid", profileRequest.fixedSid)
            put("speaker_id", profileRequest.fixedSid)
            put("acoustic_profile_id", profileRequest.acousticProfileId)
            put("cfg_value", profileRequest.cfgValue.toDouble())
            put("inference_timesteps", profileRequest.inferenceTimesteps)
            put("normalize", profileRequest.normalizeAudio)
            if (hasReferenceAudio) {
                put("reference_audio_base64", referenceAudioBase64)
                if (profileRequest.referenceTranscript.isNotBlank()) {
                    put("reference_transcript", profileRequest.referenceTranscript.trim())
                }
            }
        }
    }

    /**
     * Splits long input scripts on sentence/clause boundaries cleanly.
     */
    fun splitTextIntoChunks(text: String, maxCharsPerChunk: Int = 260): List<String> {
        val normalized = text.trim()
            .replace(Regex("[\\t\\x0B\\f\\r ]+"), " ")
            .replace(Regex("\\n{2,}"), "\n")
        if (normalized.isEmpty()) return emptyList()
        val phraseBoundary = Regex("(?<=[.!؟?!؛،,])\\s+|\\n+")
        if (normalized.length <= maxCharsPerChunk && !phraseBoundary.containsMatchIn(normalized)) {
            return listOf(normalized)
        }

        val sentences = normalized.split(phraseBoundary)
        val chunks = mutableListOf<String>()
        val current = StringBuilder()

        for (sentence in sentences) {
            val s = sentence.trim()
            if (s.isEmpty()) continue
            if (s.length > maxCharsPerChunk) {
                if (current.isNotEmpty()) {
                    chunks.add(current.toString().trim())
                    current.clear()
                }
                val words = s.split(" ")
                for (w in words) {
                    if (current.length + w.length + 1 > maxCharsPerChunk && current.isNotEmpty()) {
                        chunks.add(current.toString().trim())
                        current.clear()
                    }
                    if (current.isNotEmpty()) current.append(' ')
                    current.append(w)
                }
            } else {
                if (current.length + s.length + 1 > maxCharsPerChunk && current.isNotEmpty()) {
                    chunks.add(current.toString().trim())
                    current.clear()
                }
                if (current.isNotEmpty()) current.append(' ')
                current.append(s)
            }
            if (
                (s.endsWith('،') || s.endsWith(',') || s.endsWith('؛') || s.endsWith(';')) &&
                current.isNotEmpty()
            ) {
                chunks.add(current.toString().trim())
                current.clear()
            }
        }
        if (current.isNotEmpty()) {
            chunks.add(current.toString().trim())
        }
        return chunks
    }
}
