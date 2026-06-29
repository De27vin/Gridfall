package com.example.gridfall.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.gridfall.ui.theme.GridfallColors

internal fun DrawScope.drawTacticalBlock(
    topLeft: Offset,
    cellSize: Float,
    variant: Int,
    colors: GridfallColors,
    cornerRadiusFraction: Float = 0.16f
) {
    val corner = cellSize * cornerRadiusFraction
    val cornerRadius = CornerRadius(corner, corner)
    val palette = paletteForVariant(variant, colors)
    val isInferno = colors.isInfernoTheme()

    if (variant == 0) {
        drawRoundRect(
            color = colors.bombGlow.copy(alpha = if (isInferno) 0.62f else colors.bombGlow.alpha),
            topLeft = topLeft - Offset(cellSize * 0.08f, cellSize * 0.08f),
            size = Size(cellSize * 1.16f, cellSize * 1.16f),
            cornerRadius = CornerRadius(corner * 1.2f, corner * 1.2f)
        )
    }

    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(palette.top, palette.middle, palette.bottom),
            startY = topLeft.y,
            endY = topLeft.y + cellSize
        ),
        topLeft = topLeft,
        size = Size(cellSize, cellSize),
        cornerRadius = cornerRadius
    )

    if (isInferno) {
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.18f),
                    colors.accentStrong.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = topLeft + Offset(cellSize * 0.32f, cellSize * 0.18f),
                radius = cellSize * 0.72f
            ),
            topLeft = topLeft,
            size = Size(cellSize, cellSize),
            cornerRadius = cornerRadius
        )
        drawRoundRect(
            color = colors.accentStrong.copy(alpha = 0.26f),
            topLeft = topLeft + Offset(cellSize * 0.09f, cellSize * 0.09f),
            size = Size(cellSize * 0.82f, cellSize * 0.10f),
            cornerRadius = CornerRadius(corner * 0.70f, corner * 0.70f)
        )
        drawLine(
            color = colors.warning.copy(alpha = 0.22f),
            start = topLeft + Offset(cellSize * 0.14f, cellSize * 0.80f),
            end = topLeft + Offset(cellSize * 0.82f, cellSize * 0.72f),
            strokeWidth = (cellSize * 0.035f).coerceAtLeast(1f)
        )
    }

    drawRoundRect(
        color = Color.White.copy(alpha = if (isInferno) 0.16f else 0.22f),
        topLeft = topLeft + Offset(cellSize * 0.07f, cellSize * 0.07f),
        size = Size(cellSize * 0.86f, cellSize * 0.28f),
        cornerRadius = cornerRadius
    )

    drawRoundRect(
        color = Color.Black.copy(alpha = if (isInferno) 0.26f else 0.20f),
        topLeft = topLeft + Offset(0f, cellSize * 0.70f),
        size = Size(cellSize, cellSize * 0.30f),
        cornerRadius = cornerRadius
    )

    drawRoundRect(
        color = if (isInferno) colors.warning.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.18f),
        topLeft = topLeft,
        size = Size(cellSize, cellSize),
        cornerRadius = cornerRadius,
        style = Stroke(width = (cellSize * 0.035f).coerceAtLeast(1f))
    )

    if (variant == 0) {
        drawBombCore(topLeft = topLeft, cellSize = cellSize, colors = colors)
    }
}

private fun DrawScope.drawBombCore(
    topLeft: Offset,
    cellSize: Float,
    colors: GridfallColors
) {
    val center = topLeft + Offset(cellSize / 2f, cellSize / 2f)
    if (colors.isInfernoTheme()) {
        drawCircle(
            color = colors.bombGlow.copy(alpha = 0.52f),
            radius = cellSize * 0.42f,
            center = center
        )
        drawCircle(
            color = colors.warning.copy(alpha = 0.55f),
            radius = cellSize * 0.35f,
            center = center,
            style = Stroke(width = (cellSize * 0.045f).coerceAtLeast(1f))
        )
    }
    drawCircle(
        color = Color.Black.copy(alpha = 0.30f),
        radius = cellSize * 0.31f,
        center = center
    )
    drawCircle(
        color = colors.bombCore,
        radius = cellSize * 0.23f,
        center = center
    )
    drawCircle(
        color = colors.bombInner,
        radius = cellSize * 0.11f,
        center = center
    )
}

private data class BlockPalette(
    val top: Color,
    val middle: Color,
    val bottom: Color
)

private fun paletteForVariant(
    variant: Int,
    colors: GridfallColors
): BlockPalette {
    return when (variant) {
        1 -> BlockPalette(colors.block1Top, colors.block1Mid, colors.block1Bottom)
        2 -> BlockPalette(colors.block2Top, colors.block2Mid, colors.block2Bottom)
        3 -> BlockPalette(colors.block3Top, colors.block3Mid, colors.block3Bottom)
        4 -> BlockPalette(colors.block4Top, colors.block4Mid, colors.block4Bottom)
        else -> BlockPalette(colors.bombOuter, colors.bombOuter, colors.bombOuter)
    }
}
