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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pokyx.gollections.R
import com.pokyx.gollections.data.Collection as DBCollection
import com.pokyx.gollections.ui.viewmodels.CollectionViewModel
import com.pokyx.gollections.utils.buildPathBottomUp
import com.pokyx.gollections.utils.getEmojiForCollection
import com.pokyx.gollections.utils.getUnitForCollection
import com.pokyx.gollections.ui.components.CollectionDialog

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
    val context = LocalContext.current
    val allCollections by viewModel.collections.collectAsStateWithLifecycle()
    val allItemsWithTags by viewModel.allItemsWithTags.collectAsStateWithLifecycle()

    val currentCollection = allCollections.find { it.id == collectionId }
    val collectionName = currentCollection?.name ?: ""

    val currentPathIds = remember(collectionId, allCollections) { buildPathBottomUp(collectionId, allCollections) }
    val pathCollections = currentPathIds.mapNotNull { id -> allCollections.find { it.id == id } }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTagFilter by remember { mutableStateOf("Toutes") }

    var showAddSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }

    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteCollectionDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }
    LaunchedEffect(collectionName) { if (collectionName.isNotEmpty()) renameInput = collectionName }

    var showAddSubCollectionDialog by remember { mutableStateOf(false) }
    var newSubCollectionName by remember { mutableStateOf("") }

    var showTagOptionsDialog by remember { mutableStateOf(false) }
    var selectedTagToManage by remember { mutableStateOf("") }
    var showRenameTagDialog by remember { mutableStateOf(false) }
    var renameTagInput by remember { mutableStateOf("") }
    var showDeleteTagDialog by remember { mutableStateOf(false) }

    val allCollectionItemsWithTags by viewModel.getItemsByCollectionWithTags(collectionId).collectAsStateWithLifecycle(initialValue = emptyList())
    val dbTags by viewModel.getTagsForCollections(currentPathIds).collectAsStateWithLifecycle(initialValue = emptyList())
    val subCollections by viewModel.getSubCollections(collectionId).collectAsStateWithLifecycle(initialValue = emptyList())

    val tagsList = remember(dbTags) { listOf("Toutes") + dbTags.map { it.name }.distinct() }

    val filteredItems = allCollectionItemsWithTags.filter { itemWithTags ->
        val matchesTag = selectedTagFilter == "Toutes" || itemWithTags.tags.any { it.name == selectedTagFilter }
        val matchesSearch = searchQuery.isEmpty() || itemWithTags.item.title.contains(searchQuery, ignoreCase = true)
        matchesTag && matchesSearch
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    val collapsedFraction = scrollBehavior.state.collapsedFraction
                    if (collapsedFraction > 0.5f) {
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                            pathCollections.forEachIndexed { index, col ->
                                val isLast = index == pathCollections.lastIndex
                                Text(text = col.name, fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium, fontSize = 16.sp, color = if (isLast) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.clickable(!isLast) { onCollectionClick(col.id) })
                                if (!isLast) Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 4.dp).size(16.dp))
                            }
                        }
                    } else {
                        Text(text = collectionName, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                actions = {
                    IconButton(onClick = { showEditSheet = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) }
                },
                scrollBehavior = scrollBehavior,
                // CORRECTION ICI : Utilisation de la configuration correcte pour la LargeTopAppBar
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { showAddSheet = true }, containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_title), modifier = Modifier.size(28.dp)) } }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text(stringResource(R.string.search_placeholder)) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), shape = CircleShape, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, trailingIcon = { if (searchQuery.isNotEmpty()) { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = null) } } }, colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent, focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant), singleLine = true)

            if (dbTags.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    tagsList.forEach { tag -> CustomTagChip(text = tag, isSelected = selectedTagFilter == tag, onClick = { selectedTagFilter = tag }, onLongClick = if (tag != "Toutes") { { selectedTagToManage = tag; showTagOptionsDialog = true } } else null) }
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (subCollections.isNotEmpty()) {
                    item { Text(stringResource(R.string.title_folders), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)) }
                    items(subCollections) { subCol ->
                        Card(modifier = Modifier.fillMaxWidth().clickable { onCollectionClick(subCol.id) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                // Affichage dynamique de l'icône
                                if (subCol.cover.startsWith("file") || subCol.cover.startsWith("/") || subCol.cover.startsWith("content") || subCol.cover.startsWith("http")) {
                                    AsyncImage(model = subCol.cover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(40.dp).clip(CircleShape))
                                } else {
                                    val displayEmoji = if (subCol.cover.isNotBlank()) subCol.cover else getEmojiForCollection(subCol.name)
                                    Text(text = displayEmoji, fontSize = 28.sp)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(text = subCol.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    val subCount = viewModel.getRecursiveItemCount(subCol.id, allCollections, allItemsWithTags)
                                    val unit = getUnitForCollection(context, subCol.name, subCount)
                                    Text(text = "$subCount $unit", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                }

                if (filteredItems.isNotEmpty() || subCollections.isNotEmpty()) { item { Text(stringResource(R.string.title_items), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp)) } }
                if (filteredItems.isEmpty() && subCollections.isEmpty()) { item { Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) { Text(stringResource(R.string.empty_folder), color = MaterialTheme.colorScheme.outline) } } } else {
                    items(filteredItems) { itemWithTags ->
                        val item = itemWithTags.item
                        Card(modifier = Modifier.fillMaxWidth().clickable { onItemClick(item.id) }, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (item.imageUrl.isNotBlank()) AsyncImage(model = item.imageUrl, contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                                else Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Text(text = getEmojiForCollection(collectionName), fontSize = 24.sp) }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val tagsStr = itemWithTags.tags.joinToString(" • ") { it.name }
                                        if (tagsStr.isNotEmpty()) { Text(text = tagsStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline) }
                                        if (item.status != "Non commencé") Text(text = if (tagsStr.isNotEmpty()) " | ${item.status}" else item.status, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) { ModalBottomSheet(onDismissRequest = { showAddSheet = false }) { Column(modifier = Modifier.padding(bottom = 32.dp)) { Text(stringResource(R.string.add_title), modifier = Modifier.padding(start = 16.dp, bottom = 8.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); ListItem(headlineContent = { Text(stringResource(R.string.add_new_item), fontWeight = FontWeight.Medium) }, leadingContent = { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, modifier = Modifier.clickable { showAddSheet = false; onAddItemClick() }); ListItem(headlineContent = { Text(stringResource(R.string.add_new_subfolder), fontWeight = FontWeight.Medium) }, leadingContent = { Text("📁", fontSize = 20.sp) }, modifier = Modifier.clickable { showAddSheet = false; showAddSubCollectionDialog = true }) } } }
    if (showEditSheet) { ModalBottomSheet(onDismissRequest = { showEditSheet = false }) { Column(modifier = Modifier.padding(bottom = 32.dp)) { Text(stringResource(R.string.manage_folder, collectionName), modifier = Modifier.padding(start = 16.dp, bottom = 8.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); ListItem(headlineContent = { Text(stringResource(R.string.rename)) }, leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) }, modifier = Modifier.clickable { showEditSheet = false; showRenameDialog = true }); ListItem(headlineContent = { Text(stringResource(R.string.move_folder)) }, leadingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) }, modifier = Modifier.clickable { showEditSheet = false; showMoveDialog = true }); ListItem(headlineContent = { Text(stringResource(R.string.delete_folder), color = MaterialTheme.colorScheme.error) }, leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }, modifier = Modifier.clickable { showEditSheet = false; showDeleteCollectionDialog = true }) } } }
    if (showMoveDialog) { val validDestinations = viewModel.getValidMoveDestinations(collectionId, allCollections); AlertDialog(onDismissRequest = { showMoveDialog = false }, title = { Text(stringResource(R.string.move_folder)) }, text = { LazyColumn(modifier = Modifier.fillMaxWidth()) { item { ListItem(headlineContent = { Text(stringResource(R.string.move_to_root), fontWeight = FontWeight.Bold) }, modifier = Modifier.clickable { viewModel.updateCollectionParent(collectionId, null); showMoveDialog = false }); HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }; items(validDestinations) { dest -> ListItem(headlineContent = { Text("📁 ${dest.name}") }, modifier = Modifier.clickable { viewModel.updateCollectionParent(collectionId, dest.id); showMoveDialog = false }) } } }, confirmButton = { TextButton(onClick = { showMoveDialog = false }) { Text(stringResource(R.string.cancel)) } }) }
    if (showTagOptionsDialog) AlertDialog(onDismissRequest = { showTagOptionsDialog = false }, title = { Text(stringResource(R.string.tag_options_title, selectedTagToManage)) }, text = { Text(stringResource(R.string.tag_options_subtitle)) }, confirmButton = { Button(onClick = { renameTagInput = selectedTagToManage; showTagOptionsDialog = false; showRenameTagDialog = true }) { Text(stringResource(R.string.rename)) } }, dismissButton = { Button(onClick = { showTagOptionsDialog = false; showDeleteTagDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.delete_folder).replace("le dossier", "")) } })
    if (showRenameTagDialog) AlertDialog(onDismissRequest = { showRenameTagDialog = false }, title = { Text(stringResource(R.string.rename)) }, text = { OutlinedTextField(value = renameTagInput, onValueChange = { renameTagInput = it }, singleLine = true) }, confirmButton = { Button(onClick = { if (renameTagInput.isNotBlank()) { viewModel.renameTag(collectionId, selectedTagToManage, renameTagInput.trim()); if (selectedTagFilter == selectedTagToManage) selectedTagFilter = renameTagInput.trim(); showRenameTagDialog = false } }) { Text(stringResource(R.string.btn_save)) } }, dismissButton = { TextButton(onClick = { showRenameTagDialog = false }) { Text(stringResource(R.string.cancel)) } })
    if (showDeleteTagDialog) AlertDialog(onDismissRequest = { showDeleteTagDialog = false }, title = { Text(stringResource(R.string.delete_tag_title)) }, text = { Text(stringResource(R.string.delete_tag_warning, selectedTagToManage)) }, confirmButton = { Button(onClick = { val tagToDelete = dbTags.find { it.name == selectedTagToManage }; tagToDelete?.let { viewModel.deleteTag(it) }; if (selectedTagFilter == selectedTagToManage) selectedTagFilter = "Toutes"; showDeleteTagDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.create).replace("Créer", "Confirmer")) } }, dismissButton = { TextButton(onClick = { showDeleteTagDialog = false }) { Text(stringResource(R.string.cancel)) } })
    if (showRenameDialog) AlertDialog(onDismissRequest = { showRenameDialog = false }, title = { Text(stringResource(R.string.rename)) }, text = { OutlinedTextField(value = renameInput, onValueChange = { renameInput = it }, singleLine = true) }, confirmButton = { Button(onClick = { if (renameInput.isNotBlank()) { viewModel.renameCollection(collectionId, renameInput.trim()); showRenameDialog = false } }) { Text(stringResource(R.string.btn_save)) } }, dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text(stringResource(R.string.cancel)) } })
    if (showDeleteCollectionDialog) AlertDialog(onDismissRequest = { showDeleteCollectionDialog = false }, title = { Text(stringResource(R.string.delete_item_title).replace("l\'objet", "la collection")) }, text = { Text(stringResource(R.string.delete_folder_warning)) }, confirmButton = { Button(onClick = { viewModel.deleteCollection(collectionId); showDeleteCollectionDialog = false; onBackClick() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.delete_folder).replace(" la collection", "")) } }, dismissButton = { TextButton(onClick = { showDeleteCollectionDialog = false }) { Text(stringResource(R.string.cancel)) } })
    if (showAddSubCollectionDialog) {
        CollectionDialog(
            title = stringResource(R.string.new_subfolder_title),
            viewModel = viewModel,
            onDismiss = { showAddSubCollectionDialog = false },
            onConfirm = { name, cover ->
                viewModel.insertCollection(name = name, cover = cover, parentId = collectionId)
                showAddSubCollectionDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomTagChip(text: String, isSelected: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)?) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val borderModifier = if (!isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)) else Modifier
    Box(modifier = Modifier.background(containerColor, RoundedCornerShape(8.dp)).then(borderModifier).combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) { Text(text = text, color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
}