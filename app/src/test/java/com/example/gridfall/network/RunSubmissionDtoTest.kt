package com.example.gridfall.network

import com.example.gridfall.network.dto.LeaderboardEntryDto
import com.example.gridfall.network.dto.LeaderboardResponse
import com.example.gridfall.network.dto.RunSubmissionRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class RunSubmissionDtoTest {
    @Test
    fun runSubmissionRequestPayloadContainsScoreLevelAndStats() {
        val request = RunSubmissionRequest(
            score = 1234,
            level = 7,
            linesCleared = 50,
            contractsCompleted = 4,
            bombsUsed = 3,
            megaBombsUsed = 1,
            durationSeconds = 480,
            appVersion = "1.0"
        )

        val payload = request.toPayloadMap()

        assertEquals(1234, payload["score"])
        assertEquals(7, payload["level"])
        assertEquals(50, payload["linesCleared"])
        assertEquals(4, payload["contractsCompleted"])
        assertEquals(3, payload["bombsUsed"])
        assertEquals(1, payload["megaBombsUsed"])
        assertEquals(480, payload["durationSeconds"])
        assertEquals("1.0", payload["appVersion"])
    }

    @Test
    fun leaderboardResponseKeepsEntryMapping() {
        val response = LeaderboardResponse(
            entries = listOf(
                LeaderboardEntryDto(
                    rank = 1,
                    username = "user-one",
                    bestScore = 12450,
                    bestLevel = 11
                )
            )
        )

        val entry = response.entries.single()

        assertEquals(1, entry.rank)
        assertEquals("user-one", entry.username)
        assertEquals(12450, entry.bestScore)
        assertEquals(11, entry.bestLevel)
    }
}
