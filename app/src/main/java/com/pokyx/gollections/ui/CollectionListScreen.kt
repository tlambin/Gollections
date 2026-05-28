package com.pokyx.gollections.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CollectionListScreen(
    categoryName: String,
    viewModel: CollectionViewModel,
    onBackClick: () -> Unit,
    onItemClick: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSub by remember { mutableStateOf("Tous") }

    // États pour le dialogue de création de sous-collection
    var showAddSubDialog by remember { mutableStateOf(false) }
    var newSubName by remember { mutableStateOf("") }

    // On écoute la BDD en temps réel pour cette catégorie précise
    val allCategoryItems by viewModel.getItemsByCategory(categoryName).collectAsState(initial = emptyList())

    // On écoute les sous-catégories dynamiques de la BDD
    val dbSubCategories by viewModel.getSubCategoriesByCategory(categoryName).collectAsState(initial = emptyList())

    // On fusionne l'option universelle "Tous" avec les données de la BDD
    val subCategories = remember(dbSubCategories) {
        listOf("Tous") + dbSubCategories.map { it.name }
    }

    // Filtrage dynamique combiné
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
                Text(text = categoryName, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            // Barre de Recherche
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Rechercher dans $categoryName...") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = { Text(text = "🔍", fontSize = 18.sp) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Text(text = "❌", modifier = Modifier.clickable { searchQuery = "" }, fontSize = 14.sp)
                    }
                }
            )

            // Barre de puces de filtrage + Bouton d'ajout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                subCategories.forEach { sub ->
                    FilterChip(
                        selected = selectedSub == sub,
                        onClick = { selectedSub = sub },
                        label = { Text(sub) }
                    )
                }

                // Puce Spéciale "➕" pour ajouter une sous-collection
                InputChip(
                    selected = false,
                    onClick = { showAddSubDialog = true },
                    label = { Text("➕", fontWeight = FontWeight.Bold) },
                    colors = InputChipDefaults.inputChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                )
            }

            // Liste des vrais objets de la BDD
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemClick(item.id) },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = getLocalEmojiForCategory(item.category), fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (item.subCategory.isNotEmpty()) {
                                            Text(text = item.subCategory, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                        if (item.status != "Non commencé") {
                                            Text(
                                                text = " • ${item.status}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
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

    // Boîte de dialogue pour créer une sous-collection
    if (showAddSubDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddSubDialog = false
                newSubName = ""
            },
            title = { Text("Nouvelle Sous-Collection") },
            text = {
                OutlinedTextField(
                    value = newSubName,
                    onValueChange = { newSubName = it },
                    label = { Text("Nom (ex: Steelbook, PS4, Cassette...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSubName.isNotBlank()) {
                            viewModel.addSubCategory(newSubName.trim(), categoryName)
                            showAddSubDialog = false
                            newSubName = ""
                        }
                    }
                ) {
                    Text("Créer")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddSubDialog = false
                    newSubName = ""
                }) {
                    Text("Annuler")
                }
            }
        )
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