package com.example.gridfall.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.gridfall.game.Board
import com.example.gridfall.game.Cell
import com.example.gridfall.game.GameEngine
import com.example.gridfall.game.PieceEffect
import com.example.gridfall.ui.theme.GridfallColors
import com.example.gridfall.ui.theme.*

@Composable
fun BoardCanvas(
    board: Board,
    modifier: Modifier = Modifier,
    placementPreview: PlacementPreview? = null,
    lineClearFeedback: LineClearFeedback? = null,
    bombPulseFeedback: BombPulseFeedback? = null,
    blockBreakerFeedback: BlockBreakerFeedback? = null,
    contractWarningCells: Set<Cell> = emptySet(),
    blockBreakerTargetCells: Set<Cell> = emptySet(),
    onBlockBreakerCellTapped: (Cell) -> Unit = {},
    onBoardLayoutChanged: (BoardLayoutInfo) -> Unit = {}
) {
    val linePulse = remember { Animatable(1f) }
    val bombPulse = remember { Animatable(1f) }
    val blockBreakerPulse = remember { Animatable(1f) }
    val density = LocalDensity.current
    val theme = LocalGridfallColors.current
    val outerShape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 22.dp)))
    val innerShape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 14.dp)))

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

    LaunchedEffect(blockBreakerFeedback?.token) {
        if (blockBreakerFeedback != null) {
            blockBreakerPulse.snapTo(0f)
            blockBreakerPulse.animateTo(1f, animationSpec = tween(durationMillis = 360))
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(
                brush = Brush.verticalGradient(
                    listOf(theme.panelBackground, theme.tacticalFrame)
                ),
                shape = outerShape
            )
            .infernoBoardFrameTexture(theme)
            .retroBoardFrameTexture(theme)
            .blockworldFrameTexture(theme)
            .border(
                if (theme.isRetroTheme() || theme.isInfernoTheme() || theme.isBlockworldTheme()) 2.dp else 1.dp,
                theme.panelBorder.copy(alpha = if (theme.isInfernoTheme()) 0.92f else 1f),
                outerShape
            )
            .padding(
                when {
                    theme.isRetroTheme() -> 10.dp
                    theme.isInfernoTheme() -> 12.dp
                    theme.isBlockworldTheme() -> 10.dp
                    else -> 8.dp
                }
            )
            .clip(innerShape)
            .background(theme.boardInner)
            .infernoBoardWellTexture(theme)
            .retroBoardWellTexture(theme)
            .blockworldWellTexture(theme)
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
        val targetSpacingPx = with(density) { 4.dp.toPx() }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (blockBreakerTargetCells.isNotEmpty()) {
                        Modifier.pointerInput(blockBreakerTargetCells) {
                            detectTapGestures { offset ->
                                val spacing = targetSpacingPx
                                val cellSize = (size.width - spacing * (Board.SIZE + 1)) / Board.SIZE
                                if (cellSize <= 0f || offset.x < spacing || offset.y < spacing) return@detectTapGestures
                                val col = ((offset.x - spacing) / (cellSize + spacing)).toInt()
                                val row = ((offset.y - spacing) / (cellSize + spacing)).toInt()
                                if (row !in 0 until Board.SIZE || col !in 0 until Board.SIZE) return@detectTapGestures
                                val cellX = offset.x - spacing - col * (cellSize + spacing)
                                val cellY = offset.y - spacing - row * (cellSize + spacing)
                                if (cellX in 0f..cellSize && cellY in 0f..cellSize) onBlockBreakerCellTapped(Cell(row, col))
                            }
                        }
                    } else Modifier
                )
        ) {
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
                        drawEmptyCell(topLeft, cellSize, theme)
                    } else {
                        drawFilledCell(topLeft, cellSize, cellValue, theme)
                    }
                }
            }

            // Contract warning zones are guidance only; placement previews draw over them.
            contractWarningCells.forEach { cell ->
                if (cell.row in 0 until Board.SIZE && cell.col in 0 until Board.SIZE) {
                    val topLeft = Offset(
                        x = spacing + cell.col * (cellSize + spacing),
                        y = spacing + cell.row * (cellSize + spacing)
                    )
                    drawContractWarningCell(topLeft, cellSize, theme)
                }
            }

            blockBreakerTargetCells.forEach { cell ->
                if (cell.row in 0 until Board.SIZE && cell.col in 0 until Board.SIZE) {
                    val topLeft = Offset(spacing + cell.col * (cellSize + spacing), spacing + cell.row * (cellSize + spacing))
                    drawRoundRect(theme.warning.copy(alpha = 0.12f), topLeft, Size(cellSize, cellSize), cornerRadius)
                    drawRoundRect(theme.warning.copy(alpha = 0.72f), topLeft, Size(cellSize, cellSize), cornerRadius, style = Stroke(width = 1.5.dp.toPx()))
                }
            }

            // Placement Preview
            placementPreview?.let { preview ->
                val previewColor = if (!preview.isValid) theme.invalidPreviewBorder else theme.validPreviewBorder
                val previewFill = if (!preview.isValid) theme.invalidPreviewFill else theme.validPreviewFill
                val previewCornerRadius = if (theme.isBlockworldTheme()) {
                    CornerRadius.Zero
                } else if (theme.isRetroTheme()) {
                    CornerRadius(cellSize * 0.045f, cellSize * 0.045f)
                } else if (theme.isInfernoTheme()) {
                    CornerRadius(cellSize * 0.060f, cellSize * 0.060f)
                } else {
                    cornerRadius
                }

                if (
                    (preview.piece.effect == PieceEffect.Bomb || preview.piece.effect == PieceEffect.MegaBomb) &&
                    preview.isValid
                ) {
                    val affectedCells = if (preview.piece.effect == PieceEffect.MegaBomb) {
                        GameEngine.megaBombAffectedCells(preview.originRow, preview.originCol)
                    } else {
                        (preview.originRow - 1..preview.originRow + 1).flatMap { row ->
                            (preview.originCol - 1..preview.originCol + 1).map { col ->
                                Cell(row, col)
                            }
                        }
                    }

                    affectedCells.forEach { cell ->
                        if (cell.row in 0 until Board.SIZE && cell.col in 0 until Board.SIZE) {
                            val topLeft = Offset(
                                x = spacing + cell.col * (cellSize + spacing),
                                y = spacing + cell.row * (cellSize + spacing)
                            )
                            drawRoundRect(
                                color = previewFill,
                                topLeft = topLeft,
                                size = Size(cellSize, cellSize),
                                cornerRadius = previewCornerRadius
                            )
                            drawRoundRect(
                                color = previewColor,
                                topLeft = topLeft,
                                size = Size(cellSize, cellSize),
                                cornerRadius = previewCornerRadius,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                            )
                            if (theme.isInfernoTheme()) {
                                drawRoundRect(
                                    color = previewColor.copy(alpha = if (preview.isValid) 0.30f else 0.42f),
                                    topLeft = topLeft - Offset(1.dp.toPx(), 1.dp.toPx()),
                                    size = Size(cellSize + 2.dp.toPx(), cellSize + 2.dp.toPx()),
                                    cornerRadius = previewCornerRadius,
                                    style = Stroke(width = 1.dp.toPx())
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
                            cornerRadius = previewCornerRadius
                        )
                        drawRoundRect(
                            color = previewColor,
                            topLeft = topLeft,
                            size = Size(cellSize, cellSize),
                            cornerRadius = previewCornerRadius,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )
                        if (theme.isInfernoTheme()) {
                            drawRoundRect(
                                color = previewColor.copy(alpha = if (preview.isValid) 0.30f else 0.42f),
                                topLeft = topLeft - Offset(1.dp.toPx(), 1.dp.toPx()),
                                size = Size(cellSize + 2.dp.toPx(), cellSize + 2.dp.toPx()),
                                cornerRadius = previewCornerRadius,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                    }
                }
            }

            // Line Clear Feedback
            lineClearFeedback?.let { feedback ->
                val progress = linePulse.value.coerceIn(0f, 1f)
                if (theme.isRetroTheme()) {
                    drawRetroLineClearSweep(
                        clearedRows = feedback.clearedRows,
                        clearedColumns = feedback.clearedColumns,
                        progress = progress,
                        spacing = spacing,
                        cellSize = cellSize,
                        colors = theme
                    )
                } else if (theme.isInfernoTheme()) {
                    drawInfernoLineClearBurn(
                        clearedRows = feedback.clearedRows,
                        clearedColumns = feedback.clearedColumns,
                        progress = progress,
                        spacing = spacing,
                        cellSize = cellSize,
                        colors = theme
                    )
                } else {
                    val fade = (1f - progress).coerceIn(0f, 1f)
                    val fillColor = theme.accent.copy(alpha = 0.18f * fade)
                    val glowColor = theme.accent.copy(alpha = 0.26f * fade)
                    val railColor = theme.success.copy(alpha = 0.70f * fade)
                    val strokeColor = theme.accent.copy(alpha = 0.48f * fade)
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
            }

            blockBreakerFeedback?.let { feedback ->
                val progress = blockBreakerPulse.value.coerceIn(0f, 1f)
                val fade = (1f - progress).coerceIn(0f, 1f)
                val center = Offset(spacing + feedback.col * (cellSize + spacing) + cellSize / 2f, spacing + feedback.row * (cellSize + spacing) + cellSize / 2f)
                drawCircle(theme.warning.copy(alpha = 0.78f * fade), cellSize * (0.20f + progress * 0.62f), center, style = Stroke(width = (cellSize * 0.10f * fade).coerceAtLeast(1.dp.toPx())))
                val crack = cellSize * (0.18f + progress * 0.16f)
                drawLine(theme.textPrimary.copy(alpha = 0.72f * fade), center - Offset(crack, crack), center + Offset(crack, crack), strokeWidth = 1.5.dp.toPx())
                drawLine(theme.textPrimary.copy(alpha = 0.72f * fade), center + Offset(crack, -crack), center + Offset(-crack, crack), strokeWidth = 1.5.dp.toPx())
            }

            bombPulseFeedback?.let { feedback ->
                val progress = bombPulse.value.coerceIn(0f, 1f)
                val fade = (1f - progress).coerceIn(0f, 1f)
                val centerX = spacing + feedback.centerCol * (cellSize + spacing) + cellSize / 2f
                val centerY = spacing + feedback.centerRow * (cellSize + spacing) + cellSize / 2f
                val center = Offset(centerX, centerY)
                val affectedFill = theme.bombInner.copy(alpha = 0.20f * fade)
                val affectedStroke = theme.warning.copy(alpha = 0.48f * fade)

                val affectedCells = if (feedback.isMega) {
                    GameEngine.megaBombAffectedCells(feedback.centerRow, feedback.centerCol)
                } else {
                    (feedback.centerRow - 1..feedback.centerRow + 1).flatMap { row ->
                        (feedback.centerCol - 1..feedback.centerCol + 1).map { col ->
                            Cell(row, col)
                        }
                    }
                }

                affectedCells.forEach { cell ->
                    if (cell.row in 0 until Board.SIZE && cell.col in 0 until Board.SIZE) {
                        val topLeft = Offset(
                            x = spacing + cell.col * (cellSize + spacing),
                            y = spacing + cell.row * (cellSize + spacing)
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

                drawCircle(
                    color = theme.bombInner.copy(alpha = 0.20f * fade),
                    radius = cellSize * ((if (feedback.isMega) 1.05f else 0.65f) + progress * (if (feedback.isMega) 2.20f else 1.75f)),
                    center = center,
                    style = Stroke(width = (4.dp.toPx() * fade).coerceAtLeast(1.dp.toPx()))
                )
                drawCircle(
                    color = theme.bombCore.copy(alpha = 0.34f * fade),
                    radius = cellSize * (0.30f + progress * 0.80f),
                    center = center
                )
                if (theme.isRetroTheme()) {
                    drawRetroBombShatter(
                        center = center,
                        cellSize = cellSize,
                        progress = progress,
                        colors = theme
                    )
                } else if (theme.isInfernoTheme()) {
                    drawInfernoBombBurst(
                        center = center,
                        cellSize = cellSize,
                        progress = progress,
                        colors = theme
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawEmptyCell(
    topLeft: Offset,
    cellSize: Float,
    colors: GridfallColors
) {
    val cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())

    if (colors.isRetroTheme()) {
        drawRetroEmptyCell(topLeft, cellSize, colors)
        return
    }

    if (colors.isInfernoTheme()) {
        drawInfernoEmptyCell(topLeft, cellSize, colors)
        return
    }

    // Background fill
    drawRoundRect(
        color = colors.emptyCell,
        topLeft = topLeft,
        size = Size(cellSize, cellSize),
        cornerRadius = cornerRadius
    )
    
    // Subtle border
    drawRoundRect(
        color = colors.emptyCellBorder.copy(alpha = 0.34f),
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

private fun DrawScope.drawBlockworldEmptyCell(topLeft: Offset, cellSize: Float, colors: GridfallColors) {
    drawRect(color = Color.Black.copy(alpha = 0.24f), topLeft = topLeft + Offset(cellSize * 0.05f, cellSize * 0.06f), size = Size(cellSize, cellSize))
    drawRect(color = colors.emptyCell, topLeft = topLeft, size = Size(cellSize, cellSize))
    drawRect(color = colors.emptyCell.copy(alpha = 0.64f), topLeft = topLeft, size = Size(cellSize, cellSize * 0.18f))
    drawRect(color = colors.boardInner.copy(alpha = 0.66f), topLeft = topLeft + Offset(0f, cellSize * 0.82f), size = Size(cellSize, cellSize * 0.18f))
    drawRect(color = colors.emptyCellBorder.copy(alpha = 0.88f), topLeft = topLeft, size = Size(cellSize, cellSize), style = Stroke(width = (cellSize * 0.045f).coerceAtLeast(1f)))
}
private fun DrawScope.drawFilledCell(
    topLeft: Offset,
    cellSize: Float,
    variant: Int,
    colors: GridfallColors
) {
    drawTacticalBlock(
        topLeft = topLeft,
        cellSize = cellSize,
        variant = variant,
        colors = colors
    )
}

private fun DrawScope.drawContractWarningCell(
    topLeft: Offset,
    cellSize: Float,
    colors: GridfallColors
) {
    val cornerRadius = if (colors.isBlockworldTheme()) {
        CornerRadius.Zero
    } else if (colors.isInfernoTheme()) {
        CornerRadius(cellSize * 0.060f, cellSize * 0.060f)
    } else {
        CornerRadius(8.dp.toPx(), 8.dp.toPx())
    }

    drawRoundRect(
        color = colors.contractWarningFill,
        topLeft = topLeft,
        size = Size(cellSize, cellSize),
        cornerRadius = cornerRadius
    )
    drawRoundRect(
        color = colors.contractWarningBorder,
        topLeft = topLeft + Offset(1.dp.toPx(), 1.dp.toPx()),
        size = Size(cellSize - 2.dp.toPx(), cellSize - 2.dp.toPx()),
        cornerRadius = cornerRadius,
        style = Stroke(width = 1.dp.toPx())
    )
    drawRect(
        color = colors.contractWarningBorder.copy(alpha = 0.44f),
        topLeft = topLeft + Offset(0f, cellSize * 0.78f),
        size = Size(cellSize, 1.5.dp.toPx())
    )
}
