package com.example.gridfall.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.gridfall.network.AccountConnectionState
import com.example.gridfall.ui.theme.ActionCyan
import com.example.gridfall.ui.theme.BlueGray
import com.example.gridfall.ui.theme.ContractChipNavy
import com.example.gridfall.ui.theme.GridfallThemeMode
import com.example.gridfall.ui.theme.IceWhite
import com.example.gridfall.ui.theme.MutedSlate
import com.example.gridfall.ui.theme.SlateButton
import com.example.gridfall.ui.theme.SoftCyanBorder
import com.example.gridfall.ui.theme.SoftIce
import com.example.gridfall.ui.theme.LocalGridfallColors
import kotlin.math.roundToInt

private val themeOptions = listOf(
    GridfallThemeMode.PremiumTactical,
    GridfallThemeMode.InfernoCore,
    GridfallThemeMode.RetroArcade
)

@Composable
fun SettingsScreen(
    selectedThemeMode: GridfallThemeMode,
    soundEffectsVolume: Float,
    backgroundMusicVolume: Float,
    onThemeSelected: (GridfallThemeMode) -> Unit,
    onSoundEffectsVolumeChange: (Float) -> Unit,
    onBackgroundMusicVolumeChange: (Float) -> Unit,
    accountConnectionState: AccountConnectionState,
    onReturnToGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalGridfallColors.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.backgroundTop, theme.backgroundBottom)))
            .infernoAppTexture(theme)
            .retroAppTexture(theme)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Settings",
                color = theme.textPrimary,
                style = MaterialTheme.typography.headlineMedium.retroText(theme)
            )

            SettingsPanel(title = "Account") {
                AccountStatusSection(accountConnectionState)
            }

            SettingsPanel(title = "Theme") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    themeOptions.forEach { option ->
                        ThemeOptionRow(
                            label = option.label,
                            selected = selectedThemeMode == option,
                            onClick = { onThemeSelected(option) }
                        )
                    }
                }
            }

            SettingsPanel(title = "Sound") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    VolumeSliderRow(
                        label = "Sound Effects",
                        value = soundEffectsVolume,
                        onValueChange = onSoundEffectsVolumeChange
                    )
                    VolumeSliderRow(
                        label = "Background Music",
                        value = backgroundMusicVolume,
                        onValueChange = onBackgroundMusicVolumeChange
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val returnShape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 16.dp)))
            Button(
                onClick = onReturnToGame,
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.button,
                    contentColor = theme.textPrimary
                ),
                shape = returnShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .infernoPanelTexture(theme)
                    .retroPanelTexture(theme)
                    .border(
                        width = if (theme.isRetroTheme() || theme.isInfernoTheme()) 1.dp else 0.dp,
                        color = theme.panelBorder.copy(alpha = if (theme.isRetroTheme() || theme.isInfernoTheme()) 0.82f else 0f),
                        shape = returnShape
                    )
            ) {
                Text(
                    text = if (theme.isRetroTheme()) "RETURN TO GAME" else "Return to Game",
                    style = MaterialTheme.typography.labelLarge.retroText(theme)
                )
            }
        }
    }
}

@Composable
private fun AccountStatusSection(accountConnectionState: AccountConnectionState) {
    val theme = LocalGridfallColors.current
    val accountText = when {
        accountConnectionState.isLoading -> "Connecting guest account..."
        accountConnectionState.hasFirebaseUser && accountConnectionState.isAnonymous -> "Guest account active"
        accountConnectionState.hasFirebaseUser -> "Account active"
        else -> "Guest account unavailable"
    }
    val backendText = when {
        accountConnectionState.isBackendConnected -> "Backend connected"
        accountConnectionState.backendError != null -> "Backend unavailable"
        accountConnectionState.authError != null -> "Auth unavailable"
        else -> "Backend pending"
    }
    val uidText = accountConnectionState.firebaseUid?.let { uid ->
        "Firebase UID: ${uid.take(8)}..."
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = accountText,
            color = if (accountConnectionState.hasFirebaseUser) theme.textPrimary else theme.warning,
            style = MaterialTheme.typography.bodyLarge.retroText(theme)
        )
        Text(
            text = backendText,
            color = if (accountConnectionState.isBackendConnected) theme.success else theme.textSecondary,
            style = MaterialTheme.typography.bodyMedium.retroText(theme)
        )
        if (uidText != null) {
            Text(
                text = uidText,
                color = theme.textMuted,
                style = MaterialTheme.typography.labelMedium.retroText(theme)
            )
        }
    }
}

@Composable
private fun VolumeSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    val theme = LocalGridfallColors.current
    val clampedValue = value.coerceIn(0f, 1f)
    val percentage = (clampedValue * 100).roundToInt()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = theme.textPrimary,
                style = MaterialTheme.typography.titleSmall.retroText(theme)
            )
            Text(
                text = "$percentage%",
                color = theme.accentStrong,
                style = MaterialTheme.typography.labelLarge.retroText(theme)
            )
        }

        Slider(
            value = clampedValue,
            onValueChange = { rawValue ->
                val steppedValue = (rawValue.coerceIn(0f, 1f) * 20).roundToInt() / 20f
                onValueChange(steppedValue.coerceIn(0f, 1f))
            },
            valueRange = 0f..1f,
            steps = 19,
            colors = SliderDefaults.colors(
                thumbColor = theme.accentStrong,
                activeTrackColor = theme.accent,
                inactiveTrackColor = theme.button.copy(alpha = 0.70f),
                activeTickColor = theme.textPrimary.copy(alpha = 0.46f),
                inactiveTickColor = theme.panelBorder.copy(alpha = 0.58f)
            )
        )
    }
}

@Composable
private fun SettingsPanel(
    title: String,
    content: @Composable () -> Unit
) {
    val theme = LocalGridfallColors.current
    val shape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 18.dp)))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(theme.darkGlass)
            .infernoPanelTexture(theme)
            .retroPanelTexture(theme)
            .border(
                if (theme.isRetroTheme() || theme.isInfernoTheme()) 2.dp else 1.dp,
                theme.panelBorder.copy(alpha = if (theme.isRetroTheme() || theme.isInfernoTheme()) 0.84f else 0.48f),
                shape
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            color = theme.textSecondary,
            style = MaterialTheme.typography.titleSmall.retroText(theme)
        )
        content()
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalGridfallColors.current
    val borderColor = if (selected) theme.accentStrong else theme.panelBorder.copy(alpha = 0.34f)
    val backgroundColor = if (selected) theme.accent.copy(alpha = 0.14f) else theme.chipBackground.copy(alpha = 0.62f)
    val textColor = if (selected) theme.textPrimary else theme.textSecondary
    val markerColor = if (selected) theme.accentStrong else theme.textMuted
    val shape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 14.dp)))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
            .infernoPanelTexture(theme)
            .retroPanelTexture(theme)
            .border(BorderStroke(if (theme.isRetroTheme() || theme.isInfernoTheme()) 2.dp else 1.dp, borderColor), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.bodyLarge.retroText(theme)
        )
        Text(
            text = if (selected) "Selected" else "Select",
            color = markerColor,
            style = MaterialTheme.typography.labelMedium.retroText(theme)
        )
    }
}
