package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.aivoice.AiDeliveryPace
import com.example.audio.aivoice.AiDialogueTurn
import com.example.audio.aivoice.AiVoiceCharacter
import com.example.audio.aivoice.AiVoiceEmotion
import com.example.audio.aivoice.AiVoiceEngineKind
import com.example.audio.aivoice.AiVoiceStudioUiState
import com.example.audio.formatDurationMs
import com.example.ui.theme.StudioColors
import com.example.ui.theme.TelemetryTextStyle
import kotlin.math.roundToInt

@Composable
fun AiVoiceConfigurationBanner(
    state: AiVoiceStudioUiState,
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onRetryCloud: () -> Unit,
) {
    val title = state.errorTitle ?: return
    val isConfigRequired = !state.setupRequiredGuide.isNullOrBlank()
    val accent = if (isConfigRequired) StudioColors.Amber else StudioColors.RoseBright

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_voice_status_banner"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = title,
                        tint = accent,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("ai_voice_dismiss_error"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = if (isEnglish) "Dismiss" else "إغلاق",
                        tint = StudioColors.TextSecondary,
                    )
                }
            }

            if (!state.errorDetail.isNullOrBlank()) {
                Text(
                    text = state.errorDetail,
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioColors.TextPrimary,
                )
            }

            if (!state.setupRequiredGuide.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StudioColors.ElevatedSurface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = state.setupRequiredGuide,
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioColors.TextSecondary,
                        modifier = Modifier.padding(10.dp),
                    )
                }

                Button(
                    onClick = onRetryCloud,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioColors.Cyan,
                        contentColor = StudioColors.BgTop,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("ai_voice_retry_cloud_btn"),
                ) {
                    Text(
                        text = if (isEnglish) "Retry VoxCPM2 Cloud" else "إعادة المحاولة عبر VoxCPM2",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
fun AiVoiceOutputPlayerCard(
    state: AiVoiceStudioUiState,
    isEnglish: Boolean,
    onPlayPause: () -> Unit,
    onReplay: () -> Unit,
    onStop: () -> Unit,
    onSeekMs: (Long) -> Unit,
    onSaveWav: () -> Unit,
    onShare: () -> Unit,
    onInsertIntoStudio: () -> Unit,
) {
    val wavPath = state.lastOutputWavPath ?: return
    if (wavPath.isBlank()) return

    val durationMs = state.lastOutputDurationMs.coerceAtLeast(1L)
    val posMs = state.playbackPositionMs.coerceIn(0L, durationMs)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StudioColors.ElevatedSurface),
        border = BorderStroke(1.dp, StudioColors.Cyan.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_voice_output_player_card"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
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
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = StudioColors.Cyan,
                    )
                    Column {
                        Text(
                            text = if (isEnglish) "Generated Audio Output" else "النتيجة الصوتية المولّدة",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = StudioColors.TextPrimary,
                        )
                        state.lastOutputEngine?.let { eng ->
                            Text(
                                text = if (isEnglish) eng.shortTitleEn else eng.shortTitleAr,
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioColors.Cyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Text(
                    text = "${formatDurationMs(posMs)} / ${formatDurationMs(durationMs)}",
                    style = TelemetryTextStyle,
                    color = StudioColors.TextPrimary,
                )
            }

            if (!state.lastOutputAppliedPrompt.isNullOrBlank()) {
                Text(
                    text = state.lastOutputAppliedPrompt,
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Slider(
                value = posMs.toFloat(),
                onValueChange = { onSeekMs(it.toLong()) },
                valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = StudioColors.Cyan,
                    activeTrackColor = StudioColors.Cyan,
                    inactiveTrackColor = StudioColors.BgTop,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_voice_player_seek_slider"),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onPlayPause,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioColors.Cyan,
                        contentColor = StudioColors.BgTop,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .defaultMinSize(minHeight = 40.dp)
                        .testTag("ai_voice_play_pause_button"),
                ) {
                    Icon(
                        imageVector = if (state.isPlayingOutput) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlayingOutput) "Pause" else "Play",
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (state.isPlayingOutput) {
                            if (isEnglish) "Pause" else "إيقاف مؤقت"
                        } else {
                            if (isEnglish) "Play" else "تشغيل"
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }
                OutlinedButton(
                    onClick = onReplay,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 40.dp)
                        .testTag("ai_voice_replay_button"),
                ) {
                    Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isEnglish) "Replay" else "إعادة", maxLines = 1)
                }
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("ai_voice_stop_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Stop,
                        contentDescription = if (isEnglish) "Stop" else "إيقاف",
                        tint = StudioColors.RoseBright,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onSaveWav,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 40.dp)
                        .testTag("ai_voice_save_wav_button"),
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isEnglish) "Save WAV" else "حفظ", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onShare,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 40.dp)
                        .testTag("ai_voice_share_button"),
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isEnglish) "Share" else "مشاركة", maxLines = 1)
                }
                Button(
                    onClick = onInsertIntoStudio,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioColors.Violet,
                        contentColor = StudioColors.TextPrimary,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .defaultMinSize(minHeight = 40.dp)
                        .testTag("ai_voice_insert_studio_button"),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isEnglish) "To Studio" else "للاستوديو",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun AiVoiceDialogueTab(
    state: AiVoiceStudioUiState,
    isEnglish: Boolean,
    onAddTurn: () -> Unit,
    onRemoveTurn: (String) -> Unit,
    onMoveTurn: (String, Int) -> Unit,
    onUpdateTurn: (String, (AiDialogueTurn) -> AiDialogueTurn) -> Unit,
    onSynthesizeDialogue: (AiVoiceEngineKind?) -> Unit,
    onCancelSynthesis: () -> Unit,
) {
    val characters = state.allCharacters
    val focusManager = LocalFocusManager.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
            border = BorderStroke(1.dp, StudioColors.Border),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = if (isEnglish) "Multi-Character Dialogue Builder" else "منشئ الحوار متعدد الشخصيات",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = StudioColors.Cyan,
                )
                Text(
                    text = if (isEnglish) {
                        "Assign fixed professional voices, emotions, and pauses for each line, then stitch everything into one cohesive WAV audio file."
                    } else {
                        "خصّص لكل متحدث صوته الثابت ونبرته وفترة الصمت بعد جملته، وسيتم دمج الحوار بالكامل في ملف صوتي واحد متصل."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioColors.TextSecondary,
                )
            }
        }

        state.dialogueTurns.forEachIndexed { index, turn ->
            val selectedChar = characters.firstOrNull { it.id == turn.characterId } ?: characters.first()
            var charMenuOpen by remember(turn.id) { mutableStateOf(false) }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
                border = BorderStroke(1.dp, StudioColors.BorderBright),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialogue_turn_card_$index"),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = if (isEnglish) "Line #${index + 1}" else "الجملة #${index + 1}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = StudioColors.Amber,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onMoveTurn(turn.id, -1) },
                                enabled = index > 0,
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move Up", tint = StudioColors.TextSecondary)
                            }
                            IconButton(
                                onClick = { onMoveTurn(turn.id, 1) },
                                enabled = index < state.dialogueTurns.lastIndex,
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move Down", tint = StudioColors.TextSecondary)
                            }
                            IconButton(
                                onClick = { onRemoveTurn(turn.id) },
                                enabled = state.dialogueTurns.size > 1,
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = StudioColors.RoseBright)
                            }
                        }
                    }

                    Box {
                        OutlinedButton(
                            onClick = {
                                focusManager.clearFocus()
                                charMenuOpen = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp),
                        ) {
                            Text(
                                text = "${selectedChar.displayName(isEnglish)} • ${if (isEnglish) selectedChar.category.titleEn else selectedChar.category.titleAr}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        DropdownMenu(
                            expanded = charMenuOpen,
                            onDismissRequest = { charMenuOpen = false },
                            containerColor = StudioColors.ElevatedSurface,
                        ) {
                            Column(
                                modifier = Modifier
                                    .widthIn(min = 220.dp, max = 320.dp)
                                    .heightIn(max = 216.dp)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                characters.forEach { ch ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "${ch.displayName(isEnglish)} (${if (isEnglish) ch.category.titleEn else ch.category.titleAr})",
                                                color = if (ch.id == selectedChar.id) StudioColors.Cyan else StudioColors.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        },
                                        onClick = {
                                            focusManager.clearFocus()
                                            val safeEmotion = if (ch.defaultEmotion in state.selectedEngine.supportedEmotions()) {
                                                ch.defaultEmotion
                                            } else {
                                                AiVoiceEmotion.NEUTRAL
                                            }
                                            onUpdateTurn(turn.id) {
                                                it.copy(
                                                    characterId = ch.id,
                                                    emotion = safeEmotion,
                                                    pace = ch.defaultPace,
                                                )
                                            }
                                            charMenuOpen = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = turn.text,
                        onValueChange = { txt -> onUpdateTurn(turn.id) { it.copy(text = txt) } },
                        label = { Text(if (isEnglish) "Dialogue Text" else "نص المتحدث") },
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioColors.Cyan,
                            unfocusedBorderColor = StudioColors.Border,
                            focusedTextColor = StudioColors.TextPrimary,
                            unfocusedTextColor = StudioColors.TextPrimary,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    val turnSupportedEmotions = remember(state.selectedEngine) {
                        state.selectedEngine.supportedEmotions()
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        CompactDropdownField(
                            label = if (isEnglish) "Speaker Tone" else "نبرة المتحدث",
                            selectedText = if (isEnglish) turn.emotion.titleEn else turn.emotion.titleAr,
                            buttonTag = "dialogue_tone_${turn.id}",
                            modifier = Modifier.weight(1f),
                        ) { _, dismiss ->
                            turnSupportedEmotions.forEach { emotion ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isEnglish) emotion.titleEn else emotion.titleAr,
                                            color = if (turn.emotion == emotion) StudioColors.Cyan else StudioColors.TextPrimary,
                                        )
                                    },
                                    onClick = {
                                        focusManager.clearFocus()
                                        onUpdateTurn(turn.id) { it.copy(emotion = emotion) }
                                        dismiss()
                                    },
                                )
                            }
                        }
                        CompactDropdownField(
                            label = if (isEnglish) "Speaking Pace" else "إيقاع الكلام",
                            selectedText = if (isEnglish) turn.pace.titleEn else turn.pace.titleAr,
                            buttonTag = "dialogue_pace_${turn.id}",
                            modifier = Modifier.weight(1f),
                        ) { _, dismiss ->
                            AiDeliveryPace.entries.forEach { pace ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isEnglish) pace.titleEn else pace.titleAr,
                                            color = if (turn.pace == pace) StudioColors.Cyan else StudioColors.TextPrimary,
                                        )
                                    },
                                    onClick = {
                                        focusManager.clearFocus()
                                        onUpdateTurn(turn.id) { it.copy(pace = pace) }
                                        dismiss()
                                    },
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = if (isEnglish) {
                                "Emotion: ${turn.emotion.titleEn} • Pace: ${turn.pace.titleEn}"
                            } else {
                                "النبرة: ${turn.emotion.titleAr} • الإيقاع: ${turn.pace.titleAr}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioColors.TextSecondary,
                        )
                        Slider(
                            value = turn.emotionIntensity,
                            onValueChange = { v -> onUpdateTurn(turn.id) { it.copy(emotionIntensity = v) } },
                            valueRange = 0.1f..1.0f,
                        )
                        Slider(
                            value = turn.pauseAfterMs.toFloat(),
                            onValueChange = { v -> onUpdateTurn(turn.id) { it.copy(pauseAfterMs = v.roundToInt()) } },
                            valueRange = 0f..2000f,
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onAddTurn,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("dialogue_add_turn_button"),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(if (isEnglish) "Add Dialogue Line" else "+ إضافة جملة جديدة للحوار")
        }

        if (state.isSynthesizing) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = { state.synthesisProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = StudioColors.Cyan,
                )
                Text(
                    text = state.synthesisStatusLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = StudioColors.Cyan,
                )
                OutlinedButton(
                    onClick = onCancelSynthesis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(if (isEnglish) "Cancel Generation" else "إلغاء التوليد")
                }
            }
        } else {
            Button(
                onClick = { onSynthesizeDialogue(null) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioColors.Cyan,
                    contentColor = StudioColors.BgTop,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .testTag("dialogue_generate_full_button"),
            ) {
                Text(
                    text = if (isEnglish) "Generate & Stitch Full Dialogue" else "توليد الحوار كاملاً كملف صوتي متصل",
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun AiVoiceCharactersTab(
    state: AiVoiceStudioUiState,
    isEnglish: Boolean,
    onApplyCharacterToStudio: (AiVoiceCharacter) -> Unit,
    onAddCharacterToDialogue: (AiVoiceCharacter) -> Unit,
    onSaveCurrentAsCharacter: (String, String) -> Unit,
    onDeleteSavedCharacter: (String) -> Unit,
    onSaveOutputWav: () -> Unit,
    onShareOutput: () -> Unit,
) {
    var saveDialogOpen by remember { mutableStateOf(false) }
    var newNameAr by remember { mutableStateOf("") }
    var newNameEn by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
            border = BorderStroke(1.dp, StudioColors.Border),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("studio_profile_reference_card"),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (isEnglish) "Stable Cloud Voice Profile" else "ملف البصمة السحابية الثابتة",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = StudioColors.Cyan,
                )
                Text(
                    text = if (isEnglish) {
                        "A fixed calibration sample is created for the selected voice profile and reused for every script. Your generated script is never used as a reference."
                    } else {
                        "تُنشأ عينة معايرة ثابتة للبروفايل الصوتي المختار وتُعاد الاستفادة منها مع كل نص. لا يُستخدم النص أو الصوت الناتج مرجعاً."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioColors.TextSecondary,
                )

                HorizontalDivider(color = StudioColors.Border)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onSaveOutputWav,
                        enabled = !state.lastOutputWavPath.isNullOrBlank(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("ai_voice_adv_save_wav_button"),
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isEnglish) "Export WAV" else "تصدير WAV", maxLines = 1)
                    }
                    IconButton(
                        onClick = onShareOutput,
                        enabled = !state.lastOutputWavPath.isNullOrBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("ai_voice_adv_share_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = if (isEnglish) "Share" else "مشاركة",
                            tint = if (!state.lastOutputWavPath.isNullOrBlank()) StudioColors.Cyan else StudioColors.TextMuted,
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                newNameAr = ""
                newNameEn = ""
                saveDialogOpen = true
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = StudioColors.Violet,
                contentColor = StudioColors.TextPrimary,
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 50.dp)
                .testTag("characters_save_current_button"),
        ) {
            Icon(Icons.Rounded.BookmarkAdd, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isEnglish) "Save Current Voice Design" else "حفظ إعدادات الصوت الحالية كنسخة مفضلة",
                fontWeight = FontWeight.Bold,
            )
        }

        state.allCharacters.forEach { character ->
            val isSelected = state.selectedCharacterId == character.id
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) StudioColors.Cyan else StudioColors.Border,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("character_library_card_${character.id}"),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = character.displayName(isEnglish),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) StudioColors.Cyan else StudioColors.TextPrimary,
                            )
                            Text(
                                text = if (isEnglish) character.category.titleEn else character.category.titleAr,
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioColors.TextSecondary,
                            )
                        }
                        if (!character.isBuiltIn) {
                            IconButton(
                                onClick = { onDeleteSavedCharacter(character.id) },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = StudioColors.RoseBright)
                            }
                        }
                    }

                    Text(
                        text = character.displayDescription(isEnglish),
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioColors.TextSecondary,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { onApplyCharacterToStudio(character) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioColors.Cyan,
                                contentColor = StudioColors.BgTop,
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp),
                        ) {
                            Text(if (isEnglish) "Use in Studio" else "استخدام بالاستوديو", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { onAddCharacterToDialogue(character) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp),
                        ) {
                            Text(if (isEnglish) "+ Dialogue" else "+ للحوار", maxLines = 1)
                        }
                    }
                }
            }
        }
    }

    if (saveDialogOpen) {
        AlertDialog(
            onDismissRequest = { saveDialogOpen = false },
            title = { Text(if (isEnglish) "Save Custom Voice" else "حفظ إعدادات الصوت") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newNameAr,
                        onValueChange = { newNameAr = it },
                        label = { Text(if (isEnglish) "Voice Name" else "الاسم") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = newNameEn,
                        onValueChange = { newNameEn = it },
                        label = { Text(if (isEnglish) "English Name (Optional)" else "الاسم الإنجليزي (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveCurrentAsCharacter(newNameAr, newNameEn)
                        saveDialogOpen = false
                    },
                ) {
                    Text(if (isEnglish) "Save" else "حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { saveDialogOpen = false }) {
                    Text(if (isEnglish) "Cancel" else "إلغاء")
                }
            },
        )
    }
}

@Composable
fun AiVoiceServerSetupTab(
    state: AiVoiceStudioUiState,
    isEnglish: Boolean,
    onSaveBackendUrl: (String) -> Unit,
    onSaveBackendToken: (String) -> Unit = {},
    onCheckHealth: (String, String) -> Unit,
) {
    val health = state.backendHealth

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
            border = BorderStroke(1.dp, StudioColors.Cyan.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Memory, contentDescription = null, tint = StudioColors.Cyan)
                    Text(
                        text = if (isEnglish) "VoxCPM2 Public Cloud Engine" else "محرك VoxCPM2 السحابي العام",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = StudioColors.Cyan,
                    )
                }
                Text(
                    text = if (isEnglish) {
                        "Voice synthesis runs on demand through the public VoxCPM2 service and requires an internet connection."
                    } else {
                        "يعمل توليد الصوت عند الطلب عبر خدمة VoxCPM2 العامة ويتطلب اتصالاً بالإنترنت."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioColors.TextSecondary,
                )
                Text(
                    text = if (isEnglish) {
                        "The public demo may vary output between generation requests; repeated matching requests reuse cached audio."
                    } else {
                        "قد تختلف نتيجة الخدمة العامة بين طلبات التوليد؛ وتُعاد النتيجة المخزنة للطلبات المطابقة."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioColors.TextSecondary,
                )
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
            border = BorderStroke(1.dp, StudioColors.Border),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StudioColors.ElevatedSurface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = StudioColors.Emerald,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (isEnglish) "VoxCPM2 Cloud Endpoint" else "نقطة خدمة VoxCPM2 السحابية",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = StudioColors.TextPrimary,
                            )
                            Text(
                                text = if (isEnglish) {
                                    "Cloud synthesis; no local model is required."
                                } else {
                                    "التوليد سحابي ولا يتطلب تثبيت نموذج محلي."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioColors.Emerald,
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = { onCheckHealth("", "") },
                    enabled = !health.isChecking,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("ai_voice_check_health_button"),
                ) {
                    if (health.isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(if (isEnglish) "Check Cloud Availability" else "التحقق من توفر الخدمة السحابية")
                }

                if (health.isChecked) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StudioColors.ElevatedSurface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = if (health.serverReachable) StudioColors.Emerald else StudioColors.RoseBright,
                                )
                                Text(
                                    text = health.statusDetail.ifBlank {
                                        if (isEnglish) "VoxCPM2 cloud availability checked." else "تم التحقق من توفر خدمة VoxCPM2 السحابية."
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = StudioColors.TextPrimary,
                                )
                            }
                            if (health.serverGpuInfo.isNotBlank()) {
                                Text(
                                    text = health.serverGpuInfo,
                                    style = TelemetryTextStyle,
                                    color = StudioColors.Cyan,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
