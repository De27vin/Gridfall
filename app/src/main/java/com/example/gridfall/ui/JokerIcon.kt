package com.example.gridfall.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.gridfall.game.JokerType
import com.example.gridfall.ui.theme.LocalGridfallColors

@Composable
internal fun JokerIcon(
    jokerType: JokerType,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (jokerType == JokerType.Revert) {
        UndoJokerIcon(enabled = enabled, modifier = modifier)
        return
    }

    val colors = LocalGridfallColors.current
    Canvas(modifier = modifier.alpha(if (enabled) 1f else 0.45f)) {
        val blockColor = when (jokerType) {
            JokerType.Single -> colors.block4Mid
            JokerType.HorizontalTwo -> colors.block3Mid
            JokerType.VerticalTwo -> colors.block2Mid
            JokerType.Bomb, JokerType.MegaBomb, JokerType.Revert, JokerType.BlockBreaker -> colors.block1Mid
        }
        val dark = Color.Black.copy(alpha = 0.30f)
        val gap = size.minDimension * 0.08f
        val corner = CornerRadius(size.minDimension * 0.10f, size.minDimension * 0.10f)

        fun block(left: Float, top: Float, width: Float, height: Float) {
            drawRoundRect(
                color = dark,
                topLeft = Offset(left + gap * 0.35f, top + gap * 0.45f),
                size = Size(width, height),
                cornerRadius = corner
            )
            drawRoundRect(
                color = blockColor,
                topLeft = Offset(left, top),
                size = Size(width, height),
                cornerRadius = corner
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.18f),
                topLeft = Offset(left, top),
                size = Size(width, height * 0.25f),
                cornerRadius = corner
            )
        }

        when (jokerType) {
            JokerType.Single -> {
                val cell = size.minDimension * 0.62f
                block((size.width - cell) / 2f, (size.height - cell) / 2f, cell, cell)
            }
            JokerType.HorizontalTwo -> {
                val cell = (size.width - gap * 3f) / 2f
                val top = (size.height - cell) / 2f
                block(gap, top, cell, cell)
                block(gap * 2f + cell, top, cell, cell)
            }
            JokerType.VerticalTwo -> {
                val cell = (size.height - gap * 3f) / 2f
                val left = (size.width - cell) / 2f
                block(left, gap, cell, cell)
                block(left, gap * 2f + cell, cell, cell)
            }
            JokerType.BlockBreaker -> {
                val cell = size.minDimension * 0.64f
                val left = (size.width - cell) / 2f
                val top = (size.height - cell) / 2f
                block(left, top, cell, cell)
                val stroke = (cell * 0.13f).coerceAtLeast(1f)
                drawLine(colors.textPrimary, Offset(left + cell * 0.26f, top + cell * 0.26f), Offset(left + cell * 0.74f, top + cell * 0.74f), stroke)
                drawLine(colors.textPrimary, Offset(left + cell * 0.74f, top + cell * 0.26f), Offset(left + cell * 0.26f, top + cell * 0.74f), stroke)
            }
            JokerType.Bomb, JokerType.MegaBomb -> {
                val center = Offset(size.width / 2f, size.height / 2f + size.height * 0.05f)
                val radius = size.minDimension * if (jokerType == JokerType.MegaBomb) 0.38f else 0.31f
                drawCircle(color = colors.bombGlow.copy(alpha = 0.60f), radius = radius * 1.30f, center = center)
                drawCircle(color = colors.bombOuter, radius = radius, center = center)
                drawCircle(color = colors.bombCore, radius = radius * 0.62f, center = center)
                drawCircle(color = colors.bombInner, radius = radius * 0.26f, center = center)
                drawLine(
                    color = colors.warning,
                    start = center + Offset(radius * 0.34f, -radius * 0.72f),
                    end = center + Offset(radius * 0.66f, -radius * 1.22f),
                    strokeWidth = (radius * 0.17f).coerceAtLeast(1f)
                )
                if (jokerType == JokerType.MegaBomb) {
                    drawCircle(
                        color = colors.textPrimary.copy(alpha = 0.82f),
                        radius = radius * 1.14f,
                        center = center,
                        style = Stroke(width = (radius * 0.12f).coerceAtLeast(1f))
                    )
                }
            }
            JokerType.Revert -> Unit
        }
    }
}