package com.example.antibully.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "notification_prefs"
)

object NotificationPrefs {
    private val KEY_TEXT = booleanPreferencesKey("notif_text_enabled")
    private val KEY_IMAGE = booleanPreferencesKey("notif_image_enabled")

    data class State(
        val textEnabled: Boolean = true,
        val imageEnabled: Boolean = true
    )

    fun flow(context: Context): Flow<State> =
        context.dataStore.data
            .catch { e ->
                if (e is IOException) emit(emptyPreferences()) else throw e
            }
            .map { p ->
                State(
                    textEnabled = p[KEY_TEXT] ?: true,
                    imageEnabled = p[KEY_IMAGE] ?: true
                )
            }

    suspend fun setText(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_TEXT] = enabled }
    }

    suspend fun setImage(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_IMAGE] = enabled }
    }
}
