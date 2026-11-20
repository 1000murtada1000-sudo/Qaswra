package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.audio.MergeStudioMode
import com.example.audio.MergeStudioParams
import com.example.audio.computeMergeTimelineLayout
import com.example.audio.formatDurationMs
import com.example.i18n.t
import com.example.model.AppLanguage
import com.example.model.AudioPickerTarget
import com.example.studio.ui.lit
import com.example.ui.components.ExportFormatDialog
import com.example.ui.components.InfoBadgeButton
import com.example.ui.components.PreviewAndApplyActionRow
import com.example.ui.components.StudioFocusTopBar
import com.example.ui.components.StudioParamSlider
import com.example.ui.components.StudioScrollColumn
import com.example.ui.components.ToolInfoDialog
import com.example.ui.components.UndoRedoActionsRow
import com.example.ui.components.rememberEditHistory
import com.example.ui.theme.StudioColors
import com.example.ui.theme.TelemetryTextStyle
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PlaybackTelemetry
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
fun UnifiedMergeOverlayStudioScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    initialMode: MergeStudioMode = MergeStudioMode.SEQUENTIAL,
    initialAutoDucking: Boolean = false,
    toolId: String = "merge_studio",
    onOpenPicker: (AudioPickerTarget) -> Unit,
    onBack: () -> Unit,
) {
    val lang = uiState.language
    var params by remember(toolId) {
        mutableStateOf(
            MergeStudioParams(
                mode = initialMode,
                autoDuckingEnabled = initialAutoDucking,
                duckingStrengthPct = if (initialAutoDucking) 65f else 0f,
            ),
        )
    }

    LaunchedEffect(uiState.restoredToolSession?.id) {
        val restored = viewModel.consumeRestoredToolSession(toolId)
        if (restored != null) {
            val modeName = restored.stringParams["mode"]
            val mode = runCatching { MergeStudioMode.valueOf(modeName ?: initialMode.name) }
                .getOrDefault(initialMode)
            val fp = restored.floatParams
            params = MergeStudioParams(
                mode = mode,
                primaryStartOffsetSec = fp["primaryStartOffsetSec"] ?: params.primaryStartOffsetSec,
                crossfadeSec = fp["crossfadeSec"] ?: params.crossfadeSec,
                overlayFadeInSec = fp["overlayFadeInSec"] ?: params.overlayFadeInSec,
                overlayFadeOutSec = fp["overlayFadeOutSec"] ?: params.overlayFadeOutSec,
                overlayStartOffsetSec = fp["overlayStartOffsetSec"] ?: params.overlayStartOffsetSec,
                primaryLevelPct = fp["primaryLevelPct"] ?: params.primaryLevelPct,
                secondaryLevelPct = fp["secondaryLevelPct"] ?: params.secondaryLevelPct,
                autoDuckingEnabled = (fp["autoDuckingEnabled"] ?: 0f) > 0.5f,
                duckingStrengthPct = fp["duckingStrengthPct"] ?: params.duckingStrengthPct,
                autoLevelingEnabled = (fp["autoLevelingEnabled"] ?: 0f) > 0.5f,
                levelingStrengthPct = fp["levelingStrengthPct"] ?: params.levelingStrengthPct,
                echoTransitionEnabled = (fp["echoTransitionEnabled"] ?: 0f) > 0.5f,
                echoTransitionPct = fp["echoTransitionPct"] ?: params.echoTransitionPct,
                gapWhooshEnabled = (fp["gapWhooshEnabled"] ?: 0f) > 0.5f,
                transitionGapSec = fp["transitionGapSec"] ?: params.transitionGapSec,
                whooshIntensityPct = fp["whooshIntensityPct"] ?: params.whooshIntensityPct,
            )
        }
    }

    val latestParams by rememberUpdatedState(params)
    DisposableEffect(toolId) {
        onDispose {
            viewModel.stopToolPreview()
            viewModel.clearMergeStudioPreview()
            val cur = latestParams
            viewModel.saveToolSessionToCache(
                toolId = toolId,
                titleAr = if (toolId == "smart_mixer") "ميكس الموسيقى والدمج" else "استوديو الدمج والتراكب",
                titleEn = if (toolId == "smart_mixer") "Smart Mixer & Overlay" else "Merge & Overlay Studio",
                summaryAr = when (cur.mode) {
                    MergeStudioMode.SEQUENTIAL -> "دمج متتالي"
                    MergeStudioMode.CROSSFADE -> "تداخل ${"%.1f".format(cur.crossfadeSec)}s"
                    MergeStudioMode.OVERLAY -> "تراكب حر"
                },
                summaryEn = when (cur.mode) {
                    MergeStudioMode.SEQUENTIAL -> "Sequential"
                    MergeStudioMode.CROSSFADE -> "Crossfade ${"%.1f".format(cur.crossfadeSec)}s"
                    MergeStudioMode.OVERLAY -> "Free Overlay"
                },
                floatParams = mapOf(
                    "primaryStartOffsetSec" to cur.primaryStartOffsetSec,
                    "crossfadeSec" to cur.crossfadeSec,
                    "overlayFadeInSec" to cur.overlayFadeInSec,
                    "overlayFadeOutSec" to cur.overlayFadeOutSec,
                    "overlayStartOffsetSec" to cur.overlayStartOffsetSec,
                    "primaryLevelPct" to cur.primaryLevelPct,
                    "secondaryLevelPct" to cur.secondaryLevelPct,
                    "autoDuckingEnabled" to if (cur.autoDuckingEnabled) 1f else 0f,
                    "duckingStrengthPct" to cur.duckingStrengthPct,
                    "autoLevelingEnabled" to if (cur.autoLevelingEnabled) 1f else 0f,
                    "levelingStrengthPct" to cur.levelingStrengthPct,
                    "echoTransitionEnabled" to if (cur.echoTransitionEnabled) 1f else 0f,
                    "echoTransitionPct" to cur.echoTransitionPct,
                    "gapWhooshEnabled" to if (cur.gapWhooshEnabled) 1f else 0f,
                    "transitionGapSec" to cur.transitionGapSec,
                    "whooshIntensityPct" to cur.whooshIntensityPct,
                ),
                stringParams = mapOf("mode" to cur.mode.name),
            )
        }
    }

    val paramHistory = rememberEditHistory(params)
    var preferVmUndo by remember { mutableStateOf(false) }
    var exportDialogOpen by remember { mutableStateOf(false) }
    var infoDialogOpen by remember { mutableStateOf(false) }

    var expandDucking by remember { mutableStateOf(initialAutoDucking) }
    var expandLeveling by remember { mutableStateOf(false) }
    var expandEchoBeat by remember { mutableStateOf(false) }
    var expandGapWhoosh by remember { mutableStateOf(false) }

    val hasPrimary = uiState.hasAudio
    val hasSecondary = !uiState.mixBgName.isNullOrBlank()
    val canProcess = hasPrimary && hasSecondary

    LaunchedEffect(params, hasPrimary, hasSecondary, uiState.durationMs, uiState.mixBgDurationMs) {
        if (hasPrimary && hasSecondary) {
            viewModel.syncMergeStudioPreview(params)
        } else {
            viewModel.clearMergeStudioPreview()
        }
    }

    fun updateParams(debounceMs: Long = 0L, transform: (MergeStudioParams) -> MergeStudioParams) {
        preferVmUndo = false
        val next = transform(params)
        params = next
        paramHistory.commit(next, debounceMs = debounceMs)
    }

    StudioScrollColumn {
        StudioFocusTopBar(
            title = t(lang, "merge_studio_title"),
            subtitle = t(lang, "merge_studio_subtitle"),
            language = lang,
            onBack = onBack,
            trailing = {
                UndoRedoActionsRow(
                    canUndo = paramHistory.canUndo || uiState.canUndoEdit,
                    canRedo = paramHistory.canRedo || uiState.canRedoEdit,
                    language = lang,
                    undoTag = "merge_studio_undo_button",
                    redoTag = "merge_studio_redo_button",
                    onUndo = {
                        if ((preferVmUndo && uiState.canUndoEdit) || !paramHistory.canUndo) {
                            viewModel.undoLastEdit()
                        } else {
                            paramHistory.undo()?.let { params = it }
                        }
                    },
                    onRedo = {
                        if ((preferVmUndo && uiState.canRedoEdit) || !paramHistory.canRedo) {
                            viewModel.redoLastEdit()
                        } else {
                            paramHistory.redo()?.let { params = it }
                        }
                    },
                    extraTrailing = {
                        InfoBadgeButton(
                            contentDescription = t(lang, "merge_studio_title"),
                            testTag = "merge_studio_info_badge",
                            accentColor = StudioColors.Cyan,
                            onClick = { infoDialogOpen = true },
                        )
                    },
                )
            },
        )

        // Track Cards: Primary + Secondary
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
            border = BorderStroke(1.dp, if (hasPrimary) StudioColors.Cyan.copy(alpha = 0.45f) else StudioColors.Border),
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, tint = StudioColors.Cyan)
                        Text(
                            text = if (hasPrimary) uiState.activeProjectName else t(lang, "no_audio_loaded"),
                            style = MaterialTheme.typography.titleSmall,
                            color = StudioColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (hasPrimary) {
                        Text(
                            text = formatDurationMs(uiState.durationMs),
                            style = TelemetryTextStyle,
                            color = StudioColors.Cyan,
                        )
                    }
                }

                val pickPrimaryInteraction = remember { MutableInteractionSource() }
                OutlinedButton(
                    onClick = { onOpenPicker(AudioPickerTarget.IMPORT_MAIN) },
                    interactionSource = pickPrimaryInteraction,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .lit(pickPrimaryInteraction, StudioColors.Cyan, cornerRadius = 12.dp)
                        .testTag("merge_studio_pick_primary_button"),
                ) {
                    Icon(Icons.Rounded.LibraryMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(t(lang, "audio_tools_pick_source"))
                }
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
            border = BorderStroke(1.dp, if (hasSecondary) StudioColors.Amber.copy(alpha = 0.55f) else StudioColors.Border),
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = StudioColors.Amber)
                        Text(
                            text = uiState.mixBgName ?: t(lang, "smart_mixer_music_track"),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (hasSecondary) StudioColors.Amber else StudioColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (hasSecondary) {
                        Text(
                            text = formatDurationMs(uiState.mixBgDurationMs),
                            style = TelemetryTextStyle,
                            color = StudioColors.TextSecondary,
                        )
                        IconButton(
                            onClick = { viewModel.clearMixBackgroundTrack() },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("merge_studio_clear_secondary_button"),
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = t(lang, "delete"), tint = StudioColors.RoseBright)
                        }
                    }
                }

                val pickSecInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = { onOpenPicker(AudioPickerTarget.MIX_BACKGROUND) },
                    interactionSource = pickSecInteraction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioColors.Amber.copy(alpha = 0.2f),
                        contentColor = StudioColors.Amber,
                    ),
                    border = BorderStroke(1.dp, StudioColors.Amber.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .lit(pickSecInteraction, StudioColors.Amber, cornerRadius = 12.dp)
                        .testTag("merge_studio_pick_secondary_button"),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(t(lang, "smart_mixer_pick_music"))
                }

                if (uiState.projects.any { it.hasAudio }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        uiState.projects.filter { it.hasAudio }.forEach { proj ->
                            FilterChip(
                                selected = uiState.mixBgName == proj.name,
                                onClick = { viewModel.loadSavedProjectAsSecondaryTrack(proj) },
                                label = { Text("${proj.name} (${formatDurationMs(proj.durationMs)})") },
                                modifier = Modifier.defaultMinSize(minHeight = 44.dp),
                            )
                        }
                    }
                }
            }
        }

        // Dual-Track Interactive Timeline above Balance & Mode Settings
        if (hasPrimary || hasSecondary) {
            DualTrackInteractiveTimelineCard(
                language = lang,
                primaryName = if (hasPrimary) uiState.activeProjectName else t(lang, "no_audio_loaded"),
                primaryDurMs = if (hasPrimary) uiState.durationMs else 0L,
                primaryPeaks = uiState.waveformPeaks,
                secondaryName = uiState.mixBgName ?: t(lang, "smart_mixer_music_track"),
                secondaryDurMs = if (hasSecondary) uiState.mixBgDurationMs else 0L,
                secondaryPeaks = uiState.mixBgPeaks,
                hasPrimary = hasPrimary,
                hasSecondary = hasSecondary,
                params = params,
                playbackFlow = viewModel.playbackTelemetry,
                onSeekFraction = { frac -> viewModel.seekToFraction(frac) },
                onUpdateParams = { debounceMs, transform -> updateParams(debounceMs, transform) },
            )
        }

        // Mode Selector & Core Controls
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val selectedMergeTab = if (params.mode == MergeStudioMode.OVERLAY) 1 else 0
                TabRow(
                    selectedTabIndex = selectedMergeTab,
                    containerColor = StudioColors.BgMid,
                    contentColor = StudioColors.Cyan,
                    indicator = { tabPositions ->
                        if (selectedMergeTab in tabPositions.indices) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedMergeTab]),
                                color = StudioColors.Cyan,
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("merge_studio_tabs"),
                ) {
                    Tab(
                        selected = selectedMergeTab == 0,
                        onClick = {
                            val nextMode = if (params.mode == MergeStudioMode.CROSSFADE) MergeStudioMode.CROSSFADE else MergeStudioMode.SEQUENTIAL
                            updateParams { it.copy(mode = nextMode) }
                        },
                        text = {
                            Text(
                                text = t(lang, "merge_tab_sequential"),
                                fontWeight = if (selectedMergeTab == 0) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        modifier = Modifier.testTag("merge_tab_sequential"),
                    )
                    Tab(
                        selected = selectedMergeTab == 1,
                        onClick = {
                            expandDucking = true
                            updateParams { cur ->
                                cur.copy(
                                    mode = MergeStudioMode.OVERLAY,
                                    autoDuckingEnabled = true,
                                    duckingStrengthPct = if (cur.duckingStrengthPct < 10f) 65f else cur.duckingStrengthPct,
                                )
                            }
                        },
                        text = {
                            Text(
                                text = t(lang, "merge_tab_smart_mix"),
                                fontWeight = if (selectedMergeTab == 1) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        modifier = Modifier.testTag("merge_tab_smart_mix"),
                    )
                }

                StudioParamSlider(
                    label = if (params.mode == MergeStudioMode.OVERLAY) {
                        if (lang == AppLanguage.EN) "Primary Voice Level" else "مستوى الصوت الأساسي"
                    } else {
                        if (lang == AppLanguage.EN) "Primary Clip Balance" else "توازن المقطع الأول"
                    },
                    value = params.primaryLevelPct,
                    min = -60f,
                    max = 60f,
                    formattedValue = "${if (params.primaryLevelPct > 0f) "+" else ""}${params.primaryLevelPct.roundToInt()}%",
                    accentColor = StudioColors.Cyan,
                    testTag = "merge_primary_level_slider",
                    onValueChange = { v -> updateParams(300L) { it.copy(primaryLevelPct = v) } },
                )

                StudioParamSlider(
                    label = if (params.mode == MergeStudioMode.OVERLAY) {
                        if (lang == AppLanguage.EN) "Background Music Volume" else "مستوى صوت الخلفية"
                    } else {
                        if (lang == AppLanguage.EN) "Secondary Clip Balance" else "توازن المقطع الثاني"
                    },
                    value = params.secondaryLevelPct,
                    min = -60f,
                    max = 60f,
                    formattedValue = "${if (params.secondaryLevelPct > 0f) "+" else ""}${params.secondaryLevelPct.roundToInt()}%",
                    accentColor = StudioColors.Amber,
                    testTag = "merge_secondary_level_slider",
                    onValueChange = { v -> updateParams(300L) { it.copy(secondaryLevelPct = v) } },
                )

                if (selectedMergeTab == 0) {
                    val xfadeInteraction = remember { MutableInteractionSource() }
                    FilterChip(
                        selected = params.mode == MergeStudioMode.CROSSFADE,
                        onClick = {
                            val nextMode = if (params.mode == MergeStudioMode.CROSSFADE) {
                                MergeStudioMode.SEQUENTIAL
                            } else {
                                MergeStudioMode.CROSSFADE
                            }
                            updateParams { it.copy(mode = nextMode) }
                        },
                        interactionSource = xfadeInteraction,
                        label = { Text(t(lang, "merge_mode_crossfade")) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioColors.Emerald.copy(alpha = 0.2f),
                            selectedLabelColor = StudioColors.Emerald,
                        ),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(xfadeInteraction, StudioColors.Emerald, selected = params.mode == MergeStudioMode.CROSSFADE, cornerRadius = 10.dp)
                            .testTag("merge_mode_crossfade"),
                    )

                    if (params.mode == MergeStudioMode.CROSSFADE) {
                        StudioParamSlider(
                            label = t(lang, "merge_crossfade_duration"),
                            value = params.crossfadeSec,
                            min = 0.1f,
                            max = 15.0f,
                            formattedValue = "${"%.2f".format(params.crossfadeSec)} s",
                            accentColor = StudioColors.Emerald,
                            testTag = "merge_crossfade_slider",
                            onValueChange = { v -> updateParams(300L) { it.copy(crossfadeSec = v) } },
                        )
                    }
                }

                if (params.mode == MergeStudioMode.OVERLAY) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = StudioColors.ElevatedSurface,
                        border = BorderStroke(
                            1.dp,
                            if (params.autoDuckingEnabled) StudioColors.Emerald.copy(alpha = 0.6f) else StudioColors.Border,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = t(lang, "merge_service_ducking"),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = StudioColors.Emerald,
                                    modifier = Modifier.weight(1f),
                                )
                                Switch(
                                    checked = params.autoDuckingEnabled,
                                    onCheckedChange = { on -> updateParams { it.copy(autoDuckingEnabled = on) } },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = StudioColors.BgTop,
                                        checkedTrackColor = StudioColors.Emerald,
                                    ),
                                    modifier = Modifier.minimumInteractiveComponentSize().testTag("merge_smart_ducking_switch"),
                                )
                            }
                            if (params.autoDuckingEnabled) {
                                StudioParamSlider(
                                    label = if (lang == AppLanguage.EN) "Auto-Ducking Depth (During Speech)" else "قوة خفض الخلفية التلقائي أثناء الكلام",
                                    value = params.duckingStrengthPct,
                                    min = 0f,
                                    max = 100f,
                                    formattedValue = "${params.duckingStrengthPct.roundToInt()}%",
                                    accentColor = StudioColors.Emerald,
                                    testTag = "merge_ducking_strength_slider",
                                    onValueChange = { v -> updateParams(300L) { it.copy(duckingStrengthPct = v, autoDuckingEnabled = true) } },
                                )
                            }
                        }
                    }

                    StudioParamSlider(
                        label = t(lang, "merge_fade_in"),
                        value = params.overlayFadeInSec,
                        min = 0f,
                        max = 15f,
                        formattedValue = "${"%.2f".format(params.overlayFadeInSec)} s",
                        accentColor = StudioColors.Emerald,
                        testTag = "merge_fade_in_slider",
                        onValueChange = { v -> updateParams(300L) { it.copy(overlayFadeInSec = v) } },
                    )
                    StudioParamSlider(
                        label = t(lang, "merge_fade_out"),
                        value = params.overlayFadeOutSec,
                        min = 0f,
                        max = 15f,
                        formattedValue = "${"%.2f".format(params.overlayFadeOutSec)} s",
                        accentColor = StudioColors.Violet,
                        testTag = "merge_fade_out_slider",
                        onValueChange = { v -> updateParams(300L) { it.copy(overlayFadeOutSec = v) } },
                    )
                    StudioParamSlider(
                        label = t(lang, "merge_start_offset"),
                        value = params.overlayStartOffsetSec,
                        min = 0f,
                        max = 60f,
                        formattedValue = "${"%.2f".format(params.overlayStartOffsetSec)} s",
                        accentColor = StudioColors.Cyan,
                        testTag = "merge_start_offset_slider",
                        onValueChange = { v -> updateParams(300L) { it.copy(overlayStartOffsetSec = v) } },
                    )
                }
            }
        }

        // Collapsible Pro Services (Auto-Ducking is elevated inline when Smart Background Mix tab is active)
        if (params.mode != MergeStudioMode.OVERLAY) {
            CollapsibleServiceCard(
                title = t(lang, "merge_service_ducking"),
                expanded = expandDucking,
                enabled = params.autoDuckingEnabled,
                accent = StudioColors.Emerald,
                tagPrefix = "merge_service_ducking",
                onToggleExpand = { expandDucking = !expandDucking },
                onToggleEnabled = { on ->
                    if (on) expandDucking = true
                    updateParams { it.copy(autoDuckingEnabled = on) }
                },
            ) {
                StudioParamSlider(
                    label = if (lang == AppLanguage.EN) "Ducking Attenuation Depth" else "عمق خفض الخلفية عند التحدث",
                    value = params.duckingStrengthPct,
                    min = 0f,
                    max = 100f,
                    formattedValue = "${params.duckingStrengthPct.roundToInt()}%",
                    accentColor = StudioColors.Emerald,
                    testTag = "merge_ducking_strength_slider",
                    onValueChange = { v -> updateParams(300L) { it.copy(duckingStrengthPct = v, autoDuckingEnabled = true) } },
                )
            }
        }

        CollapsibleServiceCard(
            title = t(lang, "merge_service_leveling"),
            expanded = expandLeveling,
            enabled = params.autoLevelingEnabled,
            accent = StudioColors.Cyan,
            tagPrefix = "merge_service_leveling",
            onToggleExpand = { expandLeveling = !expandLeveling },
            onToggleEnabled = { on ->
                if (on) expandLeveling = true
                updateParams { it.copy(autoLevelingEnabled = on) }
            },
        ) {
            StudioParamSlider(
                label = if (lang == AppLanguage.EN) "Loudnorm Equalization Strength" else "قوة موازنة الصوت الذكية",
                value = params.levelingStrengthPct,
                min = 0f,
                max = 100f,
                formattedValue = "${params.levelingStrengthPct.roundToInt()}%",
                accentColor = StudioColors.Cyan,
                testTag = "merge_leveling_strength_slider",
                onValueChange = { v -> updateParams(300L) { it.copy(levelingStrengthPct = v, autoLevelingEnabled = true) } },
            )
        }

        CollapsibleServiceCard(
            title = t(lang, "merge_service_echo_beat"),
            expanded = expandEchoBeat,
            enabled = params.echoTransitionEnabled,
            accent = StudioColors.Violet,
            tagPrefix = "merge_service_echo_beat",
            onToggleExpand = { expandEchoBeat = !expandEchoBeat },
            onToggleEnabled = { on ->
                if (on) expandEchoBeat = true
                updateParams { it.copy(echoTransitionEnabled = on) }
            },
        ) {
            StudioParamSlider(
                label = if (lang == AppLanguage.EN) "Echo Tail Transition Intensity" else "كثافة صدى الانتقال",
                value = params.echoTransitionPct,
                min = 0f,
                max = 100f,
                formattedValue = "${params.echoTransitionPct.roundToInt()}%",
                accentColor = StudioColors.Violet,
                testTag = "merge_echo_transition_slider",
                onValueChange = { v -> updateParams(300L) { it.copy(echoTransitionPct = v, echoTransitionEnabled = true) } },
            )
        }

        CollapsibleServiceCard(
            title = t(lang, "merge_service_gap_whoosh"),
            expanded = expandGapWhoosh,
            enabled = params.gapWhooshEnabled,
            accent = StudioColors.Amber,
            tagPrefix = "merge_service_gap_whoosh",
            onToggleExpand = { expandGapWhoosh = !expandGapWhoosh },
            onToggleEnabled = { on ->
                if (on) expandGapWhoosh = true
                updateParams { it.copy(gapWhooshEnabled = on) }
            },
        ) {
            StudioParamSlider(
                label = if (lang == AppLanguage.EN) "Transition Gap Duration" else "مدة الفجوة الزمنية بين المقاطع",
                value = params.transitionGapSec,
                min = 0f,
                max = 10f,
                formattedValue = "${"%.2f".format(params.transitionGapSec)} s",
                accentColor = StudioColors.Amber,
                testTag = "merge_gap_duration_slider",
                onValueChange = { v -> updateParams(300L) { it.copy(transitionGapSec = v, gapWhooshEnabled = true) } },
            )
            StudioParamSlider(
                label = if (lang == AppLanguage.EN) "Whoosh Transition Intensity" else "شدة مؤثر الانتقال Whoosh",
                value = params.whooshIntensityPct,
                min = 0f,
                max = 100f,
                formattedValue = "${params.whooshIntensityPct.roundToInt()}%",
                accentColor = StudioColors.Amber,
                testTag = "merge_whoosh_intensity_slider",
                onValueChange = { v -> updateParams(300L) { it.copy(whooshIntensityPct = v, gapWhooshEnabled = true) } },
            )
        }

        // Actions Card: Preview Edit FIRST, then Apply Now, then Export
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
                uiState.exportProgress?.let { p ->
                    LinearProgressIndicator(
                        progress = { p.coerceIn(0f, 1f) },
                        color = StudioColors.Cyan,
                        trackColor = StudioColors.BgTop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                    )
                }

                MergeStudioPreviewApplyRow(
                    isToolPreviewPlaying = uiState.isToolPreviewPlaying,
                    playbackFlow = viewModel.playbackTelemetry,
                    canProcess = canProcess,
                    isBusy = uiState.isBusy,
                    language = lang,
                    onPreviewClick = { viewModel.previewUnifiedMergeOverlay(params) },
                    onApplyClick = {
                        preferVmUndo = true
                        viewModel.applyUnifiedMergeOverlay(params)
                    },
                )

                val exportInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = { exportDialogOpen = true },
                    enabled = hasPrimary && !uiState.isBusy,
                    interactionSource = exportInteraction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioColors.Emerald,
                        contentColor = StudioColors.BgTop,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .lit(exportInteraction, StudioColors.Emerald, cornerRadius = 14.dp)
                        .testTag("merge_studio_export_button"),
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(t(lang, "save_export_final"), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    ToolInfoDialog(
        open = infoDialogOpen,
        title = t(lang, "merge_studio_title"),
        description = t(lang, "merge_studio_subtitle"),
        language = lang,
        onDismiss = { infoDialogOpen = false },
    )

    ExportFormatDialog(
        open = exportDialogOpen,
        language = lang,
        initialFileName = "${uiState.activeProjectName.ifBlank { "Sonix" }}_Merged",
        onDismiss = { exportDialogOpen = false },
        onSaveToDevice = { fmt, name -> viewModel.exportUnifiedMergeOverlay(params, fmt, name, shareAfter = false) },
        onShareAudio = { fmt, name -> viewModel.exportUnifiedMergeOverlay(params, fmt, name, shareAfter = true) },
        onShareToPackage = { fmt, name, pkg -> viewModel.exportUnifiedMergeOverlay(params, fmt, name, shareAfter = true, targetPackage = pkg) },
        onSaveForUnity = { name -> viewModel.exportAudioForUnity(name, shareAfter = false) },
        onShareForUnity = { name -> viewModel.exportAudioForUnity(name, shareAfter = true) },
    )
}

