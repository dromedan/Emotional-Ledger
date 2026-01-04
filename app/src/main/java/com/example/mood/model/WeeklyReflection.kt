package com.example.mood.model

import kotlinx.serialization.Serializable

@Serializable
data class WeeklyReflection(
    val weekStart: String,   // ISO yyyy-MM-dd (Sunday)
    val note: String
)
