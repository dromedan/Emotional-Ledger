@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.example.mood.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mood.model.LedgerEntry
import com.example.mood.model.DailyReflection
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import com.example.mood.model.TagStats
import kotlinx.serialization.InternalSerializationApi
import android.util.Log
import java.time.LocalDate
import java.time.DayOfWeek
import com.example.mood.model.WeeklyReflection

import com.example.mood.model.DailyMoodPoint
import com.example.mood.model.MonthlyReflection
import com.example.mood.model.InfluenceOverride



private val Context.dataStore by preferencesDataStore(name = "mood_ledger")
private val TAG_STATS_KEY =
    stringPreferencesKey("tag_stats")

private val INFLUENCE_OVERRIDES_KEY =
    stringPreferencesKey("influence_overrides")


private val BALL_LAYOUTS_KEY =
    stringPreferencesKey("ball_layouts")

@OptIn(InternalSerializationApi::class)
object LedgerStore {

    // ✅ STRING KEYS ONLY
    private val ENTRIES_BY_DAY_KEY =
        stringPreferencesKey("entries_by_day")

    private val REFLECTIONS_BY_DAY_KEY =
        stringPreferencesKey("reflections_by_day")

    private val WEEKLY_REFLECTIONS_KEY =
        stringPreferencesKey("weekly_reflections")

    private val MONTHLY_REFLECTIONS_KEY =
        stringPreferencesKey("monthly_reflections")


    /* ---------------- ENTRIES ---------------- */

    @OptIn(InternalSerializationApi::class)
    suspend fun loadEntriesForDay(
        context: Context,
        dayKey: String
    ): List<LedgerEntry> {
        val prefs = context.dataStore.data.first()
        val json = prefs[ENTRIES_BY_DAY_KEY] ?: return emptyList()

        val map: Map<String, List<LedgerEntry>> =
            Json.decodeFromString(json)

        return map[dayKey] ?: emptyList()
    }

    suspend fun loadDaysWithEntries(
        context: Context
    ): Set<String> {
        val prefs = context.dataStore.data.first()
        val json = prefs[ENTRIES_BY_DAY_KEY] ?: return emptySet()

        val map: Map<String, List<LedgerEntry>> =
            Json.decodeFromString(json)

        return map
            .filterValues { it.isNotEmpty() }
            .keys
    }

    @OptIn(InternalSerializationApi::class)
    suspend fun saveEntriesForDay(
        context: Context,
        dayKey: String,
        entries: List<LedgerEntry>
    ) {
        context.dataStore.edit { prefs ->
            val existingJson = prefs[ENTRIES_BY_DAY_KEY]
            val map = if (existingJson != null) {
                Json.decodeFromString<Map<String, List<LedgerEntry>>>(existingJson)
            } else {
                emptyMap()
            }

            val updated = map + (dayKey to entries)

            prefs[ENTRIES_BY_DAY_KEY] =
                Json.encodeToString(updated)
        }
    }

    /* ---------------- REFLECTIONS ---------------- */

    suspend fun loadDailyReflection(
        context: Context,
        dayKey: String
    ): DailyReflection? {
        val prefs = context.dataStore.data.first()
        val json = prefs[REFLECTIONS_BY_DAY_KEY] ?: return null

        val map: Map<String, DailyReflection> =
            Json.decodeFromString(json)

        return map[dayKey]
    }

    suspend fun saveDailyReflection(
        context: Context,
        reflection: DailyReflection
    ) {
        context.dataStore.edit { prefs ->
            val existingJson = prefs[REFLECTIONS_BY_DAY_KEY]
            val map = if (existingJson != null) {
                Json.decodeFromString<Map<String, DailyReflection>>(existingJson)
            } else {
                emptyMap()
            }

            val updated = map + (reflection.date to reflection)

            prefs[REFLECTIONS_BY_DAY_KEY] =
                Json.encodeToString(updated)
        }
    }

    @OptIn(InternalSerializationApi::class)
    suspend fun loadTagStats(
        context: Context
    ): Map<String, TagStats> {
        val prefs = context.dataStore.data.first()
        val json = prefs[TAG_STATS_KEY] ?: return emptyMap()
        return Json.decodeFromString(json)
    }

    private suspend fun saveTagStats(
        context: Context,
        stats: Map<String, TagStats>
    ) {
        context.dataStore.edit { prefs ->
            prefs[TAG_STATS_KEY] = Json.encodeToString(stats)
        }
    }

