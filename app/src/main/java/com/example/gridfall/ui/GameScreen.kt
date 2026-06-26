package com.example.gridfall.ui

import android.view.HapticFeedbackConstants
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.gridfall.game.Board
import com.example.gridfall.game.ClearResult
import com.example.gridfall.game.GameEngine
import com.example.gridfall.game.LevelSystem
import com.example.gridfall.game.Piece
import com.example.gridfall.game.PieceEffect
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun GameScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val view = LocalView.current
    var gameState by remember { mutableStateOf(GameEngine.createInitialState()) }
    var dragState by remember { mutableStateOf(DragState()) }
    var boardLayoutInfo by remember { mutableStateOf<BoardLayoutInfo?>(null) }
    var highScore by remember { mutableStateOf(HighScoreStore.load(context)) }
    var isNewBestThisGame by remember { mutableStateOf(false) }
    var lineClearFeedback by remember { mutableStateOf<LineClearFeedback?>(null) }
    var lineClearFeedbackToken by remember { mutableStateOf(0) }
    val currentLevel = LevelSystem.levelForScore(gameState.score)
    val nextLevelScore = LevelSystem.nextLevelScore(currentLevel)
    val fallbackDragOffset = with(LocalDensity.current) { -72.dp.toPx() }
    val dragVisualOffset = boardLayoutInfo?.let { layoutInfo ->
        val offset = -layoutInfo.cellSizePx * 1.5f
        Offset(offset, offset)
    } ?: Offset(fallbackDragOffset, fallbackDragOffset)

    val placementPreview = createPlacementPreview(
        dragState = dragState,
        boardLayoutInfo = boardLayoutInfo,
        board = gameState.board,
        dragVisualOffset = dragVisualOffset
    )

    LaunchedEffect(lineClearFeedback?.token) {
        if (lineClearFeedback != null) {
            delay(650)
            lineClearFeedback = null
        }
    }

    var showContractResult by remember { mutableStateOf(false) }
    LaunchedEffect(
        gameState.contractState.resolvedContract,
        gameState.contractState.isCompleted,
        gameState.contractState.isFailed
    ) {
        if (
            gameState.contractState.resolvedContract != null &&
            (gameState.contractState.isCompleted || gameState.contractState.isFailed)
        ) {
            showContractResult = true
            delay(1200)
            showContractResult = false
        }
    }

    fun restartGame() {
        gameState = GameEngine.createInitialState()
        dragState = DragState()
        lineClearFeedback = null
        isNewBestThisGame = false
    }

    fun finishDrag() {
        val pieceIndex = dragState.pieceIndex
        val piece = dragState.piece
        val origin = mapPositionToBoardOrigin(
            position = dragState.dragPosition + dragVisualOffset,
            boardLayoutInfo = boardLayoutInfo
        )

        if (pieceIndex != null && piece != null && origin != null) {
            val clearResult = previewClearResult(
                board = gameState.board,
                piece = piece,
                startRow = origin.first,
                startCol = origin.second
            )
            val nextState = GameEngine.placePiece(
                state = gameState,
                pieceIndex = pieceIndex,
                startRow = origin.first,
                startCol = origin.second
            )

            if (nextState != gameState) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                gameState = nextState

                if (nextState.score > highScore) {
                    highScore = nextState.score
                    HighScoreStore.save(context, nextState.score)
                    isNewBestThisGame = true
                }

                if (clearResult != null && clearResult.clearedLineCount > 0) {
                    lineClearFeedbackToken += 1
                    lineClearFeedback = LineClearFeedback(
                        clearedRows = clearResult.clearedRows,
                        clearedColumns = clearResult.clearedColumns,
                        token = lineClearFeedbackToken
                    )
                }

                if (nextState.isGameOver) {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        } else if (pieceIndex != null) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
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
                highScore = highScore,
                level = currentLevel,
                nextLevelScore = nextLevelScore,
                combo = gameState.combo,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            ContractCard(
                contractState = gameState.contractState,
                showResolvedResult = showContractResult,
                onAccept = {
                    gameState = GameEngine.acceptContract(gameState)
                },
                onSkip = {
                    gameState = GameEngine.skipContract(gameState)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            BoardCanvas(
                board = gameState.board,
                placementPreview = placementPreview,
                lineClearFeedback = lineClearFeedback,
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
            val draggedPiecePosition = dragState.dragPosition + dragVisualOffset
            DraggedPiece(
                piece = draggedPiece,
                cellSizePx = currentBoardLayoutInfo.cellSizePx,
                spacingPx = currentBoardLayoutInfo.spacingPx,
                modifier = Modifier.offset {
                    IntOffset(
                        x = draggedPiecePosition.x.roundToInt(),
                        y = draggedPiecePosition.y.roundToInt()
                    )
                }
            )
        }
    }

    if (gameState.isGameOver) {
        GameOverDialog(
            finalScore = gameState.score,
            highScore = highScore,
            isNewBest = isNewBestThisGame,
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
            .background(
                color = if (piece.effect == PieceEffect.Bomb) Color(0x33F59E0B) else Color(0x3322D3EE),
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        val isBomb = piece.effect == PieceEffect.Bomb
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
                color = if (isBomb) Color(0xEEF59E0B) else Color(0xEE38BDF8),
                topLeft = topLeft,
                size = Size(scaledCellSize, scaledCellSize),
                cornerRadius = cornerRadius
            )

            if (isBomb) {
                val center = topLeft + Offset(scaledCellSize / 2f, scaledCellSize / 2f)
                drawCircle(
                    color = Color(0xFF111827),
                    radius = scaledCellSize * 0.24f,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFFFF7ED),
                    radius = scaledCellSize * 0.11f,
                    center = center
                )
            }
        }
    }
}

private fun createPlacementPreview(
    dragState: DragState,
    boardLayoutInfo: BoardLayoutInfo?,
    board: Board,
    dragVisualOffset: Offset
): PlacementPreview? {
    val piece = dragState.piece ?: return null
    if (!dragState.isDragging) return null
    val origin = mapPositionToBoardOrigin(dragState.dragPosition + dragVisualOffset, boardLayoutInfo) ?: return null

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

private fun previewClearResult(
    board: Board,
    piece: Piece,
    startRow: Int,
    startCol: Int
): ClearResult? {
    if (!GameEngine.canPlace(board, piece, startRow, startCol)) return null
    if (piece.effect == PieceEffect.Bomb) return null

    val placedBoard = piece.cells.fold(board) { currentBoard, cell ->
        currentBoard.fill(
            row = startRow + cell.row,
            col = startCol + cell.col
        )
    }

    return GameEngine.clearLines(placedBoard)
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
