package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.aivoice.AiDialogueTurn
import com.example.audio.aivoice.AiBreathCadence
import com.example.audio.aivoice.AiArabicDialect
import com.example.audio.aivoice.AiEnglishAccent
import com.example.audio.aivoice.AiPitchContour
import com.example.audio.aivoice.AiStudioPreset
import com.example.audio.aivoice.AiVoiceCharacter
import com.example.audio.aivoice.AiVoiceEmotion
import com.example.audio.aivoice.AiVoiceEngineKind
import com.example.audio.aivoice.AiVoiceLanguage
import com.example.audio.aivoice.AiVoiceStudioUiState
import com.example.audio.aivoice.AiVoiceTimbre
import com.example.audio.aivoice.AiVocalStyle
import com.example.audio.tts.TtsTextLimits
import com.example.i18n.t
import com.example.model.AppLanguage
import com.example.ui.components.StudioFocusTopBar
import com.example.ui.theme.StudioColors
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel
import java.util.concurrent.atomic.AtomicReference

@Composable
fun AiVoiceStudioScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.aiVoiceUiState.collectAsStateWithLifecycle()
    val lang = uiState.language
    val isEnglish = lang == AppLanguage.EN || lang == AppLanguage.TR
    val latestScriptDraft = remember { AtomicReference(state.scriptText) }
    LaunchedEffect(state.scriptText) {
        latestScriptDraft.set(state.scriptText)
    }
    val commitLatestScriptDraft = {
        viewModel.setAiVoiceScriptText(TtsTextLimits.limitWords(latestScriptDraft.get()))
    }

    BackHandler(
        onBack = {
            commitLatestScriptDraft()
            onBack()
        },
    )

    val pickReferenceAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importAiVoiceReferenceFromUri(uri)
        }
    }

    var synthesizedSignature by rememberSaveable { mutableStateOf<String?>(null) }
    var advancedSubTab by rememberSaveable { mutableIntStateOf(0) } // 0=Dialogue, 1=Clone & Save
    val isDialogueMode = state.activeTab == 1 && advancedSubTab == 0

    fun buildInputSignature(scriptText: String): String = buildString {
        append(state.activeTab).append('|')
        append(advancedSubTab).append('|')
        append(state.selectedEngine.id).append('|')
        append(state.selectedLanguage.code).append('|')
        append(state.selectedDialect.id).append('|')
        append(state.selectedEnglishAccent.id).append('|')
        append(state.selectedCategory.id).append('|')
        append(state.selectedCharacterId).append('|')
        append(state.fixedSid).append('|')
        append(state.fixedSeed).append('|')
        append(state.selectedEmotion.id).append('|')
        append(state.selectedVocalStyle.id).append('|')
        append(state.selectedBreathCadence.id).append('|')
        append(state.selectedPitchContour.id).append('|')
        append(state.selectedTimbre.id).append('|')
        append(state.articulationClarity).append('|')
        append(state.selectedStudioPreset.id).append('|')
        append(state.speechRate).append('|')
        append(state.emotionIntensity).append('|')
        append(state.pitchOffsetSemitones).append('|')
        append(state.fixedFormantShift).append('|')
        append(state.fixedEqLowDb).append('|')
        append(state.fixedEqPresenceDb).append('|')
        append(state.voiceDescriptionPrompt).append('|')
        append(scriptText).append('|')
        append(state.referenceAudioPath.orEmpty()).append('|')
        append(state.referenceTranscript).append('|')
        append(state.referenceRightsConfirmed).append('|')
        append(state.backendServerUrl).append('|')
        append(
            state.dialogueTurns.joinToString(";") { turn ->
                "${turn.id}:${turn.characterId}:${turn.text}:${turn.emotion.id}:${turn.emotionIntensity}:${turn.pace.id}:${turn.pauseAfterMs}"
            },
        )
    }
    AiVoiceStudioContentLayout(
        state = state,
        language = lang,
        hasStudioAudio = uiState.hasAudio,
        advancedSubTab = advancedSubTab,
        onSelectAdvancedSubTab = { advancedSubTab = it.coerceIn(0, 2) },
        onBack = {
            commitLatestScriptDraft()
            onBack()
        },
        onSelectTab = { tab ->
            commitLatestScriptDraft()
            viewModel.setAiVoiceTab(tab)
        },
        onDismissError = viewModel::dismissAiVoiceError,
        onRetryCloud = {
            if (isDialogueMode) {
                viewModel.synthesizeAiVoiceDialogue(AiVoiceEngineKind.VOXCPM_2)
            } else {
                viewModel.setAiVoiceScriptText(TtsTextLimits.limitWords(latestScriptDraft.get()))
                viewModel.synthesizeAiVoiceSingle(AiVoiceEngineKind.VOXCPM_2)
            }
        },
        onPlayPause = viewModel::playOrPauseAiVoiceOutput,
        onReplay = viewModel::replayAiVoiceOutput,
        onStopPlayback = viewModel::stopAiVoiceOutput,
        onSeekMs = viewModel::seekAiVoiceOutput,
        onSaveWav = viewModel::saveAiVoiceOutputToDevice,
        onShare = viewModel::shareAiVoiceOutput,
        onInsertIntoStudio = { viewModel.insertAiVoiceOutputIntoMainStudio(false) },
        onScriptChange = { nextText ->
            latestScriptDraft.set(nextText)
            if (nextText.isBlank()) {
                synthesizedSignature = null
            }
            viewModel.setAiVoiceScriptText(nextText)
        },
        onScriptDraftChange = { nextText ->
            latestScriptDraft.set(nextText)
            if (nextText.isBlank()) synthesizedSignature = null
        },
        onSelectLanguage = viewModel::setAiVoiceLanguage,
        onSelectDialect = viewModel::setAiVoiceDialect,
        onSelectCharacter = viewModel::selectAiVoiceCharacter,
        onToggleFavorite = viewModel::toggleAiVoiceCharacterFavorite,
        onSelectEmotion = viewModel::setAiVoiceEmotion,
        onSelectVocalStyle = viewModel::setAiVocalStyle,
        onSpeechRateChange = viewModel::setAiVoiceSpeechRate,
        onIntensityChange = viewModel::setAiVoiceEmotionIntensity,
        onSelectStudioPreset = viewModel::setAiVoiceStudioPreset,
        onSelectBreathCadence = viewModel::setAiBreathCadence,
        onSelectPitchContour = viewModel::setAiPitchContour,
        onSelectTimbre = viewModel::setAiVoiceTimbre,
        onArticulationClarityChange = viewModel::setAiArticulationClarity,
        onResetTuning = viewModel::resetAiVoiceTuning,
        onPickReferenceFile = { pickReferenceAudioLauncher.launch(arrayOf("audio/*")) },
        onUseStudioBufferAsReference = viewModel::importAiVoiceReferenceFromCurrentBuffer,
        onClearReference = viewModel::clearAiVoiceReference,
        onReferenceTranscriptChange = viewModel::setAiVoiceReferenceTranscript,
        onRightsConfirmedChange = viewModel::setAiVoiceReferenceRightsConfirmed,
        onAddDialogueTurn = { viewModel.addDialogueTurn() },
        onRemoveDialogueTurn = viewModel::removeDialogueTurn,
        onMoveDialogueTurn = viewModel::moveDialogueTurn,
        onUpdateDialogueTurn = viewModel::updateDialogueTurn,
        onSynthesizeDialogue = viewModel::synthesizeAiVoiceDialogue,
        onApplyCharacterToStudio = { ch ->
            viewModel.selectAiVoiceCharacter(ch)
            viewModel.setAiVoiceTab(0)
        },
        onAddCharacterToDialogue = { ch ->
            viewModel.addDialogueTurn(ch.id)
            advancedSubTab = 0
            viewModel.setAiVoiceTab(1)
        },
        onSaveCustomCharacter = viewModel::saveCustomAiVoiceCharacter,
        onDeleteSavedCharacter = viewModel::deleteSavedAiVoiceCharacter,
        onSaveBackendUrl = viewModel::saveAiVoiceBackendUrl,
        onSaveBackendToken = viewModel::saveAiVoiceBackendBearer,
        onCheckHealth = viewModel::checkAiVoiceBackendHealth,
        onPreviewClick = {
            val latestText = TtsTextLimits.limitWords(latestScriptDraft.get())
            val currentSignature = buildInputSignature(latestText)
            val outputExists = !state.lastOutputWavPath.isNullOrBlank()
            if (outputExists && synthesizedSignature == currentSignature) {
                viewModel.playOrPauseAiVoiceOutput()
            } else {
                synthesizedSignature = currentSignature
                if (isDialogueMode) {
                    viewModel.synthesizeAiVoiceDialogue()
                } else {
                    viewModel.setAiVoiceScriptText(latestText)
                    viewModel.synthesizeAiVoiceSingle()
                }
            }
        },
        onCancelSynthesis = viewModel::cancelAiVoiceSynthesis,
        onApproveClick = {
            val latestText = TtsTextLimits.limitWords(latestScriptDraft.get())
            val currentSignature = buildInputSignature(latestText)
            val hasValidText = if (isDialogueMode) {
                state.dialogueTurns.any { it.text.isNotBlank() }
            } else {
                latestText.isNotBlank()
            }
            val hasFreshOutput = hasValidText &&
                !state.lastOutputWavPath.isNullOrBlank() &&
                synthesizedSignature == currentSignature
            if (hasFreshOutput) {
                viewModel.insertAiVoiceOutputIntoMainStudio(false)
            } else {
                synthesizedSignature = currentSignature
                if (isDialogueMode) {
                    viewModel.synthesizeAiVoiceDialogue()
                } else {
                    viewModel.setAiVoiceScriptText(latestText)
                    viewModel.synthesizeAiVoiceSingle()
                }
                if (hasValidText) {
                    viewModel.showStatusBanner(
                        if (isEnglish) {
                            "Generating voice for current text — tap Approve once ready to insert."
                        } else {
                            "جاري توليد الصوت للنص الحالي — اضغط اعتماد بعد الجاهزية لإدراجه بالاستوديو."
                        },
                    )
                }
            }
        },
        onSelectEnglishAccent = viewModel::setAiVoiceEnglishAccent,
        onPrepareCloudPack = viewModel::prepareAiVoiceCloudPack,
    )
}

