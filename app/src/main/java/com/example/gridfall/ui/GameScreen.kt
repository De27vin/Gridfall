package com.example.gridfall.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.gridfall.game.Board
import com.example.gridfall.game.GameEngine
import com.example.gridfall.game.Piece
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun GameScreen(modifier: Modifier = Modifier) {
    var gameState by remember { mutableStateOf(GameEngine.createInitialState()) }
    var dragState by remember { mutableStateOf(DragState()) }
    var boardLayoutInfo by remember { mutableStateOf<BoardLayoutInfo?>(null) }

    val placementPreview = createPlacementPreview(
        dragState = dragState,
        boardLayoutInfo = boardLayoutInfo,
        board = gameState.board
    )

    fun restartGame() {
        gameState = GameEngine.createInitialState()
        dragState = DragState()
    }

    fun finishDrag() {
        val pieceIndex = dragState.pieceIndex
        val origin = mapPositionToBoardOrigin(
            position = dragState.dragPosition,
            boardLayoutInfo = boardLayoutInfo
        )

        if (pieceIndex != null && origin != null) {
            val nextState = GameEngine.placePiece(
                state = gameState,
                pieceIndex = pieceIndex,
                startRow = origin.first,
                startCol = origin.second
            )

            if (nextState != gameState) {
                gameState = nextState
            }
        }

        dragState = DragState()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF111827))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            GameTopBar(
                score = gameState.score,
                combo = gameState.combo,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            BoardCanvas(
                board = gameState.board,
                placementPreview = placementPreview,
                onBoardLayoutChanged = { layoutInfo ->
                    boardLayoutInfo = layoutInfo
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            PieceTray(
                pieces = gameState.currentPieces,
                usedPieceIndices = gameState.usedPieceIndices,
                draggingPieceIndex = dragState.pieceIndex,
                onPieceDragStarted = { pieceIndex, piece, position, startOffset ->
                    dragState = DragState(
                        pieceIndex = pieceIndex,
                        piece = piece,
                        dragPosition = position,
                        dragStartOffset = startOffset,
                        isDragging = true
                    )
                },
                onPieceDragged = { dragAmount ->
                    dragState = dragState.copy(
                        dragPosition = dragState.dragPosition + dragAmount
                    )
                },
                onPieceDragEnded = ::finishDrag,
                onPieceDragCancelled = {
                    dragState = DragState()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = ::restartGame,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF38BDF8),
                    contentColor = Color(0xFF082F49)
                )
            ) {
                Text(text = "Restart")
            }
        }

        val draggedPiece = dragState.piece
        val currentBoardLayoutInfo = boardLayoutInfo
        if (dragState.isDragging && draggedPiece != null && currentBoardLayoutInfo != null) {
            DraggedPiece(
                piece = draggedPiece,
                cellSizePx = currentBoardLayoutInfo.cellSizePx,
                spacingPx = currentBoardLayoutInfo.spacingPx,
                modifier = Modifier.offset {
                    IntOffset(
                        x = dragState.dragPosition.x.roundToInt(),
                        y = dragState.dragPosition.y.roundToInt()
                    )
                }
            )
        }
    }

    if (gameState.isGameOver) {
        GameOverDialog(
            finalScore = gameState.score,
            onRestart = ::restartGame
        )
    }
}

@Composable
private fun DraggedPiece(
    piece: Piece,
    cellSizePx: Float,
    spacingPx: Float,
    modifier: Modifier = Modifier
) {
    val bounds = pieceBounds(piece)
    val widthPx = cellSizePx * bounds.colCount + spacingPx * max(0, bounds.colCount - 1)
    val heightPx = cellSizePx * bounds.rowCount + spacingPx * max(0, bounds.rowCount - 1)
    val density = LocalDensity.current
    val widthDp = with(density) { widthPx.toDp() }
    val heightDp = with(density) { heightPx.toDp() }

    Canvas(
        modifier = modifier
            .size(widthDp, heightDp)
            .background(Color(0x3322D3EE), RoundedCornerShape(8.dp))
    ) {
        val scale = min(size.width / widthPx, size.height / heightPx)
        val scaledCellSize = cellSizePx * scale
        val scaledSpacing = spacingPx * scale
        val cornerRadius = CornerRadius(scaledCellSize * 0.16f, scaledCellSize * 0.16f)

        piece.cells.forEach { cell ->
            val row = cell.row - bounds.minRow
            val col = cell.col - bounds.minCol
            val topLeft = Offset(
                x = col * (scaledCellSize + scaledSpacing),
                y = row * (scaledCellSize + scaledSpacing)
            )

            drawRoundRect(
                color = Color(0xEE38BDF8),
                topLeft = topLeft,
                size = Size(scaledCellSize, scaledCellSize),
                cornerRadius = cornerRadius
            )
        }
    }
}

private fun createPlacementPreview(
    dragState: DragState,
    boardLayoutInfo: BoardLayoutInfo?,
    board: Board
): PlacementPreview? {
    val piece = dragState.piece ?: return null
    if (!dragState.isDragging) return null
    val origin = mapPositionToBoardOrigin(dragState.dragPosition, boardLayoutInfo) ?: return null

    return PlacementPreview(
        piece = piece,
        originRow = origin.first,
        originCol = origin.second,
        isValid = GameEngine.canPlace(
            board = board,
            piece = piece,
            startRow = origin.first,
            startCol = origin.second
        )
    )
}

private fun mapPositionToBoardOrigin(
    position: Offset,
    boardLayoutInfo: BoardLayoutInfo?
): Pair<Int, Int>? {
    val layoutInfo = boardLayoutInfo ?: return null
    val relativeX = position.x - layoutInfo.topLeft.x
    val relativeY = position.y - layoutInfo.topLeft.y

    if (relativeX < 0f || relativeY < 0f || relativeX > layoutInfo.sizePx || relativeY > layoutInfo.sizePx) {
        return null
    }

    val stride = layoutInfo.cellSizePx + layoutInfo.spacingPx
    val col = ((relativeX - layoutInfo.spacingPx) / stride).toInt()
    val row = ((relativeY - layoutInfo.spacingPx) / stride).toInt()

    if (row !in 0 until Board.SIZE || col !in 0 until Board.SIZE) {
        return null
    }

    val cellLeft = layoutInfo.spacingPx + col * stride
    val cellTop = layoutInfo.spacingPx + row * stride
    val insideCell = relativeX in cellLeft..(cellLeft + layoutInfo.cellSizePx) &&
        relativeY in cellTop..(cellTop + layoutInfo.cellSizePx)

    return if (insideCell) row to col else null
}

private data class PieceBounds(
    val minRow: Int,
    val minCol: Int,
    val rowCount: Int,
    val colCount: Int
)

private fun pieceBounds(piece: Piece): PieceBounds {
    val minRow = piece.cells.minOf { it.row }
    val maxRow = piece.cells.maxOf { it.row }
    val minCol = piece.cells.minOf { it.col }
    val maxCol = piece.cells.maxOf { it.col }

    return PieceBounds(
        minRow = minRow,
        minCol = minCol,
        rowCount = maxRow - minRow + 1,
        colCount = maxCol - minCol + 1
    )
}
