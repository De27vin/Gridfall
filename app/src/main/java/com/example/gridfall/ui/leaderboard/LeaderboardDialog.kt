package com.example.gridfall.ui.leaderboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gridfall.network.AccountConnectionState
import com.example.gridfall.network.dto.LeaderboardEntryDto
import com.example.gridfall.network.dto.LeaderboardSectionDto
import com.example.gridfall.network.dto.LeaderboardSectionsDto
import com.example.gridfall.network.dto.LeaderboardsResponse
import com.example.gridfall.network.dto.LeaderboardType
import com.example.gridfall.network.dto.YourStatsDto
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
    val response: LeaderboardsResponse? = null,
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
        modifier = Modifier.infernoPanelTexture(theme).retroPanelTexture(theme),
        containerColor = theme.dialogBackground,
        shape = dialogShape,
        tonalElevation = 0.dp,
        title = {
            Text(
                text = if (theme.isRetroTheme()) "LEADERBOARD" else "Leaderboard",
                color = theme.textPrimary,
                style = MaterialTheme.typography.headlineSmall.retroText(theme)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LeaderboardStatus(accountConnectionState, pendingRunCount)
                LeaderboardContent(
                    state = state,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 440.dp)
                )
            }
        },
        confirmButton = { LeaderboardButton("Refresh", onRefresh) },
        dismissButton = { LeaderboardButton("Close", onDismiss) }
    )
}

@Composable
fun LeaderboardStatus(account: AccountConnectionState, pendingRunCount: Int) {
    val theme = LocalGridfallColors.current
    if (account.isAnonymous) {
        Text(
            text = "Register and choose a username to appear on public leaderboards.",
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

@Composable
fun LeaderboardContent(state: LeaderboardUiState, modifier: Modifier = Modifier) {
    val theme = LocalGridfallColors.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            state.isLoading -> CircularProgressIndicator(color = theme.accentStrong)
            state.error != null -> StateText(state.error, theme.warning)
            state.response == null -> StateText("Leaderboard is unavailable.", theme.textSecondary)
            else -> LeaderboardData(state.response, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun LeaderboardData(response: LeaderboardsResponse, modifier: Modifier) {
    var selectedType by remember { mutableStateOf(LeaderboardType.BestScore) }
    val theme = LocalGridfallColors.current
    val section = response.leaderboards.sectionFor(selectedType)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        YourStatsCard(response.me)
        if (response.me?.username == null) {
            Text(
                text = "Choose a username to appear on public leaderboards.",
                color = theme.textSecondary,
                style = MaterialTheme.typography.bodySmall.retroText(theme)
            )
        }
        TabRow(
            selectedTabIndex = LeaderboardType.entries.indexOf(selectedType),
            containerColor = theme.chipBackground.copy(alpha = 0.70f),
            contentColor = theme.accentStrong
        ) {
            LeaderboardType.entries.forEach { type ->
                Tab(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    text = {
                        Text(
                            text = type.tabLabel(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall.retroText(theme)
                        )
                    }
                )
            }
        }
        LeaderboardRows(section, selectedType, Modifier.weight(1f))
    }
}

@Composable
private fun YourStatsCard(stats: YourStatsDto?) {
    val theme = LocalGridfallColors.current
    val shape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 12.dp)))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.chipBackground.copy(alpha = 0.80f), shape)
            .infernoPanelTexture(theme)
            .retroPanelTexture(theme)
            .border(BorderStroke(if (theme.isRetroTheme() || theme.isInfernoTheme()) 2.dp else 1.dp, theme.panelBorder.copy(alpha = 0.58f)), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text("Your Stats", color = theme.textPrimary, style = MaterialTheme.typography.titleMedium.retroText(theme))
        if (stats == null) {
            Text("Sign in to load your current stats.", color = theme.textSecondary, style = MaterialTheme.typography.bodySmall.retroText(theme))
        } else {
            Text("Best Score: ${formatNumber(stats.bestScore)}   Lv ${stats.bestLevel}", color = theme.accentStrong, style = MaterialTheme.typography.bodyMedium.retroText(theme))
            StatsLine("Total Points", formatNumber(stats.totalPoints))
            StatsLine("Games Played", formatNumber(stats.gamesPlayed))
            StatsLine("Lines Cleared", formatNumber(stats.totalLinesCleared))
            StatsLine("Contracts Completed", formatNumber(stats.totalContractsCompleted))
        }
    }
}

@Composable
private fun StatsLine(label: String, value: String) {
    val theme = LocalGridfallColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = theme.textSecondary, style = MaterialTheme.typography.bodySmall.retroText(theme))
        Text(value, color = theme.textPrimary, style = MaterialTheme.typography.bodySmall.retroText(theme))
    }
}

