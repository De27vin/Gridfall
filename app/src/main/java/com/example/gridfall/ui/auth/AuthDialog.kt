package com.example.gridfall.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.gridfall.ui.infernoCorner
import com.example.gridfall.ui.infernoPanelTexture
import com.example.gridfall.ui.retroCorner
import com.example.gridfall.ui.retroPanelTexture
import com.example.gridfall.ui.retroText
import com.example.gridfall.ui.theme.LocalGridfallColors

enum class AuthDialogMode {
    Register,
    Login,
    Username
}

@Composable
fun AuthDialog(
    mode: AuthDialogMode,
    isLoading: Boolean,
    message: String?,
    error: String?,
    initialUsername: String? = null,
    initialEmail: String? = null,
    onDismiss: () -> Unit,
    onSwitchMode: (AuthDialogMode) -> Unit,
    onRegister: (email: String, password: String, username: String) -> Unit,
    onLogin: (email: String, password: String) -> Unit,
    onSaveUsername: (username: String) -> Unit
) {
    val theme = LocalGridfallColors.current
    val dialogShape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 24.dp)))
    val buttonShape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 14.dp)))
    var email by remember(mode) { mutableStateOf(initialEmail.orEmpty()) }
    var password by remember(mode) { mutableStateOf("") }
    var username by remember(mode) { mutableStateOf(initialUsername.orEmpty()) }
    var localError by remember(mode) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        modifier = Modifier
            .infernoPanelTexture(theme)
            .retroPanelTexture(theme),
        containerColor = theme.dialogBackground,
        shape = dialogShape,
        tonalElevation = 0.dp,
        title = {
            Text(
                text = when (mode) {
                    AuthDialogMode.Register -> "Register"
                    AuthDialogMode.Login -> "Login"
                    AuthDialogMode.Username -> "Choose username"
                },
                color = theme.textPrimary,
                style = MaterialTheme.typography.headlineSmall.retroText(theme)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (mode == AuthDialogMode.Register || mode == AuthDialogMode.Login) {
                    AuthTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        enabled = !isLoading,
                        keyboardType = KeyboardType.Email
                    )
                    AuthTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        enabled = !isLoading,
                        isPassword = true
                    )
                }

                if (mode == AuthDialogMode.Register || mode == AuthDialogMode.Username) {
                    AuthTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = "Username",
                        enabled = !isLoading
                    )
                }

                val visibleError = localError ?: error
                if (visibleError != null) {
                    Text(
                        text = visibleError,
                        color = theme.danger,
                        style = MaterialTheme.typography.bodySmall.retroText(theme)
                    )
                } else if (message != null) {
                    Text(
                        text = message,
                        color = theme.success,
                        style = MaterialTheme.typography.bodySmall.retroText(theme)
                    )
                }

                if (mode != AuthDialogMode.Username) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                localError = null
                                onSwitchMode(if (mode == AuthDialogMode.Login) AuthDialogMode.Register else AuthDialogMode.Login)
                            },
                            enabled = !isLoading,
                            border = BorderStroke(1.dp, theme.panelBorder.copy(alpha = 0.72f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.textSecondary),
                            shape = buttonShape,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (mode == AuthDialogMode.Login) "Register" else "Login",
                                style = MaterialTheme.typography.labelLarge.retroText(theme)
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isLoading,
                border = BorderStroke(1.dp, theme.panelBorder.copy(alpha = 0.72f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.textSecondary),
                shape = buttonShape
            ) {
                Text(text = "Cancel", style = MaterialTheme.typography.labelLarge.retroText(theme))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    localError = validateInput(mode, email, password, username)
                    if (localError == null) {
                        when (mode) {
                            AuthDialogMode.Register -> onRegister(email.trim(), password, username.trim())
                            AuthDialogMode.Login -> onLogin(email.trim(), password)
                            AuthDialogMode.Username -> onSaveUsername(username.trim())
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.accentStrong,
                    contentColor = theme.chipBackground
                ),
                shape = buttonShape
            ) {
                Text(
                    text = if (isLoading) {
                        "Working..."
                    } else {
                        when (mode) {
                            AuthDialogMode.Register -> "Register"
                            AuthDialogMode.Login -> "Login"
                            AuthDialogMode.Username -> "Save Username"
                        }
                    },
                    style = MaterialTheme.typography.labelLarge.retroText(theme)
                )
            }
        }
    )
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    val theme = LocalGridfallColors.current

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = {
            Text(label, style = MaterialTheme.typography.labelMedium.retroText(theme))
        },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun validateInput(
    mode: AuthDialogMode,
    email: String,
    password: String,
    username: String
): String? {
    if ((mode == AuthDialogMode.Register || mode == AuthDialogMode.Login) && email.trim().isBlank()) {
        return "Email is required."
    }
    if ((mode == AuthDialogMode.Register || mode == AuthDialogMode.Login) && password.isBlank()) {
        return "Password is required."
    }
    if (mode == AuthDialogMode.Register || mode == AuthDialogMode.Username) {
        return validateUsername(username)
    }
    return null
}

private fun validateUsername(username: String): String? {
    val trimmed = username.trim()
    if (trimmed.length !in 3..16) return "Username must be 3-16 characters."
    if (!Regex("^[A-Za-z0-9 _-]{3,16}$").matches(trimmed)) {
        return "Use only letters, numbers, spaces, underscore, or minus."
    }
    return null
}
