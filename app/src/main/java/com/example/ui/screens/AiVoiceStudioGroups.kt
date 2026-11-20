package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.aivoice.AiVoiceCharacter
import com.example.audio.aivoice.ALL_BUILT_IN_AI_VOICE_CHARACTERS
import com.example.audio.aivoice.AiArabicDialect
import com.example.audio.aivoice.AiEnglishAccent
import com.example.audio.aivoice.AiBreathCadence
import com.example.audio.aivoice.AiVoiceEmotion
import com.example.audio.aivoice.AiVoiceEngineKind
import com.example.audio.aivoice.AiVoiceGroupFilter
import com.example.audio.aivoice.AiVoiceLanguage
import com.example.audio.aivoice.AiPitchContour
import com.example.audio.aivoice.AiStudioPreset
import com.example.audio.aivoice.AiVoiceStudioUiState
import com.example.audio.aivoice.AiVoiceTimbre
import com.example.audio.aivoice.AiVocalStyle
import com.example.audio.tts.TtsTextLimits
import com.example.ui.theme.StudioColors
import com.example.ui.theme.TelemetryTextStyle
import kotlinx.coroutines.delay

@Composable
internal fun CompactDropdownField(
    label: String,
    selectedText: String,
    buttonTag: String,
    modifier: Modifier = Modifier,
    searchable: Boolean = false,
    searchPlaceholder: String = "",
    menuContent: @Composable (searchQuery: String, dismiss: () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = {
                focusManager.clearFocus()
                searchQuery = ""
                expanded = true
            },
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, StudioColors.BorderBright),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag(buttonTag),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioColors.TextSecondary,
                    maxLines = 1,
                )
                Text(
                    text = selectedText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = StudioColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = label,
                tint = StudioColors.Cyan,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                searchQuery = ""
                expanded = false
            },
            modifier = Modifier.heightIn(max = 220.dp),
            shape = RoundedCornerShape(12.dp),
            containerColor = StudioColors.CardSurface,
            border = BorderStroke(1.dp, StudioColors.Border),
        ) {
            Column(
                modifier = Modifier.widthIn(min = 210.dp, max = 320.dp),
            ) {
                if (searchable) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = searchPlaceholder.ifBlank { "بحث..." },
                                style = MaterialTheme.typography.labelMedium,
                                color = StudioColors.TextMuted,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = StudioColors.Cyan,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioColors.Cyan,
                            unfocusedBorderColor = StudioColors.Border,
                            focusedTextColor = StudioColors.TextPrimary,
                            unfocusedTextColor = StudioColors.TextPrimary,
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    menuContent(searchQuery) {
                        searchQuery = ""
                        expanded = false
                    }
                }
            }
        }
    }
}

// =========================================================================
// Script Text Input Card
// =========================================================================
@Composable
internal fun AiVoiceGroup1TextCard(
    scriptText: String,
    isEnglish: Boolean,
    onScriptChange: (String) -> Unit,
    onScriptDraftChange: (String) -> Unit = {},
) {
    var scriptDraft by rememberSaveable { mutableStateOf(scriptText) }
    var countedText by remember { mutableStateOf(scriptText) }

    LaunchedEffect(scriptText) {
        if (scriptText != scriptDraft) scriptDraft = scriptText
        onScriptDraftChange(scriptText)
    }
    LaunchedEffect(scriptDraft) {
        delay(250)
        val limitedText = TtsTextLimits.limitWords(scriptDraft)
        countedText = limitedText
        if (limitedText != scriptDraft) scriptDraft = limitedText
        onScriptDraftChange(limitedText)
        onScriptChange(limitedText)
    }

    val words = remember(countedText) { TtsTextLimits.countWords(countedText) }
    val chars = countedText.length

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
        border = BorderStroke(1.dp, StudioColors.BorderBright),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("studio_group_text"),
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
                    Icon(
                        imageVector = Icons.Rounded.EditNote,
                        contentDescription = null,
                        tint = StudioColors.Cyan,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = if (isEnglish) "Script Text" else "النص",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = StudioColors.TextPrimary,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StudioColors.ElevatedSurface,
                        border = BorderStroke(1.dp, StudioColors.Border),
                    ) {
                        Text(
                            text = if (isEnglish) "$words / ${TtsTextLimits.MAX_WORDS} words • $chars chars" else "$words / ${TtsTextLimits.MAX_WORDS} كلمة • $chars حرف",
                            style = TelemetryTextStyle,
                            color = if (words > TtsTextLimits.MAX_WORDS) StudioColors.RoseBright else StudioColors.Cyan,
                            modifier = Modifier
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                                .testTag("ai_voice_text_counters"),
                        )
                    }
                    if (scriptDraft.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                scriptDraft = ""
                                countedText = ""
                                onScriptDraftChange("")
                                onScriptChange("")
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("ai_voice_clear_script_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = if (isEnglish) "Clear Text" else "مسح النص",
                                tint = StudioColors.RoseBright,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = scriptDraft,
                onValueChange = {
                    scriptDraft = it
                    onScriptDraftChange(it)
                },
                placeholder = {
                    Text(
                        text = if (isEnglish) {
                            "Write the script to transform into expressive voice..."
                        } else {
                            "اكتب النص المراد تحويله إلى صوت استوديو واضح..."
                        },
                        color = StudioColors.TextMuted,
                    )
                },
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StudioColors.Cyan,
                    unfocusedBorderColor = StudioColors.Border,
                    focusedTextColor = StudioColors.TextPrimary,
                    unfocusedTextColor = StudioColors.TextPrimary,
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_voice_script_input"),
            )
        }
    }
}

