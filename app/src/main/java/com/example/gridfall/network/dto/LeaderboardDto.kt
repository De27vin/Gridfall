package com.example.gridfall.network.dto

data class LeaderboardResponse(
    val entries: List<LeaderboardEntryDto>
)

data class LeaderboardsResponse(
    val me: YourStatsDto?,
    val leaderboards: LeaderboardSectionsDto
)

data class YourStatsDto(
    val username: String?,
    val bestScore: Int,
    val bestLevel: Int,
    val totalPoints: Int,
    val gamesPlayed: Int,
    val totalLinesCleared: Int,
    val totalContractsCompleted: Int
)

data class LeaderboardSectionsDto(
    val bestScore: LeaderboardSectionDto,
    val totalPoints: LeaderboardSectionDto,
    val linesCleared: LeaderboardSectionDto,
    val contractsCompleted: LeaderboardSectionDto
)

data class LeaderboardSectionDto(
    val entries: List<LeaderboardEntryDto>,
    val me: LeaderboardEntryDto?
)

data class LeaderboardEntryDto(
    val rank: Int,
    val username: String,
    val bestScore: Int = 0,
    val bestLevel: Int = 1,
    val totalPoints: Int = 0,
    val totalLinesCleared: Int = 0,
    val totalContractsCompleted: Int = 0,
    val isInTop: Boolean = false
)

enum class LeaderboardType {
    BestScore,
    TotalPoints,
    LinesCleared,
    ContractsCompleted
}