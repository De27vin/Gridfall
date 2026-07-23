package com.example.gridfall.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gridfall.ui.theme.GridfallColors
import com.example.gridfall.ui.theme.InfernoCoreColors

internal fun GridfallColors.isInfernoTheme(): Boolean = this == InfernoCoreColors

internal fun infernoCorner(colors: GridfallColors, default: Dp): Dp {
    return when { colors.isBlockworldTheme() -> 0.dp; colors.isFrutigerAeroTheme() -> 28.dp; colors.isInfernoTheme() -> 8.dp; else -> default }
}

internal fun infernoBorderWidth(colors: GridfallColors, default: Dp): Dp {
    return if (colors.isInfernoTheme() || colors.isBlockworldTheme() || colors.isFrutigerAeroTheme()) 2.dp else default
}

internal fun Modifier.infernoAppTexture(colors: GridfallColors): Modifier {
    if (colors.isFrutigerAeroTheme()) return frutigerAeroAppTexture(colors)
    if (colors.isBlockworldTheme()) return blockworldFrameTexture(colors)
    if (!colors.isInfernoTheme()) return this

    return drawWithCache {
        onDrawBehind {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.accent.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.82f, size.height * 0.16f),
                    radius = size.minDimension * 0.72f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.danger.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.08f, size.height * 0.88f),
                    radius = size.minDimension * 0.62f
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        colors.accent.copy(alpha = 0.055f),
                        colors.warning.copy(alpha = 0.085f)
                    ),
                    startY = size.height * 0.42f,
                    endY = size.height
                )
            )
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        colors.accent.copy(alpha = 0.055f),
                        Color.Transparent
                    ),
                    start = Offset(0f, size.height * 0.18f),
                    end = Offset(size.width, size.height * 0.62f)
                )
            )
            drawInfernoCracks(colors = colors, alphaScale = 0.80f)
        }
    }
}

internal fun Modifier.infernoPanelTexture(colors: GridfallColors): Modifier {
    if (colors.isBlockworldTheme()) return blockworldFrameTexture(colors)
    if (!colors.isInfernoTheme()) return this

    return drawWithCache {
        onDrawBehind {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.accent.copy(alpha = 0.10f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.18f)
                    )
                )
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.16f),
                topLeft = Offset(0f, size.height * 0.82f),
                size = Size(size.width, size.height * 0.18f)
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.warning.copy(alpha = 0.13f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.18f, 0f),
                    radius = size.minDimension * 0.86f
                )
            )
            drawInfernoCracks(colors = colors, alphaScale = 0.82f)
            drawMoltenRim(colors = colors, alphaScale = 0.88f)
            drawForgedCornerCuts(colors = colors, alphaScale = 0.70f)
        }
    }
}

internal fun Modifier.infernoBoardFrameTexture(colors: GridfallColors): Modifier {
    if (colors.isBlockworldTheme()) return blockworldFrameTexture(colors)
    if (!colors.isInfernoTheme()) return this

    return drawWithCache {
        onDrawBehind {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.accent.copy(alpha = 0.23f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.50f, size.height * 0.03f),
                    radius = size.width * 0.76f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.warning.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.50f, size.height * 0.98f),
                    radius = size.width * 0.70f
                )
            )
            drawMoltenRim(colors = colors, alphaScale = 1.18f)
            drawInfernoCracks(colors = colors, alphaScale = 0.72f)
            drawForgedCornerCuts(colors = colors, alphaScale = 0.92f)
        }
    }
}

internal fun Modifier.infernoBoardWellTexture(colors: GridfallColors): Modifier {
    if (colors.isBlockworldTheme()) return blockworldFrameTexture(colors)
    if (!colors.isInfernoTheme()) return this

    return drawWithCache {
        onDrawBehind {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.accent.copy(alpha = 0.050f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.24f)
                    )
                )
            )
            drawInfernoCracks(colors = colors, alphaScale = 0.18f)
        }
    }
}

internal fun Modifier.infernoSlotTexture(
    colors: GridfallColors,
    active: Boolean
): Modifier {
    if (colors.isBlockworldTheme()) return blockworldFrameTexture(colors)
    if (!colors.isInfernoTheme()) return this

    val intensity = if (active) 1f else 0.64f
    return drawWithCache {
        onDrawBehind {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.accentStrong.copy(alpha = 0.16f * intensity),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.50f, size.height * 0.12f),
                    radius = size.minDimension * 0.72f
                )
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.18f),
                topLeft = Offset(0f, size.height * 0.78f),
                size = Size(size.width, size.height * 0.22f)
            )
            drawInfernoCracks(colors = colors, alphaScale = 0.62f * intensity)
            drawForgedCornerCuts(colors = colors, alphaScale = 0.62f * intensity)
            drawLine(
                color = colors.accent.copy(alpha = 0.32f * intensity),
                start = Offset(size.width * 0.10f, size.height * 0.88f),
                end = Offset(size.width * 0.90f, size.height * 0.80f),
                strokeWidth = (size.minDimension * 0.024f).coerceAtLeast(2f)
            )
        }
    }
}

