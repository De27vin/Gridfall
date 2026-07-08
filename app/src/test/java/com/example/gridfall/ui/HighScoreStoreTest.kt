package com.example.gridfall.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HighScoreStoreTest {
    @Test
    fun mergedBestScoreUsesBackendWhenLocalIsLower() {
        assertEquals(
            150,
            HighScoreStore.mergedBestScore(
                localBestScore = 0,
                backendBestScore = 150
            )
        )
    }

    @Test
    fun mergedBestScoreKeepsLocalWhenLocalIsHigher() {
        assertEquals(
            200,
            HighScoreStore.mergedBestScore(
                localBestScore = 200,
                backendBestScore = 150
            )
        )
    }
}
