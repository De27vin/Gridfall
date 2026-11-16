package com.example.gridfall.network

import com.example.gridfall.BuildConfig
import okhttp3.OkHttpClient

/** Staging must not follow an API redirect into another environment. */
internal object ApiHttpPolicy {
    fun builder(buildType: String = BuildConfig.BUILD_TYPE): OkHttpClient.Builder = OkHttpClient.Builder()
        .followRedirects(buildType != "staging")
        .followSslRedirects(buildType != "staging")
}
