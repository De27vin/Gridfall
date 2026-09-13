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
import com.example.gridfall.game.CustomMapDesign
import com.example.gridfall.game.CustomMapRules
import com.example.gridfall.ui.theme.LocalGridfallColors

@Composable
fun CustomMapEditorScreen(
    initialRows: Int,
    initialColumns: Int,
    initialBlockedCells: Set<Cell>,
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
    var showResetConfirmation by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var mapName by remember(initialMapName, suggestedMapName) {
        mutableStateOf(initialMapName ?: suggestedMapName)
    }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    val validationError = CustomMapRules.validationError(blockedCells, rows, columns)
    val playableCount = rows * columns - blockedCells.size

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
                    onClick = { onStartMap(CustomMapDesign(rows, columns, blockedCells)) },
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
                    "This removes every blocked field and restores the blank 8×8 editor.",
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
                        onSaveMap(savedName, CustomMapDesign(rows, columns, blockedCells))
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
