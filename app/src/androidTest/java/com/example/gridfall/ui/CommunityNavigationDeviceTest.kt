package com.example.gridfall.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gridfall.auth.AuthPromptStore
import com.example.gridfall.game.*
import com.example.gridfall.network.*
import com.example.gridfall.ui.theme.GridfallTheme
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.delay
import org.junit.*
import org.junit.Assert.*

/** Real GameScreen navigation + synthetic gateway. Never authenticates or writes to production. */
class CommunityNavigationDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val code = "GF-"+UUID.randomUUID().toString().replace("-", "").take(10).uppercase()
    private val design = CustomMapDesign(5,4,emptySet(),listOf(MapBlockDefinition("one","One",setOf(Cell(0,0)),1000,2)))
    private val publication = CommunityPublication(code,"Fixture Author",CommunityMapDocument("Navigation fixture","Synthetic UI test only",design),true,"public")
    private val mounted = androidx.compose.runtime.mutableStateOf(true)
    private var previous: SavedInProgressRun? = null
    private var previousPrompt = false
    private var previousGrid = GridLayoutPreset.Classic
    @Before fun preserveLocalState() {
        previous=InProgressRunStore(context).load(); InProgressRunStore(context).clear()
        previousPrompt=AuthPromptStore.isSavePromptDismissed(context);AuthPromptStore.setSavePromptDismissed(context,true)
        previousGrid=GridLayoutPreferenceStore.load(context)
    }
    @After fun restoreLocalState() {
        compose.runOnIdle { mounted.value=false };compose.waitForIdle()
        previous?.let { InProgressRunStore(context).save(it) } ?: InProgressRunStore(context).clear()
        SavedCustomMapStore(context).delete("community_$code")
        AuthPromptStore.setSavePromptDismissed(context,previousPrompt)
        GridLayoutPreferenceStore.save(context,previousGrid)
    }
    private inner class Gateway : CommunityMapsGateway {
        var fail=false
        val ids=mutableListOf<String>()
        override suspend fun fetch(code: String)=publication
        override suspend fun browse(query:String,sort:String,cursor:String?,ownerToken:String?)=CommunityPage(listOf(publication),null)
        override suspend fun importMap(code:String,requestId:String,token:String?):CommunityPublication {
            ids.add(requestId);delay(250);if(fail)throw IOException("Offline fixture — retry");return publication
        }
        override suspend fun publish(token:String,document:String,visibility:String,requestId:String)=error("Not part of navigation test")
        override suspend fun rate(code:String,stars:Int,token:String)=error("No rating")
        override suspend fun unpublish(code:String,token:String)=error("No unpublish")
        override suspend fun report(code:String,reason:String,notes:String,token:String)=error("No report")
    }
    private fun open(gateway:Gateway,codeEntry:Boolean=false) {
        compose.setContent { if (mounted.value) GridfallTheme { GameScreen(communityGateway=gateway,autoCreateGuest=false) } }
        compose.onNodeWithText("Settings",ignoreCase=true).performClick()
        if(codeEntry) {
            compose.onNodeWithText("Enter Map Code").performScrollTo().performClick()
            compose.onNodeWithText("Map code").performTextInput(code)
            compose.onNodeWithText("Preview").performScrollTo().performClick()
        } else {
            compose.onNodeWithText("Community Maps").performScrollTo().performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Preview / Play").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Preview / Play").performScrollTo().performClick()
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Import & Play").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun assertGameplay() {
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("gameplay").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("gameplay").assertIsDisplayed()
        compose.onNodeWithTag("community-maps").assertDoesNotExist()
        compose.onNodeWithText("Return to Game").assertDoesNotExist()
        val board=compose.onNodeWithTag("gameplay-board").fetchSemanticsNode().size
        assertEquals(4f/5f,board.width.toFloat()/board.height,0.02f)
    }
    @Test fun browserImportWithNoProgressClosesSettingsAndPlays() {
        val gateway=Gateway();open(gateway)
        compose.onNodeWithText("Import & Play").performScrollTo().performClick();assertGameplay()
        assertEquals(design,SavedCustomMapStore(context).load().first { it.id=="community_$code" }.design)
        assertEquals(1,gateway.ids.size)
    }
    @Test fun confirmedImportAlsoClosesSettingsAndPlays() {
        InProgressRunStore(context).save(SavedInProgressRun(GameEngine.createCustomState(design).copy(score=25)))
        val gateway=Gateway();open(gateway)
        compose.onNodeWithText("Import & Play").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Start custom map?").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Start",substring=false).performClick();assertGameplay()
    }
    @Test fun failedImportStaysOpenAndRetryReusesIntentId() {
        val gateway=Gateway().also { it.fail=true };open(gateway)
        compose.onNodeWithText("Import & Play").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Offline fixture — retry").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Offline fixture — retry").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("community-maps").assertExists()
        gateway.fail=false
        compose.onNodeWithText("Import & Play").performScrollTo().performClick();assertGameplay()
        assertEquals(2,gateway.ids.size);assertEquals(gateway.ids[0],gateway.ids[1])
    }
    @Test fun codeImportRepeatedTapsStartOnlyOneRequest() {
        val gateway=Gateway();open(gateway,true)
        compose.onNodeWithText("Import & Play").performScrollTo().performTouchInput { click();click() }
        assertGameplay();assertEquals(1,gateway.ids.size)
    }
    @Test fun cancellingProgressResetPreservesThePreviousRun() {
        InProgressRunStore(context).save(SavedInProgressRun(GameEngine.createCustomState(design).copy(score=25)))
        val gateway=Gateway();open(gateway)
        compose.onNodeWithText("Import & Play").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Start custom map?").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Cancel",substring=false).performClick()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("gameplay").assertIsDisplayed()
        compose.onNodeWithText("25",substring=false).assertExists()
    }

}
