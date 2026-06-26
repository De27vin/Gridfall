package com.example.gridfall.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.example.gridfall.game.Piece
import com.example.gridfall.game.PieceEffect
import kotlin.math.max
import kotlin.math.min

@Composable
fun PieceTray(
    pieces: List<Piece>,
    usedPieceIndices: Set<Int>,
    draggingPieceIndex: Int?,
    onPieceDragStarted: (pieceIndex: Int, piece: Piece, position: Offset, startOffset: Offset) -> Unit,
    onPieceDragged: (Offset) -> Unit,
    onPieceDragEnded: () -> Unit,
    onPieceDragCancelled: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val piece = pieces.getOrNull(index)
            val isUsed = index in usedPieceIndices
            val isSelectable = piece != null && !isUsed
            val isDragging = draggingPieceIndex == index && isSelectable
            val slotShape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            var slotTopLeft by remember { mutableStateOf(Offset.Zero) }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(
                        color = if (isDragging) Color(0xFF0F3A4C) else Color(0xFF1F2937),
                        shape = slotShape
                    )
                    .border(
                        width = if (isDragging) 3.dp else 1.dp,
                        color = if (isDragging) Color(0xFF38BDF8) else Color(0xFF263241),
                        shape = slotShape
                    )
                    .onGloballyPositioned { coordinates ->
                        slotTopLeft = coordinates.positionInRoot()
                    }
                    .pointerInput(isSelectable, piece) {
                        if (!isSelectable) return@pointerInput
                        val draggablePiece = piece ?: return@pointerInput

                        detectDragGestures(
                            onDragStart = { offset ->
                                onPieceDragStarted(
                                    index,
                                    draggablePiece,
                                    slotTopLeft + offset,
                                    offset
                                )
                            },
                            onDragCancel = onPieceDragCancelled,
                            onDragEnd = onPieceDragEnded,
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onPieceDragged(dragAmount)
                            }
                        )
                    }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (piece != null && !isUsed) {
                    PiecePreview(
                        piece = piece,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(if (isDragging) 0.35f else 1f)
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

        val isBomb = piece.effect == PieceEffect.Bomb
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
                color = if (isBomb) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                topLeft = topLeft,
                size = Size(cellSize, cellSize),
                cornerRadius = cornerRadius
            )

            if (isBomb) {
                val center = topLeft + Offset(cellSize / 2f, cellSize / 2f)
                drawCircle(
                    color = Color(0xFF111827),
                    radius = cellSize * 0.24f,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFFFF7ED),
                    radius = cellSize * 0.11f,
                    center = center
                )
            }
        }
    }
}
