package com.example.gridfall.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.example.gridfall.ui.theme.BlockworldColors
import com.example.gridfall.ui.theme.GridfallColors

internal fun GridfallColors.isBlockworldTheme(): Boolean = this == BlockworldColors

internal fun Modifier.blockworldFrameTexture(colors: GridfallColors): Modifier {
    if (!colors.isBlockworldTheme()) return this

    return drawWithCache {
        onDrawBehind {
            val tile = (size.minDimension * 0.09f).coerceAtLeast(6f)
            var row = 0
            var y = 0f
            while (y < size.height) {
                var column = 0
                var x = 0f
                while (x < size.width) {
                    if ((row + column) % 3 == 0) {
                        drawRect(
                            color = if ((row + column) % 2 == 0) Color.White.copy(alpha = 0.045f) else Color.Black.copy(alpha = 0.075f),
                            topLeft = Offset(x, y),
                            size = Size(tile, tile)
                        )
                    }
                    column++
                    x += tile
                }
                row++
                y += tile
            }
            val rim = (size.minDimension * 0.018f).coerceAtLeast(2f)
            drawRect(color = colors.accentStrong.copy(alpha = 0.32f), size = Size(size.width, rim))
            drawRect(color = Color.Black.copy(alpha = 0.28f), topLeft = Offset(0f, size.height - rim), size = Size(size.width, rim))
        }
    }
}

internal fun Modifier.blockworldWellTexture(colors: GridfallColors): Modifier {
    if (!colors.isBlockworldTheme()) return this

    return drawWithCache {
        onDrawBehind {
            val strip = (size.minDimension * 0.025f).coerceAtLeast(2f)
            drawRect(color = Color.Black.copy(alpha = 0.14f), topLeft = Offset(0f, 0f), size = Size(size.width, strip))
            drawRect(color = colors.emptyCell.copy(alpha = 0.08f), topLeft = Offset(0f, size.height - strip), size = Size(size.width, strip))
        }
    }
}