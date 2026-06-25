package com.example.gridfall.ui

data class LineClearFeedback(
    val clearedRows: List<Int>,
    val clearedColumns: List<Int>,
    val token: Int
)
