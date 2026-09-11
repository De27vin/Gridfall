package com.example.gridfall.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GridfallApiClientTest {
    private val legacyResponse = """
        {
          "me": {
            "username": "Player",
            "bestScore": 9207,
            "bestLevel": 14,
            "totalPoints": 61811,
            "gamesPlayed": 20,
            "totalLinesCleared": 2762,
            "totalContractsCompleted": 46,
            "totalRiskSpinsUsed": 15
          },
          "leaderboards": {
            "bestScore": {
              "entries": [{"rank": 1, "username": "Player", "bestScore": 9207}],
              "me": null
            }
          }
        }
    """.trimIndent()

    @Test
    fun `legacy global leaderboard is hidden for non-classic grids`() {
        val response = GridfallApiClient().parseLeaderboardsResponse(
            rawJson = legacyResponse,
            requestedBoardSize = 7
        )

        assertEquals(0, response.me?.gamesPlayed)
        assertEquals(0, response.me?.bestScore)
        assertEquals("Player", response.me?.username)
        assertEquals(emptyList<Any>(), response.leaderboards.bestScore.entries)
        assertNull(response.leaderboards.bestScore.me)
    }

    @Test
    fun `legacy global leaderboard remains available for classic grid`() {
        val response = GridfallApiClient().parseLeaderboardsResponse(
            rawJson = legacyResponse,
            requestedBoardSize = 8
        )

        assertEquals(9207, response.me?.bestScore)
        assertEquals(1, response.leaderboards.bestScore.entries.size)
    }
}
