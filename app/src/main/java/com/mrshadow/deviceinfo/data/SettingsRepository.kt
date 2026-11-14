package com.mrshadow.deviceinfo.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")
    private val languageKey = stringPreferencesKey("language")
    private val autoRefreshKey = booleanPreferencesKey("auto_refresh")
    val settings: Flow<Settings> = context.settingsStore.data.map {
        Settings(it[themeKey] ?: "Dark", it[languageKey] ?: "English", it[autoRefreshKey] ?: true)
    }
    suspend fun setTheme(value: String) { context.settingsStore.edit { it[themeKey] = value } }
    suspend fun setLanguage(value: String) { context.settingsStore.edit { it[languageKey] = value } }
    suspend fun setAutoRefresh(value: Boolean) { context.settingsStore.edit { it[autoRefreshKey] = value } }
}
data class Settings(val theme: String, val language: String, val autoRefresh: Boolean)
