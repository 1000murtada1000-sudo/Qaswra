package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.formatDurationMs
import com.example.i18n.t
import com.example.model.AppLanguage
import com.example.model.AudioRecordingItem
import com.example.model.BatchFilterPreset
import com.example.model.BatchItemStatus
import com.example.model.BatchQueueItem
import com.example.model.DeviceAudioItem
import com.example.model.ExportAudioFormat
import com.example.studio.ui.lit
import com.example.ui.components.StudioFocusTopBar
import com.example.ui.components.StudioParamSlider
import com.example.ui.components.StudioScrollColumn
import com.example.ui.theme.StudioColors
import com.example.ui.theme.TelemetryTextStyle
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BatchQueueScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onOpenStudioEditor: () -> Unit,
    onBack: () -> Unit,
) {
    val lang = uiState.language
    val isEn = lang == AppLanguage.EN
    val batch = uiState.batchQueueState
    val settings = batch.settings
    var showLibraryPickerDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        onBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopToolPreview()
        }
    }

    val multiFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addBatchUris(uris)
        }
    }

    StudioScrollColumn {
        StudioFocusTopBar(
            title = t(lang, "tool_batch_queue"),
            subtitle = t(lang, "batch_queue_subtitle"),
            language = lang,
            onBack = onBack,
        )

        // 1. Source Picker & Queue Summary Card
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QueueMusic,
                            contentDescription = null,
                            tint = StudioColors.Cyan,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = if (isEn) {
                                "Queued Files (${batch.items.size})"
                            } else {
                                "الملفات في القائمة (${batch.items.size})"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = StudioColors.TextPrimary,
                        )
                    }
                    if (batch.items.isNotEmpty() && !batch.isRunning) {
                        TextButton(
                            onClick = { viewModel.clearBatchQueue(onlyCompleted = false) },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("batch_clear_all_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = t(lang, "batch_clear_queue"),
                                tint = StudioColors.RoseBright,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = t(lang, "batch_clear_queue"),
                                color = StudioColors.RoseBright,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val pickDeviceInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = { multiFilePickerLauncher.launch(arrayOf("audio/*")) },
                        enabled = !batch.isRunning,
                        interactionSource = pickDeviceInteraction,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioColors.Cyan,
                            contentColor = StudioColors.BgTop,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(pickDeviceInteraction, StudioColors.Cyan, cornerRadius = 14.dp)
                            .testTag("batch_pick_device_files_button"),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = t(lang, "batch_pick_files"),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }

                    val pickLibInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = {
                            if (uiState.deviceAudios.isEmpty()) {
                                viewModel.refreshDeviceAudios()
                            }
                            showLibraryPickerDialog = true
                        },
                        enabled = !batch.isRunning,
                        interactionSource = pickLibInteraction,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, StudioColors.Violet.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(pickLibInteraction, StudioColors.Violet, cornerRadius = 14.dp)
                            .testTag("batch_pick_library_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LibraryMusic,
                            contentDescription = null,
                            tint = StudioColors.Violet,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = t(lang, "batch_pick_library"),
                            color = StudioColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }

        // 2. Shared DSP Settings Card (Pitch, Speed, Filter, Normalize & Format)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = StudioColors.Amber,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = if (isEn) "Shared Batch DSP Chain" else "إعدادات المعالجة الموحدة",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = StudioColors.TextPrimary,
                        )
                    }
                    if (!batch.isRunning && (abs(settings.pitchSemitones) >= 0.05f || abs(settings.speedRatio - 1f) >= 0.02f)) {
                        TextButton(
                            onClick = {
                                viewModel.updateBatchDspSettings(
                                    pitchSemitones = 0f,
                                    speedRatio = 1.0f,
                                )
                            },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .testTag("batch_reset_pitch_speed_button"),
                        ) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isEn) "Reset 0st / 1x" else "إعادة 0 / 1x",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }

                // Pitch Slider (-12..+12 semitones)
                val pitchLabel = String.format(Locale.US, "%+.1f st", settings.pitchSemitones)
                StudioParamSlider(
                    label = if (isEn) "Pitch Shift (Semitones)" else "طبقة الصوت (نصف نغمة)",
                    value = settings.pitchSemitones,
                    min = -12f,
                    max = 12f,
                    formattedValue = pitchLabel,
                    accentColor = StudioColors.Cyan,
                    testTag = "batch_pitch_slider",
                    onValueChange = { next ->
                        if (!batch.isRunning) {
                            viewModel.updateBatchDspSettings(pitchSemitones = next)
                        }
                    },
                )

                // Speed Ratio Slider (0.50x..2.50x)
                val speedLabel = String.format(Locale.US, "%.2fx", settings.speedRatio)
                StudioParamSlider(
                    label = if (isEn) "Speed / Tempo Ratio" else "السرعة (Tempo)",
                    value = settings.speedRatio,
                    min = 0.5f,
                    max = 2.5f,
                    formattedValue = speedLabel,
                    accentColor = StudioColors.Amber,
                    testTag = "batch_speed_slider",
                    onValueChange = { next ->
                        if (!batch.isRunning) {
                            viewModel.updateBatchDspSettings(speedRatio = next)
                        }
                    },
                )

                // Filter Preset Selector
                Text(
                    text = if (isEn) "Audio Filter Preset" else "الفلتر والمعالجة الصوتية",
                    style = MaterialTheme.typography.labelLarge,
                    color = StudioColors.TextPrimary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    BatchFilterPreset.entries.forEach { preset ->
                        val selected = settings.filterPreset == preset
                        val activeSuffix = if (preset == BatchFilterPreset.ACTIVE_STUDIO_DSP) {
                            val activeCount = uiState.dspConfig.chain.count { it.enabled }
                            " ($activeCount)"
                        } else {
                            ""
                        }
                        FilterChip(
                            selected = selected,
                            enabled = !batch.isRunning,
                            onClick = { viewModel.updateBatchDspSettings(filterPreset = preset) },
                            label = {
                                Text(
                                    text = t(lang, preset.titleKey) + activeSuffix,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioColors.Cyan.copy(alpha = 0.2f),
                                selectedLabelColor = StudioColors.Cyan,
                            ),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .testTag("batch_filter_chip_${preset.name.lowercase()}"),
                        )
                    }
                }

                HorizontalDivider(color = StudioColors.Border)

                // Output Format Chips
                Text(
                    text = if (isEn) "Output Format" else "صيغة التصدير الدفعي",
                    style = MaterialTheme.typography.labelLarge,
                    color = StudioColors.TextPrimary,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        ExportAudioFormat.M4A,
                        ExportAudioFormat.MP3,
                        ExportAudioFormat.WAV,
                        ExportAudioFormat.FLAC,
                    ).forEach { fmt ->
                        val selected = settings.outputFormat == fmt
                        FilterChip(
                            selected = selected,
                            enabled = !batch.isRunning,
                            onClick = { viewModel.updateBatchDspSettings(outputFormat = fmt) },
                            label = {
                                Text(
                                    text = fmt.name,
                                    style = TelemetryTextStyle,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioColors.Violet.copy(alpha = 0.22f),
                                selectedLabelColor = StudioColors.Violet,
                            ),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .testTag("batch_format_chip_${fmt.name.lowercase()}"),
                        )
                    }
                }

                // Loudness Normalization Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = if (isEn) "Auto Loudness Normalization (-15 LUFS)" else "موازنة الصوت التلقائية (-15 LUFS)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = StudioColors.TextPrimary,
                        )
                        Text(
                            text = if (isEn) "Consistent broadcast level & true-peak guard" else "توحيد مستوى الصوت لجميع الملفات ومنع التشوه",
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioColors.TextSecondary,
                        )
                    }
                    Switch(
                        checked = settings.normalizeOutput,
                        enabled = !batch.isRunning,
                        onCheckedChange = { viewModel.updateBatchDspSettings(normalizeOutput = it) },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("batch_normalize_switch"),
                    )
                }
            }
        }

        // 3. Batch Run Progress & Share Card
        if (batch.isRunning || batch.statusSummary != null || batch.completedCount > 0) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StudioColors.ElevatedSurface),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (batch.isRunning) StudioColors.Cyan else StudioColors.Emerald.copy(alpha = 0.55f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("batch_progress_card"),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val pct = (batch.overallProgress.coerceIn(0f, 1f) * 100f).toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = when {
                                batch.isRunning -> {
                                    val idx = (batch.currentIndex + 1).coerceAtLeast(1)
                                    if (isEn) "Processing $idx / ${batch.items.size}..." else "جاري المعالجة $idx / ${batch.items.size}..."
                                }
                                !batch.statusSummary.isNullOrBlank() -> batch.statusSummary
                                else -> {
                                    if (isEn) "Completed: ${batch.completedCount} • Failed: ${batch.failedCount}"
                                    else "مكتمل: ${batch.completedCount} • فشل: ${batch.failedCount}"
                                }
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (batch.isRunning) StudioColors.Cyan else StudioColors.Emerald,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "$pct%",
                            style = TelemetryTextStyle,
                            color = StudioColors.Cyan,
                        )
                    }

                    LinearProgressIndicator(
                        progress = { batch.overallProgress.coerceIn(0f, 1f) },
                        color = if (batch.isRunning) StudioColors.Cyan else StudioColors.Emerald,
                        trackColor = StudioColors.BgTop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .testTag("batch_overall_progress_bar"),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (batch.isRunning) {
                            OutlinedButton(
                                onClick = { viewModel.cancelBatchProcessingQueue() },
                                border = BorderStroke(1.dp, StudioColors.RoseBright),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("batch_cancel_button"),
                            ) {
                                Icon(Icons.Rounded.Stop, contentDescription = null, tint = StudioColors.RoseBright, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(t(lang, "batch_cancel_queue"), color = StudioColors.RoseBright)
                            }
                        } else if (batch.completedCount > 0) {
                            Button(
                                onClick = { viewModel.shareAllBatchOutputs() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StudioColors.Emerald,
                                    contentColor = StudioColors.BgTop,
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("batch_share_all_button"),
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "${t(lang, "batch_share_all")} (${batch.completedCount})",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Primary Start Button
        val startInteraction = remember { MutableInteractionSource() }
        Button(
            onClick = { viewModel.startBatchProcessingQueue() },
            enabled = batch.items.isNotEmpty() && !batch.isRunning,
            interactionSource = startInteraction,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = StudioColors.Cyan,
                contentColor = StudioColors.BgTop,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .lit(startInteraction, StudioColors.Cyan, cornerRadius = 16.dp)
                .testTag("batch_start_button"),
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (batch.isRunning) {
                    if (isEn) "Processing Batch Queue..." else "جاري معالجة الملفات في الخلفية..."
                } else {
                    "${t(lang, "batch_start_queue")} (${batch.items.size})"
                },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            )
        }

        // 4. Queued Items List
        if (batch.items.isEmpty()) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
                border = BorderStroke(1.dp, StudioColors.Border),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AudioFile,
                        contentDescription = null,
                        tint = StudioColors.TextMuted,
                        modifier = Modifier.size(36.dp),
                    )
                    Text(
                        text = t(lang, "batch_empty_hint"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudioColors.TextSecondary,
                    )
                }
            }
        } else {
            batch.items.forEach { item ->
                BatchQueueItemCard(
                    item = item,
                    language = lang,
                    isRunning = batch.isRunning,
                    isPreviewing = batch.previewingItemId == item.id && uiState.isToolPreviewPlaying,
                    onPlayToggle = { viewModel.previewBatchOutputItem(item) },
                    onSave = { viewModel.saveBatchOutputItem(item) },
                    onLoadToStudio = {
                        viewModel.loadBatchOutputToStudio(item) {
                            onOpenStudioEditor()
                        }
                    },
                    onRemove = { viewModel.removeBatchQueueItem(item.id) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
    }

    if (showLibraryPickerDialog) {
        BatchLibraryMultiPickDialog(
            language = lang,
            recordings = uiState.recordings,
            deviceAudios = uiState.deviceAudios,
            isScanning = uiState.isScanningDeviceAudio,
            onDismiss = { showLibraryPickerDialog = false },
            onConfirm = { pickedRecordings, pickedDevice ->
                viewModel.addBatchRecordings(pickedRecordings)
                viewModel.addBatchDeviceAudios(pickedDevice)
                showLibraryPickerDialog = false
            },
        )
    }
}

@Composable
private fun BatchQueueItemCard(
    item: BatchQueueItem,
    language: AppLanguage,
    isRunning: Boolean,
    isPreviewing: Boolean,
    onPlayToggle: () -> Unit,
    onSave: () -> Unit,
    onLoadToStudio: () -> Unit,
    onRemove: () -> Unit,
) {
    val isEn = language == AppLanguage.EN
    val (statusLabel, statusColor) = when (item.status) {
        BatchItemStatus.QUEUED -> (if (isEn) "Queued" else "في الانتظار") to StudioColors.TextSecondary
        BatchItemStatus.PROCESSING -> (if (isEn) "Processing ${(item.itemProgress * 100).toInt()}%" else "يعالج ${(item.itemProgress * 100).toInt()}%") to StudioColors.Cyan
        BatchItemStatus.COMPLETED -> (if (isEn) "Ready (${item.outputFormat.name})" else "جاهز (${item.outputFormat.name})") to StudioColors.Emerald
        BatchItemStatus.FAILED -> (if (isEn) "Failed" else "تعذر") to StudioColors.RoseBright
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
        border = BorderStroke(
            width = 1.dp,
            color = when (item.status) {
                BatchItemStatus.PROCESSING -> StudioColors.Cyan.copy(alpha = 0.7f)
                BatchItemStatus.COMPLETED -> StudioColors.Emerald.copy(alpha = 0.5f)
                BatchItemStatus.FAILED -> StudioColors.RoseBright.copy(alpha = 0.5f)
                else -> StudioColors.Border
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("batch_item_${item.id}"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = when (item.status) {
                        BatchItemStatus.COMPLETED -> Icons.Rounded.CheckCircle
                        BatchItemStatus.FAILED -> Icons.Rounded.ErrorOutline
                        else -> Icons.Rounded.AudioFile
                    },
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(22.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = item.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = StudioColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = statusColor.copy(alpha = 0.14f),
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                        if (item.durationMs > 0L) {
                            Text(
                                text = formatDurationMs(item.durationMs),
                                style = TelemetryTextStyle,
                                color = StudioColors.TextMuted,
                            )
                        }
                    }
                }
                if (!isRunning) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("batch_remove_${item.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = if (isEn) "Remove" else "إزالة",
                            tint = StudioColors.TextMuted,
                        )
                    }
                }
            }

            if (item.status == BatchItemStatus.PROCESSING) {
                LinearProgressIndicator(
                    progress = { item.itemProgress.coerceIn(0f, 1f) },
                    color = StudioColors.Cyan,
                    trackColor = StudioColors.BgTop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                )
            }

            if (item.status == BatchItemStatus.FAILED && !item.errorMessage.isNullOrBlank()) {
                Text(
                    text = item.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioColors.RoseBright,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (item.status == BatchItemStatus.COMPLETED && !item.outputFilePath.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onPlayToggle,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, StudioColors.Cyan.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("batch_play_${item.id}"),
                    ) {
                        Icon(
                            imageVector = if (isPreviewing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = StudioColors.Cyan,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isPreviewing) (if (isEn) "Stop" else "إيقاف") else (if (isEn) "Play" else "تشغيل"),
                            color = StudioColors.Cyan,
                            maxLines = 1,
                        )
                    }

                    OutlinedButton(
                        onClick = onSave,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, StudioColors.Emerald.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("batch_save_${item.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            tint = StudioColors.Emerald,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isEn) "Save" else "حفظ",
                            color = StudioColors.Emerald,
                            maxLines = 1,
                        )
                    }

                    OutlinedButton(
                        onClick = onLoadToStudio,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, StudioColors.Violet.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("batch_open_studio_${item.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileOpen,
                            contentDescription = null,
                            tint = StudioColors.Violet,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isEn) "Edit" else "تعديل",
                            color = StudioColors.Violet,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchLibraryMultiPickDialog(
    language: AppLanguage,
    recordings: List<AudioRecordingItem>,
    deviceAudios: List<DeviceAudioItem>,
    isScanning: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (List<AudioRecordingItem>, List<DeviceAudioItem>) -> Unit,
) {
    val isEn = language == AppLanguage.EN
    val selectedRecordingIds = remember { mutableStateListOf<Long>() }
    val selectedDeviceIds = remember { mutableStateListOf<Long>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioColors.CardSurface,
        titleContentColor = StudioColors.TextPrimary,
        textContentColor = StudioColors.TextSecondary,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = t(language, "batch_pick_library"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = StudioColors.Cyan,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isScanning) {
                    LinearProgressIndicator(
                        color = StudioColors.Cyan,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (recordings.isEmpty() && deviceAudios.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (isEn) "No recordings or device audio files found yet." else "لا توجد تسجيلات أو ملفات صوتية مكتشفة حالياً.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StudioColors.TextMuted,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (recordings.isNotEmpty()) {
                            item(key = "hdr_recordings") {
                                Text(
                                    text = if (isEn) "Studio Recordings" else "تسجيلات ومشاريع الاستوديو",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = StudioColors.Violet,
                                )
                            }
                            items(recordings, key = { "rec_${it.id}" }) { rec ->
                                val checked = rec.id in selectedRecordingIds
                                Surface(
                                    onClick = {
                                        if (checked) selectedRecordingIds.remove(rec.id) else selectedRecordingIds.add(rec.id)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (checked) StudioColors.Violet.copy(alpha = 0.14f) else StudioColors.BgTop,
                                    border = BorderStroke(1.dp, if (checked) StudioColors.Violet else StudioColors.Border),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Checkbox(
                                            checked = checked,
                                            onCheckedChange = {
                                                if (checked) selectedRecordingIds.remove(rec.id) else selectedRecordingIds.add(rec.id)
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = StudioColors.Violet),
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = rec.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = StudioColors.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = formatDurationMs(rec.durationMs),
                                                style = TelemetryTextStyle,
                                                color = StudioColors.TextMuted,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (deviceAudios.isNotEmpty()) {
                            item(key = "hdr_device") {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (isEn) "Device Audio Files" else "ملفات الصوت على الجهاز",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = StudioColors.Cyan,
                                )
                            }
                            items(deviceAudios, key = { "dev_${it.id}" }) { dev ->
                                val checked = dev.id in selectedDeviceIds
                                Surface(
                                    onClick = {
                                        if (checked) selectedDeviceIds.remove(dev.id) else selectedDeviceIds.add(dev.id)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (checked) StudioColors.Cyan.copy(alpha = 0.14f) else StudioColors.BgTop,
                                    border = BorderStroke(1.dp, if (checked) StudioColors.Cyan else StudioColors.Border),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Checkbox(
                                            checked = checked,
                                            onCheckedChange = {
                                                if (checked) selectedDeviceIds.remove(dev.id) else selectedDeviceIds.add(dev.id)
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = StudioColors.Cyan),
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = dev.displayName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = StudioColors.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = formatDurationMs(dev.durationMs),
                                                    style = TelemetryTextStyle,
                                                    color = StudioColors.TextMuted,
                                                )
                                                Text(
                                                    text = dev.folderName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = StudioColors.TextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val totalSelected = selectedRecordingIds.size + selectedDeviceIds.size
            Button(
                enabled = totalSelected > 0,
                onClick = {
                    val recList = recordings.filter { it.id in selectedRecordingIds }
                    val devList = deviceAudios.filter { it.id in selectedDeviceIds }
                    onConfirm(recList, devList)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioColors.Cyan,
                    contentColor = StudioColors.BgTop,
                ),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("batch_library_confirm_button"),
            ) {
                Text(
                    text = if (isEn) "Add ($totalSelected)" else "إضافة ($totalSelected)",
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            ) {
                Text(
                    text = if (isEn) "Cancel" else "إلغاء",
                    color = StudioColors.TextSecondary,
                )
            }
        },
    )
}
