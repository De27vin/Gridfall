package com.example.gridfall.ui

import com.example.gridfall.game.Board
import com.example.gridfall.game.Cell
import com.example.gridfall.game.ContractState
import com.example.gridfall.game.ContractType

fun contractWarningCells(contractState: ContractState): Set<Cell> {
    val contract = contractState.activeContract ?: return emptySet()
    if (!contractState.isAccepted) return emptySet()

    return when (contract.type) {
        ContractType.NoEdgePlacement -> edgeWarningCells()
        ContractType.AvoidCenterArea -> centerWarningCells()
        ContractType.ClearAtLeastOneLine,
        ContractType.ClearExactlyTwoLines,
        ContractType.ScoreAtLeastTwenty -> emptySet()
    }
}

fun edgeWarningCells(): Set<Cell> {
    return buildSet {
        for (index in 0 until Board.SIZE) {
            add(Cell(0, index))
            add(Cell(Board.SIZE - 1, index))
            add(Cell(index, 0))
            add(Cell(index, Board.SIZE - 1))
        }
    }
}

fun centerWarningCells(): Set<Cell> {
    return buildSet {
        for (row in 2..5) {
            for (col in 2..5) {
                add(Cell(row, col))
            }
        }
    }
}
