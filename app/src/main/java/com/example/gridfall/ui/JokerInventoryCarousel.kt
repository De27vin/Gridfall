package com.example.gridfall.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.gridfall.game.JokerType
import com.example.gridfall.game.Piece
import com.example.gridfall.game.RiskSpinState
import com.example.gridfall.ui.theme.LocalGridfallColors

@Composable
fun JokerInventoryCarousel(
    inventory: List<JokerType>,
    draggingJokerInventoryIndex: Int?,
    canUseRevert: Boolean,
    canUseClearBoard: Boolean,
    onJokerDragStarted: (inventoryIndex: Int, jokerType: JokerType, piece: Piece, position: Offset, startOffset: Offset) -> Unit,
    onJokerDragged: (Offset) -> Unit,
    onJokerDragEnded: () -> Unit,
    onJokerDragCancelled: () -> Unit,
    onRevertClicked: () -> Unit,
    onBlockBreakerClicked: () -> Unit,
    onClearBoardClicked: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalGridfallColors.current
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.panelBackground.copy(alpha = 0.72f))
            .infernoPanelTexture(colors)
            .retroPanelTexture(colors)
            .border(1.dp, colors.panelBorder.copy(alpha = 0.55f), shape)
            .padding(horizontal = 10.dp, vertical = if (compact) 6.dp else 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (!compact) Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Jokers",
                color = colors.textPrimary,
                style = MaterialTheme.typography.labelMedium.retroText(colors)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (inventory.size > RiskSpinState.MAX_INVENTORY_SIZE) {
                    "${inventory.size} · Bonus slots"
                } else {
                    "${inventory.size}/${RiskSpinState.MAX_INVENTORY_SIZE}"
                },
                color = colors.textMuted,
                style = MaterialTheme.typography.labelSmall.retroText(colors)
            )
        }

        if (inventory.isEmpty()) {
            Text(
                text = "No jokers yet",
                color = colors.textMuted,
                style = MaterialTheme.typography.bodySmall.retroText(colors)
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (compact) {
                    Column {
                        Text("Jokers", color = colors.textPrimary,
                            style = MaterialTheme.typography.labelMedium.retroText(colors))
                        Text("${inventory.size}", color = colors.textMuted,
                            style = MaterialTheme.typography.labelSmall.retroText(colors))
                    }
                }
                inventory.forEachIndexed { index, jokerType ->
                    JokerChip(
                        jokerType = jokerType,
                        compact = compact,
                        inventoryIndex = index,
                        isDragging = draggingJokerInventoryIndex == index,
                        canUseRevert = canUseRevert,
                        canUseClearBoard = canUseClearBoard,
                        onJokerDragStarted = onJokerDragStarted,
                        onJokerDragged = onJokerDragged,
                        onJokerDragEnded = onJokerDragEnded,
                        onJokerDragCancelled = onJokerDragCancelled,
                        onRevertClicked = onRevertClicked,
                        onBlockBreakerClicked = onBlockBreakerClicked,
                        onClearBoardClicked = onClearBoardClicked
                    )
                }
            }
        }
    }
}

