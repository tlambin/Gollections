package com.pokyx.gollections.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pokyx.gollections.data.Category
import com.pokyx.gollections.data.CollectionItem

@Composable
fun DashboardScreen(
    onCategoryClick: (String) -> Unit,
    onAddObjectClick: () -> Unit,
    viewModel: CollectionViewModel = hiltViewModel() // Injection automatique via Hilt
) {
    // Collecte des flux de la BDD sous forme d'états Compose
    val categories by viewModel.allCategories.collectAsState(initial = emptyList())
    val allItems by viewModel.allItems.collectAsState(initial = emptyList())

    // État pour la recherche globale
    var searchQuery by remember { mutableStateOf("") }
    // Collecte réactive des résultats filtrés par la recherche
    val searchResults by viewModel.searchItems(searchQuery).collectAsState(initial = emptyList())

    // État pour la boîte de dialogue de création de catégorie
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Text(
                text = "Gollections",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddObjectClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Text(text = "+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Barre de Recherche globale
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher un média, un jeu...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = {
                        Text(text = "🔍", modifier = Modifier.padding(start = 4.dp), fontSize = 18.sp)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            Text(
                                text = "❌",
                                modifier = Modifier
                                    .clickable { searchQuery = "" }
                                    .padding(end = 4.dp),
                                fontSize = 14.sp
                            )
                        }
                    }
                )
            }

            if (searchQuery.isEmpty()) {
                // --- MODE NORMAL : Affichage du Dashboard ---

                // 1. Le Slider de Statistiques (Calculé dynamiquement)
                item {
                    StatsSlider(
                        totalItems = allItems.size,
                        totalCategories = categories.size,
                        loanedItems = allItems.count { it.isLoaned }
                    )
                }

                // 2. Le Titre de la section
                item {
                    Text(
                        text = "Mes Collections",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                // 3. La Grille des Catégories dynamique
                item {
                    CategoriesGrid(
                        categories = categories,
                        items = allItems,
                        onCategoryClick = onCategoryClick,
                        onAddCategoryClick = { showAddCategoryDialog = true }
                    )
                }
            } else {
                // --- MODE RECHERCHE : Affichage des résultats en direct ---
                item {
                    Text(
                        text = "Résultats de recherche (${searchResults.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                if (searchResults.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                            Text(text = "Aucun objet trouvé 😕", color = Color.Gray)
                        }
                    }
                } else {
                    items(searchResults) { item ->
                        ListItem(
                            headlineContent = { Text(item.title, fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("${item.category} • ${item.subCategory} (${item.year})") },
                            leadingContent = { Text(getEmojiForCategory(item.category), fontSize = 24.sp) },
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .clickable { onCategoryClick(item.category) } // Ouvre la liste correspondante
                        )
                    }
                }
            }
        }
    }

    // Boîte de dialogue pour ajouter une catégorie personnalisée
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddCategoryDialog = false
                newCategoryName = ""
            },
            title = { Text("Nouvelle Collection") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Nom de la catégorie (ex: Livres, Mangas...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addCategory(newCategoryName.trim())
                            showAddCategoryDialog = false
                            newCategoryName = ""
                        }
                    }
                ) {
                    Text("Créer")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddCategoryDialog = false
                    newCategoryName = ""
                }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun StatsSlider(totalItems: Int, totalCategories: Int, loanedItems: Int) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 32.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            Card(
                modifier = Modifier.fillMaxWidth().height(130.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    when (page) {
                        0 -> StatCardContent(title = "Total Objets", value = totalItems.toString(), sub = "Tous médias confondus")
                        1 -> StatCardContent(title = "Collections distinctes", value = totalCategories.toString(), sub = "Catégories enregistrées")
                        2 -> StatCardContent(title = "Prêts en cours", value = loanedItems.toString(), sub = "Objets confiés à des proches")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(Modifier.wrapContentHeight().fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(pagerState.pageCount) { iteration ->
                val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.LightGray
                Box(modifier = Modifier.padding(2.dp).clip(CircleShape).background(color).size(8.dp))
            }
        }
    }
}

@Composable
fun StatCardContent(title: String, value: String, sub: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
        Text(text = value, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(text = sub, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
    }
}

@Composable
fun CategoriesGrid(
    categories: List<Category>,
    items: List<CollectionItem>,
    onCategoryClick: (String) -> Unit,
    onAddCategoryClick: () -> Unit
) {
    // Le nombre de slots total inclut toutes les catégories + le bouton d'ajout
    val totalSlots = categories.size + 1

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Construction dynamique ligne par ligne (2 éléments par ligne)
        for (i in 0 until totalSlots step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Premier emplacement de la ligne
                if (i < categories.size) {
                    CategoryItemCard(categories[i], items, onCategoryClick, Modifier.weight(1f))
                } else if (i == categories.size) {
                    AddCategoryCard(onClick = onAddCategoryClick, modifier = Modifier.weight(1f))
                }

                // Second emplacement de la ligne
                if (i + 1 < categories.size) {
                    CategoryItemCard(categories[i + 1], items, onCategoryClick, Modifier.weight(1f))
                } else if (i + 1 == categories.size) {
                    AddCategoryCard(onClick = onAddCategoryClick, modifier = Modifier.weight(1f))
                } else {
                    // Espace vide pour équilibrer la ligne si impaire
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun CategoryItemCard(
    category: Category,
    items: List<CollectionItem>,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val count = items.count { it.category == category.name }
    val unit = when (category.name.lowercase()) {
        "blu-ray", "films" -> if (count <= 1) "film" else "films"
        "vinyles" -> if (count <= 1) "album" else "albums"
        "jeux vidéo", "jeux" -> if (count <= 1) "jeu" else "jeux"
        "livres", "mangas" -> if (count <= 1) "livre" else "livres"
        else -> if (count <= 1) "objet" else "objets"
    }

    CategoryCard(
        title = category.name,
        count = "$count $unit",
        emoji = getEmojiForCategory(category.name),
        onClick = { onCategoryClick(category.name) },
        modifier = modifier
    )
}

@Composable
fun CategoryCard(title: String, count: String, emoji: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = emoji, fontSize = 28.sp)
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = count, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun AddCategoryCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "➕", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Nouvelle collection",
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Dictionnaire intelligent d'émojis automatiques pour l'UI
fun getEmojiForCategory(categoryName: String): String {
    return when (categoryName.lowercase().trim()) {
        "blu-ray", "bluray", "film", "films", "cinéma", "cinema" -> "🎬"
        "vinyles", "vinyle", "musique", "disques", "disque", "cd" -> "🎵"
        "jeux vidéo", "jeux", "jeux video", "gaming", "switch", "ps5" -> "🎮"
        "livres", "livre", "mangas", "manga", "bd", "romans" -> "📚"
        "figurines", "figurine", "pop" -> "🧸"
        "jeux de société", "jeux de societe", "cartes" -> "🎲"
        else -> "📦" // Émoji par défaut pour le reste
    }
}