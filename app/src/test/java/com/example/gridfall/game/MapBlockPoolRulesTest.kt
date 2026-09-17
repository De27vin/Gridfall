package com.example.gridfall.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapBlockPoolRulesTest {
    @Test
    fun `default block pool totals one hundred percent`() {
        val pool = MapBlockPoolRules.defaultPool()

        assertEquals(100, pool.sumOf { it.spawnChancePercent })
        assertNull(MapBlockPoolRules.validationError(pool, 8, 8))
    }

    @Test
    fun `setting one probability rebalances all other blocks`() {
        val pool = listOf(
            block("one", 40),
            block("two", 30),
            block("three", 30)
        )

        val changed = MapBlockPoolRules.setProbability(pool, "one", 70)

        assertEquals(70, changed.first { it.id == "one" }.spawnChancePercent)
        assertEquals(100, changed.sumOf { it.spawnChancePercent })
    }

    @Test
    fun `deleting a block redistributes its probability`() {
        val pool = listOf(block("one", 50), block("two", 25), block("three", 25))

        val changed = MapBlockPoolRules.delete(pool, "one")

        assertEquals(listOf(50, 50), changed.map { it.spawnChancePercent })
    }

    @Test
    fun `piece generator honors exact map probability weights`() {
        val onlySpawnable = block("always", 100).toPiece()
        val disabled = block("never", 0).toPiece()

        val generated = PieceGenerator.generateBatch(
            count = 20,
            availablePieces = listOf(disabled, onlySpawnable),
            bombChance = 0f
        )

        assertEquals(List(20) { onlySpawnable }, generated)
    }

    private fun block(id: String, probability: Int): MapBlockDefinition {
        return MapBlockDefinition(
            id = id,
            name = id,
            cells = setOf(Cell(0, 0)),
            spawnChancePercent = probability
        )
    }
}
