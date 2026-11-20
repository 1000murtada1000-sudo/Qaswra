package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.diagnostics.AppDiagnosticLog
import com.example.model.AppLanguage
import com.example.network.AppUpdateManager
import com.example.studio.ui.lit
import com.example.ui.components.StudioFocusTopBar
import com.example.ui.components.StudioScrollColumn
import com.example.ui.theme.StudioColors
import com.example.ui.theme.TelemetryTextStyle
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel
import com.example.i18n.t

private const val SUPPORT_EMAIL = "1000iraq1000@gmail.com"

@Composable
fun SupportUpdatesSection(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    collapsible: Boolean = false,
    initiallyExpanded: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val lang = uiState.language
    val context = LocalContext.current
    val currentVersion = remember(context) { AppUpdateManager.getCurrentAppVersionName(context) }

    var expanded by rememberSaveable(collapsible, initiallyExpanded) {
        mutableStateOf(if (collapsible) initiallyExpanded else true)
    }
    var supportMessage by rememberSaveable { mutableStateOf("") }
    var attachDiagnostics by rememberSaveable { mutableStateOf(true) }
    var inlineFeedback by remember { mutableStateOf<String?>(null) }

    val innerContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            inlineFeedback?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = StudioColors.Emerald.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, StudioColors.Emerald.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("support_feedback_banner"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelMedium,
                            color = StudioColors.Emerald,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { inlineFeedback = null },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Dismiss",
                                tint = StudioColors.Emerald,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            // 1. تحديثات التطبيق (App Updates)
            AppUpdateClassicCard(
                uiState = uiState,
                currentVersion = currentVersion,
                viewModel = viewModel,
                lang = lang,
                compactMode = false,
                modifier = Modifier.fillMaxWidth(),
            )

            // 2. التواصل مع الدعم (Customer Support)
            CustomerSupportClassicCard(
                lang = lang,
                supportMessage = supportMessage,
                onMessageChange = { supportMessage = it },
                attachDiagnostics = attachDiagnostics,
                onAttachChange = { attachDiagnostics = it },
                compactMode = false,
                onSendEmail = {
                    val body = buildSupportEmailBody(
                        lang = lang,
                        userMessage = supportMessage,
                        attachDiagnostics = attachDiagnostics,
                        currentVersion = currentVersion,
                    )
                    val subject = if (lang == AppLanguage.EN) {
                        "Sonix ✦ Studio Support (v$currentVersion)"
                    } else {
                        "دعم تطبيق Sonix ✦ (إصدار $currentVersion)"
                    }
                    inlineFeedback = launchSupportEmail(
                        context = context,
                        subject = subject,
                        body = body,
                        lang = lang,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (collapsible) {
        val toggleInteraction = remember { MutableInteractionSource() }
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
            border = BorderStroke(
                1.dp,
                if (expanded) StudioColors.Cyan.copy(alpha = 0.5f) else StudioColors.BorderBright,
            ),
            modifier = modifier
                .fillMaxWidth()
                .testTag("more_and_help_card"),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = { expanded = !expanded },
                    interactionSource = toggleInteraction,
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 62.dp)
                        .lit(toggleInteraction, StudioColors.Cyan, cornerRadius = 20.dp)
                        .testTag("more_and_help_toggle"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StudioColors.Cyan.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, StudioColors.Cyan.copy(alpha = 0.4f)),
                            modifier = Modifier.size(38.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.HelpOutline,
                                    contentDescription = t(lang, "more_and_help_title"),
                                    tint = StudioColors.Cyan,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = t(lang, "more_and_help_title"),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = StudioColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = t(lang, "more_and_help_subtitle"),
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = StudioColors.ElevatedSurface,
                            border = BorderStroke(1.dp, StudioColors.Border),
                        ) {
                            Text(
                                text = "v$currentVersion",
                                style = TelemetryTextStyle.copy(fontSize = 10.sp),
                                color = StudioColors.Amber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = t(lang, "more_and_help_title"),
                            tint = StudioColors.Cyan,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        HorizontalDivider(color = StudioColors.Border)
                        innerContent()
                    }
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .testTag("support_updates_section"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            innerContent()
        }
    }
}

@Composable
private fun AppUpdateClassicCard(
    uiState: StudioUiState,
    currentVersion: String,
    viewModel: StudioViewModel,
    lang: AppLanguage,
    compactMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val result = uiState.updateCheckResult
    val borderColor = when {
        result?.isUpdateAvailable == true -> StudioColors.Amber.copy(alpha = 0.7f)
        result != null -> StudioColors.Emerald.copy(alpha = 0.55f)
        else -> StudioColors.BorderBright
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.testTag("app_update_card"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StudioColors.Amber.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, StudioColors.Amber.copy(alpha = 0.45f)),
                    modifier = Modifier.size(34.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.SystemUpdate,
                            contentDescription = t(lang, "update_card_title"),
                            tint = StudioColors.Amber,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = t(lang, "app_updates_short_title"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = StudioColors.TextPrimary,
                        maxLines = if (compactMode) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = t(lang, "update_card_title") + " • v$currentVersion",
                        style = TelemetryTextStyle.copy(fontSize = 11.sp),
                        color = StudioColors.Amber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Text(
                text = t(lang, "update_card_desc"),
                style = MaterialTheme.typography.bodySmall,
                color = StudioColors.TextSecondary,
                maxLines = if (compactMode) 3 else 4,
                overflow = TextOverflow.Ellipsis,
            )

            if (result != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (result.isUpdateAvailable) StudioColors.Amber.copy(alpha = 0.14f) else StudioColors.Emerald.copy(alpha = 0.14f),
                    border = BorderStroke(
                        1.dp,
                        if (result.isUpdateAvailable) StudioColors.Amber.copy(alpha = 0.55f) else StudioColors.Emerald.copy(alpha = 0.55f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = if (result.isUpdateAvailable) {
                                t(lang, "update_available_badge")
                            } else {
                                t(lang, "update_latest_badge")
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (result.isUpdateAvailable) StudioColors.Amber else StudioColors.Emerald,
                        )
                        Text(
                            text = "v${result.currentVersion} → v${result.latestVersion}",
                            style = TelemetryTextStyle.copy(fontSize = 11.sp),
                            color = StudioColors.TextPrimary,
                        )
                        if (result.releaseNotes.isNotBlank()) {
                            Text(
                                text = result.releaseNotes,
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioColors.TextSecondary,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                val dlInt = remember { MutableInteractionSource() }
                OutlinedButton(
                    onClick = { viewModel.openAppUpdateLink() },
                    interactionSource = dlInt,
                    border = BorderStroke(1.dp, StudioColors.Emerald.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioColors.Emerald),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .lit(dlInt, StudioColors.Emerald, cornerRadius = 12.dp)
                        .testTag("update_download_button"),
                ) {
                    Text(
                        text = t(lang, "update_download_btn"),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            uiState.updateCheckError?.let { err ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StudioColors.Rose.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, StudioColors.Rose.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = err,
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioColors.RoseBright,
                        modifier = Modifier.padding(8.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (uiState.updateCheckLoading) {
                val cancelInt = remember { MutableInteractionSource() }
                OutlinedButton(
                    onClick = { viewModel.cancelActiveOnlineRequest() },
                    interactionSource = cancelInt,
                    border = BorderStroke(1.dp, StudioColors.RoseBright),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioColors.RoseBright),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .lit(cancelInt, StudioColors.RoseBright, cornerRadius = 12.dp)
                        .testTag("update_cancel_button"),
                ) {
                    CircularProgressIndicator(
                        color = StudioColors.Amber,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = t(lang, "cancel_request"),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                val checkInt = remember { MutableInteractionSource() }
                Button(
                    onClick = { viewModel.checkForAppUpdate() },
                    interactionSource = checkInt,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioColors.Amber,
                        contentColor = StudioColors.BgTop,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .lit(checkInt, StudioColors.Amber, cornerRadius = 12.dp)
                        .testTag("update_check_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = t(lang, "update_check_btn"),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerSupportClassicCard(
    lang: AppLanguage,
    supportMessage: String,
    onMessageChange: (String) -> Unit,
    attachDiagnostics: Boolean,
    onAttachChange: (Boolean) -> Unit,
    compactMode: Boolean,
    onSendEmail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
        border = BorderStroke(1.dp, StudioColors.BorderBright),
        modifier = modifier.testTag("customer_support_card"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StudioColors.Violet.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, StudioColors.Violet.copy(alpha = 0.45f)),
                    modifier = Modifier.size(34.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.SupportAgent,
                            contentDescription = t(lang, "support_card_title"),
                            tint = StudioColors.Violet,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = t(lang, "contact_support_title"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = StudioColors.TextPrimary,
                        maxLines = if (compactMode) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = SUPPORT_EMAIL,
                        style = TelemetryTextStyle.copy(fontSize = 10.sp),
                        color = StudioColors.Cyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Text(
                text = t(lang, "support_card_desc"),
                style = MaterialTheme.typography.bodySmall,
                color = StudioColors.TextSecondary,
                maxLines = if (compactMode) 3 else 4,
                overflow = TextOverflow.Ellipsis,
            )

            OutlinedTextField(
                value = supportMessage,
                onValueChange = onMessageChange,
                placeholder = {
                    Text(
                        text = t(lang, "support_message_hint"),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                minLines = 2,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StudioColors.Violet,
                    unfocusedBorderColor = StudioColors.Border,
                    focusedTextColor = StudioColors.TextPrimary,
                    unfocusedTextColor = StudioColors.TextPrimary,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("support_message_input"),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = t(lang, "support_attach_logs_toggle"),
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = attachDiagnostics,
                    onCheckedChange = onAttachChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = StudioColors.BgTop,
                        checkedTrackColor = StudioColors.Cyan,
                    ),
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("support_attach_logs_switch"),
                )
            }

            val sendInt = remember { MutableInteractionSource() }
            Button(
                onClick = onSendEmail,
                interactionSource = sendInt,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioColors.Emerald,
                    contentColor = StudioColors.BgTop,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .lit(sendInt, StudioColors.Emerald, cornerRadius = 12.dp)
                    .testTag("support_send_email_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Email,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = t(lang, "support_send_email_btn"),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun buildSupportEmailBody(
    lang: AppLanguage,
    userMessage: String,
    attachDiagnostics: Boolean,
    currentVersion: String,
): String = buildString {
    appendLine("Sonix ✦ Studio Support Request (v$currentVersion)")
    appendLine("----------------------------------------")
    if (userMessage.isNotBlank()) {
        appendLine(userMessage.trim())
        appendLine()
    } else if (lang != AppLanguage.EN) {
        appendLine("مرحباً فريق الدعم الفني لتطبيق Sonix ✦،")
        appendLine()
    }
    if (attachDiagnostics) {
        val diagLines = AppDiagnosticLog.entries.value.take(5)
        if (diagLines.isNotEmpty()) {
            appendLine()
            appendLine("--- Recent Diagnostics ---")
            diagLines.forEach { entry ->
                appendLine(AppDiagnosticLog.formatFullReport(entry))
            }
        }
    }
}

internal fun launchSupportEmail(
    context: Context,
    subject: String,
    body: String,
    lang: AppLanguage,
): String {
    val safeBody = body.take(12_000)
    val mailtoUri = Uri.parse("mailto:$SUPPORT_EMAIL")

    val primaryStarted = runCatching {
        val mailtoIntent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, safeBody)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(mailtoIntent)
        true
    }.getOrDefault(false)

    if (primaryStarted) {
        return if (lang == AppLanguage.EN) {
            "Opening email client to $SUPPORT_EMAIL"
        } else {
            "تم فتح تطبيق البريد للمراسلة مع $SUPPORT_EMAIL"
        }
    }

    val chooserStarted = runCatching {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, safeBody)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooserTitle = if (lang == AppLanguage.EN) "Send Email to Support" else "إرسال إلى الدعم الفني"
        val chooserIntent = Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
        true
    }.getOrDefault(false)

    if (chooserStarted) {
        return if (lang == AppLanguage.EN) {
            "Opening email chooser to $SUPPORT_EMAIL"
        } else {
            "تم فتح نافذة اختيار البريد للمراسلة مع $SUPPORT_EMAIL"
        }
    }

    copyTextToClipboard(
        context = context,
        label = "Sonix Support Email & Report",
        text = "To: $SUPPORT_EMAIL\nSubject: $subject\n\n$safeBody",
    )
    return if (lang == AppLanguage.EN) {
        "No email app configured — message & address ($SUPPORT_EMAIL) copied to clipboard."
    } else {
        "لا يوجد تطبيق بريد مُعد حالياً — تم نسخ البريد ($SUPPORT_EMAIL) والرسالة إلى الحافظة."
    }
}

private fun copyTextToClipboard(
    context: Context,
    label: String,
    text: String,
) {
    runCatching {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}