// =========================================================================
// Card 1: المحرك الصوتي (محرك أساسي عالمي + محرك الاستوديو المحلي)
// =========================================================================
@Composable
internal fun AiVoiceEngineSelectionCard(
    selectedEngine: AiVoiceEngineKind,
    isEnglish: Boolean,
    onSelectEngine: (AiVoiceEngineKind) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
        border = BorderStroke(1.dp, StudioColors.Border),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("studio_card_engine"),
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
                Icon(
                    imageVector = Icons.Rounded.Memory,
                    contentDescription = null,
                    tint = StudioColors.Cyan,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = if (isEnglish) "1. Voice Engine" else "1. المحرك الصوتي",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = StudioColors.TextPrimary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AiVoiceEngineKind.entries.forEach { engine ->
                    val selected = selectedEngine == engine
                    val icon = if (engine == AiVoiceEngineKind.VOXCPM_2) {
                        Icons.Rounded.CloudQueue
                    } else {
                        Icons.Rounded.Memory
                    }
                    Surface(
                        onClick = {
                            focusManager.clearFocus()
                            onSelectEngine(engine)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) StudioColors.Cyan.copy(alpha = 0.16f) else StudioColors.ElevatedSurface,
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) StudioColors.Cyan else StudioColors.Border,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 64.dp)
                            .testTag("ai_voice_engine_${engine.id}"),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (selected) StudioColors.Cyan else StudioColors.TextSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = if (isEnglish) engine.shortTitleEn else engine.shortTitleAr,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) StudioColors.Cyan else StudioColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = if (isEnglish) "Cloud Neural" else "عصبي سحابي",
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

// =========================================================================
// Card 2: اختيار اللغة واللهجة وفئة الصوت
// =========================================================================
@Composable
internal fun AiVoiceGroup2VoiceSelectionCard(
    state: AiVoiceStudioUiState,
    isEnglish: Boolean,
    onSelectLanguage: (AiVoiceLanguage) -> Unit,
    onSelectDialect: (AiArabicDialect) -> Unit,
    onSelectEnglishAccent: (AiEnglishAccent) -> Unit,
    onSelectCharacter: (AiVoiceCharacter) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    var activeFilter by rememberSaveable { mutableStateOf(state.selectedCategory.toGroupFilter()) }
    var voiceSearch by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.selectedCharacterId, state.selectedCategory) {
        activeFilter = state.selectedCategory.toGroupFilter()
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
        border = BorderStroke(1.dp, StudioColors.Border),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("studio_group_voice"),
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
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RecordVoiceOver,
                        contentDescription = null,
                        tint = StudioColors.Cyan,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = if (isEnglish) "2. Voice Selection" else "2. اختيار الصوت",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = StudioColors.TextPrimary,
                    )
                }

                // Compact dropdown for the four supported output languages.
                Box(modifier = Modifier.widthIn(min = 120.dp, max = 160.dp)) {
                    CompactDropdownField(
                        label = if (isEnglish) "Language" else "اللغة",
                        selectedText = state.selectedLanguage.nativeName,
                        buttonTag = "ai_voice_language_dropdown",
                        searchable = true,
                        searchPlaceholder = if (isEnglish) "Search..." else "بحث...",
                    ) { query, dismiss ->
                        val q = query.trim()
                        val langs = state.selectedEngine.supportedLanguages().filter { langOpt ->
                            q.isEmpty() ||
                                langOpt.titleAr.contains(q, ignoreCase = true) ||
                                langOpt.titleEn.contains(q, ignoreCase = true) ||
                                langOpt.nativeName.contains(q, ignoreCase = true) ||
                                langOpt.code.contains(q, ignoreCase = true)
                        }
                        langs.forEach { langOpt ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isEnglish) langOpt.titleEn else langOpt.titleAr,
                                        color = if (langOpt == state.selectedLanguage) StudioColors.Cyan else StudioColors.TextPrimary,
                                    )
                                },
                                onClick = {
                                    focusManager.clearFocus()
                                    onSelectLanguage(langOpt)
                                    dismiss()
                                },
                                modifier = Modifier.testTag("ai_voice_language_${langOpt.code}"),
                            )
                        }
                    }
                }
            }

            if (state.selectedLanguage == AiVoiceLanguage.AR) {
                CompactDropdownField(
                    label = if (isEnglish) "Arabic Dialect" else "اللهجة العربية",
                    selectedText = if (isEnglish) state.selectedDialect.titleEn else state.selectedDialect.titleAr,
                    buttonTag = "ai_voice_dialect_dropdown",
                ) { _, dismiss ->
                    AiArabicDialect.entries.forEach { dialect ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isEnglish) dialect.titleEn else dialect.titleAr,
                                    color = if (dialect == state.selectedDialect) StudioColors.Cyan else StudioColors.TextPrimary,
                                )
                            },
                            onClick = {
                                focusManager.clearFocus()
                                onSelectDialect(dialect)
                                dismiss()
                            },
                            modifier = Modifier.testTag("ai_voice_dialect_${dialect.id}"),
                        )
                    }
                }
            } else if (state.selectedLanguage == AiVoiceLanguage.EN) {
                CompactDropdownField(
                    label = if (isEnglish) "English Accent" else "اللكنة الإنجليزية",
                    selectedText = if (isEnglish) {
                        state.selectedEnglishAccent.titleEn
                    } else {
                        state.selectedEnglishAccent.titleAr
                    },
                    buttonTag = "ai_voice_english_accent_dropdown",
                ) { _, dismiss ->
                    AiEnglishAccent.entries.forEach { accent ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isEnglish) accent.titleEn else accent.titleAr,
                                    color = if (accent == state.selectedEnglishAccent) StudioColors.Cyan else StudioColors.TextPrimary,
                                )
                            },
                            onClick = {
                                focusManager.clearFocus()
                                onSelectEnglishAccent(accent)
                                dismiss()
                            },
                            modifier = Modifier.testTag("ai_voice_english_accent_${accent.id}"),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AiVoiceGroupFilter.entries.forEach { filter ->
                    val isSelected = activeFilter == filter
                    Surface(
                        onClick = {
                            focusManager.clearFocus()
                            activeFilter = filter
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) StudioColors.Cyan.copy(alpha = 0.18f) else StudioColors.ElevatedSurface,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) StudioColors.Cyan else StudioColors.Border,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("ai_voice_filter_${filter.id}"),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        ) {
                            Text(
                                text = if (isEnglish) filter.titleEn else filter.titleAr,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                ),
                                color = if (isSelected) StudioColors.Cyan else StudioColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            val matchingCharacters = remember(
                state.savedCharacters,
                state.favoriteCharacterIds,
                state.selectedCharacterId,
                activeFilter,
                voiceSearch,
            ) {
                val query = voiceSearch.trim()
                (state.savedCharacters + ALL_BUILT_IN_AI_VOICE_CHARACTERS)
                    .asSequence()
                    .filter { it.category.toGroupFilter() == activeFilter }
                    .filter { character ->
                        query.isBlank() ||
                            character.id.contains(query, ignoreCase = true) ||
                            character.displayName(false).contains(query, ignoreCase = true) ||
                            character.displayName(true).contains(query, ignoreCase = true) ||
                            character.displayDescription(false).contains(query, ignoreCase = true) ||
                            character.displayDescription(true).contains(query, ignoreCase = true)
                    }
                    .sortedWith(
                        compareByDescending<AiVoiceCharacter> { state.isCharacterFavorite(it.id) }
                            .thenByDescending { it.id == state.selectedCharacterId },
                    )
                    .take(if (query.isBlank()) 24 else 50)
                    .toList()
            }

            run {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = voiceSearch,
                        onValueChange = { voiceSearch = it },
                        placeholder = {
                            Text(
                                text = if (isEnglish) "Search voice profiles..." else "ابحث في ملفات الأصوات...",
                                color = StudioColors.TextMuted,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = StudioColors.Cyan,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioColors.Cyan,
                            unfocusedBorderColor = StudioColors.Border,
                            focusedTextColor = StudioColors.TextPrimary,
                            unfocusedTextColor = StudioColors.TextPrimary,
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_voice_profile_search"),
                    )
                    Text(
                        text = if (isEnglish) {
                            "Choose a profile, then refine its style and emotion below."
                        } else {
                            "اختر بصمة ثم خصّص أسلوبها ونبرتها أدناه."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioColors.TextSecondary,
                    )
                    matchingCharacters.forEach { ch ->
                        val isCharSelected = state.selectedCharacterId == ch.id
                        val isFav = state.isCharacterFavorite(ch.id)
                        Surface(
                            onClick = {
                                focusManager.clearFocus()
                                onSelectCharacter(ch)
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCharSelected) StudioColors.Cyan.copy(alpha = 0.14f) else StudioColors.ElevatedSurface,
                            border = BorderStroke(
                                width = if (isCharSelected) 1.5.dp else 1.dp,
                                color = if (isCharSelected) StudioColors.Cyan else StudioColors.Border,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .testTag("ai_voice_character_option_${ch.id}"),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = ch.displayName(isEnglish),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isCharSelected) StudioColors.Cyan else StudioColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = ch.displayDescription(isEnglish),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioColors.TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        focusManager.clearFocus()
                                        onToggleFavorite(ch.id)
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("ai_voice_fav_toggle_${ch.id}"),
                                ) {
                                    Icon(
                                        imageVector = if (isFav) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                        contentDescription = if (isEnglish) "Favorite" else "تفضيل",
                                        tint = if (isFav) StudioColors.Amber else StudioColors.TextMuted,
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

// =========================================================================
// Card 3: emotion and vocal-style selectors
// =========================================================================
@Composable
internal fun AiVoiceGroup3EmotionCard(
    state: AiVoiceStudioUiState,
    isEnglish: Boolean,
    onSelectEmotion: (AiVoiceEmotion) -> Unit,
    onSelectVocalStyle: (AiVocalStyle) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
        border = BorderStroke(1.dp, StudioColors.Border),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("studio_group_emotion"),
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
                Icon(
                    imageVector = Icons.Rounded.Mood,
                    contentDescription = null,
                    tint = StudioColors.Cyan,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = if (isEnglish) "3. Tone & Emotion" else "3. النبرة والمشاعر",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = StudioColors.TextPrimary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CompactDropdownField(
                    label = if (isEnglish) "Expression" else "الانفعال",
                    selectedText = if (isEnglish) state.selectedEmotion.titleEn else state.selectedEmotion.titleAr,
                    buttonTag = "ai_voice_emotion_dropdown",
                    modifier = Modifier.weight(1f),
                ) { _, dismiss ->
                    state.selectedEngine.supportedEmotions().forEach { emotion ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isEnglish) emotion.titleEn else emotion.titleAr,
                                    color = if (state.selectedEmotion == emotion) StudioColors.Cyan else StudioColors.TextPrimary,
                                )
                            },
                            onClick = {
                                focusManager.clearFocus()
                                onSelectEmotion(emotion)
                                dismiss()
                            },
                            modifier = Modifier.testTag("ai_voice_emotion_${emotion.id}"),
                        )
                    }
                }

                CompactDropdownField(
                    label = if (isEnglish) "Vocal style" else "أسلوب الإلقاء",
                    selectedText = if (isEnglish) state.selectedVocalStyle.titleEn else state.selectedVocalStyle.titleAr,
                    buttonTag = "ai_voice_vocal_style_dropdown",
                    modifier = Modifier.weight(1f),
                ) { _, dismiss ->
                    AiVocalStyle.entries.forEach { style ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isEnglish) style.titleEn else style.titleAr,
                                    color = if (state.selectedVocalStyle == style) StudioColors.Cyan else StudioColors.TextPrimary,
                                )
                            },
                            onClick = {
                                focusManager.clearFocus()
                                onSelectVocalStyle(style)
                                dismiss()
                            },
                            modifier = Modifier.testTag("ai_voice_vocal_style_${style.id}"),
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// Card 4: synthesis controls and studio presets
// =========================================================================
@Composable
internal fun AiVoiceGroup4ControlsCard(
    state: AiVoiceStudioUiState,
    isEnglish: Boolean,
    onSpeechRateChange: (Float) -> Unit,
    onIntensityChange: (Float) -> Unit,
    onSelectStudioPreset: (AiStudioPreset) -> Unit,
    onSelectBreathCadence: (AiBreathCadence) -> Unit,
    onSelectPitchContour: (AiPitchContour) -> Unit,
    onSelectTimbre: (AiVoiceTimbre) -> Unit,
    onArticulationClarityChange: (Float) -> Unit,
    onResetTuning: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
        border = BorderStroke(1.dp, StudioColors.Border),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("studio_group_controls"),
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
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = StudioColors.Cyan,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = if (isEnglish) "4. Fine Tuning" else "4. الضبط الدقيق",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = StudioColors.TextPrimary,
                    )
                }

                TextButton(
                    onClick = onResetTuning,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("ai_voice_reset_tuning_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        tint = StudioColors.Cyan,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isEnglish) "Reset" else "إعادة الضبط الافتراضي",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = StudioColors.Cyan,
                    )
                }
            }

            HorizontalDivider(color = StudioColors.Border)

            CompactDropdownField(
                label = if (isEnglish) "Studio vocal preset" else "إعداد صوتي استوديو",
                selectedText = if (isEnglish) state.selectedStudioPreset.titleEn else state.selectedStudioPreset.titleAr,
                buttonTag = "ai_voice_studio_preset_dropdown",
            ) { _, dismiss ->
                AiStudioPreset.entries.filter { it != AiStudioPreset.CUSTOM }.forEach { preset ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (isEnglish) preset.titleEn else preset.titleAr,
                                color = if (state.selectedStudioPreset == preset) StudioColors.Cyan else StudioColors.TextPrimary,
                            )
                        },
                        onClick = {
                            onSelectStudioPreset(preset)
                            dismiss()
                        },
                        modifier = Modifier.testTag("ai_voice_studio_preset_${preset.id}"),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CompactDropdownField(
                    label = if (isEnglish) "Breath cadence" else "إيقاع التنفس",
                    selectedText = if (isEnglish) state.selectedBreathCadence.titleEn else state.selectedBreathCadence.titleAr,
                    buttonTag = "ai_voice_breath_cadence_dropdown",
                    modifier = Modifier.weight(1f),
                ) { _, dismiss ->
                    AiBreathCadence.entries.forEach { cadence ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isEnglish) cadence.titleEn else cadence.titleAr,
                                    color = if (state.selectedBreathCadence == cadence) StudioColors.Cyan else StudioColors.TextPrimary,
                                )
                            },
                            onClick = {
                                onSelectBreathCadence(cadence)
                                dismiss()
                            },
                            modifier = Modifier.testTag("ai_voice_breath_cadence_${cadence.id}"),
                        )
                    }
                }
                CompactDropdownField(
                    label = if (isEnglish) "Pitch contour" else "منحنى الطبقة",
                    selectedText = if (isEnglish) state.selectedPitchContour.titleEn else state.selectedPitchContour.titleAr,
                    buttonTag = "ai_voice_pitch_contour_dropdown",
                    modifier = Modifier.weight(1f),
                ) { _, dismiss ->
                    AiPitchContour.entries.forEach { contour ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isEnglish) contour.titleEn else contour.titleAr,
                                    color = if (state.selectedPitchContour == contour) StudioColors.Cyan else StudioColors.TextPrimary,
                                )
                            },
                            onClick = {
                                onSelectPitchContour(contour)
                                dismiss()
                            },
                            modifier = Modifier.testTag("ai_voice_pitch_contour_${contour.id}"),
                        )
                    }
                }
            }

            CompactDropdownField(
                label = if (isEnglish) "Resonance & timbre" else "الرنين وطابع الصوت",
                selectedText = if (isEnglish) state.selectedTimbre.titleEn else state.selectedTimbre.titleAr,
                buttonTag = "ai_voice_timbre_dropdown",
            ) { _, dismiss ->
                AiVoiceTimbre.entries.forEach { timbre ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (isEnglish) timbre.titleEn else timbre.titleAr,
                                color = if (state.selectedTimbre == timbre) StudioColors.Cyan else StudioColors.TextPrimary,
                            )
                        },
                        onClick = {
                            onSelectTimbre(timbre)
                            dismiss()
                        },
                        modifier = Modifier.testTag("ai_voice_timbre_${timbre.id}"),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isEnglish) "Speech Rate" else "سرعة الكلام",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = StudioColors.TextPrimary,
                )
                Slider(
                    value = state.speechRate,
                    onValueChange = onSpeechRateChange,
                    valueRange = 0.60f..1.50f,
                    colors = SliderDefaults.colors(
                        thumbColor = StudioColors.Cyan,
                        activeTrackColor = StudioColors.Cyan,
                        inactiveTrackColor = StudioColors.BgTop,
                    ),
                    modifier = Modifier.testTag("ai_voice_speed_slider"),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isEnglish) "Articulation Clarity" else "وضوح النطق",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = StudioColors.TextPrimary,
                )
                Slider(
                    value = state.articulationClarity,
                    onValueChange = onArticulationClarityChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = StudioColors.Cyan,
                        activeTrackColor = StudioColors.Cyan,
                        inactiveTrackColor = StudioColors.BgTop,
                    ),
                    modifier = Modifier.testTag("ai_voice_articulation_clarity_slider"),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isEnglish) "Emotion Intensity" else "قوة التعبير",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = StudioColors.TextPrimary,
                )
                Slider(
                    value = state.emotionIntensity,
                    onValueChange = onIntensityChange,
                    valueRange = 0.10f..1.00f,
                    colors = SliderDefaults.colors(
                        thumbColor = StudioColors.Cyan,
                        activeTrackColor = StudioColors.Cyan,
                        inactiveTrackColor = StudioColors.BgTop,
                    ),
                    modifier = Modifier.testTag("ai_voice_intensity_slider"),
                )
            }
        }
    }
}

