package com.example.gridfall.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.example.gridfall.game.Board

@Composable
fun BoardCanvas(
    board: Board,
    modifier: Modifier = Modifier,
    onCellTapped: (row: Int, col: Int) -> Unit
) {
    var boardPixelSize by remember { mutableStateOf(0) }

    Canvas(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .aspectRatio(1f)
            .onSizeChanged { size ->
                boardPixelSize = minOf(size.width, size.height)
            }
            .pointerInput(boardPixelSize) {
                detectTapGestures { offset ->
                    if (boardPixelSize <= 0) return@detectTapGestures

                    val boardSize = boardPixelSize.toFloat()
                    if (offset.x < 0f || offset.y < 0f || offset.x > boardSize || offset.y > boardSize) {
                        return@detectTapGestures
                    }

                    val spacing = boardSize * 0.018f
                    val cellSize = (boardSize - spacing * (Board.SIZE + 1)) / Board.SIZE
                    val col = ((offset.x - spacing) / (cellSize + spacing)).toInt()
                    val row = ((offset.y - spacing) / (cellSize + spacing)).toInt()

                    if (row !in 0 until Board.SIZE || col !in 0 until Board.SIZE) {
                        return@detectTapGestures
                    }

                    val cellLeft = spacing + col * (cellSize + spacing)
                    val cellTop = spacing + row * (cellSize + spacing)
                    val insideCell = offset.x in cellLeft..(cellLeft + cellSize) &&
                        offset.y in cellTop..(cellTop + cellSize)

                    if (insideCell) {
                        onCellTapped(row, col)
                    }
                }
            }
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
