package com.example.gridfall.ui

import androidx.compose.ui.geometry.Offset
import com.example.gridfall.game.Board
import com.example.gridfall.game.GameEngine
import com.example.gridfall.game.Piece
import kotlin.math.max

data class DragPlacementTarget(
    val startRow: Int,
    val startCol: Int
)

data class DragPlacementResolution(
    val target: DragPlacementTarget?,
    val isValid: Boolean
)

internal fun calculateDragPlacementResolution(
    piece: Piece,
    visualPieceTopLeft: Offset,
    boardLayoutInfo: BoardLayoutInfo,
    board: Board
): DragPlacementResolution {
    val target = calculatePlacementTargetFromVisualPiece(
        piece = piece,
        visualPieceTopLeft = visualPieceTopLeft,
        boardLayoutInfo = boardLayoutInfo
    )

    return DragPlacementResolution(
        target = target,
        isValid = target != null && GameEngine.canPlace(
            board = board,
            piece = piece,
            startRow = target.startRow,
            startCol = target.startCol
        )
    )
}

internal fun calculateDragVisualOffset(
    piece: Piece,
    boardLayoutInfo: BoardLayoutInfo
): Offset {
    val bounds = piece.bounds()
    val pieceWidth = visualPieceSize(
        cellCount = bounds.colCount,
        cellSizePx = boardLayoutInfo.cellSizePx,
        spacingPx = boardLayoutInfo.spacingPx
    )

    return Offset(
        x = -pieceWidth / 2f,
        y = -boardLayoutInfo.cellSizePx * 1.5f
    )
}

internal fun calculatePlacementTargetFromVisualPiece(
    piece: Piece,
    visualPieceTopLeft: Offset,
    boardLayoutInfo: BoardLayoutInfo
): DragPlacementTarget? {
    if (piece.cells.isEmpty()) return null

    val actualBoardOverlap = calculateActualBoardOverlap(
        piece = piece,
        visualPieceTopLeft = visualPieceTopLeft,
        boardLayoutInfo = boardLayoutInfo
    )
    val minimumMeaningfulOverlap = boardLayoutInfo.cellSizePx * boardLayoutInfo.cellSizePx * 0.2f
    if (actualBoardOverlap < minimumMeaningfulOverlap) return null

    val bounds = piece.bounds()
    val minStartRow = -bounds.maxRow - 1
    val maxStartRow = Board.SIZE - bounds.minRow
    val minStartCol = -bounds.maxCol - 1
    val maxStartCol = Board.SIZE - bounds.minCol
    var bestTarget: DragPlacementTarget? = null
    var bestOverlap = 0f

    for (startRow in minStartRow..maxStartRow) {
        for (startCol in minStartCol..maxStartCol) {
            val overlap = piece.cells.sumOf { cell ->
                val visualCell = visualCellRect(
                    pieceCellRow = cell.row - bounds.minRow,
                    pieceCellCol = cell.col - bounds.minCol,
                    visualPieceTopLeft = visualPieceTopLeft,
                    boardLayoutInfo = boardLayoutInfo
                )
                val targetCell = boardCellRect(
                    row = startRow + cell.row,
                    col = startCol + cell.col,
                    boardLayoutInfo = boardLayoutInfo
                )

                overlapArea(visualCell, targetCell).toDouble()
            }.toFloat()

            if (overlap > bestOverlap) {
                bestOverlap = overlap
                bestTarget = DragPlacementTarget(startRow, startCol)
            }
        }
    }

    return bestTarget
}

internal fun visualPieceWidth(
    piece: Piece,
    cellSizePx: Float,
    spacingPx: Float
): Float {
    return visualPieceSize(
        cellCount = piece.bounds().colCount,
        cellSizePx = cellSizePx,
        spacingPx = spacingPx
    )
}

internal fun visualPieceHeight(
    piece: Piece,
    cellSizePx: Float,
    spacingPx: Float
): Float {
    return visualPieceSize(
        cellCount = piece.bounds().rowCount,
        cellSizePx = cellSizePx,
        spacingPx = spacingPx
    )
}

internal data class DragPieceBounds(
    val minRow: Int,
    val maxRow: Int,
    val minCol: Int,
    val maxCol: Int,
    val rowCount: Int,
    val colCount: Int
)

internal fun Piece.bounds(): DragPieceBounds {
    val minRow = cells.minOf { it.row }
    val maxRow = cells.maxOf { it.row }
    val minCol = cells.minOf { it.col }
    val maxCol = cells.maxOf { it.col }

    return DragPieceBounds(
        minRow = minRow,
        maxRow = maxRow,
        minCol = minCol,
        maxCol = maxCol,
        rowCount = maxRow - minRow + 1,
        colCount = maxCol - minCol + 1
    )
}

private fun calculateActualBoardOverlap(
    piece: Piece,
    visualPieceTopLeft: Offset,
    boardLayoutInfo: BoardLayoutInfo
): Float {
    val bounds = piece.bounds()

    return piece.cells.sumOf { cell ->
        val visualCell = visualCellRect(
            pieceCellRow = cell.row - bounds.minRow,
            pieceCellCol = cell.col - bounds.minCol,
            visualPieceTopLeft = visualPieceTopLeft,
            boardLayoutInfo = boardLayoutInfo
        )

        var cellOverlap = 0f
        for (row in 0 until Board.SIZE) {
            for (col in 0 until Board.SIZE) {
                cellOverlap += overlapArea(
                    visualCell,
                    boardCellRect(row, col, boardLayoutInfo)
                )
            }
        }

        cellOverlap.toDouble()
    }.toFloat()
}

private fun visualCellRect(
    pieceCellRow: Int,
    pieceCellCol: Int,
    visualPieceTopLeft: Offset,
    boardLayoutInfo: BoardLayoutInfo
): CellRect {
    val stride = boardLayoutInfo.cellSizePx + boardLayoutInfo.spacingPx
    val left = visualPieceTopLeft.x + pieceCellCol * stride
    val top = visualPieceTopLeft.y + pieceCellRow * stride

    return CellRect(
        left = left,
        top = top,
        right = left + boardLayoutInfo.cellSizePx,
        bottom = top + boardLayoutInfo.cellSizePx
    )
}

private fun boardCellRect(
    row: Int,
    col: Int,
    boardLayoutInfo: BoardLayoutInfo
): CellRect {
    val stride = boardLayoutInfo.cellSizePx + boardLayoutInfo.spacingPx
    val left = boardLayoutInfo.topLeft.x + boardLayoutInfo.spacingPx + col * stride
    val top = boardLayoutInfo.topLeft.y + boardLayoutInfo.spacingPx + row * stride

    return CellRect(
        left = left,
        top = top,
        right = left + boardLayoutInfo.cellSizePx,
        bottom = top + boardLayoutInfo.cellSizePx
    )
}

private fun overlapArea(first: CellRect, second: CellRect): Float {
    val overlapWidth = minOf(first.right, second.right) - maxOf(first.left, second.left)
    val overlapHeight = minOf(first.bottom, second.bottom) - maxOf(first.top, second.top)

    return max(0f, overlapWidth) * max(0f, overlapHeight)
}

private fun visualPieceSize(
    cellCount: Int,
    cellSizePx: Float,
    spacingPx: Float
): Float {
    return cellSizePx * cellCount + spacingPx * max(0, cellCount - 1)
}

private data class CellRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)
