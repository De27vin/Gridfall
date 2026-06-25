package com.example.gridfall.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.example.gridfall.game.ContractState
import com.example.gridfall.game.ContractType

@Composable
fun ContractCard(
    contractState: ContractState,
    showResolvedResult: Boolean,
    onAccept: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val resolvedContract = contractState.resolvedContract
    val offeredContract = contractState.offeredContract
    val activeContract = contractState.activeContract

    when {
        showResolvedResult && resolvedContract != null && contractState.isCompleted -> {
            ContractSurface(modifier = modifier) {
                Text(
                    text = "Contract Complete +${resolvedContract.rewardPoints}",
                    color = Color(0xFFBAE6FD),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        showResolvedResult && resolvedContract != null && contractState.isFailed -> {
            ContractSurface(modifier = modifier) {
                Text(
                    text = "Contract Failed",
                    color = Color(0xFFFCA5A5),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        offeredContract != null -> {
            ContractSurface(modifier = modifier) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ContractText(
                        title = offeredContract.title,
                        description = offeredContract.description,
                        rewardPoints = offeredContract.rewardPoints
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

        activeContract != null -> {
            ContractSurface(modifier = modifier) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ContractText(
                        title = activeContract.title,
                        description = activeContract.description,
                        rewardPoints = activeContract.rewardPoints
                    )
                    Text(
                        text = progressText(contractState),
                        color = Color(0xFFBAE6FD),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun ContractSurface(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1F2937)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun ContractText(
    title: String,
    description: String,
    rewardPoints: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "+$rewardPoints",
                color = Color(0xFF38BDF8),
                style = MaterialTheme.typography.labelLarge
            )
        }
        Text(
            text = description,
            color = Color(0xFFD1D5DB),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun progressText(contractState: ContractState): String {
    val contract = contractState.activeContract ?: return ""
    val piecesText = "Pieces placed: ${contractState.batchPlacedPieces}/3"

    return when (contract.type) {
        ContractType.ClearAtLeastOneLine -> {
            "Lines cleared: ${contractState.batchClearedLines}\n$piecesText"
        }

        ContractType.ClearExactlyTwoLines -> {
            "Lines cleared: ${contractState.batchClearedLines}/2\n$piecesText"
        }

        ContractType.NoEdgePlacement -> {
            val edgeText = if (contractState.usedEdge) "Edge touched: yes" else "Edge touched: no"
            "$edgeText\n$piecesText"
        }

        ContractType.AvoidCenterArea -> {
            val centerText = if (contractState.usedCenter) "Center used: yes" else "Center used: no"
            "$centerText\n$piecesText"
        }

        ContractType.ScoreAtLeastTwenty -> {
            "Score this batch: ${contractState.batchScoreGained}/20\n$piecesText"
        }
    }
}
