package com.example.gridfall.ui

import com.example.gridfall.game.Board
import com.example.gridfall.game.Cell
import com.example.gridfall.game.ContractState
import com.example.gridfall.game.ContractType

fun contractWarningCells(
    contractState: ContractState,
    boardRows: Int = Board.SIZE,
    boardColumns: Int = boardRows
): Set<Cell> {
    val contract = contractState.activeContract ?: return emptySet()
    if (!contractState.isAccepted) return emptySet()

    return when (contract.type) {
        ContractType.NoEdgePlacement -> edgeWarningCells(boardRows, boardColumns)
        ContractType.AvoidCenterArea -> centerWarningCells(boardRows, boardColumns)
        ContractType.AvoidCorners -> cornerWarningCells(boardRows, boardColumns)
        ContractType.ClearAtLeastOneLine,
        ContractType.ClearExactlyOneLine,
        ContractType.ClearExactlyTwoLines,
        ContractType.ScoreAtLeastTwenty,
        ContractType.TouchEdge,
        ContractType.TouchCenter,
        ContractType.ClearNoLines,
        ContractType.ClearTwoLinesInSinglePlacement,
        ContractType.TouchEdgeAndCenter -> emptySet()
    }
}

fun edgeWarningCells(
    boardRows: Int = Board.SIZE,
    boardColumns: Int = boardRows
): Set<Cell> {
    return buildSet {
        for (column in 0 until boardColumns) {
            add(Cell(0, column))
            add(Cell(boardRows - 1, column))
        }
        for (row in 0 until boardRows) {
            add(Cell(row, 0))
            add(Cell(row, boardColumns - 1))
        }
    }
}

fun centerWarningCells(
    boardRows: Int = Board.SIZE,
    boardColumns: Int = boardRows
): Set<Cell> {
    val centerRows = Board.centerZone(boardRows)
    val centerColumns = Board.centerZone(boardColumns)
    return buildSet {
        for (row in centerRows) {
            for (col in centerColumns) {
                add(Cell(row, col))
            }
        }
    }
}

fun cornerWarningCells(
    boardRows: Int = Board.SIZE,
    boardColumns: Int = boardRows
): Set<Cell> {
    return setOf(
        Cell(0, 0),
        Cell(0, boardColumns - 1),
        Cell(boardRows - 1, 0),
        Cell(boardRows - 1, boardColumns - 1)
    )
}
