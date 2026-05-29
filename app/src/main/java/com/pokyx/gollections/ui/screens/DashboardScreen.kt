package com.pokyx.gollections.ui.screens

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
import com.pokyx.gollections.data.Collection
import com.pokyx.gollections.data.CollectionItem
import com.pokyx.gollections.ui.viewmodels.CollectionViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onCollectionClick: (Long) -> Unit,
    onAddItemClick: () -> Unit,
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val rootCollections by viewModel.rootCollections.collectAsState()
    val allCollections by viewModel.collections.collectAsState()
    val allItems by viewModel.allItems.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchedItems.collectAsState()

    var showAddCollectionDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "Gollections", fontWeight = FontWeight.Bold) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = onAddItemClick, containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) { Icon(Icons.Default.Add, contentDescription = "Ajouter", modifier = Modifier.size(28.dp)) } }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues), contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item {
                TextField(
                    value = searchQuery, onValueChange = { viewModel.updateSearchQuery(it) }, placeholder = { Text("Rechercher...") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), shape = CircleShape, leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Rechercher", tint = MaterialTheme.colorScheme.primary) }, trailingIcon = { if (searchQuery.isNotEmpty()) { IconButton(onClick = { viewModel.updateSearchQuery("") }) { Icon(Icons.Default.Clear, contentDescription = "Effacer") } } },
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent, focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant), singleLine = true
                )
            }

            if (searchQuery.isEmpty()) {
                item { StatsSlider(totalItems = allItems.size, totalCollections = rootCollections.size, loanedItems = allItems.count { it.isLoaned }) }
                item { Text(text = "Mes Collections", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 24.dp)) }
                item { CollectionsGrid(collections = rootCollections, allCollections = allCollections, items = allItems, onCollectionClick = onCollectionClick, onAddCollectionClick = { showAddCollectionDialog = true }, viewModel = viewModel) }
            } else {
                item { Text(text = "Résultats de recherche (${searchResults.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 24.dp)) }
                if (searchResults.isEmpty()) { item { Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) { Text("Aucun objet trouvé 😕", color = Color.Gray) } } }
                else {
                    items(searchResults) { item ->
                        val parentCol = allCollections.find { it.id == item.collectionId }
                        val colName = parentCol?.name ?: "Inconnu"
                        val tagsStr = item.tags.replace(",", " • ")
                        ListItem(
                            headlineContent = { Text(item.title, fontWeight = FontWeight.SemiBold) },
                            supportingContent = { if (tagsStr.isNotEmpty()) Text("$colName • $tagsStr") else Text(colName) },
                            leadingContent = { Text(getEmojiForCollection(colName), fontSize = 24.sp) },
                            modifier = Modifier.padding(horizontal = 24.dp).clickable { onCollectionClick(item.collectionId) }
                        )
                    }
                }
            }
        }
    }

    if (showAddCollectionDialog) { AlertDialog(onDismissRequest = { showAddCollectionDialog = false; newCollectionName = "" }, title = { Text("Nouvelle Collection") }, text = { OutlinedTextField(value = newCollectionName, onValueChange = { newCollectionName = it }, label = { Text("Nom (ex: Livres, Mangas...)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }, confirmButton = { Button(onClick = { if (newCollectionName.isNotBlank()) { viewModel.insertCollection(newCollectionName.trim(), parentId = null); showAddCollectionDialog = false; newCollectionName = "" } }) { Text("Créer") } }, dismissButton = { TextButton(onClick = { showAddCollectionDialog = false; newCollectionName = "" }) { Text("Annuler") } }) }
}

@Composable
fun StatsSlider(totalItems: Int, totalCollections: Int, loanedItems: Int) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(state = pagerState, contentPadding = PaddingValues(horizontal = 32.dp), pageSpacing = 16.dp) { page ->
            Card(modifier = Modifier.fillMaxWidth().height(130.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    when (page) {
                        0 -> StatCardContent(title = "Total Objets", value = totalItems.toString(), sub = "Tous médias confondus")
                        1 -> StatCardContent(title = "Collections racines", value = totalCollections.toString(), sub = "Dossiers principaux")
                        2 -> StatCardContent(title = "Prêts en cours", value = loanedItems.toString(), sub = "Objets confiés à des proches")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(Modifier.wrapContentHeight().fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(pagerState.pageCount) { iteration -> Box(modifier = Modifier.padding(2.dp).clip(CircleShape).background(if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.LightGray).size(8.dp)) }
        }
    }
}

@Composable
fun StatCardContent(title: String, value: String, sub: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(text = title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)); Text(text = value, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer); Text(text = sub, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)) } }

@Composable
fun CollectionsGrid(collections: List<Collection>, allCollections: List<Collection>, items: List<CollectionItem>, onCollectionClick: (Long) -> Unit, onAddCollectionClick: () -> Unit, viewModel: CollectionViewModel) {
    val totalSlots = collections.size + 1
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        for (i in 0 until totalSlots step 2) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (i < collections.size) CollectionItemCard(collections[i], allCollections, items, onCollectionClick, Modifier.weight(1f), viewModel) else if (i == collections.size) AddCollectionCard(onClick = onAddCollectionClick, modifier = Modifier.weight(1f))
                if (i + 1 < collections.size) CollectionItemCard(collections[i + 1], allCollections, items, onCollectionClick, Modifier.weight(1f), viewModel) else if (i + 1 == collections.size) AddCollectionCard(onClick = onAddCollectionClick, modifier = Modifier.weight(1f)) else Box(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun CollectionItemCard(collection: Collection, allCollections: List<Collection>, items: List<CollectionItem>, onCollectionClick: (Long) -> Unit, modifier: Modifier = Modifier, viewModel: CollectionViewModel) {
    val count = viewModel.getRecursiveItemCount(collection.id, allCollections, items)
    val unit = when (collection.name.lowercase()) {
        "blu-ray", "films" -> if (count <= 1) "film" else "films"
        "vinyles" -> if (count <= 1) "album" else "albums"
        "jeux vidéo", "jeux" -> if (count <= 1) "jeu" else "jeux"
        "livres", "mangas" -> if (count <= 1) "livre" else "livres"
        else -> if (count <= 1) "objet" else "objets"
    }
    CollectionCard(title = collection.name, count = "$count $unit", emoji = getEmojiForCollection(collection.name), onClick = { onCollectionClick(collection.id) }, modifier = modifier)
}

@Composable
fun CollectionCard(title: String, count: String, emoji: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(120.dp).clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) { Text(text = emoji, fontSize = 28.sp); Column { Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(text = count, fontSize = 12.sp, color = Color.Gray) } }
    }
}

@Composable
fun AddCollectionCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(120.dp).clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Icon(imageVector = Icons.Default.Add, contentDescription = "Ajouter une collection", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)); Spacer(modifier = Modifier.height(8.dp)); Text(text = "Nouvelle", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary) }
    }
}

fun getEmojiForCollection(collectionName: String): String {
    return when (collectionName.lowercase().trim()) {
        "blu-ray", "bluray", "film", "films", "cinéma", "cinema" -> "🎬"
        "vinyles", "vinyle", "musique", "disques", "disque", "cd" -> "🎵"
        "jeux vidéo", "jeux", "jeux video", "gaming", "switch", "ps5" -> "🎮"
        "livres", "livre", "mangas", "manga", "bd", "romans" -> "📚"
        "figurines", "figurine", "pop" -> "🧸"
        "jeux de société", "jeux de societe", "cartes" -> "🎲"
        else -> "📦"
    }
}