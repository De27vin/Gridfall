package com.example.gridfall.network

import java.io.IOException

class HttpStatusException(
    val statusCode: Int,
    method: String,
    path: String
) : IOException("$method $path failed with HTTP $statusCode")
