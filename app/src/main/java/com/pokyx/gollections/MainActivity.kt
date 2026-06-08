package com.pokyx.gollections

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.pokyx.gollections.data.preferences.PreferencesManager
import com.pokyx.gollections.data.preferences.ThemeConfig
import com.pokyx.gollections.ui.navigation.GollectionsNavGraph
import com.pokyx.gollections.ui.theme.GollectionsTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Conventionnellement placé juste après le super.onCreate

        setContent {
            // OPTIMISATION : Utilisation de collectAsStateWithLifecycle pour préserver la batterie
            val themePref by preferencesManager.themeFlow.collectAsStateWithLifecycle(initialValue = ThemeConfig.SYSTEM)
            val dynamicColor by preferencesManager.dynamicColorsFlow.collectAsStateWithLifecycle(initialValue = true)

            val darkTheme = when (themePref) {
                ThemeConfig.DARK -> true
                ThemeConfig.LIGHT -> false
                ThemeConfig.SYSTEM -> isSystemInDarkTheme()
            }

            GollectionsTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    GollectionsNavGraph(navController = navController)
                }
            }
        }
    }
}