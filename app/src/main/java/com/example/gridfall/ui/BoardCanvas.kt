package com.example.gridfall.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.example.gridfall.game.Board
import com.example.gridfall.game.PieceEffect
import com.example.gridfall.ui.theme.*

@Composable
fun BoardCanvas(
    board: Board,
    modifier: Modifier = Modifier,
    placementPreview: PlacementPreview? = null,
    lineClearFeedback: LineClearFeedback? = null,
    onBoardLayoutChanged: (BoardLayoutInfo) -> Unit = {}
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(TacticalFrame, RoundedCornerShape(22.dp))
            .border(1.dp, SoftCyanBorder, RoundedCornerShape(22.dp))
            .padding(8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DeepBoardNavy)
            .onGloballyPositioned { coordinates ->
                val sizePx = minOf(coordinates.size.width, coordinates.size.height).toFloat()
                val spacing = 4.dp.value * (sizePx / 300f) // Scale spacing roughly
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
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spacing = 4.dp.toPx()
            val cellSize = (size.width - spacing * (Board.SIZE + 1)) / Board.SIZE
            val cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())

            // Draw Grid
            for (row in 0 until Board.SIZE) {
                for (col in 0 until Board.SIZE) {
                    val cellValue = board.get(row, col)
                    val topLeft = Offset(
                        x = spacing + col * (cellSize + spacing),
                        y = spacing + row * (cellSize + spacing)
                    )

                    if (cellValue == 0) {
                        drawEmptyCell(topLeft, cellSize)
                    } else {
                        drawFilledCell(topLeft, cellSize, cellValue)
                    }
                }
            }

            // Placement Preview
            placementPreview?.let { preview ->
                val previewColor = if (!preview.isValid) CoralBorder else MintBorder
                val previewFill = if (!preview.isValid) CoralGhost else MintGhost

                if (preview.piece.effect == PieceEffect.Bomb && preview.isValid) {
                    // Bomb preview (3x3 area)
                    for (row in preview.originRow - 1..preview.originRow + 1) {
                        for (col in preview.originCol - 1..preview.originCol + 1) {
                            if (row in 0 until Board.SIZE && col in 0 until Board.SIZE) {
                                val topLeft = Offset(
                                    x = spacing + col * (cellSize + spacing),
                                    y = spacing + row * (cellSize + spacing)
                                )
                                drawRoundRect(
                                    color = previewFill,
                                    topLeft = topLeft,
                                    size = Size(cellSize, cellSize),
                                    cornerRadius = cornerRadius
                                )
                                drawRoundRect(
                                    color = previewColor,
                                    topLeft = topLeft,
                                    size = Size(cellSize, cellSize),
                                    cornerRadius = cornerRadius,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
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
                            color = previewFill,
                            topLeft = topLeft,
                            size = Size(cellSize, cellSize),
                            cornerRadius = cornerRadius
                        )
                        drawRoundRect(
                            color = previewColor,
                            topLeft = topLeft,
                            size = Size(cellSize, cellSize),
                            cornerRadius = cornerRadius,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }

            // Line Clear Feedback
            lineClearFeedback?.let { feedback ->
                val flashColor = Color(0xCCFFFFFF)
                
                feedback.clearedRows.forEach { row ->
                    if (row in 0 until Board.SIZE) {
                        drawRect(
                            color = flashColor,
                            topLeft = Offset(0f, spacing + row * (cellSize + spacing)),
                            size = Size(size.width, cellSize)
                        )
                    }
                }

                feedback.clearedColumns.forEach { col ->
                    if (col in 0 until Board.SIZE) {
                        drawRect(
                            color = flashColor,
                            topLeft = Offset(spacing + col * (cellSize + spacing), 0f),
                            size = Size(cellSize, size.height)
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawEmptyCell(topLeft: Offset, cellSize: Float) {
    val cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
    
    // Background fill
    drawRoundRect(
        color = MutedCellNavy,
        topLeft = topLeft,
        size = Size(cellSize, cellSize),
        cornerRadius = cornerRadius
    )
    
    // Subtle border
    drawRoundRect(
        color = Color(0x08FFFFFF),
        topLeft = topLeft,
        size = Size(cellSize, cellSize),
        cornerRadius = cornerRadius,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
    )
}

private fun DrawScope.drawFilledCell(topLeft: Offset, cellSize: Float, variant: Int) {
    val cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
    
    val (topColor, midColor, bottomColor) = when (variant) {
        1 -> Triple(ArcCyanTop, ArcCyan, ArcCyanBottom)
        2 -> Triple(TacticalVioletTop, TacticalViolet, TacticalVioletBottom)
        3 -> Triple(SignalAmberTop, SignalAmber, SignalAmberBottom)
        4 -> Triple(RewardMintTop, RewardMint, RewardMintBottom)
        else -> Triple(BombMagenta, BombMagenta, BombMagenta) // Bomb or unknown
    }

    // Main Gradient Fill
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(topColor, midColor, bottomColor),
            startY = topLeft.y,
            endY = topLeft.y + cellSize
        ),
        topLeft = topLeft,
        size = Size(cellSize, cellSize),
        cornerRadius = cornerRadius
    )

    // Inner highlight (top edge)
    drawRoundRect(
        color = Color(0x33FFFFFF),
        topLeft = topLeft + Offset(2.dp.toPx(), 2.dp.toPx()),
        size = Size(cellSize - 4.dp.toPx(), cellSize / 3f),
        cornerRadius = cornerRadius
    )

    // Outer border for depth
    drawRoundRect(
        color = Color(0x22FFFFFF),
        topLeft = topLeft,
        size = Size(cellSize, cellSize),
        cornerRadius = cornerRadius,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
    )
    
    // Bottom shade
    drawRoundRect(
        color = Color(0x24000000),
        topLeft = topLeft + Offset(0f, cellSize * 0.7f),
        size = Size(cellSize, cellSize * 0.3f),
        cornerRadius = cornerRadius
    )

    if (variant == 0) {
        // Bomb detail
        val center = topLeft + Offset(cellSize / 2f, cellSize / 2f)
        drawCircle(
            color = HotCore,
            radius = cellSize * 0.24f,
            center = center
        )
        drawCircle(
            color = WarningOrange,
            radius = cellSize * 0.12f,
            center = center
        )
    }
}
