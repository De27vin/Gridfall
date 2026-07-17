package com.example.gridfall.network

import com.example.gridfall.network.dto.LeaderboardEntryDto
import com.example.gridfall.network.dto.LeaderboardsResponse
import com.example.gridfall.network.dto.LeaderboardSectionDto
import com.example.gridfall.network.dto.LeaderboardSectionsDto
import com.example.gridfall.network.dto.YourStatsDto
import com.example.gridfall.network.dto.LeaderboardResponse
import com.example.gridfall.network.dto.MeResponse
import com.example.gridfall.network.dto.PlayerProfileDto
import com.example.gridfall.network.dto.RunSubmissionRequest
import com.example.gridfall.network.dto.RunSubmissionResponse
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import org.json.JSONObject

class GridfallApiClient(
    private val apiConfig: ApiConfig = ApiConfig,
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun getHealth(): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(apiConfig.endpoint("/health"))
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            response.isSuccessful
        }
    }

    suspend fun getMe(firebaseIdToken: String): MeResponse = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(apiConfig.endpoint("/me"))
            .header("Authorization", "Bearer $firebaseIdToken")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("GET /me failed with HTTP ${response.code}")
            }
            parseMeResponse(body)
        }
    }

    suspend fun setUsername(
        firebaseIdToken: String,
        username: String
    ): MeResponse = withContext(Dispatchers.IO) {
        val requestBody = JSONObject()
            .put("username", username)
            .toString()
            .toRequestBody(jsonMediaType)
        val request = authenticatedRequest(firebaseIdToken, "/me/username")
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(usernameErrorMessage(response.code, body))
            }
            parseMeResponse(body)
        }
    }

    suspend fun createGuestMergeToken(firebaseIdToken: String): String? = withContext(Dispatchers.IO) {
        val request = authenticatedRequest(firebaseIdToken, "/auth/guest-merge-token")
            .post("{}".toRequestBody(jsonMediaType))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) return@withContext null
            JSONObject(body).optString("mergeToken").takeIf { it.isNotBlank() }
        }
    }

    suspend fun mergeGuest(
        firebaseIdToken: String,
        mergeToken: String
    ): Boolean = withContext(Dispatchers.IO) {
        val requestBody = JSONObject()
            .put("mergeToken", mergeToken)
            .toString()
            .toRequestBody(jsonMediaType)
        val request = authenticatedRequest(firebaseIdToken, "/auth/merge-guest")
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            response.isSuccessful
        }
    }

    suspend fun submitRun(
        firebaseIdToken: String,
        request: RunSubmissionRequest
    ): RunSubmissionResponse = withContext(Dispatchers.IO) {
        val requestBody = request.toJson()
            .toString()
            .toRequestBody(jsonMediaType)
        val httpRequest = authenticatedRequest(firebaseIdToken, "/runs")
            .post(requestBody)
            .build()

        httpClient.newCall(httpRequest).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw HttpStatusException(response.code, "POST", "/runs")
            }
            parseRunSubmissionResponse(body)
        }
    }

