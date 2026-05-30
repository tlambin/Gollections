package com.pokyx.gollections.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pokyx.gollections.R
import com.pokyx.gollections.data.Collection as DBCollection
import com.pokyx.gollections.ui.components.CollectionDialog
import com.pokyx.gollections.ui.viewmodels.CollectionViewModel
import com.pokyx.gollections.utils.buildPathBottomUp
import com.pokyx.gollections.utils.getEmojiForCollection
import com.pokyx.gollections.utils.getUnitForCollection
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

enum class SortOption { NAME_ASC, NAME_DESC, PRICE_ASC, PRICE_DESC, DATE_DESC, DATE_ASC }

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
    val collectionCover = currentCollection?.cover ?: ""

    val currentPathIds = remember(collectionId, allCollections) { buildPathBottomUp(collectionId, allCollections) }
    val pathCollections = currentPathIds.mapNotNull { id -> allCollections.find { it.id == id } }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTagFilter by remember { mutableStateOf("Toutes") }

    var showTagsRow by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(SortOption.NAME_ASC) }
    var showSortMenu by remember { mutableStateOf(false) }

    var showAddSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }
    LaunchedEffect(collectionName) { if (collectionName.isNotEmpty()) renameInput = collectionName }

    var showDeleteCollectionDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }

    var showAddSubCollectionDialog by remember { mutableStateOf(false) }

    var showTagOptionsDialog by remember { mutableStateOf(false) }
    var selectedTagToManage by remember { mutableStateOf("") }
    var showRenameTagDialog by remember { mutableStateOf(false) }
    var renameTagInput by remember { mutableStateOf("") }
    var showDeleteTagDialog by remember { mutableStateOf(false) }

    val allCollectionItemsWithTags by viewModel.getItemsByCollectionWithTags(collectionId).collectAsStateWithLifecycle(initialValue = emptyList())
    val dbTags by viewModel.getTagsForCollections(currentPathIds).collectAsStateWithLifecycle(initialValue = emptyList())
    val subCollections by viewModel.getSubCollections(collectionId).collectAsStateWithLifecycle(initialValue = emptyList())

    val tagsList = remember(dbTags) { listOf("Toutes") + dbTags.map { it.name }.distinct() }

    val totalCount = remember(collectionId, allCollections, allItemsWithTags) {
        viewModel.getRecursiveItemCount(collectionId, allCollections, allItemsWithTags)
    }

    val totalValue = remember(collectionId, allCollections, allItemsWithTags) {
        val descendantIds = mutableListOf(collectionId)
        var currentLevel = allCollections.filter { it.parentId == collectionId }.map { it.id }
        while (currentLevel.isNotEmpty()) {
            descendantIds.addAll(currentLevel)
            currentLevel = allCollections.filter { it.parentId in currentLevel }.map { it.id }
        }
        allItemsWithTags
            .filter { it.item.collectionId in descendantIds }
            .mapNotNull { it.item.price.replace(",", ".").toDoubleOrNull() }
            .sum()
    }

    val formattedValue = remember(totalValue) { NumberFormat.getCurrencyInstance().format(totalValue) }

    val filteredAndSortedItems = remember(allCollectionItemsWithTags, searchQuery, selectedTagFilter, sortOption) {
        var items = allCollectionItemsWithTags.filter { itemWithTags ->
            val matchesTag = selectedTagFilter == "Toutes" || itemWithTags.tags.any { it.name == selectedTagFilter }
            val matchesSearch = searchQuery.isEmpty() || itemWithTags.item.title.contains(searchQuery, ignoreCase = true)
            matchesTag && matchesSearch
        }

        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        items = when (sortOption) {
            SortOption.NAME_ASC -> items.sortedBy { it.item.title.lowercase() }
            SortOption.NAME_DESC -> items.sortedByDescending { it.item.title.lowercase() }
            SortOption.PRICE_ASC -> items.sortedBy { it.item.price.replace(",", ".").toDoubleOrNull() ?: 0.0 }
            SortOption.PRICE_DESC -> items.sortedByDescending { it.item.price.replace(",", ".").toDoubleOrNull() ?: 0.0 }
            SortOption.DATE_ASC, SortOption.DATE_DESC -> {
                items.sortedWith(compareBy<com.pokyx.gollections.data.tag.CollectionItemWithTags> { itemWithTags ->
                    try {
                        if (itemWithTags.item.purchaseDate.isNotBlank()) LocalDate.parse(itemWithTags.item.purchaseDate, dateFormatter).toEpochDay() else 0L
                    } catch (e: DateTimeParseException) { 0L }
                }.let { if (sortOption == SortOption.DATE_DESC) it.reversed() else it })
            }
        }
        items
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (collectionCover.startsWith("file") || collectionCover.startsWith("/") || collectionCover.startsWith("content") || collectionCover.startsWith("http")) {
                                AsyncImage(model = collectionCover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(40.dp).clip(CircleShape))
                            } else {
                                val displayEmoji = if (collectionCover.isNotBlank()) collectionCover else getEmojiForCollection(collectionName)
                                Text(text = displayEmoji, fontSize = 36.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = collectionName, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                actions = { IconButton(onClick = { showEditSheet = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) } },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.background, scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { showAddSheet = true }, containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_title), modifier = Modifier.size(28.dp)) } }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // 1. Barre de recherche
            TextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text(stringResource(R.string.search_placeholder)) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), shape = CircleShape, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, trailingIcon = { if (searchQuery.isNotEmpty()) { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = null) } } }, colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent, focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant), singleLine = true)

            // 2. Ligne des Bulles CENTRÉES et dans l'ordre demandé
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Bulle Étiquettes
                Box(modifier = Modifier
                    .background(if (showTagsRow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .clickable {
                        if (dbTags.isNotEmpty()) {
                            showTagsRow = !showTagsRow
                        } else {
                            Toast.makeText(context, "Aucune étiquette dans ce dossier", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(LabelIcon, contentDescription = "Tags", tint = if (showTagsRow) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }

                // 2. Bulle Favoris (Coeur)
                Box(modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .clickable { Toast.makeText(context, "Favoris bientôt disponibles", Toast.LENGTH_SHORT).show() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = "Favoris", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }

                // 3. Bulle Nombre Total
                StatBubble(text = totalCount.toString(), isHighlight = false)

                // 4. Bulle Valeur Totale
                StatBubble(text = formattedValue, isHighlight = false)

                // 5. Bulle Corbeille
                Box(modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .clickable { Toast.makeText(context, "Corbeille bientôt disponible", Toast.LENGTH_SHORT).show() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Corbeille", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }

                // 6. Bulle Tri
                Box {
                    Box(modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .clickable { showSortMenu = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(FilterListIcon, contentDescription = "Trier", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.sort_name_asc)) }, onClick = { sortOption = SortOption.NAME_ASC; showSortMenu = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.sort_name_desc)) }, onClick = { sortOption = SortOption.NAME_DESC; showSortMenu = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.sort_price_desc)) }, onClick = { sortOption = SortOption.PRICE_DESC; showSortMenu = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.sort_price_asc)) }, onClick = { sortOption = SortOption.PRICE_ASC; showSortMenu = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.sort_date_desc)) }, onClick = { sortOption = SortOption.DATE_DESC; showSortMenu = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.sort_date_asc)) }, onClick = { sortOption = SortOption.DATE_ASC; showSortMenu = false })
                    }
                }
            }

            // Ligne de filtres de tags
            if (showTagsRow && dbTags.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    tagsList.forEach { tag -> CustomTagChip(text = tag, isSelected = selectedTagFilter == tag, onClick = { selectedTagFilter = tag }, onLongClick = if (tag != "Toutes") { { selectedTagToManage = tag; showTagOptionsDialog = true } } else null) }
                }
            }

            // 3. Contenu principal
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // Sous-collections en grille de 3
                if (subCollections.isNotEmpty()) {
                    item { Text(stringResource(R.string.title_folders), fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)) }

                    val chunkedSubCols = subCollections.chunked(3)
                    items(chunkedSubCols) { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            for (subCol in rowItems) {
                                val subCount = viewModel.getRecursiveItemCount(subCol.id, allCollections, allItemsWithTags)
                                SubCollectionSmallCard(subCol, subCount, Modifier.weight(1f), onCollectionClick)
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                }

                if (filteredAndSortedItems.isNotEmpty() || subCollections.isNotEmpty()) {
                    item { Text(stringResource(R.string.title_items), fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp)) }
                }

                if (filteredAndSortedItems.isEmpty() && subCollections.isEmpty()) {
                    item { Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) { Text(stringResource(R.string.empty_folder), color = MaterialTheme.colorScheme.outline) } }
                } else {
                    items(filteredAndSortedItems) { itemWithTags ->
                        val item = itemWithTags.item
                        Card(modifier = Modifier.fillMaxWidth().clickable { onItemClick(item.id) }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (item.imageUrl.isNotBlank()) AsyncImage(model = item.imageUrl, contentDescription = null, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                else Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { val displayEmoji = if (collectionCover.isNotBlank() && !collectionCover.startsWith("file")) collectionCover else getEmojiForCollection(collectionName); Text(text = displayEmoji, fontSize = 24.sp) }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val tagsStr = itemWithTags.tags.joinToString(" • ") { it.name }
                                        if (tagsStr.isNotEmpty()) { Text(text = tagsStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis) }
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

    if (showMoveDialog) { val validDestinations = viewModel.getValidMoveDestinations(collectionId, allCollections); AlertDialog(onDismissRequest = { showMoveDialog = false }, title = { Text(stringResource(R.string.move_folder)) }, text = { LazyColumn(modifier = Modifier.fillMaxWidth()) { item { ListItem(headlineContent = { Text(stringResource(R.string.move_to_root), fontWeight = FontWeight.Bold) }, modifier = Modifier.clickable { viewModel.updateCollectionParent(collectionId, null); showMoveDialog = false }); HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }; items(validDestinations) { dest -> ListItem(headlineContent = { Text("📁 ${dest.name}") }, modifier = Modifier.clickable { viewModel.updateCollectionParent(collectionId, dest.id); showMoveDialog = false }) } } }, confirmButton = { TextButton(onClick = { showMoveDialog = false }) { Text(stringResource(R.string.cancel)) } }) }
    if (showTagOptionsDialog) AlertDialog(onDismissRequest = { showTagOptionsDialog = false }, title = { Text(stringResource(R.string.tag_options_title, selectedTagToManage)) }, text = { Text(stringResource(R.string.tag_options_subtitle)) }, confirmButton = { Button(onClick = { renameTagInput = selectedTagToManage; showTagOptionsDialog = false; showRenameTagDialog = true }) { Text(stringResource(R.string.rename)) } }, dismissButton = { Button(onClick = { showTagOptionsDialog = false; showDeleteTagDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.delete_folder).replace("le dossier", "")) } })
    if (showRenameTagDialog) AlertDialog(onDismissRequest = { showRenameTagDialog = false }, title = { Text(stringResource(R.string.rename)) }, text = { OutlinedTextField(value = renameTagInput, onValueChange = { renameTagInput = it }, singleLine = true) }, confirmButton = { Button(onClick = { if (renameTagInput.isNotBlank()) { viewModel.renameTag(collectionId, selectedTagToManage, renameTagInput.trim()); if (selectedTagFilter == selectedTagToManage) selectedTagFilter = renameTagInput.trim(); showRenameTagDialog = false } }) { Text(stringResource(R.string.btn_save)) } }, dismissButton = { TextButton(onClick = { showRenameTagDialog = false }) { Text(stringResource(R.string.cancel)) } })
    if (showDeleteTagDialog) AlertDialog(onDismissRequest = { showDeleteTagDialog = false }, title = { Text(stringResource(R.string.delete_tag_title)) }, text = { Text(stringResource(R.string.delete_tag_warning, selectedTagToManage)) }, confirmButton = { Button(onClick = { val tagToDelete = dbTags.find { it.name == selectedTagToManage }; tagToDelete?.let { viewModel.deleteTag(it) }; if (selectedTagFilter == selectedTagToManage) selectedTagFilter = "Toutes"; showDeleteTagDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.create).replace("Créer", "Confirmer")) } }, dismissButton = { TextButton(onClick = { showDeleteTagDialog = false }) { Text(stringResource(R.string.cancel)) } })
    if (showRenameDialog) AlertDialog(onDismissRequest = { showRenameDialog = false }, title = { Text(stringResource(R.string.rename)) }, text = { OutlinedTextField(value = renameInput, onValueChange = { renameInput = it }, singleLine = true) }, confirmButton = { Button(onClick = { if (renameInput.isNotBlank()) { viewModel.renameCollection(collectionId, renameInput.trim()); showRenameDialog = false } }) { Text(stringResource(R.string.btn_save)) } }, dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text(stringResource(R.string.cancel)) } })
    if (showDeleteCollectionDialog) AlertDialog(onDismissRequest = { showDeleteCollectionDialog = false }, title = { Text(stringResource(R.string.delete_item_title).replace("l\'objet", "la collection")) }, text = { Text(stringResource(R.string.delete_folder_warning)) }, confirmButton = { Button(onClick = { viewModel.deleteCollection(collectionId); showDeleteCollectionDialog = false; onBackClick() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.delete_folder).replace(" la collection", "")) } }, dismissButton = { TextButton(onClick = { showDeleteCollectionDialog = false }) { Text(stringResource(R.string.cancel)) } })
}

