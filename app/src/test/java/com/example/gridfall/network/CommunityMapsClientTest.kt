package com.example.gridfall.network

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import java.io.File

class CommunityMapsClientTest {
    @Test fun codesAreCaseInsensitiveAndCannotInjectPaths() {
        assertEquals("GF-A7K9P23456", CommunityMapsClient.normalizeCode(" gf-a7k9p23456 "))
        listOf("../me", "GF-123", "GF-OOOOOOOOOO", "GF-A7K9P23456?x=1").forEach {
            assertTrue(runCatching { CommunityMapsClient.normalizeCode(it) }.isFailure)
        }
    }
    @Test fun parsesValidatedSnapshotWithoutAuthenticationOrPrivateIdentity() {
        val fixture = File("../shared/community-map-v1.json").readText()
        val map = CommunityMapsClient.parse(JSONObject().put("code", "GF-A7K9P23456").put("creator", "Player")
            .put("document", JSONObject(fixture)).put("visibility", "unlisted").put("published", true))
        assertEquals("Player", map.creator)
        assertTrue(map.document.design.blockPool.isNotEmpty())
        assertFalse(CommunityMapsClient.errorMessage(404).contains("HTTP"))
    }
}
