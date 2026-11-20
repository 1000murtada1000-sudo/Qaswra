package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioSignalAnalyzer
import com.example.audio.EqualizerProcessor
import com.example.i18n.t
import com.example.model.AppLanguage
import com.example.model.DistortionMode
import com.example.model.ECHO_SPACE_PRESETS
import com.example.model.EQ_10_FREQUENCIES_HZ
import com.example.model.EQ_10_LABELS
import com.example.model.EQ_PRESET_PROFILES
import com.example.model.EchoDetailGroup
import com.example.model.EchoPadAxisMode
import com.example.model.EffectType
import com.example.studio.ui.lit
import kotlin.math.ln
import com.example.ui.components.SaveDraftDialog
import com.example.ui.components.SpectrumVisualizer
import com.example.ui.components.StudioFocusTopBar
import com.example.ui.components.StudioParamSlider
import com.example.ui.components.StudioPreviewControlsBar
import com.example.ui.components.StudioScrollColumn
import com.example.ui.components.StudioSectionCard
import com.example.ui.components.UndoRedoActionsRow
import com.example.ui.theme.StudioColors
import com.example.ui.theme.StudioDropdownMenuTheme
import com.example.ui.theme.TelemetryTextStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PlaybackTelemetry
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EchoReverbScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onOpenDetail: (EchoDetailGroup) -> Unit,
    onBack: () -> Unit,
) {
    val lang = uiState.language
    val echo = uiState.dspConfig.echoReverb
    val enabled = uiState.dspConfig.isEnabled(EffectType.ECHO_REVERB)
    val (padX, padY) = viewModel.getEchoPadAxes()
    var saveDraftDialogOpen by remember { mutableStateOf(false) }
    var echoPresetExpanded by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val rippleStroke = remember(density) { Stroke(width = with(density) { 1.5.dp.toPx() }) }
    val trailPoints = remember { mutableStateListOf<Offset>() }

    LaunchedEffect(Unit) {
        viewModel.beginLiveToolSession("echo_reverb")
    }

    BackHandler {
        viewModel.discardLiveToolSession("echo_reverb")
        onBack()
    }

    LaunchedEffect(uiState.restoredToolSession?.id) {
        val restored = viewModel.consumeRestoredToolSession("echo_reverb")
        if (restored != null) {
            val fp = restored.floatParams
            viewModel.updateEchoReverbParams { prev ->
                prev.copy(
                    wetDryMix = fp["wetDryMix"] ?: prev.wetDryMix,
                    delayTimeMs = fp["delayTimeMs"] ?: prev.delayTimeMs,
                    echoStrength = fp["echoStrength"] ?: prev.echoStrength,
                    reverbSize = fp["reverbSize"] ?: prev.reverbSize,
                )
            }
        }
    }

    val latestEcho by rememberUpdatedState(echo)
    val latestHasAudio by rememberUpdatedState(uiState.hasAudio)
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopToolPreview()
            val committed = viewModel.isLiveToolSessionCommitted("echo_reverb")
            viewModel.discardLiveToolSession("echo_reverb")
            if (committed && latestHasAudio) {
                val p = latestEcho
                viewModel.saveToolSessionToCache(
                    toolId = "echo_reverb",
                    titleAr = "استوديو الصدى والفضاء",
                    titleEn = "Echo & Reverb",
                    summaryAr = "${(p.wetDryMix * 100f).roundToInt()}% / ${p.delayTimeMs.roundToInt()} ms",
                    summaryEn = "${(p.wetDryMix * 100f).roundToInt()}% / ${p.delayTimeMs.roundToInt()} ms",
                    floatParams = mapOf(
                        "wetDryMix" to p.wetDryMix,
                        "delayTimeMs" to p.delayTimeMs,
                        "echoStrength" to p.echoStrength,
                        "reverbSize" to p.reverbSize,
                    ),
                )
            }
        }
    }

    StudioScrollColumn {
        StudioFocusTopBar(
            title = t(lang, "section_echo_title"),
            subtitle = t(lang, "section_echo_subtitle"),
            language = lang,
            onBack = {
                viewModel.discardLiveToolSession("echo_reverb")
                onBack()
            },
            trailing = {
                UndoRedoActionsRow(
                    canUndo = uiState.canUndoEdit,
                    canRedo = uiState.canRedoEdit,
                    language = lang,
                    undoTag = "echo_top_undo_button",
                    redoTag = "echo_top_redo_button",
                    onUndo = { viewModel.undoLastEdit() },
                    onRedo = { viewModel.redoLastEdit() },
                    extraTrailing = {
                        Switch(
                            checked = enabled,
                            onCheckedChange = { viewModel.toggleEffectEnabled(EffectType.ECHO_REVERB, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = StudioColors.BgTop,
                                checkedTrackColor = StudioColors.Cyan,
                            ),
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("echo_enable_switch"),
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

        // Simple / Pro Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val simpleInteraction = remember { MutableInteractionSource() }
            Surface(
                onClick = { viewModel.setSimpleEchoMode(true) },
                interactionSource = simpleInteraction,
                shape = RoundedCornerShape(14.dp),
                color = if (uiState.isSimpleEchoMode) StudioColors.Cyan else StudioColors.CardSurface,
                border = BorderStroke(1.dp, if (uiState.isSimpleEchoMode) StudioColors.Cyan else StudioColors.Border),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .lit(simpleInteraction, MaterialTheme.colorScheme.primary, selected = uiState.isSimpleEchoMode, cornerRadius = 14.dp)
                    .testTag("echo_mode_simple"),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = t(lang, "mode_simple"),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (uiState.isSimpleEchoMode) StudioColors.BgTop else StudioColors.TextSecondary,
                    )
                }
            }
            val proInteraction = remember { MutableInteractionSource() }
            Surface(
                onClick = { viewModel.setSimpleEchoMode(false) },
                interactionSource = proInteraction,
                shape = RoundedCornerShape(14.dp),
                color = if (!uiState.isSimpleEchoMode) StudioColors.Violet else StudioColors.CardSurface,
                border = BorderStroke(1.dp, if (!uiState.isSimpleEchoMode) StudioColors.Violet else StudioColors.Border),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .lit(proInteraction, MaterialTheme.colorScheme.primary, selected = !uiState.isSimpleEchoMode, cornerRadius = 14.dp)
                    .testTag("echo_mode_pro"),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = t(lang, "mode_pro"),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (!uiState.isSimpleEchoMode) StudioColors.BgTop else StudioColors.TextSecondary,
                    )
                }
            }
        }

        // Ready-made echo presets stay compact; the custom Pro Echo studio remains foreground.
        ExposedDropdownMenuBox(
            expanded = echoPresetExpanded,
            onExpandedChange = { echoPresetExpanded = it },
            modifier = Modifier.fillMaxWidth(),
        ) {
            val presetLabel = ECHO_SPACE_PRESETS.firstOrNull { it.titleKey == uiState.selectedPresetLabel }
                ?.let { t(lang, it.titleKey) }
                ?: t(lang, "echo_spaces_title")
            OutlinedTextField(
                value = presetLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text(t(lang, "echo_spaces_title")) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = echoPresetExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            StudioDropdownMenuTheme {
                ExposedDropdownMenu(
                    expanded = echoPresetExpanded,
                    onDismissRequest = { echoPresetExpanded = false },
                    modifier = Modifier
                        .heightIn(max = 220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioColors.CardSurface)
                        .border(1.dp, StudioColors.Border, RoundedCornerShape(12.dp)),
                ) {
                    ECHO_SPACE_PRESETS.forEach { space ->
                        DropdownMenuItem(
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(t(lang, space.titleKey))
                                    Text(t(lang, space.descKey), style = MaterialTheme.typography.bodySmall, color = StudioColors.TextSecondary)
                                }
                            },
                            onClick = {
                                echoPresetExpanded = false
                                viewModel.applyEchoSpacePreset(space)
                            },
                            modifier = Modifier.testTag("echo_space_${space.id}"),
                        )
                    }
                }
            }
        }

        // 4 Axis Mode Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EchoPadAxisMode.entries.forEach { mode ->
                val selected = mode == uiState.echoPadAxisMode
                val label = when (mode) {
                    EchoPadAxisMode.SPACE -> if (lang == AppLanguage.EN) "Space & Depth" else "العمق والمساحة"
                    EchoPadAxisMode.DELAY -> if (lang == AppLanguage.EN) "Delay & Feedback" else "الزمن والتكرار"
                    EchoPadAxisMode.WIDTH -> if (lang == AppLanguage.EN) "Width & Diffusion" else "الاتساع والانتشار"
                    EchoPadAxisMode.DAMPING -> if (lang == AppLanguage.EN) "Damping & Pre" else "التخميد والتمهيد"
                }
                val chipInteraction = remember { MutableInteractionSource() }
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setEchoPadAxisMode(mode) },
                    interactionSource = chipInteraction,
                    label = { Text(label, maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StudioColors.Cyan.copy(alpha = 0.2f),
                        selectedLabelColor = StudioColors.Cyan,
                        containerColor = StudioColors.CardSurface,
                        labelColor = StudioColors.TextSecondary,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = StudioColors.Border,
                        selectedBorderColor = StudioColors.Cyan,
                    ),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .lit(chipInteraction, MaterialTheme.colorScheme.primary, selected = selected, cornerRadius = 10.dp)
                        .testTag("echo_axis_${mode.name.lowercase()}"),
                )
            }
        }

        // 2D Interactive Spatial Pad
        Card(
            shape = RoundedCornerShape(24.dp),
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
                    text = t(lang, "echo_pad_hint"),
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioColors.TextSecondary,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(StudioColors.BgTop)
                        .border(1.dp, StudioColors.BorderBright.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .pointerInput(uiState.echoPadAxisMode) {
                            detectTapGestures { offset ->
                                val nx = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                val ny = (1f - offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                                trailPoints.add(Offset(nx, ny))
                                if (trailPoints.size > 16) trailPoints.removeAt(0)
                                viewModel.updateEchoReverbFromPad(nx, ny)
                                viewModel.ensureLivePreviewPlaying()
                            }
                        }
                        .pointerInput(uiState.echoPadAxisMode) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val nx = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                val ny = (1f - change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                                trailPoints.add(Offset(nx, ny))
                                if (trailPoints.size > 16) trailPoints.removeAt(0)
                                viewModel.updateEchoReverbFromPad(nx, ny)
                                viewModel.ensureLivePreviewPlaying()
                            }
                        }
                        .testTag("echo_spatial_pad"),
                ) {
                    // Static pad glow, grid, trail & glowing orb (does not recompose at 60fps)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val orbPos = Offset(padX * size.width, (1f - padY) * size.height)

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    StudioColors.Cyan.copy(alpha = 0.28f),
                                    StudioColors.Violet.copy(alpha = 0.1f),
                                    Color.Transparent,
                                ),
                                center = orbPos,
                                radius = size.minDimension * 0.55f,
                            ),
                            center = orbPos,
                            radius = size.minDimension * 0.55f,
                        )

                        for (i in 1..3) {
                            val gx = size.width * (i / 4f)
                            val gy = size.height * (i / 4f)
                            drawLine(
                                color = StudioColors.Border.copy(alpha = 0.5f),
                                start = Offset(gx, 0f),
                                end = Offset(gx, size.height),
                                strokeWidth = 1.dp.toPx(),
                            )
                            drawLine(
                                color = StudioColors.Border.copy(alpha = 0.5f),
                                start = Offset(0f, gy),
                                end = Offset(size.width, gy),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }

                        if (trailPoints.size > 1) {
                            for (i in 1 until trailPoints.size) {
                                val p0 = Offset(trailPoints[i - 1].x * size.width, (1f - trailPoints[i - 1].y) * size.height)
                                val p1 = Offset(trailPoints[i].x * size.width, (1f - trailPoints[i].y) * size.height)
                                val frac = i.toFloat() / trailPoints.size
                                drawLine(
                                    color = StudioColors.Cyan.copy(alpha = frac * 0.6f),
                                    start = p0,
                                    end = p1,
                                    strokeWidth = 2.5.dp.toPx(),
                                    cap = StrokeCap.Round,
                                )
                            }
                        }

                        drawCircle(
                            color = StudioColors.Cyan.copy(alpha = 0.35f),
                            radius = 18.dp.toPx(),
                            center = orbPos,
                        )
                        drawCircle(
                            color = StudioColors.Cyan,
                            radius = 9.dp.toPx(),
                            center = orbPos,
                        )
                        drawCircle(
                            color = StudioColors.TextPrimary,
                            radius = 3.5.dp.toPx(),
                            center = orbPos,
                        )
                    }

                    // Isolated 60fps radar ripple rings & orbiting satellites
                    EchoSpatialRippleOverlay(
                        padX = padX,
                        padY = padY,
                        stereoWidth = echo.stereoWidth,
                        rippleStroke = rippleStroke,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // 3 Telemetry Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TelemetryBadge(
                        label = t(lang, "param_echo_strength"),
                        value = "${(echo.echoStrength * 100f).roundToInt()}%",
                        color = StudioColors.Cyan,
                        modifier = Modifier.weight(1f),
                    )
                    TelemetryBadge(
                        label = t(lang, "param_delay_time"),
                        value = "${echo.delayTimeMs.roundToInt()} ms",
                        color = StudioColors.Violet,
                        modifier = Modifier.weight(1f),
                    )
                    TelemetryBadge(
                        label = t(lang, "param_wet_dry"),
                        value = "${(echo.wetDryMix * 100f).roundToInt()}%",
                        color = StudioColors.Emerald,
                        modifier = Modifier.weight(1f),
                    )
                }

                StudioParamSlider(
                    label = t(lang, "param_wet_dry"),
                    value = echo.wetDryMix,
                    min = 0f,
                    max = 1f,
                    formattedValue = "${(echo.wetDryMix * 100f).roundToInt()}%",
                    accentColor = StudioColors.Cyan,
                    testTag = "echo_wet_dry_slider",
                    onValueChange = { v ->
                        viewModel.updateEchoReverbParams { it.copy(wetDryMix = v) }
                        viewModel.ensureLivePreviewPlaying()
                    },
                )

                StudioParamSlider(
                    label = t(lang, "param_echo_strength"),
                    value = echo.echoStrength,
                    min = 0f,
                    max = 1f,
                    formattedValue = "${(echo.echoStrength * 100f).roundToInt()}%",
                    accentColor = StudioColors.Emerald,
                    testTag = "echo_strength_slider",
                    onValueChange = { v ->
                        viewModel.updateEchoReverbParams { it.copy(echoStrength = v) }
                        viewModel.ensureLivePreviewPlaying()
                    },
                )

                StudioParamSlider(
                    label = t(lang, "param_delay_time"),
                    value = echo.delayTimeMs,
                    min = 40f,
                    max = 1000f,
                    formattedValue = "${echo.delayTimeMs.roundToInt()} ms",
                    accentColor = StudioColors.Violet,
                    testTag = "echo_delay_time_slider",
                    onValueChange = { v ->
                        viewModel.updateEchoReverbParams { it.copy(delayTimeMs = v) }
                        viewModel.ensureLivePreviewPlaying()
                    },
                )

                StudioParamSlider(
                    label = t(lang, "param_feedback"),
                    value = echo.feedback,
                    min = 0f,
                    max = 0.86f,
                    formattedValue = "${(echo.feedback * 100f).roundToInt()}%",
                    accentColor = StudioColors.Amber,
                    testTag = "echo_feedback_slider",
                    onValueChange = { v ->
                        viewModel.updateEchoReverbParams { it.copy(feedback = v) }
                        viewModel.ensureLivePreviewPlaying()
                    },
                )
            }
        }

        if (!uiState.isSimpleEchoMode) {
            StudioSectionCard(
                title = t(lang, "echo_node_reflections"),
                subtitle = "${t(lang, "param_early_reflections")}, ${t(lang, "param_late_reflections")}, ${t(lang, "param_diffusion")}",
                icon = Icons.Rounded.BlurOn,
                accentColor = StudioColors.Violet,
                testTag = "echo_group_reflections",
                onClick = { onOpenDetail(EchoDetailGroup.REFLECTIONS) },
            )
            StudioSectionCard(
                title = t(lang, "echo_node_space"),
                subtitle = "${t(lang, "param_reverb_size")}, ${t(lang, "param_reverb_decay")}, ${t(lang, "param_stereo_width")}",
                icon = Icons.Rounded.Waves,
                accentColor = StudioColors.Emerald,
                testTag = "echo_group_space",
                onClick = { onOpenDetail(EchoDetailGroup.SPACE) },
            )
        }

        val applyEchoInteraction = remember { MutableInteractionSource() }
        Button(
            onClick = { viewModel.commitLiveToolSession("echo_reverb", t(lang, "section_echo_title")) },
            enabled = uiState.hasAudio && !uiState.isBusy,
            interactionSource = applyEchoInteraction,
            colors = ButtonDefaults.buttonColors(containerColor = StudioColors.Cyan, contentColor = StudioColors.BgTop),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 50.dp)
                .lit(applyEchoInteraction, StudioColors.Cyan, cornerRadius = 14.dp)
                .testTag("echo_apply_button"),
        ) {
            Text(t(lang, "apply_now"), style = MaterialTheme.typography.labelLarge)
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
private fun EchoSpatialRippleOverlay(
    padX: Float,
    padY: Float,
    stereoWidth: Float,
    rippleStroke: Stroke,
    modifier: Modifier = Modifier,
) {
    val rippleTransition = rememberInfiniteTransition(label = "echoRipple")
    val ripplePhase by rippleTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ripplePhase",
    )

    Canvas(modifier = modifier) {
        val orbPos = Offset(padX * size.width, (1f - padY) * size.height)

        for (ringIdx in 0..2) {
            val phase = (ripplePhase + ringIdx / 3f) % 1f
            val radius = 18.dp.toPx() + phase * size.minDimension * 0.42f
            val alpha = (1f - phase) * 0.55f
            drawCircle(
                color = StudioColors.Cyan.copy(alpha = alpha),
                radius = radius,
                center = orbPos,
                style = rippleStroke,
            )
        }

        val orbitRadius = (18f + stereoWidth * 36f).dp.toPx()
        for (sat in 0..3) {
            val angle = ripplePhase * 2f * PI.toFloat() + sat * (PI.toFloat() / 2f)
            val satPos = Offset(
                x = orbPos.x + cos(angle) * orbitRadius,
                y = orbPos.y + sin(angle) * orbitRadius * 0.65f,
            )
            drawCircle(
                color = StudioColors.Violet.copy(alpha = 0.85f),
                radius = 4.dp.toPx(),
                center = satPos,
            )
        }
    }
}

