package com.example.gridfall.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
    GridfallThemeMode.RetroArcade,
    GridfallThemeMode.Blockworld,
    GridfallThemeMode.Aero
)

private fun pendingRunsLabel(count: Int): String {
    return if (count == 1) "1 run pending" else "$count runs pending"
}

@Composable
fun SettingsScreen(
    selectedThemeMode: GridfallThemeMode,
    soundEffectsVolume: Float,
    backgroundMusicVolume: Float,
    onThemeSelected: (GridfallThemeMode) -> Unit,
    onSoundEffectsVolumeChange: (Float) -> Unit,
    onBackgroundMusicVolumeChange: (Float) -> Unit,
    accountConnectionState: AccountConnectionState,
    debugApiBaseUrl: String?,
    runSyncMessage: String?,
    pendingRunCount: Int,
    isRetryingPendingRuns: Boolean,
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRefreshAccountClick: () -> Unit,
    onRetrySyncClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onLogoutClick: () -> Unit,
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

            SettingsPanel(
                title = "Account",
                trailingContent = {
                    AccountRefreshButton(
                        enabled = !accountConnectionState.isLoading,
                        onClick = onRefreshAccountClick
                    )
                }
            ) {
                AccountStatusSection(
                    accountConnectionState = accountConnectionState,
                    debugApiBaseUrl = debugApiBaseUrl,
                    runSyncMessage = runSyncMessage,
                    pendingRunCount = pendingRunCount,
                    isRetryingPendingRuns = isRetryingPendingRuns,
                    onRegisterClick = onRegisterClick,
                    onLoginClick = onLoginClick,
                    onRetrySyncClick = onRetrySyncClick,
                    onLeaderboardClick = onLeaderboardClick,
                    onLogoutClick = onLogoutClick
                )
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
private fun AccountStatusSection(
    accountConnectionState: AccountConnectionState,
    debugApiBaseUrl: String?,
    runSyncMessage: String?,
    pendingRunCount: Int,
    isRetryingPendingRuns: Boolean,
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRetrySyncClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val theme = LocalGridfallColors.current
    val backendUser = accountConnectionState.backendUser
    val isGuest = accountConnectionState.isAnonymous
    val accountText = when {
        accountConnectionState.isLoading -> "Connecting guest account..."
        accountConnectionState.hasFirebaseUser && isGuest -> "Guest account"
        accountConnectionState.hasFirebaseUser -> "Signed in"
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
    val syncText = when {
        isRetryingPendingRuns -> "Syncing pending runs..."
        pendingRunCount > 0 -> pendingRunsLabel(pendingRunCount)
        runSyncMessage?.contains("failed", ignoreCase = true) == true -> "Last sync failed"
        else -> "All runs synced"
    }
    val showRetrySync = pendingRunCount > 0 || runSyncMessage?.contains("failed", ignoreCase = true) == true

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = accountText,
            color = if (accountConnectionState.hasFirebaseUser) theme.textPrimary else theme.warning,
            style = MaterialTheme.typography.bodyLarge.retroText(theme)
        )
        if (!isGuest && backendUser?.email != null) {
            Text(
                text = "Email: ${backendUser.email}",
                color = theme.textSecondary,
                style = MaterialTheme.typography.bodyMedium.retroText(theme)
            )
        }
        if (!isGuest && backendUser?.username != null) {
            Text(
                text = "Username: ${backendUser.username}",
                color = theme.textSecondary,
                style = MaterialTheme.typography.bodyMedium.retroText(theme)
            )
        }
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
        if (debugApiBaseUrl != null) {
            Text(
                text = "API: $debugApiBaseUrl",
                color = theme.textMuted,
                style = MaterialTheme.typography.labelMedium.retroText(theme)
            )
        }
        Text(
            text = syncText,
            color = if (pendingRunCount > 0 || syncText.contains("failed", ignoreCase = true)) {
                theme.warning
            } else {
                theme.success
            },
            style = MaterialTheme.typography.bodySmall.retroText(theme)
        )

        if (isGuest) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountActionButton(label = "Register", onClick = onRegisterClick, modifier = Modifier.weight(1f))
                AccountActionButton(label = "Login", onClick = onLoginClick, modifier = Modifier.weight(1f))
            }
        } else {
            AccountActionButton(label = "Log out", onClick = onLogoutClick)
        }
        if (showRetrySync) {
            AccountActionButton(
                label = if (isRetryingPendingRuns) "Syncing..." else "Retry sync",
                onClick = onRetrySyncClick
            )
        }
        AccountActionButton(label = "Leaderboard", onClick = onLeaderboardClick)
    }
}

@Composable
private fun AccountActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalGridfallColors.current
    val shape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 14.dp)))

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = theme.button,
            contentColor = theme.textPrimary
        ),
        shape = shape,
        modifier = modifier
            .infernoPanelTexture(theme)
            .retroPanelTexture(theme)
    ) {
        Text(
            text = if (theme.isRetroTheme()) label.uppercase() else label,
            style = MaterialTheme.typography.labelLarge.retroText(theme)
        )
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
    trailingContent: (@Composable () -> Unit)? = null,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = theme.textSecondary,
                style = MaterialTheme.typography.titleSmall.retroText(theme)
            )
            trailingContent?.invoke()
        }
        content()
    }
}

@Composable
private fun AccountRefreshButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalGridfallColors.current
    val shape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 12.dp)))

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(34.dp)
            .clip(shape)
            .background(theme.chipBackground.copy(alpha = if (enabled) 0.72f else 0.36f))
            .infernoPanelTexture(theme)
            .retroPanelTexture(theme)
            .border(
                width = if (theme.isRetroTheme() || theme.isInfernoTheme()) 2.dp else 1.dp,
                color = theme.panelBorder.copy(alpha = if (enabled) 0.62f else 0.30f),
                shape = shape
            )
    ) {
        ReloadArrowIcon(
            enabled = enabled,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ReloadArrowIcon(
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val theme = LocalGridfallColors.current

    ThemeIconAsset(
        kind = ThemeIconKind.Reload,
        colors = theme,
        contentDescription = "Reload account connection",
        enabled = enabled,
        modifier = modifier
    ) { fallbackModifier ->
        DrawnReloadArrowIcon(
            enabled = enabled,
            modifier = fallbackModifier
        )
    }
}

@Composable
private fun DrawnReloadArrowIcon(
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val theme = LocalGridfallColors.current
    val iconColor = if (enabled) theme.accentStrong else theme.textMuted

    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.12f
        val arcInset = size.minDimension * 0.14f
        drawArc(
            color = iconColor,
            startAngle = 34f,
            sweepAngle = 282f,
            useCenter = false,
            topLeft = Offset(arcInset, arcInset),
            size = androidx.compose.ui.geometry.Size(
                width = size.width - arcInset * 2f,
                height = size.height - arcInset * 2f
            ),
            style = Stroke(width = strokeWidth)
        )
        rotate(degrees = 34f, pivot = Offset(size.width * 0.74f, size.height * 0.22f)) {
            drawLine(
                color = iconColor,
                start = Offset(size.width * 0.74f, size.height * 0.22f),
                end = Offset(size.width * 0.95f, size.height * 0.24f),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = iconColor,
                start = Offset(size.width * 0.74f, size.height * 0.22f),
                end = Offset(size.width * 0.78f, size.height * 0.44f),
                strokeWidth = strokeWidth
            )
        }
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
