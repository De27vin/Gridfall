package com.example.gridfall.game

import kotlin.math.floor
import kotlin.random.Random

enum class RiskSpinOption(
    val title: String,
    val stakePercent: Float,
    val spinCount: Int,
    val minimumCost: Int
) {
    Safe("Safe Spin", 0.025f, 1, 10),
    Risk("Risk Spin", 0.10f, 3, 20),
    Desperate("Desperate Spin", 0.175f, 5, 35);

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
    MegaBomb("Mega Bomb Joker"),
    Revert("Revert Joker"),
    BlockBreaker("Block Breaker"),
    ClearBoard("Clean Slate Joker");

    fun toJokerType(): JokerType? {
        return when (this) {
            Miss -> null
            Single -> JokerType.Single
            HorizontalTwo -> JokerType.HorizontalTwo
            VerticalTwo -> JokerType.VerticalTwo
            Bomb -> JokerType.Bomb
            MegaBomb -> JokerType.MegaBomb
            Revert -> JokerType.Revert
            BlockBreaker -> JokerType.BlockBreaker
            ClearBoard -> JokerType.ClearBoard
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
        return buildList {
            // Preserve the original relative weighting across 99 slots, then
            // reserve the final 1% for the exceptionally strong Clean Slate.
            repeat(27) { add(RiskSpinOutcome.Miss) }
            repeat(9) { add(RiskSpinOutcome.Single) }
            repeat(9) { add(RiskSpinOutcome.HorizontalTwo) }
            repeat(9) { add(RiskSpinOutcome.VerticalTwo) }
            repeat(9) { add(RiskSpinOutcome.Bomb) }
            repeat(9) { add(RiskSpinOutcome.BlockBreaker) }
            repeat(18) { add(RiskSpinOutcome.Revert) }
            repeat(9) { add(RiskSpinOutcome.MegaBomb) }
            add(RiskSpinOutcome.ClearBoard)
        }
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
