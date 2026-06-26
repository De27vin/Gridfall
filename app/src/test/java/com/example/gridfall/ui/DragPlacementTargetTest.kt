package com.example.gridfall.ui

import androidx.compose.ui.geometry.Offset
import com.example.gridfall.game.Board
import com.example.gridfall.game.Cell
import com.example.gridfall.game.Piece
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DragPlacementTargetTest {
    private val layout = BoardLayoutInfo(
        topLeft = Offset.Zero,
        sizePx = 800f,
        cellSizePx = 100f,
        spacingPx = 0f
    )

    @Test
    fun dragVisualOffsetCentersOneByOnePieceHorizontally() {
        val piece = Piece("single", listOf(Cell(0, 0)))

        val offset = calculateDragVisualOffset(piece, layout)

        assertEquals(-50f, offset.x)
        assertEquals(-150f, offset.y)
    }

    @Test
    fun dragVisualOffsetCentersWidePieceHorizontally() {
        val piece = Piece(
            id = "wide",
            cells = listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2), Cell(0, 3))
        )

        val offset = calculateDragVisualOffset(piece, layout)

        assertEquals(-200f, offset.x)
        assertEquals(-150f, offset.y)
    }

    @Test
    fun oneByOnePieceSnapsToCellWithMajorityOverlap() {
        val piece = Piece("single", listOf(Cell(0, 0)))

        val mostlyOverColThree = calculatePlacementTargetFromVisualPiece(
            piece = piece,
            visualPieceTopLeft = Offset(340f, 200f),
            boardLayoutInfo = layout
        )
        val mostlyOverColFour = calculatePlacementTargetFromVisualPiece(
            piece = piece,
            visualPieceTopLeft = Offset(360f, 200f),
            boardLayoutInfo = layout
        )

        assertEquals(DragPlacementTarget(startRow = 2, startCol = 3), mostlyOverColThree)
        assertEquals(DragPlacementTarget(startRow = 2, startCol = 4), mostlyOverColFour)
    }

    @Test
    fun horizontalPieceSnapsNaturallyNearRightSide() {
        val piece = Piece(
            id = "wide",
            cells = listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2), Cell(0, 3))
        )

        val target = calculatePlacementTargetFromVisualPiece(
            piece = piece,
            visualPieceTopLeft = Offset(410f, 300f),
            boardLayoutInfo = layout
        )

        assertEquals(DragPlacementTarget(startRow = 3, startCol = 4), target)
    }

    @Test
    fun partlyOutsidePieceCanSnapToInvalidOutsideOrigin() {
        val piece = Piece("single", listOf(Cell(0, 0)))

        val target = calculatePlacementTargetFromVisualPiece(
            piece = piece,
            visualPieceTopLeft = Offset(-60f, 200f),
            boardLayoutInfo = layout
        )

        assertEquals(DragPlacementTarget(startRow = 2, startCol = -1), target)
    }

    @Test
    fun pieceWithoutMeaningfulBoardOverlapReturnsNull() {
        val piece = Piece("single", listOf(Cell(0, 0)))

        val target = calculatePlacementTargetFromVisualPiece(
            piece = piece,
            visualPieceTopLeft = Offset(-90f, 200f),
            boardLayoutInfo = layout
        )

        assertNull(target)
    }

    @Test
    fun placementResolutionUsesTargetForValidPreviewAndDrop() {
        val piece = Piece("single", listOf(Cell(0, 0)))

        val resolution = calculateDragPlacementResolution(
            piece = piece,
            visualPieceTopLeft = Offset(360f, 200f),
            boardLayoutInfo = layout,
            board = Board.empty()
        )

        assertEquals(DragPlacementTarget(startRow = 2, startCol = 4), resolution.target)
        assertTrue(resolution.isValid)
    }

    @Test
    fun placementResolutionKeepsOccupiedTargetButMarksItInvalid() {
        val piece = Piece("single", listOf(Cell(0, 0)))

        val resolution = calculateDragPlacementResolution(
            piece = piece,
            visualPieceTopLeft = Offset(360f, 200f),
            boardLayoutInfo = layout,
            board = Board.empty().fill(row = 2, col = 4)
        )

        assertEquals(DragPlacementTarget(startRow = 2, startCol = 4), resolution.target)
        assertFalse(resolution.isValid)
    }
}
