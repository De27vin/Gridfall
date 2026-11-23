package com.example.gridfall.network

import android.content.Context
import java.security.MessageDigest

/** Only acknowledged personal votes, scoped by account and map. No tokens are stored. */
class CommunityRatingStore(context: Context) {
    private val preferences = context.getSharedPreferences("community_personal_ratings", Context.MODE_PRIVATE)
    private fun key(owner: String, code: String): String = MessageDigest.getInstance("SHA-256")
        .digest("$owner\u0000$code".toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    fun load(owner: String, code: String): Int? = preferences.getInt(key(owner, code), 0).takeIf { it in 1..5 }
    fun save(owner: String, code: String, stars: Int) {
        require(stars in 1..5)
        preferences.edit().putInt(key(owner, code), stars).apply()
    }
}
