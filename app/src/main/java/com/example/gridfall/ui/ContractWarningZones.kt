package com.example.gridfall.ui

import com.example.gridfall.game.Board
import com.example.gridfall.game.Cell
import com.example.gridfall.game.ContractState
import com.example.gridfall.game.ContractType

fun contractWarningCells(contractState: ContractState, boardSize: Int = Board.SIZE): Set<Cell> {
    val contract = contractState.activeContract ?: return emptySet()
    if (!contractState.isAccepted) return emptySet()

    return when (contract.type) {
        ContractType.NoEdgePlacement -> edgeWarningCells(boardSize)
        ContractType.AvoidCenterArea -> centerWarningCells(boardSize)
        ContractType.ClearAtLeastOneLine,
        ContractType.ClearExactlyTwoLines,
        ContractType.ScoreAtLeastTwenty -> emptySet()
    }
}

fun edgeWarningCells(boardSize: Int = Board.SIZE): Set<Cell> {
    return buildSet {
        for (index in 0 until boardSize) {
            add(Cell(0, index))
            add(Cell(boardSize - 1, index))
            add(Cell(index, 0))
            add(Cell(index, boardSize - 1))
        }
    }
}

fun centerWarningCells(boardSize: Int = Board.SIZE): Set<Cell> {
    val centerZone = Board.centerZone(boardSize)
    return buildSet {
        for (row in centerZone) {
            for (col in centerZone) {
                add(Cell(row, col))
            }
        }
    }
}
