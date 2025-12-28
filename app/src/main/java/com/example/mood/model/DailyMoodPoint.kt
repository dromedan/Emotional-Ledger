package com.example.mood.model

import java.time.LocalDate

data class DailyMoodPoint(
    val date: LocalDate,
    val score: Float
)