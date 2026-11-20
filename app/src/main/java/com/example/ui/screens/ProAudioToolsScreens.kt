package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.audio.MergeStudioMode
import com.example.audio.SilenceInsertMode
import com.example.audio.TARGET_SAMPLE_RATE
import com.example.audio.formatDurationMs
import com.example.i18n.t
import com.example.model.AppLanguage
import com.example.model.AudioPickerTarget
import com.example.model.EffectType
import com.example.model.ExportAudioFormat
import com.example.model.VocalSplitMode
import com.example.studio.ui.lit
import com.example.ui.components.PreviewAndApplyActionRow
import com.example.ui.components.SaveDraftDialog
import com.example.ui.components.StudioFocusTopBar
import com.example.ui.components.StudioParamSlider
import com.example.ui.components.StudioPreviewControlsBar
import com.example.ui.components.StudioScrollColumn
import com.example.ui.components.UndoRedoActionsRow
import com.example.ui.components.rememberEditHistory
import com.example.ui.theme.StudioColors
import com.example.ui.theme.TelemetryTextStyle
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ActiveTrackPromptCard(
    uiState: StudioUiState,
    onPickMainAudio: () -> Unit,
) {
    val lang = uiState.language
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
        border = BorderStroke(1.dp, if (uiState.hasAudio) StudioColors.Border else StudioColors.Cyan.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (uiState.hasAudio) uiState.activeProjectName else t(lang, "no_audio_loaded"),
                        style = MaterialTheme.typography.titleSmall,
                        color = StudioColors.TextPrimary,
                    )
                    if (uiState.hasAudio) {
                        Text(
                            text = formatDurationMs(uiState.durationMs),
                            style = TelemetryTextStyle,
                            color = StudioColors.Cyan,
                        )
                    }
                }
            }
            val pickInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = onPickMainAudio,
                interactionSource = pickInteraction,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, StudioColors.Cyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .lit(pickInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 12.dp)
                    .testTag("tool_pick_audio_button"),
            ) {
                Icon(Icons.Rounded.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(t(lang, "audio_tools_pick_source"))
            }
        }
    }
}

@Composable
fun AudioMergeScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onOpenPicker: (AudioPickerTarget) -> Unit,
    onBack: () -> Unit,
) {
    UnifiedMergeOverlayStudioScreen(
        uiState = uiState,
        viewModel = viewModel,
        initialMode = MergeStudioMode.SEQUENTIAL,
        initialAutoDucking = false,
        toolId = "merge_studio",
        onOpenPicker = onOpenPicker,
        onBack = onBack,
    )
}

@Composable
fun AudioSplitScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onOpenPicker: (AudioPickerTarget) -> Unit,
    onBack: () -> Unit,
) {
    val lang = uiState.language
    var splitPoint by remember { mutableFloatStateOf(0.5f) }
    var saveBothToLibrary by remember { mutableStateOf(true) }
    var loadPart by remember { mutableIntStateOf(1) }
    var preferVmUndo by remember { mutableStateOf(false) }
    val splitHistory = rememberEditHistory(Triple(splitPoint, loadPart, saveBothToLibrary))

    val splitMs = (uiState.durationMs * splitPoint).roundToInt().toLong()

    StudioScrollColumn {
        StudioFocusTopBar(
            title = t(lang, "tool_split_tracks"),
            subtitle = if (lang == AppLanguage.EN) "Split by timeline point or isolate Stereo L/R channels" else "تقسيم المقطع زمنياً أو فصل قنوات الستيريو (يمين / يسار)",
            language = lang,
            onBack = onBack,
            trailing = {
                UndoRedoActionsRow(
                    canUndo = splitHistory.canUndo || uiState.canUndoEdit,
                    canRedo = splitHistory.canRedo || uiState.canRedoEdit,
                    language = lang,
                    undoTag = "split_undo_button",
                    redoTag = "split_redo_button",
                    onUndo = {
                        if ((preferVmUndo && uiState.canUndoEdit) || !splitHistory.canUndo) {
                            viewModel.undoLastEdit()
                        } else {
                            splitHistory.undo()?.let { (pt, part, saveBoth) ->
                                splitPoint = pt
                                loadPart = part
                                saveBothToLibrary = saveBoth
                            }
                        }
                    },
                    onRedo = {
                        if ((preferVmUndo && uiState.canRedoEdit) || !splitHistory.canRedo) {
                            viewModel.redoLastEdit()
                        } else {
                            splitHistory.redo()?.let { (pt, part, saveBoth) ->
                                splitPoint = pt
                                loadPart = part
                                saveBothToLibrary = saveBoth
                            }
                        }
                    },
                )
            },
        )

        ActiveTrackPromptCard(uiState = uiState, onPickMainAudio = { onOpenPicker(AudioPickerTarget.IMPORT_MAIN) })

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
                    label = if (lang == AppLanguage.EN) "Split Point Position" else "نقطة التقسيم الزمنية",
                    value = splitPoint,
                    min = 0.05f,
                    max = 0.95f,
                    formattedValue = formatDurationMs(splitMs),
                    accentColor = StudioColors.Cyan,
                    testTag = "split_point_slider",
                    onValueChange = {
                        preferVmUndo = false
                        splitPoint = it
                        splitHistory.commit(Triple(it, loadPart, saveBothToLibrary), debounceMs = 350L)
                    },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val part1Interaction = remember { MutableInteractionSource() }
                    FilterChip(
                        selected = loadPart == 1,
                        onClick = {
                            preferVmUndo = false
                            loadPart = 1
                            splitHistory.commit(Triple(splitPoint, 1, saveBothToLibrary))
                        },
                        interactionSource = part1Interaction,
                        label = { Text(if (lang == AppLanguage.EN) "Open Part 1" else "فتح الجزء الأول") },
                        modifier = Modifier
                            .weight(1f)
                            .lit(part1Interaction, MaterialTheme.colorScheme.primary, selected = loadPart == 1, cornerRadius = 10.dp),
                    )
                    val part2Interaction = remember { MutableInteractionSource() }
                    FilterChip(
                        selected = loadPart == 2,
                        onClick = {
                            preferVmUndo = false
                            loadPart = 2
                            splitHistory.commit(Triple(splitPoint, 2, saveBothToLibrary))
                        },
                        interactionSource = part2Interaction,
                        label = { Text(if (lang == AppLanguage.EN) "Open Part 2" else "فتح الجزء الثاني") },
                        modifier = Modifier
                            .weight(1f)
                            .lit(part2Interaction, MaterialTheme.colorScheme.primary, selected = loadPart == 2, cornerRadius = 10.dp),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (lang == AppLanguage.EN) "Save both parts to studio archive" else "حفظ الجزئين في أرشيف الاستوديو",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioColors.TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = saveBothToLibrary,
                        onCheckedChange = { saveBothToLibrary = it },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    )
                }

                val splitExecInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        preferVmUndo = true
                        viewModel.splitAudioAtFraction(splitPoint, loadPart, saveBothToLibrary)
                    },
                    enabled = uiState.hasAudio && !uiState.isBusy && !uiState.isProcessing,
                    interactionSource = splitExecInteraction,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioColors.Cyan, contentColor = StudioColors.BgTop),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .lit(splitExecInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                        .testTag("split_execute_button"),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.CallSplit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(t(lang, "apply_now"))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val leftInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = { viewModel.splitStereoChannel(keepLeft = true) },
                        enabled = uiState.hasAudio && !uiState.isBusy && !uiState.isProcessing,
                        interactionSource = leftInteraction,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(leftInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 16.dp),
                    ) {
                        Text(if (lang == AppLanguage.EN) "Isolate Left (L)" else "عزل القناة اليسرى")
                    }
                    val rightInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = { viewModel.splitStereoChannel(keepLeft = false) },
                        enabled = uiState.hasAudio && !uiState.isBusy && !uiState.isProcessing,
                        interactionSource = rightInteraction,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(rightInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 16.dp),
                    ) {
                        Text(if (lang == AppLanguage.EN) "Isolate Right (R)" else "عزل القناة اليمنى")
                    }
                }
            }
        }

        var vocalSplitMode by remember { mutableStateOf(VocalSplitMode.VOCALS_ONLY) }
        var vocalBlendRatio by remember { mutableFloatStateOf(0.55f) }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
            border = BorderStroke(1.dp, StudioColors.Violet.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = t(lang, "vocal_splitter_title"),
                    style = MaterialTheme.typography.titleMedium,
                    color = StudioColors.Violet,
                )
                Text(
                    text = t(lang, "vocal_splitter_desc"),
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioColors.TextSecondary,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    VocalSplitMode.entries.forEach { mode ->
                        val selected = vocalSplitMode == mode
                        val modeInteraction = remember { MutableInteractionSource() }
                        FilterChip(
                            selected = selected,
                            onClick = { vocalSplitMode = mode },
                            interactionSource = modeInteraction,
                            label = { Text(t(lang, mode.titleKey)) },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .lit(modeInteraction, MaterialTheme.colorScheme.primary, selected = selected, cornerRadius = 10.dp)
                                .testTag("split_vocal_mode_${mode.name}"),
                        )
                    }
                }

                if (vocalSplitMode == VocalSplitMode.CUSTOM_BLEND) {
                    StudioParamSlider(
                        label = t(lang, "vocal_splitter_blend_label"),
                        value = vocalBlendRatio,
                        min = 0f,
                        max = 1f,
                        formattedValue = "${(vocalBlendRatio * 100f).roundToInt()}%",
                        accentColor = StudioColors.Violet,
                        testTag = "split_vocal_blend_slider",
                        onValueChange = { vocalBlendRatio = it },
                    )
                }

                PreviewAndApplyActionRow(
                    isPreviewPlaying = uiState.isToolPreviewPlaying,
                    previewEnabled = uiState.hasAudio && !uiState.isBusy && !uiState.isProcessing,
                    applyEnabled = uiState.hasAudio && !uiState.isBusy && !uiState.isProcessing,
                    language = lang,
                    previewTag = "split_vocal_preview_button",
                    applyTag = "split_vocal_apply_button",
                    applyColor = StudioColors.Violet,
                    onPreviewClick = { viewModel.previewVocalSplitter(vocalSplitMode, vocalBlendRatio) },
                    onApplyClick = {
                        preferVmUndo = true
                        viewModel.applyVocalSplitter(vocalSplitMode, vocalBlendRatio)
                    },
                )
            }
        }
    }
}

