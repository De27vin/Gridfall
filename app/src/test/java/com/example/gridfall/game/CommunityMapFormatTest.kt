package com.example.gridfall.game

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class CommunityMapFormatTest {
    private val fixture get() = listOf(File("../shared/community-map-v1.json"), File("shared/community-map-v1.json"))
        .first { it.isFile }.readText().trim()

    @Test fun crossLanguageGoldenRoundTrip() {
        val d = CommunityMapFormat.decode(fixture)
        assertEquals(fixture, CommunityMapFormat.export(d.name, d.description, d.design))
        val state = GameEngine.createCustomState(d.design)
        assertTrue(state.board.isCustom)
        // Generation intentionally has a random bomb chance, even in a single-shape pool.
        // This format/normal-placement assertion must choose its piece explicitly.
        val normalState = state.copy(currentPieces = listOf(d.design.blockPool.single().toPiece()) + state.currentPieces.drop(1))
        val played = GameEngine.placePiece(normalState, 0, 1, 1)
        assertTrue(played.board.get(1, 1) != 0)
        assertTrue(played.board.isCustom)
        assertEquals(d.design.blockedCells, played.board.blockedCells)
        // Reproduce the former flaky outcome deliberately: bombs clear instead of occupying.
        val bombState = state.copy(
            currentPieces = listOf(PieceLibrary.bombPiece) + state.currentPieces.drop(1),
            board = state.board.fill(1, 2)
        )
        val bombed = GameEngine.placePiece(bombState, 0, 1, 1)
        assertEquals(0, bombed.board.get(1, 1))
        assertEquals(0, bombed.board.get(1, 2))
        assertTrue(0 in bombed.usedPieceIndices)
        assertTrue(bombed.board.isCustom)
        assertEquals(d.design.blockedCells, bombed.board.blockedCells)
    }
    @Test fun preservesPoolOrderProbabilitiesColorsAndDisconnectedShapes() {
        val pool = listOf(MapBlockDefinition("b", "B", setOf(Cell(1, 1), Cell(2, 2)), 123, 4),
            MapBlockDefinition("a", "A", setOf(Cell(0, 0)), 877, 2))
        val d = CustomMapDesign(5, 7, emptySet(), pool)
        val decoded = CommunityMapFormat.decode(CommunityMapFormat.export("Map", design = d))
        assertEquals(listOf("b", "a"), decoded.design.blockPool.map { it.id })
        assertEquals(pool.map { it.spawnChanceTenthsPercent }, decoded.design.blockPool.map { it.spawnChanceTenthsPercent })
        assertEquals(4, decoded.design.blockPool.first().colorVariant)
        assertEquals(setOf(Cell(0, 0), Cell(1, 1)), decoded.design.blockPool.first().cells)
    }
    @Test fun rejectsUnsafePayloadsWithoutTouchingLocalSaves() {
        listOf("{", " ".repeat(65537), fixture.replace("\"formatVersion\":1", "\"formatVersion\":2"),
            fixture.replace("\"rows\":4", "\"rows\":\"4\""),
            fixture.replace("\"spawnChanceTenthsPercent\":1000", "\"spawnChanceTenthsPercent\":999"))
            .forEach { assertTrue(runCatching { CommunityMapFormat.decode(it) }.isFailure) }
        val impossible = CustomMapDesign(4, 4, (0..3).flatMap { r -> (0..3).map { Cell(r, it) } }.toSet())
        assertTrue(runCatching { CommunityMapFormat.export("Blocked", design = impossible) }.isFailure)
    }
}
