package com.pokyx.gollections

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.pokyx.gollections.ui.navigation.AddItemRoute
import com.pokyx.gollections.ui.navigation.CollectionDetailRoute
import com.pokyx.gollections.ui.navigation.DashboardRoute
import com.pokyx.gollections.ui.navigation.EditItemRoute
import com.pokyx.gollections.ui.navigation.ItemDetailRoute
import com.pokyx.gollections.ui.navigation.ProfileRoute // <-- IMPORT AJOUTÉ
import com.pokyx.gollections.ui.screens.AddItemScreen
import com.pokyx.gollections.ui.screens.CollectionDetailScreen
import com.pokyx.gollections.ui.screens.DashboardScreen
import com.pokyx.gollections.ui.screens.EditItemScreen
import com.pokyx.gollections.ui.screens.ItemDetailScreen
import com.pokyx.gollections.ui.screens.ProfileScreen // <-- IMPORT AJOUTÉ
import com.pokyx.gollections.ui.theme.GollectionsTheme
import com.pokyx.gollections.ui.viewmodels.CollectionDetailViewModel
import com.pokyx.gollections.ui.viewmodels.DashboardViewModel
import com.pokyx.gollections.ui.viewmodels.ItemViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import com.pokyx.gollections.data.preferences.PreferencesManager
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager // <-- On injecte les préférences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            // On observe le thème et les couleurs
            val theme by preferencesManager.themeFlow.collectAsState(initial = "Système")
            val dynamicColor by preferencesManager.dynamicColorsFlow.collectAsState(initial = true)

            // On déduit si on doit être en mode sombre
            val darkTheme = when (theme) {
                "Sombre" -> true
                "Clair" -> false
                else -> isSystemInDarkTheme()
            }

            // On applique tes choix au thème Compose
            GollectionsTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = DashboardRoute,
                        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) + fadeIn(tween(300)) },
                        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) + fadeOut(tween(300)) },
                        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) + fadeIn(tween(300)) },
                        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) + fadeOut(tween(300)) }
                    ) {
                        composable<DashboardRoute> {
                            val viewModel: DashboardViewModel = hiltViewModel()
                            DashboardScreen(
                                viewModel = viewModel,
                                onCollectionClick = { collectionId ->
                                    navController.navigate(CollectionDetailRoute(collectionId = collectionId))
                                },
                                onAddItemClick = { title, imageUrl ->
                                    navController.navigate(AddItemRoute(scannedTitle = title, scannedImageUrl = imageUrl))
                                },
                                onProfileClick = { // <-- PARAMÈTRE AJOUTÉ ICI
                                    navController.navigate(ProfileRoute)
                                }
                            )
                        }

                        // <-- NOUVEL ÉCRAN AJOUTÉ ICI
                        composable<ProfileRoute> {
                            ProfileScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        composable<CollectionDetailRoute> { backStackEntry ->
                            val route: CollectionDetailRoute = backStackEntry.toRoute()
                            val viewModel: CollectionDetailViewModel = hiltViewModel()

                            CollectionDetailScreen(
                                collectionId = route.collectionId,
                                viewModel = viewModel,
                                onBackClick = { navController.popBackStack() },
                                onItemClick = { itemId ->
                                    navController.navigate(ItemDetailRoute(itemId = itemId))
                                },
                                onAddItemClick = { title, imageUrl ->
                                    navController.navigate(AddItemRoute(
                                        preSelectedCollectionId = route.collectionId,
                                        scannedTitle = title,
                                        scannedImageUrl = imageUrl
                                    ))
                                },
                                onCollectionClick = { subCollectionId ->
                                    navController.navigate(CollectionDetailRoute(collectionId = subCollectionId))
                                }
                            )
                        }

                        composable<AddItemRoute> { backStackEntry ->
                            val route: AddItemRoute = backStackEntry.toRoute()
                            val viewModel: ItemViewModel = hiltViewModel()

                            AddItemScreen(
                                preSelectedCollectionId = route.preSelectedCollectionId,
                                viewModel = viewModel,
                                scannedTitle = route.scannedTitle,
                                scannedImageUrl = route.scannedImageUrl,
                                onBackClick = { navController.popBackStack() },
                                onSaveClick = { newItem, tags, properties ->
                                    viewModel.insertItemWithTags(newItem, tags, properties)
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable<ItemDetailRoute> { backStackEntry ->
                            val route: ItemDetailRoute = backStackEntry.toRoute()
                            val viewModel: ItemViewModel = hiltViewModel()

                            ItemDetailScreen(
                                itemId = route.itemId,
                                viewModel = viewModel,
                                onBackClick = { navController.popBackStack() },
                                onEditClick = { id ->
                                    navController.navigate(EditItemRoute(itemId = id))
                                }
                            )
                        }

                        composable<EditItemRoute> { backStackEntry ->
                            val route: EditItemRoute = backStackEntry.toRoute()
                            val viewModel: ItemViewModel = hiltViewModel()

                            EditItemScreen(
                                itemId = route.itemId,
                                viewModel = viewModel,
                                onBackClick = { navController.popBackStack() },
                                onSaveClick = { updatedItem, tags, properties ->
                                    viewModel.updateItemWithTags(updatedItem, tags, properties)
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}