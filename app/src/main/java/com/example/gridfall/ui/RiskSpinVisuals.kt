package com.example.gridfall.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.gridfall.ui.theme.GridfallColors

internal fun Modifier.riskSpinAvailabilityGlow(
    colors: GridfallColors,
    isAvailable: Boolean
): Modifier = composed {
    if (!isAvailable) return@composed this

    val transition = rememberInfiniteTransition(label = "riskSpinAvailabilityGlow")
    val intensity = transition.animateFloat(
        initialValue = 0.26f,
        targetValue = 0.94f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 820, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "riskSpinGlowIntensity"
    ).value
    val primary = when {
        colors.isInfernoTheme() -> colors.warning
        colors.isRetroTheme() -> colors.accent
        colors.isBlockworldTheme() -> colors.success
        else -> colors.accent
    }
    val secondary = when {
        colors.isInfernoTheme() -> colors.accentStrong
        colors.isRetroTheme() -> colors.accentStrong
        colors.isBlockworldTheme() -> colors.accentStrong
        else -> colors.success
    }

    drawWithContent {
        drawContent()

        val corner = when {
            colors.isBlockworldTheme() -> 0f
            colors.isRetroTheme() -> 6.dp.toPx()
            colors.isInfernoTheme() -> 8.dp.toPx()
            else -> 16.dp.toPx()
        }
        val pulseInset = (1.5.dp.toPx() * (1f - intensity)).coerceAtLeast(0f)
        val pulseTopLeft = Offset(pulseInset, pulseInset)
        val pulseSize = Size(
            (size.width - pulseInset * 2f).coerceAtLeast(0f),
            (size.height - pulseInset * 2f).coerceAtLeast(0f)
        )

        drawRoundRect(
            color = primary.copy(alpha = 0.22f * intensity),
            topLeft = pulseTopLeft,
            size = pulseSize,
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = (1.dp.toPx() + intensity * 1.2.dp.toPx()))
        )

        when {
            colors.isInfernoTheme() -> {
                drawRoundRect(
                    color = secondary.copy(alpha = 0.22f * intensity),
                    topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                    size = Size(
                        (size.width - 8.dp.toPx()).coerceAtLeast(0f),
                        (size.height - 8.dp.toPx()).coerceAtLeast(0f)
                    ),
                    cornerRadius = CornerRadius(corner, corner),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            colors.isRetroTheme() -> {
                drawRect(
                    color = secondary.copy(alpha = 0.50f * intensity),
                    topLeft = Offset(size.width * 0.16f, 2.dp.toPx()),
                    size = Size(size.width * 0.68f, 1.dp.toPx())
                )
                drawRect(
                    color = secondary.copy(alpha = 0.34f * intensity),
                    topLeft = Offset(size.width * 0.26f, size.height - 3.dp.toPx()),
                    size = Size(size.width * 0.48f, 1.dp.toPx())
                )
            }
            colors.isBlockworldTheme() -> {
                val tile = 3.dp.toPx()
                drawRect(
                    color = secondary.copy(alpha = 0.46f * intensity),
                    topLeft = Offset(tile, tile),
                    size = Size(tile, tile)
                )
                drawRect(
                    color = secondary.copy(alpha = 0.46f * intensity),
                    topLeft = Offset(size.width - tile * 2f, size.height - tile * 2f),
                    size = Size(tile, tile)
                )
            }
            else -> {
                drawRoundRect(
                    color = secondary.copy(alpha = 0.20f * intensity),
                    topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                    size = Size(
                        (size.width - 8.dp.toPx()).coerceAtLeast(0f),
                        (size.height - 8.dp.toPx()).coerceAtLeast(0f)
                    ),
                    cornerRadius = CornerRadius(corner, corner),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
    }
}