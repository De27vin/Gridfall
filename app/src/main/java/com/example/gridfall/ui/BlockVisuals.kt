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

    if (colors.isBlockworldTheme()) {
        drawBlockworldBlock(topLeft = topLeft, cellSize = cellSize, variant = variant, colors = colors)
        return
    }

    if (colors.isRetroTheme()) {
        drawRetroBlock(topLeft = topLeft, cellSize = cellSize, variant = variant, colors = colors)
        return
    }

    if (isInferno) {
        drawInfernoBlock(topLeft = topLeft, cellSize = cellSize, variant = variant, colors = colors)
        return
    }

    if (colors.isAeroTheme()) {
        drawAeroBlock(topLeft = topLeft, cellSize = cellSize, variant = variant, colors = colors)
        return
    }

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


private fun DrawScope.drawAeroBlock(
    topLeft: Offset,
    cellSize: Float,
    variant: Int,
    colors: GridfallColors
) {
    val palette = paletteForVariant(variant, colors)
    val corner = CornerRadius(cellSize * 0.17f, cellSize * 0.17f)
    if (variant == 0) drawCircle(colors.bombGlow.copy(alpha = 0.52f), cellSize * 0.62f, topLeft + Offset(cellSize / 2f, cellSize / 2f))
    drawRoundRect(Brush.verticalGradient(listOf(palette.top, palette.middle, palette.bottom), topLeft.y, topLeft.y + cellSize), topLeft, Size(cellSize, cellSize), corner)
    drawRoundRect(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.08f), Color.Transparent), topLeft, topLeft + Offset(cellSize, cellSize * 0.48f)), topLeft + Offset(cellSize * 0.08f, cellSize * 0.07f), Size(cellSize * 0.84f, cellSize * 0.42f), CornerRadius(cellSize * 0.12f, cellSize * 0.12f))
    drawRoundRect(Color.White.copy(alpha = 0.42f), topLeft, Size(cellSize, cellSize), corner, style = Stroke(width = (cellSize * 0.035f).coerceAtLeast(1f)))
    drawCircle(Color.White.copy(alpha = 0.34f), cellSize * 0.10f, topLeft + Offset(cellSize * 0.26f, cellSize * 0.25f))
    drawLine(colors.accentStrong.copy(alpha = 0.42f), topLeft + Offset(cellSize * 0.16f, cellSize * 0.76f), topLeft + Offset(cellSize * 0.82f, cellSize * 0.76f), strokeWidth = (cellSize * 0.035f).coerceAtLeast(1f))
    if (variant == 0) drawBombCore(topLeft, cellSize, colors)
}
private fun DrawScope.drawBlockworldBlock(topLeft: Offset, cellSize: Float, variant: Int, colors: GridfallColors) {
    val palette = paletteForVariant(variant, colors)
    val shadowOffset = cellSize * 0.07f
    drawRect(color = Color.Black.copy(alpha = 0.42f), topLeft = topLeft + Offset(shadowOffset, shadowOffset), size = Size(cellSize, cellSize))
    drawRect(color = palette.middle, topLeft = topLeft, size = Size(cellSize, cellSize))
    drawRect(color = palette.top, topLeft = topLeft, size = Size(cellSize, cellSize * 0.24f))
    drawRect(color = palette.bottom, topLeft = topLeft + Offset(0f, cellSize * 0.76f), size = Size(cellSize, cellSize * 0.24f))
    drawRect(color = palette.bottom.copy(alpha = 0.72f), topLeft = topLeft + Offset(cellSize * 0.82f, cellSize * 0.24f), size = Size(cellSize * 0.18f, cellSize * 0.52f))
    val fleckSize = (cellSize * 0.12f).coerceAtLeast(1f)
    listOf(Offset(0.18f, 0.38f), Offset(0.55f, 0.48f), Offset(0.34f, 0.68f)).forEachIndexed { index, point ->
        drawRect(color = if (index % 2 == 0) Color.White.copy(alpha = 0.13f) else Color.Black.copy(alpha = 0.14f), topLeft = topLeft + Offset(cellSize * point.x, cellSize * point.y), size = Size(fleckSize, fleckSize))
    }
    drawRect(color = colors.boardInner.copy(alpha = 0.88f), topLeft = topLeft, size = Size(cellSize, cellSize), style = Stroke(width = (cellSize * 0.055f).coerceAtLeast(1.5f)))
    if (variant == 0) {
        val coreSize = cellSize * 0.42f
        val coreTopLeft = topLeft + Offset((cellSize - coreSize) / 2f, (cellSize - coreSize) / 2f)
        drawRect(color = colors.bombOuter, topLeft = coreTopLeft, size = Size(coreSize, coreSize))
        drawRect(color = colors.bombCore, topLeft = coreTopLeft + Offset(coreSize * 0.24f, coreSize * 0.24f), size = Size(coreSize * 0.52f, coreSize * 0.52f))
        drawRect(color = colors.bombInner, topLeft = coreTopLeft + Offset(coreSize * 0.40f, coreSize * 0.08f), size = Size(coreSize * 0.20f, coreSize * 0.84f))
    }
}
private fun DrawScope.drawInfernoBlock(
    topLeft: Offset,
    cellSize: Float,
    variant: Int,
    colors: GridfallColors
) {
    val palette = paletteForVariant(variant, colors)
    val corner = cellSize * 0.055f
    val cornerRadius = CornerRadius(corner, corner)
    val inset = cellSize * 0.105f

    if (variant == 0) {
        drawRoundRect(
            color = colors.bombGlow.copy(alpha = 0.70f),
            topLeft = topLeft - Offset(cellSize * 0.13f, cellSize * 0.13f),
            size = Size(cellSize * 1.26f, cellSize * 1.26f),
            cornerRadius = CornerRadius(corner * 3.0f, corner * 3.0f)
        )
    } else {
        drawRoundRect(
            color = colors.accent.copy(alpha = 0.18f),
            topLeft = topLeft - Offset(cellSize * 0.05f, cellSize * 0.05f),
            size = Size(cellSize * 1.10f, cellSize * 1.10f),
            cornerRadius = CornerRadius(corner * 2.0f, corner * 2.0f)
        )
    }

    drawRoundRect(
        color = Color.Black.copy(alpha = 0.42f),
        topLeft = topLeft + Offset(cellSize * 0.07f, cellSize * 0.09f),
        size = Size(cellSize, cellSize),
        cornerRadius = cornerRadius
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2B0B05),
                palette.bottom,
                Color.Black.copy(alpha = 0.56f)
            ),
            startY = topLeft.y,
            endY = topLeft.y + cellSize
        ),
        topLeft = topLeft,
        size = Size(cellSize, cellSize),
        cornerRadius = cornerRadius
    )
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(
                palette.top.copy(alpha = 0.98f),
                palette.middle.copy(alpha = 0.82f),
                Color.Transparent
            ),
            center = topLeft + Offset(cellSize * 0.48f, cellSize * 0.38f),
            radius = cellSize * 0.72f
        ),
        topLeft = topLeft + Offset(inset, inset),
        size = Size(cellSize - inset * 2f, cellSize - inset * 2f),
        cornerRadius = CornerRadius(corner * 0.70f, corner * 0.70f)
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.52f),
        topLeft = topLeft,
        size = Size(cellSize, cellSize),
        cornerRadius = cornerRadius,
        style = Stroke(width = (cellSize * 0.075f).coerceAtLeast(2f))
    )
    drawLine(
        color = colors.accentStrong.copy(alpha = 0.44f),
        start = topLeft + Offset(cellSize * 0.16f, cellSize * 0.18f),
        end = topLeft + Offset(cellSize * 0.70f, cellSize * 0.18f),
        strokeWidth = (cellSize * 0.045f).coerceAtLeast(1f)
    )
    drawLine(
        color = colors.warning.copy(alpha = 0.30f),
        start = topLeft + Offset(cellSize * 0.20f, cellSize * 0.72f),
        end = topLeft + Offset(cellSize * 0.78f, cellSize * 0.52f),
        strokeWidth = (cellSize * 0.035f).coerceAtLeast(1f)
    )
    drawLine(
        color = colors.accent.copy(alpha = 0.20f),
        start = topLeft + Offset(cellSize * 0.62f, cellSize * 0.20f),
        end = topLeft + Offset(cellSize * 0.82f, cellSize * 0.44f),
        strokeWidth = (cellSize * 0.026f).coerceAtLeast(1f)
    )

    if (variant == 0) {
        drawInfernoBombCore(topLeft = topLeft, cellSize = cellSize, colors = colors)
    }
}

