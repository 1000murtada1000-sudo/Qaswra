package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SpatialAudioOff
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.model.PlaybackTelemetry
import com.example.model.RecorderTelemetry
import kotlinx.coroutines.flow.StateFlow
import com.example.audio.formatDurationMs
import com.example.data.RecentEditEntry
import com.example.diagnostics.AppDiagnosticLog
import com.example.i18n.t
import com.example.model.AppLanguage
import com.example.model.AudioPickerTarget
import com.example.model.AuthSession
import com.example.model.EffectType
import com.example.model.StudioRoute
import com.example.studio.ui.lit
import com.example.ui.components.LanguageBadgeButton
import com.example.ui.components.LanguageSelectorDialog
import com.example.ui.components.RealTimeWaveformCanvas
import com.example.ui.components.SonixExitConfirmDialog
import com.example.ui.components.ToolInfoDialog
import com.example.ui.theme.StudioColors
import com.example.ui.theme.TelemetryTextStyle
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel

private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}

@Immutable
private data class HomeToolTile(
    val id: String,
    val titleKey: String,
    val icon: ImageVector,
    val accent: Color,
    val route: StudioRoute,
    val infoKey: String? = null,
)

@Immutable
private data class HomeToolFamily(
    val id: String,
    val titleKey: String,
    val icon: ImageVector,
    val accent: Color,
    val tools: List<HomeToolTile>,
)

