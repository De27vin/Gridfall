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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.gridfall.game.GridLayoutPreset
import com.example.gridfall.game.Cell
import com.example.gridfall.game.CustomMapRules
import com.example.gridfall.game.CustomMapDesign
import com.example.gridfall.game.SavedCustomMap
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
    GridfallThemeMode.FrutigerAero
)

private fun pendingRunsLabel(count: Int): String {
    return if (count == 1) "1 run pending" else "$count runs pending"
}

@Composable
fun SettingsScreen(
    selectedThemeMode: GridfallThemeMode,
    selectedGridLayout: GridLayoutPreset,
    activeBoardSize: Int,
    activeIsCustomMap: Boolean,
    activeCustomMapDesign: CustomMapDesign?,
    savedCustomMaps: List<SavedCustomMap>,
    soundEffectsVolume: Float,
    backgroundMusicVolume: Float,
    onThemeSelected: (GridfallThemeMode) -> Unit,
    onGridLayoutSelected: (GridLayoutPreset) -> Unit,
    onCustomMapClick: () -> Unit,
    onSavedMapPlay: (SavedCustomMap) -> Unit,
    onSavedMapEdit: (SavedCustomMap) -> Unit,
    onSavedMapDelete: (SavedCustomMap) -> Unit,
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
    var mapPendingDelete by remember { mutableStateOf<SavedCustomMap?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.backgroundTop, theme.backgroundBottom)))
            .infernoAppTexture(theme)
            .retroAppTexture(theme)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .fillMaxHeight()
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

            SettingsPanel(title = "Grid Layout") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choose a size. Changing it starts a new run.",
                        color = theme.textMuted,
                        style = MaterialTheme.typography.bodySmall.retroText(theme)
                    )
                    GridLayoutPreset.entries.forEach { preset ->
                        GridLayoutOptionRow(
                            preset = preset,
                            selected = !activeIsCustomMap && selectedGridLayout == preset,
                            active = !activeIsCustomMap && activeBoardSize == preset.boardSize,
                            onClick = { onGridLayoutSelected(preset) }
                        )
                    }
                    CustomMapOptionRow(
                        active = activeIsCustomMap,
                        onClick = onCustomMapClick
                    )
                    Text(
                        text = "Saved Maps",
                        color = theme.textSecondary,
                        style = MaterialTheme.typography.titleSmall.retroText(theme),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (savedCustomMaps.isEmpty()) {
                        Text(
                            text = "No saved maps yet. Create one in the map editor.",
                            color = theme.textMuted,
                            style = MaterialTheme.typography.bodySmall.retroText(theme)
                        )
                    } else {
                        savedCustomMaps.forEach { savedMap ->
                            SavedMapOptionRow(
                                savedMap = savedMap,
                                active = activeCustomMapDesign == savedMap.design,
                                onPlay = { onSavedMapPlay(savedMap) },
                                onEdit = { onSavedMapEdit(savedMap) },
                                onDelete = { mapPendingDelete = savedMap }
                            )
                        }
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

    mapPendingDelete?.let { savedMap ->
        AlertDialog(
            onDismissRequest = { mapPendingDelete = null },
            containerColor = theme.dialogBackground,
            title = { Text("Delete ${savedMap.name}?", color = theme.textPrimary) },
            text = {
                Text(
                    "This removes the saved layout. A running game using it is not affected.",
                    color = theme.textSecondary
                )
            },
            dismissButton = {
                OutlinedButton(onClick = { mapPendingDelete = null }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSavedMapDelete(savedMap)
                        mapPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.danger,
                        contentColor = theme.textPrimary
                    )
                ) { Text("Delete") }
            }
        )
    }
}

@Composable
private fun CustomMapOptionRow(
    active: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalGridfallColors.current
    val shape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 14.dp)))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (active) theme.accent.copy(alpha = 0.14f) else theme.chipBackground.copy(alpha = 0.62f))
            .infernoPanelTexture(theme)
            .retroPanelTexture(theme)
            .border(
                BorderStroke(
                    if (theme.isRetroTheme() || theme.isInfernoTheme()) 2.dp else 1.dp,
                    if (active) theme.accentStrong else theme.panelBorder.copy(alpha = 0.34f)
                ),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GridLayoutPreview(
            boardSize = CustomMapRules.BOARD_SIZE,
            selected = active,
            blockedCells = CustomMapRules.diamondBlockedCells(),
            modifier = Modifier.size(48.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (theme.isRetroTheme()) "CUSTOM MAP" else "Custom Map",
                    color = if (active) theme.textPrimary else theme.textSecondary,
                    style = MaterialTheme.typography.bodyLarge.retroText(theme)
                )
                Text(
                    text = "EDITOR",
                    color = if (active) theme.accentStrong else theme.textMuted,
                    style = MaterialTheme.typography.labelLarge.retroText(theme)
                )
            }
            Text(
                text = "Design an unranked 3×3–10×10 board${if (active) " · Current run" else ""}",
                color = if (active) theme.success else theme.textMuted,
                style = MaterialTheme.typography.labelSmall.retroText(theme)
            )
        }
    }
}

