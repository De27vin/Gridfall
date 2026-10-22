package com.example.gridfall.game

object ContractBonusReward {
    const val BLOCK_BREAKER_DROP_PERCENT = 15

    fun addBlockBreakerIfRolled(
        inventory: List<JokerType>,
        rollPercent: Int
    ): List<JokerType> {
        if (rollPercent !in 0..99 || rollPercent >= BLOCK_BREAKER_DROP_PERCENT) return inventory
        return inventory + JokerType.BlockBreaker
    }
}
