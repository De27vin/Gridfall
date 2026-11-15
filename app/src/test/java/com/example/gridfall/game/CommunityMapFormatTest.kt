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
        assertTrue(GameEngine.createCustomState(d.design).board.isCustom)
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