@Composable
private fun TelemetryBadge(
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = StudioColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = TelemetryTextStyle,
                color = color,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun EchoDetailScreen(
    group: EchoDetailGroup,
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onBack: () -> Unit,
) {
    val lang = uiState.language
    val echo = uiState.dspConfig.echoReverb
    val title = when (group) {
        EchoDetailGroup.CORE -> t(lang, "echo_node_core")
        EchoDetailGroup.REFLECTIONS -> t(lang, "echo_node_reflections")
        EchoDetailGroup.SPACE -> t(lang, "echo_node_space")
    }

    StudioScrollColumn {
        StudioFocusTopBar(
            title = title,
            language = lang,
            onBack = onBack,
            trailing = {
                UndoRedoActionsRow(
                    canUndo = uiState.canUndoEdit,
                    canRedo = uiState.canRedoEdit,
                    language = lang,
                    undoTag = "echo_detail_undo_button",
                    redoTag = "echo_detail_redo_button",
                    onUndo = { viewModel.undoLastEdit() },
                    onRedo = { viewModel.redoLastEdit() },
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
                when (group) {
                    EchoDetailGroup.CORE -> {
                        StudioParamSlider(
                            label = t(lang, "param_echo_strength"),
                            value = echo.echoStrength,
                            min = 0f,
                            max = 1f,
                            formattedValue = "${(echo.echoStrength * 100f).roundToInt()}%",
                            accentColor = StudioColors.Cyan,
                            testTag = "echo_detail_strength",
                            onValueChange = { v ->
                                viewModel.updateEchoReverbParams { it.copy(echoStrength = v) }
                                viewModel.ensureLivePreviewPlaying()
                            },
                        )
                        StudioParamSlider(
                            label = t(lang, "param_echo_depth"),
                            value = echo.echoDepth,
                            min = 0f,
                            max = 1f,
                            formattedValue = "${(echo.echoDepth * 100f).roundToInt()}%",
                            accentColor = StudioColors.Cyan,
                            testTag = "echo_detail_depth",
                            onValueChange = { v ->
                                viewModel.updateEchoReverbParams { it.copy(echoDepth = v) }
                                viewModel.ensureLivePreviewPlaying()
                            },
                        )
                        StudioParamSlider(
                            label = t(lang, "param_delay_time"),
                            value = echo.delayTimeMs,
                            min = 40f,
                            max = 1000f,
                            formattedValue = "${echo.delayTimeMs.roundToInt()} ms",
                            accentColor = StudioColors.Violet,
                            testTag = "echo_detail_delay",
                            onValueChange = { v ->
                                viewModel.updateEchoReverbParams { it.copy(delayTimeMs = v) }
                                viewModel.ensureLivePreviewPlaying()
                            },
                        )
                        StudioParamSlider(
                            label = t(lang, "param_feedback"),
                            value = echo.feedback,
                            min = 0f,
                            max = 0.88f,
                            formattedValue = "${(echo.feedback * 100f).roundToInt()}%",
                            accentColor = StudioColors.Amber,
                            testTag = "echo_detail_feedback",
                            onValueChange = { v ->
                                viewModel.updateEchoReverbParams { it.copy(feedback = v) }
                                viewModel.ensureLivePreviewPlaying()
                            },
                        )
                    }
                    EchoDetailGroup.REFLECTIONS -> {
                        StudioParamSlider(
                            label = t(lang, "param_early_reflections"),
                            value = echo.earlyReflections,
                            min = 0f,
                            max = 1f,
                            formattedValue = "${(echo.earlyReflections * 100f).roundToInt()}%",
                            accentColor = StudioColors.Violet,
                            testTag = "echo_detail_early",
                            onValueChange = { v ->
                                viewModel.updateEchoReverbParams { it.copy(earlyReflections = v) }
                                viewModel.ensureLivePreviewPlaying()
                            },
                        )
                        StudioParamSlider(
                            label = t(lang, "param_late_reflections"),
                            value = echo.lateReflections,
                            min = 0f,
                            max = 1f,
                            formattedValue = "${(echo.lateReflections * 100f).roundToInt()}%",
                            accentColor = StudioColors.Violet,
                            testTag = "echo_detail_late",
                            onValueChange = { v ->
                                viewModel.updateEchoReverbParams { it.copy(lateReflections = v) }
                                viewModel.ensureLivePreviewPlaying()
                            },
                        )
                        StudioParamSlider(
                            label = t(lang, "param_pre_delay"),
                            value = echo.preDelayMs,
                            min = 0f,
                            max = 150f,
                            formattedValue = "${echo.preDelayMs.roundToInt()} ms",
                            accentColor = StudioColors.Cyan,
                            testTag = "echo_detail_predelay",
                            onValueChange = { v ->
                                viewModel.updateEchoReverbParams { it.copy(preDelayMs = v) }
                                viewModel.ensureLivePreviewPlaying()
                            },
                        )
                        StudioParamSlider(
                            label = t(lang, "param_diffusion"),
                            value = echo.diffusion,
                            min = 0f,
                            max = 1f,
                            formattedValue = "${(echo.diffusion * 100f).roundToInt()}%",
                            accentColor = StudioColors.Emerald,
                            testTag = "echo_detail_diffusion",
                            onValueChange = { v ->
                                viewModel.updateEchoReverbParams { it.copy(diffusion = v) }
                                viewModel.ensureLivePreviewPlaying()
                            },
                        )
                    }
                    EchoDetailGroup.SPACE -> {
                        StudioParamSlider(
                            label = t(lang, "param_reverb_size"),
                            value = echo.reverbSize,
                            min = 0.1f,
                            max = 1f,
                            formattedValue = "${(echo.reverbSize * 100f).roundToInt()}%",
                            accentColor = StudioColors.Emerald,
                            testTag = "echo_detail_size",
                            onValueChange = { v ->
                                viewModel.updateEchoReverbParams { it.copy(reverbSize = v) }
                                viewModel.ensureLivePreviewPlaying()
                            },
                        )
                        StudioParamSlider(
                            label = t(lang, "param_reverb_decay"),
                            value = echo.reverbDecay,
                            min = 0.1f,
                            max = 0.96f,
                            formattedValue = "${(echo.reverbDecay * 100f).roundToInt()}%",
                            accentColor = StudioColors.Emerald,
                            testTag = "echo_detail_decay",
                            onValueChange = { v ->
                                viewModel.updateEchoReverbParams { it.copy(reverbDecay = v) }
                                viewModel.ensureLivePreviewPlaying()
                            },
                        )
                        StudioParamSlider(
                            label = t(lang, "param_stereo_width"),
                            value = echo.stereoWidth,
                            min = 0f,
                            max = 1f,
                            formattedValue = "${(echo.stereoWidth * 100f).roundToInt()}%",
                            accentColor = StudioColors.Cyan,
                            testTag = "echo_detail_width",
                            onValueChange = { v ->
                                viewModel.updateEchoReverbParams { it.copy(stereoWidth = v) }
                                viewModel.ensureLivePreviewPlaying()
                            },
                        )
                        StudioParamSlider(
                            label = t(lang, "param_damping"),
                            value = echo.damping,
                            min = 0f,
                            max = 1f,
                            formattedValue = "${(echo.damping * 100f).roundToInt()}%",
                            accentColor = StudioColors.Amber,
                            testTag = "echo_detail_damping",
                            onValueChange = { v ->
                                viewModel.updateEchoReverbParams { it.copy(damping = v) }
                                viewModel.ensureLivePreviewPlaying()
                            },
                        )
                    }
                }

                val applyDetailInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = { viewModel.commitLiveToolSession("echo_reverb", title) },
                    enabled = uiState.hasAudio && !uiState.isBusy,
                    interactionSource = applyDetailInteraction,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioColors.Cyan, contentColor = StudioColors.BgTop),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 50.dp)
                        .lit(applyDetailInteraction, StudioColors.Cyan, cornerRadius = 14.dp)
                        .testTag("echo_detail_apply_button"),
                ) {
                    Text(t(lang, "apply_now"), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun EffectsChainScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onOpenEffect: (EffectType) -> Unit,
    onBack: () -> Unit,
) {
    val lang = uiState.language
    val chain = uiState.dspConfig.chain
    val itemCenters = remember { mutableMapOf<EffectType, Float>() }
    val latestChain by rememberUpdatedState(chain)

    StudioScrollColumn {
        StudioFocusTopBar(
            title = t(lang, "section_chain_title"),
            subtitle = t(lang, "chain_order_hint"),
            language = lang,
            onBack = onBack,
            trailing = {
                UndoRedoActionsRow(
                    canUndo = uiState.canUndoEdit,
                    canRedo = uiState.canRedoEdit,
                    language = lang,
                    undoTag = "chain_undo_button",
                    redoTag = "chain_redo_button",
                    onUndo = { viewModel.undoLastEdit() },
                    onRedo = { viewModel.redoLastEdit() },
                )
            },
        )

        EffectsChainSpectrumSection(
            playbackFlow = viewModel.playbackTelemetry,
            language = lang,
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StudioParamSlider(
                    label = if (lang == AppLanguage.EN) "Master Output Gain" else "كسب الخرج الرئيسي (Master Gain)",
                    value = uiState.dspConfig.masterGainDb,
                    min = -18f,
                    max = 12f,
                    formattedValue = "${if (uiState.dspConfig.masterGainDb >= 0f) "+" else ""}${"%.1f".format(uiState.dspConfig.masterGainDb)} dB",
                    accentColor = StudioColors.Cyan,
                    testTag = "chain_master_gain_slider",
                    onValueChange = { v ->
                        viewModel.updateDspConfig { it.copy(masterGainDb = v) }
                        viewModel.ensureLivePreviewPlaying()
                    },
                )

                if (uiState.hasAudio && AudioSignalAnalyzer.willGainCauseClipping(uiState.originalPeakDbfs, uiState.dspConfig.masterGainDb)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StudioColors.Rose.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, StudioColors.RoseBright.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chain_master_clipping_warning"),
                    ) {
                        Text(
                            text = t(lang, "clipping_warning"),
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioColors.RoseBright,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }

                val resetInteraction = remember { MutableInteractionSource() }
                OutlinedButton(
                    onClick = { viewModel.resetAllEffects() },
                    interactionSource = resetInteraction,
                    border = BorderStroke(1.dp, StudioColors.Rose.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioColors.RoseBright),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .lit(resetInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                        .testTag("chain_reset_all_button"),
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (lang == AppLanguage.EN) "Reset All Effects" else "إعادة تعيين كافة المؤثرات")
                }
            }
        }

        chain.forEachIndexed { idx, slot ->
            val slotInteraction = remember { MutableInteractionSource() }
            var isDragging by remember(slot.type) { mutableStateOf(false) }
            var dragOffsetY by remember(slot.type) { mutableFloatStateOf(0f) }
            var dragOriginY by remember(slot.type) { mutableFloatStateOf(0f) }
            var dragStartIndex by remember(slot.type) { mutableIntStateOf(idx) }
            var dragTargetIndex by remember(slot.type) { mutableIntStateOf(idx) }
            Card(
                onClick = { onOpenEffect(slot.type) },
                interactionSource = slotInteraction,
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (slot.enabled) StudioColors.ElevatedSurface else StudioColors.CardSurface,
                ),
                border = BorderStroke(
                    1.dp,
                    if (slot.enabled) StudioColors.Cyan.copy(alpha = 0.55f) else StudioColors.Border,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        itemCenters[slot.type] = coordinates.positionInRoot().y + coordinates.size.height / 2f
                    }
                    .pointerInput(slot.type) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                val currentIndex = latestChain.indexOfFirst { it.type == slot.type }
                                if (currentIndex >= 0) {
                                    dragStartIndex = currentIndex
                                    dragTargetIndex = currentIndex
                                    dragOriginY = itemCenters[slot.type] ?: 0f
                                    dragOffsetY = 0f
                                    isDragging = true
                                }
                            },
                            onDragEnd = {
                                if (dragTargetIndex != dragStartIndex) {
                                    viewModel.moveEffectInChain(dragStartIndex, dragTargetIndex)
                                }
                                isDragging = false
                                dragOffsetY = 0f
                            },
                            onDragCancel = {
                                isDragging = false
                                dragOffsetY = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffsetY += amount.y
                                val draggedCenter: Float = dragOriginY + dragOffsetY
                                val targetIndex = latestChain.indices.minByOrNull { candidate ->
                                    val candidateCenter = itemCenters[latestChain[candidate].type]
                                        ?: Float.POSITIVE_INFINITY
                                    abs(candidateCenter - draggedCenter)
                                }
                                if (targetIndex != null) {
                                    dragTargetIndex = targetIndex
                                }
                            },
                        )
                    }
                    .graphicsLayer { translationY = dragOffsetY }
                    .shadow(
                        elevation = if (isDragging) 10.dp else 0.dp,
                        shape = RoundedCornerShape(18.dp),
                    )
                    .lit(slotInteraction, MaterialTheme.colorScheme.primary, selected = slot.enabled, cornerRadius = 18.dp)
                    .testTag("chain_slot_${slot.type.name.lowercase()}"),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (slot.enabled) StudioColors.Cyan else StudioColors.BgMid),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${idx + 1}",
                            style = TelemetryTextStyle,
                            color = if (slot.enabled) StudioColors.BgTop else StudioColors.TextSecondary,
                        )
                    }

                    Icon(
                        imageVector = Icons.Rounded.DragIndicator,
                        contentDescription = if (lang == AppLanguage.EN) "Reorder effect" else "سحب لإعادة ترتيب المؤثر",
                        tint = if (isDragging) StudioColors.Cyan else StudioColors.TextMuted,
                        modifier = Modifier
                            .size(18.dp)
                            .testTag("chain_drag_handle_${slot.type.name.lowercase()}"),
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = t(lang, slot.type.titleKey),
                            style = MaterialTheme.typography.titleSmall,
                            color = StudioColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (slot.enabled) {
                                if (lang == AppLanguage.EN) "Active • Tap to adjust" else "مفعّل • اضغط للتعديل"
                            } else {
                                if (lang == AppLanguage.EN) "Bypassed • Tap to adjust" else "متجاوز • اضغط للتعديل"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (slot.enabled) StudioColors.Cyan else StudioColors.TextMuted,
                        )
                    }

                    Switch(
                        checked = slot.enabled,
                        onCheckedChange = { viewModel.toggleEffectEnabled(slot.type, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StudioColors.BgTop,
                            checkedTrackColor = StudioColors.Cyan,
                        ),
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("chain_toggle_${slot.type.name.lowercase()}"),
                    )
                }
            }
        }
    }
}

