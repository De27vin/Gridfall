package com.example.gridfall.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gridfall.game.Contract
import com.example.gridfall.game.ContractState
import com.example.gridfall.game.ContractType

@Composable
fun ContractOfferPopup(
    contract: Contract,
    onAccept: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1F2937)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = contract.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "+${contract.rewardPoints} / -${contract.penaltyPoints}",
                    color = Color(0xFFBAE6FD),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Text(
                text = contract.description,
                color = Color(0xFFD1D5DB),
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Reward +${contract.rewardPoints}   Penalty -${contract.penaltyPoints}",
                color = Color(0xFFFCA5A5),
                style = MaterialTheme.typography.labelLarge
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF38BDF8),
                        contentColor = Color(0xFF082F49)
                    )
                ) {
                    Text(text = "Accept")
                }

                OutlinedButton(
                    onClick = onSkip,
                    border = BorderStroke(1.dp, Color(0xFF9CA3AF)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFE5E7EB)
                    )
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

    Text(
        text = "${contract.title} - ${shortProgressText(contractState)} - +${contract.rewardPoints}/-${contract.penaltyPoints}",
        color = Color(0xFFBAE6FD),
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .background(Color(0xEE1F2937), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
fun ContractResultChip(
    contractState: ContractState,
    modifier: Modifier = Modifier
) {
    val contract = contractState.resolvedContract ?: return
    val text = if (contractState.isCompleted) {
        "Contract Complete +${contract.rewardPoints}"
    } else {
        "Contract Failed -${contract.penaltyPoints}"
    }
    val textColor = if (contractState.isCompleted) Color(0xFFBAE6FD) else Color(0xFFFCA5A5)

    Text(
        text = text,
        color = textColor,
        style = MaterialTheme.typography.titleSmall,
        modifier = modifier
            .background(Color(0xF01F2937), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    )
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
