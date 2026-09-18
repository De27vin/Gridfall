package com.example.gridfall.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.gridfall.game.Cell
import com.example.gridfall.game.CustomBlockRules
import com.example.gridfall.game.CustomMapDesign
import com.example.gridfall.game.CustomMapRules
import com.example.gridfall.game.MapBlockDefinition
import com.example.gridfall.game.MapBlockPoolRules
import com.example.gridfall.ui.theme.LocalGridfallColors

@Composable
fun CustomMapEditorScreen(
    initialRows: Int,
    initialColumns: Int,
    initialBlockedCells: Set<Cell>,
    initialBlockPool: List<MapBlockDefinition> = MapBlockPoolRules.defaultPool(),
    initialMapName: String? = null,
    suggestedMapName: String = "Custom Map",
    onBack: () -> Unit,
    onStartMap: (CustomMapDesign) -> Unit,
    onSaveMap: (String, CustomMapDesign) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalGridfallColors.current
    var rows by remember(initialRows) { mutableStateOf(initialRows) }
    var columns by remember(initialColumns) { mutableStateOf(initialColumns) }
    var blockedCells by remember(initialBlockedCells) { mutableStateOf(initialBlockedCells) }
    var blockPool by remember(initialBlockPool) {
        mutableStateOf(MapBlockPoolRules.normalize(initialBlockPool))
    }
    var showBlockList by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var mapName by remember(initialMapName, suggestedMapName) {
        mutableStateOf(initialMapName ?: suggestedMapName)
    }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    val mapValidationError = CustomMapRules.validationError(blockedCells, rows, columns)
    val blockPoolValidationError = MapBlockPoolRules.validationError(blockPool, rows, columns)
    val validationError = mapValidationError ?: blockPoolValidationError
    val playableCount = rows * columns - blockedCells.size

    if (showBlockList) {
        MapBlockListScreen(
            blocks = blockPool,
            boardRows = rows,
            boardColumns = columns,
            onBlocksChanged = { updatedBlockPool ->
                blockPool = updatedBlockPool
                val savedName = mapName.trim().ifBlank { suggestedMapName }
                mapName = savedName
                onSaveMap(
                    savedName,
                    CustomMapDesign(
                        rows = rows,
                        columns = columns,
                        blockedCells = blockedCells,
                        blockPool = updatedBlockPool
                    )
                )
                savedMessage = "Block list saved automatically"
            },
            onBack = { showBlockList = false },
            modifier = modifier
        )
        return
    }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onBack) { Text("Back") }
                Text(
                    text = "Map Editor",
                    color = theme.textPrimary,
                    style = MaterialTheme.typography.headlineSmall.retroText(theme)
                )
                OutlinedButton(onClick = { showResetConfirmation = true }) { Text("Reset") }
            }

            Text(
                text = "Tap a field to toggle it. A red X means that field is blocked.",
                color = theme.textSecondary,
                style = MaterialTheme.typography.bodyMedium.retroText(theme)
            )

            Text(
                text = "${columns}×$rows · $playableCount fields playable",
                color = theme.accentStrong,
                style = MaterialTheme.typography.labelLarge.retroText(theme)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LaneControl(
                    label = "Horizontal",
                    canRemove = rows > CustomMapRules.MIN_BOARD_SIZE,
                    canAdd = rows < CustomMapRules.MAX_ROWS,
                    onRemove = {
                        rows -= 1
                        blockedCells = blockedCells.filter { it.row < rows }.toSet()
                    },
                    onAdd = { rows += 1 },
                    modifier = Modifier.weight(1f)
                )
                LaneControl(
                    label = "Vertical",
                    canRemove = columns > CustomMapRules.MIN_BOARD_SIZE,
                    canAdd = columns < CustomMapRules.MAX_COLUMNS,
                    onRemove = {
                        columns -= 1
                        blockedCells = blockedCells.filter { it.col < columns }.toSet()
                    },
                    onAdd = { columns += 1 },
                    modifier = Modifier.weight(1f)
                )
            }

            CustomMapEditorGrid(
                rows = rows,
                columns = columns,
                blockedCells = blockedCells,
                onCellTapped = { cell ->
                    blockedCells = if (cell in blockedCells) blockedCells - cell else blockedCells + cell
                },
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Block List",
                    color = theme.textPrimary,
                    style = MaterialTheme.typography.titleMedium.retroText(theme)
                )
                Text(
                    text = "${blockPool.size} blocks · Edit shapes and exact spawn probabilities for this map.",
                    color = theme.textSecondary,
                    style = MaterialTheme.typography.bodySmall.retroText(theme)
                )
                OutlinedButton(
                    onClick = { showBlockList = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open Block List") }
                blockPoolValidationError?.let { error ->
                    Text(
                        text = error,
                        color = theme.warning,
                        style = MaterialTheme.typography.bodySmall.retroText(theme)
                    )
                }
            }

            Text(
                text = validationError ?: "Custom maps are unranked in this prototype.",
                color = if (validationError == null) theme.success else theme.warning,
                style = MaterialTheme.typography.bodySmall.retroText(theme)
            )

            savedMessage?.let { message ->
                Text(
                    text = message,
                    color = theme.accentStrong,
                    style = MaterialTheme.typography.bodySmall.retroText(theme)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showSaveDialog = true },
                    enabled = validationError == null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (initialMapName == null) "Save Map" else "Update Map")
                }
                Button(
                    onClick = {
                        onStartMap(
                            CustomMapDesign(
                                rows,
                                columns,
                                blockedCells,
                                blockPool
                            )
                        )
                    },
                    enabled = validationError == null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.button,
                        contentColor = theme.textPrimary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start Map")
                }
            }
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            containerColor = theme.dialogBackground,
            title = { Text("Reset custom map?", color = theme.textPrimary) },
            text = {
                Text(
                    "This restores the blank 8×8 editor and its default block list.",
                    color = theme.textSecondary
                )
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetConfirmation = false }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        blockedCells = emptySet()
                        blockPool = MapBlockPoolRules.defaultPool()
                        rows = CustomMapRules.BOARD_SIZE
                        columns = CustomMapRules.BOARD_SIZE
                        showResetConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.danger,
                        contentColor = theme.textPrimary
                    )
                ) { Text("Reset") }
            }
        )
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = theme.dialogBackground,
            title = {
                Text(
                    if (initialMapName == null) "Save custom map" else "Update custom map",
                    color = theme.textPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = mapName,
                    onValueChange = { mapName = it.take(24) },
                    label = { Text("Map name") },
                    singleLine = true
                )
            },
            dismissButton = {
                OutlinedButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val savedName = mapName.trim().ifBlank { suggestedMapName }
                        mapName = savedName
                        onSaveMap(
                            savedName,
                            CustomMapDesign(
                                rows,
                                columns,
                                blockedCells,
                                blockPool
                            )
                        )
                        savedMessage = "Saved as $savedName"
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.button,
                        contentColor = theme.textPrimary
                    )
                ) { Text("Save") }
            }
        )
    }
}

