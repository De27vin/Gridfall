package com.example.gridfall.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GridfallThemeModeTest {
    @Test
    fun aeroThemeHasStableUniqueIdAndPalette() {
        assertEquals(GridfallThemeMode.Aero, GridfallThemeMode.fromId("aero"))
        assertEquals(GridfallThemeMode.entries.size, GridfallThemeMode.entries.map { it.id }.toSet().size)
        assertEquals(AeroColors, colorsForThemeMode(GridfallThemeMode.Aero))
        assertNotEquals(PremiumTacticalColors, AeroColors)
        assertTrue(GridfallThemeMode.entries.any { it.label == "Fruchtiger Earo" })
    }
}