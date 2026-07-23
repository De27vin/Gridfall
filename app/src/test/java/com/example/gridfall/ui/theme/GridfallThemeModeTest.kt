package com.example.gridfall.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GridfallThemeModeTest {
    @Test
    fun frutigerEaroThemeHasStableUniqueIdAndPalette() {
        assertEquals(GridfallThemeMode.FrutigerEaro, GridfallThemeMode.fromId("frutiger_earo"))
        assertEquals(GridfallThemeMode.FrutigerEaro, GridfallThemeMode.fromId("aero"))
        assertEquals(GridfallThemeMode.entries.size, GridfallThemeMode.entries.map { it.id }.toSet().size)
        assertEquals(FrutigerEaroColors, colorsForThemeMode(GridfallThemeMode.FrutigerEaro))
        assertNotEquals(PremiumTacticalColors, FrutigerEaroColors)
        assertTrue(GridfallThemeMode.entries.any { it.label == "Frutiger Earo" })
    }
}