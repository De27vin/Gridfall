package com.example.gridfall.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapBlockPoolRulesTest {
    @Test
    fun `default block pool totals one hundred percent`() {
        val pool = MapBlockPoolRules.defaultPool()

        assertEquals(1_000, pool.sumOf { it.spawnChanceTenthsPercent })
        assertNull(MapBlockPoolRules.validationError(pool, 8, 8))
    }

    @Test
    fun `map fit is validated separately from block structure`() {
        val pool = listOf(
            MapBlockDefinition(
                id = "four-tall",
                name = "Four Tall",
                cells = setOf(Cell(0, 0), Cell(1, 0), Cell(2, 0), Cell(3, 0)),
                spawnChanceTenthsPercent = 1_000
            )
        )

        assertNull(MapBlockPoolRules.validationError(pool))
        assertEquals(
            "Four Tall: The custom block must fit inside this map.",
            MapBlockPoolRules.validationError(pool, boardRows = 3, boardColumns = 8)
        )
    }

    @Test
    fun `setting one probability rebalances all other blocks`() {
        val pool = listOf(
            block("one", 400),
            block("two", 300),
            block("three", 300)
        )

        val changed = MapBlockPoolRules.setProbability(pool, "one", 703)

        assertEquals(703, changed.first { it.id == "one" }.spawnChanceTenthsPercent)
        assertEquals(1_000, changed.sumOf { it.spawnChanceTenthsPercent })
    }

    @Test
    fun `deleting a block redistributes its probability`() {
        val pool = listOf(block("one", 500), block("two", 250), block("three", 250))

        val changed = MapBlockPoolRules.delete(pool, "one")

        assertEquals(listOf(500, 500), changed.map { it.spawnChanceTenthsPercent })
    }

    @Test
    fun `piece generator honors exact map probability weights`() {
        val onlySpawnable = block("always", 1_000).toPiece()
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
            spawnChanceTenthsPercent = probability
        )
    }
}