@Composable
fun PitchAndSpeedScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onOpenPicker: (AudioPickerTarget) -> Unit,
    onBack: () -> Unit,
) {
    val lang = uiState.language
    val vmSemitones = uiState.dspConfig.pitchShift.semitones
    val vmTempo = uiState.dspConfig.timeStretch.tempoRatio
    val vmPreserveFormants = uiState.dspConfig.pitchShift.preserveFormants

    var localSemitones by remember { mutableFloatStateOf(vmSemitones) }
    var localTempo by remember { mutableFloatStateOf(vmTempo) }
    var localPreserveFormants by remember { mutableStateOf(vmPreserveFormants) }
    var userEditedSliders by remember { mutableStateOf(false) }
    var saveDraftDialogOpen by remember { mutableStateOf(false) }

    // Keep local slider values synced with Undo / Redo / Reset-to-zero / Apply resets
    LaunchedEffect(vmSemitones, vmTempo, vmPreserveFormants) {
        if (abs(localSemitones - vmSemitones) > 0.01f) {
            localSemitones = vmSemitones
        }
        if (abs(localTempo - vmTempo) > 0.005f) {
            localTempo = vmTempo
        }
        if (localPreserveFormants != vmPreserveFormants) {
            localPreserveFormants = vmPreserveFormants
        }
    }

    LaunchedEffect(Unit) {
        viewModel.beginLiveToolSession("pitch_speed")
        viewModel.setPitchSpeedLivePreviewActive(true)
    }

    // 80ms debounce for zero-stutter native PlaybackParams live preview
    LaunchedEffect(localSemitones, localTempo, localPreserveFormants, userEditedSliders) {
        if (!userEditedSliders) return@LaunchedEffect
        delay(80L)
        viewModel.updatePitchSpeedParamsLive(localSemitones, localTempo, localPreserveFormants)
        viewModel.ensureLivePreviewPlaying()
    }

    androidx.activity.compose.BackHandler {
        viewModel.setPitchSpeedLivePreviewActive(false)
        viewModel.discardLiveToolSession("pitch_speed")
        onBack()
    }

    LaunchedEffect(uiState.restoredToolSession?.id) {
        val restored = viewModel.consumeRestoredToolSession("pitch_speed")
        if (restored != null) {
            val st = restored.floatParams["semitones"] ?: 0f
            val tr = restored.floatParams["tempoRatio"] ?: 1f
            val pf = (restored.floatParams["preserveFormants"] ?: 1f) >= 0.5f
            localSemitones = st
            localTempo = tr
            localPreserveFormants = pf
            viewModel.updatePitchSpeedParamsLive(st, tr, pf)
        }
    }

    val latestSemi by rememberUpdatedState(localSemitones)
    val latestTempo by rememberUpdatedState(localTempo)
    val latestFormants by rememberUpdatedState(localPreserveFormants)
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopToolPreview()
            viewModel.setPitchSpeedLivePreviewActive(false)
            val committed = viewModel.isLiveToolSessionCommitted("pitch_speed")
            viewModel.discardLiveToolSession("pitch_speed")
            if (committed) {
                viewModel.saveToolSessionToCache(
                    toolId = "pitch_speed",
                    titleAr = "الطبقة والسرعة",
                    titleEn = "Pitch & Speed",
                    summaryAr = "${"%+.1f".format(latestSemi)} st • ${"%.2f".format(latestTempo)}x",
                    summaryEn = "${"%+.1f".format(latestSemi)} st • ${"%.2f".format(latestTempo)}x",
                    floatParams = mapOf(
                        "semitones" to latestSemi,
                        "tempoRatio" to latestTempo,
                        "preserveFormants" to if (latestFormants) 1f else 0f,
                    ),
                )
            }
        }
    }

    StudioScrollColumn {
        StudioFocusTopBar(
            title = t(lang, "tool_pitch_speed"),
            subtitle = if (lang == AppLanguage.EN) {
                "Decoupled studio pitch & speed live preview with FFmpeg Rubberband baking"
            } else {
                "تعديل حدة الطبقة والسرعة بشكل مستقل مع معاينة حية فورية ومعالجة احترافية"
            },
            language = lang,
            onBack = {
                viewModel.setPitchSpeedLivePreviewActive(false)
                viewModel.discardLiveToolSession("pitch_speed")
                onBack()
            },
            trailing = {
                UndoRedoActionsRow(
                    canUndo = uiState.canUndoEdit,
                    canRedo = uiState.canRedoEdit,
                    language = lang,
                    undoTag = "pitch_speed_top_undo_button",
                    redoTag = "pitch_speed_top_redo_button",
                    onUndo = {
                        userEditedSliders = false
                        viewModel.undoLastEdit()
                    },
                    onRedo = {
                        userEditedSliders = false
                        viewModel.redoLastEdit()
                    },
                )
            },
            bottomContent = {
                StudioPreviewControlsBar(
                    uiState = uiState,
                    onHoldBypassChange = { viewModel.setHoldBypass(it) },
                    onToggleBypass = { viewModel.toggleBypass() },
                    onUndo = {
                        userEditedSliders = false
                        viewModel.undoLastEdit()
                    },
                    onRedo = {
                        userEditedSliders = false
                        viewModel.redoLastEdit()
                    },
                    onResetZero = {
                        userEditedSliders = false
                        localSemitones = 0f
                        localTempo = 1.0f
                        localPreserveFormants = true
                        viewModel.resetAllEffects()
                    },
                    onOpenSaveDraft = { saveDraftDialogOpen = true },
                    embedded = true,
                )
            },
        )

        ActiveTrackPromptCard(uiState = uiState, onPickMainAudio = { onOpenPicker(AudioPickerTarget.IMPORT_MAIN) })

        val bypassActive = uiState.dspConfig.bypassAll || uiState.isHoldBypassing
        if (bypassActive) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = StudioColors.Amber.copy(alpha = 0.14f)),
                border = BorderStroke(1.dp, StudioColors.Amber.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (lang == AppLanguage.EN) {
                        "Bypass Active: Playing original unmodified audio"
                    } else {
                        "وضع مقارنة الأصل مفعّل: يتم الآن سماع الصوت الأصلي بدون تعديل"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = StudioColors.Amber,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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
                    label = t(lang, "effect_pitch_shift"),
                    value = localSemitones,
                    min = -12f,
                    max = 12f,
                    formattedValue = "${"%+.1f".format(localSemitones)} st",
                    accentColor = StudioColors.Cyan,
                    testTag = "pitch_tool_semitones",
                    onValueChange = {
                        userEditedSliders = true
                        localSemitones = it
                    },
                )

                StudioParamSlider(
                    label = t(lang, "effect_time_stretch"),
                    value = localTempo,
                    min = 0.5f,
                    max = 2.5f,
                    formattedValue = "${"%.2f".format(localTempo)}x",
                    accentColor = StudioColors.Violet,
                    testTag = "speed_tool_tempo",
                    onValueChange = {
                        userEditedSliders = true
                        localTempo = it
                    },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = t(lang, "pitch_preserve_formants"),
                            style = MaterialTheme.typography.labelLarge,
                            color = StudioColors.TextPrimary,
                        )
                        Text(
                            text = t(lang, "pitch_preserve_formants_desc"),
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioColors.TextMuted,
                        )
                    }
                    Switch(
                        checked = localPreserveFormants,
                        onCheckedChange = { checked ->
                            userEditedSliders = true
                            localPreserveFormants = checked
                        },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("pitch_preserve_formants_switch"),
                    )
                }

                if (uiState.isProcessing || uiState.purifyProgress != null) {
                    val prog = uiState.purifyProgress ?: 0f
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = if (lang == AppLanguage.EN) "Rendering high-precision DSP..." else "جاري المعالجة الصوتية عالية الدقة...",
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioColors.Cyan,
                            )
                            Text(
                                text = "${(prog.coerceIn(0f, 1f) * 100f).roundToInt()}%",
                                style = TelemetryTextStyle,
                                color = StudioColors.Cyan,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { prog.coerceIn(0f, 1f) },
                            color = StudioColors.Cyan,
                            trackColor = StudioColors.BgTop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .testTag("pitch_speed_progress_bar"),
                        )
                    }
                }

                val applyPitchInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        userEditedSliders = false
                        viewModel.applyPitchAndSpeed(localSemitones, localTempo, localPreserveFormants)
                    },
                    enabled = uiState.hasAudio && !uiState.isBusy && !uiState.isProcessing &&
                        (abs(localSemitones) >= 0.05f || abs(localTempo - 1f) >= 0.02f),
                    interactionSource = applyPitchInteraction,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioColors.Cyan, contentColor = StudioColors.BgTop),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 50.dp)
                        .lit(applyPitchInteraction, StudioColors.Cyan, cornerRadius = 14.dp)
                        .testTag("pitch_speed_bake_button"),
                ) {
                    Text(t(lang, "apply_now"), style = MaterialTheme.typography.labelLarge)
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
fun ReverseAudioScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onOpenPicker: (AudioPickerTarget) -> Unit,
    onBack: () -> Unit,
) {
    val lang = uiState.language

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopToolPreview()
            viewModel.saveToolSessionToCache(
                toolId = "reverse_audio",
                titleAr = "عكس الصوت",
                titleEn = "Reverse Audio",
                summaryAr = "جلسة عكس المقطع",
                summaryEn = "Reverse clip session",
                floatParams = emptyMap(),
            )
        }
    }

    StudioScrollColumn {
        StudioFocusTopBar(
            title = t(lang, "tool_reverse_audio"),
            subtitle = if (lang == AppLanguage.EN) "Reverse audio playback cleanly with Undo support" else "عكس اتجاه المقطع الصوتي مع دعم التراجع الفوري",
            language = lang,
            onBack = onBack,
            trailing = {
                UndoRedoActionsRow(
                    canUndo = uiState.canUndoEdit,
                    canRedo = uiState.canRedoEdit,
                    language = lang,
                    undoTag = "reverse_undo_button",
                    redoTag = "reverse_redo_button",
                    onUndo = { viewModel.undoLastEdit() },
                    onRedo = { viewModel.redoLastEdit() },
                )
            },
        )

        ActiveTrackPromptCard(uiState = uiState, onPickMainAudio = { onOpenPicker(AudioPickerTarget.IMPORT_MAIN) })

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
                PreviewAndApplyActionRow(
                    isPreviewPlaying = uiState.isToolPreviewPlaying,
                    previewEnabled = uiState.hasAudio && !uiState.isBusy && !uiState.isProcessing,
                    applyEnabled = uiState.hasAudio && !uiState.isBusy && !uiState.isProcessing,
                    language = lang,
                    previewTag = "reverse_preview_button",
                    applyTag = "reverse_apply_button",
                    applyLabel = t(lang, "apply_now"),
                    applyColor = StudioColors.Violet,
                    onPreviewClick = { viewModel.previewReverseAudio() },
                    onApplyClick = { viewModel.applyReverseAudio() },
                )
            }
        }
    }
}