@Composable
private fun CollapsibleServiceCard(
    title: String,
    expanded: Boolean,
    enabled: Boolean,
    accent: Color,
    tagPrefix: String,
    onToggleExpand: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
        border = BorderStroke(1.dp, if (enabled) accent.copy(alpha = 0.6f) else StudioColors.Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    onClick = onToggleExpand,
                    color = Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("${tagPrefix}_expand"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = if (enabled) accent else StudioColors.TextMuted,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (enabled) accent else StudioColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = StudioColors.TextSecondary,
                        )
                    }
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = StudioColors.BgTop,
                        checkedTrackColor = accent,
                    ),
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("${tagPrefix}_switch"),
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun DualTrackInteractiveTimelineCard(
    language: AppLanguage,
    primaryName: String,
    primaryDurMs: Long,
    primaryPeaks: List<Float>,
    secondaryName: String,
    secondaryDurMs: Long,
    secondaryPeaks: List<Float>,
    hasPrimary: Boolean,
    hasSecondary: Boolean,
    params: MergeStudioParams,
    playbackFlow: StateFlow<PlaybackTelemetry>,
    onSeekFraction: (Float) -> Unit,
    onUpdateParams: (Long, (MergeStudioParams) -> MergeStudioParams) -> Unit,
) {
    val layout = remember(primaryDurMs, secondaryDurMs, params) {
        computeMergeTimelineLayout(primaryDurMs, secondaryDurMs, params)
    }
    val computedSpanMs = remember(layout) { max(layout.totalDurationMs, 2000L) }
    var draggingTrack by remember { mutableIntStateOf(0) } // 0 = none, 1 = primary, 2 = secondary
    var dragBadgeMs by remember { mutableStateOf<Long?>(null) }
    var dragScaleMs by remember { mutableLongStateOf(computedSpanMs) }
    val spanMs = if (draggingTrack == 0) computedSpanMs else dragScaleMs

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
        border = BorderStroke(1.dp, StudioColors.Cyan.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("merge_dual_track_timeline_card"),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Layers,
                        contentDescription = null,
                        tint = StudioColors.Cyan,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = if (language == AppLanguage.EN) "Dual-Track Timeline" else "المساران الزمنيان التفاعليان",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = StudioColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StudioColors.BgTop,
                        border = BorderStroke(1.dp, StudioColors.Cyan.copy(alpha = 0.45f)),
                    ) {
                        Text(
                            text = formatDurationMs(layout.totalDurationMs),
                            style = TelemetryTextStyle,
                            color = StudioColors.Cyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    if (params.primaryStartOffsetSec > 0.05f || params.overlayStartOffsetSec > 0.05f) {
                        IconButton(
                            onClick = {
                                onUpdateParams(0L) {
                                    it.copy(
                                        primaryStartOffsetSec = 0f,
                                        overlayStartOffsetSec = 0f,
                                        transitionGapSec = 0f,
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("merge_timeline_reset_offsets"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = if (language == AppLanguage.EN) "Reset track offsets" else "إعادة ضبط مواضع المسارات",
                                tint = StudioColors.TextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            Text(
                text = if (language == AppLanguage.EN) {
                    "Drag either track horizontally to offset start time • Tap ruler to seek"
                } else {
                    "اسحب أي مسار أفقياً لتعديل نقطة البداية • اضغط على المسطرة للتمرير الفوري"
                },
                style = MaterialTheme.typography.labelSmall,
                color = StudioColors.TextSecondary,
            )

            dragBadgeMs?.let { liveMs ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (draggingTrack == 1) StudioColors.Cyan else StudioColors.Amber,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    val trackLabel = if (draggingTrack == 1) {
                        if (language == AppLanguage.EN) "Track 1 Start" else "بداية المسار الأول"
                    } else {
                        when (params.mode) {
                            MergeStudioMode.CROSSFADE -> if (language == AppLanguage.EN) "Crossfade" else "مدة التداخل"
                            else -> if (language == AppLanguage.EN) "Track 2 Start" else "بداية المسار الثاني"
                        }
                    }
                    Text(
                        text = "$trackLabel: ${formatDurationMs(liveMs)}",
                        style = TelemetryTextStyle.copy(fontWeight = FontWeight.Bold),
                        color = StudioColors.BgTop,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            // Force LTR coordinate axis so audio timeline always advances left -> right cleanly
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(StudioColors.BgTop)
                        .border(1.dp, StudioColors.Border, RoundedCornerShape(14.dp))
                        .padding(vertical = 8.dp),
                ) {
                    val laneWidthDp = maxWidth
                    val laneWidthPx = with(LocalDensity.current) { laneWidthDp.toPx().coerceAtLeast(1f) }
                    val latestParams by rememberUpdatedState(params)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Interactive Time Ruler
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .padding(horizontal = 8.dp)
                                .pointerInput(spanMs) {
                                    detectTapGestures { offset ->
                                        val frac = (offset.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                                        onSeekFraction(frac)
                                    }
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("00:00", style = TelemetryTextStyle, color = StudioColors.TextMuted)
                            Text(
                                text = formatDurationMs(spanMs / 2L),
                                style = TelemetryTextStyle,
                                color = StudioColors.TextMuted,
                            )
                            Text(
                                text = formatDurationMs(spanMs),
                                style = TelemetryTextStyle,
                                color = StudioColors.Cyan,
                            )
                        }

                        // Top Track: Primary Audio (Cyan)
                        TimelineTrackLane(
                            label = primaryName,
                            durationMs = primaryDurMs,
                            startMs = layout.primaryStartMs,
                            spanMs = spanMs,
                            peaks = primaryPeaks,
                            accent = StudioColors.Cyan,
                            enabled = hasPrimary,
                            isDragging = draggingTrack == 1,
                            laneWidthPx = laneWidthPx,
                            tag = "merge_timeline_track_primary",
                            onDragDeltaPx = { dx ->
                                if (draggingTrack == 0) dragScaleMs = spanMs
                                draggingTrack = 1
                                val dSec = (dx / laneWidthPx) * (dragScaleMs / 1000f)
                                val nextSec = (latestParams.primaryStartOffsetSec + dSec).coerceIn(0f, 60f)
                                dragBadgeMs = (nextSec * 1000f).roundToLong()
                                onUpdateParams(120L) { it.copy(primaryStartOffsetSec = nextSec) }
                            },
                            onDragEnd = {
                                draggingTrack = 0
                                dragBadgeMs = null
                            },
                        )

                        // Bottom Track: Secondary Audio (Amber)
                        TimelineTrackLane(
                            label = secondaryName,
                            durationMs = secondaryDurMs,
                            startMs = layout.secondaryStartMs,
                            spanMs = spanMs,
                            peaks = secondaryPeaks,
                            accent = StudioColors.Amber,
                            enabled = hasSecondary,
                            isDragging = draggingTrack == 2,
                            laneWidthPx = laneWidthPx,
                            tag = "merge_timeline_track_secondary",
                            onDragDeltaPx = { dx ->
                                if (draggingTrack == 0) dragScaleMs = spanMs
                                draggingTrack = 2
                                val dSec = (dx / laneWidthPx) * (dragScaleMs / 1000f)
                                when (latestParams.mode) {
                                    MergeStudioMode.OVERLAY -> {
                                        val nextSec = (latestParams.overlayStartOffsetSec + dSec).coerceIn(0f, 60f)
                                        dragBadgeMs = (nextSec * 1000f).roundToLong()
                                        onUpdateParams(120L) { it.copy(overlayStartOffsetSec = nextSec) }
                                    }
                                    MergeStudioMode.SEQUENTIAL -> {
                                        val nextGap = (latestParams.transitionGapSec + dSec).coerceIn(0f, 10f)
                                        val nextStartMs = latestParams.primaryStartOffsetSec * 1000f + primaryDurMs + nextGap * 1000f
                                        dragBadgeMs = nextStartMs.roundToLong()
                                        onUpdateParams(120L) { it.copy(transitionGapSec = nextGap) }
                                    }
                                    MergeStudioMode.CROSSFADE -> {
                                        // Dragging left increases overlap (crossfadeSec), dragging right decreases overlap
                                        val nextXf = (latestParams.crossfadeSec - dSec).coerceIn(0.1f, 15f)
                                        dragBadgeMs = (nextXf * 1000f).roundToLong()
                                        onUpdateParams(120L) { it.copy(crossfadeSec = nextXf) }
                                    }
                                }
                            },
                            onDragEnd = {
                                draggingTrack = 0
                                dragBadgeMs = null
                            },
                        )
                    }

                    // Crossfade overlap visual highlight when active
                    if (params.mode == MergeStudioMode.CROSSFADE && layout.crossfadeActualMs > 0L) {
                        val xfStartFrac = (layout.secondaryStartMs.toFloat() / spanMs.toFloat()).coerceIn(0f, 1f)
                        val xfWidthFrac = (layout.crossfadeActualMs.toFloat() / spanMs.toFloat()).coerceIn(0.01f, 1f - xfStartFrac)
                        Canvas(modifier = Modifier.matchParentSize()) {
                            drawRect(
                                color = StudioColors.Emerald.copy(alpha = 0.16f),
                                topLeft = Offset(xfStartFrac * size.width, 28.dp.toPx()),
                                size = Size(xfWidthFrac * size.width, size.height - 32.dp.toPx()),
                            )
                        }
                    }

                    // Continuous vertical synchronized 60fps Playhead spanning both tracks
                    TimelinePlayheadOverlay(
                        playbackFlow = playbackFlow,
                        spanMs = spanMs,
                        laneWidthPx = laneWidthPx,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelinePlayheadOverlay(
    playbackFlow: StateFlow<PlaybackTelemetry>,
    spanMs: Long,
    laneWidthPx: Float,
    modifier: Modifier = Modifier,
) {
    val playbackState by playbackFlow.collectAsStateWithLifecycle()
    var smoothPosMs by remember { mutableLongStateOf(playbackState.positionMs) }

    LaunchedEffect(playbackState.positionMs, playbackState.isPlaying, spanMs) {
        smoothPosMs = playbackState.positionMs
        if (!playbackState.isPlaying) return@LaunchedEffect
        val anchor = playbackState.positionMs
        val startNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { nowNanos ->
                val elapsed = ((nowNanos - startNanos) / 1_000_000L).coerceAtLeast(0L)
                smoothPosMs = if (spanMs > 0L) (anchor + elapsed).coerceIn(0L, spanMs) else anchor + elapsed
            }
        }
    }

    val density = LocalDensity.current
    val indicatorWidthDp = 12.dp
    val halfIndicatorPx = with(density) { (indicatorWidthDp / 2f).toPx() }
    val safeSpan = spanMs.coerceAtLeast(1L)
    val playheadFrac = (smoothPosMs.toFloat() / safeSpan.toFloat()).coerceIn(0f, 1f)
    val px = (playheadFrac * laneWidthPx).coerceIn(2f, (laneWidthPx - 2f).coerceAtLeast(2f))

    Box(modifier = modifier, contentAlignment = Alignment.TopStart) {
        Canvas(
            modifier = Modifier
                .width(indicatorWidthDp)
                .fillMaxHeight()
                .graphicsLayer {
                    translationX = px - halfIndicatorPx
                },
        ) {
            val cx = size.width / 2f
            drawLine(
                color = StudioColors.TextPrimary,
                start = Offset(cx, 0f),
                end = Offset(cx, size.height),
                strokeWidth = 2.2.dp.toPx(),
            )
            drawCircle(
                color = StudioColors.Cyan,
                radius = 5.dp.toPx(),
                center = Offset(cx, 12.dp.toPx()),
            )
        }
    }
}

@Composable
private fun MergeStudioPreviewApplyRow(
    isToolPreviewPlaying: Boolean,
    playbackFlow: StateFlow<PlaybackTelemetry>,
    canProcess: Boolean,
    isBusy: Boolean,
    language: AppLanguage,
    onPreviewClick: () -> Unit,
    onApplyClick: () -> Unit,
) {
    val playback by playbackFlow.collectAsStateWithLifecycle()
    val isPreviewActive = isToolPreviewPlaying || (canProcess && playback.isPlaying)
    PreviewAndApplyActionRow(
        isPreviewPlaying = isPreviewActive,
        previewEnabled = canProcess && !isBusy,
        applyEnabled = canProcess && !isBusy,
        language = language,
        previewTag = "merge_studio_preview_button",
        applyTag = "merge_studio_apply_button",
        applyLabel = t(language, "apply_now"),
        applyColor = StudioColors.Violet,
        onPreviewClick = onPreviewClick,
        onApplyClick = onApplyClick,
    )
}

@Composable
private fun TimelineTrackLane(
    label: String,
    durationMs: Long,
    startMs: Long,
    spanMs: Long,
    peaks: List<Float>,
    accent: Color,
    enabled: Boolean,
    isDragging: Boolean,
    laneWidthPx: Float,
    tag: String,
    onDragDeltaPx: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val latestOnDragDelta by rememberUpdatedState(onDragDeltaPx)
    val latestOnDragEnd by rememberUpdatedState(onDragEnd)
    val startFrac = (startMs.toFloat() / spanMs.toFloat()).coerceIn(0f, 1f)
    val durFrac = if (enabled && durationMs > 0L) {
        val remainingFrac = (1f - startFrac).coerceAtLeast(0.001f)
        (durationMs.toFloat() / spanMs.toFloat()).coerceIn(minOf(0.08f, remainingFrac), remainingFrac)
    } else {
        0.30f
    }
    val offsetPx = (startFrac * laneWidthPx).roundToInt()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .background(StudioColors.ElevatedSurface.copy(alpha = 0.45f)),
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = accent.copy(alpha = if (isDragging) 0.30f else 0.18f),
            border = BorderStroke(
                width = if (isDragging) 2.dp else 1.2.dp,
                color = if (enabled) accent else StudioColors.Border,
            ),
            modifier = Modifier
                .offset { IntOffset(offsetPx, 0) }
                .fillMaxWidth(durFrac)
                .fillMaxHeight()
                .padding(vertical = 3.dp)
                .then(
                    if (enabled) {
                        Modifier.pointerInput(tag) {
                            detectHorizontalDragGestures(
                                onDragEnd = { latestOnDragEnd() },
                                onDragCancel = { latestOnDragEnd() },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    latestOnDragDelta(dragAmount)
                                },
                            )
                        }
                    } else {
                        Modifier
                    },
                )
                .testTag(tag),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Mini live waveform inside clip block
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 14.dp),
                ) {
                    val bars = if (peaks.isNotEmpty()) peaks else List(24) { 0.35f }
                    val step = size.width / bars.size.coerceAtLeast(1)
                    val midY = size.height * 0.5f
                    bars.forEachIndexed { idx, amp ->
                        val h = max(3f, amp.coerceIn(0.08f, 1f) * size.height * 0.9f)
                        drawRoundRect(
                            color = accent.copy(alpha = 0.52f),
                            topLeft = Offset(idx * step + 1f, midY - h * 0.5f),
                            size = Size(max(2f, step * 0.6f), h),
                            cornerRadius = CornerRadius(2f, 2f),
                        )
                    }
                }

                // Header pill inside clip: name + duration + start badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DragIndicator,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = StudioColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = "+${formatDurationMs(startMs)} (${formatDurationMs(durationMs)})",
                        style = TelemetryTextStyle,
                        color = accent,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
