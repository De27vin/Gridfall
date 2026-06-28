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
    AlertDialog(
        onDismissRequest = {},
        containerColor = DialogNavy,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Game Over",
                    color = IceWhite,
                    style = MaterialTheme.typography.headlineMedium
                )
                if (isNewBest) {
                    Box(
                        modifier = Modifier
                            .background(RewardMint.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                            .border(1.dp, RewardMint.copy(alpha = 0.42f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "New Best!",
                            color = RewardMint,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ScoreRow(label = "FINAL SCORE", value = finalScore, accent = PremiumGold)
                ScoreRow(label = "HIGH SCORE", value = highScore, accent = ActionCyan)
            }
        },
        confirmButton = {
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ActionCyan,
                    contentColor = ContractChipNavy
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = "Restart")
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ContractChipNavy.copy(alpha = 0.72f), RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.34f)), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = BlueGray,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = String.format(Locale.US, "%,d", value),
            color = SoftIce,
            style = MaterialTheme.typography.titleSmall
        )
    }
}