@Composable
fun SurroundStereoScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onOpenPicker: (AudioPickerTarget) -> Unit,
    onBack: () -> Unit,
) {
    val lang = uiState.language
    // True Zero 0% neutral start across all Surround Stereo sliders
    var midAdjustPct by remember { mutableFloatStateOf(0f) }
    var sideBoostPct by remember { mutableFloatStateOf(0f) }
    var widthPct by remember { mutableFloatStateOf(0f) }
    var saveDraftDialogOpen by remember { mutableStateOf(false) }
    var preferVmUndo by remember { mutableStateOf(false) }
    val stereoHistory = rememberEditHistory(Triple(midAdjustPct, sideBoostPct, widthPct))

    LaunchedEffect(Unit) {
        viewModel.beginLiveToolSession("surround_stereo")
    }

    androidx.activity.compose.BackHandler {
        viewModel.discardLiveToolSession("surround_stereo")
        onBack()
    }

    LaunchedEffect(uiState.restoredToolSession?.id) {
        val restored = viewModel.consumeRestoredToolSession("surround_stereo")
        if (restored != null) {
            midAdjustPct = restored.floatParams["midAdjustPct"] ?: 0f
            sideBoostPct = restored.floatParams["sideBoostPct"] ?: 0f
            widthPct = restored.floatParams["widthPct"] ?: 0f
        }
    }

    val latestMid by rememberUpdatedState(midAdjustPct)
    val latestSide by rememberUpdatedState(sideBoostPct)
    val latestWidth by rememberUpdatedState(widthPct)
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopToolPreview()
            val committed = viewModel.isLiveToolSessionCommitted("surround_stereo")
            viewModel.discardLiveToolSession("surround_stereo")
            if (committed) {
                viewModel.saveToolSessionToCache(
                    toolId = "surround_stereo",
                    titleAr = "ستيريو محيطي M/S",
                    titleEn = "Surround Stereo",
                    summaryAr = "Mid ${latestMid.roundToInt()}% • Side ${latestSide.roundToInt()}% • ${latestWidth.roundToInt()}%",
                    summaryEn = "Mid ${latestMid.roundToInt()}% • Side ${latestSide.roundToInt()}% • ${latestWidth.roundToInt()}%",
                    floatParams = mapOf(
                        "midAdjustPct" to latestMid,
                        "sideBoostPct" to latestSide,
                        "widthPct" to latestWidth,
                    ),
                )
            }
        }
    }

    StudioScrollColumn {
        StudioFocusTopBar(
            title = t(lang, "tool_stereo_surround"),
            subtitle = if (lang == AppLanguage.EN) "Convert mono or narrow clips into wide spatial stereo" else "توسيع المجال الصوتي M/S وتحويل المونو إلى ستيريو محيطي",
            language = lang,
            onBack = {
                viewModel.discardLiveToolSession("surround_stereo")
                onBack()
            },
            trailing = {
                UndoRedoActionsRow(
                    canUndo = stereoHistory.canUndo || uiState.canUndoEdit,
                    canRedo = stereoHistory.canRedo || uiState.canRedoEdit,
                    language = lang,
                    undoTag = "stereo_top_undo_button",
                    redoTag = "stereo_top_redo_button",
                    onUndo = {
                        if ((preferVmUndo && uiState.canUndoEdit) || !stereoHistory.canUndo) {
                            viewModel.undoLastEdit()
                        } else {
                            stereoHistory.undo()?.let { (m, s, w) ->
                                midAdjustPct = m
                                sideBoostPct = s
                                widthPct = w
                            }
                        }
                    },
                    onRedo = {
                        if ((preferVmUndo && uiState.canRedoEdit) || !stereoHistory.canRedo) {
                            viewModel.redoLastEdit()
                        } else {
                            stereoHistory.redo()?.let { (m, s, w) ->
                                midAdjustPct = m
                                sideBoostPct = s
                                widthPct = w
                            }
                        }
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
                        midAdjustPct = 0f
                        sideBoostPct = 0f
                        widthPct = 0f
                        stereoHistory.commit(Triple(0f, 0f, 0f))
                        viewModel.updateSurroundLiveParams(0f, 0f, 0f)
                        viewModel.resetAllEffects()
                    },
                    onOpenSaveDraft = { saveDraftDialogOpen = true },
                    embedded = true,
                )
            },
        )

        ActiveTrackPromptCard(uiState = uiState, onPickMainAudio = { onOpenPicker(AudioPickerTarget.IMPORT_MAIN) })

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
                    label = if (lang == AppLanguage.EN) "Mid Center Offset" else "إزاحة المنتصف (Mid)",
                    value = midAdjustPct,
                    min = -60f,
                    max = 60f,
                    formattedValue = "${if (midAdjustPct > 0f) "+" else ""}${midAdjustPct.roundToInt()}%",
                    accentColor = StudioColors.Cyan,
                    testTag = "stereo_mid_slider",
                    onValueChange = {
                        preferVmUndo = false
                        midAdjustPct = it
                        stereoHistory.commit(Triple(it, sideBoostPct, widthPct), debounceMs = 350L)
                        viewModel.updateSurroundLiveParams(it, sideBoostPct, widthPct)
                        viewModel.ensureLivePreviewPlaying()
                    },
                )
                StudioParamSlider(
                    label = if (lang == AppLanguage.EN) "Side Surround Boost" else "توسيع الجوانب المحيطية (Side)",
                    value = sideBoostPct,
                    min = 0f,
                    max = 120f,
                    formattedValue = "+${sideBoostPct.roundToInt()}%",
                    accentColor = StudioColors.Violet,
                    testTag = "stereo_side_slider",
                    onValueChange = {
                        preferVmUndo = false
                        sideBoostPct = it
                        stereoHistory.commit(Triple(midAdjustPct, it, widthPct), debounceMs = 350L)
                        viewModel.updateSurroundLiveParams(midAdjustPct, it, widthPct)
                        viewModel.ensureLivePreviewPlaying()
                    },
                )
                StudioParamSlider(
                    label = t(lang, "param_stereo_width"),
                    value = widthPct,
                    min = 0f,
                    max = 100f,
                    formattedValue = "${widthPct.roundToInt()}%",
                    accentColor = StudioColors.Emerald,
                    testTag = "stereo_width_slider",
                    onValueChange = {
                        preferVmUndo = false
                        widthPct = it
                        stereoHistory.commit(Triple(midAdjustPct, sideBoostPct, it), debounceMs = 350L)
                        viewModel.updateSurroundLiveParams(midAdjustPct, sideBoostPct, it)
                        viewModel.ensureLivePreviewPlaying()
                    },
                )

                val phaseReport = remember(midAdjustPct, sideBoostPct, widthPct, uiState.durationMs, uiState.hasAudio) {
                    viewModel.estimateSurroundPhaseReport(midAdjustPct, sideBoostPct, widthPct)
                }
                val phaseColor = if (phaseReport.isMonoCompatible) StudioColors.Emerald else StudioColors.Amber
                Surface(
                    color = phaseColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, phaseColor.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().testTag("stereo_phase_meter"),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = t(lang, if (phaseReport.isMonoCompatible) "stereo_mono_ok" else "stereo_mono_warn"),
                                style = MaterialTheme.typography.labelMedium,
                                color = phaseColor,
                            )
                            Text(
                                text = t(lang, "stereo_phase_hint"),
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioColors.TextMuted,
                            )
                        }
                        Text(
                            text = "r=${"%+.2f".format(phaseReport.correlation)}",
                            style = TelemetryTextStyle,
                            color = phaseColor,
                        )
                    }
                }

                val applyStereoInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        preferVmUndo = true
                        viewModel.applySurroundStereoFromOffsets(midAdjustPct, sideBoostPct, widthPct)
                    },
                    enabled = uiState.hasAudio && !uiState.isBusy && !uiState.isProcessing,
                    interactionSource = applyStereoInteraction,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioColors.Cyan, contentColor = StudioColors.BgTop),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 50.dp)
                        .lit(applyStereoInteraction, StudioColors.Cyan, cornerRadius = 14.dp)
                        .testTag("stereo_apply_button"),
                ) {
                    Text(t(lang, "apply_now"), style = MaterialTheme.typography.labelLarge)
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
fun GenerateSilenceScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onBack: () -> Unit,
) {
    val lang = uiState.language
    var durationSec by remember { mutableFloatStateOf(3.0f) }
    var insertMode by remember(uiState.hasAudio) {
        mutableStateOf(if (uiState.hasAudio) SilenceInsertMode.APPEND_END else SilenceInsertMode.NEW_TRACK)
    }
    var sampleRate by remember { mutableIntStateOf(TARGET_SAMPLE_RATE) }
    var channels by remember { mutableIntStateOf(2) }
    var preferVmUndo by remember { mutableStateOf(false) }
    val silenceHistory = rememberEditHistory(durationSec to insertMode)

    LaunchedEffect(uiState.restoredToolSession?.id) {
        val restored = viewModel.consumeRestoredToolSession("generate_silence")
        if (restored != null) {
            durationSec = (restored.floatParams["durationSec"] ?: durationSec).coerceIn(0.5f, 600f)
            val modeOrd = restored.floatParams["modeOrdinal"]?.roundToInt()
            if (modeOrd != null && modeOrd in SilenceInsertMode.entries.indices) {
                insertMode = SilenceInsertMode.entries[modeOrd]
            } else {
                val oldApp = (restored.floatParams["appendMode"] ?: 0f) > 0.5f
                insertMode = if (oldApp && uiState.hasAudio) SilenceInsertMode.APPEND_END else SilenceInsertMode.NEW_TRACK
            }
            sampleRate = (restored.floatParams["sampleRate"]?.roundToInt() ?: sampleRate).let {
                if (it in listOf(16000, 44100, 48000)) it else TARGET_SAMPLE_RATE
            }
            channels = (restored.floatParams["channels"]?.roundToInt() ?: channels).coerceIn(1, 2)
        }
    }

    val latestDur by rememberUpdatedState(durationSec)
    val latestMode by rememberUpdatedState(insertMode)
    val latestSr by rememberUpdatedState(sampleRate)
    val latestCh by rememberUpdatedState(channels)
    DisposableEffect(Unit) {
        onDispose {
            val formattedDur = if (latestDur >= 60f) {
                "${latestDur.toInt() / 60}m ${latestDur.toInt() % 60}s"
            } else {
                "${"%.1f".format(latestDur)}s"
            }
            viewModel.saveToolSessionToCache(
                toolId = "generate_silence",
                titleAr = "توليد صمت",
                titleEn = "Generate Silence",
                summaryAr = formattedDur,
                summaryEn = formattedDur,
                floatParams = mapOf(
                    "durationSec" to latestDur,
                    "modeOrdinal" to latestMode.ordinal.toFloat(),
                    "sampleRate" to latestSr.toFloat(),
                    "channels" to latestCh.toFloat(),
                ),
            )
        }
    }

    val formattedDuration = remember(durationSec) {
        if (durationSec >= 60f) {
            val total = durationSec.roundToInt()
            "${total / 60}m %02ds".format(total % 60)
        } else {
            "${"%.1f".format(durationSec)} s"
        }
    }

    StudioScrollColumn {
        StudioFocusTopBar(
            title = t(lang, "tool_generate_silence"),
            subtitle = if (lang == AppLanguage.EN) "Create clean silence (0.5s – 10m) or insert at start/end" else "إنشاء مسار صمت نقي (حتى 10 دقائق) أو إدراجه في البداية أو النهاية",
            language = lang,
            onBack = onBack,
            trailing = {
                UndoRedoActionsRow(
                    canUndo = silenceHistory.canUndo || uiState.canUndoEdit,
                    canRedo = silenceHistory.canRedo || uiState.canRedoEdit,
                    language = lang,
                    undoTag = "silence_undo_button",
                    redoTag = "silence_redo_button",
                    onUndo = {
                        if ((preferVmUndo && uiState.canUndoEdit) || !silenceHistory.canUndo) {
                            viewModel.undoLastEdit()
                        } else {
                            silenceHistory.undo()?.let { (d, m) ->
                                durationSec = d
                                insertMode = m
                            }
                        }
                    },
                    onRedo = {
                        if ((preferVmUndo && uiState.canRedoEdit) || !silenceHistory.canRedo) {
                            viewModel.redoLastEdit()
                        } else {
                            silenceHistory.redo()?.let { (d, m) ->
                                durationSec = d
                                insertMode = m
                            }
                        }
                    },
                )
            },
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StudioParamSlider(
                    label = if (lang == AppLanguage.EN) "Silence Duration (0.5s – 10m)" else "مدة الصمت (0.5 ثانية – 10 دقائق)",
                    value = durationSec,
                    min = 0.5f,
                    max = 600f,
                    formattedValue = formattedDuration,
                    accentColor = StudioColors.Cyan,
                    testTag = "silence_duration_slider",
                    onValueChange = {
                        preferVmUndo = false
                        durationSec = it
                        silenceHistory.commit(it to insertMode, debounceMs = 350L)
                    },
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val quickDurations = listOf(
                        1f to "1s",
                        3f to "3s",
                        5f to "5s",
                        15f to "15s",
                        30f to "30s",
                        60f to "1m",
                        180f to "3m",
                        300f to "5m",
                        600f to "10m",
                    )
                    quickDurations.forEach { (sec, label) ->
                        FilterChip(
                            selected = abs(durationSec - sec) < 0.4f,
                            onClick = {
                                preferVmUndo = false
                                durationSec = sec
                                silenceHistory.commit(sec to insertMode)
                            },
                            label = { Text(label) },
                            modifier = Modifier.testTag("silence_preset_${label}"),
                        )
                    }
                }

                if (uiState.hasAudio) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val modes = listOf(
                            SilenceInsertMode.APPEND_END to t(lang, "silence_mode_end"),
                            SilenceInsertMode.INSERT_START to t(lang, "silence_mode_start"),
                            SilenceInsertMode.NEW_TRACK to t(lang, "silence_mode_new"),
                        )
                        modes.forEach { (modeOption, label) ->
                            FilterChip(
                                selected = insertMode == modeOption,
                                onClick = {
                                    preferVmUndo = false
                                    insertMode = modeOption
                                    silenceHistory.commit(durationSec to modeOption)
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("silence_mode_${modeOption.name.lowercase()}"),
                            )
                        }
                    }
                }

                if (!uiState.hasAudio || insertMode == SilenceInsertMode.NEW_TRACK) {
                    Text(
                        text = "${t(lang, "silence_sample_rate")} • ${t(lang, "silence_channels")}",
                        style = MaterialTheme.typography.labelMedium,
                        color = StudioColors.TextMuted,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            16000 to "16 kHz",
                            44100 to "44.1 kHz",
                            48000 to "48 kHz",
                        ).forEach { (srOption, label) ->
                            FilterChip(
                                selected = sampleRate == srOption,
                                onClick = { sampleRate = srOption },
                                label = { Text(label) },
                                modifier = Modifier.testTag("silence_sr_$srOption"),
                            )
                        }
                        FilterChip(
                            selected = channels == 1,
                            onClick = { channels = 1 },
                            label = { Text("Mono (1ch)") },
                            modifier = Modifier.testTag("silence_ch_1"),
                        )
                        FilterChip(
                            selected = channels == 2,
                            onClick = { channels = 2 },
                            label = { Text("Stereo (2ch)") },
                            modifier = Modifier.testTag("silence_ch_2"),
                        )
                    }
                }

                val silenceInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        preferVmUndo = true
                        viewModel.insertGeneratedSilence(
                            durationSeconds = durationSec,
                            mode = insertMode,
                            sampleRate = sampleRate,
                            channels = channels,
                        )
                    },
                    enabled = !uiState.isBusy && !uiState.isProcessing,
                    interactionSource = silenceInteraction,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioColors.Cyan, contentColor = StudioColors.BgTop),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .lit(silenceInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                        .testTag("silence_generate_button"),
                ) {
                    Icon(Icons.Rounded.HourglassEmpty, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(t(lang, "apply_now"))
                }
            }
        }
    }
}

