package com.example.gridfall.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomMapRulesTest {
    @Test
    fun `built in shapes are valid`() {
        assertNull(CustomMapRules.validationError(CustomMapRules.diamondBlockedCells()))
        assertNull(CustomMapRules.validationError(CustomMapRules.cornerCutBlockedCells()))
    }

    @Test
    fun `empty custom map is accepted`() {
        assertNull(CustomMapRules.validationError(emptySet()))
    }

    @Test
    fun `blocked cells reject placement and are not filled cells`() {
        val blocked = Cell(0, 0)
        val board = Board.empty(blockedCells = setOf(blocked))
        val piece = Piece("single", listOf(Cell(0, 0)))

        assertFalse(board.isPlayable(0, 0))
        assertTrue(board.isEmpty())
        assertFalse(GameEngine.canPlace(board, piece, 0, 0))
    }

    @Test
    fun `line completes using only playable cells`() {
        val blocked = setOf(Cell(0, 0), Cell(0, 7))
        var board = Board.empty(blockedCells = blocked)
        for (col in 1..6) board = board.fill(0, col)

        assertEquals(listOf(0), GameEngine.findFullRows(board))
        val cleared = GameEngine.clearLines(board).board
        assertTrue(cleared.isEmpty())
        assertEquals(blocked, cleared.blockedCells)
    }

    @Test
    fun `custom state supports independently added horizontal and vertical lanes`() {
        val state = GameEngine.createCustomState(
            CustomMapDesign(
                rows = 9,
                columns = 10,
                blockedCells = setOf(Cell(0, 0))
            )
        )

        assertEquals(9, state.board.rowCount)
        assertEquals(10, state.board.columnCount)
        assertTrue(state.board.isCustom)
        assertTrue(state.board.isPlayable(8, 9))
    }

    @Test
    fun `custom state keeps its normalized custom block`() {
        val state = GameEngine.createCustomState(
            CustomMapDesign(
                rows = 8,
                columns = 8,
                blockedCells = emptySet(),
                customBlockCells = setOf(Cell(2, 2), Cell(2, 3), Cell(3, 2))
            )
        )

        assertEquals(
            setOf(Cell(0, 0), Cell(0, 1), Cell(1, 0)),
            state.customBlockCells
        )
    }
}
