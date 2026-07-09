package com.example.gridfall.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiConfigTest {
    @Test
    fun normalizeBaseUrlTrimsWhitespaceAndTrailingSlash() {
        assertEquals(
            "https://api.example.test",
            ApiConfig.normalizeBaseUrl("  https://api.example.test/  ")
        )
    }

    @Test
    fun endpointJoinsConfiguredBaseUrlAndPath() {
        val path = "/leaderboard?limit=50"
        val expected = "${ApiConfig.baseUrl}$path"

        assertEquals(expected, ApiConfig.endpoint(path))
    }

    @Test
    fun endpointAddsLeadingSlashWhenMissing() {
        assertEquals(
            "${ApiConfig.baseUrl}/health",
            ApiConfig.endpoint("health")
        )
    }
}
