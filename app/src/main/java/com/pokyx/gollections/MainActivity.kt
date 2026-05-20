package com.pokyx.gollections

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pokyx.gollections.ui.DashboardScreen
import com.pokyx.gollections.ui.CollectionListScreen
import com.pokyx.gollections.ui.AddObjectScreen
import com.pokyx.gollections.ui.CollectionViewModel
import com.pokyx.gollections.ui.CollectionViewModelFactory
import com.pokyx.gollections.ui.theme.GollectionsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GollectionsTheme {
                // Initialisation officielle du ViewModel avec sa Factory
                val viewModel: CollectionViewModel = viewModel(
                    factory = CollectionViewModelFactory(application)
                )

                var currentScreen by remember { mutableStateOf("dashboard") }
                var selectedCategory by remember { mutableStateOf("") }

                when (currentScreen) {
                    "dashboard" -> {
                        DashboardScreen(
                            onCategoryClick = { category ->
                                selectedCategory = category
                                currentScreen = "list"
                            },
                            onAddObjectClick = { currentScreen = "add" }
                        )
                    }
                    "list" -> {
                        // On donne le ViewModel à l'écran de liste pour lire la BDD
                        CollectionListScreen(
                            categoryName = selectedCategory,
                            viewModel = viewModel,
                            onBackClick = { currentScreen = "dashboard" }
                        )
                    }
                    "add" -> {
                        AddObjectScreen(
                            onBackClick = { currentScreen = "dashboard" },
                            onSaveClick = { title, year, category, subCategory ->
                                // ACTION REELLE : Sauvegarde dans la base de données Room
                                viewModel.addItem(title, year, category, subCategory)
                                currentScreen = "dashboard"
                            }
                        )
                    }
                }
            }
        }
    }
}