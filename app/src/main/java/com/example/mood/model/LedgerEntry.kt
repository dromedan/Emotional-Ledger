package com.example.mood.model

data class LedgerEntry(
    val id: Long,
    val timestamp: Long,
    val delta: Float,
    val tags: List<String>,
    val note: String,
    val title: String,
    val feeling: String
)