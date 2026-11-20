package com.example.viewmodel

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.SonixCrashGuard
import com.example.audio.AudioDecodeOutcome
import com.example.audio.AudioDecoder
import com.example.audio.AudioFileUploader
import com.example.audio.ExclusiveAudioController
import com.example.audio.AudioPlayerManager
import com.example.audio.AudioSignalAnalyzer
import com.example.audio.AudioUploadOutcome
import com.example.audio.ExportedAudioResult
import com.example.audio.LARGE_AUDIO_THRESHOLD_BYTES
import com.example.audio.MergeStudioMode
import com.example.audio.MergeStudioParams
import com.example.audio.NativeSpeechState
import com.example.audio.OnlineGuard
import com.example.audio.ReelsTemplateParams
import com.example.audio.SileroVadProcessor
import com.example.audio.SttBackendMode
import com.example.audio.VadSpeechSegment
import com.example.audio.ai.LocalModelStatus
import com.example.audio.ai.LocalNeuralVoiceEngine
import com.example.audio.ai.OnlineSoundHistoryItem
import com.example.audio.ai.OnlineSoundItem
import com.example.audio.ai.SmartAssistantAndSearch
import com.example.audio.ai.SonixUnifiedAIEngine
import com.example.audio.ai.ToolGuideResult
import com.example.audio.aivoice.AiArabicDialect
import com.example.audio.aivoice.AiEnglishAccent
import com.example.audio.aivoice.AiBreathCadence
import com.example.audio.aivoice.AiDeliveryPace
import com.example.audio.aivoice.AiDialogueTurn
import com.example.audio.aivoice.AiPitchContour
import com.example.audio.aivoice.AiStudioPreset
import com.example.audio.aivoice.AiVoiceAudioStitcher
import com.example.audio.aivoice.AiVoiceCategory
import com.example.audio.aivoice.AiVoiceCharacter
import com.example.audio.aivoice.AiVoiceEmotion
import com.example.audio.aivoice.AiVoiceEngineKind
import com.example.audio.aivoice.AiVoiceLanguage
import com.example.audio.aivoice.AiVoiceStudioCoordinator
import com.example.audio.aivoice.AiVoiceStudioUiState
import com.example.audio.aivoice.AiVoiceSynthOutcome
import com.example.audio.aivoice.AiVoiceSynthRequest
import com.example.audio.aivoice.AiVoiceTimbre
import com.example.audio.aivoice.AiVocalStyle
import com.example.audio.aivoice.FIXED_DETERMINISTIC_SEED
import com.example.audio.aivoice.defaultVocalStyle
import com.example.audio.fx.StudioLuxuryAudioFilter
import com.example.audio.fx.VoiceEffect
import com.example.audio.fx.VoiceProcessor
import com.example.audio.network.NetworkImmunityManager
import com.example.audio.network.NetworkFetchOutcome
import com.example.audio.processAudioFileWithFfmpeg
import com.example.audio.processBatchItemWithDsp
import com.example.audio.processReelsAudioTemplate
import com.example.audio.stageLargeFileToCleanAsciiCache
import com.example.audio.tts.FreeEmotionalTtsClient
import com.example.audio.tts.SmartVoiceRouter
import com.example.audio.tts.TtsTextLimits
import com.example.audio.tts.VoiceRouteOutcome
import com.example.audio.unity.UnityExporter
import com.example.audio.web.ArticleExtractionResult
import com.example.audio.web.WebArticleExtractor
import com.example.audio.RingtoneSaveResult
import com.example.audio.SonixMediaPlaybackService
import com.example.audio.StudioAudioPlayer
import com.example.audio.StudioAudioRecorder
import com.example.audio.StudioPlaybackBridge
import com.example.audio.StudioSpeechTranscriber
import com.example.audio.analyzeAndOptimizeAudio
import com.example.audio.applyAutoCompressorLimiter
import com.example.audio.applyFadeIn
import com.example.audio.applyFadeOut
import com.example.audio.applyGainDb
import com.example.audio.applyOneTapSocialPreset
import com.example.audio.applyOptionalSmartAutoFadeOut
import com.example.audio.applyPitchAndSpeedFileWithFfmpeg
import com.example.audio.applyPitchAndSpeedOffline
import com.example.audio.cache.SmartCacheManager
import com.example.audio.clamp
import com.example.audio.computeMergeTimelineLayout
import com.example.audio.computeWaveformPeaksAsync
import com.example.audio.convertToSurroundStereoFromOffsets
import com.example.audio.estimateStereoCompatibilityForOffsets
import com.example.audio.analyzeStereoCompatibility
import com.example.audio.buildSurroundStereoFfmpegFilter
import com.example.audio.StereoCompatibilityReport
import com.example.audio.cutSelection
import com.example.audio.cutSelectionWithFade
import com.example.audio.executeUnifiedMergeOverlay
import com.example.audio.exportRenderedAudioToFile
import com.example.audio.exportSourceFileDirectlyWithFfmpeg
import com.example.audio.formatDurationMs
import com.example.audio.SilenceInsertMode
import com.example.audio.generateSilence
import com.example.audio.insertSilenceStreaming
import com.example.audio.mergeClips
import com.example.audio.mergeClipsStreamingOnDisk
import com.example.audio.mixAudioFilesWithFfmpeg
import com.example.audio.mixTracks
import com.example.audio.mixWithSidechainAutoDucking
import com.example.audio.normalizeAudio
import com.example.audio.purifyBufferOffline
import com.example.audio.removeSilenceAuto
import com.example.audio.renderGlobalProPreset
import com.example.audio.renderOfflineDsp
import com.example.audio.reverseAudio
import com.example.audio.reverseAudioWithFfmpeg
import com.example.audio.saveAndSetRingtone
import com.example.audio.splitAudioByTime
import com.example.audio.splitStereoChannels
import com.example.audio.splitVocalAndInstrumentalFile
import com.example.audio.splitVocalAndInstrumental
import com.example.audio.transformVoiceOffline as runTransformVoiceOffline
import com.example.audio.trimToSelection
import com.example.audio.trimToSelectionWithFade
import com.example.audio.writeStereoWavFile
import kotlinx.coroutines.sync.Mutex
import com.example.data.AuthResult
import com.example.data.MediaStoreAudioRepository
import com.example.data.RecentEditEntry
import com.example.data.RecentEditsCacheManager
import com.example.data.StudioRepository
import com.example.data.ai.AiRepository
import com.example.data.ai.AiResponse
import com.example.data.ai.SonixUnifiedAiRepository
import com.example.data.remote.FirestoreStudioRepository
import com.example.devpanel.AdminTestType
import com.example.devpanel.CreditTransactionRecord
import com.example.devpanel.CreditType
import com.example.devpanel.DevChangelogCategory
import com.example.devpanel.DevChangelogStatus
import com.example.devpanel.DeveloperPanelController
import com.example.devpanel.DeveloperPanelTab
import com.example.devpanel.DeveloperPanelUiState
import com.example.devpanel.FinanceEntryType
import com.example.devpanel.PaymentStatus
import com.example.devpanel.PaymentType
import com.example.diagnostics.AppDiagnosticEntry
import com.example.diagnostics.AppDiagnosticLog
import com.example.i18n.t
import com.example.model.AiStreamState
import com.example.model.AppLanguage
import com.example.model.AssistantChatMessage
import com.example.model.AudioMarker
import com.example.model.AudioRecordingItem
import com.example.model.AuthSession
import com.example.model.BUILT_IN_PRESETS
import com.example.model.BatchDspSettings
import com.example.model.BatchFilterPreset
import com.example.model.BatchItemStatus
import com.example.model.BatchQueueItem
import com.example.model.BatchQueueUiState
import com.example.model.CloudDraft
import com.example.model.DeviceAudioItem
import com.example.model.DspConfig
import com.example.model.EchoPadAxisMode
import com.example.model.EchoReverbParams
import com.example.model.EchoSpacePreset
import com.example.model.EffectType
import com.example.model.ExportAudioFormat
import com.example.model.GlobalProPreset
import com.example.model.OneTapSocialPreset
import com.example.model.OnlineSearchCategory
import com.example.model.PlatformLoudnessTarget
import com.example.model.PlaybackTelemetry
import com.example.model.RecorderTelemetry
import com.example.model.RecordingSourceType
import com.example.model.SavedAssistantChatSession
import com.example.model.SleepTimerOption
import com.example.model.SmartMasterAnalysis
import com.example.model.StereoAudioBuffer
import com.example.model.StudioPreset
import com.example.model.StudioProject
import com.example.model.StudioRoute
import com.example.model.TtsDeliveryStyle
import com.example.model.TtsDialect
import com.example.model.TtsEngineMode
import com.example.model.TtsGender
import com.example.model.TtsPersonaAcousticMode
import com.example.model.TtsStudioUiState
import com.example.model.TtsVoicePersona
import com.example.model.VocalSplitMode
import com.example.model.VoiceCharacterPreset
import com.example.model.VoicePurifyPreset
import com.example.network.AppUpdateCheckResult
import com.example.network.AppUpdateManager
import com.example.network.AppUpdateOutcome
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

sealed interface AccountDeletionResult {
    data object Success : AccountDeletionResult
    data object RecentLoginRequired : AccountDeletionResult
    data object NetworkError : AccountDeletionResult
    data object Failed : AccountDeletionResult
}

sealed interface StudioSideEffect {
    data class PromptSaveDocument(val result: ExportedAudioResult, val suggestedName: String) : StudioSideEffect
    data class PromptPickSaveFolderOnce(val result: ExportedAudioResult, val suggestedName: String) : StudioSideEffect
    data class LaunchShareIntent(
        val uri: Uri,
        val mimeType: String,
        val title: String,
        val targetPackage: String? = null,
    ) : StudioSideEffect
    data class LaunchMultiShareIntent(
        val uris: List<Uri>,
        val mimeType: String,
        val title: String,
    ) : StudioSideEffect
}

@Immutable
data class StudioUiState(
    val language: AppLanguage = AppLanguage.AR,
    val authSession: AuthSession = AuthSession.SignedOut,
    val hasAudio: Boolean = false,
    val durationMs: Long = 0L,
    val waveformPeaks: List<Float> = emptyList(),
    val dspConfig: DspConfig = DspConfig(),
    val activePresetId: Long? = null,
    val selectedPresetLabel: String = "",
    val isSimpleEchoMode: Boolean = true,
    val echoPadAxisMode: EchoPadAxisMode = EchoPadAxisMode.SPACE,
    val isHoldBypassing: Boolean = false,
    val playback: PlaybackTelemetry = PlaybackTelemetry(),
    val recorder: RecorderTelemetry = RecorderTelemetry(),
    val micPermissionError: String? = null,
    val trimStart: Float = 0f,
    val trimEnd: Float = 1f,
    val clipGainDb: Float = 0f,
    val loopSelection: Boolean = false,
    val autoLoopOnPlayback: Boolean = false,
    val canUndoEdit: Boolean = false,
    val canRedoEdit: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val projects: List<StudioProject> = emptyList(),
    val cloudDrafts: List<CloudDraft> = emptyList(),
    val activeCloudDraftId: String? = null,
    val isCloudSynced: Boolean = false,
    val isCloudSyncing: Boolean = false,
    val cloudSyncLabel: String = "",
    val previewWavPath: String? = null,
    val recordings: List<AudioRecordingItem> = emptyList(),
    val customPresets: List<StudioPreset> = emptyList(),
    val activeProjectId: Long = 0L,
    val activeProjectName: String = "مشروع صوتي جديد",
    val mixBgName: String? = null,
    val mixBgDurationMs: Long = 0L,
    val mixBgPeaks: List<Float> = emptyList(),
    val transcriptionState: AiStreamState = AiStreamState(),
    val smartMasterState: SmartMasterAnalysis? = null,
    val searchAssistantState: AiStreamState = AiStreamState(),
    val ttsUiState: TtsStudioUiState = TtsStudioUiState(),
    val deviceAudios: List<DeviceAudioItem> = emptyList(),
    val isScanningDeviceAudio: Boolean = false,
    val statusBanner: String? = null,
    val statusBannerRepeatCount: Int = 1,
    val isBusy: Boolean = false,
    val isProcessing: Boolean = false,
    val importProgress: Float? = null,
    val importLabel: String? = null,
    val importBytesLabel: String? = null,
    val importBytesProgress: Float? = null,
    val exportProgress: Float? = null,
    val isExporting: Boolean = false,
    val exportComplete: Boolean = false,
    val exportFileName: String? = null,
    val exportSaveLocation: String? = null,
    val purifyProgress: Float? = null,
    val ttsEngineMode: TtsEngineMode = TtsEngineMode.VOX_NEURAL_ONLINE,
    val studioLuxuryEnhancement: Boolean = true,
    val homeScrollIndex: Int = 0,
    val homeScrollOffset: Int = 0,
    val speechState: NativeSpeechState = NativeSpeechState(),
    val sleepTimerOption: SleepTimerOption = SleepTimerOption.OFF,
    val sleepTimerRemainingSec: Int = 0,
    val smartMixBgVolume: Float = 0.25f,
    val isSmartMixPreviewActive: Boolean = false,
    val isLiveVoiceChangerActive: Boolean = false,
    val liveVoiceEffect: VoiceEffect = VoiceEffect.CHILD_NATURAL,
    val isExtractingArticle: Boolean = false,
    val isLoadingAiGuide: Boolean = false,
    val assistantAiSummary: String? = null,
    val assistantGuideOfflineError: String? = null,
    val assistantGuideResults: List<ToolGuideResult> = emptyList(),
    val assistantChatMessages: List<AssistantChatMessage> = emptyList(),
    val activeAssistantChatId: String? = null,
    val savedAssistantChats: List<SavedAssistantChatSession> = emptyList(),
    val onlineSearchCategory: OnlineSearchCategory = OnlineSearchCategory.VOICES_CLIPS,
    val soundSearchOfflineError: String? = null,
    val onlineSoundCategories: Map<String, List<OnlineSoundItem>> = emptyMap(),
    val favoriteOnlineSounds: List<OnlineSoundItem> = emptyList(),
    val onlineListeningHistory: List<OnlineSoundHistoryItem> = emptyList(),
    val isSearchingOnlineSounds: Boolean = false,
    val previewingOnlineUrl: String? = null,
    val loadingOnlineSoundId: String? = null,
    val defaultSaveFolderName: String? = null,
    val originalPeakDbfs: Float = -96f,
    val fricativeWarningPurifier: Boolean = false,
    val fricativeWarningEq: Boolean = false,
    val pendingExtractVideoUri: String? = null,
    val diagnosticEntries: List<AppDiagnosticEntry> = emptyList(),
    val unreadDiagnosticCount: Int = 0,
    val recentEdits: List<RecentEditEntry> = emptyList(),
    val isToolPreviewPlaying: Boolean = false,
    val restoredToolSession: RecentEditEntry? = null,
    val lockedSliders: Set<String> = emptySet(),
    val audioMarkers: List<AudioMarker> = emptyList(),
    val monitoringSpeed: Float = 1.0f,
    val lastAppliedToolLabel: String? = null,
    val selectedExportLoudness: PlatformLoudnessTarget = PlatformLoudnessTarget.ORIGINAL,
    val isOnlineTaskRunning: Boolean = false,
    val onlineTaskLabel: String = "",
    val updateCheckLoading: Boolean = false,
    val updateCheckResult: AppUpdateCheckResult? = null,
    val updateCheckError: String? = null,
    val batchQueueState: BatchQueueUiState = BatchQueueUiState(),
    val localModelStatus: LocalModelStatus = LocalModelStatus(),
    val isVoiceAiConverting: Boolean = false,
    val voiceAiProgress: Float? = null,
    val voiceAiStatusNote: String? = null,
    val smartMasterAnalyzingAi: Boolean = false,
    val transcriptionProgressLabel: String? = null,
    val vadSpeechSegments: List<VadSpeechSegment> = emptyList(),
    val vadUsedSilero: Boolean = false,
) {
    val allPresets: List<StudioPreset> get() = customPresets + BUILT_IN_PRESETS
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StudioRepository(application)
    private val firestoreRepo = FirestoreStudioRepository(application)
    private val aiRepository: AiRepository = SonixUnifiedAiRepository(application)
    private val smartVoiceRouter = SmartVoiceRouter()
    private val voiceProcessor = VoiceProcessor()
    private var ttsPreviewTempFile: File? = null
    private var toolPreviewBuffer: StereoAudioBuffer? = null
    private var toolPreviewPlaybackFile: File? = null
    private var toolPreviewFileDurationMs: Long = 0L
    private var toolPreviewFileSampleRate: Int = 44100
    private var toolPreviewDeleteFileOnStop: Boolean = false
    private var toolPreviewOriginalHasAudio: Boolean? = null
    private var toolPreviewOriginalDurationMs: Long? = null
    private var onlinePreviewFile: File? = null
    private val speechTranscriber = StudioSpeechTranscriber(application)

    private val safeCoroutineHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is CancellationException) return@CoroutineExceptionHandler
        Log.e("StudioVM", "Intercepted coroutine exception", throwable)
        AppDiagnosticLog.logRecoverable(
            toolName = "Background Task",
            titleAr = "تم تجاوز خطأ في مهمة خلفية",
            titleEn = "Background Task Error Bypassed",
            causeAr = throwable.localizedMessage ?: throwable.javaClass.simpleName,
            causeEn = throwable.localizedMessage ?: throwable.javaClass.simpleName,
            errorCode = throwable.javaClass.simpleName,
            throwable = throwable,
        )
        _uiState.update {
            it.copy(
                isBusy = false,
                isProcessing = false,
                isOnlineTaskRunning = false,
                onlineTaskLabel = "",
                isVoiceAiConverting = false,
                voiceAiProgress = null,
                smartMasterAnalyzingAi = false,
                importProgress = null,
                importLabel = null,
                importBytesLabel = null,
                importBytesProgress = null,
                exportProgress = null,
                purifyProgress = null,
                isToolPreviewPlaying = false,
                ttsUiState = it.ttsUiState.copy(
                    isSynthesizingPreview = false,
                    isPreviewing = false,
                    isGenerating = false,
                ),
            )
        }
        postBanner(throwable.localizedMessage ?: "تم تجاوز خطأ تقني وتسجيله في الإشعارات")
    }

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private val aiVoiceCoordinator = AiVoiceStudioCoordinator()
    private val _aiVoiceUiState = MutableStateFlow(AiVoiceStudioUiState())
    val aiVoiceUiState: StateFlow<AiVoiceStudioUiState> = _aiVoiceUiState.asStateFlow()

    private val developerPanelController = DeveloperPanelController(
        app = application,
        scope = viewModelScope,
        coordinator = aiVoiceCoordinator,
        getBackendUrl = { _aiVoiceUiState.value.backendServerUrl },
        getBackendBearerToken = { _aiVoiceUiState.value.backendBearerToken },
    )
    val developerPanelUiState: StateFlow<DeveloperPanelUiState> = developerPanelController.uiState

    // Dedicated high-frequency telemetry streams to isolate 60/120 FPS visualizers & playheads
    // from triggering full-screen StudioUiState recompositions every audio buffer frame.
    private val _playbackTelemetry = MutableStateFlow(PlaybackTelemetry())
    val playbackTelemetry: StateFlow<PlaybackTelemetry> = _playbackTelemetry.asStateFlow()

    private val _recorderTelemetry = MutableStateFlow(RecorderTelemetry())
    val recorderTelemetry: StateFlow<RecorderTelemetry> = _recorderTelemetry.asStateFlow()

    private val _sideEffects = MutableSharedFlow<StudioSideEffect>(extraBufferCapacity = 8)
    val sideEffects = _sideEffects.asSharedFlow()

    val homeListState: LazyListState = LazyListState()

    fun updateHomeScrollPosition(index: Int, offset: Int) {
        val cur = _uiState.value
        if (cur.homeScrollIndex != index || cur.homeScrollOffset != offset) {
            _uiState.update { it.copy(homeScrollIndex = index, homeScrollOffset = offset) }
        }
    }

    private data class EditorSnapshot(
        val buffer: StereoAudioBuffer?,
        val dspConfig: DspConfig,
        val activePresetId: Long?,
        val selectedPresetLabel: String,
        val waveformPeaks: List<Float> = emptyList(),
    )

    private data class LiveToolSessionSnapshot(
        val toolKey: String,
        val dspConfig: DspConfig,
        val clipGainDb: Float,
        val activePresetId: Long?,
        val selectedPresetLabel: String,
        val undoSize: Int,
        val wasPlayingOnEntry: Boolean,
        var committed: Boolean = false,
    )

    private var activeBuffer: StereoAudioBuffer? = null
    private var activeSourceFile: File? = null
    private var activeSourceSampleRate: Int = 44100
    private var mixBgBuffer: StereoAudioBuffer? = null
    private var mixBgSourceFile: File? = null
    @Volatile
    private var pitchSpeedLiveModeActive: Boolean = false
    private val processingMutex = Mutex()
    private val undoStack = ArrayDeque<EditorSnapshot>()
    private val redoStack = ArrayDeque<EditorSnapshot>()
    private val maxUndoPcmBytes = 96L * 1024L * 1024L
    private var lastDspSnapshotTimeMs: Long = 0L
    private var activeLiveToolSession: LiveToolSessionSnapshot? = null
    private var livePreviewStartedPlayback: Boolean = false
    private var preEntryClipGainDb: Float = 0f
    private var clipGainUnappliedDirty: Boolean = false
    @Volatile private var editorPlaybackBoundsEnabled: Boolean = false

    private fun hasGlobalToolPreview(): Boolean =
        toolPreviewBuffer != null || toolPreviewPlaybackFile != null

    private fun hasLoadedAudio(): Boolean {
        if (toolPreviewBuffer != null) return true
        if (toolPreviewPlaybackFile != null) return toolPreviewPlaybackFile?.exists() == true
        val buf = activeBuffer
        if (buf != null && buf.frameCount > 0) return true
        val file = activeSourceFile ?: return false
        val path = file.absolutePath
        return path.isNotBlank() && file.exists() && file.length() > 0L
    }

    fun canStartPlayback(): Boolean = hasLoadedAudio()

    fun setEditorPlaybackBoundsEnabled(enabled: Boolean) {
        editorPlaybackBoundsEnabled = enabled
        val state = _uiState.value
        player.setPlaybackBounds(enabled, state.trimStart, state.trimEnd)
        if (!enabled) {
            player.setLoop(state.loopSelection || state.autoLoopOnPlayback, state.trimStart, state.trimEnd)
        }
    }

    private inline fun launchSingleFlightProcessing(
        purifyInitProgress: Float? = null,
        exportInitProgress: Float? = null,
        trackExport: Boolean = false,
        notifyCompletion: Boolean = true,
        crossinline block: suspend () -> Unit,
    ) {
        if (!processingMutex.tryLock()) {
            if (trackExport) cancelExportTracking()
            return
        }
        try {
            viewModelScope.launch(safeCoroutineHandler) {
                val bannerBefore = _uiState.value.statusBanner
                val bannerRepeatCountBefore = _uiState.value.statusBannerRepeatCount
                _uiState.update {
                    it.copy(
                        isBusy = true,
                        isProcessing = true,
                        purifyProgress = purifyInitProgress ?: it.purifyProgress,
                        exportProgress = exportInitProgress ?: it.exportProgress,
                        isExporting = if (trackExport) true else it.isExporting,
                        exportComplete = if (trackExport) false else it.exportComplete,
                        exportSaveLocation = if (trackExport) null else it.exportSaveLocation,
                    )
                }
                try {
                    block()
                    val completedBanner = _uiState.value.statusBanner
                    if (
                        notifyCompletion &&
                        completedBanner != null &&
                        (
                            completedBanner != bannerBefore ||
                                _uiState.value.statusBannerRepeatCount != bannerRepeatCountBefore
                            ) &&
                        (
                            completedBanner.startsWith("تم") ||
                                completedBanner.startsWith("اكتمل") ||
                                completedBanner.startsWith("اكتملت") ||
                                completedBanner.startsWith("Saved", ignoreCase = true) ||
                                completedBanner.startsWith("Completed", ignoreCase = true)
                            )
                    ) {
                        notifyAudioCompletionIfBackground(
                            title = if (_uiState.value.language == AppLanguage.EN) "Audio operation complete" else "اكتملت معالجة الصوت",
                            message = completedBanner,
                        )
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (t: Throwable) {
                    if (trackExport) cancelExportTracking()
                    AppDiagnosticLog.logRecoverable(
                        toolName = "AudioProcessing",
                        titleAr = "تعذر إكمال المعالجة الصوتية",
                        titleEn = "Audio Processing Error",
                        causeAr = t.localizedMessage ?: t.javaClass.simpleName,
                        causeEn = t.localizedMessage ?: t.javaClass.simpleName,
                        errorCode = t.javaClass.simpleName,
                        throwable = t,
                    )
                    postBanner(
                        AppDiagnosticLog.userFacingTechnicalMessage(
                            t.localizedMessage ?: t.javaClass.simpleName,
                            _uiState.value.language,
                        ),
                    )
                } finally {
                    if (trackExport && pendingExportResult == null && !_uiState.value.exportComplete) {
                        cancelExportTracking()
                    }
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            isProcessing = false,
                            purifyProgress = null,
                            exportProgress = if (trackExport && (_uiState.value.isExporting || _uiState.value.exportComplete)) {
                                _uiState.value.exportProgress ?: 0f
                            } else {
                                null
                            },
                        )
                    }
                    runCatching { if (processingMutex.isLocked) processingMutex.unlock() }
                }
            }
        } catch (t: Throwable) {
            runCatching { if (processingMutex.isLocked) processingMutex.unlock() }
        }
    }

    private var bannerJob: Job? = null
    private var tempPreviewJob: Job? = null
    private var sliderDebounceJob: Job? = null
    private var activeOnlineJob: Job? = null
    private var smartMixPreviewJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var batchProcessingJob: Job? = null
    private var cloudPresetsJob: Job? = null
    private var cloudDraftsJob: Job? = null
    private var localCloudDraftsJob: Job? = null
    private var aiVoiceSynthJob: Job? = null
    private var aiVoiceReferenceImportJob: Job? = null
    private var pendingExportResult: ExportedAudioResult? = null
    private var pendingSuggestedFileName: String? = null
    @Volatile private var defaultSaveTreeUriString: String? = null
    @Volatile private var lastNotifSyncMs: Long = 0L
    @Volatile private var lastNotifPlaying: Boolean = false
    @Volatile private var lastLivePreviewRequestMs: Long = 0L

    private val cloudPresetsFlow = MutableStateFlow<List<StudioPreset>>(emptyList())
    private val cloudDraftsFlow = MutableStateFlow<List<CloudDraft>>(emptyList())
    private val remoteCloudDraftsFlow = MutableStateFlow<List<CloudDraft>>(emptyList())
    private val localCloudDraftsFlow = MutableStateFlow<List<CloudDraft>>(emptyList())
    private val isCloudSyncedFlow = MutableStateFlow(false)

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val user = auth.currentUser
        attachCloudListeners(user?.uid)
        if (BuildConfig.DEVELOPER_PANEL_ENABLED) {
            developerPanelController.onAuthSessionChanged()
        }
        if (user != null) {
            viewModelScope.launch {
                val currentSession = repository.authSessionFlow.first()
                if (currentSession !is AuthSession.SignedIn || currentSession.uid != user.uid) {
                    completeGoogleSignIn(
                        uid = user.uid,
                        displayName = user.displayName.orEmpty(),
                        email = user.email.orEmpty(),
                    )
                }
            }
        }
    }

    private val player = StudioAudioPlayer(_uiState.value.dspConfig, application) { telemetry ->
        _playbackTelemetry.value = telemetry
        if (hasGlobalToolPreview()) {
            _uiState.update { it.copy(isToolPreviewPlaying = telemetry.isPlaying) }
        }
        val onlineFile = onlinePreviewFile
        if (
            !telemetry.isPlaying &&
            onlineFile != null &&
            toolPreviewPlaybackFile?.absolutePath == onlineFile.absolutePath &&
            telemetry.durationMs > 0L &&
            telemetry.positionMs >= telemetry.durationMs
        ) {
            _uiState.update { it.copy(previewingOnlineUrl = null) }
        }
        val aiOutputFile = toolPreviewPlaybackFile
        if (aiOutputFile != null && aiOutputFile.absolutePath == _aiVoiceUiState.value.lastOutputWavPath) {
            _aiVoiceUiState.update {
                it.copy(
                    isPlayingOutput = telemetry.isPlaying,
                    playbackPositionMs = telemetry.positionMs,
                    lastOutputDurationMs = if (telemetry.durationMs > 0L) telemetry.durationMs else it.lastOutputDurationMs,
                )
            }
        }
        val ttsPreviewFile = ttsPreviewTempFile
        if (ttsPreviewFile != null && aiOutputFile?.absolutePath == ttsPreviewFile.absolutePath) {
            _uiState.update { it.copy(ttsUiState = it.ttsUiState.copy(isPreviewing = telemetry.isPlaying)) }
        }
        val prevDiscrete = _uiState.value.playback
        if (prevDiscrete.isPlaying != telemetry.isPlaying || prevDiscrete.durationMs != telemetry.durationMs) {
            _uiState.update { cur ->
                cur.copy(
                    playback = cur.playback.copy(
                        isPlaying = telemetry.isPlaying,
                        durationMs = telemetry.durationMs,
                    ),
                )
            }
        }
        val now = System.currentTimeMillis()
        val stateFlipped = telemetry.isPlaying != lastNotifPlaying
        if (stateFlipped || (telemetry.isPlaying && now - lastNotifSyncMs >= 1500L)) {
            lastNotifPlaying = telemetry.isPlaying
            lastNotifSyncMs = now
            val cur = _uiState.value
            val title = cur.activeProjectName.ifBlank { "Sonix ✦" }
            val sub = "${formatDurationMs(telemetry.positionMs)} / ${formatDurationMs(telemetry.durationMs)}"
            SonixMediaPlaybackService.syncPlaybackState(
                context = getApplication(),
                isPlaying = telemetry.isPlaying,
                title = title,
                subtitle = sub,
            )
        }
        if (!telemetry.isPlaying && speechTranscriber.state.value.isRoutingClip) {
            speechTranscriber.stop()
        }
    }

    private val recorder = StudioAudioRecorder(application) { telemetry ->
        _recorderTelemetry.value = telemetry
        val prevRec = _uiState.value.recorder
        if (prevRec.isRecording != telemetry.isRecording || prevRec.isPaused != telemetry.isPaused) {
            _uiState.update { cur ->
                cur.copy(
                    recorder = cur.recorder.copy(
                        isRecording = telemetry.isRecording,
                        isPaused = telemetry.isPaused,
                    ),
                )
            }
        }
    }

    init {
        ExclusiveAudioController.registerStudioPlayer(player)
        StudioPlaybackBridge.onTogglePlayPause = { togglePlayPause() }
        StudioPlaybackBridge.onStopPlayback = { stopPlayback() }
        AudioPlayerManager.onBeforeStartExclusive = {
            if (_playbackTelemetry.value.isPlaying) {
                player.pause()
            }
        }

        val appCtx = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO + safeCoroutineHandler) {
            AppDiagnosticLog.init(appCtx)
            RecentEditsCacheManager.init(appCtx)
            val luxury = FreeEmotionalTtsClient.getSavedStudioLuxuryEnhancement(appCtx)
            val modelStatus = LocalNeuralVoiceEngine.detectLocalModels(appCtx)
            _uiState.update {
                it.copy(
                    studioLuxuryEnhancement = luxury,
                    localModelStatus = modelStatus,
                )
            }
        }

        viewModelScope.launch(safeCoroutineHandler) {
            AppDiagnosticLog.entries.collect { list ->
                _uiState.update { it.copy(diagnosticEntries = list) }
            }
        }
        viewModelScope.launch(safeCoroutineHandler) {
            AppDiagnosticLog.unreadCount.collect { cnt ->
                _uiState.update { it.copy(unreadDiagnosticCount = cnt) }
            }
        }
        viewModelScope.launch(safeCoroutineHandler) {
            RecentEditsCacheManager.entries.collect { recents ->
                _uiState.update { it.copy(recentEdits = recents) }
            }
        }

        viewModelScope.launch(safeCoroutineHandler) {
            speechTranscriber.state.collect { st ->
                _uiState.update { it.copy(speechState = st) }
            }
        }
        viewModelScope.launch(safeCoroutineHandler) {
            repository.savedAssistantChatsFlow.collect { chats ->
                _uiState.update { it.copy(savedAssistantChats = chats) }
            }
        }
        viewModelScope.launch(safeCoroutineHandler) {
            repository.favoriteOnlineSoundsFlow.collect { favs ->
                _uiState.update { it.copy(favoriteOnlineSounds = favs) }
            }
        }
        viewModelScope.launch(safeCoroutineHandler) {
            repository.onlineListeningHistoryFlow.collect { hist ->
                _uiState.update { it.copy(onlineListeningHistory = hist) }
            }
        }
        viewModelScope.launch(safeCoroutineHandler) {
            repository.savedAiVoiceCharactersFlow.collect { chars ->
                _aiVoiceUiState.update { it.copy(savedCharacters = chars) }
            }
        }
        viewModelScope.launch(safeCoroutineHandler) {
            repository.favoriteAiVoiceCharacterIdsFlow.collect { ids ->
                _aiVoiceUiState.update { it.copy(favoriteCharacterIds = ids) }
            }
        }
        viewModelScope.launch(safeCoroutineHandler) {
            repository.aiVoiceBackendUrlFlow.collect { url ->
                _aiVoiceUiState.update { it.copy(backendServerUrl = url) }
            }
        }
        viewModelScope.launch(safeCoroutineHandler) {
            repository.aiVoiceBackendBearerFlow.collect { token ->
                _aiVoiceUiState.update { it.copy(backendBearerToken = token) }
            }
        }

        val firebaseReady = FirebaseApp.getApps(getApplication()).isNotEmpty()
        val firebaseAuth = if (firebaseReady) runCatching { Firebase.auth }.getOrNull() else null
        firebaseAuth?.addAuthStateListener(authStateListener)
        attachCloudListeners(firebaseAuth?.currentUser?.uid)
        if (BuildConfig.DEVELOPER_PANEL_ENABLED) {
            developerPanelController.onAuthSessionChanged()
        }

        viewModelScope.launch {
            val baseFlow = combine(
                repository.languageFlow,
                repository.authSessionFlow,
                repository.autoLoopFlow,
                repository.projectsFlow,
                repository.recordingsFlow,
                repository.customPresetsFlow,
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                LocalDbSnapshot(
                    language = values[0] as AppLanguage,
                    authSession = values[1] as AuthSession,
                    autoLoop = values[2] as Boolean,
                    projects = values[3] as List<StudioProject>,
                    recordings = values[4] as List<AudioRecordingItem>,
                    localCustomPresets = values[5] as List<StudioPreset>,
                )
            }

            combine(
                baseFlow,
                cloudPresetsFlow,
                cloudDraftsFlow,
                isCloudSyncedFlow,
                repository.defaultSaveFolderFlow,
            ) { local, cloudPresets, cloudDrafts, isSynced, defaultFolder ->
                defaultSaveTreeUriString = defaultFolder.first
                PrefAndDbSnapshot(
                    language = local.language,
                    authSession = local.authSession,
                    autoLoop = local.autoLoop,
                    projects = local.projects,
                    recordings = local.recordings,
                    customPresets = if (isSynced) (cloudPresets + local.localCustomPresets).distinctBy { it.name.lowercase() } else local.localCustomPresets,
                    cloudDrafts = cloudDrafts,
                    isCloudSynced = isSynced,
                    defaultSaveFolderName = defaultFolder.second,
                )
            }.stateIn(viewModelScope, SharingStarted.Eagerly, null).collect { snap ->
                snap ?: return@collect
                _uiState.update { cur ->
                    val defaultUntitledAr = "مشروع صوتي جديد"
                    val updatedProjectName = if (cur.activeProjectId == 0L && cur.activeProjectName == defaultUntitledAr) {
                        t(snap.language, "untitled_project")
                    } else {
                        cur.activeProjectName
                    }
                    val effectiveSession = if (
                        snap.authSession is AuthSession.SignedOut &&
                        cur.authSession is AuthSession.SignedIn &&
                        FirebaseApp.getApps(getApplication()).isNotEmpty() &&
                        runCatching { FirebaseAuth.getInstance().currentUser != null }.getOrDefault(false)
                    ) {
                        cur.authSession
                    } else {
                        snap.authSession
                    }
                    cur.copy(
                        language = snap.language,
                        authSession = effectiveSession,
                        autoLoopOnPlayback = snap.autoLoop,
                        projects = snap.projects,
                        recordings = snap.recordings,
                        customPresets = snap.customPresets,
                        cloudDrafts = snap.cloudDrafts,
                        isCloudSynced = snap.isCloudSynced,
                        defaultSaveFolderName = snap.defaultSaveFolderName,
                        activeProjectName = updatedProjectName,
                    )
                }
                val s = _uiState.value
                player.setLoop(s.loopSelection || s.autoLoopOnPlayback, s.trimStart, s.trimEnd)
            }
        }
    }

    private fun attachCloudListeners(uid: String?) {
        cloudPresetsJob?.cancel()
        cloudPresetsJob = null
        cloudDraftsJob?.cancel()
        cloudDraftsJob = null
        localCloudDraftsJob?.cancel()
        localCloudDraftsJob = null

        if (uid.isNullOrBlank()) {
            isCloudSyncedFlow.value = false
            cloudPresetsFlow.value = emptyList()
            remoteCloudDraftsFlow.value = emptyList()
            localCloudDraftsFlow.value = emptyList()
            cloudDraftsFlow.value = emptyList()
            return
        }

        isCloudSyncedFlow.value = true
        cloudPresetsJob = viewModelScope.launch(safeCoroutineHandler) {
            firestoreRepo.observeCustomPresets(uid)
                .catch { err ->
                    if (err is CancellationException) throw err
                    Log.w("CloudSync", "Cloud presets listener warning: ${err.message}", err)
                    postBanner(t(_uiState.value.language, "cloud_sync_load_error"))
                }
                .collect { list ->
                    cloudPresetsFlow.value = list
                }
        }
        localCloudDraftsJob = viewModelScope.launch(safeCoroutineHandler) {
            repository.localCloudDraftsFlow(uid).collect { list ->
                localCloudDraftsFlow.value = list
                publishCloudDrafts()
            }
        }
        cloudDraftsJob = viewModelScope.launch(safeCoroutineHandler) {
            firestoreRepo.observeDrafts(uid)
                .catch { err ->
                    if (err is CancellationException) throw err
                    Log.w("CloudSync", "Cloud drafts listener warning: ${err.message}", err)
                    postBanner(t(_uiState.value.language, "cloud_sync_load_error"))
                }
                .collect { list ->
                    remoteCloudDraftsFlow.value = list
                    publishCloudDrafts()
                }
        }
    }

    private fun publishCloudDrafts() {
        val remote = remoteCloudDraftsFlow.value
        val remoteIds = remote.mapTo(HashSet()) { it.docId }
        cloudDraftsFlow.value = (remote + localCloudDraftsFlow.value.filterNot { it.docId in remoteIds })
            .sortedByDescending { it.updatedAtMs }
    }

    private data class LocalDbSnapshot(
        val language: AppLanguage,
        val authSession: AuthSession,
        val autoLoop: Boolean,
        val projects: List<StudioProject>,
        val recordings: List<AudioRecordingItem>,
        val localCustomPresets: List<StudioPreset>,
    )

    private data class PrefAndDbSnapshot(
        val language: AppLanguage,
        val authSession: AuthSession,
        val autoLoop: Boolean,
        val projects: List<StudioProject>,
        val recordings: List<AudioRecordingItem>,
        val customPresets: List<StudioPreset>,
        val cloudDrafts: List<CloudDraft>,
        val isCloudSynced: Boolean,
        val defaultSaveFolderName: String?,
    )

    private fun postBanner(msg: String) {
        bannerJob?.cancel()
        _uiState.update { state ->
            val clean = msg.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty().take(160)
            val repeated = state.statusBanner == clean
            state.copy(
                statusBanner = clean,
                statusBannerRepeatCount = if (repeated) state.statusBannerRepeatCount + 1 else 1,
            )
        }
        bannerJob = viewModelScope.launch {
            delay(4200L)
            _uiState.update { it.copy(statusBanner = null) }
        }
    }

    private fun beginExportTracking(fileName: String) {
        _uiState.update {
            it.copy(
                isExporting = true,
                exportComplete = false,
                exportProgress = 0f,
                exportFileName = fileName,
                exportSaveLocation = null,
            )
        }
    }

    private fun updateExportProgress(progress: Float) {
        _uiState.update {
            if (it.isExporting) it.copy(exportProgress = progress.coerceIn(0f, 1f)) else it
        }
    }

    private fun completeExportTracking(fileName: String, destination: String) {
        pendingExportResult = null
        pendingSuggestedFileName = null
        _uiState.update {
            it.copy(
                isExporting = false,
                exportComplete = true,
                exportProgress = 1f,
                exportFileName = fileName,
                exportSaveLocation = destination,
            )
        }
        notifyAudioCompletionIfBackground(
            title = if (_uiState.value.language == AppLanguage.EN) "Audio export complete" else "اكتمل حفظ وتصدير الملف الصوتي بنجاح",
            message = "$fileName — $destination",
        )
    }

    private fun cancelExportTracking() {
        pendingExportResult = null
        pendingSuggestedFileName = null
        _uiState.update {
            it.copy(
                isExporting = false,
                exportComplete = false,
                exportProgress = null,
                exportFileName = null,
                exportSaveLocation = null,
            )
        }
    }

    fun dismissExportStatus() {
        if (_uiState.value.exportComplete) cancelExportTracking()
    }

    private fun notifyAudioCompletionIfBackground(title: String, message: String) {
        if (SonixCrashGuard.isAppInForeground()) return
        val app = getApplication<Application>()
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = app.getSystemService(NotificationManager::class.java) ?: return
        val channelId = "sonix_audio_operations"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    if (_uiState.value.language == AppLanguage.EN) "Sonix audio operations" else "عمليات Sonix الصوتية",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = if (_uiState.value.language == AppLanguage.EN) {
                        "Audio export and processing completion alerts"
                    } else {
                        "تنبيهات اكتمال حفظ الملفات ومعالجتها صوتياً"
                    }
                },
            )
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(app, channelId)
        } else {
            Notification.Builder(app)
        }
        val notification = builder
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(message.take(180))
            .setStyle(Notification.BigTextStyle().bigText(message.take(500)))
            .setAutoCancel(true)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun clearBanner() {
        bannerJob?.cancel()
        _uiState.update { it.copy(statusBanner = null, statusBannerRepeatCount = 1) }
    }

    fun showStatusBanner(msg: String) {
        postBanner(msg)
    }

    // Auth & Language
    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch {
            repository.saveLanguage(lang)
        }
    }

    fun continueAsGuest() {
        _uiState.update { it.copy(authSession = AuthSession.Guest) }
        viewModelScope.launch {
            repository.saveAuthSession(AuthSession.Guest)
        }
    }

    fun requestSignIn() {
        _uiState.update { it.copy(authSession = AuthSession.SignedOut) }
        viewModelScope.launch {
            repository.saveAuthSession(AuthSession.SignedOut)
        }
    }

    suspend fun completeGoogleSignIn(uid: String, displayName: String, email: String) {
        val cleanName = displayName.trim().ifEmpty {
            email.substringBefore('@').ifEmpty { "Google User" }
        }
        val session = AuthSession.SignedIn(
            uid = uid,
            displayName = cleanName,
            email = email.trim(),
        )
        _uiState.update { it.copy(authSession = session) }
        runCatching {
            attachCloudListeners(uid)
            if (BuildConfig.DEVELOPER_PANEL_ENABLED) {
                developerPanelController.onAuthSessionChanged()
            }
        }.onFailure { err ->
            Log.w("Auth", "Non-fatal cloud listener setup warning after sign-in: ${err.message}")
        }
        runCatching {
            repository.saveAuthSession(session)
        }.onFailure { err ->
            Log.w("Auth", "Failed persisting session locally: ${err.message}")
        }
    }

    fun onGoogleLoginSuccess(uid: String, displayName: String, email: String) {
        val cleanName = displayName.trim().ifEmpty {
            email.substringBefore('@').ifEmpty { "Google User" }
        }
        val session = AuthSession.SignedIn(
            uid = uid,
            displayName = cleanName,
            email = email.trim(),
        )
        _uiState.update { it.copy(authSession = session) }
        attachCloudListeners(uid)
        if (BuildConfig.DEVELOPER_PANEL_ENABLED) {
            developerPanelController.onAuthSessionChanged()
        }
        viewModelScope.launch {
            repository.saveAuthSession(session)
        }
    }

    suspend fun deleteCurrentAccountAndData(): AccountDeletionResult {
        val session = _uiState.value.authSession as? AuthSession.SignedIn
            ?: return AccountDeletionResult.Failed
        val firebaseUser = runCatching { Firebase.auth.currentUser }.getOrNull()

        if (firebaseUser != null) {
            if (firebaseUser.uid != session.uid) return AccountDeletionResult.Failed
            developerPanelController.stopCollectors()
            attachCloudListeners(null)
            val cloudDeletion = firestoreRepo.deleteUserProjectData(session.uid)
            if (cloudDeletion.isFailure) {
                attachCloudListeners(session.uid)
                if (BuildConfig.DEVELOPER_PANEL_ENABLED) {
                    developerPanelController.onAuthSessionChanged()
                }
                return accountDeletionFailure(cloudDeletion.exceptionOrNull() ?: IllegalStateException())
            }
            try {
                firebaseUser.delete().await()
            } catch (e: CancellationException) {
                attachCloudListeners(session.uid)
                if (BuildConfig.DEVELOPER_PANEL_ENABLED) {
                    developerPanelController.onAuthSessionChanged()
                }
                throw e
            } catch (e: Exception) {
                Log.e("AccountDeletion", "Firebase account deletion failed after cloud project cleanup.", e)
                attachCloudListeners(session.uid)
                if (BuildConfig.DEVELOPER_PANEL_ENABLED) {
                    developerPanelController.onAuthSessionChanged()
                }
                return accountDeletionFailure(e)
            }
        } else if (session.uid.startsWith("local_")) {
            try {
                if (!repository.deleteLocalAccount(session.uid)) {
                    return AccountDeletionResult.Failed
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("AccountDeletion", "Local account deletion failed.", e)
                return accountDeletionFailure(e)
            }
        } else {
            return AccountDeletionResult.Failed
        }

        completeSignOut()
        return AccountDeletionResult.Success
    }

    private fun accountDeletionFailure(error: Throwable): AccountDeletionResult {
        val causes = generateSequence(error) { it.cause }
        return when {
            causes.any {
                it is FirebaseAuthRecentLoginRequiredException ||
                    (it is FirebaseAuthException && it.errorCode == "ERROR_REQUIRES_RECENT_LOGIN")
            } -> AccountDeletionResult.RecentLoginRequired
            causes.any {
                it is FirebaseNetworkException ||
                    it is IOException ||
                    (it is FirebaseFirestoreException &&
                        it.code in setOf(
                            FirebaseFirestoreException.Code.UNAVAILABLE,
                            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                        ))
            } -> AccountDeletionResult.NetworkError
            else -> AccountDeletionResult.Failed
        }
    }

    fun signOut() {
        viewModelScope.launch {
            completeSignOut()
        }
    }

    private suspend fun completeSignOut() {
        withContext(NonCancellable) {
            // Stop active Firestore snapshot collectors BEFORE Firebase.auth.signOut()
            // to avoid spurious PERMISSION_DENIED race conditions.
            developerPanelController.stopCollectors()
            attachCloudListeners(null)
            _uiState.update {
                it.copy(
                    authSession = AuthSession.SignedOut,
                    activeCloudDraftId = null,
                )
            }
            runCatching {
                CredentialManager.create(getApplication()).clearCredentialState(ClearCredentialStateRequest())
            }.onFailure { err ->
                Log.w("Auth", "Failed to clear credential state: ${err.message}")
            }
            runCatching {
                Firebase.auth.signOut()
            }.onFailure { err ->
                Log.w("Auth", "Firebase signOut warning: ${err.message}")
            }
            if (BuildConfig.DEVELOPER_PANEL_ENABLED) {
                developerPanelController.onAuthSessionChanged()
            }
            runCatching {
                repository.saveAuthSession(AuthSession.SignedOut)
            }.onFailure { err ->
                Log.e("Auth", "Failed to persist signed-out state.", err)
            }
        }
    }

    suspend fun signInWithEmail(email: String, password: String): String? {
        return when (val res = repository.signInLocalEmail(email, password, _uiState.value.language)) {
            is AuthResult.Success -> {
                _uiState.update { it.copy(authSession = res.session) }
                null
            }
            is AuthResult.Error -> res.message
        }
    }

    suspend fun createAccountWithEmail(name: String, email: String, password: String): String? {
        return when (val res = repository.registerLocalEmail(name, email, password, _uiState.value.language)) {
            is AuthResult.Success -> {
                _uiState.update { it.copy(authSession = res.session) }
                null
            }
            is AuthResult.Error -> res.message
        }
    }

    private fun currentSnapshot(): EditorSnapshot {
        val st = _uiState.value
        return EditorSnapshot(
            buffer = activeBuffer,
            dspConfig = st.dspConfig,
            activePresetId = st.activePresetId,
            selectedPresetLabel = st.selectedPresetLabel,
            waveformPeaks = st.waveformPeaks,
        )
    }

    private fun snapshotPcmBytes(snapshot: EditorSnapshot): Long =
        snapshot.buffer?.frameCount?.toLong()?.times(8L) ?: 0L

    private fun appendBoundedSnapshot(
        stack: ArrayDeque<EditorSnapshot>,
        snapshot: EditorSnapshot,
    ): Boolean {
        val snapshotBytes = snapshotPcmBytes(snapshot)
        if (snapshotBytes > maxUndoPcmBytes) return false
        while (
            stack.isNotEmpty() &&
            (stack.size >= 20 || stack.sumOf(::snapshotPcmBytes) + snapshotBytes > maxUndoPcmBytes)
        ) {
            stack.removeFirst()
        }
        stack.addLast(snapshot)
        return true
    }

    private fun pushUndoSnapshot(debounced: Boolean) {
        val now = System.currentTimeMillis()
        if (debounced && now - lastDspSnapshotTimeMs < 550L) {
            return
        }
        lastDspSnapshotTimeMs = if (debounced) now else 0L
        val snapshot = currentSnapshot()
        if (!appendBoundedSnapshot(undoStack, snapshot)) {
            undoStack.clear()
            redoStack.clear()
            _uiState.update { it.copy(canUndoEdit = false, canRedoEdit = false) }
            postBanner("تم تقليص سجل التراجع لحماية الذاكرة في المقاطع الكبيرة")
            return
        }
        redoStack.clear()
    }

    private fun previewTempFile(): File = File(getApplication<Application>().cacheDir, "temp_preview.wav")

    private fun deleteTempPreviewFile() {
        tempPreviewJob?.cancel()
        tempPreviewJob = null
        runCatching { previewTempFile().delete() }
        runCatching { File(getApplication<Application>().cacheDir, "temp_preview.wav.tmp").delete() }
        _uiState.update { it.copy(previewWavPath = null) }
    }

    private fun scheduleTempPreviewRender() {
        val src = activeBuffer
        if (src == null) {
            deleteTempPreviewFile()
            return
        }
        val cfg = _uiState.value.dspConfig
        tempPreviewJob?.cancel()
        tempPreviewJob = viewModelScope.launch(Dispatchers.Default) {
            delay(220L)
            val rendered = renderOfflineDsp(src, cfg)
            val tmpFile = File(getApplication<Application>().cacheDir, "temp_preview.wav.tmp")
            val finalFile = previewTempFile()
            writeStereoWavFile(tmpFile, rendered)
            if (!tmpFile.renameTo(finalFile)) {
                writeStereoWavFile(finalFile, rendered)
                tmpFile.delete()
            }
            _uiState.update { it.copy(previewWavPath = finalFile.absolutePath) }
        }
    }

    private fun replaceActiveSourceFile(newFile: File?) {
        if (hasGlobalToolPreview()) stopToolPreview()
        val old = activeSourceFile
        activeSourceFile = newFile
        if (old != null && old != newFile) {
            runCatching { old.delete() }
        }
    }

    private fun replaceMixBgSourceFile(newFile: File?) {
        val old = mixBgSourceFile
        mixBgSourceFile = newFile
        if (old != null && old != newFile) {
            runCatching { old.delete() }
        }
    }

    private suspend fun applyProcessedDiskSourceFile(
        newFile: File,
        recordUndo: Boolean = false,
        appliedToolLabel: String? = null,
    ) {
        val meta = AudioDecoder.inspectLargeAudioStreaming(newFile, buckets = 800, context = getApplication()) {}
        if (meta == null) {
            runCatching { newFile.delete() }
            postBanner("تعذر قراءة الملف المعالج")
            return
        }
        if (recordUndo && activeBuffer != null) {
            pushUndoSnapshot(debounced = false)
        }
        activeBuffer = null
        replaceActiveSourceFile(newFile)
        activeSourceSampleRate = meta.sampleRate
        player.setSourceFile(
            file = newFile,
            durationMs = meta.durationMs,
            sampleRate = meta.sampleRate,
            resetPosition = true,
        )
        player.setPlaybackBounds(editorPlaybackBoundsEnabled, 0f, 1f)
        _uiState.update { cur ->
            cur.copy(
                hasAudio = true,
                durationMs = meta.durationMs,
                waveformPeaks = meta.waveformPeaks,
                originalPeakDbfs = meta.peakDbfs,
                trimStart = 0f,
                trimEnd = 1f,
                canUndoEdit = undoStack.isNotEmpty(),
                canRedoEdit = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
                lastAppliedToolLabel = appliedToolLabel ?: cur.lastAppliedToolLabel,
                vadSpeechSegments = emptyList(),
                vadUsedSilero = false,
            )
        }
    }

    private suspend fun resolveAsDiskSourceFile(
        buffer: StereoAudioBuffer?,
        diskFile: File?,
        prefix: String,
    ): Pair<File?, Boolean> = withContext(Dispatchers.IO) {
        if (diskFile != null && diskFile.exists() && diskFile.length() > 0L) {
            return@withContext diskFile to false
        }
        if (buffer != null && buffer.frameCount > 0) {
            val dir = File(getApplication<Application>().cacheDir, "staged_inputs").apply { mkdirs() }
            val tmpWav = File(dir, "${prefix}_${UUID.randomUUID()}.wav")
            writeStereoWavFile(tmpWav, buffer)
            return@withContext tmpWav to true
        }
        null to false
    }

    // Audio Buffer & Unified Undo/Redo Management
    private fun setActiveBuffer(
        buf: StereoAudioBuffer,
        recordUndo: Boolean,
        resetDspOnNewEntry: Boolean = false,
    ) {
        if (hasGlobalToolPreview()) stopToolPreview()
        if (recordUndo && activeBuffer != null) {
            pushUndoSnapshot(debounced = false)
        } else if (!recordUndo) {
            undoStack.clear()
            redoStack.clear()
            lastDspSnapshotTimeMs = 0L
        }
        replaceActiveSourceFile(null)
        activeBuffer = buf
        player.setBuffer(buf, resetPosition = true)
        player.setPlaybackBounds(editorPlaybackBoundsEnabled, 0f, 1f)
        val cleanCfg = DspConfig()
        if (resetDspOnNewEntry) {
            syncPlayerConfig(cleanCfg, holdBypass = false)
            player.setClipGainDb(0f)
        }
        val evalCfg = if (resetDspOnNewEntry) cleanCfg else _uiState.value.dspConfig
        _uiState.update { cur ->
            val updatedCfg = if (resetDspOnNewEntry) cleanCfg else cur.dspConfig
            cur.copy(
                hasAudio = true,
                durationMs = buf.durationMs,
                dspConfig = updatedCfg,
                activePresetId = if (resetDspOnNewEntry) null else cur.activePresetId,
                selectedPresetLabel = if (resetDspOnNewEntry) "" else cur.selectedPresetLabel,
                activeCloudDraftId = if (resetDspOnNewEntry) null else cur.activeCloudDraftId,
                isHoldBypassing = if (resetDspOnNewEntry) false else cur.isHoldBypassing,
                audioMarkers = if (!recordUndo && resetDspOnNewEntry) emptyList() else cur.audioMarkers,
                trimStart = 0f,
                trimEnd = 1f,
                canUndoEdit = undoStack.isNotEmpty(),
                canRedoEdit = redoStack.isNotEmpty(),
                hasUnsavedChanges = recordUndo,
                vadSpeechSegments = emptyList(),
                vadUsedSilero = false,
            )
        }
        viewModelScope.launch(Dispatchers.Default) {
            val peakDbfs = AudioSignalAnalyzer.analyzeBufferPeakDbfs(buf)
            val peaks = computeWaveformPeaksAsync(buf, 800)
            val nrWarn = computePurifierWarning(buf, evalCfg)
            val eqWarn = computeEqWarning(buf, evalCfg)
            if (activeBuffer === buf) {
                _uiState.update {
                    it.copy(
                        originalPeakDbfs = peakDbfs,
                        waveformPeaks = peaks,
                        fricativeWarningPurifier = nrWarn,
                        fricativeWarningEq = eqWarn,
                    )
                }
            }
        }
        scheduleTempPreviewRender()
    }

    private fun computePurifierWarning(buf: StereoAudioBuffer?, cfg: DspConfig): Boolean {
        val nr = cfg.noiseReduction
        return AudioSignalAnalyzer.willFricativeClarityDrop(
            buffer = buf,
            highPassHz = nr.highPassHz,
            lowPassHz = nr.lowPassHz,
            spectralStrength = if (nr.spectralCleanerEnabled) nr.spectralStrength else 0f,
            reductionDb = nr.reductionDb,
            deEssAmount = nr.deEssAmount,
            clarityBoostDb = nr.clarityBoostDb,
        )
    }

    private fun computeEqWarning(buf: StereoAudioBuffer?, cfg: DspConfig): Boolean {
        val b10 = cfg.equalizer.resolved10Bands()
        val presenceCutDb = minOf(b10[7], cfg.equalizer.band3600HzDb)
        val airCutDb = minOf(b10[8], b10[9], cfg.equalizer.band14000HzDb)
        if (presenceCutDb >= -1.5f && airCutDb >= -2.5f) return false
        return AudioSignalAnalyzer.willFricativeClarityDrop(
            buffer = buf,
            highPassHz = 20f,
            lowPassHz = if (airCutDb < -7f) 5200f else 16000f,
            spectralStrength = 0f,
            reductionDb = 0f,
            deEssAmount = 0f,
            clarityBoostDb = 0f,
            eq3600Db = presenceCutDb,
        )
    }

    fun applyEqPresetProfile(preset: com.example.model.EqPresetProfile) {
        updateDspConfig {
            it.copy(equalizer = com.example.model.EqualizerParams.from10Bands(preset.bands10Db))
                .withSlotEnabled(EffectType.EQUALIZER, preset.id != "flat")
        }
        ensureLivePreviewPlaying()
    }

    fun resetEqualizerOnly() {
        updateDspConfig {
            it.copy(equalizer = com.example.model.EqualizerParams())
                .withSlotEnabled(EffectType.EQUALIZER, false)
        }
    }

    private fun syncPlayerConfig(cfg: DspConfig, holdBypass: Boolean = _uiState.value.isHoldBypassing) {
        val effective = if (holdBypass || hasGlobalToolPreview()) cfg.copy(bypassAll = true) else cfg
        player.updateConfig(effective)
        if (pitchSpeedLiveModeActive) {
            player.setPitchSpeedPreview(
                active = true,
                semitones = cfg.pitchShift.semitones,
                tempoRatio = cfg.timeStretch.tempoRatio,
            )
        }
    }

    fun setPitchSpeedLivePreviewActive(active: Boolean) {
        pitchSpeedLiveModeActive = active
        val cfg = _uiState.value.dspConfig
        player.setPitchSpeedPreview(
            active = active,
            semitones = cfg.pitchShift.semitones,
            tempoRatio = cfg.timeStretch.tempoRatio,
        )
    }

    fun updatePitchSpeedParamsLive(
        semitones: Float,
        tempoRatio: Float,
        preserveFormants: Boolean = _uiState.value.dspConfig.pitchShift.preserveFormants,
    ) {
        val currentCfg = _uiState.value.dspConfig
        val clampedTempo = clamp(tempoRatio, 0.5f, 2.5f)
        val next = currentCfg.copy(
            bypassAll = false,
            pitchShift = currentCfg.pitchShift.copy(
                semitones = clamp(semitones, -12f, 12f),
                wetMix = 1f,
                preserveFormants = preserveFormants,
            ),
            timeStretch = currentCfg.timeStretch.copy(tempoRatio = clampedTempo),
        )
            .withSlotEnabled(EffectType.PITCH_SHIFT, kotlin.math.abs(semitones) >= 0.05f)
            .withSlotEnabled(EffectType.TIME_STRETCH, kotlin.math.abs(clampedTempo - 1f) >= 0.02f)
            .normalized()
        pushUndoSnapshot(debounced = true)
        _uiState.update {
            it.copy(
                dspConfig = next,
                activePresetId = null,
                selectedPresetLabel = "",
                canUndoEdit = undoStack.isNotEmpty(),
                canRedoEdit = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
            )
        }
        player.updateConfig(if (_uiState.value.isHoldBypassing) next.copy(bypassAll = true) else next)
        player.setPitchSpeedPreview(true, next.pitchShift.semitones, next.timeStretch.tempoRatio)
    }

    fun syncMergeStudioPreview(params: MergeStudioParams) {
        val pDur = _uiState.value.durationMs
        val sDur = _uiState.value.mixBgDurationMs
        val hasBoth = hasLoadedAudio() && !_uiState.value.mixBgName.isNullOrBlank()
        if (!hasBoth) {
            player.setMergePreviewBuffers(false, null, null, params, null)
            return
        }
        val layout = computeMergeTimelineLayout(pDur, sDur, params)
        player.setMergePreviewBuffers(
            active = true,
            primary = activeBuffer,
            secondary = mixBgBuffer,
            params = params,
            totalDurationOverrideMs = layout.totalDurationMs,
        )
    }

    fun clearMergeStudioPreview() {
        player.setMergePreviewBuffers(false, null, null, MergeStudioParams(), null)
    }

    fun ensureLivePreviewPlaying() {
        if (!hasLoadedAudio() || _playbackTelemetry.value.isPlaying) return
        val now = System.currentTimeMillis()
        if (now - lastLivePreviewRequestMs < 260L) return
        lastLivePreviewRequestMs = now
        stopToolPreview()
        stopMixPreviewPlayer()
        ExclusiveAudioController.onBeforeStudioPlayerPlay(player)
        livePreviewStartedPlayback = true
        player.play()
    }

    fun beginLiveToolSession(toolKey: String) {
        val current = activeLiveToolSession
        if (current != null && current.toolKey == toolKey) return
        val st = _uiState.value
        livePreviewStartedPlayback = false
        activeLiveToolSession = LiveToolSessionSnapshot(
            toolKey = toolKey,
            dspConfig = st.dspConfig,
            clipGainDb = st.clipGainDb,
            activePresetId = st.activePresetId,
            selectedPresetLabel = st.selectedPresetLabel,
            undoSize = undoStack.size,
            wasPlayingOnEntry = st.playback.isPlaying,
            committed = false,
        )
    }

    fun isLiveToolSessionCommitted(toolKey: String): Boolean {
        val s = activeLiveToolSession ?: return false
        return s.toolKey == toolKey && s.committed
    }

    private fun collapseSessionSliderUndoSnapshots(toolKey: String, srcBuffer: StereoAudioBuffer?) {
        val snap = activeLiveToolSession
        if (snap != null && snap.toolKey == toolKey) {
            while (undoStack.size > snap.undoSize) {
                undoStack.removeLast()
            }
            if (_uiState.value.dspConfig != snap.dspConfig) {
                val snapshot = EditorSnapshot(
                    buffer = srcBuffer,
                    dspConfig = snap.dspConfig,
                    activePresetId = snap.activePresetId,
                    selectedPresetLabel = snap.selectedPresetLabel,
                )
                if (!appendBoundedSnapshot(undoStack, snapshot)) undoStack.clear()
                redoStack.clear()
            }
            snap.committed = true
        }
    }

    fun commitLiveToolSession(
        toolKey: String,
        appliedLabel: String? = null,
    ) {
        sliderDebounceJob?.cancel()
        com.example.diagnostics.FfmpegKitBridge.cancelActiveSessions()
        stopToolPreview()
        stopMixPreviewPlayer()
        AudioPlayerManager.releaseAll()
        collapseSessionSliderUndoSnapshots(toolKey, activeBuffer)
        val st = _uiState.value
        syncPlayerConfig(st.dspConfig)
        _uiState.update { cur ->
            cur.copy(
                canUndoEdit = undoStack.isNotEmpty(),
                canRedoEdit = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
                lastAppliedToolLabel = appliedLabel ?: cur.lastAppliedToolLabel,
            )
        }
        scheduleTempPreviewRender()
        val msg = if (st.language == AppLanguage.EN) {
            "Changes applied${appliedLabel?.let { ": $it" } ?: ""}"
        } else {
            "تم اعتماد وتطبيق التعديل${appliedLabel?.let { ": $it" } ?: ""}"
        }
        postBanner(msg)
    }

    fun discardLiveToolSession(toolKey: String) {
        val snap = activeLiveToolSession ?: return
        if (snap.toolKey != toolKey) return
        activeLiveToolSession = null
        if (snap.committed) {
            livePreviewStartedPlayback = false
            return
        }
        sliderDebounceJob?.cancel()
        sliderDebounceJob = null
        if (livePreviewStartedPlayback || !snap.wasPlayingOnEntry) {
            livePreviewStartedPlayback = false
            if (_uiState.value.playback.isPlaying) {
                player.pause()
            }
        }
        while (undoStack.size > snap.undoSize) {
            undoStack.removeLast()
        }
        lastDspSnapshotTimeMs = 0L
        val restoredCfg = snap.dspConfig.normalized()
        syncPlayerConfig(restoredCfg, holdBypass = _uiState.value.isHoldBypassing)
        val nrWarn = computePurifierWarning(activeBuffer, restoredCfg)
        val eqWarn = computeEqWarning(activeBuffer, restoredCfg)
        _uiState.update { cur ->
            cur.copy(
                dspConfig = restoredCfg,
                activePresetId = snap.activePresetId,
                selectedPresetLabel = snap.selectedPresetLabel,
                fricativeWarningPurifier = nrWarn,
                fricativeWarningEq = eqWarn,
                canUndoEdit = undoStack.isNotEmpty(),
                canRedoEdit = redoStack.isNotEmpty(),
            )
        }
        deleteTempPreviewFile()
        if (restoredCfg.hasAnyActiveProcessing()) {
            scheduleTempPreviewRender()
        }
    }

    // DSP Controls
    fun setSimpleEchoMode(simple: Boolean) {
        _uiState.update { it.copy(isSimpleEchoMode = simple) }
    }

    fun setEchoPadAxisMode(mode: EchoPadAxisMode) {
        _uiState.update { it.copy(echoPadAxisMode = mode) }
    }

    fun setHoldBypass(holding: Boolean) {
        _uiState.update { it.copy(isHoldBypassing = holding) }
        if (hasGlobalToolPreview()) {
            switchToolPreviewSource(showOriginal = holding || _uiState.value.dspConfig.bypassAll)
        } else {
            syncPlayerConfig(_uiState.value.dspConfig, holding)
        }
    }

    fun toggleBypass() {
        val next = _uiState.value.dspConfig.let { it.copy(bypassAll = !it.bypassAll) }
        _uiState.update { it.copy(dspConfig = next) }
        if (hasGlobalToolPreview()) {
            switchToolPreviewSource(showOriginal = next.bypassAll || _uiState.value.isHoldBypassing)
            return
        }
        syncPlayerConfig(next)
    }

    private fun switchToolPreviewSource(showOriginal: Boolean) {
        if (!hasGlobalToolPreview()) return
        if (showOriginal && activeBuffer == null && activeSourceFile?.exists() != true) {
            postBanner(if (_uiState.value.language == AppLanguage.EN) "No original audio is available to compare." else "لا يوجد صوت أصلي متاح للمقارنة.")
            return
        }
        val telemetry = _playbackTelemetry.value
        val wasPlaying = telemetry.isPlaying
        val positionFraction = if (telemetry.durationMs > 0L) {
            telemetry.positionMs.toFloat() / telemetry.durationMs.toFloat()
        } else {
            0f
        }
        player.stop()
        if (showOriginal) {
            restoreActivePlayerSource()
        } else if (toolPreviewBuffer != null) {
            player.setBuffer(toolPreviewBuffer)
        } else {
            val previewFile = toolPreviewPlaybackFile ?: return
            player.setSourceFile(
                file = previewFile,
                durationMs = toolPreviewFileDurationMs,
                sampleRate = toolPreviewFileSampleRate,
            )
        }
        if (!showOriginal) player.setPlaybackBounds(false, 0f, 1f)
        syncPlayerConfig(_uiState.value.dspConfig)
        player.seekToFraction(positionFraction)
        if (wasPlaying) {
            ExclusiveAudioController.onBeforeStudioPlayerPlay(player)
            player.play()
        }
    }

    fun updateDspConfig(debouncedUndo: Boolean = true, updater: (DspConfig) -> DspConfig) {
        pushUndoSnapshot(debounced = debouncedUndo)
        val next = updater(_uiState.value.dspConfig).normalized()
        _uiState.update {
            it.copy(
                dspConfig = next,
                activePresetId = null,
                selectedPresetLabel = "",
                canUndoEdit = undoStack.isNotEmpty(),
                canRedoEdit = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
            )
        }
        sliderDebounceJob?.cancel()
        sliderDebounceJob = viewModelScope.launch(Dispatchers.Default) {
            delay(60L)
            syncPlayerConfig(next)
            val nrWarn = computePurifierWarning(activeBuffer, next)
            val eqWarn = computeEqWarning(activeBuffer, next)
            _uiState.update {
                it.copy(
                    fricativeWarningPurifier = nrWarn,
                    fricativeWarningEq = eqWarn,
                )
            }
            scheduleTempPreviewRender()
        }
    }

    fun resetAllEffects() {
        pushUndoSnapshot(debounced = false)
        val def = DspConfig().let { d ->
            d.copy(chain = d.chain.map { it.copy(enabled = false) })
        }
        _uiState.update {
            it.copy(
                dspConfig = def,
                activePresetId = null,
                selectedPresetLabel = "",
                isHoldBypassing = false,
                canUndoEdit = undoStack.isNotEmpty(),
                canRedoEdit = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
            )
        }
        syncPlayerConfig(def, holdBypass = false)
        scheduleTempPreviewRender()
        postBanner("تم إيقاف كافة المؤثرات وإرجاعها إلى 0%")
    }

    fun toggleEffectEnabled(type: EffectType, enabled: Boolean) {
        updateDspConfig(debouncedUndo = false) { prev ->
            prev.copy(bypassAll = false).withSlotEnabled(type, enabled)
        }
    }

    fun moveEffectInChain(fromIdx: Int, toIdx: Int) {
        val cur = _uiState.value.dspConfig.chain
        if (fromIdx !in cur.indices || toIdx !in cur.indices || fromIdx == toIdx) return
        val mutable = cur.toMutableList()
        val moved = mutable.removeAt(fromIdx)
        mutable.add(toIdx, moved)
        updateDspConfig(debouncedUndo = false) { prev -> prev.copy(chain = mutable) }
    }

    fun updateEchoReverbParams(updater: (EchoReverbParams) -> EchoReverbParams) {
        updateDspConfig { prev ->
            prev.copy(bypassAll = false, echoReverb = updater(prev.echoReverb))
                .withSlotEnabled(EffectType.ECHO_REVERB, true)
        }
    }

    fun applyEchoSpacePreset(space: EchoSpacePreset) {
        val label = t(_uiState.value.language, space.titleKey)
        updateDspConfig(debouncedUndo = false) { prev ->
            prev.copy(
                bypassAll = false,
                echoReverb = space.echoReverb,
                limiter = prev.limiter.copy(ceilingDb = minOf(prev.limiter.ceilingDb, -0.6f)),
            )
                .withSlotEnabled(EffectType.ECHO_REVERB, true)
                .withSlotEnabled(EffectType.LIMITER, true)
        }
        _uiState.update { it.copy(selectedPresetLabel = label) }
        if (hasLoadedAudio()) {
            ensureLivePreviewPlaying()
        }
        postBanner(
            if (_uiState.value.language == AppLanguage.EN) {
                "Acoustic Space: $label"
            } else {
                "تم تفعيل البيئة الصوتية: $label"
            },
        )
    }

    private fun mapNormalized(tVal: Float, minV: Float, maxV: Float): Float =
        clamp(minV + clamp(tVal, 0f, 1f) * (maxV - minV), minOf(minV, maxV), maxOf(minV, maxV))

    private fun unmapNormalized(v: Float, minV: Float, maxV: Float): Float =
        clamp((v - minV) / (maxV - minV), 0f, 1f)

    fun updateEchoReverbFromPad(rawX: Float, rawY: Float) {
        val nx = clamp(rawX, 0f, 1f)
        val ny = clamp(rawY, 0f, 1f)
        val mode = _uiState.value.echoPadAxisMode
        updateEchoReverbParams { prev ->
            when (mode) {
                EchoPadAxisMode.SPACE -> prev.copy(
                    echoStrength = nx,
                    echoDepth = ny,
                    reverbSize = mapNormalized(ny, 0.2f, 0.96f),
                    reverbDecay = mapNormalized(ny, 0.15f, 0.92f),
                    wetDryMix = mapNormalized((nx + ny) * 0.5f, 0.08f, 0.85f),
                )
                EchoPadAxisMode.DELAY -> prev.copy(
                    delayTimeMs = mapNormalized(nx, 40f, 1000f),
                    feedback = mapNormalized(ny, 0.05f, 0.85f),
                )
                EchoPadAxisMode.WIDTH -> prev.copy(
                    stereoWidth = nx,
                    diffusion = mapNormalized(ny, 0.15f, 0.96f),
                    earlyReflections = mapNormalized(ny, 0.15f, 0.9f),
                )
                EchoPadAxisMode.DAMPING -> prev.copy(
                    damping = nx,
                    preDelayMs = mapNormalized(ny, 0f, 140f),
                    lateReflections = mapNormalized(1f - nx * 0.35f, 0.25f, 0.9f),
                )
            }
        }
    }

    fun getEchoPadAxes(): Pair<Float, Float> {
        val e = _uiState.value.dspConfig.echoReverb
        return when (_uiState.value.echoPadAxisMode) {
            EchoPadAxisMode.SPACE -> clamp(e.echoStrength, 0f, 1f) to clamp(e.echoDepth, 0f, 1f)
            EchoPadAxisMode.DELAY -> unmapNormalized(e.delayTimeMs, 40f, 1000f) to unmapNormalized(e.feedback, 0.05f, 0.85f)
            EchoPadAxisMode.WIDTH -> clamp(e.stereoWidth, 0f, 1f) to unmapNormalized(e.diffusion, 0.15f, 0.96f)
            EchoPadAxisMode.DAMPING -> clamp(e.damping, 0f, 1f) to unmapNormalized(e.preDelayMs, 0f, 140f)
        }
    }

    // Transport
    fun togglePlayPause() {
        try {
            if (!hasLoadedAudio()) {
                val msg = if (_uiState.value.language == AppLanguage.EN) {
                    "Please record audio or import an audio file first"
                } else {
                    "يرجى تسجيل صوت أو استيراد ملف صوتي أولاً"
                }
                postBanner(msg)
                return
            }
            livePreviewStartedPlayback = false
            if (_playbackTelemetry.value.isPlaying) {
                player.pause()
            } else {
                ExclusiveAudioController.onBeforeStudioPlayerPlay(player)
                val started = player.play()
                if (!started) {
                    val msg = if (_uiState.value.language == AppLanguage.EN) {
                        "Please record audio or import an audio file first"
                    } else {
                        "يرجى تسجيل صوت أو استيراد ملف صوتي أولاً"
                    }
                    postBanner(msg)
                }
            }
        } catch (e: Exception) {
            Log.e("StudioViewModel", "togglePlayPause failed safely", e)
            val msg = if (_uiState.value.language == AppLanguage.EN) {
                "Please record audio or import an audio file first"
            } else {
                "يرجى تسجيل صوت أو استيراد ملف صوتي أولاً"
            }
            postBanner(msg)
        }
    }

    fun stopPlayback() {
        ExclusiveAudioController.stopAllExclusive()
        player.stop()
        stopMixPreviewPlayer()
        SonixMediaPlaybackService.stopService(getApplication())
    }

    fun setSleepTimer(option: SleepTimerOption) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        val mins = option.minutes
        if (mins == null || mins <= 0) {
            _uiState.update { it.copy(sleepTimerOption = SleepTimerOption.OFF, sleepTimerRemainingSec = 0) }
            return
        }
        val totalSec = mins * 60
        _uiState.update { it.copy(sleepTimerOption = option, sleepTimerRemainingSec = totalSec) }
        sleepTimerJob = viewModelScope.launch {
            var remaining = totalSec
            while (remaining > 0) {
                delay(1000L)
                remaining -= 1
                _uiState.update { it.copy(sleepTimerRemainingSec = remaining) }
            }
            if (_uiState.value.playback.isPlaying) {
                player.pause()
            }
            stopMixPreviewPlayer()
            _uiState.update { it.copy(sleepTimerOption = SleepTimerOption.OFF, sleepTimerRemainingSec = 0) }
            postBanner(t(_uiState.value.language, "sleep_timer_triggered"))
        }
    }

    fun seekToFraction(frac: Float) {
        player.seekToFraction(frac)
    }

    fun toggleLoopSelection() {
        val next = !_uiState.value.loopSelection
        _uiState.update { it.copy(loopSelection = next) }
        val s = _uiState.value
        player.setLoop(next || s.autoLoopOnPlayback, s.trimStart, s.trimEnd)
    }

    fun setAutoLoopOnPlayback(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveAutoLoop(enabled)
        }
        val s = _uiState.value
        player.setLoop(s.loopSelection || enabled, s.trimStart, s.trimEnd)
    }

    // Recording & Import
    fun hasMicPermission(): Boolean = recorder.hasMicPermission()

    fun setMicPermissionDeniedError() {
        _uiState.update {
            it.copy(micPermissionError = t(it.language, "mic_permission_denied"))
        }
    }

    fun startRecording() {
        _uiState.update { it.copy(micPermissionError = null) }
        player.stop()
        val ok = recorder.start()
        if (!ok) {
            setMicPermissionDeniedError()
        }
    }

    fun pauseRecording() = recorder.pause()
    fun resumeRecording() = recorder.resume()
    fun discardRecording() = recorder.discard()

    fun finishRecording() {
        viewModelScope.launch {
            val buf = recorder.stopAndBuildBuffer()
            if (buf == null || buf.frameCount == 0) {
                postBanner("التسجيل قصير جداً أو لم يتم التقاط صوت")
                return@launch
            }
            setActiveBuffer(buf, recordUndo = false, resetDspOnNewEntry = true)
            val nowStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            repository.indexAudioRecording("تسجيل $nowStr", buf, RecordingSourceType.RECORDING)
            postBanner("تم تحميل التسجيل بالصوت الأصلي النظيف (0%) في الاستوديو")
        }
    }

    fun importAudioUri(
        uri: Uri,
        onComplete: (() -> Unit)? = null,
    ) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            withContext(Dispatchers.IO) {
                runCatching {
                    app.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
            }
            val resolvedName = withContext(Dispatchers.IO) {
                AudioFileUploader.resolveDisplayName(app, uri)
            }
            _uiState.update {
                it.copy(
                    isBusy = true,
                    isProcessing = true,
                    importProgress = 0f,
                    importLabel = resolvedName,
                    importBytesLabel = null,
                    importBytesProgress = null,
                )
            }
            var tempCachedFile: File? = null
            try {
                player.stop()
                // Phase 1 (0% -> 50%): Copy URI into context.cacheDir
                val uploadOutcome = AudioFileUploader.copyAudioUriToCacheDetailed(app, uri) { info ->
                    val bytesText = if (info.totalBytes > 0L) {
                        "${AudioFileUploader.formatBytes(info.bytesCopied)} / ${AudioFileUploader.formatBytes(info.totalBytes)}"
                    } else {
                        AudioFileUploader.formatBytes(info.bytesCopied)
                    }
                    _uiState.update {
                        it.copy(
                            importProgress = (info.fraction * 0.50f).coerceIn(0f, 0.50f),
                            importLabel = info.fileName,
                            importBytesLabel = bytesText,
                            importBytesProgress = info.fraction.takeIf { info.totalBytes > 0L },
                        )
                    }
                }
                val uploaded = when (uploadOutcome) {
                    is AudioUploadOutcome.Success -> uploadOutcome.uploaded
                    is AudioUploadOutcome.Failure -> {
                        postBanner("تعذر نسخ الملف الصوتي: ${uploadOutcome.errorMessage}")
                        return@launch
                    }
                }
                tempCachedFile = uploaded.file

                // Phase 2 (50% -> 100%): Prepare player & extract waveform peaks
                _uiState.update {
                    it.copy(
                        importProgress = 0.50f,
                        importLabel = "جاري تجهيز الصوت والموجة: ${uploaded.displayName}",
                        importBytesLabel = null,
                        importBytesProgress = null,
                    )
                }

                val title = uploaded.displayName.substringBeforeLast('.').ifEmpty { "ملف مستورد" }
                if (withContext(Dispatchers.IO) { AudioDecoder.shouldStreamOnDisk(uploaded.file) }) {
                    // Zero-RAM Guard: Never decode full large/long audio files into RAM;
                    // inspect <1s via 8192-byte streaming & pass file.absolutePath to player/FFmpegKit.
                    val stagedFile = stageLargeFileToCleanAsciiCache(app, uploaded.file, prefix = "main")
                    tempCachedFile = null // Ownership transferred to staged activeSourceFile
                    val meta = AudioDecoder.inspectLargeAudioStreaming(stagedFile, buckets = 800, context = app) { frac ->
                        _uiState.update {
                            it.copy(importProgress = (0.50f + frac * 0.46f).coerceIn(0.50f, 0.96f))
                        }
                    }
                    if (meta == null) {
                        runCatching { stagedFile.delete() }
                        postBanner("تعذر قراءة الملف الصوتي الكبير: ${uploaded.displayName}")
                        return@launch
                    }
                    undoStack.clear()
                    redoStack.clear()
                    lastDspSnapshotTimeMs = 0L
                    activeBuffer = null
                    replaceActiveSourceFile(stagedFile)
                    activeSourceSampleRate = meta.sampleRate
                    val cleanCfg = DspConfig()
                    syncPlayerConfig(cleanCfg, holdBypass = false)
                    player.setClipGainDb(0f)
                    player.setSourceFile(
                        file = stagedFile,
                        durationMs = meta.durationMs,
                        sampleRate = meta.sampleRate,
                        resetPosition = true,
                    )
                    _uiState.update { cur ->
                        cur.copy(
                            hasAudio = true,
                            durationMs = meta.durationMs,
                            waveformPeaks = meta.waveformPeaks,
                            originalPeakDbfs = meta.peakDbfs,
                            dspConfig = cleanCfg,
                            activePresetId = null,
                            selectedPresetLabel = "",
                            activeCloudDraftId = null,
                            isHoldBypassing = false,
                            audioMarkers = emptyList(),
                            trimStart = 0f,
                            trimEnd = 1f,
                            canUndoEdit = false,
                            canRedoEdit = false,
                            hasUnsavedChanges = false,
                            activeProjectName = if (cur.activeProjectId == 0L) title else cur.activeProjectName,
                            importProgress = 1.0f,
                        )
                    }
                    postBanner("تم تجهيز الملف ($title) بالتدفق المباشر الآمن (0% تأثيرات)")
                    onComplete?.invoke()
                } else {
                    val decodeOutcome = AudioDecoder.decodeFileDetailedWithProgress(uploaded.file) { decFrac ->
                        _uiState.update {
                            it.copy(importProgress = (0.50f + decFrac * 0.46f).coerceIn(0.50f, 0.96f))
                        }
                    }
                    val buf = when (decodeOutcome) {
                        is AudioDecodeOutcome.Success -> decodeOutcome.buffer
                        is AudioDecodeOutcome.Failure -> {
                            postBanner("تعذر قراءة الملف الصوتي: ${decodeOutcome.reason}")
                            return@launch
                        }
                    }
                    if (buf.frameCount == 0) {
                        postBanner("تعذر قراءة الملف الصوتي: ملف فارغ (0 إطارات)")
                        return@launch
                    }
                    setActiveBuffer(buf, recordUndo = false, resetDspOnNewEntry = true)
                    if (_uiState.value.activeProjectId == 0L) {
                        _uiState.update { it.copy(activeProjectName = title) }
                    }
                    repository.indexAudioRecording(title, buf, RecordingSourceType.IMPORTED)
                    _uiState.update { it.copy(importProgress = 1.0f) }
                    postBanner("تم استيراد الملف ($title) بالصوت الأصلي النقي (0%) جاهزاً للمعالجة")
                    onComplete?.invoke()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                AppDiagnosticLog.logRecoverable(
                    toolName = "ImportAudio",
                    titleAr = "تعذر استيراد الملف الصوتي",
                    titleEn = "Audio Import Failed",
                    causeAr = t.localizedMessage ?: t.javaClass.simpleName,
                    causeEn = t.localizedMessage ?: t.javaClass.simpleName,
                    errorCode = t.javaClass.simpleName,
                    throwable = t,
                )
                postBanner("تعذر استيراد الملف الصوتي: ${t.javaClass.simpleName}: ${t.message}")
            } finally {
                runCatching { tempCachedFile?.delete() }
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        isProcessing = false,
                        importProgress = null,
                        importLabel = null,
                        importBytesLabel = null,
                        importBytesProgress = null,
                    )
                }
            }
        }
    }

    // Purifier
    fun applyVoicePurifyPreset(preset: VoicePurifyPreset) {
        updateDspConfig { prev ->
            prev.copy(
                bypassAll = false,
                noiseReduction = prev.noiseReduction.copy(
                    thresholdDb = preset.thresholdDb,
                    reductionDb = preset.reductionDb,
                    highPassHz = preset.highPassHz,
                    lowPassHz = preset.lowPassHz,
                    spectralStrength = preset.spectralStrength,
                    deEssAmount = preset.deEssAmount,
                    clarityBoostDb = preset.clarityBoostDb,
                    spectralCleanerEnabled = preset.spectralCleanerEnabled,
                ),
            ).withSlotEnabled(EffectType.NOISE_REDUCTION, true)
        }
        val title = if (_uiState.value.language == AppLanguage.EN) preset.titleEn else preset.titleAr
        postBanner("تم تطبيق وضع التنقية: $title")
    }

    fun purifyAudioOffline() {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner("سجّل أو استورد مقطعاً صوتياً أولاً لتنقيته")
            return
        }
        launchSingleFlightProcessing(purifyInitProgress = 0.05f) {
            val nrParams = _uiState.value.dspConfig.noiseReduction
            val snap = activeLiveToolSession?.takeIf { it.toolKey == "voice_purifier" }
            if (snap != null) {
                while (undoStack.size > snap.undoSize) {
                    undoStack.removeLast()
                }
                snap.committed = true
            }
            sliderDebounceJob?.cancel()
            val bakedCfg = _uiState.value.dspConfig.withSlotEnabled(EffectType.NOISE_REDUCTION, false)
            syncPlayerConfig(bakedCfg)
            _uiState.update { it.copy(dspConfig = bakedCfg, lastAppliedToolLabel = "VoicePurifier") }
            if (src != null) {
                val cleaned = purifyBufferOffline(src, nrParams) { p ->
                    _uiState.update { it.copy(purifyProgress = p) }
                }
                setActiveBuffer(cleaned, recordUndo = true)
            } else if (srcFile != null) {
                val hp = nrParams.highPassHz.toInt().coerceIn(20, 400)
                val lp = nrParams.lowPassHz.toInt().coerceIn(3000, 19000)
                val nr = nrParams.reductionDb.toInt().coerceIn(4, 40)
                val outFile = processAudioFileWithFfmpeg(
                    context = getApplication(),
                    inputFile = srcFile,
                    audioFilter = "highpass=f=$hp,lowpass=f=$lp,afftdn=nr=$nr:nf=-25",
                    toolName = "VoicePurifier",
                )
                if (outFile == null) {
                    postBanner("تعذر تنقية الملف الصوتي الكبير")
                    return@launchSingleFlightProcessing
                }
                applyProcessedDiskSourceFile(outFile, appliedToolLabel = "VoicePurifier")
            }
            postBanner("تمت تصفية وعزل الضوضاء من المقطع وحفظ النسخة في التراجع")
        }
    }

    // Editor Ops
    fun beginClipGainSession() {
        preEntryClipGainDb = _uiState.value.clipGainDb
        clipGainUnappliedDirty = false
    }

    fun discardUnapprovedClipGainSession() {
        if (!clipGainUnappliedDirty) return
        clipGainUnappliedDirty = false
        if (livePreviewStartedPlayback) {
            livePreviewStartedPlayback = false
            if (_uiState.value.playback.isPlaying) {
                player.pause()
            }
        }
        _uiState.update { it.copy(clipGainDb = preEntryClipGainDb) }
        player.setClipGainDb(0f)
    }

    fun updateTrimRange(s: Float, e: Float) {
        val durMs = _uiState.value.durationMs
        val minGapFrac = if (durMs > 500L) (500f / durMs.toFloat()).coerceIn(0.0005f, 0.98f) else 0.02f
        val curStart = _uiState.value.trimStart
        val curEnd = _uiState.value.trimEnd
        val movingStart = abs(s - curStart) >= abs(e - curEnd)
        var cs: Float
        var ce: Float
        if (movingStart) {
            cs = clamp(s, 0f, (curEnd - minGapFrac).coerceAtLeast(0f))
            ce = clamp(e, (cs + minGapFrac).coerceAtMost(1f), 1f)
        } else {
            ce = clamp(e, (curStart + minGapFrac).coerceAtMost(1f), 1f)
            cs = clamp(s, 0f, (ce - minGapFrac).coerceAtLeast(0f))
        }
        if (ce <= cs) {
            cs = 0f
            ce = 1f
        }
        _uiState.update { it.copy(trimStart = cs, trimEnd = ce) }
        val st = _uiState.value
        player.setLoop(st.loopSelection || st.autoLoopOnPlayback, cs, ce)
        player.setPlaybackBounds(editorPlaybackBoundsEnabled, cs, ce)
    }

    fun detectSpeechBoundsWithSileroVad() {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner(t(_uiState.value.language, "no_audio_loaded"))
            return
        }
        launchSingleFlightProcessing {
            val result = SileroVadProcessor.detectSpeechAndSilence(
                context = getApplication(),
                buffer = src,
                diskFile = srcFile,
                vadThreshold = 0.40f,
                minSilenceMs = 380,
                minSpeechMs = 150,
            ) { p ->
                _uiState.update { it.copy(purifyProgress = p.coerceIn(0.05f, 0.98f)) }
            }
            if (result.speechSegments.isEmpty() || result.totalDurationMs <= 0L) {
                _uiState.update { it.copy(vadSpeechSegments = emptyList(), purifyProgress = null) }
                postBanner("لم يتم اكتشاف مقاطع كلام واضحة")
                return@launchSingleFlightProcessing
            }
            val durF = result.totalDurationMs.toFloat().coerceAtLeast(1f)
            val firstStartFrac = (result.speechSegments.first().startMs / durF).coerceIn(0f, 0.98f)
            val lastEndFrac = (result.speechSegments.last().endMs / durF).coerceIn(firstStartFrac + 0.01f, 1f)
            _uiState.update {
                it.copy(
                    vadSpeechSegments = result.speechSegments,
                    vadUsedSilero = result.usedSileroOnnx,
                    purifyProgress = 1f,
                )
            }
            updateTrimRange(firstStartFrac, lastEndFrac)
            val engineTag = if (result.usedSileroOnnx) "Silero VAD" else "VAD"
            postBanner("تم تحديد ${result.speechSegments.size} مقطع كلام عبر $engineTag (${result.silenceSegments.size} فترة صمت)")
        }
    }

    fun removeUnwantedSilenceWithSileroVad() {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner(t(_uiState.value.language, "no_audio_loaded"))
            return
        }
        stopToolPreview()
        launchSingleFlightProcessing {
            if (src != null) {
                val cleaned = removeSilenceAuto(
                    context = getApplication(),
                    buffer = src,
                    thresholdDb = -38f,
                    minSilenceSec = 0.42f,
                )
                setActiveBuffer(cleaned, recordUndo = true)
            } else if (srcFile != null) {
                val sileroOut = SileroVadProcessor.removeSilenceFileWithSileroVad(
                    context = getApplication(),
                    inputFile = srcFile,
                    thresholdDb = -38f,
                    minSilenceSec = 0.42f,
                )
                val outFile = sileroOut ?: processAudioFileWithFfmpeg(
                    context = getApplication(),
                    inputFile = srcFile,
                    audioFilter = "silenceremove=start_periods=1:start_duration=0.05:start_threshold=-38dB:stop_periods=-1:stop_duration=0.42:stop_threshold=-38dB",
                    toolName = "EditorVadSilenceRemoveLarge",
                ) ?: run {
                    postBanner("تعذر إزالة فترات الصمت")
                    return@launchSingleFlightProcessing
                }
                applyProcessedDiskSourceFile(outFile, appliedToolLabel = "SileroVadSilenceRemove")
            }
            postBanner("تمت إزالة فترات الصمت مع الحفاظ على مخارج الحروف والكلمات")
        }
    }

    fun resetSelection() {
        _uiState.update { it.copy(trimStart = 0f, trimEnd = 1f) }
        val st = _uiState.value
        player.setLoop(st.loopSelection || st.autoLoopOnPlayback, 0f, 1f)
        player.setPlaybackBounds(editorPlaybackBoundsEnabled, 0f, 1f)
        postBanner("تم تحديد المقطع بالكامل")
    }

    fun setClipGainDb(db: Float) {
        clipGainUnappliedDirty = true
        _uiState.update { it.copy(clipGainDb = db) }
        player.setClipGainDb(db)
        if (abs(db) > 0.05f) {
            ensureLivePreviewPlaying()
        }
    }

    fun bakeClipGainDb(db: Float) {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) return
        clipGainUnappliedDirty = false
        preEntryClipGainDb = db
        launchSingleFlightProcessing {
            if (src != null) {
                val gained = withContext(Dispatchers.Default) { applyGainDb(src, db) }
                setActiveBuffer(gained, recordUndo = true)
            } else if (srcFile != null) {
                val dbFmt = String.format(Locale.US, "%.2f", db)
                val outFile = processAudioFileWithFfmpeg(
                    context = getApplication(),
                    inputFile = srcFile,
                    audioFilter = "volume=${dbFmt}dB,alimiter=limit=0.98",
                    toolName = "BakeClipGain",
                ) ?: run {
                    postBanner("تعذر تعديل شدة الصوت")
                    return@launchSingleFlightProcessing
                }
                applyProcessedDiskSourceFile(outFile, appliedToolLabel = "ClipGain")
            }
            // Keep clipGainDb slider position preserved after Apply, clear player double gain
            player.setClipGainDb(0f)
            postBanner("تم تعديل شدة الصوت في المقطع بنجاح")
        }
    }

    fun resetClipGainDb() {
        clipGainUnappliedDirty = false
        preEntryClipGainDb = 0f
        _uiState.update { it.copy(clipGainDb = 0f) }
        player.setClipGainDb(0f)
    }

    private suspend fun applyTrimSelection(
        autoFade: Boolean,
        recordUndo: Boolean,
    ): Boolean {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) return false
        val st = _uiState.value
        if (src != null) {
            val trimmed = withContext(Dispatchers.Default) {
                trimToSelectionWithFade(src, st.trimStart, st.trimEnd, autoFadeSec = if (autoFade) 0.2f else 0f)
            }
            setActiveBuffer(trimmed, recordUndo = recordUndo)
        } else if (srcFile != null) {
            val durSec = (st.durationMs / 1000.0).coerceAtLeast(0.2)
            val sSec = (durSec * st.trimStart).coerceIn(0.0, durSec - 0.05)
            val eSec = (durSec * st.trimEnd).coerceIn(sSec + 0.05, durSec)
            val segLen = (eSec - sSec).coerceAtLeast(0.05)
            val fadeFilter = if (autoFade && segLen > 0.45) {
                val fadeOutStart = String.format(Locale.US, "%.3f", segLen - 0.2)
                "afade=t=in:st=0:d=0.2,afade=t=out:st=$fadeOutStart:d=0.2"
            } else {
                ""
            }
            val ssStr = String.format(Locale.US, "%.3f", sSec)
            val toStr = String.format(Locale.US, "%.3f", eSec)
            val outFile = processAudioFileWithFfmpeg(
                context = getApplication(),
                inputFile = srcFile,
                preInputArgs = "-ss $ssStr -to $toStr",
                audioFilter = fadeFilter,
                toolName = "TrimKeepLarge",
            ) ?: run {
                postBanner("تعذر اقتصاص الملف الصوتي الكبير")
                return false
            }
            applyProcessedDiskSourceFile(
                outFile,
                recordUndo = recordUndo,
                appliedToolLabel = "TrimKeep",
            )
            if (activeSourceFile != outFile) return false
        }
        return true
    }

    fun trimKeepSelection(autoFade: Boolean = false) {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) return
        launchSingleFlightProcessing {
            if (applyTrimSelection(autoFade = autoFade, recordUndo = true)) {
                postBanner("تم اقتصاص المقطع والإبقاء على الجزء المحدد")
            }
        }
    }

    fun cutSelectedRange(autoFade: Boolean = false) {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) return
        val st = _uiState.value
        launchSingleFlightProcessing {
            if (src != null) {
                val cut = withContext(Dispatchers.Default) {
                    cutSelectionWithFade(src, st.trimStart, st.trimEnd, autoFadeSec = if (autoFade) 0.2f else 0f)
                }
                setActiveBuffer(cut, recordUndo = true)
            } else if (srcFile != null) {
                val durSec = (st.durationMs / 1000.0).coerceAtLeast(0.2)
                val sSec = (durSec * st.trimStart).coerceIn(0.0, durSec - 0.05)
                val eSec = (durSec * st.trimEnd).coerceIn(sSec + 0.05, durSec)
                val app = getApplication<Application>()
                val outFile = withContext(Dispatchers.IO) {
                    val safeIn = SmartCacheManager.ensureSafeFfmpegInputFile(app, srcFile)
                    val stagedDir = File(app.cacheDir, "staged_inputs").apply { mkdirs() }
                    val target = File(stagedDir, "ff_cut_${UUID.randomUUID()}.m4a")
                    val cmd = when {
                        sSec <= 0.02 -> {
                            val startAt = String.format(Locale.US, "%.3f", eSec)
                            "-y -ss $startAt -i \"${safeIn.absolutePath}\" -af \"afade=t=in:st=0:d=0.015\" -vn -ar 44100 -ac 2 -c:a aac -b:a 192k \"${target.absolutePath}\""
                        }
                        eSec >= durSec - 0.02 -> {
                            val endAt = String.format(Locale.US, "%.3f", sSec)
                            val fadeStart = String.format(Locale.US, "%.3f", (sSec - 0.015).coerceAtLeast(0.0))
                            "-y -i \"${safeIn.absolutePath}\" -t $endAt -af \"afade=t=out:st=$fadeStart:d=0.015\" -vn -ar 44100 -ac 2 -c:a aac -b:a 192k \"${target.absolutePath}\""
                        }
                        else -> {
                            val sStr = String.format(Locale.US, "%.3f", sSec)
                            val eStr = String.format(Locale.US, "%.3f", eSec)
                            val fc = "[0:a]atrim=duration=$sStr,asetpts=PTS-STARTPTS[a0];[0:a]atrim=start=$eStr,asetpts=PTS-STARTPTS[a1];[a0][a1]acrossfade=d=0.015:c1=tri:c2=tri[out]"
                            "-y -i \"${safeIn.absolutePath}\" -filter_complex \"$fc\" -map \"[out]\" -vn -ar 44100 -ac 2 -c:a aac -b:a 192k \"${target.absolutePath}\""
                        }
                    }
                    val ok = com.example.diagnostics.FfmpegKitBridge.execute(cmd, toolName = "CutSelectionLarge", logNonZeroFailure = true)
                    if (safeIn != srcFile) runCatching { safeIn.delete() }
                    if (ok && target.exists() && target.length() > 512L) target else {
                        runCatching { target.delete() }
                        null
                    }
                } ?: run {
                    postBanner("تعذر حذف الجزء المحدد من الملف الكبير")
                    return@launchSingleFlightProcessing
                }
                applyProcessedDiskSourceFile(outFile, appliedToolLabel = "CutSelection")
            }
            postBanner("تم حذف الجزء المحدد ووصل الأطراف بنجاح")
        }
    }

    fun normalizeAudioBuffer() {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner("لا يوجد صوت لموازنته حالياً")
            return
        }
        launchSingleFlightProcessing {
            if (src != null) {
                val normed = withContext(Dispatchers.Default) { normalizeAudio(src, -1f) }
                setActiveBuffer(normed, recordUndo = true)
            } else if (srcFile != null) {
                val outFile = processAudioFileWithFfmpeg(
                    context = getApplication(),
                    inputFile = srcFile,
                    audioFilter = "loudnorm=I=-16:TP=-1.0:LRA=11",
                    toolName = "NormalizeAudioLarge",
                ) ?: run {
                    postBanner("تعذر موازنة الملف الصوتي الكبير")
                    return@launchSingleFlightProcessing
                }
                applyProcessedDiskSourceFile(outFile, appliedToolLabel = "Normalize")
            }
            postBanner("تمت موازنة مستوى الصوت إلى -1 dBFS")
        }
    }

    fun applyFadeInSeconds(seconds: Float) {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner("قم بتسجيل أو استيراد صوت أولاً لتطبيق التدرج")
            return
        }
        launchSingleFlightProcessing {
            if (src != null) {
                val faded = withContext(Dispatchers.Default) { applyFadeIn(src, seconds) }
                setActiveBuffer(faded, recordUndo = true)
            } else if (srcFile != null) {
                val secStr = String.format(Locale.US, "%.2f", seconds.coerceAtLeast(0.05f))
                val outFile = processAudioFileWithFfmpeg(
                    context = getApplication(),
                    inputFile = srcFile,
                    audioFilter = "afade=t=in:st=0:d=$secStr",
                    toolName = "FadeInLarge",
                ) ?: run {
                    postBanner("تعذر تطبيق الظهور التدريجي")
                    return@launchSingleFlightProcessing
                }
                applyProcessedDiskSourceFile(outFile, appliedToolLabel = "FadeIn")
            }
            postBanner("تم تطبيق الظهور التدريجي (${"%.1f".format(seconds)} ثانية)")
        }
    }

    fun applyFadeOutSeconds(seconds: Float) {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner("قم بتسجيل أو استيراد صوت أولاً لتطبيق التدرج")
            return
        }
        val durSec = (_uiState.value.durationMs / 1000.0).coerceAtLeast(0.1)
        launchSingleFlightProcessing {
            if (src != null) {
                val faded = withContext(Dispatchers.Default) { applyFadeOut(src, seconds) }
                setActiveBuffer(faded, recordUndo = true)
            } else if (srcFile != null) {
                val fadeLen = seconds.toDouble().coerceIn(0.05, durSec)
                val startAt = String.format(Locale.US, "%.3f", (durSec - fadeLen).coerceAtLeast(0.0))
                val lenStr = String.format(Locale.US, "%.2f", fadeLen)
                val outFile = processAudioFileWithFfmpeg(
                    context = getApplication(),
                    inputFile = srcFile,
                    audioFilter = "afade=t=out:st=$startAt:d=$lenStr",
                    toolName = "FadeOutLarge",
                ) ?: run {
                    postBanner("تعذر تطبيق الاختفاء التدريجي")
                    return@launchSingleFlightProcessing
                }
                applyProcessedDiskSourceFile(outFile, appliedToolLabel = "FadeOut")
            }
            postBanner("تم تطبيق الاختفاء التدريجي (${"%.1f".format(seconds)} ثانية)")
        }
    }

    fun applyFadeInOutSeconds(fadeInSec: Float, fadeOutSec: Float) {
        val hasIn = fadeInSec > 0.02f
        val hasOut = fadeOutSec > 0.02f
        if (!hasIn && !hasOut) {
            postBanner("اضبط مدة التلاشي أولاً")
            return
        }
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner("قم بتسجيل أو استيراد صوت أولاً لتطبيق التدرج")
            return
        }
        val durSec = (_uiState.value.durationMs / 1000.0).coerceAtLeast(0.1)
        launchSingleFlightProcessing {
            if (src != null) {
                val faded = withContext(Dispatchers.Default) {
                    var res = src
                    if (hasIn) res = applyFadeIn(res, fadeInSec)
                    if (hasOut) res = applyFadeOut(res, fadeOutSec)
                    res
                }
                setActiveBuffer(faded, recordUndo = true)
            } else if (srcFile != null) {
                val filters = buildList {
                    if (hasIn) {
                        add("afade=t=in:st=0:d=${String.format(Locale.US, "%.2f", fadeInSec.coerceAtLeast(0.05f))}")
                    }
                    if (hasOut) {
                        val fadeLen = fadeOutSec.toDouble().coerceIn(0.05, durSec)
                        val startAt = String.format(Locale.US, "%.3f", (durSec - fadeLen).coerceAtLeast(0.0))
                        add("afade=t=out:st=$startAt:d=${String.format(Locale.US, "%.2f", fadeLen)}")
                    }
                }.joinToString(",")
                val outFile = processAudioFileWithFfmpeg(
                    context = getApplication(),
                    inputFile = srcFile,
                    audioFilter = filters,
                    toolName = "FadeInOutLarge",
                ) ?: run {
                    postBanner("تعذر تطبيق التلاشي التدريجي")
                    return@launchSingleFlightProcessing
                }
                applyProcessedDiskSourceFile(outFile, appliedToolLabel = "FadeInOut")
            }
            postBanner("تم تطبيق التلاشي التدريجي بنجاح")
        }
    }

    fun loadMixBackgroundUri(
        uri: Uri,
        onComplete: (() -> Unit)? = null,
    ) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            withContext(Dispatchers.IO) {
                runCatching {
                    app.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
            }
            val name = withContext(Dispatchers.IO) {
                AudioFileUploader.resolveDisplayName(app, uri)
            }
            _uiState.update {
                it.copy(
                    isBusy = true,
                    isProcessing = true,
                    importProgress = 0f,
                    importLabel = name,
                    importBytesLabel = null,
                    importBytesProgress = null,
                )
            }
            var cachedFile: File? = null
            try {
                // Phase 1 (0% -> 50%): Copy URI into context.cacheDir
                val uploadOutcome = AudioFileUploader.copyAudioUriToCacheDetailed(app, uri) { info ->
                    val bytesText = if (info.totalBytes > 0L) {
                        "${AudioFileUploader.formatBytes(info.bytesCopied)} / ${AudioFileUploader.formatBytes(info.totalBytes)}"
                    } else {
                        AudioFileUploader.formatBytes(info.bytesCopied)
                    }
                    _uiState.update {
                        it.copy(
                            importProgress = (info.fraction * 0.50f).coerceIn(0f, 0.50f),
                            importLabel = info.fileName,
                            importBytesLabel = bytesText,
                            importBytesProgress = info.fraction.takeIf { info.totalBytes > 0L },
                        )
                    }
                }
                val uploaded = when (uploadOutcome) {
                    is AudioUploadOutcome.Success -> uploadOutcome.uploaded
                    is AudioUploadOutcome.Failure -> {
                        postBanner("تعذر نسخ ملف الخلفية الصوتية: ${uploadOutcome.errorMessage}")
                        return@launch
                    }
                }
                cachedFile = uploaded.file

                // Phase 2 (50% -> 100%): Prepare background & extract peaks without locking at 100%
                _uiState.update {
                    it.copy(
                        importProgress = 0.50f,
                        importLabel = "جاري تجهيز الخلفية والموجة: ${uploaded.displayName}",
                        importBytesLabel = null,
                        importBytesProgress = null,
                    )
                }

                if (withContext(Dispatchers.IO) { AudioDecoder.shouldStreamOnDisk(uploaded.file) }) {
                    val stagedBg = stageLargeFileToCleanAsciiCache(app, uploaded.file, prefix = "bg")
                    cachedFile = null // Ownership transferred to mixBgSourceFile
                    val meta = AudioDecoder.inspectLargeAudioStreaming(stagedBg, buckets = 240, context = app) { frac ->
                        _uiState.update {
                            it.copy(importProgress = (0.50f + frac * 0.46f).coerceIn(0.50f, 0.96f))
                        }
                    }
                    if (meta == null) {
                        runCatching { stagedBg.delete() }
                        postBanner("تعذر قراءة ملف الخلفية الصوتية الكبير: ${uploaded.displayName}")
                        return@launch
                    }
                    mixBgBuffer = null
                    replaceMixBgSourceFile(stagedBg)
                    _uiState.update {
                        it.copy(
                            mixBgName = uploaded.displayName,
                            mixBgDurationMs = meta.durationMs,
                            mixBgPeaks = meta.waveformPeaks,
                            importProgress = 1.0f,
                        )
                    }
                    postBanner("تم تجهيز مسار الخلفية الكبير بالتدفق الآمن جاهزاً للمزج عبر FFmpeg")
                    onComplete?.invoke()
                } else {
                    val decodeOutcome = AudioDecoder.decodeFileDetailedWithProgress(uploaded.file) { decFrac ->
                        _uiState.update {
                            it.copy(importProgress = (0.50f + decFrac * 0.46f).coerceIn(0.50f, 0.96f))
                        }
                    }
                    val bg = when (decodeOutcome) {
                        is AudioDecodeOutcome.Success -> decodeOutcome.buffer
                        is AudioDecodeOutcome.Failure -> {
                            postBanner("تعذر قراءة ملف الخلفية الصوتية: ${decodeOutcome.reason}")
                            return@launch
                        }
                    }
                    if (bg.frameCount == 0) {
                        postBanner("تعذر قراءة ملف الخلفية الصوتية: ملف فارغ (0 إطارات)")
                        return@launch
                    }
                    replaceMixBgSourceFile(null)
                    mixBgBuffer = bg
                    val bgPeaks = computeWaveformPeaksAsync(bg, 96)
                    _uiState.update {
                        it.copy(
                            mixBgName = uploaded.displayName,
                            mixBgDurationMs = bg.durationMs,
                            mixBgPeaks = bgPeaks,
                        )
                    }
                    scheduleSmartDuckingPreviewRender()
                    _uiState.update { it.copy(importProgress = 1.0f) }
                    postBanner("تم تحميل مسار الخلفية بنجاح، يمكنك الآن ضبط المستويات والمعاينة التلقائية")
                    onComplete?.invoke()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                AppDiagnosticLog.logRecoverable(
                    toolName = "LoadMixBackground",
                    titleAr = "تعذر تحميل ملف الخلفية الصوتية",
                    titleEn = "Background Track Load Failed",
                    causeAr = t.localizedMessage ?: t.javaClass.simpleName,
                    causeEn = t.localizedMessage ?: t.javaClass.simpleName,
                    errorCode = t.javaClass.simpleName,
                    throwable = t,
                )
                postBanner("تعذر قراءة ملف الخلفية الصوتية: ${t.javaClass.simpleName}: ${t.message}")
            } finally {
                runCatching { cachedFile?.delete() }
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        isProcessing = false,
                        importProgress = null,
                        importLabel = null,
                        importBytesLabel = null,
                        importBytesProgress = null,
                    )
                }
            }
        }
    }

    fun clearMixBackgroundTrack() {
        mixBgBuffer = null
        replaceMixBgSourceFile(null)
        stopMixPreviewPlayer()
        player.setMergePreviewBuffers(false, null, null, MergeStudioParams(), null)
        _uiState.update {
            it.copy(
                mixBgName = null,
                mixBgDurationMs = 0L,
                mixBgPeaks = emptyList(),
                isSmartMixPreviewActive = false,
            )
        }
    }

    fun setSmartMixBgVolume(volume: Float) {
        val clamped = clamp(volume, 0f, 1.0f)
        _uiState.update { it.copy(smartMixBgVolume = clamped) }
        scheduleSmartDuckingPreviewRender()
    }

    private fun scheduleSmartDuckingPreviewRender() {
        val voice = activeBuffer ?: return
        val music = mixBgBuffer ?: return
        val bgVol = _uiState.value.smartMixBgVolume
        smartMixPreviewJob?.cancel()
        smartMixPreviewJob = viewModelScope.launch(Dispatchers.Default) {
            delay(260L)
            val mixed = mixWithSidechainAutoDucking(
                voice = voice,
                music = music,
                musicBaseVolume = bgVol,
            )
            val tmpFile = File(getApplication<Application>().cacheDir, "temp_preview.wav.tmp")
            val finalFile = previewTempFile()
            writeStereoWavFile(tmpFile, mixed)
            if (!tmpFile.renameTo(finalFile)) {
                writeStereoWavFile(finalFile, mixed)
                tmpFile.delete()
            }
            _uiState.update {
                it.copy(
                    previewWavPath = finalFile.absolutePath,
                    isSmartMixPreviewActive = true,
                )
            }
        }
    }

    fun toggleSmartDuckingPreviewPlayback() {
        val existingMixPreview = _uiState.value.previewWavPath?.let(::File)
        if (
            _uiState.value.isSmartMixPreviewActive &&
            existingMixPreview != null &&
            existingMixPreview.absolutePath == toolPreviewPlaybackFile?.absolutePath
        ) {
            if (_playbackTelemetry.value.isPlaying) {
                player.pause()
            } else {
                ExclusiveAudioController.onBeforeStudioPlayerPlay(player)
                player.play()
            }
            return
        }
        val voice = activeBuffer
        val voiceFile = activeSourceFile
        val music = mixBgBuffer
        val musicFile = mixBgSourceFile
        if ((voice == null && voiceFile == null) || (music == null && musicFile == null)) {
            postBanner("يرجى اختيار مسار الصوت وموسيقى الخلفية أولاً")
            return
        }
        launchSingleFlightProcessing {
            player.pause()
            val finalFile = previewTempFile()
            if (voice != null && music != null) {
                val mixed = withContext(Dispatchers.Default) {
                    mixWithSidechainAutoDucking(
                        voice = voice,
                        music = music,
                        musicBaseVolume = _uiState.value.smartMixBgVolume,
                    )
                }
                withContext(Dispatchers.IO) {
                    writeStereoWavFile(finalFile, mixed)
                }
            } else {
                val (f1, del1) = resolveAsDiskSourceFile(voice, voiceFile, "pv_v")
                val (f2, del2) = resolveAsDiskSourceFile(music, musicFile, "pv_m")
                try {
                    if (f1 == null || f2 == null) {
                        postBanner("تعذر تجهيز ملفات المعاينة")
                        return@launchSingleFlightProcessing
                    }
                    mixAudioFilesWithFfmpeg(
                        context = getApplication(),
                        primaryFile = f1,
                        secondaryFile = f2,
                        primaryGain = 1.0f,
                        secondaryGain = _uiState.value.smartMixBgVolume,
                        autoDucking = true,
                        mode = MergeStudioMode.OVERLAY,
                        outFile = finalFile,
                    ) ?: run {
                        postBanner("تعذر توليد معاينة الدمج التلقائي عبر FFmpeg")
                        return@launchSingleFlightProcessing
                    }
                } finally {
                    if (del1) runCatching { f1?.delete() }
                    if (del2) runCatching { f2?.delete() }
                }
            }
            _uiState.update { it.copy(previewWavPath = finalFile.absolutePath, isSmartMixPreviewActive = true) }
            if (!startGlobalFilePreview(finalFile)) {
                postBanner("تعذر تشغيل معاينة الدمج التلقائي")
            }
        }
    }

    private fun stopMixPreviewPlayer() {
        val activePreview = toolPreviewPlaybackFile
        if (activePreview != null && activePreview.absolutePath == _uiState.value.previewWavPath) {
            stopToolPreview()
        } else {
            AudioPlayerManager.stop("mix_preview")
        }
    }

    fun applySmartDuckingMixToStudio() {
        val voice = activeBuffer
        val voiceFile = activeSourceFile
        val music = mixBgBuffer
        val musicFile = mixBgSourceFile
        if ((voice == null && voiceFile == null) || (music == null && musicFile == null)) {
            postBanner("يرجى اختيار مسار الصوت وموسيقى الخلفية أولاً")
            return
        }
        launchSingleFlightProcessing {
            stopMixPreviewPlayer()
            if (voice != null && music != null) {
                val mixed = withContext(Dispatchers.Default) {
                    mixWithSidechainAutoDucking(
                        voice = voice,
                        music = music,
                        musicBaseVolume = _uiState.value.smartMixBgVolume,
                    )
                }
                setActiveBuffer(mixed, recordUndo = true)
            } else {
                val (f1, del1) = resolveAsDiskSourceFile(voice, voiceFile, "dm_v")
                val (f2, del2) = resolveAsDiskSourceFile(music, musicFile, "dm_m")
                try {
                    if (f1 == null || f2 == null) {
                        postBanner("تعذر تجهيز مسارات الدمج")
                        return@launchSingleFlightProcessing
                    }
                    val outFile = mixAudioFilesWithFfmpeg(
                        context = getApplication(),
                        primaryFile = f1,
                        secondaryFile = f2,
                        primaryGain = 1.0f,
                        secondaryGain = _uiState.value.smartMixBgVolume,
                        autoDucking = true,
                        mode = MergeStudioMode.OVERLAY,
                    ) ?: run {
                        postBanner("تعذر تطبيق الدمج الذكي للملف الكبير")
                        return@launchSingleFlightProcessing
                    }
                    applyProcessedDiskSourceFile(outFile, appliedToolLabel = "SmartDuckingMix")
                } finally {
                    if (del1) runCatching { f1?.delete() }
                    if (del2) runCatching { f2?.delete() }
                }
            }
            postBanner("تم اعتماد الدمج الذكي (Auto-Ducking) وحفظ النسخة في التراجع")
        }
    }

    fun exportSmartDuckingMix(
        format: ExportAudioFormat,
        fileName: String,
        shareAfter: Boolean = false,
        targetPackage: String? = null,
    ) {
        val voice = activeBuffer
        val voiceFile = activeSourceFile
        if (voice == null && (voiceFile == null || !voiceFile.exists())) {
            postBanner(t(_uiState.value.language, "no_audio_loaded"))
            return
        }
        val music = mixBgBuffer
        val musicFile = mixBgSourceFile
        if (music == null && (musicFile == null || !musicFile.exists())) {
            exportRenderedAudio(
                format = format,
                fileName = fileName,
                shareAfter = shareAfter,
                targetPackage = targetPackage,
            )
            return
        }
        beginExportTracking(fileName)
        launchSingleFlightProcessing(
            exportInitProgress = 0.08f,
            trackExport = true,
            notifyCompletion = false,
        ) {
            val bgVol = _uiState.value.smartMixBgVolume
            val res: ExportedAudioResult = if (voice != null && music != null) {
                val mixed = withContext(Dispatchers.Default) {
                    mixWithSidechainAutoDucking(
                        voice = voice,
                        music = music,
                        musicBaseVolume = bgVol,
                    )
                }
                _uiState.update { it.copy(exportProgress = 0.45f) }
                exportRenderedAudioToFile(
                    context = getApplication(),
                    buffer = mixed,
                    format = format,
                    baseFileName = fileName.ifBlank { "Sonix_Smart_Mix" },
                ) { p ->
                    _uiState.update { it.copy(exportProgress = 0.45f + p * 0.55f) }
                }
            } else {
                val (f1, del1) = resolveAsDiskSourceFile(voice, voiceFile, "ex_v")
                val (f2, del2) = resolveAsDiskSourceFile(music, musicFile, "ex_m")
                var mixedFile: File? = null
                try {
                    if (f1 == null || f2 == null) {
                        postBanner("تعذر تجهيز ملفات التصدير")
                        return@launchSingleFlightProcessing
                    }
                    mixedFile = mixAudioFilesWithFfmpeg(
                        context = getApplication(),
                        primaryFile = f1,
                        secondaryFile = f2,
                        primaryGain = 1.0f,
                        secondaryGain = bgVol,
                        autoDucking = true,
                        mode = MergeStudioMode.OVERLAY,
                    ) ?: run {
                        postBanner("تعذر مزج الملفات قبل التصدير")
                        return@launchSingleFlightProcessing
                    }
                    _uiState.update { it.copy(exportProgress = 0.55f) }
                    exportSourceFileDirectlyWithFfmpeg(
                        context = getApplication(),
                        sourceFile = mixedFile,
                        format = format,
                        baseFileName = fileName.ifBlank { "Sonix_Smart_Mix" },
                        loudnessTarget = _uiState.value.selectedExportLoudness,
                    ) ?: run {
                        postBanner("تعذر تصدير الدمج الذكي")
                        return@launchSingleFlightProcessing
                    }
                } finally {
                    if (del1) runCatching { f1?.delete() }
                    if (del2) runCatching { f2?.delete() }
                    runCatching { mixedFile?.delete() }
                }
            }
            pendingExportResult = res
            if (shareAfter) {
                val app = getApplication<Application>()
                val uri = FileProvider.getUriForFile(
                    app,
                    "${app.packageName}.fileprovider",
                    res.file,
                )
                _sideEffects.tryEmit(
                    StudioSideEffect.LaunchShareIntent(
                        uri = uri,
                        mimeType = res.mimeType,
                        title = res.file.name,
                        targetPackage = targetPackage,
                    ),
                )
                completeExportTracking(res.file.name, if (_uiState.value.language == AppLanguage.EN) "Share" else "مشاركة")
            } else {
                val cleanName = fileName.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_").ifEmpty { "Sonix_Smart_Mix" }
                dispatchSaveToDevice(res, "$cleanName.${res.extension}")
            }
        }
    }

    fun mixWithBackgroundTrack(mainGain: Float, bgGain: Float) {
        val main = activeBuffer
        val mainFile = activeSourceFile
        val bg = mixBgBuffer
        val bgFile = mixBgSourceFile
        if ((main == null && mainFile == null) || (bg == null && bgFile == null)) {
            postBanner("يرجى التأكد من وجود الصوت الرئيسي واستيراد مسار الخلفية أولاً")
            return
        }
        launchSingleFlightProcessing {
            if (main != null && bg != null) {
                val mixed = withContext(Dispatchers.Default) { mixTracks(main, bg, mainGain, bgGain) }
                setActiveBuffer(mixed, recordUndo = true)
            } else {
                val (f1, del1) = resolveAsDiskSourceFile(main, mainFile, "mx_1")
                val (f2, del2) = resolveAsDiskSourceFile(bg, bgFile, "mx_2")
                try {
                    if (f1 == null || f2 == null) {
                        postBanner("تعذر تجهيز مسارات المزج")
                        return@launchSingleFlightProcessing
                    }
                    val outFile = mixAudioFilesWithFfmpeg(
                        context = getApplication(),
                        primaryFile = f1,
                        secondaryFile = f2,
                        primaryGain = mainGain,
                        secondaryGain = bgGain,
                        autoDucking = false,
                        mode = MergeStudioMode.OVERLAY,
                    ) ?: run {
                        postBanner("تعذر مزج المسارين عبر FFmpeg")
                        return@launchSingleFlightProcessing
                    }
                    applyProcessedDiskSourceFile(outFile, appliedToolLabel = "MixTracks")
                } finally {
                    if (del1) runCatching { f1?.delete() }
                    if (del2) runCatching { f2?.delete() }
                }
            }
            postBanner("تم مزج المسارين معاً في مسار استوديو واحد بنجاح")
        }
    }

    fun appendAudioUriToCurrent(
        uri: Uri,
        onComplete: (() -> Unit)? = null,
    ) {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            importAudioUri(uri, onComplete)
            return
        }
        viewModelScope.launch {
            val app = getApplication<Application>()
            withContext(Dispatchers.IO) {
                runCatching {
                    app.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
            }
            val name = withContext(Dispatchers.IO) {
                AudioFileUploader.resolveDisplayName(app, uri)
            }
            _uiState.update {
                it.copy(
                    isBusy = true,
                    isProcessing = true,
                    importProgress = 0f,
                    importLabel = name,
                    importBytesLabel = null,
                    importBytesProgress = null,
                )
            }
            var cachedFile: File? = null
            try {
                // Phase 1 (0% -> 50%): Copy URI into cacheDir
                val uploadOutcome = AudioFileUploader.copyAudioUriToCacheDetailed(app, uri) { info ->
                    val bytesText = if (info.totalBytes > 0L) {
                        "${AudioFileUploader.formatBytes(info.bytesCopied)} / ${AudioFileUploader.formatBytes(info.totalBytes)}"
                    } else {
                        AudioFileUploader.formatBytes(info.bytesCopied)
                    }
                    _uiState.update {
                        it.copy(
                            importProgress = (info.fraction * 0.50f).coerceIn(0f, 0.50f),
                            importLabel = info.fileName,
                            importBytesLabel = bytesText,
                            importBytesProgress = info.fraction.takeIf { info.totalBytes > 0L },
                        )
                    }
                }
                val uploaded = when (uploadOutcome) {
                    is AudioUploadOutcome.Success -> uploadOutcome.uploaded
                    is AudioUploadOutcome.Failure -> {
                        postBanner("تعذر نسخ المقطع الإضافي: ${uploadOutcome.errorMessage}")
                        return@launch
                    }
                }
                cachedFile = uploaded.file

                // Phase 2 (50% -> 100%): Stream merge on disk if either track is > 10 MB
                _uiState.update {
                    it.copy(
                        importProgress = 0.55f,
                        importLabel = "جاري تجهيز ودمج المقطع: ${uploaded.displayName}",
                        importBytesLabel = null,
                        importBytesProgress = null,
                    )
                }

                if (srcFile != null || withContext(Dispatchers.IO) { AudioDecoder.shouldStreamOnDisk(uploaded.file) }) {
                    val (f1, del1) = resolveAsDiskSourceFile(src, srcFile, "ap_1")
                    val stagedExtra = stageLargeFileToCleanAsciiCache(app, uploaded.file, prefix = "ap_2")
                    try {
                        if (f1 == null) {
                            postBanner("تعذر قراءة المقطع الأساسي للدمج")
                            return@launch
                        }
                        val mergedFile = mixAudioFilesWithFfmpeg(
                            context = app,
                            primaryFile = f1,
                            secondaryFile = stagedExtra,
                            primaryGain = 1.0f,
                            secondaryGain = 1.0f,
                            mode = MergeStudioMode.SEQUENTIAL,
                        ) ?: run {
                            postBanner("تعذر دمج الملف الصوتي الكبير عبر FFmpeg")
                            return@launch
                        }
                        applyProcessedDiskSourceFile(mergedFile, appliedToolLabel = "AppendClip")
                        _uiState.update { it.copy(importProgress = 1.0f) }
                        postBanner("تم دمج المقطع الصوتي في نهاية الصوت بنجاح")
                        onComplete?.invoke()
                    } finally {
                        if (del1) runCatching { f1?.delete() }
                        runCatching { stagedExtra.delete() }
                    }
                } else if (src != null) {
                    val decodeOutcome = AudioDecoder.decodeFileDetailedWithProgress(uploaded.file) { decFrac ->
                        _uiState.update {
                            it.copy(importProgress = (0.50f + decFrac * 0.35f).coerceIn(0.50f, 0.85f))
                        }
                    }
                    val extra = when (decodeOutcome) {
                        is AudioDecodeOutcome.Success -> decodeOutcome.buffer
                        is AudioDecodeOutcome.Failure -> {
                            postBanner("تعذر فك ترميز المقطع الإضافي للدمج: ${decodeOutcome.reason}")
                            return@launch
                        }
                    }
                    if (extra.frameCount == 0) {
                        postBanner("تعذر فك ترميز المقطع الإضافي للدمج: ملف فارغ (0 إطارات)")
                        return@launch
                    }
                    val merged = mergeClipsStreamingOnDisk(app, src, extra, crossfadeMs = 15)
                    setActiveBuffer(merged, recordUndo = true)
                    _uiState.update { it.copy(importProgress = 1.0f) }
                    postBanner("تم دمج المقطع الصوتي في نهاية الصوت بنجاح")
                    onComplete?.invoke()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                AppDiagnosticLog.logRecoverable(
                    toolName = "MergeClips",
                    titleAr = "تعذر دمج المقطع الصوتي",
                    titleEn = "Audio Merge Failed",
                    causeAr = t.localizedMessage ?: t.javaClass.simpleName,
                    causeEn = t.localizedMessage ?: t.javaClass.simpleName,
                    errorCode = t.javaClass.simpleName,
                    throwable = t,
                )
                postBanner("تعذر دمج المقطع الصوتي: ${t.javaClass.simpleName}: ${t.message}")
            } finally {
                runCatching { cachedFile?.delete() }
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        isProcessing = false,
                        importProgress = null,
                        importLabel = null,
                        importBytesLabel = null,
                        importBytesProgress = null,
                    )
                }
            }
        }
    }

    fun appendSavedProjectToCurrent(project: StudioProject) {
        launchSingleFlightProcessing {
            val extra = repository.loadProjectAudioBuffer(project.id)
            if (extra == null) {
                postBanner("هذا المشروع لا يحتوي على ملف صوتي للدمج")
                return@launchSingleFlightProcessing
            }
            val cur = activeBuffer
            val curFile = activeSourceFile
            if (cur == null && curFile == null) {
                setActiveBuffer(extra, recordUndo = false)
            } else if (cur != null) {
                val merged = mergeClipsStreamingOnDisk(getApplication(), cur, extra, crossfadeMs = 15)
                setActiveBuffer(merged, recordUndo = true)
            } else if (curFile != null) {
                val (f2, del2) = resolveAsDiskSourceFile(extra, null, "proj_ap")
                try {
                    if (f2 != null) {
                        val mergedFile = mixAudioFilesWithFfmpeg(
                            context = getApplication(),
                            primaryFile = curFile,
                            secondaryFile = f2,
                            mode = MergeStudioMode.SEQUENTIAL,
                        )
                        if (mergedFile != null) {
                            applyProcessedDiskSourceFile(mergedFile, appliedToolLabel = "AppendProject")
                        }
                    }
                } finally {
                    if (del2) runCatching { f2?.delete() }
                }
            }
            postBanner("تم دمج \"${project.name}\" في نهاية المقطع بنجاح")
        }
    }

    fun loadSavedProjectAsSecondaryTrack(project: StudioProject) {
        launchSingleFlightProcessing {
            val extra = repository.loadProjectAudioBuffer(project.id)
            if (extra == null || extra.frameCount <= 0) {
                postBanner("هذا المشروع لا يحتوي على ملف صوتي للدمج")
                return@launchSingleFlightProcessing
            }
            replaceMixBgSourceFile(null)
            mixBgBuffer = extra
            val bgPeaks = computeWaveformPeaksAsync(extra, 96)
            _uiState.update {
                it.copy(
                    mixBgName = project.name,
                    mixBgDurationMs = extra.durationMs,
                    mixBgPeaks = bgPeaks,
                )
            }
            scheduleSmartDuckingPreviewRender()
            postBanner("تم تعيين \"${project.name}\" كمسار ثانٍ في الدمج بنجاح")
        }
    }

    fun undoLastEdit() {
        val prev = undoStack.removeLastOrNull() ?: return
        lastDspSnapshotTimeMs = 0L
        appendBoundedSnapshot(redoStack, currentSnapshot())

        val bufferChanged = prev.buffer != null && prev.buffer !== activeBuffer
        if (bufferChanged) {
            activeBuffer = prev.buffer
            player.setBuffer(prev.buffer, resetPosition = true)
        }
        val restoredCfg = prev.dspConfig.normalized()
        syncPlayerConfig(restoredCfg)
        val targetBuf = prev.buffer
        val cachedPeaks = prev.waveformPeaks
        _uiState.update { cur ->
            val newPeaks = if (bufferChanged && cachedPeaks.isNotEmpty()) cachedPeaks else cur.waveformPeaks
            cur.copy(
                hasAudio = targetBuf != null || cur.hasAudio,
                durationMs = if (bufferChanged) targetBuf!!.durationMs else cur.durationMs,
                waveformPeaks = newPeaks,
                trimStart = if (bufferChanged) 0f else cur.trimStart,
                trimEnd = if (bufferChanged) 1f else cur.trimEnd,
                dspConfig = restoredCfg,
                activePresetId = prev.activePresetId,
                selectedPresetLabel = prev.selectedPresetLabel,
                canUndoEdit = undoStack.isNotEmpty(),
                canRedoEdit = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
            )
        }
        if (bufferChanged && cachedPeaks.isEmpty()) {
            val nonNullBuf = targetBuf!!
            viewModelScope.launch(Dispatchers.Default) {
                val asyncPeaks = computeWaveformPeaksAsync(nonNullBuf, 800)
                if (activeBuffer === nonNullBuf) {
                    _uiState.update { it.copy(waveformPeaks = asyncPeaks) }
                }
            }
        }
        scheduleTempPreviewRender()
        postBanner("تم التراجع عن آخر تعديل")
    }

    fun redoLastEdit() {
        val next = redoStack.removeLastOrNull() ?: return
        lastDspSnapshotTimeMs = 0L
        appendBoundedSnapshot(undoStack, currentSnapshot())

        val bufferChanged = next.buffer != null && next.buffer !== activeBuffer
        if (bufferChanged) {
            activeBuffer = next.buffer
            player.setBuffer(next.buffer, resetPosition = true)
        }
        val restoredCfg = next.dspConfig.normalized()
        syncPlayerConfig(restoredCfg)
        val targetBuf = next.buffer
        val cachedPeaks = next.waveformPeaks
        _uiState.update { cur ->
            val newPeaks = if (bufferChanged && cachedPeaks.isNotEmpty()) cachedPeaks else cur.waveformPeaks
            cur.copy(
                hasAudio = targetBuf != null || cur.hasAudio,
                durationMs = if (bufferChanged) targetBuf!!.durationMs else cur.durationMs,
                waveformPeaks = newPeaks,
                trimStart = if (bufferChanged) 0f else cur.trimStart,
                trimEnd = if (bufferChanged) 1f else cur.trimEnd,
                dspConfig = restoredCfg,
                activePresetId = next.activePresetId,
                selectedPresetLabel = next.selectedPresetLabel,
                canUndoEdit = undoStack.isNotEmpty(),
                canRedoEdit = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
            )
        }
        if (bufferChanged && cachedPeaks.isEmpty()) {
            val nonNullBuf = targetBuf!!
            viewModelScope.launch(Dispatchers.Default) {
                val asyncPeaks = computeWaveformPeaksAsync(nonNullBuf, 800)
                if (activeBuffer === nonNullBuf) {
                    _uiState.update { it.copy(waveformPeaks = asyncPeaks) }
                }
            }
        }
        scheduleTempPreviewRender()
        postBanner("تمت إعادة التعديل")
    }

    // Projects, Presets, Cloud Drafts & Export
    fun setActiveProjectName(name: String) {
        _uiState.update { it.copy(activeProjectName = name) }
    }

    fun saveCurrentProject() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                val st = _uiState.value
                val saved = repository.saveProject(
                    existingId = st.activeProjectId,
                    name = st.activeProjectName,
                    buffer = activeBuffer,
                    config = st.dspConfig,
                )
                _uiState.update {
                    it.copy(
                        activeProjectId = saved.id,
                        activeProjectName = saved.name,
                        hasUnsavedChanges = false,
                    )
                }
                postBanner("تم حفظ المشروع بنجاح")
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun saveEditsPermanent() {
        val state = _uiState.value
        val selectionChanged = state.trimStart > 0.0005f || state.trimEnd < 0.9995f
        launchSingleFlightProcessing {
            if (selectionChanged && !applyTrimSelection(autoFade = false, recordUndo = false)) {
                return@launchSingleFlightProcessing
            }
            undoStack.clear()
            redoStack.clear()
            lastDspSnapshotTimeMs = 0L
            val current = _uiState.value
            val saved = repository.saveProject(
                existingId = current.activeProjectId,
                name = current.activeProjectName,
                buffer = activeBuffer,
                config = current.dspConfig,
            )
            _uiState.update {
                it.copy(
                    activeProjectId = saved.id,
                    activeProjectName = saved.name,
                    canUndoEdit = false,
                    canRedoEdit = false,
                    hasUnsavedChanges = false,
                )
            }
            postBanner(
                if (selectionChanged) "تم حفظ التعديلات وتحديث الموجة للمقطع المحدد"
                else "تم حفظ المشروع بنجاح",
            )
        }
    }

    fun newProject() {
        player.stop()
        activeBuffer = null
        replaceActiveSourceFile(null)
        replaceMixBgSourceFile(null)
        mixBgBuffer = null
        undoStack.clear()
        redoStack.clear()
        lastDspSnapshotTimeMs = 0L
        deleteTempPreviewFile()
        val def = DspConfig()
        player.setBuffer(null, resetPosition = true)
        syncPlayerConfig(def, holdBypass = false)
        _uiState.update {
            it.copy(
                activeProjectId = 0L,
                activeCloudDraftId = null,
                activeProjectName = t(it.language, "untitled_project"),
                hasAudio = false,
                durationMs = 0L,
                waveformPeaks = emptyList(),
                dspConfig = def,
                activePresetId = null,
                selectedPresetLabel = "",
                hasUnsavedChanges = false,
                canUndoEdit = false,
                canRedoEdit = false,
                isHoldBypassing = false,
                audioMarkers = emptyList(),
                monitoringSpeed = 1.0f,
                lastAppliedToolLabel = null,
                mixBgName = null,
                mixBgDurationMs = 0L,
                mixBgPeaks = emptyList(),
            )
        }
        postBanner("تم إنشاء مشروع صوتي جديد")
    }

    fun loadProject(project: StudioProject) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                player.stop()
                val buf = repository.loadProjectAudioBuffer(project.id)
                _uiState.update {
                    it.copy(
                        activeProjectId = project.id,
                        activeCloudDraftId = null,
                        activeProjectName = project.name,
                        dspConfig = project.config,
                        activePresetId = null,
                        selectedPresetLabel = "",
                    )
                }
                syncPlayerConfig(project.config)
                if (buf != null) {
                    setActiveBuffer(buf, recordUndo = false, resetDspOnNewEntry = false)
                } else {
                    activeBuffer = null
                    replaceActiveSourceFile(null)
                    player.setBuffer(null, resetPosition = true)
                    deleteTempPreviewFile()
                    _uiState.update {
                        it.copy(
                            hasAudio = false,
                            durationMs = 0L,
                            waveformPeaks = emptyList(),
                        )
                    }
                }
                _uiState.update { it.copy(hasUnsavedChanges = false) }
                postBanner("تم فتح المشروع \"${project.name}\"")
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun deleteProject(project: StudioProject) {
        viewModelScope.launch {
            repository.deleteProject(project.id)
            if (_uiState.value.activeProjectId == project.id) {
                _uiState.update { cur ->
                    cur.copy(
                        activeProjectId = 0L,
                        activeProjectName = t(cur.language, "untitled_project"),
                        activeCloudDraftId = null,
                        hasUnsavedChanges = activeBuffer != null,
                    )
                }
            }
            postBanner("تم حذف المشروع \"${project.name}\"")
        }
    }

    fun loadRecordingIntoStudio(rec: AudioRecordingItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                val buf = repository.loadRecordingBuffer(rec.id)
                if (buf == null) {
                    postBanner("تعذر تحميل التسجيل المحفوظ")
                    return@launch
                }
                setActiveBuffer(buf, recordUndo = false, resetDspOnNewEntry = true)
                postBanner("تم فتح \"${rec.title}\" بالصوت الأصلي النظيف في الاستوديو")
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun deleteRecording(rec: AudioRecordingItem) {
        viewModelScope.launch {
            repository.deleteRecording(rec.id)
            postBanner("تم حذف التسجيل \"${rec.title}\"")
        }
    }

    fun applyPreset(preset: StudioPreset) {
        pushUndoSnapshot(debounced = false)
        val next = preset.config.copy(bypassAll = false).normalized()
        _uiState.update {
            it.copy(
                dspConfig = next,
                activePresetId = preset.id,
                selectedPresetLabel = preset.name,
                isHoldBypassing = false,
                canUndoEdit = undoStack.isNotEmpty(),
                canRedoEdit = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
            )
        }
        syncPlayerConfig(next, holdBypass = false)
        scheduleTempPreviewRender()
        if (hasLoadedAudio()) {
            ensureLivePreviewPlaying()
        }
        postBanner("تم تطبيق قالب \"${preset.name}\" للمعاينة الفورية")
    }

    fun saveCustomPreset(name: String, description: String) {
        if (name.isBlank()) return
        viewModelScope.launch(safeCoroutineHandler) {
            val uid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
            if (!uid.isNullOrBlank()) {
                if (_uiState.value.isCloudSyncing) {
                    postBanner("تجري مزامنة سحابية أخرى حالياً")
                    return@launch
                }
                val lang = _uiState.value.language
                _uiState.update {
                    it.copy(
                        isCloudSyncing = true,
                        cloudSyncLabel = if (lang == AppLanguage.EN) "Saving preset to cloud..." else "جاري حفظ القالب سحابياً...",
                    )
                }
                postBanner(if (lang == AppLanguage.EN) "Saving preset to cloud..." else "جاري حفظ القالب في السحابة...")
                try {
                    val res = runCatching {
                        firestoreRepo.saveCustomPreset(uid, name, description, _uiState.value.dspConfig)
                    }.getOrElse { Result.failure(it) }
                    if (res.isSuccess) {
                        postBanner("تم حفظ القالب \"$name\" في السحابة بنجاح")
                    } else {
                        Log.w("CloudSync", "Cloud preset save fell back to local storage", res.exceptionOrNull())
                        val localSaveSucceeded = runCatching {
                            repository.saveCustomPreset(name, description, _uiState.value.dspConfig)
                        }.isSuccess
                        postBanner(
                            t(
                                _uiState.value.language,
                                if (localSaveSucceeded) "cloud_sync_preset_local_fallback" else "cloud_sync_save_error",
                            ),
                        )
                    }
                } finally {
                    _uiState.update { it.copy(isCloudSyncing = false, cloudSyncLabel = "") }
                }
            } else {
                repository.saveCustomPreset(name, description, _uiState.value.dspConfig)
                postBanner("تم حفظ القالب \"$name\"")
            }
        }
    }

    fun deleteCustomPreset(presetId: Long) {
        viewModelScope.launch(safeCoroutineHandler) {
            val uid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
            val match = _uiState.value.customPresets.firstOrNull { it.id == presetId }
            val cloudDocId = match?.cloudDocId
            if (!uid.isNullOrBlank() && !cloudDocId.isNullOrBlank()) {
                if (_uiState.value.isCloudSyncing) {
                    postBanner("تجري مزامنة سحابية أخرى حالياً")
                    return@launch
                }
                val lang = _uiState.value.language
                _uiState.update {
                    it.copy(
                        isCloudSyncing = true,
                        cloudSyncLabel = if (lang == AppLanguage.EN) "Deleting cloud preset..." else "جاري حذف القالب السحابي...",
                    )
                }
                postBanner(if (lang == AppLanguage.EN) "Deleting cloud preset..." else "جاري حذف القالب من السحابة...")
                try {
                    val res = runCatching {
                        firestoreRepo.deleteCustomPreset(uid, cloudDocId)
                    }.getOrElse { Result.failure(it) }
                    if (res.isSuccess) {
                        postBanner("تم حذف القالب المخصص من السحابة")
                    } else {
                        Log.w("CloudSync", "Cloud preset delete warning", res.exceptionOrNull())
                        val localDeleteSucceeded = runCatching {
                            repository.deleteCustomPreset(presetId)
                        }.isSuccess
                        postBanner(
                            t(
                                _uiState.value.language,
                                if (localDeleteSucceeded) "cloud_sync_preset_delete_local" else "cloud_sync_delete_error",
                            ),
                        )
                    }
                } finally {
                    _uiState.update { it.copy(isCloudSyncing = false, cloudSyncLabel = "") }
                }
            } else {
                repository.deleteCustomPreset(presetId)
                postBanner("تم حذف القالب المخصص")
            }
        }
    }

    fun saveCloudDraft(draftName: String, onComplete: (Boolean) -> Unit = {}) {
        val cleanName = draftName.trim()
        if (cleanName.isBlank()) {
            onComplete(false)
            return
        }
        val uid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
        val lang = _uiState.value.language
        if (uid.isNullOrBlank()) {
            postBanner(t(lang, "cloud_drafts_signin_hint"))
            onComplete(false)
            return
        }
        if (_uiState.value.isCloudSyncing) {
            postBanner(if (lang == AppLanguage.EN) "A cloud sync is already in progress." else "تجري مزامنة سحابية أخرى حالياً")
            onComplete(false)
            return
        }
        val st = _uiState.value
        val presetName = st.selectedPresetLabel.ifBlank { st.activeProjectName }
        val draftId = st.activeCloudDraftId ?: UUID.randomUUID().toString()
        val draft = CloudDraft(
            docId = draftId,
            name = cleanName,
            selectedPresetName = presetName,
            durationMs = st.durationMs,
            sampleRate = activeBuffer?.sampleRate ?: activeSourceSampleRate,
            config = st.dspConfig,
            updatedAtMs = System.currentTimeMillis(),
        )
        viewModelScope.launch(safeCoroutineHandler) {
            _uiState.update {
                it.copy(
                    isBusy = true,
                    isCloudSyncing = true,
                    cloudSyncLabel = if (lang == AppLanguage.EN) "Saving project to cloud..." else "جاري حفظ المشروع سحابياً...",
                )
            }
            postBanner(if (lang == AppLanguage.EN) "Saving project to cloud..." else "جاري حفظ المشروع في السحابة...")
            try {
                repository.saveLocalCloudDraft(uid, draft)
                localCloudDraftsFlow.value =
                    (listOf(draft) + localCloudDraftsFlow.value.filterNot { it.docId == draftId }).take(100)
                publishCloudDrafts()
                _uiState.update { state ->
                    if (state.activeCloudDraftId == null) state.copy(activeCloudDraftId = draftId) else state
                }

                var res: Result<String>? = null
                for (attempt in 0 until 3) {
                    res = runCatching {
                        firestoreRepo.saveDraft(
                            userId = uid,
                            existingDraftId = st.activeCloudDraftId,
                            draftId = draftId,
                            name = cleanName,
                            selectedPresetName = presetName,
                            durationMs = st.durationMs,
                            sampleRate = draft.sampleRate,
                            config = st.dspConfig,
                        )
                    }.getOrElse { Result.failure(it) }
                    if (res.isSuccess) break
                    if (attempt < 2) delay(500L shl attempt)
                }

                if (res?.isSuccess == true) {
                    val localBackupRemoved = runCatching {
                        repository.removeLocalCloudDraft(uid, draftId)
                    }.onFailure { error ->
                        Log.w("CloudSync", "Cloud draft is confirmed, but local backup cleanup failed", error)
                    }.isSuccess
                    if (localBackupRemoved) {
                        localCloudDraftsFlow.value = localCloudDraftsFlow.value.filterNot { it.docId == draftId }
                        publishCloudDrafts()
                    }
                    remoteCloudDraftsFlow.value =
                        (listOf(draft) + remoteCloudDraftsFlow.value.filterNot { it.docId == draftId })
                    publishCloudDrafts()
                    _uiState.update {
                        it.copy(
                            activeCloudDraftId = draftId,
                            activeProjectName = cleanName,
                            hasUnsavedChanges = false,
                        )
                    }
                    postBanner("محفوظ ومؤمّن في السحابة 100% ✓")
                    notifyAudioCompletionIfBackground(
                        title = if (lang == AppLanguage.EN) "Cloud draft saved" else "اكتمل حفظ المسودة السحابية",
                        message = cleanName,
                    )
                    onComplete(true)
                } else {
                    Log.w("CloudSync", "Cloud draft save warning", res?.exceptionOrNull())
                    postBanner(
                        if (lang == AppLanguage.EN) {
                            "Draft saved locally; cloud sync will retry when available."
                        } else {
                            "حُفظت المسودة محلياً، وستُعاد مزامنتها عند توفر الاتصال."
                        },
                    )
                    onComplete(false)
                }
            } catch (error: Exception) {
                Log.e("CloudSync", "Local draft backup failed", error)
                postBanner(if (lang == AppLanguage.EN) "Could not secure the local draft." else "تعذر حفظ النسخة الاحتياطية المحلية.")
                onComplete(false)
            } finally {
                _uiState.update {
                    it.copy(isBusy = false, isCloudSyncing = false, cloudSyncLabel = "")
                }
            }
        }
    }

    fun loadCloudDraft(draft: CloudDraft) {
        pushUndoSnapshot(debounced = false)
        val restored = draft.config.copy(bypassAll = false).normalized()
        syncPlayerConfig(restored, holdBypass = false)
        _uiState.update {
            it.copy(
                activeCloudDraftId = draft.docId,
                activeProjectName = draft.name,
                dspConfig = restored,
                activePresetId = null,
                selectedPresetLabel = draft.selectedPresetName,
                isHoldBypassing = false,
                canUndoEdit = undoStack.isNotEmpty(),
                canRedoEdit = redoStack.isNotEmpty(),
                hasUnsavedChanges = false,
            )
        }
        scheduleTempPreviewRender()
        postBanner("تم تحميل المسودة \"${draft.name}\" للمعاينة")
    }

    fun deleteCloudDraft(draftId: String) {
        val uid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull() ?: return
        if (_uiState.value.isCloudSyncing) {
            postBanner("تجري مزامنة سحابية أخرى حالياً")
            return
        }
        viewModelScope.launch(safeCoroutineHandler) {
            val lang = _uiState.value.language
            _uiState.update {
                it.copy(
                    isCloudSyncing = true,
                    cloudSyncLabel = if (lang == AppLanguage.EN) "Deleting cloud project..." else "جاري حذف المشروع السحابي...",
                )
            }
            postBanner(if (lang == AppLanguage.EN) "Deleting cloud project..." else "جاري حذف المشروع من السحابة...")
            try {
                val res = runCatching {
                    firestoreRepo.deleteDraft(uid, draftId)
                }.getOrElse { Result.failure(it) }
                if (res.isSuccess) {
                    if (_uiState.value.activeCloudDraftId == draftId) {
                        _uiState.update { it.copy(activeCloudDraftId = null) }
                    }
                    val uid = Firebase.auth.currentUser?.uid
                    if (!uid.isNullOrBlank()) {
                        repository.removeLocalCloudDraft(uid, draftId)
                        localCloudDraftsFlow.value = localCloudDraftsFlow.value.filterNot { it.docId == draftId }
                        publishCloudDrafts()
                    }
                    postBanner("تم حذف المسودة السحابية")
                } else {
                    Log.w("CloudSync", "Cloud draft delete warning", res.exceptionOrNull())
                    postBanner(t(_uiState.value.language, "cloud_sync_delete_error"))
                }
            } finally {
                _uiState.update { it.copy(isCloudSyncing = false, cloudSyncLabel = "") }
            }
        }
    }

    fun setSelectedExportLoudness(target: PlatformLoudnessTarget) {
        _uiState.update { it.copy(selectedExportLoudness = target) }
    }

    fun exportRenderedAudio(
        format: ExportAudioFormat,
        fileName: String,
        shareAfter: Boolean = false,
        targetPackage: String? = null,
        loudnessTarget: PlatformLoudnessTarget = _uiState.value.selectedExportLoudness,
    ) {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner("لا يوجد صوت لتصديره حالياً")
            return
        }
        beginExportTracking(fileName)
        launchSingleFlightProcessing(exportInitProgress = 0.05f, trackExport = true, notifyCompletion = false) {
            val res: ExportedAudioResult = if (src != null) {
                val cfg = _uiState.value.dspConfig
                val rendered = withContext(Dispatchers.Default) {
                    renderOfflineDsp(src, cfg)
                }
                _uiState.update { it.copy(exportProgress = 0.45f) }
                exportRenderedAudioToFile(
                    context = getApplication(),
                    buffer = rendered,
                    format = format,
                    baseFileName = fileName,
                    loudnessTarget = loudnessTarget,
                ) { p ->
                    _uiState.update { it.copy(exportProgress = 0.45f + p * 0.55f) }
                }
            } else {
                _uiState.update { it.copy(exportProgress = 0.35f) }
                val directRes = exportSourceFileDirectlyWithFfmpeg(
                    context = getApplication(),
                    sourceFile = srcFile!!,
                    format = format,
                    baseFileName = fileName,
                    loudnessTarget = loudnessTarget,
                    dspConfig = _uiState.value.dspConfig,
                ) ?: run {
                    postBanner("تعذر تصدير الملف الكبير عبر FFmpeg")
                    cancelExportTracking()
                    return@launchSingleFlightProcessing
                }
                _uiState.update { it.copy(exportProgress = 1.0f) }
                directRes
            }
            pendingExportResult = res

            if (shareAfter) {
                val app = getApplication<Application>()
                val uri = FileProvider.getUriForFile(
                    app,
                    "${app.packageName}.fileprovider",
                    res.file,
                )
                _sideEffects.tryEmit(
                    StudioSideEffect.LaunchShareIntent(
                        uri = uri,
                        mimeType = res.mimeType,
                        title = res.file.name,
                        targetPackage = targetPackage,
                    ),
                )
                completeExportTracking(res.file.name, if (_uiState.value.language == AppLanguage.EN) "Share" else "مشاركة")
            } else {
                val cleanName = fileName.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_").ifEmpty { "Sonix_Export" }
                dispatchSaveToDevice(res, "$cleanName.${res.extension}")
            }
        }
    }

    fun exportAudioForUnity(
        assetName: String,
        shareAfter: Boolean = false,
        useSmartMixIfPresent: Boolean = false,
    ) {
        val src = activeBuffer ?: run {
            postBanner("لا يوجد صوت لتصديره لمحرك Unity حالياً")
            return
        }
        beginExportTracking(assetName.ifBlank { "SonixGameAudio" })
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, exportProgress = 0.08f) }
            try {
                val cfg = _uiState.value.dspConfig
                val bg = mixBgBuffer
                val bgVol = _uiState.value.smartMixBgVolume
                val rendered = withContext(Dispatchers.Default) {
                    if (useSmartMixIfPresent && bg != null) {
                        mixWithSidechainAutoDucking(
                            voice = src,
                            music = bg,
                            musicBaseVolume = bgVol,
                        )
                    } else {
                        renderOfflineDsp(src, cfg)
                    }
                }
                _uiState.update { it.copy(exportProgress = 0.50f) }

                val app = getApplication<Application>()
                val unityDir = File(app.cacheDir, "exports/unity").apply { mkdirs() }
                val bundle = UnityExporter.exportBufferForUnity(
                    buffer = rendered,
                    outputDir = unityDir,
                    assetName = assetName.ifBlank { "SonixGameAudio" },
                )
                if (bundle == null) {
                    postBanner("تعذر توليد حزمة الصوت لمحرك Unity")
                    cancelExportTracking()
                    return@launch
                }
                _uiState.update { it.copy(exportProgress = 1.0f) }

                val fileToSave = bundle.zipBundleFile ?: bundle.wavFile
                val ext = if (bundle.zipBundleFile != null) "zip" else "wav"
                val mime = if (bundle.zipBundleFile != null) "application/zip" else "audio/wav"
                val res = ExportedAudioResult(fileToSave, ext, mime)
                pendingExportResult = res

                if (shareAfter) {
                    val wavUri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", bundle.wavFile)
                    val csUri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", bundle.csharpScriptFile)
                    _sideEffects.tryEmit(
                        StudioSideEffect.LaunchMultiShareIntent(
                            uris = listOf(wavUri, csUri),
                            mimeType = "*/*",
                            title = "${bundle.wavFile.nameWithoutExtension} • Unity Assets",
                        ),
                    )
                    postBanner("تم تجهيز ملف الصوت (${bundle.wavFile.name}) وسكربت (${bundle.csharpScriptFile.name}) لمحرك Unity")
                    completeExportTracking(fileToSave.name, if (_uiState.value.language == AppLanguage.EN) "Shared" else "تمت المشاركة")
                } else {
                    dispatchSaveToDevice(res, fileToSave.name)
                    postBanner("تم تجهيز حزمة Unity 48kHz (${bundle.wavFile.name} + ${bundle.csharpScriptFile.name})")
                }
            } catch (error: CancellationException) {
                cancelExportTracking()
                throw error
            } catch (error: Exception) {
                Log.e("AudioExport", "Failed to export Unity audio bundle", error)
                AppDiagnosticLog.logRecoverable(
                    toolName = "UnityExport",
                    titleAr = "تعذر تصدير حزمة الصوت لمحرك Unity",
                    titleEn = "Unity audio export failed",
                    causeAr = error.localizedMessage ?: error.javaClass.simpleName,
                    causeEn = error.localizedMessage ?: error.javaClass.simpleName,
                    throwable = error,
                )
                postBanner("تعذر تصدير حزمة محرك الألعاب Unity")
                cancelExportTracking()
            } finally {
                if (pendingExportResult == null && !_uiState.value.exportComplete) {
                    cancelExportTracking()
                }
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        exportProgress = if (it.isExporting) it.exportProgress ?: 0f else null,
                    )
                }
            }
        }
    }

    private fun dispatchSaveToDevice(result: ExportedAudioResult, suggestedName: String) {
        if (!_uiState.value.isExporting) beginExportTracking(suggestedName)
        updateExportProgress(0f)
        pendingExportResult = result
        pendingSuggestedFileName = suggestedName
        val treeUriStr = defaultSaveTreeUriString
        if (treeUriStr.isNullOrBlank()) {
            _sideEffects.tryEmit(StudioSideEffect.PromptPickSaveFolderOnce(result, suggestedName))
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val ok = writeResultToTreeFolder(result, suggestedName, treeUriStr)
            if (ok) {
                val folderLabel = _uiState.value.defaultSaveFolderName ?: "Default Folder"
                val msg = if (_uiState.value.language == AppLanguage.EN) {
                    "Saved ${result.extension.uppercase()} to $folderLabel"
                } else {
                    "تم حفظ الملف (${result.extension.uppercase()}) تلقائياً في: $folderLabel"
                }
                postBanner(msg)
                completeExportTracking(suggestedName, folderLabel)
            } else {
                updateExportProgress(0f)
                _sideEffects.tryEmit(StudioSideEffect.PromptSaveDocument(result, suggestedName))
            }
        }
    }

    fun onDefaultSaveFolderPicked(treeUri: Uri?) {
        val pending = pendingExportResult
        val suggested = pendingSuggestedFileName
        if (treeUri == null) {
            if (pending != null && !suggested.isNullOrBlank()) {
                _sideEffects.tryEmit(StudioSideEffect.PromptSaveDocument(pending, suggested))
            }
            return
        }

        val app = getApplication<Application>()
        runCatching {
            app.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }

        val treeDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull().orEmpty()
        val folderName = treeDocId
            .substringAfter(':', treeDocId)
            .substringAfterLast('/')
            .ifBlank { treeUri.lastPathSegment?.substringAfterLast('/') ?: "Sonix Exports" }

        defaultSaveTreeUriString = treeUri.toString()
        viewModelScope.launch {
            repository.saveDefaultSaveFolder(treeUri.toString(), folderName)
            if (pending != null && !suggested.isNullOrBlank()) {
                val ok = withContext(Dispatchers.IO) {
                    writeResultToTreeFolder(pending, suggested, treeUri.toString())
                }
                if (ok) {
                    val msg = if (_uiState.value.language == AppLanguage.EN) {
                        "Default save folder set ($folderName) & file saved"
                    } else {
                        "تم تعيين المجلد ($folderName) وحفظ الملف بنجاح"
                    }
                    postBanner(msg)
                    completeExportTracking(suggested ?: pending.file.name, folderName)
                } else {
                    updateExportProgress(0f)
                    _sideEffects.tryEmit(StudioSideEffect.PromptSaveDocument(pending, suggested))
                }
            } else {
                val msg = if (_uiState.value.language == AppLanguage.EN) {
                    "Default save folder updated: $folderName"
                } else {
                    "تم تحديث مجلد الحفظ الافتراضي: $folderName"
                }
                postBanner(msg)
            }
        }
    }

    private fun writeResultToTreeFolder(
        result: ExportedAudioResult,
        suggestedName: String,
        treeUriStr: String,
    ): Boolean = runCatching {
        val app = getApplication<Application>()
        val cr = app.contentResolver
        val treeUri = Uri.parse(treeUriStr)
        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
        val createdUri = DocumentsContract.createDocument(
            cr,
            parentDocUri,
            result.mimeType,
            suggestedName,
        ) ?: return@runCatching false

        try {
            val out = cr.openOutputStream(createdUri)
            if (out == null) {
                DocumentsContract.deleteDocument(cr, createdUri)
                return@runCatching false
            }
            out.use { copyExportBytes(result, it) }
        } catch (error: Exception) {
            runCatching { DocumentsContract.deleteDocument(cr, createdUri) }
            throw error
        }
        true
    }.onFailure { Log.w("AudioExport", "Failed to write exported audio to selected folder", it) }
        .getOrDefault(false)

    fun completeSaveExportedDocument(destinationUri: Uri?) {
        val result = pendingExportResult ?: return
        val suggestedName = pendingSuggestedFileName ?: result.file.name
        if (destinationUri == null) {
            pendingExportResult = null
            pendingSuggestedFileName = null
            cancelExportTracking()
            return
        }
        updateExportProgress(0f)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val out = getApplication<Application>().contentResolver.openOutputStream(destinationUri)
                    ?: throw IllegalStateException("Could not open the selected destination")
                out.use { copyExportBytes(result, it) }
                val displayName = queryDisplayName(destinationUri) ?: suggestedName
                val location = querySaveLocation(destinationUri) ?: _uiState.value.defaultSaveFolderName ?: if (_uiState.value.language == AppLanguage.EN) {
                    "Selected location"
                } else {
                    "الموقع الذي اخترته"
                }
                postBanner("تم حفظ الملف بصيغة ${result.extension.uppercase()} بنجاح")
                completeExportTracking(displayName, location)
            }.onFailure { error ->
                Log.e("AudioExport", "Failed saving exported audio document", error)
                pendingExportResult = null
                pendingSuggestedFileName = null
                cancelExportTracking()
                postBanner("تعذر حفظ الملف في الموقع المحدد")
            }
        }
    }

    private fun copyExportBytes(result: ExportedAudioResult, output: java.io.OutputStream) {
        val totalBytes = result.file.length().coerceAtLeast(1L)
        val buffer = ByteArray(32 * 1024)
        var copied = 0L
        updateExportProgress(0f)
        result.file.inputStream().buffered().use { input ->
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                output.write(buffer, 0, count)
                copied += count
                updateExportProgress((copied.toFloat() / totalBytes).coerceIn(0f, 1f))
            }
        }
        output.flush()
        updateExportProgress(1f)
    }

    private fun queryDisplayName(uri: Uri): String? =
        getApplication<Application>().contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0)?.takeIf { it.isNotBlank() } else null
        }

    private fun querySaveLocation(uri: Uri): String? = runCatching {
        val documentId = DocumentsContract.getDocumentId(uri)
        documentId.substringAfter(':', documentId).substringBeforeLast('/').takeIf { it.isNotBlank() }
    }.getOrNull()

    // AI Studio — Hybrid: uses Gemini API when BuildConfig.GEMINI_API_KEY is present, otherwise Sherpa-ONNX / Local Engines seamlessly
    fun transcribeActiveAudio() {
        val lang = _uiState.value.language
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            _uiState.update {
                it.copy(
                    transcriptionState = AiStreamState(
                        isLoading = false,
                        errorMessage = "لا يوجد مقطع صوتي حالياً لتفريغه. سجّل أو استورد صوتاً أولاً.",
                    ),
                )
            }
            return
        }
        activeOnlineJob?.cancel()
        activeOnlineJob = viewModelScope.launch(safeCoroutineHandler) {
            _uiState.update {
                it.copy(
                    isOnlineTaskRunning = true,
                    onlineTaskLabel = "AI Transcription",
                    transcriptionProgressLabel = null,
                    transcriptionState = AiStreamState(isLoading = true),
                )
            }
            try {
                val res = aiRepository.transcribeAudioSource(
                    buffer = src,
                    sourceFile = srcFile,
                    language = lang,
                ) { progressMsg ->
                    _uiState.update { it.copy(transcriptionProgressLabel = progressMsg) }
                }
                when (res) {
                    is AiResponse.Success -> _uiState.update {
                        it.copy(
                            transcriptionState = AiStreamState(
                                isLoading = false,
                                outputText = res.text,
                            ),
                        )
                    }
                    is AiResponse.NeedsCloudProvisioning -> _uiState.update {
                        it.copy(
                            transcriptionState = AiStreamState(
                                isLoading = false,
                                errorMessage = res.message,
                            ),
                        )
                    }
                }
            } finally {
                _uiState.update {
                    it.copy(
                        isOnlineTaskRunning = false,
                        onlineTaskLabel = "",
                        transcriptionProgressLabel = null,
                        transcriptionState = it.transcriptionState.copy(isLoading = false),
                    )
                }
            }
        }
    }

    fun clearTranscription() {
        _uiState.update { it.copy(transcriptionState = AiStreamState(), transcriptionProgressLabel = null) }
    }

    fun runSmartAutoMaster() {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner("قم بتسجيل أو استيراد مقطع صوتي أولاً لتحليله")
            return
        }
        launchSingleFlightProcessing {
            val sampleBuf = src ?: withContext(Dispatchers.IO) {
                AudioDecoder.decodeSampleWindowForAnalysis(srcFile!!, maxSeconds = 18f)
            }
            if (sampleBuf == null || sampleBuf.frameCount == 0) {
                postBanner("تعذر تحليل عينة من الملف الصوتي")
                return@launchSingleFlightProcessing
            }
            pushUndoSnapshot(debounced = false)
            val acousticAnalysis = withContext(Dispatchers.Default) {
                analyzeAndOptimizeAudio(sampleBuf, _uiState.value.dspConfig)
            }
            val lang = _uiState.value.language
            _uiState.update {
                it.copy(
                    smartMasterState = acousticAnalysis,
                    smartMasterAnalyzingAi = OnlineGuard.isOnline(getApplication()),
                    dspConfig = acousticAnalysis.optimizedConfig,
                    activePresetId = null,
                    selectedPresetLabel = "Smart Auto-Master",
                    canUndoEdit = undoStack.isNotEmpty(),
                    canRedoEdit = redoStack.isNotEmpty(),
                    hasUnsavedChanges = true,
                )
            }
            syncPlayerConfig(acousticAnalysis.optimizedConfig)
            scheduleTempPreviewRender()
            postBanner("تم تحليل الصوت وتطبيق الماسترينغ الذكي تلقائياً")

            // Enrich with Gemini 2.5 Flash Multimodal Audio Engineer diagnosis & 1-tap FFmpegKit filter chain
            if (OnlineGuard.isOnline(getApplication())) {
                try {
                    val aiRec = SonixUnifiedAIEngine.analyzeAudioForSmartEngineer(
                        context = getApplication(),
                        sourceBuffer = src,
                        sourceFile = srcFile,
                        acousticMetrics = acousticAnalysis,
                        language = lang,
                    )
                    if (aiRec != null) {
                        val combinedActions = (aiRec.recommendations + acousticAnalysis.appliedActions).distinct()
                        _uiState.update { cur ->
                            cur.copy(
                                smartMasterAnalyzingAi = false,
                                smartMasterState = (cur.smartMasterState ?: acousticAnalysis).copy(
                                    detectedProfile = aiRec.detectedProfile,
                                    appliedActions = combinedActions,
                                    aiDiagnosticNotes = aiRec.diagnosticNotes,
                                    recommendedFfmpegFilter = aiRec.recommendedFfmpegFilter,
                                    isAiCloudAnalyzed = true,
                                    ffmpegFilterApplied = false,
                                ),
                            )
                        }
                    } else {
                        _uiState.update { it.copy(smartMasterAnalyzingAi = false) }
                    }
                } catch (_: Throwable) {
                    _uiState.update { it.copy(smartMasterAnalyzingAi = false) }
                }
            }
        }
    }

    fun applyAiEngineerFfmpegFilter() {
        val analysis = _uiState.value.smartMasterState ?: return
        val filterGraph = analysis.recommendedFfmpegFilter?.takeIf { it.isNotBlank() } ?: return
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner("قم بتحميل أو تسجيل مقطع صوتي أولاً")
            return
        }
        launchSingleFlightProcessing(purifyInitProgress = 0.2f) {
            val (newBuf, newFile) = SonixUnifiedAIEngine.applyRecommendedFfmpegFilter(
                context = getApplication(),
                sourceBuffer = src,
                sourceFile = srcFile,
                filterGraph = filterGraph,
            )
            when {
                newBuf != null && newBuf.frameCount > 0 -> {
                    setActiveBuffer(newBuf, recordUndo = true)
                    _uiState.update { cur ->
                        cur.copy(
                            purifyProgress = 1.0f,
                            smartMasterState = cur.smartMasterState?.copy(ffmpegFilterApplied = true),
                        )
                    }
                    postBanner("تم تطبيق سلسلة فلاتر مهندس الصوت الذكي عبر FFmpegKit بنجاح")
                }
                newFile != null && newFile.exists() -> {
                    applyProcessedDiskSourceFile(newFile, appliedToolLabel = "SmartAiFfmpegMaster")
                    _uiState.update { cur ->
                        cur.copy(
                            purifyProgress = 1.0f,
                            smartMasterState = cur.smartMasterState?.copy(ffmpegFilterApplied = true),
                        )
                    }
                    postBanner("تم تطبيق سلسلة فلاتر مهندس الصوت الذكي على الملف الكبير عبر FFmpegKit بنجاح")
                }
                else -> {
                    postBanner("تعذر تنفيذ سلسلة FFmpegKit المقترحة، إعدادات الماسترينغ الصوتية مفعلة بالفعل")
                }
            }
        }
    }

    fun clearSmartMasterAnalysis() {
        _uiState.update { it.copy(smartMasterState = null, smartMasterAnalyzingAi = false) }
    }

    fun askSearchAssistant(query: String) {
        if (query.isBlank()) return
        val lang = _uiState.value.language
        activeOnlineJob?.cancel()
        activeOnlineJob = viewModelScope.launch(safeCoroutineHandler) {
            _uiState.update {
                it.copy(
                    isOnlineTaskRunning = true,
                    onlineTaskLabel = "AI Knowledge Search",
                    searchAssistantState = AiStreamState(isLoading = true),
                )
            }
            try {
                when (val res = aiRepository.askSearchAssistant(query.trim(), lang)) {
                    is AiResponse.Success -> _uiState.update {
                        it.copy(
                            searchAssistantState = AiStreamState(
                                isLoading = false,
                                outputText = res.text,
                                searchQueries = res.searchQueries,
                                sources = res.sources,
                            ),
                        )
                    }
                    is AiResponse.NeedsCloudProvisioning -> _uiState.update {
                        it.copy(
                            searchAssistantState = AiStreamState(
                                isLoading = false,
                                errorMessage = res.message,
                            ),
                        )
                    }
                }
            } finally {
                _uiState.update {
                    it.copy(
                        isOnlineTaskRunning = false,
                        onlineTaskLabel = "",
                        searchAssistantState = it.searchAssistantState.copy(isLoading = false),
                    )
                }
            }
        }
    }

    // In-App Audio Library Scanner & Actions
    fun refreshDeviceAudios() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningDeviceAudio = true) }
            try {
                val items = MediaStoreAudioRepository.queryDeviceAudioFiles(getApplication())
                _uiState.update { it.copy(deviceAudios = items) }
            } finally {
                _uiState.update { it.copy(isScanningDeviceAudio = false) }
            }
        }
    }

    fun importDeviceAudioItem(
        item: DeviceAudioItem,
        onComplete: (() -> Unit)? = null,
    ) {
        importAudioUri(Uri.parse(item.uriString), onComplete)
    }

    fun appendDeviceAudioItem(
        item: DeviceAudioItem,
        onComplete: (() -> Unit)? = null,
    ) {
        appendAudioUriToCurrent(Uri.parse(item.uriString), onComplete)
    }

    fun loadMixBackgroundDeviceItem(
        item: DeviceAudioItem,
        onComplete: (() -> Unit)? = null,
    ) {
        loadMixBackgroundUri(Uri.parse(item.uriString), onComplete)
    }

    // Global Pro Presets & 18-Tool Operations (100% Non-Destructive Live Preview + Optional Bake)
    fun applyGlobalProPreset(preset: GlobalProPreset, bakeOffline: Boolean = false) {
        val title = t(_uiState.value.language, preset.titleKey)
        if (bakeOffline) {
            val src = activeBuffer
            val srcFile = activeSourceFile
            if (src != null) {
                launchSingleFlightProcessing {
                    val rendered = renderGlobalProPreset(src, preset)
                    setActiveBuffer(rendered, recordUndo = true)
                    _uiState.update { it.copy(selectedPresetLabel = title, lastAppliedToolLabel = title) }
                    postBanner("تم دمج وتطبيق القالب العالمي \"$title\" على الصوت")
                }
                return
            } else if (srcFile != null && srcFile.exists()) {
                launchSingleFlightProcessing {
                    val outFile = processAudioFileWithFfmpeg(
                        context = getApplication(),
                        inputFile = srcFile,
                        audioFilter = preset.ffmpegFilter,
                        toolName = "GlobalProPreset_${preset.id.name}",
                    ) ?: run {
                        postBanner("تعذر تطبيق القالب على الملف الكبير")
                        return@launchSingleFlightProcessing
                    }
                    applyProcessedDiskSourceFile(outFile, appliedToolLabel = title)
                    _uiState.update { it.copy(selectedPresetLabel = title) }
                    postBanner("تم تطبيق القالب العالمي \"$title\" عبر FFmpeg")
                }
                return
            }
        }
        pushUndoSnapshot(debounced = false)
        val nextCfg = preset.dspConfig.copy(bypassAll = false).normalized()
        _uiState.update {
            it.copy(
                dspConfig = nextCfg,
                activePresetId = null,
                selectedPresetLabel = title,
                isHoldBypassing = false,
                canUndoEdit = undoStack.isNotEmpty(),
                canRedoEdit = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
            )
        }
        syncPlayerConfig(nextCfg, holdBypass = false)
        scheduleTempPreviewRender()
        if (hasLoadedAudio()) {
            ensureLivePreviewPlaying()
        }
        postBanner("تم تفعيل القالب العالمي \"$title\" للمعاينة الحية بدون المساس بالأصل")
    }

    fun applyReverseAudio() {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner("يرجى تسجيل أو اختيار ملف صوتي أولاً")
            return
        }
        launchSingleFlightProcessing {
            if (src != null) {
                val reversed = reverseAudioWithFfmpeg(getApplication(), src)
                setActiveBuffer(reversed, recordUndo = true)
            } else if (srcFile != null) {
                val outFile = processAudioFileWithFfmpeg(
                    context = getApplication(),
                    inputFile = srcFile,
                    audioFilter = "areverse,afade=t=in:st=0:d=0.003",
                    toolName = "ReverseLarge",
                ) ?: run {
                    postBanner("تعذر عكس الملف الصوتي الكبير")
                    return@launchSingleFlightProcessing
                }
                applyProcessedDiskSourceFile(outFile, appliedToolLabel = "Reverse")
            }
            _uiState.update { it.copy(lastAppliedToolLabel = "Reverse") }
            postBanner("تم عكس مسار الصوت بنجاح (FFmpeg Reverse)")
        }
    }

    fun previewReverseAudio() {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner(t(_uiState.value.language, "no_audio_loaded"))
            return
        }
        if (src != null) {
            toggleToolBufferPreview("ReversePreview") {
                reverseAudioWithFfmpeg(getApplication(), src)
            }
            return
        }
        if (_uiState.value.isToolPreviewPlaying && hasGlobalToolPreview()) {
            stopToolPreview()
            return
        }
        launchSingleFlightProcessing {
            stopToolPreview()
            player.pause()
            stopMixPreviewPlayer()
            val previewFile = processAudioFileWithFfmpeg(
                context = getApplication(),
                inputFile = srcFile!!,
                audioFilter = "areverse,afade=t=in:st=0:d=0.003",
                toolName = "ReversePreviewLarge",
            )
            if (previewFile == null || !previewFile.exists()) {
                postBanner("تعذر توليد معاينة العكس للملف الكبير")
                return@launchSingleFlightProcessing
            }
            val started = startGlobalFilePreview(previewFile, deleteFileOnStop = true)
            if (!started) {
                postBanner("تعذر تشغيل المعاينة")
            }
        }
    }

    fun insertGeneratedSilence(
        durationSeconds: Float,
        mode: SilenceInsertMode = SilenceInsertMode.APPEND_END,
        sampleRate: Int = 44100,
        channels: Int = 2,
    ) {
        val cur = activeBuffer
        val curDisk = activeSourceFile
        val hasExisting = (cur != null && cur.frameCount > 0) || (curDisk != null && curDisk.exists() && curDisk.length() > 0L)
        val effectiveMode = if (hasExisting) mode else SilenceInsertMode.NEW_TRACK
        val isModifiedTrack = effectiveMode != SilenceInsertMode.NEW_TRACK
        launchSingleFlightProcessing {
            val result = insertSilenceStreaming(
                context = getApplication(),
                currentBuffer = cur,
                currentDiskFile = curDisk,
                durationSeconds = durationSeconds,
                mode = effectiveMode,
                sampleRate = sampleRate,
                channels = channels,
            )
            val durSecFmt = if (durationSeconds >= 60f) {
                val mins = durationSeconds.toInt() / 60
                val secs = durationSeconds.toInt() % 60
                "${mins}m ${secs}s"
            } else {
                "${"%.1f".format(durationSeconds)}s"
            }
            val chLabel = if (result.channels == 1) "Mono" else "Stereo"
            val srLabel = if (result.sampleRate % 1000 == 0) "${result.sampleRate / 1000}kHz" else "${"%.1f".format(result.sampleRate / 1000f)}kHz"
            val label = "توليد صمت ($durSecFmt)"
            if (result.buffer != null) {
                setActiveBuffer(
                    buf = result.buffer,
                    recordUndo = isModifiedTrack,
                    resetDspOnNewEntry = !isModifiedTrack,
                )
                _uiState.update { it.copy(lastAppliedToolLabel = label) }
            } else if (result.diskFile != null) {
                applyProcessedDiskSourceFile(
                    newFile = result.diskFile,
                    recordUndo = isModifiedTrack,
                    appliedToolLabel = label,
                )
            }
            val modeText = when (effectiveMode) {
                SilenceInsertMode.NEW_TRACK -> "مسار صمت جديد"
                SilenceInsertMode.INSERT_START -> "إدراج الصمت في البداية"
                SilenceInsertMode.APPEND_END -> "إلحاق الصمت بالنهاية"
            }
            postBanner("تم $modeText ($durSecFmt • $srLabel • $chLabel)")
        }
    }

    fun updateSurroundLiveParams(midAdjustPct: Float, sideBoostPct: Float, widthPct: Float) {
        updateDspConfig { prev ->
            prev.copy(
                bypassAll = false,
                surroundMidPct = midAdjustPct,
                surroundSidePct = sideBoostPct,
                surroundWidthPct = widthPct,
            )
        }
        if (abs(midAdjustPct) > 0.5f || abs(sideBoostPct) > 0.5f || widthPct > 0.5f) {
            ensureLivePreviewPlaying()
        }
    }

    fun estimateSurroundPhaseReport(
        midAdjustPct: Float,
        sideBoostPct: Float,
        widthPct: Float,
    ): StereoCompatibilityReport = estimateStereoCompatibilityForOffsets(
        buffer = activeBuffer,
        midAdjustPct = midAdjustPct,
        sideBoostPct = sideBoostPct,
        widthPct = widthPct,
    )

    fun applySurroundStereoFromOffsets(midAdjustPct: Float, sideBoostPct: Float, widthPct: Float) {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner("يرجى اختيار ملف صوتي أولاً للتحويل إلى ستيريو")
            return
        }
        launchSingleFlightProcessing {
            val snap = activeLiveToolSession?.takeIf { it.toolKey == "surround_stereo" }
            if (snap != null) {
                while (undoStack.size > snap.undoSize) {
                    undoStack.removeLast()
                }
                snap.committed = true
            }
            sliderDebounceJob?.cancel()
            val bakedCfg = _uiState.value.dspConfig.copy(
                surroundMidPct = 0f,
                surroundSidePct = 0f,
                surroundWidthPct = 0f,
            )
            syncPlayerConfig(bakedCfg)
            _uiState.update { it.copy(dspConfig = bakedCfg, lastAppliedToolLabel = "SurroundStereo") }
            var report: StereoCompatibilityReport? = null
            if (src != null) {
                val widened = withContext(Dispatchers.Default) {
                    convertToSurroundStereoFromOffsets(src, midAdjustPct, sideBoostPct, widthPct)
                }
                report = analyzeStereoCompatibility(widened)
                setActiveBuffer(widened, recordUndo = true)
            } else if (srcFile != null) {
                val (primaryFilter, fallbackFilter) = buildSurroundStereoFfmpegFilter(
                    midAdjustPct = midAdjustPct,
                    sideBoostPct = sideBoostPct,
                    widthPct = widthPct,
                )
                val outFile = processAudioFileWithFfmpeg(
                    context = getApplication(),
                    inputFile = srcFile,
                    audioFilter = primaryFilter,
                    toolName = "SurroundStereoWide",
                ) ?: processAudioFileWithFfmpeg(
                    context = getApplication(),
                    inputFile = srcFile,
                    audioFilter = fallbackFilter,
                    toolName = "SurroundStereoFallback",
                ) ?: run {
                    postBanner("تعذر تحويل الملف الكبير إلى ستيريو محيطي")
                    return@launchSingleFlightProcessing
                }
                applyProcessedDiskSourceFile(outFile, appliedToolLabel = "SurroundStereo")
            }
            val phaseSuffix = report?.let { r ->
                val badge = if (r.isMonoCompatible) "متوافق مع المونو" else "تحذير تعارض طور"
                " (${badge}: r=${"%+.2f".format(Locale.US, r.correlation)})"
            }.orEmpty()
            postBanner("تم تحويل المقطع إلى ستيريو محيطي متسع (M/S Surround)$phaseSuffix")
        }
    }

    fun applyPitchAndSpeed(
        semitones: Float,
        tempoRatio: Float,
        preserveFormants: Boolean = _uiState.value.dspConfig.pitchShift.preserveFormants,
    ) {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner("يرجى اختيار ملف صوتي أولاً لتعديل الطبقة والسرعة")
            return
        }
        launchSingleFlightProcessing(purifyInitProgress = 0.10f) {
            val snap = activeLiveToolSession?.takeIf { it.toolKey == "pitch_speed" }
            if (snap != null) {
                while (undoStack.size > snap.undoSize) {
                    undoStack.removeLast()
                }
                snap.committed = true
            }
            sliderDebounceJob?.cancel()
            val bakedCfg = _uiState.value.dspConfig
                .copy(
                    pitchShift = _uiState.value.dspConfig.pitchShift.copy(semitones = 0f, preserveFormants = preserveFormants),
                    timeStretch = _uiState.value.dspConfig.timeStretch.copy(tempoRatio = 1f),
                )
                .withSlotEnabled(EffectType.PITCH_SHIFT, false)
                .withSlotEnabled(EffectType.TIME_STRETCH, false)
            syncPlayerConfig(bakedCfg)
            _uiState.update { it.copy(dspConfig = bakedCfg, lastAppliedToolLabel = "PitchAndSpeed") }

            if (src != null) {
                val processed = applyPitchAndSpeedOffline(
                    buffer = src,
                    semitones = semitones,
                    tempoRatio = tempoRatio,
                    preserveFormants = preserveFormants,
                    context = getApplication(),
                ) { p ->
                    _uiState.update { it.copy(purifyProgress = (0.10f + p * 0.85f).coerceIn(0.10f, 0.98f)) }
                }
                setActiveBuffer(processed, recordUndo = true)
            } else if (srcFile != null) {
                val outFile = applyPitchAndSpeedFileWithFfmpeg(
                    context = getApplication(),
                    inputFile = srcFile,
                    semitones = semitones,
                    tempoRatio = tempoRatio,
                    preserveFormants = preserveFormants,
                ) { p ->
                    _uiState.update { it.copy(purifyProgress = (0.10f + p * 0.85f).coerceIn(0.10f, 0.98f)) }
                } ?: run {
                    postBanner("تعذر تعديل الطبقة والسرعة للملف الكبير")
                    return@launchSingleFlightProcessing
                }
                applyProcessedDiskSourceFile(outFile, appliedToolLabel = "PitchAndSpeed")
            }
            _uiState.update { it.copy(purifyProgress = 1.0f) }
            postBanner("تم تعديل الطبقة والسرعة وحفظ النسخة في التراجع")
        }
    }

    fun previewEditorSelectionOrFade(
        fadeInSec: Float,
        fadeOutSec: Float,
        autoFadeEdges: Boolean,
        previewSelectionOnly: Boolean = true,
    ) {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner(t(_uiState.value.language, "no_audio_loaded"))
            return
        }
        val st = _uiState.value
        if (src != null) {
            toggleToolBufferPreview("EditorPreview") {
                withContext(Dispatchers.Default) {
                    var buf = if (previewSelectionOnly) {
                        trimToSelectionWithFade(src, st.trimStart, st.trimEnd, autoFadeSec = if (autoFadeEdges) 0.2f else 0f)
                    } else {
                        src
                    }
                    if (abs(st.clipGainDb) >= 0.05f && clipGainUnappliedDirty) {
                        buf = applyGainDb(buf, st.clipGainDb)
                    }
                    if (fadeInSec > 0.05f) buf = applyFadeIn(buf, fadeInSec)
                    if (fadeOutSec > 0.05f) buf = applyFadeOut(buf, fadeOutSec)
                    renderOfflineDsp(buf, st.dspConfig)
                }
            }
            return
        }
        if (_uiState.value.isToolPreviewPlaying && hasGlobalToolPreview()) {
            stopToolPreview()
            return
        }
        launchSingleFlightProcessing {
            stopToolPreview()
            player.pause()
            stopMixPreviewPlayer()
            val durSec = (st.durationMs / 1000.0).coerceAtLeast(0.2)
            val sSec = if (previewSelectionOnly) (durSec * st.trimStart).coerceIn(0.0, durSec - 0.05) else 0.0
            val eSec = if (previewSelectionOnly) (durSec * st.trimEnd).coerceIn(sSec + 0.05, durSec) else durSec
            val segLen = (eSec - sSec).coerceAtLeast(0.05)
            val filterList = buildList {
                if (abs(st.clipGainDb) >= 0.05f && clipGainUnappliedDirty) {
                    add("volume=${String.format(Locale.US, "%.2f", st.clipGainDb)}dB")
                }
                val effIn = if (fadeInSec > 0.05f) fadeInSec.toDouble() else if (autoFadeEdges && segLen > 0.45) 0.2 else 0.0
                val effOut = if (fadeOutSec > 0.05f) fadeOutSec.toDouble() else if (autoFadeEdges && segLen > 0.45) 0.2 else 0.0
                if (effIn > 0.02 && segLen > effIn) {
                    add("afade=t=in:st=0:d=${String.format(Locale.US, "%.2f", effIn)}")
                }
                if (effOut > 0.02 && segLen > effOut + 0.05) {
                    val outStart = (segLen - effOut).coerceAtLeast(0.0)
                    add("afade=t=out:st=${String.format(Locale.US, "%.3f", outStart)}:d=${String.format(Locale.US, "%.2f", effOut)}")
                }
                add("alimiter=limit=0.96")
            }.joinToString(",")
            val ssStr = String.format(Locale.US, "%.3f", sSec)
            val toStr = String.format(Locale.US, "%.3f", eSec)
            val previewFile = processAudioFileWithFfmpeg(
                context = getApplication(),
                inputFile = srcFile!!,
                preInputArgs = "-ss $ssStr -to $toStr",
                audioFilter = filterList,
                toolName = "EditorPreviewLarge",
            )
            if (previewFile == null || !previewFile.exists()) {
                postBanner("تعذر توليد معاينة صوتية للملف الكبير")
                return@launchSingleFlightProcessing
            }
            val started = startGlobalFilePreview(previewFile, deleteFileOnStop = true)
            if (!started) {
                postBanner("تعذر تشغيل المعاينة")
            }
        }
    }

    fun previewUnifiedMergeOverlay(params: MergeStudioParams) {
        val primary = activeBuffer
        val primaryFile = activeSourceFile
        if (primary == null && (primaryFile == null || !primaryFile.exists())) {
            postBanner(t(_uiState.value.language, "no_audio_loaded"))
            return
        }
        val secondary = mixBgBuffer
        val secondaryFile = mixBgSourceFile
        if (secondary == null && (secondaryFile == null || !secondaryFile.exists())) {
            postBanner("يرجى اختيار المقطع الثاني أولاً للمعاينة")
            return
        }

        // Real-time dual-track playback synced directly with PersistentTransportBar & Bypass when both buffers fit in memory
        if (primary != null && secondary != null) {
            stopToolPreview()
            stopMixPreviewPlayer()
            syncMergeStudioPreview(params)
            if (_uiState.value.playback.isPlaying || player.isCurrentlyPlaying()) {
                player.pause()
            } else {
                player.play()
            }
            return
        }

        // Large disk file (>10MB) FFmpegKit preview stream
        if (_uiState.value.isToolPreviewPlaying && hasGlobalToolPreview()) {
            stopToolPreview()
            return
        }
        launchSingleFlightProcessing(exportInitProgress = 0.15f) {
            stopToolPreview()
            player.pause()
            val (f1, del1) = resolveAsDiskSourceFile(primary, primaryFile, "ump_1")
            val (f2, del2) = resolveAsDiskSourceFile(secondary, secondaryFile, "ump_2")
            try {
                if (f1 == null || f2 == null) {
                    postBanner("تعذر تجهيز ملفات المعاينة")
                    return@launchSingleFlightProcessing
                }
                val gain1 = (1f + params.primaryLevelPct / 100f).coerceIn(0.1f, 2.0f)
                val gain2 = (1f + params.secondaryLevelPct / 100f).coerceIn(0.1f, 2.0f)
                val effSecGain = if (params.mode == MergeStudioMode.OVERLAY && params.autoDuckingEnabled) {
                    (0.45f * gain2).coerceIn(0.05f, 1.35f)
                } else {
                    gain2
                }
                val outPreview = File(getApplication<Application>().cacheDir, "merge_preview_large.m4a")
                _uiState.update { it.copy(exportProgress = 0.45f) }
                val rendered = mixAudioFilesWithFfmpeg(
                    context = getApplication(),
                    primaryFile = f1,
                    secondaryFile = f2,
                    primaryGain = gain1,
                    secondaryGain = effSecGain,
                    autoDucking = params.autoDuckingEnabled,
                    mode = params.mode,
                    crossfadeSec = params.crossfadeSec,
                    offsetSec = params.overlayStartOffsetSec,
                    outFile = outPreview,
                    primaryStartOffsetSec = params.primaryStartOffsetSec,
                    overlayFadeInSec = params.overlayFadeInSec,
                    overlayFadeOutSec = params.overlayFadeOutSec,
                    secondaryDurationSec = _uiState.value.mixBgDurationMs / 1000f,
                    transitionGapSec = params.transitionGapSec,
                    autoLeveling = params.autoLevelingEnabled,
                    duckingStrengthPct = params.duckingStrengthPct,
                    echoTransitionEnabled = params.echoTransitionEnabled,
                    echoTransitionPct = params.echoTransitionPct,
                    gapWhooshEnabled = params.gapWhooshEnabled,
                    whooshIntensityPct = params.whooshIntensityPct,
                    levelingStrengthPct = params.levelingStrengthPct,
                    primaryDurationSec = _uiState.value.durationMs / 1000f,
                )
                if (rendered == null || !rendered.exists()) {
                    postBanner("تعذر توليد معاينة الدمج للملفات الكبيرة")
                    return@launchSingleFlightProcessing
                }
                val started = startGlobalFilePreview(rendered, deleteFileOnStop = true)
                if (started) {
                    _uiState.update { it.copy(exportProgress = 1.0f) }
                } else {
                    postBanner("تعذر تشغيل معاينة الدمج")
                }
            } finally {
                if (del1) runCatching { f1?.delete() }
                if (del2) runCatching { f2?.delete() }
            }
        }
    }

    fun applyUnifiedMergeOverlay(params: MergeStudioParams) {
        val primary = activeBuffer
        val primaryFile = activeSourceFile
        if (primary == null && (primaryFile == null || !primaryFile.exists())) {
            postBanner(t(_uiState.value.language, "no_audio_loaded"))
            return
        }
        val secondary = mixBgBuffer
        val secondaryFile = mixBgSourceFile
        if (secondary == null && (secondaryFile == null || !secondaryFile.exists())) {
            postBanner("يرجى اختيار المقطع الثاني أولاً للتطبيق")
            return
        }
        stopToolPreview()
        clearMergeStudioPreview()
        launchSingleFlightProcessing(exportInitProgress = 0.25f) {
            if (primary != null && secondary != null) {
                val res = executeUnifiedMergeOverlay(getApplication(), primary, secondary, params)
                setActiveBuffer(res, recordUndo = true)
            } else {
                val (f1, del1) = resolveAsDiskSourceFile(primary, primaryFile, "um_1")
                val (f2, del2) = resolveAsDiskSourceFile(secondary, secondaryFile, "um_2")
                try {
                    if (f1 == null || f2 == null) {
                        postBanner("تعذر تجهيز ملفات الدمج")
                        return@launchSingleFlightProcessing
                    }
                    val gain1 = (1f + params.primaryLevelPct / 100f).coerceIn(0.1f, 2.0f)
                    val gain2 = (1f + params.secondaryLevelPct / 100f).coerceIn(0.1f, 2.0f)
                    val effSecGain = if (params.mode == MergeStudioMode.OVERLAY && params.autoDuckingEnabled) {
                        (0.45f * gain2).coerceIn(0.05f, 1.35f)
                    } else {
                        gain2
                    }
                    val outFile = mixAudioFilesWithFfmpeg(
                        context = getApplication(),
                        primaryFile = f1,
                        secondaryFile = f2,
                        primaryGain = gain1,
                        secondaryGain = effSecGain,
                        autoDucking = params.autoDuckingEnabled,
                        mode = params.mode,
                        crossfadeSec = params.crossfadeSec,
                        offsetSec = params.overlayStartOffsetSec,
                        primaryStartOffsetSec = params.primaryStartOffsetSec,
                        overlayFadeInSec = params.overlayFadeInSec,
                        overlayFadeOutSec = params.overlayFadeOutSec,
                        secondaryDurationSec = _uiState.value.mixBgDurationMs / 1000f,
                        transitionGapSec = params.transitionGapSec,
                        autoLeveling = params.autoLevelingEnabled,
                        duckingStrengthPct = params.duckingStrengthPct,
                        echoTransitionEnabled = params.echoTransitionEnabled,
                        echoTransitionPct = params.echoTransitionPct,
                        gapWhooshEnabled = params.gapWhooshEnabled,
                        whooshIntensityPct = params.whooshIntensityPct,
                        levelingStrengthPct = params.levelingStrengthPct,
                        primaryDurationSec = _uiState.value.durationMs / 1000f,
                    ) ?: run {
                        postBanner("تعذر دمج الملفات الكبيرة عبر FFmpeg")
                        return@launchSingleFlightProcessing
                    }
                    applyProcessedDiskSourceFile(outFile, appliedToolLabel = "MergeStudio")
                } finally {
                    if (del1) runCatching { f1?.delete() }
                    if (del2) runCatching { f2?.delete() }
                }
            }
            postBanner("تم تطبيق الدمج والتراكب في الاستوديو مع حفظ التراجع")
        }
    }

    fun exportUnifiedMergeOverlay(
        params: MergeStudioParams,
        format: ExportAudioFormat,
        fileName: String,
        shareAfter: Boolean = false,
        targetPackage: String? = null,
        loudnessTarget: PlatformLoudnessTarget = _uiState.value.selectedExportLoudness,
    ) {
        val primary = activeBuffer
        val primaryFile = activeSourceFile
        if (primary == null && (primaryFile == null || !primaryFile.exists())) {
            postBanner(t(_uiState.value.language, "no_audio_loaded"))
            return
        }
        val secondary = mixBgBuffer
        val secondaryFile = mixBgSourceFile
        if (secondary == null && (secondaryFile == null || !secondaryFile.exists())) {
            exportRenderedAudio(format, fileName, shareAfter, targetPackage, loudnessTarget)
            return
        }
        stopToolPreview()
        beginExportTracking(fileName)
        launchSingleFlightProcessing(
            exportInitProgress = 0.1f,
            trackExport = true,
            notifyCompletion = false,
        ) {
            val res: ExportedAudioResult = if (primary != null && secondary != null) {
                val combined = executeUnifiedMergeOverlay(getApplication(), primary, secondary, params)
                val finalBuf = withContext(Dispatchers.Default) { renderOfflineDsp(combined, _uiState.value.dspConfig) }
                _uiState.update { it.copy(exportProgress = 0.5f) }
                exportRenderedAudioToFile(
                    context = getApplication(),
                    buffer = finalBuf,
                    format = format,
                    baseFileName = fileName.ifBlank { "Sonix_Merge_Studio" },
                    loudnessTarget = loudnessTarget,
                ) { p ->
                    _uiState.update { it.copy(exportProgress = 0.5f + p * 0.5f) }
                }
            } else {
                val (f1, del1) = resolveAsDiskSourceFile(primary, primaryFile, "umex_1")
                val (f2, del2) = resolveAsDiskSourceFile(secondary, secondaryFile, "umex_2")
                var mergedFile: File? = null
                try {
                    if (f1 == null || f2 == null) {
                        postBanner("تعذر تجهيز ملفات الدمج للتصدير")
                        return@launchSingleFlightProcessing
                    }
                    val gain1 = (1f + params.primaryLevelPct / 100f).coerceIn(0.1f, 2.0f)
                    val gain2 = (1f + params.secondaryLevelPct / 100f).coerceIn(0.1f, 2.0f)
                    val effSecGain = if (params.mode == MergeStudioMode.OVERLAY && params.autoDuckingEnabled) {
                        (0.45f * gain2).coerceIn(0.05f, 1.35f)
                    } else {
                        gain2
                    }
                    mergedFile = mixAudioFilesWithFfmpeg(
                        context = getApplication(),
                        primaryFile = f1,
                        secondaryFile = f2,
                        primaryGain = gain1,
                        secondaryGain = effSecGain,
                        autoDucking = params.autoDuckingEnabled,
                        mode = params.mode,
                        crossfadeSec = params.crossfadeSec,
                        offsetSec = params.overlayStartOffsetSec,
                        primaryStartOffsetSec = params.primaryStartOffsetSec,
                        overlayFadeInSec = params.overlayFadeInSec,
                        overlayFadeOutSec = params.overlayFadeOutSec,
                        secondaryDurationSec = _uiState.value.mixBgDurationMs / 1000f,
                        transitionGapSec = params.transitionGapSec,
                        autoLeveling = params.autoLevelingEnabled,
                        duckingStrengthPct = params.duckingStrengthPct,
                        echoTransitionEnabled = params.echoTransitionEnabled,
                        echoTransitionPct = params.echoTransitionPct,
                        gapWhooshEnabled = params.gapWhooshEnabled,
                        whooshIntensityPct = params.whooshIntensityPct,
                        levelingStrengthPct = params.levelingStrengthPct,
                        primaryDurationSec = _uiState.value.durationMs / 1000f,
                    ) ?: run {
                        postBanner("تعذر دمج الملفات قبل التصدير")
                        return@launchSingleFlightProcessing
                    }
                    _uiState.update { it.copy(exportProgress = 0.55f) }
                    exportSourceFileDirectlyWithFfmpeg(
                        context = getApplication(),
                        sourceFile = mergedFile,
                        format = format,
                        baseFileName = fileName.ifBlank { "Sonix_Merge_Studio" },
                        loudnessTarget = loudnessTarget,
                    ) ?: run {
                        postBanner("تعذر تصدير الناتج النهائي")
                        return@launchSingleFlightProcessing
                    }
                } finally {
                    if (del1) runCatching { f1?.delete() }
                    if (del2) runCatching { f2?.delete() }
                    runCatching { mergedFile?.delete() }
                }
            }
            pendingExportResult = res
            if (shareAfter) {
                val app = getApplication<Application>()
                val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", res.file)
                _sideEffects.tryEmit(StudioSideEffect.LaunchShareIntent(uri, res.mimeType, res.file.name, targetPackage))
                completeExportTracking(res.file.name, if (_uiState.value.language == AppLanguage.EN) "Share" else "مشاركة")
            } else {
                val cleanName = fileName.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_").ifEmpty { "Sonix_Merge_Studio" }
                dispatchSaveToDevice(res, "$cleanName.${res.extension}")
            }
        }
    }

    fun toggleToolBufferPreview(
        toolLabel: String,
        computePreview: suspend () -> StereoAudioBuffer?,
    ): Job? {
        if (
            _uiState.value.isToolPreviewPlaying &&
            hasGlobalToolPreview()
        ) {
            stopToolPreview()
            return null
        }
        return viewModelScope.launch(safeCoroutineHandler) {
            stopToolPreview()
            captureToolPreviewOriginalState()
            player.pause()
            stopMixPreviewPlayer()
            _uiState.update { it.copy(isBusy = true) }
            try {
                val buf = computePreview()
                if (buf == null || buf.frameCount <= 0) {
                    postBanner("تعذر توليد معاينة صوتية لهذا التعديل")
                    return@launch
                }
                toolPreviewBuffer = buf
                syncPlayerConfig(_uiState.value.dspConfig)
                player.setBuffer(buf)
                player.setPlaybackBounds(false, 0f, 1f)
                ExclusiveAudioController.onBeforeStudioPlayerPlay(player)
                val started = player.play()
                if (started) {
                    _uiState.update { it.copy(isToolPreviewPlaying = true) }
                } else {
                    stopToolPreview()
                    postBanner("تعذر تشغيل المعاينة")
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                AppDiagnosticLog.logRecoverable(
                    toolName = toolLabel,
                    titleAr = "تعذر تشغيل معاينة التعديل",
                    titleEn = "Preview Playback Failed",
                    causeAr = t.localizedMessage ?: t.javaClass.simpleName,
                    causeEn = t.localizedMessage ?: t.javaClass.simpleName,
                    errorCode = t.javaClass.simpleName,
                    throwable = t,
                )
                stopToolPreview()
                postBanner("تعذر تشغيل المعاينة")
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun stopToolPreview() {
        val activePreviewFile = toolPreviewPlaybackFile
        val stoppingOnlinePreview = activePreviewFile != null &&
            activePreviewFile.absolutePath == onlinePreviewFile?.absolutePath
        val stoppingAiOutput = activePreviewFile != null &&
            activePreviewFile.absolutePath == _aiVoiceUiState.value.lastOutputWavPath
        val stoppingTtsPreview = activePreviewFile != null &&
            activePreviewFile.absolutePath == ttsPreviewTempFile?.absolutePath
        val previewFileToDelete = if (toolPreviewDeleteFileOnStop) activePreviewFile else null
        if (hasGlobalToolPreview()) {
            player.stop()
            toolPreviewBuffer = null
            toolPreviewPlaybackFile = null
            toolPreviewFileDurationMs = 0L
            toolPreviewFileSampleRate = 44100
            toolPreviewDeleteFileOnStop = false
            restoreActivePlayerSource()
            syncPlayerConfig(_uiState.value.dspConfig)
        }
        val restoredHasAudio = toolPreviewOriginalHasAudio ?: hasLoadedAudio()
        val restoredDurationMs = toolPreviewOriginalDurationMs ?: _uiState.value.durationMs
        toolPreviewOriginalHasAudio = null
        toolPreviewOriginalDurationMs = null
        _uiState.update {
            it.copy(
                hasAudio = restoredHasAudio,
                durationMs = restoredDurationMs,
                isToolPreviewPlaying = false,
                batchQueueState = it.batchQueueState.copy(previewingItemId = null),
            )
        }
        if (stoppingAiOutput) {
            _aiVoiceUiState.update { it.copy(isPlayingOutput = false, playbackPositionMs = 0L) }
        }
        if (stoppingTtsPreview) {
            runCatching { ttsPreviewTempFile?.delete() }
            ttsPreviewTempFile = null
            _uiState.update { it.copy(ttsUiState = it.ttsUiState.copy(isPreviewing = false)) }
        }
        if (stoppingOnlinePreview) {
            onlinePreviewFile = null
            _uiState.update { it.copy(previewingOnlineUrl = null) }
        }
        runCatching { previewFileToDelete?.delete() }
    }

    fun previewFileInGlobalPlayer(file: File, onFailure: () -> Unit) {
        viewModelScope.launch(safeCoroutineHandler) {
            if (!startGlobalFilePreview(file)) {
                onFailure()
            }
        }
    }

    private fun captureToolPreviewOriginalState() {
        if (toolPreviewOriginalHasAudio == null) {
            toolPreviewOriginalHasAudio = _uiState.value.hasAudio
            toolPreviewOriginalDurationMs = _uiState.value.durationMs
        }
    }

    private suspend fun startGlobalFilePreview(
        file: File,
        deleteFileOnStop: Boolean = false,
    ): Boolean {
        if (!file.exists() || file.length() <= 0L) return false
        stopToolPreview()
        player.pause()
        val metadata = AudioDecoder.inspectLargeAudioStreaming(
            file = file,
            buckets = 1,
            context = getApplication(),
        ) {}
        if (metadata == null) return false

        captureToolPreviewOriginalState()
        toolPreviewPlaybackFile = file
        toolPreviewFileDurationMs = metadata.durationMs
        toolPreviewFileSampleRate = metadata.sampleRate
        toolPreviewDeleteFileOnStop = deleteFileOnStop
        syncPlayerConfig(_uiState.value.dspConfig)
        player.setSourceFile(file, metadata.durationMs, metadata.sampleRate)
        player.setPlaybackBounds(false, 0f, 1f)
        _uiState.update { it.copy(hasAudio = true) }
        ExclusiveAudioController.onBeforeStudioPlayerPlay(player)
        val started = player.play()
        if (started) {
            _uiState.update { it.copy(isToolPreviewPlaying = true) }
        } else {
            stopToolPreview()
        }
        return started
    }

    private fun restoreActivePlayerSource() {
        val sourceBuffer = activeBuffer
        if (sourceBuffer != null) {
            player.setBuffer(sourceBuffer)
        } else {
            player.setSourceFile(
                file = activeSourceFile,
                durationMs = _uiState.value.durationMs,
                sampleRate = activeSourceSampleRate,
            )
        }
        player.setPlaybackBounds(editorPlaybackBoundsEnabled, 0f, 1f)
    }

    // Recent Edits Temporary Cache & Diagnostics Helpers
    fun saveToolSessionToCache(
        toolId: String,
        titleAr: String,
        titleEn: String,
        summaryAr: String,
        summaryEn: String,
        floatParams: Map<String, Float>,
        stringParams: Map<String, String> = emptyMap(),
    ) {
        val buf = activeBuffer ?: return
        if (buf.frameCount <= 0) return
        val audioName = _uiState.value.activeProjectName.ifBlank { "Studio Clip" }
        val app = getApplication<Application>()
        viewModelScope.launch(safeCoroutineHandler) {
            RecentEditsCacheManager.saveSession(
                context = app,
                toolId = toolId,
                titleAr = titleAr,
                titleEn = titleEn,
                audioName = audioName,
                summaryAr = summaryAr,
                summaryEn = summaryEn,
                buffer = buf,
                floatParams = floatParams,
                stringParams = stringParams,
            )
        }
    }

    fun restoreRecentEditSession(
        entry: RecentEditEntry,
        onNavigateToRoute: (StudioRoute) -> Unit,
    ) {
        viewModelScope.launch(safeCoroutineHandler) {
            _uiState.update { it.copy(isBusy = true) }
            try {
                val file = File(entry.wavPath)
                if (!file.exists()) {
                    RecentEditsCacheManager.removeEntry(entry.id)
                    postBanner("انتهت صلاحية الجلسة المؤقتة لهذا التعديل")
                    return@launch
                }
                if (withContext(Dispatchers.IO) { AudioDecoder.shouldStreamOnDisk(file) }) {
                    val staged = stageLargeFileToCleanAsciiCache(getApplication(), file, prefix = "recent")
                    val meta = AudioDecoder.inspectLargeAudioStreaming(staged, buckets = 800, context = getApplication()) {}
                    if (meta == null) {
                        runCatching { staged.delete() }
                        postBanner("تعذر استرجاع الملف المؤقت")
                        return@launch
                    }
                    undoStack.clear()
                    redoStack.clear()
                    activeBuffer = null
                    replaceActiveSourceFile(staged)
                    activeSourceSampleRate = meta.sampleRate
                    player.setSourceFile(staged, meta.durationMs, meta.sampleRate, resetPosition = true)
                    _uiState.update {
                        it.copy(
                            hasAudio = true,
                            durationMs = meta.durationMs,
                            waveformPeaks = meta.waveformPeaks,
                            originalPeakDbfs = meta.peakDbfs,
                            activeProjectName = entry.audioName,
                            restoredToolSession = entry,
                        )
                    }
                } else {
                    val decoded = withContext(Dispatchers.IO) { AudioDecoder.decodeFile(file) }
                    if (decoded == null || decoded.frameCount <= 0) {
                        postBanner("تعذر استرجاع الملف المؤقت")
                        return@launch
                    }
                    setActiveBuffer(decoded, recordUndo = false, resetDspOnNewEntry = false)
                    _uiState.update {
                        it.copy(
                            activeProjectName = entry.audioName,
                            restoredToolSession = entry,
                        )
                    }
                }
                val route = when (entry.toolId) {
                    "editor" -> StudioRoute.Editor
                    "surround_stereo" -> StudioRoute.SurroundStereo
                    "merge_studio" -> StudioRoute.AudioMerge
                    "smart_mixer" -> StudioRoute.SmartMusicMixer
                    "pitch_speed" -> StudioRoute.PitchAndSpeed
                    "voice_purifier" -> StudioRoute.VoicePurifier
                    "voice_transformer" -> StudioRoute.VoiceChanger
                    "echo_reverb" -> StudioRoute.EchoSpace
                    "reverse_audio" -> StudioRoute.ReverseAudio
                    "generate_silence" -> StudioRoute.GenerateSilence
                    "reels_audio_templates" -> StudioRoute.ReelsAudioTemplates
                    else -> StudioRoute.Editor
                }
                onNavigateToRoute(route)
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun consumeRestoredToolSession(toolId: String): RecentEditEntry? {
        val cur = _uiState.value.restoredToolSession ?: return null
        if (cur.toolId != toolId) return null
        _uiState.update { it.copy(restoredToolSession = null) }
        return cur
    }

    fun markDiagnosticRead(id: String) {
        AppDiagnosticLog.markRead(id)
    }

    fun markDiagnosticsRead() {
        AppDiagnosticLog.markAllRead()
    }

    fun clearAllDiagnostics() {
        AppDiagnosticLog.clearAll()
    }

    fun splitAudioAtFraction(fraction: Float, loadPart: Int, saveBothPartsToLibrary: Boolean) {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner("يرجى اختيار ملف صوتي أولاً لتقسيمه")
            return
        }
        launchSingleFlightProcessing {
            if (src != null) {
                val (part1, part2) = splitAudioByTime(src, fraction)
                if (saveBothPartsToLibrary) {
                    val base = _uiState.value.activeProjectName.ifBlank { "Clip" }
                    repository.indexAudioRecording("$base • الجزء 1", part1, RecordingSourceType.RECORDING)
                    repository.indexAudioRecording("$base • الجزء 2", part2, RecordingSourceType.RECORDING)
                }
                setActiveBuffer(if (loadPart == 2) part2 else part1, recordUndo = true)
            } else if (srcFile != null) {
                val durSec = (_uiState.value.durationMs / 1000.0).coerceAtLeast(0.2)
                val splitPointSec = (durSec * fraction).coerceIn(0.05, durSec - 0.05)
                val splitSec = String.format(Locale.US, "%.3f", splitPointSec)
                val preArgs = if (loadPart == 2) "-ss $splitSec" else ""
                val postArgs = if (loadPart == 2) "" else "-t $splitSec"
                val partFilter = if (loadPart == 2) {
                    "afade=t=in:st=0:d=0.004"
                } else {
                    val fStart = String.format(Locale.US, "%.3f", (splitPointSec - 0.004).coerceAtLeast(0.0))
                    "afade=t=out:st=$fStart:d=0.004"
                }
                val outFile = processAudioFileWithFfmpeg(
                    context = getApplication(),
                    inputFile = srcFile,
                    audioFilter = partFilter,
                    preInputArgs = preArgs,
                    postInputArgs = postArgs,
                    toolName = "SplitAudioLarge",
                ) ?: run {
                    postBanner("تعذر تقسيم الملف الصوتي الكبير")
                    return@launchSingleFlightProcessing
                }
                applyProcessedDiskSourceFile(outFile, appliedToolLabel = "Split")
            }
            postBanner("تم تقسيم المسار الصوتي وتحميل الجزء المختار في الاستوديو")
        }
    }

    fun splitStereoChannel(keepLeft: Boolean) {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner("يرجى اختيار ملف صوتي أولاً لفصل القنوات")
            return
        }
        launchSingleFlightProcessing {
            if (src != null) {
                val (leftBuf, rightBuf) = withContext(Dispatchers.Default) { splitStereoChannels(src) }
                setActiveBuffer(if (keepLeft) leftBuf else rightBuf, recordUndo = true)
            } else if (srcFile != null) {
                val filter = if (keepLeft) "pan=stereo|c0=c0|c1=c0" else "pan=stereo|c0=c1|c1=c1"
                val outFile = processAudioFileWithFfmpeg(
                    context = getApplication(),
                    inputFile = srcFile,
                    audioFilter = filter,
                    toolName = "SplitStereoChannelLarge",
                ) ?: run {
                    postBanner("تعذر عزل القناة الصوتية")
                    return@launchSingleFlightProcessing
                }
                applyProcessedDiskSourceFile(outFile, appliedToolLabel = "StereoSplit")
            }
            postBanner(if (keepLeft) "تم عزل القناة اليسرى (Left Channel)" else "تم عزل القناة اليمنى (Right Channel)")
        }
    }

    fun compressAndExportAudio(bitrateKbps: Int, fileName: String, shareAfter: Boolean) {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner("لا يوجد صوت لضغطه حالياً")
            return
        }
        beginExportTracking(fileName)
        launchSingleFlightProcessing(
            exportInitProgress = 0.1f,
            trackExport = true,
            notifyCompletion = false,
        ) {
            val res: ExportedAudioResult = if (src != null) {
                val rendered = withContext(Dispatchers.Default) {
                    renderOfflineDsp(src, _uiState.value.dspConfig)
                }
                _uiState.update { it.copy(exportProgress = 0.45f) }
                exportRenderedAudioToFile(
                    context = getApplication(),
                    buffer = rendered,
                    format = ExportAudioFormat.M4A,
                    baseFileName = fileName.ifBlank { "Sonix_${bitrateKbps}kbps" },
                    bitrateKbps = bitrateKbps,
                ) { p ->
                    _uiState.update { it.copy(exportProgress = 0.45f + p * 0.55f) }
                }
            } else {
                exportSourceFileDirectlyWithFfmpeg(
                    context = getApplication(),
                    sourceFile = srcFile!!,
                    format = ExportAudioFormat.M4A,
                    baseFileName = fileName.ifBlank { "Sonix_${bitrateKbps}kbps" },
                    loudnessTarget = _uiState.value.selectedExportLoudness,
                    dspConfig = _uiState.value.dspConfig,
                ) ?: run {
                    postBanner("تعذر ضغط وتصدير الملف الصوتي")
                    return@launchSingleFlightProcessing
                }
            }
            pendingExportResult = res
            if (shareAfter) {
                val app = getApplication<Application>()
                val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", res.file)
                _sideEffects.tryEmit(StudioSideEffect.LaunchShareIntent(uri, res.mimeType, res.file.name))
                completeExportTracking(res.file.name, if (_uiState.value.language == AppLanguage.EN) "Share" else "مشاركة")
            } else {
                val cleanName = fileName.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_").ifEmpty { "Sonix_${bitrateKbps}k" }
                dispatchSaveToDevice(res, "$cleanName.${res.extension}")
            }
        }
    }

    fun stageVideoForExtract(videoUri: Uri) {
        _uiState.update { it.copy(pendingExtractVideoUri = videoUri.toString()) }
    }

    fun consumePendingExtractVideoUri(): Uri? {
        val raw = _uiState.value.pendingExtractVideoUri ?: return null
        _uiState.update { it.copy(pendingExtractVideoUri = null) }
        return runCatching { Uri.parse(raw) }.getOrNull()
    }

    fun extractAudioFromVideoUri(videoUri: Uri) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            withContext(Dispatchers.IO) {
                runCatching {
                    app.contentResolver.takePersistableUriPermission(
                        videoUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
            }
            val resolvedName = withContext(Dispatchers.IO) {
                AudioFileUploader.resolveDisplayName(app, videoUri)
            }
            _uiState.update {
                it.copy(
                    isBusy = true,
                    isProcessing = true,
                    importProgress = 0f,
                    importLabel = resolvedName,
                    importBytesLabel = null,
                    importBytesProgress = null,
                )
            }
            var tempVideoFile: File? = null
            var tempExtractedFile: File? = null
            try {
                player.stop()
                // Phase 1 (0% -> 40%): Copy selected video URI into cacheDir
                val uploadOutcome = AudioFileUploader.copyAudioUriToCacheDetailed(app, videoUri) { info ->
                    val bytesText = if (info.totalBytes > 0L) {
                        "${AudioFileUploader.formatBytes(info.bytesCopied)} / ${AudioFileUploader.formatBytes(info.totalBytes)}"
                    } else {
                        AudioFileUploader.formatBytes(info.bytesCopied)
                    }
                    _uiState.update {
                        it.copy(
                            importProgress = (info.fraction * 0.40f).coerceIn(0f, 0.40f),
                            importLabel = info.fileName,
                            importBytesLabel = bytesText,
                            importBytesProgress = info.fraction.takeIf { info.totalBytes > 0L },
                        )
                    }
                }
                val uploaded = when (uploadOutcome) {
                    is AudioUploadOutcome.Success -> uploadOutcome.uploaded
                    is AudioUploadOutcome.Failure -> {
                        postBanner("تعذر نسخ ملف الفيديو: ${uploadOutcome.errorMessage}")
                        return@launch
                    }
                }
                tempVideoFile = uploaded.file

                // Phase 2 (40% -> 75%): Strip video frames (-vn) and extract clean audio track
                _uiState.update {
                    it.copy(
                        importProgress = 0.42f,
                        importLabel = "جاري فصل المسار الصوتي من الفيديو: ${uploaded.displayName}",
                        importBytesLabel = null,
                        importBytesProgress = null,
                    )
                }
                val extractedFile = AudioDecoder.extractAudioTrackFromVideoFile(app, uploaded.file) { frac ->
                    _uiState.update {
                        it.copy(importProgress = (0.40f + frac * 0.35f).coerceIn(0.40f, 0.75f))
                    }
                }
                runCatching { uploaded.file.delete() }
                tempVideoFile = null

                if (extractedFile == null || !extractedFile.exists() || extractedFile.length() <= 0L) {
                    postBanner("تعذر العثور على مسار صوتي صالح داخل الفيديو المحدد")
                    return@launch
                }
                tempExtractedFile = extractedFile

                // Phase 3 (75% -> 100%): Load extracted audio into Studio (RAM or zero-RAM streaming)
                val title = uploaded.displayName.substringBeforeLast('.').ifEmpty { "صوت مستخرج من فيديو" }
                if (withContext(Dispatchers.IO) { AudioDecoder.shouldStreamOnDisk(extractedFile) }) {
                    val stagedFile = stageLargeFileToCleanAsciiCache(app, extractedFile, prefix = "video_audio")
                    runCatching { extractedFile.delete() }
                    tempExtractedFile = null
                    val meta = AudioDecoder.inspectLargeAudioStreaming(stagedFile, buckets = 800, context = app) { frac ->
                        _uiState.update {
                            it.copy(importProgress = (0.75f + frac * 0.21f).coerceIn(0.75f, 0.96f))
                        }
                    }
                    if (meta == null) {
                        runCatching { stagedFile.delete() }
                        postBanner("تعذر قراءة الصوت المستخرج من الفيديو")
                        return@launch
                    }
                    undoStack.clear()
                    redoStack.clear()
                    lastDspSnapshotTimeMs = 0L
                    activeBuffer = null
                    replaceActiveSourceFile(stagedFile)
                    activeSourceSampleRate = meta.sampleRate
                    val cleanCfg = DspConfig()
                    syncPlayerConfig(cleanCfg, holdBypass = false)
                    player.setClipGainDb(0f)
                    player.setSourceFile(
                        file = stagedFile,
                        durationMs = meta.durationMs,
                        sampleRate = meta.sampleRate,
                        resetPosition = true,
                    )
                    _uiState.update { cur ->
                        cur.copy(
                            hasAudio = true,
                            durationMs = meta.durationMs,
                            waveformPeaks = meta.waveformPeaks,
                            originalPeakDbfs = meta.peakDbfs,
                            dspConfig = cleanCfg,
                            activePresetId = null,
                            selectedPresetLabel = "",
                            activeCloudDraftId = null,
                            isHoldBypassing = false,
                            audioMarkers = emptyList(),
                            trimStart = 0f,
                            trimEnd = 1f,
                            canUndoEdit = false,
                            canRedoEdit = false,
                            hasUnsavedChanges = false,
                            activeProjectName = if (cur.activeProjectId == 0L) title else cur.activeProjectName,
                            importProgress = 1.0f,
                        )
                    }
                    postBanner("تم استخراج الصوت من الفيديو ($title) وتحميله بالتدفق المباشر الآمن")
                } else {
                    val decodeOutcome = AudioDecoder.decodeFileDetailedWithProgress(extractedFile) { decFrac ->
                        _uiState.update {
                            it.copy(importProgress = (0.75f + decFrac * 0.21f).coerceIn(0.75f, 0.96f))
                        }
                    }
                    runCatching { extractedFile.delete() }
                    tempExtractedFile = null
                    val buf = when (decodeOutcome) {
                        is AudioDecodeOutcome.Success -> decodeOutcome.buffer
                        is AudioDecodeOutcome.Failure -> {
                            postBanner("تعذر فك ترميز الصوت المستخرج: ${decodeOutcome.reason}")
                            return@launch
                        }
                    }
                    if (buf.frameCount == 0) {
                        postBanner("تعذر استخراج الصوت: المسار الصوتي فارغ")
                        return@launch
                    }
                    setActiveBuffer(buf, recordUndo = false, resetDspOnNewEntry = true)
                    if (_uiState.value.activeProjectId == 0L) {
                        _uiState.update { it.copy(activeProjectName = title) }
                    }
                    repository.indexAudioRecording(title, buf, RecordingSourceType.IMPORTED)
                    _uiState.update { it.copy(importProgress = 1.0f) }
                    postBanner("تم استخراج الصوت من الفيديو ($title) بنقاء 100% في الاستوديو")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                AppDiagnosticLog.logRecoverable(
                    toolName = "ExtractVideoAudio",
                    titleAr = "تعذر استخراج الصوت من الفيديو",
                    titleEn = "Video Audio Extraction Failed",
                    causeAr = t.localizedMessage ?: t.javaClass.simpleName,
                    causeEn = t.localizedMessage ?: t.javaClass.simpleName,
                    errorCode = t.javaClass.simpleName,
                    throwable = t,
                )
                postBanner("تعذر استخراج الصوت من الفيديو: ${t.localizedMessage ?: t.javaClass.simpleName}")
            } finally {
                runCatching { tempVideoFile?.delete() }
                runCatching { tempExtractedFile?.delete() }
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        isProcessing = false,
                        importProgress = null,
                        importLabel = null,
                        importBytesLabel = null,
                        importBytesProgress = null,
                    )
                }
            }
        }
    }

    fun previewReelsAudioTemplate(params: ReelsTemplateParams) {
        val src = activeBuffer ?: run {
            postBanner(t(_uiState.value.language, "no_audio_loaded"))
            return
        }
        toggleToolBufferPreview("ReelsTemplatePreview") {
            processReelsAudioTemplate(getApplication(), src, params)
        }
    }

    fun applyReelsAudioTemplate(params: ReelsTemplateParams) {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner(t(_uiState.value.language, "no_audio_loaded"))
            return
        }
        stopToolPreview()
        launchSingleFlightProcessing(purifyInitProgress = 0.05f) {
            if (src != null) {
                val rendered = processReelsAudioTemplate(getApplication(), src, params) { p ->
                    _uiState.update { it.copy(purifyProgress = p) }
                }
                setActiveBuffer(rendered, recordUndo = true)
            } else if (srcFile != null) {
                val outFile = processAudioFileWithFfmpeg(
                    context = getApplication(),
                    inputFile = srcFile,
                    audioFilter = "highpass=f=75,acompressor=threshold=-18dB:ratio=3:attack=15:release=160,loudnorm=I=-14:TP=-1.0:LRA=9",
                    toolName = "ReelsTemplateLarge",
                ) ?: run {
                    postBanner("تعذر تطبيق قالب الريلز على الملف الكبير")
                    return@launchSingleFlightProcessing
                }
                applyProcessedDiskSourceFile(outFile, appliedToolLabel = "ReelsTemplate")
            }
            postBanner("تم تطبيق هندسة الريلز والبودكاست بمعيار -14 LUFS وحفظ التراجع")
        }
    }

    fun createRingtoneFromSelection(
        startFrac: Float,
        endFrac: Float,
        title: String,
        setAsDefault: Boolean,
        onNeedsWriteSettings: () -> Unit,
    ) {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner("يرجى اختيار أو تسجيل مقطع صوتي لصناعة النغمة")
            return
        }
        launchSingleFlightProcessing {
            val clipped: StereoAudioBuffer = if (src != null) {
                trimToSelection(src, clamp(startFrac, 0f, 0.98f), clamp(endFrac, 0.02f, 1f))
            } else {
                val durSec = (_uiState.value.durationMs / 1000.0).coerceAtLeast(0.2)
                val safeStart = clamp(startFrac, 0f, 0.98f)
                val safeEnd = clamp(endFrac, (safeStart + 0.02f).coerceAtMost(1f), 1f)
                val sSec = (durSec * safeStart).coerceIn(0.0, durSec - 0.05)
                val rawEnd = (durSec * safeEnd).coerceIn(sSec + 0.05, durSec)
                // Ringtone clips should fit safely in memory when applying offline DSP & ringtone MUX
                val eSec = rawEnd.coerceAtMost(sSec + 120.0)
                val ssStr = String.format(Locale.US, "%.3f", sSec)
                val toStr = String.format(Locale.US, "%.3f", eSec)
                val trimmedFile = processAudioFileWithFfmpeg(
                    context = getApplication(),
                    inputFile = srcFile!!,
                    preInputArgs = "-ss $ssStr -to $toStr",
                    audioFilter = "",
                    toolName = "RingtoneTrim",
                )
                if (trimmedFile == null) {
                    postBanner("تعذر قص الجزء المحدد لصناعة النغمة")
                    return@launchSingleFlightProcessing
                }
                val decoded = try {
                    AudioDecoder.decodeFile(trimmedFile)
                } finally {
                    runCatching { trimmedFile.delete() }
                }
                if (decoded == null || decoded.frameCount == 0) {
                    postBanner("تعذر قراءة المقطع المقصوص لصناعة النغمة")
                    return@launchSingleFlightProcessing
                }
                decoded
            }
            val rendered = withContext(Dispatchers.Default) {
                renderOfflineDsp(clipped, _uiState.value.dspConfig)
            }
            when (val res = saveAndSetRingtone(getApplication(), rendered, title, setAsDefault)) {
                is RingtoneSaveResult.Saved -> {
                    if (res.needsWriteSettings) {
                        postBanner("تم حفظ النغمة بمجلد الرنين — اسمح بتعديل إعدادات النظام لتعيينها نغمة افتراضية")
                        onNeedsWriteSettings()
                    } else if (res.appliedAsDefault) {
                        postBanner("تم حفظ النغمة وتعيينها كنغمة رنين افتراضية بنجاح")
                    } else {
                        postBanner("تم حفظ النغمة في مجلد نغمات الرنين بنجاح")
                    }
                }
                is RingtoneSaveResult.Error -> postBanner(res.message)
            }
        }
    }

    // TTS Studio (VoxCPM2 neural cloud synthesis)
    fun setStudioLuxuryEnhancement(enabled: Boolean) {
        FreeEmotionalTtsClient.saveStudioLuxuryEnhancement(getApplication(), enabled)
        _uiState.update { it.copy(studioLuxuryEnhancement = enabled) }
    }

    private fun releaseTtsPreviewPlayer() {
        val ttsFile = ttsPreviewTempFile
        if (ttsFile != null && toolPreviewPlaybackFile?.absolutePath == ttsFile.absolutePath) {
            stopToolPreview()
        } else {
            AudioPlayerManager.stop("tts_preview")
            runCatching { ttsFile?.delete() }
            ttsPreviewTempFile = null
        }
    }

    fun previewTtsSpeech(
        text: String,
        dialect: TtsDialect,
        gender: TtsGender,
        style: TtsDeliveryStyle,
        speechRate: Float,
        pitch: Float,
        persona: TtsVoicePersona? = null,
        acousticMode: TtsPersonaAcousticMode? = null,
    ) {
        val lang = _uiState.value.language
        if (text.isBlank()) {
            _uiState.update {
                it.copy(
                    ttsUiState = it.ttsUiState.copy(
                        errorMessage = if (lang == AppLanguage.EN) "Please enter text first." else "يرجى إدخال النص أولاً.",
                    ),
                )
            }
            return
        }
        if (TtsTextLimits.countWords(text) > TtsTextLimits.MAX_WORDS) {
            _uiState.update {
                it.copy(
                    ttsUiState = it.ttsUiState.copy(
                        errorMessage = if (lang == AppLanguage.EN) {
                            "Text is limited to ${TtsTextLimits.MAX_WORDS} words."
                        } else {
                            "الحد الأقصى للنص هو ${TtsTextLimits.MAX_WORDS} كلمة."
                        },
                    ),
                )
            }
            return
        }
        val engineMode = TtsEngineMode.VOX_NEURAL_ONLINE
        activeOnlineJob?.cancel()
        activeOnlineJob = viewModelScope.launch(safeCoroutineHandler) {
            stopTtsPreview()
            _uiState.update {
                it.copy(
                    isOnlineTaskRunning = true,
                    onlineTaskLabel = "TTS Preview",
                    ttsUiState = it.ttsUiState.copy(
                        selectedEngine = engineMode,
                        isSynthesizingPreview = true,
                        isPreviewing = false,
                        isGenerating = false,
                        usedFallbackEngine = false,
                        isOffline = false,
                        errorMessage = null,
                    ),
                )
            }
            try {
                val appCtx = getApplication<Application>()
                val cur = _uiState.value
                val outcome = smartVoiceRouter.synthesizeStudioVoiceWithStatus(
                    context = appCtx,
                    text = text.trim(),
                    dialect = dialect,
                    gender = gender,
                    style = style,
                    speechRate = speechRate,
                    pitch = pitch,
                    language = lang,
                    studioLuxuryEnhancement = cur.studioLuxuryEnhancement,
                    persona = persona,
                    acousticMode = acousticMode,
                )
                when (outcome) {
                    is VoiceRouteOutcome.Success -> {
                        val synthRes = outcome.result
                        ttsPreviewTempFile = synthRes.audioFile
                        val initiated = startGlobalFilePreview(synthRes.audioFile, deleteFileOnStop = true)
                        if (initiated) {
                            _uiState.update {
                                it.copy(
                                    ttsUiState = it.ttsUiState.copy(
                                        selectedEngine = engineMode,
                                        activeEngineUsed = outcome.engineUsed,
                                        usedFallbackEngine = outcome.usedFallback,
                                        isSynthesizingPreview = false,
                                        isPreviewing = true,
                                        errorMessage = null,
                                    ),
                                )
                            }
                        } else {
                            releaseTtsPreviewPlayer()
                            _uiState.update {
                                it.copy(
                                    ttsUiState = it.ttsUiState.copy(
                                        selectedEngine = engineMode,
                                        isSynthesizingPreview = false,
                                        isPreviewing = false,
                                        errorMessage = if (lang == AppLanguage.EN) {
                                            "Unable to start audio preview playback."
                                        } else {
                                            "تعذر بدء تشغيل المعاينة الصوتية."
                                        },
                                    ),
                                )
                            }
                        }
                    }
                    is VoiceRouteOutcome.ProviderError -> {
                        _uiState.update {
                            it.copy(
                                ttsUiState = it.ttsUiState.copy(
                                    selectedEngine = engineMode,
                                    isSynthesizingPreview = false,
                                    isPreviewing = false,
                                    errorMessage = outcome.message,
                                ),
                            )
                        }
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                releaseTtsPreviewPlayer()
                AppDiagnosticLog.logRecoverable(
                    toolName = "TTS Preview",
                    titleAr = "تعذر تشغيل معاينة تحويل النص",
                    titleEn = "TTS Preview Safely Recovered",
                    causeAr = t.localizedMessage ?: t.javaClass.simpleName,
                    causeEn = t.localizedMessage ?: t.javaClass.simpleName,
                    errorCode = t.javaClass.simpleName,
                    throwable = t,
                )
                val safeMsg = t.localizedMessage?.takeIf { it.isNotBlank() } ?: t.javaClass.simpleName
                _uiState.update {
                    it.copy(
                        ttsUiState = it.ttsUiState.copy(
                            selectedEngine = engineMode,
                            isSynthesizingPreview = false,
                            isPreviewing = false,
                            errorMessage = if (lang == AppLanguage.EN) {
                                "TTS preview stopped safely: $safeMsg"
                            } else {
                                "تم إيقاف المعاينة بأمان لتفادي الإغلاق: $safeMsg"
                            },
                        ),
                    )
                }
            } finally {
                _uiState.update {
                    it.copy(
                        isOnlineTaskRunning = false,
                        onlineTaskLabel = "",
                        ttsUiState = it.ttsUiState.copy(isSynthesizingPreview = false),
                    )
                }
            }
        }
    }

    fun stopTtsPreview() {
        releaseTtsPreviewPlayer()
        _uiState.update {
            it.copy(
                ttsUiState = it.ttsUiState.copy(
                    isSynthesizingPreview = false,
                    isPreviewing = false,
                ),
            )
        }
    }

    fun generateTtsToStudio(
        text: String,
        dialect: TtsDialect,
        gender: TtsGender,
        style: TtsDeliveryStyle,
        speechRate: Float,
        pitch: Float,
        appendToExisting: Boolean,
        persona: TtsVoicePersona? = null,
        acousticMode: TtsPersonaAcousticMode? = null,
    ) {
        val lang = _uiState.value.language
        if (TtsTextLimits.countWords(text) > TtsTextLimits.MAX_WORDS) {
            _uiState.update {
                it.copy(
                    ttsUiState = it.ttsUiState.copy(
                        errorMessage = if (lang == AppLanguage.EN) {
                            "Text is limited to ${TtsTextLimits.MAX_WORDS} words."
                        } else {
                            "الحد الأقصى للنص هو ${TtsTextLimits.MAX_WORDS} كلمة."
                        },
                    ),
                )
            }
            return
        }
        if (text.isBlank()) return
        val engineMode = TtsEngineMode.VOX_NEURAL_ONLINE
        activeOnlineJob?.cancel()
        activeOnlineJob = viewModelScope.launch(safeCoroutineHandler) {
            _uiState.update {
                it.copy(
                    isBusy = true,
                    isOnlineTaskRunning = true,
                    onlineTaskLabel = "TTS Generate",
                    ttsUiState = it.ttsUiState.copy(
                        selectedEngine = engineMode,
                        isGenerating = true,
                        usedFallbackEngine = false,
                        errorMessage = null,
                    ),
                )
            }
            try {
                stopTtsPreview()
                val cur = _uiState.value
                val outcome = smartVoiceRouter.synthesizeStudioVoiceWithStatus(
                    context = getApplication(),
                    text = text.trim(),
                    dialect = dialect,
                    gender = gender,
                    style = style,
                    speechRate = speechRate,
                    pitch = pitch,
                    language = lang,
                    studioLuxuryEnhancement = cur.studioLuxuryEnhancement,
                    persona = persona,
                    acousticMode = acousticMode,
                )
                when (outcome) {
                    is VoiceRouteOutcome.Success -> {
                        val synthRes = outcome.result
                        runCatching { synthRes.audioFile.delete() }
                        val rawBuf = synthRes.buffer
                        if (rawBuf.frameCount == 0) {
                            _uiState.update {
                                it.copy(
                                    ttsUiState = it.ttsUiState.copy(
                                        selectedEngine = engineMode,
                                        isGenerating = false,
                                        errorMessage = if (lang == AppLanguage.EN) {
                                            "Synthesized audio buffer is empty."
                                        } else {
                                            "الملف الصوتي المولّد فارغ."
                                        },
                                    ),
                                )
                            }
                            return@launch
                        }
                        val existing = activeBuffer
                        val isAppend = appendToExisting && existing != null
                        val finalBuf = if (isAppend) {
                            mergeClipsStreamingOnDisk(getApplication(), existing!!, rawBuf, crossfadeMs = 15)
                        } else {
                            rawBuf
                        }
                        setActiveBuffer(
                            buf = finalBuf,
                            recordUndo = existing != null,
                            resetDspOnNewEntry = existing == null && !isAppend,
                        )
                        val dialectLabel = t(lang, dialect.titleKey)
                        val personaTag = persona?.let {
                            if (lang == AppLanguage.EN) it.titleEn else it.titleAr
                        }
                        val engineTag = "VoxCPM2 Neural • Cloud"
                        val titleLabel = listOfNotNull(engineTag, personaTag, dialectLabel).joinToString(" • ")
                        repository.indexAudioRecording(titleLabel, rawBuf, RecordingSourceType.TTS)
                        _uiState.update {
                            it.copy(
                                ttsUiState = it.ttsUiState.copy(
                                    selectedEngine = engineMode,
                                    activeEngineUsed = outcome.engineUsed,
                                    usedFallbackEngine = outcome.usedFallback,
                                    isGenerating = false,
                                    errorMessage = null,
                                ),
                            )
                        }
                        val bannerPersona = personaTag?.let { " — $it" }.orEmpty()
                        val modeNote = "VoxCPM2"
                        postBanner("تم توليد وإدراج الصوت ($modeNote • $dialectLabel$bannerPersona) بنجاح")
                    }
                    is VoiceRouteOutcome.ProviderError -> {
                        _uiState.update {
                            it.copy(
                                ttsUiState = it.ttsUiState.copy(
                                    selectedEngine = engineMode,
                                    isGenerating = false,
                                    errorMessage = outcome.message,
                                ),
                            )
                        }
                    }
                }
            } finally {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        isOnlineTaskRunning = false,
                        onlineTaskLabel = "",
                        ttsUiState = it.ttsUiState.copy(isGenerating = false),
                    )
                }
            }
        }
    }

    // Studio-Quality Formant & AudioSkillsPresets Voice Character Transformer
    fun transformVoiceOffline(
        pitchSemitones: Float,
        formantShift: Float,
        warmthDb: Float,
        presenceDb: Float,
        mix: Float,
        characterLabel: String,
        bassBoostDb: Float = 0f,
        bassFreqHz: Float = 75f,
        vibratoHz: Float = 0f,
        vibratoDepth: Float = 0f,
        echoDelayMs: Float = 0f,
        echoDecay: Float = 0f,
        dualLayerPitchSemitones: Float = 0f,
        dualLayerFormantShift: Float = 1.0f,
        dualLayerMix: Float = 0f,
        acousticMode: TtsPersonaAcousticMode = TtsPersonaAcousticMode.NATURAL,
        style: TtsDeliveryStyle = TtsDeliveryStyle.NATURAL_WARM,
        gender: TtsGender = TtsGender.MALE,
    ) {
        val src = activeBuffer ?: run {
            postBanner("سجّل أو استورد مقطعاً صوتياً أولاً لتغيير الشخصية الصوتية")
            return
        }
        launchSingleFlightProcessing(purifyInitProgress = 0.05f) {
            val rawTransformed = runTransformVoiceOffline(
                source = src,
                pitchSemitones = pitchSemitones,
                formantShift = formantShift,
                warmthDb = warmthDb,
                presenceDb = presenceDb,
                mix = mix,
                bassBoostDb = bassBoostDb,
                bassFreqHz = bassFreqHz,
                vibratoHz = vibratoHz,
                vibratoDepth = vibratoDepth,
                echoDelayMs = echoDelayMs,
                echoDecay = echoDecay,
                dualLayerPitchSemitones = dualLayerPitchSemitones,
                dualLayerFormantShift = dualLayerFormantShift,
                dualLayerMix = dualLayerMix,
                studioMasteringPolish = true,
            ) { p ->
                _uiState.update { it.copy(purifyProgress = p * 0.88f) }
            }
            val finalTransformed = if (acousticMode == TtsPersonaAcousticMode.NATURAL) {
                rawTransformed
            } else {
                StudioLuxuryAudioFilter.applyAcousticMastering(
                    context = getApplication(),
                    decodedBuffer = rawTransformed,
                    mode = acousticMode,
                    style = style,
                    gender = gender,
                    luxuryBoost = true,
                )
            }
            _uiState.update { it.copy(purifyProgress = 1f) }
            setActiveBuffer(finalTransformed, recordUndo = true)
            postBanner("تم تحويل الصوت إلى شخصية \"$characterLabel\" بجودة الاستوديو بنجاح")
        }
    }

    /**
     * تحويل صوت بشري ذكي (Voice-to-Voice) عبر المحرك الهجين:
     * 1. نموذج ONNX المحلي في حال توفره داخل `assets/models/`
     * 2. Gemini 2.5 Audio Multimodal + إعادة الصياغة الصوتي بالشخصية المستهدفة + تلميع FFmpegKit
     */
    fun convertVoiceWithAi(
        preset: VoiceCharacterPreset?,
        targetLabel: String,
        pitchSemitones: Float,
        formantShift: Float,
        warmthDb: Float,
        presenceDb: Float,
        targetPersona: TtsVoicePersona? = null,
        acousticMode: TtsPersonaAcousticMode = targetPersona?.defaultAcousticMode ?: TtsPersonaAcousticMode.NATURAL,
    ) {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner("سجّل أو استورد مقطعاً صوتياً أولاً لتحويل الشخصية بالذكاء الاصطناعي")
            return
        }
        val lang = _uiState.value.language
        activeOnlineJob?.cancel()
        activeOnlineJob = viewModelScope.launch(safeCoroutineHandler) {
            stopToolPreview()
            player.pause()
            _uiState.update {
                it.copy(
                    isBusy = true,
                    isVoiceAiConverting = true,
                    isOnlineTaskRunning = true,
                    onlineTaskLabel = "AI Voice-to-Voice",
                    voiceAiStatusNote = null,
                )
            }
            try {
                when (
                    val res = SonixUnifiedAIEngine.convertVoiceToVoice(
                        context = getApplication(),
                        sourceBuffer = src,
                        sourceFile = srcFile,
                        targetPreset = preset,
                        targetLabel = targetLabel,
                        pitchSemitones = pitchSemitones,
                        formantShift = formantShift,
                        warmthDb = warmthDb,
                        presenceDb = presenceDb,
                        language = lang,
                        targetPersona = targetPersona,
                        acousticMode = acousticMode,
                    )
                ) {
                    is SonixUnifiedAIEngine.UnifiedSpeechResult.Success -> {
                        setActiveBuffer(res.buffer, recordUndo = src != null)
                        _uiState.update {
                            it.copy(voiceAiStatusNote = res.engineSummaryAr)
                        }
                        postBanner("تم تحويل الشخصية الصوتية بالذكاء الاصطناعي ($targetLabel) بنجاح")
                    }
                    is SonixUnifiedAIEngine.UnifiedSpeechResult.Offline -> {
                        _uiState.update { it.copy(voiceAiStatusNote = res.message) }
                        postBanner(res.message)
                    }
                    is SonixUnifiedAIEngine.UnifiedSpeechResult.Error -> {
                        _uiState.update { it.copy(voiceAiStatusNote = res.message) }
                        postBanner(res.message)
                    }
                }
            } finally {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        isVoiceAiConverting = false,
                        isOnlineTaskRunning = false,
                        onlineTaskLabel = "",
                    )
                }
            }
        }
    }

    fun previewVoiceWithAi(
        preset: VoiceCharacterPreset?,
        targetLabel: String,
        pitchSemitones: Float,
        formantShift: Float,
        warmthDb: Float,
        presenceDb: Float,
        targetPersona: TtsVoicePersona? = null,
        acousticMode: TtsPersonaAcousticMode = targetPersona?.defaultAcousticMode ?: TtsPersonaAcousticMode.NATURAL,
    ) {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner(t(_uiState.value.language, "no_audio_loaded"))
            return
        }
        val lang = _uiState.value.language
        toggleToolBufferPreview("AiVoiceToVoicePreview") {
            when (
                val res = SonixUnifiedAIEngine.convertVoiceToVoice(
                    context = getApplication(),
                    sourceBuffer = src,
                    sourceFile = srcFile,
                    targetPreset = preset,
                    targetLabel = targetLabel,
                    pitchSemitones = pitchSemitones,
                    formantShift = formantShift,
                    warmthDb = warmthDb,
                    presenceDb = presenceDb,
                    language = lang,
                    targetPersona = targetPersona,
                    acousticMode = acousticMode,
                )
            ) {
                is SonixUnifiedAIEngine.UnifiedSpeechResult.Success -> {
                    _uiState.update { it.copy(voiceAiStatusNote = res.engineSummaryAr) }
                    res.buffer
                }
                is SonixUnifiedAIEngine.UnifiedSpeechResult.Offline -> {
                    _uiState.update { it.copy(voiceAiStatusNote = res.message) }
                    postBanner(res.message)
                    null
                }
                is SonixUnifiedAIEngine.UnifiedSpeechResult.Error -> {
                    _uiState.update { it.copy(voiceAiStatusNote = res.message) }
                    postBanner(res.message)
                    null
                }
            }
        }
    }

    // Native Android SpeechRecognizer + Sherpa-ONNX Moonshine AR Studio
    fun isSpeechRecognizerAvailable(): Boolean = speechTranscriber.isAvailable()

    fun isLocalSpeechModelReady(): Boolean = speechTranscriber.isLocalArabicModelReady()

    fun setSpeechBackendMode(mode: SttBackendMode) {
        speechTranscriber.setBackendMode(mode)
    }

    fun downloadLocalSpeechModel() {
        viewModelScope.launch(safeCoroutineHandler) {
            val ok = speechTranscriber.downloadLocalArabicModel()
            if (ok) {
                postBanner("تم تحميل نموذج Moonshine العربي بنجاح — جاهز للتفريغ المستمر أوفلاين")
            }
        }
    }

    fun cancelLocalSpeechDownload() {
        speechTranscriber.cancelLocalModelDownload()
    }

    fun clearLocalSpeechModel() {
        speechTranscriber.clearLocalArabicModel()
        postBanner("تم حذف ملفات نموذج الكلام المحلي لتحرير المساحة")
    }

    fun setSpeechLocale(localeTag: String) {
        speechTranscriber.setLocale(localeTag)
    }

    fun updateSpeechTranscript(text: String) {
        speechTranscriber.setEditedText(text)
    }

    fun canTranscribeClipOffline(): Boolean =
        speechTranscriber.canTranscribeClipOffline(_uiState.value.speechState.localeTag)

    fun startLiveSpeechDictation() {
        player.pause()
        stopMixPreviewPlayer()
        val loc = _uiState.value.speechState.localeTag
        speechTranscriber.startListening(localeTag = loc, isClipRouting = false)
    }

    fun startStudioClipSpeechTranscription() {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner(t(_uiState.value.language, "no_audio_loaded"))
            return
        }
        stopMixPreviewPlayer()
        val loc = _uiState.value.speechState.localeTag
        if (speechTranscriber.canTranscribeClipOffline(loc)) {
            if (_uiState.value.playback.isPlaying) {
                player.pause()
            }
            val startedOffline = when {
                src != null -> speechTranscriber.transcribeBufferOffline(src)
                srcFile != null -> speechTranscriber.transcribeAudioFileOffline(srcFile)
                else -> false
            }
            if (startedOffline) return
        }
        player.seekToFraction(0f)
        player.play()
        speechTranscriber.startListening(localeTag = loc, isClipRouting = true)
    }

    fun stopSpeechTranscription() {
        val wasClipRouting = speechTranscriber.state.value.isRoutingClip
        speechTranscriber.stop()
        if (wasClipRouting && _uiState.value.playback.isPlaying) {
            player.pause()
        }
    }

    fun clearSpeechTranscription() {
        speechTranscriber.clear()
    }

    // Live Zero-Latency Microphone VoiceProcessor
    fun startLiveVoiceProcessor(effect: VoiceEffect) {
        if (!recorder.hasMicPermission()) {
            setMicPermissionDeniedError()
            return
        }
        player.pause()
        stopMixPreviewPlayer()
        stopOnlineSoundPreview()
        voiceProcessor.effect = effect
        voiceProcessor.start(viewModelScope)
        _uiState.update {
            it.copy(
                micPermissionError = null,
                isLiveVoiceChangerActive = true,
                liveVoiceEffect = effect,
            )
        }
    }

    fun setLiveVoiceEffect(effect: VoiceEffect) {
        voiceProcessor.effect = effect
        _uiState.update { it.copy(liveVoiceEffect = effect) }
    }

    fun stopLiveVoiceProcessor() {
        voiceProcessor.stop()
        _uiState.update { it.copy(isLiveVoiceChangerActive = false) }
    }

    // Web Article Extractor for TTS Podcast via https://jina.ai (100% Online only)
    fun extractArticleTextForTts(url: String, onExtracted: (String) -> Unit) {
        if (url.isBlank()) return
        val lang = _uiState.value.language
        val offlineErr = OnlineGuard.requireOnlineOrError(getApplication(), lang)
        if (offlineErr != null) {
            _uiState.update {
                it.copy(
                    ttsUiState = it.ttsUiState.copy(
                        isOffline = true,
                        errorMessage = offlineErr,
                    ),
                )
            }
            postBanner(offlineErr)
            return
        }
        activeOnlineJob?.cancel()
        activeOnlineJob = viewModelScope.launch(safeCoroutineHandler) {
            _uiState.update {
                it.copy(
                    isExtractingArticle = true,
                    isOnlineTaskRunning = true,
                    onlineTaskLabel = "Web Article Extractor",
                )
            }
            try {
                when (val res = WebArticleExtractor.extractCleanTextFromUrl(getApplication(), url, lang)) {
                    is ArticleExtractionResult.Success -> {
                        onExtracted(res.text)
                        _uiState.update {
                            it.copy(
                                ttsUiState = it.ttsUiState.copy(
                                    isOffline = false,
                                    errorMessage = null,
                                ),
                            )
                        }
                        postBanner("تم استخراج النص الصافي من المقال بنجاح (${res.text.length} حرف)")
                    }
                    is ArticleExtractionResult.Offline -> {
                        _uiState.update {
                            it.copy(
                                ttsUiState = it.ttsUiState.copy(
                                    isOffline = true,
                                    errorMessage = res.message,
                                ),
                            )
                        }
                        postBanner(res.message)
                    }
                    ArticleExtractionResult.EmptyOrFailed -> {
                        postBanner(t(lang, "tts_web_article_empty"))
                    }
                }
            } finally {
                _uiState.update {
                    it.copy(
                        isExtractingArticle = false,
                        isOnlineTaskRunning = false,
                        onlineTaskLabel = "",
                    )
                }
            }
        }
    }

    // Smart Tool Guide (Pollinations AI Online) & Archive.org Online Sound Search
    fun searchToolGuide(query: String) {
        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    assistantGuideResults = emptyList(),
                    assistantAiSummary = null,
                    assistantGuideOfflineError = null,
                )
            }
            return
        }
        val quickLocalMatches = SmartAssistantAndSearch.findBestToolWithTypoTolerance(query)
        _uiState.update {
            it.copy(
                assistantGuideResults = quickLocalMatches,
                assistantGuideOfflineError = null,
            )
        }
    }

    fun sendAssistantChatMessage(userText: String) {
        val clean = userText.trim()
        if (clean.isEmpty()) return
        val lang = _uiState.value.language
        val offlineMsg = OnlineGuard.requireOnlineOrError(getApplication(), lang)
        if (offlineMsg != null) {
            _uiState.update {
                it.copy(
                    isLoadingAiGuide = false,
                    assistantGuideOfflineError = offlineMsg,
                )
            }
            postBanner(offlineMsg)
            return
        }
        val nowMs = System.currentTimeMillis()
        val sessionId = _uiState.value.activeAssistantChatId ?: "chat_${UUID.randomUUID()}"
        val userMsg = AssistantChatMessage(
            id = "u_${UUID.randomUUID()}",
            isUser = true,
            text = clean,
            timestampMs = nowMs,
        )
        val historyBefore = _uiState.value.assistantChatMessages
        val sessionCreatedAt = historyBefore.firstOrNull()?.timestampMs?.takeIf { it > 0L } ?: nowMs
        val sessionTitle = historyBefore.firstOrNull { it.isUser }?.text?.take(48) ?: clean.take(48)

        _uiState.update {
            it.copy(
                isLoadingAiGuide = true,
                isOnlineTaskRunning = true,
                onlineTaskLabel = "AI Assistant",
                assistantGuideOfflineError = null,
                activeAssistantChatId = sessionId,
                assistantChatMessages = historyBefore + userMsg,
            )
        }
        activeOnlineJob?.cancel()
        activeOnlineJob = viewModelScope.launch(safeCoroutineHandler) {
            try {
                val turn = SmartAssistantAndSearch.askPollinationsChat(
                    history = historyBefore,
                    userMessage = clean,
                    language = lang,
                    context = getApplication(),
                )
                if (turn.isOfflineError) {
                    _uiState.update {
                        it.copy(
                            assistantGuideOfflineError = turn.replyText,
                            assistantChatMessages = historyBefore,
                        )
                    }
                    postBanner(turn.replyText)
                    return@launch
                }
                val botMsg = AssistantChatMessage(
                    id = "a_${UUID.randomUUID()}",
                    isUser = false,
                    text = turn.replyText,
                    recommendedToolId = turn.recommendedToolId,
                    recommendedToolLabel = turn.recommendedToolLabel,
                    timestampMs = System.currentTimeMillis(),
                )
                val finalMessages = _uiState.value.assistantChatMessages + botMsg
                _uiState.update {
                    it.copy(
                        assistantAiSummary = turn.replyText,
                        assistantChatMessages = finalMessages,
                        assistantGuideOfflineError = null,
                    )
                }
                val savedSession = SavedAssistantChatSession(
                    id = sessionId,
                    title = sessionTitle,
                    createdAtMs = sessionCreatedAt,
                    updatedAtMs = System.currentTimeMillis(),
                    messages = finalMessages,
                )
                repository.upsertAssistantChatSession(savedSession)
            } finally {
                _uiState.update {
                    it.copy(
                        isLoadingAiGuide = false,
                        isOnlineTaskRunning = false,
                        onlineTaskLabel = "",
                    )
                }
            }
        }
    }

    fun clearAssistantChat() {
        startNewAssistantChat()
    }

    fun startNewAssistantChat() {
        _uiState.update {
            it.copy(
                activeAssistantChatId = null,
                assistantChatMessages = emptyList(),
                assistantAiSummary = null,
                assistantGuideResults = emptyList(),
                assistantGuideOfflineError = null,
            )
        }
    }

    fun openSavedAssistantChat(sessionId: String) {
        val target = _uiState.value.savedAssistantChats.firstOrNull { it.id == sessionId } ?: return
        _uiState.update {
            it.copy(
                activeAssistantChatId = target.id,
                assistantChatMessages = target.messages,
                assistantAiSummary = target.messages.lastOrNull { m -> !m.isUser }?.text,
                assistantGuideOfflineError = null,
            )
        }
    }

    fun deleteSavedAssistantChat(sessionId: String) {
        viewModelScope.launch(safeCoroutineHandler) {
            repository.deleteAssistantChatSession(sessionId)
            if (_uiState.value.activeAssistantChatId == sessionId) {
                startNewAssistantChat()
            }
        }
    }

    fun formatAssistantChatForCopy(
        messages: List<AssistantChatMessage> = _uiState.value.assistantChatMessages,
    ): String {
        val lang = _uiState.value.language
        val userTag = if (lang == AppLanguage.EN) "You" else if (lang == AppLanguage.FA) "شما" else "أنت"
        val aiTag = if (lang == AppLanguage.EN) "Sonix AI" else "Sonix ✦ AI"
        return messages.joinToString("\n\n") { msg ->
            val speaker = if (msg.isUser) userTag else aiTag
            "$speaker:\n${msg.text}"
        }
    }

    fun toggleOnlineSoundFavorite(item: OnlineSoundItem) {
        viewModelScope.launch(safeCoroutineHandler) {
            val added = repository.toggleFavoriteOnlineSound(item)
            val msg = if (_uiState.value.language == AppLanguage.EN) {
                if (added) "Added to favorites" else "Removed from favorites"
            } else {
                if (added) "تمت الإضافة إلى المفضلة ❤️" else "تمت الإزالة من المفضلة"
            }
            postBanner(msg)
        }
    }

    fun clearOnlineListeningHistory() {
        viewModelScope.launch(safeCoroutineHandler) {
            repository.clearOnlineListeningHistory()
        }
    }

    fun askAiToolGuide(query: String) {
        sendAssistantChatMessage(query)
    }

    fun setOnlineSearchCategory(category: OnlineSearchCategory) {
        if (_uiState.value.onlineSearchCategory != category) {
            _uiState.update {
                it.copy(
                    onlineSearchCategory = category,
                    onlineSoundCategories = emptyMap(),
                    soundSearchOfflineError = null,
                )
            }
        }
    }

    fun searchInternetSounds(query: String) {
        val lang = _uiState.value.language
        val offlineMsg = OnlineGuard.requireOnlineOrError(getApplication(), lang)
        if (offlineMsg != null) {
            _uiState.update {
                it.copy(
                    isSearchingOnlineSounds = false,
                    soundSearchOfflineError = offlineMsg,
                )
            }
            postBanner(offlineMsg)
            return
        }
        val cat = _uiState.value.onlineSearchCategory
        activeOnlineJob?.cancel()
        activeOnlineJob = viewModelScope.launch(safeCoroutineHandler) {
            _uiState.update {
                it.copy(
                    isSearchingOnlineSounds = true,
                    isOnlineTaskRunning = true,
                    onlineTaskLabel = "Sound Search",
                    soundSearchOfflineError = null,
                )
            }
            try {
                if (!NetworkImmunityManager.pingReachable(getApplication())) {
                    val pingOffline = OnlineGuard.offlineErrorMessage(lang)
                    _uiState.update { it.copy(soundSearchOfflineError = pingOffline) }
                    postBanner(pingOffline)
                    return@launch
                }
                val grouped = SmartAssistantAndSearch.searchOnlineCatalog(
                    query = query,
                    category = cat,
                    context = getApplication(),
                )
                _uiState.update { it.copy(onlineSoundCategories = grouped) }
                if (grouped.isEmpty()) {
                    postBanner("لم يتم العثور على نتائج حالياً — جرب كلمة بحث أخرى")
                }
            } finally {
                _uiState.update {
                    it.copy(
                        isSearchingOnlineSounds = false,
                        isOnlineTaskRunning = false,
                        onlineTaskLabel = "",
                    )
                }
            }
        }
    }

    fun toggleOnlineSoundPreview(item: OnlineSoundItem) {
        if (_uiState.value.previewingOnlineUrl == item.streamUrl) {
            stopOnlineSoundPreview()
            return
        }
        val lang = _uiState.value.language
        val offlineMsg = OnlineGuard.requireOnlineOrError(getApplication(), lang)
        if (offlineMsg != null) {
            _uiState.update { it.copy(soundSearchOfflineError = offlineMsg) }
            postBanner(offlineMsg)
            return
        }
        stopOnlineSoundPreview()
        stopToolPreview()
        player.pause()
        stopMixPreviewPlayer()
        _uiState.update { it.copy(previewingOnlineUrl = item.streamUrl, soundSearchOfflineError = null) }
        viewModelScope.launch(safeCoroutineHandler) {
            repository.recordOnlineSoundListen(item)
            val outcome = NetworkImmunityManager.fetchAudioWithFallback(
                urls = listOf(item.streamUrl),
                context = getApplication(),
                timeoutMs = NetworkImmunityManager.DEFAULT_NETWORK_TIMEOUT_MS,
                maxBytes = 25 * 1024 * 1024,
                providerLabel = "OnlineSoundPreview",
            )
            val response = (outcome as? NetworkFetchOutcome.Success)?.value
            if (response == null) {
                if (outcome is NetworkFetchOutcome.Offline) {
                    val offlineMessage = OnlineGuard.offlineErrorMessage(lang)
                    _uiState.update { it.copy(soundSearchOfflineError = offlineMessage) }
                    postBanner(offlineMessage)
                } else {
                    postBanner("تعذر تنزيل المعاينة الصوتية من الإنترنت")
                }
                stopOnlineSoundPreview()
                return@launch
            }

            if (_uiState.value.previewingOnlineUrl != item.streamUrl) return@launch

            val extension = item.streamUrl
                .substringBefore('?')
                .substringAfterLast('.', "")
                .lowercase(Locale.ROOT)
                .takeIf { it in setOf("mp3", "wav", "m4a", "aac", "ogg", "flac") }
                ?: "m4a"
            val previewFile = File(
                getApplication<Application>().cacheDir,
                "online_preview_${UUID.randomUUID()}.$extension",
            )
            try {
                withContext(Dispatchers.IO) {
                    previewFile.outputStream().use { it.write(response.bytes) }
                }
            } catch (e: CancellationException) {
                runCatching { previewFile.delete() }
                throw e
            } catch (_: Exception) {
                runCatching { previewFile.delete() }
                stopOnlineSoundPreview()
                postBanner("تعذر حفظ ملف المعاينة المؤقت")
                return@launch
            }

            onlinePreviewFile = previewFile
            if (!startGlobalFilePreview(previewFile, deleteFileOnStop = true)) {
                runCatching { previewFile.delete() }
                onlinePreviewFile = null
                stopOnlineSoundPreview()
                postBanner("تعذر تشغيل المعاينة من الإنترنت")
            }
        }
    }

    fun stopOnlineSoundPreview() {
        val file = onlinePreviewFile
        if (file != null && toolPreviewPlaybackFile?.absolutePath == file.absolutePath) {
            stopToolPreview()
        } else {
            runCatching { file?.delete() }
            onlinePreviewFile = null
        }
        _uiState.update { it.copy(previewingOnlineUrl = null) }
    }

    fun importOnlineSoundToStudio(item: OnlineSoundItem) {
        val lang = _uiState.value.language
        val offlineMsg = OnlineGuard.requireOnlineOrError(getApplication(), lang)
        if (offlineMsg != null) {
            _uiState.update { it.copy(soundSearchOfflineError = offlineMsg) }
            postBanner(offlineMsg)
            return
        }
        activeOnlineJob?.cancel()
        activeOnlineJob = viewModelScope.launch(safeCoroutineHandler) {
            stopOnlineSoundPreview()
            _uiState.update {
                it.copy(
                    isBusy = true,
                    isOnlineTaskRunning = true,
                    onlineTaskLabel = "Archive Audio Download",
                    loadingOnlineSoundId = item.identifier,
                    soundSearchOfflineError = null,
                )
            }
            try {
                if (!NetworkImmunityManager.pingReachable(getApplication())) {
                    val pingOffline = OnlineGuard.offlineErrorMessage(lang)
                    _uiState.update { it.copy(soundSearchOfflineError = pingOffline) }
                    postBanner(pingOffline)
                    return@launch
                }
                val buf = SmartAssistantAndSearch.downloadSoundToStudioBuffer(getApplication(), item)
                if (buf == null || buf.frameCount == 0) {
                    postBanner("تعذر تحميل الصوت من Archive.org — حاول مقطعاً آخر")
                    return@launch
                }
                setActiveBuffer(buf, recordUndo = false, resetDspOnNewEntry = true)
                _uiState.update { it.copy(activeProjectName = item.title.take(40)) }
                repository.indexAudioRecording(item.title.take(40), buf, RecordingSourceType.IMPORTED)
                repository.recordOnlineSoundListen(item)
                postBanner("تمت إضافة \"${item.title.take(28)}\" بالصوت النقي إلى الاستوديو")
            } finally {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        isOnlineTaskRunning = false,
                        onlineTaskLabel = "",
                        loadingOnlineSoundId = null,
                    )
                }
            }
        }
    }

    fun cancelActiveOnlineRequest() {
        activeOnlineJob?.cancel()
        activeOnlineJob = null
        stopOnlineSoundPreview()
        releaseTtsPreviewPlayer()
        _uiState.update {
            it.copy(
                isBusy = false,
                isOnlineTaskRunning = false,
                onlineTaskLabel = "",
                isExtractingArticle = false,
                isLoadingAiGuide = false,
                isSearchingOnlineSounds = false,
                loadingOnlineSoundId = null,
                updateCheckLoading = false,
                isVoiceAiConverting = false,
                voiceAiProgress = null,
                transcriptionState = it.transcriptionState.copy(isLoading = false),
                searchAssistantState = it.searchAssistantState.copy(isLoading = false),
                ttsUiState = it.ttsUiState.copy(
                    isSynthesizingPreview = false,
                    isPreviewing = false,
                    isGenerating = false,
                ),
            )
        }
        postBanner(t(_uiState.value.language, "cancelling_request"))
    }

    fun toggleSliderLock(lockKey: String) {
        _uiState.update { cur ->
            val updated = if (lockKey in cur.lockedSliders) cur.lockedSliders - lockKey else cur.lockedSliders + lockKey
            cur.copy(lockedSliders = updated)
        }
    }

    fun cycleMonitoringSpeed() {
        val speeds = listOf(0.5f, 1.0f, 1.5f, 2.0f)
        val current = _uiState.value.monitoringSpeed
        val idx = speeds.indexOfFirst { abs(it - current) < 0.05f }
        val next = speeds[(idx + 1) % speeds.size]
        setMonitoringSpeed(next)
    }

    fun setMonitoringSpeed(speed: Float) {
        val clamped = clamp(speed, 0.5f, 2.0f)
        player.setMonitoringSpeed(clamped)
        _uiState.update { it.copy(monitoringSpeed = clamped) }
    }

    fun addMarkerAtCurrentPosition() {
        val dur = _uiState.value.durationMs
        if (!_uiState.value.hasAudio || dur <= 0L) {
            postBanner(t(_uiState.value.language, "no_audio_loaded"))
            return
        }
        val posMs = _playbackTelemetry.value.positionMs.coerceIn(0L, dur)
        val frac = (posMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
        val existing = _uiState.value.audioMarkers
        val marker = AudioMarker(
            id = UUID.randomUUID().toString().take(8),
            positionFrac = frac,
            label = "#${existing.size + 1} (${formatDurationMs(posMs)})",
            colorIndex = existing.size % 4,
        )
        _uiState.update { cur ->
            cur.copy(audioMarkers = (cur.audioMarkers + marker).sortedBy { it.positionFrac })
        }
    }

    fun jumpToMarker(marker: AudioMarker) {
        if (!_uiState.value.hasAudio) return
        seekToFraction(marker.positionFrac)
    }

    fun selectRangeFromMarkers(first: AudioMarker, second: AudioMarker) {
        val a = minOf(first.positionFrac, second.positionFrac).coerceIn(0f, 0.99f)
        val b = maxOf(first.positionFrac, second.positionFrac).coerceIn(0.01f, 1f)
        if (b - a < 0.01f) return
        updateTrimRange(a, b)
    }

    fun removeMarker(id: String) {
        _uiState.update { cur ->
            cur.copy(audioMarkers = cur.audioMarkers.filterNot { it.id == id })
        }
    }

    fun previewAutoSilenceRemover(thresholdDb: Float, minSilenceSec: Float) {
        val src = activeBuffer ?: run {
            postBanner(t(_uiState.value.language, "no_audio_loaded"))
            return
        }
        toggleToolBufferPreview("AutoSilenceRemover") {
            removeSilenceAuto(getApplication(), src, thresholdDb, minSilenceSec)
        }
    }

    fun applyAutoSilenceRemover(thresholdDb: Float, minSilenceSec: Float) {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner(t(_uiState.value.language, "no_audio_loaded"))
            return
        }
        stopToolPreview()
        launchSingleFlightProcessing {
            if (src != null) {
                val res = removeSilenceAuto(getApplication(), src, thresholdDb, minSilenceSec)
                setActiveBuffer(res, recordUndo = true)
            } else if (srcFile != null) {
                val sileroOut = SileroVadProcessor.removeSilenceFileWithSileroVad(
                    context = getApplication(),
                    inputFile = srcFile,
                    thresholdDb = thresholdDb,
                    minSilenceSec = minSilenceSec,
                )
                val thDb = String.format(Locale.US, "%.1f", thresholdDb.coerceIn(-65f, -12f))
                val minD = String.format(Locale.US, "%.2f", minSilenceSec.coerceIn(0.1f, 5.0f))
                val outFile = sileroOut ?: processAudioFileWithFfmpeg(
                    context = getApplication(),
                    inputFile = srcFile,
                    audioFilter = "silenceremove=stop_periods=-1:stop_duration=$minD:stop_threshold=${thDb}dB",
                    toolName = "AutoSilenceRemoverLarge",
                ) ?: run {
                    postBanner("تعذر إزالة السكتات من الملف الكبير")
                    return@launchSingleFlightProcessing
                }
                applyProcessedDiskSourceFile(outFile, appliedToolLabel = "AutoSilenceRemover")
            }
            _uiState.update { it.copy(lastAppliedToolLabel = "AutoSilenceRemover") }
            postBanner("تم إزالة السكتات الصامتة الطويلة بنجاح")
        }
    }

    fun previewAutoCompressorLimiter(strengthPct: Float) {
        val src = activeBuffer ?: run {
            postBanner(t(_uiState.value.language, "no_audio_loaded"))
            return
        }
        toggleToolBufferPreview("AutoCompressorLimiter") {
            applyAutoCompressorLimiter(getApplication(), src, strengthPct)
        }
    }

    fun applyAutoCompressorLimiter(strengthPct: Float) {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner(t(_uiState.value.language, "no_audio_loaded"))
            return
        }
        stopToolPreview()
        launchSingleFlightProcessing {
            if (src != null) {
                val res = applyAutoCompressorLimiter(getApplication(), src, strengthPct)
                setActiveBuffer(res, recordUndo = true)
            } else if (srcFile != null) {
                val normS = (strengthPct / 100f).coerceIn(0f, 1f)
                val ratio = String.format(Locale.US, "%.1f", (2f + normS * 4.5f).coerceIn(1.5f, 8f))
                val thresh = String.format(Locale.US, "%.1f", (-16f - normS * 10f))
                val makeup = String.format(Locale.US, "%.1f", (1.5f + normS * 4.5f).coerceIn(0.5f, 6f))
                val dynaudStage = if (normS >= 0.45f) "dynaudnorm=f=150:g=15:p=0.90:m=8," else ""
                val loudnormStage = if (normS >= 0.60f) "loudnorm=I=-16:TP=-1.5:LRA=11," else ""
                val outFile = processAudioFileWithFfmpeg(
                    context = getApplication(),
                    inputFile = srcFile,
                    audioFilter = "${dynaudStage}acompressor=threshold=${thresh}dB:ratio=$ratio:attack=8:release=95:makeup=$makeup,${loudnormStage}alimiter=limit=0.945:attack=5:release=50",
                    toolName = "AutoCompressorLimiterLarge",
                ) ?: run {
                    postBanner("تعذر موازنة الملف الصوتي الكبير")
                    return@launchSingleFlightProcessing
                }
                applyProcessedDiskSourceFile(outFile, appliedToolLabel = "AutoCompressorLimiter")
            }
            _uiState.update { it.copy(lastAppliedToolLabel = "AutoCompressorLimiter") }
            postBanner("تمت موازنة الصوت وحماية القمم (Compressor & Limiter)")
        }
    }

    fun previewVocalSplitter(mode: VocalSplitMode, blendRatio: Float) {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src != null) {
            toggleToolBufferPreview("VocalSplitter") {
                splitVocalAndInstrumental(src, mode, blendRatio)
            }
            return
        }
        if (srcFile == null || !srcFile.exists()) {
            postBanner(t(_uiState.value.language, "no_audio_loaded"))
            return
        }
        if (_uiState.value.isToolPreviewPlaying && hasGlobalToolPreview()) {
            stopToolPreview()
            return
        }
        launchSingleFlightProcessing {
            stopToolPreview()
            player.pause()
            val outFile = splitVocalAndInstrumentalFile(getApplication(), srcFile, mode, blendRatio)
            if (outFile == null || !outFile.exists()) {
                postBanner("تعذر توليد معاينة فصل الصوت للملف الكبير")
                return@launchSingleFlightProcessing
            }
            val started = startGlobalFilePreview(outFile, deleteFileOnStop = true)
            if (!started) {
                stopToolPreview()
                postBanner("تعذر تشغيل معاينة فصل الصوت للملف الكبير")
            }
        }
    }

    fun applyVocalSplitter(mode: VocalSplitMode, blendRatio: Float) {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner(t(_uiState.value.language, "no_audio_loaded"))
            return
        }
        stopToolPreview()
        launchSingleFlightProcessing {
            if (src != null) {
                val res = splitVocalAndInstrumental(src, mode, blendRatio)
                setActiveBuffer(res, recordUndo = true)
            } else if (srcFile != null) {
                val outFile = splitVocalAndInstrumentalFile(getApplication(), srcFile, mode, blendRatio) ?: run {
                    postBanner("تعذر فصل الصوت عن الموسيقى في الملف الكبير")
                    return@launchSingleFlightProcessing
                }
                applyProcessedDiskSourceFile(outFile, appliedToolLabel = "VocalSplitter")
            }
            _uiState.update { it.copy(lastAppliedToolLabel = "VocalSplitter") }
            postBanner("تم تطبيق فصل الصوت البشري عن الموسيقى")
        }
    }

    fun applySmartAutoFadeOut(durationSec: Float) {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner(t(_uiState.value.language, "no_audio_loaded"))
            return
        }
        val durSec = (_uiState.value.durationMs / 1000.0).coerceAtLeast(0.1)
        launchSingleFlightProcessing {
            if (src != null) {
                val res = applyOptionalSmartAutoFadeOut(src, durationSec)
                setActiveBuffer(res, recordUndo = true)
            } else if (srcFile != null) {
                val fadeLen = durationSec.toDouble().coerceIn(0.05, durSec)
                val startAt = String.format(Locale.US, "%.3f", (durSec - fadeLen).coerceAtLeast(0.0))
                val lenStr = String.format(Locale.US, "%.2f", fadeLen)
                val outFile = processAudioFileWithFfmpeg(
                    context = getApplication(),
                    inputFile = srcFile,
                    audioFilter = "afade=t=out:st=$startAt:d=$lenStr:curve=exp",
                    toolName = "SmartAutoFadeLarge",
                ) ?: run {
                    postBanner("تعذر تطبيق التلاشي الذكي")
                    return@launchSingleFlightProcessing
                }
                applyProcessedDiskSourceFile(outFile, appliedToolLabel = "SmartAutoFade")
            }
            _uiState.update { it.copy(lastAppliedToolLabel = "SmartAutoFade") }
            postBanner("تم تطبيق التلاشي الذكي على نهاية الكلام (${"%.1f".format(durationSec)}s)")
        }
    }

    fun applyOneTapSocialPreset(preset: OneTapSocialPreset) {
        val src = activeBuffer
        val srcFile = activeSourceFile
        if (src == null && (srcFile == null || !srcFile.exists())) {
            postBanner(t(_uiState.value.language, "no_audio_loaded"))
            return
        }
        stopToolPreview()
        launchSingleFlightProcessing {
            val title = t(_uiState.value.language, preset.titleKey)
            if (src != null) {
                val res = applyOneTapSocialPreset(getApplication(), src, preset)
                setActiveBuffer(res, recordUndo = true)
            } else if (srcFile != null) {
                val outFile = processAudioFileWithFfmpeg(
                    context = getApplication(),
                    inputFile = srcFile,
                    audioFilter = "acompressor=threshold=-19dB:ratio=3:attack=15:release=160,loudnorm=I=-14:TP=-1.0:LRA=9",
                    toolName = "OneTapSocialPresetLarge",
                ) ?: run {
                    postBanner("تعذر تطبيق القالب على الملف الكبير")
                    return@launchSingleFlightProcessing
                }
                applyProcessedDiskSourceFile(outFile, appliedToolLabel = title)
            }
            _uiState.update {
                it.copy(
                    selectedPresetLabel = title,
                    lastAppliedToolLabel = title,
                )
            }
            postBanner("تم تطبيق قالب «$title» بلمسة واحدة")
        }
    }

    fun checkForAppUpdate() {
        val lang = _uiState.value.language
        val offlineMsg = OnlineGuard.requireOnlineOrError(getApplication(), lang)
        if (offlineMsg != null) {
            _uiState.update { it.copy(updateCheckLoading = false, updateCheckError = offlineMsg) }
            postBanner(offlineMsg)
            return
        }
        activeOnlineJob?.cancel()
        activeOnlineJob = viewModelScope.launch(safeCoroutineHandler) {
            _uiState.update {
                it.copy(
                    updateCheckLoading = true,
                    isOnlineTaskRunning = true,
                    onlineTaskLabel = "App Update Check",
                    updateCheckError = null,
                )
            }
            try {
                when (val outcome = AppUpdateManager.checkLatestRelease(getApplication())) {
                    is AppUpdateOutcome.Checked -> {
                        _uiState.update {
                            it.copy(
                                updateCheckResult = outcome.info,
                                updateCheckError = null,
                            )
                        }
                    }
                    is AppUpdateOutcome.Offline -> {
                        _uiState.update { it.copy(updateCheckError = outcome.message) }
                        postBanner(outcome.message)
                    }
                    is AppUpdateOutcome.Error -> {
                        _uiState.update { it.copy(updateCheckError = outcome.message) }
                    }
                }
            } finally {
                _uiState.update {
                    it.copy(
                        updateCheckLoading = false,
                        isOnlineTaskRunning = false,
                        onlineTaskLabel = "",
                    )
                }
            }
        }
    }

    fun openAppUpdateLink() {
        val info = _uiState.value.updateCheckResult ?: return
        AppUpdateManager.openUpdateLinkInBrowser(getApplication(), info)
    }

    // Batch Processing Queue (Multi-file DSP: Pitch, Speed, Filter, Normalize & Export)
    fun addBatchUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val app = getApplication<Application>()
        viewModelScope.launch(safeCoroutineHandler) {
            val existingKeys = _uiState.value.batchQueueState.items.map { it.sourceUriString }.toMutableSet()
            val added = mutableListOf<BatchQueueItem>()
            for (uri in uris) {
                val uriStr = uri.toString()
                if (!existingKeys.add(uriStr)) continue
                withContext(Dispatchers.IO) {
                    runCatching {
                        app.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    }
                }
                val name = withContext(Dispatchers.IO) {
                    AudioFileUploader.resolveDisplayName(app, uri)
                }
                added += BatchQueueItem(
                    id = "batch_${UUID.randomUUID().toString().take(8)}",
                    displayName = name,
                    sourceUriString = uriStr,
                    outputFormat = _uiState.value.batchQueueState.settings.outputFormat,
                )
            }
            if (added.isNotEmpty()) {
                _uiState.update { cur ->
                    val merged = cur.batchQueueState.items + added
                    cur.copy(
                        batchQueueState = cur.batchQueueState.copy(
                            items = merged,
                            completedCount = merged.count { it.status == BatchItemStatus.COMPLETED },
                            failedCount = merged.count { it.status == BatchItemStatus.FAILED },
                        ),
                    )
                }
                val isEn = _uiState.value.language == AppLanguage.EN
                postBanner(
                    if (isEn) "Added ${added.size} file(s) to batch queue"
                    else "تمت إضافة ${added.size} ملفات إلى قائمة المعالجة",
                )
            }
        }
    }

    fun addBatchDeviceAudios(items: List<DeviceAudioItem>) {
        if (items.isEmpty()) return
        _uiState.update { cur ->
            val existingKeys = cur.batchQueueState.items.map { it.sourceUriString }.toMutableSet()
            val added = items.mapNotNull { dev ->
                if (!existingKeys.add(dev.uriString)) null
                else BatchQueueItem(
                    id = "batch_${UUID.randomUUID().toString().take(8)}",
                    displayName = dev.displayName,
                    sourceUriString = dev.uriString,
                    durationMs = dev.durationMs,
                    outputFormat = cur.batchQueueState.settings.outputFormat,
                )
            }
            val merged = cur.batchQueueState.items + added
            cur.copy(
                batchQueueState = cur.batchQueueState.copy(
                    items = merged,
                    completedCount = merged.count { it.status == BatchItemStatus.COMPLETED },
                    failedCount = merged.count { it.status == BatchItemStatus.FAILED },
                ),
            )
        }
    }

    fun addBatchRecordings(items: List<AudioRecordingItem>) {
        if (items.isEmpty()) return
        viewModelScope.launch(safeCoroutineHandler) {
            val existingKeys = _uiState.value.batchQueueState.items.map { it.sourceUriString }.toMutableSet()
            val added = mutableListOf<BatchQueueItem>()
            for (rec in items) {
                val file = repository.getRecordingFile(rec.id) ?: continue
                val key = "recording://${rec.id}"
                if (!existingKeys.add(key)) continue
                added += BatchQueueItem(
                    id = "batch_${UUID.randomUUID().toString().take(8)}",
                    displayName = rec.title,
                    sourceUriString = key,
                    localFilePath = file.absolutePath,
                    durationMs = rec.durationMs,
                    outputFormat = _uiState.value.batchQueueState.settings.outputFormat,
                )
            }
            if (added.isNotEmpty()) {
                _uiState.update { cur ->
                    val merged = cur.batchQueueState.items + added
                    cur.copy(
                        batchQueueState = cur.batchQueueState.copy(
                            items = merged,
                            completedCount = merged.count { it.status == BatchItemStatus.COMPLETED },
                            failedCount = merged.count { it.status == BatchItemStatus.FAILED },
                        ),
                    )
                }
            }
        }
    }

    fun removeBatchQueueItem(itemId: String) {
        if (_uiState.value.batchQueueState.isRunning) return
        if (_uiState.value.batchQueueState.previewingItemId == itemId) {
            stopToolPreview()
        }
        _uiState.update { cur ->
            val target = cur.batchQueueState.items.firstOrNull { it.id == itemId }
            target?.outputFilePath?.let { path -> runCatching { File(path).delete() } }
            val filtered = cur.batchQueueState.items.filterNot { it.id == itemId }
            cur.copy(
                batchQueueState = cur.batchQueueState.copy(
                    items = filtered,
                    completedCount = filtered.count { it.status == BatchItemStatus.COMPLETED },
                    failedCount = filtered.count { it.status == BatchItemStatus.FAILED },
                    overallProgress = 0f,
                ),
            )
        }
    }

    fun clearBatchQueue(onlyCompleted: Boolean = false) {
        if (_uiState.value.batchQueueState.isRunning) return
        stopToolPreview()
        _uiState.update { cur ->
            val currentItems = cur.batchQueueState.items
            val toKeep = if (onlyCompleted) {
                currentItems.filter { it.status != BatchItemStatus.COMPLETED }
            } else {
                emptyList()
            }
            val keepIds = toKeep.map { it.id }.toSet()
            currentItems.filter { it.id !in keepIds }.forEach { removed ->
                removed.outputFilePath?.let { p -> runCatching { File(p).delete() } }
            }
            cur.copy(
                batchQueueState = cur.batchQueueState.copy(
                    items = toKeep,
                    completedCount = toKeep.count { it.status == BatchItemStatus.COMPLETED },
                    failedCount = toKeep.count { it.status == BatchItemStatus.FAILED },
                    overallProgress = 0f,
                    currentIndex = -1,
                    statusSummary = null,
                    previewingItemId = null,
                ),
            )
        }
    }

    fun updateBatchDspSettings(
        pitchSemitones: Float? = null,
        speedRatio: Float? = null,
        filterPreset: BatchFilterPreset? = null,
        normalizeOutput: Boolean? = null,
        outputFormat: ExportAudioFormat? = null,
    ) {
        if (_uiState.value.batchQueueState.isRunning) return
        _uiState.update { cur ->
            val old = cur.batchQueueState.settings
            val next = old.copy(
                pitchSemitones = (pitchSemitones ?: old.pitchSemitones).coerceIn(-12f, 12f),
                speedRatio = (speedRatio ?: old.speedRatio).coerceIn(0.5f, 2.5f),
                filterPreset = filterPreset ?: old.filterPreset,
                normalizeOutput = normalizeOutput ?: old.normalizeOutput,
                outputFormat = outputFormat ?: old.outputFormat,
            )
            cur.copy(batchQueueState = cur.batchQueueState.copy(settings = next))
        }
    }

    fun startBatchProcessingQueue() {
        val currentBatch = _uiState.value.batchQueueState
        if (currentBatch.isRunning || currentBatch.items.isEmpty()) return

        batchProcessingJob?.cancel()
        val settings = currentBatch.settings
        val activeDspSnapshot = _uiState.value.dspConfig
        val app = getApplication<Application>()
        val allCompletedAlready = currentBatch.items.all { it.status == BatchItemStatus.COMPLETED }

        val preparedItems = currentBatch.items.map { item ->
            if (allCompletedAlready || item.status != BatchItemStatus.COMPLETED) {
                item.copy(
                    status = BatchItemStatus.QUEUED,
                    itemProgress = 0f,
                    errorMessage = null,
                )
            } else {
                item
            }
        }

        _uiState.update { cur ->
            cur.copy(
                batchQueueState = cur.batchQueueState.copy(
                    items = preparedItems,
                    isRunning = true,
                    currentIndex = 0,
                    overallProgress = 0f,
                    completedCount = preparedItems.count { it.status == BatchItemStatus.COMPLETED },
                    failedCount = 0,
                    statusSummary = null,
                ),
            )
        }

        batchProcessingJob = viewModelScope.launch(safeCoroutineHandler) {
            val total = preparedItems.size.coerceAtLeast(1)
            try {
                for (index in preparedItems.indices) {
                    val snapshotList = _uiState.value.batchQueueState.items
                    val item = snapshotList.getOrNull(index) ?: continue
                    if (item.status == BatchItemStatus.COMPLETED) continue

                    _uiState.update { cur ->
                        val updated = cur.batchQueueState.items.mapIndexed { idx, entry ->
                            if (idx == index) entry.copy(status = BatchItemStatus.PROCESSING, itemProgress = 0.04f) else entry
                        }
                        cur.copy(
                            batchQueueState = cur.batchQueueState.copy(
                                items = updated,
                                currentIndex = index,
                            ),
                        )
                    }

                    var tempCopiedSource: File? = null
                    try {
                        val localPathFile = item.localFilePath?.let { File(it) }?.takeIf { it.exists() && it.length() > 0L }
                        val resolvedSource: File? = if (localPathFile != null) {
                            localPathFile
                        } else {
                            when (
                                val up = AudioFileUploader.copyAudioUriToCacheDetailed(app, Uri.parse(item.sourceUriString)) { info ->
                                    val frac = (info.fraction * 0.20f).coerceIn(0.04f, 0.20f)
                                    updateBatchItemProgress(index, frac, total)
                                }
                            ) {
                                is AudioUploadOutcome.Success -> {
                                    tempCopiedSource = up.uploaded.file
                                    up.uploaded.file
                                }
                                is AudioUploadOutcome.Failure -> {
                                    throw IllegalStateException(up.errorMessage)
                                }
                            }
                        }

                        if (resolvedSource == null || !resolvedSource.exists()) {
                            throw IllegalStateException("Missing audio source file")
                        }

                        val result = processBatchItemWithDsp(
                            context = app,
                            sourceFile = resolvedSource,
                            displayBaseName = "${item.displayName.substringBeforeLast('.')}_sonix",
                            settings = settings,
                            activeDspConfig = activeDspSnapshot,
                        ) { procFrac ->
                            val mapped = if (localPathFile != null) procFrac else (0.20f + procFrac * 0.80f)
                            updateBatchItemProgress(index, mapped.coerceIn(0.05f, 0.98f), total)
                        } ?: throw IllegalStateException("Audio engine returned empty output")

                        // Auto-write copy to user's configured default save folder if selected
                        defaultSaveTreeUriString?.takeIf { it.isNotBlank() }?.let { treeUriStr ->
                            val cleanOutName = "${item.displayName.substringBeforeLast('.')}_sonix.${result.extension}"
                            withContext(Dispatchers.IO) {
                                writeResultToTreeFolder(result, cleanOutName, treeUriStr)
                            }
                        }

                        // Index lighter outputs into Studio Recordings library for convenience
                        if (!withContext(Dispatchers.IO) { AudioDecoder.shouldStreamOnDisk(result.file) }) {
                            runCatching {
                                AudioDecoder.decodeFile(result.file)?.let { buf ->
                                    if (buf.frameCount > 0) {
                                        repository.indexAudioRecording(
                                            title = "${item.displayName.substringBeforeLast('.')} (Batch)",
                                            buffer = buf,
                                            sourceType = RecordingSourceType.IMPORTED,
                                        )
                                    }
                                }
                            }
                        }

                        _uiState.update { cur ->
                            val updated = cur.batchQueueState.items.mapIndexed { idx, entry ->
                                if (idx == index) {
                                    entry.copy(
                                        status = BatchItemStatus.COMPLETED,
                                        itemProgress = 1f,
                                        outputFilePath = result.file.absolutePath,
                                        outputFormat = settings.outputFormat,
                                        errorMessage = null,
                                    )
                                } else {
                                    entry
                                }
                            }
                            cur.copy(
                                batchQueueState = cur.batchQueueState.copy(
                                    items = updated,
                                    completedCount = updated.count { it.status == BatchItemStatus.COMPLETED },
                                    failedCount = updated.count { it.status == BatchItemStatus.FAILED },
                                    overallProgress = ((index + 1).toFloat() / total.toFloat()).coerceIn(0f, 1f),
                                ),
                            )
                        }
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (t: Throwable) {
                        AppDiagnosticLog.logRecoverable(
                            toolName = "BatchProcessingQueue",
                            titleAr = "تعذر معالجة ملف في القائمة الدفعية",
                            titleEn = "Batch Item Failed",
                            causeAr = "${item.displayName}: ${t.localizedMessage ?: t.javaClass.simpleName}",
                            causeEn = "${item.displayName}: ${t.localizedMessage ?: t.javaClass.simpleName}",
                            errorCode = t.javaClass.simpleName,
                            throwable = t,
                        )
                        _uiState.update { cur ->
                            val updated = cur.batchQueueState.items.mapIndexed { idx, entry ->
                                if (idx == index) {
                                    entry.copy(
                                        status = BatchItemStatus.FAILED,
                                        itemProgress = 0f,
                                        errorMessage = t.localizedMessage ?: t.javaClass.simpleName,
                                    )
                                } else {
                                    entry
                                }
                            }
                            cur.copy(
                                batchQueueState = cur.batchQueueState.copy(
                                    items = updated,
                                    completedCount = updated.count { it.status == BatchItemStatus.COMPLETED },
                                    failedCount = updated.count { it.status == BatchItemStatus.FAILED },
                                    overallProgress = ((index + 1).toFloat() / total.toFloat()).coerceIn(0f, 1f),
                                ),
                            )
                        }
                    } finally {
                        tempCopiedSource?.let { f -> runCatching { f.delete() } }
                    }
                }

                val doneBatch = _uiState.value.batchQueueState
                val isEn = _uiState.value.language == AppLanguage.EN
                val summary = if (isEn) {
                    "Batch finished: ${doneBatch.completedCount} succeeded, ${doneBatch.failedCount} failed"
                } else {
                    "اكتملت المعالجة الدفعية: نجاح ${doneBatch.completedCount} • فشل ${doneBatch.failedCount}"
                }
                _uiState.update { cur ->
                    cur.copy(
                        batchQueueState = cur.batchQueueState.copy(
                            isRunning = false,
                            currentIndex = -1,
                            overallProgress = 1f,
                            statusSummary = summary,
                        ),
                    )
                }
                postBanner(summary)
            } catch (ce: CancellationException) {
                val isEn = _uiState.value.language == AppLanguage.EN
                val cancelledMsg = if (isEn) "Batch processing cancelled" else "تم إيقاف المعالجة الدفعية"
                _uiState.update { cur ->
                    val resetItems = cur.batchQueueState.items.map { entry ->
                        if (entry.status == BatchItemStatus.PROCESSING) {
                            entry.copy(status = BatchItemStatus.QUEUED, itemProgress = 0f)
                        } else {
                            entry
                        }
                    }
                    cur.copy(
                        batchQueueState = cur.batchQueueState.copy(
                            items = resetItems,
                            isRunning = false,
                            currentIndex = -1,
                            statusSummary = cancelledMsg,
                        ),
                    )
                }
                throw ce
            } finally {
                _uiState.update { cur ->
                    if (cur.batchQueueState.isRunning) {
                        cur.copy(
                            batchQueueState = cur.batchQueueState.copy(
                                isRunning = false,
                                currentIndex = -1,
                            ),
                        )
                    } else {
                        cur
                    }
                }
            }
        }
    }

    private fun updateBatchItemProgress(index: Int, itemFrac: Float, totalItems: Int) {
        _uiState.update { cur ->
            val updated = cur.batchQueueState.items.mapIndexed { idx, entry ->
                if (idx == index) entry.copy(itemProgress = itemFrac) else entry
            }
            val doneBefore = updated.count { it.status == BatchItemStatus.COMPLETED || it.status == BatchItemStatus.FAILED }
            val overall = ((doneBefore + itemFrac) / totalItems.coerceAtLeast(1).toFloat()).coerceIn(0f, 0.99f)
            cur.copy(
                batchQueueState = cur.batchQueueState.copy(
                    items = updated,
                    overallProgress = overall,
                ),
            )
        }
    }

    fun cancelBatchProcessingQueue() {
        batchProcessingJob?.cancel()
        batchProcessingJob = null
    }

    fun previewBatchOutputItem(item: BatchQueueItem) {
        val currentPreviewId = _uiState.value.batchQueueState.previewingItemId
        if (currentPreviewId == item.id && _uiState.value.isToolPreviewPlaying) {
            stopToolPreview()
            return
        }
        val path = item.outputFilePath ?: return
        val file = File(path)
        if (!file.exists() || file.length() <= 0L) {
            postBanner(
                if (_uiState.value.language == AppLanguage.EN) "Output file is missing"
                else "الملف الناتج غير موجود",
            )
            return
        }
        viewModelScope.launch(safeCoroutineHandler) {
            stopToolPreview()
            stopMixPreviewPlayer()
            val started = startGlobalFilePreview(file)
            if (started) {
                _uiState.update {
                    it.copy(
                        isToolPreviewPlaying = true,
                        batchQueueState = it.batchQueueState.copy(previewingItemId = item.id),
                    )
                }
            } else {
                stopToolPreview()
                postBanner(
                    if (_uiState.value.language == AppLanguage.EN) "Unable to play preview"
                    else "تعذر تشغيل معاينة الملف الناتج",
                )
            }
        }
    }

    fun saveBatchOutputItem(item: BatchQueueItem) {
        val path = item.outputFilePath ?: return
        val file = File(path)
        if (!file.exists() || file.length() <= 0L) {
            postBanner(
                if (_uiState.value.language == AppLanguage.EN) "Output file not found"
                else "الملف الناتج غير متاح للحفظ",
            )
            return
        }
        val ext = file.extension.lowercase().ifBlank { "m4a" }
        val mime = when (ext) {
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            "mp3" -> "audio/mpeg"
            "ogg" -> "audio/ogg"
            else -> "audio/mp4"
        }
        val cleanBase = "${item.displayName.substringBeforeLast('.')}_sonix"
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .ifBlank { "Sonix_Batch_Output" }
        dispatchSaveToDevice(ExportedAudioResult(file, ext, mime), "$cleanBase.$ext")
    }

    fun shareAllBatchOutputs() {
        val app = getApplication<Application>()
        val completedFiles = _uiState.value.batchQueueState.items
            .filter { it.status == BatchItemStatus.COMPLETED && !it.outputFilePath.isNullOrBlank() }
            .mapNotNull { entry -> File(entry.outputFilePath!!).takeIf { it.exists() && it.length() > 0L } }

        if (completedFiles.isEmpty()) {
            postBanner(
                if (_uiState.value.language == AppLanguage.EN) "No completed batch files to share yet"
                else "لا توجد ملفات مكتملة للمشاركة بعد",
            )
            return
        }
        val uris = completedFiles.mapNotNull { file ->
            runCatching { FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file) }.getOrNull()
        }
        if (uris.isEmpty()) return

        if (uris.size == 1) {
            val f = completedFiles.first()
            val mime = when (f.extension.lowercase()) {
                "wav" -> "audio/wav"
                "flac" -> "audio/flac"
                "mp3" -> "audio/mpeg"
                "ogg" -> "audio/ogg"
                else -> "audio/mp4"
            }
            _sideEffects.tryEmit(StudioSideEffect.LaunchShareIntent(uris.first(), mime, f.name))
        } else {
            _sideEffects.tryEmit(
                StudioSideEffect.LaunchMultiShareIntent(
                    uris = uris,
                    mimeType = "audio/*",
                    title = "Sonix Batch Outputs (${uris.size})",
                ),
            )
        }
    }

    fun loadBatchOutputToStudio(
        item: BatchQueueItem,
        onComplete: (() -> Unit)? = null,
    ) {
        val path = item.outputFilePath ?: return
        val file = File(path)
        if (!file.exists() || file.length() <= 0L) return
        importAudioUri(Uri.fromFile(file), onComplete)
    }

    // =========================================================================
    // AI VOICE STUDIO – استوديو الصوت الذكي (VoxCPM2 + محرك الاستوديو المحلي)
    // =========================================================================

    fun setAiVoiceTab(tab: Int) {
        _aiVoiceUiState.update { it.copy(activeTab = tab.coerceIn(0, 1)) }
    }

    fun setAiVoiceEngine(engine: AiVoiceEngineKind) {
        _aiVoiceUiState.update { cur ->
            val clampedEmotion = if (cur.selectedEmotion in engine.supportedEmotions()) {
                cur.selectedEmotion
            } else {
                AiVoiceEmotion.NEUTRAL
            }
            val clampedLanguage = if (engine.supportsLanguage(cur.selectedLanguage)) {
                cur.selectedLanguage
            } else {
                AiVoiceLanguage.AR
            }
            cur.copy(
                selectedEngine = engine,
                selectedEmotion = clampedEmotion,
                selectedLanguage = clampedLanguage,
                errorTitle = null,
                errorDetail = null,
                setupRequiredGuide = null,
            )
        }
    }

    fun toggleAiVoiceCharacterFavorite(characterId: String) {
        val current = _aiVoiceUiState.value.favoriteCharacterIds
        val next = if (characterId in current) current - characterId else current + characterId
        _aiVoiceUiState.update { it.copy(favoriteCharacterIds = next) }
        viewModelScope.launch(safeCoroutineHandler) {
            repository.saveFavoriteAiVoiceCharacterIds(next)
        }
    }

    fun setAiVoiceLanguage(language: AiVoiceLanguage) {
        _aiVoiceUiState.update {
            it.copy(
                selectedLanguage = language,
                selectedDialect = if (language == AiVoiceLanguage.AR) it.selectedDialect else AiArabicDialect.MSA,
                selectedStudioPreset = AiStudioPreset.CUSTOM,
            )
        }
    }

    fun setAiVoiceDialect(dialect: AiArabicDialect) {
        _aiVoiceUiState.update {
            it.copy(selectedDialect = dialect, selectedStudioPreset = AiStudioPreset.CUSTOM)
        }
    }

    fun setAiVoiceEnglishAccent(accent: AiEnglishAccent) {
        _aiVoiceUiState.update {
            it.copy(selectedEnglishAccent = accent, selectedStudioPreset = AiStudioPreset.CUSTOM)
        }
    }

    fun setAiVoiceCategory(category: AiVoiceCategory) {
        _aiVoiceUiState.update {
            it.copy(selectedCategory = category, selectedStudioPreset = AiStudioPreset.CUSTOM)
        }
    }

    fun setAiVoiceEmotion(emotion: AiVoiceEmotion) {
        _aiVoiceUiState.update {
            it.copy(selectedEmotion = emotion, selectedStudioPreset = AiStudioPreset.CUSTOM)
        }
    }

    fun setAiVocalStyle(style: AiVocalStyle) {
        _aiVoiceUiState.update {
            it.copy(selectedVocalStyle = style, selectedStudioPreset = AiStudioPreset.CUSTOM)
        }
    }

    fun setAiBreathCadence(cadence: AiBreathCadence) {
        _aiVoiceUiState.update {
            it.copy(selectedBreathCadence = cadence, selectedStudioPreset = AiStudioPreset.CUSTOM)
        }
    }

    fun setAiPitchContour(contour: AiPitchContour) {
        _aiVoiceUiState.update {
            it.copy(selectedPitchContour = contour, selectedStudioPreset = AiStudioPreset.CUSTOM)
        }
    }

    fun setAiVoiceTimbre(timbre: AiVoiceTimbre) {
        _aiVoiceUiState.update {
            it.copy(selectedTimbre = timbre, selectedStudioPreset = AiStudioPreset.CUSTOM)
        }
    }

    fun setAiArticulationClarity(clarity: Float) {
        _aiVoiceUiState.update {
            it.copy(
                articulationClarity = clarity.coerceIn(0f, 1f),
                selectedStudioPreset = AiStudioPreset.CUSTOM,
            )
        }
    }

    fun setAiVoiceStudioPreset(preset: AiStudioPreset) {
        _aiVoiceUiState.update { cur ->
            when (preset) {
                AiStudioPreset.CUSTOM -> cur.copy(selectedStudioPreset = preset)
                AiStudioPreset.BROADCAST -> cur.copy(
                    selectedStudioPreset = preset,
                    selectedVocalStyle = AiVocalStyle.NEWS_ANCHOR,
                    selectedEmotion = AiVoiceEmotion.SERIOUS,
                    selectedBreathCadence = AiBreathCadence.NATURAL,
                    selectedPitchContour = AiPitchContour.STEADY,
                    selectedTimbre = AiVoiceTimbre.BALANCED,
                    articulationClarity = 0.78f,
                    selectedPace = AiDeliveryPace.NORMAL,
                    speechRate = 1.0f,
                    emotionIntensity = 0.65f,
                )
                AiStudioPreset.WARM_PODCAST -> cur.copy(
                    selectedStudioPreset = preset,
                    selectedVocalStyle = AiVocalStyle.WARM,
                    selectedEmotion = AiVoiceEmotion.CALM,
                    selectedBreathCadence = AiBreathCadence.NATURAL,
                    selectedPitchContour = AiPitchContour.STEADY,
                    selectedTimbre = AiVoiceTimbre.WARM,
                    articulationClarity = 0.68f,
                    selectedPace = AiDeliveryPace.NORMAL,
                    speechRate = 1.0f,
                    emotionIntensity = 0.55f,
                )
                AiStudioPreset.CINEMATIC_STORY -> cur.copy(
                    selectedStudioPreset = preset,
                    selectedVocalStyle = AiVocalStyle.STORYTELLING,
                    selectedEmotion = AiVoiceEmotion.CALM,
                    selectedBreathCadence = AiBreathCadence.MEASURED,
                    selectedPitchContour = AiPitchContour.GENTLE_FALL,
                    selectedTimbre = AiVoiceTimbre.DEEP,
                    articulationClarity = 0.62f,
                    selectedPace = AiDeliveryPace.SLOW,
                    speechRate = 0.92f,
                    emotionIntensity = 0.70f,
                )
                AiStudioPreset.BRIGHT_PROMO -> cur.copy(
                    selectedStudioPreset = preset,
                    selectedVocalStyle = AiVocalStyle.ENERGETIC,
                    selectedEmotion = AiVoiceEmotion.EXCITED,
                    selectedBreathCadence = AiBreathCadence.BRISK,
                    selectedPitchContour = AiPitchContour.GENTLE_RISE,
                    selectedTimbre = AiVoiceTimbre.BRIGHT,
                    articulationClarity = 0.86f,
                    selectedPace = AiDeliveryPace.FAST,
                    speechRate = 1.08f,
                    emotionIntensity = 0.72f,
                )
                AiStudioPreset.INTIMATE_WHISPER -> cur.copy(
                    selectedStudioPreset = preset,
                    selectedVocalStyle = AiVocalStyle.WHISPERED,
                    selectedEmotion = AiVoiceEmotion.WHISPER,
                    selectedBreathCadence = AiBreathCadence.MEASURED,
                    selectedPitchContour = AiPitchContour.STEADY,
                    selectedTimbre = AiVoiceTimbre.AIRY,
                    articulationClarity = 0.58f,
                    selectedPace = AiDeliveryPace.SLOW,
                    speechRate = 0.90f,
                    emotionIntensity = 0.65f,
                )
            }
        }
    }

    fun setAiVoiceEmotionIntensity(intensity: Float) {
        _aiVoiceUiState.update {
            it.copy(
                emotionIntensity = intensity.coerceIn(0.1f, 1.0f),
                selectedStudioPreset = AiStudioPreset.CUSTOM,
            )
        }
    }

    fun setAiVoicePace(pace: AiDeliveryPace) {
        _aiVoiceUiState.update {
            it.copy(
                selectedPace = pace,
                speechRate = pace.speedMultiplier,
                selectedStudioPreset = AiStudioPreset.CUSTOM,
            )
        }
    }

    fun setAiVoiceSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.60f, 1.50f)
        _aiVoiceUiState.update {
            it.copy(
                speechRate = clamped,
                selectedPace = AiDeliveryPace.fromSpeechRate(clamped),
                selectedStudioPreset = AiStudioPreset.CUSTOM,
            )
        }
    }

    fun resetAiVoiceTuning() {
        _aiVoiceUiState.update { cur ->
            val activeChar = cur.allCharacters.firstOrNull { it.id == cur.selectedCharacterId }
            val defaultRate = activeChar?.defaultSpeechRate ?: 1.0f
            cur.copy(
                selectedEmotion = activeChar?.defaultEmotion ?: AiVoiceEmotion.NEUTRAL,
                selectedVocalStyle = activeChar?.defaultVocalStyle ?: AiVoiceEmotion.NEUTRAL.defaultVocalStyle(),
                selectedBreathCadence = activeChar?.defaultBreathCadence ?: AiBreathCadence.NATURAL,
                selectedPitchContour = activeChar?.defaultPitchContour ?: AiPitchContour.STEADY,
                selectedTimbre = activeChar?.defaultTimbre ?: AiVoiceTimbre.BALANCED,
                articulationClarity = activeChar?.defaultArticulationClarity ?: 0.55f,
                selectedStudioPreset = AiStudioPreset.CUSTOM,
                emotionIntensity = activeChar?.defaultEmotionIntensity ?: 0.65f,
                speechRate = defaultRate,
                selectedPace = activeChar?.defaultPace ?: AiDeliveryPace.fromSpeechRate(defaultRate),
                pitchOffsetSemitones = activeChar?.pitchOffsetSemitones ?: cur.pitchOffsetSemitones,
            )
        }
    }

    fun setAiVoicePitchOffset(semitones: Float) {
        _aiVoiceUiState.update {
            it.copy(
                pitchOffsetSemitones = semitones.coerceIn(-8f, 8f),
                selectedStudioPreset = AiStudioPreset.CUSTOM,
            )
        }
    }

    fun setAiVoiceDescriptionPrompt(prompt: String) {
        _aiVoiceUiState.update { it.copy(voiceDescriptionPrompt = prompt) }
    }

    fun setAiVoiceScriptText(text: String) {
        val prevText = _aiVoiceUiState.value.scriptText
        if (prevText != text) {
            stopAiVoiceOutput()
        }
        if (text.isBlank()) {
            aiVoiceSynthJob?.cancel()
            aiVoiceSynthJob = null
            val oldPath = _aiVoiceUiState.value.lastOutputWavPath
            if (!oldPath.isNullOrBlank()) {
                runCatching { File(oldPath).delete() }
            }
            _aiVoiceUiState.update {
                it.copy(
                    scriptText = text,
                    isSynthesizing = false,
                    synthesisProgress = 0f,
                    synthesisStatusLabel = "",
                    lastOutputWavPath = null,
                    lastOutputDurationMs = 0L,
                    lastOutputAppliedPrompt = null,
                    lastOutputEngine = null,
                    errorTitle = null,
                    errorDetail = null,
                    setupRequiredGuide = null,
                )
            }
        } else {
            _aiVoiceUiState.update {
                it.copy(
                    scriptText = text,
                    errorTitle = null,
                    errorDetail = null,
                    setupRequiredGuide = null,
                )
            }
        }
    }

    fun setAiVoiceReferenceTranscript(text: String) {
        _aiVoiceUiState.update { it.copy(referenceTranscript = text) }
    }

    fun setAiVoiceReferenceRightsConfirmed(confirmed: Boolean) {
        _aiVoiceUiState.update { it.copy(referenceRightsConfirmed = confirmed) }
    }

    fun dismissAiVoiceError() {
        _aiVoiceUiState.update { it.copy(errorTitle = null, errorDetail = null, setupRequiredGuide = null) }
    }

    fun selectAiVoiceCharacter(character: AiVoiceCharacter) {
        stopAiVoiceOutput()
        AudioPlayerManager.releaseAll()
        _aiVoiceUiState.update { cur ->
            val targetEngine = cur.selectedEngine
            val safeEmotion = if (character.defaultEmotion in targetEngine.supportedEmotions()) {
                character.defaultEmotion
            } else {
                AiVoiceEmotion.NEUTRAL
            }
            val safeLanguage = if (targetEngine.supportsLanguage(character.language)) {
                character.language
            } else {
                AiVoiceLanguage.AR
            }
            cur.copy(
                selectedCharacterId = character.id,
                selectedEngine = targetEngine,
                selectedLanguage = safeLanguage,
                selectedDialect = character.dialect,
                selectedCategory = character.category,
                selectedEmotion = safeEmotion,
                selectedVocalStyle = character.defaultVocalStyle,
                selectedBreathCadence = character.defaultBreathCadence,
                selectedPitchContour = character.defaultPitchContour,
                selectedTimbre = character.defaultTimbre,
                articulationClarity = character.defaultArticulationClarity,
                selectedStudioPreset = AiStudioPreset.CUSTOM,
                emotionIntensity = character.defaultEmotionIntensity,
                selectedPace = character.defaultPace,
                speechRate = character.defaultSpeechRate,
                pitchOffsetSemitones = character.pitchOffsetSemitones,
                fixedSid = character.fixedSid,
                fixedSeed = character.fixedSeed,
                fixedFormantShift = character.fixedFormantShift,
                fixedEqLowDb = character.fixedEqLowDb,
                fixedEqPresenceDb = character.fixedEqPresenceDb,
                voiceDescriptionPrompt = character.voiceDescriptionPrompt,
                referenceAudioPath = null,
                referenceAudioName = null,
                referenceAudioDurationMs = 0L,
                referenceTranscript = "",
                referenceRightsConfirmed = false,
                isReferenceImporting = false,
                referenceImportProgress = null,
                referenceImportStatus = "",
                errorTitle = null,
                errorDetail = null,
                setupRequiredGuide = null,
            )
        }
    }

    fun importAiVoiceReferenceFromUri(uri: Uri) {
        if (_aiVoiceUiState.value.isReferenceImporting) return
        aiVoiceReferenceImportJob = viewModelScope.launch(safeCoroutineHandler) {
            val app = getApplication<Application>()
            val refDir = File(app.filesDir, "ai_voice_refs").apply { mkdirs() }
            val outWav = File(refDir, "ref_${System.currentTimeMillis()}.wav")
            _aiVoiceUiState.update {
                it.copy(
                    isReferenceImporting = true,
                    referenceImportProgress = 0f,
                    referenceImportStatus = "جاري رفع العينة وتحضيرها...",
                    errorTitle = null,
                    errorDetail = null,
                )
            }
            try {
                val displayName = withContext(Dispatchers.IO) {
                    AudioFileUploader.resolveDisplayName(app, uri)
                }
                val prepared = AiVoiceAudioStitcher.prepareReferenceWavFromUri(
                    context = app,
                    uri = uri,
                    outWavFile = outWav,
                ) { progress, status ->
                    _aiVoiceUiState.update {
                        it.copy(
                            referenceImportProgress = progress,
                            referenceImportStatus = status,
                        )
                    }
                } ?: throw IllegalStateException("تعذر قراءة الملف المرجعي للاستنساخ الصوتي")
                _aiVoiceUiState.update {
                    it.copy(
                        referenceAudioPath = prepared.wavFile.absolutePath,
                        referenceAudioName = displayName,
                        referenceAudioDurationMs = prepared.durationMs,
                        referenceImportProgress = 1f,
                        referenceImportStatus = "اكتمل تجهيز العينة المرجعية",
                    )
                }
                postBanner("تم تجهيز العينة المرجعية ($displayName) للاستنساخ الصوتي")
            } catch (ce: CancellationException) {
                runCatching { outWav.delete() }
                throw ce
            } catch (e: Exception) {
                runCatching { outWav.delete() }
                val message = e.localizedMessage ?: e.javaClass.simpleName
                _aiVoiceUiState.update {
                    it.copy(
                        referenceImportProgress = null,
                        referenceImportStatus = "تعذر تجهيز العينة المرجعية: $message",
                        errorTitle = "تعذر استيراد العينة المرجعية",
                        errorDetail = message,
                    )
                }
                postBanner("تعذر تجهيز العينة المرجعية: $message")
            } finally {
                _aiVoiceUiState.update { it.copy(isReferenceImporting = false) }
            }
        }
    }

    fun importAiVoiceReferenceFromCurrentBuffer() {
        val buf = activeBuffer
        if (buf == null || buf.frameCount <= 0) {
            postBanner("لا يوجد تسجيل أو ملف صوتي حالي في الاستوديو لاستخدامه كمرجع")
            return
        }
        if (_aiVoiceUiState.value.isReferenceImporting) return
        aiVoiceReferenceImportJob = viewModelScope.launch(safeCoroutineHandler) {
            val app = getApplication<Application>()
            val refDir = File(app.filesDir, "ai_voice_refs").apply { mkdirs() }
            val outWav = File(refDir, "studio_ref_${System.currentTimeMillis()}.wav")
            _aiVoiceUiState.update {
                it.copy(
                    isReferenceImporting = true,
                    referenceImportProgress = 0f,
                    referenceImportStatus = "جاري تجهيز صوت الاستوديو كعينة مرجعية...",
                    errorTitle = null,
                    errorDetail = null,
                )
            }
            try {
                val prepared = AiVoiceAudioStitcher.prepareReferenceWavFromBuffer(
                    buffer = buf,
                    outWavFile = outWav,
                ) { progress, status ->
                    _aiVoiceUiState.update {
                        it.copy(
                            referenceImportProgress = progress,
                            referenceImportStatus = status,
                        )
                    }
                } ?: throw IllegalStateException("تعذر تجهيز صوت الاستوديو كعينة مرجعية")
                val label = _uiState.value.activeProjectName.ifBlank { "تسجيل الاستوديو الحالي" }
                _aiVoiceUiState.update {
                    it.copy(
                        referenceAudioPath = prepared.wavFile.absolutePath,
                        referenceAudioName = label,
                        referenceAudioDurationMs = prepared.durationMs,
                        referenceImportProgress = 1f,
                        referenceImportStatus = "اكتمل تجهيز العينة المرجعية",
                    )
                }
                postBanner("تم استخدام صوت الاستوديو كعينة مرجعية (${formatDurationMs(prepared.durationMs)})")
            } catch (ce: CancellationException) {
                runCatching { outWav.delete() }
                throw ce
            } catch (e: Exception) {
                runCatching { outWav.delete() }
                val message = e.localizedMessage ?: e.javaClass.simpleName
                _aiVoiceUiState.update {
                    it.copy(
                        referenceImportProgress = null,
                        referenceImportStatus = "تعذر تجهيز العينة المرجعية: $message",
                        errorTitle = "تعذر تجهيز العينة المرجعية",
                        errorDetail = message,
                    )
                }
                postBanner("تعذر تجهيز العينة المرجعية: $message")
            } finally {
                _aiVoiceUiState.update { it.copy(isReferenceImporting = false) }
            }
        }
    }

    fun clearAiVoiceReference() {
        aiVoiceReferenceImportJob?.cancel()
        val oldPath = _aiVoiceUiState.value.referenceAudioPath
        if (!oldPath.isNullOrBlank()) {
            val isUsedBySavedChar = _aiVoiceUiState.value.savedCharacters.any { it.referenceAudioPath == oldPath }
            if (!isUsedBySavedChar) {
                runCatching { File(oldPath).delete() }
            }
        }
        _aiVoiceUiState.update {
            it.copy(
                referenceAudioPath = null,
                referenceAudioName = null,
                referenceAudioDurationMs = 0L,
                referenceTranscript = "",
                isReferenceImporting = false,
                referenceImportProgress = null,
                referenceImportStatus = "",
            )
        }
    }

    fun saveCustomAiVoiceCharacter(nameAr: String, nameEn: String = "") {
        val cur = _aiVoiceUiState.value
        val cleanName = nameAr.trim()
        if (cleanName.isEmpty()) {
            postBanner("يرجى إدخال اسم الشخصية الصوتية أولاً")
            return
        }
        val charObj = AiVoiceCharacter(
            id = "custom_${UUID.randomUUID()}",
            nameAr = cleanName,
            nameEn = nameEn.trim().ifBlank { cleanName },
            roleDescriptionAr = cur.voiceDescriptionPrompt.trim().take(80),
            category = cur.selectedCategory,
            language = cur.selectedLanguage,
            dialect = cur.selectedDialect,
            preferredEngine = cur.selectedEngine,
            voiceDescriptionPrompt = cur.voiceDescriptionPrompt.trim(),
            defaultEmotion = cur.selectedEmotion,
            defaultVocalStyle = cur.selectedVocalStyle,
            defaultBreathCadence = cur.selectedBreathCadence,
            defaultPitchContour = cur.selectedPitchContour,
            defaultTimbre = cur.selectedTimbre,
            defaultArticulationClarity = cur.articulationClarity,
            defaultEmotionIntensity = cur.emotionIntensity,
            defaultPace = cur.selectedPace,
            defaultSpeechRate = cur.speechRate,
            pitchOffsetSemitones = cur.pitchOffsetSemitones,
            fixedSid = cur.fixedSid,
            fixedSeed = cur.fixedSeed,
            fixedFormantShift = cur.fixedFormantShift,
            fixedEqLowDb = cur.fixedEqLowDb,
            fixedEqPresenceDb = cur.fixedEqPresenceDb,
            isBuiltIn = false,
            createdAtMs = System.currentTimeMillis(),
        )
        val updated = listOf(charObj) + cur.savedCharacters
        viewModelScope.launch(safeCoroutineHandler) {
            repository.saveAiVoiceCharacters(updated)
            _aiVoiceUiState.update { it.copy(selectedCharacterId = charObj.id) }
            postBanner("تم حفظ الشخصية \"$cleanName\" في مكتبة الشخصيات")
        }
    }

    fun deleteSavedAiVoiceCharacter(characterId: String) {
        val cur = _aiVoiceUiState.value
        val updated = cur.savedCharacters.filterNot { it.id == characterId }
        viewModelScope.launch(safeCoroutineHandler) {
            repository.saveAiVoiceCharacters(updated)
            postBanner("تم حذف الشخصية المحفوظة")
        }
    }

    fun addDialogueTurn(characterId: String? = null) {
        _aiVoiceUiState.update { cur ->
            val targetChar = characterId ?: cur.selectedCharacterId
            val newTurn = AiDialogueTurn(
                id = "turn_${UUID.randomUUID()}",
                characterId = targetChar,
                text = "",
                emotion = cur.selectedEmotion,
                emotionIntensity = cur.emotionIntensity,
                pace = cur.selectedPace,
                pauseAfterMs = 450,
            )
            cur.copy(dialogueTurns = cur.dialogueTurns + newTurn)
        }
    }

    fun removeDialogueTurn(turnId: String) {
        _aiVoiceUiState.update { cur ->
            if (cur.dialogueTurns.size <= 1) return@update cur
            cur.copy(dialogueTurns = cur.dialogueTurns.filterNot { it.id == turnId })
        }
    }

    fun updateDialogueTurn(turnId: String, transform: (AiDialogueTurn) -> AiDialogueTurn) {
        _aiVoiceUiState.update { cur ->
            cur.copy(
                dialogueTurns = cur.dialogueTurns.map { turn ->
                    if (turn.id == turnId) transform(turn) else turn
                },
            )
        }
    }

    fun moveDialogueTurn(turnId: String, delta: Int) {
        _aiVoiceUiState.update { cur ->
            val list = cur.dialogueTurns.toMutableList()
            val idx = list.indexOfFirst { it.id == turnId }
            val nextIdx = idx + delta
            if (idx !in list.indices || nextIdx !in list.indices) return@update cur
            val item = list.removeAt(idx)
            list.add(nextIdx, item)
            cur.copy(dialogueTurns = list)
        }
    }

    fun saveAiVoiceBackendUrl(url: String) {
        viewModelScope.launch(safeCoroutineHandler) {
            repository.saveAiVoiceBackendUrl("")
            _aiVoiceUiState.update { it.copy(backendServerUrl = "") }
            postBanner("يستخدم استوديو الصوت خدمة VoxCPM2 السحابية")
        }
    }

    fun saveAiVoiceBackendBearer(token: String) {
        viewModelScope.launch(safeCoroutineHandler) {
            repository.saveAiVoiceBackendBearer("")
            _aiVoiceUiState.update { it.copy(backendBearerToken = "") }
        }
    }

    fun checkAiVoiceBackendHealth(
        pendingUrl: String? = null,
        pendingToken: String? = null,
    ) {
        viewModelScope.launch(safeCoroutineHandler) {
            _aiVoiceUiState.update {
                it.copy(
                    backendServerUrl = "",
                    backendBearerToken = "",
                    backendHealth = it.backendHealth.copy(isChecking = true),
                )
            }
            val health = aiVoiceCoordinator.checkBackendHealth(
                context = getApplication(),
                backendUrl = "",
                bearerToken = "",
            )
            _aiVoiceUiState.update { it.copy(backendHealth = health) }
        }
    }

    fun synthesizeAiVoiceSingle(overrideEngine: AiVoiceEngineKind? = null) {
        val cur = _aiVoiceUiState.value
        val engine = overrideEngine ?: cur.selectedEngine
        if (cur.scriptText.isBlank()) {
            _aiVoiceUiState.update {
                it.copy(
                    errorTitle = "النص مطلوب",
                    errorDetail = "يرجى كتابة النص المراد تحويله إلى صوت أولاً.",
                )
            }
            return
        }
        if (
            !cur.referenceAudioPath.isNullOrBlank() &&
            !cur.referenceRightsConfirmed
        ) {
            _aiVoiceUiState.update {
                it.copy(
                    errorTitle = "تأكيد حقوق العينة المرجعية مطلوب",
                    errorDetail = "يرجى تأكيد أنك تملك حق استخدام العينة المرجعية قبل الاستنساخ الصوتي.",
                )
            }
            return
        }

        stopAiVoiceOutput()
        aiVoiceSynthJob?.cancel()
        aiVoiceSynthJob = viewModelScope.launch(safeCoroutineHandler) {
            _aiVoiceUiState.update {
                it.copy(
                    selectedEngine = engine,
                    isSynthesizing = true,
                    synthesisProgress = 0.04f,
                    synthesisStatusLabel = "جاري الاتصال بمحرك VoxCPM2 السحابي...",
                    errorTitle = null,
                    errorDetail = null,
                    setupRequiredGuide = null,
                )
            }
            val app = getApplication<Application>()
            val outDir = File(app.cacheDir, "ai_voice_outputs").apply { mkdirs() }
            val req = AiVoiceSynthRequest(
                engine = engine,
                text = cur.scriptText.trim(),
                language = cur.selectedLanguage,
                voiceCharacterId = cur.selectedCharacterId,
                dialect = cur.selectedDialect,
                englishAccent = cur.selectedEnglishAccent,
                category = cur.selectedCategory,
                voiceDescriptionPrompt = cur.voiceDescriptionPrompt.trim(),
                emotion = cur.selectedEmotion,
                emotionIntensity = cur.emotionIntensity,
                pace = cur.selectedPace,
                speechRate = cur.speechRate,
                pitchOffsetSemitones = cur.pitchOffsetSemitones,
                fixedSid = cur.fixedSid,
                fixedSeed = cur.fixedSeed,
                fixedFormantShift = cur.fixedFormantShift,
                fixedEqLowDb = cur.fixedEqLowDb,
                fixedEqPresenceDb = cur.fixedEqPresenceDb,
                vocalStyle = cur.selectedVocalStyle,
                breathCadence = cur.selectedBreathCadence,
                pitchContour = cur.selectedPitchContour,
                timbre = cur.selectedTimbre,
                articulationClarity = cur.articulationClarity,
            )
            val outcome = aiVoiceCoordinator.synthesizeSingle(
                context = app,
                request = req,
                backendUrl = cur.backendServerUrl,
                outputDir = outDir,
                bearerToken = cur.backendBearerToken,
            ) { progress, label ->
                _aiVoiceUiState.update {
                    it.copy(synthesisProgress = progress, synthesisStatusLabel = label)
                }
            }
            handleAiVoiceOutcome(outcome)
        }
    }

    fun synthesizeAiVoiceDialogue(overrideEngine: AiVoiceEngineKind? = null) {
        val cur = _aiVoiceUiState.value
        val activeTurns = cur.dialogueTurns.filter { it.text.trim().isNotEmpty() }
        if (activeTurns.isEmpty()) {
            _aiVoiceUiState.update {
                it.copy(
                    errorTitle = "الحوار فارغ",
                    errorDetail = "أضف جملة واحدة على الأقل في سيناريو الحوار قبل التوليد.",
                )
            }
            return
        }
        stopAiVoiceOutput()
        aiVoiceSynthJob?.cancel()
        aiVoiceSynthJob = viewModelScope.launch(safeCoroutineHandler) {
            _aiVoiceUiState.update {
                it.copy(
                    isSynthesizing = true,
                    synthesisProgress = 0.03f,
                    synthesisStatusLabel = "بدء توليد حوار متعدد الشخصيات (${activeTurns.size} جمل)...",
                    errorTitle = null,
                    errorDetail = null,
                    setupRequiredGuide = null,
                )
            }
            val app = getApplication<Application>()
            val outDir = File(app.cacheDir, "ai_voice_outputs").apply { mkdirs() }
            val outcome = aiVoiceCoordinator.synthesizeMultiCharacterDialogue(
                context = app,
                turns = cur.dialogueTurns,
                characters = cur.allCharacters,
                selectedEngineOverride = overrideEngine ?: cur.selectedEngine,
                backendUrl = cur.backendServerUrl,
                outputDir = outDir,
                bearerToken = cur.backendBearerToken,
            ) { progress, label ->
                _aiVoiceUiState.update {
                    it.copy(synthesisProgress = progress, synthesisStatusLabel = label)
                }
            }
            handleAiVoiceOutcome(outcome)
        }
    }

    fun cancelAiVoiceSynthesis() {
        aiVoiceSynthJob?.cancel()
        aiVoiceSynthJob = null
        _aiVoiceUiState.update {
            it.copy(
                isSynthesizing = false,
                synthesisProgress = 0f,
                synthesisStatusLabel = "",
            )
        }
    }

    private fun handleAiVoiceOutcome(outcome: AiVoiceSynthOutcome) {
        val isEn = _uiState.value.language == AppLanguage.EN
        when (outcome) {
            is AiVoiceSynthOutcome.Success -> {
                val oldPath = _aiVoiceUiState.value.lastOutputWavPath
                if (!oldPath.isNullOrBlank() && oldPath != outcome.audioFilePath) {
                    runCatching { File(oldPath).delete() }
                }
                _aiVoiceUiState.update {
                    it.copy(
                        isSynthesizing = false,
                        synthesisProgress = 1f,
                        synthesisStatusLabel = "جاهز للاستماع والحفظ",
                        lastOutputWavPath = outcome.audioFilePath,
                        lastOutputDurationMs = outcome.durationMs,
                        lastOutputSampleRate = outcome.sampleRate,
                        lastOutputEngine = outcome.engineUsed,
                        lastOutputAppliedPrompt = outcome.appliedPromptSummary,
                        playbackPositionMs = 0L,
                        isPlayingOutput = false,
                        errorTitle = null,
                        errorDetail = null,
                        setupRequiredGuide = null,
                    )
                }
                postBanner("تم توليد المقطع الصوتي بنجاح (${formatDurationMs(outcome.durationMs)})")
                notifyAudioCompletionIfBackground(
                    title = if (isEn) "Audio generation complete" else "اكتمل توليد المقطع الصوتي",
                    message = File(outcome.audioFilePath).name,
                )
            }
            is AiVoiceSynthOutcome.ConfigurationRequired -> {
                _aiVoiceUiState.update {
                    it.copy(
                        isSynthesizing = false,
                        synthesisProgress = 0f,
                        synthesisStatusLabel = "",
                        errorTitle = if (isEn) outcome.titleEn else outcome.titleAr,
                        errorDetail = if (isEn) outcome.explanationEn else outcome.explanationAr,
                        setupRequiredGuide = if (isEn) outcome.setupStepsEn else outcome.setupStepsAr,
                    )
                }
            }
            is AiVoiceSynthOutcome.Error -> {
                val detail = buildString {
                    append(if (isEn) outcome.messageEn else outcome.messageAr)
                    if (!outcome.technicalDetail.isNullOrBlank()) {
                        append("\n\n")
                        append(outcome.technicalDetail)
                    }
                }
                _aiVoiceUiState.update {
                    it.copy(
                        isSynthesizing = false,
                        synthesisProgress = 0f,
                        synthesisStatusLabel = "",
                        errorTitle = "تعذر إتمام التوليد (${outcome.engine.modelRepoId})",
                        errorDetail = detail,
                        setupRequiredGuide = null,
                    )
                }
            }
        }
    }

    fun playOrPauseAiVoiceOutput() {
        val path = _aiVoiceUiState.value.lastOutputWavPath
        if (path.isNullOrBlank() || !File(path).exists()) {
            postBanner("لا يوجد ملف صوتي مولّد للتشغيل بعد")
            return
        }
        val file = File(path)
        if (toolPreviewPlaybackFile?.absolutePath == file.absolutePath) {
            if (_playbackTelemetry.value.isPlaying) {
                player.pause()
            } else {
                if (_playbackTelemetry.value.positionMs >= toolPreviewFileDurationMs) {
                    player.seekToFraction(0f)
                }
                ExclusiveAudioController.onBeforeStudioPlayerPlay(player)
                if (!player.play()) postBanner("تعذر تشغيل الصوت المولّد")
            }
            return
        }

        viewModelScope.launch(safeCoroutineHandler) {
            if (!startGlobalFilePreview(file)) {
                postBanner("تعذر تشغيل الصوت المولّد")
            }
        }
    }

    fun stopAiVoiceOutput() {
        AudioPlayerManager.stop("ai_voice_preview")
        if (toolPreviewPlaybackFile != null) stopToolPreview()
        _aiVoiceUiState.update { it.copy(isPlayingOutput = false, playbackPositionMs = 0L) }
    }

    fun saveAiVoiceOutputToDevice() {
        val path = _aiVoiceUiState.value.lastOutputWavPath ?: return
        val file = File(path)
        if (!file.exists() || file.length() <= 44L) {
            postBanner("يرجى توليد صوت أولاً قبل الحفظ")
            return
        }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        dispatchSaveToDevice(
            result = ExportedAudioResult(file = file, extension = "wav", mimeType = "audio/wav"),
            suggestedName = "Sonix_AiVoice_$stamp.wav",
        )
    }

    fun shareAiVoiceOutput() {
        val app = getApplication<Application>()
        val path = _aiVoiceUiState.value.lastOutputWavPath ?: return
        val file = File(path)
        if (!file.exists() || file.length() <= 44L) {
            postBanner("يرجى توليد صوت أولاً قبل المشاركة")
            return
        }
        val uri = runCatching {
            FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
        }.getOrNull() ?: return
        _sideEffects.tryEmit(
            StudioSideEffect.LaunchShareIntent(
                uri = uri,
                mimeType = "audio/wav",
                title = "AI Voice Studio Output",
            ),
        )
    }

    fun insertAiVoiceOutputIntoMainStudio(appendToExisting: Boolean = false) {
        val path = _aiVoiceUiState.value.lastOutputWavPath ?: return
        val file = File(path)
        if (!file.exists() || file.length() <= 44L) {
            postBanner("لا يوجد صوت مولّد لإدراجه في الاستوديو")
            return
        }
        viewModelScope.launch(safeCoroutineHandler) {
            val app = getApplication<Application>()
            val decoded = withContext(Dispatchers.IO) {
                AiVoiceAudioStitcher.readSimplePcmWav(file) ?: AudioDecoder.decodeFile(file)
            }
            if (decoded == null || decoded.frameCount <= 0) {
                postBanner("تعذر قراءة الملف المولّد لإدراجه")
                return@launch
            }
            val existing = activeBuffer
            val shouldAppend = appendToExisting && existing != null
            val finalBuf = if (shouldAppend) {
                mergeClipsStreamingOnDisk(app, existing!!, decoded, crossfadeMs = 15)
            } else {
                decoded
            }
            setActiveBuffer(
                buf = finalBuf,
                recordUndo = existing != null,
                resetDspOnNewEntry = existing == null && !shouldAppend,
            )
            repository.indexAudioRecording(
                title = "AI Voice Studio • ${_aiVoiceUiState.value.lastOutputEngine?.titleAr ?: "Studio"}",
                buffer = decoded,
                sourceType = RecordingSourceType.TTS,
            )
            postBanner("تم إدراج الصوت المولّد داخل مشروع Sonix الرئيسي بنجاح")
        }
    }

    // =========================================================================
    // DEVELOPER & ADMIN PANEL (Temporary Owner-Only Section via BuildConfig flag)
    // =========================================================================

    fun selectDeveloperTab(tab: DeveloperPanelTab) = developerPanelController.selectTab(tab)
    fun selectDeveloperTestEngine(engineId: String) = developerPanelController.selectTestEngine(engineId)
    fun refreshDeveloperPanel() = developerPanelController.verifyAndRefreshAll()
    fun clearDeveloperFeedback() = developerPanelController.clearFeedback()

    fun adjustDeveloperCredits(
        targetUserId: String,
        targetEmail: String,
        creditType: CreditType,
        isGrant: Boolean,
        amount: Long,
        reason: String,
    ) = developerPanelController.adjustUserCredits(targetUserId, targetEmail, creditType, isGrant, amount, reason)

    fun refundDeveloperTransaction(record: CreditTransactionRecord) =
        developerPanelController.refundTransaction(record)

    fun recordDeveloperPayment(
        userId: String,
        paymentType: PaymentType,
        productId: String,
        amountUsd: Double,
        status: PaymentStatus,
        provider: String,
    ) = developerPanelController.recordPayment(userId, paymentType, productId, amountUsd, status, provider)

    fun recordDeveloperFinanceEntry(
        entryType: FinanceEntryType,
        source: String,
        amountUsd: Double,
        periodLabel: String,
        notes: String,
    ) = developerPanelController.recordFinanceEntry(entryType, source, amountUsd, periodLabel, notes)

    fun deleteDeveloperFinanceEntry(docId: String) = developerPanelController.deleteFinanceEntry(docId)

    fun saveDeveloperPartnerConfig(partnerSharePercent: Double, partnerName: String) =
        developerPanelController.savePartnerShareConfig(partnerSharePercent, partnerName)

    fun addDeveloperChangelog(
        title: String,
        category: DevChangelogCategory,
        status: DevChangelogStatus,
        details: String,
        changeDate: String,
    ) = developerPanelController.addDevChangelog(title, category, status, details, changeDate)

    fun deleteDeveloperChangelog(docId: String) = developerPanelController.deleteDevChangelog(docId)

    fun runDeveloperTest(testType: AdminTestType) = developerPanelController.runDiagnosticTest(testType)

    override fun onCleared() {
        super.onCleared()
        developerPanelController.stopCollectors()
        if (FirebaseApp.getApps(getApplication()).isNotEmpty()) {
            runCatching { Firebase.auth.removeAuthStateListener(authStateListener) }
        }
        sliderDebounceJob?.cancel()
        activeOnlineJob?.cancel()
        aiVoiceSynthJob?.cancel()
        cloudPresetsJob?.cancel()
        cloudDraftsJob?.cancel()
        sleepTimerJob?.cancel()
        batchProcessingJob?.cancel()
        smartMixPreviewJob?.cancel()
        deleteTempPreviewFile()
        replaceActiveSourceFile(null)
        replaceMixBgSourceFile(null)
        voiceProcessor.stop()
        ExclusiveAudioController.stopAllExclusive()
        player.dispose()
        recorder.discard()
        stopToolPreview()
        stopMixPreviewPlayer()
        stopOnlineSoundPreview()
        releaseTtsPreviewPlayer()
        AudioPlayerManager.releaseAll()
        speechTranscriber.destroy()
        SmartCacheManager.trimSessionCache(getApplication(), keepCount = 5)
        SonixMediaPlaybackService.stopService(getApplication())
    }
}
