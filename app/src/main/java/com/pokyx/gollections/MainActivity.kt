package com.pokyx.gollections

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.pokyx.gollections.ui.screens.DashboardScreen
import com.pokyx.gollections.ui.screens.CollectionListScreen
import com.pokyx.gollections.ui.screens.AddItemScreen
import com.pokyx.gollections.ui.screens.ItemDetailScreen
import com.pokyx.gollections.ui.screens.EditItemScreen
import com.pokyx.gollections.ui.viewmodels.CollectionViewModel
import com.pokyx.gollections.ui.navigation.DashboardRoute
import com.pokyx.gollections.ui.navigation.CollectionListRoute
import com.pokyx.gollections.ui.navigation.AddItemRoute
import com.pokyx.gollections.ui.navigation.ItemDetailRoute
import com.pokyx.gollections.ui.navigation.EditItemRoute
import com.pokyx.gollections.ui.theme.GollectionsTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            GollectionsTheme {
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
                            DashboardScreen(
                                onCollectionClick = { collectionId ->
                                    navController.navigate(CollectionListRoute(collectionId = collectionId))
                                },
                                onAddItemClick = {
                                    navController.navigate(AddItemRoute())
                                }
                            )
                        }

                        composable<CollectionListRoute> { backStackEntry ->
                            val route: CollectionListRoute = backStackEntry.toRoute()
                            val viewModel: CollectionViewModel = hiltViewModel()

                            CollectionListScreen(
                                collectionId = route.collectionId,
                                viewModel = viewModel,
                                onBackClick = { navController.popBackStack() },
                                onItemClick = { itemId ->
                                    navController.navigate(ItemDetailRoute(itemId = itemId))
                                },
                                onAddItemClick = {
                                    navController.navigate(AddItemRoute(preSelectedCollectionId = route.collectionId))
                                },
                                onCollectionClick = { subCollectionId ->
                                    navController.navigate(CollectionListRoute(collectionId = subCollectionId))
                                }
                            )
                        }

                        composable<AddItemRoute> { backStackEntry ->
                            val route: AddItemRoute = backStackEntry.toRoute()
                            val viewModel: CollectionViewModel = hiltViewModel()

                            AddItemScreen(
                                preSelectedCollectionId = route.preSelectedCollectionId,
                                onBackClick = { navController.popBackStack() },
                                onSaveClick = { newItem ->
                                    viewModel.insertItem(newItem)
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable<ItemDetailRoute> { backStackEntry ->
                            val route: ItemDetailRoute = backStackEntry.toRoute()
                            ItemDetailScreen(
                                itemId = route.itemId,
                                onBackClick = { navController.popBackStack() },
                                onEditClick = { id ->
                                    navController.navigate(EditItemRoute(itemId = id))
                                }
                            )
                        }

                        composable<EditItemRoute> { backStackEntry ->
                            val route: EditItemRoute = backStackEntry.toRoute()
                            val viewModel: CollectionViewModel = hiltViewModel()

                            EditItemScreen(
                                itemId = route.itemId,
                                onBackClick = { navController.popBackStack() },
                                onSaveClick = { updatedItem ->
                                    viewModel.updateItem(updatedItem)
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