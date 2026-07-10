package com.example.gridfall.ui.leaderboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gridfall.network.AccountConnectionState
import com.example.gridfall.network.dto.LeaderboardEntryDto
import com.example.gridfall.ui.infernoCorner
import com.example.gridfall.ui.infernoPanelTexture
import com.example.gridfall.ui.isInfernoTheme
import com.example.gridfall.ui.isRetroTheme
import com.example.gridfall.ui.retroCorner
import com.example.gridfall.ui.retroPanelTexture
import com.example.gridfall.ui.retroText
import com.example.gridfall.ui.theme.LocalGridfallColors
import java.util.Locale

data class LeaderboardUiState(
    val isLoading: Boolean = false,
    val entries: List<LeaderboardEntryDto> = emptyList(),
    val error: String? = null
)

@Composable
fun LeaderboardDialog(
    state: LeaderboardUiState,
    accountConnectionState: AccountConnectionState,
    pendingRunCount: Int,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalGridfallColors.current
    val dialogShape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 24.dp)))

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .infernoPanelTexture(theme)
            .retroPanelTexture(theme),
        containerColor = theme.dialogBackground,
        shape = dialogShape,
        tonalElevation = 0.dp,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (theme.isRetroTheme()) "LEADERBOARD" else "Leaderboard",
                    color = theme.textPrimary,
                    style = MaterialTheme.typography.headlineSmall.retroText(theme)
                )
                if (accountConnectionState.isAnonymous) {
                    Text(
                        text = "Register and choose a username to appear on the leaderboard.",
                        color = theme.textSecondary,
                        style = MaterialTheme.typography.bodySmall.retroText(theme)
                    )
                }
                if (pendingRunCount > 0) {
                    Text(
                        text = "Some runs are still pending sync.",
                        color = theme.warning,
                        style = MaterialTheme.typography.bodySmall.retroText(theme)
                    )
                }
            }
        },
        text = {
            LeaderboardContent(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 380.dp)
            )
        },
        confirmButton = {
            LeaderboardButton(label = "Refresh", onClick = onRefresh)
        },
        dismissButton = {
            LeaderboardButton(label = "Close", onClick = onDismiss)
        }
    )
}

@Composable
fun LeaderboardContent(
    state: LeaderboardUiState,
    modifier: Modifier = Modifier
) {
    val theme = LocalGridfallColors.current

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when {
            state.isLoading -> CircularProgressIndicator(color = theme.accentStrong)
            state.error != null -> StateText(text = state.error, color = theme.warning)
            state.entries.isEmpty() -> StateText(text = "No leaderboard entries yet.", color = theme.textSecondary)
            else -> LeaderboardList(entries = state.entries)
        }
    }
}

@Composable
private fun LeaderboardList(entries: List<LeaderboardEntryDto>) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            LeaderboardHeaderRow()
        }
        items(entries, key = { entry -> entry.rank }) { entry ->
            LeaderboardRow(entry = entry)
        }
    }
}

@Composable
private fun LeaderboardHeaderRow() {
    val theme = LocalGridfallColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Rank",
            color = theme.textMuted,
            style = MaterialTheme.typography.labelSmall.retroText(theme),
            modifier = Modifier.weight(0.55f)
        )
        Text(
            text = "Username",
            color = theme.textMuted,
            style = MaterialTheme.typography.labelSmall.retroText(theme),
            modifier = Modifier.weight(1.60f)
        )
        Text(
            text = "Best score",
            color = theme.textMuted,
            style = MaterialTheme.typography.labelSmall.retroText(theme),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Best level",
            color = theme.textMuted,
            style = MaterialTheme.typography.labelSmall.retroText(theme),
            modifier = Modifier.weight(0.75f)
        )
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntryDto) {
    val theme = LocalGridfallColors.current
    val shape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 12.dp)))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.chipBackground.copy(alpha = 0.74f), shape)
            .infernoPanelTexture(theme)
            .retroPanelTexture(theme)
            .border(
                BorderStroke(
                    if (theme.isRetroTheme() || theme.isInfernoTheme()) 2.dp else 1.dp,
                    theme.panelBorder.copy(alpha = 0.50f)
                ),
                shape
            )
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#${entry.rank}",
            color = theme.accentStrong,
            style = MaterialTheme.typography.labelLarge.retroText(theme),
            modifier = Modifier.weight(0.55f)
        )
        Text(
            text = entry.username,
            color = theme.textPrimary,
            style = MaterialTheme.typography.bodyMedium.retroText(theme),
            modifier = Modifier.weight(1.60f)
        )
        Text(
            text = String.format(Locale.US, "%,d", entry.bestScore),
            color = theme.textSecondary,
            style = MaterialTheme.typography.labelLarge.retroText(theme),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Lv ${entry.bestLevel}",
            color = theme.textMuted,
            style = MaterialTheme.typography.labelMedium.retroText(theme),
            modifier = Modifier.weight(0.75f)
        )
    }
}

@Composable
private fun StateText(
    text: String,
    color: Color
) {
    val theme = LocalGridfallColors.current
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.bodyMedium.retroText(theme)
    )
}

@Composable
private fun LeaderboardButton(
    label: String,
    onClick: () -> Unit
) {
    val theme = LocalGridfallColors.current

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = theme.button,
            contentColor = theme.textPrimary
        ),
        shape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 14.dp)))
    ) {
        Text(
            text = if (theme.isRetroTheme()) label.uppercase() else label,
            style = MaterialTheme.typography.labelLarge.retroText(theme)
        )
    }
}
