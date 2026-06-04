package com.pokyx.gollections.ui.viewmodels

import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokyx.gollections.data.preferences.LanguageConfig
import com.pokyx.gollections.data.preferences.PreferencesManager
import com.pokyx.gollections.data.preferences.ThemeConfig
import com.pokyx.gollections.data.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val backupRepository: BackupRepository
) : ViewModel() {

    val theme = prefs.themeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeConfig.SYSTEM)
    val dynamicColors = prefs.dynamicColorsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val language = prefs.languageFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LanguageConfig.FR)

    fun setTheme(theme: ThemeConfig) = viewModelScope.launch { prefs.updateTheme(theme) }
    fun setDynamicColors(use: Boolean) = viewModelScope.launch { prefs.updateDynamicColors(use) }

    fun setLanguage(lang: LanguageConfig) {
        viewModelScope.launch {
            prefs.updateLanguage(lang)
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang.tag))
        }
    }

    fun exportDatabase(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = backupRepository.exportDatabase(uri)
            onResult(result.isSuccess)
        }
    }

    fun importDatabase(uri: Uri, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = backupRepository.importDatabase(uri)
            onResult(result.isSuccess, result.exceptionOrNull()?.localizedMessage)
        }
    }
}