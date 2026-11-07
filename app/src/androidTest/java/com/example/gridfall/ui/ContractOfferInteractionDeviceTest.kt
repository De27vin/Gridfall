package com.example.gridfall.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gridfall.game.*
import com.example.gridfall.ui.theme.GridfallTheme
import com.example.gridfall.ui.theme.GridfallThemeMode
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ContractOfferInteractionDeviceTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val single = Piece("offer-device-single", listOf(Cell(0, 0)))

    @Test fun modalOfferBlocksBackgroundActionsAndAcceptResumesOnlyFreshDrag() {
        verifyDecision("Accept")
    }

    @Test fun modalOfferBlocksBackgroundActionsAndSkipResumesOnlyFreshDrag() {
        verifyDecision("Skip")
    }

    @Test fun backDoesNotDismissTheUnresolvedOffer() {
        showPendingGame()
        compose.onNodeWithTag("contract-offer").assertIsDisplayed()
        androidx.test.espresso.Espresso.pressBack()
        compose.onNodeWithTag("contract-offer").assertIsDisplayed()
        compose.onNodeWithText("Skip").performClick()
        compose.onNodeWithTag("contract-offer").assertDoesNotExist()
    }

    @Test fun pieceDragIsCancelledWhenOfferArrivesAndCannotFinishAfterDecision() {
        verifyGestureCancellation(joker = false)
    }

    @Test fun jokerDragIsCancelledWhenOfferArrivesAndCannotFinishAfterDecision() {
        verifyGestureCancellation(joker = true)
    }

    private fun showPendingGame(score: Int = 20_000): GameState {
        val state = GameEngine.createInitialState().copy(
            board = Board.empty().fill(7, 7),
            currentPieces = List(3) { single },
            score = score,
            maxScoreReached = score,
            riskSpinState = RiskSpinState(inventory = listOf(JokerType.Single, JokerType.ClearBoard, JokerType.Revert)),
            contractState = ContractState(offeredContract = Contract(
                "offer-device", "Center Stage", "Use the center at least once.", 100, 65, ContractType.TouchCenter
            ))
        )
        ThemePreferenceStore.save(context, GridfallThemeMode.PremiumTactical)
        InProgressRunStore(context).save(SavedInProgressRun(state))
        compose.setContent { GridfallTheme { GameScreen() } }
        return state
    }

    private fun verifyDecision(decision: String) {
        val initial = showPendingGame()
        compose.onNodeWithTag("contract-offer").assertIsDisplayed()
        compose.onNodeWithText("Restart").assertIsNotEnabled()
        compose.onNodeWithText("Settings").performClick()
        // Exercise real pointer input against the underlying gameplay owner.
        dragToBoard("gameplay-piece-0")
        dragToBoard("gameplay-joker-0")
        compose.onNodeWithTag("gameplay-joker-1").performTouchInput { click() }
        compose.onNodeWithTag("gameplay-joker-2").performTouchInput { click() }
        compose.onNodeWithTag("contract-offer").assertIsDisplayed()
        compose.runOnIdle {
            val saved = requireNotNull(InProgressRunStore(context).load()).gameState
            assertEquals(initial.board, saved.board)
            assertEquals(initial.score, saved.score)
            assertEquals(initial.riskSpinState.inventory, saved.riskSpinState.inventory)
        }
        compose.onNodeWithText(decision).performClick()
        compose.onNodeWithTag("contract-offer").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(initial.board, InProgressRunStore(context).load()!!.gameState.board)
        }
        dragToBoard("gameplay-piece-0")
        compose.waitUntil(5_000) { InProgressRunStore(context).load()?.gameState?.score == initial.score + 1 }
    }

    private fun dragToBoard(tag: String) {
        val root = compose.onNodeWithTag("gameplay").fetchSemanticsNode().boundsInRoot
        val source = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.center - root.topLeft
        val board = compose.onNodeWithTag("gameplay-board").fetchSemanticsNode().boundsInRoot
        val density = context.resources.displayMetrics.density
        val target = board.center + Offset(0f, 64f * density) - root.topLeft
        compose.onNodeWithTag("gameplay").performTouchInput {
            down(source)
            moveBy(Offset(0f, -24f))
            moveTo(target, delayMillis = 300)
            up()
        }
    }

    private fun verifyGestureCancellation(joker: Boolean) {
        var pending by mutableStateOf(false)
        var starts = 0
        var ends = 0
        compose.setContent {
            GridfallTheme {
                Column {
                    if (joker) {
                        JokerInventoryCarousel(
                            inventory = listOf(JokerType.Single),
                            draggingJokerInventoryIndex = null,
                            canUseRevert = false,
                            canUseClearBoard = false,
                            interactionsEnabled = !pending,
                            onJokerDragStarted = { _, _, _, _, _ -> starts++ },
                            onJokerDragged = {}, onJokerDragEnded = { ends++ },
                            onJokerDragCancelled = {},
                            onRevertClicked = {}, onBlockBreakerClicked = {}, onClearBoardClicked = {}
                        )
                    } else {
                        PieceTray(
                            pieces = List(3) { single }, usedPieceIndices = emptySet(), draggingPieceIndex = null,
                            enabled = !pending,
                            onPieceDragStarted = { _, _, _, _ -> starts++ },
                            onPieceDragged = {}, onPieceDragEnded = { ends++ },
                            onPieceDragCancelled = {}
                        )
                    }
                }
            }
        }
        val node = compose.onNodeWithTag(if (joker) "gameplay-joker-0" else "gameplay-piece-0")
        node.performTouchInput { down(center); moveBy(Offset(0f, -40f)) }
        compose.runOnIdle { assertEquals(1, starts); pending = true }
        // Pointer-input coroutine cancellation need not invoke onDragCancel.
        // The contract is that the old gesture never produces a drop.
        compose.runOnIdle { pending = false }
        node.performTouchInput { moveBy(Offset(0f, 40f)); up() }
        compose.runOnIdle { assertEquals(0, ends) }
        node.performTouchInput { down(center); moveBy(Offset(0f, -40f)); up() }
        compose.runOnIdle { assertEquals(2, starts); assertEquals(1, ends) }
    }
}
