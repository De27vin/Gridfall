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
import com.example.gridfall.ui.theme.DarkGlass
import com.example.gridfall.ui.theme.DeepGraphite
import com.example.gridfall.ui.theme.IceWhite
import com.example.gridfall.ui.theme.MidnightNavy
import com.example.gridfall.ui.theme.MutedSlate
import com.example.gridfall.ui.theme.SlateButton
import com.example.gridfall.ui.theme.SoftCyanBorder
import com.example.gridfall.ui.theme.SoftIce

private val themeOptions = listOf(
    "Premium Tactical",
    "Neon Grid",
    "Classic Dark"
)

@Composable
fun SettingsScreen(
    selectedTheme: String,
    soundEnabled: Boolean,
    onThemeSelected: (String) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onReturnToGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MidnightNavy, DeepGraphite)))
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
                color = IceWhite,
                style = MaterialTheme.typography.headlineMedium
            )

            SettingsPanel(title = "Theme") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    themeOptions.forEach { option ->
                        ThemeOptionRow(
                            label = option,
                            selected = selectedTheme == option,
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
                            color = IceWhite,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Placeholder for future audio.",
                            color = BlueGray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = onSoundEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ContractChipNavy,
                            checkedTrackColor = ActionCyan,
                            uncheckedThumbColor = SoftIce,
                            uncheckedTrackColor = SlateButton,
                            uncheckedBorderColor = SoftCyanBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onReturnToGame,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SlateButton,
                    contentColor = IceWhite
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkGlass)
            .border(1.dp, SoftCyanBorder.copy(alpha = 0.48f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            color = SoftIce,
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
    val borderColor = if (selected) ActionCyan else SoftCyanBorder.copy(alpha = 0.34f)
    val backgroundColor = if (selected) ActionCyan.copy(alpha = 0.14f) else ContractChipNavy.copy(alpha = 0.62f)
    val textColor = if (selected) IceWhite else SoftIce
    val markerColor = if (selected) ActionCyan else MutedSlate

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
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
