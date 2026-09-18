package com.example.gridfall.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.gridfall.game.Cell
import com.example.gridfall.game.CustomBlockRules
import com.example.gridfall.game.MapBlockDefinition
import com.example.gridfall.game.MapBlockPoolRules
import com.example.gridfall.ui.theme.LocalGridfallColors
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun MapBlockListScreen(
    blocks: List<MapBlockDefinition>,
    onBlocksChanged: (List<MapBlockDefinition>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalGridfallColors.current
    var editingBlock by remember { mutableStateOf<MapBlockDefinition?>(null) }
    var addingBlock by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<MapBlockDefinition?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.backgroundTop, theme.backgroundBottom)))
            .infernoAppTexture(theme)
            .retroAppTexture(theme)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onBack) { Text("Back") }
                Text(
                    text = "Block List",
                    color = theme.textPrimary,
                    style = MaterialTheme.typography.headlineSmall.retroText(theme)
                )
                Button(
                    onClick = { addingBlock = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.button,
                        contentColor = theme.textPrimary
                    )
                ) { Text("+ Add") }
            }

            Text(
                text = "Every block belongs only to this map. Probabilities always rebalance to exactly 100%.",
                color = theme.textSecondary,
                style = MaterialTheme.typography.bodyMedium.retroText(theme)
            )

            Text(
                text = "${blocks.size} blocks · ${formatTenthsPercent(blocks.sumOf { it.spawnChanceTenthsPercent })} total",
                color = theme.accentStrong,
                style = MaterialTheme.typography.labelLarge.retroText(theme)
            )

            blocks.forEach { block ->
                MapBlockRow(
                    block = block,
                    canDelete = blocks.size > 1,
                    onEdit = { editingBlock = block },
                    onDelete = { pendingDelete = block }
                )
            }
        }
    }

    if (addingBlock || editingBlock != null) {
        MapBlockEditorDialog(
            initialBlock = editingBlock,
            suggestedName = "Custom ${blocks.count { it.id.startsWith("custom_") } + 1}",
            onDismiss = {
                addingBlock = false
                editingBlock = null
            },
            onSave = { name, cells, probability ->
                val existing = editingBlock
                val updated = if (existing == null) {
                    MapBlockPoolRules.add(
                        blocks,
                        MapBlockDefinition(
                            id = "custom_${UUID.randomUUID()}",
                            name = name,
                            cells = cells,
                            spawnChanceTenthsPercent = probability,
                            colorVariant = ((blocks.size % 4) + 1)
                        )
                    )
                } else {
                    val replaced = blocks.map { block ->
                        if (block.id == existing.id) {
                            block.copy(name = name, cells = cells)
                        } else {
                            block
                        }
                    }
                    MapBlockPoolRules.setProbability(replaced, existing.id, probability)
                }
                onBlocksChanged(updated)
                addingBlock = false
                editingBlock = null
            }
        )
    }

    pendingDelete?.let { block ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = theme.dialogBackground,
            title = { Text("Delete ${block.name}?", color = theme.textPrimary) },
            text = {
                Text(
                    "This block will no longer spawn on this map. Its probability will be shared by the remaining blocks.",
                    color = theme.textSecondary
                )
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBlocksChanged(MapBlockPoolRules.delete(blocks, block.id))
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.danger,
                        contentColor = theme.textPrimary
                    )
                ) { Text("Delete") }
            }
        )
    }
}

