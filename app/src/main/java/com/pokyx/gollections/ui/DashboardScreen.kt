package com.pokyx.gollections.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@Composable
fun DashboardScreen(
    onCategoryClick: (String) -> Unit,
    onAddObjectClick: () -> Unit
) {
    // AJOUT : État pour stocker la recherche globale de l'écran d'accueil
    var searchQuery by remember { mutableStateOf("") }

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
            // 1. Le Slider de Statistiques
            item { StatsSlider() }

            // AJOUT : La Barre de Recherche globale (alignée sur 24.dp)
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

            // 2. Le Titre de la section
            item {
                Text(
                    text = "Mes Collections",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            // 3. La Grille des Catégories
            item { CategoriesGrid(onCategoryClick = onCategoryClick) }
        }
    }
}

@Composable
fun StatsSlider() {
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
                        0 -> StatCardContent(title = "Total Objets", value = "142", sub = "Tous médias confondus")
                        1 -> StatCardContent(title = "Valeur Estimée", value = "1 240 €", sub = "Basé sur les prix saisis")
                        2 -> StatCardContent(title = "Prêts en cours", value = "3", sub = "Objets confiés à des amis")
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
fun CategoriesGrid(onCategoryClick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CategoryCard(title = "Blu-ray", count = "48 films", emoji = "🎬", onClick = { onCategoryClick("Blu-ray") }, modifier = Modifier.weight(1f))
            CategoryCard(title = "Vinyles", count = "32 albums", emoji = "🎵", onClick = { onCategoryClick("Vinyles") }, modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CategoryCard(title = "Jeux Vidéo", count = "62 jeux", emoji = "🎮", onClick = { onCategoryClick("Jeux Vidéo") }, modifier = Modifier.weight(1f))
            Box(modifier = Modifier.weight(1f))
        }
    }
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