private fun DrawScope.drawInfernoBombCore(
    topLeft: Offset,
    cellSize: Float,
    colors: GridfallColors
) {
    val center = topLeft + Offset(cellSize / 2f, cellSize / 2f)
    drawCircle(
        color = colors.bombGlow.copy(alpha = 0.68f),
        radius = cellSize * 0.46f,
        center = center
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                colors.bombCore,
                colors.bombInner,
                colors.bombOuter.copy(alpha = 0.96f),
                Color.Black.copy(alpha = 0.66f)
            ),
            center = center - Offset(cellSize * 0.08f, cellSize * 0.10f),
            radius = cellSize * 0.42f
        ),
        radius = cellSize * 0.34f,
        center = center
    )
    drawCircle(
        color = colors.accentStrong.copy(alpha = 0.72f),
        radius = cellSize * 0.25f,
        center = center,
        style = Stroke(width = (cellSize * 0.045f).coerceAtLeast(1f))
    )
    drawLine(
        color = Color.Black.copy(alpha = 0.54f),
        start = center + Offset(-cellSize * 0.18f, -cellSize * 0.11f),
        end = center + Offset(cellSize * 0.18f, cellSize * 0.13f),
        strokeWidth = (cellSize * 0.040f).coerceAtLeast(1f)
    )
    drawLine(
        color = colors.bombCore.copy(alpha = 0.82f),
        start = center + Offset(-cellSize * 0.10f, cellSize * 0.16f),
        end = center + Offset(cellSize * 0.14f, -cellSize * 0.16f),
        strokeWidth = (cellSize * 0.034f).coerceAtLeast(1f)
    )
}

