package com.example.gridfall.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gridfall.ui.theme.BombMagenta
import com.example.gridfall.ui.theme.ContractChipNavy
import com.example.gridfall.ui.theme.CoralWarning
import com.example.gridfall.ui.theme.IceWhite
import com.example.gridfall.ui.theme.LevelCyan
import com.example.gridfall.ui.theme.RewardMint

data class LineClearFeedback(
    val clearedRows: List<Int>,
    val clearedColumns: List<Int>,
    val token: Int
)

data class BombPulseFeedback(
    val centerRow: Int,
    val centerCol: Int,
    val isMega: Boolean = false,
    val token: Int
)

data class ScoreEventFeedback(
    val text: String,
    val tone: FeedbackTone,
    val token: Int
)

enum class FeedbackTone {
    Accent,
    Success,
    Warning,
    Bomb
}

@Composable
fun ScoreEventChip(
    feedback: ScoreEventFeedback,
    modifier: Modifier = Modifier
) {
    val theme = com.example.gridfall.ui.theme.LocalGridfallColors.current
    val accent = when (feedback.tone) {
        FeedbackTone.Accent -> theme.accent
        FeedbackTone.Success -> theme.success
        FeedbackTone.Warning -> theme.warning
        FeedbackTone.Bomb -> theme.bombOuter
    }
    val textColor = if (feedback.tone == FeedbackTone.Warning) theme.danger else theme.textPrimary
    val shape = RoundedCornerShape(
        when {
            theme.isRetroTheme() -> 6.dp
            theme.isInfernoTheme() -> 7.dp
            else -> 999.dp
        }
    )

    Text(
        text = feedback.text,
        color = textColor,
        style = MaterialTheme.typography.titleSmall.retroText(theme),
        modifier = modifier
            .background(theme.chipBackground.copy(alpha = 0.94f), shape)
            .infernoPanelTexture(theme)
            .retroPanelTexture(theme)
            .border(if (theme.isRetroTheme() || theme.isInfernoTheme()) 2.dp else 1.dp, accent.copy(alpha = 0.70f), shape)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}
