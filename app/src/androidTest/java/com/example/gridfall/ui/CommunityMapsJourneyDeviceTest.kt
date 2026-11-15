package com.example.gridfall.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gridfall.game.*
import com.example.gridfall.network.CommunityApiException
import com.example.gridfall.network.CommunityMapsClient
import com.example.gridfall.sync.RunSubmissionPolicy
import com.example.gridfall.ui.theme.GridfallTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.util.UUID

/** Opt-in cross-stack journey using the strictly isolated API harness, never production. */
class CommunityMapsJourneyDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun createPublishCopyImportPlayRateDiscoverAndUnpublish(): Unit = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("communityTestServer") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = SavedCustomMapStore(context)
        val name = "Journey${UUID.randomUUID().toString().take(8)}"
        val design = CustomMapDesign(4, 5, setOf(Cell(0, 0), Cell(3, 4)), listOf(
            MapBlockDefinition("diagonal", "Diagonal", setOf(Cell(0, 0), Cell(1, 1)), 123, 4),
            MapBlockDefinition("single", "Single", setOf(Cell(0, 0)), 877, 2)))
        val saved = store.create(name, design).copy(description = "Device journey")
        store.save(saved)
        val client = CommunityMapsClient(endpoint = { "http://127.0.0.1:55440$it" })
        var stage by mutableStateOf(0)
        var code by mutableStateOf("")
        var imported: SavedCustomMap? = null
        compose.setContent { GridfallTheme { CommunityMapsTheme {
            key(stage) {
                when(stage) {
                    0 -> CommunityMapDialog(saved, client, true, { "device-owner" }, { _, _ -> }, { stage = 1 })
                    1 -> CommunityMapDialog(null, client, true, { "device-player" }, { publication, play ->
                        assertTrue(play)
                        imported = store.importCommunityMap(publication.code, publication.document)
                        stage = 2
                    }, {}, initialCode = code)
                    else -> Text("Imported locally")
                }
            }
        } } }
        // Publish publicly through the actual UI/API, then copy its generated code.
        compose.onNodeWithText("Public · marketplace").performScrollTo().performClick()
        compose.onNodeWithText("Publish Map").performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Copy Code").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Copy Code").performScrollTo().performClick()
        compose.runOnIdle {
            code = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip!!.getItemAt(0).text.toString()
            assertTrue(code.startsWith("GF-"))
        }
        compose.onNodeWithText("Close").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Import & Play").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Import & Play").performScrollTo().performClick()
        compose.waitUntil(15_000) { imported != null }
        val local = store.load().first { it.id == imported!!.id }
        assertEquals(design, local.design)
        assertEquals("Device journey", local.description)
        val state = GameEngine.createCustomState(local.design).copy(score = 5000, isGameOver = true)
        assertFalse(RunSubmissionPolicy.shouldSubmitGameOverRun(state))
        val placement = local.design.blockPool.last().toPiece()
        assertTrue(GameEngine.canPlace(state.board, placement, 1, 1))
        assertEquals(5.0, client.rate(code, 5, "device-player").ratingAverage, 0.0)
        assertTrue(client.browse(name, "newest", null).items.any { it.code == code })
        client.unpublish(code, "device-owner")
        val lookup = runCatching { client.fetch(code) }.exceptionOrNull()
        assertEquals(404, (lookup as CommunityApiException).status)
        val importError = runCatching { client.importMap(code, UUID.randomUUID().toString(), null) }.exceptionOrNull()
        assertEquals(404, (importError as CommunityApiException).status)
        assertEquals(local.design, store.load().first { it.id == local.id }.design)
        store.delete(saved.id); store.delete(local.id)
        Unit
    }
}
