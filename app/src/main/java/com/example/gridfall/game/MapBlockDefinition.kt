package com.example.gridfall.game

data class MapBlockDefinition(
    val id: String,
    val name: String,
    val cells: Set<Cell>,
    val spawnChanceTenthsPercent: Int,
    val colorVariant: Int = 1
) {
    fun toPiece(): Piece {
        return Piece(
            id = "map_block_$id",
            cells = CustomBlockRules.normalize(cells).sortedWith(compareBy(Cell::row, Cell::col)),
            rarity = PieceRarity.Common,
            colorVariant = colorVariant.coerceIn(1, 4),
            spawnWeight = spawnChanceTenthsPercent.coerceIn(0, 1_000)
        )
    }
}

object MapBlockPoolRules {
    const val TOTAL_TENTHS_PERCENT = 1_000

    fun defaultPool(): List<MapBlockDefinition> {
        val chances = distribute(TOTAL_TENTHS_PERCENT, List(PieceLibrary.starterPieces.size) { 1 })
        return PieceLibrary.starterPieces.mapIndexed { index, piece ->
            MapBlockDefinition(
                id = piece.id,
                name = piece.id
                    .split('_')
                    .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) },
                cells = piece.cells.toSet(),
                spawnChanceTenthsPercent = chances[index],
                colorVariant = piece.colorVariant
            )
        }
    }

    fun validationError(blocks: List<MapBlockDefinition>): String? {
        return validationErrorInternal(blocks, boardRows = null, boardColumns = null)
    }

    fun validationError(
        blocks: List<MapBlockDefinition>,
        boardRows: Int,
        boardColumns: Int
    ): String? {
        return validationErrorInternal(blocks, boardRows, boardColumns)
    }

    private fun validationErrorInternal(
        blocks: List<MapBlockDefinition>,
        boardRows: Int?,
        boardColumns: Int?
    ): String? {
        if (blocks.isEmpty()) return "Keep at least one block in the map's block list."
        if (blocks.map(MapBlockDefinition::id).toSet().size != blocks.size) {
            return "Every block needs a unique ID."
        }
        blocks.forEach { block ->
            val blockError = if (boardRows != null && boardColumns != null) {
                CustomBlockRules.validationError(block.cells, boardRows, boardColumns)
            } else {
                CustomBlockRules.validationError(block.cells)
            }
            blockError?.let { error ->
                return "${block.name}: $error"
            }
            if (block.spawnChanceTenthsPercent !in 0..TOTAL_TENTHS_PERCENT) {
                return "${block.name}: spawn probability must be between 0% and 100%."
            }
        }
        if (blocks.sumOf(MapBlockDefinition::spawnChanceTenthsPercent) != TOTAL_TENTHS_PERCENT) {
            return "Block spawn probabilities must total 100%."
        }
        return null
    }

    fun normalize(blocks: List<MapBlockDefinition>): List<MapBlockDefinition> {
        if (blocks.isEmpty()) return emptyList()
        val chances = distribute(
            TOTAL_TENTHS_PERCENT,
            blocks.map { it.spawnChanceTenthsPercent.coerceAtLeast(0) }
        )
        return blocks.mapIndexed { index, block ->
            block.copy(
                cells = CustomBlockRules.normalize(block.cells),
                spawnChanceTenthsPercent = chances[index],
                colorVariant = block.colorVariant.coerceIn(1, 4)
            )
        }
    }

    fun setProbability(
        blocks: List<MapBlockDefinition>,
        blockId: String,
        probability: Int
    ): List<MapBlockDefinition> {
        if (blocks.none { it.id == blockId }) return normalize(blocks)
        if (blocks.size == 1) {
            return listOf(blocks.first().copy(spawnChanceTenthsPercent = TOTAL_TENTHS_PERCENT))
        }

        val target = probability.coerceIn(0, TOTAL_TENTHS_PERCENT)
        val others = blocks.filterNot { it.id == blockId }
        val otherChances = distribute(
            TOTAL_TENTHS_PERCENT - target,
            others.map { it.spawnChanceTenthsPercent.coerceAtLeast(0) }
        )
        var otherIndex = 0
        return blocks.map { block ->
            if (block.id == blockId) {
                block.copy(spawnChanceTenthsPercent = target)
            } else {
                block.copy(spawnChanceTenthsPercent = otherChances[otherIndex++])
            }
        }
    }

    fun add(
        blocks: List<MapBlockDefinition>,
        block: MapBlockDefinition
    ): List<MapBlockDefinition> {
        val uniqueBlock = block.copy(
            id = block.id.takeIf { id -> id.isNotBlank() && blocks.none { it.id == id } }
                ?: "custom_${System.currentTimeMillis()}"
        )
        return setProbability(
            normalize(blocks) + uniqueBlock.copy(spawnChanceTenthsPercent = 0),
            uniqueBlock.id,
            block.spawnChanceTenthsPercent.coerceIn(0, TOTAL_TENTHS_PERCENT)
        )
    }

    fun delete(blocks: List<MapBlockDefinition>, blockId: String): List<MapBlockDefinition> {
        return normalize(blocks.filterNot { it.id == blockId })
    }

    private fun distribute(total: Int, weights: List<Int>): List<Int> {
        if (weights.isEmpty()) return emptyList()
        val safeWeights = if (weights.sum() > 0) weights else List(weights.size) { 1 }
        val weightTotal = safeWeights.sum().coerceAtLeast(1)
        val values = safeWeights.map { total * it / weightTotal }.toMutableList()
        var remainder = total - values.sum()
        var index = 0
        while (remainder > 0) {
            values[index % values.size] += 1
            index += 1
            remainder -= 1
        }
        return values
    }
}
