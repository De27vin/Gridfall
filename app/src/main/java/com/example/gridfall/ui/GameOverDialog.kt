package com.example.gridfall.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun GameOverDialog(
    finalScore: Int,
    onRestart: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(text = "Game Over")
        },
        text = {
            Text(text = "Final score: $finalScore")
        },
        confirmButton = {
            Button(onClick = onRestart) {
                Text(text = "Restart")
            }
        }
    )
}
