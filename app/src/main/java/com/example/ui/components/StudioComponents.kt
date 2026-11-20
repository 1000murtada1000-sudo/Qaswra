package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.formatDurationMs
import com.example.i18n.t
import com.example.model.AppLanguage
import com.example.studio.ui.lit
import com.example.ui.theme.StudioColors
import com.example.ui.theme.TelemetryTextStyle
import com.example.model.PlaybackTelemetry
import com.example.viewmodel.StudioUiState
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun InfoBadgeButton(
    contentDescription: String,
    testTag: String,
    accentColor: Color = StudioColors.Cyan,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = CircleShape,
        color = accentColor.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(48.dp)
            .padding(8.dp)
            .lit(interaction, MaterialTheme.colorScheme.primary, cornerRadius = 24.dp)
            .testTag(testTag),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "!",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = accentColor,
            )
        }
    }
}

@Composable
fun StudioScrollColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun MicrophonePermissionRationaleDialog(
    open: Boolean,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!open) return
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioColors.CardSurface,
        title = {
            Text(
                text = t(language, "mic_permission_title"),
                style = MaterialTheme.typography.titleMedium,
                color = StudioColors.TextPrimary,
            )
        },
        text = {
            Text(
                text = t(language, "mic_permission_desc"),
                style = MaterialTheme.typography.bodyMedium,
                color = StudioColors.TextSecondary,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(t(language, "grant_permission"), color = StudioColors.Cyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(t(language, "cancel"), color = StudioColors.TextSecondary)
            }
        },
        modifier = Modifier.testTag("mic_permission_rationale_dialog"),
    )
}

@Composable
fun StudioFocusTopBar(
    title: String,
    subtitle: String? = null,
    language: AppLanguage,
    onBack: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
    bottomContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = StudioColors.CardSurface,
        border = BorderStroke(1.dp, StudioColors.Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val backInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = onBack,
                    interactionSource = backInteraction,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = StudioColors.ElevatedSurface,
                        contentColor = StudioColors.Cyan,
                    ),
                    modifier = Modifier
                        .size(34.dp)
                        .lit(backInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 17.dp)
                        .testTag("back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = t(language, "back"),
                        modifier = Modifier.size(18.dp),
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = StudioColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                if (trailing != null) {
                    trailing()
                }
            }

            if (bottomContent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(StudioColors.Border.copy(alpha = 0.6f)),
                )
                bottomContent()
            }
        }
    }
}

