package com.example.gridfall.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridfall.game.LevelSystem
import com.example.gridfall.ui.theme.*
import java.util.Locale

@Composable
fun GameTopBar(
    score: Int,
    highScore: Int,
    level: Int,
    nextLevelScore: Int?,
    combo: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkGlass)
            .border(1.dp, Color(0xFF2B3A50), RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Score and Best
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "SCORE",
                        color = BlueGray,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp
                    )
                    Text(
                        text = String.format(Locale.US, "%,d", score),
                        color = IceWhite,
                        style = MaterialTheme.typography.displayMedium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "BEST",
                        color = BlueGray,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp
                    )
                    Text(
                        text = String.format(Locale.US, "%,d", highScore),
                        color = SoftIce,
                        style = MaterialTheme.typography.displaySmall
                    )
                }
            }

            // Level Display
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LEVEL $level",
                        color = LevelCyan,
                        style = MaterialTheme.typography.labelLarge
                    )

                    val progressText = if (nextLevelScore == null) {
                        "MAX"
                    } else {
                        val currentLevelMin = if (level > 1) LevelSystem.nextLevelScore(level - 1) ?: 0 else 0
                        val progress = (score - currentLevelMin).toFloat() / (nextLevelScore - currentLevelMin)
                        String.format(Locale.US, "%d%%", (progress * 100).toInt().coerceIn(0, 100))
                    }

                    Text(
                        text = progressText,
                        color = BlueGray,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Progress Bar
                val progress = if (nextLevelScore == null) {
                    1f
                } else {
                    val currentLevelMin = if (level > 1) LevelSystem.nextLevelScore(level - 1) ?: 0 else 0
                    ((score - currentLevelMin).toFloat() / (nextLevelScore - currentLevelMin)).coerceIn(0f, 1f)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(SlateButton)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(LevelCyan)
                    )
                }
            }

        }

        if (combo > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xE6261D0D))
                    .border(1.dp, ComboAmber.copy(alpha = 0.58f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "COMBO x$combo",
                    color = ComboAmber,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
