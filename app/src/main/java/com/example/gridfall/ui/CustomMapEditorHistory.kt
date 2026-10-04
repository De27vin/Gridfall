package com.example.gridfall.ui

import com.example.gridfall.game.CustomMapDesign
import com.example.gridfall.game.CustomMapRules
import com.example.gridfall.game.MapBlockPoolRules

internal object CustomMapEditorExitPolicy {
    fun hasUnsavedChanges(
        currentDesign: CustomMapDesign,
        lastSavedDesign: CustomMapDesign?
    ): Boolean {
        val cleanDesign = lastSavedDesign ?: CustomMapDesign(
            rows = CustomMapRules.BOARD_SIZE,
            columns = CustomMapRules.BOARD_SIZE,
            blockedCells = emptySet(),
            blockPool = MapBlockPoolRules.defaultPool()
        )
        return currentDesign != cleanDesign
    }
}

internal data class CustomMapEditorHistory(
    val current: CustomMapDesign,
    private val undoStack: List<CustomMapDesign> = emptyList(),
    private val redoStack: List<CustomMapDesign> = emptyList()
) {
    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    fun record(next: CustomMapDesign): CustomMapEditorHistory {
        if (next == current) return this
        return copy(
            current = next,
            undoStack = (undoStack + current).takeLast(MAX_HISTORY_SIZE),
            redoStack = emptyList()
        )
    }

    fun undo(): CustomMapEditorHistory {
        if (!canUndo) return this
        return copy(
            current = undoStack.last(),
            undoStack = undoStack.dropLast(1),
            redoStack = (redoStack + current).takeLast(MAX_HISTORY_SIZE)
        )
    }

    fun redo(): CustomMapEditorHistory {
        if (!canRedo) return this
        return copy(
            current = redoStack.last(),
            undoStack = (undoStack + current).takeLast(MAX_HISTORY_SIZE),
            redoStack = redoStack.dropLast(1)
        )
    }

    private companion object {
        const val MAX_HISTORY_SIZE = 100
    }
}