@Composable
fun StudioSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    badgeText: String? = null,
    testTag: String,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
        border = BorderStroke(1.dp, StudioColors.Border),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 76.dp)
            .lit(interaction, MaterialTheme.colorScheme.primary, cornerRadius = 20.dp)
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.14f))
                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = StudioColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (!badgeText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = accentColor.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = accentColor,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = StudioColors.TextMuted,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
fun StudioParamSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    formattedValue: String,
    accentColor: Color = StudioColors.Cyan,
    steps: Int = 0,
    testTag: String,
    debounceMs: Long = 60L,
    onValueChange: (Float) -> Unit,
) {
    var inputDialogOpen by remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf("") }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val clampedExternal = value.coerceIn(min, max)
    var localValue by remember(min, max) { mutableFloatStateOf(clampedExternal) }
    var isDragging by remember { mutableStateOf(false) }
    var pendingEmit by remember { mutableStateOf<Float?>(null) }
    val latestOnValueChange by rememberUpdatedState(onValueChange)

    LaunchedEffect(clampedExternal) {
        if (!isDragging) {
            localValue = clampedExternal
            pendingEmit = null
        }
    }

    LaunchedEffect(pendingEmit) {
        val target = pendingEmit ?: return@LaunchedEffect
        if (debounceMs > 0L) {
            delay(debounceMs)
        }
        latestOnValueChange(target)
        pendingEmit = null
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = StudioColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = StudioColors.BgTop,
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                modifier = Modifier
                    .defaultMinSize(minHeight = 30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        textValue = String.format(Locale.US, "%.2f", localValue.coerceIn(min, max))
                        inputDialogOpen = true
                    }
                    .testTag("${testTag}_counter"),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = formattedValue,
                        style = TelemetryTextStyle,
                        color = accentColor,
                    )
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = if (isRtl) "تعديل القيمة" else "Edit value",
                        tint = accentColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }

        Slider(
            value = localValue.coerceIn(min, max),
            onValueChange = { next ->
                isDragging = true
                val clamped = next.coerceIn(min, max)
                localValue = clamped
                pendingEmit = clamped
            },
            onValueChangeFinished = {
                isDragging = false
                val finalVal = localValue.coerceIn(min, max)
                pendingEmit = null
                latestOnValueChange(finalVal)
            },
            valueRange = min..max,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = StudioColors.ElevatedSurface,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .testTag(testTag),
        )
    }

    if (inputDialogOpen) {
        val minText = String.format(Locale.US, "%.2f", min)
        val maxText = String.format(Locale.US, "%.2f", max)
        AlertDialog(
            onDismissRequest = { inputDialogOpen = false },
            containerColor = StudioColors.CardSurface,
            titleContentColor = StudioColors.TextPrimary,
            textContentColor = StudioColors.TextSecondary,
            title = {
                Text(
                    text = if (isRtl) "إدخال القيمة بدقة عشرية" else "Exact Decimal Value",
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "$label ($minText .. $maxText)",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioColors.Cyan,
                    )
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { next ->
                            textValue = next
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text(if (isRtl) "أدخل القيمة الرقمية" else "Enter value") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = StudioColors.Border,
                            focusedTextColor = StudioColors.TextPrimary,
                            unfocusedTextColor = StudioColors.TextPrimary,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("${testTag}_dialog_input"),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = textValue
                            .trim()
                            .replace(',', '.')
                            .toFloatOrNull()
                        if (parsed != null) {
                            val clamped = parsed.coerceIn(min, max)
                            isDragging = false
                            localValue = clamped
                            pendingEmit = null
                            latestOnValueChange(clamped)
                        }
                        inputDialogOpen = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = StudioColors.BgTop,
                    ),
                ) {
                    Text(if (isRtl) "تعيين" else "Set")
                }
            },
            dismissButton = {
                TextButton(onClick = { inputDialogOpen = false }) {
                    Text(
                        text = if (isRtl) "إلغاء" else "Cancel",
                        color = StudioColors.TextSecondary,
                    )
                }
            },
        )
    }
}