@Composable
fun FormatConverterScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onOpenPicker: (AudioPickerTarget) -> Unit,
    onBack: () -> Unit,
) {
    val lang = uiState.language
    var selectedFormat by remember { mutableStateOf(ExportAudioFormat.MP3) }
    var fileName by remember(uiState.activeProjectName) {
        mutableStateOf(uiState.activeProjectName.ifBlank { "Sonix_Converted" })
    }
    val convertHistory = rememberEditHistory(selectedFormat to fileName)

    StudioScrollColumn {
        StudioFocusTopBar(
            title = t(lang, "tool_format_convert"),
            subtitle = if (lang == AppLanguage.EN) "Convert between MP3, M4A, WAV, FLAC, and OGG" else "تحويل الصيغ بين MP3 و M4A و WAV و FLAC و OGG",
            language = lang,
            onBack = onBack,
            trailing = {
                UndoRedoActionsRow(
                    canUndo = convertHistory.canUndo || uiState.canUndoEdit,
                    canRedo = convertHistory.canRedo || uiState.canRedoEdit,
                    language = lang,
                    undoTag = "convert_undo_button",
                    redoTag = "convert_redo_button",
                    onUndo = {
                        if (convertHistory.canUndo) {
                            convertHistory.undo()?.let { (fmt, name) ->
                                selectedFormat = fmt
                                fileName = name
                            }
                        } else {
                            viewModel.undoLastEdit()
                        }
                    },
                    onRedo = {
                        if (convertHistory.canRedo) {
                            convertHistory.redo()?.let { (fmt, name) ->
                                selectedFormat = fmt
                                fileName = name
                            }
                        } else {
                            viewModel.redoLastEdit()
                        }
                    },
                )
            },
        )

        ActiveTrackPromptCard(uiState = uiState, onPickMainAudio = { onOpenPicker(AudioPickerTarget.IMPORT_MAIN) })

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
                    value = fileName,
                    onValueChange = {
                        fileName = it
                        convertHistory.commit(selectedFormat to it, debounceMs = 350L)
                    },
                    singleLine = true,
                    label = { Text(if (lang == AppLanguage.EN) "Output Filename" else "اسم الملف الناتج") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioColors.Cyan,
                        unfocusedBorderColor = StudioColors.Border,
                        focusedTextColor = StudioColors.TextPrimary,
                        unfocusedTextColor = StudioColors.TextPrimary,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ExportAudioFormat.entries.forEach { fmt ->
                        val isFmtSelected = selectedFormat == fmt
                        val fmtInteraction = remember { MutableInteractionSource() }
                        FilterChip(
                            selected = isFmtSelected,
                            onClick = {
                                selectedFormat = fmt
                                convertHistory.commit(fmt to fileName)
                            },
                            interactionSource = fmtInteraction,
                            label = { Text(fmt.displayName) },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .lit(fmtInteraction, MaterialTheme.colorScheme.primary, selected = isFmtSelected, cornerRadius = 10.dp),
                        )
                    }
                }

                uiState.exportProgress?.let { p ->
                    LinearProgressIndicator(
                        progress = { p.coerceIn(0f, 1f) },
                        color = StudioColors.Cyan,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val convertSaveInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = { viewModel.exportRenderedAudio(selectedFormat, fileName, shareAfter = false) },
                        enabled = uiState.hasAudio && !uiState.isBusy && !uiState.isProcessing,
                        interactionSource = convertSaveInteraction,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioColors.Cyan, contentColor = StudioColors.BgTop),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(convertSaveInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                            .testTag("convert_save_button"),
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(t(lang, "export_and_save"))
                    }

                    val convertShareInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = { viewModel.exportRenderedAudio(selectedFormat, fileName, shareAfter = true) },
                        enabled = uiState.hasAudio && !uiState.isBusy && !uiState.isProcessing,
                        interactionSource = convertShareInteraction,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(convertShareInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp),
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = t(lang, "share_audio"))
                    }
                }
            }
        }
    }
}

