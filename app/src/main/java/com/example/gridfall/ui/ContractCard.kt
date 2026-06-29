package com.example.gridfall.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gridfall.game.Contract
import com.example.gridfall.game.ContractState
import com.example.gridfall.game.ContractType
import com.example.gridfall.ui.theme.ActionCyan
import com.example.gridfall.ui.theme.BlueGray
import com.example.gridfall.ui.theme.ContractChipNavy
import com.example.gridfall.ui.theme.CoralWarning
import com.example.gridfall.ui.theme.DeepContractGlass
import com.example.gridfall.ui.theme.IceWhite
import com.example.gridfall.ui.theme.LocalGridfallColors
import com.example.gridfall.ui.theme.RewardMint
import com.example.gridfall.ui.theme.SoftCyanBorder
import com.example.gridfall.ui.theme.SoftIce

@Composable
fun ContractOfferPopup(
    contract: Contract,
    onAccept: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalGridfallColors.current

    Card(
        modifier = modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, theme.panelBorder.copy(alpha = 0.70f)),
        colors = CardDefaults.cardColors(
            containerColor = theme.panelBackground
        )
    ) {
        Column(
            modifier = Modifier
                .infernoPanelTexture(theme)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = contract.title,
                    color = theme.textPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "+${contract.rewardPoints}",
                    color = theme.success,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Text(
                text = contract.description,
                color = theme.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContractMiniBadge(
                    text = "Reward +${contract.rewardPoints}",
                    color = theme.success
                )
                ContractMiniBadge(
                    text = "Risk -${contract.penaltyPoints}",
                    color = theme.danger
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.accentStrong,
                        contentColor = theme.chipBackground
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = "Accept")
                }

                OutlinedButton(
                    onClick = onSkip,
                    border = BorderStroke(1.dp, theme.panelBorder.copy(alpha = 0.70f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = theme.textSecondary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = "Skip")
                }
            }
        }
    }
}

@Composable
fun ContractActiveChip(
    contractState: ContractState,
    modifier: Modifier = Modifier
) {
    val contract = contractState.activeContract ?: return
    val theme = LocalGridfallColors.current

    Row(
        modifier = modifier
            .background(theme.chipBackground.copy(alpha = 0.94f), RoundedCornerShape(999.dp))
            .infernoPanelTexture(theme)
            .border(1.dp, theme.panelBorder.copy(alpha = 0.62f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = contract.title,
            color = theme.textPrimary,
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = shortProgressText(contractState),
            color = theme.textMuted,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = "+${contract.rewardPoints}/-${contract.penaltyPoints}",
            color = theme.success,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun ContractResultChip(
    contractState: ContractState,
    modifier: Modifier = Modifier
) {
    val contract = contractState.resolvedContract ?: return
    val text = if (contractState.isCompleted) {
        "Mission Complete +${contract.rewardPoints}"
    } else {
        "Mission Failed -${contract.penaltyPoints}"
    }
    val theme = LocalGridfallColors.current
    val accent = if (contractState.isCompleted) theme.success else theme.danger

    Text(
        text = text,
        color = if (contractState.isCompleted) theme.textPrimary else theme.danger,
        style = MaterialTheme.typography.titleSmall,
        modifier = modifier
            .background(theme.chipBackground.copy(alpha = 0.96f), RoundedCornerShape(999.dp))
            .infernoPanelTexture(theme)
            .border(1.dp, accent.copy(alpha = 0.72f), RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun ContractMiniBadge(
    text: String,
    color: androidx.compose.ui.graphics.Color
) {
    val theme = LocalGridfallColors.current

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .infernoPanelTexture(theme)
            .border(1.dp, color.copy(alpha = 0.36f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

private fun shortProgressText(contractState: ContractState): String {
    val contract = contractState.activeContract ?: return ""

    return when (contract.type) {
        ContractType.ClearAtLeastOneLine -> "Lines ${contractState.batchClearedLines}/1"
        ContractType.ClearExactlyTwoLines -> "Lines ${contractState.batchClearedLines}/2"
        ContractType.NoEdgePlacement -> "${contractState.batchPlacedPieces}/3 pieces"
        ContractType.AvoidCenterArea -> "${contractState.batchPlacedPieces}/3 pieces"
        ContractType.ScoreAtLeastTwenty -> "Score ${contractState.batchScoreGained}/20"
    }
}