@Composable
fun PreviewAndApplyActionRow(
    isPreviewPlaying: Boolean,
    previewEnabled: Boolean,
    applyEnabled: Boolean,
    language: AppLanguage,
    previewTag: String,
    applyTag: String,
    applyLabel: String = t(language, "apply_now"),
    applyColor: Color = StudioColors.Violet,
    onPreviewClick: () -> Unit,
    onApplyClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val previewInteraction = remember { MutableInteractionSource() }
        Button(
            onClick = onPreviewClick,
            enabled = previewEnabled,
            interactionSource = previewInteraction,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPreviewPlaying) StudioColors.Amber else StudioColors.Cyan,
                contentColor = StudioColors.BgTop,
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .lit(previewInteraction, MaterialTheme.colorScheme.primary, selected = isPreviewPlaying, cornerRadius = 14.dp)
                .testTag(previewTag),
        ) {
            Icon(
                imageVector = if (isPreviewPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isPreviewPlaying) t(language, "stop_preview_edit") else t(language, "preview_edit"),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
        }

        val applyInteraction = remember { MutableInteractionSource() }
        OutlinedButton(
            onClick = onApplyClick,
            enabled = applyEnabled,
            interactionSource = applyInteraction,
            border = BorderStroke(1.dp, applyColor.copy(alpha = 0.7f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = applyColor),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .lit(applyInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                .testTag(applyTag),
        ) {
            Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(applyLabel, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun RealTimeWaveformCanvas(
    bars: List<Float>,
    isActive: Boolean,
    accentColor: Color = StudioColors.Cyan,
    modifier: Modifier = Modifier,
) {
    val activeBrush = remember(accentColor) {
        Brush.verticalGradient(
            colors = listOf(accentColor, StudioColors.Violet),
        )
    }
    val inactiveBrush = remember {
        Brush.verticalGradient(
            colors = listOf(StudioColors.WaveformInactive, StudioColors.WaveformTrack),
        )
    }

    Canvas(modifier = modifier) {
        val count = bars.size.coerceAtLeast(1)
        val centerY = size.height / 2f

        drawLine(
            color = StudioColors.Border.copy(alpha = 0.6f),
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 1.dp.toPx(),
        )

        val stepW = size.width / count
        val barW = (stepW * 0.56f).coerceAtLeast(2.dp.toPx())
        val radius = barW / 2f

        for (i in 0 until count) {
            val amp = bars[i].coerceIn(0.05f, 1f)
            val barH = amp * size.height * 0.88f
            val left = i * stepW + (stepW - barW) / 2f
            val top = centerY - barH / 2f
            drawRoundRect(
                brush = if (isActive) activeBrush else inactiveBrush,
                topLeft = Offset(left, top),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(radius, radius),
            )
        }
    }
}

@Composable
fun StudioPreviewControlsBar(
    uiState: StudioUiState,
    onHoldBypassChange: (Boolean) -> Unit,
    onToggleBypass: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onResetZero: () -> Unit,
    onOpenSaveDraft: () -> Unit,
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
) {
    val lang = uiState.language
    val effectiveBypass = uiState.dspConfig.bypassAll || uiState.isHoldBypassing

    val controls: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (embedded) 8.dp else 12.dp,
                    vertical = if (embedded) 6.dp else 10.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(if (embedded) 6.dp else 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val compareInteraction = remember { MutableInteractionSource() }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (effectiveBypass) StudioColors.Amber.copy(alpha = 0.2f) else StudioColors.ElevatedSurface,
                    border = BorderStroke(
                        1.dp,
                        if (effectiveBypass) StudioColors.Amber else StudioColors.Cyan.copy(alpha = 0.45f),
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = if (embedded) 34.dp else 48.dp)
                        .lit(compareInteraction, MaterialTheme.colorScheme.primary, selected = effectiveBypass, cornerRadius = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val press = PressInteraction.Press(offset)
                                    compareInteraction.tryEmit(press)
                                    onHoldBypassChange(true)
                                    val released = try {
                                        tryAwaitRelease()
                                    } finally {
                                        onHoldBypassChange(false)
                                    }
                                    compareInteraction.tryEmit(
                                        if (released) PressInteraction.Release(press) else PressInteraction.Cancel(press),
                                    )
                                },
                                onTap = {
                                    onToggleBypass()
                                },
                            )
                        }
                        .testTag("preview_compare_original_button"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = if (embedded) 6.dp else 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CompareArrows,
                            contentDescription = t(lang, "compare_original"),
                            tint = if (effectiveBypass) StudioColors.Amber else StudioColors.Cyan,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (effectiveBypass) t(lang, "bypass_all") else t(lang, "compare_original"),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (effectiveBypass) StudioColors.Amber else StudioColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (!embedded) {
                    val undoInteraction = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = onUndo,
                        interactionSource = undoInteraction,
                        enabled = uiState.canUndoEdit,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = StudioColors.ElevatedSurface,
                            contentColor = StudioColors.Cyan,
                            disabledContainerColor = StudioColors.BgMid,
                            disabledContentColor = StudioColors.TextMuted.copy(alpha = 0.4f),
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .lit(undoInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 24.dp)
                            .testTag("preview_undo_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Undo,
                            contentDescription = t(lang, "undo_edit"),
                        )
                    }
                }

                if (!embedded) {
                    val redoInteraction = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = onRedo,
                        interactionSource = redoInteraction,
                        enabled = uiState.canRedoEdit,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = StudioColors.ElevatedSurface,
                            contentColor = StudioColors.Cyan,
                            disabledContainerColor = StudioColors.BgMid,
                            disabledContentColor = StudioColors.TextMuted.copy(alpha = 0.4f),
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .lit(redoInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 24.dp)
                            .testTag("preview_redo_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Redo,
                            contentDescription = t(lang, "redo_edit"),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val resetInteraction = remember { MutableInteractionSource() }
                OutlinedButton(
                    onClick = onResetZero,
                    interactionSource = resetInteraction,
                    border = BorderStroke(1.dp, StudioColors.Rose.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioColors.RoseBright),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = if (embedded) 34.dp else 48.dp)
                        .lit(resetInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 12.dp)
                        .testTag("preview_reset_zero_button"),
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = t(lang, "reset_zero"),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                val saveDraftInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = onOpenSaveDraft,
                    interactionSource = saveDraftInteraction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioColors.Violet.copy(alpha = 0.22f),
                        contentColor = StudioColors.Violet,
                    ),
                    border = BorderStroke(1.dp, StudioColors.Violet.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = if (embedded) 34.dp else 48.dp)
                        .lit(saveDraftInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 12.dp)
                        .testTag("preview_save_draft_button"),
                ) {
                    Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = t(lang, "save_to_drafts"),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    if (embedded) {
        Box(modifier = modifier.fillMaxWidth()) {
            controls()
        }
    } else {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
            border = BorderStroke(1.dp, StudioColors.Border),
            modifier = modifier.fillMaxWidth(),
        ) {
            controls()
        }
    }
}

@Composable
fun PersistentTransportBar(
    uiState: StudioUiState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onSeekFraction: (Float) -> Unit,
    onToggleAutoLoop: () -> Unit,
    onToggleBypass: () -> Unit,
    onHoldBypassChange: (Boolean) -> Unit,
    onOpenSleepTimer: (() -> Unit)? = null,
    onUndo: (() -> Unit)? = null,
    onRedo: (() -> Unit)? = null,
    includeNavBarsPadding: Boolean = true,
    playbackFlow: StateFlow<PlaybackTelemetry>? = null,
    modifier: Modifier = Modifier,
) {
    val lang = uiState.language
    val effectiveBypass = uiState.dspConfig.bypassAll || uiState.isHoldBypassing

    val barModifier = modifier
        .fillMaxWidth()
        .let { if (includeNavBarsPadding) it.navigationBarsPadding() else it }

    Surface(
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        color = StudioColors.CardSurface,
        border = BorderStroke(1.dp, StudioColors.BorderBright.copy(alpha = 0.7f)),
        shadowElevation = 12.dp,
        modifier = barModifier,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TransportSeekRow(
                    playbackFlow = playbackFlow,
                    fallbackPlayback = uiState.playback,
                    fallbackDurationMs = uiState.durationMs,
                    hasAudio = uiState.hasAudio,
                    onSeekFraction = onSeekFraction,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TransportPlayPauseButton(
                        playbackFlow = playbackFlow,
                        fallbackIsPlaying = uiState.playback.isPlaying,
                        lang = lang,
                        onPlayPause = onPlayPause,
                    )

                    val loopInteraction = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = onToggleAutoLoop,
                        interactionSource = loopInteraction,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (uiState.autoLoopOnPlayback) StudioColors.Emerald.copy(alpha = 0.2f) else StudioColors.ElevatedSurface,
                            contentColor = if (uiState.autoLoopOnPlayback) StudioColors.Emerald else StudioColors.TextSecondary,
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .lit(loopInteraction, MaterialTheme.colorScheme.primary, selected = uiState.autoLoopOnPlayback, cornerRadius = 24.dp)
                            .testTag("transport_loop_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Repeat,
                            contentDescription = t(lang, "loop_repeat"),
                        )
                    }

                    if (onOpenSleepTimer != null) {
                        val timerActive = uiState.sleepTimerRemainingSec > 0
                        val timerInteraction = remember { MutableInteractionSource() }
                        Surface(
                            onClick = onOpenSleepTimer,
                            interactionSource = timerInteraction,
                            shape = RoundedCornerShape(14.dp),
                            color = if (timerActive) StudioColors.Violet.copy(alpha = 0.22f) else StudioColors.ElevatedSurface,
                            border = if (timerActive) BorderStroke(1.dp, StudioColors.Violet.copy(alpha = 0.6f)) else null,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .lit(timerInteraction, MaterialTheme.colorScheme.primary, selected = timerActive, cornerRadius = 14.dp)
                                .testTag("transport_sleep_timer_button"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = if (timerActive) 10.dp else 0.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bedtime,
                                    contentDescription = t(lang, "sleep_timer_title"),
                                    tint = if (timerActive) StudioColors.Violet else StudioColors.TextSecondary,
                                    modifier = Modifier.size(20.dp),
                                )
                                if (timerActive) {
                                    Spacer(Modifier.width(4.dp))
                                    val mins = uiState.sleepTimerRemainingSec / 60
                                    val secs = uiState.sleepTimerRemainingSec % 60
                                    Text(
                                        text = String.format("%02d:%02d", mins, secs),
                                        style = TelemetryTextStyle.copy(fontSize = 11.sp),
                                        color = StudioColors.Violet,
                                    )
                                }
                            }
                        }
                    }

                    if (onUndo != null) {
                        val tUndoInteraction = remember { MutableInteractionSource() }
                        IconButton(
                            onClick = onUndo,
                            interactionSource = tUndoInteraction,
                            enabled = uiState.canUndoEdit,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = StudioColors.ElevatedSurface,
                                contentColor = StudioColors.Cyan,
                                disabledContainerColor = StudioColors.BgMid,
                                disabledContentColor = StudioColors.TextMuted.copy(alpha = 0.35f),
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .lit(tUndoInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 24.dp)
                                .testTag("transport_undo_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Undo,
                                contentDescription = t(lang, "undo_edit"),
                            )
                        }
                    }

                    if (onRedo != null) {
                        val tRedoInteraction = remember { MutableInteractionSource() }
                        IconButton(
                            onClick = onRedo,
                            interactionSource = tRedoInteraction,
                            enabled = uiState.canRedoEdit,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = StudioColors.ElevatedSurface,
                                contentColor = StudioColors.Cyan,
                                disabledContainerColor = StudioColors.BgMid,
                                disabledContentColor = StudioColors.TextMuted.copy(alpha = 0.35f),
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .lit(tRedoInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 24.dp)
                                .testTag("transport_redo_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Redo,
                                contentDescription = t(lang, "redo_edit"),
                            )
                        }
                    }

                    TransportOutputMeter(
                        playbackFlow = playbackFlow,
                        fallbackLevel = uiState.playback.outputLevel,
                        modifier = Modifier.weight(1f),
                    )

                    // A/B Compare / Bypass chip supporting tap-to-toggle AND press-and-hold
                    val bypassInteraction = remember { MutableInteractionSource() }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (effectiveBypass) StudioColors.Amber.copy(alpha = 0.18f) else StudioColors.ElevatedSurface,
                        border = BorderStroke(
                            1.dp,
                            if (effectiveBypass) StudioColors.Amber else StudioColors.BorderBright,
                        ),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .lit(bypassInteraction, MaterialTheme.colorScheme.primary, selected = effectiveBypass, cornerRadius = 14.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = { offset ->
                                        val press = PressInteraction.Press(offset)
                                        bypassInteraction.tryEmit(press)
                                        onHoldBypassChange(true)
                                        val released = try {
                                            tryAwaitRelease()
                                        } finally {
                                            onHoldBypassChange(false)
                                        }
                                        bypassInteraction.tryEmit(
                                            if (released) PressInteraction.Release(press) else PressInteraction.Cancel(press),
                                        )
                                    },
                                    onTap = {
                                        onToggleBypass()
                                    },
                                )
                            }
                            .testTag("transport_bypass_chip"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = if (effectiveBypass) Icons.Rounded.GraphicEq else Icons.Rounded.Tune,
                                contentDescription = if (effectiveBypass) t(lang, "bypass_all") else t(lang, "dsp_active"),
                                tint = if (effectiveBypass) StudioColors.Amber else StudioColors.Cyan,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = if (effectiveBypass) t(lang, "bypass_all") else t(lang, "dsp_active"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (effectiveBypass) StudioColors.Amber else StudioColors.TextPrimary,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransportSeekRow(
    playbackFlow: StateFlow<PlaybackTelemetry>?,
    fallbackPlayback: PlaybackTelemetry,
    fallbackDurationMs: Long,
    hasAudio: Boolean,
    onSeekFraction: (Float) -> Unit,
) {
    val collected = playbackFlow?.collectAsStateWithLifecycle()
    val telemetry = collected?.value ?: fallbackPlayback
    val durationMs = if (telemetry.durationMs > 0L) telemetry.durationMs else fallbackDurationMs
    val progress = if (durationMs > 0L) {
        (telemetry.positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    var isSeeking by remember { mutableStateOf(false) }
    var seekFraction by remember { mutableFloatStateOf(progress) }
    LaunchedEffect(progress) {
        if (!isSeeking) seekFraction = progress
    }
    val displayedPositionMs = (seekFraction * durationMs).toLong().coerceIn(0L, durationMs)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = formatDurationMs(if (isSeeking) displayedPositionMs else telemetry.positionMs),
                style = TelemetryTextStyle,
                color = StudioColors.Cyan,
            )

            Slider(
                value = seekFraction,
                onValueChange = {
                    isSeeking = true
                    seekFraction = it
                },
                onValueChangeFinished = {
                    if (isSeeking) onSeekFraction(seekFraction)
                    isSeeking = false
                },
                enabled = hasAudio,
                colors = SliderDefaults.colors(
                    thumbColor = StudioColors.Cyan,
                    activeTrackColor = StudioColors.Cyan,
                    inactiveTrackColor = StudioColors.ElevatedSurface,
                    disabledThumbColor = StudioColors.TextMuted,
                    disabledActiveTrackColor = StudioColors.WaveformInactive,
                    disabledInactiveTrackColor = StudioColors.BgTop,
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .testTag("transport_seek_slider"),
            )

            Text(
                text = formatDurationMs(durationMs),
                style = TelemetryTextStyle,
                color = StudioColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun TransportPlayPauseButton(
    playbackFlow: StateFlow<PlaybackTelemetry>?,
    fallbackIsPlaying: Boolean,
    lang: AppLanguage,
    onPlayPause: () -> Unit,
) {
    val collected = playbackFlow?.collectAsStateWithLifecycle()
    val isPlaying = collected?.value?.isPlaying ?: fallbackIsPlaying
    val playInteraction = remember { MutableInteractionSource() }
    Button(
        onClick = onPlayPause,
        interactionSource = playInteraction,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPlaying) StudioColors.Amber else StudioColors.Cyan,
            contentColor = StudioColors.BgTop,
        ),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .lit(playInteraction, MaterialTheme.colorScheme.primary, selected = isPlaying, cornerRadius = 14.dp)
            .testTag("transport_play_pause_button"),
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) t(lang, "pause") else t(lang, "play"),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (isPlaying) t(lang, "pause") else t(lang, "play"),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}

@Composable
private fun TransportOutputMeter(
    playbackFlow: StateFlow<PlaybackTelemetry>?,
    fallbackLevel: Float,
    modifier: Modifier = Modifier,
) {
    val collected = playbackFlow?.collectAsStateWithLifecycle()
    val level = collected?.value?.outputLevel ?: fallbackLevel
    val meterColor = when {
        level > 0.85f -> StudioColors.Rose
        level > 0.6f -> StudioColors.Amber
        else -> StudioColors.Emerald
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(StudioColors.BgTop),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(level.coerceIn(0.02f, 1f))
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(meterColor),
            )
        }
        Text(
            text = "${(level * 100f).roundToInt()}%",
            style = TelemetryTextStyle.copy(fontSize = 10.sp),
            color = StudioColors.TextMuted,
            maxLines = 1,
        )
    }
}

@Composable
fun SonixExitConfirmDialog(
    open: Boolean,
    language: AppLanguage,
    hasUnsavedChanges: Boolean = false,
    onStay: () -> Unit,
    onConfirmExit: () -> Unit,
    onSaveToDraftsAndExit: () -> Unit = onConfirmExit,
) {
    if (!open) return
    val isEn = language == AppLanguage.EN
    AlertDialog(
        onDismissRequest = onStay,
        containerColor = StudioColors.CardSurface,
        titleContentColor = StudioColors.TextPrimary,
        textContentColor = StudioColors.TextSecondary,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = if (isEn) "Confirm Exit" else "تأكيد المغادرة",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = StudioColors.Cyan,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isEn) "Choose how to leave Sonix ✦." else "اختر كيفية مغادرة Sonix ✦.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StudioColors.TextSecondary,
                )
                if (hasUnsavedChanges) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = onSaveToDraftsAndExit,
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 52.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) { Text(if (isEn) "Save draft" else "حفظ في المسودات والخروج", textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
                        OutlinedButton(
                            onClick = onConfirmExit,
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 52.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, StudioColors.RoseBright.copy(alpha = 0.75f)),
                        ) { Text(if (isEn) "Exit" else "خروج بدون حفظ", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = StudioColors.RoseBright) }
                        OutlinedButton(
                            onClick = onStay,
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 52.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, StudioColors.Cyan.copy(alpha = 0.65f)),
                        ) { Text(if (isEn) "Stay" else "البقاء", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = StudioColors.Cyan) }
                    }
                }
            }
        },
        confirmButton = {
            if (!hasUnsavedChanges) {
                Button(
                    onClick = onConfirmExit,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioColors.RoseBright, contentColor = StudioColors.BgTop),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("exit_dialog_confirm_button"),
                ) { Text(if (isEn) "Exit" else "خروج", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) }
            } else Unit
        },
        dismissButton = {
            if (!hasUnsavedChanges) OutlinedButton(onClick = onStay, border = BorderStroke(1.dp, StudioColors.Cyan.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("exit_dialog_stay_button")) {
                Text(if (isEn) "Stay" else "البقاء", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = StudioColors.Cyan)
            } else Unit
        },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(20.dp)
    )
}