@Composable
fun HomeScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    listState: LazyListState,
    onNavigate: (StudioRoute) -> Unit,
    onOpenNotifications: () -> Unit = { onNavigate(StudioRoute.Notifications) },
    onRestoreRecentEdit: (RecentEditEntry) -> Unit = { entry ->
        viewModel.restoreRecentEditSession(entry) { route -> onNavigate(route) }
    },
) {
    val lang = uiState.language
    val context = LocalContext.current
    val devPanelState by viewModel.developerPanelUiState.collectAsStateWithLifecycle()
    val showDeveloperPanelEntry = BuildConfig.DEVELOPER_PANEL_ENABLED && devPanelState.ownerVerification.isAuthorized
    var langDialogOpen by remember { mutableStateOf(false) }
    var accountSheetOpen by remember { mutableStateOf(false) }
    var activeToolInfo by remember { mutableStateOf<Pair<String, String>?>(null) }
    var exitConfirmDialogOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        exitConfirmDialogOpen = true
    }

    val activeEffectsCount = uiState.dspConfig.chain.count { it.enabled }
    val userLabel = when (val auth = uiState.authSession) {
        is AuthSession.SignedIn -> auth.displayName
        is AuthSession.Guest -> if (lang == AppLanguage.EN) "Guest" else "ضيف"
        is AuthSession.SignedOut -> ""
    }

    val families = remember {
        listOf(
            HomeToolFamily(
                id = "ai_voice",
                titleKey = "family_ai_voice",
                icon = Icons.Rounded.RecordVoiceOver,
                accent = StudioColors.Cyan,
                tools = listOf(
                    HomeToolTile(
                        id = "ai_voice_studio",
                        titleKey = "tool_ai_voice_studio_short",
                        icon = Icons.Rounded.RecordVoiceOver,
                        accent = StudioColors.Cyan,
                        route = StudioRoute.AiVoiceStudio,
                    ),
                    HomeToolTile(
                        id = "voice_morph",
                        titleKey = "tool_voice_morph",
                        icon = Icons.Rounded.SpatialAudioOff,
                        accent = StudioColors.Violet,
                        route = StudioRoute.VoiceChanger,
                    ),
                    HomeToolTile(
                        id = "smart_engineer",
                        titleKey = "tool_smart_engineer",
                        icon = Icons.Rounded.AutoFixHigh,
                        accent = StudioColors.Emerald,
                        route = StudioRoute.VoicePurifier,
                        infoKey = "tool_smart_engineer_info",
                    ),
                    HomeToolTile(
                        id = "speech_to_text",
                        titleKey = "tool_speech_to_text",
                        icon = Icons.Rounded.ClosedCaption,
                        accent = StudioColors.Cyan,
                        route = StudioRoute.SpeechToText,
                        infoKey = "speech_to_text_info",
                    ),
                    HomeToolTile(
                        id = "smart_assistant",
                        titleKey = "tool_smart_assistant",
                        icon = Icons.Rounded.AutoAwesome,
                        accent = StudioColors.Cyan,
                        route = StudioRoute.SmartAssistant,
                        infoKey = "tool_smart_assistant_info",
                    ),
                ),
            ),
            HomeToolFamily(
                id = "editing",
                titleKey = "family_editing",
                icon = Icons.Rounded.ContentCut,
                accent = StudioColors.Amber,
                tools = listOf(
                    HomeToolTile(
                        id = "trim_edit",
                        titleKey = "tool_trim_edit",
                        icon = Icons.Rounded.ContentCut,
                        accent = StudioColors.Amber,
                        route = StudioRoute.Editor,
                    ),
                    HomeToolTile(
                        id = "merge_music_audio",
                        titleKey = "tool_merge_music_audio",
                        icon = Icons.Rounded.Layers,
                        accent = StudioColors.Cyan,
                        route = StudioRoute.AudioMerge,
                        infoKey = "tool_merge_music_audio_info",
                    ),
                    HomeToolTile(
                        id = "split_tracks",
                        titleKey = "tool_split_tracks",
                        icon = Icons.AutoMirrored.Rounded.CallSplit,
                        accent = StudioColors.Violet,
                        route = StudioRoute.AudioSplit,
                    ),
                ),
            ),
            HomeToolFamily(
                id = "effects",
                titleKey = "family_effects",
                icon = Icons.Rounded.Waves,
                accent = StudioColors.Violet,
                tools = listOf(
                    HomeToolTile(
                        id = "pro_presets_echo",
                        titleKey = "tool_pro_presets_echo",
                        icon = Icons.Rounded.Waves,
                        accent = StudioColors.Cyan,
                        route = StudioRoute.Presets,
                        infoKey = "tool_pro_presets_echo_info",
                    ),
                    HomeToolTile(
                        id = "equalizer",
                        titleKey = "tool_equalizer",
                        icon = Icons.Rounded.Tune,
                        accent = StudioColors.Violet,
                        route = StudioRoute.EffectDetail(EffectType.EQUALIZER),
                    ),
                    HomeToolTile(
                        id = "pitch_speed",
                        titleKey = "tool_pitch_speed",
                        icon = Icons.Rounded.Speed,
                        accent = StudioColors.Amber,
                        route = StudioRoute.PitchAndSpeed,
                    ),
                    HomeToolTile(
                        id = "reverse_audio",
                        titleKey = "tool_reverse_audio",
                        icon = Icons.Rounded.Undo,
                        accent = StudioColors.RoseBright,
                        route = StudioRoute.ReverseAudio,
                    ),
                    HomeToolTile(
                        id = "stereo_surround",
                        titleKey = "tool_stereo_surround",
                        icon = Icons.Rounded.SurroundSound,
                        accent = StudioColors.Emerald,
                        route = StudioRoute.SurroundStereo,
                    ),
                    HomeToolTile(
                        id = "generate_silence",
                        titleKey = "tool_generate_silence",
                        icon = Icons.Rounded.HourglassEmpty,
                        accent = StudioColors.Cyan,
                        route = StudioRoute.GenerateSilence,
                    ),
                ),
            ),
            HomeToolFamily(
                id = "quick_tools",
                titleKey = "family_quick_tools",
                icon = Icons.Rounded.GraphicEq,
                accent = StudioColors.Emerald,
                tools = listOf(
                    HomeToolTile(
                        id = "format_convert",
                        titleKey = "tool_format_convert",
                        icon = Icons.Rounded.SwapHoriz,
                        accent = StudioColors.Cyan,
                        route = StudioRoute.FormatConverter,
                    ),
                    HomeToolTile(
                        id = "extract_video",
                        titleKey = "tool_extract_video",
                        icon = Icons.Rounded.Movie,
                        accent = StudioColors.Amber,
                        route = StudioRoute.ExtractVideoAudio,
                    ),
                    HomeToolTile(
                        id = "reels_audio_templates",
                        titleKey = "tool_reels_audio_templates",
                        icon = Icons.Rounded.Podcasts,
                        accent = StudioColors.Amber,
                        route = StudioRoute.ReelsAudioTemplates,
                        infoKey = "tool_reels_audio_templates_info",
                    ),
                    HomeToolTile(
                        id = "compress_audio",
                        titleKey = "tool_compress_audio",
                        icon = Icons.Rounded.Compress,
                        accent = StudioColors.Emerald,
                        route = StudioRoute.CompressAudio,
                    ),
                    HomeToolTile(
                        id = "live_record",
                        titleKey = "tool_live_record",
                        icon = Icons.Rounded.Mic,
                        accent = StudioColors.RoseBright,
                        route = StudioRoute.Record,
                    ),
                    HomeToolTile(
                        id = "ringtone_maker",
                        titleKey = "tool_ringtone_maker",
                        icon = Icons.Rounded.NotificationsActive,
                        accent = StudioColors.Amber,
                        route = StudioRoute.RingtoneMaker,
                        infoKey = "tool_ringtone_maker_info",
                    ),
                ),
            ),
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "hero_banner", contentType = "hero_banner") {
                val heroBgBrush = remember {
                    Brush.linearGradient(
                        colors = listOf(
                            StudioColors.BgTop,
                            StudioColors.CardSurface,
                            Color(0xFF151E3B),
                        ),
                    )
                }
                val heroRadialBrush = remember {
                    Brush.radialGradient(
                        colors = listOf(
                            StudioColors.Cyan.copy(alpha = 0.16f),
                            StudioColors.Violet.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(heroBgBrush),
                ) {
                    // Ambient glowing cyan-violet studio backdrop + watermark of the primary logo silhouette
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(heroRadialBrush),
                    )
                    Image(
                        painter = painterResource(R.drawable.ic_splash_vector),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(StudioColors.Cyan.copy(alpha = 0.12f)),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 10.dp)
                            .size(100.dp)
                            .alpha(0.85f),
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val logoBorderBrush = remember {
                                Brush.linearGradient(
                                    colors = listOf(StudioColors.Cyan, StudioColors.Violet),
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFFFFFFE),
                                border = BorderStroke(
                                    width = 1.dp,
                                    brush = logoBorderBrush,
                                ),
                                shadowElevation = 4.dp,
                                modifier = Modifier.size(38.dp),
                            ) {
                                Image(
                                    bitmap = ImageBitmap.imageResource(R.drawable.sonix_primary_logo),
                                    contentDescription = t(lang, "app_name"),
                                    contentScale = ContentScale.Crop,
                                    filterQuality = FilterQuality.High,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(11.dp)),
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(0.dp),
                            ) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = "Sonix ✦",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            textDirection = TextDirection.Ltr,
                                            letterSpacing = 0.4.sp,
                                        ),
                                        color = StudioColors.TextPrimary,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                                Text(
                                    text = t(lang, "studio_tagline"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioColors.Cyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            val bellInteraction = remember { MutableInteractionSource() }
                            val badgeCount = uiState.unreadDiagnosticCount
                            val totalCount = uiState.diagnosticEntries.size
                            IconButton(
                                onClick = onOpenNotifications,
                                interactionSource = bellInteraction,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = StudioColors.ElevatedSurface.copy(alpha = 0.92f),
                                    contentColor = if (badgeCount > 0) StudioColors.Amber else StudioColors.Cyan,
                                ),
                                modifier = Modifier
                                    .size(36.dp)
                                    .lit(bellInteraction, StudioColors.Cyan, cornerRadius = 18.dp)
                                    .testTag("home_notifications_button"),
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (badgeCount > 0 || totalCount > 0) {
                                            Badge(
                                                containerColor = if (badgeCount > 0) StudioColors.RoseBright else StudioColors.Amber,
                                                contentColor = StudioColors.BgTop,
                                            ) {
                                                Text(
                                                    text = (if (badgeCount > 0) badgeCount else totalCount).coerceAtMost(99).toString(),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 10.sp,
                                                    ),
                                                )
                                            }
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Notifications,
                                        contentDescription = t(lang, "notifications_title"),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val authChipInteraction = remember { MutableInteractionSource() }
                            Surface(
                                onClick = { accountSheetOpen = true },
                                interactionSource = authChipInteraction,
                                shape = RoundedCornerShape(999.dp),
                                color = StudioColors.ElevatedSurface.copy(alpha = 0.92f),
                                border = BorderStroke(1.dp, StudioColors.BorderBright),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .lit(authChipInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 17.dp)
                                    .testTag("home_auth_chip"),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Icon(
                                        imageVector = if (uiState.authSession is AuthSession.SignedIn) {
                                            Icons.Rounded.Person
                                        } else {
                                            Icons.Rounded.PersonOutline
                                        },
                                        contentDescription = userLabel,
                                        tint = if (uiState.authSession is AuthSession.SignedIn) StudioColors.Cyan else StudioColors.Amber,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = userLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                        color = StudioColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }

                            LanguageBadgeButton(
                                currentLanguage = lang,
                                onClick = { langDialogOpen = true },
                                compact = true,
                            )
                        }

                        HomeLiveWaveformSection(
                            playbackFlow = viewModel.playbackTelemetry,
                            recorderFlow = viewModel.recorderTelemetry,
                        )
                    }
                }
            }

            // 2) المشروع النشط (استيراد وتسجيل)
            item(key = "active_project_summary", contentType = "active_project_summary") {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
                    border = BorderStroke(1.dp, StudioColors.Border),
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = t(lang, "active_project"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioColors.TextMuted,
                                )
                                Text(
                                    text = uiState.activeProjectName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = StudioColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = formatDurationMs(uiState.durationMs),
                                    style = TelemetryTextStyle,
                                    color = StudioColors.Cyan,
                                )
                                Text(
                                    text = if (uiState.dspConfig.bypassAll) {
                                        t(lang, "bypass_all")
                                    } else if (lang == AppLanguage.EN) {
                                        "$activeEffectsCount DSP Active"
                                    } else {
                                        "$activeEffectsCount مؤثر مفعّل"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (uiState.dspConfig.bypassAll) StudioColors.Amber else StudioColors.Emerald,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val importInteraction = remember { MutableInteractionSource() }
                            OutlinedButton(
                                onClick = { onNavigate(StudioRoute.AudioPicker(AudioPickerTarget.IMPORT_MAIN)) },
                                interactionSource = importInteraction,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .lit(importInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 12.dp)
                                    .testTag("home_import_library_button"),
                            ) {
                                Icon(Icons.Rounded.LibraryMusic, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = t(lang, "import_audio"),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            val recordInteraction = remember { MutableInteractionSource() }
                            OutlinedButton(
                                onClick = { onNavigate(StudioRoute.Record) },
                                interactionSource = recordInteraction,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, StudioColors.Rose.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .lit(recordInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 12.dp)
                                    .testTag("home_card_record"),
                            ) {
                                Icon(Icons.Rounded.Mic, contentDescription = null, tint = StudioColors.RoseBright, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = t(lang, "tool_live_record"),
                                    color = StudioColors.RoseBright,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.recentEdits.isNotEmpty()) {
                item(key = "recent_edits_strip", contentType = "recent_edits_strip") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 2.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                tint = StudioColors.Cyan,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = t(lang, "recent_edits_title"),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = StudioColors.TextPrimary,
                            )
                        }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp),
                        ) {
                            items(
                                items = uiState.recentEdits,
                                key = { it.id },
                            ) { entry ->
                                val recentInteraction = remember { MutableInteractionSource() }
                                Surface(
                                    onClick = { onRestoreRecentEdit(entry) },
                                    interactionSource = recentInteraction,
                                    shape = RoundedCornerShape(14.dp),
                                    color = StudioColors.CardSurface,
                                    border = BorderStroke(1.dp, StudioColors.Cyan.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .widthIn(min = 168.dp, max = 232.dp)
                                        .lit(recentInteraction, StudioColors.Cyan, cornerRadius = 14.dp)
                                        .testTag("recent_edit_${entry.id}"),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        Text(
                                            text = if (lang == AppLanguage.EN) entry.titleEn else entry.titleAr,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = StudioColors.Cyan,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = entry.audioName,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = StudioColors.TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "${if (lang == AppLanguage.EN) entry.summaryEn else entry.summaryAr} • ${AppDiagnosticLog.formatTimestamp(entry.timestampMs).takeLast(8)}",
                                            style = TelemetryTextStyle.copy(fontSize = 10.sp),
                                            color = StudioColors.TextMuted,
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

            val aiVoiceFamily = families.first { it.id == "ai_voice" }
            item(key = "family_card_ai_voice", contentType = "family_card") {
                val columns = 3
                val rows = aiVoiceFamily.tools.chunked(columns)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
                    border = BorderStroke(1.dp, aiVoiceFamily.accent.copy(alpha = 0.28f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_family_card_${aiVoiceFamily.id}"),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FamilyParentHeader(
                            title = t(lang, aiVoiceFamily.titleKey),
                            icon = aiVoiceFamily.icon,
                            accent = aiVoiceFamily.accent,
                            toolCount = aiVoiceFamily.tools.size,
                        )
                        rows.forEach { rowTools ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                rowTools.forEach { tile ->
                                    ToolSquareTile(
                                        title = t(lang, tile.titleKey),
                                        icon = tile.icon,
                                        accent = tile.accent,
                                        testTag = "home_tile_${tile.id}",
                                        hasInfo = tile.infoKey != null,
                                        aspectRatio = 1.05f,
                                        onInfoClick = {
                                            tile.infoKey?.let { key ->
                                                activeToolInfo = t(lang, tile.titleKey) to t(lang, key)
                                            }
                                        },
                                        onClick = { onNavigate(tile.route) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(columns - rowTools.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // 4) أدوات وإعدادات الاستوديو (الصدى والعمق وسلسلة المؤثرات)
            item(key = "studio_extras_strip", contentType = "extras_strip") {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
                    border = BorderStroke(1.dp, StudioColors.BorderBright),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_studio_utilities_card"),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FamilyParentHeader(
                            title = t(lang, "studio_utilities_title"),
                            icon = Icons.Rounded.Settings,
                            accent = StudioColors.Cyan,
                            toolCount = if (showDeveloperPanelEntry) 6 else 5,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ExtraQuickChip(
                                label = t(lang, "section_ai_title"),
                                icon = Icons.Rounded.AutoAwesome,
                                accent = StudioColors.Violet,
                                testTag = "home_card_ai",
                                onClick = { onNavigate(StudioRoute.AiStudio) },
                                modifier = Modifier.weight(1f),
                            )
                            ExtraQuickChip(
                                label = t(lang, "section_echo_title"),
                                icon = Icons.Rounded.Waves,
                                accent = StudioColors.Cyan,
                                testTag = "home_card_echo",
                                onClick = { onNavigate(StudioRoute.EchoSpace) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ExtraQuickChip(
                                label = t(lang, "section_chain_title"),
                                icon = Icons.Rounded.Tune,
                                accent = StudioColors.Emerald,
                                testTag = "home_card_chain",
                                onClick = { onNavigate(StudioRoute.EffectsChain) },
                                modifier = Modifier.weight(1f),
                            )
                            ExtraQuickChip(
                                label = t(lang, "section_settings_title"),
                                icon = Icons.Rounded.Settings,
                                accent = StudioColors.Amber,
                                testTag = "home_card_settings",
                                onClick = { onNavigate(StudioRoute.Settings) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (showDeveloperPanelEntry) {
                            ExtraQuickChip(
                                label = t(lang, "developer_panel_button"),
                                icon = Icons.Rounded.AdminPanelSettings,
                                accent = StudioColors.Violet,
                                testTag = "home_card_developer_panel",
                                onClick = { onNavigate(StudioRoute.DeveloperPanel) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        val batchInteraction = remember { MutableInteractionSource() }
                        val batchState = uiState.batchQueueState
                        Card(
                            onClick = { onNavigate(StudioRoute.BatchQueue) },
                            interactionSource = batchInteraction,
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = StudioColors.ElevatedSurface),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (batchState.isRunning) StudioColors.Cyan else StudioColors.Violet.copy(alpha = 0.45f),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .lit(batchInteraction, StudioColors.Cyan, cornerRadius = 16.dp)
                                .testTag("home_card_batch_queue"),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = StudioColors.Cyan.copy(alpha = 0.16f),
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.QueueMusic,
                                            contentDescription = t(lang, "tool_batch_queue"),
                                            tint = StudioColors.Cyan,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = t(lang, "tool_batch_queue"),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = StudioColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = if (batchState.isRunning) {
                                            val pct = (batchState.overallProgress * 100f).toInt()
                                            if (lang == AppLanguage.EN) "Running in background ($pct%)" else "قيد المعالجة بالخلفية ($pct%)"
                                        } else {
                                            t(lang, "batch_queue_subtitle")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (batchState.isRunning) StudioColors.Cyan else StudioColors.TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (batchState.items.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = StudioColors.Cyan.copy(alpha = 0.18f),
                                    ) {
                                        Text(
                                            text = "${batchState.items.size}",
                                            style = TelemetryTextStyle,
                                            color = StudioColors.Cyan,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5) استوديو التحرير والمونتاج، 6) المؤثرات والهندسة الصوتية، 7) الأدوات السريعة والتحويل
            families.filter { it.id != "ai_voice" }.forEach { family ->
                val columns = 3
                val rows = family.tools.chunked(columns)
                item(key = "family_card_${family.id}", contentType = "family_card") {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
                        border = BorderStroke(1.dp, family.accent.copy(alpha = 0.28f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_family_card_${family.id}"),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            FamilyParentHeader(
                                title = t(lang, family.titleKey),
                                icon = family.icon,
                                accent = family.accent,
                                toolCount = family.tools.size,
                            )
                            rows.forEach { rowTools ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    rowTools.forEach { tile ->
                                        ToolSquareTile(
                                            title = t(lang, tile.titleKey),
                                            icon = tile.icon,
                                            accent = tile.accent,
                                            testTag = "home_tile_${tile.id}",
                                            hasInfo = tile.infoKey != null,
                                            aspectRatio = 1.05f,
                                            onInfoClick = {
                                                tile.infoKey?.let { key ->
                                                    activeToolInfo = t(lang, tile.titleKey) to t(lang, key)
                                                }
                                            },
                                            onClick = { onNavigate(tile.route) },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    repeat(columns - rowTools.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "support_updates_qa_section", contentType = "support_updates_qa_section") {
                SupportUpdatesSection(
                    uiState = uiState,
                    viewModel = viewModel,
                    collapsible = true,
                    initiallyExpanded = false,
                )
            }

            item(key = "home_bottom_spacer", contentType = "spacer") {
                Spacer(Modifier.height(6.dp))
            }
        }
    }

    LanguageSelectorDialog(
        open = langDialogOpen,
        currentLanguage = lang,
        onSelect = { viewModel.setLanguage(it) },
        onDismiss = { langDialogOpen = false },
    )

    activeToolInfo?.let { (infoTitle, infoDesc) ->
        ToolInfoDialog(
            open = true,
            title = infoTitle,
            description = infoDesc,
            language = lang,
            onDismiss = { activeToolInfo = null },
        )
    }

    SonixExitConfirmDialog(
        open = exitConfirmDialogOpen,
        language = lang,
        hasUnsavedChanges = uiState.hasUnsavedChanges,
        onStay = { exitConfirmDialogOpen = false },
        onSaveToDraftsAndExit = {
            exitConfirmDialogOpen = false
            viewModel.saveCloudDraft(uiState.activeProjectName.ifBlank { "مسودة Sonix" })
            context.findHostActivity()?.finish()
        },
        onConfirmExit = {
            exitConfirmDialogOpen = false
            context.findHostActivity()?.finish()
        },
    )

    if (accountSheetOpen) {
        SonixAccountBottomSheet(
            authSession = uiState.authSession,
            language = lang,
            onDismiss = { accountSheetOpen = false },
            onOpenBatchQueue = {
                accountSheetOpen = false
                onNavigate(StudioRoute.BatchQueue)
            },
            onOpenProjects = {
                accountSheetOpen = false
                onNavigate(StudioRoute.ProjectsExport)
            },
            onOpenSettings = {
                accountSheetOpen = false
                onNavigate(StudioRoute.Settings)
            },
            showDeveloperPanel = showDeveloperPanelEntry,
            onOpenDeveloperPanel = {
                accountSheetOpen = false
                onNavigate(StudioRoute.DeveloperPanel)
            },
            onOpenLanguage = {
                accountSheetOpen = false
                langDialogOpen = true
            },
            onSignIn = {
                accountSheetOpen = false
                viewModel.requestSignIn()
            },
            onSignOut = {
                accountSheetOpen = false
                viewModel.signOut()
            },
        )
    }
}

@Composable
private fun FamilyParentHeader(
    title: String,
    icon: ImageVector,
    accent: Color,
    toolCount: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
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
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accent.copy(alpha = 0.16f),
                    modifier = Modifier.size(30.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accent,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = StudioColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = accent.copy(alpha = 0.14f),
            ) {
                Text(
                    text = "$toolCount",
                    style = TelemetryTextStyle.copy(fontSize = 11.sp),
                    color = accent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        HorizontalDivider(color = accent.copy(alpha = 0.25f), thickness = 1.dp)
    }
}

@Composable
private fun ToolSquareTile(
    title: String,
    icon: ImageVector,
    accent: Color,
    testTag: String,
    hasInfo: Boolean = false,
    aspectRatio: Float = 1f,
    onInfoClick: () -> Unit = {},
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tileInteraction = remember { MutableInteractionSource() }
    val tileGradient = remember(accent) {
        Brush.verticalGradient(
            colors = listOf(
                accent.copy(alpha = 0.14f),
                StudioColors.CardSurface,
                StudioColors.ElevatedSurface.copy(alpha = 0.85f),
            ),
        )
    }
    Surface(
        onClick = onClick,
        interactionSource = tileInteraction,
        shape = RoundedCornerShape(16.dp),
        color = StudioColors.CardSurface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
        modifier = modifier
            .aspectRatio(aspectRatio)
            .defaultMinSize(minHeight = 48.dp)
            .lit(tileInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 16.dp)
            .testTag(testTag),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(tileGradient)
                .padding(horizontal = 6.dp, vertical = 8.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = accent.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                    modifier = Modifier.size(42.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 14.sp,
                    ),
                    color = StudioColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (hasInfo) {
                val infoInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = onInfoClick,
                    interactionSource = infoInteraction,
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(26.dp)
                        .lit(infoInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 13.dp)
                        .testTag("${testTag}_info"),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "!",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                            ),
                            color = accent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtraQuickChip(
    label: String,
    icon: ImageVector,
    accent: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chipInteraction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = chipInteraction,
        shape = RoundedCornerShape(14.dp),
        color = StudioColors.ElevatedSurface,
        border = BorderStroke(1.dp, StudioColors.Border),
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .lit(chipInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = StudioColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SonixAccountBottomSheet(
    authSession: AuthSession,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onOpenBatchQueue: () -> Unit,
    onOpenProjects: () -> Unit,
    onOpenSettings: () -> Unit,
    showDeveloperPanel: Boolean = false,
    onOpenDeveloperPanel: () -> Unit = {},
    onOpenLanguage: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
) {
    val isEn = language == AppLanguage.EN
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val signedIn = authSession as? AuthSession.SignedIn
    val initialChar = signedIn?.displayName?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "S"
    val avatarBorderBrush = remember {
        Brush.linearGradient(colors = listOf(StudioColors.Cyan, StudioColors.Violet))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StudioColors.CardSurface,
        contentColor = StudioColors.TextPrimary,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        modifier = Modifier.testTag("account_bottom_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = if (isEn) "Account & Studio Profile" else "حسابي في Sonix ✦",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = StudioColors.TextPrimary,
            )

            // Account Info Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = StudioColors.ElevatedSurface,
                border = BorderStroke(1.dp, StudioColors.BorderBright),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = StudioColors.BgTop,
                        border = BorderStroke(2.dp, avatarBorderBrush),
                        modifier = Modifier.size(56.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (signedIn != null) {
                                Text(
                                    text = initialChar,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = StudioColors.Cyan,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.PersonOutline,
                                    contentDescription = null,
                                    tint = StudioColors.Amber,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = signedIn?.displayName ?: if (isEn) "Guest User" else "مستخدم ضيف",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = StudioColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = signedIn?.email?.takeIf { it.isNotBlank() }
                                ?: if (isEn) "Offline studio mode • No linked account" else "وضع الاستوديو المحلي • بدون حساب مرتبط",
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (signedIn != null) {
                                StudioColors.Emerald.copy(alpha = 0.16f)
                            } else {
                                StudioColors.Amber.copy(alpha = 0.16f)
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (signedIn != null) {
                                    StudioColors.Emerald.copy(alpha = 0.5f)
                                } else {
                                    StudioColors.Amber.copy(alpha = 0.5f)
                                },
                            ),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = null,
                                    tint = if (signedIn != null) StudioColors.Emerald else StudioColors.Amber,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = if (signedIn != null) {
                                        if (isEn) "Connected & Active" else "حساب متصل ومفعّل"
                                    } else {
                                        if (isEn) "Guest Session" else "جلسة ضيف مؤقتة"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (signedIn != null) StudioColors.Emerald else StudioColors.Amber,
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AccountSheetShortcutRow(
                    icon = Icons.Rounded.QueueMusic,
                    label = t(language, "tool_batch_queue"),
                    accent = StudioColors.Cyan,
                    testTag = "sheet_batch_queue_button",
                    onClick = onOpenBatchQueue,
                )
                AccountSheetShortcutRow(
                    icon = Icons.Rounded.FolderOpen,
                    label = t(language, "nav_projects"),
                    accent = StudioColors.Violet,
                    testTag = "sheet_projects_button",
                    onClick = onOpenProjects,
                )
                AccountSheetShortcutRow(
                    icon = Icons.Rounded.Settings,
                    label = t(language, "section_settings_title"),
                    accent = StudioColors.Amber,
                    testTag = "sheet_settings_button",
                    onClick = onOpenSettings,
                )
                if (showDeveloperPanel) {
                    AccountSheetShortcutRow(
                        icon = Icons.Rounded.AdminPanelSettings,
                        label = t(language, "developer_panel_button"),
                        accent = StudioColors.Violet,
                        testTag = "sheet_developer_panel_button",
                        onClick = onOpenDeveloperPanel,
                    )
                }
                AccountSheetShortcutRow(
                    icon = Icons.Rounded.Language,
                    label = if (isEn) "App Language" else "لغة التطبيق",
                    accent = StudioColors.Emerald,
                    testTag = "sheet_language_button",
                    onClick = onOpenLanguage,
                )
            }

            HorizontalDivider(color = StudioColors.Border)

            if (signedIn != null) {
                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, StudioColors.Rose.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = StudioColors.Rose.copy(alpha = 0.14f),
                        contentColor = StudioColors.RoseBright,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 50.dp)
                        .testTag("sheet_logout_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isEn) "Sign Out" else "تسجيل الخروج",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
            } else {
                Button(
                    onClick = onSignIn,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioColors.Cyan,
                        contentColor = StudioColors.BgTop,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 50.dp)
                        .testTag("sheet_login_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Login,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = t(language, "auth_sign_in"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountSheetShortcutRow(
    icon: ImageVector,
    label: String,
    accent: Color,
    testTag: String,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(14.dp),
        color = StudioColors.ElevatedSurface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, StudioColors.Border),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .lit(interaction, accent, cornerRadius = 14.dp)
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = StudioColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeLiveWaveformSection(
    playbackFlow: StateFlow<PlaybackTelemetry>,
    recorderFlow: StateFlow<RecorderTelemetry>,
) {
    val playback by playbackFlow.collectAsStateWithLifecycle()
    val recorder by recorderFlow.collectAsStateWithLifecycle()
    val isActive = recorder.isRecording || playback.isPlaying
    val bars = if (recorder.isRecording) recorder.waveformBars else playback.waveformBars
    val accentColor = if (recorder.isRecording) StudioColors.RoseBright else StudioColors.Cyan

    RealTimeWaveformCanvas(
        bars = bars,
        isActive = isActive,
        accentColor = accentColor,
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .testTag("home_waveform"),
    )
}

