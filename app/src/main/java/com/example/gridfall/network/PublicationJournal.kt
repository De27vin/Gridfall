package com.example.gridfall.network

import android.content.Context
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONObject

data class PendingPublication(val document: String, val visibility: String, val requestId: String = UUID.randomUUID().toString())

/** Write-ahead operation identity: committed to disk before a publication is sent. */
class PublicationJournal(context: Context) {
    private val preferences = context.getSharedPreferences("community_publication_journal", Context.MODE_PRIVATE)
    @Synchronized fun requestId(owner: String, document: String, visibility: String): String {
        val key = key(owner, document, visibility)
        preferences.getString(key, null)?.let { return it }
        val id = UUID.randomUUID().toString()
        check(preferences.edit().putString(key, id).commit()) { "Cannot save publication retry state" }
        return id
    }
    @Synchronized fun received(owner: String, document: String, visibility: String, code: String) {
        check(preferences.edit().putString("code_$code", key(owner, document, visibility)).commit())
    }
    @Synchronized fun pending(owner: String, mapId: String): PendingPublication? {
        val raw = preferences.getString(key(owner, mapId, "draft"), null) ?: return null
        val json = JSONObject(raw)
        val document = json.getString("document")
        val visibility = json.getString("visibility")
        val id = json.optString("requestId").ifBlank { requestId(owner, document, visibility) }
        if (!json.has("requestId")) check(preferences.edit().putString(key(owner, mapId, "draft"), json.put("requestId", id).toString()).commit())
        return PendingPublication(document, visibility, id)
    }
    @Synchronized fun begin(owner: String, mapId: String, document: String, visibility: String): PendingPublication {
        pending(owner, mapId)?.let { return it }
        val pending = PendingPublication(document, visibility)
        val raw = JSONObject().put("document", document).put("visibility", visibility).put("requestId", pending.requestId).toString()
        check(preferences.edit().putString(key(owner, mapId, "draft"), raw).commit())
        return pending
    }
    @Synchronized fun receivedDraft(owner: String, mapId: String, code: String) {
        check(preferences.edit().putString("draft_code_$code", key(owner, mapId, "draft")).commit())
    }
    @Synchronized fun finish(code: String) {
        val key = preferences.getString("code_$code", null) ?: return
        val edit = preferences.edit().remove(key).remove("code_$code")
        preferences.getString("draft_code_$code", null)?.let { edit.remove(it) }
        check(edit.remove("draft_code_$code").commit())
    }
    private fun key(owner: String, document: String, visibility: String): String = MessageDigest.getInstance("SHA-256")
        .digest("${owner.length}:$owner:$visibility:$document".toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
