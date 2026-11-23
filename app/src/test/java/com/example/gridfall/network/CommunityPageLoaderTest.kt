package com.example.gridfall.network

import com.example.gridfall.game.*
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CommunityPageLoaderTest {
    private fun map(code: String, published: Boolean = true) = CommunityPublication(code, "Fixture",
        CommunityMapDocument(code, "", CustomMapDesign(4, 4, emptySet())), published, "public")
    @Test fun followsHistoryOnlyPagesWithoutInventingOrSkippingServerCursors() = runBlocking {
        val requested = mutableListOf<String?>()
        val page = loadVisibleCommunityPage(null, true) { cursor ->
            requested.add(cursor)
            when (cursor) {
                null -> CommunityPage(listOf(map("old", false)), "history")
                "history" -> CommunityPage(listOf(map("older", false)), "active")
                else -> CommunityPage(listOf(map("live")), "next-live")
            }
        }
        assertEquals(listOf(null, "history", "active"), requested)
        assertEquals(listOf("live"), page.items.map { it.code })
        assertEquals("next-live", page.nextCursor)
    }
    @Test fun mixedHistoryRetainsVisibleOrderAndOriginalNextCursor() = runBlocking {
        val page = loadVisibleCommunityPage("start", true) {
            CommunityPage(listOf(map("a"), map("old", false), map("b")), "server-cursor")
        }
        assertEquals(listOf("a", "b"), page.items.map { it.code })
        assertEquals("server-cursor", page.nextCursor)
    }
    @Test fun emptyMeansTheHistoryReallyReachedItsEnd() = runBlocking {
        var count = 0
        val page = loadVisibleCommunityPage(null, true) {
            count++; CommunityPage(listOf(map("old$count", false)), if (count < 3) "cursor$count" else null)
        }
        assertEquals(3, count); assertTrue(page.items.isEmpty()); assertNull(page.nextCursor)
    }
    @Test fun successfulLocalUnpublishCannotBeResurrectedByAnOlderResponse() = runBlocking {
        var count = 0
        val page = loadVisibleCommunityPage(null, true, setOf("removed")) {
            count++
            if (count == 1) CommunityPage(listOf(map("removed")), "next") else CommunityPage(listOf(map("kept")), null)
        }
        assertEquals(listOf("kept"), page.items.map { it.code }); assertEquals(2, count)
    }
    @Test fun cycleFailsInsteadOfSpinningOrReportingAnEmptyList() = runBlocking {
        val failure = runCatching {
            loadVisibleCommunityPage(null, true) { cursor ->
                CommunityPage(listOf(map("old", false)), if (cursor == "one") "two" else "one")
            }
        }.exceptionOrNull()
        assertTrue(failure is CommunityProtocolException)
    }
    @Test fun interruptedHistoryScanCheckpointsTheExactRetryCursor() = runBlocking {
        val progress = mutableListOf<String>()
        val failure = runCatching {
            loadVisibleCommunityPage(null, true, onSkippedPage = progress::add) { cursor ->
                when (cursor) {
                    null -> CommunityPage(listOf(map("old", false)), "one")
                    "one" -> CommunityPage(emptyList(), "two")
                    else -> throw IOException("Local fixture offline")
                }
            }
        }.exceptionOrNull()
        assertTrue(failure is IOException); assertEquals(listOf("one", "two"), progress)
        val retry = loadVisibleCommunityPage(progress.last(), true) { cursor ->
            assertEquals("two", cursor); CommunityPage(listOf(map("live")), null)
        }
        assertEquals("live", retry.items.single().code)
    }
}