@Composable
private fun JokerChip(
    jokerType: JokerType,
    compact: Boolean,
    inventoryIndex: Int,
    isDragging: Boolean,
    canUseRevert: Boolean,
    canUseClearBoard: Boolean,
    onJokerDragStarted: (inventoryIndex: Int, jokerType: JokerType, piece: Piece, position: Offset, startOffset: Offset) -> Unit,
    onJokerDragged: (Offset) -> Unit,
    onJokerDragEnded: () -> Unit,
    onJokerDragCancelled: () -> Unit,
    onRevertClicked: () -> Unit,
    onBlockBreakerClicked: () -> Unit,
    onClearBoardClicked: () -> Unit
) {
    val colors = LocalGridfallColors.current
    val piece = jokerType.toPiece()
    val shape = RoundedCornerShape(retroCorner(colors, infernoCorner(colors, 12.dp)))
    var chipTopLeft by remember { mutableStateOf(Offset.Zero) }
    val enabled = when (jokerType) {
        JokerType.Revert -> canUseRevert
        JokerType.ClearBoard -> canUseClearBoard
        else -> true
    }

    Box(
        modifier = Modifier
            .size(width = 58.dp, height = if (compact) 48.dp else 54.dp)
            .testTag("gameplay-joker-$inventoryIndex")
            .clip(shape)
            .background(colors.chipBackground.copy(alpha = if (enabled) 0.82f else 0.42f))
            .infernoPanelTexture(colors)
            .retroPanelTexture(colors)
            .border(
                width = if (colors.isRetroTheme() || colors.isInfernoTheme()) 2.dp else 1.dp,
                color = if (enabled) {
                    colors.accentStrong.copy(alpha = if (colors.isRetroTheme() || colors.isInfernoTheme()) 0.82f else 0.62f)
                } else {
                    colors.panelBorder.copy(alpha = 0.34f)
                },
                shape = shape
            )
            .onGloballyPositioned { coordinates ->
                chipTopLeft = coordinates.positionInRoot()
            }
            .then(
                if (piece != null) {
                    Modifier.pointerInput(inventoryIndex, jokerType) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                onJokerDragStarted(
                                    inventoryIndex,
                                    jokerType,
                                    piece,
                                    chipTopLeft + offset,
                                    offset
                                )
                            },
                            onDragCancel = onJokerDragCancelled,
                            onDragEnd = onJokerDragEnded,
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onJokerDragged(dragAmount)
                            }
                        )
                    }
                } else {
                    Modifier.pointerInput(canUseRevert, canUseClearBoard) {
                        detectTapGestures(
                            onTap = {
                                when (jokerType) {
                                    JokerType.Revert -> if (canUseRevert) onRevertClicked()
                                    JokerType.BlockBreaker -> onBlockBreakerClicked()
                                    JokerType.ClearBoard -> if (canUseClearBoard) onClearBoardClicked()
                                    else -> Unit
                                }
                            }
                        )
                    }
                }
            )
            .padding(5.dp),
        contentAlignment = Alignment.Center
    ) {
        val iconModifier = Modifier
            .size(30.dp)
            .alpha(if (isDragging) 0.35f else 1f)
        if (jokerType == JokerType.Bomb || jokerType == JokerType.MegaBomb) {
            PiecePreview(
                piece = requireNotNull(piece),
                colors = colors,
                modifier = iconModifier
            )
        } else {
            JokerIcon(
                jokerType = jokerType,
                enabled = enabled,
                modifier = iconModifier
            )
        }

        Text(
            text = shortJokerLabel(jokerType),
            color = colors.textSecondary,
            style = MaterialTheme.typography.labelSmall.retroText(colors),
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
internal fun UndoJokerIcon(
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalGridfallColors.current

    ThemeIconAsset(
        kind = ThemeIconKind.Undo,
        colors = colors,
        contentDescription = "Undo joker",
        enabled = enabled,
        modifier = modifier
    ) { fallbackModifier ->
        DrawnUndoJokerIcon(
            enabled = enabled,
            modifier = fallbackModifier
        )
    }
}

@Composable
private fun DrawnUndoJokerIcon(
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalGridfallColors.current
    val iconColor = if (enabled) colors.warning else colors.textMuted

    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.13f
        val path = Path().apply {
            moveTo(size.width * 0.70f, size.height * 0.24f)
            quadraticTo(
                size.width * 0.30f,
                size.height * 0.12f,
                size.width * 0.28f,
                size.height * 0.48f
            )
            quadraticTo(
                size.width * 0.28f,
                size.height * 0.80f,
                size.width * 0.70f,
                size.height * 0.72f
            )
        }
        val arrowHead = Path().apply {
            moveTo(size.width * 0.28f, size.height * 0.48f)
            lineTo(size.width * 0.44f, size.height * 0.34f)
            moveTo(size.width * 0.28f, size.height * 0.48f)
            lineTo(size.width * 0.43f, size.height * 0.62f)
        }

        drawPath(
            path = path,
            color = iconColor.copy(alpha = if (enabled) 0.95f else 0.52f),
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        drawPath(
            path = arrowHead,
            color = iconColor.copy(alpha = if (enabled) 0.95f else 0.52f),
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

private fun shortJokerLabel(jokerType: JokerType): String {
    return when (jokerType) {
        JokerType.Single -> "1x1"
        JokerType.HorizontalTwo -> "2x1"
        JokerType.VerticalTwo -> "1x2"
        JokerType.Bomb -> "BOMB"
        JokerType.MegaBomb -> "MEGA"
        JokerType.Revert -> "UNDO"
        JokerType.BlockBreaker -> "BREAK"
        JokerType.ClearBoard -> "CLEAR"
    }
}