    suspend fun applyEntryToTagStats(
        context: Context,
        entry: LedgerEntry,
        previousEntry: LedgerEntry? = null
    ) {
        val stats = loadTagStats(context).toMutableMap()

        // Remove previous entry impact
        previousEntry?.let { old ->
            old.tags.forEach { tag ->
                val s = stats[tag] ?: return@forEach
                stats[tag] = s.copy(
                    count = s.count - 1,
                    totalDelta = s.totalDelta - old.delta
                )
            }
        }

        // Apply new entry impact
        entry.tags.forEach { tag ->
            val s = stats[tag]
            stats[tag] =
                if (s == null)
                    TagStats(tag, 1, entry.delta)
                else
                    s.copy(
                        count = s.count + 1,
                        totalDelta = s.totalDelta + entry.delta
                    )
        }

        // Clean zero-count tags SAFELY
        val cleaned = stats
            .filterValues { it.count > 0 }
            .toMutableMap()

        saveTagStats(context, cleaned)
        Log.d(
            "TagStats",
            cleaned.values.joinToString("\n") {
                "${it.tag}: avg=${"%.2f".format(it.average)} count=${it.count}"
            }
        )
    }

    suspend fun removeEntryFromTagStats(
        context: Context,
        entry: LedgerEntry
    ) {
        val stats = loadTagStats(context).toMutableMap()

        entry.tags.forEach { tag ->
            val s = stats[tag] ?: return@forEach
            stats[tag] = s.copy(
                count = s.count - 1,
                totalDelta = s.totalDelta - entry.delta
            )
        }

        val cleaned = stats
            .filterValues { it.count > 0 }
            .toMutableMap()

        saveTagStats(context, cleaned)
    }


    suspend fun loadWeeklyReflection(
        context: Context,
        weekStart: String
    ): WeeklyReflection? {
        val prefs = context.dataStore.data.first()
        val json = prefs[WEEKLY_REFLECTIONS_KEY] ?: return null

        val map: Map<String, WeeklyReflection> =
            Json.decodeFromString(json)

        return map[weekStart]
    }

    suspend fun saveWeeklyReflection(
        context: Context,
        reflection: WeeklyReflection
    ) {
        context.dataStore.edit { prefs ->
            val existingJson = prefs[WEEKLY_REFLECTIONS_KEY]
            val map =
                if (existingJson != null)
                    Json.decodeFromString<Map<String, WeeklyReflection>>(existingJson)
                else
                    emptyMap()

            prefs[WEEKLY_REFLECTIONS_KEY] =
                Json.encodeToString(map + (reflection.weekStart to reflection))
        }
    }


    suspend fun loadWeeklyMood(
        context: Context,
        weekStart: LocalDate,
        baseline: Float = 5.0f
    ): List<DailyMoodPoint> {

        return (0..6).map { offset ->
            val day = weekStart.plusDays(offset.toLong())
            val dayKey = day.toString()

            val entries =
                LedgerStore.loadEntriesForDay(context, dayKey)

            val reflection =
                LedgerStore.loadDailyReflection(context, dayKey)

            val eventTotal =
                entries.sumOf { it.delta.toDouble() }.toFloat()

            val computed = baseline + eventTotal

            val final =
                if (reflection != null)
                    computed + reflection.drift
                else
                    computed

            DailyMoodPoint(
                date = day,
                score = final
            )
        }
    }

    suspend fun loadMonthlyMood(
        context: Context,
        monthStart: LocalDate,
        baseline: Float = 5.0f
    ): List<DailyMoodPoint> {

        val firstWeekStart =
            monthStart.with(java.time.DayOfWeek.SUNDAY)
                .takeIf { !it.isAfter(monthStart) }
                ?: monthStart.minusWeeks(1).with(java.time.DayOfWeek.SUNDAY)


        val weeks = (0..5).mapNotNull { offset ->
            val weekStart = firstWeekStart.plusWeeks(offset.toLong())

            // Stop if week is completely outside the month
            if (weekStart.month != monthStart.month &&
                weekStart.plusDays(6).month != monthStart.month
            ) return@mapNotNull null

            weekStart
        }

        return weeks.map { weekStart ->
            val days =
                loadWeeklyMood(context, weekStart, baseline)
                    .filter { it.date.month == monthStart.month }

            val avg =
                if (days.isNotEmpty())
                    days.map { it.score }.average().toFloat()
                else
                    baseline

            DailyMoodPoint(
                date = weekStart, // represents the week
                score = avg
            )
        }
    }