suspend fun getLeaderboards(
        firebaseIdToken: String?,
        limit: Int = 10
    ): LeaderboardsResponse = withContext(Dispatchers.IO) {
        val safeLimit = limit.coerceIn(1, 100)
        val builder = Request.Builder()
            .url(apiConfig.endpoint("/leaderboards?limit=$safeLimit"))
            .get()
        firebaseIdToken?.takeIf { it.isNotBlank() }?.let { token ->
            builder.header("Authorization", "Bearer $token")
        }

        httpClient.newCall(builder.build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("GET /leaderboards failed with HTTP ${response.code}")
            }
            parseLeaderboardsResponse(body)
        }
    }

    suspend fun getLeaderboard(limit: Int = 50): LeaderboardResponse = withContext(Dispatchers.IO) {
        val safeLimit = limit.coerceIn(1, 100)
        val request = Request.Builder()
            .url(apiConfig.endpoint("/leaderboard?limit=$safeLimit"))
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("GET /leaderboard failed with HTTP ${response.code}")
            }
            parseLeaderboardResponse(body)
        }
    }

    private fun authenticatedRequest(
        firebaseIdToken: String,
        path: String
    ): Request.Builder {
        return Request.Builder()
            .url(apiConfig.endpoint(path))
            .header("Authorization", "Bearer $firebaseIdToken")
    }

    private fun usernameErrorMessage(
        code: Int,
        body: String
    ): String {
        if (code == 409) return "Username already taken."
        val backendMessage = runCatching {
            JSONObject(body).optString("error")
        }.getOrNull()
        return backendMessage?.takeIf { it.isNotBlank() } ?: "Username could not be saved."
    }

    private fun parseMeResponse(rawJson: String): MeResponse {
        val json = JSONObject(rawJson)
        val profile = json.getJSONObject("profile")

        return MeResponse(
            id = json.getString("id"),
            firebaseUid = json.getString("firebaseUid"),
            email = json.optString("email").takeUnless { it.isBlank() || it == "null" },
            isAnonymous = json.getBoolean("isAnonymous"),
            username = json.optString("username").takeUnless { it.isBlank() || it == "null" },
            profile = PlayerProfileDto(
                bestScore = profile.getInt("bestScore"),
                bestLevel = profile.getInt("bestLevel"),
                gamesPlayed = profile.getInt("gamesPlayed"),
                totalPoints = profile.optInt("totalPoints"),
                totalLinesCleared = profile.getInt("totalLinesCleared"),
                totalContractsCompleted = profile.getInt("totalContractsCompleted"),
                totalBombsUsed = profile.getInt("totalBombsUsed"),
                totalMegaBombsUsed = profile.getInt("totalMegaBombsUsed")
            )
        )
    }

    private fun parseRunSubmissionResponse(rawJson: String): RunSubmissionResponse {
        val json = JSONObject(rawJson)
        val run = json.optJSONObject("run")
        val profile = json.optJSONObject("profile")

        return RunSubmissionResponse(
            runId = run?.optString("id")?.takeIf { it.isNotBlank() },
            bestScore = profile?.optInt("bestScore"),
            bestLevel = profile?.optInt("bestLevel")
        )
    }

    private fun parseLeaderboardsResponse(rawJson: String): LeaderboardsResponse {
        val json = JSONObject(rawJson)
        val me = json.optJSONObject("me")?.let { stats ->
            YourStatsDto(
                username = stats.optString("username").takeIf { it.isNotBlank() && it != "null" },
                bestScore = stats.optInt("bestScore"),
                bestLevel = stats.optInt("bestLevel", 1),
                totalPoints = stats.optInt("totalPoints"),
                gamesPlayed = stats.optInt("gamesPlayed"),
                totalLinesCleared = stats.optInt("totalLinesCleared"),
                totalContractsCompleted = stats.optInt("totalContractsCompleted")
            )
        }
        val sections = json.optJSONObject("leaderboards") ?: JSONObject()
        return LeaderboardsResponse(
            me = me,
            leaderboards = LeaderboardSectionsDto(
                bestScore = parseLeaderboardSection(sections.optJSONObject("bestScore")),
                totalPoints = parseLeaderboardSection(sections.optJSONObject("totalPoints")),
                linesCleared = parseLeaderboardSection(sections.optJSONObject("linesCleared")),
                contractsCompleted = parseLeaderboardSection(sections.optJSONObject("contractsCompleted"))
            )
        )
    }

    private fun parseLeaderboardSection(section: JSONObject?): LeaderboardSectionDto {
        val entries = section?.optJSONArray("entries")
        val parsedEntries = (0 until (entries?.length() ?: 0)).mapNotNull { index ->
            entries?.optJSONObject(index)?.let { parseLeaderboardEntry(it, index + 1) }
        }
        return LeaderboardSectionDto(
            entries = parsedEntries,
            me = section?.optJSONObject("me")?.let { parseLeaderboardEntry(it, 0) }
        )
    }

    private fun parseLeaderboardEntry(entry: JSONObject, fallbackRank: Int): LeaderboardEntryDto? {
        val username = entry.optString("username").takeIf { it.isNotBlank() && it != "null" } ?: return null
        return LeaderboardEntryDto(
            rank = entry.optInt("rank", fallbackRank),
            username = username,
            bestScore = entry.optInt("bestScore"),
            bestLevel = entry.optInt("bestLevel", 1),
            totalPoints = entry.optInt("totalPoints"),
            totalLinesCleared = entry.optInt("totalLinesCleared"),
            totalContractsCompleted = entry.optInt("totalContractsCompleted"),
            isInTop = entry.optBoolean("isInTop")
        )
    }
    private fun parseLeaderboardResponse(rawJson: String): LeaderboardResponse {
        val json = JSONObject(rawJson)
        val entries = json.optJSONArray("entries")
        val parsedEntries = (0 until (entries?.length() ?: 0)).mapNotNull { index ->
            val entry = entries?.optJSONObject(index) ?: return@mapNotNull null
            val username = entry.optString("username").takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            LeaderboardEntryDto(
                rank = entry.optInt("rank", index + 1),
                username = username,
                bestScore = entry.optInt("bestScore"),
                bestLevel = entry.optInt("bestLevel", 1)
            )
        }

        return LeaderboardResponse(entries = parsedEntries)
    }
}
