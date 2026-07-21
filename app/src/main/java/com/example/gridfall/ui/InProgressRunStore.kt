package com.example.gridfall.ui

import android.content.Context
import com.example.gridfall.game.Board
import com.example.gridfall.game.Cell
import com.example.gridfall.game.Contract
import com.example.gridfall.game.ContractState
import com.example.gridfall.game.ContractType
import com.example.gridfall.game.GameState
import com.example.gridfall.game.JokerType
import com.example.gridfall.game.Piece
import com.example.gridfall.game.PieceEffect
import com.example.gridfall.game.PieceRarity
import com.example.gridfall.game.RiskSpinMemoryField
import com.example.gridfall.game.RiskSpinMemorySession
import com.example.gridfall.game.RiskSpinOption
import com.example.gridfall.game.RiskSpinOutcome
import com.example.gridfall.game.RiskSpinState
import com.example.gridfall.game.RunStats
import org.json.JSONArray
import org.json.JSONObject

data class SavedInProgressRun(
    val gameState: GameState,
    val riskSpinMemorySession: RiskSpinMemorySession? = null,
    val riskSpinPaidCost: Int? = null,
    val savedAtEpochMillis: Long = System.currentTimeMillis()
)

class InProgressRunStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): SavedInProgressRun? {
        val rawJson = preferences.getString(KEY_SAVED_RUN, null) ?: return null
        return InProgressRunJson.decode(rawJson)
    }

    fun save(savedRun: SavedInProgressRun) {
        preferences.edit()
            .putString(KEY_SAVED_RUN, InProgressRunJson.encode(savedRun))
            .commit()
    }

    fun clear() {
        preferences.edit()
            .remove(KEY_SAVED_RUN)
            .commit()
    }

    private companion object {
        const val PREFS_NAME = "gridfall_in_progress_run"
        const val KEY_SAVED_RUN = "saved_run"
    }
}

object InProgressRunJson {
    private const val VERSION = 1

    fun hasProgress(state: GameState, memorySession: RiskSpinMemorySession? = null): Boolean {
        return state.score > 0 ||
            !state.board.isEmpty() ||
            state.usedPieceIndices.isNotEmpty() ||
            state.riskSpinState.inventory.isNotEmpty() ||
            state.runStats.linesCleared > 0 ||
            state.runStats.contractsCompleted > 0 ||
            state.runStats.bombsUsed > 0 ||
            state.runStats.megaBombsUsed > 0 ||
            memorySession != null
    }

    fun encode(savedRun: SavedInProgressRun): String {
        return JSONObject()
            .put("version", VERSION)
            .put("savedAt", savedRun.savedAtEpochMillis)
            .put("gameState", savedRun.gameState.toJson())
            .put("riskSpinMemorySession", savedRun.riskSpinMemorySession?.toJson() ?: JSONObject.NULL)
            .put("riskSpinPaidCost", savedRun.riskSpinPaidCost ?: JSONObject.NULL)
            .toString()
    }

    fun decode(rawJson: String): SavedInProgressRun? {
        return runCatching {
            val root = JSONObject(rawJson)
            if (root.optInt("version") != VERSION) return null
            val state = root.optJSONObject("gameState")?.toGameState() ?: return null
            if (state.isGameOver) return null
            SavedInProgressRun(
                gameState = state,
                riskSpinMemorySession = root.optJSONObject("riskSpinMemorySession")?.toMemorySession(),
                riskSpinPaidCost = root.optNullableInt("riskSpinPaidCost"),
                savedAtEpochMillis = root.optLong("savedAt", System.currentTimeMillis())
            )
        }.getOrNull()
    }

    private fun GameState.toJson(includeRevertSnapshot: Boolean = true): JSONObject {
        return JSONObject()
            .put("board", board.toJson())
            .put("pieces", JSONArray().also { array -> currentPieces.forEach { array.put(it.toJson()) } })
            .put("usedPieceIndices", JSONArray().also { array -> usedPieceIndices.sorted().forEach(array::put) })
            .put("score", score)
            .put("maxScoreReached", maxScoreReached)
            .put("combo", combo)
            .put("isGameOver", isGameOver)
            .put("contractState", contractState.toJson())
            .put("riskSpinState", riskSpinState.toJson(includeRevertSnapshot))
            .put("runStats", runStats.toJson())
    }