    suspend fun loadMonthlyReflection(
        context: Context,
        monthKey: String
    ): MonthlyReflection? {
        val prefs = context.dataStore.data.first()
        val json = prefs[MONTHLY_REFLECTIONS_KEY] ?: return null

        val map: Map<String, MonthlyReflection> =
            Json.decodeFromString(json)

        return map[monthKey]
    }

    suspend fun saveMonthlyReflection(
        context: Context,
        reflection: MonthlyReflection
    ) {
        context.dataStore.edit { prefs ->
            val existingJson = prefs[MONTHLY_REFLECTIONS_KEY]
            val map =
                if (existingJson != null)
                    Json.decodeFromString<Map<String, MonthlyReflection>>(existingJson)
                else
                    emptyMap()

            prefs[MONTHLY_REFLECTIONS_KEY] =
                Json.encodeToString(map + (reflection.monthKey to reflection))
        }
    }


    suspend fun loadYearlyMood(
        context: Context,
        year: Int,
        baseline: Float = 5.0f
    ): List<DailyMoodPoint> {

        val today = LocalDate.now()

        return (1..12).mapNotNull { month ->
            val monthStart = LocalDate.of(year, month, 1)
            val monthEnd = monthStart.plusMonths(1).minusDays(1)

            // Skip future months
            if (monthStart.isAfter(today)) return@mapNotNull null

            val daysInMonth =
                generateSequence(monthStart) { it.plusDays(1) }
                    .takeWhile { !it.isAfter(monthEnd) && !it.isAfter(today) }
                    .toList()

            if (daysInMonth.isEmpty()) return@mapNotNull null

            val scores = mutableListOf<Float>()

            for (day in daysInMonth) {
                val dayKey = day.toString()

                val entries =
                    LedgerStore.loadEntriesForDay(context, dayKey)

                val reflection =
                    LedgerStore.loadDailyReflection(context, dayKey)

                val eventTotal =
                    entries.sumOf { it.delta.toDouble() }.toFloat()

                val computed = baseline + eventTotal

                val final =
                    if (reflection != null)
                        computed + reflection.drift
                    else
                        computed

                scores += final
            }

            if (scores.isEmpty()) return@mapNotNull null

            DailyMoodPoint(
                date = monthStart,
                score = scores.average().toFloat()
            )
        }
    }


    @OptIn(InternalSerializationApi::class)
    suspend fun loadBallLayout(
        context: Context,
        dayKey: String
    ): List<Float>? {
        val prefs = context.dataStore.data.first()
        val json = prefs[BALL_LAYOUTS_KEY] ?: return null

        val map: Map<String, List<Float>> =
            Json.decodeFromString(json)

        return map[dayKey]
    }

    @OptIn(InternalSerializationApi::class)
    suspend fun saveBallLayout(
        context: Context,
        dayKey: String,
        angles: List<Float>
    ) {
        context.dataStore.edit { prefs ->
            val existingJson = prefs[BALL_LAYOUTS_KEY]
            val map =
                if (existingJson != null)
                    Json.decodeFromString<Map<String, List<Float>>>(existingJson)
                else
                    emptyMap()

            prefs[BALL_LAYOUTS_KEY] =
                Json.encodeToString(map + (dayKey to angles))
        }
    }
    suspend fun loadInfluenceOverrides(
        context: Context
    ): Map<String, InfluenceOverride> {
        val prefs = context.dataStore.data.first()
        val json = prefs[INFLUENCE_OVERRIDES_KEY] ?: return emptyMap()
        return Json.decodeFromString(json)
    }

    suspend fun saveInfluenceOverride(
        context: Context,
        override: InfluenceOverride
    ) {
        context.dataStore.edit { prefs ->
            val existing =
                prefs[INFLUENCE_OVERRIDES_KEY]
                    ?.let {
                        Json.decodeFromString<Map<String, InfluenceOverride>>(it)
                    }
                    ?: emptyMap()

            prefs[INFLUENCE_OVERRIDES_KEY] =
                Json.encodeToString(
                    existing + (override.tag to override)
                )
        }
    }

}
