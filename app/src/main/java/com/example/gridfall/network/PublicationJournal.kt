package com.example.gridfall.network

import android.content.Context
import java.security.MessageDigest
import java.util.UUID

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
    @Synchronized fun finish(code: String) {
        val key = preferences.getString("code_$code", null) ?: return
        check(preferences.edit().remove(key).remove("code_$code").commit())
    }
    private fun key(owner: String, document: String, visibility: String): String = MessageDigest.getInstance("SHA-256")
        .digest("${owner.length}:$owner:$visibility:$document".toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
