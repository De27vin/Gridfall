package com.example.gridfall.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import com.example.gridfall.ui.theme.*
import kotlinx.coroutines.delay
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
    val density = LocalDensity.current
    
    // Updated drag offset for better UX (above finger)
    val verticalShiftPx = with(density) { -64.dp.toPx() }
    val dragVisualOffset = boardLayoutInfo?.let { layoutInfo ->
        dragState.piece?.let { piece ->
            calculateDragVisualOffset(
                piece = piece,
                boardLayoutInfo = layoutInfo
            ).let { offset ->
                Offset(offset.x, offset.y + verticalShiftPx)
            }
        } ?: Offset(0f, verticalShiftPx)
    } ?: Offset(0f, verticalShiftPx)

    val placementPreview = createPlacementPreview(
        dragState = dragState,
        placementResolution = dragState.placementResolution
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
        val resolution = dragState.placementResolution
        val target = resolution?.target

        if (pieceIndex != null && piece != null && target != null && resolution.isValid) {
            val clearResult = previewClearResult(
                board = gameState.board,
                piece = piece,
                startRow = target.startRow,
                startCol = target.startCol
            )
            val nextState = GameEngine.placePiece(
                state = gameState,
                pieceIndex = pieceIndex,
                startRow = target.startRow,
                startCol = target.startCol
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
            .background(Brush.verticalGradient(listOf(MidnightNavy, DeepGraphite)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
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

            BoardCanvas(
                board = gameState.board,
                placementPreview = placementPreview,
                lineClearFeedback = lineClearFeedback,
                onBoardLayoutChanged = { layoutInfo ->
                    boardLayoutInfo = layoutInfo
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            PieceTray(
                pieces = gameState.currentPieces,
                usedPieceIndices = gameState.usedPieceIndices,
                draggingPieceIndex = dragState.pieceIndex,
                onPieceDragStarted = { pieceIndex, piece, position, startOffset ->
                    val resolution = calculateCurrentDragPlacementResolution(
                        piece = piece,
                        dragPosition = position,
                        boardLayoutInfo = boardLayoutInfo,
                        board = gameState.board,
                        verticalShiftPx = verticalShiftPx
                    )
                    dragState = DragState(
                        pieceIndex = pieceIndex,
                        piece = piece,
                        dragPosition = position,
                        dragStartOffset = startOffset,
                        placementResolution = resolution,
                        isDragging = true
                    )
                },
                onPieceDragged = { dragAmount ->
                    val piece = dragState.piece
                    val nextPosition = dragState.dragPosition + dragAmount
                    val resolution = if (piece != null) {
                        calculateCurrentDragPlacementResolution(
                            piece = piece,
                            dragPosition = nextPosition,
                            boardLayoutInfo = boardLayoutInfo,
                            board = gameState.board,
                            verticalShiftPx = verticalShiftPx
                        )
                    } else {
                        null
                    }

                    dragState = dragState.copy(
                        dragPosition = nextPosition,
                        placementResolution = resolution
                    )
                },
                onPieceDragEnded = ::finishDrag,
                onPieceDragCancelled = {
                    dragState = DragState()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = ::restartGame,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SlateButton,
                    contentColor = IceWhite
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(44.dp)
                    .widthIn(min = 132.dp)
            ) {
                Text(text = "Restart", style = MaterialTheme.typography.labelLarge)
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

        if (showContractResult && gameState.contractState.resolvedContract != null) {
            ContractResultChip(
                contractState = gameState.contractState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .systemBarsPadding()
                    .padding(top = 112.dp)
            )
        } else if (gameState.contractState.activeContract != null) {
            ContractActiveChip(
                contractState = gameState.contractState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .systemBarsPadding()
                    .padding(top = 112.dp, start = 20.dp, end = 20.dp)
            )
        }

        val offeredContract = gameState.contractState.offeredContract
        if (offeredContract != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC02060B))
                    .padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                ContractOfferPopup(
                    contract = offeredContract,
                    onAccept = {
                        gameState = GameEngine.acceptContract(gameState)
                    },
                    onSkip = {
                        gameState = GameEngine.skipContract(gameState)
                    }
                )
            }
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
    val bounds = piece.bounds()
    val widthPx = visualPieceWidth(
        piece = piece,
        cellSizePx = cellSizePx,
        spacingPx = spacingPx
    )
    val heightPx = visualPieceHeight(
        piece = piece,
        cellSizePx = cellSizePx,
        spacingPx = spacingPx
    )
    val density = LocalDensity.current
    val widthDp = with(density) { widthPx.toDp() }
    val heightDp = with(density) { heightPx.toDp() }

    Canvas(
        modifier = modifier
            .size(widthDp, heightDp)
    ) {
        piece.cells.forEach { cell ->
            val row = cell.row - bounds.minRow
            val col = cell.col - bounds.minCol
            val topLeft = Offset(
                x = col * (cellSizePx + spacingPx),
                y = row * (cellSizePx + spacingPx)
            )

            drawDraggedCell(topLeft, cellSizePx, piece.colorVariant)
        }
    }
}

private fun DrawScope.drawDraggedCell(topLeft: Offset, cellSize: Float, variant: Int) {
    val cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
    
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
        topLeft = topLeft + Offset(2.dp.toPx(), 2.dp.toPx()),
        size = Size(cellSize - 4.dp.toPx(), cellSize / 3f),
        cornerRadius = cornerRadius
    )

    // Outer border for depth
    drawRoundRect(
        color = Color(0x22FFFFFF),
        topLeft = topLeft,
        size = Size(cellSize, cellSize),
        cornerRadius = cornerRadius,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
    )
    
    // Bottom shade
    drawRoundRect(
        color = Color(0x24000000),
        topLeft = topLeft + Offset(0f, cellSize * 0.7f),
        size = Size(cellSize, cellSize * 0.3f),
        cornerRadius = cornerRadius
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

private fun createPlacementPreview(
    dragState: DragState,
    placementResolution: DragPlacementResolution?
): PlacementPreview? {
    val piece = dragState.piece ?: return null
    if (!dragState.isDragging) return null
    val target = placementResolution?.target ?: return null

    return PlacementPreview(
        piece = piece,
        originRow = target.startRow,
        originCol = target.startCol,
        isValid = placementResolution.isValid
    )
}

private fun calculateCurrentDragPlacementResolution(
    piece: Piece,
    dragPosition: Offset,
    boardLayoutInfo: BoardLayoutInfo?,
    board: Board,
    verticalShiftPx: Float
): DragPlacementResolution? {
    val layoutInfo = boardLayoutInfo ?: return null
    
    val currentDragVisualOffset = calculateDragVisualOffset(
        piece = piece,
        boardLayoutInfo = layoutInfo
    )
    
    return calculateDragPlacementResolution(
        piece = piece,
        visualPieceTopLeft = dragPosition + currentDragVisualOffset + Offset(0f, verticalShiftPx), 
        boardLayoutInfo = layoutInfo,
        board = board
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
            col = startCol + cell.col,
            value = piece.colorVariant
        )
    }

    return GameEngine.clearLines(placedBoard)
}