@Composable
fun ExtractVideoAudioScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onBack: () -> Unit,
) {
    val lang = uiState.language
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(ExportAudioFormat.WAV) }

    LaunchedEffect(uiState.pendingExtractVideoUri) {
        val stagedUri = viewModel.consumePendingExtractVideoUri()
        if (stagedUri != null) {
            viewModel.extractAudioFromVideoUri(stagedUri)
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.extractAudioFromVideoUri(uri)
        }
    }

    StudioScrollColumn {
        StudioFocusTopBar(
            title = t(lang, "tool_extract_video"),
            subtitle = if (lang == AppLanguage.EN) "Extract high-fidelity audio from any video" else "استخراج المسار الصوتي عالي الدقة من أي فيديو فوراً",
            language = lang,
            onBack = onBack,
            trailing = {
                UndoRedoActionsRow(
                    canUndo = uiState.canUndoEdit,
                    canRedo = uiState.canRedoEdit,
                    language = lang,
                    undoTag = "extract_video_undo_button",
                    redoTag = "extract_video_redo_button",
                    onUndo = { viewModel.undoLastEdit() },
                    onRedo = { viewModel.redoLastEdit() },
                )
            },
        )

        val pickVideoInteraction = remember { MutableInteractionSource() }
        Button(
            onClick = { videoPicker.launch(arrayOf("video/*")) },
            enabled = !uiState.isBusy && !uiState.isProcessing,
            interactionSource = pickVideoInteraction,
            colors = ButtonDefaults.buttonColors(containerColor = StudioColors.Cyan, contentColor = StudioColors.BgTop),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .lit(pickVideoInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 16.dp)
                .testTag("extract_pick_video_button"),
        ) {
            Icon(Icons.Rounded.VideoFile, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(t(lang, "audio_tools_pick_video"), style = MaterialTheme.typography.labelLarge)
        }

        if (uiState.hasAudio) {
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
                        text = "${uiState.activeProjectName} • ${formatDurationMs(uiState.durationMs)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = StudioColors.Emerald,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            ExportAudioFormat.WAV,
                            ExportAudioFormat.MP3,
                            ExportAudioFormat.FLAC,
                            ExportAudioFormat.M4A,
                        ).forEach { fmt ->
                            val isFmtSelected = selectedFormat == fmt
                            val fmtInteraction = remember { MutableInteractionSource() }
                            FilterChip(
                                selected = isFmtSelected,
                                onClick = { selectedFormat = fmt },
                                interactionSource = fmtInteraction,
                                label = { Text(fmt.displayName) },
                                modifier = Modifier.lit(fmtInteraction, MaterialTheme.colorScheme.primary, selected = isFmtSelected, cornerRadius = 10.dp),
                            )
                        }
                    }
                    val extractExportInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = { viewModel.exportRenderedAudio(selectedFormat, uiState.activeProjectName, shareAfter = false) },
                        enabled = !uiState.isBusy && !uiState.isProcessing,
                        interactionSource = extractExportInteraction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(extractExportInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 16.dp),
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(t(lang, "export_and_save"))
                    }
                }
            }
        }
    }
}

