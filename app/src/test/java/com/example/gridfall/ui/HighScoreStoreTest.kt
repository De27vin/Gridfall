package com.example.gridfall.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HighScoreStoreTest {
    @Test
    fun accountBestScoreUsesTheBackendScore() {
        assertEquals(150, HighScoreStore.accountBestScore(150))
    }

    @Test
    fun accountBestScoreDoesNotKeepThePreviousAccountsScore() {
        assertEquals(0, HighScoreStore.accountBestScore(0))
    }

    @Test
    fun accountBestScoreDoesNotReturnNegativeValues() {
        assertEquals(0, HighScoreStore.accountBestScore(-1))
    }
}