    private fun JSONObject.toGameState(allowRevertSnapshot: Boolean = true): GameState? {
        val board = optJSONArray("board")?.toBoard() ?: return null
        val pieces = optJSONArray("pieces")?.toPieces() ?: return null
        val usedIndices = optJSONArray("usedPieceIndices")
            ?.let { array -> (0 until array.length()).mapTo(mutableSetOf()) { array.optInt(it) } }
            ?: emptySet()
        if (usedIndices.any { it !in pieces.indices }) return null

        return GameState(
            board = board,
            currentPieces = pieces,
            usedPieceIndices = usedIndices,
            score = optInt("score").coerceAtLeast(0),
            maxScoreReached = maxOf(
                optInt("maxScoreReached", optInt("score")).coerceAtLeast(0),
                optInt("score").coerceAtLeast(0)
            ),
            combo = optInt("combo").coerceAtLeast(0),
            isGameOver = optBoolean("isGameOver"),
            contractState = optJSONObject("contractState")?.toContractState() ?: ContractState(),
            riskSpinState = optJSONObject("riskSpinState")?.toRiskSpinState(allowRevertSnapshot)
                ?: RiskSpinState(),
            runStats = optJSONObject("runStats")?.toRunStats() ?: return null
        )
    }

    private fun Board.toJson(): JSONArray {
        return JSONArray().also { boardArray ->
            cells.forEach { row ->
                boardArray.put(JSONArray().also { rowArray -> row.forEach(rowArray::put) })
            }
        }
    }

    private fun JSONArray.toBoard(): Board? {
        if (length() != Board.SIZE) return null
        val rows = (0 until length()).map { rowIndex ->
            val row = optJSONArray(rowIndex) ?: return null
            if (row.length() != Board.SIZE) return null
            List(Board.SIZE) { columnIndex -> row.optInt(columnIndex).coerceAtLeast(0) }
        }
        return Board(rows)
    }

