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
    val accent = when (feedback.tone) {
        FeedbackTone.Accent -> LevelCyan
        FeedbackTone.Success -> RewardMint
        FeedbackTone.Warning -> CoralWarning
        FeedbackTone.Bomb -> BombMagenta
    }
    val textColor = if (feedback.tone == FeedbackTone.Warning) CoralWarning else IceWhite

    Text(
        text = feedback.text,
        color = textColor,
        style = MaterialTheme.typography.titleSmall,
        modifier = modifier
            .background(ContractChipNavy.copy(alpha = 0.94f), RoundedCornerShape(999.dp))
            .border(1.dp, accent.copy(alpha = 0.70f), RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}
