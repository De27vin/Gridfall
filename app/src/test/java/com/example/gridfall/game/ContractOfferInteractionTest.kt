package com.example.gridfall.game

import org.junit.Assert.*
import org.junit.Test

class ContractOfferInteractionTest {
    private val single = Piece("offer-test-single", listOf(Cell(0, 0)))
    private val offer = Contract("offer-test", "Center Stage", "Use the center at least once.",
        100, 65, ContractType.TouchCenter)

    private fun pending(): GameState {
        val previous = GameEngine.createInitialState().copy(score = 20_000)
        return previous.copy(
            board = Board.empty().fill(7, 7),
            currentPieces = List(3) { single },
            contractState = ContractState(offeredContract = offer),
            riskSpinState = RiskSpinState(
                inventory = JokerType.entries,
                previousMoveSnapshot = previous
            )
        )
    }

    @Test fun offeredContractBlocksPiecesAndEveryPlaceableJokerWithoutMutatingState() {
        val state = pending()
        assertTrue(state.isAwaitingContractDecision)
        assertSame(state, GameEngine.placePiece(state, 0, 2, 2))
        JokerType.entries.filter { it.toPiece() != null }.forEach {
            assertSame("$it", state, GameEngine.placeJoker(state, it, 2, 2))
        }
    }

    @Test fun offeredContractBlocksTargetedClearBoardAndRevertJokers() {
        val state = pending()
        assertSame(state, GameEngine.useBlockBreaker(state, 7, 7))
        assertSame(state, GameEngine.useClearBoardJoker(state))
        assertSame(state, GameEngine.useRevertJoker(state))
    }

    @Test fun offeredContractBlocksAllRiskSpinEntryPointsEvenWithFreeInventory() {
        val state = pending().copy(riskSpinState = RiskSpinState())
        assertFalse(GameEngine.riskSpinAvailability(state).isAvailable)
        RiskSpinOption.entries.forEach {
            assertFalse(GameEngine.canSelectRiskSpinOption(state, it))
            assertNull(GameEngine.performRiskSpin(state, it))
            assertNull(GameEngine.startRiskSpinMemorySession(state, it))
        }
        val session = RiskSpinMemorySession(RiskSpinOption.Safe, 1,
            listOf(RiskSpinMemoryField(0, RiskSpinOutcome.Single)))
        assertNull(GameEngine.revealRiskSpinMemoryField(state, session, 0))
    }

    @Test fun acceptingOrDecliningResumesNormalPlacementsAndJokerUse() {
        listOf(GameEngine.acceptContract(pending()), GameEngine.skipContract(pending())).forEach { state ->
            assertFalse(state.isAwaitingContractDecision)
            assertNotEquals(state.board, GameEngine.placePiece(state, 0, 2, 2).board)
            assertNotEquals(state.board, GameEngine.placeJoker(state, JokerType.Single, 2, 2).board)
            assertTrue(GameEngine.useBlockBreaker(state, 7, 7).board.isEmpty())
            assertTrue(GameEngine.useClearBoardJoker(state).board.isEmpty())
            assertNotEquals(state, GameEngine.useRevertJoker(state))
        }
    }

    @Test fun acceptingOrDecliningResumesRiskSpinWithoutChangingItsRules() {
        val state = pending().copy(riskSpinState = RiskSpinState())
        listOf(GameEngine.acceptContract(state), GameEngine.skipContract(state)).forEach {
            assertTrue(GameEngine.riskSpinAvailability(it).isAvailable)
            assertNotNull(GameEngine.startRiskSpinMemorySession(it, RiskSpinOption.Safe))
        }
    }

    @Test fun placementThatCreatesOfferCannotBeFollowedByAnExtraPlacement() {
        val state = pending().copy(
            usedPieceIndices = setOf(0, 1),
            contractState = ContractState()
        )
        val offered = GameEngine.placePiece(state, 2, 1, 1)
        assertNotNull(offered.contractState.offeredContract)
        assertSame(offered, GameEngine.placePiece(offered, 0, 2, 2))
    }

    @Test fun terminalRunWithUnshownOfferDoesNotLockExistingUndoRecovery() {
        val state = pending().copy(isGameOver = true)
        assertFalse(state.isAwaitingContractDecision)
        assertFalse(GameEngine.useRevertJoker(state).isGameOver)
    }
}
