package com.example.gridfall.network

import com.example.gridfall.network.dto.MeResponse
import com.example.gridfall.network.dto.PlayerProfileDto
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class GridfallApiClient(
    private val baseUrl: String = DEVELOPMENT_BASE_URL,
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    suspend fun getHealth(): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/health")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            response.isSuccessful
        }
    }

    suspend fun getMe(firebaseIdToken: String): MeResponse = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/me")
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
                totalLinesCleared = profile.getInt("totalLinesCleared"),
                totalContractsCompleted = profile.getInt("totalContractsCompleted"),
                totalBombsUsed = profile.getInt("totalBombsUsed"),
                totalMegaBombsUsed = profile.getInt("totalMegaBombsUsed")
            )
        )
    }

    companion object {
        const val DEVELOPMENT_BASE_URL = "http://192.168.222.172:8080"
    }
}

