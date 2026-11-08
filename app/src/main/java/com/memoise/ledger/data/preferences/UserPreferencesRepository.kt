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

    suspend fun setThemePreference(preference: ThemePreference) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[THEME_PREFERENCE] = preference.storedValue
        }
    }

    private companion object {
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
    }
}
