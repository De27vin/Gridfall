package com.example.gridfall.network

import com.example.gridfall.game.CommunityMapDocument
import com.example.gridfall.game.CommunityMapFormat
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class CommunityPublication(val code: String, val creator: String, val document: CommunityMapDocument,
    val published: Boolean, val visibility: String, val downloads: Int = 0, val ratingAverage: Double = 0.0,
    val ratingCount: Int = 0, val hidden: Boolean = false)
data class CommunityPage(val items: List<CommunityPublication>, val nextCursor: String?)
interface CommunityMapsGateway {
    suspend fun fetch(code: String): CommunityPublication
    suspend fun publish(token: String, document: String, visibility: String, requestId: String): CommunityPublication
    suspend fun browse(query: String, sort: String, cursor: String?, ownerToken: String? = null): CommunityPage
    suspend fun importMap(code: String, requestId: String, token: String?): CommunityPublication
    suspend fun rate(code: String, stars: Int, token: String): CommunityPublication
    suspend fun unpublish(code: String, token: String)
    suspend fun report(code: String, reason: String, notes: String, token: String)
}

class CommunityMapsClient(
    private val endpoint: (String) -> String = ApiConfig::endpoint,
    private val http: OkHttpClient = OkHttpClient()
) : CommunityMapsGateway {
    override suspend fun fetch(code: String): CommunityPublication = parse(request("GET", "/community/maps/${normalizeCode(code)}"))

    override suspend fun publish(token: String, document: String, visibility: String, requestId: String): CommunityPublication =
        parse(request("POST", "/community/maps", token, JSONObject().put("document", JSONObject(document))
            .put("visibility", visibility).put("requestId", requestId)))

    override suspend fun browse(query: String, sort: String, cursor: String?, ownerToken: String?): CommunityPage {
        fun escape(value: String) = java.net.URLEncoder.encode(value, "UTF-8")
        val path = "/community/maps${if (ownerToken != null) "/mine" else ""}?q=${escape(query)}&sort=${escape(sort)}" +
            (cursor?.let { "&cursor=${escape(it)}" } ?: "")
        val response = request("GET", path, ownerToken)
        val items = response.getJSONArray("items")
        return CommunityPage((0 until items.length()).map { parse(items.getJSONObject(it)) },
            response.optString("nextCursor").takeUnless { it.isBlank() || it == "null" })
    }
    override suspend fun importMap(code: String, requestId: String, token: String?) =
        parse(request("POST", "/community/maps/${normalizeCode(code)}/import", token, JSONObject().put("requestId", requestId)))
    override suspend fun rate(code: String, stars: Int, token: String) =
        parse(request("PUT", "/community/maps/${normalizeCode(code)}/rating", token, JSONObject().put("stars", stars)))
    override suspend fun unpublish(code: String, token: String) { request("DELETE", "/community/maps/${normalizeCode(code)}", token) }
    override suspend fun report(code: String, reason: String, notes: String, token: String) {
        request("POST", "/community/maps/${normalizeCode(code)}/report", token, JSONObject().put("reason", reason).put("notes", notes))
    }

    internal suspend fun request(method: String, path: String, token: String? = null, body: JSONObject? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder().url(endpoint(path))
            token?.let { builder.header("Authorization", "Bearer $it") }
            builder.method(method, if (method == "GET") null else (body ?: JSONObject()).toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType()))
            try {
                http.newCall(builder.build()).execute().use { response ->
                    if (!response.isSuccessful) throw CommunityApiException(response.code, errorMessage(response.code))
                    val source = response.body?.source() ?: throw IOException("Empty response")
                    if (source.request(1_048_577)) throw IOException("Community response is too large")
                    JSONObject(source.readUtf8())
                }
            } catch (e: CommunityApiException) { throw e
            } catch (e: IOException) { throw IOException("Cannot reach Community Maps. Check your connection and try again.", e) }
        }

    companion object {
        fun normalizeCode(raw: String): String = raw.trim().uppercase(java.util.Locale.ROOT).also {
            require(it.matches(Regex("GF-[0-9A-HJKMNP-TV-Z]{10}"))) { "Enter a valid code: GF-XXXXXXXXXX" }
        }
        fun parse(json: JSONObject) = CommunityPublication(json.getString("code"), json.getString("creator"),
            CommunityMapFormat.decode(json.getJSONObject("document").toString()), json.getBoolean("published"), json.getString("visibility"),
            json.optInt("downloads"), json.optDouble("ratingAverage", 0.0), json.optInt("ratingCount"), json.optBoolean("hidden"))
        fun errorMessage(status: Int): String = when(status) {
            400 -> "Invalid map code or map data."
            401, 403 -> "Sign in with a completed profile to use this action."
            404 -> "This map was deleted, unpublished, hidden, or does not exist."
            409 -> "This request changed. Close and reopen sharing to publish a new version."
            429 -> "Too many requests. Please try again later."
            else -> "Community Maps is temporarily unavailable. Please try again."
        }
    }
}
class CommunityApiException(val status: Int, message: String): IOException(message)
