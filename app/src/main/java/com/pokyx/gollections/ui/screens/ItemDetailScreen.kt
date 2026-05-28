package com.pokyx.gollections.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.pokyx.gollections.ui.viewmodels.CollectionViewModel
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    itemId: Int,
    onBackClick: () -> Unit,
    onEditClick: (Int) -> Unit,
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val item by viewModel.getItemById(itemId).collectAsState(initial = null)
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val currentItem = item!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Informations", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        // 🚀 ICI : On place le Split Button en tant que Floating Action Button !
        floatingActionButton = {
            Row(
                modifier = Modifier
                    .height(56.dp) // Hauteur standard d'un FAB M3
                    .wrapContentWidth()
                    .clip(RoundedCornerShape(16.dp)) // Coins légèrement plus carrés, typique des FAB M3
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(end = 4.dp), // Ajustement visuel pour équilibrer l'icône de suppression
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Partie Gauche : Cliquer pour Éditer
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clickable { onEditClick(currentItem.id) }
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Modifier",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Modifier",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 14.sp
                    )
                }

                // Ligne de séparation subtile
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight(0.6f) // La ligne ne prend pas toute la hauteur pour le style
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                )

                // Partie Droite : Extension pour Supprimer
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clickable { showDeleteDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                if (currentItem.imageUrl.isNotBlank()) AsyncImage(model = currentItem.imageUrl, contentDescription = "Image", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Text(text = getEmojiForCollection(currentItem.collection), fontSize = 80.sp)
            }

            Column(
                modifier = Modifier
                    .padding(24.dp)
                    // Ajout d'un padding en bas pour éviter que le contenu final soit caché par le bouton flottant
                    .padding(bottom = 72.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = currentItem.title, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestionChip(onClick = { }, label = { Text(currentItem.collection) }, colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer))
                    if (currentItem.category.isNotBlank()) SuggestionChip(onClick = { }, label = { Text(currentItem.category) })
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                DetailRow(label = "Date d'achat", value = currentItem.purchaseDate.ifBlank { "Non renseignée" })
                DetailRow(label = "Prix", value = if (currentItem.price.isNotBlank()) "${currentItem.price} €" else "Non renseigné")
                DetailRow(label = "Statut", value = currentItem.status)

                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentItem.isLoaned) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Statut du prêt", fontWeight = FontWeight.Bold, color = if (currentItem.isLoaned) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer)
                            if (currentItem.isLoaned) {
                                Text("Prêté à : ${currentItem.loanTo.ifBlank { "Inconnu" }}", fontSize = 14.sp)
                                Text("Depuis le : ${currentItem.loanDate.ifBlank { "Date inconnue" }}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
                            } else {
                                Text("Dans ma collection", fontSize = 14.sp)
                            }
                        }
                        if (currentItem.isLoaned) Text(text = "⚠️", fontSize = 24.sp) else Text(text = "✅", fontSize = 24.sp)
                    }
                }
            }
        }
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer l'objet ?") },
            text = { Text("Voulez-vous vraiment supprimer \"${currentItem.title}\" ? Cette action est irréversible.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteItem(currentItem)
                        showDeleteDialog = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(text = label, color = MaterialTheme.colorScheme.outline, fontSize = 14.sp); Text(text = value, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
}