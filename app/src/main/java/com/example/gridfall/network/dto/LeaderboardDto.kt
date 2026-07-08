package com.example.gridfall.network.dto

data class LeaderboardResponse(
    val entries: List<LeaderboardEntryDto>
)

data class LeaderboardEntryDto(
    val rank: Int,
    val username: String,
    val bestScore: Int,
    val bestLevel: Int
)