@Composable
fun CompressAudioScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onOpenPicker: (AudioPickerTarget) -> Unit,
    onBack: () -> Unit,
) {
    val lang = uiState.language
    var bitrateKbps by remember { mutableIntStateOf(96) }
    var fileName by remember(uiState.activeProjectName) {
        mutableStateOf("${uiState.activeProjectName.ifBlank { "Sonix" }}_Compressed")
    }
    val compressHistory = rememberEditHistory(bitrateKbps to fileName)
    val estKb = ((uiState.durationMs / 1000.0) * bitrateKbps / 8.0).roundToInt().coerceAtLeast(1)

    StudioScrollColumn {
        StudioFocusTopBar(
            title = t(lang, "tool_compress_audio"),
            subtitle = if (lang == AppLanguage.EN) "Reduce audio file size smoothly with AAC compression" else "ضغط وتقليل حجم الملف الصوتي مع الحفاظ على نقاء الصوت",
            language = lang,
            onBack = onBack,
            trailing = {
                UndoRedoActionsRow(
                    canUndo = compressHistory.canUndo || uiState.canUndoEdit,
                    canRedo = compressHistory.canRedo || uiState.canRedoEdit,
                    language = lang,
                    undoTag = "compress_undo_button",
                    redoTag = "compress_redo_button",
                    onUndo = {
                        if (compressHistory.canUndo) {
                            compressHistory.undo()?.let { (br, name) ->
                                bitrateKbps = br
                                fileName = name
                            }
                        } else {
                            viewModel.undoLastEdit()
                        }
                    },
                    onRedo = {
                        if (compressHistory.canRedo) {
                            compressHistory.redo()?.let { (br, name) ->
                                bitrateKbps = br
                                fileName = name
                            }
                        } else {
                            viewModel.redoLastEdit()
                        }
                    },
                )
            },
        )

        ActiveTrackPromptCard(uiState = uiState, onPickMainAudio = { onOpenPicker(AudioPickerTarget.IMPORT_MAIN) })

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
                Text(
                    text = if (lang == AppLanguage.EN) "Target Bitrate" else "معدل الضغط (Bitrate)",
                    style = MaterialTheme.typography.titleSmall,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(64, 96, 128, 192).forEach { br ->
                        val brSelected = bitrateKbps == br
                        val brInteraction = remember { MutableInteractionSource() }
                        FilterChip(
                            selected = brSelected,
                            onClick = {
                                bitrateKbps = br
                                compressHistory.commit(br to fileName)
                            },
                            interactionSource = brInteraction,
                            label = { Text("${br}k") },
                            modifier = Modifier
                                .weight(1f)
                                .lit(brInteraction, MaterialTheme.colorScheme.primary, selected = brSelected, cornerRadius = 10.dp),
                        )
                    }
                }

                if (uiState.hasAudio) {
                    Text(
                        text = if (lang == AppLanguage.EN) "Estimated Size: ~${estKb} KB" else "الحجم التقديري بعد الضغط: ~${estKb} KB",
                        style = TelemetryTextStyle,
                        color = StudioColors.Emerald,
                    )
                }

                OutlinedTextField(
                    value = fileName,
                    onValueChange = {
                        fileName = it
                        compressHistory.commit(bitrateKbps to it, debounceMs = 350L)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioColors.Cyan,
                        unfocusedBorderColor = StudioColors.Border,
                        focusedTextColor = StudioColors.TextPrimary,
                        unfocusedTextColor = StudioColors.TextPrimary,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                val compressInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = { viewModel.compressAndExportAudio(bitrateKbps, fileName, shareAfter = false) },
                    enabled = uiState.hasAudio && !uiState.isBusy && !uiState.isProcessing,
                    interactionSource = compressInteraction,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioColors.Cyan, contentColor = StudioColors.BgTop),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .lit(compressInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                        .testTag("compress_export_button"),
                ) {
                    Icon(Icons.Rounded.Compress, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(t(lang, "export_and_save"))
                }
            }
        }
    }
}

