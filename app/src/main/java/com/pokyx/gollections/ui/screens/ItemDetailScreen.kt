package com.pokyx.gollections.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pokyx.gollections.ui.viewmodels.CollectionViewModel
import com.pokyx.gollections.utils.DetailRow
import com.pokyx.gollections.utils.getEmojiForCollection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    itemId: Int,
    onBackClick: () -> Unit,
    onEditClick: (Int) -> Unit,
    viewModel: CollectionViewModel
) {
    val itemWithTags by viewModel.getItemByIdWithTags(itemId).collectAsStateWithLifecycle(initialValue = null)
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (itemWithTags == null) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    val currentItem = itemWithTags!!.item
    val tagsList = itemWithTags!!.tags

    val allCollections by viewModel.collections.collectAsStateWithLifecycle()

    val pathFolderNames = remember(currentItem.collectionId, allCollections) {
        val path = mutableListOf<String>()
        var curr: Long? = currentItem.collectionId
        while (curr != null) {
            val c = allCollections.find { it.id == curr }
            if (c != null) { path.add(0, c.name); curr = c.parentId } else break
        }
        path
    }

    val directParentName = allCollections.find { it.id == currentItem.collectionId }?.name ?: ""

    Scaffold(
        topBar = { TopAppBar(title = { Text("Informations", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) },
        floatingActionButton = {
            Row(modifier = Modifier.height(56.dp).wrapContentWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.fillMaxHeight().clickable { onEditClick(currentItem.id) }.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { Icon(imageVector = Icons.Default.Edit, contentDescription = "Modifier", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(10.dp)); Text(text = "Modifier", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 14.sp) }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.6f).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)))
                // CORRECTION ICI : Remplacement de MaterialTheme.error par MaterialTheme.colorScheme.error
                Box(modifier = Modifier.fillMaxHeight().aspectRatio(1f).clickable { showDeleteDialog = true }, contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState())) {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                if (currentItem.imageUrl.isNotBlank()) AsyncImage(model = currentItem.imageUrl, contentDescription = "Image", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Text(text = getEmojiForCollection(directParentName), fontSize = 80.sp)
            }

            Column(modifier = Modifier.padding(24.dp).padding(bottom = 72.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = currentItem.title, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold))

                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pathFolderNames.forEach { folderName ->
                        SuggestionChip(
                            onClick = { },
                            label = { Text(folderName, fontWeight = FontWeight.Medium) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = null
                        )
                    }

                    tagsList.forEach { tag ->
                        SuggestionChip(
                            onClick = { },
                            label = { Text(tag.name) }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                DetailRow(label = "Date d'achat", value = currentItem.purchaseDate.ifBlank { "Non renseignée" })
                DetailRow(label = "Prix", value = if (currentItem.price.isNotBlank()) "${currentItem.price} €" else "Non renseigné")
                DetailRow(label = "Statut", value = currentItem.status)

                Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), colors = CardDefaults.cardColors(containerColor = if (currentItem.isLoaned) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column { Text("Statut du prêt", fontWeight = FontWeight.Bold, color = if (currentItem.isLoaned) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer); if (currentItem.isLoaned) { Text("Prêté à : ${currentItem.loanTo.ifBlank { "Inconnu" }}", fontSize = 14.sp); Text("Depuis le : ${currentItem.loanDate.ifBlank { "Date inconnue" }}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)) } else { Text("Dans ma collection", fontSize = 14.sp) } }
                        if (currentItem.isLoaned) Text(text = "⚠️", fontSize = 24.sp) else Text(text = "✅", fontSize = 24.sp)
                    }
                }
            }
        }
    }
    if (showDeleteDialog) { AlertDialog(onDismissRequest = { showDeleteDialog = false }, title = { Text("Supprimer l'objet ?") }, text = { Text("Voulez-vous vraiment supprimer \"${currentItem.title}\" ? Cette action est irréversible.") }, confirmButton = { TextButton(onClick = { viewModel.deleteItem(currentItem); showDeleteDialog = false; onBackClick() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Supprimer") } }, dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Annuler") } }) }
}