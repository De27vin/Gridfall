package com.example.gridfall.game

import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

data class CommunityMapDocument(val name: String, val description: String, val design: CustomMapDesign)

/** Strict, versioned sharing format; deliberately separate from legacy local-save recovery. */
object CommunityMapFormat {
    const val MAX_BYTES = 65_536
    fun export(name: String, description: String = "", design: CustomMapDesign): String {
        val d = design.copy(blockPool = design.blockPool.map { it.copy(cells = CustomBlockRules.normalize(it.cells)) })
        val root = JSONObject().put("formatVersion", 1).put("name", name.trim()).put("description", description.trim())
            .put("rows", d.rows).put("columns", d.columns).put("blockedCells", cellsJson(d.blockedCells))
            .put("blockPool", JSONArray().also { a -> d.blockPool.forEach { b ->
                a.put(JSONObject().put("id", b.id).put("name", b.name.trim()).put("cells", cellsJson(b.cells))
                    .put("colorVariant", b.colorVariant).put("spawnChanceTenthsPercent", b.spawnChanceTenthsPercent))
            } })
        val raw = canonical(root)
        decode(raw)
        return raw
    }

    fun decode(raw: String): CommunityMapDocument {
        require(raw.toByteArray(StandardCharsets.UTF_8).size <= MAX_BYTES) { "Map is too large" }
        val j = JSONObject(raw)
        keys(j, setOf("formatVersion", "name", "description", "rows", "columns", "blockedCells", "blockPool"))
        require(number(j, "formatVersion", 1..1) == 1)
        val name = text(j, "name", 24, false)
        val description = text(j, "description", 500, true)
        val rows = number(j, "rows", 3..10)
        val columns = number(j, "columns", 3..10)
        val blocked = cells(j.getJSONArray("blockedCells"), 100, rows, columns)
        require(blocked.size < rows * columns) { "Map has no playable cells" }
        val pool = j.getJSONArray("blockPool")
        require(pool.length() in 1..64) { "Map needs between 1 and 64 blocks" }
        val blocks = (0 until pool.length()).map { i ->
            val b = pool.getJSONObject(i)
            keys(b, setOf("id", "name", "cells", "colorVariant", "spawnChanceTenthsPercent"))
            val id = text(b, "id", 64, false)
            require(id.matches(Regex("[A-Za-z0-9_-]{1,64}"))) { "Invalid block ID" }
            val shape = cells(b.getJSONArray("cells"), 16, 4, 4)
            require(shape.isNotEmpty() && shape == CustomBlockRules.normalize(shape)) { "Invalid block shape" }
            MapBlockDefinition(id, text(b, "name", 64, false), shape,
                number(b, "spawnChanceTenthsPercent", 0..1000), number(b, "colorVariant", 1..4))
        }
        require(MapBlockPoolRules.validationError(blocks, rows, columns) == null) { "Invalid block pool" }
        val board = Board.empty(rows, columns, blocked, true)
        require(blocks.filter { it.spawnChanceTenthsPercent > 0 }.all { block ->
            (0 until rows).any { r -> (0 until columns).any { c -> GameEngine.canPlace(board, block.toPiece(), r, c) } }
        }) { "A spawnable block cannot fit this layout" }
        return CommunityMapDocument(name, description, CustomMapDesign(rows, columns, blocked, blocks))
    }

    private fun keys(j: JSONObject, expected: Set<String>) {
        require(j.keys().asSequence().toSet() == expected) { "Unknown or missing map fields" }
    }
    private fun number(j: JSONObject, key: String, range: IntRange): Int {
        val n = j.get(key)
        require(n is Number && n.toDouble().isFinite() && n.toDouble() == n.toInt().toDouble() && n.toInt() in range) { "Invalid $key" }
        return n.toInt()
    }
    private fun text(j: JSONObject, key: String, max: Int, empty: Boolean): String {
        val s = j.get(key)
        require(s is String && s.length <= max && s == s.trim() && (empty || s.isNotEmpty()) &&
            s.none { it.code < 32 || it.code == 127 } && StandardCharsets.UTF_8.newEncoder().canEncode(s)) { "Invalid $key" }
        return s
    }
    private fun cells(a: JSONArray, max: Int, rows: Int, columns: Int): Set<Cell> {
        require(a.length() <= max) { "Too many cells" }
        val list = (0 until a.length()).map { i -> a.getJSONObject(i).let { j ->
            keys(j, setOf("row", "col")); Cell(number(j, "row", 0 until rows), number(j, "col", 0 until columns))
        } }
        require(list.toSet().size == list.size) { "Duplicate cells" }
        return list.toSet()
    }
    private fun cellsJson(cells: Set<Cell>) = JSONArray().also { a ->
        cells.sortedWith(compareBy(Cell::row, Cell::col)).forEach { a.put(JSONObject().put("row", it.row).put("col", it.col)) }
    }
    private fun quote(s: String): String = buildString {
        append('"'); s.forEach { c -> append(when (c) {
            '"' -> "\\\""; '\\' -> "\\\\"; '\b' -> "\\b"; '\t' -> "\\t"; '\n' -> "\\n"; '\r' -> "\\r"; '\u000c' -> "\\f"
            else -> if (c.code < 32) "\\u%04x".format(c.code) else c.toString()
        }) }; append('"')
    }
    private fun canonical(v: Any): String = when (v) {
        is JSONObject -> v.keys().asSequence().sorted().joinToString(",", "{", "}") { quote(it) + ":" + canonical(v.get(it)) }
        is JSONArray -> (0 until v.length()).joinToString(",", "[", "]") { canonical(v.get(it)) }
        is String -> quote(v)
        else -> v.toString()
    }
}
