package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.formatDurationMs
import com.example.i18n.t
import com.example.model.AppLanguage
import com.example.model.EffectType
import com.example.model.VOICE_PURIFY_PRESETS
import com.example.studio.ui.lit
import com.example.ui.components.SaveDraftDialog
import com.example.ui.components.SpectrumVisualizer
import com.example.ui.components.StudioFocusTopBar
import com.example.ui.components.MicrophonePermissionRationaleDialog
import com.example.ui.components.StudioParamSlider
import com.example.ui.components.StudioPreviewControlsBar
import com.example.ui.components.StudioScrollColumn
import com.example.ui.components.UndoRedoActionsRow
import com.example.ui.theme.StudioColors
import com.example.ui.theme.TelemetryTextStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PlaybackTelemetry
import com.example.model.RecorderTelemetry
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun RecordingStudioScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onOpenAudioPicker: () -> Unit,
    onBack: () -> Unit,
) {
    val lang = uiState.language
    val context = LocalContext.current
    var showMicPermissionRationale by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.startRecording()
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

    DisposableEffect(Unit) {
        onDispose {
            if (viewModel.uiState.value.recorder.isRecording) {
                viewModel.finishRecording()
            }
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "micPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (uiState.recorder.isRecording && !uiState.recorder.isPaused) 1.16f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "micScale",
    )

    StudioScrollColumn {
        StudioFocusTopBar(
            title = t(lang, "section_record_title"),
            subtitle = t(lang, "section_record_subtitle"),
            language = lang,
            onBack = onBack,
            trailing = {
                UndoRedoActionsRow(
                    canUndo = uiState.canUndoEdit,
                    canRedo = uiState.canRedoEdit,
                    language = lang,
                    undoTag = "recorder_undo_button",
                    redoTag = "recorder_redo_button",
                    onUndo = { viewModel.undoLastEdit() },
                    onRedo = { viewModel.redoLastEdit() },
                )
            },
        )

        if (!uiState.micPermissionError.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = StudioColors.Rose.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, StudioColors.Rose.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = t(lang, "mic_permission_title"),
                        style = MaterialTheme.typography.titleSmall,
                        color = StudioColors.RoseBright,
                    )
                    Text(
                        text = uiState.micPermissionError,
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioColors.TextPrimary,
                    )
                    val grantMicInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = { showMicPermissionRationale = true },
                        interactionSource = grantMicInteraction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioColors.Rose,
                            contentColor = StudioColors.TextPrimary,
                        ),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(grantMicInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 12.dp)
                            .testTag("grant_mic_permission_button"),
                    ) {
                        Text(t(lang, "grant_permission"))
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
            border = BorderStroke(1.dp, StudioColors.Border),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                        .clip(CircleShape)
                        .background(
                            if (uiState.recorder.isRecording) {
                                StudioColors.Rose.copy(alpha = 0.2f)
                            } else {
                                StudioColors.ElevatedSurface
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = t(lang, "section_record_title"),
                        tint = if (uiState.recorder.isRecording) StudioColors.Rose else StudioColors.Cyan,
                        modifier = Modifier.size(34.dp),
                    )
                }

                RecorderLiveTelemetrySection(
                    language = lang,
                    isRecording = uiState.recorder.isRecording,
                    isPaused = uiState.recorder.isPaused,
                    recorderFlow = viewModel.recorderTelemetry,
                    playbackFlow = viewModel.playbackTelemetry,
                )

                if (!uiState.recorder.isRecording) {
                    val startRecInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                viewModel.startRecording()
                            } else {
                                showMicPermissionRationale = true
                            }
                        },
                        interactionSource = startRecInteraction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioColors.Rose,
                            contentColor = StudioColors.TextPrimary,
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .lit(startRecInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 16.dp)
                            .testTag("record_start_button"),
                    ) {
                        Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(t(lang, "record_start"), style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val pauseRecInteraction = remember { MutableInteractionSource() }
                        Button(
                            onClick = {
                                if (uiState.recorder.isPaused) viewModel.resumeRecording() else viewModel.pauseRecording()
                            },
                            interactionSource = pauseRecInteraction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioColors.Amber,
                                contentColor = StudioColors.BgTop,
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .lit(pauseRecInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                                .testTag("record_pause_resume_button"),
                        ) {
                            Icon(
                                imageVector = if (uiState.recorder.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                contentDescription = null,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (uiState.recorder.isPaused) t(lang, "record_resume") else t(lang, "record_pause"),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        val discardRecInteraction = remember { MutableInteractionSource() }
                        OutlinedButton(
                            onClick = { viewModel.discardRecording() },
                            interactionSource = discardRecInteraction,
                            border = BorderStroke(1.dp, StudioColors.Rose),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioColors.RoseBright),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .lit(discardRecInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                                .testTag("record_discard_button"),
                        ) {
                            Text(t(lang, "record_discard"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    val finishRecInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = { viewModel.finishRecording() },
                        interactionSource = finishRecInteraction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioColors.Emerald,
                            contentColor = StudioColors.BgTop,
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .lit(finishRecInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 16.dp)
                            .testTag("record_finish_button"),
                    ) {
                        Icon(Icons.Rounded.Stop, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(t(lang, "record_finish"), style = MaterialTheme.typography.labelLarge)
                    }
                }

                val importAudioInteraction = remember { MutableInteractionSource() }
                OutlinedButton(
                    onClick = onOpenAudioPicker,
                    interactionSource = importAudioInteraction,
                    border = BorderStroke(1.dp, StudioColors.Cyan),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioColors.Cyan),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .lit(importAudioInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                        .testTag("record_import_audio_button"),
                ) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(t(lang, "import_audio"), style = MaterialTheme.typography.labelLarge)
                }

                uiState.importProgress?.let { prog ->
                    val byteTransferActive = !uiState.importBytesLabel.isNullOrBlank()
                    val byteFraction = uiState.importBytesProgress
                    val pct = (prog.coerceIn(0f, 1f) * 100f).toInt()
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = StudioColors.BgMid,
                        border = BorderStroke(1.dp, StudioColors.Cyan.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val phaseLabel = if (prog >= 0.50f && !uiState.importLabel.isNullOrBlank()) {
                                    uiState.importLabel
                                } else {
                                    t(lang, "uploading_to_cache_label")
                                }
                                Text(
                                    text = if (byteTransferActive && byteFraction == null) {
                                        phaseLabel.orEmpty()
                                    } else {
                                        "$phaseLabel $pct%"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StudioColors.Cyan,
                                    modifier = Modifier.weight(1f),
                                )
                                uiState.importBytesLabel?.let { bytes ->
                                    Text(
                                        text = bytes,
                                        style = TelemetryTextStyle.copy(fontSize = 11.sp),
                                        color = StudioColors.TextSecondary,
                                    )
                                }
                            }
                            if (byteTransferActive && byteFraction == null) {
                                LinearProgressIndicator(
                                    color = StudioColors.Cyan,
                                    trackColor = StudioColors.BgTop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(CircleShape)
                                        .testTag("recording_import_progress_bar"),
                                )
                            } else {
                                LinearProgressIndicator(
                                    progress = { prog.coerceIn(0f, 1f) },
                                    color = StudioColors.Cyan,
                                    trackColor = StudioColors.BgTop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(CircleShape)
                                        .testTag("recording_import_progress_bar"),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Saved Recordings Archive
        if (uiState.recordings.isNotEmpty()) {
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
                        text = if (lang == AppLanguage.EN) "Studio Recordings Archive" else "أرشيف التسجيلات والاستيراد",
                        style = MaterialTheme.typography.titleMedium,
                        color = StudioColors.TextPrimary,
                    )

                    uiState.recordings.forEach { rec ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = StudioColors.BgMid,
                            border = BorderStroke(1.dp, StudioColors.Border),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = rec.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = StudioColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${formatDurationMs(rec.durationMs)} • ${rec.sampleRate} Hz • ${rec.sourceType.name}",
                                        style = TelemetryTextStyle.copy(fontSize = 11.sp),
                                        color = StudioColors.TextSecondary,
                                    )
                                }

                                val loadRecInteraction = remember { MutableInteractionSource() }
                                IconButton(
                                    onClick = { viewModel.loadRecordingIntoStudio(rec) },
                                    interactionSource = loadRecInteraction,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = StudioColors.ElevatedSurface,
                                        contentColor = StudioColors.Cyan,
                                    ),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .lit(loadRecInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 24.dp)
                                        .testTag("recording_load_${rec.id}"),
                                ) {
                                    Icon(Icons.Rounded.FileOpen, contentDescription = t(lang, "load"))
                                }

                                val deleteRecInteraction = remember { MutableInteractionSource() }
                                IconButton(
                                    onClick = { viewModel.deleteRecording(rec) },
                                    interactionSource = deleteRecInteraction,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = StudioColors.ElevatedSurface,
                                        contentColor = StudioColors.RoseBright,
                                    ),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .lit(deleteRecInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 24.dp)
                                        .testTag("recording_delete_${rec.id}"),
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = t(lang, "delete"))
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
fun VoicePurifierScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onBack: () -> Unit,
) {
    val lang = uiState.language
    val nr = uiState.dspConfig.noiseReduction
    val enabled = uiState.dspConfig.isEnabled(EffectType.NOISE_REDUCTION)
    var saveDraftDialogOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.beginLiveToolSession("voice_purifier")
    }

    androidx.activity.compose.BackHandler {
        viewModel.discardLiveToolSession("voice_purifier")
        onBack()
    }

    LaunchedEffect(uiState.restoredToolSession?.id) {
        val restored = viewModel.consumeRestoredToolSession("voice_purifier")
        if (restored != null) {
            val fp = restored.floatParams
            viewModel.updateDspConfig { prev ->
                prev.copy(
                    noiseReduction = prev.noiseReduction.copy(
                        spectralStrength = fp["spectralStrength"] ?: prev.noiseReduction.spectralStrength,
                        highPassHz = fp["highPassHz"] ?: prev.noiseReduction.highPassHz,
                        lowPassHz = fp["lowPassHz"] ?: prev.noiseReduction.lowPassHz,
                        clarityBoostDb = fp["clarityBoostDb"] ?: prev.noiseReduction.clarityBoostDb,
                    ),
                ).withSlotEnabled(EffectType.NOISE_REDUCTION, true)
            }
        }
    }

    val latestNr by rememberUpdatedState(nr)
    val latestHasAudio by rememberUpdatedState(uiState.hasAudio)
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopToolPreview()
            val committed = viewModel.isLiveToolSessionCommitted("voice_purifier")
            viewModel.discardLiveToolSession("voice_purifier")
            if (committed && latestHasAudio) {
                val p = latestNr
                val pct = (p.spectralStrength * 100f).roundToInt()
                viewModel.saveToolSessionToCache(
                    toolId = "voice_purifier",
                    titleAr = "تصفية وعزل الضوضاء",
                    titleEn = "Noise Purifier",
                    summaryAr = "$pct% / ${p.highPassHz.roundToInt()}-${p.lowPassHz.roundToInt()} Hz",
                    summaryEn = "$pct% / ${p.highPassHz.roundToInt()}-${p.lowPassHz.roundToInt()} Hz",
                    floatParams = mapOf(
                        "spectralStrength" to p.spectralStrength,
                        "highPassHz" to p.highPassHz,
                        "lowPassHz" to p.lowPassHz,
                        "clarityBoostDb" to p.clarityBoostDb,
                    ),
                )
            }
        }
    }

    StudioScrollColumn {
        StudioFocusTopBar(
            title = t(lang, "section_purifier_title"),
            subtitle = t(lang, "section_purifier_subtitle"),
            language = lang,
            onBack = {
                viewModel.discardLiveToolSession("voice_purifier")
                onBack()
            },
            trailing = {
                UndoRedoActionsRow(
                    canUndo = uiState.canUndoEdit,
                    canRedo = uiState.canRedoEdit,
                    language = lang,
                    undoTag = "purifier_top_undo_button",
                    redoTag = "purifier_top_redo_button",
                    onUndo = { viewModel.undoLastEdit() },
                    onRedo = { viewModel.redoLastEdit() },
                    extraTrailing = {
                        Switch(
                            checked = enabled,
                            onCheckedChange = { viewModel.toggleEffectEnabled(EffectType.NOISE_REDUCTION, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = StudioColors.BgTop,
                                checkedTrackColor = StudioColors.Emerald,
                            ),
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("purifier_enable_switch"),
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
                    onResetZero = { viewModel.resetAllEffects() },
                    onOpenSaveDraft = { saveDraftDialogOpen = true },
                    embedded = true,
                )
            },
        )

        // Live Noise Suppression Telemetry Meter (isolated telemetry collector)
        PurifierLiveTelemetrySection(
            language = lang,
            playbackFlow = viewModel.playbackTelemetry,
        )

        // 4 Voice Purify Presets
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
                    text = if (lang == AppLanguage.EN) "Quick Purification Modes" else "أوضاع التنقية السريعة",
                    style = MaterialTheme.typography.titleMedium,
                    color = StudioColors.TextPrimary,
                )

                VOICE_PURIFY_PRESETS.forEach { preset ->
                    val isMatched = kotlin.math.abs(nr.spectralStrength - preset.spectralStrength) < 0.03f &&
                        kotlin.math.abs(nr.reductionDb - preset.reductionDb) < 1f
                    val presetInteraction = remember { MutableInteractionSource() }
                    Surface(
                        onClick = {
                            viewModel.applyVoicePurifyPreset(preset)
                            viewModel.ensureLivePreviewPlaying()
                        },
                        interactionSource = presetInteraction,
                        shape = RoundedCornerShape(14.dp),
                        color = if (isMatched) StudioColors.ElevatedSurface else StudioColors.BgMid,
                        border = BorderStroke(
                            1.dp,
                            if (isMatched) StudioColors.Emerald else StudioColors.Border,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .lit(presetInteraction, MaterialTheme.colorScheme.primary, selected = isMatched, cornerRadius = 14.dp)
                            .testTag("purify_preset_${preset.id}"),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = if (lang == AppLanguage.EN) preset.titleEn else preset.titleAr,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = StudioColors.TextPrimary,
                                )
                                Text(
                                    text = if (lang == AppLanguage.EN) preset.subtitleEn else preset.subtitleAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StudioColors.TextSecondary,
                                )
                            }
                            if (isMatched) {
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

        // 3 Core High-Impact Purifier Controls
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
                // 1) Smart Noise & Wind Isolation
                StudioParamSlider(
                    label = if (lang == AppLanguage.EN) "1. Smart Noise & Wind Isolation" else "1. عزل الضوضاء والهواء الذكي",
                    value = nr.spectralStrength,
                    min = 0f,
                    max = 1f,
                    formattedValue = "${(nr.spectralStrength * 100f).roundToInt()}%",
                    accentColor = StudioColors.Emerald,
                    testTag = "purifier_spectral_strength_slider",
                    onValueChange = { v ->
                        viewModel.updateDspConfig { prev ->
                            prev.copy(
                                noiseReduction = prev.noiseReduction.copy(
                                    spectralStrength = v,
                                    spectralCleanerEnabled = v > 0.02f,
                                    reductionDb = (v * 28f).coerceIn(0f, 30f),
                                ),
                            ).withSlotEnabled(EffectType.NOISE_REDUCTION, true)
                        }
                        viewModel.ensureLivePreviewPlaying()
                    },
                )

                // 2) Annoying Frequency Band Filters (Low Rumble + High Hiss)
                StudioParamSlider(
                    label = if (lang == AppLanguage.EN) "2A. Low-Cut Noise Filter (High-Pass)" else "2أ. قص الترددات المزعجة المنخفضة (High-Pass)",
                    value = nr.highPassHz,
                    min = 20f,
                    max = 450f,
                    formattedValue = "${nr.highPassHz.roundToInt()} Hz",
                    accentColor = StudioColors.Violet,
                    testTag = "purifier_highpass_slider",
                    onValueChange = { v ->
                        viewModel.updateDspConfig { prev ->
                            prev.copy(noiseReduction = prev.noiseReduction.copy(highPassHz = v))
                                .withSlotEnabled(EffectType.NOISE_REDUCTION, true)
                        }
                        viewModel.ensureLivePreviewPlaying()
                    },
                )

                StudioParamSlider(
                    label = if (lang == AppLanguage.EN) "2B. High-Cut Hiss Filter (Low-Pass)" else "2ب. قص الترددات المزعجة العالية (Low-Pass)",
                    value = nr.lowPassHz,
                    min = 2200f,
                    max = 16000f,
                    formattedValue = "${nr.lowPassHz.roundToInt()} Hz",
                    accentColor = StudioColors.Cyan,
                    testTag = "purifier_lowpass_slider",
                    onValueChange = { v ->
                        viewModel.updateDspConfig { prev ->
                            prev.copy(noiseReduction = prev.noiseReduction.copy(lowPassHz = v))
                                .withSlotEnabled(EffectType.NOISE_REDUCTION, true)
                        }
                        viewModel.ensureLivePreviewPlaying()
                    },
                )

                // 3) Vocal Presence & Clarity EQ
                StudioParamSlider(
                    label = if (lang == AppLanguage.EN) "3. Vocal Presence Clarity EQ" else "3. بروز ووضوح الصوت البشري",
                    value = nr.clarityBoostDb,
                    min = 0f,
                    max = 10f,
                    formattedValue = "+${"%.1f".format(nr.clarityBoostDb)} dB",
                    accentColor = StudioColors.Amber,
                    testTag = "purifier_clarity_slider",
                    onValueChange = { v ->
                        viewModel.updateDspConfig { prev ->
                            prev.copy(noiseReduction = prev.noiseReduction.copy(clarityBoostDb = v))
                                .withSlotEnabled(EffectType.NOISE_REDUCTION, true)
                        }
                        viewModel.ensureLivePreviewPlaying()
                    },
                )

                if (uiState.fricativeWarningPurifier) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StudioColors.Amber.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, StudioColors.Amber.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("purifier_fricative_warning"),
                    ) {
                        Text(
                            text = t(lang, "fricative_clarity_warning"),
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioColors.Amber,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }

                uiState.purifyProgress?.let { prog ->
                    LinearProgressIndicator(
                        progress = { prog.coerceIn(0f, 1f) },
                        color = StudioColors.Emerald,
                        trackColor = StudioColors.BgTop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                    )
                }

                val applyPurifyInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = { viewModel.purifyAudioOffline() },
                    enabled = uiState.hasAudio && !uiState.isBusy,
                    interactionSource = applyPurifyInteraction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioColors.Emerald,
                        contentColor = StudioColors.BgTop,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 50.dp)
                        .lit(applyPurifyInteraction, StudioColors.Emerald, cornerRadius = 14.dp)
                        .testTag("purifier_deep_clean_button"),
                ) {
                    Text(
                        text = if (lang == AppLanguage.EN) "Apply Deep Clean" else "تطبيق التنقية وحفظها",
                        style = MaterialTheme.typography.labelLarge,
                    )
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
}

@Composable
fun LiquidAudioWavePreview(
    amplitude: Float,
    waveformBars: List<Float>,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    // Perceptual boost so normal speech creates rich fluid motion while peaks hit smoothly at 1.0f
    val boostedTarget = if (isActive) {
        amplitude.coerceIn(0f, 1f).pow(0.65f).coerceIn(0.08f, 1f)
    } else {
        0f
    }

    val smoothAmp by animateFloatAsState(
        targetValue = boostedTarget,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = 260f,
        ),
        label = "liquidAmplitude",
    )

    val transition = rememberInfiniteTransition(label = "liquidPhase")
    val phasePrimary by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phasePrimary",
    )
    val phaseSecondary by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phaseSecondary",
    )
    val breathPulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathPulse",
    )

    val boxBg = Color(0xFF0B1120)
    val crestTeal = Color(0xFF2DD4BF)
    val crestSky = Color(0xFF38BDF8)
    val indigoDeep = Color(0xFF6366F1)
    val indigoSoft = Color(0xFF818CF8)

    val density = LocalDensity.current
    val fillPath = remember { Path() }
    val crestPath = remember { Path() }
    val borderStroke = remember(density) { Stroke(width = with(density) { 1.dp.toPx() }) }
    val borderCornerRadiusPx = with(density) { 20.dp.toPx() }
    val rimGlowStroke1 = remember(density) {
        Stroke(width = with(density) { 1.4.dp.toPx() * 3.0f }, cap = StrokeCap.Round, join = StrokeJoin.Round)
    }
    val rimStroke1 = remember(density) {
        Stroke(width = with(density) { 1.4.dp.toPx() }, cap = StrokeCap.Round, join = StrokeJoin.Round)
    }
    val rimGlowStroke2 = remember(density) {
        Stroke(width = with(density) { 1.8.dp.toPx() * 3.0f }, cap = StrokeCap.Round, join = StrokeJoin.Round)
    }
    val rimStroke2 = remember(density) {
        Stroke(width = with(density) { 1.8.dp.toPx() }, cap = StrokeCap.Round, join = StrokeJoin.Round)
    }
    val rimGlowStroke3 = remember(density) {
        Stroke(width = with(density) { 2.2.dp.toPx() * 3.0f }, cap = StrokeCap.Round, join = StrokeJoin.Round)
    }
    val rimStroke3 = remember(density) {
        Stroke(width = with(density) { 2.2.dp.toPx() }, cap = StrokeCap.Round, join = StrokeJoin.Round)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(boxBg),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val barCount = waveformBars.size
            val sampleStepPx = (w / 72f).coerceAtLeast(3f)

            // Subtle ambient underwater glow reacting to voice level
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        crestSky.copy(alpha = 0.14f + 0.24f * smoothAmp),
                        indigoDeep.copy(alpha = 0.08f + 0.14f * smoothAmp),
                        Color.Transparent,
                    ),
                    center = Offset(w * 0.5f, h * (0.68f - 0.18f * smoothAmp)),
                    radius = w * 0.62f,
                ),
                center = Offset(w * 0.5f, h * (0.68f - 0.18f * smoothAmp)),
                radius = w * 0.62f,
            )

            fun sampleBarEnvelope(normX: Float): Float {
                if (!isActive || barCount == 0) return 0f
                val scaled = normX.coerceIn(0f, 1f) * (barCount - 1)
                val i0 = scaled.toInt().coerceIn(0, barCount - 1)
                val i1 = (i0 + 1).coerceIn(0, barCount - 1)
                val frac = scaled - i0
                val raw = waveformBars[i0] * (1f - frac) + waveformBars[i1] * frac
                return (raw - 0.05f).coerceIn(0f, 1f)
            }

            fun drawLiquidLayer(
                baseFraction: Float,
                riseFraction: Float,
                cyclesA: Float,
                cyclesB: Float,
                speedOffsetA: Float,
                speedOffsetB: Float,
                waveAmpRatio: Float,
                barWeight: Float,
                fillBrush: Brush,
                rimColor: Color,
                glowColor: Color?,
                rimGlowStroke: Stroke,
                rimStroke: Stroke,
            ) {
                fillPath.reset()
                crestPath.reset()

                val baseY = h * (baseFraction - riseFraction * smoothAmp - 0.022f * breathPulse)
                val ampPx = h * (0.042f + waveAmpRatio * smoothAmp + 0.012f * breathPulse)

                var x = 0f
                var first = true
                while (x <= w + sampleStepPx) {
                    val clampedX = x.coerceAtMost(w)
                    val nx = clampedX / w
                    val angleA = nx * cyclesA * (2.0 * PI).toFloat() + speedOffsetA
                    val angleB = nx * cyclesB * (2.0 * PI).toFloat() + speedOffsetB
                    val env = sampleBarEnvelope(nx) * barWeight
                    val ripple = sin(angleA) * 0.65f + sin(angleB) * 0.35f
                    val y = (baseY - ripple * ampPx * (1f + env * 1.15f) - env * h * 0.10f)
                        .coerceIn(h * 0.10f, h * 0.94f)

                    if (first) {
                        fillPath.moveTo(clampedX, y)
                        crestPath.moveTo(clampedX, y)
                        first = false
                    } else {
                        fillPath.lineTo(clampedX, y)
                        crestPath.lineTo(clampedX, y)
                    }
                    if (clampedX >= w) break
                    x += sampleStepPx
                }

                fillPath.lineTo(w, h)
                fillPath.lineTo(0f, h)
                fillPath.close()

                drawPath(path = fillPath, brush = fillBrush)

                if (glowColor != null) {
                    drawPath(
                        path = crestPath,
                        color = glowColor,
                        style = rimGlowStroke,
                    )
                }

                drawPath(
                    path = crestPath,
                    color = rimColor,
                    style = rimStroke,
                )
            }

            // Layer 1 (Back): Deep translucent indigo-sky swell
            drawLiquidLayer(
                baseFraction = 0.62f,
                riseFraction = 0.22f,
                cyclesA = 1.45f,
                cyclesB = 2.6f,
                speedOffsetA = -phaseSecondary,
                speedOffsetB = phasePrimary * 0.75f,
                waveAmpRatio = 0.13f,
                barWeight = 0.35f,
                fillBrush = Brush.verticalGradient(
                    colors = listOf(
                        crestSky.copy(alpha = 0.36f + 0.18f * smoothAmp),
                        indigoSoft.copy(alpha = 0.35f),
                    ),
                    startY = h * 0.25f,
                    endY = h,
                ),
                rimColor = indigoSoft.copy(alpha = 0.45f + 0.25f * smoothAmp),
                glowColor = null,
                rimGlowStroke = rimGlowStroke1,
                rimStroke = rimStroke1,
            )

            // Layer 2 (Middle): Electric cyan-to-indigo flowing wave
            drawLiquidLayer(
                baseFraction = 0.69f,
                riseFraction = 0.20f,
                cyclesA = 2.1f,
                cyclesB = 3.4f,
                speedOffsetA = phaseSecondary * 1.25f,
                speedOffsetB = -phasePrimary * 1.15f,
                waveAmpRatio = 0.15f,
                barWeight = 0.6f,
                fillBrush = Brush.verticalGradient(
                    colors = listOf(
                        crestSky.copy(alpha = 0.62f),
                        indigoDeep.copy(alpha = 0.56f),
                    ),
                    startY = h * 0.30f,
                    endY = h,
                ),
                rimColor = crestSky.copy(alpha = 0.72f + 0.22f * smoothAmp),
                glowColor = crestSky.copy(alpha = 0.18f + 0.18f * smoothAmp),
                rimGlowStroke = rimGlowStroke2,
                rimStroke = rimStroke2,
            )

            // Layer 3 (Front): Luminous teal-cyan crest blending into electric indigo
            drawLiquidLayer(
                baseFraction = 0.76f,
                riseFraction = 0.18f,
                cyclesA = 1.8f,
                cyclesB = 3.1f,
                speedOffsetA = phasePrimary * 1.5f,
                speedOffsetB = -phaseSecondary * 1.4f,
                waveAmpRatio = 0.17f,
                barWeight = 0.9f,
                fillBrush = Brush.verticalGradient(
                    colors = listOf(
                        crestTeal.copy(alpha = 0.75f),
                        crestSky.copy(alpha = 0.62f),
                        indigoDeep.copy(alpha = 0.72f),
                    ),
                    startY = h * 0.36f,
                    endY = h,
                ),
                rimColor = crestTeal.copy(alpha = 0.92f),
                glowColor = crestTeal.copy(alpha = 0.28f + 0.22f * smoothAmp),
                rimGlowStroke = rimGlowStroke3,
                rimStroke = rimStroke3,
            )

            val borderColor = if (isActive) {
                crestSky.copy(alpha = 0.35f + 0.35f * smoothAmp)
            } else {
                StudioColors.BorderBright.copy(alpha = 0.45f)
            }
            drawRoundRect(
                color = borderColor,
                cornerRadius = CornerRadius(borderCornerRadiusPx, borderCornerRadiusPx),
                style = borderStroke,
            )
        }
    }
}

