package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SpatialAudioOff
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.audio.OnlineGuard
import com.example.audio.aivoice.AiArabicDialect
import com.example.audio.aivoice.AiEnglishAccent
import com.example.audio.aivoice.AiVoiceLanguage
import com.example.audio.fx.VoiceEffect
import com.example.audio.tts.TtsTextLimits
import com.example.i18n.t
import com.example.model.AiToolType
import com.example.model.AppLanguage
import com.example.model.AuthSession
import com.example.model.TTS_DIALECTS
import com.example.model.TTS_VOICE_PERSONAS
import com.example.model.TtsDeliveryStyle
import com.example.model.TtsGender
import com.example.model.TtsPersonaAcousticMode
import com.example.model.TtsVoicePersona
import com.example.studio.ui.lit
import com.example.ui.components.InfoBadgeButton
import com.example.ui.components.LanguageSelectorDialog
import com.example.ui.components.MicrophonePermissionRationaleDialog
import com.example.ui.components.SaveDraftDialog
import com.example.ui.components.StudioFocusTopBar
import com.example.ui.components.StudioParamSlider
import com.example.ui.components.StudioPreviewControlsBar
import com.example.ui.components.StudioScrollColumn
import com.example.ui.components.StudioSectionCard
import com.example.ui.components.ToolInfoDialog
import com.example.ui.components.UndoRedoActionsRow
import com.example.ui.components.rememberEditHistory
import com.example.ui.theme.StudioColors
import com.example.ui.theme.StudioDropdownMenuTheme
import com.example.ui.theme.TelemetryTextStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PlaybackTelemetry
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel
import com.example.viewmodel.AccountDeletionResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

private data class TtsEditorSnapshot(
    val textInput: String,
    val dialectId: String,
    val gender: TtsGender,
    val style: TtsDeliveryStyle,
    val rateOffsetPercent: Float,
    val pitchOffsetPercent: Float,
    val personaId: String? = null,
    val acousticMode: TtsPersonaAcousticMode = TtsPersonaAcousticMode.NATURAL,
)

private data class VoiceTransformSnapshot(
    val pitchSemitones: Float,
    val formantShift: Float,
    val warmthDb: Float,
    val presenceDb: Float,
    val mix: Float,
    val personaId: String? = null,
    val acousticMode: TtsPersonaAcousticMode = TtsPersonaAcousticMode.NATURAL,
)

@Composable
fun AiStudioScreen(
    uiState: StudioUiState,
    onOpenTool: (AiToolType) -> Unit,
    onBack: () -> Unit,
) {
    val lang = uiState.language

    StudioScrollColumn {
        StudioFocusTopBar(
            title = t(lang, "section_ai_title"),
            subtitle = t(lang, "section_ai_subtitle"),
            language = lang,
            onBack = onBack,
        )

        StudioSectionCard(
            title = if (lang == AppLanguage.EN) "Smart Audio Transcription" else "تفريغ الصوت إلى نص",
            subtitle = if (lang == AppLanguage.EN) "Convert voice clips into clean punctuated text" else "تحويل الكلام المسجل إلى نص عربي أو إنجليزي منسق",
            icon = Icons.Rounded.Subtitles,
            accentColor = StudioColors.Cyan,
            badgeText = "AI",
            testTag = "ai_card_transcribe",
            onClick = { onOpenTool(AiToolType.TRANSCRIBE) },
        )

        StudioSectionCard(
            title = if (lang == AppLanguage.EN) "Smart Auto-Master" else "مهندس الصوت الذكي (Auto-Master)",
            subtitle = if (lang == AppLanguage.EN) "Analyze RMS, peak, noise floor & optimize DSP chain" else "تحليل الطيف والضوضاء وضبط سلسلة المؤثرات تلقائياً",
            icon = Icons.Rounded.AutoAwesome,
            accentColor = StudioColors.Emerald,
            badgeText = "DSP",
            testTag = "ai_card_smart_master",
            onClick = { onOpenTool(AiToolType.SMART_MASTER) },
        )

        StudioSectionCard(
            title = if (lang == AppLanguage.EN) "Audio Knowledge Assistant" else "مساعد الهندسة الصوتية الذكي",
            subtitle = if (lang == AppLanguage.EN) "Ask mixing, acoustic & microphone placement questions" else "اسأل عن تقنيات المكساج، العزل الصوتي، والمايكات",
            icon = Icons.Rounded.Search,
            accentColor = StudioColors.Violet,
            badgeText = "Q&A",
            testTag = "ai_card_search",
            onClick = { onOpenTool(AiToolType.SEARCH) },
        )
    }
}

