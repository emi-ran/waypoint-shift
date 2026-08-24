package com.waypointshift.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "waypoint_shift_preferences")

class PreferencesLocationPreferencesStorage(
    private val dataStore: DataStore<Preferences>,
) : LocationPreferencesStorage {

    override val favoritesJson: Flow<String?> = dataStore.data.map { preferences ->
        preferences[FAVORITES_JSON_KEY]
    }

    override val recentsJson: Flow<String?> = dataStore.data.map { preferences ->
        preferences[RECENTS_JSON_KEY]
    }

    override val onboardingSeen: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ONBOARDING_SEEN_KEY] ?: false
    }

    override suspend fun updateFavorites(transform: (String?) -> String?) {
        dataStore.edit { preferences ->
            val current = preferences[FAVORITES_JSON_KEY]
            val updated = transform(current)
            if (updated != null) {
                preferences[FAVORITES_JSON_KEY] = updated
            } else {
                preferences.remove(FAVORITES_JSON_KEY)
            }
        }
    }

    override suspend fun updateRecents(transform: (String?) -> String?) {
        dataStore.edit { preferences ->
            val current = preferences[RECENTS_JSON_KEY]
            val updated = transform(current)
            if (updated != null) {
                preferences[RECENTS_JSON_KEY] = updated
            } else {
                preferences.remove(RECENTS_JSON_KEY)
            }
        }
    }

    override suspend fun setOnboardingSeen(seen: Boolean) {
        dataStore.edit { preferences ->
            preferences[ONBOARDING_SEEN_KEY] = seen
        }
    }

    companion object {
        val FAVORITES_JSON_KEY = stringPreferencesKey("favorites_json_v1")
        val RECENTS_JSON_KEY = stringPreferencesKey("recents_json_v1")
        val ONBOARDING_SEEN_KEY = booleanPreferencesKey("onboarding_seen_v1")

        fun fromContext(context: Context): PreferencesLocationPreferencesStorage =
            PreferencesLocationPreferencesStorage(context.applicationContext.dataStore)
    }
}
