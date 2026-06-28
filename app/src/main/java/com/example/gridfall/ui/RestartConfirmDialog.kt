package com.example.gridfall.ui

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
import androidx.compose.ui.unit.dp
import com.example.gridfall.ui.theme.ContractChipNavy
import com.example.gridfall.ui.theme.CoralWarning
import com.example.gridfall.ui.theme.DialogNavy
import com.example.gridfall.ui.theme.IceWhite
import com.example.gridfall.ui.theme.SoftCyanBorder
import com.example.gridfall.ui.theme.SoftIce

@Composable
fun RestartConfirmDialog(
    onCancel: () -> Unit,
    onConfirmRestart: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = DialogNavy,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp,
        title = {
            Text(
                text = "Restart game?",
                color = IceWhite,
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Your current run will be lost.",
                    color = SoftIce,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCancel,
                border = BorderStroke(1.dp, SoftCyanBorder.copy(alpha = 0.72f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = SoftIce
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = "Cancel")
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmRestart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CoralWarning,
                    contentColor = ContractChipNavy
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = "Restart")
            }
        }
    )
}
