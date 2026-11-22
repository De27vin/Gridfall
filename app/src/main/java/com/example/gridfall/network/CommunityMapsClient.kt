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
    val ratingCount: Int = 0, val hidden: Boolean = false, val yourRating: Int? = null)
data class CommunityPage(val items: List<CommunityPublication>, val nextCursor: String?)
interface CommunityMapsGateway {
    fun finishPublication(code: String) {}
    suspend fun pendingPublication(token: String, mapId: String): PendingPublication? = null
    suspend fun publishSavedMap(token: String, mapId: String, document: String, visibility: String, requestId: String): CommunityPublication =
        publish(token, document, visibility, requestId)
    suspend fun personalRating(code: String, token: String): Int? = null
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
    private val journal: PublicationJournal? = null,
    private val ratings: CommunityRatingStore? = null,
    private val identityOfToken: (String) -> String = { token ->
        val payload = android.util.Base64.decode(token.split(".")[1], android.util.Base64.URL_SAFE)
        JSONObject(String(payload, Charsets.UTF_8)).getString("sub")
    },
    private val http: OkHttpClient = ApiHttpPolicy.builder().callTimeout(30, java.util.concurrent.TimeUnit.SECONDS).build()
) : CommunityMapsGateway {
    override suspend fun fetch(code: String): CommunityPublication = parse(request("GET", "/community/maps/${normalizeCode(code)}"))

    override suspend fun pendingPublication(token: String, mapId: String): PendingPublication? = journal?.pending(identityOfToken(token), mapId)
    override suspend fun publishSavedMap(token: String, mapId: String, document: String, visibility: String, requestId: String): CommunityPublication {
        val owner = if (journal != null) identityOfToken(token) else ""
        val pending = journal?.begin(owner, mapId, document, visibility) ?: PendingPublication(document, visibility)
        val result = try {
            parse(request("POST", "/community/maps", token, JSONObject().put("document", JSONObject(pending.document))
                .put("visibility", pending.visibility).put("requestId", if (journal != null) pending.requestId else requestId)))
        } catch (error: CommunityApiException) {
            // 422 is a definitive owner-content rejection, after server idempotency
            // lookup. No publication exists for this request: permit a legitimate edit.
            if (error.status == 422) journal?.rejectDraft(owner, mapId)
            throw error
        }
        journal?.received(owner, pending.document, pending.visibility, result.code)
        journal?.receivedDraft(owner, mapId, result.code)
        return result
    }
    override fun finishPublication(code: String) { journal?.finish(code) }
    override suspend fun publish(token: String, document: String, visibility: String, requestId: String): CommunityPublication {
        val owner = if (journal != null) identityOfToken(token) else ""
        val stableId = journal?.requestId(owner, document, visibility) ?: requestId
        val result = parse(request("POST", "/community/maps", token, JSONObject().put("document", JSONObject(document))
            .put("visibility", visibility).put("requestId", stableId)))
        journal?.received(owner, document, visibility, result.code)
        return result
    }

    override suspend fun browse(query: String, sort: String, cursor: String?, ownerToken: String?): CommunityPage {
        fun escape(value: String) = java.net.URLEncoder.encode(value, "UTF-8")
        val path = "/community/maps${if (ownerToken != null) "/mine" else ""}?q=${escape(query)}&sort=${escape(sort)}&limit=10" +
            (cursor?.let { "&cursor=${escape(it)}" } ?: "")
        val page = parsePage(request("GET", path, ownerToken))
        if (cursor != null && page.nextCursor == cursor) throw CommunityProtocolException("Community Maps returned a repeated page. Please refresh.")
        return page
    }
    override suspend fun importMap(code: String, requestId: String, token: String?) =
        parse(request("POST", "/community/maps/${normalizeCode(code)}/import", token, JSONObject().put("requestId", requestId)))
    override suspend fun personalRating(code: String, token: String): Int? =
        ratings?.load(identityOfToken(token), normalizeCode(code))
    override suspend fun rate(code: String, stars: Int, token: String): CommunityPublication {
        val normalized = normalizeCode(code)
        val owner = if (ratings != null) identityOfToken(token) else null
        val result = parse(request("PUT", "/community/maps/$normalized/rating", token, JSONObject().put("stars", stars)))
        if (owner != null) ratings?.save(owner, normalized, result.yourRating ?: stars)
        return result
    }
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
                    val source = response.body?.source() ?: throw IOException("Empty response")
                    if (source.request(1_048_577)) throw CommunityProtocolException("Community Maps returned an oversized response.")
                    val raw = source.readUtf8()
                    if (!response.isSuccessful) {
                        val permissionMessage = if (response.code == 403) runCatching {
                            JSONObject(raw).getString("error").trim().takeIf { it.length in 1..240 && it.none { c -> c.code < 32 } }
                        }.getOrNull() else null
                        val message = if (response.code == 403) permissionMessage
                            ?: "Access to Community Maps was blocked before reaching the API. Please retry later."
                            else errorMessage(response.code)
                        throw CommunityApiException(response.code, message)
                    }
                    try { JSONObject(raw) } catch (e: org.json.JSONException) { throw invalidResponse(e) }
                }
            } catch (e: CommunityApiException) { throw e
            } catch (e: CommunityProtocolException) { throw e
            } catch (e: IOException) { throw IOException("Cannot reach Community Maps. Check your connection and try again.", e) }
        }

    companion object {
        fun normalizeCode(raw: String): String = raw.trim().uppercase(java.util.Locale.ROOT).also {
            require(it.matches(Regex("GF-[0-9A-HJKMNP-TV-Z]{10}"))) { "Enter a valid code: GF-XXXXXXXXXX" }
        }
        private fun invalidResponse(cause: Exception) = CommunityProtocolException("Community Maps returned an invalid response. Please retry.", cause)
        fun parsePage(json: JSONObject): CommunityPage = try {
            val items = json.getJSONArray("items")
            val cursor = json.get("nextCursor")
            require(cursor === JSONObject.NULL || cursor is String && cursor.isNotBlank() && cursor.length <= 4096)
            CommunityPage((0 until items.length()).map { parse(items.getJSONObject(it)) }, cursor as? String)
        } catch (e: org.json.JSONException) { throw invalidResponse(e)
        } catch (e: IllegalArgumentException) { throw invalidResponse(e) }

        fun parse(json: JSONObject): CommunityPublication = try {
            fun count(key: String): Int {
                if (!json.has(key)) return 0
                val n = json.get(key)
                require(n is Number && n.toDouble().isFinite() && n.toDouble() in 0.0..Int.MAX_VALUE.toDouble() && n.toDouble() % 1.0 == 0.0)
                return n.toInt()
            }
            val average = if (json.has("ratingAverage")) json.get("ratingAverage") else 0.0
            require(average is Number && average.toDouble().isFinite() && average.toDouble() in 0.0..5.0)
            val visibility = json.getString("visibility")
            require(visibility == "public" || visibility == "unlisted")
            require(json.get("published") is Boolean)
            val ownRating = if (json.has("yourRating") && !json.isNull("yourRating")) {
                val own = json.get("yourRating")
                require(own is Number && own.toDouble().isFinite() && own.toDouble() % 1.0 == 0.0 && own.toDouble() in 1.0..5.0)
                own.toInt()
            } else null
            CommunityPublication(normalizeCode(json.getString("code")), json.getString("creator"),
                CommunityMapFormat.decode(json.getJSONObject("document").toString()), json.getBoolean("published"), visibility,
                count("downloads"), average.toDouble(), count("ratingCount"), json.optBoolean("hidden"), ownRating)
        } catch (e: org.json.JSONException) { throw invalidResponse(e)
        } catch (e: IllegalArgumentException) { throw invalidResponse(e) }
        fun errorMessage(status: Int): String = when(status) {
            400 -> "Invalid map code or map data."
            401, 403 -> "Sign in with a completed profile to use this action."
            404 -> "This map was deleted, unpublished, hidden, or does not exist."
            409 -> "This request changed. Close and reopen sharing to publish a new version."
            422 -> "This content was removed by moderation. Edit the map before publishing again."
            429 -> "Too many requests. Please try again later."
            else -> "Community Maps is temporarily unavailable. Please try again."
        }
    }
}
class CommunityApiException(val status: Int, message: String): IOException(message)
class CommunityProtocolException(message: String, cause: Exception? = null): IOException(message, cause)
