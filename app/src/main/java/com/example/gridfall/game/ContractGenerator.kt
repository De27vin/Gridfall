package com.example.gridfall.game

import kotlin.random.Random

object ContractGenerator {
    private const val FIRST_OFFER_MIN_BATCHES = 3
    private const val FIRST_OFFER_MAX_BATCHES = 5
    private const val NEXT_OFFER_MIN_BATCHES = 12
    private const val NEXT_OFFER_MAX_BATCHES = 18

    private val templates = listOf(
        ContractTemplate(
            id = "clear_at_least_one_line",
            title = "Clear 1 line",
            description = "Clear at least 1 line this batch.",
            baseRewardPoints = 25,
            type = ContractType.ClearAtLeastOneLine,
            weight = 1
        ),
        ContractTemplate(
            id = "clear_exactly_two_lines",
            title = "Clear exactly 2",
            description = "Clear exactly 2 lines this batch.",
            baseRewardPoints = 40,
            type = ContractType.ClearExactlyTwoLines,
            weight = 1
        ),
        ContractTemplate(
            id = "no_edge_placement",
            title = "Stay off edges",
            description = "Place all 3 pieces without touching the board edge.",
            baseRewardPoints = 25,
            type = ContractType.NoEdgePlacement,
            weight = 3
        ),
        ContractTemplate(
            id = "avoid_center_area",
            title = "Avoid center",
            description = "Place all 3 pieces without using the center 4x4 area.",
            baseRewardPoints = 25,
            type = ContractType.AvoidCenterArea,
            weight = 3
        ),
        ContractTemplate(
            id = "score_at_least_twenty",
            title = "Score 20",
            description = "Score at least 20 points this batch.",
            baseRewardPoints = 25,
            type = ContractType.ScoreAtLeastTwenty,
            weight = 3
        )
    )

    fun generate(
        level: Int = 1,
        random: Random = Random.Default
    ): Contract {
        return contractFromTemplate(
            template = templates.weightedRandom(random),
            level = level
        )
    }

    internal fun generateForType(
        type: ContractType,
        level: Int = 1
    ): Contract {
        val template = templates.first { it.type == type }
        return contractFromTemplate(template = template, level = level)
    }

    internal fun contractTypeWeights(): Map<ContractType, Int> {
        return templates.associate { template -> template.type to template.weight }
    }

    fun initialOfferCooldown(): Int {
        return Random.nextInt(FIRST_OFFER_MIN_BATCHES, FIRST_OFFER_MAX_BATCHES + 1)
    }

    fun nextOfferCooldown(): Int {
        return Random.nextInt(NEXT_OFFER_MIN_BATCHES, NEXT_OFFER_MAX_BATCHES + 1)
    }

    private data class ContractTemplate(
        val id: String,
        val title: String,
        val description: String,
        val baseRewardPoints: Int,
        val type: ContractType,
        val weight: Int
    )

    private fun contractFromTemplate(
        template: ContractTemplate,
        level: Int
    ): Contract {
        val rewardPoints = template.baseRewardPoints + (level.coerceAtLeast(1) - 1) * 5

        return Contract(
            id = template.id,
            title = template.title,
            description = template.description,
            rewardPoints = rewardPoints,
            penaltyPoints = rewardPoints * 2,
            type = template.type
        )
    }

    private fun List<ContractTemplate>.weightedRandom(random: Random): ContractTemplate {
        val totalWeight = sumOf { template -> template.weight.coerceAtLeast(0) }
        if (totalWeight <= 0) return first()

        var target = random.nextInt(totalWeight)
        forEach { template ->
            val weight = template.weight.coerceAtLeast(0)
            if (target < weight) return template
            target -= weight
        }

        return last()
    }
}
