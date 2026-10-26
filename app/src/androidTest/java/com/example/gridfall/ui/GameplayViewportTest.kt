package com.example.gridfall.ui

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gridfall.game.Board
import com.example.gridfall.game.Cell
import com.example.gridfall.game.Contract
import com.example.gridfall.game.ContractState
import com.example.gridfall.game.ContractType
import com.example.gridfall.game.GameEngine
import com.example.gridfall.game.GridLayoutPreset
import com.example.gridfall.game.JokerType
import com.example.gridfall.game.Piece
import com.example.gridfall.game.RiskSpinState
import com.example.gridfall.ui.theme.GridfallTheme
import com.example.gridfall.ui.theme.GridfallThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Run on small phones as well as tablet/foldable emulator configurations. */
class GameplayViewportTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun classicFitsAndCannotBeSwipedVertically() {
        showGame(8, 8)
        assertLayoutFits()
        val before = bounds("gameplay-board")
        compose.onNodeWithTag("gameplay").performTouchInput { swipeUp() }
        compose.onNodeWithTag("gameplay").performTouchInput { swipeDown() }
        assertEquals(before, bounds("gameplay-board"))
        assertLayoutFits()
        compose.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
        ).assertCountEquals(0)
    }

    @Test fun rushFitsWithNextPreview() {
        showGame(7, 7)
        assertLayoutFits()
        compose.onNodeWithText("NEXT").assertIsDisplayed()
    }

    @Test fun marathonFitsWithFourPieces() {
        showGame(10, 10)
        assertLayoutFits()
        compose.onNodeWithTag("gameplay-piece-3").assertIsDisplayed()
    }

    @Test fun tallCustomMapFits() {
        showGame(10, 3)
        assertLayoutFits()
        val board = bounds("gameplay-board")
        assertEquals(3f / 10f, board.width / board.height, 0.02f)
    }

    @Test fun wideCustomMapFits() {
        showGame(3, 10)
        assertLayoutFits()
        val board = bounds("gameplay-board")
        assertEquals(10f / 3f, board.width / board.height, 0.02f)
    }

    @Test fun bonusContractResultFitsWithoutHidingBoard() {
        showGame(8, 8, resolved = true)
        assertLayoutFits()
    }

    @Test fun retroThemeFits() {
        showGame(8, 8, theme = GridfallThemeMode.RetroArcade)
        assertLayoutFits()
    }

    @Test fun freshGameFitsWithUnlockMessage() {
        showGame(8, 8, score = 0)
        assertLayoutFits()
        compose.onNodeWithText("Unlocks at Level 3").assertIsDisplayed()
    }

    @Test fun piecesAndInventoryJokersStillDragOntoBoard() {
        showGame(8, 8)
        dragToBoard("gameplay-piece-0", Offset(-0.15f, 0f))
        compose.onAllNodesWithText("20,001")[0].assertIsDisplayed()
        dragToBoard("gameplay-joker-0", Offset(0.15f, 0f))
        compose.onAllNodesWithText("20,002")[0].assertIsDisplayed()
        assertLayoutFits()
    }

    @Test fun overflowInventoryScrollsHorizontallyAndTapJokerWorks() {
        showGame(8, 8)
        compose.onNodeWithTag("gameplay-joker-7").performScrollTo()
        compose.onNodeWithTag("gameplay-joker-7").assertIsDisplayed()
        compose.onNodeWithTag("gameplay-joker-7").performTouchInput { click() }
        compose.onAllNodesWithText("21,000")[0].assertIsDisplayed()
        assertLayoutFits()
    }

    @Test fun settingsKeepsItsVerticalScrolling() {
        showGame(8, 8)
        compose.onNodeWithText("Settings").performClick()
        assertTrue(compose.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
        ).fetchSemanticsNodes().isNotEmpty())
    }

    private fun showGame(
        rows: Int,
        columns: Int,
        resolved: Boolean = false,
        score: Int = 20_000,
        theme: GridfallThemeMode = GridfallThemeMode.PremiumTactical
    ) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val layout = GridLayoutPreset.fromBoardSize(rows)
        val custom = rows != columns
        val single = Piece("viewport-single", listOf(Cell(0, 0)))
        val contract = Contract("viewport-contract", "Point Rush", "Score 100 points this batch.",
            1_500, 975, ContractType.ScoreAtLeastTwenty, 100)
        val state = GameEngine.createInitialState(layout).copy(
            board = Board.empty(rows, columns, isCustom = custom).fill(rows - 1, columns - 1),
            currentPieces = List(if (custom) 3 else layout.piecesPerBatch) { single },
            nextPiece = if (!custom && layout.showsNextPiecePreview) single else null,
            score = score,
            maxScoreReached = score,
            riskSpinState = RiskSpinState(inventory = if (score == 0) emptyList()
                else List(7) { JokerType.Single } + JokerType.ClearBoard),
            contractState = if (score == 0) ContractState() else if (resolved) ContractState(
                resolvedContract = contract,
                isCompleted = true,
                rewardClaimed = true,
                bonusJokerReward = JokerType.BlockBreaker
            ) else ContractState(
                activeContract = contract,
                isAccepted = true
            )
        )
        InProgressRunStore(context).save(SavedInProgressRun(state))
        ThemePreferenceStore.save(context, theme)
        compose.activityRule.scenario.onActivity { it.enableEdgeToEdge() }
        compose.setContent { GridfallTheme { GameScreen() } }
        compose.onNodeWithTag("gameplay-board").assertIsDisplayed()
    }

    private fun assertLayoutFits() {
        compose.waitForIdle()
        val viewport = bounds("gameplay")
        val decor = compose.activity.window.decorView
        val insets = ViewCompat.getRootWindowInsets(decor)!!.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        assertTrue("Gameplay intrudes into system bars or cutout",
            viewport.top >= insets.top && viewport.left >= insets.left &&
                viewport.bottom <= decor.height - insets.bottom &&
                viewport.right <= decor.width - insets.right)
        var bottom = viewport.top
        listOf("gameplay-score", "gameplay-contract", "gameplay-board", "gameplay-tray",
            "gameplay-controls", "gameplay-inventory").forEach { tag ->
            if (tag == "gameplay-contract" &&
                compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isEmpty()
            ) return@forEach
            compose.onNodeWithTag(tag).assertIsDisplayed()
            val rect = bounds(tag)
            assertTrue("$tag overlaps previous element: $rect", rect.top >= bottom - 1f)
            assertTrue("$tag exceeds viewport: $rect vs $viewport",
                rect.bottom <= viewport.bottom + 1f && rect.left >= viewport.left - 1f &&
                    rect.right <= viewport.right + 1f)
            assertTrue("$tag has no usable area", rect.width > 0f && rect.height > 0f)
            bottom = rect.bottom
        }
        val tray = bounds("gameplay-tray")
        val piece = bounds("gameplay-piece-0")
        assertTrue("Piece overflows its tray", piece.top >= tray.top - 1f && piece.bottom <= tray.bottom + 1f)
        val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        assertTrue("Board collapsed", bounds("gameplay-board").height >= 64f * density)
    }

    private fun bounds(tag: String): Rect {
        val node = compose.onNodeWithTag(tag).fetchSemanticsNode()
        return Rect(node.positionInRoot, Size(node.size.width.toFloat(), node.size.height.toFloat()))
    }

    private fun dragToBoard(tag: String, relativeTarget: Offset) {
        val root = bounds("gameplay")
        val source = bounds(tag).center - root.topLeft
        val board = bounds("gameplay-board")
        val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        val target = board.center + Offset(board.width * relativeTarget.x, 64f * density) - root.topLeft
        compose.onNodeWithTag("gameplay").performTouchInput {
            down(source)
            moveBy(Offset(0f, -24f))
            moveTo(target, delayMillis = 500)
            up()
        }
    }
}