@Composable
private fun LeaderboardRows(section: LeaderboardSectionDto, type: LeaderboardType, modifier: Modifier) {
    val theme = LocalGridfallColors.current
    if (section.entries.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            StateText("No public entries yet.", theme.textSecondary)
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        items(section.entries, key = { entry -> "top-${entry.rank}-${entry.username}" }) { entry ->
            LeaderboardRow(entry, type)
        }
        if (section.me != null && !section.me.isInTop) {
            item { Text("Your Rank", color = theme.textMuted, style = MaterialTheme.typography.labelMedium.retroText(theme), modifier = Modifier.padding(top = 7.dp)) }
            item { LeaderboardRow(section.me, type, isPinned = true) }
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntryDto, type: LeaderboardType, isPinned: Boolean = false) {
    val theme = LocalGridfallColors.current
    val shape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 10.dp)))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background((if (isPinned) theme.accent else theme.chipBackground).copy(alpha = if (isPinned) 0.20f else 0.72f), shape)
            .border(BorderStroke(if (theme.isRetroTheme() || theme.isInfernoTheme()) 2.dp else 1.dp, (if (isPinned) theme.accentStrong else theme.panelBorder).copy(alpha = 0.62f)), shape)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("#${entry.rank}", color = theme.accentStrong, style = MaterialTheme.typography.labelLarge.retroText(theme), modifier = Modifier.weight(0.55f))
        Text(entry.username, color = theme.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium.retroText(theme), modifier = Modifier.weight(1.25f))
        Text(entryValue(entry, type), color = theme.textSecondary, maxLines = 1, style = MaterialTheme.typography.labelMedium.retroText(theme), modifier = Modifier.weight(1.20f))
    }
}

private fun LeaderboardSectionsDto.sectionFor(type: LeaderboardType): LeaderboardSectionDto = when (type) {
    LeaderboardType.BestScore -> bestScore
    LeaderboardType.TotalPoints -> totalPoints
    LeaderboardType.LinesCleared -> linesCleared
    LeaderboardType.ContractsCompleted -> contractsCompleted
}

private fun LeaderboardType.tabLabel(): String = when (this) {
    LeaderboardType.BestScore -> "Score"
    LeaderboardType.TotalPoints -> "Points"
    LeaderboardType.LinesCleared -> "Lines"
    LeaderboardType.ContractsCompleted -> "Contracts"
}

private fun entryValue(entry: LeaderboardEntryDto, type: LeaderboardType): String = when (type) {
    LeaderboardType.BestScore -> "${formatNumber(entry.bestScore)}  Lv ${entry.bestLevel}"
    LeaderboardType.TotalPoints -> "${formatNumber(entry.totalPoints)} pts"
    LeaderboardType.LinesCleared -> "${formatNumber(entry.totalLinesCleared)} lines"
    LeaderboardType.ContractsCompleted -> "${formatNumber(entry.totalContractsCompleted)} contracts"
}

private fun formatNumber(value: Int): String = String.format(Locale.US, "%,d", value)

@Composable
private fun StateText(text: String, color: Color) {
    val theme = LocalGridfallColors.current
    Text(text, color = color, style = MaterialTheme.typography.bodyMedium.retroText(theme))
}

@Composable
private fun LeaderboardButton(label: String, onClick: () -> Unit) {
    val theme = LocalGridfallColors.current
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = theme.button, contentColor = theme.textPrimary),
        shape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 14.dp)))
    ) {
        Text(if (theme.isRetroTheme()) label.uppercase() else label, style = MaterialTheme.typography.labelLarge.retroText(theme))
    }
}
