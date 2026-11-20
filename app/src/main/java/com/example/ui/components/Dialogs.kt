package com.example.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.i18n.t
import com.example.model.AppLanguage
import com.example.model.ExportAudioFormat
import com.example.model.SleepTimerOption
import com.example.studio.ui.lit
import com.example.ui.theme.StudioColors
import com.example.ui.theme.TelemetryTextStyle

@Composable
fun LanguageBadgeButton(
    currentLanguage: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(999.dp),
        color = StudioColors.ElevatedSurface.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, StudioColors.Cyan.copy(alpha = 0.45f)),
        modifier = modifier
            .then(if (compact) Modifier.height(34.dp) else Modifier.defaultMinSize(minHeight = 48.dp))
            .lit(
                interaction,
                MaterialTheme.colorScheme.primary,
                cornerRadius = if (compact) 17.dp else 24.dp,
            )
            .testTag("language_badge_button"),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 12.dp,
                vertical = if (compact) 4.dp else 8.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Language,
                contentDescription = t(currentLanguage, "app_language_title"),
                tint = StudioColors.Cyan,
                modifier = Modifier.size(if (compact) 14.dp else 18.dp),
            )
            Text(
                text = currentLanguage.shortBadge,
                style = if (compact) {
                    MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp)
                } else {
                    MaterialTheme.typography.labelLarge
                },
                color = StudioColors.TextPrimary,
            )
        }
    }
}

@Composable
fun LanguageSelectorDialog(
    open: Boolean,
    currentLanguage: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!open) return

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = StudioColors.CardSurface,
            border = BorderStroke(1.dp, StudioColors.BorderBright),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = t(currentLanguage, "choose_language_title"),
                    style = MaterialTheme.typography.headlineSmall,
                    color = StudioColors.TextPrimary,
                )
                Text(
                    text = t(currentLanguage, "language_direction_hint"),
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioColors.TextSecondary,
                )

                AppLanguage.entries.forEach { lang ->
                    val selected = lang == currentLanguage
                    val langInteraction = remember { MutableInteractionSource() }
                    Card(
                        onClick = {
                            onSelect(lang)
                            onDismiss()
                        },
                        interactionSource = langInteraction,
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) StudioColors.ElevatedSurface else StudioColors.BgMid,
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selected) StudioColors.Cyan else StudioColors.Border,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .lit(langInteraction, MaterialTheme.colorScheme.primary, selected = selected, cornerRadius = 16.dp)
                            .testTag("lang_option_${lang.tag}"),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = lang.nativeName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = StudioColors.TextPrimary,
                                )
                                Text(
                                    text = "${lang.shortBadge} • ${if (lang.isRtl) "RTL" else "LTR"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StudioColors.TextSecondary,
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = lang.nativeName,
                                    tint = StudioColors.Cyan,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    val cancelInteraction = remember { MutableInteractionSource() }
                    TextButton(
                        onClick = onDismiss,
                        interactionSource = cancelInteraction,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(cancelInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 12.dp)
                            .testTag("lang_dialog_cancel"),
                    ) {
                        Text(t(currentLanguage, "cancel"), color = StudioColors.TextSecondary)
                    }
                }
            }
        }
    }
}

private data class QuickShareAppTarget(
    val id: String,
    val labelKey: String,
    val packageName: String?,
    val icon: ImageVector,
    val accent: Color,
)

