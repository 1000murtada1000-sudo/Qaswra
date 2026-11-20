package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.formatDurationMs
import com.example.i18n.t
import com.example.model.AppLanguage
import com.example.model.BUILT_IN_PRESETS
import com.example.model.ExportAudioFormat
import com.example.model.GLOBAL_PRO_PRESETS
import com.example.model.StudioPreset
import com.example.studio.ui.lit
import com.example.ui.components.ExportFormatDialog
import com.example.ui.components.SaveDraftDialog
import com.example.ui.components.StudioFocusTopBar
import com.example.ui.components.StudioPreviewControlsBar
import com.example.ui.components.StudioScrollColumn
import com.example.ui.components.UndoRedoActionsRow
import com.example.ui.theme.StudioColors
import com.example.ui.theme.TelemetryTextStyle
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel

@Composable
fun PresetsScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onOpenEchoSpace: () -> Unit,
    onBack: () -> Unit,
) {
    val lang = uiState.language
    var saveDialogOpen by remember { mutableStateOf(false) }
    var saveDraftDialogOpen by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }
    var newPresetDesc by remember { mutableStateOf("") }

    StudioScrollColumn {
        StudioFocusTopBar(
            title = t(lang, "section_presets_title"),
            subtitle = t(lang, "section_presets_subtitle"),
            language = lang,
            onBack = onBack,
            trailing = {
                UndoRedoActionsRow(
                    canUndo = uiState.canUndoEdit,
                    canRedo = uiState.canRedoEdit,
                    language = lang,
                    undoTag = "presets_top_undo_button",
                    redoTag = "presets_top_redo_button",
                    onUndo = { viewModel.undoLastEdit() },
                    onRedo = { viewModel.redoLastEdit() },
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val openSaveInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = { saveDialogOpen = true },
                interactionSource = openSaveInteraction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioColors.Cyan,
                    contentColor = StudioColors.BgTop,
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .lit(openSaveInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                    .testTag("preset_open_save_dialog_button"),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = t(lang, "save_as_custom_preset_btn"),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val openEchoInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = onOpenEchoSpace,
                interactionSource = openEchoInteraction,
                border = BorderStroke(1.dp, StudioColors.Violet.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioColors.Violet),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .lit(openEchoInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                    .testTag("preset_open_echo_3d_button"),
            ) {
                Icon(Icons.Rounded.Waves, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (lang == AppLanguage.EN) "3D Echo" else "الصدى 3D",
                    maxLines = 1,
                )
            }
        }

        // Global Pro Presets Section (5 Studio Non-Destructive Live Presets)
        Text(
            text = t(lang, "pro_presets_title"),
            style = MaterialTheme.typography.titleMedium,
            color = StudioColors.Cyan,
        )

        GLOBAL_PRO_PRESETS.forEach { proPreset ->
            val presetTitle = t(lang, proPreset.titleKey)
            val isSelected = uiState.selectedPresetLabel == presetTitle && !uiState.dspConfig.bypassAll
            val proPresetInteraction = remember { MutableInteractionSource() }
            Card(
                onClick = { viewModel.applyGlobalProPreset(proPreset, bakeOffline = false) },
                interactionSource = proPresetInteraction,
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) StudioColors.ElevatedSurface else StudioColors.CardSurface,
                ),
                border = BorderStroke(
                    if (isSelected) 1.5.dp else 1.dp,
                    if (isSelected) StudioColors.Cyan else StudioColors.Cyan.copy(alpha = 0.38f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .lit(proPresetInteraction, MaterialTheme.colorScheme.primary, selected = isSelected, cornerRadius = 18.dp)
                    .testTag("global_pro_preset_${proPreset.id.name}"),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = presetTitle,
                        tint = StudioColors.Cyan,
                        modifier = Modifier.size(28.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = presetTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = StudioColors.TextPrimary,
                        )
                        Text(
                            text = t(lang, proPreset.descKey),
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioColors.TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = StudioColors.Cyan,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        if (uiState.customPresets.isNotEmpty()) {
            Text(
                text = t(lang, "custom_presets"),
                style = MaterialTheme.typography.titleMedium,
                color = StudioColors.Violet,
            )
            uiState.customPresets.forEach { preset ->
                PresetRowCard(
                    preset = preset,
                    isActive = uiState.activePresetId == preset.id,
                    lang = lang,
                    onApply = { viewModel.applyPreset(preset) },
                    onDelete = { viewModel.deleteCustomPreset(preset.id) },
                )
            }
        }

        Text(
            text = t(lang, "built_in_presets"),
            style = MaterialTheme.typography.titleMedium,
            color = StudioColors.Emerald,
        )
        BUILT_IN_PRESETS.forEach { preset ->
            PresetRowCard(
                preset = preset,
                isActive = uiState.activePresetId == preset.id,
                lang = lang,
                onApply = { viewModel.applyPreset(preset) },
                onDelete = null,
            )
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

    if (saveDialogOpen) {
        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = StudioColors.Cyan,
            unfocusedBorderColor = StudioColors.Border,
            focusedTextColor = StudioColors.TextPrimary,
            unfocusedTextColor = StudioColors.TextPrimary,
        )
        AlertDialog(
            onDismissRequest = { saveDialogOpen = false },
            containerColor = StudioColors.CardSurface,
            title = {
                Text(
                    text = t(lang, "save_custom_preset"),
                    style = MaterialTheme.typography.titleMedium,
                    color = StudioColors.TextPrimary,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPresetName,
                        onValueChange = { newPresetName = it },
                        singleLine = true,
                        label = { Text(t(lang, "preset_name_hint")) },
                        colors = fieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("preset_name_input"),
                    )
                    OutlinedTextField(
                        value = newPresetDesc,
                        onValueChange = { newPresetDesc = it },
                        singleLine = true,
                        label = { Text(t(lang, "preset_desc_hint")) },
                        colors = fieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("preset_desc_input"),
                    )
                }
            },
            confirmButton = {
                val confirmInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        viewModel.saveCustomPreset(
                            name = newPresetName.trim(),
                            description = newPresetDesc.trim().ifEmpty {
                                if (lang == AppLanguage.EN) "Custom Sonix Rack" else "إعدادات Sonix مخصصة"
                            },
                        )
                        newPresetName = ""
                        newPresetDesc = ""
                        saveDialogOpen = false
                    },
                    enabled = newPresetName.isNotBlank(),
                    interactionSource = confirmInteraction,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioColors.Cyan, contentColor = StudioColors.BgTop),
                    modifier = Modifier
                        .lit(confirmInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 12.dp)
                        .testTag("preset_save_custom_button"),
                ) {
                    Text(t(lang, "confirm"))
                }
            },
            dismissButton = {
                val dismissInteraction = remember { MutableInteractionSource() }
                TextButton(
                    onClick = { saveDialogOpen = false },
                    interactionSource = dismissInteraction,
                    modifier = Modifier.lit(dismissInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 12.dp),
                ) {
                    Text(t(lang, "cancel"), color = StudioColors.TextSecondary)
                }
            },
        )
    }
}

@Composable
private fun PresetRowCard(
    preset: StudioPreset,
    isActive: Boolean,
    lang: AppLanguage,
    onApply: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val activeCount = preset.config.chain.count { it.enabled }
    val cardInteraction = remember { MutableInteractionSource() }
    Card(
        onClick = onApply,
        interactionSource = cardInteraction,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) StudioColors.ElevatedSurface else StudioColors.CardSurface,
        ),
        border = BorderStroke(1.dp, if (isActive) StudioColors.Cyan else StudioColors.Border),
        modifier = Modifier
            .fillMaxWidth()
            .lit(cardInteraction, MaterialTheme.colorScheme.primary, selected = isActive, cornerRadius = 18.dp)
            .testTag("preset_card_${preset.id}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = preset.name,
                tint = if (isActive) StudioColors.Cyan else StudioColors.Violet,
                modifier = Modifier.size(26.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = StudioColors.TextPrimary,
                )
                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (lang == AppLanguage.EN) "$activeCount effects" else "$activeCount مؤثر",
                    style = TelemetryTextStyle.copy(fontSize = 11.sp),
                    color = StudioColors.Cyan,
                )
            }
            if (isActive) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = StudioColors.Cyan,
                    modifier = Modifier.size(22.dp),
                )
            }
            if (onDelete != null) {
                val deleteInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = onDelete,
                    interactionSource = deleteInteraction,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = StudioColors.RoseBright),
                    modifier = Modifier
                        .size(48.dp)
                        .lit(deleteInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 24.dp)
                        .testTag("preset_delete_${preset.id}"),
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = t(lang, "delete"))
                }
            }
        }
    }
}