@Composable
fun AiToolDetailScreen(
    tool: AiToolType,
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onBack: () -> Unit,
) {
    val lang = uiState.language
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    val title = when (tool) {
        AiToolType.TRANSCRIBE -> if (lang == AppLanguage.EN) "Smart Audio Transcription" else "تفريغ الصوت إلى نص"
        AiToolType.SMART_MASTER -> if (lang == AppLanguage.EN) "Smart Auto-Master" else "مهندس الصوت الذكي"
        AiToolType.SEARCH -> if (lang == AppLanguage.EN) "Audio Knowledge Assistant" else "مساعد الهندسة الصوتية"
    }
    var searchQuery by remember { mutableStateOf("") }
    val searchHistory = rememberEditHistory("")

    StudioScrollColumn {
        StudioFocusTopBar(
            title = title,
            language = lang,
            onBack = onBack,
            trailing = {
                UndoRedoActionsRow(
                    canUndo = if (tool == AiToolType.SEARCH) searchHistory.canUndo else uiState.canUndoEdit,
                    canRedo = if (tool == AiToolType.SEARCH) searchHistory.canRedo else uiState.canRedoEdit,
                    language = lang,
                    undoTag = "ai_tool_undo_button",
                    redoTag = "ai_tool_redo_button",
                    onUndo = {
                        if (tool == AiToolType.SEARCH) {
                            searchHistory.undo()?.let { searchQuery = it }
                        } else {
                            viewModel.undoLastEdit()
                        }
                    },
                    onRedo = {
                        if (tool == AiToolType.SEARCH) {
                            searchHistory.redo()?.let { searchQuery = it }
                        } else {
                            viewModel.redoLastEdit()
                        }
                    },
                )
            },
        )

        when (tool) {
            AiToolType.TRANSCRIBE -> {
                val st = uiState.transcriptionState
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
                    border = BorderStroke(1.dp, StudioColors.Border),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val startInteraction = remember { MutableInteractionSource() }
                            Button(
                                onClick = { viewModel.transcribeActiveAudio() },
                                enabled = !st.isLoading,
                                interactionSource = startInteraction,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StudioColors.Cyan,
                                    contentColor = StudioColors.BgTop,
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .lit(startInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                                    .testTag("ai_transcribe_start_button"),
                            ) {
                                if (st.isLoading) {
                                    CircularProgressIndicator(
                                        color = StudioColors.BgTop,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(if (lang == AppLanguage.EN) "Transcribe Studio Audio" else "بدء تفريغ الصوت")
                            }

                            val clearInteraction = remember { MutableInteractionSource() }
                            OutlinedButton(
                                onClick = { viewModel.clearTranscription() },
                                interactionSource = clearInteraction,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .lit(clearInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                                    .testTag("ai_transcribe_clear_button"),
                            ) {
                                Icon(Icons.Rounded.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }

                        if (st.isLoading) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                uiState.transcriptionProgressLabel?.takeIf { it.isNotBlank() }?.let { progLabel ->
                                    Text(
                                        text = progLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StudioColors.Cyan,
                                    )
                                }
                                val cancelInt = remember { MutableInteractionSource() }
                                OutlinedButton(
                                    onClick = { viewModel.cancelActiveOnlineRequest() },
                                    interactionSource = cancelInt,
                                    border = BorderStroke(1.dp, StudioColors.RoseBright),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioColors.RoseBright),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 48.dp)
                                        .lit(cancelInt, StudioColors.RoseBright, cornerRadius = 12.dp)
                                        .testTag("ai_transcribe_cancel_button"),
                                ) {
                                    Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(t(lang, "cancel_request"))
                                }
                            }
                        }

                        if (!st.errorMessage.isNullOrBlank()) {
                            AiStatusCard(message = st.errorMessage, isAmber = true)
                        }

                        if (st.outputText.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = StudioColors.BgMid,
                                border = BorderStroke(1.dp, StudioColors.BorderBright),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = if (lang == AppLanguage.EN) "Transcript" else "النص المفرّغ",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = StudioColors.Cyan,
                                        )
                                        val copyInteraction = remember { MutableInteractionSource() }
                                        IconButton(
                                            onClick = { clipboard.setText(AnnotatedString(st.outputText)) },
                                            interactionSource = copyInteraction,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .lit(copyInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 24.dp)
                                                .testTag("ai_transcribe_copy_button"),
                                        ) {
                                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = StudioColors.Cyan)
                                        }
                                    }
                                    Text(
                                        text = st.outputText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = StudioColors.TextPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AiToolType.SMART_MASTER -> {
                val analysis = uiState.smartMasterState
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
                    border = BorderStroke(1.dp, StudioColors.Border),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val masterInteraction = remember { MutableInteractionSource() }
                        Button(
                            onClick = { viewModel.runSmartAutoMaster() },
                            enabled = !uiState.isBusy,
                            interactionSource = masterInteraction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioColors.Emerald,
                                contentColor = StudioColors.BgTop,
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 52.dp)
                                .lit(masterInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                                .testTag("ai_smart_master_run_button"),
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (lang == AppLanguage.EN) "Analyze & Auto-Master" else "تحليل الصوت وتطبيق الماسترينغ الذكي",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }

                        if (uiState.smartMasterAnalyzingAi) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CircularProgressIndicator(
                                    color = StudioColors.Cyan,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = if (lang == AppLanguage.EN) {
                                        "Analyzing the acoustic profile..."
                                    } else {
                                        "يجري تحليل البصمة الصوتية..."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StudioColors.Cyan,
                                )
                            }
                        }

                        if (analysis != null) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = StudioColors.ElevatedSurface,
                                border = BorderStroke(1.dp, StudioColors.Emerald.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = if (lang == AppLanguage.EN) "Detected Acoustic Profile" else "البصمة الصوتية المكتشفة",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioColors.Emerald,
                                    )
                                    Text(
                                        text = analysis.detectedProfile,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = StudioColors.TextPrimary,
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                MetricTile("RMS", "${"%.1f".format(analysis.rmsDb)} dB", StudioColors.Cyan, Modifier.weight(1f))
                                MetricTile("Peak", "${"%.1f".format(analysis.peakDb)} dB", StudioColors.Amber, Modifier.weight(1f))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                MetricTile("Crest", "${"%.1f".format(analysis.crestFactorDb)} dB", StudioColors.Violet, Modifier.weight(1f))
                                MetricTile("Noise", "${"%.1f".format(analysis.noiseFloorDb)} dB", StudioColors.Emerald, Modifier.weight(1f))
                            }

                            if (!analysis.recommendedFfmpegFilter.isNullOrBlank()) {
                                val applyFfmpegInt = remember { MutableInteractionSource() }
                                Button(
                                    onClick = { viewModel.applyAiEngineerFfmpegFilter() },
                                    enabled = !uiState.isBusy && !uiState.isProcessing && !analysis.ffmpegFilterApplied,
                                    interactionSource = applyFfmpegInt,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (analysis.ffmpegFilterApplied) StudioColors.Emerald else StudioColors.Violet,
                                        contentColor = StudioColors.BgTop,
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 48.dp)
                                        .lit(applyFfmpegInt, StudioColors.Violet, cornerRadius = 12.dp)
                                        .testTag("ai_smart_master_apply_ffmpeg_button"),
                                ) {
                                    Icon(
                                        imageVector = if (analysis.ffmpegFilterApplied) Icons.Rounded.CheckCircle else Icons.Rounded.GraphicEq,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (analysis.ffmpegFilterApplied) {
                                            if (lang == AppLanguage.EN) "Processing applied" else "تم تنفيذ المعالجة"
                                        } else {
                                            if (lang == AppLanguage.EN) "Apply sound adjustments" else "تنفيذ التعديلات الصوتية"
                                        },
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (lang == AppLanguage.EN) "Applied Optimizations" else "التعديلات المطبقة تلقائياً",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = StudioColors.TextPrimary,
                                )
                                analysis.appliedActions.forEach { act ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = StudioColors.Emerald,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            text = act,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = StudioColors.TextSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AiToolType.SEARCH -> {
                val st = uiState.searchAssistantState

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
                    border = BorderStroke(1.dp, StudioColors.Border),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                searchHistory.commit(it, debounceMs = 350L)
                            },
                            label = {
                                Text(if (lang == AppLanguage.EN) "Ask an audio engineering question" else "اكتب سؤالك في الهندسة الصوتية")
                            },
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StudioColors.Violet,
                                unfocusedBorderColor = StudioColors.Border,
                                focusedTextColor = StudioColors.TextPrimary,
                                unfocusedTextColor = StudioColors.TextPrimary,
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_search_query_input"),
                        )

                        val searchInteraction = remember { MutableInteractionSource() }
                        Button(
                            onClick = { viewModel.askSearchAssistant(searchQuery) },
                            enabled = searchQuery.isNotBlank() && !st.isLoading,
                            interactionSource = searchInteraction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioColors.Violet,
                                contentColor = StudioColors.BgTop,
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .lit(searchInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                                .testTag("ai_search_submit_button"),
                        ) {
                            if (st.isLoading) {
                                CircularProgressIndicator(
                                    color = StudioColors.BgTop,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(if (lang == AppLanguage.EN) "Ask Studio Assistant" else "بحث وإجابة الاستوديو")
                        }

                        if (st.isLoading) {
                            val cancelSearchInt = remember { MutableInteractionSource() }
                            OutlinedButton(
                                onClick = { viewModel.cancelActiveOnlineRequest() },
                                interactionSource = cancelSearchInt,
                                border = BorderStroke(1.dp, StudioColors.RoseBright),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioColors.RoseBright),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .lit(cancelSearchInt, StudioColors.RoseBright, cornerRadius = 12.dp)
                                    .testTag("ai_search_cancel_button"),
                            ) {
                                Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(t(lang, "cancel_request"))
                            }
                        }

                        if (!st.errorMessage.isNullOrBlank()) {
                            AiStatusCard(message = st.errorMessage, isAmber = true)
                        }

                        if (st.outputText.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = StudioColors.BgMid,
                                border = BorderStroke(1.dp, StudioColors.Border),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = st.outputText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StudioColors.TextPrimary,
                                    modifier = Modifier.padding(14.dp),
                                )
                            }
                        }

                        if (st.sources.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (lang == AppLanguage.EN) "Sources" else "المصادر",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = StudioColors.Cyan,
                                )
                                st.sources.forEach { src ->
                                    val srcInteraction = remember { MutableInteractionSource() }
                                    Surface(
                                        onClick = { runCatching { uriHandler.openUri(src.uri) } },
                                        interactionSource = srcInteraction,
                                        shape = RoundedCornerShape(12.dp),
                                        color = StudioColors.ElevatedSurface,
                                        border = BorderStroke(1.dp, StudioColors.Border),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 48.dp)
                                            .lit(srcInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 12.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(src.title, style = MaterialTheme.typography.labelLarge, color = StudioColors.TextPrimary)
                                                Text(src.domain, style = MaterialTheme.typography.bodySmall, color = StudioColors.Cyan)
                                            }
                                            Icon(Icons.Rounded.OpenInNew, contentDescription = src.title, tint = StudioColors.Cyan)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiStatusCard(message: String, isAmber: Boolean) {
    val accent = if (isAmber) StudioColors.Amber else StudioColors.Rose
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = StudioColors.TextPrimary,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = StudioColors.BgMid,
        border = BorderStroke(1.dp, StudioColors.Border),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = StudioColors.TextSecondary)
            Text(value, style = TelemetryTextStyle, color = color)
        }
    }
}

@Composable
fun TextToSpeechStudioScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onBack: () -> Unit,
) {
    val lang = uiState.language
    val context = LocalContext.current
    var selectedDialect by remember { mutableStateOf(TTS_DIALECTS.first()) }
    var selectedPersona by remember { mutableStateOf<TtsVoicePersona?>(TTS_VOICE_PERSONAS.first()) }
    var selectedGender by remember { mutableStateOf(TTS_VOICE_PERSONAS.first().gender) }
    var selectedStyle by remember { mutableStateOf(TTS_VOICE_PERSONAS.first().defaultStyle) }
    var selectedAcousticMode by remember { mutableStateOf(TTS_VOICE_PERSONAS.first().defaultAcousticMode) }
    var textInput by rememberSaveable { mutableStateOf(TTS_DIALECTS.first().sampleText) }
    var wordLimitExceeded by remember { mutableStateOf(false) }
    var rateOffsetPercent by remember { mutableFloatStateOf(0f) }
    var pitchOffsetPercent by remember { mutableFloatStateOf(0f) }
    var appendToExisting by remember { mutableStateOf(false) }
    var articleUrl by rememberSaveable { mutableStateOf("") }
    var infoOpen by remember { mutableStateOf(false) }
    var preferVmUndo by remember { mutableStateOf(false) }
    val ttsHistory = rememberEditHistory(
        TtsEditorSnapshot(
            textInput = textInput,
            dialectId = selectedDialect.id,
            gender = selectedGender,
            style = selectedStyle,
            rateOffsetPercent = rateOffsetPercent,
            pitchOffsetPercent = pitchOffsetPercent,
            personaId = selectedPersona?.id,
            acousticMode = selectedAcousticMode,
        ),
    )
    LaunchedEffect(Unit) {
        val limitedText = TtsTextLimits.limitWords(textInput)
        if (limitedText != textInput) {
            textInput = limitedText
            wordLimitExceeded = true
        }
    }

    fun currentTtsSnap(
        txt: String = textInput,
        dialectId: String = selectedDialect.id,
        gender: TtsGender = selectedGender,
        style: TtsDeliveryStyle = selectedStyle,
        rate: Float = rateOffsetPercent,
        pitch: Float = pitchOffsetPercent,
        personaId: String? = selectedPersona?.id,
        acousticMode: TtsPersonaAcousticMode = selectedAcousticMode,
    ) = TtsEditorSnapshot(
        textInput = txt,
        dialectId = dialectId,
        gender = gender,
        style = style,
        rateOffsetPercent = rate,
        pitchOffsetPercent = pitch,
        personaId = personaId,
        acousticMode = acousticMode,
    )

    fun applyTtsSnap(snap: TtsEditorSnapshot) {
        val limitedText = TtsTextLimits.limitWords(snap.textInput)
        textInput = limitedText
        wordLimitExceeded = limitedText != snap.textInput
        selectedDialect = TTS_DIALECTS.firstOrNull { it.id == snap.dialectId } ?: TTS_DIALECTS.first()
        selectedPersona = snap.personaId?.let { id -> TTS_VOICE_PERSONAS.firstOrNull { it.id == id } }
        selectedGender = snap.gender
        selectedStyle = snap.style
        selectedAcousticMode = snap.acousticMode
        rateOffsetPercent = snap.rateOffsetPercent
        pitchOffsetPercent = snap.pitchOffsetPercent
    }

    val effectiveSpeechRate = (selectedDialect.defaultRate * (1f + rateOffsetPercent / 100f)).coerceIn(0.55f, 1.65f)
    val effectiveSpeechPitch = (selectedDialect.defaultPitch * (1f + pitchOffsetPercent / 100f)).coerceIn(0.65f, 1.45f)

    Scaffold(
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                color = StudioColors.CardSurface,
                border = BorderStroke(1.dp, StudioColors.Border),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val previewInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = {
                            if (uiState.ttsUiState.isPreviewing) {
                                viewModel.stopTtsPreview()
                            } else if (!uiState.ttsUiState.isSynthesizingPreview) {
                                viewModel.previewTtsSpeech(
                                    text = textInput,
                                    dialect = selectedDialect,
                                    gender = selectedGender,
                                    style = selectedStyle,
                                    speechRate = effectiveSpeechRate,
                                    pitch = effectiveSpeechPitch,
                                    persona = selectedPersona,
                                    acousticMode = selectedAcousticMode,
                                )
                            }
                        },
                        enabled = !uiState.ttsUiState.isSynthesizingPreview && textInput.isNotBlank(),
                        interactionSource = previewInteraction,
                        border = BorderStroke(1.dp, StudioColors.Cyan),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioColors.Cyan),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(previewInteraction, MaterialTheme.colorScheme.primary, selected = uiState.ttsUiState.isPreviewing, cornerRadius = 14.dp)
                            .testTag("tts_preview_button"),
                    ) {
                        if (uiState.ttsUiState.isSynthesizingPreview) {
                            CircularProgressIndicator(
                                color = StudioColors.Cyan,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = t(lang, "tts_synthesizing"),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else {
                            Icon(
                                imageVector = if (uiState.ttsUiState.isPreviewing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (uiState.ttsUiState.isPreviewing) t(lang, "tts_stop_preview") else t(lang, "tts_preview_button"),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    val insertInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = {
                            preferVmUndo = true
                            viewModel.generateTtsToStudio(
                                text = textInput,
                                dialect = selectedDialect,
                                gender = selectedGender,
                                style = selectedStyle,
                                speechRate = effectiveSpeechRate,
                                pitch = effectiveSpeechPitch,
                                appendToExisting = appendToExisting,
                                persona = selectedPersona,
                                acousticMode = selectedAcousticMode,
                            )
                        },
                        enabled = !uiState.ttsUiState.isGenerating && !uiState.isBusy && textInput.isNotBlank(),
                        interactionSource = insertInteraction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioColors.Cyan,
                            contentColor = StudioColors.BgTop,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(insertInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                            .testTag("tts_insert_button"),
                    ) {
                        Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(t(lang, "approve_and_start_processing"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
    ) { scaffoldPadding ->
        StudioScrollColumn(modifier = Modifier.padding(scaffoldPadding)) {
        StudioFocusTopBar(
            title = t(lang, "section_tts_title"),
            subtitle = t(lang, "section_tts_subtitle"),
            language = lang,
            onBack = onBack,
            trailing = {
                UndoRedoActionsRow(
                    canUndo = ttsHistory.canUndo || uiState.canUndoEdit,
                    canRedo = ttsHistory.canRedo || uiState.canRedoEdit,
                    language = lang,
                    undoTag = "tts_undo_button",
                    redoTag = "tts_redo_button",
                    onUndo = {
                        if ((preferVmUndo && uiState.canUndoEdit) || !ttsHistory.canUndo) {
                            viewModel.undoLastEdit()
                        } else {
                            ttsHistory.undo()?.let(::applyTtsSnap)
                        }
                    },
                    onRedo = {
                        if ((preferVmUndo && uiState.canRedoEdit) || !ttsHistory.canRedo) {
                            viewModel.redoLastEdit()
                        } else {
                            ttsHistory.redo()?.let(::applyTtsSnap)
                        }
                    },
                    extraTrailing = {
                        InfoBadgeButton(
                            contentDescription = t(lang, "section_tts_title"),
                            testTag = "tts_info_button",
                            accentColor = StudioColors.Cyan,
                            onClick = { infoOpen = true },
                        )
                    },
                )
            },
        )

        // VoxCPM2 online neural engine
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
            border = BorderStroke(1.dp, StudioColors.Cyan.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RecordVoiceOver,
                        contentDescription = null,
                        tint = StudioColors.Cyan,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = t(lang, "tts_engine_title"),
                        style = MaterialTheme.typography.titleSmall,
                        color = StudioColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StudioColors.Cyan.copy(alpha = 0.16f),
                    ) {
                        Text(
                            text = if (lang == AppLanguage.EN) "Online" else "سحابي",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = StudioColors.Cyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = StudioColors.ElevatedSurface,
                    border = BorderStroke(1.dp, StudioColors.Border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tts_engine_free_card"),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = if (lang == AppLanguage.EN) "VoxCPM2 Neural TTS" else "محرك VoxCPM2 العصبي",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = StudioColors.Cyan,
                                )
                                Text(
                                    text = if (lang == AppLanguage.EN) {
                                        "Generated on demand by the public VoxCPM2 cloud service."
                                    } else {
                                        "يُولّد الصوت عند الطلب عبر خدمة VoxCPM2 السحابية العامة."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StudioColors.TextSecondary,
                                )
                                Text(
                                    text = if (lang == AppLanguage.EN) {
                                        "Internet is required. The public service may vary voice output between different text requests."
                                    } else {
                                        "يتطلب اتصالاً بالإنترنت. قد تختلف نتيجة الصوت عبر الخدمة العامة عند تغيير النص."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioColors.Cyan,
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = StudioColors.Cyan,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                val luxuryInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = { viewModel.setStudioLuxuryEnhancement(!uiState.studioLuxuryEnhancement) },
                    interactionSource = luxuryInteraction,
                    shape = RoundedCornerShape(14.dp),
                    color = if (uiState.studioLuxuryEnhancement) StudioColors.ElevatedSurface else StudioColors.BgMid,
                    border = BorderStroke(
                        1.dp,
                        if (uiState.studioLuxuryEnhancement) StudioColors.Cyan else StudioColors.Border,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                        .lit(
                            luxuryInteraction,
                            StudioColors.Cyan,
                            selected = uiState.studioLuxuryEnhancement,
                            cornerRadius = 14.dp,
                        )
                        .testTag("tts_studio_luxury_toggle"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = if (uiState.studioLuxuryEnhancement) StudioColors.Cyan else StudioColors.TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = t(lang, "tts_studio_luxury_title"),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (uiState.studioLuxuryEnhancement) StudioColors.Cyan else StudioColors.TextPrimary,
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = StudioColors.Cyan.copy(alpha = 0.14f),
                                ) {
                                    Text(
                                        text = t(lang, "tts_studio_luxury_badge"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioColors.Cyan,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            Text(
                                text = t(lang, "tts_studio_luxury_desc"),
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioColors.TextSecondary,
                            )
                        }
                        Switch(
                            checked = uiState.studioLuxuryEnhancement,
                            onCheckedChange = { viewModel.setStudioLuxuryEnhancement(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = StudioColors.BgTop,
                                checkedTrackColor = StudioColors.Cyan,
                            ),
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("tts_studio_luxury_switch"),
                        )
                    }
                }
            }
        }

        TtsLiveWavePreview(
            isPreviewing = uiState.ttsUiState.isPreviewing,
            isSynthesizingPreview = uiState.ttsUiState.isSynthesizingPreview,
            playbackFlow = viewModel.playbackTelemetry,
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
            border = BorderStroke(1.dp, StudioColors.Border),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = t(lang, "tts_web_article_title"),
                    style = MaterialTheme.typography.titleSmall,
                    color = StudioColors.Cyan,
                )
                OutlinedTextField(
                    value = articleUrl,
                    onValueChange = { articleUrl = it },
                    placeholder = {
                        Text(
                            text = t(lang, "tts_web_article_hint"),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioColors.Cyan,
                        unfocusedBorderColor = StudioColors.Border,
                        focusedTextColor = StudioColors.TextPrimary,
                        unfocusedTextColor = StudioColors.TextPrimary,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tts_article_url_input"),
                )
                val fetchArticleInteraction = remember { MutableInteractionSource() }
                OutlinedButton(
                    onClick = {
                        viewModel.extractArticleTextForTts(articleUrl) { extracted ->
                            preferVmUndo = false
                            val limitedText = TtsTextLimits.limitWords(extracted)
                            wordLimitExceeded = limitedText != extracted
                            textInput = limitedText
                            ttsHistory.commit(currentTtsSnap(txt = limitedText))
                        }
                    },
                    enabled = !uiState.isExtractingArticle && articleUrl.isNotBlank(),
                    interactionSource = fetchArticleInteraction,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StudioColors.Cyan.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioColors.Cyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .lit(fetchArticleInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 12.dp)
                        .testTag("tts_fetch_article_button"),
                ) {
                    if (uiState.isExtractingArticle) {
                        CircularProgressIndicator(
                            color = StudioColors.Cyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (uiState.isExtractingArticle) {
                            t(lang, "tts_web_article_fetching")
                        } else {
                            t(lang, "tts_web_article_fetch")
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (uiState.isExtractingArticle) {
                    val cancelExtractInt = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = { viewModel.cancelActiveOnlineRequest() },
                        interactionSource = cancelExtractInt,
                        border = BorderStroke(1.dp, StudioColors.RoseBright),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioColors.RoseBright),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(cancelExtractInt, StudioColors.RoseBright, cornerRadius = 12.dp)
                            .testTag("tts_article_cancel_button"),
                    ) {
                        Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(t(lang, "cancel_request"))
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
            border = BorderStroke(1.dp, StudioColors.Border),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(t(lang, "tts_input_label"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    val sampleInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = {
                            viewModel.stopTtsPreview()
                            preferVmUndo = false
                            textInput = selectedDialect.sampleText
                            ttsHistory.commit(currentTtsSnap(txt = selectedDialect.sampleText))
                        },
                        interactionSource = sampleInteraction,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(sampleInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 12.dp)
                            .testTag("tts_fill_sample_button"),
                    ) {
                        Text(t(lang, "tts_sample_button"))
                    }
                }

                OutlinedTextField(
                    value = textInput,
                    onValueChange = {
                        if (it != textInput) {
                            viewModel.stopTtsPreview()
                        }
                        preferVmUndo = false
                        val limitedText = TtsTextLimits.limitWords(it)
                        wordLimitExceeded = limitedText != it
                        textInput = limitedText
                        ttsHistory.commit(currentTtsSnap(txt = limitedText), debounceMs = 350L)
                    },
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioColors.Cyan,
                        unfocusedBorderColor = StudioColors.Border,
                        focusedTextColor = StudioColors.TextPrimary,
                        unfocusedTextColor = StudioColors.TextPrimary,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tts_text_input"),
                )
                Text(
                    text = if (lang == AppLanguage.EN) {
                        "${TtsTextLimits.countWords(textInput)} / ${TtsTextLimits.MAX_WORDS} words"
                    } else {
                        "${TtsTextLimits.countWords(textInput)} / ${TtsTextLimits.MAX_WORDS} كلمة"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (wordLimitExceeded) StudioColors.RoseBright else StudioColors.TextSecondary,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (wordLimitExceeded) {
                    Text(
                        text = if (lang == AppLanguage.EN) {
                            "Text is limited to ${TtsTextLimits.MAX_WORDS} words."
                        } else {
                            "الحد الأقصى للنص هو ${TtsTextLimits.MAX_WORDS} كلمة."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioColors.RoseBright,
                    )
                }

                CategorizedVoicePersonasSection(
                    selectedPersona = selectedPersona,
                    selectedAcousticMode = selectedAcousticMode,
                    language = lang,
                    isLocalStudioTts = true,
                    onSelectPersona = { persona ->
                        preferVmUndo = false
                        selectedPersona = persona
                        selectedGender = persona.gender
                        selectedStyle = persona.defaultStyle
                        selectedAcousticMode = persona.defaultAcousticMode
                        ttsHistory.commit(
                            currentTtsSnap(
                                gender = persona.gender,
                                style = persona.defaultStyle,
                                personaId = persona.id,
                                acousticMode = persona.defaultAcousticMode,
                            ),
                        )
                    },
                    onSelectAcousticMode = { mode ->
                        preferVmUndo = false
                        selectedAcousticMode = mode
                        ttsHistory.commit(currentTtsSnap(acousticMode = mode))
                    },
                )

                HorizontalDivider(
                    color = StudioColors.Border,
                    modifier = Modifier.padding(vertical = 2.dp),
                )

                Text(t(lang, "tts_gender_label"), style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        TtsGender.MALE to t(lang, "tts_gender_male"),
                        TtsGender.FEMALE to t(lang, "tts_gender_female"),
                    ).forEach { (genderOption, label) ->
                        val isSel = selectedGender == genderOption
                        val genderInteraction = remember { MutableInteractionSource() }
                        Surface(
                            onClick = {
                                preferVmUndo = false
                                selectedGender = genderOption
                                val nextPersona = if (selectedPersona?.gender == genderOption) {
                                    selectedPersona
                                } else {
                                    TTS_VOICE_PERSONAS.firstOrNull { it.gender == genderOption }
                                }
                                selectedPersona = nextPersona
                                val nextStyle = nextPersona?.defaultStyle ?: selectedStyle
                                val nextMode = nextPersona?.defaultAcousticMode ?: selectedAcousticMode
                                selectedStyle = nextStyle
                                selectedAcousticMode = nextMode
                                ttsHistory.commit(
                                    currentTtsSnap(
                                        gender = genderOption,
                                        style = nextStyle,
                                        personaId = nextPersona?.id,
                                        acousticMode = nextMode,
                                    ),
                                )
                            },
                            interactionSource = genderInteraction,
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSel) StudioColors.ElevatedSurface else StudioColors.BgMid,
                            border = BorderStroke(1.dp, if (isSel) StudioColors.Cyan else StudioColors.Border),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .lit(genderInteraction, MaterialTheme.colorScheme.primary, selected = isSel, cornerRadius = 14.dp)
                                .testTag("tts_gender_${genderOption.name}"),
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSel) StudioColors.Cyan else StudioColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                Text(t(lang, "tts_style_label"), style = MaterialTheme.typography.titleSmall)
                TtsDeliveryStyle.entries.chunked(2).forEach { rowStyles ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowStyles.forEach { st ->
                            val isSel = selectedStyle == st
                            val styleInteraction = remember { MutableInteractionSource() }
                            Surface(
                                onClick = {
                                    preferVmUndo = false
                                    selectedStyle = st
                                    ttsHistory.commit(currentTtsSnap(style = st))
                                },
                                interactionSource = styleInteraction,
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSel) StudioColors.ElevatedSurface else StudioColors.BgMid,
                                border = BorderStroke(1.dp, if (isSel) StudioColors.Violet else StudioColors.Border),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 46.dp)
                                    .lit(styleInteraction, MaterialTheme.colorScheme.primary, selected = isSel, cornerRadius = 14.dp)
                                    .testTag("tts_style_${st.name}"),
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                                    Text(
                                        text = t(lang, st.titleKey),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSel) StudioColors.Violet else StudioColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }

                Text(t(lang, "tts_dialect_section"), style = MaterialTheme.typography.titleSmall)

                TTS_DIALECTS.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        pair.forEach { dialect ->
                            val selected = dialect.id == selectedDialect.id
                            val dialectInteraction = remember { MutableInteractionSource() }
                            Surface(
                                onClick = {
                                    preferVmUndo = false
                                    selectedDialect = dialect
                                    rateOffsetPercent = 0f
                                    pitchOffsetPercent = 0f
                                    ttsHistory.commit(
                                        currentTtsSnap(
                                            dialectId = dialect.id,
                                            rate = 0f,
                                            pitch = 0f,
                                        ),
                                    )
                                },
                                interactionSource = dialectInteraction,
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) StudioColors.ElevatedSurface else StudioColors.BgMid,
                                border = BorderStroke(1.dp, if (selected) StudioColors.Cyan else StudioColors.Border),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .lit(dialectInteraction, MaterialTheme.colorScheme.primary, selected = selected, cornerRadius = 14.dp)
                                    .testTag("tts_dialect_${dialect.id}"),
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
                                    Text(
                                        text = t(lang, dialect.titleKey),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) StudioColors.Cyan else StudioColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }

                StudioParamSlider(
                    label = t(lang, "tts_speed_label"),
                    value = rateOffsetPercent,
                    min = -45f,
                    max = 55f,
                    formattedValue = "${if (rateOffsetPercent > 0f) "+" else ""}${rateOffsetPercent.roundToInt()}%",
                    accentColor = StudioColors.Cyan,
                    testTag = "tts_rate_slider",
                    onValueChange = {
                        preferVmUndo = false
                        rateOffsetPercent = it
                        ttsHistory.commit(currentTtsSnap(rate = it), debounceMs = 350L)
                    },
                )

                StudioParamSlider(
                    label = t(lang, "tts_pitch_label"),
                    value = pitchOffsetPercent,
                    min = -35f,
                    max = 45f,
                    formattedValue = "${if (pitchOffsetPercent > 0f) "+" else ""}${pitchOffsetPercent.roundToInt()}%",
                    accentColor = StudioColors.Violet,
                    testTag = "tts_pitch_slider",
                    onValueChange = {
                        preferVmUndo = false
                        pitchOffsetPercent = it
                        ttsHistory.commit(currentTtsSnap(pitch = it), debounceMs = 350L)
                    },
                )

                if (uiState.hasAudio) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = t(lang, "tts_append_toggle"),
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioColors.TextSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = appendToExisting,
                            onCheckedChange = { appendToExisting = it },
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("tts_append_switch"),
                        )
                    }
                }

                if (uiState.ttsUiState.isGenerating || uiState.ttsUiState.isSynthesizingPreview) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(
                            color = StudioColors.Cyan,
                            trackColor = StudioColors.BgTop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .testTag("tts_synthesis_progress"),
                        )
                        val cancelTtsInt = remember { MutableInteractionSource() }
                        OutlinedButton(
                            onClick = { viewModel.cancelActiveOnlineRequest() },
                            interactionSource = cancelTtsInt,
                            border = BorderStroke(1.dp, StudioColors.RoseBright),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioColors.RoseBright),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .lit(cancelTtsInt, StudioColors.RoseBright, cornerRadius = 12.dp)
                                .testTag("tts_cancel_request_button"),
                        ) {
                            Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(t(lang, "cancel_request"))
                        }
                    }
                }

                if (uiState.ttsUiState.usedFallbackEngine) {
                    AiStatusCard(message = t(lang, "tts_fallback_notice"), isAmber = true)
                }

                uiState.ttsUiState.errorMessage?.let { err ->
                    AiStatusCard(message = err, isAmber = false)
                }

            }
            }
        }
    }

    ToolInfoDialog(
        open = infoOpen,
        title = t(lang, "section_tts_title"),
        description = "${t(lang, "section_tts_subtitle")} • ${t(lang, "tts_web_article_title")}",
        language = lang,
        onDismiss = { infoOpen = false },
    )
}

@Composable
fun VoiceChangerScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onBack: () -> Unit,
) {
    val lang = uiState.language
    val voiceChangerPersonas = remember {
        val cartoonBase = TTS_VOICE_PERSONAS.first { it.id == "male_modern_energetic" }
        listOf(
            TTS_VOICE_PERSONAS.first { it.id == "male_doc_deep" },
            TTS_VOICE_PERSONAS.first { it.id == "female_story_soft" },
            TTS_VOICE_PERSONAS.first { it.id == "child_voice" },
            TTS_VOICE_PERSONAS.first { it.id == "female_news_anchor" },
            TTS_VOICE_PERSONAS.first { it.id == "male_promo_dramatic" },
            cartoonBase.copy(
                id = "cartoon_voice",
                titleAr = "صوت كرتوني",
                titleEn = "Cartoon Voice",
                descAr = "نبرة مرحة ومبالغ فيها بطابع كرتوني.",
                descEn = "A playful, stylized cartoon-like vocal character.",
                defaultPitchSemitones = 4.2f,
                defaultFormantShift = 1.24f,
                defaultWarmthDb = 1.2f,
                defaultPresenceDb = 2.8f,
                geminiDirectionPromptAr = "تحدث بطابع كرتوني مرح وواضح، مع الحفاظ على الكلمات ومخارج الحروف.",
            ),
        )
    }
    var selectedPersona by remember { mutableStateOf<TtsVoicePersona?>(voiceChangerPersonas.first()) }
    var selectedAcousticMode by remember { mutableStateOf(voiceChangerPersonas.first().defaultAcousticMode) }
    var pitchSemitones by remember { mutableFloatStateOf(voiceChangerPersonas.first().defaultPitchSemitones) }
    var formantShift by remember { mutableFloatStateOf(voiceChangerPersonas.first().defaultFormantShift) }
    var warmthDb by remember { mutableFloatStateOf(voiceChangerPersonas.first().defaultWarmthDb) }
    var presenceDb by remember { mutableFloatStateOf(voiceChangerPersonas.first().defaultPresenceDb) }
    var mix by remember { mutableFloatStateOf(1f) }
    var saveDraftDialogOpen by remember { mutableStateOf(false) }
    var infoOpen by remember { mutableStateOf(false) }
    var selectedLiveEffect by remember { mutableStateOf(uiState.liveVoiceEffect) }
    var preferVmUndo by remember { mutableStateOf(false) }
    var showMicPermissionRationale by remember { mutableStateOf(false) }

    fun selectedVoiceLabel(): String = selectedPersona?.let {
        if (lang == AppLanguage.EN) it.titleEn else it.titleAr
    } ?: if (lang == AppLanguage.EN) "Natural voice" else "الصوت الطبيعي"

    fun previewSelectedVoice() {
        viewModel.previewVoiceWithAi(
            preset = null,
            targetLabel = selectedVoiceLabel(),
            pitchSemitones = pitchSemitones * mix,
            formantShift = 1f + (formantShift - 1f) * mix,
            warmthDb = warmthDb * mix,
            presenceDb = presenceDb * mix,
            targetPersona = selectedPersona,
            acousticMode = selectedAcousticMode,
        )
    }

    fun applySelectedVoice() {
        preferVmUndo = true
        viewModel.convertVoiceWithAi(
            preset = null,
            targetLabel = selectedVoiceLabel(),
            pitchSemitones = pitchSemitones * mix,
            formantShift = 1f + (formantShift - 1f) * mix,
            warmthDb = warmthDb * mix,
            presenceDb = presenceDb * mix,
            targetPersona = selectedPersona,
            acousticMode = selectedAcousticMode,
        )
    }
    val voiceHistory = rememberEditHistory(
        VoiceTransformSnapshot(
            pitchSemitones = pitchSemitones,
            formantShift = formantShift,
            warmthDb = warmthDb,
            presenceDb = presenceDb,
            mix = mix,
            personaId = selectedPersona?.id,
            acousticMode = selectedAcousticMode,
        ),
    )

    fun currentVoiceSnap(
        pitch: Float = pitchSemitones,
        formant: Float = formantShift,
        warmth: Float = warmthDb,
        presence: Float = presenceDb,
        mixVal: Float = mix,
        personaId: String? = selectedPersona?.id,
        acousticMode: TtsPersonaAcousticMode = selectedAcousticMode,
    ) = VoiceTransformSnapshot(
        pitchSemitones = pitch,
        formantShift = formant,
        warmthDb = warmth,
        presenceDb = presence,
        mix = mixVal,
        personaId = personaId,
        acousticMode = acousticMode,
    )

    fun applyVoiceSnap(snap: VoiceTransformSnapshot) {
        selectedPersona = snap.personaId?.let { id -> voiceChangerPersonas.firstOrNull { it.id == id } }
        selectedAcousticMode = snap.acousticMode
        pitchSemitones = snap.pitchSemitones
        formantShift = snap.formantShift
        warmthDb = snap.warmthDb
        presenceDb = snap.presenceDb
        mix = snap.mix
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.startLiveVoiceProcessor(selectedLiveEffect)
        } else {
            viewModel.setMicPermissionDeniedError()
        }
    }

    MicrophonePermissionRationaleDialog(
        open = showMicPermissionRationale,
        language = lang,
        onDismiss = { showMicPermissionRationale = false },
        onConfirm = {
            showMicPermissionRationale = false
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
    )

    LaunchedEffect(uiState.restoredToolSession?.id) {
        val restored = viewModel.consumeRestoredToolSession("voice_transformer")
        if (restored != null) {
            val fp = restored.floatParams
            val personaId = restored.stringParams["personaId"]
            val legacyPresetId = restored.stringParams["presetId"]
            val modeName = restored.stringParams["acousticMode"]
            selectedPersona = personaId?.let { id ->
                voiceChangerPersonas.firstOrNull { it.id == id }
            }
                ?: when (legacyPresetId) {
                    "male_luxury" -> voiceChangerPersonas.firstOrNull { it.id == "male_doc_deep" }
                    "male_calm" -> voiceChangerPersonas.firstOrNull { it.id == "male_doc_deep" }
                    "female_soft" -> voiceChangerPersonas.firstOrNull { it.id == "female_story_soft" }
                    "female_balanced" -> voiceChangerPersonas.firstOrNull { it.id == "female_news_anchor" }
                    "child" -> voiceChangerPersonas.firstOrNull { it.id == "child_voice" }
                    else -> null
                } ?: voiceChangerPersonas.first()
            selectedAcousticMode = modeName?.let { runCatching { TtsPersonaAcousticMode.valueOf(it) }.getOrNull() }
                ?: selectedPersona?.defaultAcousticMode
                ?: TtsPersonaAcousticMode.NATURAL
            pitchSemitones = fp["pitchSemitones"] ?: pitchSemitones
            formantShift = fp["formantShift"] ?: formantShift
            warmthDb = fp["warmthDb"] ?: warmthDb
            presenceDb = fp["presenceDb"] ?: presenceDb
            mix = fp["mix"] ?: mix
        }
    }

    val latestHasAudio by rememberUpdatedState(uiState.hasAudio)
    val latestPersonaId by rememberUpdatedState(selectedPersona?.id)
    val latestAcousticMode by rememberUpdatedState(selectedAcousticMode)
    val latestPitch by rememberUpdatedState(pitchSemitones)
    val latestFormant by rememberUpdatedState(formantShift)
    val latestWarmth by rememberUpdatedState(warmthDb)
    val latestPresence by rememberUpdatedState(presenceDb)
    val latestMix by rememberUpdatedState(mix)

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopLiveVoiceProcessor()
            viewModel.stopToolPreview()
            if (latestHasAudio) {
                viewModel.saveToolSessionToCache(
                    toolId = "voice_transformer",
                    titleAr = "مغيّر الأصوات",
                    titleEn = "Voice Changer",
                    summaryAr = "${"%.1f".format(latestPitch)} st / ${(latestMix * 100f).toInt()}%",
                    summaryEn = "${"%.1f".format(latestPitch)} st / ${(latestMix * 100f).toInt()}%",
                    floatParams = mapOf(
                        "pitchSemitones" to latestPitch,
                        "formantShift" to latestFormant,
                        "warmthDb" to latestWarmth,
                        "presenceDb" to latestPresence,
                        "mix" to latestMix,
                    ),
                    stringParams = buildMap {
                        latestPersonaId?.let { put("personaId", it) }
                        put("acousticMode", latestAcousticMode.name)
                    },
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    StudioScrollColumn(modifier = Modifier.padding(bottom = 128.dp)) {
        StudioFocusTopBar(
            title = t(lang, "section_voice_transformer_title"),
            subtitle = t(lang, "section_voice_transformer_subtitle"),
            language = lang,
            onBack = onBack,
            trailing = {
                UndoRedoActionsRow(
                    canUndo = voiceHistory.canUndo || uiState.canUndoEdit,
                    canRedo = voiceHistory.canRedo || uiState.canRedoEdit,
                    language = lang,
                    undoTag = "voice_transformer_undo_button",
                    redoTag = "voice_transformer_redo_button",
                    onUndo = {
                        if ((preferVmUndo && uiState.canUndoEdit) || !voiceHistory.canUndo) {
                            viewModel.undoLastEdit()
                        } else {
                            voiceHistory.undo()?.let(::applyVoiceSnap)
                        }
                    },
                    onRedo = {
                        if ((preferVmUndo && uiState.canRedoEdit) || !voiceHistory.canRedo) {
                            viewModel.redoLastEdit()
                        } else {
                            voiceHistory.redo()?.let(::applyVoiceSnap)
                        }
                    },
                    extraTrailing = {
                        InfoBadgeButton(
                            contentDescription = t(lang, "section_voice_transformer_title"),
                            testTag = "voice_transformer_info_button",
                            accentColor = StudioColors.Violet,
                            onClick = { infoOpen = true },
                        )
                    },
                )
            },
            bottomContent = {
                StudioPreviewControlsBar(
                    uiState = uiState,
                    onHoldBypassChange = { viewModel.setHoldBypass(it) },
                    onToggleBypass = { viewModel.toggleBypass() },
                    onUndo = { viewModel.undoLastEdit() },
                    onRedo = { viewModel.redoLastEdit() },
                    onResetZero = {
                        preferVmUndo = false
                        selectedPersona = null
                        selectedAcousticMode = TtsPersonaAcousticMode.NATURAL
                        pitchSemitones = 0f
                        formantShift = 1.0f
                        warmthDb = 0f
                        presenceDb = 0f
                        mix = 0f
                        voiceHistory.commit(
                            currentVoiceSnap(
                                pitch = 0f,
                                formant = 1.0f,
                                warmth = 0f,
                                presence = 0f,
                                mixVal = 0f,
                                personaId = null,
                                acousticMode = TtsPersonaAcousticMode.NATURAL,
                            ),
                        )
                        viewModel.resetAllEffects()
                    },
                    onOpenSaveDraft = { saveDraftDialogOpen = true },
                    embedded = true,
                )
            },
        )

        // Live zero-latency microphone character tester (VoiceProcessor)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
            border = BorderStroke(
                1.dp,
                if (uiState.isLiveVoiceChangerActive) StudioColors.Emerald else StudioColors.Border,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = null,
                        tint = if (uiState.isLiveVoiceChangerActive) StudioColors.Emerald else StudioColors.Cyan,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = t(lang, "live_mic_fx_title"),
                            style = MaterialTheme.typography.titleSmall,
                            color = StudioColors.TextPrimary,
                        )
                        Text(
                            text = t(lang, "live_mic_fx_subtitle"),
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioColors.TextSecondary,
                        )
                    }
                }

                val liveOptions = remember {
                    listOf(
                        VoiceEffect.CHILD_NATURAL to "Child",
                        VoiceEffect.DEEP to "Deep",
                        VoiceEffect.ECHO to "Echo",
                        VoiceEffect.NONE to "Natural",
                    )
                }
                VoiceModeDropdown(
                    label = if (lang == AppLanguage.EN) "Live microphone effect" else "تأثير الميكروفون الحي",
                    selected = selectedLiveEffect,
                    options = liveOptions.map { it.first },
                    optionLabel = { effect ->
                        when (effect) {
                            VoiceEffect.CHILD_NATURAL -> t(lang, "voice_char_child")
                            VoiceEffect.DEEP -> if (lang == AppLanguage.EN) "Deep" else "عميق"
                            VoiceEffect.ECHO -> if (lang == AppLanguage.EN) "Echo" else "صدى"
                            VoiceEffect.NONE -> if (lang == AppLanguage.EN) "Natural" else "طبيعي"
                            else -> liveOptions.first { it.first == effect }.second
                        }
                    },
                    testTag = "live_fx_dropdown",
                    onSelect = { effect ->
                        selectedLiveEffect = effect
                        if (uiState.isLiveVoiceChangerActive) {
                            viewModel.setLiveVoiceEffect(effect)
                        }
                    },
                )

                val liveToggleInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        if (uiState.isLiveVoiceChangerActive) {
                            viewModel.stopLiveVoiceProcessor()
                        } else if (viewModel.hasMicPermission()) {
                            viewModel.startLiveVoiceProcessor(selectedLiveEffect)
                        } else {
                            showMicPermissionRationale = true
                        }
                    },
                    interactionSource = liveToggleInteraction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isLiveVoiceChangerActive) StudioColors.RoseBright else StudioColors.Emerald,
                        contentColor = StudioColors.BgTop,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .lit(
                            liveToggleInteraction,
                            MaterialTheme.colorScheme.primary,
                            selected = uiState.isLiveVoiceChangerActive,
                            cornerRadius = 14.dp,
                        )
                        .testTag("live_mic_fx_toggle_button"),
                ) {
                    Icon(
                        imageVector = if (uiState.isLiveVoiceChangerActive) Icons.Rounded.Stop else Icons.Rounded.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isLiveVoiceChangerActive) {
                            t(lang, "live_mic_fx_stop")
                        } else {
                            t(lang, "live_mic_fx_start")
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        // A single profile menu avoids duplicate gender-specific lists and keeps voice processing settings together.
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
            border = BorderStroke(1.dp, StudioColors.Cyan.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (lang == AppLanguage.EN) "Professional voice profile" else "الشخصية الصوتية الاحترافية",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = StudioColors.Cyan,
                )
                VoiceProfileDropdown(
                    selectedPersona = selectedPersona,
                    personas = voiceChangerPersonas,
                    language = lang,
                    onSelectPersona = { persona ->
                        preferVmUndo = false
                        selectedPersona = persona
                        pitchSemitones = persona.defaultPitchSemitones
                        formantShift = persona.defaultFormantShift
                        warmthDb = persona.defaultWarmthDb
                        presenceDb = persona.defaultPresenceDb
                        mix = 1f
                        selectedAcousticMode = persona.defaultAcousticMode
                        voiceHistory.commit(
                            currentVoiceSnap(
                                pitch = persona.defaultPitchSemitones,
                                formant = persona.defaultFormantShift,
                                warmth = persona.defaultWarmthDb,
                                presence = persona.defaultPresenceDb,
                                mixVal = 1f,
                                personaId = persona.id,
                                acousticMode = persona.defaultAcousticMode,
                            ),
                        )
                    },
                )
                AcousticModeDropdown(
                    selectedAcousticMode = selectedAcousticMode,
                    language = lang,
                    onSelectAcousticMode = { mode ->
                        preferVmUndo = false
                        selectedAcousticMode = mode
                        voiceHistory.commit(currentVoiceSnap(acousticMode = mode))
                    },
                )
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
            border = BorderStroke(1.dp, StudioColors.Border),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StudioParamSlider(
                    label = t(lang, "voice_pitch_semitones"),
                    value = pitchSemitones,
                    min = -10f,
                    max = 10f,
                    formattedValue = "${if (pitchSemitones >= 0f) "+" else ""}${"%.1f".format(pitchSemitones)} st",
                    accentColor = StudioColors.Violet,
                    testTag = "voice_pitch_slider",
                    onValueChange = {
                        preferVmUndo = false
                        pitchSemitones = it
                        val nextMix = if (mix == 0f) 1f else mix
                        mix = nextMix
                        voiceHistory.commit(currentVoiceSnap(pitch = it, mixVal = nextMix), debounceMs = 350L)
                    },
                )
                StudioParamSlider(
                    label = t(lang, "voice_formant_shift"),
                    value = formantShift,
                    min = 0.7f,
                    max = 1.42f,
                    formattedValue = "${"%.2f".format(formantShift)}x",
                    accentColor = StudioColors.Cyan,
                    testTag = "voice_formant_slider",
                    onValueChange = {
                        preferVmUndo = false
                        formantShift = it
                        val nextMix = if (mix == 0f) 1f else mix
                        mix = nextMix
                        voiceHistory.commit(currentVoiceSnap(formant = it, mixVal = nextMix), debounceMs = 350L)
                    },
                )
                StudioParamSlider(
                    label = t(lang, "voice_warmth"),
                    value = warmthDb,
                    min = -6f,
                    max = 8f,
                    formattedValue = "${"%.1f".format(warmthDb)} dB",
                    accentColor = StudioColors.Amber,
                    testTag = "voice_warmth_slider",
                    onValueChange = {
                        preferVmUndo = false
                        warmthDb = it
                        val nextMix = if (mix == 0f) 1f else mix
                        mix = nextMix
                        voiceHistory.commit(currentVoiceSnap(warmth = it, mixVal = nextMix), debounceMs = 350L)
                    },
                )
                StudioParamSlider(
                    label = t(lang, "voice_presence"),
                    value = presenceDb,
                    min = -6f,
                    max = 8f,
                    formattedValue = "${"%.1f".format(presenceDb)} dB",
                    accentColor = StudioColors.Emerald,
                    testTag = "voice_presence_slider",
                    onValueChange = {
                        preferVmUndo = false
                        presenceDb = it
                        val nextMix = if (mix == 0f) 1f else mix
                        mix = nextMix
                        voiceHistory.commit(currentVoiceSnap(presence = it, mixVal = nextMix), debounceMs = 350L)
                    },
                )
                StudioParamSlider(
                    label = if (lang == AppLanguage.EN) "Vocal character intensity" else "شدة الشخصية الصوتية",
                    value = mix,
                    min = 0f,
                    max = 1f,
                    formattedValue = "${(mix * 100f).roundToInt()}%",
                    accentColor = StudioColors.Cyan,
                    testTag = "voice_mix_slider",
                    onValueChange = {
                        preferVmUndo = false
                        mix = it
                        voiceHistory.commit(currentVoiceSnap(mixVal = it), debounceMs = 350L)
                    },
                )

                uiState.purifyProgress?.let { prog ->
                    LinearProgressIndicator(
                        progress = { prog.coerceIn(0f, 1f) },
                        color = StudioColors.Violet,
                        trackColor = StudioColors.BgTop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                    )
                }

            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
            border = BorderStroke(1.dp, StudioColors.Cyan.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("voice_changer_source_card"),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = if (lang == AppLanguage.EN) "Voice-to-voice conversion" else "تحويل الصوت الفعلي",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = StudioColors.Cyan,
                )
                Text(
                    text = if (uiState.hasAudio) {
                        if (lang == AppLanguage.EN) "The loaded or recorded audio is ready for conversion." else "المقطع الصوتي المحمّل أو المسجّل جاهز للتحويل."
                    } else {
                        if (lang == AppLanguage.EN) "Record or import an audio clip before previewing or applying a voice change." else "سجّل أو استورد مقطعاً صوتياً قبل معاينة تغيير الصوت أو تطبيقه."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioColors.TextSecondary,
                )
                uiState.voiceAiStatusNote?.takeIf { it.isNotBlank() }?.let { status ->
                    AiStatusCard(message = status, isAmber = true)
                }
                if (uiState.isVoiceAiConverting) {
                    LinearProgressIndicator(
                        progress = { uiState.voiceAiProgress?.coerceIn(0f, 1f) ?: 0f },
                        color = StudioColors.Cyan,
                        trackColor = StudioColors.BgTop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                    )
                    OutlinedButton(
                        onClick = { viewModel.cancelActiveOnlineRequest() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(t(lang, "cancel_request"))
                    }
                }
            }
        }

    }
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .padding(16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
            border = BorderStroke(1.dp, StudioColors.Cyan.copy(alpha = 0.45f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (uiState.isVoiceAiConverting) {
                    LinearProgressIndicator(
                        progress = { uiState.voiceAiProgress?.coerceIn(0f, 1f) ?: 0f },
                        color = StudioColors.Cyan,
                        trackColor = StudioColors.BgTop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                    )
                    Text(
                        text = "${((uiState.voiceAiProgress ?: 0f) * 100f).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioColors.Cyan,
                    )
                    uiState.voiceAiStatusNote?.let { status ->
                        Text(status, style = MaterialTheme.typography.labelSmall, color = StudioColors.TextSecondary)
                    }
                }
                Button(
                    onClick = {
                        if (uiState.isToolPreviewPlaying) viewModel.stopToolPreview() else previewSelectedVoice()
                    },
                    enabled = uiState.hasAudio && !uiState.isVoiceAiConverting && !uiState.isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isToolPreviewPlaying) StudioColors.Amber else StudioColors.Cyan,
                        contentColor = StudioColors.BgTop,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("voice_changer_preview_button"),
                ) {
                    Icon(
                        if (uiState.isToolPreviewPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (uiState.isToolPreviewPlaying) {
                            if (lang == AppLanguage.EN) "Stop preview" else "إيقاف المعاينة"
                        } else {
                            if (lang == AppLanguage.EN) "Preview voice change" else "معاينة تغيير الصوت"
                        },
                    )
                }
                Button(
                    onClick = { applySelectedVoice() },
                    enabled = uiState.hasAudio && !uiState.isVoiceAiConverting && !uiState.isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioColors.Violet,
                        contentColor = StudioColors.BgTop,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("voice_changer_apply_button"),
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (lang == AppLanguage.EN) "Apply voice change" else "تطبيق تغيير الصوت")
                }
            }
        }
    }

    SaveDraftDialog(
        open = saveDraftDialogOpen,
        language = lang,
        initialName = uiState.activeProjectName.ifBlank { t(lang, "untitled_project") },
        activePresetLabel = uiState.selectedPresetLabel,
        isCloudSignedIn = uiState.isCloudSynced,
        onDismiss = { saveDraftDialogOpen = false },
        onSaveDraft = { draftName -> viewModel.saveCloudDraft(draftName) },
    )

    ToolInfoDialog(
        open = infoOpen,
        title = t(lang, "section_voice_transformer_title"),
        description = t(lang, "section_voice_transformer_subtitle"),
        language = lang,
        onDismiss = { infoOpen = false },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> VoiceModeDropdown(
    label: String,
    selected: T,
    options: List<T>,
    optionLabel: (T) -> String,
    testTag: String,
    onSelect: (T) -> Unit,
) {
    var expanded by rememberSaveable(testTag) { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = StudioColors.Cyan,
                unfocusedBorderColor = StudioColors.Border,
                focusedTextColor = StudioColors.TextPrimary,
                unfocusedTextColor = StudioColors.TextPrimary,
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .testTag(testTag),
        )
        StudioDropdownMenuTheme {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .heightIn(max = 220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioColors.CardSurface)
                    .border(1.dp, StudioColors.Border, RoundedCornerShape(12.dp)),
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            expanded = false
                            onSelect(option)
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceProfileDropdown(
    selectedPersona: TtsVoicePersona?,
    personas: List<TtsVoicePersona>,
    language: AppLanguage,
    onSelectPersona: (TtsVoicePersona) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectedLabel = selectedPersona?.let {
        if (language == AppLanguage.EN) it.titleEn else it.titleAr
    } ?: if (language == AppLanguage.EN) "Choose a profile" else "اختر شخصية"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(if (language == AppLanguage.EN) "Voice profile" else "الشخصية الصوتية") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .testTag("voice_profile_dropdown"),
        )
        StudioDropdownMenuTheme {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .heightIn(max = 220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioColors.CardSurface)
                    .border(1.dp, StudioColors.Border, RoundedCornerShape(12.dp)),
            ) {
                personas.forEach { persona ->
                    DropdownMenuItem(
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(if (language == AppLanguage.EN) persona.titleEn else persona.titleAr)
                                Text(
                                    text = if (language == AppLanguage.EN) persona.descEn else persona.descAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StudioColors.TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        },
                        onClick = {
                            expanded = false
                            onSelectPersona(persona)
                        },
                        modifier = Modifier.testTag("voice_profile_${persona.id}"),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AcousticModeDropdown(
    selectedAcousticMode: TtsPersonaAcousticMode,
    language: AppLanguage,
    onSelectAcousticMode: (TtsPersonaAcousticMode) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectedLabel = if (language == AppLanguage.EN) {
        selectedAcousticMode.titleEn
    } else {
        selectedAcousticMode.titleAr
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(if (language == AppLanguage.EN) "Studio processing" else "المعالجة الاستوديوية") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .testTag("voice_acoustic_mode_dropdown"),
        )
        StudioDropdownMenuTheme {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .heightIn(max = 220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioColors.CardSurface)
                    .border(1.dp, StudioColors.Border, RoundedCornerShape(12.dp)),
            ) {
                TtsPersonaAcousticMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(if (language == AppLanguage.EN) mode.titleEn else mode.titleAr)
                                Text(
                                    text = if (language == AppLanguage.EN) mode.descEn else mode.descAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StudioColors.TextSecondary,
                                )
                            }
                        },
                        onClick = {
                            expanded = false
                            onSelectAcousticMode(mode)
                        },
                        modifier = Modifier.testTag("voice_acoustic_mode_${mode.name}"),
                    )
                }
            }
        }
    }
}

@Composable
private fun CategorizedVoicePersonasSection(
    selectedPersona: TtsVoicePersona?,
    selectedAcousticMode: TtsPersonaAcousticMode,
    language: AppLanguage,
    isLocalStudioTts: Boolean = false,
    onSelectPersona: (TtsVoicePersona) -> Unit,
    onSelectAcousticMode: (TtsPersonaAcousticMode) -> Unit,
) {
    val malePersonas = remember { TTS_VOICE_PERSONAS.filter { it.gender == TtsGender.MALE && it.id != "child_voice" } }
    val femalePersonas = remember { TTS_VOICE_PERSONAS.filter { it.gender == TtsGender.FEMALE && it.id != "child_voice" } }
    val specialPersonas = remember { TTS_VOICE_PERSONAS.filter { it.id == "child_voice" } }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = when {
                isLocalStudioTts && (language == AppLanguage.EN || language == AppLanguage.TR) -> "Men's Voices (Local TTS + FFmpegKit)"
                isLocalStudioTts -> "قسم نبرات الرجال (محلي + FFmpegKit)"
                language == AppLanguage.EN || language == AppLanguage.TR -> "Men's Voices (Gemini 2.5 Audio)"
                else -> "قسم نبرات الرجال (Gemini 2.5)"
            },
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = StudioColors.Cyan,
        )
        malePersonas.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pair.forEach { persona ->
                    VoicePersonaCard(
                        persona = persona,
                        selected = persona.id == selectedPersona?.id,
                        accentColor = StudioColors.Cyan,
                        language = language,
                        isLocalStudioTts = isLocalStudioTts,
                        onClick = { onSelectPersona(persona) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        HorizontalDivider(
            color = StudioColors.Border,
            modifier = Modifier.padding(vertical = 2.dp),
        )

        Text(
            text = when {
                isLocalStudioTts && (language == AppLanguage.EN || language == AppLanguage.TR) -> "Women's Voices (Local TTS + FFmpegKit)"
                isLocalStudioTts -> "قسم نبرات النساء (محلي + FFmpegKit)"
                language == AppLanguage.EN || language == AppLanguage.TR -> "Women's Voices (Gemini 2.5 Audio)"
                else -> "قسم نبرات النساء (Gemini 2.5)"
            },
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = StudioColors.Violet,
        )
        femalePersonas.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pair.forEach { persona ->
                    VoicePersonaCard(
                        persona = persona,
                        selected = persona.id == selectedPersona?.id,
                        accentColor = StudioColors.Violet,
                        language = language,
                        isLocalStudioTts = isLocalStudioTts,
                        onClick = { onSelectPersona(persona) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        if (specialPersonas.isNotEmpty()) {
            HorizontalDivider(
                color = StudioColors.Border,
                modifier = Modifier.padding(vertical = 2.dp),
            )
            Text(
                text = if (language == AppLanguage.EN) {
                    "Special Voices (Child)"
                } else {
                    "أصوات خاصة (طفل)"
                },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = StudioColors.Amber,
            )
            specialPersonas.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    pair.forEach { persona ->
                        VoicePersonaCard(
                            persona = persona,
                            selected = persona.id == selectedPersona?.id,
                            accentColor = StudioColors.Amber,
                            language = language,
                            isLocalStudioTts = isLocalStudioTts,
                            onClick = { onSelectPersona(persona) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        HorizontalDivider(
            color = StudioColors.Border,
            modifier = Modifier.padding(vertical = 2.dp),
        )

        Text(
            text = if (language == AppLanguage.EN) {
                "Acoustic Mastering Style (FFmpegKit)"
            } else {
                "أسلوب المعالجة الهندسية (FFmpegKit Acoustic Mastering)"
            },
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = StudioColors.Emerald,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TtsPersonaAcousticMode.entries.forEach { mode ->
                val isSel = selectedAcousticMode == mode
                val modeInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = { onSelectAcousticMode(mode) },
                    interactionSource = modeInteraction,
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSel) StudioColors.ElevatedSurface else StudioColors.BgMid,
                    border = BorderStroke(1.dp, if (isSel) StudioColors.Emerald else StudioColors.Border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .lit(modeInteraction, StudioColors.Emerald, selected = isSel, cornerRadius = 14.dp)
                        .testTag("acoustic_mode_${mode.name}"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = if (language == AppLanguage.EN) mode.titleEn else mode.titleAr,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isSel) StudioColors.Emerald else StudioColors.TextPrimary,
                            )
                            Text(
                                text = if (language == AppLanguage.EN) mode.descEn else mode.descAr,
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioColors.TextSecondary,
                            )
                        }
                        if (isSel) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = StudioColors.Emerald,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoicePersonaCard(
    persona: TtsVoicePersona,
    selected: Boolean,
    accentColor: Color,
    language: AppLanguage,
    isLocalStudioTts: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val personaInteraction = remember { MutableInteractionSource() }
    val badgeText = if (isLocalStudioTts) {
        if (language == AppLanguage.EN) {
            persona.defaultAcousticMode.titleEn.substringBefore('(').trim()
        } else {
            persona.defaultAcousticMode.titleAr.substringBefore('(').trim()
        }
    } else {
        persona.geminiVoiceName
    }
    Surface(
        onClick = onClick,
        interactionSource = personaInteraction,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) StudioColors.ElevatedSurface else StudioColors.BgMid,
        border = BorderStroke(1.dp, if (selected) accentColor else StudioColors.Border),
        modifier = modifier
            .defaultMinSize(minHeight = 68.dp)
            .lit(personaInteraction, accentColor, selected = selected, cornerRadius = 14.dp)
            .testTag("persona_card_${persona.id}"),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.16f),
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                text = if (language == AppLanguage.EN) persona.titleEn else persona.titleAr,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) accentColor else StudioColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (language == AppLanguage.EN) persona.descEn else persona.descAr,
                style = MaterialTheme.typography.labelSmall,
                color = StudioColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SettingsScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onChangeDefaultSaveFolder: () -> Unit = {},
    onOpenDeveloperPanel: () -> Unit = {},
    onBack: () -> Unit,
) {
    val lang = uiState.language
    var langDialogOpen by remember { mutableStateOf(false) }
    var privacyDialogOpen by rememberSaveable { mutableStateOf(false) }
    var deleteAccountDialogOpen by rememberSaveable { mutableStateOf(false) }
    var deletionInProgress by remember { mutableStateOf(false) }
    var deletionMessageKey by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val devPanelState by viewModel.developerPanelUiState.collectAsStateWithLifecycle()
    val showDeveloperEntry = BuildConfig.DEVELOPER_PANEL_ENABLED && devPanelState.ownerVerification.isAuthorized

    StudioScrollColumn {
        StudioFocusTopBar(
            title = t(lang, "section_settings_title"),
            subtitle = t(lang, "section_settings_subtitle"),
            language = lang,
            onBack = onBack,
        )

        val saveFolderInteraction = remember { MutableInteractionSource() }
        Card(
            onClick = onChangeDefaultSaveFolder,
            interactionSource = saveFolderInteraction,
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
            border = BorderStroke(1.dp, StudioColors.Border),
            modifier = Modifier
                .fillMaxWidth()
                .lit(saveFolderInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 20.dp)
                .testTag("settings_save_folder_card"),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = StudioColors.Amber, modifier = Modifier.size(26.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(t(lang, "settings_save_folder_title"), style = MaterialTheme.typography.titleMedium, color = StudioColors.TextPrimary)
                    Text(
                        text = uiState.defaultSaveFolderName ?: t(lang, "settings_save_folder_not_set"),
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioColors.Amber,
                    )
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = StudioColors.TextMuted)
            }
        }

        val langCardInteraction = remember { MutableInteractionSource() }
        Card(
            onClick = { langDialogOpen = true },
            interactionSource = langCardInteraction,
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
            border = BorderStroke(1.dp, StudioColors.Border),
            modifier = Modifier
                .fillMaxWidth()
                .lit(langCardInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 20.dp)
                .testTag("settings_language_card"),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.Language, contentDescription = null, tint = StudioColors.Cyan, modifier = Modifier.size(26.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(t(lang, "app_language_title"), style = MaterialTheme.typography.titleMedium, color = StudioColors.TextPrimary)
                    Text(
                        text = "${lang.nativeName} (${if (lang.isRtl) "RTL" else "LTR"})",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioColors.Cyan,
                    )
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = StudioColors.TextMuted)
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
            border = BorderStroke(1.dp, StudioColors.Border),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = t(lang, "settings_playback_section"),
                    style = MaterialTheme.typography.titleMedium,
                    color = StudioColors.TextPrimary,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = StudioColors.Emerald)
                        Text(t(lang, "loop_repeat"), style = MaterialTheme.typography.bodyMedium, color = StudioColors.TextPrimary)
                    }
                    Switch(
                        checked = uiState.autoLoopOnPlayback,
                        onCheckedChange = { viewModel.setAutoLoopOnPlayback(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StudioColors.BgTop,
                            checkedTrackColor = StudioColors.Emerald,
                        ),
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("settings_loop_switch"),
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
            border = BorderStroke(1.dp, StudioColors.Border),
            onClick = { privacyDialogOpen = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_privacy_card"),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 64.dp)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.Security, contentDescription = null, tint = StudioColors.Emerald)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = t(lang, "settings_privacy_section"),
                        style = MaterialTheme.typography.titleMedium,
                        color = StudioColors.TextPrimary,
                    )
                    Text(
                        text = t(lang, "settings_privacy_subtitle"),
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioColors.TextSecondary,
                    )
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = StudioColors.TextMuted)
            }
        }

        if (uiState.authSession is AuthSession.SignedIn) {
            Card(
                onClick = {
                    deletionMessageKey = null
                    deleteAccountDialogOpen = true
                },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
                border = BorderStroke(1.dp, StudioColors.RoseBright.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_delete_account_card"),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 64.dp)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = StudioColors.RoseBright)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = t(lang, "account_delete_button"),
                            style = MaterialTheme.typography.titleSmall,
                            color = StudioColors.RoseBright,
                        )
                        Text(
                            text = t(lang, "account_delete_scope_note"),
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioColors.TextSecondary,
                        )
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = StudioColors.TextMuted)
                }
            }
        }

        if (showDeveloperEntry) {
            val devInteraction = remember { MutableInteractionSource() }
            Card(
                onClick = onOpenDeveloperPanel,
                interactionSource = devInteraction,
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
                border = BorderStroke(1.dp, StudioColors.Violet.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .lit(devInteraction, StudioColors.Violet, cornerRadius = 20.dp)
                    .testTag("settings_developer_panel_card"),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 64.dp)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AdminPanelSettings,
                        contentDescription = t(lang, "developer_panel_button"),
                        tint = StudioColors.Violet,
                        modifier = Modifier.size(26.dp),
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = t(lang, "developer_panel_button"),
                            style = MaterialTheme.typography.titleMedium,
                            color = StudioColors.TextPrimary,
                        )
                        Text(
                            text = t(lang, "developer_panel_subtitle"),
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioColors.VioletBright,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = StudioColors.TextMuted)
                }
            }
        }

        SupportUpdatesSection(
            uiState = uiState,
            viewModel = viewModel,
            collapsible = true,
            initiallyExpanded = false,
        )
    }

    LanguageSelectorDialog(
        open = langDialogOpen,
        currentLanguage = lang,
        onSelect = { viewModel.setLanguage(it) },
        onDismiss = { langDialogOpen = false },
    )

    if (privacyDialogOpen) {
        AlertDialog(
            onDismissRequest = { privacyDialogOpen = false },
            containerColor = StudioColors.CardSurface,
            title = {
                Text(
                    text = t(lang, "settings_privacy_dialog_title"),
                    style = MaterialTheme.typography.titleMedium,
                    color = StudioColors.TextPrimary,
                )
            },
            text = {
                Text(
                    text = t(lang, "settings_privacy_dialog_body"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = StudioColors.TextSecondary,
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                if (uiState.authSession is AuthSession.SignedIn) {
                    TextButton(
                        onClick = {
                            privacyDialogOpen = false
                            viewModel.signOut()
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    ) {
                        Text(t(lang, "auth_sign_out"), color = StudioColors.RoseBright)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { privacyDialogOpen = false },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(t(lang, "settings_privacy_close"), color = StudioColors.Cyan)
                }
            },
        )
    }

    if (deleteAccountDialogOpen) {
        AlertDialog(
            onDismissRequest = {
                if (!deletionInProgress) {
                    deleteAccountDialogOpen = false
                    deletionMessageKey = null
                }
            },
            containerColor = StudioColors.CardSurface,
            title = {
                Text(
                    text = t(lang, "account_delete_confirm_title"),
                    style = MaterialTheme.typography.titleMedium,
                    color = StudioColors.RoseBright,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = t(lang, "account_delete_confirm_body"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudioColors.TextSecondary,
                    )
                    deletionMessageKey?.let { messageKey ->
                        Text(
                            text = t(lang, messageKey),
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioColors.Amber,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !deletionInProgress,
                    onClick = {
                        scope.launch {
                            deletionInProgress = true
                            try {
                                deletionMessageKey = null
                                when (viewModel.deleteCurrentAccountAndData()) {
                                    AccountDeletionResult.Success -> {
                                        deleteAccountDialogOpen = false
                                        viewModel.showStatusBanner(t(lang, "account_delete_success"))
                                    }
                                    AccountDeletionResult.RecentLoginRequired -> {
                                        deletionMessageKey = "account_delete_recent_login"
                                    }
                                    AccountDeletionResult.NetworkError -> {
                                        deletionMessageKey = "account_delete_network_error"
                                    }
                                    AccountDeletionResult.Failed -> {
                                        deletionMessageKey = "account_delete_failed"
                                    }
                                }
                            } finally {
                                deletionInProgress = false
                            }
                        }
                    },
                ) {
                    Text(
                        text = t(lang, if (deletionInProgress) "account_delete_progress" else "account_delete_confirm"),
                        color = StudioColors.RoseBright,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !deletionInProgress,
                    onClick = {
                        deleteAccountDialogOpen = false
                        deletionMessageKey = null
                    },
                ) {
                    Text(t(lang, "settings_privacy_close"), color = StudioColors.Cyan)
                }
            },
            modifier = Modifier.testTag("settings_delete_account_dialog"),
        )
    }
}

@Composable
private fun TtsLiveWavePreview(
    isPreviewing: Boolean,
    isSynthesizingPreview: Boolean,
    playbackFlow: StateFlow<PlaybackTelemetry>,
) {
    val playback by playbackFlow.collectAsStateWithLifecycle()
    LiquidAudioWavePreview(
        amplitude = if (isPreviewing || playback.isPlaying) {
            0.72f.coerceAtLeast(playback.outputLevel)
        } else {
            0.15f
        },
        waveformBars = playback.waveformBars,
        isActive = isPreviewing || isSynthesizingPreview || playback.isPlaying,
        modifier = Modifier
            .fillMaxWidth()
            .height(106.dp)
            .testTag("tts_liquid_wave"),
    )
}
