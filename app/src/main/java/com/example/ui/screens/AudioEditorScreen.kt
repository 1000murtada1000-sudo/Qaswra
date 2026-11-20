package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AudioSignalAnalyzer
import com.example.audio.formatDurationCentisMs
import com.example.audio.formatDurationMs
import com.example.i18n.t
import com.example.model.AppLanguage
import com.example.model.AudioPickerTarget
import com.example.model.PlaybackTelemetry
import com.example.studio.ui.lit
import com.example.ui.components.ExportFormatDialog
import com.example.ui.components.PreviewAndApplyActionRow
import com.example.ui.components.SaveDraftDialog
import com.example.ui.components.StudioFocusTopBar
import com.example.ui.components.StudioParamSlider
import com.example.ui.theme.StudioColors
import com.example.ui.theme.TelemetryTextStyle
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun AudioEditorScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onOpenPicker: (AudioPickerTarget) -> Unit,
    onBack: () -> Unit,
) {
    val lang = uiState.language
    val durationMs = uiState.durationMs.coerceAtLeast(0L)
    val startMs = (durationMs * uiState.trimStart).roundToInt().toLong()
    val endMs = (durationMs * uiState.trimEnd).roundToInt().toLong()
    val selLenMs = (endMs - startMs).coerceAtLeast(0L)

    var zoomLevel by remember { mutableFloatStateOf(1.0f) }
    var panFraction by remember { mutableFloatStateOf(0f) }
    var autoFadeEdges by remember { mutableStateOf(true) }
    var fadeInSec by remember { mutableFloatStateOf(0.0f) }
    var fadeOutSec by remember { mutableFloatStateOf(0.0f) }
    var mainMixGain by remember { mutableFloatStateOf(1.0f) }
    var bgMixGain by remember { mutableFloatStateOf(0.45f) }

    var exportDialogOpen by remember { mutableStateOf(false) }
    var saveDraftDialogOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.beginClipGainSession()
        viewModel.setEditorPlaybackBoundsEnabled(true)
    }

    BackHandler {
        viewModel.discardUnapprovedClipGainSession()
        onBack()
    }

    LaunchedEffect(uiState.restoredToolSession?.id) {
        val restored = viewModel.consumeRestoredToolSession("editor")
        if (restored != null) {
            val fp = restored.floatParams
            zoomLevel = (fp["zoomLevel"] ?: 1.0f).coerceIn(1f, 15f)
            panFraction = panFraction.coerceIn(0f, (1f - 1f / zoomLevel).coerceAtLeast(0f))
            autoFadeEdges = (fp["autoFadeEdges"] ?: 1f) >= 0.5f
            fadeInSec = (fp["fadeInSec"] ?: 0f).coerceIn(0f, 10f)
            fadeOutSec = (fp["fadeOutSec"] ?: 0f).coerceIn(0f, 10f)
            mainMixGain = (fp["mainMixGain"] ?: 1f).coerceIn(0f, 1.5f)
            bgMixGain = (fp["bgMixGain"] ?: 0.45f).coerceIn(0f, 1.5f)
            val savedStart = fp["trimStart"]
            val savedEnd = fp["trimEnd"]
            if (savedStart != null && savedEnd != null) {
                viewModel.updateTrimRange(savedStart, savedEnd)
            }
        }
    }

    val latestState by rememberUpdatedState(uiState)
    val latestZoom by rememberUpdatedState(zoomLevel)
    val latestAutoFade by rememberUpdatedState(autoFadeEdges)
    val latestFadeIn by rememberUpdatedState(fadeInSec)
    val latestFadeOut by rememberUpdatedState(fadeOutSec)
    val latestMainMix by rememberUpdatedState(mainMixGain)
    val latestBgMix by rememberUpdatedState(bgMixGain)

    DisposableEffect(Unit) {
        onDispose {
            viewModel.setEditorPlaybackBoundsEnabled(false)
            viewModel.stopToolPreview()
            viewModel.discardUnapprovedClipGainSession()
            val cur = latestState
            if (cur.hasAudio) {
                val curDur = cur.durationMs.coerceAtLeast(0L)
                val sMs = (curDur * cur.trimStart).roundToInt().toLong()
                val eMs = (curDur * cur.trimEnd).roundToInt().toLong()
                viewModel.saveToolSessionToCache(
                    toolId = "editor",
                    titleAr = "محرر القص والدمج",
                    titleEn = "Trim & Editor",
                    summaryAr = "${formatDurationCentisMs(sMs)} → ${formatDurationCentisMs(eMs)}",
                    summaryEn = "${formatDurationCentisMs(sMs)} → ${formatDurationCentisMs(eMs)}",
                    floatParams = mapOf(
                        "zoomLevel" to latestZoom,
                        "autoFadeEdges" to if (latestAutoFade) 1f else 0f,
                        "fadeInSec" to latestFadeIn,
                        "fadeOutSec" to latestFadeOut,
                        "mainMixGain" to latestMainMix,
                        "bgMixGain" to latestBgMix,
                        "trimStart" to cur.trimStart,
                        "trimEnd" to cur.trimEnd,
                    ),
                )
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .testTag("editor_lazy_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "editor_top_bar") {
                StudioFocusTopBar(
                    title = t(lang, "section_editor_title"),
                    subtitle = "${formatDurationCentisMs(startMs)} → ${formatDurationCentisMs(endMs)} (${formatDurationCentisMs(selLenMs)})",
                    language = lang,
                    onBack = {
                        viewModel.discardUnapprovedClipGainSession()
                        onBack()
                    },
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val undoInteraction = remember { MutableInteractionSource() }
                            IconButton(
                                onClick = { viewModel.undoLastEdit() },
                                enabled = uiState.canUndoEdit,
                                interactionSource = undoInteraction,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = StudioColors.ElevatedSurface,
                                    contentColor = StudioColors.Cyan,
                                    disabledContentColor = StudioColors.TextMuted.copy(alpha = 0.4f),
                                ),
                                modifier = Modifier
                                    .size(34.dp)
                                    .lit(undoInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 17.dp)
                                    .testTag("editor_undo_button"),
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = t(lang, "undo_edit"), modifier = Modifier.size(18.dp))
                            }
                            val redoInteraction = remember { MutableInteractionSource() }
                            IconButton(
                                onClick = { viewModel.redoLastEdit() },
                                enabled = uiState.canRedoEdit,
                                interactionSource = redoInteraction,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = StudioColors.ElevatedSurface,
                                    contentColor = StudioColors.Cyan,
                                    disabledContentColor = StudioColors.TextMuted.copy(alpha = 0.4f),
                                ),
                                modifier = Modifier
                                    .size(34.dp)
                                    .lit(redoInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 17.dp)
                                    .testTag("editor_redo_button"),
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Redo, contentDescription = t(lang, "redo_edit"), modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                )
            }

            uiState.importProgress?.let { prog ->
                item(key = "editor_import_progress") {
                    val byteTransferActive = !uiState.importBytesLabel.isNullOrBlank()
                    val byteFraction = uiState.importBytesProgress
                    val pct = (prog.coerceIn(0f, 1f) * 100f).toInt()
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = StudioColors.CardSurface,
                        border = BorderStroke(1.dp, StudioColors.Cyan.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val phaseText = if (prog >= 0.50f && !uiState.importLabel.isNullOrBlank()) {
                                    uiState.importLabel
                                } else {
                                    t(lang, "uploading_to_cache_label")
                                }
                                Text(
                                    text = if (byteTransferActive && byteFraction == null) {
                                        phaseText.orEmpty()
                                    } else {
                                        "$phaseText $pct%"
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
                                        .testTag("editor_import_progress_bar"),
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
                                        .testTag("editor_import_progress_bar"),
                                )
                            }
                        }
                    }
                }
            }

            item(key = "editor_waveform_card") {
        Card(
            shape = RoundedCornerShape(22.dp),
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
                    val loopChipInteraction = remember { MutableInteractionSource() }
                    FilterChip(
                        selected = uiState.loopSelection,
                        onClick = { viewModel.toggleLoopSelection() },
                        interactionSource = loopChipInteraction,
                        leadingIcon = {
                            Icon(Icons.Rounded.Repeat, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(t(lang, "loop_selection")) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioColors.Emerald.copy(alpha = 0.2f),
                            selectedLabelColor = StudioColors.Emerald,
                        ),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(loopChipInteraction, MaterialTheme.colorScheme.primary, selected = uiState.loopSelection, cornerRadius = 10.dp)
                            .testTag("editor_loop_selection_chip"),
                    )

                    val savePermInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = { viewModel.saveEditsPermanent() },
                        enabled = uiState.hasAudio && !uiState.isBusy && !uiState.isProcessing,
                        interactionSource = savePermInteraction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioColors.ElevatedSurface,
                            contentColor = StudioColors.Cyan,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(savePermInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 12.dp)
                            .testTag("editor_save_permanent_button"),
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(t(lang, "save_changes"))
                    }
                }

                // Enforce consistent Left-to-Right timeline axis across Zoom, Waveform, Playhead, and Start/End boxes
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Zoom Bar (1.0x .. 15.0x)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            IconButton(
                                onClick = {
                                    val nextZoom = (zoomLevel - 0.5f).coerceAtLeast(1f)
                                    val oldSpan = 1f / zoomLevel
                                    val newSpan = 1f / nextZoom
                                    val center = panFraction + oldSpan * 0.5f
                                    zoomLevel = nextZoom
                                    panFraction = (center - newSpan * 0.5f).coerceIn(0f, (1f - newSpan).coerceAtLeast(0f))
                                },
                                enabled = zoomLevel > 1f,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("editor_zoom_out_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ZoomOut,
                                    contentDescription = t(lang, "editor_zoom_label"),
                                    tint = StudioColors.Cyan,
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "${"%.1f".format(Locale.US, zoomLevel)}x",
                                        style = TelemetryTextStyle.copy(fontSize = 11.sp),
                                        color = StudioColors.Cyan,
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = t(lang, "editor_zoom_label"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioColors.TextSecondary,
                                        maxLines = 1,
                                    )
                                }
                                Slider(
                                    value = zoomLevel,
                                    onValueChange = { next ->
                                        val clamped = next.coerceIn(1f, 15f)
                                        val oldSpan = 1f / zoomLevel
                                        val newSpan = 1f / clamped
                                        val center = panFraction + oldSpan * 0.5f
                                        zoomLevel = clamped
                                        panFraction = (center - newSpan * 0.5f).coerceIn(0f, (1f - newSpan).coerceAtLeast(0f))
                                    },
                                    valueRange = 1f..15f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = StudioColors.Cyan,
                                        activeTrackColor = StudioColors.Cyan,
                                        inactiveTrackColor = StudioColors.BgTop,
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("editor_zoom_slider"),
                                )
                            }
                            IconButton(
                                onClick = {
                                    val nextZoom = (zoomLevel + 0.5f).coerceAtMost(15f)
                                    val oldSpan = 1f / zoomLevel
                                    val newSpan = 1f / nextZoom
                                    val center = panFraction + oldSpan * 0.5f
                                    zoomLevel = nextZoom
                                    panFraction = (center - newSpan * 0.5f).coerceIn(0f, (1f - newSpan).coerceAtLeast(0f))
                                },
                                enabled = zoomLevel < 15f,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("editor_zoom_in_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ZoomIn,
                                    contentDescription = t(lang, "editor_zoom_label"),
                                    tint = StudioColors.Cyan,
                                )
                            }
                        }

                        // Interactive Pinch-to-Zoom + Horizontal Pan + Viewport-Culled Waveform Canvas
                        val density = LocalDensity.current
                        val edgePadPx = remember(density) { with(density) { 10.dp.toPx() } }
                        val handleRadiusPx = remember(density) { with(density) { 8.dp.toPx() } }
                        val handleHitRadiusPx = remember(density) { with(density) { 28.dp.toPx() } }
                        val lineHitRadiusPx = remember(density) { with(density) { 18.dp.toPx() } }
                        val topHandleYPx = remember(density) { with(density) { 14.dp.toPx() } }
                        val latestPan by rememberUpdatedState(panFraction)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(154.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(StudioColors.BgTop)
                                .border(1.dp, StudioColors.BorderBright.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .pointerInput(uiState.hasAudio) {
                                    if (!uiState.hasAudio) return@pointerInput
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val downPos = down.position
                                        val w = size.width.toFloat().coerceAtLeast(1f)
                                        val h = size.height.toFloat().coerceAtLeast(1f)
                                        val usableW = (w - 2f * edgePadPx).coerceAtLeast(1f)

                                        val curZoom = latestZoom.coerceIn(1f, 15f)
                                        val curSpan = 1f / curZoom
                                        val curStart = latestPan.coerceIn(0f, (1f - curSpan).coerceAtLeast(0f))

                                        fun fracToPx(f: Float): Float =
                                            edgePadPx + ((f - curStart) / curSpan) * usableW

                                        fun pxToFrac(x: Float): Float {
                                            val z = latestZoom.coerceIn(1f, 15f)
                                            val s = 1f / z
                                            val p = latestPan.coerceIn(0f, (1f - s).coerceAtLeast(0f))
                                            return (p + ((x - edgePadPx) / usableW) * s).coerceIn(0f, 1f)
                                        }

                                        val st0 = viewModel.uiState.value
                                        val sx0 = fracToPx(st0.trimStart)
                                        val ex0 = fracToPx(st0.trimEnd)
                                        val distStart = (downPos - Offset(sx0, topHandleYPx)).getDistance()
                                        val distEnd = (downPos - Offset(ex0, h - topHandleYPx)).getDistance()
                                        val dxStart = abs(downPos.x - sx0)
                                        val dxEnd = abs(downPos.x - ex0)

                                        // mode: 0 = UNDECIDED, 1 = HANDLE_START, 2 = HANDLE_END, 3 = PAN, 4 = SCRUB, 5 = PINCH
                                        var gestureMode = when {
                                            distStart <= handleHitRadiusPx && distStart <= distEnd -> 1
                                            distEnd <= handleHitRadiusPx -> 2
                                            dxStart <= lineHitRadiusPx && dxStart <= dxEnd -> 1
                                            dxEnd <= lineHitRadiusPx -> 2
                                            else -> 0
                                        }
                                        var movedPastSlop = false

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val pressed = event.changes.filter { it.pressed }
                                            if (pressed.isEmpty()) {
                                                if (!movedPastSlop && gestureMode == 0) {
                                                    viewModel.seekToFraction(pxToFrac(downPos.x))
                                                }
                                                break
                                            }
                                            if (pressed.size >= 2) {
                                                gestureMode = 5
                                                movedPastSlop = true
                                                val zFactor = event.calculateZoom()
                                                val panVec = event.calculatePan()
                                                val centroid = event.calculateCentroid(useCurrent = true)
                                                val oldZ = latestZoom.coerceIn(1f, 15f)
                                                val oldSpan = 1f / oldZ
                                                val oldPan = latestPan.coerceIn(0f, (1f - oldSpan).coerceAtLeast(0f))
                                                val centroidNormX = ((centroid.x - edgePadPx) / usableW).coerceIn(0f, 1f)
                                                val anchorFrac = (oldPan + centroidNormX * oldSpan).coerceIn(0f, 1f)

                                                val newZ = (oldZ * zFactor).coerceIn(1f, 15f)
                                                val newSpan = 1f / newZ
                                                val maxP = (1f - newSpan).coerceAtLeast(0f)
                                                var nextPan = anchorFrac - centroidNormX * newSpan
                                                nextPan -= (panVec.x / usableW) * newSpan
                                                zoomLevel = newZ
                                                panFraction = nextPan.coerceIn(0f, maxP)
                                                event.changes.forEach { it.consume() }
                                                continue
                                            }

                                            val change = pressed.first()
                                            if (gestureMode == 0) {
                                                val dx = change.position.x - downPos.x
                                                val dy = change.position.y - downPos.y
                                                if (hypot(dx, dy) > viewConfiguration.touchSlop) {
                                                    movedPastSlop = true
                                                    if (abs(dy) > abs(dx) * 1.15f) {
                                                        // Yield vertical scrolling to LazyColumn cleanly
                                                        break
                                                    }
                                                    gestureMode = if (latestZoom > 1.02f) 3 else 4
                                                }
                                            }

                                            when (gestureMode) {
                                                1 -> {
                                                    change.consume()
                                                    val st = viewModel.uiState.value
                                                    viewModel.updateTrimRange(pxToFrac(change.position.x), st.trimEnd)
                                                }
                                                2 -> {
                                                    change.consume()
                                                    val st = viewModel.uiState.value
                                                    viewModel.updateTrimRange(st.trimStart, pxToFrac(change.position.x))
                                                }
                                                3 -> {
                                                    change.consume()
                                                    val dx = change.position.x - change.previousPosition.x
                                                    val z = latestZoom.coerceIn(1f, 15f)
                                                    val span = 1f / z
                                                    val maxP = (1f - span).coerceAtLeast(0f)
                                                    panFraction = (latestPan - (dx / usableW) * span).coerceIn(0f, maxP)
                                                }
                                                4 -> {
                                                    change.consume()
                                                    viewModel.seekToFraction(pxToFrac(change.position.x))
                                                }
                                            }
                                        }
                                    }
                                }
                                .testTag("editor_waveform_canvas"),
                        ) {
                            val peaks = remember(uiState.waveformPeaks) {
                                if (uiState.waveformPeaks.isNotEmpty()) {
                                    uiState.waveformPeaks
                                } else {
                                    List(120) { 0.08f }
                                }
                            }

                            // Static Waveform + Trim Handles Canvas (never recomposes on 60fps playhead ticks)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val usableW = (size.width - 2f * edgePadPx).coerceAtLeast(1f)
                                val visSpan = 1f / zoomLevel.coerceIn(1f, 15f)
                                val visStart = panFraction.coerceIn(0f, (1f - visSpan).coerceAtLeast(0f))
                                val visEnd = (visStart + visSpan).coerceAtMost(1f)

                                fun fracToX(f: Float): Float =
                                    edgePadPx + ((f - visStart) / visSpan) * usableW

                                val sxRaw = fracToX(uiState.trimStart)
                                val exRaw = fracToX(uiState.trimEnd)
                                val sxClip = sxRaw.coerceIn(0f, size.width)
                                val exClip = exRaw.coerceIn(0f, size.width)

                                if (exClip > sxClip) {
                                    drawRect(
                                        color = StudioColors.Cyan.copy(alpha = 0.12f),
                                        topLeft = Offset(sxClip, 0f),
                                        size = Size((exClip - sxClip).coerceAtLeast(1f), size.height),
                                    )
                                }

                                // Subtle Silero VAD speech indicator strip along the bottom edge when analyzed
                                if (uiState.vadSpeechSegments.isNotEmpty() && durationMs > 0L) {
                                    val durF = durationMs.toFloat()
                                    val barH = 3.dp.toPx()
                                    val barY = size.height - barH
                                    for (seg in uiState.vadSpeechSegments) {
                                        val x0 = fracToX((seg.startMs / durF).coerceIn(0f, 1f)).coerceIn(0f, size.width)
                                        val x1 = fracToX((seg.endMs / durF).coerceIn(0f, 1f)).coerceIn(0f, size.width)
                                        if (x1 > x0 + 1f) {
                                            drawRect(
                                                color = StudioColors.Emerald.copy(alpha = 0.75f),
                                                topLeft = Offset(x0, barY),
                                                size = Size(x1 - x0, barH),
                                            )
                                        }
                                    }
                                }

                                // Viewport-culled & column-pooled waveform bars so peaks stay sharp at 1.0x and ultra-detailed at 15.0x
                                val count = peaks.size
                                val startIdx = (visStart * count).toInt().coerceIn(0, max(0, count - 1))
                                val endIdx = ceil((visEnd * count).toDouble()).toInt().coerceIn(startIdx + 1, count)
                                val visibleBars = (endIdx - startIdx).coerceAtLeast(1)
                                val maxCols = (usableW / 2.6.dp.toPx()).toInt().coerceIn(48, 220)
                                val cy = size.height / 2f

                                if (visibleBars <= maxCols) {
                                    val slotW = usableW / visibleBars
                                    val barW = (slotW * 0.62f).coerceIn(1.5.dp.toPx(), 6.dp.toPx())
                                    for (i in startIdx until endIdx) {
                                        val barFrac = (i + 0.5f) / count
                                        val inSelection = barFrac in uiState.trimStart..uiState.trimEnd
                                        val h = peaks[i].coerceIn(0.04f, 1f) * size.height * 0.86f
                                        val x = fracToX(barFrac) - barW / 2f
                                        drawRoundRect(
                                            color = if (inSelection) StudioColors.Cyan else StudioColors.WaveformInactive.copy(alpha = 0.5f),
                                            topLeft = Offset(x, cy - h / 2f),
                                            size = Size(barW, h),
                                            cornerRadius = CornerRadius(barW / 2f, barW / 2f),
                                        )
                                    }
                                } else {
                                    val slotW = usableW / maxCols
                                    val barW = (slotW * 0.64f).coerceIn(1.5.dp.toPx(), 4.5.dp.toPx())
                                    for (c in 0 until maxCols) {
                                        val b0 = startIdx + ((c.toLong() * visibleBars) / maxCols).toInt()
                                        val b1 = (startIdx + (((c + 1).toLong() * visibleBars) / maxCols).toInt())
                                            .coerceIn(b0 + 1, endIdx)
                                        var peak = 0.04f
                                        for (bi in b0 until b1) {
                                            if (peaks[bi] > peak) peak = peaks[bi]
                                        }
                                        val barFrac = ((b0 + b1) * 0.5f) / count
                                        val inSelection = barFrac in uiState.trimStart..uiState.trimEnd
                                        val h = peak.coerceIn(0.04f, 1f) * size.height * 0.86f
                                        val x = fracToX(barFrac) - barW / 2f
                                        drawRoundRect(
                                            color = if (inSelection) StudioColors.Cyan else StudioColors.WaveformInactive.copy(alpha = 0.5f),
                                            topLeft = Offset(x, cy - h / 2f),
                                            size = Size(barW, h),
                                            cornerRadius = CornerRadius(barW / 2f, barW / 2f),
                                        )
                                    }
                                }

                                // StartHandle (Green — Left edge of selection)
                                if (sxRaw in -handleRadiusPx..(size.width + handleRadiusPx)) {
                                    val sxDraw = sxRaw.coerceIn(edgePadPx, (size.width - edgePadPx).coerceAtLeast(edgePadPx))
                                    drawLine(
                                        color = StudioColors.Emerald,
                                        start = Offset(sxDraw, 0f),
                                        end = Offset(sxDraw, size.height),
                                        strokeWidth = 2.5.dp.toPx(),
                                    )
                                    drawCircle(
                                        color = StudioColors.Emerald,
                                        radius = handleRadiusPx,
                                        center = Offset(sxDraw, topHandleYPx),
                                    )
                                    drawCircle(
                                        color = StudioColors.BgTop,
                                        radius = 2.5.dp.toPx(),
                                        center = Offset(sxDraw, topHandleYPx),
                                    )
                                }

                                // EndHandle (Red — Right edge of selection)
                                if (exRaw in -handleRadiusPx..(size.width + handleRadiusPx)) {
                                    val exDraw = exRaw.coerceIn(edgePadPx, (size.width - edgePadPx).coerceAtLeast(edgePadPx))
                                    drawLine(
                                        color = StudioColors.RoseBright,
                                        start = Offset(exDraw, 0f),
                                        end = Offset(exDraw, size.height),
                                        strokeWidth = 2.5.dp.toPx(),
                                    )
                                    drawCircle(
                                        color = StudioColors.RoseBright,
                                        radius = handleRadiusPx,
                                        center = Offset(exDraw, size.height - topHandleYPx),
                                    )
                                    drawCircle(
                                        color = StudioColors.BgTop,
                                        radius = 2.5.dp.toPx(),
                                        center = Offset(exDraw, size.height - topHandleYPx),
                                    )
                                }
                            }

                            // Isolated 60fps Playhead Cursor (uses withFrameNanos + graphicsLayer GPU translation)
                            EditorPlayheadOverlay(
                                playbackFlow = viewModel.playbackTelemetry,
                                durationMs = durationMs,
                                selectionStartMs = startMs,
                                selectionEndMs = endMs,
                                zoomLevel = zoomLevel,
                                panFraction = panFraction,
                                edgePadPx = edgePadPx,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        // Centisecond Readouts + (-0.1s / +0.1s) Nudge Controls aligned 1:1 with Left(Start) & Right(End) handles
                        val nudgeStepFrac = if (durationMs > 0L) (100f / durationMs.toFloat()).coerceAtLeast(0.0005f) else 0.01f
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StudioColors.BgMid,
                                border = BorderStroke(1.dp, StudioColors.Emerald.copy(alpha = 0.45f)),
                                modifier = Modifier.weight(1f),
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = if (lang == AppLanguage.EN) "Start" else "البداية",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioColors.Emerald.copy(alpha = 0.85f),
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = formatDurationCentisMs(startMs),
                                        style = TelemetryTextStyle.copy(fontWeight = FontWeight.Bold),
                                        color = StudioColors.Emerald,
                                        maxLines = 1,
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = { viewModel.updateTrimRange(uiState.trimStart - nudgeStepFrac, uiState.trimEnd) },
                                            enabled = uiState.hasAudio,
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .defaultMinSize(minHeight = 48.dp)
                                                .testTag("editor_start_minus_button"),
                                        ) {
                                            Text(
                                                text = "-0.1s",
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.updateTrimRange(uiState.trimStart + nudgeStepFrac, uiState.trimEnd) },
                                            enabled = uiState.hasAudio,
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .defaultMinSize(minHeight = 48.dp)
                                                .testTag("editor_start_plus_button"),
                                        ) {
                                            Text(
                                                text = "+0.1s",
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                            )
                                        }
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StudioColors.BgMid,
                                border = BorderStroke(1.dp, StudioColors.RoseBright.copy(alpha = 0.45f)),
                                modifier = Modifier.weight(1f),
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = if (lang == AppLanguage.EN) "End" else "النهاية",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioColors.RoseBright.copy(alpha = 0.85f),
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = formatDurationCentisMs(endMs),
                                        style = TelemetryTextStyle.copy(fontWeight = FontWeight.Bold),
                                        color = StudioColors.RoseBright,
                                        maxLines = 1,
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = { viewModel.updateTrimRange(uiState.trimStart, uiState.trimEnd - nudgeStepFrac) },
                                            enabled = uiState.hasAudio,
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .defaultMinSize(minHeight = 48.dp)
                                                .testTag("editor_end_minus_button"),
                                        ) {
                                            Text(
                                                text = "-0.1s",
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.updateTrimRange(uiState.trimStart, uiState.trimEnd + nudgeStepFrac) },
                                            enabled = uiState.hasAudio,
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .defaultMinSize(minHeight = 48.dp)
                                                .testTag("editor_end_plus_button"),
                                        ) {
                                            Text(
                                                text = "+0.1s",
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Auto-Fade 0.2s Click-Prevention Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = t(lang, "editor_auto_fade_toggle"),
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioColors.TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = autoFadeEdges,
                        onCheckedChange = { autoFadeEdges = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StudioColors.BgTop,
                            checkedTrackColor = StudioColors.Cyan,
                        ),
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_auto_fade_toggle"),
                    )
                }

                // 4 Trim / Cut / Select All / Normalize Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val trimInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = { viewModel.trimKeepSelection(autoFadeEdges) },
                        enabled = uiState.hasAudio && !uiState.isBusy && !uiState.isProcessing,
                        interactionSource = trimInteraction,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(trimInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 12.dp)
                            .testTag("editor_trim_keep_button"),
                    ) {
                        Icon(Icons.Rounded.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(t(lang, "trim_keep"), maxLines = 1)
                    }
                    val cutInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = { viewModel.cutSelectedRange(autoFadeEdges) },
                        enabled = uiState.hasAudio && !uiState.isBusy && !uiState.isProcessing,
                        interactionSource = cutInteraction,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(cutInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 12.dp)
                            .testTag("editor_cut_button"),
                    ) {
                        Icon(Icons.Rounded.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(t(lang, "cut_selection"), maxLines = 1)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val selectAllInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = { viewModel.resetSelection() },
                        enabled = uiState.hasAudio && !uiState.isBusy && !uiState.isProcessing,
                        interactionSource = selectAllInteraction,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(selectAllInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 12.dp)
                            .testTag("editor_select_all_button"),
                    ) {
                        Icon(Icons.Rounded.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(t(lang, "reset_selection"), maxLines = 1)
                    }
                    val normalizeInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = { viewModel.normalizeAudioBuffer() },
                        enabled = uiState.hasAudio && !uiState.isBusy && !uiState.isProcessing,
                        interactionSource = normalizeInteraction,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(normalizeInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 12.dp)
                            .testTag("editor_normalize_button"),
                    ) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(t(lang, "normalize_audio"), maxLines = 1)
                    }
                }

                // Silero VAD Speech Detection & Auto Silence Removal Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val vadDetectInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = { viewModel.detectSpeechBoundsWithSileroVad() },
                        enabled = uiState.hasAudio && !uiState.isBusy && !uiState.isProcessing,
                        interactionSource = vadDetectInteraction,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(vadDetectInteraction, StudioColors.Emerald, cornerRadius = 12.dp)
                            .testTag("editor_vad_detect_button"),
                    ) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = StudioColors.Emerald, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (lang == AppLanguage.EN) "Detect Speech (VAD)" else "تحديد الكلام (VAD)",
                            maxLines = 1,
                        )
                    }
                    val vadSilenceInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = { viewModel.removeUnwantedSilenceWithSileroVad() },
                        enabled = uiState.hasAudio && !uiState.isBusy && !uiState.isProcessing,
                        interactionSource = vadSilenceInteraction,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(vadSilenceInteraction, StudioColors.Amber, cornerRadius = 12.dp)
                            .testTag("editor_vad_silence_remove_button"),
                    ) {
                        Icon(Icons.Rounded.ContentCut, contentDescription = null, tint = StudioColors.Amber, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (lang == AppLanguage.EN) "Remove Silence (VAD)" else "إزالة الصمت (VAD)",
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        } // end item(key = "editor_waveform_card")

        item(key = "editor_clip_gain_card") {
        // Clip Gain Card (Instant real-time feedback + Math-based true clipping warning)
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
                    label = t(lang, "clip_volume"),
                    value = uiState.clipGainDb,
                    min = -24f,
                    max = 18f,
                    formattedValue = "${if (uiState.clipGainDb >= 0f) "+" else ""}${"%.1f".format(uiState.clipGainDb)} dB",
                    accentColor = StudioColors.Amber,
                    testTag = "editor_clip_gain_slider",
                    onValueChange = {
                        viewModel.setClipGainDb(it)
                        viewModel.ensureLivePreviewPlaying()
                    },
                )

                if (uiState.hasAudio && AudioSignalAnalyzer.willGainCauseClipping(uiState.originalPeakDbfs, uiState.clipGainDb)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StudioColors.Rose.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, StudioColors.RoseBright.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_clipping_warning"),
                    ) {
                        Text(
                            text = t(lang, "clipping_warning"),
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioColors.RoseBright,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }

                val applyClipGainInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = { viewModel.bakeClipGainDb(uiState.clipGainDb) },
                    enabled = uiState.hasAudio && abs(uiState.clipGainDb) > 0.05f && !uiState.isBusy && !uiState.isProcessing,
                    interactionSource = applyClipGainInteraction,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioColors.Amber, contentColor = StudioColors.BgTop),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .lit(applyClipGainInteraction, StudioColors.Amber, cornerRadius = 14.dp)
                        .testTag("editor_apply_clip_gain_button"),
                ) {
                    Text(
                        text = if (lang == AppLanguage.EN) "Apply Clip Gain" else "تطبيق مستوى الصوت على المقطع",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
        } // end item(key = "editor_clip_gain_card")

        item(key = "editor_fade_card") {
        // Fade In & Fade Out Card (clean 2 sliders + 1 unified Apply button — no duplicate preview/bottom buttons)
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
                    label = t(lang, "fade_in_title"),
                    value = fadeInSec,
                    min = 0.0f,
                    max = 10f,
                    formattedValue = "${"%.2f".format(fadeInSec)} s",
                    accentColor = StudioColors.Emerald,
                    testTag = "editor_fade_in_slider",
                    onValueChange = { fadeInSec = it },
                )
                StudioParamSlider(
                    label = t(lang, "fade_out_title"),
                    value = fadeOutSec,
                    min = 0.0f,
                    max = 10f,
                    formattedValue = "${"%.2f".format(fadeOutSec)} s",
                    accentColor = StudioColors.Violet,
                    testTag = "editor_fade_out_slider",
                    onValueChange = { fadeOutSec = it },
                )

                val applyFadeInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = { viewModel.applyFadeInOutSeconds(fadeInSec, fadeOutSec) },
                    enabled = uiState.hasAudio && (fadeInSec > 0.02f || fadeOutSec > 0.02f) && !uiState.isBusy && !uiState.isProcessing,
                    interactionSource = applyFadeInteraction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioColors.Emerald,
                        contentColor = StudioColors.BgTop,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .lit(applyFadeInteraction, StudioColors.Emerald, cornerRadius = 14.dp)
                        .testTag("editor_apply_fade_button"),
                ) {
                    Text(
                        text = if (lang == AppLanguage.EN) "Apply Fade In / Out" else "تطبيق التدرج (Fade)",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
        } // end item(key = "editor_fade_card")

        item(key = "editor_dual_mix_card") {
        // Dual-Track Mix Card
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Rounded.Layers, contentDescription = null, tint = StudioColors.Violet)
                        Text(
                            text = t(lang, "mix_tracks_title"),
                            style = MaterialTheme.typography.titleMedium,
                            color = StudioColors.TextPrimary,
                        )
                    }
                    if (uiState.mixBgName != null) {
                        val clearMixInteraction = remember { MutableInteractionSource() }
                        IconButton(
                            onClick = { viewModel.clearMixBackgroundTrack() },
                            interactionSource = clearMixInteraction,
                            modifier = Modifier
                                .size(48.dp)
                                .lit(clearMixInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 24.dp)
                                .testTag("editor_clear_mix_bg_button"),
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = t(lang, "delete"), tint = StudioColors.RoseBright)
                        }
                    }
                }

                val pickMixBgInteraction = remember { MutableInteractionSource() }
                OutlinedButton(
                    onClick = { onOpenPicker(AudioPickerTarget.MIX_BACKGROUND) },
                    interactionSource = pickMixBgInteraction,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .lit(pickMixBgInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 12.dp)
                        .testTag("editor_pick_mix_bg_button"),
                ) {
                    Text(
                        text = uiState.mixBgName?.let { "$it (${formatDurationMs(uiState.mixBgDurationMs)})" }
                            ?: if (lang == AppLanguage.EN) "Select Background Track" else "استيراد مسار الخلفية للمزج",
                    )
                }

                StudioParamSlider(
                    label = if (lang == AppLanguage.EN) "Main Track Level" else "مستوى الصوت الرئيسي",
                    value = mainMixGain,
                    min = 0f,
                    max = 1.5f,
                    formattedValue = "${(mainMixGain * 100f).roundToInt()}%",
                    accentColor = StudioColors.Cyan,
                    testTag = "editor_mix_main_gain",
                    onValueChange = { mainMixGain = it },
                )
                StudioParamSlider(
                    label = if (lang == AppLanguage.EN) "Background Track Level" else "مستوى مسار الخلفية",
                    value = bgMixGain,
                    min = 0f,
                    max = 1.5f,
                    formattedValue = "${(bgMixGain * 100f).roundToInt()}%",
                    accentColor = StudioColors.Violet,
                    testTag = "editor_mix_bg_gain",
                    onValueChange = { bgMixGain = it },
                )

                val applyMixInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = { viewModel.mixWithBackgroundTrack(mainMixGain, bgMixGain) },
                    enabled = uiState.hasAudio && uiState.mixBgName != null && !uiState.isBusy && !uiState.isProcessing,
                    interactionSource = applyMixInteraction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioColors.Violet,
                        contentColor = StudioColors.BgTop,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .lit(applyMixInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 12.dp)
                        .testTag("editor_apply_mix_button"),
                ) {
                    Text(if (lang == AppLanguage.EN) "Mix Tracks Together" else "دمج ومزج المسارين")
                }
            }
        }
        } // end item(key = "editor_dual_mix_card")

        item(key = "editor_export_card") {
        // Append File + Append Saved Projects + Export
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
                val appendFileInteraction = remember { MutableInteractionSource() }
                OutlinedButton(
                    onClick = { onOpenPicker(AudioPickerTarget.APPEND_CLIP) },
                    interactionSource = appendFileInteraction,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .lit(appendFileInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 12.dp)
                        .testTag("editor_append_file_button"),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(t(lang, "merge_clip"))
                }

                if (uiState.projects.any { it.hasAudio }) {
                    Text(
                        text = if (lang == AppLanguage.EN) "Append from Saved Projects:" else "إلحاق من المشاريع المحفوظة:",
                        style = MaterialTheme.typography.labelMedium,
                        color = StudioColors.TextSecondary,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        uiState.projects.filter { it.hasAudio }.forEach { proj ->
                            val projChipInteraction = remember { MutableInteractionSource() }
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.appendSavedProjectToCurrent(proj) },
                                interactionSource = projChipInteraction,
                                label = { Text("${proj.name} (${formatDurationMs(proj.durationMs)})") },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .lit(projChipInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 10.dp)
                                    .testTag("editor_append_project_${proj.id}"),
                            )
                        }
                    }
                }

                uiState.exportProgress?.let { prog ->
                    LinearProgressIndicator(
                        progress = { prog.coerceIn(0f, 1f) },
                        color = StudioColors.Cyan,
                        trackColor = StudioColors.BgTop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                    )
                }

                val saveDraftInteraction = remember { MutableInteractionSource() }
                OutlinedButton(
                    onClick = { saveDraftDialogOpen = true },
                    enabled = uiState.hasAudio && !uiState.isBusy && !uiState.isProcessing,
                    interactionSource = saveDraftInteraction,
                    border = BorderStroke(1.dp, StudioColors.Violet.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioColors.Violet),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .lit(saveDraftInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                        .testTag("editor_save_draft_button"),
                ) {
                    Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(t(lang, "save_to_drafts"), style = MaterialTheme.typography.labelLarge)
                }

                val openExportInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = { exportDialogOpen = true },
                    enabled = uiState.hasAudio && !uiState.isBusy && !uiState.isProcessing,
                    interactionSource = openExportInteraction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioColors.Cyan,
                        contentColor = StudioColors.BgTop,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .lit(openExportInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                        .testTag("editor_open_export_dialog_button"),
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(t(lang, "save_export_final"), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        } // end item(key = "editor_export_card")

        item(key = "editor_bottom_spacer") {
            Spacer(Modifier.height(12.dp))
        }
        } // end LazyColumn

        val previewSelInteraction = remember { MutableInteractionSource() }
        Button(
            onClick = {
                viewModel.previewEditorSelectionOrFade(
                    fadeInSec = fadeInSec,
                    fadeOutSec = fadeOutSec,
                    autoFadeEdges = autoFadeEdges,
                    previewSelectionOnly = true,
                )
            },
            enabled = uiState.hasAudio && !uiState.isBusy && !uiState.isProcessing,
            interactionSource = previewSelInteraction,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (uiState.isToolPreviewPlaying) StudioColors.Amber else StudioColors.Cyan,
                contentColor = StudioColors.BgTop,
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .defaultMinSize(minHeight = 48.dp)
                .lit(previewSelInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                .testTag("editor_preview_button"),
        ) {
            Icon(
                imageVector = if (uiState.isToolPreviewPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (uiState.isToolPreviewPlaying) t(lang, "stop_preview_edit") else t(lang, "preview_edit"),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    } // end Box

    SaveDraftDialog(
        open = saveDraftDialogOpen,
        language = lang,
        initialName = uiState.activeProjectName.ifBlank { t(lang, "untitled_project") },
        activePresetLabel = uiState.selectedPresetLabel,
        isCloudSignedIn = uiState.isCloudSynced,
        onDismiss = { saveDraftDialogOpen = false },
        onSaveDraft = { draftName -> viewModel.saveCloudDraft(draftName) },
    )

    ExportFormatDialog(
        open = exportDialogOpen,
        language = lang,
        initialFileName = uiState.activeProjectName.ifBlank { "Sonix_Master" },
        onDismiss = { exportDialogOpen = false },
        onSaveToDevice = { fmt, name -> viewModel.exportRenderedAudio(fmt, name, shareAfter = false) },
        onShareAudio = { fmt, name -> viewModel.exportRenderedAudio(fmt, name, shareAfter = true) },
        onShareToPackage = { fmt, name, pkg -> viewModel.exportRenderedAudio(fmt, name, shareAfter = true, targetPackage = pkg) },
        onSaveForUnity = { name -> viewModel.exportAudioForUnity(name, shareAfter = false) },
        onShareForUnity = { name -> viewModel.exportAudioForUnity(name, shareAfter = true) },
    )
}

@Composable
private fun EditorPlayheadOverlay(
    playbackFlow: StateFlow<PlaybackTelemetry>,
    durationMs: Long,
    selectionStartMs: Long,
    selectionEndMs: Long,
    zoomLevel: Float,
    panFraction: Float,
    edgePadPx: Float,
    modifier: Modifier = Modifier,
) {
    val playback by playbackFlow.collectAsStateWithLifecycle()
    val boundedStartMs = selectionStartMs.coerceIn(0L, durationMs)
    val boundedEndMs = selectionEndMs.coerceIn(boundedStartMs, durationMs)
    var smoothPosMs by remember { mutableLongStateOf(playback.positionMs.coerceIn(boundedStartMs, boundedEndMs)) }

    LaunchedEffect(playback.positionMs, playback.isPlaying, durationMs, boundedStartMs, boundedEndMs) {
        smoothPosMs = playback.positionMs.coerceIn(boundedStartMs, boundedEndMs)
        if (!playback.isPlaying) return@LaunchedEffect
        val anchorPos = playback.positionMs.coerceIn(boundedStartMs, boundedEndMs)
        val startFrameNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { nowNanos ->
                val elapsedMs = ((nowNanos - startFrameNanos) / 1_000_000L).coerceAtLeast(0L)
                smoothPosMs = if (durationMs > 0L) {
                    (anchorPos + elapsedMs).coerceIn(boundedStartMs, boundedEndMs)
                } else {
                    (anchorPos + elapsedMs).coerceIn(boundedStartMs, boundedEndMs)
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.TopStart,
    ) {
        val density = LocalDensity.current
        val boxWidthPx = with(density) { maxWidth.toPx() }
        val indicatorWidthDp = 12.dp
        val halfIndicatorPx = with(density) { (indicatorWidthDp / 2f).toPx() }

        val usableW = (boxWidthPx - 2f * edgePadPx).coerceAtLeast(1f)
        val visSpan = 1f / zoomLevel.coerceIn(1f, 15f)
        val visStart = panFraction.coerceIn(0f, (1f - visSpan).coerceAtLeast(0f))
        val visEnd = (visStart + visSpan).coerceAtMost(1f)
        val playFrac = if (durationMs > 0L) {
            (smoothPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        val visible = playFrac in visStart..visEnd
        val px = (edgePadPx + ((playFrac - visStart) / visSpan) * usableW)
            .coerceIn(2f, (boxWidthPx - 2f).coerceAtLeast(2f))

        Canvas(
            modifier = Modifier
                .width(indicatorWidthDp)
                .fillMaxHeight()
                .graphicsLayer {
                    translationX = px - halfIndicatorPx
                    alpha = if (visible) 1f else 0f
                },
        ) {
            val cx = size.width / 2f
            drawLine(
                color = StudioColors.TextPrimary,
                start = Offset(cx, 0f),
                end = Offset(cx, size.height),
                strokeWidth = 2.4.dp.toPx(),
            )
            drawCircle(
                color = StudioColors.Cyan,
                radius = 4.5.dp.toPx(),
                center = Offset(cx, 6.dp.toPx()),
            )
        }
    }
}
