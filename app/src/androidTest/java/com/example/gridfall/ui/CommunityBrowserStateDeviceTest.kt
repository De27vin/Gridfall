package com.example.gridfall.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.example.gridfall.game.*
import com.example.gridfall.network.*
import com.example.gridfall.ui.theme.GridfallTheme
import java.io.IOException
import kotlinx.coroutines.delay
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CommunityBrowserStateDeviceTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val first=CommunityPublication("GF-A7K9P23456","Fixture",CommunityMapDocument("Fixture first","",CustomMapDesign(4,4,emptySet())),true,"public")
    private val second=first.copy(code="GF-B7K9P23456",document=first.document.copy(name="Fixture second"))
    private inner class Gateway:CommunityMapsGateway {
        var attempts=0
        val cursors=mutableListOf<String?>()
        var lastQuery="";var lastSort=""
        override suspend fun browse(query:String,sort:String,cursor:String?,ownerToken:String?):CommunityPage {
            lastQuery=query;lastSort=sort;cursors.add(cursor)
            if(cursor==null)return CommunityPage(listOf(first),"next")
            attempts++;delay(300);if(attempts==1)throw IOException("Temporary page fixture error")
            return CommunityPage(listOf(second),null)
        }
        override suspend fun fetch(code:String)=first
        override suspend fun publish(token:String,document:String,visibility:String,requestId:String)=error("Not used")
        override suspend fun importMap(code:String,requestId:String,token:String?)=error("Not used")
        override suspend fun rate(code:String,stars:Int,token:String)=error("Not used")
        override suspend fun unpublish(code:String,token:String)=error("Not used")
        override suspend fun report(code:String,reason:String,notes:String,token:String)=error("Not used")
    }
    @Test fun repeatedPageTapsAreCoalescedAndFailureRetryKeepsCursor() {
        val gateway=Gateway()
        compose.setContent { GridfallTheme { CommunityMapsTheme { CommunityMapsScreen(gateway,false,{error("No auth")},{_,_->},{}) } } }
        compose.onNodeWithTag("community-maps").performScrollToNode(hasText("Load More"))
        compose.onNodeWithText("Load More").performTouchInput { click();click() }
        compose.waitUntil(5_000){compose.onAllNodesWithText("Temporary page fixture error").fetchSemanticsNodes().isNotEmpty()}
        assertEquals(1,gateway.attempts)
        compose.onNodeWithText("Retry").performScrollTo().performClick()
        compose.waitUntil(5_000){compose.onAllNodesWithText("Fixture second").fetchSemanticsNodes().isNotEmpty()}
        assertEquals(listOf(null,"next","next"),gateway.cursors)
        compose.onNodeWithTag("community-maps").performScrollToNode(hasText("Fixture first"))
        compose.onNodeWithText("Fixture first").assertExists()
    }
    @Test fun queryAndSortSurviveStateRestoration() {
        val gateway=Gateway();val restoration=StateRestorationTester(compose)
        restoration.setContent { GridfallTheme { CommunityMapsTheme { CommunityMapsScreen(gateway,false,{error("No auth")},{_,_->},{}) } } }
        compose.waitUntil(5_000) { gateway.lastSort == "rated" &&
            compose.onAllNodesWithTag("community-loading").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Search maps or creators").performTextInput("restored fixture")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Search",substring=false).performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil(5_000) { gateway.lastQuery == "restored fixture" &&
            compose.onAllNodesWithTag("community-loading").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Newest").performScrollTo().performClick()
        compose.waitUntil(5_000) { gateway.lastSort == "newest" }
        compose.waitForIdle()
        val previousRequests = gateway.cursors.size
        restoration.emulateSavedInstanceStateRestore()
        compose.waitUntil(5_000) { gateway.cursors.size > previousRequests &&
            gateway.lastQuery == "restored fixture" && gateway.lastSort == "newest" }
        assertEquals("restored fixture",gateway.lastQuery);assertEquals("newest",gateway.lastSort)
    }
    @Test fun lightThemeDetailsAndBackRemainUsable() {
        val gateway=Gateway()
        compose.setContent { GridfallTheme(darkTheme=false) {
            CompositionLocalProvider(com.example.gridfall.ui.theme.LocalGridfallColors provides
                com.example.gridfall.ui.theme.colorsForThemeMode(com.example.gridfall.ui.theme.GridfallThemeMode.FrutigerAero)) {
                CommunityMapsTheme { CommunityMapsScreen(gateway,false,{error("No auth")},{_,_->},{}) }
            }
        } }
        compose.onNodeWithTag("community-maps").performScrollToNode(hasText("Fixture first"))
        compose.onNodeWithText("Preview / Play").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Import & Play").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Import & Play").assertIsDisplayed().assertIsDisplayed()
        val context=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val screenshot=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        java.io.File(context.getExternalFilesDir(null),"community-light.png").outputStream().use { screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
        screenshot.recycle()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithTag("community-maps").assertExists()
    }

}
