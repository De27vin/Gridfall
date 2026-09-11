package com.example.gridfall.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GridLayoutPresetTest {
    @Test
    fun presetsExposeThreeDistinctBoardSizes() {
        assertEquals(listOf(7, 8, 10), GridLayoutPreset.entries.map { it.boardSize })
        assertEquals(listOf(3, 3, 4), GridLayoutPreset.entries.map { it.piecesPerBatch })
        assertEquals(3, GridLayoutPreset.entries.map { it.id }.toSet().size)
    }

    @Test
    fun unknownPreferenceFallsBackToClassic() {
        assertEquals(GridLayoutPreset.Classic, GridLayoutPreset.fromId(null))
        assertEquals(GridLayoutPreset.Classic, GridLayoutPreset.fromId("unknown"))
    }

    @Test
    fun initialStateUsesSelectedGridSize() {
        GridLayoutPreset.entries.forEach { preset ->
            val state = GameEngine.createInitialState(preset)
            assertEquals(preset.boardSize, state.board.cells.size)
            assertTrue(state.board.cells.all { it.size == preset.boardSize })
            assertEquals(preset.piecesPerBatch, state.currentPieces.size)
        }
    }

    @Test
    fun marathonRegeneratesFourPiecesAfterCompletingABatch() {
        val singleCellPiece = PieceLibrary.starterPieces.first()
        var state = GameEngine.createInitialState(GridLayoutPreset.Marathon).copy(
            currentPieces = List(GridLayoutPreset.Marathon.piecesPerBatch) { singleCellPiece }
        )

        repeat(GridLayoutPreset.Marathon.piecesPerBatch) { index ->
            state = GameEngine.placePiece(
                state = state,
                pieceIndex = index,
                startRow = 0,
                startCol = index
            )
        }

        assertEquals(GridLayoutPreset.Marathon.piecesPerBatch, state.currentPieces.size)
        assertTrue(state.usedPieceIndices.isEmpty())
    }

    @Test
    fun lineClearingUsesTheActiveBoardSize() {
        val almostFullRow = (0 until GridLayoutPreset.Rush.boardSize - 1)
            .fold(Board.empty(GridLayoutPreset.Rush.boardSize)) { board, col ->
                board.fill(0, col)
            }

        val result = GameEngine.placePiece(
            state = GameState(
                board = almostFullRow,
                currentPieces = listOf(PieceLibrary.starterPieces.first()),
                usedPieceIndices = emptySet(),
                score = 0,
                combo = 0,
                isGameOver = false
            ),
            pieceIndex = 0,
            startRow = 0,
            startCol = GridLayoutPreset.Rush.boardSize - 1
        )

        assertTrue(result.board.isEmpty())
        assertFalse(result.isGameOver)
        assertEquals(1, result.runStats.linesCleared)
    }
}
