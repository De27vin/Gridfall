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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gridfall.ui.theme.ActionCyan
import com.example.gridfall.ui.theme.BlueGray
import com.example.gridfall.ui.theme.ContractChipNavy
import com.example.gridfall.ui.theme.DialogNavy
import com.example.gridfall.ui.theme.IceWhite
import com.example.gridfall.ui.theme.PremiumGold
import com.example.gridfall.ui.theme.RewardMint
import com.example.gridfall.ui.theme.SoftIce
import java.util.Locale

@Composable
fun GameOverDialog(
    finalScore: Int,
    highScore: Int,
    isNewBest: Boolean,
    onRestart: () -> Unit
) {
    val theme = com.example.gridfall.ui.theme.LocalGridfallColors.current
    val dialogShape = RoundedCornerShape(retroCorner(theme, 24.dp))

    AlertDialog(
        onDismissRequest = {},
        modifier = Modifier
            .infernoPanelTexture(theme)
            .retroPanelTexture(theme),
        containerColor = theme.dialogBackground,
        shape = dialogShape,
        tonalElevation = 0.dp,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (theme.isRetroTheme()) "GAME OVER" else "Game Over",
                    color = theme.textPrimary,
                    style = MaterialTheme.typography.headlineMedium.retroText(theme)
                )
                if (isNewBest) {
                    Box(
                        modifier = Modifier
                            .background(theme.success.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                            .border(1.dp, theme.success.copy(alpha = 0.42f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "New Best!",
                            color = theme.success,
                            style = MaterialTheme.typography.labelLarge.retroText(theme)
                        )
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ScoreRow(label = "FINAL SCORE", value = finalScore, accent = theme.warning)
                ScoreRow(label = "HIGH SCORE", value = highScore, accent = theme.accentStrong)
            }
        },
        confirmButton = {
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.accentStrong,
                    contentColor = theme.chipBackground
                ),
                shape = RoundedCornerShape(retroCorner(theme, 14.dp))
            ) {
                Text(text = if (theme.isRetroTheme()) "RESTART" else "Restart", style = MaterialTheme.typography.labelLarge.retroText(theme))
            }
        }
    )
}

@Composable
private fun ScoreRow(
    label: String,
    value: Int,
    accent: androidx.compose.ui.graphics.Color
) {
    val theme = com.example.gridfall.ui.theme.LocalGridfallColors.current
    val shape = RoundedCornerShape(retroCorner(theme, 14.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.chipBackground.copy(alpha = 0.72f), shape)
            .retroPanelTexture(theme)
            .border(BorderStroke(if (theme.isRetroTheme()) 2.dp else 1.dp, accent.copy(alpha = if (theme.isRetroTheme()) 0.62f else 0.34f)), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = theme.textMuted,
            style = MaterialTheme.typography.labelMedium.retroText(theme)
        )
        Text(
            text = String.format(Locale.US, "%,d", value),
            color = theme.textSecondary,
            style = MaterialTheme.typography.titleSmall.retroText(theme)
        )
    }
}
