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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.pokyx.gollections.data.Collection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionListScreen(
    collectionId: Long,
    viewModel: CollectionViewModel,
    onBackClick: () -> Unit,
    onItemClick: (Int) -> Unit,
    onAddItemClick: () -> Unit,
    onCollectionClick: (Long) -> Unit
) {
    val allCollections by viewModel.collections.collectAsState()
    val allItems by viewModel.allItems.collectAsState()

    val currentCollection = allCollections.find { it.id == collectionId }
    val collectionName = currentCollection?.name ?: "Chargement..."

    val currentPathIds = remember(collectionId, allCollections) { buildPathBottomUp(collectionId, allCollections) }
    val pathCollections = currentPathIds.mapNotNull { id -> allCollections.find { it.id == id } }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTagFilter by remember { mutableStateOf("Toutes") }

    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteCollectionDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }
    LaunchedEffect(collectionName) { if (collectionName != "Chargement...") renameInput = collectionName }

    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }
    var showAddSubCollectionDialog by remember { mutableStateOf(false) }
    var newSubCollectionName by remember { mutableStateOf("") }

    var showTagOptionsDialog by remember { mutableStateOf(false) }
    var selectedTagToManage by remember { mutableStateOf("") }
    var showRenameTagDialog by remember { mutableStateOf(false) }
    var renameTagInput by remember { mutableStateOf("") }
    var showDeleteTagDialog by remember { mutableStateOf(false) }

    val allCollectionItems by viewModel.getItemsByCollection(collectionId).collectAsState(initial = emptyList())
    val dbTags by viewModel.getTagsForCollections(currentPathIds).collectAsState(initial = emptyList())
    val subCollections by viewModel.getSubCollections(collectionId).collectAsState(initial = emptyList())

    val tagsList = remember(dbTags) { listOf("Toutes") + dbTags.map { it.name }.distinct() }
    val filteredItems = allCollectionItems.filter { item -> (selectedTagFilter == "Toutes" || item.tags.split(",").contains(selectedTagFilter)) && (searchQuery.isEmpty() || item.title.contains(searchQuery, ignoreCase = true)) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    val collapsedFraction = scrollBehavior.state.collapsedFraction

                    // Si on a fait défiler l'en-tête à plus de la moitié, on affiche le fil d'Ariane complet
                    if (collapsedFraction > 0.5f) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            pathCollections.forEachIndexed { index, col ->
                                val isLast = index == pathCollections.lastIndex
                                Text(
                                    text = col.name,
                                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 16.sp,
                                    color = if (isLast) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.clickable(!isLast) { onCollectionClick(col.id) }
                                )
                                if (!isLast) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        modifier = Modifier.padding(horizontal = 4.dp).size(16.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Quand l'en-tête est déplié au maximum, on affiche uniquement le titre épuré
                        Text(
                            text = collectionName,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") } },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Options") }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text("Nouveau sous-dossier") }, onClick = { showMenu = false; showAddSubCollectionDialog = true }, leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) })
                            DropdownMenuItem(text = { Text("Renommer") }, onClick = { showMenu = false; showRenameDialog = true }, leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) })
                            DropdownMenuItem(text = { Text("Déplacer") }, onClick = { showMenu = false; showMoveDialog = true }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp)) })
                            DropdownMenuItem(text = { Text("Supprimer") }, onClick = { showMenu = false; showDeleteCollectionDialog = true }, leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) })
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = onAddItemClick, containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) { Icon(Icons.Default.Add, contentDescription = "Ajouter", modifier = Modifier.size(28.dp)) } }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TextField(value = searchQuery, onValueChange = { viewModel.updateSearchQuery(it) }, placeholder = { Text("Rechercher...") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), shape = CircleShape, leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Rechercher", tint = MaterialTheme.colorScheme.primary) }, trailingIcon = { if (searchQuery.isNotEmpty()) { IconButton(onClick = { viewModel.updateSearchQuery("") }) { Icon(Icons.Default.Clear, contentDescription = "Effacer") } } }, colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent, focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant), singleLine = true)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                tagsList.forEach { tag -> CustomTagChip(text = tag, isSelected = selectedTagFilter == tag, onClick = { selectedTagFilter = tag }, onLongClick = if (tag != "Toutes") { { selectedTagToManage = tag; showTagOptionsDialog = true } } else null) }
                InputChip(selected = false, onClick = { showAddTagDialog = true }, label = { Icon(Icons.Default.Add, contentDescription = "Ajouter", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }, colors = InputChipDefaults.inputChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)))
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (subCollections.isNotEmpty()) {
                    item { Text("Dossiers", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)) }
                    items(subCollections) { subCol ->
                        Card(modifier = Modifier.fillMaxWidth().clickable { onCollectionClick(subCol.id) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = getEmojiForCollection(subCol.name), fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(text = subCol.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    val subCount = viewModel.getRecursiveItemCount(subCol.id, allCollections, allItems)
                                    val unit = getUnitForCollection(subCol.name, subCount)
                                    Text(text = "$subCount $unit", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                }

                if (filteredItems.isNotEmpty() || subCollections.isNotEmpty()) { item { Text("Objets", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp)) } }
                if (filteredItems.isEmpty() && subCollections.isEmpty()) { item { Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) { Text("Dossier vide 📦", color = MaterialTheme.colorScheme.outline) } } } else {
                    items(filteredItems) { item ->
                        Card(modifier = Modifier.fillMaxWidth().clickable { onItemClick(item.id) }, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (item.imageUrl.isNotBlank()) AsyncImage(model = item.imageUrl, contentDescription = "Miniature", modifier = Modifier.size(50.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                                else Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Text(text = getEmojiForCollection(collectionName), fontSize = 24.sp) }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val tagsStr = item.tags.replace(",", " • ")
                                        if (tagsStr.isNotEmpty()) { Text(text = tagsStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline) }
                                        if (item.status != "Non commencé") Text(text = " | ${item.status}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMoveDialog) { val validDestinations = viewModel.getValidMoveDestinations(collectionId, allCollections); AlertDialog(onDismissRequest = { showMoveDialog = false }, title = { Text("Déplacer '$collectionName'") }, text = { LazyColumn(modifier = Modifier.fillMaxWidth()) { item { ListItem(headlineContent = { Text("🏠 À la racine (Dashboard)", fontWeight = FontWeight.Bold) }, modifier = Modifier.clickable { viewModel.updateCollectionParent(collectionId, null); showMoveDialog = false }); HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }; items(validDestinations) { dest -> ListItem(headlineContent = { Text("📁 ${dest.name}") }, modifier = Modifier.clickable { viewModel.updateCollectionParent(collectionId, dest.id); showMoveDialog = false }) } } }, confirmButton = { TextButton(onClick = { showMoveDialog = false }) { Text("Annuler") } }) }
    if (showTagOptionsDialog) AlertDialog(onDismissRequest = { showTagOptionsDialog = false }, title = { Text("Options : $selectedTagToManage") }, text = { Text("Que souhaitez-vous faire ?") }, confirmButton = { Button(onClick = { renameTagInput = selectedTagToManage; showTagOptionsDialog = false; showRenameTagDialog = true }) { Text("Renommer") } }, dismissButton = { Button(onClick = { showTagOptionsDialog = false; showDeleteTagDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Supprimer") } })
    if (showRenameTagDialog) AlertDialog(onDismissRequest = { showRenameTagDialog = false }, title = { Text("Renommer l'étiquette") }, text = { OutlinedTextField(value = renameTagInput, onValueChange = { renameTagInput = it }, singleLine = true) }, confirmButton = { Button(onClick = { if (renameTagInput.isNotBlank()) { viewModel.renameTag(collectionId, selectedTagToManage, renameTagInput.trim()); if (selectedTagFilter == selectedTagToManage) selectedTagFilter = renameTagInput.trim(); showRenameTagDialog = false } }) { Text("Enregistrer") } }, dismissButton = { TextButton(onClick = { showRenameTagDialog = false }) { Text("Annuler") } })
    if (showDeleteTagDialog) AlertDialog(onDismissRequest = { showDeleteTagDialog = false }, title = { Text("Supprimer l'étiquette ?") }, text = { Text("Voulez-vous supprimer l'étiquette \"$selectedTagToManage\" de tous vos objets ?") }, confirmButton = { Button(onClick = { val tagToDelete = dbTags.find { it.name == selectedTagToManage }; tagToDelete?.let { viewModel.deleteTag(it) }; if (selectedTagFilter == selectedTagToManage) selectedTagFilter = "Toutes"; showDeleteTagDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Confirmer") } }, dismissButton = { TextButton(onClick = { showDeleteTagDialog = false }) { Text("Annuler") } })
    if (showRenameDialog) AlertDialog(onDismissRequest = { showRenameDialog = false }, title = { Text("Renommer la collection") }, text = { OutlinedTextField(value = renameInput, onValueChange = { renameInput = it }, singleLine = true) }, confirmButton = { Button(onClick = { if (renameInput.isNotBlank()) { viewModel.renameCollection(collectionId, renameInput.trim()); showRenameDialog = false } }) { Text("Enregistrer") } }, dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Annuler") } })
    if (showDeleteCollectionDialog) AlertDialog(onDismissRequest = { showDeleteCollectionDialog = false }, title = { Text("Tout supprimer ?") }, text = { Text("Attention ! Cette action supprimera également tous les sous-dossiers et objets.") }, confirmButton = { Button(onClick = { viewModel.deleteCollection(collectionId); showDeleteCollectionDialog = false; onBackClick() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Tout supprimer") } }, dismissButton = { TextButton(onClick = { showDeleteCollectionDialog = false }) { Text("Annuler") } })
    if (showAddTagDialog) AlertDialog(onDismissRequest = { showAddTagDialog = false; newTagName = "" }, title = { Text("Nouvelle Étiquette") }, text = { OutlinedTextField(value = newTagName, onValueChange = { newTagName = it }, label = { Text("Nom (ex: 4K, Collector...)") }, singleLine = true) }, confirmButton = { Button(onClick = { if (newTagName.isNotBlank()) { viewModel.insertTag(newTagName.trim(), collectionId); showAddTagDialog = false; newTagName = "" } }) { Text("Créer") } }, dismissButton = { TextButton(onClick = { showAddTagDialog = false; newTagName = "" }) { Text("Annuler") } })
    if (showAddSubCollectionDialog) AlertDialog(onDismissRequest = { showAddSubCollectionDialog = false; newSubCollectionName = "" }, title = { Text("Nouveau sous-dossier") }, text = { OutlinedTextField(value = newSubCollectionName, onValueChange = { newSubCollectionName = it }, label = { Text("Nom du dossier") }, singleLine = true) }, confirmButton = { Button(onClick = { if (newSubCollectionName.isNotBlank()) { viewModel.insertCollection(newSubCollectionName.trim(), parentId = collectionId); showAddSubCollectionDialog = false; newSubCollectionName = "" } }) { Text("Créer") } }, dismissButton = { TextButton(onClick = { showAddSubCollectionDialog = false; newSubCollectionName = "" }) { Text("Annuler") } })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomTagChip(text: String, isSelected: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)?) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val borderModifier = if (!isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)) else Modifier
    Box(modifier = Modifier.background(containerColor, RoundedCornerShape(8.dp)).then(borderModifier).combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) { Text(text = text, color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
}

fun buildPathBottomUp(targetId: Long, allCols: List<Collection>): List<Long> { val path = mutableListOf<Long>(); var curr: Long? = targetId; while (curr != null) { path.add(0, curr); curr = allCols.find { it.id == curr }?.parentId }; return path }
fun getUnitForCollection(name: String, count: Int): String { return when (name.lowercase().trim()) { "blu-ray", "films", "cinéma", "cinema" -> if (count <= 1) "film" else "films"; "vinyles" -> if (count <= 1) "album" else "albums"; "jeux vidéo", "jeux", "jeux video", "gaming", "switch", "ps5" -> if (count <= 1) "jeu" else "jeux"; "livres", "mangas", "bd", "romans" -> if (count <= 1) "livre" else "livres"; else -> if (count <= 1) "objet" else "objets" } }