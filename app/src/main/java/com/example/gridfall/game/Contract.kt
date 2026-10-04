package com.example.gridfall.game

data class Contract(
    val id: String,
    val title: String,
    val description: String,
    val rewardPoints: Int,
    val penaltyPoints: Int,
    val type: ContractType,
    val targetValue: Int = 0
)

enum class ContractType {
    ClearAtLeastOneLine,
    ClearExactlyOneLine,
    ClearExactlyTwoLines,
    NoEdgePlacement,
    AvoidCenterArea,
    ScoreAtLeastTwenty,
    AvoidCorners,
    TouchEdge,
    TouchCenter,
    ClearNoLines,
    ClearTwoLinesInSinglePlacement,
    TouchEdgeAndCenter
}

fun Contract.effectiveTargetValue(): Int {
    if (targetValue > 0) return targetValue
    return when (type) {
        ContractType.ClearAtLeastOneLine,
        ContractType.ClearExactlyOneLine -> 1
        ContractType.ClearExactlyTwoLines,
        ContractType.ClearTwoLinesInSinglePlacement -> 2
        ContractType.ScoreAtLeastTwenty -> 20
        ContractType.NoEdgePlacement,
        ContractType.AvoidCenterArea,
        ContractType.AvoidCorners,
        ContractType.TouchEdge,
        ContractType.TouchCenter,
        ContractType.ClearNoLines,
        ContractType.TouchEdgeAndCenter -> 0
    }
}
