package com.memoise.ledger.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences",
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val themePreference: Flow<ThemePreference> = context.userPreferencesDataStore.data
        .map { preferences ->
            ThemePreference.fromStoredValue(preferences[THEME_PREFERENCE])
        }
    val recentSearches: Flow<List<String>> = context.userPreferencesDataStore.data
        .map { preferences ->
            preferences[RECENT_SEARCHES]
                ?.lineSequence()
                ?.map(String::trim)
                ?.filter(String::isNotBlank)
                ?.take(MAX_RECENT_SEARCHES)
                ?.toList()
                .orEmpty()
        }

    suspend fun setThemePreference(preference: ThemePreference) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[THEME_PREFERENCE] = preference.storedValue
        }
    }

    suspend fun rememberSearch(query: String) {
        val normalized = query.trim().replace(Regex("\\s+"), " ")
        if (normalized.isBlank()) return
        context.userPreferencesDataStore.edit { preferences ->
            val existing = preferences[RECENT_SEARCHES]
                ?.lineSequence()
                ?.map(String::trim)
                ?.filter(String::isNotBlank)
                .orEmpty()
            val updated = sequenceOf(normalized) + existing.filterNot {
                it.equals(normalized, ignoreCase = true)
            }
            preferences[RECENT_SEARCHES] = updated.take(MAX_RECENT_SEARCHES).joinToString("\n")
        }
    }

    suspend fun clearRecentSearches() {
        context.userPreferencesDataStore.edit { preferences ->
            preferences.remove(RECENT_SEARCHES)
        }
    }

    private companion object {
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches")
        const val MAX_RECENT_SEARCHES = 5
    }
}
