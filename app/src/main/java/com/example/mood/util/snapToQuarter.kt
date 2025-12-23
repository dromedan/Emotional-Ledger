package com.example.mood.util

fun snapToQuarter(value: Float): Float {
    val step = 0.25f
    val snapped = kotlin.math.round(value / step) * step
    return when {
        kotlin.math.abs(snapped) < 0.125f -> 0f
        snapped < -2f -> -2f
        snapped > 2f -> 2f
        else -> snapped
    }
}
