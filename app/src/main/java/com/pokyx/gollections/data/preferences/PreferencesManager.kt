package com.pokyx.gollections.data.preferences

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

val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.dataStore

    val themeFlow: Flow<String> = dataStore.data.map { it[THEME_KEY] ?: "Système" }
    val dynamicColorsFlow: Flow<Boolean> = dataStore.data.map { it[DYNAMIC_COLORS_KEY] ?: true }
    val languageFlow: Flow<String> = dataStore.data.map { it[LANGUAGE_KEY] ?: "Français" }

    suspend fun updateTheme(theme: String) { dataStore.edit { it[THEME_KEY] = theme } }
    suspend fun updateDynamicColors(useDynamic: Boolean) { dataStore.edit { it[DYNAMIC_COLORS_KEY] = useDynamic } }
    suspend fun updateLanguage(lang: String) { dataStore.edit { it[LANGUAGE_KEY] = lang } }

    companion object {
        val THEME_KEY = stringPreferencesKey("theme")
        val DYNAMIC_COLORS_KEY = booleanPreferencesKey("dynamic_colors")
        val LANGUAGE_KEY = stringPreferencesKey("language")
    }
}