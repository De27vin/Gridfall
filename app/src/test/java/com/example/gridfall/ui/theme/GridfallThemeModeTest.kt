package com.example.gridfall.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GridfallThemeModeTest {
    @Test
    fun frutigerAeroThemeHasStableUniqueIdAndPalette() {
        assertEquals(GridfallThemeMode.FrutigerAero, GridfallThemeMode.fromId("frutiger_aero"))
        assertEquals(GridfallThemeMode.FrutigerAero, GridfallThemeMode.fromId("aero"))
        assertEquals(GridfallThemeMode.FrutigerAero, GridfallThemeMode.fromId("frutiger_earo"))
        assertEquals(GridfallThemeMode.entries.size, GridfallThemeMode.entries.map { it.id }.toSet().size)
        assertEquals(FrutigerAeroColors, colorsForThemeMode(GridfallThemeMode.FrutigerAero))
        assertNotEquals(PremiumTacticalColors, FrutigerAeroColors)
        assertTrue(GridfallThemeMode.entries.any { it.label == "Frutiger Aero" })
    }
}