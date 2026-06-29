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
import com.example.gridfall.ui.theme.GridfallColors
import com.example.gridfall.ui.theme.InfernoCoreColors

internal fun GridfallColors.isInfernoTheme(): Boolean = this == InfernoCoreColors

internal fun Modifier.infernoAppTexture(colors: GridfallColors): Modifier {
    if (!colors.isInfernoTheme()) return this

    return drawWithCache {
        onDrawBehind {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.accent.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.82f, size.height * 0.16f),
                    radius = size.minDimension * 0.72f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.danger.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.12f, size.height * 0.70f),
                    radius = size.minDimension * 0.62f
                )
            )
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        colors.accent.copy(alpha = 0.035f),
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
    if (!colors.isInfernoTheme()) return this

    return drawWithCache {
        onDrawBehind {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.accent.copy(alpha = 0.075f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.10f)
                    )
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.warning.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.18f, 0f),
                    radius = size.minDimension * 0.86f
                )
            )
            drawInfernoCracks(colors = colors, alphaScale = 0.62f)
            drawMoltenRim(colors = colors, alphaScale = 0.60f)
        }
    }
}

internal fun Modifier.infernoBoardFrameTexture(colors: GridfallColors): Modifier {
    if (!colors.isInfernoTheme()) return this

    return drawWithCache {
        onDrawBehind {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.accent.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.50f, size.height * 0.03f),
                    radius = size.width * 0.76f
                )
            )
            drawMoltenRim(colors = colors, alphaScale = 0.95f)
            drawInfernoCracks(colors = colors, alphaScale = 0.50f)
        }
    }
}

internal fun Modifier.infernoBoardWellTexture(colors: GridfallColors): Modifier {
    if (!colors.isInfernoTheme()) return this

    return drawWithCache {
        onDrawBehind {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.accent.copy(alpha = 0.035f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.12f)
                    )
                )
            )
        }
    }
}

internal fun Modifier.infernoSlotTexture(
    colors: GridfallColors,
    active: Boolean
): Modifier {
    if (!colors.isInfernoTheme()) return this

    val intensity = if (active) 1f else 0.64f
    return drawWithCache {
        onDrawBehind {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.accentStrong.copy(alpha = 0.11f * intensity),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.50f, size.height * 0.12f),
                    radius = size.minDimension * 0.72f
                )
            )
            drawInfernoCracks(colors = colors, alphaScale = 0.46f * intensity)
            drawLine(
                color = colors.accent.copy(alpha = 0.16f * intensity),
                start = Offset(size.width * 0.16f, size.height * 0.90f),
                end = Offset(size.width * 0.86f, size.height * 0.82f),
                strokeWidth = (size.minDimension * 0.018f).coerceAtLeast(1f)
            )
        }
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