@Composable
private fun SavedMapOptionRow(
    savedMap: SavedCustomMap,
    active: Boolean,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val theme = LocalGridfallColors.current
    val shape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 14.dp)))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (active) theme.accent.copy(alpha = 0.14f) else theme.chipBackground.copy(alpha = 0.62f))
            .infernoPanelTexture(theme)
            .retroPanelTexture(theme)
            .border(
                BorderStroke(
                    if (theme.isRetroTheme() || theme.isInfernoTheme()) 2.dp else 1.dp,
                    if (active) theme.accentStrong else theme.panelBorder.copy(alpha = 0.34f)
                ),
                shape
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SavedMapPreview(savedMap.design, Modifier.size(52.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = savedMap.name,
                    color = if (active) theme.textPrimary else theme.textSecondary,
                    style = MaterialTheme.typography.bodyLarge.retroText(theme)
                )
                Text(
                    text = "${savedMap.design.columns}×${savedMap.design.rows}",
                    color = if (active) theme.accentStrong else theme.textMuted,
                    style = MaterialTheme.typography.labelLarge.retroText(theme)
                )
            }
            if (active) {
                Text(
                    text = "Current run",
                    color = theme.success,
                    style = MaterialTheme.typography.labelSmall.retroText(theme)
                )
            }
            Text(
                text = "${savedMap.design.blockPool.size} blocks in pool",
                color = theme.accentStrong,
                style = MaterialTheme.typography.labelSmall.retroText(theme)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                SavedMapAction("Play", theme.accentStrong, onPlay)
                SavedMapAction("Edit", theme.textSecondary, onEdit)
                SavedMapAction("Delete", theme.danger, onDelete)
            }
        }
    }
}

@Composable
private fun SavedMapAction(label: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    val theme = LocalGridfallColors.current
    Text(
        text = label,
        color = color,
        style = MaterialTheme.typography.labelMedium.retroText(theme),
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 4.dp)
    )
}

@Composable
private fun SavedMapPreview(design: CustomMapDesign, modifier: Modifier = Modifier) {
    val theme = LocalGridfallColors.current
    Canvas(modifier = modifier) {
        val gap = size.minDimension * 0.035f
        val cellSize = minOf(
            (size.width - gap * (design.columns + 1)) / design.columns,
            (size.height - gap * (design.rows + 1)) / design.rows
        )
        val gridWidth = cellSize * design.columns + gap * (design.columns + 1)
        val gridHeight = cellSize * design.rows + gap * (design.rows + 1)
        val startX = (size.width - gridWidth) / 2f + gap
        val startY = (size.height - gridHeight) / 2f + gap
        drawRoundRect(
            color = theme.boardInner,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * 0.10f)
        )
        repeat(design.rows) { row ->
            repeat(design.columns) { col ->
                val blocked = Cell(row, col) in design.blockedCells
                val topLeft = Offset(
                    startX + col * (cellSize + gap),
                    startY + row * (cellSize + gap)
                )
                drawRoundRect(
                    color = if (blocked) theme.danger.copy(alpha = 0.72f) else theme.emptyCell,
                    topLeft = topLeft,
                    size = androidx.compose.ui.geometry.Size(cellSize, cellSize),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cellSize * 0.16f)
                )
            }
        }
    }
}

@Composable
private fun GridLayoutOptionRow(
    preset: GridLayoutPreset,
    selected: Boolean,
    active: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalGridfallColors.current
    val shape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 14.dp)))
    val borderColor = if (selected) theme.accentStrong else theme.panelBorder.copy(alpha = 0.34f)
    val backgroundColor = if (selected) {
        theme.accent.copy(alpha = 0.14f)
    } else {
        theme.chipBackground.copy(alpha = 0.62f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
            .infernoPanelTexture(theme)
            .retroPanelTexture(theme)
            .border(
                BorderStroke(
                    if (theme.isRetroTheme() || theme.isInfernoTheme()) 2.dp else 1.dp,
                    borderColor
                ),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GridLayoutPreview(
            boardSize = preset.boardSize,
            selected = selected,
            modifier = Modifier.size(48.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (theme.isRetroTheme()) preset.title.uppercase() else preset.title,
                    color = if (selected) theme.textPrimary else theme.textSecondary,
                    style = MaterialTheme.typography.bodyLarge.retroText(theme)
                )
                Text(
                    text = preset.sizeLabel,
                    color = if (selected) theme.accentStrong else theme.textMuted,
                    style = MaterialTheme.typography.labelLarge.retroText(theme)
                )
            }
            Text(
                text = buildString {
                    append(preset.description)
                    if (preset.showsNextPiecePreview) {
                        append(" · ${preset.piecesPerBatch} choices + next preview")
                    } else {
                        append(" · ${preset.piecesPerBatch}-piece hand")
                    }
                    if (active) append(" · Current run")
                },
                color = if (active) theme.success else theme.textMuted,
                style = MaterialTheme.typography.labelSmall.retroText(theme)
            )
        }
    }
}

@Composable
private fun GridLayoutPreview(
    boardSize: Int,
    selected: Boolean,
    blockedCells: Set<Cell> = emptySet(),
    modifier: Modifier = Modifier
) {
    val theme = LocalGridfallColors.current
    Canvas(modifier = modifier) {
        val gap = size.minDimension * 0.035f
        val cellSize = (size.minDimension - gap * (boardSize + 1)) / boardSize
        val cellColor = if (selected) theme.accentStrong else theme.emptyCellBorder
        drawRoundRect(
            color = theme.boardInner,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * 0.10f)
        )
        repeat(boardSize) { row ->
            repeat(boardSize) { col ->
                val blocked = Cell(row, col) in blockedCells
                val topLeft = Offset(
                    x = gap + col * (cellSize + gap),
                    y = gap + row * (cellSize + gap)
                )
                drawRoundRect(
                    color = if (blocked) androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.50f) else theme.emptyCell,
                    topLeft = topLeft,
                    size = androidx.compose.ui.geometry.Size(cellSize, cellSize),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cellSize * 0.16f)
                )
                drawRoundRect(
                    color = cellColor.copy(alpha = if (blocked) 0.16f else if (selected) 0.58f else 0.34f),
                    topLeft = topLeft,
                    size = androidx.compose.ui.geometry.Size(cellSize, cellSize),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cellSize * 0.16f),
                    style = Stroke(width = (cellSize * 0.08f).coerceAtLeast(0.5f))
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
