package com.example.gridfall.game

import kotlin.random.Random

object ContractGenerator {
    private val templates = listOf(
        ContractTemplate(
            id = "clear_at_least_one_line",
            title = "Clear 1 line",
            description = "Clear at least 1 line this batch.",
            baseRewardPoints = 25,
            type = ContractType.ClearAtLeastOneLine
        ),
        ContractTemplate(
            id = "clear_exactly_two_lines",
            title = "Clear exactly 2",
            description = "Clear exactly 2 lines this batch.",
            baseRewardPoints = 35,
            type = ContractType.ClearExactlyTwoLines
        ),
        ContractTemplate(
            id = "no_edge_placement",
            title = "Stay off edges",
            description = "Place all 3 pieces without touching the board edge.",
            baseRewardPoints = 25,
            type = ContractType.NoEdgePlacement
        ),
        ContractTemplate(
            id = "avoid_center_area",
            title = "Avoid center",
            description = "Place all 3 pieces without using the center 4x4 area.",
            baseRewardPoints = 25,
            type = ContractType.AvoidCenterArea
        ),
        ContractTemplate(
            id = "score_at_least_twenty",
            title = "Score 20",
            description = "Score at least 20 points this batch.",
            baseRewardPoints = 25,
            type = ContractType.ScoreAtLeastTwenty
        )
    )

    fun generate(level: Int = 1): Contract {
        val template = templates[Random.nextInt(templates.size)]
        val rewardPoints = template.baseRewardPoints + (level.coerceAtLeast(1) - 1) * 5

        return Contract(
            id = template.id,
            title = template.title,
            description = template.description,
            rewardPoints = rewardPoints,
            type = template.type
        )
    }

    private data class ContractTemplate(
        val id: String,
        val title: String,
        val description: String,
        val baseRewardPoints: Int,
        val type: ContractType
    )
}
