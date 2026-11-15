package com.example.gridfall.network

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import java.io.File
import kotlinx.coroutines.runBlocking
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.UUID

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
    @Test fun previewsAreReadOnlyAndIntentionalImportsCarryStableRetryIds() = runBlocking {
        val requests = java.util.Collections.synchronizedList(mutableListOf<Pair<String, String>>())
        val fixture = File("../shared/community-map-v1.json").readText()
        val response = JSONObject().put("code", "GF-A7K9P23456").put("creator", "Player").put("document", JSONObject(fixture))
            .put("visibility", "public").put("published", true).put("downloads", 2).put("ratingAverage", 4.5).put("ratingCount", 2).toString().toByteArray()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/community/maps") { exchange ->
            requests.add(exchange.requestMethod to exchange.requestBody.reader().readText())
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        server.start()
        try {
            val client = CommunityMapsClient(endpoint = { "http://127.0.0.1:${server.address.port}$it" })
            assertEquals(4.5, client.fetch("gf-a7k9p23456").ratingAverage, 0.0)
            client.fetch("GF-A7K9P23456")
            val retry = UUID.randomUUID().toString()
            client.importMap("GF-A7K9P23456", retry, null)
            client.importMap("GF-A7K9P23456", retry, null)
            assertEquals(listOf("GET", "GET", "POST", "POST"), requests.map { it.first })
            requests.drop(2).forEach { assertEquals(retry, JSONObject(it.second).getString("requestId")) }
        } finally { server.stop(0) }
    }
}
