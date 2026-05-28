package com.pokyx.gollections

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.pokyx.gollections.ui.DashboardScreen
import com.pokyx.gollections.ui.CollectionListScreen
import com.pokyx.gollections.ui.AddObjectScreen
import com.pokyx.gollections.ui.CollectionViewModel
import com.pokyx.gollections.ui.ObjectDetailScreen
import com.pokyx.gollections.ui.navigation.DashboardRoute
import com.pokyx.gollections.ui.navigation.CollectionListRoute
import com.pokyx.gollections.ui.navigation.AddObjectRoute
import com.pokyx.gollections.ui.navigation.ObjectDetailRoute
import com.pokyx.gollections.ui.theme.GollectionsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint // <--- Indispensable pour que Hilt fonctionne dans cette Activity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GollectionsTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = DashboardRoute
                ) {
                    composable<DashboardRoute> {
                        DashboardScreen(
                            onCategoryClick = { category ->
                                navController.navigate(CollectionListRoute(categoryName = category))
                            },
                            onAddObjectClick = {
                                navController.navigate(AddObjectRoute)
                            }
                        )
                    }

                    composable<CollectionListRoute> { backStackEntry ->
                        val route: CollectionListRoute = backStackEntry.toRoute()

                        // hiltViewModel() s'occupe de tout : il cherche le ViewModel,
                        // l'instancie avec ses dépendances (le DAO) et respecte son cycle de vie.
                        val viewModel: CollectionViewModel = hiltViewModel()

                        CollectionListScreen(
                            categoryName = route.categoryName,
                            viewModel = viewModel,
                            onBackClick = { navController.popBackStack() },
                            onItemClick = { itemId ->
                                // Navigation vers l'écran détail de l'objet cliqué
                                navController.navigate(ObjectDetailRoute(itemId = itemId))
                            }
                        )
                    }

                    composable<AddObjectRoute> {
                        val viewModel: CollectionViewModel = hiltViewModel()

                        AddObjectScreen(
                            onBackClick = { navController.popBackStack() },
                            onSaveClick = { title, category, subCategory, purchaseDate, price ->
                                viewModel.addItem(title, category, subCategory, purchaseDate, price)
                                navController.popBackStack()
                            }
                        )
                    }

                    composable<ObjectDetailRoute> { backStackEntry ->
                        val route: ObjectDetailRoute = backStackEntry.toRoute()
                        ObjectDetailScreen(
                            itemId = route.itemId,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}