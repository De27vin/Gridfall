package com.example.gridfall.ui

import com.example.gridfall.game.Cell
import com.example.gridfall.game.CustomMapDesign
import com.example.gridfall.game.MapBlockPoolRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomMapEditorHistoryTest {
    private val initial = CustomMapDesign(
        rows = 8,
        columns = 8,
        blockedCells = emptySet(),
        blockPool = MapBlockPoolRules.defaultPool()
    )

    @Test
    fun `undo and redo restore complete map snapshots`() {
        val changed = initial.copy(
            rows = 7,
            blockedCells = setOf(Cell(6, 2))
        )
        val history = CustomMapEditorHistory(initial).record(changed)

        assertTrue(history.canUndo)
        assertEquals(initial, history.undo().current)
        assertEquals(changed, history.undo().redo().current)
    }

    @Test
    fun `new edit after undo clears redo history`() {
        val first = initial.copy(blockedCells = setOf(Cell(0, 0)))
        val replacement = initial.copy(columns = 9)

        val history = CustomMapEditorHistory(initial)
            .record(first)
            .undo()
            .record(replacement)

        assertFalse(history.canRedo)
        assertEquals(replacement, history.current)
    }

    @Test
    fun `recording identical snapshot does not create history`() {
        val history = CustomMapEditorHistory(initial).record(initial)

        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
    }

    @Test
    fun `untouched new map can exit without a save prompt`() {
        assertFalse(CustomMapEditorExitPolicy.hasUnsavedChanges(initial, null))
    }

    @Test
    fun `changed new map requires a save prompt`() {
        val changed = initial.copy(blockedCells = setOf(Cell(0, 0)))

        assertTrue(CustomMapEditorExitPolicy.hasUnsavedChanges(changed, null))
    }

    @Test
    fun `saved map only prompts after its design changes`() {
        assertFalse(CustomMapEditorExitPolicy.hasUnsavedChanges(initial, initial))
        assertTrue(
            CustomMapEditorExitPolicy.hasUnsavedChanges(
                initial.copy(rows = 7),
                initial
            )
        )
    }
}
