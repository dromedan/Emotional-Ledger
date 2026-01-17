@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.example.mood.util

import com.example.mood.model.LedgerEntry
import java.time.LocalDate
import kotlin.math.abs

fun impactOrderIndices(
    entries: List<LedgerEntry>
): List<Int> {
    return entries
        .mapIndexed { index, entry -> index to entry.delta }
        .sortedWith(
            compareByDescending<Pair<Int, Float>> { it.second }
        )
        .map { it.first }
}

fun angularDistance(a: Float, b: Float): Float {
    val diff = abs(a - b) % 360f
    return if (diff > 180f) 360f - diff else diff
}

fun normalizeAngle(a: Float): Float =
    ((a % 360f) + 360f) % 360f
fun todayKey(): String =
    LocalDate.now().toString() // yyyy-MM-dd
