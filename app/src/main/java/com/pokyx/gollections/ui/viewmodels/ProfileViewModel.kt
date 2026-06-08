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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// OPTIMISATION : Création d'une classe scellée pour les événements UI (Toasts, Snackbar...)
sealed class ProfileUiEvent {
    object ExportSuccess : ProfileUiEvent()
    data class ExportError(val message: String?) : ProfileUiEvent()
    object ImportSuccess : ProfileUiEvent()
    data class ImportError(val message: String?) : ProfileUiEvent()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val backupRepository: BackupRepository
) : ViewModel() {

    val theme = prefs.themeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeConfig.SYSTEM)
    val dynamicColors = prefs.dynamicColorsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val language = prefs.languageFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LanguageConfig.FR)

    // OPTIMISATION : Utilisation d'un Channel à la place des Callbacks
    private val _uiEvent = Channel<ProfileUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun setTheme(theme: ThemeConfig) = viewModelScope.launch { prefs.updateTheme(theme) }
    fun setDynamicColors(use: Boolean) = viewModelScope.launch { prefs.updateDynamicColors(use) }

    fun setLanguage(lang: LanguageConfig) {
        viewModelScope.launch {
            prefs.updateLanguage(lang)
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang.tag))
        }
    }

    fun exportDatabase(uri: Uri) {
        viewModelScope.launch {
            val result = backupRepository.exportDatabase(uri)
            if (result.isSuccess) {
                _uiEvent.send(ProfileUiEvent.ExportSuccess)
            } else {
                _uiEvent.send(ProfileUiEvent.ExportError(result.exceptionOrNull()?.localizedMessage))
            }
        }
    }

    fun importDatabase(uri: Uri) {
        viewModelScope.launch {
            val result = backupRepository.importDatabase(uri)
            if (result.isSuccess) {
                _uiEvent.send(ProfileUiEvent.ImportSuccess)
            } else {
                _uiEvent.send(ProfileUiEvent.ImportError(result.exceptionOrNull()?.localizedMessage))
            }
        }
    }
}