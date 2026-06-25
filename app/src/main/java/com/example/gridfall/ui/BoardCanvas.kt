package com.example.gridfall.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gridfall.game.Board

@Composable
fun BoardCanvas(
    board: Board,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .aspectRatio(1f)
    ) {
        val spacing = size.width * 0.018f
        val cellSize = (size.width - spacing * (Board.SIZE + 1)) / Board.SIZE
        val cornerRadius = CornerRadius(cellSize * 0.18f, cellSize * 0.18f)

        drawRoundRect(
            color = Color(0xFF1F2937),
            topLeft = Offset.Zero,
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(cellSize * 0.25f, cellSize * 0.25f)
        )

        for (row in 0 until Board.SIZE) {
            for (col in 0 until Board.SIZE) {
                val cellValue = board.get(row, col)
                val topLeft = Offset(
                    x = spacing + col * (cellSize + spacing),
                    y = spacing + row * (cellSize + spacing)
                )

                drawRoundRect(
                    color = if (cellValue == 0) Color(0xFF374151) else Color(0xFF38BDF8),
                    topLeft = topLeft,
                    size = Size(cellSize, cellSize),
                    cornerRadius = cornerRadius
                )
            }
        }
    }
}
