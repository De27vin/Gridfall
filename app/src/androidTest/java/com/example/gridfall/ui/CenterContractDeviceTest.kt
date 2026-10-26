package com.example.gridfall.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.example.gridfall.game.Board
import com.example.gridfall.game.Cell
import com.example.gridfall.game.Contract
import com.example.gridfall.game.ContractState
import com.example.gridfall.game.ContractType
import com.example.gridfall.ui.theme.GridfallTheme
import com.example.gridfall.ui.theme.GridfallThemeMode
import com.example.gridfall.ui.theme.LocalGridfallColors
import com.example.gridfall.ui.theme.colorsForThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.math.roundToInt

/** Compare real canvas pixels against an unhighlighted board, including boundary cells. */
class CenterContractDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun customFourByFourShowsExactlyFourTargets() =
        assertPaintedCenter(4, 4, 1..2, 1..2, ContractType.TouchCenter)

    @Test fun customFourByFourShowsExactlyFourAvoidanceWarnings() =
        assertPaintedCenter(4, 4, 1..2, 1..2, ContractType.AvoidCenterArea)

    @Test fun fullRouteUsesTheSameFourCenterTargets() =
        assertPaintedCenter(4, 4, 1..2, 1..2, ContractType.TouchEdgeAndCenter)

    @Test fun classicEightByEightKeepsItsFourByFourCenter() =
        assertPaintedCenter(8, 8, 2..5, 2..5, ContractType.TouchCenter)

    @Test fun oddNineByNineHighlightsASymmetricCenter() =
        assertPaintedCenter(9, 9, 3..5, 3..5, ContractType.TouchCenter)

    @Test fun rectangleUsesItsActualWidthAndHeight() =
        assertPaintedCenter(4, 7, 1..2, 2..4, ContractType.AvoidCenterArea)

    private fun assertPaintedCenter(
        rows: Int,
        columns: Int,
        centerRows: IntRange,
        centerColumns: IntRange,
        type: ContractType
    ) {
        var showHints by mutableStateOf(false)
        var geometry: BoardLayoutInfo? = null
        val board = Board.empty(rows, columns, isCustom = rows != 8 || columns != 8)
        val state = ContractState(
            activeContract = Contract(type.name, type.name, type.name, 100, 65, type),
            isAccepted = true,
            usedEdge = true
        )
        compose.setContent {
            GridfallTheme {
                CompositionLocalProvider(LocalGridfallColors provides colorsForThemeMode(GridfallThemeMode.PremiumTactical)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BoardCanvas(
                            board = board,
                            modifier = Modifier.size(280.dp, 280.dp * rows / columns).testTag("center-board"),
                            contractTargetCells = if (showHints) contractTargetCells(state, rows, columns) else emptySet(),
                            contractWarningCells = if (showHints) contractWarningCells(state, rows, columns) else emptySet(),
                            onBoardLayoutChanged = { geometry = it }
                        )
                    }
                }
            }
        }
        val node = compose.onNodeWithTag("center-board")
        val before = node.captureToImage().toPixelMap()
        compose.runOnIdle { showHints = true }
        val after = node.captureToImage().toPixelMap()
        val info = requireNotNull(geometry)
        val origin = info.topLeft - node.fetchSemanticsNode().boundsInRoot.topLeft
        for (row in 0 until rows) for (col in 0 until columns) {
            val center = origin + Offset(
                info.spacingPx + col * (info.cellSizePx + info.spacingPx) + info.cellSizePx / 2f,
                info.spacingPx + row * (info.cellSizePx + info.spacingPx) + info.cellSizePx / 2f
            )
            val x = center.x.roundToInt()
            val y = center.y.roundToInt()
            assertEquals("$rows×$columns $type: painted cell ${Cell(row, col)}",
                row in centerRows && col in centerColumns,
                before[x, y] != after[x, y])
        }
    }
}
