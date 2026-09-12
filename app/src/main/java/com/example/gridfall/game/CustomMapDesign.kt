package com.example.gridfall.game

data class CustomMapDesign(
    val rows: Int,
    val columns: Int,
    val blockedCells: Set<Cell>
)
