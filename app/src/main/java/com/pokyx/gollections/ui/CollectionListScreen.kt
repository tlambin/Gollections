package com.pokyx.gollections.ui

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
import coil.compose.AsyncImage // <-- AJOUT DE COIL POUR LA LISTE
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon

@Composable
fun CollectionListScreen(
    categoryName: String,
    viewModel: CollectionViewModel,
    onBackClick: () -> Unit,
    onItemClick: (Int) -> Unit,
    onAddClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSub by remember { mutableStateOf("Tous") }

    // États pour le menu d'options de la collection principale
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteCollectionDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf(categoryName) }

    // États pour la création de sous-collection
    var showAddSubDialog by remember { mutableStateOf(false) }
    var newSubName by remember { mutableStateOf("") }

    // Gestion des sous-collections via appui long
    var showSubOptionsDialog by remember { mutableStateOf(false) }
    var selectedSubToManage by remember { mutableStateOf("") }
    var showRenameSubDialog by remember { mutableStateOf(false) }
    var renameSubInput by remember { mutableStateOf("") }
    var showDeleteSubDialog by remember { mutableStateOf(false) }

    // Données de la BDD
    val allCategoryItems by viewModel.getItemsByCategory(categoryName).collectAsState(initial = emptyList())
    val dbSubCategories by viewModel.getSubCategoriesByCategory(categoryName).collectAsState(initial = emptyList())

    val subCategories = remember(dbSubCategories) {
        listOf("Tous") + dbSubCategories.map { it.name }
    }

    val filteredItems = allCategoryItems.filter { item ->
        val matchesSub = selectedSub == "Tous" || item.subCategory == selectedSub
        val matchesSearch = searchQuery.isEmpty() || item.title.contains(searchQuery, ignoreCase = true)
        matchesSub && matchesSearch
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⬅ Retour",
                    modifier = Modifier.clickable { onBackClick() }.padding(8.dp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Text("⋮", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("✏️ Renommer la collection") },
                            onClick = { showMenu = false; showRenameDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("🗑️ Supprimer la collection", color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; showDeleteCollectionDialog = true }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = FloatingActionButtonDefaults.shape
            ) {
                Icon(
                    imageVector = Icons.Default.Add, // <-- Syntaxe courte et propre
                    contentDescription = "Ajouter un objet",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Barre de Recherche
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Rechercher dans $categoryName...") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = { Text(text = "🔍", fontSize = 18.sp) }
            )

            // Barre de puces de filtrage
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                subCategories.forEach { sub ->
                    CustomSubChip(
                        text = sub,
                        isSelected = selectedSub == sub,
                        onClick = { selectedSub = sub },
                        onLongClick = if (sub != "Tous") {
                            {
                                selectedSubToManage = sub
                                showSubOptionsDialog = true
                            }
                        } else null
                    )
                }

                InputChip(
                    selected = false,
                    onClick = { showAddSubDialog = true },
                    label = { Text("➕", fontWeight = FontWeight.Bold) },
                    colors = InputChipDefaults.inputChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                )
            }

            // Liste des objets
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filteredItems.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                            Text(text = "Aucun objet dans cette catégorie 📦", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    items(filteredItems) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onItemClick(item.id) },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp), // Réduit légèrement le padding pour harmoniser avec la miniature
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                // --- MODIFICATION : SÉLECTION DYNAMIQUE IMAGE / EMOJI ---
                                if (item.imageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = "Miniature de l'objet",
                                        modifier = Modifier
                                            .size(50.dp) // Taille de la jolie miniature
                                            .clip(RoundedCornerShape(6.dp)), // Coins légèrement arrondis
                                        contentScale = ContentScale.Crop // Coupe proprement pour remplir le carré
                                    )
                                } else {
                                    // Cercle de fond léger autour de l'émoji pour garder l'alignement
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = getLocalEmojiForCategory(item.category), fontSize = 24.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (item.subCategory.isNotEmpty()) {
                                            Text(text = item.subCategory, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                        if (item.status != "Non commencé") {
                                            Text(text = " • ${item.status}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGUES DE GESTION DES SOUS-COLLECTIONS ---
    if (showSubOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showSubOptionsDialog = false },
            title = { Text("Options : $selectedSubToManage") },
            text = { Text("Que souhaitez-vous faire avec cette sous-collection ?") },
            confirmButton = {
                Button(onClick = {
                    renameSubInput = selectedSubToManage
                    showSubOptionsDialog = false
                    showRenameSubDialog = true
                }) { Text("Modifier le nom") }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showSubOptionsDialog = false
                        showDeleteSubDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Supprimer") }
            }
        )
    }

    if (showRenameSubDialog) {
        AlertDialog(
            onDismissRequest = { showRenameSubDialog = false },
            title = { Text("Renommer la sous-collection") },
            text = {
                OutlinedTextField(value = renameSubInput, onValueChange = { renameSubInput = it }, label = { Text("Nouveau nom") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameSubInput.isNotBlank() && renameSubInput.trim() != selectedSubToManage) {
                            viewModel.renameSubCollection(categoryName, selectedSubToManage, renameSubInput.trim())
                            if (selectedSub == selectedSubToManage) {
                                selectedSub = renameSubInput.trim()
                            }
                            showRenameSubDialog = false
                        }
                    }
                ) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { showRenameSubDialog = false }) { Text("Annuler") } }
        )
    }

    if (showDeleteSubDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSubDialog = false },
            title = { Text("Supprimer la sous-collection ?") },
            text = { Text("Voulez-vous supprimer le format \"$selectedSubToManage\" ? Les objets correspondants perdront simplement cette étiquette mais ne seront pas supprimés.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSubCollection(categoryName, selectedSubToManage)
                        if (selectedSub == selectedSubToManage) {
                            selectedSub = "Tous"
                        }
                        showDeleteSubDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Confirmer la suppression") }
            },
            dismissButton = { TextButton(onClick = { showDeleteSubDialog = false }) { Text("Annuler") } }
        )
    }

    // --- DIALOGUES DE LA COLLECTION PRINCIPALE ---
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Renommer la collection") },
            text = {
                OutlinedTextField(value = renameInput, onValueChange = { renameInput = it }, label = { Text("Nouveau nom") }, singleLine = true)
            },
            confirmButton = {
                Button(onClick = {
                    if (renameInput.isNotBlank() && renameInput.trim() != categoryName) {
                        viewModel.renameCollection(categoryName, renameInput.trim())
                        showRenameDialog = false
                        onBackClick()
                    }
                }) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Annuler") } }
        )
    }

    if (showDeleteCollectionDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCollectionDialog = false },
            title = { Text("Supprimer la collection ?") },
            text = { Text("Attention ! Supprimer la collection \"$categoryName\" supprimera définitivement tous les objets et sous-collections associés.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteCollection(categoryName)
                    showDeleteCollectionDialog = false
                    onBackClick()
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Tout supprimer") }
            },
            dismissButton = { TextButton(onClick = { showDeleteCollectionDialog = false }) { Text("Annuler") } }
        )
    }

    if (showAddSubDialog) {
        AlertDialog(
            onDismissRequest = { showAddSubDialog = false; newSubName = "" },
            title = { Text("Nouvelle Sous-Collection") },
            text = {
                OutlinedTextField(value = newSubName, onValueChange = { newSubName = it }, label = { Text("Nom (ex: Steelbook...)") }, singleLine = true)
            },
            confirmButton = {
                Button(onClick = {
                    if (newSubName.isNotBlank()) {
                        viewModel.addSubCategory(newSubName.trim(), categoryName)
                        showAddSubDialog = false
                        newSubName = ""
                    }
                }) { Text("Créer") }
            },
            dismissButton = { TextButton(onClick = { showAddSubDialog = false; newSubName = "" }) { Text("Annuler") } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomSubChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val borderModifier = if (!isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)) else Modifier

    Box(
        modifier = Modifier
            .background(containerColor, RoundedCornerShape(8.dp))
            .then(borderModifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

private fun getLocalEmojiForCategory(categoryName: String): String {
    return when (categoryName.lowercase().trim()) {
        "blu-ray", "bluray", "film", "films", "cinéma", "cinema" -> "🎬"
        "vinyles", "vinyle", "musique", "disques", "disque", "cd" -> "🎵"
        "jeux vidéo", "jeux", "jeux video", "gaming", "switch", "ps5" -> "🎮"
        "livres", "livre", "mangas", "manga", "bd", "romans" -> "📚"
        "figurines", "figurine", "pop" -> "🧸"
        "jeux de société", "jeux de societe", "cartes" -> "🎲"
        else -> "📦"
    }
}