package com.example.gridfall.game

import com.example.gridfall.ui.contractTargetCells
import com.example.gridfall.ui.contractWarningCells
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContractCenterRegionTest {
    private val expectedAxes = mapOf(
        3 to 1..1, 4 to 1..2, 5 to 2..2, 6 to 2..3,
        7 to 2..4, 8 to 2..5, 9 to 3..5, 10 to 3..6
    )

    @Test
    fun centerIsSymmetricForEverySupportedAxisAndPreservesPresets() {
        expectedAxes.forEach { (size, expected) ->
            val actual = Board.centerZone(size)
            assertEquals("Axis $size", expected, actual)
            assertEquals(actual.first, size - 1 - actual.last)
        }
    }

    @Test
    fun fourByFourHighlightsExactlyTheMiddleTwoByTwoForAllCenterContracts() {
        val expected = setOf(Cell(1, 1), Cell(1, 2), Cell(2, 1), Cell(2, 2))
        assertEquals(expected, contractTargetCells(contractState(ContractType.TouchCenter), 4, 4))
        assertEquals(expected, contractWarningCells(contractState(ContractType.AvoidCenterArea), 4, 4))
        assertEquals(expected, contractTargetCells(
            contractState(ContractType.TouchEdgeAndCenter).copy(usedEdge = true), 4, 4
        ))
    }

    @Test
    fun validationAndGuidanceAgreeForEveryCellAcrossSquareOddAndRectangularBoards() {
        val dimensions = expectedAxes.keys.map { it to it } +
            listOf(4 to 7, 7 to 4, 5 to 8, 8 to 5, 3 to 10, 10 to 3)
        dimensions.forEach { (rows, columns) ->
            val expected = buildSet {
                for (row in expectedAxes.getValue(rows)) {
                    for (col in expectedAxes.getValue(columns)) add(Cell(row, col))
                }
            }
            assertEquals(expected, contractTargetCells(contractState(ContractType.TouchCenter), rows, columns))
            assertEquals(expected, contractWarningCells(contractState(ContractType.AvoidCenterArea), rows, columns))
            assertEquals(expected, contractTargetCells(
                contractState(ContractType.TouchEdgeAndCenter).copy(usedEdge = true), rows, columns
            ))

            for (type in listOf(ContractType.TouchCenter, ContractType.AvoidCenterArea, ContractType.TouchEdgeAndCenter)) {
                val state = state(rows, columns, type)
                for (row in 0 until rows) for (col in 0 until columns) {
                    val next = GameEngine.placePiece(state, 0, row, col)
                    val inside = Cell(row, col) in expected
                    val label = "$rows×$columns $type at ($row,$col)"
                    if (type == ContractType.AvoidCenterArea) {
                        assertEquals(label, inside, next.contractState.isFailed)
                    } else {
                        assertEquals(label, inside, next.contractState.usedCenter)
                    }
                }
            }
        }
    }

    @Test
    fun customFourByFourContractsResolveFromCorrectRegion() {
        for (type in listOf(ContractType.TouchCenter, ContractType.AvoidCenterArea, ContractType.TouchEdgeAndCenter)) {
            for (target in listOf(Cell(1, 1), Cell(0, 1))) {
                val first = GameEngine.placePiece(state(4, 4, type), 0, target.row, target.col)
                if (type == ContractType.AvoidCenterArea && target.row == 1) {
                    assertTrue(first.contractState.isFailed)
                    continue
                }
                val second = GameEngine.placePiece(first, 1, 3, 0)
                val third = GameEngine.placePiece(second, 2, 3, 3)
                val shouldComplete = if (type == ContractType.AvoidCenterArea) target.row == 0 else target.row == 1
                assertEquals("$type at $target", shouldComplete, third.contractState.isCompleted)
            }
        }
    }

    @Test
    fun multiCellPieceUsesCenterWhenOnlyItsSecondCellTouchesIt() {
        val state = state(4, 4, ContractType.TouchCenter).copy(
            currentPieces = listOf(
                Piece("cross-center-boundary", listOf(Cell(0, 0), Cell(0, 1))),
                single, single
            )
        )
        val outside = GameEngine.placePiece(state, 0, 0, 0)
        val inside = GameEngine.placePiece(state, 0, 1, 0)
        assertFalse(outside.contractState.usedCenter)
        assertTrue(inside.contractState.usedCenter)
    }

    private val single = Piece("center-test-single", listOf(Cell(0, 0)))

    private fun state(rows: Int, columns: Int, type: ContractType) = GameState(
        board = Board.empty(rows, columns, isCustom = rows != 8 || columns != 8),
        currentPieces = listOf(single, single, single),
        usedPieceIndices = emptySet(),
        score = 20_000,
        combo = 0,
        isGameOver = false,
        contractState = contractState(type)
    )

    private fun contractState(type: ContractType) = ContractState(
        activeContract = Contract(type.name, type.name, type.name, 100, 65, type),
        isAccepted = true
    )
}