@Composable
fun EffectDetailScreen(
    effect: EffectType,
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onOpenEchoDetail: (EchoDetailGroup) -> Unit,
    onBack: () -> Unit,
) {
    if (effect == EffectType.ECHO_REVERB) {
        EchoReverbScreen(
            uiState = uiState,
            viewModel = viewModel,
            onOpenDetail = onOpenEchoDetail,
            onBack = onBack,
        )
        return
    }

    val lang = uiState.language
    val cfg = uiState.dspConfig
    val enabled = cfg.isEnabled(effect)
    var saveDraftDialogOpen by remember { mutableStateOf(false) }
    val sessionKey = "effect_${effect.name}"

    LaunchedEffect(sessionKey) {
        viewModel.beginLiveToolSession(sessionKey)
    }

    BackHandler {
        viewModel.discardLiveToolSession(sessionKey)
        onBack()
    }

    DisposableEffect(sessionKey) {
        onDispose {
            viewModel.stopToolPreview()
            viewModel.discardLiveToolSession(sessionKey)
        }
    }

    StudioScrollColumn {
        StudioFocusTopBar(
            title = t(lang, effect.titleKey),
            subtitle = if (enabled) {
                if (lang == AppLanguage.EN) "Processor Active" else "المعالج مفعّل بالمسار"
            } else {
                if (lang == AppLanguage.EN) "Processor Bypassed" else "المعالج متجاوز حالياً"
            },
            language = lang,
            onBack = {
                viewModel.discardLiveToolSession(sessionKey)
                onBack()
            },
            trailing = {
                UndoRedoActionsRow(
                    canUndo = uiState.canUndoEdit,
                    canRedo = uiState.canRedoEdit,
                    language = lang,
                    undoTag = "effect_detail_top_undo_button",
                    redoTag = "effect_detail_top_redo_button",
                    onUndo = { viewModel.undoLastEdit() },
                    onRedo = { viewModel.redoLastEdit() },
                    extraTrailing = {
                        Switch(
                            checked = enabled,
                            onCheckedChange = { viewModel.toggleEffectEnabled(effect, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = StudioColors.BgTop,
                                checkedTrackColor = StudioColors.Cyan,
                            ),
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("effect_detail_enable_switch"),
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
                when (effect) {
                    EffectType.EQUALIZER -> {
                        val eq = cfg.equalizer
                        val bands10 = eq.resolved10Bands()
                        val eqDensity = LocalDensity.current
                        val eqStroke = remember(eqDensity) {
                            Stroke(
                                width = with(eqDensity) { 2.5.dp.toPx() },
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            )
                        }
                        val eqPath = remember { Path() }
                        val curveFreqs = remember {
                            val minLn = ln(25.0)
                            val maxLn = ln(18000.0)
                            FloatArray(48) { idx ->
                                kotlin.math.exp(minLn + (maxLn - minLn) * (idx / 47.0)).toFloat()
                            }
                        }
                        val curveDb = remember(bands10.contentHashCode()) {
                            EqualizerProcessor.computeMagnitudeCurveDb(
                                sampleRate = 44100,
                                p = eq,
                                freqPointsHz = curveFreqs,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = t(lang, "eq_presets_title"),
                                style = MaterialTheme.typography.titleSmall,
                                color = StudioColors.Cyan,
                            )
                            OutlinedButton(
                                onClick = { viewModel.resetEqualizerOnly() },
                                border = BorderStroke(1.dp, StudioColors.Border),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 40.dp)
                                    .testTag("eq_reset_button"),
                            ) {
                                Text(t(lang, "eq_reset"), style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            EQ_PRESET_PROFILES.forEach { profile ->
                                val isSelected = bands10.indices.all { i ->
                                    kotlin.math.abs(bands10[i] - profile.bands10Db[i]) < 0.35f
                                }
                                val chipInt = remember { MutableInteractionSource() }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.applyEqPresetProfile(profile) },
                                    interactionSource = chipInt,
                                    label = { Text(t(lang, profile.titleKey), maxLines = 1) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StudioColors.Cyan.copy(alpha = 0.2f),
                                        selectedLabelColor = StudioColors.Cyan,
                                        containerColor = StudioColors.BgMid,
                                        labelColor = StudioColors.TextSecondary,
                                    ),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .lit(chipInt, MaterialTheme.colorScheme.primary, selected = isSelected, cornerRadius = 10.dp)
                                        .testTag("eq_preset_${profile.id}"),
                                )
                            }
                        }

                        // Real 10-band cascaded biquad frequency response curve Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(126.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(StudioColors.BgTop)
                                .border(1.dp, StudioColors.Border, RoundedCornerShape(14.dp))
                                .padding(12.dp),
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val midY = size.height / 2f
                                val scaleY = size.height * 0.40f / 12f
                                // +6 dB, 0 dB, -6 dB guides
                                listOf(-6f, 0f, 6f).forEach { refDb ->
                                    val gy = midY - refDb * scaleY
                                    drawLine(
                                        color = if (refDb == 0f) {
                                            StudioColors.BorderBright.copy(alpha = 0.55f)
                                        } else {
                                            StudioColors.Border.copy(alpha = 0.38f)
                                        },
                                        start = Offset(0f, gy),
                                        end = Offset(size.width, gy),
                                        strokeWidth = 1.dp.toPx(),
                                    )
                                }

                                eqPath.reset()
                                val minLn = ln(25.0)
                                val maxLn = ln(18000.0)
                                curveFreqs.forEachIndexed { idx, freq ->
                                    val normX = ((ln(freq.toDouble()) - minLn) / (maxLn - minLn)).toFloat().coerceIn(0f, 1f)
                                    val x = normX * size.width
                                    val y = (midY - curveDb[idx] * scaleY).coerceIn(4f, size.height - 4f)
                                    if (idx == 0) eqPath.moveTo(x, y) else eqPath.lineTo(x, y)
                                }
                                drawPath(
                                    path = eqPath,
                                    color = StudioColors.Cyan,
                                    style = eqStroke,
                                )
                                bands10.forEachIndexed { idx, db ->
                                    val normX = ((ln(EQ_10_FREQUENCIES_HZ[idx].toDouble()) - minLn) / (maxLn - minLn)).toFloat().coerceIn(0f, 1f)
                                    val cx = normX * size.width
                                    val cy = (midY - db * scaleY).coerceIn(4f, size.height - 4f)
                                    drawCircle(color = StudioColors.Violet, radius = 4.5.dp.toPx(), center = Offset(cx, cy))
                                    drawCircle(color = StudioColors.TextPrimary, radius = 1.8.dp.toPx(), center = Offset(cx, cy))
                                }
                            }
                        }

                        val bandColors = remember {
                            listOf(
                                StudioColors.Cyan,
                                StudioColors.Cyan,
                                StudioColors.Cyan,
                                StudioColors.Violet,
                                StudioColors.Violet,
                                StudioColors.Violet,
                                StudioColors.Emerald,
                                StudioColors.Emerald,
                                StudioColors.Amber,
                                StudioColors.Amber,
                            )
                        }
                        EQ_10_FREQUENCIES_HZ.forEachIndexed { bandIdx, freqHz ->
                            val bandVal = bands10[bandIdx]
                            val formatted = "${if (bandVal > 0f) "+" else ""}${"%.1f".format(bandVal)} dB"
                            StudioParamSlider(
                                label = EQ_10_LABELS[bandIdx],
                                value = bandVal,
                                min = -12f,
                                max = 12f,
                                formattedValue = formatted,
                                accentColor = bandColors[bandIdx],
                                testTag = "eq_$freqHz",
                            ) { v ->
                                viewModel.updateDspConfig {
                                    it.copy(equalizer = it.equalizer.with10Band(bandIdx, v))
                                        .withSlotEnabled(EffectType.EQUALIZER, true)
                                }
                                viewModel.ensureLivePreviewPlaying()
                            }
                        }

                        if (uiState.fricativeWarningEq) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StudioColors.Amber.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, StudioColors.Amber.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("eq_fricative_warning"),
                            ) {
                                Text(
                                    text = t(lang, "fricative_clarity_warning"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StudioColors.Amber,
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                        }
                    }

                    EffectType.BASS_BOOST -> {
                        val b = cfg.bassBoost
                        StudioParamSlider(
                            label = if (lang == AppLanguage.EN) "Bass Gain" else "شدة تعزيز الجهير",
                            value = b.gainDb,
                            min = 0f,
                            max = 12f,
                            formattedValue = "+${"%.1f".format(b.gainDb)} dB",
                            accentColor = StudioColors.Amber,
                            testTag = "bass_gain_slider",
                        ) { v ->
                            viewModel.updateDspConfig { it.copy(bassBoost = it.bassBoost.copy(gainDb = v)).withSlotEnabled(EffectType.BASS_BOOST, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                        StudioParamSlider(
                            label = if (lang == AppLanguage.EN) "Cutoff Frequency" else "تردد القطع",
                            value = b.frequencyHz,
                            min = 50f,
                            max = 220f,
                            formattedValue = "${b.frequencyHz.roundToInt()} Hz",
                            accentColor = StudioColors.Cyan,
                            testTag = "bass_freq_slider",
                        ) { v ->
                            viewModel.updateDspConfig { it.copy(bassBoost = it.bassBoost.copy(frequencyHz = v)).withSlotEnabled(EffectType.BASS_BOOST, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                    }

                    EffectType.TREBLE -> {
                        val tr = cfg.treble
                        StudioParamSlider(
                            label = if (lang == AppLanguage.EN) "Treble Gain" else "شدة الترددات العالية",
                            value = tr.gainDb,
                            min = -10f,
                            max = 12f,
                            formattedValue = "${"%.1f".format(tr.gainDb)} dB",
                            accentColor = StudioColors.Cyan,
                            testTag = "treble_gain_slider",
                        ) { v ->
                            viewModel.updateDspConfig { it.copy(treble = it.treble.copy(gainDb = v)).withSlotEnabled(EffectType.TREBLE, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                        StudioParamSlider(
                            label = if (lang == AppLanguage.EN) "Shelf Frequency" else "تردد الحدة",
                            value = tr.frequencyHz,
                            min = 2500f,
                            max = 12000f,
                            formattedValue = "${tr.frequencyHz.roundToInt()} Hz",
                            accentColor = StudioColors.Violet,
                            testTag = "treble_freq_slider",
                        ) { v ->
                            viewModel.updateDspConfig { it.copy(treble = it.treble.copy(frequencyHz = v)).withSlotEnabled(EffectType.TREBLE, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                    }

                    EffectType.PITCH_SHIFT -> {
                        val ps = cfg.pitchShift
                        StudioParamSlider(
                            label = if (lang == AppLanguage.EN) "Pitch Semitones" else "إزاحة الطبقة (Semitones)",
                            value = ps.semitones,
                            min = -12f,
                            max = 12f,
                            formattedValue = "${if (ps.semitones >= 0f) "+" else ""}${"%.1f".format(ps.semitones)} st",
                            accentColor = StudioColors.Violet,
                            testTag = "pitch_semitones_slider",
                        ) { v ->
                            viewModel.updateDspConfig { it.copy(pitchShift = it.pitchShift.copy(semitones = v)).withSlotEnabled(EffectType.PITCH_SHIFT, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                        StudioParamSlider(
                            label = if (lang == AppLanguage.EN) "Wet Mix" else "نسبة المزج",
                            value = ps.wetMix,
                            min = 0f,
                            max = 1f,
                            formattedValue = "${(ps.wetMix * 100f).roundToInt()}%",
                            accentColor = StudioColors.Cyan,
                            testTag = "pitch_mix_slider",
                        ) { v ->
                            viewModel.updateDspConfig { it.copy(pitchShift = it.pitchShift.copy(wetMix = v)).withSlotEnabled(EffectType.PITCH_SHIFT, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                    }

                    EffectType.TIME_STRETCH -> {
                        val ts = cfg.timeStretch
                        StudioParamSlider(
                            label = if (lang == AppLanguage.EN) "Tempo Ratio" else "سرعة التشغيل",
                            value = ts.tempoRatio,
                            min = 0.5f,
                            max = 2.5f,
                            formattedValue = "${"%.2f".format(ts.tempoRatio)}x",
                            accentColor = StudioColors.Amber,
                            testTag = "tempo_ratio_slider",
                        ) { v ->
                            viewModel.updateDspConfig { it.copy(timeStretch = it.timeStretch.copy(tempoRatio = v)).withSlotEnabled(EffectType.TIME_STRETCH, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                    }

                    EffectType.CHORUS -> {
                        val ch = cfg.chorus
                        StudioParamSlider("Rate", ch.rateHz, 0.1f, 5f, "${"%.2f".format(ch.rateHz)} Hz", StudioColors.Cyan, testTag = "chorus_rate") { v ->
                            viewModel.updateDspConfig { it.copy(chorus = it.chorus.copy(rateHz = v)).withSlotEnabled(EffectType.CHORUS, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                        StudioParamSlider("Depth", ch.depthMs, 1f, 18f, "${"%.1f".format(ch.depthMs)} ms", StudioColors.Violet, testTag = "chorus_depth") { v ->
                            viewModel.updateDspConfig { it.copy(chorus = it.chorus.copy(depthMs = v)).withSlotEnabled(EffectType.CHORUS, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                        StudioParamSlider("Mix", ch.mix, 0f, 1f, "${(ch.mix * 100f).roundToInt()}%", StudioColors.Emerald, testTag = "chorus_mix") { v ->
                            viewModel.updateDspConfig { it.copy(chorus = it.chorus.copy(mix = v)).withSlotEnabled(EffectType.CHORUS, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                    }

                    EffectType.FLANGER -> {
                        val fl = cfg.flanger
                        StudioParamSlider("Rate", fl.rateHz, 0.05f, 3f, "${"%.2f".format(fl.rateHz)} Hz", StudioColors.Cyan, testTag = "flanger_rate") { v ->
                            viewModel.updateDspConfig { it.copy(flanger = it.flanger.copy(rateHz = v)).withSlotEnabled(EffectType.FLANGER, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                        StudioParamSlider("Depth", fl.depthMs, 0.5f, 8f, "${"%.1f".format(fl.depthMs)} ms", StudioColors.Violet, testTag = "flanger_depth") { v ->
                            viewModel.updateDspConfig { it.copy(flanger = it.flanger.copy(depthMs = v)).withSlotEnabled(EffectType.FLANGER, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                        StudioParamSlider("Feedback", fl.feedback, 0f, 0.85f, "${(fl.feedback * 100f).roundToInt()}%", StudioColors.Amber, testTag = "flanger_feedback") { v ->
                            viewModel.updateDspConfig { it.copy(flanger = it.flanger.copy(feedback = v)).withSlotEnabled(EffectType.FLANGER, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                        StudioParamSlider("Mix", fl.mix, 0f, 1f, "${(fl.mix * 100f).roundToInt()}%", StudioColors.Emerald, testTag = "flanger_mix") { v ->
                            viewModel.updateDspConfig { it.copy(flanger = it.flanger.copy(mix = v)).withSlotEnabled(EffectType.FLANGER, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                    }

                    EffectType.PHASER -> {
                        val ph = cfg.phaser
                        StudioParamSlider("Rate", ph.rateHz, 0.1f, 4f, "${"%.2f".format(ph.rateHz)} Hz", StudioColors.Cyan, testTag = "phaser_rate") { v ->
                            viewModel.updateDspConfig { it.copy(phaser = it.phaser.copy(rateHz = v)).withSlotEnabled(EffectType.PHASER, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                        StudioParamSlider("Depth", ph.depth, 0f, 1f, "${(ph.depth * 100f).roundToInt()}%", StudioColors.Violet, testTag = "phaser_depth") { v ->
                            viewModel.updateDspConfig { it.copy(phaser = it.phaser.copy(depth = v)).withSlotEnabled(EffectType.PHASER, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                        StudioParamSlider("Feedback", ph.feedback, 0f, 0.85f, "${(ph.feedback * 100f).roundToInt()}%", StudioColors.Amber, testTag = "phaser_feedback") { v ->
                            viewModel.updateDspConfig { it.copy(phaser = it.phaser.copy(feedback = v)).withSlotEnabled(EffectType.PHASER, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                        StudioParamSlider("Mix", ph.mix, 0f, 1f, "${(ph.mix * 100f).roundToInt()}%", StudioColors.Emerald, testTag = "phaser_mix") { v ->
                            viewModel.updateDspConfig { it.copy(phaser = it.phaser.copy(mix = v)).withSlotEnabled(EffectType.PHASER, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                    }

                    EffectType.DISTORTION -> {
                        val d = cfg.distortion
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            DistortionMode.entries.forEach { mode ->
                                val selected = mode == d.mode
                                val chipInteraction = remember { MutableInteractionSource() }
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        viewModel.updateDspConfig {
                                            it.copy(distortion = it.distortion.copy(mode = mode)).withSlotEnabled(EffectType.DISTORTION, true)
                                        }
                                        viewModel.ensureLivePreviewPlaying()
                                    },
                                    interactionSource = chipInteraction,
                                    label = { Text(mode.name.replace('_', ' '), maxLines = 1) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                        .lit(chipInteraction, MaterialTheme.colorScheme.primary, selected = selected, cornerRadius = 10.dp)
                                        .testTag("distortion_mode_${mode.name.lowercase()}"),
                                )
                            }
                        }
                        StudioParamSlider("Drive", d.drive, 1f, 12f, "${"%.1f".format(d.drive)}x", StudioColors.Rose, testTag = "dist_drive") { v ->
                            viewModel.updateDspConfig { it.copy(distortion = it.distortion.copy(drive = v)).withSlotEnabled(EffectType.DISTORTION, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                        StudioParamSlider("Tone Cutoff", d.toneHz, 800f, 12000f, "${d.toneHz.roundToInt()} Hz", StudioColors.Amber, testTag = "dist_tone") { v ->
                            viewModel.updateDspConfig { it.copy(distortion = it.distortion.copy(toneHz = v)).withSlotEnabled(EffectType.DISTORTION, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                        StudioParamSlider("Mix", d.mix, 0f, 1f, "${(d.mix * 100f).roundToInt()}%", StudioColors.Cyan, testTag = "dist_mix") { v ->
                            viewModel.updateDspConfig { it.copy(distortion = it.distortion.copy(mix = v)).withSlotEnabled(EffectType.DISTORTION, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                    }

                    EffectType.COMPRESSOR -> {
                        val comp = cfg.compressor
                        CompressorGainReductionMeter(
                            language = lang,
                            playbackFlow = viewModel.playbackTelemetry,
                        )

                        StudioParamSlider("Threshold", comp.thresholdDb, -42f, -2f, "${comp.thresholdDb.roundToInt()} dB", StudioColors.Cyan, testTag = "comp_thresh") { v ->
                            viewModel.updateDspConfig { it.copy(compressor = it.compressor.copy(thresholdDb = v)).withSlotEnabled(EffectType.COMPRESSOR, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                        StudioParamSlider("Ratio", comp.ratio, 1.2f, 12f, "${"%.1f".format(comp.ratio)}:1", StudioColors.Violet, testTag = "comp_ratio") { v ->
                            viewModel.updateDspConfig { it.copy(compressor = it.compressor.copy(ratio = v)).withSlotEnabled(EffectType.COMPRESSOR, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                        StudioParamSlider("Attack", comp.attackMs, 1f, 80f, "${comp.attackMs.roundToInt()} ms", StudioColors.Emerald, testTag = "comp_attack") { v ->
                            viewModel.updateDspConfig { it.copy(compressor = it.compressor.copy(attackMs = v)).withSlotEnabled(EffectType.COMPRESSOR, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                        StudioParamSlider("Release", comp.releaseMs, 20f, 500f, "${comp.releaseMs.roundToInt()} ms", StudioColors.Emerald, testTag = "comp_release") { v ->
                            viewModel.updateDspConfig { it.copy(compressor = it.compressor.copy(releaseMs = v)).withSlotEnabled(EffectType.COMPRESSOR, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                        StudioParamSlider("Makeup Gain", comp.makeupGainDb, -6f, 12f, "+${"%.1f".format(comp.makeupGainDb)} dB", StudioColors.Amber, testTag = "comp_makeup") { v ->
                            viewModel.updateDspConfig { it.copy(compressor = it.compressor.copy(makeupGainDb = v)).withSlotEnabled(EffectType.COMPRESSOR, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                    }

                    EffectType.LIMITER -> {
                        val lim = cfg.limiter
                        StudioParamSlider("Ceiling", lim.ceilingDb, -6f, -0.1f, "${"%.1f".format(lim.ceilingDb)} dB", StudioColors.Rose, testTag = "limiter_ceiling") { v ->
                            viewModel.updateDspConfig { it.copy(limiter = it.limiter.copy(ceilingDb = v)).withSlotEnabled(EffectType.LIMITER, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                        StudioParamSlider("Release", lim.releaseMs, 10f, 300f, "${lim.releaseMs.roundToInt()} ms", StudioColors.Cyan, testTag = "limiter_release") { v ->
                            viewModel.updateDspConfig { it.copy(limiter = it.limiter.copy(releaseMs = v)).withSlotEnabled(EffectType.LIMITER, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                    }

                    EffectType.NOISE_REDUCTION -> {
                        val nr = cfg.noiseReduction
                        StudioParamSlider("Spectral Strength", nr.spectralStrength, 0f, 1f, "${(nr.spectralStrength * 100f).roundToInt()}%", StudioColors.Emerald, testTag = "nr_strength") { v ->
                            viewModel.updateDspConfig { it.copy(noiseReduction = it.noiseReduction.copy(spectralStrength = v)).withSlotEnabled(EffectType.NOISE_REDUCTION, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                        StudioParamSlider("Gate Threshold", nr.thresholdDb, -70f, -18f, "${nr.thresholdDb.roundToInt()} dB", StudioColors.Cyan, testTag = "nr_thresh") { v ->
                            viewModel.updateDspConfig { it.copy(noiseReduction = it.noiseReduction.copy(thresholdDb = v)).withSlotEnabled(EffectType.NOISE_REDUCTION, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                        StudioParamSlider("Reduction Depth", nr.reductionDb, 0f, 36f, "${nr.reductionDb.roundToInt()} dB", StudioColors.Violet, testTag = "nr_depth") { v ->
                            viewModel.updateDspConfig { it.copy(noiseReduction = it.noiseReduction.copy(reductionDb = v)).withSlotEnabled(EffectType.NOISE_REDUCTION, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                        StudioParamSlider("High-Pass Cutoff", nr.highPassHz, 20f, 260f, "${nr.highPassHz.roundToInt()} Hz", StudioColors.Amber, testTag = "nr_hp") { v ->
                            viewModel.updateDspConfig { it.copy(noiseReduction = it.noiseReduction.copy(highPassHz = v)).withSlotEnabled(EffectType.NOISE_REDUCTION, true) }
                            viewModel.ensureLivePreviewPlaying()
                        }
                    }

                    EffectType.ECHO_REVERB -> Unit
                }

                val applyEffectInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = { viewModel.commitLiveToolSession(sessionKey, t(lang, effect.titleKey)) },
                    enabled = uiState.hasAudio && !uiState.isBusy,
                    interactionSource = applyEffectInteraction,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioColors.Cyan, contentColor = StudioColors.BgTop),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 50.dp)
                        .lit(applyEffectInteraction, StudioColors.Cyan, cornerRadius = 14.dp)
                        .testTag("effect_detail_apply_button"),
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
private fun EffectsChainSpectrumSection(
    playbackFlow: StateFlow<PlaybackTelemetry>,
    language: AppLanguage,
) {
    val playback by playbackFlow.collectAsStateWithLifecycle()
    SpectrumVisualizer(
        spectrumBands = playback.spectrumBands,
        isActive = playback.isPlaying,
        language = language,
        peakLevel = playback.outputLevel,
        accentColor = StudioColors.VioletBright,
    )
}

@Composable
private fun CompressorGainReductionMeter(
    language: AppLanguage,
    playbackFlow: StateFlow<PlaybackTelemetry>,
) {
    val playback by playbackFlow.collectAsStateWithLifecycle()
    val grNorm = (playback.gainReductionDb / 24f).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (language == AppLanguage.EN) "Gain Reduction" else "خفض القمم (GR)",
                style = MaterialTheme.typography.labelMedium,
                color = StudioColors.TextSecondary,
            )
            Text(
                text = "-${"%.1f".format(playback.gainReductionDb)} dB",
                style = TelemetryTextStyle,
                color = StudioColors.Amber,
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
                    .fillMaxWidth(grNorm.coerceAtLeast(0.02f))
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(StudioColors.Amber),
            )
        }
    }
}
