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

private val Context.dataStore by preferencesDataStore(name = "mood_ledger")

object LedgerStore {

    // ✅ STRING KEYS ONLY
    private val ENTRIES_BY_DAY_KEY =
        stringPreferencesKey("entries_by_day")

    private val REFLECTIONS_BY_DAY_KEY =
        stringPreferencesKey("reflections_by_day")

    /* ---------------- ENTRIES ---------------- */

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
}


