package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diagnostics.AppDiagnosticEntry
import com.example.diagnostics.AppDiagnosticLog
import com.example.diagnostics.DiagnosticSeverity
import com.example.i18n.t
import com.example.model.AppLanguage
import com.example.studio.ui.lit
import com.example.ui.components.StudioFocusTopBar
import com.example.ui.components.StudioScrollColumn
import com.example.ui.theme.StudioColors
import com.example.ui.theme.TelemetryTextStyle
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel

@Composable
fun NotificationsListScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onOpenDetail: (String) -> Unit,
    onBack: () -> Unit,
) {
    val lang = uiState.language
    val entries = uiState.diagnosticEntries

    StudioScrollColumn {
        StudioFocusTopBar(
            title = t(lang, "notifications_title"),
            subtitle = t(lang, "notifications_subtitle"),
            language = lang,
            onBack = onBack,
            trailing = {
                if (entries.isNotEmpty()) {
                    val clearInteraction = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = { viewModel.clearAllDiagnostics() },
                        interactionSource = clearInteraction,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = StudioColors.ElevatedSurface,
                            contentColor = StudioColors.RoseBright,
                        ),
                        modifier = Modifier
                            .size(34.dp)
                            .lit(clearInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 17.dp)
                            .testTag("notifications_clear_all"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = t(lang, "notifications_clear_all"),
                        )
                    }
                }
            },
        )

        if (entries.isEmpty()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
                border = BorderStroke(1.dp, StudioColors.Emerald.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notifications_empty_card"),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = StudioColors.Emerald,
                        modifier = Modifier.size(30.dp),
                    )
                    Text(
                        text = t(lang, "notifications_empty"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudioColors.TextPrimary,
                    )
                }
            }
        } else {
            entries.forEach { entry ->
                DiagnosticEntryCard(
                    entry = entry,
                    lang = lang,
                    onClick = {
                        viewModel.markDiagnosticRead(entry.id)
                        onOpenDetail(entry.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun DiagnosticEntryCard(
    entry: AppDiagnosticEntry,
    lang: AppLanguage,
    onClick: () -> Unit,
) {
    val isCritical = entry.severity == DiagnosticSeverity.CRITICAL
    val accent = if (isCritical) StudioColors.RoseBright else StudioColors.Amber
    val interaction = remember { MutableInteractionSource() }

    val title = AppDiagnosticLog.userFacingTitle(entry, lang)
    val cause = AppDiagnosticLog.userFacingCause(entry, lang)

    Card(
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .lit(interaction, accent, cornerRadius = 18.dp)
            .testTag("notification_entry_${entry.id}"),
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (isCritical) Icons.Rounded.ErrorOutline else Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = StudioColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accent.copy(alpha = 0.16f),
                ) {
                    Text(
                        text = if (lang == AppLanguage.EN) "Audio" else "الصوت",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                if (entry.occurrenceCount > 1) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StudioColors.Rose.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, StudioColors.RoseBright.copy(alpha = 0.55f)),
                        modifier = Modifier.testTag("notification_repeat_badge_${entry.id}"),
                    ) {
                        Text(
                            text = "×${entry.occurrenceCount}",
                            style = TelemetryTextStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold),
                            color = StudioColors.RoseBright,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Text(
                text = cause,
                style = MaterialTheme.typography.bodySmall,
                color = StudioColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = StudioColors.TextMuted,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = AppDiagnosticLog.formatTimestamp(entry.timestampMs),
                        style = TelemetryTextStyle.copy(fontSize = 11.sp),
                        color = StudioColors.TextMuted,
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationDetailScreen(
    entryId: String,
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onBack: () -> Unit,
) {
    val lang = uiState.language
    val context = LocalContext.current
    val entry = remember(entryId, uiState.diagnosticEntries) {
        uiState.diagnosticEntries.firstOrNull { it.id == entryId }
            ?: AppDiagnosticLog.findById(entryId)
    }
    LaunchedEffect(entryId) {
        viewModel.markDiagnosticRead(entryId)
    }
    var copied by remember { mutableStateOf(false) }

    StudioScrollColumn {
        StudioFocusTopBar(
            title = t(lang, "notification_detail_title"),
            subtitle = if (lang == AppLanguage.EN) "Audio alert" else "تنبيه صوتي",
            language = lang,
            onBack = onBack,
        )

        if (entry == null) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
                border = BorderStroke(1.dp, StudioColors.Border),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = t(lang, "notifications_empty"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudioColors.TextSecondary,
                    )
                }
            }
        } else {
            val isCritical = entry.severity == DiagnosticSeverity.CRITICAL
            val accent = if (isCritical) StudioColors.RoseBright else StudioColors.Amber
            val title = AppDiagnosticLog.userFacingTitle(entry, lang)
            val cause = AppDiagnosticLog.userFacingCause(entry, lang)

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BugReport,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(24.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = StudioColors.TextPrimary,
                            )
                            Text(
                                text = "${if (lang == AppLanguage.EN) "Audio" else "الصوت"} • ${AppDiagnosticLog.formatTimestamp(entry.timestampMs)}",
                                style = TelemetryTextStyle.copy(fontSize = 11.sp),
                                color = StudioColors.TextSecondary,
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StudioColors.BgMid,
                        border = BorderStroke(1.dp, StudioColors.Border),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = if (lang == AppLanguage.EN) "What happened" else "ملخص التنبيه",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = StudioColors.Cyan,
                            )
                            Text(
                                text = cause,
                                style = MaterialTheme.typography.bodyMedium,
                                color = StudioColors.TextPrimary,
                            )
                            if (entry.occurrenceCount > 1) {
                                Text(
                                    text = "${t(lang, "notifications_repeat_count")} ×${entry.occurrenceCount}",
                                    style = TelemetryTextStyle.copy(fontSize = 11.sp),
                                    color = accent,
                                )
                            }
                        }
                    }

                    val copyInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = {
                            runCatching {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(
                                    ClipData.newPlainText(
                                        "Sonix Diagnostic",
                                        AppDiagnosticLog.formatFullReport(entry),
                                    ),
                                )
                                copied = true
                                viewModel.showStatusBanner(t(lang, "notifications_copied"))
                            }
                        },
                        interactionSource = copyInteraction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioColors.Cyan,
                            contentColor = StudioColors.BgTop,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(copyInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                            .testTag("notifications_copy_code"),
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (copied) t(lang, "notifications_copied") else t(lang, "notifications_copy_code"),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }

                    val sendSupportInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = {
                            val subject = if (lang == AppLanguage.EN) {
                                "Sonix Support Summary"
                            } else {
                                "ملخص دعم تطبيق Sonix"
                            }
                            val feedback = launchSupportEmail(
                                context = context,
                                subject = subject,
                                body = AppDiagnosticLog.formatFullReport(entry),
                                lang = lang,
                            )
                            viewModel.showStatusBanner(feedback)
                        },
                        interactionSource = sendSupportInteraction,
                        border = BorderStroke(1.dp, StudioColors.Violet.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioColors.Violet),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .lit(sendSupportInteraction, StudioColors.Violet, cornerRadius = 14.dp)
                            .testTag("notifications_send_support"),
                    ) {
                        Icon(Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = t(lang, "notifications_send_support"),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}
