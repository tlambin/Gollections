package com.pokyx.gollections.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pokyx.gollections.ui.viewmodels.CollectionViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionListScreen(
    collectionName: String,
    viewModel: CollectionViewModel,
    onBackClick: () -> Unit,
    onItemClick: (Int) -> Unit,
    onAddItemClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Tous") }

    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteCollectionDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf(collectionName) }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    var showCategoryOptionsDialog by remember { mutableStateOf(false) }
    var selectedCategoryToManage by remember { mutableStateOf("") }
    var showRenameCategoryDialog by remember { mutableStateOf(false) }
    var renameCategoryInput by remember { mutableStateOf("") }
    var showDeleteCategoryDialog by remember { mutableStateOf(false) }

    val allCollectionItems by viewModel.getItemsByCollection(collectionName).collectAsState(initial = emptyList())
    val dbCategories by viewModel.getCategoriesForCollection(collectionName).collectAsState(initial = emptyList())

    val categories = remember(dbCategories) { listOf("Tous") + dbCategories.map { it.name } }
    val filteredItems = allCollectionItems.filter { item -> (selectedCategory == "Tous" || item.category == selectedCategory) && (searchQuery.isEmpty() || item.title.contains(searchQuery, ignoreCase = true)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(collectionName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Renommer") },
                                onClick = { showMenu = false; showRenameDialog = true },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) } // <-- Style M3
                            )
                            DropdownMenuItem(
                                text = { Text("Supprimer") },
                                onClick = { showMenu = false; showDeleteCollectionDialog = true },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) } // <-- Style M3
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = onAddItemClick, containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) { Icon(Icons.Default.Add, contentDescription = "Ajouter", modifier = Modifier.size(28.dp)) } }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) }, // ou { searchQuery = it } dans CollectionListScreen
                placeholder = { Text("Rechercher...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp), // 16.dp au lieu de 24.dp dans CollectionListScreen
                shape = CircleShape,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Rechercher", tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) { // ou { searchQuery = "" }
                            Icon(Icons.Default.Clear, contentDescription = "Effacer")
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent, // Enlève la ligne du bas
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                singleLine = true
            )
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                categories.forEach { cat -> CustomCategoryChip(text = cat, isSelected = selectedCategory == cat, onClick = { selectedCategory = cat }, onLongClick = if (cat != "Tous") { { selectedCategoryToManage = cat; showCategoryOptionsDialog = true } } else null) }
                InputChip(
                    selected = false,
                    onClick = { showAddCategoryDialog = true },
                    label = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Ajouter une catégorie",
                            modifier = Modifier.size(16.dp), // Taille discrète adaptée au format Chip
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = InputChipDefaults.inputChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (filteredItems.isEmpty()) { item { Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) { Text("Aucun objet 📦", color = MaterialTheme.colorScheme.outline) } } }
                else {
                    items(filteredItems) { item ->
                        Card(modifier = Modifier.fillMaxWidth().clickable { onItemClick(item.id) }, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (item.imageUrl.isNotBlank()) AsyncImage(model = item.imageUrl, contentDescription = "Miniature", modifier = Modifier.size(50.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                                else Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Text(text = getEmojiForCollection(item.collection), fontSize = 24.sp) }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (item.category.isNotEmpty()) Text(text = item.category, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                        if (item.status != "Non commencé") Text(text = " • ${item.status}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCategoryOptionsDialog) AlertDialog(onDismissRequest = { showCategoryOptionsDialog = false }, title = { Text("Options : $selectedCategoryToManage") }, text = { Text("Que souhaitez-vous faire ?") }, confirmButton = { Button(onClick = { renameCategoryInput = selectedCategoryToManage; showCategoryOptionsDialog = false; showRenameCategoryDialog = true }) { Text("Renommer") } }, dismissButton = { Button(onClick = { showCategoryOptionsDialog = false; showDeleteCategoryDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Supprimer") } })
    if (showRenameCategoryDialog) AlertDialog(onDismissRequest = { showRenameCategoryDialog = false }, title = { Text("Renommer la catégorie") }, text = { OutlinedTextField(value = renameCategoryInput, onValueChange = { renameCategoryInput = it }, singleLine = true) }, confirmButton = { Button(onClick = { if (renameCategoryInput.isNotBlank()) { viewModel.renameCategory(collectionName, selectedCategoryToManage, renameCategoryInput.trim()); if (selectedCategory == selectedCategoryToManage) selectedCategory = renameCategoryInput.trim(); showRenameCategoryDialog = false } }) { Text("Enregistrer") } }, dismissButton = { TextButton(onClick = { showRenameCategoryDialog = false }) { Text("Annuler") } })
    if (showDeleteCategoryDialog) AlertDialog(onDismissRequest = { showDeleteCategoryDialog = false }, title = { Text("Supprimer la catégorie ?") }, text = { Text("Voulez-vous supprimer le format \"$selectedCategoryToManage\" ?") }, confirmButton = { Button(onClick = { val catToDelete = dbCategories.find { it.name == selectedCategoryToManage }; catToDelete?.let { viewModel.deleteCategory(it) }; if (selectedCategory == selectedCategoryToManage) selectedCategory = "Tous"; showDeleteCategoryDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Confirmer") } }, dismissButton = { TextButton(onClick = { showDeleteCategoryDialog = false }) { Text("Annuler") } })
    if (showRenameDialog) AlertDialog(onDismissRequest = { showRenameDialog = false }, title = { Text("Renommer la collection") }, text = { OutlinedTextField(value = renameInput, onValueChange = { renameInput = it }, singleLine = true) }, confirmButton = { Button(onClick = { if (renameInput.isNotBlank()) { viewModel.renameCollection(collectionName, renameInput.trim()); showRenameDialog = false; onBackClick() } }) { Text("Enregistrer") } }, dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Annuler") } })
    if (showDeleteCollectionDialog) AlertDialog(onDismissRequest = { showDeleteCollectionDialog = false }, title = { Text("Tout supprimer ?") }, text = { Text("Attention ! Cette action est irréversible.") }, confirmButton = { Button(onClick = { viewModel.deleteCollection(com.pokyx.gollections.data.Collection(name = collectionName)); showDeleteCollectionDialog = false; onBackClick() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Tout supprimer") } }, dismissButton = { TextButton(onClick = { showDeleteCollectionDialog = false }) { Text("Annuler") } })
    if (showAddCategoryDialog) AlertDialog(onDismissRequest = { showAddCategoryDialog = false; newCategoryName = "" }, title = { Text("Nouvelle Catégorie") }, text = { OutlinedTextField(value = newCategoryName, onValueChange = { newCategoryName = it }, label = { Text("Nom (ex: 4K, PS5...)") }, singleLine = true) }, confirmButton = { Button(onClick = { if (newCategoryName.isNotBlank()) { viewModel.insertCategory(newCategoryName.trim(), collectionName); showAddCategoryDialog = false; newCategoryName = "" } }) { Text("Créer") } }, dismissButton = { TextButton(onClick = { showAddCategoryDialog = false; newCategoryName = "" }) { Text("Annuler") } })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomCategoryChip(text: String, isSelected: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)?) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val borderModifier = if (!isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)) else Modifier
    Box(modifier = Modifier.background(containerColor, RoundedCornerShape(8.dp)).then(borderModifier).combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) { Text(text = text, color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
}