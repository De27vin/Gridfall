package com.example.gridfall.network

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** Loopback-only protocol tests: synthetic tokens and responses, never production. */
class CommunityMapsHttpContractTest {
    private fun map(code: String = "GF-A7K9P23456") = JSONObject().put("code", code).put("creator", "Fixture")
        .put("document", JSONObject(File("../shared/community-map-v1.json").readText()))
        .put("visibility", "public").put("published", true).put("downloads", 4).put("ratingAverage", 4.5).put("ratingCount", 2)
    private fun server(block: (HttpServer, CommunityMapsClient) -> Unit) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        try { server.start(); block(server, CommunityMapsClient(endpoint = { "http://127.0.0.1:${server.address.port}$it" })) }
        finally { server.stop(0) }
    }
    @Test fun cursorSearchSortAndAuthorizationMatchBackendContract() = server { server, client -> runBlocking {
        val calls = mutableListOf<Pair<String, String?>>()
        server.createContext("/community/maps") { e ->
            calls.add(e.requestURI.toString() to e.requestHeaders.getFirst("Authorization"))
            val response = JSONObject().put("items", JSONArray().put(map()))
                .put("nextCursor", if (calls.size == 1) "opaque+/=" else JSONObject.NULL).toString().toByteArray()
            e.sendResponseHeaders(200, response.size.toLong()); e.responseBody.use { it.write(response) }
        }
        assertEquals("opaque+/=", client.browse("café & neon", "popular", null).nextCursor)
        assertNull(client.browse("café & neon", "popular", "opaque+/=", "fresh-fixture-token").nextCursor)
        assertNull(calls[0].second)
        assertEquals("Bearer fresh-fixture-token", calls[1].second)
        assertTrue(calls[1].first.startsWith("/community/maps/mine?"))
        val decoded = URLDecoder.decode(calls[1].first, "UTF-8")
        assertTrue(decoded.contains("q=café & neon")); assertTrue(decoded.contains("sort=popular"))
        assertTrue(decoded.contains("limit=10")); assertTrue(decoded.contains("cursor=opaque+/="))
    } }
    @Test fun malformedPageIsAnErrorNotAnEmptyMarketplace() {
        for (bad in listOf(JSONObject().put("items", "wrong").put("nextCursor", JSONObject.NULL),
            JSONObject().put("items", JSONArray()).put("nextCursor", 4), JSONObject().put("items", JSONArray()))) {
            val error = runCatching { CommunityMapsClient.parsePage(bad) }.exceptionOrNull()
            assertTrue(error is CommunityProtocolException); assertTrue(error!!.message!!.contains("invalid response"))
        }
        assertTrue(CommunityMapsClient.parsePage(JSONObject().put("items", JSONArray()).put("nextCursor", JSONObject.NULL)).items.isEmpty())
    }
    @Test fun rejectsInvalidMapMetadataRatherThanCorruptingLocalMaps() {
        for (bad in listOf(map().put("downloads", -1), map().put("ratingAverage", 6), map().put("ratingCount", "many"), map().put("code", "../me"))) {
            assertTrue(runCatching { CommunityMapsClient.parse(bad) }.exceptionOrNull() is CommunityProtocolException)
        }
    }
    @Test fun apiPermissionAndEdgeDenialHaveDifferentMessages() = server { server, client -> runBlocking {
        var edge = false
        server.createContext("/community/maps") { e ->
            val response = (if (edge) "error code: 1010" else "{\"error\":\"Creators cannot rate their own maps\"}").toByteArray()
            e.sendResponseHeaders(403, response.size.toLong()); e.responseBody.use { it.write(response) }
        }
        val api = runCatching { client.rate("GF-A7K9P23456", 5, "fixture") }.exceptionOrNull() as CommunityApiException
        assertEquals("Creators cannot rate their own maps", api.message)
        edge = true
        val blocked = runCatching { client.browse("", "newest", null) }.exceptionOrNull() as CommunityApiException
        assertTrue(blocked.message!!.contains("blocked before")); assertFalse(blocked.message!!.contains("Sign in"))
    } }
    @Test fun unauthorizedRetryCanSendFreshTokenWithoutChangingRequestId() = server { server, client -> runBlocking {
        val received = mutableListOf<Pair<String?, String>>()
        server.createContext("/community/maps") { e ->
            val token = e.requestHeaders.getFirst("Authorization")
            received.add(token to JSONObject(e.requestBody.reader().readText()).getString("requestId"))
            val status = if (token == "Bearer expired-fixture") 401 else 200
            val response = (if (status == 200) map().toString() else "{\"error\":\"Invalid Firebase token\"}").toByteArray()
            e.sendResponseHeaders(status, response.size.toLong()); e.responseBody.use { it.write(response) }
        }
        val id = UUID.randomUUID().toString()
        assertEquals(401, (runCatching { client.importMap("GF-A7K9P23456", id, "expired-fixture") }.exceptionOrNull() as CommunityApiException).status)
        client.importMap("GF-A7K9P23456", id, "fresh-fixture")
        assertEquals(listOf(id, id), received.map { it.second })
        assertEquals(listOf("Bearer expired-fixture", "Bearer fresh-fixture"), received.map { it.first })
    } }
    @Test fun transientServerFailureRetriesAndMalformedSuccessDoesNotLookOffline() = server { server, client -> runBlocking {
        var count = 0
        server.createContext("/community/maps") { e ->
            count++
            val status = if (count == 1) 503 else 200
            val response = (if (count == 1) "{}" else if (count == 2) "not JSON" else "{\"items\":[],\"nextCursor\":null}").toByteArray()
            e.sendResponseHeaders(status, response.size.toLong()); e.responseBody.use { it.write(response) }
        }
        assertEquals(503, (runCatching { client.browse("", "rated", null) }.exceptionOrNull() as CommunityApiException).status)
        assertTrue(runCatching { client.browse("", "rated", null) }.exceptionOrNull() is CommunityProtocolException)
        assertTrue(client.browse("", "rated", null).items.isEmpty())
    } }
    @Test fun repeatedCursorCannotCreateEndlessPagination() = server { server, client -> runBlocking {
        server.createContext("/community/maps") { e ->
            val response = "{\"items\":[],\"nextCursor\":\"same\"}".toByteArray()
            e.sendResponseHeaders(200, response.size.toLong()); e.responseBody.use { it.write(response) }
        }
        assertTrue(runCatching { client.browse("", "newest", "same") }.exceptionOrNull() is CommunityProtocolException)
    } }
}
