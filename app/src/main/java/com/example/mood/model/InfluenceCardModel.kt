package com.example.mood.model

import kotlinx.serialization.Serializable
/**
 * Pure data model describing everything needed
 * to render an Influence card.
 */
data class InfluenceCardModel(
    val name: String,

    // Polarity & intensity
    val polarity: CardPolarity,
    val deepGreenCount: Int,
    val greenCount: Int,
    val redCount: Int,
    val deepRedCount: Int,


    // Visual identity
    val imageResId: Int,
    val imagePath: String? = null,


    // Classification
    val type: String,
    val subType: String?,
    val rank: CardRank,

    // Computed stats
    val averageImpact: Float,

    // User-facing description
    val description: String
)

enum class CardPolarity {
    GREEN,
    RED
}

enum class CardRank {
    COMMON,
    UNCOMMON,
    RARE,
    LEGENDARY
}

@kotlinx.serialization.InternalSerializationApi
@Serializable
data class InfluenceOverride(
    val tag: String,
    val type: String? = null,
    val subType: String? = null,
    val description: String? = null,
    val imageResId: Int? = null,
    val imagePath: String? = null
)
