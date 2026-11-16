package com.example.gridfall.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gridfall.game.*
import com.example.gridfall.network.*
import com.example.gridfall.ui.theme.GridfallTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class CommunityMapsDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val map = CommunityPublication("GF-A7K9P23456", "Creator", CommunityMapDocument("Small challenge", "Test map",
        CustomMapDesign(4, 4, setOf(Cell(0, 0)), listOf(MapBlockDefinition("single", "Single", setOf(Cell(0, 0)), 1000, 2)))), true, "public", 12, 4.2, 8)
    private inner class FakeGateway : CommunityMapsGateway {
        var offline = false
        var imports = 0
        var lastSort = ""
        override suspend fun fetch(code: String) = map
        override suspend fun publish(token: String, document: String, visibility: String, requestId: String) = map
        override suspend fun browse(query: String, sort: String, cursor: String?, ownerToken: String?): CommunityPage {
            if (offline) throw IOException("Offline test: check connection")
            lastSort = sort
            return CommunityPage(if (query == "missing") emptyList() else listOf(map), null)
        }
        override suspend fun importMap(code: String, requestId: String, token: String?): CommunityPublication { imports++; return map }
        override suspend fun rate(code: String, stars: Int, token: String) = map.copy(ratingAverage = stars.toDouble())
        override suspend fun unpublish(code: String, token: String) {}
        override suspend fun report(code: String, reason: String, notes: String, token: String) {}
    }
    @Test fun guestCanBrowseSortAndSeeActualMapDetailsAtLargeFont() {
        val gateway = FakeGateway()
        compose.setContent { GridfallTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                CommunityMapsScreen(gateway, false, { error("Guest must not request token") }, { _, _ -> }, {})
            }
        } }
        compose.onNodeWithTag("community-maps").performScrollToNode(hasText("Small challenge"))
        compose.onNodeWithText("Small challenge").assertIsDisplayed()
        compose.onNodeWithText("Most Downloaded").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("downloads", gateway.lastSort)
        compose.onNodeWithText("Preview / Play").performScrollTo().performClick()
        compose.onNodeWithText("By Creator · 4×4").assertExists()
        compose.onNodeWithText("Import & Play").performScrollTo().assertIsDisplayed()
    }
    @Test fun codeImportUsesIntentionalEndpointAndExistingOfflineStore() {
        val gateway = FakeGateway()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = SavedCustomMapStore(context)
        val id = "community_device_import"
        store.delete(id)
        compose.setContent { GridfallTheme {
            CommunityMapDialog(null, gateway, false, { error("Guest token") }, { publication, play ->
                assertTrue(play)
                store.save(SavedCustomMap(id, publication.document.name, publication.document.design))
            }, {}, initialCode = map.code)
        } }
        compose.onNodeWithText("Import & Play").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(1, gateway.imports)
        val saved = SavedCustomMapStore(context).load().first { it.id == id }
        assertEquals(map.document.design, saved.design)
        assertTrue(GameEngine.createCustomState(saved.design).board.isCustom)
        store.delete(id)
    }
    @Test fun offlineStateHasRetryAndRecoversWithoutAuth() {
        val gateway = FakeGateway().also { it.offline = true }
        compose.setContent { GridfallTheme { CommunityMapsScreen(gateway, false, { error("Guest token") }, { _, _ -> }, {}) } }
        compose.onNodeWithTag("community-maps").performScrollToNode(hasText("Retry"))
        compose.onNodeWithText("Retry").assertIsDisplayed()
        gateway.offline = false
        compose.onNodeWithText("Retry").performClick()
        compose.onNodeWithTag("community-maps").performScrollToNode(hasText("Small challenge"))
        compose.onNodeWithText("Small challenge").assertIsDisplayed()
    }
    @Test fun emptySearchAndGuestPublishingRestrictionsAreVisible() {
        val gateway = FakeGateway()
        compose.setContent { GridfallTheme { CommunityMapsScreen(gateway, false, { error("Guest token") }, { _, _ -> }, {}) } }
        compose.onNodeWithText("My Published Maps").assertIsNotEnabled()
        compose.onNodeWithText("Search maps or creators").performTextInput("missing")
        compose.onNodeWithText("Search", substring = false).performScrollTo().performClick()
        compose.onNodeWithTag("community-maps").performScrollToNode(hasText("No maps found. Try another search."))
        compose.onNodeWithText("No maps found. Try another search.").performScrollTo()
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        java.io.File(context.getExternalFilesDir(null), "community-search-empty.png").outputStream().use {
            screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        screenshot.recycle()
        // IME dismissal animates outside Compose's idle clock. Verify the final
        // visible state, allowing insets to settle rather than asserting mid-animation.
        compose.waitUntil(5_000) {
            runCatching {
                compose.onNodeWithText("No maps found. Try another search.").performScrollTo().assertIsDisplayed()
            }.isSuccess
        }
    }
}
