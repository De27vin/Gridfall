package com.example.gridfall.game

import kotlin.math.roundToInt
import kotlin.random.Random

object ContractGenerator {
    const val CONTRACT_UNLOCK_SCORE = 5_000
    const val NEXT_CONTRACT_MIN_SCORE_DELTA = 1_300
    const val NEXT_CONTRACT_MAX_SCORE_DELTA = 1_700
    private const val BACK_TO_BACK_OFFER_PERCENT = 1

    private val templatesByLayout = mapOf(
        GridLayoutPreset.Rush to listOf(
            template("rush_line_hunter", "Line Hunter", "Clear at least 1 line this batch.", ContractType.ClearAtLeastOneLine, 1, 4, 110),
            template("rush_precision_clear", "Precision Clear", "Clear exactly 1 line this batch.", ContractType.ClearExactlyOneLine, 1, 3, 120),
            template("rush_point_rush", "Quick Points", "Score 12 points this batch.", ContractType.ScoreAtLeastTwenty, 12, 5, 100),
            template("rush_edge_tap", "Edge Tap", "Touch the outer edge at least once.", ContractType.TouchEdge, weight = 4, rewardPercent = 90),
            template("rush_center_stage", "Center Stage", "Use the center at least once.", ContractType.TouchCenter, weight = 3, rewardPercent = 95),
            template("rush_safe_corners", "Safe Corners", "Keep every piece off the four corners.", ContractType.AvoidCorners, weight = 2, rewardPercent = 110),
            template("rush_quiet_batch", "Quiet Batch", "Clear no lines this batch.", ContractType.ClearNoLines, weight = 2, rewardPercent = 90)
        ),
        GridLayoutPreset.Classic to listOf(
            template("clear_at_least_one_line", "Line Hunter", "Clear at least 1 line this batch.", ContractType.ClearAtLeastOneLine, 1, 5, 100),
            template("clear_exactly_one_line", "Precision Clear", "Clear exactly 1 line this batch.", ContractType.ClearExactlyOneLine, 1, 3, 110),
            template("clear_exactly_two_lines", "Double Clear", "Clear exactly 2 lines this batch.", ContractType.ClearExactlyTwoLines, 2, 3, 125),
            template("no_edge_placement", "No Borders", "Keep every piece in this batch off the edge.", ContractType.NoEdgePlacement, weight = 1, rewardPercent = 130),
            template("avoid_center_area", "Clear Core", "Keep every piece in this batch out of the center.", ContractType.AvoidCenterArea, weight = 1, rewardPercent = 125),
            template("score_at_least_twenty", "Point Rush", "Score 20 points this batch.", ContractType.ScoreAtLeastTwenty, 20, 5, 100),
            template("classic_safe_corners", "Safe Corners", "Keep every piece off the four corners.", ContractType.AvoidCorners, weight = 2, rewardPercent = 105),
            template("classic_edge_tap", "Edge Tap", "Touch the outer edge at least once.", ContractType.TouchEdge, weight = 2, rewardPercent = 90),
            template("classic_center_stage", "Center Stage", "Use the center at least once.", ContractType.TouchCenter, weight = 2, rewardPercent = 90),
            template("classic_quiet_batch", "Quiet Batch", "Clear no lines this batch.", ContractType.ClearNoLines, weight = 2, rewardPercent = 90),
            template("classic_double_strike", "Double Strike", "Clear 2 lines with a single piece.", ContractType.ClearTwoLinesInSinglePlacement, 2, 1, 140)
        ),
        GridLayoutPreset.Marathon to listOf(
            template("marathon_line_hunter", "Long Shot", "Clear at least 1 line this batch.", ContractType.ClearAtLeastOneLine, 1, 2, 135),
            template("marathon_precision_clear", "Precision Clear", "Clear exactly 1 line this batch.", ContractType.ClearExactlyOneLine, 1, 2, 145),
            template("marathon_point_rush", "Point Marathon", "Score 30 points this batch.", ContractType.ScoreAtLeastTwenty, 30, 5, 105),
            template("marathon_no_borders", "Inner Route", "Keep every piece in this batch off the edge.", ContractType.NoEdgePlacement, weight = 3, rewardPercent = 105),
            template("marathon_clear_core", "Clear Core", "Keep every piece in this batch out of the center.", ContractType.AvoidCenterArea, weight = 2, rewardPercent = 110),
            template("marathon_safe_corners", "Safe Corners", "Keep every piece off the four corners.", ContractType.AvoidCorners, weight = 3, rewardPercent = 90),
            template("marathon_edge_tap", "Outer Reach", "Touch the outer edge at least once.", ContractType.TouchEdge, weight = 2, rewardPercent = 105),
            template("marathon_center_stage", "Center Stage", "Use the center at least once.", ContractType.TouchCenter, weight = 2, rewardPercent = 95),
            template("marathon_quiet_batch", "Quiet Batch", "Clear no lines this batch.", ContractType.ClearNoLines, weight = 3, rewardPercent = 90),
            template("marathon_full_route", "Full Route", "Use both the center and outer edge this batch.", ContractType.TouchEdgeAndCenter, weight = 1, rewardPercent = 130)
        )
    )

    fun generate(score: Int, random: Random = Random.Default): Contract {
        return generate(score, GridLayoutPreset.Classic, random)
    }

    fun generate(
        score: Int,
        gridLayout: GridLayoutPreset,
        random: Random = Random.Default
    ): Contract {
        val templates = templatesFor(gridLayout)
        return contractFromTemplate(templates.weightedRandom(random), score)
    }

    internal fun generateForType(
        type: ContractType,
        score: Int,
        gridLayout: GridLayoutPreset = GridLayoutPreset.Classic
    ): Contract {
        return contractFromTemplate(templatesFor(gridLayout).first { it.type == type }, score)
    }

    internal fun contractTypeWeights(
        gridLayout: GridLayoutPreset = GridLayoutPreset.Classic
    ): Map<ContractType, Int> {
        return templatesFor(gridLayout).associate { it.type to it.weight }
    }

    internal fun availableTypes(gridLayout: GridLayoutPreset): Set<ContractType> {
        return templatesFor(gridLayout).mapTo(linkedSetOf()) { it.type }
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
        val targetValue: Int,
        val weight: Int,
        val rewardPercent: Int
    )

    private fun template(
        id: String,
        title: String,
        description: String,
        type: ContractType,
        targetValue: Int = 0,
        weight: Int,
        rewardPercent: Int
    ) = ContractTemplate(id, title, description, type, targetValue, weight, rewardPercent)

    private fun templatesFor(gridLayout: GridLayoutPreset): List<ContractTemplate> {
        return checkNotNull(templatesByLayout[gridLayout])
    }

    private fun contractFromTemplate(template: ContractTemplate, score: Int): Contract {
        val rewardPoints = (rewardForScore(score) * template.rewardPercent / 100f).roundToInt()
        return Contract(
            id = template.id,
            title = template.title,
            description = template.description,
            rewardPoints = rewardPoints,
            penaltyPoints = penaltyForReward(rewardPoints),
            type = template.type,
            targetValue = template.targetValue
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
