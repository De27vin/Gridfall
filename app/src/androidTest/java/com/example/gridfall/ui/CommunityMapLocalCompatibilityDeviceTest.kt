package com.example.gridfall.ui

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gridfall.game.*
import org.junit.Assert.*
import org.junit.Test

class CommunityMapLocalCompatibilityDeviceTest {
    @Test fun legacySavesSurviveImportsAndEditedCopiesAreNotOverwritten() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("gridfall_saved_custom_maps", Context.MODE_PRIVATE)
        val original = preferences.getString("maps", null)
        try {
            preferences.edit().putString("maps", """[{"id":"legacy-map","name":"Old map","rows":8,"columns":8,"blockedCells":[{"row":0,"col":0}],"customBlockCells":[{"row":0,"col":0},{"row":1,"col":1}]}]""").commit()
            val store = SavedCustomMapStore(context)
            val legacy = store.load().single()
            assertEquals("", legacy.description)
            assertTrue(legacy.design.blockPool.any { it.id == "migrated_custom" })
            val doc = CommunityMapDocument("Imported", "Offline description", CustomMapDesign(4, 5, emptySet(), listOf(
                MapBlockDefinition("a", "Diagonal", setOf(Cell(0, 0), Cell(1, 1)), 123, 4),
                MapBlockDefinition("b", "Single", setOf(Cell(0, 0)), 877, 2))))
            val first = store.importCommunityMap("GF-A7K9P23456", doc)
            assertEquals(first, store.importCommunityMap("GF-A7K9P23456", doc))
            assertEquals(2, store.load().size)
            assertEquals(doc.design, store.load().first { it.id == first.id }.design)
            store.save(first.copy(name = "My edited map"))
            val second = store.importCommunityMap("GF-A7K9P23456", doc)
            assertNotEquals(first.id, second.id)
            assertEquals("My edited map", store.load().first { it.id == first.id }.name)
            assertEquals(doc.description, store.load().first { it.id == second.id }.description)
            assertEquals(legacy.design, store.load().first { it.id == legacy.id }.design)
        } finally {
            if (original == null) preferences.edit().remove("maps").commit()
            else preferences.edit().putString("maps", original).commit()
        }
    }
}
