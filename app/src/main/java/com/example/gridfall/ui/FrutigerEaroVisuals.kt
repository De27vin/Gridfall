package com.example.gridfall.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.gridfall.ui.theme.FrutigerEaroColors
import com.example.gridfall.ui.theme.GridfallColors

internal fun GridfallColors.isFrutigerEaroTheme(): Boolean = this == FrutigerEaroColors

internal fun Modifier.frutigerEaroAppTexture(colors: GridfallColors): Modifier {
    if (!colors.isFrutigerEaroTheme()) return this
    return drawWithCache {
        onDrawBehind {
            drawRect(Brush.radialGradient(listOf(colors.accent.copy(alpha = 0.26f), Color.Transparent), Offset(size.width * 0.74f, size.height * 0.10f), size.minDimension * 0.90f))
            drawRect(Brush.radialGradient(listOf(colors.success.copy(alpha = 0.16f), Color.Transparent), Offset(size.width * 0.16f, size.height * 0.88f), size.minDimension * 0.72f))
            listOf(Offset(0.12f, 0.14f), Offset(0.84f, 0.28f), Offset(0.70f, 0.76f), Offset(0.26f, 0.63f)).forEachIndexed { index, point ->
                val radius = size.minDimension * (0.035f + index * 0.009f)
                drawCircle(colors.textPrimary.copy(alpha = 0.045f), radius, Offset(size.width * point.x, size.height * point.y))
                drawCircle(colors.accent.copy(alpha = 0.11f), radius, Offset(size.width * point.x, size.height * point.y), style = Stroke(width = 1.dp.toPx()))
            }
        }
    }
}

internal fun Modifier.frutigerEaroBoardFrameTexture(colors: GridfallColors): Modifier {
    if (!colors.isFrutigerEaroTheme()) return this
    return drawWithCache {
        onDrawBehind {
            drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.Transparent, colors.accent.copy(alpha = 0.08f))))
        }
    }
}

internal fun Modifier.frutigerEaroBoardWellTexture(colors: GridfallColors): Modifier {
    if (!colors.isFrutigerEaroTheme()) return this
    return drawWithCache {
        onDrawBehind {
            drawRect(Brush.radialGradient(listOf(colors.accent.copy(alpha = 0.16f), Color.Transparent), Offset(size.width * 0.5f, size.height * 0.12f), size.width * 0.84f))
        }
    }
}

internal fun DrawScope.drawFrutigerEaroEmptyCell(topLeft: Offset, cellSize: Float, colors: GridfallColors) {
    val corner = CornerRadius(cellSize * 0.25f, cellSize * 0.25f)
    drawRoundRect(Brush.verticalGradient(listOf(colors.emptyCell.copy(alpha = 0.94f), colors.boardInner.copy(alpha = 0.80f)), topLeft.y, topLeft.y + cellSize), topLeft, Size(cellSize, cellSize), corner)
    drawRoundRect(Color.White.copy(alpha = 0.20f), topLeft + Offset(cellSize * 0.10f, cellSize * 0.10f), Size(cellSize * 0.80f, cellSize * 0.22f), CornerRadius(cellSize * 0.11f, cellSize * 0.11f))
    drawRoundRect(colors.emptyCellBorder.copy(alpha = 0.78f), topLeft, Size(cellSize, cellSize), corner, style = Stroke(width = 1.25.dp.toPx()))
    drawCircle(Color.White.copy(alpha = 0.18f), cellSize * 0.12f, topLeft + Offset(cellSize * 0.28f, cellSize * 0.28f))
}