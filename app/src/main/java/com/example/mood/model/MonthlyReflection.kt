package com.example.mood.model

import kotlinx.serialization.Serializable

@Serializable
data class MonthlyReflection(
    val monthKey: String, // yyyy-MM
    val note: String
)
