package com.example.gridfall.network

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import okhttp3.Request
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class ApiHttpPolicyTest {
    @Test fun stagingDoesNotFollowAnEnvironmentRedirectWhileReleaseBehaviorIsPreserved() {
        val destinationHits = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/redirect") { e ->
            e.responseHeaders.add("Location", "/destination")
            e.sendResponseHeaders(302, -1); e.close()
        }
        server.createContext("/destination") { e ->
            destinationHits.incrementAndGet(); e.sendResponseHeaders(200, -1); e.close()
        }
        server.start()
        try {
            val request = Request.Builder().url("http://127.0.0.1:${server.address.port}/redirect").build()
            ApiHttpPolicy.builder("staging").build().newCall(request).execute().use { assertEquals(302, it.code) }
            assertEquals(0, destinationHits.get())
            ApiHttpPolicy.builder("release").build().newCall(request).execute().use { assertEquals(200, it.code) }
            assertEquals(1, destinationHits.get())
        } finally { server.stop(0) }
    }
}