internal fun DrawScope.drawInfernoEmptyCell(
    topLeft: Offset,
    cellSize: Float,
    colors: GridfallColors
) {
    val corner = cellSize * 0.055f
    val cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
    val inset = cellSize * 0.055f

    drawRoundRect(
        color = Color.Black.copy(alpha = 0.30f),
        topLeft = topLeft + Offset(cellSize * 0.035f, cellSize * 0.045f),
        size = Size(cellSize, cellSize),
        cornerRadius = cornerRadius
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                colors.emptyCell.copy(alpha = 0.98f),
                colors.boardInner.copy(alpha = 0.94f),
                Color.Black.copy(alpha = 0.38f)
            ),
            startY = topLeft.y,
            endY = topLeft.y + cellSize
        ),
        topLeft = topLeft,
        size = Size(cellSize, cellSize),
        cornerRadius = cornerRadius
    )
    drawRoundRect(
        color = colors.emptyCellBorder.copy(alpha = 0.42f),
        topLeft = topLeft,
        size = Size(cellSize, cellSize),
        cornerRadius = cornerRadius,
        style = Stroke(width = (cellSize * 0.030f).coerceAtLeast(1f))
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.42f),
        topLeft = topLeft + Offset(inset, inset),
        size = Size(cellSize - inset * 2f, cellSize - inset * 2f),
        cornerRadius = cornerRadius,
        style = Stroke(width = (cellSize * 0.018f).coerceAtLeast(1f))
    )
    drawLine(
        color = colors.accent.copy(alpha = 0.09f),
        start = topLeft + Offset(cellSize * 0.14f, cellSize * 0.24f),
        end = topLeft + Offset(cellSize * 0.60f, cellSize * 0.68f),
        strokeWidth = (cellSize * 0.018f).coerceAtLeast(1f)
    )
    drawLine(
        color = colors.warning.copy(alpha = 0.05f),
        start = topLeft + Offset(cellSize * 0.64f, cellSize * 0.16f),
        end = topLeft + Offset(cellSize * 0.84f, cellSize * 0.34f),
        strokeWidth = (cellSize * 0.014f).coerceAtLeast(1f)
    )
}

internal fun DrawScope.drawInfernoLineClearBurn(
    clearedRows: List<Int>,
    clearedColumns: List<Int>,
    progress: Float,
    spacing: Float,
    cellSize: Float,
    colors: GridfallColors
) {
    val fade = (1f - progress).coerceIn(0f, 1f)
    val scanOffset = cellSize * progress
    val moltenFill = colors.accent.copy(alpha = 0.20f * fade)
    val moltenEdge = colors.warning.copy(alpha = 0.62f * fade)
    val hotCore = colors.accentStrong.copy(alpha = 0.36f * fade)

    clearedRows.forEach { row ->
        if (row in 0 until 8) {
            val topLeft = Offset(0f, spacing + row * (cellSize + spacing))
            drawRect(
                color = moltenFill,
                topLeft = topLeft,
                size = Size(size.width, cellSize)
            )
            drawRect(
                color = hotCore,
                topLeft = topLeft + Offset(size.width * 0.18f, cellSize * 0.32f),
                size = Size(size.width * 0.64f, cellSize * 0.36f)
            )
            drawLine(
                color = moltenEdge,
                start = topLeft + Offset(0f, scanOffset.coerceAtMost(cellSize)),
                end = topLeft + Offset(size.width, (scanOffset + cellSize * 0.10f).coerceAtMost(cellSize)),
                strokeWidth = (cellSize * 0.070f).coerceAtLeast(2f)
            )
        }
    }

    clearedColumns.forEach { col ->
        if (col in 0 until 8) {
            val topLeft = Offset(spacing + col * (cellSize + spacing), 0f)
            drawRect(
                color = moltenFill,
                topLeft = topLeft,
                size = Size(cellSize, size.height)
            )
            drawRect(
                color = hotCore,
                topLeft = topLeft + Offset(cellSize * 0.32f, size.height * 0.18f),
                size = Size(cellSize * 0.36f, size.height * 0.64f)
            )
            drawLine(
                color = moltenEdge,
                start = topLeft + Offset(scanOffset.coerceAtMost(cellSize), 0f),
                end = topLeft + Offset((scanOffset + cellSize * 0.10f).coerceAtMost(cellSize), size.height),
                strokeWidth = (cellSize * 0.070f).coerceAtLeast(2f)
            )
        }
    }
}

