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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.example.gridfall.game.Piece
import com.example.gridfall.ui.theme.*
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
        modifier = modifier
            .fillMaxWidth()
            .height(116.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val piece = pieces.getOrNull(index)
            val isUsed = index in usedPieceIndices
            val isSelectable = piece != null && !isUsed
            val isDragging = draggingPieceIndex == index && isSelectable
            val slotShape = RoundedCornerShape(18.dp)
            var slotTopLeft by remember { mutableStateOf(Offset.Zero) }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(slotShape)
                    .background(if (isDragging) Color(0x99151D2A) else Color(0x66151D2A))
                    .border(
                        width = 1.dp,
                        color = if (isDragging) LevelCyan.copy(alpha = 0.5f) else Color(0x552B3A50),
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
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (piece != null && !isUsed) {
                    PiecePreview(
                        piece = piece,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(if (isDragging) 0.35f else 1f)
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

        piece.cells.forEach { cell ->
            val row = cell.row - minRow
            val col = cell.col - minCol
            val topLeft = Offset(
                x = origin.x + col * (cellSize + spacing),
                y = origin.y + row * (cellSize + spacing)
            )

            drawTrayCell(topLeft, cellSize, piece.colorVariant)
        }
    }
}

private fun DrawScope.drawTrayCell(topLeft: Offset, cellSize: Float, variant: Int) {
    val cornerRadius = CornerRadius(cellSize * 0.16f, cellSize * 0.16f)
    
    val (topColor, midColor, bottomColor) = when (variant) {
        1 -> Triple(ArcCyanTop, ArcCyan, ArcCyanBottom)
        2 -> Triple(TacticalVioletTop, TacticalViolet, TacticalVioletBottom)
        3 -> Triple(SignalAmberTop, SignalAmber, SignalAmberBottom)
        4 -> Triple(RewardMintTop, RewardMint, RewardMintBottom)
        else -> Triple(BombMagenta, BombMagenta, BombMagenta) // Bomb
    }

    // Main Gradient Fill
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(topColor, midColor, bottomColor),
            startY = topLeft.y,
            endY = topLeft.y + cellSize
        ),
        topLeft = topLeft,
        size = Size(cellSize, cellSize),
        cornerRadius = cornerRadius
    )

    // Inner highlight (top edge)
    drawRoundRect(
        color = Color(0x33FFFFFF),
        topLeft = topLeft + Offset(cellSize * 0.05f, cellSize * 0.05f),
        size = Size(cellSize * 0.9f, cellSize * 0.3f),
        cornerRadius = cornerRadius
    )

    // Outer border for depth
    drawRoundRect(
        color = Color(0x22FFFFFF),
        topLeft = topLeft,
        size = Size(cellSize, cellSize),
        cornerRadius = cornerRadius,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = (cellSize * 0.03f).coerceAtLeast(1f))
    )

    if (variant == 0) {
        // Bomb detail
        val center = topLeft + Offset(cellSize / 2f, cellSize / 2f)
        drawCircle(
            color = HotCore,
            radius = cellSize * 0.24f,
            center = center
        )
        drawCircle(
            color = WarningOrange,
            radius = cellSize * 0.12f,
            center = center
        )
    }
}