@Composable
fun AiVoiceStudioContentLayout(
    state: AiVoiceStudioUiState,
    language: AppLanguage,
    hasStudioAudio: Boolean,
    advancedSubTab: Int = 0,
    onSelectAdvancedSubTab: (Int) -> Unit = {},
    onBack: () -> Unit,
    onSelectTab: (Int) -> Unit,
    onDismissError: () -> Unit,
    onRetryCloud: () -> Unit,
    onPlayPause: () -> Unit,
    onReplay: () -> Unit,
    onStopPlayback: () -> Unit,
    onSeekMs: (Long) -> Unit,
    onSaveWav: () -> Unit,
    onShare: () -> Unit,
    onInsertIntoStudio: () -> Unit,
    onScriptChange: (String) -> Unit,
    onScriptDraftChange: (String) -> Unit = {},
    onSelectLanguage: (AiVoiceLanguage) -> Unit,
    onSelectDialect: (AiArabicDialect) -> Unit = {},
    onSelectEnglishAccent: (AiEnglishAccent) -> Unit = {},
    onSelectCharacter: (AiVoiceCharacter) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSelectEmotion: (AiVoiceEmotion) -> Unit,
    onSelectVocalStyle: (AiVocalStyle) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onIntensityChange: (Float) -> Unit,
    onSelectStudioPreset: (AiStudioPreset) -> Unit,
    onSelectBreathCadence: (AiBreathCadence) -> Unit,
    onSelectPitchContour: (AiPitchContour) -> Unit,
    onSelectTimbre: (AiVoiceTimbre) -> Unit,
    onArticulationClarityChange: (Float) -> Unit,
    onResetTuning: () -> Unit,
    onPickReferenceFile: () -> Unit,
    onUseStudioBufferAsReference: () -> Unit,
    onClearReference: () -> Unit,
    onReferenceTranscriptChange: (String) -> Unit,
    onRightsConfirmedChange: (Boolean) -> Unit,
    onAddDialogueTurn: () -> Unit,
    onRemoveDialogueTurn: (String) -> Unit,
    onMoveDialogueTurn: (String, Int) -> Unit,
    onUpdateDialogueTurn: (String, (AiDialogueTurn) -> AiDialogueTurn) -> Unit,
    onSynthesizeDialogue: (AiVoiceEngineKind?) -> Unit,
    onApplyCharacterToStudio: (AiVoiceCharacter) -> Unit,
    onAddCharacterToDialogue: (AiVoiceCharacter) -> Unit,
    onSaveCustomCharacter: (String, String) -> Unit,
    onDeleteSavedCharacter: (String) -> Unit,
    onSaveBackendUrl: (String) -> Unit = {},
    onSaveBackendToken: (String) -> Unit = {},
    onCheckHealth: (String, String) -> Unit = { _, _ -> },
    onPrepareCloudPack: () -> Unit = {},
    onPreviewClick: () -> Unit,
    onCancelSynthesis: () -> Unit,
    onApproveClick: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
) {
    val isEnglish = language == AppLanguage.EN || language == AppLanguage.TR

    Scaffold(
        containerColor = StudioColors.BgTop,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AiVoiceStickyBottomActionBar(
                state = state,
                hasStudioAudio = hasStudioAudio,
                isEnglish = isEnglish,
                onPreviewClick = onPreviewClick,
                onCancelSynthesis = onCancelSynthesis,
                onApproveClick = onApproveClick,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 700.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StudioFocusTopBar(
                    title = if (isEnglish) "AI VOICE STUDIO" else "AI VOICE STUDIO – استوديو الصوت",
                    subtitle = if (isEnglish) "VoxCPM2 • Neural Cloud TTS" else "VoxCPM2 • تحويل عصبي سحابي",
                    language = language,
                    onBack = onBack,
                )

                val tabs = listOf(
                    t(language, "ai_voice_tab_studio"),
                    t(language, "ai_voice_tab_advanced"),
                )
                val safeActiveTab = state.activeTab.coerceIn(0, tabs.lastIndex)

                TabRow(
                    selectedTabIndex = safeActiveTab,
                    containerColor = StudioColors.CardSurface,
                    contentColor = StudioColors.Cyan,
                    indicator = { tabPositions ->
                        if (safeActiveTab in tabPositions.indices) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[safeActiveTab]),
                                color = StudioColors.Cyan,
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("ai_voice_studio_tabs"),
                ) {
                    tabs.forEachIndexed { idx, label ->
                        Tab(
                            selected = safeActiveTab == idx,
                            onClick = { onSelectTab(idx) },
                            text = {
                                Text(
                                    text = label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (safeActiveTab == idx) FontWeight.Bold else FontWeight.Medium,
                                )
                            },
                            modifier = Modifier.testTag("ai_voice_tab_$idx"),
                        )
                    }
                }

                if (safeActiveTab == 0) {
                    AiVoiceGroup1TextCard(
                        scriptText = state.scriptText,
                        isEnglish = isEnglish,
                        onScriptChange = onScriptChange,
                        onScriptDraftChange = onScriptDraftChange,
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("ai_voice_scroll_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    if (!state.cloudVoicePackReady) {
                        item(key = "cloud_voice_pack_top") {
                            AiVoiceCloudPackCard(
                                state = state,
                                isEnglish = isEnglish,
                                onPrepare = onPrepareCloudPack,
                            )
                        }
                    }

                    if (!state.errorTitle.isNullOrBlank()) {
                        item(key = "status_banner") {
                            AiVoiceConfigurationBanner(
                                state = state,
                                isEnglish = isEnglish,
                                onDismiss = onDismissError,
                                onRetryCloud = onRetryCloud,
                            )
                        }
                    }

                    if (!state.lastOutputWavPath.isNullOrBlank()) {
                        item(key = "output_player") {
                            AiVoiceOutputPlayerCard(
                                state = state,
                                isEnglish = isEnglish,
                                onPlayPause = onPlayPause,
                                onReplay = onReplay,
                                onStop = onStopPlayback,
                                onSeekMs = onSeekMs,
                                onSaveWav = onSaveWav,
                                onShare = onShare,
                                onInsertIntoStudio = onInsertIntoStudio,
                            )
                        }
                    }

                    when (safeActiveTab) {
                        0 -> {
                            item(key = "card_2_voice") {
                                AiVoiceGroup2VoiceSelectionCard(
                                    state = state,
                                    isEnglish = isEnglish,
                                    onSelectLanguage = onSelectLanguage,
                                    onSelectDialect = onSelectDialect,
                                    onSelectEnglishAccent = onSelectEnglishAccent,
                                    onSelectCharacter = onSelectCharacter,
                                    onToggleFavorite = onToggleFavorite,
                                )
                            }
                            item(key = "card_3_emotion") {
                                AiVoiceGroup3EmotionCard(
                                    state = state,
                                    isEnglish = isEnglish,
                                    onSelectEmotion = onSelectEmotion,
                                    onSelectVocalStyle = onSelectVocalStyle,
                                )
                            }
                            item(key = "card_4_controls") {
                                AiVoiceGroup4ControlsCard(
                                    state = state,
                                    isEnglish = isEnglish,
                                    onSpeechRateChange = onSpeechRateChange,
                                    onIntensityChange = onIntensityChange,
                                    onSelectStudioPreset = onSelectStudioPreset,
                                    onSelectBreathCadence = onSelectBreathCadence,
                                    onSelectPitchContour = onSelectPitchContour,
                                    onSelectTimbre = onSelectTimbre,
                                    onArticulationClarityChange = onArticulationClarityChange,
                                    onResetTuning = onResetTuning,
                                )
                            }
                        }
                        else -> {
                            item(key = "advanced_sub_switcher") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        val subItems = listOf(
                                            0 to (if (isEnglish) "Dialogue" else "الحوار المتعدد"),
                                            1 to (if (isEnglish) "Voice Cloning" else "استنساخ الصوت"),
                                            2 to (if (isEnglish) "Cloud Engine" else "المحرك السحابي"),
                                        )
                                        subItems.forEach { (idx, title) ->
                                            val selected = advancedSubTab == idx
                                            Surface(
                                                onClick = { onSelectAdvancedSubTab(idx) },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (selected) StudioColors.Cyan.copy(alpha = 0.16f) else StudioColors.CardSurface,
                                                border = BorderStroke(
                                                    width = if (selected) 1.5.dp else 1.dp,
                                                    color = if (selected) StudioColors.Cyan else StudioColors.Border,
                                                ),
                                                modifier = Modifier
                                                    .widthIn(min = 112.dp)
                                                    .defaultMinSize(minHeight = 48.dp)
                                                    .testTag("ai_voice_advanced_sub_$idx"),
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                                ) {
                                                    Text(
                                                        text = title,
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (selected) StudioColors.Cyan else StudioColors.TextPrimary,
                                                        maxLines = 2,
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                    )
                                                }
                                            }

                                        }
                                    }
                                }

                            }
                            when (advancedSubTab) {
                                0 -> {
                                    item(key = "dialogue_studio") {
                                        AiVoiceDialogueTab(
                                            state = state,
                                            isEnglish = isEnglish,
                                            onAddTurn = onAddDialogueTurn,
                                            onRemoveTurn = onRemoveDialogueTurn,
                                            onMoveTurn = onMoveDialogueTurn,
                                            onUpdateTurn = onUpdateDialogueTurn,
                                            onSynthesizeDialogue = onSynthesizeDialogue,
                                            onCancelSynthesis = onCancelSynthesis,
                                        )
                                    }

                                }
                                1 -> {
                                    item(key = "characters_and_cloning") {
                                        AiVoiceCharactersTab(
                                            state = state,
                                            hasStudioAudio = hasStudioAudio,
                                            isEnglish = isEnglish,
                                            onApplyCharacterToStudio = onApplyCharacterToStudio,
                                            onAddCharacterToDialogue = onAddCharacterToDialogue,
                                            onSaveCurrentAsCharacter = onSaveCustomCharacter,
                                            onDeleteSavedCharacter = onDeleteSavedCharacter,
                                            onPickReferenceFile = onPickReferenceFile,
                                            onUseStudioBufferAsReference = onUseStudioBufferAsReference,
                                            onClearReference = onClearReference,
                                            onScriptChange = onScriptChange,
                                            onReferenceTranscriptChange = onReferenceTranscriptChange,
                                            onRightsConfirmedChange = onRightsConfirmedChange,
                                            onSaveOutputWav = onSaveWav,
                                            onShareOutput = onShare,
                                        )
                                    }
                                }
                                else -> {
                                    item(key = "server_setup") {
                                        AiVoiceServerSetupTab(
                                            state = state,
                                            isEnglish = isEnglish,
                                            onSaveBackendUrl = onSaveBackendUrl,
                                            onSaveBackendToken = onSaveBackendToken,
                                            onCheckHealth = onCheckHealth,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (state.cloudVoicePackReady) {
                        item(key = "cloud_voice_pack_bottom") {
                            AiVoiceCloudPackCard(
                                state = state,
                                isEnglish = isEnglish,
                                onPrepare = onPrepareCloudPack,
                            )
                        }
                    }
                }
            }
        }
    }
}