@Composable
private fun LaneControl(
    label: String,
    canRemove: Boolean,
    canAdd: Boolean,
    onRemove: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalGridfallColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedButton(
            onClick = onRemove,
            enabled = canRemove,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = theme.chipBackground,
                contentColor = theme.accentStrong
            ),
            modifier = Modifier.weight(1f)
        ) { Text("−", style = MaterialTheme.typography.titleMedium.retroText(theme)) }
        Box(
            modifier = Modifier.weight(2f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                color = theme.textPrimary,
                style = MaterialTheme.typography.labelMedium.retroText(theme)
            )
        }
        OutlinedButton(
            onClick = onAdd,
            enabled = canAdd,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = theme.chipBackground,
                contentColor = theme.accentStrong
            ),
            modifier = Modifier.weight(1f)
        ) { Text("+", style = MaterialTheme.typography.titleMedium.retroText(theme)) }
    }
}

@Composable
private fun CustomMapEditorGrid(
    rows: Int,
    columns: Int,
    blockedCells: Set<Cell>,
    onCellTapped: (Cell) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalGridfallColors.current
    val shape = RoundedCornerShape(18.dp)
    BoxWithConstraints(modifier = modifier) {
        val previewHeight = minOf(400.dp, maxWidth * (rows.toFloat() / columns.toFloat()))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(previewHeight)
                .background(theme.boardInner, shape)
                .border(1.dp, theme.panelBorder, shape)
                .padding(8.dp)
                .pointerInput(blockedCells) {
                detectTapGestures { offset ->
                    val gap = size.width * 0.012f
                    val cellSize = minOf(
                        (size.width - gap * (columns + 1)) / columns,
                        (size.height - gap * (rows + 1)) / rows
                    )
                    val gridWidth = gap * (columns + 1) + cellSize * columns
                    val gridHeight = gap * (rows + 1) + cellSize * rows
                    val startX = (size.width - gridWidth) / 2f + gap
                    val startY = (size.height - gridHeight) / 2f + gap
                    val col = ((offset.x - startX) / (cellSize + gap)).toInt()
                    val row = ((offset.y - startY) / (cellSize + gap)).toInt()
                    if (row in 0 until rows && col in 0 until columns) {
                        onCellTapped(Cell(row, col))
                    }
                }
                }
        ) {
        val gap = size.width * 0.012f
        val cellSize = minOf(
            (size.width - gap * (columns + 1)) / columns,
            (size.height - gap * (rows + 1)) / rows
        )
        val gridWidth = gap * (columns + 1) + cellSize * columns
        val gridHeight = gap * (rows + 1) + cellSize * rows
        val startX = (size.width - gridWidth) / 2f + gap
        val startY = (size.height - gridHeight) / 2f + gap
        repeat(rows) { row ->
            repeat(columns) { col ->
                val blocked = Cell(row, col) in blockedCells
                val topLeft = Offset(
                    startX + col * (cellSize + gap),
                    startY + row * (cellSize + gap)
                )
                drawRoundRect(
                    color = theme.emptyCell,
                    topLeft = topLeft,
                    size = Size(cellSize, cellSize),
                    cornerRadius = CornerRadius(cellSize * 0.16f)
                )
                drawRoundRect(
                    color = if (blocked) theme.danger.copy(alpha = 0.72f) else theme.accentStrong.copy(alpha = 0.54f),
                    topLeft = topLeft,
                    size = Size(cellSize, cellSize),
                    cornerRadius = CornerRadius(cellSize * 0.16f),
                    style = Stroke(width = (cellSize * 0.055f).coerceAtLeast(1f))
                )
                if (blocked) {
                    val inset = cellSize * 0.24f
                    drawLine(
                        color = theme.danger,
                        start = topLeft + Offset(inset, inset),
                        end = topLeft + Offset(cellSize - inset, cellSize - inset),
                        strokeWidth = (cellSize * 0.10f).coerceAtLeast(2f)
                    )
                    drawLine(
                        color = theme.danger,
                        start = topLeft + Offset(cellSize - inset, inset),
                        end = topLeft + Offset(inset, cellSize - inset),
                        strokeWidth = (cellSize * 0.10f).coerceAtLeast(2f)
                    )
                }
            }
        }
        }
    }
}

