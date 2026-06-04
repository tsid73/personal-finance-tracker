package com.example.finance.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferenceManager(private val context: Context) {
    private val themeKey = booleanPreferencesKey("dark_theme")
    private val monthKey = stringPreferencesKey("selected_month")
    private val syncEnabledKey = booleanPreferencesKey("sync_enabled")
    private val syncBaseUrlKey = stringPreferencesKey("sync_base_url")
    private val biometricEnabledKey = booleanPreferencesKey("biometric_enabled")

    val darkTheme: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[themeKey]
    }

    val selectedMonth: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[monthKey] ?: LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }

    val syncEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[syncEnabledKey] ?: false
    }

    val syncBaseUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[syncBaseUrlKey] ?: ""
    }

    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[biometricEnabledKey] ?: false
    }

    suspend fun setTheme(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = isDark
        }
    }

    suspend fun clearTheme() {
        context.dataStore.edit { preferences ->
            preferences.remove(themeKey)
        }
    }

    suspend fun setMonth(month: String) {
        context.dataStore.edit { preferences ->
            preferences[monthKey] = month
        }
    }

    suspend fun setSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[syncEnabledKey] = enabled
        }
    }

    suspend fun setSyncBaseUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[syncBaseUrlKey] = url.trim().trimEnd('/')
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[biometricEnabledKey] = enabled
        }
    }

    suspend fun getSnapshot(): AppSettingsSnapshot {
        return AppSettingsSnapshot(
            darkTheme = darkTheme.first(),
            selectedMonth = selectedMonth.first(),
            syncEnabled = syncEnabled.first(),
            syncBaseUrl = syncBaseUrl.first(),
            biometricEnabled = biometricEnabled.first()
        )
    }

    suspend fun restoreSnapshot(snapshot: AppSettingsSnapshot) {
        context.dataStore.edit { preferences ->
            if (snapshot.darkTheme == null) {
                preferences.remove(themeKey)
            } else {
                preferences[themeKey] = snapshot.darkTheme
            }
            preferences[monthKey] = snapshot.selectedMonth ?: LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
            preferences[syncEnabledKey] = snapshot.syncEnabled ?: false
            preferences[syncBaseUrlKey] = (snapshot.syncBaseUrl ?: "").trim().trimEnd('/')
            preferences[biometricEnabledKey] = snapshot.biometricEnabled ?: false
        }
    }
}