@Composable
fun RingtoneMakerScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onOpenPicker: (AudioPickerTarget) -> Unit,
    onBack: () -> Unit,
) {
    val lang = uiState.language
    val context = LocalContext.current
    var startFrac by remember(uiState.trimStart) { mutableFloatStateOf(uiState.trimStart) }
    var endFrac by remember(uiState.trimEnd) { mutableFloatStateOf(uiState.trimEnd) }
    var ringtoneTitle by remember(uiState.activeProjectName) {
        mutableStateOf("Sonix_${uiState.activeProjectName.ifBlank { "Ringtone" }}")
    }
    var setAsDefault by remember { mutableStateOf(true) }
    val ringtoneHistory = rememberEditHistory(
        listOf<Any>(startFrac, endFrac, ringtoneTitle, setAsDefault),
    )

    fun commitRingtone(
        s: Float = startFrac,
        e: Float = endFrac,
        title: String = ringtoneTitle,
        def: Boolean = setAsDefault,
        debounceMs: Long = 0L,
    ) {
        ringtoneHistory.commit(listOf<Any>(s, e, title, def), debounceMs = debounceMs)
    }

    fun applyRingtone(snap: List<Any>) {
        startFrac = snap[0] as Float
        endFrac = snap[1] as Float
        ringtoneTitle = snap[2] as String
        setAsDefault = snap[3] as Boolean
    }

    val clipLenMs = ((endFrac - startFrac).coerceAtLeast(0.02f) * uiState.durationMs).roundToInt().toLong()

    StudioScrollColumn {
        StudioFocusTopBar(
            title = t(lang, "tool_ringtone_maker"),
            subtitle = if (lang == AppLanguage.EN) "Cut custom ringtones and assign to device" else "قص وتخصيص نغمات الرنين وحفظها وتعيينها للهاتف",
            language = lang,
            onBack = onBack,
            trailing = {
                UndoRedoActionsRow(
                    canUndo = ringtoneHistory.canUndo || uiState.canUndoEdit,
                    canRedo = ringtoneHistory.canRedo || uiState.canRedoEdit,
                    language = lang,
                    undoTag = "ringtone_undo_button",
                    redoTag = "ringtone_redo_button",
                    onUndo = {
                        if (ringtoneHistory.canUndo) {
                            ringtoneHistory.undo()?.let(::applyRingtone)
                        } else {
                            viewModel.undoLastEdit()
                        }
                    },
                    onRedo = {
                        if (ringtoneHistory.canRedo) {
                            ringtoneHistory.redo()?.let(::applyRingtone)
                        } else {
                            viewModel.redoLastEdit()
                        }
                    },
                )
            },
        )

        ActiveTrackPromptCard(uiState = uiState, onPickMainAudio = { onOpenPicker(AudioPickerTarget.IMPORT_MAIN) })

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
                    value = ringtoneTitle,
                    onValueChange = {
                        ringtoneTitle = it
                        commitRingtone(title = it, debounceMs = 350L)
                    },
                    singleLine = true,
                    label = { Text(if (lang == AppLanguage.EN) "Ringtone Title" else "اسم نغمة الرنين") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioColors.Cyan,
                        unfocusedBorderColor = StudioColors.Border,
                        focusedTextColor = StudioColors.TextPrimary,
                        unfocusedTextColor = StudioColors.TextPrimary,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                StudioParamSlider(
                    label = if (lang == AppLanguage.EN) "Start Point" else "بداية النغمة",
                    value = startFrac,
                    min = 0f,
                    max = (endFrac - 0.05f).coerceAtLeast(0.01f),
                    formattedValue = formatDurationMs((uiState.durationMs * startFrac).roundToInt().toLong()),
                    accentColor = StudioColors.Cyan,
                    testTag = "ringtone_start_slider",
                    onValueChange = {
                        startFrac = it
                        commitRingtone(s = it, debounceMs = 350L)
                    },
                )

                StudioParamSlider(
                    label = if (lang == AppLanguage.EN) "End Point" else "نهاية النغمة",
                    value = endFrac,
                    min = (startFrac + 0.05f).coerceAtMost(0.99f),
                    max = 1f,
                    formattedValue = formatDurationMs((uiState.durationMs * endFrac).roundToInt().toLong()),
                    accentColor = StudioColors.Emerald,
                    testTag = "ringtone_end_slider",
                    onValueChange = {
                        endFrac = it
                        commitRingtone(e = it, debounceMs = 350L)
                    },
                )

                Text(
                    text = if (lang == AppLanguage.EN) "Ringtone length: ${formatDurationMs(clipLenMs)}" else "مدة النغمة المحددة: ${formatDurationMs(clipLenMs)}",
                    style = TelemetryTextStyle,
                    color = StudioColors.Cyan,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (lang == AppLanguage.EN) "Set as default phone ringtone" else "تعيين كنغمة رنين افتراضية للجهاز",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioColors.TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = setAsDefault,
                        onCheckedChange = {
                            setAsDefault = it
                            commitRingtone(def = it)
                        },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    )
                }

                val ringtoneSaveInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        viewModel.createRingtoneFromSelection(
                            startFrac = startFrac,
                            endFrac = endFrac,
                            title = ringtoneTitle,
                            setAsDefault = setAsDefault,
                            onNeedsWriteSettings = {
                                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                runCatching { context.startActivity(intent) }
                            },
                        )
                    },
                    enabled = uiState.hasAudio && !uiState.isBusy && !uiState.isProcessing,
                    interactionSource = ringtoneSaveInteraction,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioColors.Emerald, contentColor = StudioColors.BgTop),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .lit(ringtoneSaveInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                        .testTag("ringtone_save_button"),
                ) {
                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (lang == AppLanguage.EN) "Save & Set Ringtone" else "حفظ وتعيين النغمة")
                }
            }
        }
    }
}
