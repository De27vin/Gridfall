package com.example.gridfall.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gridfall.network.*
import com.example.gridfall.ui.theme.GridfallTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.util.UUID

/** Explicit opt-in REAL production GETs only. No token, Firebase login, imports or mutations. */
class CommunityProductionReadOnlyDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun client(): CommunityMapsClient {
        assumeTrue(InstrumentationRegistry.getArguments().getString("productionReadOnly") == "true")
        assertEquals("https://api.gridfall.site", ApiConfig.baseUrl)
        return CommunityMapsClient()
    }
    @Test fun realPublicSortSearchAndErrorContract(): Unit = runBlocking {
        val client=client()
        for(sort in listOf("rated","popular","downloads","newest")) {
            val page=client.browse("",sort,null)
            assertTrue(page.items.size<=10)
        }
        assertTrue(client.browse("qa${UUID.randomUUID().toString().replace("-", "")}","newest",null).items.isEmpty())
        assertEquals(400,(runCatching { client.browse("","invalid-sort",null) }.exceptionOrNull() as CommunityApiException).status)
        assertEquals(400,(runCatching { client.browse("","rated","not-a-cursor") }.exceptionOrNull() as CommunityApiException).status)
        val code="GF-"+UUID.randomUUID().toString().replace("-", "").take(10).uppercase()
        assertEquals(404,(runCatching { client.fetch(code) }.exceptionOrNull() as CommunityApiException).status)
    }
    @Test fun realGuestBrowserCanSearchSortAndRenderEmptySearchResult() {
        val client=client()
        compose.setContent { GridfallTheme { CommunityMapsTheme {
            CommunityMapsScreen(client,false,{ error("Guest browsing must never authenticate") },{_,_->error("No imports authorized in read-only test")},{})
        } } }
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("community-loading").fetchSemanticsNodes().isEmpty() &&
            compose.onAllNodesWithText("Retry").fetchSemanticsNodes().isEmpty() &&
            (compose.onAllNodesWithText("No maps found. Try another search.").fetchSemanticsNodes().isNotEmpty() ||
             compose.onAllNodesWithText("Preview / Play").fetchSemanticsNodes().isNotEmpty()) }
        compose.onNodeWithText("My Published Maps").assertIsNotEnabled()
        compose.onNodeWithText("Search maps or creators").performTextInput("qa${UUID.randomUUID().toString().take(8)}")
        compose.onNodeWithText("Search",substring=false).performScrollTo().performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("community-loading").fetchSemanticsNodes().isEmpty() &&
            compose.onAllNodesWithText("No maps found. Try another search.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Newest").performScrollTo().performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("community-loading").fetchSemanticsNodes().isEmpty() &&
            compose.onAllNodesWithText("No maps found. Try another search.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("No maps found. Try another search.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Retry").assertDoesNotExist()
    }
}