private fun DrawScope.drawRetroBlock(
    topLeft: Offset,
    cellSize: Float,
    variant: Int,
    colors: GridfallColors
) {
    val corner = cellSize * 0.045f
    val cornerRadius = CornerRadius(corner, corner)
    val palette = paletteForVariant(variant, colors)

    if (variant == 0) {
        drawRoundRect(
            color = colors.bombGlow.copy(alpha = 0.55f),
            topLeft = topLeft - Offset(cellSize * 0.10f, cellSize * 0.10f),
            size = Size(cellSize * 1.20f, cellSize * 1.20f),
            cornerRadius = CornerRadius(corner * 2.2f, corner * 2.2f)
        )
    }

    drawRoundRect(
        color = Color.Black.copy(alpha = 0.34f),
        topLeft = topLeft + Offset(cellSize * 0.09f, cellSize * 0.10f),
        size = Size(cellSize, cellSize),
        cornerRadius = cornerRadius
    )
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
    drawRoundRect(
        color = Color.White.copy(alpha = 0.20f),
        topLeft = topLeft + Offset(cellSize * 0.08f, cellSize * 0.08f),
        size = Size(cellSize * 0.84f, cellSize * 0.16f),
        cornerRadius = cornerRadius
    )
    drawLine(
        color = Color.Black.copy(alpha = 0.34f),
        start = topLeft + Offset(0f, cellSize * 0.74f),
        end = topLeft + Offset(cellSize, cellSize * 0.74f),
        strokeWidth = (cellSize * 0.050f).coerceAtLeast(1f)
    )
    drawRoundRect(
        color = colors.boardInner.copy(alpha = 0.86f),
        topLeft = topLeft,
        size = Size(cellSize, cellSize),
        cornerRadius = cornerRadius,
        style = Stroke(width = (cellSize * 0.085f).coerceAtLeast(2f))
    )
    drawRoundRect(
        color = colors.accent.copy(alpha = if (variant == 1) 0.56f else 0.24f),
        topLeft = topLeft + Offset(cellSize * 0.03f, cellSize * 0.03f),
        size = Size(cellSize * 0.94f, cellSize * 0.94f),
        cornerRadius = cornerRadius,
        style = Stroke(width = (cellSize * 0.030f).coerceAtLeast(1f))
    )

    if (variant == 0) {
        drawRetroBombCore(topLeft = topLeft, cellSize = cellSize, colors = colors)
    }
}

private fun DrawScope.drawRetroBombCore(
    topLeft: Offset,
    cellSize: Float,
    colors: GridfallColors
) {
    val center = topLeft + Offset(cellSize / 2f, cellSize / 2f)
    drawCircle(
        color = colors.accentStrong.copy(alpha = 0.82f),
        radius = cellSize * 0.36f,
        center = center
    )
    drawCircle(
        color = colors.bombCore,
        radius = cellSize * 0.25f,
        center = center
    )
    drawCircle(
        color = colors.bombInner,
        radius = cellSize * 0.12f,
        center = center
    )
    drawLine(
        color = colors.dialogBackground.copy(alpha = 0.82f),
        start = center + Offset(-cellSize * 0.20f, 0f),
        end = center + Offset(cellSize * 0.20f, 0f),
        strokeWidth = (cellSize * 0.045f).coerceAtLeast(1f)
    )
    drawLine(
        color = colors.dialogBackground.copy(alpha = 0.82f),
        start = center + Offset(0f, -cellSize * 0.20f),
        end = center + Offset(0f, cellSize * 0.20f),
        strokeWidth = (cellSize * 0.045f).coerceAtLeast(1f)
    )
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
