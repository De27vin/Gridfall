package com.example.gridfall.game

import kotlin.random.Random

object ContractGenerator {
    private val contracts = listOf(
        Contract(
            id = "clear_at_least_one_line",
            title = "Clear 1 line",
            description = "Clear at least 1 line this batch.",
            rewardPoints = 25,
            type = ContractType.ClearAtLeastOneLine
        ),
        Contract(
            id = "clear_exactly_two_lines",
            title = "Clear exactly 2",
            description = "Clear exactly 2 lines this batch.",
            rewardPoints = 35,
            type = ContractType.ClearExactlyTwoLines
        ),
        Contract(
            id = "no_edge_placement",
            title = "Stay off edges",
            description = "Place all 3 pieces without touching the board edge.",
            rewardPoints = 25,
            type = ContractType.NoEdgePlacement
        ),
        Contract(
            id = "avoid_center_area",
            title = "Avoid center",
            description = "Place all 3 pieces without using the center 4x4 area.",
            rewardPoints = 25,
            type = ContractType.AvoidCenterArea
        ),
        Contract(
            id = "score_at_least_twenty",
            title = "Score 20",
            description = "Score at least 20 points this batch.",
            rewardPoints = 25,
            type = ContractType.ScoreAtLeastTwenty
        )
    )

    fun generate(): Contract {
        return contracts[Random.nextInt(contracts.size)]
    }
}
