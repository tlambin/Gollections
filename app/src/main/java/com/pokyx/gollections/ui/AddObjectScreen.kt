package com.pokyx.gollections.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AddObjectScreen(
    onBackClick: () -> Unit,
    onSaveClick: (title: String, year: String, category: String, subCategory: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Blu-ray") }
    var selectedSubCategory by remember { mutableStateOf("4K") }

    val categories = listOf("Blu-ray", "Vinyles", "Jeux Vidéo")

    // Détermination des sous-catégories selon la catégorie principale
    val subCategories = when (selectedCategory) {
        "Blu-ray" -> listOf("4K", "3D", "Standard")
        "Jeux Vidéo" -> listOf("Switch", "PC", "PS5", "Xbox")
        else -> emptyList() // Pas de sous-catégorie pour les Vinyles par exemple
    }

    // Force la première sous-catégorie si on change de catégorie principale
    LaunchedEffect(selectedCategory) {
        selectedSubCategory = subCategories.firstOrNull() ?: ""
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✖",
                    modifier = Modifier.clickable { onBackClick() }.padding(8.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "Nouvel Objet", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre") },
                placeholder = { Text("Ex: Inception, Elden Ring...") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = year,
                onValueChange = { year = it },
                label = { Text("Année de sortie") },
                modifier = Modifier.fillMaxWidth()
            )

            // 1. Choix Catégorie Principale
            Text(text = "Catégorie", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }

            // 2. Choix Sous-Catégorie (Conditionnel)
            if (subCategories.isNotEmpty()) {
                Text(text = "Format / Plateforme", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subCategories.forEach { sub ->
                        FilterChip(
                            selected = selectedSubCategory == sub,
                            onClick = { selectedSubCategory = sub },
                            label = { Text(sub) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSaveClick(title, year, selectedCategory, selectedSubCategory)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = title.isNotBlank()
            ) {
                Text(text = "Enregistrer dans ma collection", fontSize = 16.sp)
            }
        }
    }
}