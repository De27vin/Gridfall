package com.example.gridfall.game

import kotlin.math.floor
import kotlin.random.Random

enum class RiskSpinOption(
    val title: String,
    val stakePercent: Float,
    val spinCount: Int,
    val minimumCost: Int
) {
    Safe("Safe Spin", 0.10f, 1, 10),
    Risk("Risk Spin", 0.20f, 3, 20),
    Desperate("Desperate Spin", 0.35f, 5, 35);

    fun cost(score: Int): Int {
        return maxOf(minimumCost, floor(score * stakePercent).toInt())
    }
}

enum class RiskSpinOutcome(val title: String) {
    Miss("Miss"),
    Single("1x1 Joker"),
    HorizontalTwo("2x1 Joker"),
    VerticalTwo("1x2 Joker"),
    Bomb("Bomb Joker"),
    Revert("Revert Joker");

    fun toJokerType(): JokerType? {
        return when (this) {
            Miss -> null
            Single -> JokerType.Single
            HorizontalTwo -> JokerType.HorizontalTwo
            VerticalTwo -> JokerType.VerticalTwo
            Bomb -> JokerType.Bomb
            Revert -> JokerType.Revert
        }
    }
}

data class RiskSpinAvailability(
    val isAvailable: Boolean,
    val reason: String? = null
)

data class RiskSpinResultEntry(
    val outcome: RiskSpinOutcome,
    val jokerAdded: JokerType? = null,
    val lostBecauseInventoryFull: Boolean = false
)

data class RiskSpinResult(
    val state: GameState,
    val option: RiskSpinOption,
    val cost: Int,
    val entries: List<RiskSpinResultEntry>
)

object RiskSpin {
    const val COOLDOWN_MIN_BATCHES = 10
    const val COOLDOWN_MAX_BATCHES = 15
    const val MEMORY_GRID_SIZE = 9
    const val MEMORY_FIELD_COUNT = MEMORY_GRID_SIZE * MEMORY_GRID_SIZE

    fun rewardPool(): List<RiskSpinOutcome> {
        return listOf(
            RiskSpinOutcome.Miss,
            RiskSpinOutcome.Miss,
            RiskSpinOutcome.Single,
            RiskSpinOutcome.HorizontalTwo,
            RiskSpinOutcome.VerticalTwo,
            RiskSpinOutcome.Bomb,
            RiskSpinOutcome.Revert,
            RiskSpinOutcome.Revert
        )
    }

    fun randomCooldown(random: Random = Random.Default): Int {
        return random.nextInt(
            from = COOLDOWN_MIN_BATCHES,
            until = COOLDOWN_MAX_BATCHES + 1
        )
    }

    fun spinEntries(
        spinCount: Int,
        startingInventory: List<JokerType>,
        random: Random = Random.Default
    ): Pair<List<JokerType>, List<RiskSpinResultEntry>> {
        val pool = rewardPool()
        val inventory = startingInventory.toMutableList()
        val entries = List(spinCount) {
            val outcome = pool[random.nextInt(pool.size)]
            val joker = outcome.toJokerType()

            when {
                joker == null -> RiskSpinResultEntry(outcome = outcome)
                inventory.size < RiskSpinState.MAX_INVENTORY_SIZE -> {
                    inventory += joker
                    RiskSpinResultEntry(outcome = outcome, jokerAdded = joker)
                }
                else -> RiskSpinResultEntry(
                    outcome = outcome,
                    lostBecauseInventoryFull = true
                )
            }
        }

        return inventory.toList() to entries
    }

    fun generateMemoryFields(random: Random = Random.Default): List<RiskSpinMemoryField> {
        val pool = rewardPool()
        return List(MEMORY_FIELD_COUNT) { index ->
            RiskSpinMemoryField(
                id = index,
                outcome = pool[random.nextInt(pool.size)]
            )
        }
    }

    fun createMemorySession(
        option: RiskSpinOption,
        random: Random = Random.Default
    ): RiskSpinMemorySession {
        return RiskSpinMemorySession(
            option = option,
            revealsLeft = option.spinCount,
            fields = generateMemoryFields(random)
        )
    }
}
