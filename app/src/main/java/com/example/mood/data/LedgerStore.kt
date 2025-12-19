package com.example.mood.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import com.example.mood.model.LedgerEntry

private val Context.dataStore by preferencesDataStore("ledger_store")

object LedgerStore {

    private val ENTRIES_KEY = stringPreferencesKey("entries")

    suspend fun saveEntries(
        context: Context,
        entries: List<LedgerEntry>
    ) {
        context.dataStore.edit { prefs ->
            prefs[ENTRIES_KEY] = Json.encodeToString(entries)
        }
    }

    suspend fun loadEntries(context: Context): List<LedgerEntry> {
        val prefs = context.dataStore.data.first()
        return prefs[ENTRIES_KEY]?.let {
            Json.decodeFromString(it)
        } ?: emptyList()
    }
}


