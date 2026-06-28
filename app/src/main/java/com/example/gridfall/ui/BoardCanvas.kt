package com.example.gridfall.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
    bombPulseFeedback: BombPulseFeedback? = null,
    onBoardLayoutChanged: (BoardLayoutInfo) -> Unit = {}
) {
    val linePulse = remember { Animatable(1f) }
    val bombPulse = remember { Animatable(1f) }

    LaunchedEffect(lineClearFeedback?.token) {
        if (lineClearFeedback != null) {
            linePulse.snapTo(0f)
            linePulse.animateTo(1f, animationSpec = tween(durationMillis = 560))
        }
    }

    LaunchedEffect(bombPulseFeedback?.token) {
        if (bombPulseFeedback != null) {
            bombPulse.snapTo(0f)
            bombPulse.animateTo(1f, animationSpec = tween(durationMillis = 520))
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF152235), TacticalFrame)
                ),
                shape = RoundedCornerShape(22.dp)
            )
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
                val progress = linePulse.value.coerceIn(0f, 1f)
                val fade = (1f - progress).coerceIn(0f, 1f)
                val fillColor = LevelCyan.copy(alpha = 0.18f * fade)
                val glowColor = LevelCyan.copy(alpha = 0.26f * fade)
                val railColor = RewardMint.copy(alpha = 0.70f * fade)
                val strokeColor = LevelCyan.copy(alpha = 0.48f * fade)
                val scanOffset = cellSize * progress

                feedback.clearedRows.forEach { row ->
                    if (row in 0 until Board.SIZE) {
                        val topLeft = Offset(0f, spacing + row * (cellSize + spacing))
                        drawRoundRect(
                            color = fillColor,
                            topLeft = topLeft,
                            size = Size(size.width, cellSize),
                            cornerRadius = cornerRadius
                        )
                        drawRoundRect(
                            color = strokeColor,
                            topLeft = topLeft,
                            size = Size(size.width, cellSize),
                            cornerRadius = cornerRadius,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        drawRect(
                            color = glowColor,
                            topLeft = topLeft + Offset(0f, cellSize * 0.26f),
                            size = Size(size.width, cellSize * 0.48f)
                        )
                        drawRect(
                            color = railColor,
                            topLeft = topLeft + Offset(0f, scanOffset.coerceAtMost(cellSize - 2.dp.toPx())),
                            size = Size(size.width, 2.dp.toPx())
                        )
                    }
                }

                feedback.clearedColumns.forEach { col ->
                    if (col in 0 until Board.SIZE) {
                        val topLeft = Offset(spacing + col * (cellSize + spacing), 0f)
                        drawRoundRect(
                            color = fillColor,
                            topLeft = topLeft,
                            size = Size(cellSize, size.height),
                            cornerRadius = cornerRadius
                        )
                        drawRoundRect(
                            color = strokeColor,
                            topLeft = topLeft,
                            size = Size(cellSize, size.height),
                            cornerRadius = cornerRadius,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        drawRect(
                            color = glowColor,
                            topLeft = topLeft + Offset(cellSize * 0.26f, 0f),
                            size = Size(cellSize * 0.48f, size.height)
                        )
                        drawRect(
                            color = railColor,
                            topLeft = topLeft + Offset(scanOffset.coerceAtMost(cellSize - 2.dp.toPx()), 0f),
                            size = Size(2.dp.toPx(), size.height)
                        )
                    }
                }
            }

            bombPulseFeedback?.let { feedback ->
                val progress = bombPulse.value.coerceIn(0f, 1f)
                val fade = (1f - progress).coerceIn(0f, 1f)
                val centerX = spacing + feedback.centerCol * (cellSize + spacing) + cellSize / 2f
                val centerY = spacing + feedback.centerRow * (cellSize + spacing) + cellSize / 2f
                val center = Offset(centerX, centerY)
                val affectedFill = WarningOrange.copy(alpha = 0.20f * fade)
                val affectedStroke = PremiumGold.copy(alpha = 0.48f * fade)

                for (row in feedback.centerRow - 1..feedback.centerRow + 1) {
                    for (col in feedback.centerCol - 1..feedback.centerCol + 1) {
                        if (row in 0 until Board.SIZE && col in 0 until Board.SIZE) {
                            val topLeft = Offset(
                                x = spacing + col * (cellSize + spacing),
                                y = spacing + row * (cellSize + spacing)
                            )
                            drawRoundRect(
                                color = affectedFill,
                                topLeft = topLeft,
                                size = Size(cellSize, cellSize),
                                cornerRadius = cornerRadius
                            )
                            drawRoundRect(
                                color = affectedStroke,
                                topLeft = topLeft,
                                size = Size(cellSize, cellSize),
                                cornerRadius = cornerRadius,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }
                }

                drawCircle(
                    color = WarningOrange.copy(alpha = 0.20f * fade),
                    radius = cellSize * (0.65f + progress * 1.75f),
                    center = center,
                    style = Stroke(width = (4.dp.toPx() * fade).coerceAtLeast(1.dp.toPx()))
                )
                drawCircle(
                    color = PremiumGold.copy(alpha = 0.34f * fade),
                    radius = cellSize * (0.30f + progress * 0.80f),
                    center = center
                )
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
        color = Color.White.copy(alpha = 0.06f),
        topLeft = topLeft,
        size = Size(cellSize, cellSize),
        cornerRadius = cornerRadius,
        style = Stroke(width = 1.dp.toPx())
    )

    drawRoundRect(
        color = Color.Black.copy(alpha = 0.14f),
        topLeft = topLeft + Offset(1.dp.toPx(), 1.dp.toPx()),
        size = Size(cellSize - 2.dp.toPx(), cellSize - 2.dp.toPx()),
        cornerRadius = cornerRadius,
        style = Stroke(width = 1.dp.toPx())
    )
}

private fun DrawScope.drawFilledCell(topLeft: Offset, cellSize: Float, variant: Int) {
    drawTacticalBlock(
        topLeft = topLeft,
        cellSize = cellSize,
        variant = variant
    )
}
