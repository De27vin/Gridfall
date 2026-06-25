package com.example.gridfall.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gridfall.game.Piece
import kotlin.math.max
import kotlin.math.min

@Composable
fun PieceTray(
    pieces: List<Piece>,
    usedPieceIndices: Set<Int>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val piece = pieces.getOrNull(index)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(Color(0xFF1F2937), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (piece != null && index !in usedPieceIndices) {
                    PiecePreview(
                        piece = piece,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.35f)
                    )
                }
            }
        }
    }
}

@Composable
internal fun PiecePreview(
    piece: Piece,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (piece.cells.isEmpty()) return@Canvas

        val minRow = piece.cells.minOf { it.row }
        val maxRow = piece.cells.maxOf { it.row }
        val minCol = piece.cells.minOf { it.col }
        val maxCol = piece.cells.maxOf { it.col }
        val rowCount = maxRow - minRow + 1
        val colCount = maxCol - minCol + 1
        val spacing = size.minDimension * 0.04f
        val cellSize = min(
            (size.width - spacing * max(0, colCount - 1)) / colCount,
            (size.height - spacing * max(0, rowCount - 1)) / rowCount
        )
        val previewWidth = cellSize * colCount + spacing * max(0, colCount - 1)
        val previewHeight = cellSize * rowCount + spacing * max(0, rowCount - 1)
        val origin = Offset(
            x = (size.width - previewWidth) / 2f,
            y = (size.height - previewHeight) / 2f
        )
        val cornerRadius = CornerRadius(cellSize * 0.16f, cellSize * 0.16f)

        piece.cells.forEach { cell ->
            val row = cell.row - minRow
            val col = cell.col - minCol
            val topLeft = Offset(
                x = origin.x + col * (cellSize + spacing),
                y = origin.y + row * (cellSize + spacing)
            )

            drawRoundRect(
                color = Color(0xFF38BDF8),
                topLeft = topLeft,
                size = Size(cellSize, cellSize),
                cornerRadius = cornerRadius
            )
        }
    }
}
