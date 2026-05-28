package com.pokyx.gollections.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AddObjectScreen(
    onBackClick: () -> Unit,
    onSaveClick: (title: String, category: String, subCategory: String, purchaseDate: String, price: String) -> Unit, // <-- Modifié ici
    viewModel: CollectionViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }

    // Génération automatique de la date du jour (ex: 21/05/2026)
    val todayDate = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }
    var purchaseDate by remember { mutableStateOf(todayDate) }
    var price by remember { mutableStateOf("") }

    val categoriesList by viewModel.allCategories.collectAsState(initial = emptyList())
    val categories = categoriesList.map { it.name }

    var selectedCategory by remember { mutableStateOf("") }
    var selectedSubCategory by remember { mutableStateOf("") }

    LaunchedEffect(categories) {
        if (selectedCategory.isEmpty() && categories.isNotEmpty()) {
            selectedCategory = categories.first()
        }
    }

    val dbSubCategories by viewModel.getSubCategoriesByCategory(selectedCategory).collectAsState(initial = emptyList())
    val subCategories = dbSubCategories.map { it.name }

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
            // Champ Titre
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre") },
                placeholder = { Text("Ex: Inception, Elden Ring...") },
                modifier = Modifier.fillMaxWidth()
            )

            // AJOUT : Section Informations d'acquisition côte à côte
            Text(text = "Informations d'acquisition", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Prix (€) - Optionnel") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = purchaseDate,
                    onValueChange = { purchaseDate = it },
                    label = { Text("Date d'achat") },
                    placeholder = { Text("JJ/MM/AAAA") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // 1. Choix Catégorie Principale
            if (categories.isNotEmpty()) {
                Text(text = "Catégorie", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
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
            }

            // 2. Choix Sous-Catégorie (Conditionnel)
            if (subCategories.isNotEmpty()) {
                Text(text = "Format / Plateforme", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
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
                        // Envoi de toutes les données validées
                        onSaveClick(title, selectedCategory, selectedSubCategory, purchaseDate.trim(), price.trim())
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = title.isNotBlank() && selectedCategory.isNotBlank()
            ) {
                Text(text = "Enregistrer dans ma collection", fontSize = 16.sp)
            }
        }
    }
}