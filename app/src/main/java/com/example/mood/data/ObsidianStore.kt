package com.example.mood.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "obsidian")

object ObsidianStore {

    private val FOLDER_URI = stringPreferencesKey("obsidian_folder_uri")

    suspend fun saveFolder(context: Context, uri: Uri) {
        context.dataStore.edit { prefs ->
            prefs[FOLDER_URI] = uri.toString()
        }
    }

    suspend fun loadFolder(context: Context): Uri? {
        val prefs = context.dataStore.data.first()
        return prefs[FOLDER_URI]?.let { Uri.parse(it) }
    }

    suspend fun clear(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.remove(FOLDER_URI)
        }
    }
}
