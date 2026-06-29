package com.example.gridfall.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gridfall.ui.theme.GridfallColors
import com.example.gridfall.ui.theme.RetroArcadeColors

internal fun GridfallColors.isRetroTheme(): Boolean = this == RetroArcadeColors

internal fun TextStyle.retroText(colors: GridfallColors): TextStyle {
    return if (colors.isRetroTheme()) copy(fontFamily = FontFamily.Monospace) else this
}

internal fun retroCorner(colors: GridfallColors, default: Dp): Dp {
    return if (colors.isRetroTheme()) 6.dp else default
}

internal fun Modifier.retroAppTexture(colors: GridfallColors): Modifier {
    if (!colors.isRetroTheme()) return this

    return drawWithCache {
        onDrawBehind {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(colors.accentStrong.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.82f, size.height * 0.18f),
                    radius = size.minDimension * 0.72f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(colors.accent.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 0.10f, size.height * 0.72f),
                    radius = size.minDimension * 0.62f
                )
            )
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        colors.accent.copy(alpha = 0.055f),
                        colors.accentStrong.copy(alpha = 0.045f),
                        Color.Transparent
                    ),
                    start = Offset(0f, size.height * 0.18f),
                    end = Offset(size.width, size.height * 0.78f)
                )
            )
            drawCrtScanlines(colors = colors, alphaScale = 0.76f)
            drawArcadeGrid(colors = colors, alphaScale = 0.42f)
        }
    }
}

internal fun Modifier.retroPanelTexture(colors: GridfallColors): Modifier {
    if (!colors.isRetroTheme()) return this

    return drawWithCache {
        onDrawBehind {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.accent.copy(alpha = 0.07f),
                        Color.Transparent,
                        colors.accentStrong.copy(alpha = 0.045f)
                    )
                )
            )
            drawCrtScanlines(colors = colors, alphaScale = 0.34f)
            drawArcadeCornerTicks(colors = colors, alphaScale = 0.82f)
        }
    }
}

internal fun Modifier.retroBoardFrameTexture(colors: GridfallColors): Modifier {
    if (!colors.isRetroTheme()) return this

    return drawWithCache {
        onDrawBehind {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(colors.accent.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(size.width * 0.50f, size.height * 0.52f),
                    radius = size.minDimension * 0.76f
                )
            )
            drawArcadeCornerTicks(colors = colors, alphaScale = 1f)
            drawRect(
                color = colors.accent.copy(alpha = 0.26f),
                topLeft = Offset(size.width * 0.08f, size.height * 0.045f),
                size = Size(size.width * 0.84f, 2f)
            )
            drawRect(
                color = colors.accentStrong.copy(alpha = 0.22f),
                topLeft = Offset(size.width * 0.08f, size.height - 4f),
                size = Size(size.width * 0.84f, 2f)
            )
        }
    }
}

internal fun Modifier.retroBoardWellTexture(colors: GridfallColors): Modifier {
    if (!colors.isRetroTheme()) return this

    return drawWithCache {
        onDrawBehind {
            drawCrtScanlines(colors = colors, alphaScale = 0.30f)
            drawArcadeGrid(colors = colors, alphaScale = 0.22f)
        }
    }
}

internal fun Modifier.retroSlotTexture(
    colors: GridfallColors,
    active: Boolean
): Modifier {
    if (!colors.isRetroTheme()) return this

    val intensity = if (active) 1f else 0.70f
    return drawWithCache {
        onDrawBehind {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(colors.accent.copy(alpha = 0.13f * intensity), Color.Transparent),
                    center = Offset(size.width * 0.50f, size.height * 0.50f),
                    radius = size.minDimension * 0.70f
                )
            )
            drawCrtScanlines(colors = colors, alphaScale = 0.30f * intensity)
            drawArcadeCornerTicks(colors = colors, alphaScale = 0.70f * intensity)
        }
    }
}

internal fun DrawScope.drawRetroEmptyCell(
    topLeft: Offset,
    cellSize: Float,
    colors: GridfallColors
) {
    val cornerRadius = CornerRadius(cellSize * 0.045f, cellSize * 0.045f)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                colors.emptyCell.copy(alpha = 0.98f),
                colors.boardInner.copy(alpha = 0.94f)
            ),
            startY = topLeft.y,
            endY = topLeft.y + cellSize
        ),
        topLeft = topLeft,
        size = Size(cellSize, cellSize),
        cornerRadius = cornerRadius
    )
    drawRoundRect(
        color = colors.emptyCellBorder.copy(alpha = 0.82f),
        topLeft = topLeft,
        size = Size(cellSize, cellSize),
        cornerRadius = cornerRadius,
        style = Stroke(width = (cellSize * 0.028f).coerceAtLeast(1f))
    )
    drawLine(
        color = colors.accent.copy(alpha = 0.18f),
        start = topLeft + Offset(cellSize * 0.12f, cellSize * 0.18f),
        end = topLeft + Offset(cellSize * 0.88f, cellSize * 0.18f),
        strokeWidth = (cellSize * 0.020f).coerceAtLeast(1f)
    )
}

