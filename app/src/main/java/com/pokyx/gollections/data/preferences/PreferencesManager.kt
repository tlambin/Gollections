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

enum class ThemeConfig(val title: String) { LIGHT("Clair"), DARK("Sombre"), SYSTEM("Système") }
enum class LanguageConfig(val tag: String, val title: String) { FR("fr", "Français"), EN("en", "English") }

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.dataStore

    // On transforme la String sauvegardée en un Enum strict. En cas d'erreur (ancienne valeur), on force "SYSTEM"
    val themeFlow: Flow<ThemeConfig> = dataStore.data.map { prefs ->
        val savedValue = prefs[THEME_KEY] ?: ThemeConfig.SYSTEM.name
        runCatching { ThemeConfig.valueOf(savedValue) }.getOrDefault(ThemeConfig.SYSTEM)
    }

    val dynamicColorsFlow: Flow<Boolean> = dataStore.data.map { it[DYNAMIC_COLORS_KEY] ?: true }

    val languageFlow: Flow<LanguageConfig> = dataStore.data.map { prefs ->
        val savedValue = prefs[LANGUAGE_KEY] ?: LanguageConfig.FR.name
        runCatching { LanguageConfig.valueOf(savedValue) }.getOrDefault(LanguageConfig.FR)
    }

    suspend fun updateTheme(theme: ThemeConfig) { dataStore.edit { it[THEME_KEY] = theme.name } }
    suspend fun updateDynamicColors(useDynamic: Boolean) { dataStore.edit { it[DYNAMIC_COLORS_KEY] = useDynamic } }
    suspend fun updateLanguage(lang: LanguageConfig) { dataStore.edit { it[LANGUAGE_KEY] = lang.name } }

    companion object {
        val THEME_KEY = stringPreferencesKey("theme")
        val DYNAMIC_COLORS_KEY = booleanPreferencesKey("dynamic_colors")
        val LANGUAGE_KEY = stringPreferencesKey("language")
    }
}