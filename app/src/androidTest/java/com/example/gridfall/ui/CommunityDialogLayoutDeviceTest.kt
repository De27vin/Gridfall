package com.example.gridfall.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
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

internal fun communityUiFixture(blockCount: Int = 64): CommunityPublication {
    val blocks = (0 until blockCount).map { i ->
        MapBlockDefinition("block_$i", "Block ${i + 1}", setOf(Cell(0, 0)),
            1000 / blockCount + if (i < 1000 % blockCount) 1 else 0, i % 4 + 1)
    }
    return CommunityPublication("GF-A7K9P23456", "Fixture Creator",
        CommunityMapDocument("Layout fixture", "Local test data only", CustomMapDesign(8, 8, emptySet(), blocks)),
        true, "public", 12, 4.2, 8)
}

internal open class CommunityUiGateway(val map: CommunityPublication = communityUiFixture()) : CommunityMapsGateway {
    override suspend fun fetch(code: String) = map
    override suspend fun browse(query: String, sort: String, cursor: String?, ownerToken: String?) = CommunityPage(listOf(map), null)
    override suspend fun publish(token: String, document: String, visibility: String, requestId: String): CommunityPublication = error("Publishing not configured in this fixture")
    override suspend fun importMap(code: String, requestId: String, token: String?) = map
    override suspend fun rate(code: String, stars: Int, token: String) = map.copy(ratingAverage = stars.toDouble())
    override suspend fun unpublish(code: String, token: String) = Unit
    override suspend fun report(code: String, reason: String, notes: String, token: String) = Unit
}

class CommunityDialogLayoutDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var density = 1f
    private fun open(fontScale: Float = 1f, gateway: CommunityUiGateway = CommunityUiGateway()) {
        compose.setContent {
            val d = LocalDensity.current
            density = d.density
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) {
                GridfallTheme { CommunityMapsTheme {
                    CommunityMapDialog(null, gateway, true, { "local-fixture-token" }, { _, _ -> }, {}, initialCode = gateway.map.code)
                } }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Import & Play").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun reachable(text: String) {
        val node = compose.onNodeWithText(text)
        if (runCatching { node.assertIsDisplayed() }.isFailure) node.performScrollTo()
        node.assertIsDisplayed()
    }
    private fun bounds(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
    @Test fun wideDialogAndActionsFitTheAvailableWindow() {
        open()
        val dialog = bounds("community-map-dialog")
        val viewport = bounds("community-dialog-viewport")
        assertTrue(dialog.width >= minOf(viewport.width, 640f * density) - 2)
        assertTrue(dialog.height <= viewport.height * 0.91f + 2)
        assertTrue(dialog.left >= viewport.left - 1 && dialog.right <= viewport.right + 1)
        assertTrue(dialog.top >= viewport.top - 1 && dialog.bottom <= viewport.bottom + 1)
        compose.onNodeWithText("Close").assertIsDisplayed()
        reachable("Save Map Offline"); reachable("Import & Play")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        java.io.File(context.filesDir, "community-dialog-layout.png").outputStream().use {
            image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        image.recycle()
    }
    @Test fun allFiveRatingsShareOneRowWithAccessibleTargetsAtLargeFont() {
        open(fontScale = 1.5f)
        compose.onNodeWithTag("community-rating-row").performScrollTo()
        val buttons = (1..5).map { bounds("community-rating-$it") }
        buttons.forEach {
            assertEquals(buttons.first().top, it.top, 1f)
            assertTrue("Rating target must be at least 48dp wide", it.width >= 48 * density - 1)
            assertTrue("Rating target must be at least 48dp high", it.height >= 48 * density - 1)
        }
        (1..5).forEach { compose.onNodeWithTag("community-rating-$it").assertIsDisplayed() }
        compose.onNodeWithTag("community-rating-3").performClick()
        compose.waitUntil(5_000) { runCatching { compose.onNodeWithTag("community-rating-3").assertIsSelected() }.isSuccess }
        compose.onNodeWithContentDescription("Rate 3 out of 5 stars").assertExists()
    }
    @Test fun longPiecePoolScrollsIndependentlyWithoutMovingTheBody() {
        open()
        compose.onNodeWithTag("community-piece-list").performScrollTo()
        assertTrue(bounds("community-piece-list").height <= 128 * density + 1)
        fun bodyOffset() = compose.onNodeWithTag("community-dialog-body").fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange].value()
        val before = bodyOffset()
        val pool = compose.onNodeWithTag("community-piece-list")
        val poolBefore = pool.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        pool.performTouchInput { swipeUp() }
        assertEquals(before, bodyOffset(), 1f)
        assertTrue(pool.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value() > poolBefore)
        // Accessibility scrolling may also bring the target into the outer viewport.
        pool.performScrollToNode(hasTestTag("community-piece-block_63"))
        compose.onNodeWithTag("community-piece-block_63").assertIsDisplayed()
        reachable("Import & Play")
    }
    @Test fun reportEditorAndActionsRemainReachableAboveKeyboard() {
        open()
        compose.onNodeWithText("Report Map").performScrollTo().performClick()
        val editor = compose.onNodeWithText("Report details (optional)")
        editor.performScrollTo()
        val before = bounds("community-dialog-viewport").height
        editor.performClick()
        editor.performTextInput("Local layout QA")
        compose.waitUntil(8_000) { bounds("community-dialog-viewport").height < before - 40 * density }
        editor.performScrollTo().assertIsDisplayed()
        val field = editor.fetchSemanticsNode().boundsInRoot
        val dialog = bounds("community-map-dialog")
        assertTrue(field.top >= dialog.top && field.bottom <= dialog.bottom)
        reachable("Import & Play")
        compose.onNodeWithText("Close").assertIsDisplayed()
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.waitUntil(8_000) { bounds("community-dialog-viewport").height >= before - 2 }
    }
}
