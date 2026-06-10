package com.pokyx.gollections.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.pokyx.gollections.ui.screens.AddItemScreen
import com.pokyx.gollections.ui.screens.CollectionDetailScreen
import com.pokyx.gollections.ui.screens.DashboardScreen
import com.pokyx.gollections.ui.screens.EditItemScreen
import com.pokyx.gollections.ui.screens.ItemDetailScreen
import com.pokyx.gollections.ui.screens.ProfileScreen
import com.pokyx.gollections.ui.viewmodels.CollectionDetailViewModel
import com.pokyx.gollections.ui.viewmodels.DashboardViewModel
import com.pokyx.gollections.ui.viewmodels.ItemViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GollectionsNavGraph(navController: NavHostController) {
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = DashboardRoute,
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) + fadeIn(tween(300))
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) + fadeOut(tween(300))
            },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) + fadeIn(tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) + fadeOut(tween(300))
            }
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
                    onProfileClick = {
                        navController.navigate(ProfileRoute)
                    }
                )
            }

            composable<ProfileRoute> {
                ProfileScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable<CollectionDetailRoute> { backStackEntry ->
                val route: CollectionDetailRoute = backStackEntry.toRoute()
                val viewModel: CollectionDetailViewModel = hiltViewModel()

                CollectionDetailScreen(
                    animatedVisibilityScope = this@composable,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    collectionId = route.collectionId,
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onItemClick = { itemId ->
                        navController.navigate(ItemDetailRoute(itemId = itemId))
                    },
                    onAddItemClick = { title, imageUrl ->
                        navController.navigate(
                            AddItemRoute(
                                preSelectedCollectionId = route.collectionId,
                                scannedTitle = title,
                                scannedImageUrl = imageUrl
                            )
                        )
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
                    scannedTitle = route.scannedTitle,
                    scannedImageUrl = route.scannedImageUrl,
                    onBackClick = { navController.popBackStack() },
                    // ✅ CORRECTION ICI : Ajout du paramètre attachments
                    onSaveClick = { newItem, tags, properties, attachments ->
                        viewModel.insertItemWithTags(newItem, tags, properties, attachments)
                        navController.popBackStack() // Retour instantané après la demande de sauvegarde
                    },
                    viewModel = viewModel
                )
            }

            composable<ItemDetailRoute> { backStackEntry ->
                val route: ItemDetailRoute = backStackEntry.toRoute()
                val viewModel: ItemViewModel = hiltViewModel()

                ItemDetailScreen(
                    animatedVisibilityScope = this@composable,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    itemId = route.itemId,
                    onBackClick = { navController.popBackStack() },
                    onEditClick = { id ->
                        navController.navigate(EditItemRoute(itemId = id))
                    },
                    viewModel = viewModel
                )
            }

            composable<EditItemRoute> { backStackEntry ->
                val route: EditItemRoute = backStackEntry.toRoute()
                val viewModel: ItemViewModel = hiltViewModel()

                EditItemScreen(
                    itemId = route.itemId,
                    onBackClick = { navController.popBackStack() },
                    // ✅ CORRECTION ICI : Ajout du paramètre attachments
                    onSaveClick = { updatedItem, tags, properties, attachments ->
                        viewModel.updateItemWithTags(updatedItem, tags, properties, attachments)
                        navController.popBackStack()
                    },
                    viewModel = viewModel
                )
            }
        }
    }
}