// =========================================================================
// Sticky Bottom Action Bar (Always Pinned Above Keyboard / Nav Bar)
// =========================================================================
@Composable
internal fun AiVoiceStickyBottomActionBar(
    state: AiVoiceStudioUiState,
    hasStudioAudio: Boolean,
    isEnglish: Boolean,
    onPreviewClick: () -> Unit,
    onCancelSynthesis: () -> Unit,
    onApproveClick: () -> Unit,
) {
    val hasOutput = !state.lastOutputWavPath.isNullOrBlank()
    val focusManager = LocalFocusManager.current
    val insetModifier = if (!hasStudioAudio) {
        Modifier.windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
    } else {
        Modifier.imePadding()
    }

    Surface(
        color = StudioColors.CardSurface,
        border = BorderStroke(1.dp, StudioColors.Cyan.copy(alpha = 0.35f)),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .then(insetModifier)
            .testTag("ai_voice_sticky_bottom_bar"),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.isSynthesizing) {
                    LinearProgressIndicator(
                        progress = { state.synthesisProgress.coerceIn(0.02f, 1f) },
                        color = StudioColors.Cyan,
                        trackColor = StudioColors.BgTop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                    )
                    if (state.synthesisStatusLabel.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = state.synthesisStatusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioColors.Cyan,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = {
                            focusManager.clearFocus()
                            if (state.isSynthesizing) onCancelSynthesis() else onPreviewClick()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (state.isSynthesizing) StudioColors.RoseBright else StudioColors.Cyan,
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (state.isSynthesizing) StudioColors.RoseBright else StudioColors.Cyan,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("ai_voice_sticky_preview_button"),
                    ) {
                        Icon(
                            imageVector = when {
                                state.isSynthesizing -> Icons.Rounded.Close
                                state.isPlayingOutput -> Icons.Rounded.Pause
                                hasOutput -> Icons.Rounded.PlayArrow
                                else -> Icons.Rounded.GraphicEq
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = when {
                                state.isSynthesizing -> if (isEnglish) "Cancel" else "إلغاء"
                                state.isPlayingOutput -> if (isEnglish) "Pause" else "إيقاف المعاينة"
                                else -> if (isEnglish) "Preview Voice" else "معاينة الصوت"
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onApproveClick()
                        },
                        enabled = !state.isSynthesizing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasOutput) StudioColors.Emerald else StudioColors.Cyan,
                            contentColor = StudioColors.BgTop,
                            disabledContainerColor = StudioColors.ElevatedSurface,
                            disabledContentColor = StudioColors.TextMuted,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("ai_voice_sticky_approve_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isEnglish) "Approve Voice" else "موافقة / اعتماد",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
