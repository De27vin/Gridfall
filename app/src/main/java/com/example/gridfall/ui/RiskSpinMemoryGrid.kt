package com.example.gridfall.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gridfall.game.RiskSpin
import com.example.gridfall.game.RiskSpinMemoryField
import com.example.gridfall.game.RiskSpinMemorySession
import com.example.gridfall.game.RiskSpinOutcome
import com.example.gridfall.game.RiskSpinState
import com.example.gridfall.ui.theme.LocalGridfallColors

@Composable
fun RiskSpinMemoryOverlay(
    session: RiskSpinMemorySession?,
    paidCost: Int?,
    inventoryCount: Int,
    isGridActive: Boolean,
    onFieldClick: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalGridfallColors.current
    val panelShape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 18.dp)))
    val title = if (session == null) {
        "Choose a spin"
    } else if (session.isComplete) {
        "Reveals complete"
    } else {
        "Reveals left: ${session.revealsLeft}"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.backgroundTop.copy(alpha = 0.90f))
            .infernoAppTexture(theme)
            .retroAppTexture(theme)
            .padding(horizontal = 18.dp, vertical = 44.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(panelShape)
                .background(theme.panelBackground.copy(alpha = 0.92f))
                .infernoPanelTexture(theme)
                .retroPanelTexture(theme)
                .border(
                    width = if (theme.isRetroTheme() || theme.isInfernoTheme()) 2.dp else 1.dp,
                    color = theme.panelBorder.copy(alpha = 0.82f),
                    shape = panelShape
                )
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (theme.isRetroTheme()) title.uppercase() else title,
                color = theme.textPrimary,
                style = MaterialTheme.typography.titleMedium.retroText(theme),
                textAlign = TextAlign.Center
            )
            Text(
                text = if (session == null) {
                    "Pick an option to activate the board"
                } else {
                    "Inventory $inventoryCount/${RiskSpinState.MAX_INVENTORY_SIZE}" +
                        (paidCost?.let { "  Paid $it" } ?: "")
                },
                color = theme.textSecondary,
                style = MaterialTheme.typography.labelSmall.retroText(theme),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            MemoryGrid(
                fields = session?.fields ?: hiddenPlaceholderFields(),
                isGridActive = isGridActive && session != null && !session.isComplete,
                onFieldClick = onFieldClick,
                modifier = Modifier.fillMaxWidth()
            )

            if (session?.isComplete == true) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.accentStrong,
                        contentColor = theme.dialogBackground
                    ),
                    shape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 14.dp)))
                ) {
                    Text(
                        text = if (theme.isRetroTheme()) "CLOSE" else "Close",
                        style = MaterialTheme.typography.labelLarge.retroText(theme)
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryGrid(
    fields: List<RiskSpinMemoryField>,
    isGridActive: Boolean,
    onFieldClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        fields.chunked(RiskSpin.MEMORY_GRID_SIZE).forEach { rowFields ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                rowFields.forEach { field ->
                    MemoryTile(
                        field = field,
                        isGridActive = isGridActive,
                        onFieldClick = onFieldClick,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryTile(
    field: RiskSpinMemoryField,
    isGridActive: Boolean,
    onFieldClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalGridfallColors.current
    val shape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 6.dp)))
    val borderColor = when {
        !field.isRevealed -> theme.panelBorder.copy(alpha = if (isGridActive) 0.78f else 0.42f)
        field.lostBecauseInventoryFull -> theme.danger.copy(alpha = 0.82f)
        field.jokerAdded != null -> theme.warning.copy(alpha = 0.86f)
        field.outcome == RiskSpinOutcome.Miss -> theme.danger.copy(alpha = 0.55f)
        else -> theme.accentStrong.copy(alpha = 0.70f)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (field.isRevealed) {
                    theme.chipBackground.copy(alpha = 0.92f)
                } else {
                    theme.boardInner.copy(alpha = if (isGridActive) 0.90f else 0.62f)
                }
            )
            .border(
                width = if (theme.isRetroTheme() || theme.isInfernoTheme()) 2.dp else 1.dp,
                color = borderColor,
                shape = shape
            )
            .clickable(
                enabled = isGridActive && !field.isRevealed,
                onClick = { onFieldClick(field.id) }
            )
            .padding(2.dp)
            .alpha(if (!isGridActive && !field.isRevealed) 0.72f else 1f),
        contentAlignment = Alignment.Center
    ) {
        if (field.isRevealed) {
            RevealedReward(field)
        } else {
            Text(
                text = "?",
                color = theme.textMuted,
                style = MaterialTheme.typography.labelSmall.retroText(theme)
            )
        }
    }
}

@Composable
private fun RevealedReward(field: RiskSpinMemoryField) {
    val theme = LocalGridfallColors.current
    val jokerType = field.outcome.toJokerType()
    val piece = jokerType?.toPiece()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            field.outcome == RiskSpinOutcome.Miss -> {
                Text(
                    text = "MISS",
                    color = theme.danger,
                    style = MaterialTheme.typography.labelSmall.retroText(theme),
                    textAlign = TextAlign.Center
                )
            }
            piece != null -> {
                PiecePreview(
                    piece = piece,
                    colors = theme,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = rewardLabel(field.outcome, field.lostBecauseInventoryFull),
                    color = rewardTextColor(field),
                    style = MaterialTheme.typography.labelSmall.retroText(theme),
                    textAlign = TextAlign.Center
                )
            }
            field.outcome == RiskSpinOutcome.Revert -> {
                UndoJokerIcon(
                    enabled = !field.lostBecauseInventoryFull,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = rewardLabel(field.outcome, field.lostBecauseInventoryFull),
                    color = rewardTextColor(field),
                    style = MaterialTheme.typography.labelSmall.retroText(theme),
                    textAlign = TextAlign.Center
                )
            }
            else -> Unit
        }
    }
}

private fun rewardLabel(
    outcome: RiskSpinOutcome,
    lostBecauseInventoryFull: Boolean
): String {
    if (lostBecauseInventoryFull) return "FULL"
    return when (outcome) {
        RiskSpinOutcome.Miss -> "MISS"
        RiskSpinOutcome.Single -> "1x1"
        RiskSpinOutcome.HorizontalTwo -> "2x1"
        RiskSpinOutcome.VerticalTwo -> "1x2"
        RiskSpinOutcome.Bomb -> "BOMB"
        RiskSpinOutcome.Revert -> "UNDO"
    }
}

@Composable
private fun rewardTextColor(field: RiskSpinMemoryField) = when {
    field.lostBecauseInventoryFull -> LocalGridfallColors.current.danger
    field.jokerAdded != null -> LocalGridfallColors.current.success
    else -> LocalGridfallColors.current.textSecondary
}

private fun hiddenPlaceholderFields(): List<RiskSpinMemoryField> {
    return List(RiskSpin.MEMORY_FIELD_COUNT) { index ->
        RiskSpinMemoryField(
            id = index,
            outcome = RiskSpinOutcome.Miss
        )
    }
}