@Composable
internal fun CustomBlockEditorGrid(
    selectedCells: Set<Cell>,
    onCellTapped: (Cell) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalGridfallColors.current
    val shape = RoundedCornerShape(18.dp)
    Canvas(
        modifier = modifier
            .background(theme.boardInner, shape)
            .border(1.dp, theme.panelBorder, shape)
            .padding(10.dp)
            .pointerInput(selectedCells) {
                detectTapGestures { offset ->
                    val gap = size.width * 0.035f
                    val cellSize = (size.width - gap * (CustomBlockRules.EDITOR_SIZE + 1)) /
                        CustomBlockRules.EDITOR_SIZE
                    val col = ((offset.x - gap) / (cellSize + gap)).toInt()
                    val row = ((offset.y - gap) / (cellSize + gap)).toInt()
                    if (row in 0 until CustomBlockRules.EDITOR_SIZE &&
                        col in 0 until CustomBlockRules.EDITOR_SIZE
                    ) {
                        onCellTapped(Cell(row, col))
                    }
                }
            }
    ) {
        val gap = size.width * 0.035f
        val cellSize = (size.width - gap * (CustomBlockRules.EDITOR_SIZE + 1)) /
            CustomBlockRules.EDITOR_SIZE
        repeat(CustomBlockRules.EDITOR_SIZE) { row ->
            repeat(CustomBlockRules.EDITOR_SIZE) { col ->
                val selected = Cell(row, col) in selectedCells
                val topLeft = Offset(
                    gap + col * (cellSize + gap),
                    gap + row * (cellSize + gap)
                )
                drawRoundRect(
                    color = if (selected) theme.accentStrong.copy(alpha = 0.78f) else theme.emptyCell,
                    topLeft = topLeft,
                    size = Size(cellSize, cellSize),
                    cornerRadius = CornerRadius(cellSize * 0.18f)
                )
                drawRoundRect(
                    color = if (selected) theme.accentStrong else theme.panelBorder,
                    topLeft = topLeft,
                    size = Size(cellSize, cellSize),
                    cornerRadius = CornerRadius(cellSize * 0.18f),
                    style = Stroke(width = (cellSize * 0.06f).coerceAtLeast(1f))
                )
            }
        }
    }
}
