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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
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

private val themeOptions = listOf(
    GridfallThemeMode.PremiumTactical,
    GridfallThemeMode.InfernoCore
)

@Composable
fun SettingsScreen(
    selectedThemeMode: GridfallThemeMode,
    soundEnabled: Boolean,
    onThemeSelected: (GridfallThemeMode) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onReturnToGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalGridfallColors.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.backgroundTop, theme.backgroundBottom)))
            .infernoAppTexture(theme)
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
                style = MaterialTheme.typography.headlineMedium
            )

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (soundEnabled) "Sound On" else "Sound Off",
                            color = theme.textPrimary,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Placeholder for future audio.",
                            color = theme.textMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = onSoundEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = theme.chipBackground,
                            checkedTrackColor = theme.accentStrong,
                            uncheckedThumbColor = theme.textSecondary,
                            uncheckedTrackColor = theme.button,
                            uncheckedBorderColor = theme.panelBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onReturnToGame,
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.button,
                    contentColor = theme.textPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Return to Game", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    title: String,
    content: @Composable () -> Unit
) {
    val theme = LocalGridfallColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(theme.darkGlass)
            .infernoPanelTexture(theme)
            .border(1.dp, theme.panelBorder.copy(alpha = 0.48f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            color = theme.textSecondary,
            style = MaterialTheme.typography.titleSmall
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .infernoPanelTexture(theme)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = if (selected) "Selected" else "Select",
            color = markerColor,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
