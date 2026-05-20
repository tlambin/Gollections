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
    onBackClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSub by remember { mutableStateOf("Tous") }

    // On écoute la BDD en temps réel pour cette catégorie précise
    val allCategoryItems by viewModel.getItemsByCategory(categoryName).collectAsState(initial = emptyList())

    // Filtrage dynamique combiné (Format/Plateforme + Recherche textuelle)
    val filteredItems = allCategoryItems.filter { item ->
        val matchesSub = selectedSub == "Tous" || item.subCategory == selectedSub
        val matchesSearch = searchQuery.isEmpty() || item.title.contains(searchQuery, ignoreCase = true)
        matchesSub && matchesSearch
    }

    val subCategories = when (categoryName) {
        "Blu-ray" -> listOf("Tous", "4K", "3D", "Standard")
        "Jeux Vidéo" -> listOf("Tous", "Switch", "PC", "PS5", "Xbox")
        else -> listOf("Tous")
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

            // Barre de puces de filtrage
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subCategories.forEach { sub ->
                    FilterChip(
                        selected = selectedSub == sub,
                        onClick = { selectedSub = sub },
                        label = { Text(sub) }
                    )
                }
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
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val emoji = when(item.category) {
                                    "Blu-ray" -> "🎬"
                                    "Vinyles" -> "🎵"
                                    "Jeux Vidéo" -> "🎮"
                                    else -> "📦"
                                }
                                Text(text = emoji, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Row {
                                        if (item.subCategory.isNotEmpty()) {
                                            Text(text = "${item.subCategory} • ", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                        Text(text = "Année : ${item.year.ifBlank { "Inconnue" }}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                                // BONUS : Bouton de suppression directe
                                Text(
                                    text = "🗑️",
                                    modifier = Modifier.clickable { viewModel.deleteItem(item) }.padding(8.dp),
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}