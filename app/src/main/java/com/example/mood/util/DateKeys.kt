// util/DateKeys.kt
package com.example.mood.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun todayKey(): String =
    LocalDate.now().toString() // yyyy-MM-dd

fun dateKeyFromTimestamp(ts: Long): String =
    Instant.ofEpochMilli(ts)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toString()