    private fun Piece.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("cells", JSONArray().also { array ->
                cells.forEach { cell ->
                    array.put(JSONObject().put("row", cell.row).put("col", cell.col))
                }
            })
            .put("effect", effect.name)
            .put("rarity", rarity.name)
            .put("colorVariant", colorVariant)
    }

    private fun JSONArray.toPieces(): List<Piece>? {
        return (0 until length()).map { index ->
            optJSONObject(index)?.toPiece() ?: return null
        }
    }

    private fun JSONObject.toPiece(): Piece? {
        val id = optString("id").takeIf { it.isNotBlank() } ?: return null
        val cells = optJSONArray("cells")?.let { array ->
            (0 until array.length()).map { index ->
                val cell = array.optJSONObject(index) ?: return null
                Cell(cell.optInt("row"), cell.optInt("col"))
            }
        } ?: return null
        if (cells.isEmpty()) return null

        return Piece(
            id = id,
            cells = cells,
            effect = enumOrNull<PieceEffect>(optString("effect")) ?: PieceEffect.Normal,
            rarity = enumOrNull<PieceRarity>(optString("rarity")) ?: PieceRarity.Common,
            colorVariant = optInt("colorVariant", 1)
        )
    }

    private fun Contract.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("title", title)
            .put("description", description)
            .put("rewardPoints", rewardPoints)
            .put("penaltyPoints", penaltyPoints)
            .put("type", type.name)
    }

    private fun JSONObject.toContract(): Contract? {
        val id = optString("id").takeIf { it.isNotBlank() } ?: return null
        val type = enumOrNull<ContractType>(optString("type")) ?: return null
        return Contract(
            id = id,
            title = optString("title"),
            description = optString("description"),
            rewardPoints = optInt("rewardPoints").coerceAtLeast(0),
            penaltyPoints = optInt("penaltyPoints").coerceAtLeast(0),
            type = type
        )
    }

    private fun ContractState.toJson(): JSONObject {
        return JSONObject()
            .put("offeredContract", offeredContract?.toJson() ?: JSONObject.NULL)
            .put("activeContract", activeContract?.toJson() ?: JSONObject.NULL)
            .put("resolvedContract", resolvedContract?.toJson() ?: JSONObject.NULL)
            .put("isAccepted", isAccepted)
            .put("isCompleted", isCompleted)
            .put("isFailed", isFailed)
            .put("batchPlacedPieces", batchPlacedPieces)
            .put("batchClearedLines", batchClearedLines)
            .put("batchScoreGained", batchScoreGained)
            .put("usedEdge", usedEdge)
            .put("usedCenter", usedCenter)
            .put("rewardClaimed", rewardClaimed)
            .put("penaltyApplied", penaltyApplied)
            .put("batchesUntilNextOffer", batchesUntilNextOffer)
            .put("completedBatchCount", completedBatchCount)
    }

    private fun JSONObject.toContractState(): ContractState {
        return ContractState(
            offeredContract = optJSONObject("offeredContract")?.toContract(),
            activeContract = optJSONObject("activeContract")?.toContract(),
            resolvedContract = optJSONObject("resolvedContract")?.toContract(),
            isAccepted = optBoolean("isAccepted"),
            isCompleted = optBoolean("isCompleted"),
            isFailed = optBoolean("isFailed"),
            batchPlacedPieces = optInt("batchPlacedPieces").coerceAtLeast(0),
            batchClearedLines = optInt("batchClearedLines").coerceAtLeast(0),
            batchScoreGained = optInt("batchScoreGained").coerceAtLeast(0),
            usedEdge = optBoolean("usedEdge"),
            usedCenter = optBoolean("usedCenter"),
            rewardClaimed = optBoolean("rewardClaimed"),
            penaltyApplied = optBoolean("penaltyApplied"),
            batchesUntilNextOffer = optInt("batchesUntilNextOffer").coerceAtLeast(0),
            completedBatchCount = optInt("completedBatchCount").coerceAtLeast(0)
        )
    }

    private fun RiskSpinState.toJson(includeRevertSnapshot: Boolean): JSONObject {
        return JSONObject()
            .put("inventory", JSONArray().also { array -> inventory.forEach { array.put(it.name) } })
            .put("cooldownBatchesRemaining", cooldownBatchesRemaining)
            .put("hasUsedRevertSinceLastMove", hasUsedRevertSinceLastMove)
            .put(
                "previousMoveSnapshot",
                if (includeRevertSnapshot) previousMoveSnapshot?.toJson(includeRevertSnapshot = false)
                    ?: JSONObject.NULL else JSONObject.NULL
            )
    }

    private fun JSONObject.toRiskSpinState(allowRevertSnapshot: Boolean): RiskSpinState {
        val inventory = optJSONArray("inventory")
            ?.let { array ->
                (0 until array.length()).mapNotNull { index ->
                    enumOrNull<JokerType>(array.optString(index))
                }.take(RiskSpinState.MAX_INVENTORY_SIZE)
            }
            ?: emptyList()
        return RiskSpinState(
            inventory = inventory,
            cooldownBatchesRemaining = optInt("cooldownBatchesRemaining").coerceAtLeast(0),
            hasUsedRevertSinceLastMove = optBoolean("hasUsedRevertSinceLastMove"),
            previousMoveSnapshot = if (allowRevertSnapshot) {
                optJSONObject("previousMoveSnapshot")?.toGameState(allowRevertSnapshot = false)
            } else {
                null
            }
        )
    }

    private fun RunStats.toJson(): JSONObject {
        return JSONObject()
            .put("runId", runId)
            .put("startedAtEpochMillis", startedAtEpochMillis)
            .put("linesCleared", linesCleared)
            .put("contractsCompleted", contractsCompleted)
            .put("bombsUsed", bombsUsed)
            .put("megaBombsUsed", megaBombsUsed)
    }

    private fun JSONObject.toRunStats(): RunStats? {
        val runId = optString("runId").takeIf { it.isNotBlank() } ?: return null
        return RunStats(
            runId = runId,
            startedAtEpochMillis = optLong("startedAtEpochMillis", System.currentTimeMillis()),
            linesCleared = optInt("linesCleared").coerceAtLeast(0),
            contractsCompleted = optInt("contractsCompleted").coerceAtLeast(0),
            bombsUsed = optInt("bombsUsed").coerceAtLeast(0),
            megaBombsUsed = optInt("megaBombsUsed").coerceAtLeast(0)
        )
    }

    private fun RiskSpinMemorySession.toJson(): JSONObject {
        return JSONObject()
            .put("option", option.name)
            .put("revealsLeft", revealsLeft)
            .put("fields", JSONArray().also { array ->
                fields.forEach { field ->
                    array.put(
                        JSONObject()
                            .put("id", field.id)
                            .put("outcome", field.outcome.name)
                            .put("isRevealed", field.isRevealed)
                            .put("jokerAdded", field.jokerAdded?.name ?: JSONObject.NULL)
                            .put("lostBecauseInventoryFull", field.lostBecauseInventoryFull)
                    )
                }
            })
    }

    private fun JSONObject.toMemorySession(): RiskSpinMemorySession? {
        val option = enumOrNull<RiskSpinOption>(optString("option")) ?: return null
        val fields = optJSONArray("fields")?.let { array ->
            (0 until array.length()).map { index ->
                val field = array.optJSONObject(index) ?: return null
                val outcome = enumOrNull<RiskSpinOutcome>(field.optString("outcome")) ?: return null
                RiskSpinMemoryField(
                    id = field.optInt("id"),
                    outcome = outcome,
                    isRevealed = field.optBoolean("isRevealed"),
                    jokerAdded = enumOrNull<JokerType>(field.optString("jokerAdded")),
                    lostBecauseInventoryFull = field.optBoolean("lostBecauseInventoryFull")
                )
            }
        } ?: return null
        return RiskSpinMemorySession(
            option = option,
            revealsLeft = optInt("revealsLeft").coerceAtLeast(0),
            fields = fields
        )
    }

    private fun JSONObject.optNullableInt(name: String): Int? {
        return if (has(name) && !isNull(name)) optInt(name) else null
    }

    private inline fun <reified T : Enum<T>> enumOrNull(value: String): T? {
        return enumValues<T>().firstOrNull { it.name == value }
    }
}
