package com.example.gridfall.game

data class Contract(
    val id: String,
    val title: String,
    val description: String,
    val rewardPoints: Int,
    val penaltyPoints: Int,
    val type: ContractType
)

enum class ContractType {
    ClearAtLeastOneLine,
    ClearExactlyTwoLines,
    NoEdgePlacement,
    AvoidCenterArea,
    ScoreAtLeastTwenty
}
