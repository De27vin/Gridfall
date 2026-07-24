package com.example.gridfall.game

import kotlin.math.roundToInt
import kotlin.random.Random

object ContractGenerator {
    const val CONTRACT_UNLOCK_SCORE = 5_000
    const val NEXT_CONTRACT_MIN_SCORE_DELTA = 1_300
    const val NEXT_CONTRACT_MAX_SCORE_DELTA = 1_700
    private const val BACK_TO_BACK_OFFER_PERCENT = 1

    private val templates = listOf(
        ContractTemplate(
            id = "clear_at_least_one_line",
            title = "Line Hunter",
            description = "Clear 1 line this batch.",
            type = ContractType.ClearAtLeastOneLine,
            weight = 5
        ),
        ContractTemplate(
            id = "clear_exactly_two_lines",
            title = "Double Clear",
            description = "Clear exactly 2 lines this batch.",
            type = ContractType.ClearExactlyTwoLines,
            weight = 4
        ),
        ContractTemplate(
            id = "no_edge_placement",
            title = "No Borders",
            description = "Keep all 3 pieces off the edge.",
            type = ContractType.NoEdgePlacement,
            weight = 1
        ),
        ContractTemplate(
            id = "avoid_center_area",
            title = "Clear Core",
            description = "Keep all 3 pieces out of the center.",
            type = ContractType.AvoidCenterArea,
            weight = 1
        ),
        ContractTemplate(
            id = "score_at_least_twenty",
            title = "Point Rush",
            description = "Score 20 points this batch.",
            type = ContractType.ScoreAtLeastTwenty,
            weight = 5
        )
    )

    fun generate(score: Int, random: Random = Random.Default): Contract {
        return contractFromTemplate(templates.weightedRandom(random), score)
    }

    internal fun generateForType(type: ContractType, score: Int): Contract {
        return contractFromTemplate(templates.first { it.type == type }, score)
    }

    internal fun contractTypeWeights(): Map<ContractType, Int> {
        return templates.associate { it.type to it.weight }
    }

    fun nextContractScoreThreshold(
        currentThreshold: Int,
        random: Random = Random.Default
    ): Int {
        return currentThreshold.coerceAtLeast(CONTRACT_UNLOCK_SCORE) +
            random.nextInt(NEXT_CONTRACT_MIN_SCORE_DELTA, NEXT_CONTRACT_MAX_SCORE_DELTA + 1)
    }

    fun shouldOfferBackToBack(random: Random = Random.Default): Boolean {
        return random.nextInt(100) < BACK_TO_BACK_OFFER_PERCENT
    }

    fun rewardForScore(score: Int): Int {
        return when {
            score < 6_500 -> 600
            score < 8_000 -> 750
            score < 9_500 -> 900
            score < 11_000 -> 1_100
            score < 12_500 -> 1_300
            else -> 1_500
        }
    }

    fun penaltyForReward(rewardPoints: Int): Int = (rewardPoints * 0.65f).roundToInt()

    private data class ContractTemplate(
        val id: String,
        val title: String,
        val description: String,
        val type: ContractType,
        val weight: Int
    )

    private fun contractFromTemplate(template: ContractTemplate, score: Int): Contract {
        val rewardPoints = rewardForScore(score)
        return Contract(
            id = template.id,
            title = template.title,
            description = template.description,
            rewardPoints = rewardPoints,
            penaltyPoints = penaltyForReward(rewardPoints),
            type = template.type
        )
    }

    private fun List<ContractTemplate>.weightedRandom(random: Random): ContractTemplate {
        var target = random.nextInt(sumOf { it.weight })
        forEach { template ->
            if (target < template.weight) return template
            target -= template.weight
        }
        return last()
    }
}