@Composable
fun ExportFormatDialog(
    open: Boolean,
    language: AppLanguage,
    initialFileName: String,
    onDismiss: () -> Unit,
    onSaveToDevice: (ExportAudioFormat, String) -> Unit,
    onShareAudio: (ExportAudioFormat, String) -> Unit,
    onShareToPackage: ((ExportAudioFormat, String, String?) -> Unit)? = null,
    onSaveForUnity: ((String) -> Unit)? = null,
    onShareForUnity: ((String) -> Unit)? = null,
) {
    if (!open) return

    var selectedFormat by remember { mutableStateOf(ExportAudioFormat.MP3) }
    var isUnitySelected by remember(open) { mutableStateOf(false) }
    var unityInfoOpen by remember(open) { mutableStateOf(false) }
    var fileName by remember(initialFileName, open) { mutableStateOf(initialFileName) }

    LaunchedEffect(initialFileName, open) {
        if (open) {
            fileName = initialFileName
        }
    }

    val orderedFormats = remember {
        listOf(
            ExportAudioFormat.MP3,
            ExportAudioFormat.WAV,
            ExportAudioFormat.M4A,
            ExportAudioFormat.FLAC,
            ExportAudioFormat.OGG,
        )
    }

    val shareTargets = remember {
        listOf(
            QuickShareAppTarget("whatsapp", "share_whatsapp", "com.whatsapp", Icons.Rounded.Send, StudioColors.Emerald),
            QuickShareAppTarget("telegram", "share_telegram", "org.telegram.messenger", Icons.Rounded.Send, StudioColors.Cyan),
            QuickShareAppTarget("instagram", "share_instagram", "com.instagram.android", Icons.Rounded.PhotoCamera, StudioColors.RoseBright),
            QuickShareAppTarget("capcut", "share_capcut", "com.lemon.lvoverseas", Icons.Rounded.MovieCreation, StudioColors.Amber),
            QuickShareAppTarget("more", "share_more_apps", null, Icons.Rounded.Share, StudioColors.Violet),
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = StudioColors.CardSurface,
            border = BorderStroke(1.dp, StudioColors.BorderBright),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = t(language, "save_export_final"),
                        style = MaterialTheme.typography.headlineSmall,
                        color = StudioColors.TextPrimary,
                    )
                    Text(
                        text = t(language, "quick_share_card_subtitle"),
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioColors.TextSecondary,
                    )
                }

                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    singleLine = true,
                    label = {
                        Text(
                            when (language) {
                                AppLanguage.EN -> "File Name"
                                AppLanguage.FA -> "نام فایل"
                                AppLanguage.TR -> "Dosya adı"
                                AppLanguage.AR -> "اسم الملف"
                            },
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioColors.Cyan,
                        unfocusedBorderColor = StudioColors.Border,
                        focusedTextColor = StudioColors.TextPrimary,
                        unfocusedTextColor = StudioColors.TextPrimary,
                        focusedLabelColor = StudioColors.Cyan,
                        unfocusedLabelColor = StudioColors.TextSecondary,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_filename_input"),
                )

                orderedFormats.forEach { fmt ->
                    val selected = !isUnitySelected && fmt == selectedFormat
                    val badgeText = when (fmt) {
                        ExportAudioFormat.MP3 -> t(language, "format_mp3_badge")
                        ExportAudioFormat.WAV -> t(language, "format_wav_badge")
                        ExportAudioFormat.M4A -> t(language, "format_m4a_badge")
                        else -> null
                    }
                    val badgeColor = when (fmt) {
                        ExportAudioFormat.MP3 -> StudioColors.Emerald
                        ExportAudioFormat.WAV -> StudioColors.Violet
                        ExportAudioFormat.M4A -> StudioColors.Cyan
                        else -> StudioColors.TextSecondary
                    }
                    val fmtInteraction = remember { MutableInteractionSource() }
                    Card(
                        onClick = {
                            selectedFormat = fmt
                            isUnitySelected = false
                        },
                        interactionSource = fmtInteraction,
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) StudioColors.ElevatedSurface else StudioColors.BgMid,
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selected) StudioColors.Cyan else StudioColors.Border,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .lit(fmtInteraction, MaterialTheme.colorScheme.primary, selected = selected, cornerRadius = 14.dp)
                            .testTag("export_format_${fmt.extension}"),
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
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = "${fmt.displayName} (.${fmt.extension})",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (selected) StudioColors.Cyan else StudioColors.TextPrimary,
                                    )
                                    if (badgeText != null) {
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = badgeColor.copy(alpha = 0.16f),
                                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f)),
                                        ) {
                                            Text(
                                                text = badgeText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = badgeColor,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = fmt.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StudioColors.TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = fmt.displayName,
                                    tint = StudioColors.Cyan,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }

                if (onSaveForUnity != null || onShareForUnity != null) {
                    val unityInteraction = remember { MutableInteractionSource() }
                    Card(
                        onClick = { isUnitySelected = true },
                        interactionSource = unityInteraction,
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUnitySelected) StudioColors.ElevatedSurface else StudioColors.BgMid,
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isUnitySelected) StudioColors.Amber else StudioColors.Amber.copy(alpha = 0.45f),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .lit(unityInteraction, StudioColors.Amber, selected = isUnitySelected, cornerRadius = 14.dp)
                            .testTag("export_format_unity"),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SportsEsports,
                                contentDescription = null,
                                tint = StudioColors.Amber,
                                modifier = Modifier.size(22.dp),
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
                                        text = t(language, "unity_export_title"),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isUnitySelected) StudioColors.Amber else StudioColors.TextPrimary,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                }
                                Text(
                                    text = t(language, "unity_export_subtitle"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StudioColors.TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            InfoBadgeButton(
                                contentDescription = t(language, "unity_export_info_title"),
                                testTag = "unity_export_info_button",
                                accentColor = StudioColors.Amber,
                                onClick = { unityInfoOpen = true },
                            )
                            if (isUnitySelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = t(language, "unity_export_badge"),
                                    tint = StudioColors.Amber,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }

                // 1-Tap Quick Share Directly To Creator Apps
                if (!isUnitySelected) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = t(language, "share_direct_apps"),
                            style = MaterialTheme.typography.labelLarge,
                            color = StudioColors.TextPrimary,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            shareTargets.forEach { target ->
                                val cleanOutName = fileName.ifBlank { initialFileName }
                                val targetInteraction = remember { MutableInteractionSource() }
                                Surface(
                                    onClick = {
                                        onDismiss()
                                        if (onShareToPackage != null) {
                                            onShareToPackage(selectedFormat, cleanOutName, target.packageName)
                                        } else {
                                            onShareAudio(selectedFormat, cleanOutName)
                                        }
                                    },
                                    interactionSource = targetInteraction,
                                    shape = RoundedCornerShape(14.dp),
                                    color = target.accent.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, target.accent.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .lit(targetInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                                        .testTag("quick_share_${target.id}"),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            imageVector = target.icon,
                                            contentDescription = t(language, target.labelKey),
                                            tint = target.accent,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            text = t(language, target.labelKey),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = StudioColors.TextPrimary,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val cleanOutName = fileName.ifBlank { initialFileName }
                    val saveBtnInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = {
                            onDismiss()
                            if (isUnitySelected && onSaveForUnity != null) {
                                onSaveForUnity(cleanOutName)
                            } else {
                                onSaveToDevice(selectedFormat, cleanOutName)
                            }
                        },
                        interactionSource = saveBtnInteraction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isUnitySelected) StudioColors.Amber else StudioColors.Cyan,
                            contentColor = StudioColors.BgTop,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(
                                saveBtnInteraction,
                                if (isUnitySelected) StudioColors.Amber else MaterialTheme.colorScheme.primary,
                                cornerRadius = 14.dp,
                            )
                            .testTag(if (isUnitySelected) "unity_export_approve_save_button" else "export_confirm_save_button"),
                    ) {
                        Icon(
                            imageVector = if (isUnitySelected) Icons.Rounded.Code else Icons.Rounded.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isUnitySelected) t(language, "approve_and_start_processing") else t(language, "save_export_audio"),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val shareBtnInteraction = remember { MutableInteractionSource() }
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                if (isUnitySelected && onShareForUnity != null) {
                                    onShareForUnity(cleanOutName)
                                } else {
                                    onShareAudio(selectedFormat, cleanOutName)
                                }
                            },
                            interactionSource = shareBtnInteraction,
                            border = BorderStroke(1.dp, if (isUnitySelected) StudioColors.Amber else StudioColors.Emerald),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isUnitySelected) StudioColors.Amber else StudioColors.Emerald,
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .lit(
                                    shareBtnInteraction,
                                    if (isUnitySelected) StudioColors.Amber else MaterialTheme.colorScheme.primary,
                                    cornerRadius = 14.dp,
                                )
                                .testTag("export_confirm_share_button"),
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isUnitySelected) t(language, "unity_share_bundle_btn") else t(language, "share_audio"),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        val cancelBtnInteraction = remember { MutableInteractionSource() }
                        TextButton(
                            onClick = onDismiss,
                            interactionSource = cancelBtnInteraction,
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .lit(cancelBtnInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 12.dp)
                                .testTag("export_dialog_cancel"),
                        ) {
                            Text(t(language, "cancel"), color = StudioColors.TextSecondary)
                        }
                    }
                }
            }
        }
    }

    ToolInfoDialog(
        open = unityInfoOpen,
        title = t(language, "unity_export_info_title"),
        description = t(language, "unity_export_info_body"),
        language = language,
        onDismiss = { unityInfoOpen = false },
    )
}

