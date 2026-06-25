package com.example.gridfall.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gridfall.game.GameEngine

@Composable
fun GameScreen(modifier: Modifier = Modifier) {
    var gameState by remember { mutableStateOf(GameEngine.createInitialState()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF111827))
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        GameTopBar(
            score = gameState.score,
            combo = gameState.combo,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(28.dp))

        BoardCanvas(
            board = gameState.board,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(28.dp))

        PieceTray(
            pieces = gameState.currentPieces,
            usedPieceIndices = gameState.usedPieceIndices,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                gameState = GameEngine.createInitialState()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF38BDF8),
                contentColor = Color(0xFF082F49)
            )
        ) {
            androidx.compose.material3.Text(text = "Restart")
        }
    }
}
