package com.example.gridfall.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CustomBlockRulesTest {
    @Test
    fun `empty custom block is rejected`() {
        assertEquals(
            "Select at least one cell for this block.",
            CustomBlockRules.validationError(emptySet())
        )
        assertEquals(null, CustomBlockRules.toPiece(emptySet()))
    }

    @Test
    fun `connected custom block is normalized into a piece`() {
        val cells = setOf(Cell(2, 1), Cell(2, 2), Cell(3, 2))

        val piece = CustomBlockRules.toPiece(cells)

        assertNotNull(piece)
        assertEquals(
            setOf(Cell(0, 0), Cell(0, 1), Cell(1, 1)),
            piece?.cells?.toSet()
        )
    }

    @Test
    fun `disconnected custom block is allowed`() {
        val cells = setOf(Cell(0, 0), Cell(3, 3))

        assertNull(CustomBlockRules.validationError(cells))
        assertEquals(cells, CustomBlockRules.toPiece(cells)?.cells?.toSet())
    }

    @Test
    fun `custom block must fit the selected map`() {
        val fourTall = setOf(Cell(0, 0), Cell(1, 0), Cell(2, 0), Cell(3, 0))

        assertNull(CustomBlockRules.validationError(fourTall))
        assertEquals(
            "The custom block must fit inside this map.",
            CustomBlockRules.validationError(fourTall, boardRows = 3, boardColumns = 8)
        )
    }
}
