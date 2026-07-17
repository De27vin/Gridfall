package com.example.gridfall.network.dto

data class MeResponse(
    val id: String,
    val firebaseUid: String,
    val email: String?,
    val isAnonymous: Boolean,
    val username: String?,
    val profile: PlayerProfileDto
)

data class PlayerProfileDto(
    val bestScore: Int,
    val bestLevel: Int,
    val gamesPlayed: Int,
    val totalPoints: Int,
    val totalLinesCleared: Int,
    val totalContractsCompleted: Int,
    val totalBombsUsed: Int,
    val totalMegaBombsUsed: Int
)