@Composable
fun SleepTimerDialog(
    open: Boolean,
    language: AppLanguage,
    selectedOption: SleepTimerOption,
    remainingSec: Int,
    onSelect: (SleepTimerOption) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!open) return

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = StudioColors.CardSurface,
            border = BorderStroke(1.dp, StudioColors.BorderBright),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bedtime,
                        contentDescription = t(language, "sleep_timer_title"),
                        tint = StudioColors.Violet,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = t(language, "sleep_timer_title"),
                        style = MaterialTheme.typography.headlineSmall,
                        color = StudioColors.TextPrimary,
                    )
                }

                Text(
                    text = t(language, "sleep_timer_subtitle"),
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioColors.TextSecondary,
                )

                if (remainingSec > 0) {
                    val mm = remainingSec / 60
                    val ss = remainingSec % 60
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StudioColors.Violet.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, StudioColors.Violet.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = String.format("%02d:%02d", mm, ss),
                            style = TelemetryTextStyle,
                            color = StudioColors.Violet,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }

                SleepTimerOption.entries.forEach { option ->
                    val selected = option == selectedOption && (option != SleepTimerOption.OFF || remainingSec == 0)
                    val optInteraction = remember { MutableInteractionSource() }
                    Card(
                        onClick = {
                            onSelect(option)
                            onDismiss()
                        },
                        interactionSource = optInteraction,
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) StudioColors.ElevatedSurface else StudioColors.BgMid,
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selected) StudioColors.Violet else StudioColors.Border,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 50.dp)
                            .lit(optInteraction, MaterialTheme.colorScheme.primary, selected = selected, cornerRadius = 14.dp)
                            .testTag("sleep_timer_option_${option.name.lowercase()}"),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = t(language, option.labelKey),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (selected) StudioColors.Violet else StudioColors.TextPrimary,
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = t(language, option.labelKey),
                                    tint = StudioColors.Violet,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    val sleepCancelInteraction = remember { MutableInteractionSource() }
                    TextButton(
                        onClick = onDismiss,
                        interactionSource = sleepCancelInteraction,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(sleepCancelInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 12.dp),
                    ) {
                        Text(t(language, "cancel"), color = StudioColors.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun ToolInfoDialog(
    open: Boolean,
    title: String,
    description: String,
    language: AppLanguage,
    onDismiss: () -> Unit,
) {
    if (!open) return

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = StudioColors.CardSurface,
            border = BorderStroke(1.dp, StudioColors.Cyan.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = title,
                        tint = StudioColors.Cyan,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = StudioColors.TextPrimary,
                    )
                }

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StudioColors.TextSecondary,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    val closeInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = onDismiss,
                        interactionSource = closeInteraction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioColors.Cyan,
                            contentColor = StudioColors.BgTop,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(closeInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 12.dp)
                            .testTag("tool_info_close_button"),
                    ) {
                        Text(t(language, "tool_info_close"), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun SaveDraftDialog(
    open: Boolean,
    language: AppLanguage,
    initialName: String,
    activePresetLabel: String,
    isCloudSignedIn: Boolean,
    onDismiss: () -> Unit,
    onSaveDraft: (String) -> Unit,
) {
    if (!open) return

    var draftName by remember(initialName, open) { mutableStateOf(initialName) }
    LaunchedEffect(initialName, open) {
        if (open) {
            draftName = initialName
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = StudioColors.CardSurface,
            border = BorderStroke(1.dp, StudioColors.BorderBright),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudUpload,
                        contentDescription = null,
                        tint = StudioColors.Cyan,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = t(language, "save_to_drafts"),
                        style = MaterialTheme.typography.headlineSmall,
                        color = StudioColors.TextPrimary,
                    )
                }

                if (activePresetLabel.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StudioColors.ElevatedSurface,
                        border = BorderStroke(1.dp, StudioColors.Border),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "${t(language, "preset_label")}: $activePresetLabel",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = StudioColors.Cyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }

                if (!isCloudSignedIn) {
                    Text(
                        text = t(language, "cloud_drafts_signin_hint"),
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioColors.Amber,
                    )
                }

                OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it },
                    singleLine = true,
                    label = { Text(t(language, "draft_name_hint")) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioColors.Cyan,
                        unfocusedBorderColor = StudioColors.Border,
                        focusedTextColor = StudioColors.TextPrimary,
                        unfocusedTextColor = StudioColors.TextPrimary,
                        focusedLabelColor = StudioColors.Cyan,
                        unfocusedLabelColor = StudioColors.TextSecondary,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_draft_name_input"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val draftCancelInteraction = remember { MutableInteractionSource() }
                    TextButton(
                        onClick = onDismiss,
                        interactionSource = draftCancelInteraction,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(draftCancelInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 12.dp)
                            .testTag("save_draft_cancel_button"),
                    ) {
                        Text(t(language, "cancel"), color = StudioColors.TextSecondary)
                    }

                    Spacer(Modifier.weight(1f))

                    val draftConfirmInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = {
                            onDismiss()
                            onSaveDraft(draftName.trim().ifBlank { initialName })
                        },
                        interactionSource = draftConfirmInteraction,
                        enabled = draftName.isNotBlank() && isCloudSignedIn,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioColors.Cyan,
                            contentColor = StudioColors.BgTop,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(draftConfirmInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                            .testTag("save_draft_confirm_button"),
                    ) {
                        Text(t(language, "confirm"), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
