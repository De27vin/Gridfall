package com.example.gridfall.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.gridfall.ui.theme.ArcCyan
import com.example.gridfall.ui.theme.ArcCyanBottom
import com.example.gridfall.ui.theme.ArcCyanTop
import com.example.gridfall.ui.theme.BombGlow
import com.example.gridfall.ui.theme.BombMagenta
import com.example.gridfall.ui.theme.HotCore
import com.example.gridfall.ui.theme.RewardMint
import com.example.gridfall.ui.theme.RewardMintBottom
import com.example.gridfall.ui.theme.RewardMintTop
import com.example.gridfall.ui.theme.SignalAmber
import com.example.gridfall.ui.theme.SignalAmberBottom
import com.example.gridfall.ui.theme.SignalAmberTop
import com.example.gridfall.ui.theme.TacticalViolet
import com.example.gridfall.ui.theme.TacticalVioletBottom
import com.example.gridfall.ui.theme.TacticalVioletTop
import com.example.gridfall.ui.theme.WarningOrange

internal fun DrawScope.drawTacticalBlock(
    topLeft: Offset,
    cellSize: Float,
    variant: Int,
    cornerRadiusFraction: Float = 0.16f
) {
    val corner = cellSize * cornerRadiusFraction
    val cornerRadius = CornerRadius(corner, corner)
    val palette = paletteForVariant(variant)

    if (variant == 0) {
        drawRoundRect(
            color = BombGlow,
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

    drawRoundRect(
        color = Color.White.copy(alpha = 0.22f),
        topLeft = topLeft + Offset(cellSize * 0.07f, cellSize * 0.07f),
        size = Size(cellSize * 0.86f, cellSize * 0.28f),
        cornerRadius = cornerRadius
    )

    drawRoundRect(
        color = Color.Black.copy(alpha = 0.20f),
        topLeft = topLeft + Offset(0f, cellSize * 0.70f),
        size = Size(cellSize, cellSize * 0.30f),
        cornerRadius = cornerRadius
    )

    drawRoundRect(
        color = Color.White.copy(alpha = 0.18f),
        topLeft = topLeft,
        size = Size(cellSize, cellSize),
        cornerRadius = cornerRadius,
        style = Stroke(width = (cellSize * 0.035f).coerceAtLeast(1f))
    )

    if (variant == 0) {
        drawBombCore(topLeft = topLeft, cellSize = cellSize)
    }
}

private fun DrawScope.drawBombCore(topLeft: Offset, cellSize: Float) {
    val center = topLeft + Offset(cellSize / 2f, cellSize / 2f)
    drawCircle(
        color = Color.Black.copy(alpha = 0.30f),
        radius = cellSize * 0.31f,
        center = center
    )
    drawCircle(
        color = HotCore,
        radius = cellSize * 0.23f,
        center = center
    )
    drawCircle(
        color = WarningOrange,
        radius = cellSize * 0.11f,
        center = center
    )
}

private data class BlockPalette(
    val top: Color,
    val middle: Color,
    val bottom: Color
)

private fun paletteForVariant(variant: Int): BlockPalette {
    return when (variant) {
        1 -> BlockPalette(ArcCyanTop, ArcCyan, ArcCyanBottom)
        2 -> BlockPalette(TacticalVioletTop, TacticalViolet, TacticalVioletBottom)
        3 -> BlockPalette(SignalAmberTop, SignalAmber, SignalAmberBottom)
        4 -> BlockPalette(RewardMintTop, RewardMint, RewardMintBottom)
        else -> BlockPalette(BombMagenta, BombMagenta, BombMagenta)
    }
}
