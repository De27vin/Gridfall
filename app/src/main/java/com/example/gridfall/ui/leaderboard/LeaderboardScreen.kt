package com.example.gridfall.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.gridfall.network.AccountConnectionState
import com.example.gridfall.ui.infernoAppTexture
import com.example.gridfall.ui.infernoCorner
import com.example.gridfall.ui.infernoPanelTexture
import com.example.gridfall.ui.isInfernoTheme
import com.example.gridfall.ui.isRetroTheme
import com.example.gridfall.ui.retroAppTexture
import com.example.gridfall.ui.retroCorner
import com.example.gridfall.ui.retroPanelTexture
import com.example.gridfall.ui.retroText
import com.example.gridfall.ui.theme.LocalGridfallColors

@Composable
fun LeaderboardScreen(
    state: LeaderboardUiState,
    accountConnectionState: AccountConnectionState,
    pendingRunCount: Int,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalGridfallColors.current
    val panelShape = RoundedCornerShape(retroCorner(theme, infernoCorner(theme, 18.dp)))

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.backgroundTop, theme.backgroundBottom)))
            .infernoAppTexture(theme)
            .retroAppTexture(theme)
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (theme.isRetroTheme()) "LEADERBOARD" else "Leaderboard",
                color = theme.textPrimary,
                style = MaterialTheme.typography.headlineMedium.retroText(theme)
            )
            LeaderboardPageButton(label = "Back", onClick = onBack)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.darkGlass, panelShape)
                .infernoPanelTexture(theme)
                .retroPanelTexture(theme)
                .border(
                    width = if (theme.isRetroTheme() || theme.isInfernoTheme()) 2.dp else 1.dp,
                    color = theme.panelBorder.copy(alpha = if (theme.isRetroTheme() || theme.isInfernoTheme()) 0.84f else 0.48f),
                    shape = panelShape
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LeaderboardStatus(accountConnectionState, pendingRunCount)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                LeaderboardPageButton(label = "Refresh", onClick = onRefresh)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(theme.darkGlass, panelShape)
                .infernoPanelTexture(theme)
                .retroPanelTexture(theme)
                .border(
                    width = if (theme.isRetroTheme() || theme.isInfernoTheme()) 2.dp else 1.dp,
                    color = theme.panelBorder.copy(alpha = if (theme.isRetroTheme() || theme.isInfernoTheme()) 0.84f else 0.48f),
                    shape = panelShape
                )
                .padding(14.dp)
        ) {
            LeaderboardContent(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun LeaderboardPageButton(
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