@Composable
private fun RecorderLiveTelemetrySection(
    language: AppLanguage,
    isRecording: Boolean,
    isPaused: Boolean,
    recorderFlow: StateFlow<RecorderTelemetry>,
    playbackFlow: StateFlow<PlaybackTelemetry>,
) {
    val rec by recorderFlow.collectAsStateWithLifecycle()
    val playback by playbackFlow.collectAsStateWithLifecycle()

    Text(
        text = formatDurationMs(rec.elapsedMs),
        style = TelemetryTextStyle.copy(fontSize = 32.sp),
        color = if (isRecording) StudioColors.RoseBright else StudioColors.TextPrimary,
    )

    val liveAmplitude = when {
        isRecording && !isPaused -> rec.micLevel
        playback.isPlaying -> playback.outputLevel
        else -> 0f
    }
    val liveBars = if (isRecording) {
        rec.waveformBars
    } else {
        playback.waveformBars
    }
    val liveSpectrum = if (isRecording) {
        rec.spectrumBands
    } else {
        playback.spectrumBands
    }
    val liveActive = (isRecording && !isPaused) || playback.isPlaying

    LiquidAudioWavePreview(
        amplitude = liveAmplitude,
        waveformBars = liveBars,
        isActive = liveActive,
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .testTag("recording_liquid_wave_preview"),
    )

    SpectrumVisualizer(
        spectrumBands = liveSpectrum,
        isActive = liveActive,
        language = language,
        peakLevel = liveAmplitude,
        accentColor = if (isRecording) StudioColors.RoseBright else StudioColors.Cyan,
    )

    // Mic Input Meter
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = t(language, "mic_level"),
                style = MaterialTheme.typography.labelMedium,
                color = StudioColors.TextSecondary,
            )
            Text(
                text = "${(rec.micLevel * 100f).roundToInt()}%",
                style = TelemetryTextStyle,
                color = StudioColors.Cyan,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(StudioColors.BgTop),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(rec.micLevel.coerceIn(0.02f, 1f))
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (rec.micLevel > 0.85f) StudioColors.Rose else StudioColors.Emerald,
                    ),
            )
        }
    }
}

@Composable
private fun PurifierLiveTelemetrySection(
    language: AppLanguage,
    playbackFlow: StateFlow<PlaybackTelemetry>,
) {
    val playback by playbackFlow.collectAsStateWithLifecycle()
    val suppNorm = (playback.noiseSuppressionDb / 32f).coerceIn(0f, 1f)

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
                Text(
                    text = if (language == AppLanguage.EN) "Live Noise Suppression" else "مستوى عزل الضوضاء المباشر",
                    style = MaterialTheme.typography.titleSmall,
                    color = StudioColors.TextPrimary,
                )
                Text(
                    text = "-${"%.1f".format(playback.noiseSuppressionDb)} dB",
                    style = TelemetryTextStyle,
                    color = StudioColors.Emerald,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(StudioColors.BgTop),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(suppNorm.coerceAtLeast(0.02f))
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(StudioColors.Emerald),
                )
            }
        }
    }

    SpectrumVisualizer(
        spectrumBands = playback.spectrumBands,
        isActive = playback.isPlaying,
        language = language,
        peakLevel = playback.outputLevel,
        accentColor = StudioColors.Emerald,
    )
}

