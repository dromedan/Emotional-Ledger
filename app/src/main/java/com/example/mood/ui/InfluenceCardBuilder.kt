@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.mood.ui

import com.example.mood.R
import com.example.mood.model.CardPolarity
import com.example.mood.model.CardRank
import com.example.mood.model.InfluenceCardModel
import com.example.mood.model.TagStats
import com.example.mood.model.LedgerEntry
import kotlin.math.abs

/**
 * Builds a complete InfluenceCardModel from raw influence data.
 * This function is PURE and deterministic.
 */
fun buildInfluenceCardModel(
    tag: String,
    stats: TagStats,
    entries: List<LedgerEntry>
): InfluenceCardModel {

    val polarity =
        if (stats.average >= 0f) CardPolarity.GREEN
        else CardPolarity.RED

    // --- Count intensity tiers with per-entry polarity
    val deepGreenCount = entries.count {
        it.delta >= 0f && kotlin.math.abs(it.delta) >= 0.7f
    }

    val greenCount = entries.count {
        it.delta > 0f && kotlin.math.abs(it.delta) < 0.7f
    }

    val deepRedCount = entries.count {
        it.delta <= 0f && kotlin.math.abs(it.delta) >= 0.7f
    }

    val redCount = entries.count {
        it.delta < 0f && kotlin.math.abs(it.delta) < 0.7f
    }




    // --- Rank derived from total occurrences
    val rank =
        when (stats.count) {
            in 0..10 -> CardRank.COMMON
            in 11..25 -> CardRank.UNCOMMON
            in 26..100 -> CardRank.RARE
            else -> CardRank.LEGENDARY
        }

    // --- Image selection (Phase 1: tag-based)
    val imageRes =
        when {
            tag.contains("code", ignoreCase = true) ->
                R.drawable.coding_image

            else ->
                R.drawable.card_default
        }

    // --- Defaults (from Card Properties.md)
    val type = ""
    val subType = null


    val description =
        when {
            tag.equals("coding", ignoreCase = true) ->
                "Translating ideas into structured, functional systems through focused problem-solving."

            else ->
                "An influence that shapes behavior and emotional state over time."
        }

    return InfluenceCardModel(
        name = tag,
        polarity = polarity,
        deepGreenCount = deepGreenCount,
        greenCount = greenCount,
        redCount = redCount,
        deepRedCount = deepRedCount,
        imageResId = imageRes,
        imagePath = null,
        type = type,
        subType = subType,
        rank = rank,
        averageImpact = stats.average,
        description = description
    )

}