internal fun DrawScope.drawRetroLineClearSweep(
    clearedRows: List<Int>,
    clearedColumns: List<Int>,
    progress: Float,
    spacing: Float,
    cellSize: Float,
    colors: GridfallColors
) {
    val fade = (1f - progress).coerceIn(0f, 1f)
    val beamLength = cellSize * 1.30f
    val trailLength = cellSize * 2.20f
    val boardSpan = spacing + 8f * (cellSize + spacing)

    clearedRows.forEachIndexed { index, row ->
        val rowTop = spacing + row * (cellSize + spacing)
        val rowCenter = rowTop + cellSize / 2f
        val x = -beamLength + (boardSpan + beamLength) * progress
        val accent = if (index % 2 == 0) colors.accent else colors.accentStrong

        drawRoundRect(
            color = colors.accent.copy(alpha = 0.13f * fade),
            topLeft = Offset(0f, rowTop),
            size = Size(size.width, cellSize),
            cornerRadius = CornerRadius(cellSize * 0.04f, cellSize * 0.04f)
        )
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    accent.copy(alpha = 0.20f * fade),
                    colors.warning.copy(alpha = 0.44f * fade),
                    Color.White.copy(alpha = 0.55f * fade),
                    Color.Transparent
                ),
                startX = x - trailLength,
                endX = x + beamLength
            ),
            topLeft = Offset((x - trailLength).coerceAtLeast(0f), rowTop + cellSize * 0.12f),
            size = Size((trailLength + beamLength).coerceAtMost(size.width), cellSize * 0.76f)
        )
        drawRect(
            color = colors.warning.copy(alpha = 0.88f * fade),
            topLeft = Offset(x.coerceIn(0f, size.width), rowTop),
            size = Size((cellSize * 0.10f).coerceAtLeast(3f), cellSize)
        )
        drawRetroSweepPixels(
            horizontal = true,
            lead = x,
            center = rowCenter,
            cellSize = cellSize,
            fade = fade,
            colors = colors
        )
    }

    clearedColumns.forEachIndexed { index, col ->
        val colLeft = spacing + col * (cellSize + spacing)
        val colCenter = colLeft + cellSize / 2f
        val y = -beamLength + (boardSpan + beamLength) * progress
        val accent = if (index % 2 == 0) colors.accentStrong else colors.accent

        drawRoundRect(
            color = colors.accentStrong.copy(alpha = 0.12f * fade),
            topLeft = Offset(colLeft, 0f),
            size = Size(cellSize, size.height),
            cornerRadius = CornerRadius(cellSize * 0.04f, cellSize * 0.04f)
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    accent.copy(alpha = 0.20f * fade),
                    colors.warning.copy(alpha = 0.44f * fade),
                    Color.White.copy(alpha = 0.55f * fade),
                    Color.Transparent
                ),
                startY = y - trailLength,
                endY = y + beamLength
            ),
            topLeft = Offset(colLeft + cellSize * 0.12f, (y - trailLength).coerceAtLeast(0f)),
            size = Size(cellSize * 0.76f, (trailLength + beamLength).coerceAtMost(size.height))
        )
        drawRect(
            color = colors.warning.copy(alpha = 0.88f * fade),
            topLeft = Offset(colLeft, y.coerceIn(0f, size.height)),
            size = Size(cellSize, (cellSize * 0.10f).coerceAtLeast(3f))
        )
        drawRetroSweepPixels(
            horizontal = false,
            lead = y,
            center = colCenter,
            cellSize = cellSize,
            fade = fade,
            colors = colors
        )
    }
}

internal fun DrawScope.drawRetroBombShatter(
    center: Offset,
    cellSize: Float,
    progress: Float,
    colors: GridfallColors
) {
    val fade = (1f - progress).coerceIn(0f, 1f)
    val burstRadius = cellSize * (0.38f + progress * 2.55f)

    drawCircle(
        color = colors.bombGlow.copy(alpha = 0.24f * fade),
        radius = cellSize * (0.55f + progress * 1.85f),
        center = center,
        style = Stroke(width = (cellSize * 0.11f * fade).coerceAtLeast(1f))
    )
    drawCircle(
        color = colors.warning.copy(alpha = 0.30f * fade),
        radius = cellSize * (0.22f + progress * 0.80f),
        center = center
    )

    repeat(18) { index ->
        val direction = shatterDirections[index]
        val travel = burstRadius * shatterSpeeds[index]
        val fragmentCenter = center + Offset(direction.x * travel, direction.y * travel)
        val fragmentSize = cellSize * (0.080f + (index % 3) * 0.028f) * (0.70f + fade * 0.50f)
        val fragmentColor = when (index % 3) {
            0 -> colors.accent
            1 -> colors.accentStrong
            else -> colors.warning
        }.copy(alpha = 0.84f * fade)

        if (index % 2 == 0) {
            val path = Path().apply {
                moveTo(fragmentCenter.x, fragmentCenter.y - fragmentSize)
                lineTo(fragmentCenter.x + fragmentSize * 0.82f, fragmentCenter.y + fragmentSize * 0.55f)
                lineTo(fragmentCenter.x - fragmentSize * 0.66f, fragmentCenter.y + fragmentSize * 0.72f)
                close()
            }
            drawPath(path = path, color = fragmentColor)
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.24f * fade),
                style = Stroke(width = (fragmentSize * 0.18f).coerceAtLeast(1f))
            )
        } else {
            drawRect(
                color = Color.Black.copy(alpha = 0.26f * fade),
                topLeft = fragmentCenter + Offset(fragmentSize * 0.20f, fragmentSize * 0.20f),
                size = Size(fragmentSize * 1.25f, fragmentSize * 1.25f)
            )
            drawRect(
                color = fragmentColor,
                topLeft = fragmentCenter - Offset(fragmentSize * 0.55f, fragmentSize * 0.55f),
                size = Size(fragmentSize * 1.10f, fragmentSize * 1.10f)
            )
        }
    }
}

