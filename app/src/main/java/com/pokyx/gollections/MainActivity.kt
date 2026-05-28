package com.pokyx.gollections

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
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

@AndroidEntryPoint // Indispensable pour que Hilt fonctionne dans cette Activity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            GollectionsTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(), // Repousse l'app sous la barre de statut et l'encoche photo
                    color = MaterialTheme.colorScheme.background
                ) {
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
                                    navController.navigate(AddObjectRoute()) // Parenthèses ajoutées pour la data class
                                }
                            )
                        }

                        composable<CollectionListRoute> { backStackEntry ->
                            val route: CollectionListRoute = backStackEntry.toRoute()
                            val viewModel: CollectionViewModel = hiltViewModel()

                            CollectionListScreen(
                                categoryName = route.categoryName,
                                viewModel = viewModel,
                                onBackClick = { navController.popBackStack() },
                                onItemClick = { itemId ->
                                    navController.navigate(ObjectDetailRoute(itemId = itemId))
                                },
                                onAddClick = {
                                    navController.navigate(AddObjectRoute(preSelectedCategory = route.categoryName))
                                }
                            )
                        }

                        composable<AddObjectRoute> { backStackEntry ->
                            val route: AddObjectRoute = backStackEntry.toRoute()
                            val viewModel: CollectionViewModel = hiltViewModel()

                            AddObjectScreen(
                                preSelectedCategory = route.preSelectedCategory,
                                onBackClick = { navController.popBackStack() },
                                // Ajout du paramètre "imageUrl" ici :
                                onSaveClick = { title, category, subCategory, purchaseDate, price, imageUrl ->
                                    // Si ta fonction addItem du ViewModel prend déjà en charge l'imageUrl :
                                    viewModel.addItem(title, category, subCategory, purchaseDate, price, imageUrl)
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
}