@Composable
fun ProjectsExportScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onBack: () -> Unit,
) {
    val lang = uiState.language
    var exportDialogOpen by remember { mutableStateOf(false) }
    var pendingDraftDeletion by remember { mutableStateOf<Pair<String, String>?>(null) }

    StudioScrollColumn {
        StudioFocusTopBar(
            title = t(lang, "section_projects_title"),
            subtitle = t(lang, "section_projects_subtitle"),
            language = lang,
            onBack = onBack,
        )

        // Active Project Name + Save / New Project
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
                    text = t(lang, "active_project"),
                    style = MaterialTheme.typography.titleMedium,
                    color = StudioColors.TextPrimary,
                )

                OutlinedTextField(
                    value = uiState.activeProjectName,
                    onValueChange = { viewModel.setActiveProjectName(it) },
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
                        .testTag("project_name_input"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val saveProjInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = { viewModel.saveCurrentProject() },
                        enabled = !uiState.isBusy,
                        interactionSource = saveProjInteraction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioColors.Emerald,
                            contentColor = StudioColors.BgTop,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(saveProjInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                            .testTag("project_save_button"),
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(t(lang, "save_project"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    val newProjInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = { viewModel.newProject() },
                        interactionSource = newProjInteraction,
                        border = BorderStroke(1.dp, StudioColors.BorderBright),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(newProjInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                            .testTag("project_new_button"),
                    ) {
                        Icon(Icons.Rounded.NoteAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(t(lang, "new_project"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // Mastered Export Card
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
                    text = if (lang == AppLanguage.EN) "Mastered Audio Export" else "التصدير الاحترافي بالمعالجة الكاملة",
                    style = MaterialTheme.typography.titleMedium,
                    color = StudioColors.TextPrimary,
                )

                uiState.exportProgress?.let { prog ->
                    Text(
                        text = t(lang, "exporting_progress"),
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioColors.Cyan,
                    )
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val m4aInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = {
                            viewModel.exportRenderedAudio(
                                ExportAudioFormat.M4A,
                                uiState.activeProjectName.ifBlank { "Sonix_Master" },
                                shareAfter = false,
                            )
                        },
                        enabled = uiState.hasAudio && !uiState.isBusy,
                        interactionSource = m4aInteraction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioColors.Cyan,
                            contentColor = StudioColors.BgTop,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(m4aInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                            .testTag("export_quick_m4a_button"),
                    ) {
                        Icon(Icons.Rounded.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(t(lang, "export_mp3"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    val wavInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = {
                            viewModel.exportRenderedAudio(
                                ExportAudioFormat.WAV,
                                uiState.activeProjectName.ifBlank { "Sonix_Master" },
                                shareAfter = false,
                            )
                        },
                        enabled = uiState.hasAudio && !uiState.isBusy,
                        interactionSource = wavInteraction,
                        border = BorderStroke(1.dp, StudioColors.Emerald),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioColors.Emerald),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(wavInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                            .testTag("export_quick_wav_button"),
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(t(lang, "export_wav"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                val openExportInteraction = remember { MutableInteractionSource() }
                OutlinedButton(
                    onClick = { exportDialogOpen = true },
                    enabled = uiState.hasAudio && !uiState.isBusy,
                    interactionSource = openExportInteraction,
                    border = BorderStroke(1.dp, StudioColors.Violet),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioColors.Violet),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .lit(openExportInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                        .testTag("projects_open_export_dialog_button"),
                ) {
                    Text(t(lang, "save_export_final"))
                }
            }
        }

        // Cloud Drafts (Firebase Firestore per authenticated Google UID)
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDone,
                        contentDescription = null,
                        tint = StudioColors.Violet,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = t(lang, "cloud_drafts_title"),
                        style = MaterialTheme.typography.titleMedium,
                        color = StudioColors.TextPrimary,
                    )
                }

                if (uiState.isCloudSyncing) {
                    Text(
                        text = uiState.cloudSyncLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioColors.Cyan,
                    )
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = StudioColors.Cyan,
                    )
                }

                if (!uiState.isCloudSynced) {
                    Text(
                        text = t(lang, "cloud_drafts_signin_hint"),
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioColors.TextSecondary,
                    )
                    val cloudSignInInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = { viewModel.requestSignIn() },
                        interactionSource = cloudSignInInteraction,
                        border = BorderStroke(1.dp, StudioColors.Cyan),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioColors.Cyan),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(cloudSignInInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                            .testTag("cloud_drafts_signin_button"),
                    ) {
                        Text(t(lang, "auth_google_signin"))
                    }
                } else {
                    val saveDraftInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = { viewModel.saveCloudDraft(uiState.activeProjectName.ifBlank { t(lang, "untitled_project") }) },
                        enabled = !uiState.isBusy,
                        interactionSource = saveDraftInteraction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioColors.Violet.copy(alpha = 0.22f),
                            contentColor = StudioColors.Violet,
                        ),
                        border = BorderStroke(1.dp, StudioColors.Violet.copy(alpha = 0.65f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(saveDraftInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                            .testTag("cloud_draft_save_current_button"),
                    ) {
                        Text(t(lang, "save_to_drafts"))
                    }

                    if (uiState.cloudDrafts.isEmpty()) {
                        Text(
                            text = t(lang, "cloud_drafts_empty"),
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioColors.TextSecondary,
                        )
                    } else {
                        uiState.cloudDrafts.forEach { draft ->
                            val isActiveDraft = draft.docId == uiState.activeCloudDraftId
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isActiveDraft) StudioColors.ElevatedSurface else StudioColors.BgMid,
                                border = BorderStroke(
                                    1.dp,
                                    if (isActiveDraft) StudioColors.Violet else StudioColors.Border,
                                ),
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
                                            text = draft.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = StudioColors.TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (draft.selectedPresetName.isNotBlank()) {
                                            Text(
                                                text = draft.selectedPresetName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = StudioColors.Violet,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        Text(
                                            text = "${formatDurationMs(draft.durationMs)} • ${draft.sampleRate} Hz",
                                            style = TelemetryTextStyle.copy(fontSize = 11.sp),
                                            color = StudioColors.TextSecondary,
                                        )
                                    }

                                    val loadDraftInteraction = remember { MutableInteractionSource() }
                                    IconButton(
                                        onClick = { viewModel.loadCloudDraft(draft) },
                                        interactionSource = loadDraftInteraction,
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = StudioColors.ElevatedSurface,
                                            contentColor = StudioColors.Cyan,
                                        ),
                                        modifier = Modifier
                                            .size(48.dp)
                                            .lit(loadDraftInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 24.dp)
                                            .testTag("cloud_draft_open_${draft.docId}"),
                                    ) {
                                        Icon(Icons.Rounded.FolderOpen, contentDescription = t(lang, "load"))
                                    }

                                    val deleteDraftInteraction = remember { MutableInteractionSource() }
                                    IconButton(
                                        onClick = { pendingDraftDeletion = draft.docId to draft.name },
                                        interactionSource = deleteDraftInteraction,
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = StudioColors.ElevatedSurface,
                                            contentColor = StudioColors.RoseBright,
                                        ),
                                        modifier = Modifier
                                            .size(48.dp)
                                            .lit(deleteDraftInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 24.dp)
                                            .testTag("cloud_draft_delete_${draft.docId}"),
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

        // Saved Projects List
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
                    text = t(lang, "saved_projects"),
                    style = MaterialTheme.typography.titleMedium,
                    color = StudioColors.TextPrimary,
                )

                if (uiState.projects.isEmpty()) {
                    Text(
                        text = if (lang == AppLanguage.EN) "No saved projects yet." else "لا توجد مشاريع محفوظة بعد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioColors.TextSecondary,
                    )
                } else {
                    uiState.projects.forEach { proj ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (proj.id == uiState.activeProjectId) StudioColors.ElevatedSurface else StudioColors.BgMid,
                            border = BorderStroke(
                                1.dp,
                                if (proj.id == uiState.activeProjectId) StudioColors.Cyan else StudioColors.Border,
                            ),
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
                                        text = proj.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = StudioColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${formatDurationMs(proj.durationMs)} • ${proj.sampleRate} Hz",
                                        style = TelemetryTextStyle.copy(fontSize = 11.sp),
                                        color = StudioColors.TextSecondary,
                                    )
                                }

                                val openProjInteraction = remember { MutableInteractionSource() }
                                IconButton(
                                    onClick = { viewModel.loadProject(proj) },
                                    interactionSource = openProjInteraction,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = StudioColors.ElevatedSurface,
                                        contentColor = StudioColors.Cyan,
                                    ),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .lit(openProjInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 24.dp)
                                        .testTag("project_open_${proj.id}"),
                                ) {
                                    Icon(Icons.Rounded.FolderOpen, contentDescription = t(lang, "load"))
                                }

                                val deleteProjInteraction = remember { MutableInteractionSource() }
                                IconButton(
                                    onClick = { viewModel.deleteProject(proj) },
                                    interactionSource = deleteProjInteraction,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = StudioColors.ElevatedSurface,
                                        contentColor = StudioColors.RoseBright,
                                    ),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .lit(deleteProjInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 24.dp)
                                        .testTag("project_delete_${proj.id}"),
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

    pendingDraftDeletion?.let { (draftId, draftName) ->
        AlertDialog(
            onDismissRequest = { pendingDraftDeletion = null },
            title = {
                Text(if (lang == AppLanguage.EN) "Delete cloud draft?" else "حذف المسودة السحابية؟")
            },
            text = {
                Text(
                    if (lang == AppLanguage.EN) {
                        "This will permanently delete \"$draftName\" from your cloud drafts."
                    } else {
                        "سيؤدي هذا إلى حذف \"$draftName\" نهائياً من مسوداتك السحابية."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDraftDeletion = null
                        viewModel.deleteCloudDraft(draftId)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = StudioColors.RoseBright),
                ) {
                    Text(if (lang == AppLanguage.EN) "Delete" else "حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDraftDeletion = null }) {
                    Text(if (lang == AppLanguage.EN) "Cancel" else "إلغاء")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = StudioColors.CardSurface,
            titleContentColor = StudioColors.TextPrimary,
            textContentColor = StudioColors.TextSecondary,
        )
    }

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