private fun DrawScope.drawRetroSweepPixels(
    horizontal: Boolean,
    lead: Float,
    center: Float,
    cellSize: Float,
    fade: Float,
    colors: GridfallColors
) {
    repeat(6) { index ->
        val offset = cellSize * (0.28f + index * 0.24f)
        val pixelSize = (cellSize * (0.055f + (index % 2) * 0.020f)).coerceAtLeast(2f)
        val tone = when (index % 3) {
            0 -> colors.accent
            1 -> colors.accentStrong
            else -> colors.warning
        }
        val jitter = if (index % 2 == 0) -cellSize * 0.20f else cellSize * 0.18f
        val topLeft = if (horizontal) {
            Offset(lead - offset, center + jitter)
        } else {
            Offset(center + jitter, lead - offset)
        }

        drawRect(
            color = tone.copy(alpha = 0.58f * fade),
            topLeft = topLeft,
            size = Size(pixelSize, pixelSize)
        )
    }
}

private fun DrawScope.drawCrtScanlines(
    colors: GridfallColors,
    alphaScale: Float
) {
    val gap = 8f
    var y = 0f
    while (y < size.height) {
        drawRect(
            color = colors.accent.copy(alpha = 0.030f * alphaScale),
            topLeft = Offset(0f, y),
            size = Size(size.width, 1f)
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.060f * alphaScale),
            topLeft = Offset(0f, y + 3f),
            size = Size(size.width, 1f)
        )
        y += gap
    }
}

private val shatterDirections = listOf(
    Offset(1.00f, 0.00f),
    Offset(0.86f, 0.50f),
    Offset(0.50f, 0.86f),
    Offset(0.00f, 1.00f),
    Offset(-0.50f, 0.86f),
    Offset(-0.86f, 0.50f),
    Offset(-1.00f, 0.00f),
    Offset(-0.86f, -0.50f),
    Offset(-0.50f, -0.86f),
    Offset(0.00f, -1.00f),
    Offset(0.50f, -0.86f),
    Offset(0.86f, -0.50f),
    Offset(0.72f, 0.22f),
    Offset(0.22f, 0.72f),
    Offset(-0.72f, 0.22f),
    Offset(-0.22f, -0.72f),
    Offset(0.38f, -0.58f),
    Offset(-0.58f, -0.38f)
)

private val shatterSpeeds = listOf(
    1.00f, 0.82f, 1.08f, 0.74f, 0.96f, 1.12f,
    0.86f, 1.04f, 0.78f, 1.16f, 0.92f, 1.06f,
    0.58f, 0.66f, 0.72f, 0.62f, 0.88f, 0.80f
)

private fun DrawScope.drawArcadeGrid(
    colors: GridfallColors,
    alphaScale: Float
) {
    val gap = (size.minDimension * 0.145f).coerceAtLeast(44f)
    var x = 0f
    while (x < size.width) {
        drawLine(
            color = colors.accentStrong.copy(alpha = 0.040f * alphaScale),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
        x += gap
    }
    var y = 0f
    while (y < size.height) {
        drawLine(
            color = colors.accent.copy(alpha = 0.045f * alphaScale),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += gap
    }
}

private fun DrawScope.drawArcadeCornerTicks(
    colors: GridfallColors,
    alphaScale: Float
) {
    val tick = size.minDimension * 0.16f
    val stroke = (size.minDimension * 0.012f).coerceAtLeast(2f)
    val corners = listOf(
        Offset.Zero,
        Offset(size.width, 0f),
        Offset(0f, size.height),
        Offset(size.width, size.height)
    )

    corners.forEach { corner ->
        val xDirection = if (corner.x == 0f) 1f else -1f
        val yDirection = if (corner.y == 0f) 1f else -1f
        val startX = corner.x + xDirection * stroke
        val startY = corner.y + yDirection * stroke
        val path = Path().apply {
            moveTo(startX, startY + yDirection * tick)
            lineTo(startX, startY)
            lineTo(startX + xDirection * tick, startY)
        }
        drawPath(
            path = path,
            color = colors.accentStrong.copy(alpha = 0.42f * alphaScale),
            style = Stroke(width = stroke)
        )
        drawPath(
            path = path,
            color = colors.accent.copy(alpha = 0.18f * alphaScale),
            style = Stroke(width = stroke * 2f)
        )
    }
}
