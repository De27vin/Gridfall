package com.example.gridfall.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gridfall.game.SavedCustomMap
import com.example.gridfall.network.*
import com.example.gridfall.ui.theme.GridfallTheme
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.delay
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CommunityUnpublishDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private class HistoryGateway : CommunityUiGateway(communityUiFixture(1).let {
        it.copy(document = it.document.copy(name = "Target fixture"))
    }) {
        var published = true
        var failDelete = false
        var deleteCalls = 0
        var failHistoryOnce = false
        var historyFailed = false
        val requests = mutableListOf<Triple<String, String, Int>>()
        override suspend fun browse(query: String, sort: String, cursor: String?, ownerToken: String?): CommunityPage {
            if (ownerToken == null) return CommunityPage(emptyList(), null)
            val prefix = "$sort|$query|"
            check(cursor == null || cursor.startsWith(prefix)) { "Cursor crossed query/sort boundary" }
            val offset = cursor?.removePrefix(prefix)?.toInt() ?: 0
            requests.add(Triple(query, sort, offset))
            if (failHistoryOnce && offset == 2 && !historyFailed) {
                historyFailed = true; throw IOException("History retry fixture")
            }
            var rows = listOf(map.copy(published = published),
                map.copy(code = "GF-D7K9P23456", published = false, document = map.document.copy(name = "Old fixture")),
                map.copy(code = "GF-B7K9P23456", document = map.document.copy(name = "Second fixture")),
                map.copy(code = "GF-C7K9P23456", document = map.document.copy(name = "Third fixture")))
                .filter { query.isBlank() || it.document.name.contains(query, ignoreCase = true) }
            if (sort == "newest") rows = rows.reversed()
            return CommunityPage(rows.drop(offset).take(1), if (offset + 1 < rows.size) prefix + (offset + 1) else null)
        }
        override suspend fun unpublish(code: String, token: String) {
            assertEquals(map.code, code)
            deleteCalls++
            delay(250)
            if (failDelete) throw IOException("Unpublish rejected fixture")
            published = false
        }
    }
    private fun open(gateway: HistoryGateway, restore: StateRestorationTester) {
        restore.setContent { GridfallTheme { CommunityMapsTheme {
            CommunityMapsScreen(gateway, true, { "local-owner-fixture" }, { _, _ -> }, {})
        } } }
        compose.waitUntil(5_000) { runCatching { compose.onNodeWithText("My Published Maps").assertIsEnabled() }.isSuccess }
        compose.onNodeWithText("My Published Maps").performScrollTo().performClick()
        settled()
    }
    private fun settled() {
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("community-loading").fetchSemanticsNodes().isEmpty() }
        compose.waitForIdle()
    }
    private fun see(text: String) {
        compose.onNodeWithTag("community-maps").performScrollToNode(hasText(text))
        compose.onNodeWithText(text).assertIsDisplayed()
    }
    private fun confirm(gateway: HistoryGateway) {
        compose.onNodeWithTag("community-unpublish-${gateway.map.code}").performScrollTo().performClick()
        compose.onNodeWithTag("community-confirm-unpublish").performTouchInput { click(); click() }
    }
    @Test fun successfulUnpublishSurvivesRefreshAndRecreationWithoutDeletingLocalMap() {
        val gateway = HistoryGateway(); val restore = StateRestorationTester(compose)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = SavedCustomMapStore(context)
        val saved = SavedCustomMap("unpublish-local-${UUID.randomUUID()}", gateway.map.document.name, gateway.map.document.design)
        store.save(saved)
        try {
            open(gateway, restore); see("Target fixture"); confirm(gateway)
            compose.waitUntil(5_000) { !gateway.published && compose.onAllNodesWithTag("community-confirm-unpublish").fetchSemanticsNodes().isEmpty() }
            settled(); see("Second fixture")
            compose.onNodeWithText("Target fixture").assertDoesNotExist()
            assertEquals(1, gateway.deleteCalls)
            assertEquals(saved, store.load().first { it.id == saved.id })
            compose.onNodeWithText("Refresh").performScrollTo().performClick(); settled(); see("Second fixture")
            compose.onNodeWithText("Target fixture").assertDoesNotExist()
            restore.emulateSavedInstanceStateRestore(); settled(); see("Second fixture")
            compose.onNodeWithText("Target fixture").assertDoesNotExist()
            compose.onNodeWithText("Load More").performScrollTo().performClick(); settled(); see("Third fixture")
            assertEquals(saved, store.load().first { it.id == saved.id })
        } finally { store.delete(saved.id) }
    }
    @Test fun rejectedUnpublishRetainsTheMapAndCoalescesRepeatedTaps() {
        val gateway = HistoryGateway().also { it.failDelete = true }; val restore = StateRestorationTester(compose)
        open(gateway, restore); see("Target fixture"); confirm(gateway)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("community-unpublish-error").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, gateway.deleteCalls); assertTrue(gateway.published)
        compose.onNodeWithText("Cancel").performClick(); see("Target fixture")
        compose.onNodeWithText("Refresh").performScrollTo().performClick(); settled(); see("Target fixture")
        restore.emulateSavedInstanceStateRestore(); settled(); see("Target fixture")
    }
    @Test fun activeFilteringWorksWithHistorySearchSortAndPagination() {
        val gateway = HistoryGateway().also { it.published = false }; val restore = StateRestorationTester(compose)
        open(gateway, restore); see("Second fixture")
        assertEquals(listOf(0, 1, 2), gateway.requests.map { it.third })
        compose.onNodeWithText("Load More").performScrollTo().performClick(); settled(); see("Third fixture")
        compose.onNodeWithText("Target fixture").assertDoesNotExist()
        compose.onNodeWithText("Search maps or creators").performTextInput("Target")
        compose.onNodeWithText("Search", substring = false).performScrollTo().performClick(); settled()
        see("No published maps match this search.")
        compose.onNodeWithText("Search maps or creators").performTextClearance()
        compose.onNodeWithText("Search maps or creators").performTextInput("Second")
        compose.onNodeWithText("Search", substring = false).performScrollTo().performClick(); settled(); see("Second fixture")
        compose.onNodeWithText("Newest").performScrollTo().performClick(); settled(); see("Second fixture")
        assertEquals(Triple("Second", "newest", 0), gateway.requests.last())
        compose.onNodeWithText("Load More").assertDoesNotExist()
    }
    @Test fun interruptedHistoryScanRetriesFromItsCheckpointWithoutFalseEmptyState() {
        val gateway = HistoryGateway().also { it.published = false; it.failHistoryOnce = true }
        open(gateway, StateRestorationTester(compose)); see("History retry fixture")
        compose.onNodeWithText("You have no published maps. Share one from Saved Maps.").assertDoesNotExist()
        compose.onNodeWithText("Retry").performScrollTo().performClick(); settled(); see("Second fixture")
        assertEquals(listOf(0, 1, 2, 2), gateway.requests.map { it.third })
    }
}
