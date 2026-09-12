package com.example.gridfall.ui

import com.example.gridfall.game.GridLayoutPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class HighScoreStoreTest {
    @Test
    fun eachGridLayoutUsesADifferentHighScoreKey() {
        val keys = GridLayoutPreset.entries.map(HighScoreStore::keyFor)

        assertEquals(3, keys.toSet().size)
        assertNotEquals(
            HighScoreStore.keyFor(GridLayoutPreset.Classic),
            HighScoreStore.keyFor(GridLayoutPreset.Rush)
        )
    }

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
