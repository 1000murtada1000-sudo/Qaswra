package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.i18n.t
import com.example.model.AppLanguage
import com.example.studio.ui.lit
import com.example.ui.theme.StudioColors

/**
 * Lightweight, Compose-observable undo/redo state stack for tool screens
 * (Zero-State sliders, TTS script/voice editors, Cinematic ReShade & Reels, etc.).
 */
@Stable
class LocalEditHistory<T>(
    initialState: T,
    private val maxEntries: Int = 30,
) {
    private val undoStack = mutableStateListOf<T>()
    private val redoStack = mutableStateListOf<T>()
    private var lastEventTimeMs: Long = 0L

    var current by mutableStateOf(initialState)
        private set

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun commit(newState: T, debounceMs: Long = 0L) {
        if (newState == current) return
        val now = System.currentTimeMillis()
        val shouldPushSnapshot = debounceMs <= 0L || (now - lastEventTimeMs) >= debounceMs
        if (shouldPushSnapshot) {
            if (undoStack.size >= maxEntries) {
                undoStack.removeAt(0)
            }
            undoStack.add(current)
            redoStack.clear()
        }
        lastEventTimeMs = now
        current = newState
    }

    fun undo(): T? {
        if (undoStack.isEmpty()) return null
        val prev = undoStack.removeAt(undoStack.lastIndex)
        if (redoStack.size >= maxEntries) {
            redoStack.removeAt(0)
        }
        redoStack.add(current)
        current = prev
        lastEventTimeMs = 0L
        return prev
    }

    fun redo(): T? {
        if (redoStack.isEmpty()) return null
        val next = redoStack.removeAt(redoStack.lastIndex)
        if (undoStack.size >= maxEntries) {
            undoStack.removeAt(0)
        }
        undoStack.add(current)
        current = next
        lastEventTimeMs = 0L
        return next
    }
}

@Composable
fun <T> rememberEditHistory(initialState: T): LocalEditHistory<T> = remember {
    LocalEditHistory(initialState)
}

@Composable
fun UndoRedoActionsRow(
    canUndo: Boolean,
    canRedo: Boolean,
    language: AppLanguage,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    undoTag: String = "tool_undo_button",
    redoTag: String = "tool_redo_button",
    extraTrailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val undoInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        IconButton(
            onClick = onUndo,
            enabled = canUndo,
            interactionSource = undoInteraction,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = StudioColors.ElevatedSurface,
                contentColor = StudioColors.Cyan,
                disabledContentColor = StudioColors.TextMuted.copy(alpha = 0.35f),
            ),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(34.dp)
                .lit(undoInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 17.dp)
                .testTag(undoTag),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Undo,
                contentDescription = t(language, "undo_edit"),
            )
        }

        val redoInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        IconButton(
            onClick = onRedo,
            enabled = canRedo,
            interactionSource = redoInteraction,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = StudioColors.ElevatedSurface,
                contentColor = StudioColors.Cyan,
                disabledContentColor = StudioColors.TextMuted.copy(alpha = 0.35f),
            ),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(34.dp)
                .lit(redoInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 17.dp)
                .testTag(redoTag),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Redo,
                contentDescription = t(language, "redo_edit"),
            )
        }

        extraTrailing?.invoke()
    }
}