@Composable
fun StatBubble(text: String, isHighlight: Boolean) {
    Box(
        modifier = Modifier
            .background(
                color = if (isHighlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isHighlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SubCollectionSmallCard(collection: DBCollection, count: Int, modifier: Modifier = Modifier, onClick: (Long) -> Unit) {
    Card(
        modifier = modifier
            .aspectRatio(1.1f)
            .clickable { onClick(collection.id) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (collection.cover.startsWith("file") || collection.cover.startsWith("/") || collection.cover.startsWith("content") || collection.cover.startsWith("http")) {
                AsyncImage(model = collection.cover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(32.dp).clip(CircleShape))
            } else {
                val displayEmoji = if (collection.cover.isNotBlank()) collection.cover else getEmojiForCollection(collection.name)
                Text(text = displayEmoji, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = collection.name,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = "$count obj",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1
            )
        }
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

val LabelIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Label", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(17.63f, 5.84f)
            curveTo(17.27f, 5.33f, 16.67f, 5.0f, 16.0f, 5.0f)
            lineTo(5.01f, 5.0f)
            curveTo(3.9f, 5.0f, 3.0f, 5.9f, 3.0f, 7.0f)
            lineTo(3.0f, 17.0f)
            curveTo(3.0f, 18.1f, 3.9f, 19.0f, 5.01f, 19.0f)
            lineTo(16.0f, 19.0f)
            curveTo(16.67f, 19.0f, 17.27f, 18.66f, 17.63f, 18.15f)
            lineTo(22.0f, 12.0f)
            lineTo(17.63f, 5.84f)
            close()
        }
    }.build()

val FilterListIcon: ImageVector
    get() = ImageVector.Builder(
        name = "FilterList", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(10.0f, 18.0f)
            horizontalLineToRelative(4.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(-4.0f)
            verticalLineToRelative(2.0f)
            close()
            moveTo(3.0f, 6.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(18.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineTo(3.0f)
            close()
            moveTo(6.0f, 13.0f)
            horizontalLineToRelative(12.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineTo(6.0f)
            verticalLineToRelative(2.0f)
            close()
        }
    }.build()