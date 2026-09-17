package com.example.gridfall.ui

import android.content.Context
import com.example.gridfall.game.Cell
import com.example.gridfall.game.CustomBlockRules
import com.example.gridfall.game.CustomMapDesign
import com.example.gridfall.game.CustomMapRules
import com.example.gridfall.game.MapBlockDefinition
import com.example.gridfall.game.MapBlockPoolRules
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
            design = normalizedDesign(design)
        )
    }

    fun save(savedMap: SavedCustomMap): List<SavedCustomMap> {
        val normalized = savedMap.copy(
            name = normalizedName(savedMap.name),
            design = normalizedDesign(savedMap.design)
        )
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
            .put("blockPool", JSONArray().also { blocks ->
                design.blockPool.forEach { block -> blocks.put(block.toJson()) }
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
        val legacyCustomBlockCells = buildSet {
            val cells = optJSONArray("customBlockCells") ?: JSONArray()
            repeat(cells.length()) { index ->
                val cell = cells.optJSONObject(index) ?: return@repeat
                val row = cell.optInt("row", -1)
                val col = cell.optInt("col", -1)
                if (row in 0 until CustomBlockRules.EDITOR_SIZE &&
                    col in 0 until CustomBlockRules.EDITOR_SIZE
                ) add(Cell(row, col))
            }
        }.takeIf { CustomBlockRules.validationError(it, rows, columns) == null }
            ?.let(CustomBlockRules::normalize)
            ?: emptySet()
        val savedBlockPool = optJSONArray("blockPool")?.toBlockPool(rows, columns)
        val blockPool = when {
            savedBlockPool != null && savedBlockPool.isNotEmpty() -> savedBlockPool
            legacyCustomBlockCells.isNotEmpty() -> MapBlockPoolRules.add(
                MapBlockPoolRules.defaultPool(),
                MapBlockDefinition(
                    id = "migrated_custom",
                    name = "Custom Block",
                    cells = legacyCustomBlockCells,
                    spawnChancePercent = 10,
                    colorVariant = 2
                )
            )
            else -> MapBlockPoolRules.defaultPool()
        }
        return SavedCustomMap(
            id = id,
            name = normalizedName(optString("name")),
            design = CustomMapDesign(rows, columns, blockedCells, blockPool)
        )
    }

    private fun normalizedDesign(design: CustomMapDesign): CustomMapDesign {
        val blockPool = MapBlockPoolRules.normalize(design.blockPool).takeIf {
            MapBlockPoolRules.validationError(it, design.rows, design.columns) == null
        } ?: MapBlockPoolRules.defaultPool()
        return design.copy(blockPool = blockPool)
    }

    private fun MapBlockDefinition.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("name", name)
            .put("cells", JSONArray().also { array ->
                cells.sortedWith(compareBy(Cell::row, Cell::col)).forEach { cell ->
                    array.put(JSONObject().put("row", cell.row).put("col", cell.col))
                }
            })
            .put("spawnChancePercent", spawnChancePercent)
            .put("colorVariant", colorVariant)
    }

    private fun JSONArray.toBlockPool(rows: Int, columns: Int): List<MapBlockDefinition>? {
        val blocks = (0 until length()).map { index ->
            val json = optJSONObject(index) ?: return null
            val cells = buildSet {
                val cellArray = json.optJSONArray("cells") ?: return null
                repeat(cellArray.length()) { cellIndex ->
                    val cell = cellArray.optJSONObject(cellIndex) ?: return null
                    add(Cell(cell.optInt("row", -1), cell.optInt("col", -1)))
                }
            }
            MapBlockDefinition(
                id = json.optString("id").takeIf { it.isNotBlank() } ?: return null,
                name = json.optString("name").takeIf { it.isNotBlank() } ?: "Block ${index + 1}",
                cells = cells,
                spawnChancePercent = json.optInt("spawnChancePercent", 0),
                colorVariant = json.optInt("colorVariant", 1)
            )
        }
        val normalized = MapBlockPoolRules.normalize(blocks)
        return normalized.takeIf { MapBlockPoolRules.validationError(it, rows, columns) == null }
    }

    private fun normalizedName(name: String): String = name.trim().take(MAX_NAME_LENGTH).ifBlank { "Custom Map" }

    private companion object {
        const val PREFS_NAME = "gridfall_saved_custom_maps"
        const val KEY_MAPS = "maps"
        const val MAX_NAME_LENGTH = 24
    }
}
