package com.example.gridfall.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RiskSpinTest {
    @Test
    fun riskSpinIsLockedBeforeLevelThree() {
        val state = testState(score = 149)

        val availability = GameEngine.riskSpinAvailability(state)

        assertFalse(availability.isAvailable)
        assertEquals("Unlocks at Level 3", availability.reason)
    }

    @Test
    fun riskSpinIsAvailableAtLevelThreeWhenReady() {
        val state = testState(score = 170)

        val availability = GameEngine.riskSpinAvailability(state)

        assertTrue(availability.isAvailable)
        assertNull(availability.reason)
    }

    @Test
    fun riskSpinIsDisabledDuringCooldownAndWhenInventoryIsFull() {
        val coolingDownState = testState(
            score = 150,
            riskSpinState = RiskSpinState(cooldownBatchesRemaining = 2)
        )
        val fullInventoryState = testState(
            score = 150,
            riskSpinState = RiskSpinState(
                inventory = List(RiskSpinState.MAX_INVENTORY_SIZE) { JokerType.Single }
            )
        )

        assertFalse(GameEngine.riskSpinAvailability(coolingDownState).isAvailable)
        assertFalse(GameEngine.riskSpinAvailability(fullInventoryState).isAvailable)
    }

    @Test
    fun riskSpinRewardPoolContainsMegaBombAsRareReward() {
        val pool = RiskSpin.rewardPool()

        assertEquals(3, pool.count { it == RiskSpinOutcome.Miss })
        assertEquals(2, pool.count { it == RiskSpinOutcome.Revert })
        assertEquals(1, pool.count { it == RiskSpinOutcome.MegaBomb })
        assertTrue(RiskSpinOutcome.Single in pool)
        assertTrue(RiskSpinOutcome.HorizontalTwo in pool)
        assertTrue(RiskSpinOutcome.VerticalTwo in pool)
        assertTrue(RiskSpinOutcome.Bomb in pool)
    }

    @Test
    fun performRiskSpinChargesScoreImmediatelyAndStartsCooldown() {
        val state = testState(score = 200)

        val result = GameEngine.performRiskSpin(
            state = state,
            option = RiskSpinOption.Safe,
            random = Random(4)
        )

        assertNotNull(result)
        val nextState = result!!.state
        assertEquals(180, nextState.score)
        assertEquals(20, result.cost)
        assertEquals(1, result.entries.size)
        assertTrue(
            nextState.riskSpinState.cooldownBatchesRemaining in
                RiskSpin.COOLDOWN_MIN_BATCHES..RiskSpin.COOLDOWN_MAX_BATCHES
        )
    }

    @Test
    fun memorySessionStartsWithSafeRiskAndDesperateRevealCounts() {
        val state = testState(score = 500)

        val safe = GameEngine.startRiskSpinMemorySession(state, RiskSpinOption.Safe, Random(1))
        val risk = GameEngine.startRiskSpinMemorySession(state, RiskSpinOption.Risk, Random(1))
        val desperate = GameEngine.startRiskSpinMemorySession(state, RiskSpinOption.Desperate, Random(1))

        assertEquals(1, safe!!.session.revealsLeft)
        assertEquals(3, risk!!.session.revealsLeft)
        assertEquals(5, desperate!!.session.revealsLeft)
    }

    @Test
    fun memorySessionStartPaysCostOnceStartsCooldownAndDoesNotAddInventory() {
        val state = testState(score = 200)

        val result = GameEngine.startRiskSpinMemorySession(
            state = state,
            option = RiskSpinOption.Safe,
            random = Random(4)
        )

        assertNotNull(result)
        assertEquals(180, result!!.state.score)
        assertEquals(20, result.cost)
        assertEquals(emptyList<JokerType>(), result.state.riskSpinState.inventory)
        assertTrue(
            result.state.riskSpinState.cooldownBatchesRemaining in
                RiskSpin.COOLDOWN_MIN_BATCHES..RiskSpin.COOLDOWN_MAX_BATCHES
        )
        assertFalse(GameEngine.riskSpinAvailability(result.state).isAvailable)
    }

    @Test
    fun memorySessionGeneratedBoardHasEightyOneWeightedFields() {
        val fields = RiskSpin.generateMemoryFields(Random(2))
        val pool = RiskSpin.rewardPool().toSet()

        assertEquals(81, fields.size)
        assertTrue(fields.all { it.outcome in pool })
    }

    @Test
    fun spinOptionCostsUseMinimumOrScorePercentage() {
        assertEquals(10, RiskSpinOption.Safe.cost(50))
        assertEquals(25, RiskSpinOption.Safe.cost(250))
        assertEquals(20, RiskSpinOption.Risk.cost(50))
        assertEquals(50, RiskSpinOption.Risk.cost(250))
        assertEquals(35, RiskSpinOption.Desperate.cost(50))
        assertEquals(87, RiskSpinOption.Desperate.cost(250))
        assertFalse(GameEngine.canSelectRiskSpinOption(testState(score = 5), RiskSpinOption.Safe))
    }

    @Test
    fun spinOptionIsDisabledIfPaymentWouldDropBelowLevelThree() {
        assertFalse(GameEngine.canSelectRiskSpinOption(testState(score = 150), RiskSpinOption.Safe))
        assertTrue(GameEngine.canSelectRiskSpinOption(testState(score = 167), RiskSpinOption.Safe))
    }

    @Test
    fun riskSpinUnavailableIfNoOptionKeepsLevelThree() {
        val state = testState(score = 150)

        val availability = GameEngine.riskSpinAvailability(state)

        assertFalse(availability.isAvailable)
        assertEquals("Need enough score to stay Level 3", availability.reason)
        assertNull(
            GameEngine.performRiskSpin(
                state = state,
                option = RiskSpinOption.Safe,
                random = Random(1)
            )
        )
    }

    @Test
    fun cannotSpinAgainUntilCooldownFinishes() {
        val state = testState(score = 500)

        val result = GameEngine.performRiskSpin(
            state = state,
            option = RiskSpinOption.Safe,
            random = Random(5)
        )

        assertNotNull(result)
        assertFalse(GameEngine.riskSpinAvailability(result!!.state).isAvailable)
    }

    @Test
    fun spinEntriesDoNotOverflowInventory() {
        val fullInventory = List(RiskSpinState.MAX_INVENTORY_SIZE) { JokerType.Single }

        val (nextInventory, entries) = RiskSpin.spinEntries(
            spinCount = 5,
            startingInventory = fullInventory,
            random = Random(8)
        )

        assertEquals(RiskSpinState.MAX_INVENTORY_SIZE, nextInventory.size)
        assertTrue(entries.none { it.jokerAdded != null })
    }

    @Test
    fun riskSpinInventoryMaxIsFive() {
        assertEquals(5, RiskSpinState.MAX_INVENTORY_SIZE)
    }

    @Test
    fun partialInventoryCannotGrowBeyondFive() {
        val nearlyFullInventory = List(4) { JokerType.Single }

        val (nextInventory, _) = RiskSpin.spinEntries(
            spinCount = 5,
            startingInventory = nearlyFullInventory,
            random = Random(12)
        )

        assertTrue(nextInventory.size <= RiskSpinState.MAX_INVENTORY_SIZE)
    }

    @Test
    fun missOutcomeDoesNotCreateJoker() {
        assertNull(RiskSpinOutcome.Miss.toJokerType())
    }

    @Test
    fun memoryRevealDecrementsRevealsLeftAndRevealedFieldCannotRevealAgain() {
        val state = testState(score = 200)
        val session = memorySession(
            revealsLeft = 1,
            outcomes = listOf(RiskSpinOutcome.Single)
        )

        val afterReveal = GameEngine.revealRiskSpinMemoryField(state, session, fieldId = 0)

        assertNotNull(afterReveal)
        assertEquals(0, afterReveal!!.session.revealsLeft)
        assertTrue(afterReveal.session.fields.first().isRevealed)
        assertNull(
            GameEngine.revealRiskSpinMemoryField(
                state = afterReveal.state,
                session = afterReveal.session,
                fieldId = 0
            )
        )
    }

    @Test
    fun memoryMissAddsNoJoker() {
        val state = testState(score = 200)
        val session = memorySession(
            revealsLeft = 1,
            outcomes = listOf(RiskSpinOutcome.Miss)
        )

        val afterReveal = GameEngine.revealRiskSpinMemoryField(state, session, fieldId = 0)

        assertNotNull(afterReveal)
        assertEquals(emptyList<JokerType>(), afterReveal!!.state.riskSpinState.inventory)
        assertNull(afterReveal.entry.jokerAdded)
    }

    @Test
    fun memoryJokerRewardAddsOneJokerWhenInventoryHasSpace() {
        val state = testState(score = 200)
        val session = memorySession(
            revealsLeft = 1,
            outcomes = listOf(RiskSpinOutcome.Revert)
        )

        val afterReveal = GameEngine.revealRiskSpinMemoryField(state, session, fieldId = 0)

        assertNotNull(afterReveal)
        assertEquals(listOf(JokerType.Revert), afterReveal!!.state.riskSpinState.inventory)
        assertEquals(JokerType.Revert, afterReveal.entry.jokerAdded)
    }

    @Test
    fun memoryMegaBombRewardAddsMegaBombJokerWhenInventoryHasSpace() {
        val state = testState(score = 200)
        val session = memorySession(
            revealsLeft = 1,
            outcomes = listOf(RiskSpinOutcome.MegaBomb)
        )

        val afterReveal = GameEngine.revealRiskSpinMemoryField(state, session, fieldId = 0)

        assertNotNull(afterReveal)
        assertEquals(listOf(JokerType.MegaBomb), afterReveal!!.state.riskSpinState.inventory)
        assertEquals(JokerType.MegaBomb, afterReveal.entry.jokerAdded)
    }

    @Test
    fun memoryJokerRewardIsLostWhenInventoryIsFull() {
        val state = testState(
            score = 200,
            riskSpinState = RiskSpinState(
                inventory = List(RiskSpinState.MAX_INVENTORY_SIZE) { JokerType.Single }
            )
        )
        val session = memorySession(
            revealsLeft = 1,
            outcomes = listOf(RiskSpinOutcome.Bomb)
        )

        val afterReveal = GameEngine.revealRiskSpinMemoryField(state, session, fieldId = 0)

        assertNotNull(afterReveal)
        assertEquals(RiskSpinState.MAX_INVENTORY_SIZE, afterReveal!!.state.riskSpinState.inventory.size)
        assertTrue(afterReveal.entry.lostBecauseInventoryFull)
        assertTrue(afterReveal.session.fields.first().lostBecauseInventoryFull)
    }

    @Test
    fun memoryMegaBombRewardIsLostWhenInventoryIsFull() {
        val state = testState(
            score = 200,
            riskSpinState = RiskSpinState(
                inventory = List(RiskSpinState.MAX_INVENTORY_SIZE) { JokerType.Single }
            )
        )
        val session = memorySession(
            revealsLeft = 1,
            outcomes = listOf(RiskSpinOutcome.MegaBomb)
        )

        val afterReveal = GameEngine.revealRiskSpinMemoryField(state, session, fieldId = 0)

        assertNotNull(afterReveal)
        assertEquals(RiskSpinState.MAX_INVENTORY_SIZE, afterReveal!!.state.riskSpinState.inventory.size)
        assertTrue(afterReveal.entry.lostBecauseInventoryFull)
        assertTrue(afterReveal.session.fields.first().lostBecauseInventoryFull)
    }

    @Test
    fun memoryCannotRevealAfterRevealsLeftIsZero() {
        val state = testState(score = 200)
        val session = memorySession(
            revealsLeft = 0,
            outcomes = listOf(RiskSpinOutcome.Single)
        )

        assertNull(GameEngine.revealRiskSpinMemoryField(state, session, fieldId = 0))
    }

    @Test
    fun riskSpinCooldownAdvancesAfterCompletedBatch() {
        val state = testState(
            score = 200,
            currentPieces = listOf(singlePiece(), singlePiece(), singlePiece()),
            riskSpinState = RiskSpinState(cooldownBatchesRemaining = 12)
        )

        val afterFirst = GameEngine.placePiece(state, pieceIndex = 0, startRow = 0, startCol = 0)
        val afterSecond = GameEngine.placePiece(afterFirst, pieceIndex = 1, startRow = 0, startCol = 1)
        val afterThird = GameEngine.placePiece(afterSecond, pieceIndex = 2, startRow = 0, startCol = 2)

        assertEquals(11, afterThird.riskSpinState.cooldownBatchesRemaining)
    }

    @Test
    fun successfulJokerPlacementConsumesJokerAndStoresRevertSnapshot() {
        val state = testState(
            score = 200,
            riskSpinState = RiskSpinState(inventory = listOf(JokerType.Single))
        )

        val nextState = GameEngine.placeJoker(
            state = state,
            jokerType = JokerType.Single,
            startRow = 2,
            startCol = 2
        )

        assertEquals(4, nextState.board.get(2, 2))
        assertEquals(emptyList<JokerType>(), nextState.riskSpinState.inventory)
        assertEquals(201, nextState.score)
        assertNotNull(nextState.riskSpinState.previousMoveSnapshot)
    }

    @Test
    fun horizontalTwoJokerPlacesAndConsumes() {
        val state = testState(
            riskSpinState = RiskSpinState(inventory = listOf(JokerType.HorizontalTwo))
        )

        val nextState = GameEngine.placeJoker(
            state = state,
            jokerType = JokerType.HorizontalTwo,
            startRow = 2,
            startCol = 2
        )

        assertEquals(3, nextState.board.get(2, 2))
        assertEquals(3, nextState.board.get(2, 3))
        assertEquals(emptyList<JokerType>(), nextState.riskSpinState.inventory)
    }

    @Test
    fun verticalTwoJokerPlacesAndConsumes() {
        val state = testState(
            riskSpinState = RiskSpinState(inventory = listOf(JokerType.VerticalTwo))
        )

        val nextState = GameEngine.placeJoker(
            state = state,
            jokerType = JokerType.VerticalTwo,
            startRow = 2,
            startCol = 2
        )

        assertEquals(2, nextState.board.get(2, 2))
        assertEquals(2, nextState.board.get(3, 2))
        assertEquals(emptyList<JokerType>(), nextState.riskSpinState.inventory)
    }

    @Test
    fun bombJokerUsesBombBehaviorAndConsumes() {
        val board = Board.empty()
            .fill(2, 2)
            .fill(2, 3)
            .fill(3, 2)
        val state = testState(
            board = board,
            riskSpinState = RiskSpinState(inventory = listOf(JokerType.Bomb))
        )

        val nextState = GameEngine.placeJoker(
            state = state,
            jokerType = JokerType.Bomb,
            startRow = 3,
            startCol = 3
        )

        assertEquals(0, nextState.board.get(2, 2))
        assertEquals(0, nextState.board.get(2, 3))
        assertEquals(0, nextState.board.get(3, 2))
        assertEquals(emptyList<JokerType>(), nextState.riskSpinState.inventory)
        assertTrue(nextState.score > state.score)
    }

    @Test
    fun megaBombJokerClearsFourByFourAreaAndConsumes() {
        val board = (0 until 4).fold(Board.empty()) { currentBoard, row ->
            (0 until 4).fold(currentBoard) { rowBoard, col ->
                if (row == 0 && col == 0) rowBoard else rowBoard.fill(row, col)
            }
        }.fill(5, 5)
        val state = testState(
            board = board,
            riskSpinState = RiskSpinState(inventory = listOf(JokerType.MegaBomb))
        )

        val nextState = GameEngine.placeJoker(
            state = state,
            jokerType = JokerType.MegaBomb,
            startRow = 0,
            startCol = 0
        )

        for (row in 0 until 4) {
            for (col in 0 until 4) {
                assertEquals(0, nextState.board.get(row, col))
            }
        }
        assertEquals(1, nextState.board.get(5, 5))
        assertEquals(emptyList<JokerType>(), nextState.riskSpinState.inventory)
        assertEquals(ScoreSystem.BOMB_PLACE_POINTS + 15 * ScoreSystem.BOMB_CLEAR_POINTS_PER_CELL, nextState.score)
    }

    @Test
    fun megaBombNearEdgesClampsToFullFourByFourWhenPossible() {
        val board = (4 until 8).fold(Board.empty()) { currentBoard, row ->
            (4 until 8).fold(currentBoard) { rowBoard, col ->
                if (row == 6 && col == 6) rowBoard else rowBoard.fill(row, col)
            }
        }.fill(3, 3)
        val state = testState(
            board = board,
            riskSpinState = RiskSpinState(inventory = listOf(JokerType.MegaBomb))
        )

        val nextState = GameEngine.placeJoker(
            state = state,
            jokerType = JokerType.MegaBomb,
            startRow = 6,
            startCol = 6
        )

        for (row in 4 until 8) {
            for (col in 4 until 8) {
                assertEquals(0, nextState.board.get(row, col))
            }
        }
        assertEquals(1, nextState.board.get(3, 3))
    }

    @Test
    fun invalidMegaBombPlacementDoesNotConsumeJoker() {
        val state = testState(
            board = Board.empty().fill(2, 2),
            riskSpinState = RiskSpinState(inventory = listOf(JokerType.MegaBomb))
        )

        val nextState = GameEngine.placeJoker(
            state = state,
            jokerType = JokerType.MegaBomb,
            startRow = 2,
            startCol = 2
        )

        assertEquals(state, nextState)
        assertEquals(listOf(JokerType.MegaBomb), nextState.riskSpinState.inventory)
    }

    @Test
    fun invalidJokerPlacementDoesNotConsumeJoker() {
        val state = testState(
            board = Board.empty().fill(2, 2),
            riskSpinState = RiskSpinState(inventory = listOf(JokerType.Single))
        )

        val nextState = GameEngine.placeJoker(
            state = state,
            jokerType = JokerType.Single,
            startRow = 2,
            startCol = 2
        )

        assertEquals(state, nextState)
        assertEquals(listOf(JokerType.Single), nextState.riskSpinState.inventory)
    }

    @Test
    fun revertRestoresPreviousStateConsumesRevertAndClearsSnapshot() {
        val state = testState(
            score = 200,
            riskSpinState = RiskSpinState(
                inventory = listOf(JokerType.Single, JokerType.Revert)
            )
        )
        val afterJoker = GameEngine.placeJoker(
            state = state,
            jokerType = JokerType.Single,
            startRow = 3,
            startCol = 3
        )

        val reverted = GameEngine.useRevertJoker(afterJoker)

        assertEquals(Board.empty(), reverted.board)
        assertEquals(200, reverted.score)
        assertEquals(listOf(JokerType.Single), reverted.riskSpinState.inventory)
        assertNull(reverted.riskSpinState.previousMoveSnapshot)
    }

    @Test
    fun revertCannotChainBackWithoutANewMove() {
        val state = testState(
            riskSpinState = RiskSpinState(
                inventory = listOf(JokerType.Single, JokerType.Revert, JokerType.Revert)
            )
        )
        val afterJoker = GameEngine.placeJoker(
            state = state,
            jokerType = JokerType.Single,
            startRow = 3,
            startCol = 3
        )
        val reverted = GameEngine.useRevertJoker(afterJoker)

        val secondRevertAttempt = GameEngine.useRevertJoker(reverted)

        assertEquals(reverted, secondRevertAttempt)
        assertEquals(listOf(JokerType.Single, JokerType.Revert), reverted.riskSpinState.inventory)
        assertNull(reverted.riskSpinState.previousMoveSnapshot)
    }

    private fun testState(
        board: Board = Board.empty(),
        currentPieces: List<Piece> = listOf(singlePiece(), singlePiece(), singlePiece()),
        usedPieceIndices: Set<Int> = emptySet(),
        score: Int = 0,
        combo: Int = 0,
        isGameOver: Boolean = false,
        contractState: ContractState = ContractState(),
        riskSpinState: RiskSpinState = RiskSpinState()
    ): GameState {
        return GameState(
            board = board,
            currentPieces = currentPieces,
            usedPieceIndices = usedPieceIndices,
            score = score,
            combo = combo,
            isGameOver = isGameOver,
            contractState = contractState,
            riskSpinState = riskSpinState
        )
    }

    private fun singlePiece(): Piece {
        return Piece(
            id = "single",
            cells = listOf(Cell(0, 0)),
            colorVariant = 1
        )
    }

    private fun memorySession(
        revealsLeft: Int,
        outcomes: List<RiskSpinOutcome>
    ): RiskSpinMemorySession {
        return RiskSpinMemorySession(
            option = RiskSpinOption.Safe,
            revealsLeft = revealsLeft,
            fields = outcomes.mapIndexed { index, outcome ->
                RiskSpinMemoryField(
                    id = index,
                    outcome = outcome
                )
            }
        )
    }
}
