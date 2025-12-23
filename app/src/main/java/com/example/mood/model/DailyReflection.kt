package com.example.mood.model

import kotlinx.serialization.Serializable

@Serializable
data class DailyReflection(
    val date: String,          // ISO yyyy-MM-dd
    val note: String,
    val drift: Float           // -1.0 .. +1.0
)
