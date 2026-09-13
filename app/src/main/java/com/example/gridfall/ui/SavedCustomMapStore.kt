package com.example.gridfall.ui

import android.content.Context
import com.example.gridfall.game.Cell
import com.example.gridfall.game.CustomMapDesign
import com.example.gridfall.game.CustomMapRules
import com.example.gridfall.game.SavedCustomMap
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class SavedCustomMapStore(private val context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<SavedCustomMap> {
        val raw = preferences.getString(KEY_MAPS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                repeat(array.length()) { index ->
                    array.optJSONObject(index)?.toSavedMap()?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun create(name: String, design: CustomMapDesign): SavedCustomMap {
        return SavedCustomMap(
            id = UUID.randomUUID().toString(),
            name = normalizedName(name),
            design = design
        )
    }

    fun save(savedMap: SavedCustomMap): List<SavedCustomMap> {
        val normalized = savedMap.copy(name = normalizedName(savedMap.name))
        val maps = load().toMutableList()
        val existingIndex = maps.indexOfFirst { it.id == normalized.id }
        if (existingIndex >= 0) maps[existingIndex] = normalized else maps.add(normalized)
        persist(maps)
        return maps
    }

    fun delete(id: String): List<SavedCustomMap> {
        val maps = load().filterNot { it.id == id }
        persist(maps)
        return maps
    }

    private fun persist(maps: List<SavedCustomMap>) {
        val array = JSONArray().also { json -> maps.forEach { json.put(it.toJson()) } }
        preferences.edit().putString(KEY_MAPS, array.toString()).apply()
    }

    private fun SavedCustomMap.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("name", name)
            .put("rows", design.rows)
            .put("columns", design.columns)
            .put("blockedCells", JSONArray().also { cells ->
                design.blockedCells.sortedWith(compareBy(Cell::row, Cell::col)).forEach { cell ->
                    cells.put(JSONObject().put("row", cell.row).put("col", cell.col))
                }
            })
    }

    private fun JSONObject.toSavedMap(): SavedCustomMap? {
        val id = optString("id").takeIf { it.isNotBlank() } ?: return null
        val rows = optInt("rows")
        val columns = optInt("columns")
        if (rows !in CustomMapRules.MIN_BOARD_SIZE..CustomMapRules.MAX_ROWS ||
            columns !in CustomMapRules.MIN_BOARD_SIZE..CustomMapRules.MAX_COLUMNS
        ) return null
        val blockedCells = buildSet {
            val cells = optJSONArray("blockedCells") ?: JSONArray()
            repeat(cells.length()) { index ->
                val cell = cells.optJSONObject(index) ?: return@repeat
                val row = cell.optInt("row", -1)
                val col = cell.optInt("col", -1)
                if (row in 0 until rows && col in 0 until columns) add(Cell(row, col))
            }
        }
        return SavedCustomMap(
            id = id,
            name = normalizedName(optString("name")),
            design = CustomMapDesign(rows, columns, blockedCells)
        )
    }

    private fun normalizedName(name: String): String = name.trim().take(MAX_NAME_LENGTH).ifBlank { "Custom Map" }

    private companion object {
        const val PREFS_NAME = "gridfall_saved_custom_maps"
        const val KEY_MAPS = "maps"
        const val MAX_NAME_LENGTH = 24
    }
}
