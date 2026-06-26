package com.example.gridfall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun GameTopBar(
    score: Int,
    highScore: Int,
    level: Int,
    nextLevelScore: Int?,
    combo: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Gridfall",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium
            )

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Score $score",
                    color = Color(0xFFE5E7EB),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Best $highScore",
                    color = Color(0xFF9CA3AF),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Level $level",
                color = Color(0xFFBAE6FD),
                style = MaterialTheme.typography.labelLarge
            )

            Text(
                text = if (nextLevelScore == null) {
                    "Max Level"
                } else {
                    "Next level: $score / $nextLevelScore"
                },
                color = Color(0xFF9CA3AF),
                style = MaterialTheme.typography.labelLarge
            )
        }

        if (combo > 0) {
            Text(
                text = "Combo x$combo",
                color = Color(0xFF38BDF8),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
