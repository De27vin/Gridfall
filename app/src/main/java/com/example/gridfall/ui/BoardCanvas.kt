package com.example.gridfall.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.example.gridfall.game.Board
import com.example.gridfall.game.PieceEffect

@Composable
fun BoardCanvas(
    board: Board,
    modifier: Modifier = Modifier,
    placementPreview: PlacementPreview? = null,
    lineClearFeedback: LineClearFeedback? = null,
    onBoardLayoutChanged: (BoardLayoutInfo) -> Unit = {}
) {
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .onGloballyPositioned { coordinates ->
                val sizePx = minOf(coordinates.size.width, coordinates.size.height).toFloat()
                val spacing = sizePx * 0.018f
                val cellSize = (sizePx - spacing * (Board.SIZE + 1)) / Board.SIZE

                onBoardLayoutChanged(
                    BoardLayoutInfo(
                        topLeft = coordinates.positionInRoot(),
                        sizePx = sizePx,
                        cellSizePx = cellSize,
                        spacingPx = spacing
                    )
                )
            }
    ) {
        val spacing = size.width * 0.018f
        val cellSize = (size.width - spacing * (Board.SIZE + 1)) / Board.SIZE
        val cornerRadius = CornerRadius(cellSize * 0.18f, cellSize * 0.18f)

        drawRoundRect(
            color = Color(0xFF1F2937),
            topLeft = Offset.Zero,
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(cellSize * 0.25f, cellSize * 0.25f)
        )

        for (row in 0 until Board.SIZE) {
            for (col in 0 until Board.SIZE) {
                val cellValue = board.get(row, col)
                val topLeft = Offset(
                    x = spacing + col * (cellSize + spacing),
                    y = spacing + row * (cellSize + spacing)
                )

                drawRoundRect(
                    color = if (cellValue == 0) Color(0xFF374151) else Color(0xFF38BDF8),
                    topLeft = topLeft,
                    size = Size(cellSize, cellSize),
                    cornerRadius = cornerRadius
                )
            }
        }

        placementPreview?.let { preview ->
            val previewColor = when {
                !preview.isValid -> Color(0x88F87171)
                preview.piece.effect == PieceEffect.Bomb -> Color(0x66F59E0B)
                else -> Color(0x8834D399)
            }

            if (preview.piece.effect == PieceEffect.Bomb && preview.isValid) {
                for (row in preview.originRow - 1..preview.originRow + 1) {
                    for (col in preview.originCol - 1..preview.originCol + 1) {
                        if (row in 0 until Board.SIZE && col in 0 until Board.SIZE) {
                            val topLeft = Offset(
                                x = spacing + col * (cellSize + spacing),
                                y = spacing + row * (cellSize + spacing)
                            )

                            drawRoundRect(
                                color = previewColor,
                                topLeft = topLeft,
                                size = Size(cellSize, cellSize),
                                cornerRadius = cornerRadius
                            )
                        }
                    }
                }
            }

            preview.piece.cells.forEach { cell ->
                val row = preview.originRow + cell.row
                val col = preview.originCol + cell.col

                if (row in 0 until Board.SIZE && col in 0 until Board.SIZE) {
                    val topLeft = Offset(
                        x = spacing + col * (cellSize + spacing),
                        y = spacing + row * (cellSize + spacing)
                    )

                    drawRoundRect(
                        color = if (preview.piece.effect == PieceEffect.Bomb && preview.isValid) {
                            Color(0xAAF59E0B)
                        } else {
                            previewColor
                        },
                        topLeft = topLeft,
                        size = Size(cellSize, cellSize),
                        cornerRadius = cornerRadius
                    )
                }
            }
        }

        lineClearFeedback?.let { feedback ->
            val feedbackColor = Color(0xAAFACC15)

            feedback.clearedRows.forEach { row ->
                if (row in 0 until Board.SIZE) {
                    drawRoundRect(
                        color = feedbackColor,
                        topLeft = Offset(spacing, spacing + row * (cellSize + spacing)),
                        size = Size(size.width - spacing * 2, cellSize),
                        cornerRadius = cornerRadius
                    )
                }
            }

            feedback.clearedColumns.forEach { col ->
                if (col in 0 until Board.SIZE) {
                    drawRoundRect(
                        color = feedbackColor,
                        topLeft = Offset(spacing + col * (cellSize + spacing), spacing),
                        size = Size(cellSize, size.height - spacing * 2),
                        cornerRadius = cornerRadius
                    )
                }
            }
        }
    }
}
