package com.example.gridfall.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gridfall.ui.infernoCorner
import com.example.gridfall.ui.infernoPanelTexture
import com.example.gridfall.ui.retroCorner
import com.example.gridfall.ui.retroPanelTexture
import com.example.gridfall.ui.retroText
import com.example.gridfall.ui.theme.LocalGridfallColors

@Composable
fun LogoutConfirmDialog(
    isLoading: Boolean,
    onCancel: () -> Unit,
    onConfirmLogout: () -> Unit
) {
    val theme = LocalGridfallColors.current
    val dialogShape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 24.dp)))
    val buttonShape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 14.dp)))

    AlertDialog(
        onDismissRequest = { if (!isLoading) onCancel() },
        modifier = Modifier
            .infernoPanelTexture(theme)
            .retroPanelTexture(theme),
        containerColor = theme.dialogBackground,
        shape = dialogShape,
        tonalElevation = 0.dp,
        title = {
            Text(
                text = "Log out?",
                color = theme.textPrimary,
                style = MaterialTheme.typography.headlineSmall.retroText(theme)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "You will return to a guest account. Your registered account can be used again by logging in.",
                    color = theme.textSecondary,
                    style = MaterialTheme.typography.bodyMedium.retroText(theme)
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCancel,
                enabled = !isLoading,
                border = BorderStroke(1.dp, theme.panelBorder.copy(alpha = 0.72f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.textSecondary),
                shape = buttonShape
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge.retroText(theme))
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmLogout,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.danger,
                    contentColor = theme.textPrimary
                ),
                shape = buttonShape
            ) {
                Text(
                    text = if (isLoading) "Working..." else "Log out",
                    style = MaterialTheme.typography.labelLarge.retroText(theme)
                )
            }
        }
    )
}
