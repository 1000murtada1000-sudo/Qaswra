package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.audio.SttBackendMode
import com.example.audio.formatDurationMs
import com.example.i18n.t
import com.example.model.AppLanguage
import com.example.model.AudioPickerTarget
import com.example.studio.ui.lit
import com.example.ui.components.ExportFormatDialog
import com.example.ui.components.InfoBadgeButton
import com.example.ui.components.MicrophonePermissionRationaleDialog
import com.example.ui.components.StudioFocusTopBar
import com.example.ui.components.StudioParamSlider
import com.example.ui.components.StudioScrollColumn
import com.example.ui.components.ToolInfoDialog
import com.example.ui.components.UndoRedoActionsRow
import com.example.ui.components.rememberEditHistory
import com.example.ui.theme.StudioColors
import com.example.ui.theme.TelemetryTextStyle
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel
import kotlin.math.roundToInt

@Immutable
private data class SpeechLocaleChoice(
    val tag: String,
    val labelAr: String,
    val labelEn: String,
    val labelFa: String,
)

@Composable
fun SpeechToTextScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onBack: () -> Unit,
) {
    val lang = uiState.language
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val stt = uiState.speechState
    val systemRecognizerAvailable = remember(context) {
        SpeechRecognizer.isRecognitionAvailable(context.applicationContext)
    }
    val recognizerAvailable = systemRecognizerAvailable ||
        (stt.localeTag.startsWith("ar", ignoreCase = true) && viewModel.canTranscribeClipOffline())

    var infoDialogOpen by remember { mutableStateOf(false) }
    var showMicPermissionRationale by remember { mutableStateOf(false) }
    val textHistory = rememberEditHistory(stt.displayText)

    LaunchedEffect(stt.displayText) {
        if (stt.displayText != textHistory.current) {
            textHistory.commit(stt.displayText, debounceMs = if (stt.isListening) 600L else 0L)
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.startLiveSpeechDictation()
        } else {
            viewModel.showStatusBanner(t(lang, "mic_permission_denied"))
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

    DisposableEffect(Unit) {
        onDispose {
            if (viewModel.uiState.value.speechState.isListening) {
                viewModel.stopSpeechTranscription()
            }
        }
    }

    val locales = remember {
        listOf(
            SpeechLocaleChoice("ar", "العربية", "Arabic", "عربی"),
            SpeechLocaleChoice("en-US", "English", "English", "انگلیسی"),
            SpeechLocaleChoice("fa-IR", "فارسی", "Persian", "فارسی"),
            SpeechLocaleChoice("tr-TR", "Türkçe", "Turkish", "ترکی"),
        )
    }

    fun launchWithMicPermission() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.startLiveSpeechDictation()
        } else {
            showMicPermissionRationale = true
        }
    }

    val transcriptVisualTransformation = remember(stt.uncertainWordRanges) {
        VisualTransformation { original ->
            val annotated = AnnotatedString.Builder(original.text)
            stt.uncertainWordRanges.forEach { range ->
                val start = range.first.coerceAtLeast(0)
                val end = (range.last + 1).coerceAtMost(original.length)
                if (start < end) {
                    annotated.addStyle(
                        SpanStyle(
                            color = StudioColors.RoseBright.copy(alpha = 0.85f),
                            textDecoration = TextDecoration.Underline,
                        ),
                        start,
                        end,
                    )
                }
            }
            TransformedText(annotated.toAnnotatedString(), OffsetMapping.Identity)
        }
    }

    StudioScrollColumn {
        StudioFocusTopBar(
            title = t(lang, "tool_speech_to_text"),
            subtitle = t(lang, "speech_to_text_subtitle"),
            language = lang,
            onBack = onBack,
            trailing = {
                UndoRedoActionsRow(
                    canUndo = textHistory.canUndo,
                    canRedo = textHistory.canRedo,
                    language = lang,
                    undoTag = "stt_undo_button",
                    redoTag = "stt_redo_button",
                    onUndo = { textHistory.undo()?.let { viewModel.updateSpeechTranscript(it) } },
                    onRedo = { textHistory.redo()?.let { viewModel.updateSpeechTranscript(it) } },
                    extraTrailing = {
                        InfoBadgeButton(
                            contentDescription = t(lang, "tool_speech_to_text"),
                            testTag = "stt_info_badge",
                            accentColor = StudioColors.Cyan,
                            onClick = { infoDialogOpen = true },
                        )
                    },
                )
            },
        )

        if (!recognizerAvailable) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = StudioColors.Amber.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, StudioColors.Amber.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = t(lang, "stt_unavailable"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = StudioColors.Amber,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }

        // Local Moonshine Arabic v2 + Silero VAD Card (Continuous Offline Recognition)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
            border = BorderStroke(
                1.dp,
                if (stt.localModelState.isInstalled) StudioColors.Emerald.copy(alpha = 0.55f) else StudioColors.Border,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("stt_moonshine_card"),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (lang == AppLanguage.EN) {
                            "Local Continuous Arabic Engine (Moonshine v2 + Silero VAD)"
                        } else {
                            "المحرك المحلي المستمر (Moonshine AR v2 + Silero VAD)"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = StudioColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (stt.localModelState.isInstalled) {
                            StudioColors.Emerald.copy(alpha = 0.18f)
                        } else {
                            StudioColors.ElevatedSurface
                        },
                        border = BorderStroke(
                            1.dp,
                            if (stt.localModelState.isInstalled) StudioColors.Emerald else StudioColors.Border,
                        ),
                    ) {
                        Text(
                            text = if (stt.localModelState.isInstalled) {
                                if (lang == AppLanguage.EN) "Offline Ready" else "جاهز أوفلاين"
                            } else {
                                "119 MB"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (stt.localModelState.isInstalled) StudioColors.Emerald else StudioColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                if (stt.localModelState.isDownloading) {
                    LinearProgressIndicator(
                        progress = { stt.localModelState.progress.coerceIn(0.02f, 1f) },
                        color = StudioColors.Cyan,
                        trackColor = StudioColors.BgTop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(CircleShape),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stt.localModelState.activeStageLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioColors.Cyan,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(
                            onClick = { viewModel.cancelLocalSpeechDownload() },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("stt_cancel_model_download_btn"),
                        ) {
                            Text(t(lang, "cancel"))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            SttBackendMode.AUTO to (if (lang == AppLanguage.EN) "Auto (Local First)" else "تلقائي (محلي أولاً)"),
                            SttBackendMode.LOCAL_MOONSHINE_AR to (if (lang == AppLanguage.EN) "Moonshine Offline" else "Moonshine محلي"),
                            SttBackendMode.ANDROID_SYSTEM to (if (lang == AppLanguage.EN) "Android Speech" else "خدمة أندرويد"),
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = stt.backendMode == mode,
                                enabled = mode != SttBackendMode.LOCAL_MOONSHINE_AR ||
                                    stt.localeTag.startsWith("ar", ignoreCase = true),
                                onClick = { viewModel.setSpeechBackendMode(mode) },
                                label = { Text(label, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioColors.Cyan.copy(alpha = 0.20f),
                                    selectedLabelColor = StudioColors.Cyan,
                                ),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("stt_backend_chip_${mode.name}"),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (!stt.localModelState.isInstalled) {
                            Button(
                                onClick = { viewModel.downloadLocalSpeechModel() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StudioColors.Emerald,
                                    contentColor = StudioColors.BgTop,
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("stt_download_moonshine_button"),
                            ) {
                                Text(
                                    text = if (lang == AppLanguage.EN) {
                                        "Download Offline Arabic Model (119 MB)"
                                    } else {
                                        "تحميل نموذج Moonshine العربي (119 MB أوفلاين)"
                                    },
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.clearLocalSpeechModel() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("stt_clear_moonshine_button"),
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (lang == AppLanguage.EN) "Remove Offline Cache" else "حذف الملفات المحملة")
                            }
                        }
                    }
                }

                if (!stt.localModelState.errorMessage.isNullOrBlank()) {
                    Text(
                        text = stt.localModelState.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioColors.RoseBright,
                    )
                }
            }
        }

        // Recognition Language Selector
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = t(lang, "stt_language_label"),
                    style = MaterialTheme.typography.titleSmall,
                    color = StudioColors.TextPrimary,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    locales.forEach { loc ->
                        val label = when (lang) {
                            AppLanguage.AR -> loc.labelAr
                            AppLanguage.EN -> loc.labelEn
                            AppLanguage.FA -> loc.labelFa
                            AppLanguage.TR -> loc.labelEn
                        }
                        val selected = stt.localeTag == loc.tag
                        val localeInteraction = remember { MutableInteractionSource() }
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setSpeechLocale(loc.tag) },
                            interactionSource = localeInteraction,
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioColors.Cyan.copy(alpha = 0.22f),
                                selectedLabelColor = StudioColors.Cyan,
                            ),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .lit(localeInteraction, MaterialTheme.colorScheme.primary, selected = selected, cornerRadius = 10.dp)
                                .testTag("stt_locale_${loc.tag}"),
                        )
                    }
                }
            }
        }

        // Live Mic & Studio Clip Transcription Controls
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
            border = BorderStroke(
                1.dp,
                if (stt.isListening) StudioColors.RoseBright.copy(alpha = 0.65f) else StudioColors.Border,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (stt.isListening) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(StudioColors.RoseBright),
                            )
                            LinearProgressIndicator(
                                progress = { (stt.rmsDb / 10f).coerceIn(0.08f, 1f) },
                                color = StudioColors.Cyan,
                                trackColor = StudioColors.BgTop,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(CircleShape),
                            )
                        }
                        if (!stt.isRoutingClip) {
                            val activeBadge = stt.activeEngineLabel.ifBlank { t(lang, "stt_continuous_active") }
                            Text(
                                text = activeBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioColors.Emerald,
                            )
                        }
                    }
                }

                if (stt.partialText.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StudioColors.ElevatedSurface,
                        border = BorderStroke(1.dp, StudioColors.Cyan.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stt.partialText,
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioColors.Cyan,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }

                if (!stt.errorMessage.isNullOrBlank()) {
                    Text(
                        text = stt.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioColors.Amber,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (stt.isListening) {
                        val stopInteraction = remember { MutableInteractionSource() }
                        Button(
                            onClick = { viewModel.stopSpeechTranscription() },
                            interactionSource = stopInteraction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioColors.Rose,
                                contentColor = StudioColors.TextPrimary,
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .lit(stopInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                                .testTag("stt_stop_button"),
                        ) {
                            Icon(Icons.Rounded.Stop, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(t(lang, "stt_stop_dictate"), style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        val dictateInteraction = remember { MutableInteractionSource() }
                        Button(
                            onClick = { launchWithMicPermission() },
                            enabled = recognizerAvailable,
                            interactionSource = dictateInteraction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioColors.Cyan,
                                contentColor = StudioColors.BgTop,
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .lit(dictateInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                                .testTag("stt_mic_dictate_button"),
                        ) {
                            Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(t(lang, "stt_dictate_mic"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // Editable Transcript Text Box + Copy / Share
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = t(lang, "tool_speech_to_text"),
                        style = MaterialTheme.typography.titleSmall,
                        color = StudioColors.TextPrimary,
                    )
                    if (stt.displayText.isNotBlank()) {
                        val clearInteraction = remember { MutableInteractionSource() }
                        IconButton(
                            onClick = {
                                textHistory.commit("")
                                viewModel.clearSpeechTranscription()
                            },
                            interactionSource = clearInteraction,
                            modifier = Modifier
                                .size(48.dp)
                                .lit(clearInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 24.dp)
                                .testTag("stt_clear_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = t(lang, "delete"),
                                tint = StudioColors.TextSecondary,
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = stt.displayText,
                    onValueChange = {
                        textHistory.commit(it, debounceMs = 350L)
                        viewModel.updateSpeechTranscript(it)
                    },
                    visualTransformation = transcriptVisualTransformation,
                    minLines = 6,
                    maxLines = 14,
                    placeholder = {
                        Text(
                            text = t(lang, "stt_input_placeholder"),
                            color = StudioColors.TextMuted,
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioColors.Cyan,
                        unfocusedBorderColor = StudioColors.Border,
                        focusedTextColor = StudioColors.TextPrimary,
                        unfocusedTextColor = StudioColors.TextPrimary,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stt_transcript_editor"),
                )

                if (stt.uncertainWordRanges.isNotEmpty()) {
                    Text(
                        text = if (lang == AppLanguage.EN) {
                            "Red underlines mark low-confidence recognition; tap and edit to correct."
                        } else {
                            "الكلمات ذات التسطير الأحمر قد تكون غير مؤكدة؛ المسها لتعديلها."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioColors.TextSecondary,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val copyInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = {
                            if (stt.displayText.isNotBlank()) {
                                clipboard.setText(AnnotatedString(stt.displayText))
                                viewModel.showStatusBanner(t(lang, "stt_copied_banner"))
                            }
                        },
                        enabled = stt.displayText.isNotBlank(),
                        interactionSource = copyInteraction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioColors.Cyan,
                            contentColor = StudioColors.BgTop,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(copyInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                            .testTag("stt_copy_text_button"),
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(t(lang, "stt_copy_text"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    val shareInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = {
                            if (stt.displayText.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, stt.displayText)
                                }
                                context.startActivity(Intent.createChooser(intent, t(lang, "stt_share_text")))
                            }
                        },
                        enabled = stt.displayText.isNotBlank(),
                        interactionSource = shareInteraction,
                        border = BorderStroke(1.dp, StudioColors.Emerald),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioColors.Emerald),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(shareInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                            .testTag("stt_share_text_button"),
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(t(lang, "stt_share_text"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }

    ToolInfoDialog(
        open = infoDialogOpen,
        title = t(lang, "tool_speech_to_text"),
        description = t(lang, "speech_to_text_info"),
        language = lang,
        onDismiss = { infoDialogOpen = false },
    )
}

@Composable
fun SmartMusicMixerScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onOpenPicker: (AudioPickerTarget) -> Unit,
    onBack: () -> Unit,
) {
    UnifiedMergeOverlayStudioScreen(
        uiState = uiState,
        viewModel = viewModel,
        initialMode = com.example.audio.MergeStudioMode.OVERLAY,
        initialAutoDucking = true,
        toolId = "smart_mixer",
        onOpenPicker = onOpenPicker,
        onBack = onBack,
    )
}
