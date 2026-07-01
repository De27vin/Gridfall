package com.example.gridfall.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import com.example.gridfall.game.GameEngine
import com.example.gridfall.game.GameState
import com.example.gridfall.game.RiskSpinOption
import com.example.gridfall.game.RiskSpinState
import com.example.gridfall.ui.theme.LocalGridfallColors

@Composable
fun RiskSpinDialog(
    gameState: GameState,
    onOptionSelected: (RiskSpinOption) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalGridfallColors.current
    val dialogShape = RoundedCornerShape(retroCorner(colors, infernoCorner(colors, 22.dp)))
    val buttonShape = RoundedCornerShape(retroCorner(colors, infernoCorner(colors, 12.dp)))

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .infernoPanelTexture(colors)
            .retroPanelTexture(colors),
        containerColor = colors.dialogBackground,
        shape = dialogShape,
        tonalElevation = 0.dp,
        title = {
            Text(
                text = "Risk Spin",
                color = colors.textPrimary,
                style = MaterialTheme.typography.headlineSmall.retroText(colors)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Pay score now, then reveal hidden fields. Misses give nothing. Inventory max is ${RiskSpinState.MAX_INVENTORY_SIZE}.",
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium.retroText(colors)
                )
                RiskSpinOption.entries.forEach { option ->
                    val cost = option.cost(gameState.score)
                    val canSelect = GameEngine.canSelectRiskSpinOption(gameState, option)
                    Button(
                        onClick = { onOptionSelected(option) },
                        enabled = canSelect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.button,
                            contentColor = colors.textPrimary,
                            disabledContainerColor = colors.chipBackground.copy(alpha = 0.58f),
                            disabledContentColor = colors.textMuted
                        ),
                        shape = buttonShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .infernoPanelTexture(colors)
                            .retroPanelTexture(colors)
                            .border(
                                width = if (colors.isRetroTheme() || colors.isInfernoTheme()) 2.dp else 1.dp,
                                color = if (canSelect) {
                                    colors.accentStrong.copy(alpha = if (colors.isRetroTheme() || colors.isInfernoTheme()) 0.82f else 0.48f)
                                } else {
                                    colors.panelBorder.copy(alpha = 0.28f)
                                },
                                shape = buttonShape
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = option.title,
                                    style = MaterialTheme.typography.labelLarge.retroText(colors)
                                )
                                Text(
                                    text = "${option.spinCount} reveal${if (option.spinCount == 1) "" else "s"}",
                                    color = if (canSelect) colors.textSecondary else colors.textMuted,
                                    style = MaterialTheme.typography.bodySmall.retroText(colors)
                                )
                                if (!canSelect) {
                                    Text(
                                        text = "Would drop below Level 3",
                                        color = colors.textMuted,
                                        style = MaterialTheme.typography.labelSmall.retroText(colors)
                                    )
                                }
                            }
                            Text(
                                text = "-$cost",
                                color = if (canSelect) colors.warning else colors.textMuted,
                                style = MaterialTheme.typography.labelLarge.retroText(colors)
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
                Text(
                    text = if (colors.isRetroTheme()) "CANCEL" else "Cancel",
                    style = MaterialTheme.typography.labelLarge.retroText(colors)
                )
            }
        },
        confirmButton = {}
    )
}