internal fun DrawScope.drawInfernoBombBurst(
    center: Offset,
    cellSize: Float,
    progress: Float,
    colors: GridfallColors
) {
    val fade = (1f - progress).coerceIn(0f, 1f)
    repeat(2) { index ->
        drawCircle(
            color = colors.warning.copy(alpha = (0.38f - index * 0.12f) * fade),
            radius = cellSize * (0.70f + progress * (1.05f + index * 0.70f)),
            center = center,
            style = Stroke(width = (cellSize * (0.080f - index * 0.020f) * fade).coerceAtLeast(1f))
        )
    }
    repeat(8) { index ->
        val angle = index * 0.7853982f
        val length = cellSize * (0.58f + progress * 1.18f)
        val startLength = cellSize * (0.24f + progress * 0.28f)
        val start = Offset(
            x = center.x + kotlin.math.cos(angle) * startLength,
            y = center.y + kotlin.math.sin(angle) * startLength
        )
        val end = Offset(
            x = center.x + kotlin.math.cos(angle) * length,
            y = center.y + kotlin.math.sin(angle) * length
        )
        drawLine(
            color = colors.accentStrong.copy(alpha = 0.40f * fade),
            start = start,
            end = end,
            strokeWidth = (cellSize * 0.035f * fade).coerceAtLeast(1f)
        )
    }
}

private fun DrawScope.drawInfernoCracks(
    colors: GridfallColors,
    alphaScale: Float
) {
    val stroke = (size.minDimension * 0.006f).coerceAtLeast(1f)
    repeat(4) { index ->
        val y = size.height * (0.18f + index * 0.19f)
        val path = Path().apply {
            moveTo(size.width * -0.08f, y)
            cubicTo(
                size.width * 0.22f,
                y - size.height * 0.060f,
                size.width * 0.36f,
                y + size.height * 0.046f,
                size.width * 0.58f,
                y - size.height * 0.010f
            )
            cubicTo(
                size.width * 0.76f,
                y - size.height * 0.050f,
                size.width * 0.88f,
                y + size.height * 0.032f,
                size.width * 1.08f,
                y - size.height * 0.028f
            )
        }
        drawPath(
            path = path,
            color = colors.accent.copy(alpha = (0.030f - index * 0.003f) * alphaScale),
            style = Stroke(width = stroke)
        )
        drawPath(
            path = path,
            color = colors.warning.copy(alpha = (0.012f - index * 0.001f) * alphaScale),
            style = Stroke(width = stroke * 2.6f)
        )
    }

    repeat(5) { index ->
        val x = size.width * (0.14f + index * 0.18f)
        val y = size.height * (0.24f + (index % 2) * 0.32f)
        drawLine(
            color = colors.accentStrong.copy(alpha = 0.045f * alphaScale),
            start = Offset(x, y),
            end = Offset(x + size.width * 0.055f, y + size.height * 0.040f),
            strokeWidth = stroke * 0.75f
        )
    }
}

private fun DrawScope.drawMoltenRim(
    colors: GridfallColors,
    alphaScale: Float
) {
    val rimHeight = (size.minDimension * 0.020f).coerceAtLeast(2f)
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                colors.accent.copy(alpha = 0.15f * alphaScale),
                colors.warning.copy(alpha = 0.11f * alphaScale),
                Color.Transparent
            )
        ),
        topLeft = Offset(0f, 0f),
        size = Size(size.width, rimHeight)
    )
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                colors.danger.copy(alpha = 0.08f * alphaScale),
                colors.accent.copy(alpha = 0.12f * alphaScale),
                Color.Transparent
            )
        ),
        topLeft = Offset(0f, size.height - rimHeight),
        size = Size(size.width, rimHeight)
    )
}

private fun DrawScope.drawForgedCornerCuts(
    colors: GridfallColors,
    alphaScale: Float
) {
    val cut = size.minDimension * 0.16f
    val stroke = (size.minDimension * 0.012f).coerceAtLeast(1.5f)
    val cornerColor = colors.warning.copy(alpha = 0.16f * alphaScale)
    val shadowColor = Color.Black.copy(alpha = 0.26f * alphaScale)

    listOf(
        Offset(0f, cut) to Offset(cut, 0f),
        Offset(size.width - cut, 0f) to Offset(size.width, cut),
        Offset(0f, size.height - cut) to Offset(cut, size.height),
        Offset(size.width - cut, size.height) to Offset(size.width, size.height - cut)
    ).forEach { (start, end) ->
        drawLine(color = shadowColor, start = start, end = end, strokeWidth = stroke * 1.9f)
        drawLine(color = cornerColor, start = start, end = end, strokeWidth = stroke)
    }
}
