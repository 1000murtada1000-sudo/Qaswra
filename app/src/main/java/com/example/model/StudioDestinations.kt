package com.example.model

import kotlinx.serialization.Serializable

@Serializable
enum class AudioPickerTarget {
    LIBRARY_BROWSE,
    IMPORT_MAIN,
    APPEND_CLIP,
    MIX_BACKGROUND,
}

@Serializable
sealed interface StudioRoute {
    @Serializable
    data object Home : StudioRoute

    @Serializable
    data object Record : StudioRoute

    @Serializable
    data object VoicePurifier : StudioRoute

    @Serializable
    data object EchoSpace : StudioRoute

    @Serializable
    data class EchoDetail(val group: EchoDetailGroup) : StudioRoute

    @Serializable
    data object EffectsChain : StudioRoute

    @Serializable
    data class EffectDetail(val effect: EffectType) : StudioRoute

    @Serializable
    data object Editor : StudioRoute

    @Serializable
    data object Presets : StudioRoute

    @Serializable
    data object ProjectsExport : StudioRoute

    @Serializable
    data object AiStudio : StudioRoute

    @Serializable
    data class AiToolDetail(val tool: AiToolType) : StudioRoute

    @Serializable
    data object AiVoiceStudio : StudioRoute

    @Serializable
    data object VoiceChanger : StudioRoute

    @Serializable
    data object Settings : StudioRoute

    @Serializable
    data class AudioPicker(val target: AudioPickerTarget = AudioPickerTarget.LIBRARY_BROWSE) : StudioRoute

    @Serializable
    data object AudioMerge : StudioRoute

    @Serializable
    data object AudioSplit : StudioRoute

    @Serializable
    data object PitchAndSpeed : StudioRoute

    @Serializable
    data object ReverseAudio : StudioRoute

    @Serializable
    data object SurroundStereo : StudioRoute

    @Serializable
    data object GenerateSilence : StudioRoute

    @Serializable
    data object FormatConverter : StudioRoute

    @Serializable
    data object ExtractVideoAudio : StudioRoute

    @Serializable
    data object CompressAudio : StudioRoute

    @Serializable
    data object RingtoneMaker : StudioRoute

    @Serializable
    data object SpeechToText : StudioRoute

    @Serializable
    data object SmartMusicMixer : StudioRoute

    @Serializable
    data object SmartAssistant : StudioRoute

    @Serializable
    data object ReelsAudioTemplates : StudioRoute

    @Serializable
    data object Notifications : StudioRoute

    @Serializable
    data class NotificationDetail(val entryId: String) : StudioRoute

    @Serializable
    data object AutoSilenceRemover : StudioRoute

    @Serializable
    data object AutoCompressor : StudioRoute

    @Serializable
    data object VocalSplitter : StudioRoute

    @Serializable
    data object BatchQueue : StudioRoute

    @Serializable
    data object DeveloperPanel : StudioRoute
}
