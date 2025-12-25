// model/TagStats.kt
@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.example.mood.model

import kotlinx.serialization.Serializable

@Serializable
data class TagStats(
    val tag: String,
    val count: Int,
    val totalDelta: Float
) {
    val average: Float
        get() = if (count == 0) 0f else totalDelta / count
}
