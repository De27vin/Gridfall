package com.example.gridfall.network

import com.example.gridfall.BuildConfig

object ApiConfig {
    val baseUrl: String = normalizeBaseUrl(BuildConfig.GRIDFALL_API_BASE_URL)

    val debugBaseUrlLabel: String?
        get() = if (BuildConfig.DEBUG) baseUrl else null

    fun endpoint(path: String): String {
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        return "$baseUrl$normalizedPath"
    }

    fun normalizeBaseUrl(rawBaseUrl: String): String {
        return rawBaseUrl.trim().trimEnd('/')
    }
}
