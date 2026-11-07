package com.example.gridfall.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gridfall.game.*
import com.example.gridfall.ui.theme.GridfallTheme
import com.example.gridfall.ui.theme.GridfallThemeMode
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class ContractBarLayoutDeviceTest(private val theme: GridfallThemeMode, private val status: String) {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun contractAndAllContentFitWithoutOverlappingGrid() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val piece = Piece("contract-layout-single", listOf(Cell(0, 0)))
        val contract = if (status == "route") Contract("layout-route", "Full Route",
            "Use both the center and outer edge this batch.", 2_175, 1_414, ContractType.TouchEdgeAndCenter)
        else Contract("layout-precision", "Precision Clear", "Clear exactly 1 line this batch.",
            2_175, 1_414, ContractType.ClearExactlyOneLine, 1)
        val state = GameEngine.createInitialState(GridLayoutPreset.Marathon).copy(
            currentPieces = List(4) { piece }, score = 20_000, maxScoreReached = 20_000,
            board = Board.empty(10).fill(9, 9),
            riskSpinState = RiskSpinState(inventory = List(7) { JokerType.Single } + JokerType.ClearBoard),
            contractState = if (status == "bonus") ContractState(
                resolvedContract = contract, isCompleted = true, rewardClaimed = true,
                bonusJokerReward = JokerType.BlockBreaker
            ) else ContractState(activeContract = contract, isAccepted = true,
                batchClearedLines = if (status == "goal") 1 else 0)
        )
        InProgressRunStore(context).save(SavedInProgressRun(state))
        ThemePreferenceStore.save(context, theme)
        compose.activityRule.scenario.onActivity { it.enableEdgeToEdge() }
        compose.setContent { GridfallTheme { GameScreen() } }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false

        val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.getExternalFilesDir(null), "contract-layout-${theme.id}-$status.png").outputStream().use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        screenshot.recycle()

        val viewport = bounds("gameplay")
        val board = bounds("gameplay-board")
        val contractBounds = bounds("gameplay-contract")
        val density = context.resources.displayMetrics.density
        assertTrue("Contract overlaps board: $contractBounds / $board",
            contractBounds.bottom + 4f * density <= board.top + 1f)
        val decor = compose.activity.window.decorView
        val insets = ViewCompat.getRootWindowInsets(decor)!!.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        assertTrue("Content crosses display insets", viewport.top >= insets.top &&
            viewport.bottom <= decor.height - insets.bottom)
        var previousBottom = viewport.top
        for (tag in listOf("gameplay-score", "gameplay-contract", "gameplay-board", "gameplay-tray",
            "gameplay-controls", "gameplay-inventory")) {
            compose.onNodeWithTag(tag).assertIsDisplayed()
            val rect = bounds(tag)
            assertTrue("$tag overlaps sibling", rect.top >= previousBottom - 1f)
            assertTrue("$tag is clipped", rect.bottom <= viewport.bottom + 1f &&
                rect.left >= viewport.left - 1f && rect.right <= viewport.right + 1f)
            previousBottom = rect.bottom
        }
        assertTrue("Board collapsed", board.height >= 64f * density)
        val textNodes = compose.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true
        ).fetchSemanticsNodes()
        textNodes.forEach { node ->
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            layouts.forEach { text ->
                // Paragraph width can include unused parent space; compare actual
                // line extents to the measured text node rather than that width.
                for (line in 0 until text.lineCount) {
                    assertTrue("Text clipped horizontally: ${text.layoutInput.text}",
                        text.getLineLeft(line) >= -1f && text.getLineRight(line) <= node.size.width + 1f)
                    assertTrue("Text clipped vertically: ${text.layoutInput.text}",
                        text.getLineTop(line) >= -1f && text.getLineBottom(line) <= node.size.height + 1f)
                }
                assertTrue("Text truncated: ${text.layoutInput.text}", !text.multiParagraph.didExceedMaxLines)
            }
        }
    }

    private fun bounds(tag: String): Rect {
        val node = compose.onNodeWithTag(tag).fetchSemanticsNode()
        return Rect(node.positionInRoot, Size(node.size.width.toFloat(), node.size.height.toFloat()))
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}-{1}")
        fun cases(): List<Array<Any>> = GridfallThemeMode.entries.flatMap { theme ->
            listOf("active", "goal", "bonus", "route").map { arrayOf<Any>(theme, it) }
        }
    }
}
