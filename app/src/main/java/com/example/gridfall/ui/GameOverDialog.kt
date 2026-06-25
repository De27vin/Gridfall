package com.example.gridfall.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun GameOverDialog(
    finalScore: Int,
    highScore: Int,
    isNewBest: Boolean,
    onRestart: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(text = "Game Over")
        },
        text = {
            androidx.compose.foundation.layout.Column {
                Text(text = "Final score: $finalScore")
                Text(text = "High score: $highScore")
                if (isNewBest) {
                    Text(
                        text = "New Best!",
                        color = Color(0xFF38BDF8),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onRestart) {
                Text(text = "Restart")
            }
        }
    )
}
