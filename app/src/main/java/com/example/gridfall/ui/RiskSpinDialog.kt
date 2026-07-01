package com.example.gridfall.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.gridfall.game.GameState
import com.example.gridfall.game.RiskSpinOption
import com.example.gridfall.game.RiskSpinResult
import com.example.gridfall.ui.theme.PremiumTacticalColors

@Composable
fun RiskSpinDialog(
    gameState: GameState,
    onOptionSelected: (RiskSpinOption) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = PremiumTacticalColors
    val dialogShape = RoundedCornerShape(22.dp)
    val buttonShape = RoundedCornerShape(12.dp)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.dialogBackground,
        shape = dialogShape,
        tonalElevation = 0.dp,
        title = {
            Text(
                text = "Risk Spin",
                color = colors.textPrimary,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Pay score now for a chance at joker tools. Misses give nothing.",
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                RiskSpinOption.entries.forEach { option ->
                    val cost = option.cost(gameState.score)
                    val canAfford = cost <= gameState.score
                    Button(
                        onClick = { onOptionSelected(option) },
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.button,
                            contentColor = colors.textPrimary,
                            disabledContainerColor = colors.chipBackground.copy(alpha = 0.58f),
                            disabledContentColor = colors.textMuted
                        ),
                        shape = buttonShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = option.title,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "${option.spinCount} spin${if (option.spinCount == 1) "" else "s"}",
                                    color = if (canAfford) colors.textSecondary else colors.textMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = "-$cost",
                                color = if (canAfford) colors.warning else colors.textMuted,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, colors.panelBorder.copy(alpha = 0.72f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                shape = buttonShape
            ) {
                Text(text = "Cancel")
            }
        },
        confirmButton = {}
    )
}

@Composable
fun RiskSpinResultDialog(
    result: RiskSpinResult,
    onDismiss: () -> Unit
) {
    val colors = PremiumTacticalColors
    val dialogShape = RoundedCornerShape(22.dp)
    val buttonShape = RoundedCornerShape(12.dp)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.dialogBackground,
        shape = dialogShape,
        tonalElevation = 0.dp,
        title = {
            Text(
                text = "Spin Result",
                color = colors.textPrimary,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${result.option.title} paid ${result.cost} score.",
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(2.dp))
                result.entries.forEachIndexed { index, entry ->
                    val label = when {
                        entry.jokerAdded != null -> entry.jokerAdded.title
                        entry.lostBecauseInventoryFull -> "${entry.outcome.title} lost"
                        else -> entry.outcome.title
                    }
                    val detail = when {
                        entry.jokerAdded != null -> "Added"
                        entry.lostBecauseInventoryFull -> "Inventory Full"
                        else -> "No reward"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.chipBackground.copy(alpha = 0.72f))
                            .border(
                                width = 1.dp,
                                color = colors.panelBorder.copy(alpha = 0.42f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}. $label",
                            color = colors.textPrimary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = detail,
                            color = if (entry.jokerAdded != null) colors.success else colors.textMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentStrong,
                    contentColor = colors.dialogBackground
                ),
                shape = buttonShape
            ) {
                Text(text = "Close")
            }
        },
        dismissButton = {}
    )
}
