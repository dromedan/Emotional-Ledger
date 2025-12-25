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



private val Context.dataStore by preferencesDataStore(name = "mood_ledger")
private val TAG_STATS_KEY =
    stringPreferencesKey("tag_stats")

@OptIn(InternalSerializationApi::class)
object LedgerStore {

    // ✅ STRING KEYS ONLY
    private val ENTRIES_BY_DAY_KEY =
        stringPreferencesKey("entries_by_day")

    private val REFLECTIONS_BY_DAY_KEY =
        stringPreferencesKey("reflections_by_day")

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

}