@Composable
private fun MapBlockRow(
    block: MapBlockDefinition,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val theme = LocalGridfallColors.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.chipBackground.copy(alpha = 0.72f), shape)
            .border(1.dp, theme.panelBorder.copy(alpha = 0.55f), shape)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MapBlockPreview(block, Modifier.size(62.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = block.name,
                color = theme.textPrimary,
                style = MaterialTheme.typography.bodyLarge.retroText(theme)
            )
            Text(
                text = "${block.cells.size} cells · ${formatTenthsPercent(block.spawnChanceTenthsPercent)} spawn",
                color = theme.accentStrong,
                style = MaterialTheme.typography.labelMedium.retroText(theme)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit) { Text("Edit") }
                OutlinedButton(onClick = onDelete, enabled = canDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun MapBlockEditorDialog(
    initialBlock: MapBlockDefinition?,
    suggestedName: String,
    onDismiss: () -> Unit,
    onSave: (String, Set<Cell>, Int) -> Unit
) {
    val theme = LocalGridfallColors.current
    var name by remember(initialBlock, suggestedName) {
        mutableStateOf(initialBlock?.name ?: suggestedName)
    }
    var cells by remember(initialBlock) {
        mutableStateOf(initialBlock?.cells ?: setOf(Cell(0, 0)))
    }
    var probability by remember(initialBlock) {
        mutableStateOf(initialBlock?.spawnChanceTenthsPercent ?: 100)
    }
    val error = CustomBlockRules.validationError(cells)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.dialogBackground,
        title = {
            Text(
                if (initialBlock == null) "Add block" else "Edit block",
                color = theme.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    label = { Text("Block name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Tap any cells to shape the block.",
                    color = theme.textSecondary,
                    style = MaterialTheme.typography.bodySmall.retroText(theme)
                )
                CustomBlockEditorGrid(
                    selectedCells = cells,
                    onCellTapped = { cell ->
                        cells = if (cell in cells) cells - cell else cells + cell
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(max = 220.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                Text(
                    text = error ?: "${cells.size} cells selected",
                    color = if (error == null) theme.accentStrong else theme.warning,
                    style = MaterialTheme.typography.bodySmall.retroText(theme)
                )
                Text(
                    text = "Spawn probability: ${formatTenthsPercent(probability)}",
                    color = theme.textPrimary,
                    style = MaterialTheme.typography.labelLarge.retroText(theme)
                )
                Slider(
                    value = probability.toFloat(),
                    onValueChange = { probability = it.roundToInt() },
                    valueRange = 0f..1_000f
                )
                Text(
                    "The other blocks will rebalance automatically.",
                    color = theme.textSecondary,
                    style = MaterialTheme.typography.bodySmall.retroText(theme)
                )
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name.trim().ifBlank { suggestedName },
                        CustomBlockRules.normalize(cells),
                        probability
                    )
                },
                enabled = error == null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.button,
                    contentColor = theme.textPrimary
                )
            ) { Text("Save") }
        }
    )
}

@Composable
private fun MapBlockPreview(block: MapBlockDefinition, modifier: Modifier = Modifier) {
    val theme = LocalGridfallColors.current
    Canvas(modifier = modifier) {
        val normalized = CustomBlockRules.normalize(block.cells)
        val rows = (normalized.maxOfOrNull(Cell::row) ?: 0) + 1
        val columns = (normalized.maxOfOrNull(Cell::col) ?: 0) + 1
        val gap = size.minDimension * 0.045f
        val cellSize = minOf(
            (size.width - gap * (columns + 1)) / columns,
            (size.height - gap * (rows + 1)) / rows
        )
        val gridWidth = columns * cellSize + (columns - 1) * gap
        val gridHeight = rows * cellSize + (rows - 1) * gap
        val startX = (size.width - gridWidth) / 2f
        val startY = (size.height - gridHeight) / 2f
        normalized.forEach { cell ->
            val topLeft = Offset(
                startX + cell.col * (cellSize + gap),
                startY + cell.row * (cellSize + gap)
            )
            drawRoundRect(
                color = theme.accentStrong.copy(alpha = 0.78f),
                topLeft = topLeft,
                size = Size(cellSize, cellSize),
                cornerRadius = CornerRadius(cellSize * 0.17f)
            )
            drawRoundRect(
                color = theme.accentStrong,
                topLeft = topLeft,
                size = Size(cellSize, cellSize),
                cornerRadius = CornerRadius(cellSize * 0.17f),
                style = Stroke(width = (cellSize * 0.06f).coerceAtLeast(1f))
            )
        }
    }
}

private fun formatTenthsPercent(value: Int): String {
    return String.format(Locale.US, "%.1f%%", value / 10.0)
}
