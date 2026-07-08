package com.example.gridfall.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import com.example.gridfall.ui.infernoCorner
import com.example.gridfall.ui.infernoPanelTexture
import com.example.gridfall.ui.retroCorner
import com.example.gridfall.ui.retroPanelTexture
import com.example.gridfall.ui.retroText
import com.example.gridfall.ui.theme.LocalGridfallColors

@Composable
fun SaveProgressPrompt(
    onRegister: () -> Unit,
    onLogin: () -> Unit,
    onSkip: () -> Unit
) {
    val theme = LocalGridfallColors.current
    val dialogShape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 24.dp)))
    val buttonShape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 14.dp)))

    AlertDialog(
        onDismissRequest = onSkip,
        modifier = androidx.compose.ui.Modifier
            .infernoPanelTexture(theme)
            .retroPanelTexture(theme),
        containerColor = theme.dialogBackground,
        shape = dialogShape,
        tonalElevation = 0.dp,
        title = {
            Text(
                text = "Save your progress?",
                color = theme.textPrimary,
                style = MaterialTheme.typography.headlineSmall.retroText(theme)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Create an account or log in to save your progress and join the leaderboard.",
                    color = theme.textSecondary,
                    style = MaterialTheme.typography.bodyMedium.retroText(theme)
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onSkip,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.textSecondary),
                shape = buttonShape
            ) {
                Text("Skip", style = MaterialTheme.typography.labelLarge.retroText(theme))
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRegister,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.accentStrong,
                        contentColor = theme.chipBackground
                    ),
                    shape = buttonShape
                ) {
                    Text("Register", style = MaterialTheme.typography.labelLarge.retroText(theme))
                }
                OutlinedButton(
                    onClick = onLogin,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.textSecondary),
                    shape = buttonShape
                ) {
                    Text("Login", style = MaterialTheme.typography.labelLarge.retroText(theme))
                }
            }
        }
    )
}
