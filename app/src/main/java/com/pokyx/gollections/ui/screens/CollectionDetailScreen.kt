package com.pokyx.gollections.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // <-- L'IMPORT QUI MANQUAIT EST ICI
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.pokyx.gollections.R
import com.pokyx.gollections.ui.components.CollectionDialog
import com.pokyx.gollections.ui.viewmodels.CollectionDetailViewModel
import com.pokyx.gollections.utils.BarcodeScanner
import com.pokyx.gollections.utils.buildPathBottomUp
import com.pokyx.gollections.utils.getEmojiForCollection
import java.text.NumberFormat
import com.pokyx.gollections.ui.components.*

enum class SortOption { NAME_ASC, NAME_DESC, PRICE_ASC, PRICE_DESC, DATE_DESC, DATE_ASC }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionDetailScreen(
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
    collectionId: Long,
    viewModel: CollectionDetailViewModel,
    onBackClick: () -> Unit,
    onItemClick: (Int) -> Unit,
    onAddItemClick: (title: String?, imageUrl: String?) -> Unit,
    onCollectionClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val allCollections by viewModel.allCollections.collectAsStateWithLifecycle()

    val currentCollection = allCollections.find { it.id == collectionId }
    val collectionName = currentCollection?.name ?: ""
    val collectionCover = currentCollection?.cover ?: ""

    val currentPathIds = remember(collectionId, allCollections) { buildPathBottomUp(collectionId, allCollections) }
    val pathCollections = currentPathIds.mapNotNull { id -> allCollections.find { it.id == id } }

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTagFilter by viewModel.tagFilter.collectAsStateWithLifecycle()
    val sortOptionString by viewModel.sortOption.collectAsStateWithLifecycle()

    var showTagsRow by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }
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

    val dbTags by viewModel.getTagsForCollections(currentPathIds).collectAsStateWithLifecycle(initialValue = emptyList())
    val subCollections by viewModel.getSubCollections(collectionId).collectAsStateWithLifecycle(initialValue = emptyList())
    val tagsList = remember(dbTags) { listOf("Toutes") + dbTags.map { it.name }.distinct() }

    val totalCount by remember(collectionId) { viewModel.getTotalCount(collectionId) }.collectAsStateWithLifecycle(initialValue = 0)
    val totalValue by remember(collectionId) { viewModel.getTotalValue(collectionId) }.collectAsStateWithLifecycle(initialValue = 0.0)
    val formattedValue = remember(totalValue) { NumberFormat.getCurrencyInstance().format(totalValue) }

    val pagedItems = viewModel.getPagedItems(collectionId).collectAsLazyPagingItems()

    val listState = rememberLazyListState()
    var isHeaderVisible by remember { mutableStateOf(true) }
    val isAtTop by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 } }
    val showHeader = isHeaderVisible || isAtTop

    val nestedScrollConnectionForHeader = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isFabExpanded) isFabExpanded = false
                if (available.y < -15f) isHeaderVisible = false
                else if (available.y > 15f) isHeaderVisible = true
                return Offset.Zero
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // --- CORRECTION : Les ressources textuelles doivent être récupérées ici ---
    val errorScanLimitText = stringResource(R.string.error_scan_limit)
    val errorScanNotFoundText = stringResource(R.string.error_scan_not_found)
    val errorScanSearchingText = stringResource(R.string.error_scan_searching)

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
                navigationIcon = { IconButton(onClick = onBackClick, modifier = Modifier.padding(start = 8.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape)) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = MaterialTheme.colorScheme.onPrimaryContainer) } },
                actions = { IconButton(onClick = { showEditSheet = true }, modifier = Modifier.padding(end = 8.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape)) { Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onPrimaryContainer) } },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.background, scrolledContainerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(visible = isFabExpanded, enter = fadeIn() + slideInVertically(initialOffsetY = { 50 }), exit = fadeOut() + slideOutVertically(targetOffsetY = { 50 })) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                        MultiFabItem(text = stringResource(R.string.action_scan), icon = CameraIcon, onClick = {
                            isFabExpanded = false
                            val barcodeScanner = BarcodeScanner(context)
                            barcodeScanner.startScan(onScanSuccess = { barcode ->
                                // --- CORRECTION : Utilisation de la variable préparée ---
                                Toast.makeText(context, errorScanSearchingText, Toast.LENGTH_SHORT).show()
                                viewModel.fetchItemFromBarcode(barcode) { title, imageUrl, errorMsg ->
                                    if (title != null) {
                                        onAddItemClick(title, imageUrl)
                                    } else {
                                        val toastMsg = if (errorMsg == "error_scan_limit") errorScanLimitText else errorScanNotFoundText
                                        Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }, onScanFailure = {})
                        })
                        MultiFabItem(text = stringResource(R.string.action_create_collection), icon = FolderIcon, onClick = { isFabExpanded = false; showAddSubCollectionDialog = true })
                        MultiFabItem(text = stringResource(R.string.action_add_item), icon = Icons.Default.Add, onClick = { isFabExpanded = false; onAddItemClick(null, null) })
                    }
                }
                FloatingActionButton(onClick = { isFabExpanded = !isFabExpanded }, containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                    val rotation by animateFloatAsState(targetValue = if (isFabExpanded) 45f else 0f, label = "fab_rotation")
                    Icon(Icons.Default.Add, contentDescription = "Menu Actions", modifier = Modifier.size(28.dp).rotate(rotation))
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize().nestedScroll(nestedScrollConnectionForHeader)) {
                AnimatedVisibility(visible = showHeader, enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(), exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()) {
                    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(bottom = 8.dp)) {

                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text(stringResource(R.string.search_placeholder)) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                            shape = CircleShape,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = { if (searchQuery.isNotEmpty()) { IconButton(onClick = { viewModel.updateSearchQuery("") }) { Icon(Icons.Default.Clear, contentDescription = null) } } },
                            colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent, focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant),
                            singleLine = true
                        )

                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.width(52.dp).height(40.dp).background(if (showTagsRow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp)).clickable { if (dbTags.isNotEmpty()) { showTagsRow = !showTagsRow } else { Toast.makeText(context, "Aucune étiquette dans ce dossier", Toast.LENGTH_SHORT).show() } }, contentAlignment = Alignment.Center) { Icon(LabelIcon, contentDescription = "Tags", tint = if (showTagsRow) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) }
                            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).clickable { Toast.makeText(context, "Favoris bientôt disponibles", Toast.LENGTH_SHORT).show() }, contentAlignment = Alignment.Center) { Icon(Icons.Default.FavoriteBorder, contentDescription = "Favoris", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) }
                            Box(modifier = Modifier.width(85.dp).height(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) { Text(text = totalCount.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            Box(modifier = Modifier.width(85.dp).height(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) { Text(text = formattedValue, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).clickable { Toast.makeText(context, "Corbeille bientôt disponible", Toast.LENGTH_SHORT).show() }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Delete, contentDescription = "Corbeille", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) }
                            Box {
                                Box(modifier = Modifier.width(52.dp).height(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp)).clickable { showSortMenu = true }, contentAlignment = Alignment.Center) { Icon(FilterListIcon, contentDescription = "Trier", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) }
                                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                    DropdownMenuItem(text = { Text(stringResource(R.string.sort_name_asc)) }, onClick = { viewModel.updateSortOption(SortOption.NAME_ASC.name); showSortMenu = false })
                                    DropdownMenuItem(text = { Text(stringResource(R.string.sort_name_desc)) }, onClick = { viewModel.updateSortOption(SortOption.NAME_DESC.name); showSortMenu = false })
                                    DropdownMenuItem(text = { Text(stringResource(R.string.sort_price_desc)) }, onClick = { viewModel.updateSortOption(SortOption.PRICE_DESC.name); showSortMenu = false })
                                    DropdownMenuItem(text = { Text(stringResource(R.string.sort_price_asc)) }, onClick = { viewModel.updateSortOption(SortOption.PRICE_ASC.name); showSortMenu = false })
                                    DropdownMenuItem(text = { Text(stringResource(R.string.sort_date_desc)) }, onClick = { viewModel.updateSortOption(SortOption.DATE_DESC.name); showSortMenu = false })
                                    DropdownMenuItem(text = { Text(stringResource(R.string.sort_date_asc)) }, onClick = { viewModel.updateSortOption(SortOption.DATE_ASC.name); showSortMenu = false })
                                }
                            }
                        }

                        if (showTagsRow && dbTags.isNotEmpty()) {
                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                tagsList.forEach { tag -> CustomTagChip(text = tag, isSelected = selectedTagFilter == tag, onClick = { viewModel.updateTagFilter(tag) }, onLongClick = if (tag != "Toutes") { { selectedTagToManage = tag; showTagOptionsDialog = true } } else null) }
                            }
                        }
                    }
                }

                LazyColumn(state = listState, modifier = Modifier.weight(1f), contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (subCollections.isNotEmpty()) {
                        item { Text(stringResource(R.string.title_folders), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) }
                        val chunkedSubCols = subCollections.chunked(3)
                        items(chunkedSubCols) { rowItems ->
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                for (subCol in rowItems) {
                                    val subCount = viewModel.getValidMoveDestinations(subCol.id, allCollections).size
                                    SubCollectionSmallCard(subCol, subCount, Modifier.weight(1f), onCollectionClick)
                                }
                                repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }
                        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                    }

                    if (pagedItems.itemCount > 0 || subCollections.isNotEmpty()) {
                        item { Text(stringResource(R.string.title_items), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp)) }
                    }

                    if (pagedItems.itemCount == 0 && subCollections.isEmpty()) {
                        item { Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) { Text(stringResource(R.string.empty_folder), color = MaterialTheme.colorScheme.outline) } }
                    } else {
                        items(
                            count = pagedItems.itemCount,
                            key = { index -> pagedItems[index]?.item?.id ?: index }
                        ) { index ->
                            val itemWithTags = pagedItems[index]
                            if (itemWithTags != null) {
                                val item = itemWithTags.item
                                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onItemClick(item.id) }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        with(sharedTransitionScope) {
                                            if (item.imageUrl.isNotBlank()) {
                                                AsyncImage(model = item.imageUrl, contentDescription = null, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).sharedElement(rememberSharedContentState(key = "item_image_${item.id}"), animatedVisibilityScope = animatedVisibilityScope, boundsTransform = { _, _ -> tween(durationMillis = 400) }), contentScale = ContentScale.Crop)
                                            } else {
                                                Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).sharedElement(rememberSharedContentState(key = "item_image_${item.id}"), animatedVisibilityScope = animatedVisibilityScope, boundsTransform = { _, _ -> tween(durationMillis = 400) }), contentAlignment = Alignment.Center) { val displayEmoji = if (collectionCover.isNotBlank() && !collectionCover.startsWith("file")) collectionCover else getEmojiForCollection(collectionName); Text(text = displayEmoji, fontSize = 24.sp) }
                                            }
                                        }
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
            if (isFabExpanded) { Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { isFabExpanded = false }) }
        }
    }

    if (showEditSheet) { ModalBottomSheet(onDismissRequest = { showEditSheet = false }) { Column(modifier = Modifier.padding(bottom = 32.dp)) { Text(stringResource(R.string.manage_folder, collectionName), modifier = Modifier.padding(start = 16.dp, bottom = 8.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); ListItem(headlineContent = { Text(stringResource(R.string.rename)) }, leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) }, modifier = Modifier.clickable { showEditSheet = false; showRenameDialog = true }); ListItem(headlineContent = { Text(stringResource(R.string.move_folder)) }, leadingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) }, modifier = Modifier.clickable { showEditSheet = false; showMoveDialog = true }); ListItem(headlineContent = { Text(stringResource(R.string.delete_folder), color = MaterialTheme.colorScheme.error) }, leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }, modifier = Modifier.clickable { showEditSheet = false; showDeleteCollectionDialog = true }) } } }
    if (showAddSubCollectionDialog) { CollectionDialog(title = stringResource(R.string.new_subfolder_title), onDismiss = { showAddSubCollectionDialog = false }, onConfirm = { name, cover -> viewModel.insertCollection(name = name, cover = cover, parentId = collectionId); showAddSubCollectionDialog = false }, onProcessImage = { uri, cutout, callback -> viewModel.processAndSaveImage(uri, cutout, callback) }) }
    if (showMoveDialog) { val validDestinations = viewModel.getValidMoveDestinations(collectionId, allCollections); AlertDialog(onDismissRequest = { showMoveDialog = false }, title = { Text(stringResource(R.string.move_folder)) }, text = { LazyColumn(modifier = Modifier.fillMaxWidth()) { item { ListItem(headlineContent = { Text(stringResource(R.string.move_to_root), fontWeight = FontWeight.Bold) }, modifier = Modifier.clickable { viewModel.updateCollectionParent(collectionId, null); showMoveDialog = false }); HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }; items(validDestinations) { dest -> ListItem(headlineContent = { Text("📁 ${dest.name}") }, modifier = Modifier.clickable { viewModel.updateCollectionParent(collectionId, dest.id); showMoveDialog = false }) } } }, confirmButton = { TextButton(onClick = { showMoveDialog = false }) { Text(stringResource(R.string.cancel)) } }) }
    if (showTagOptionsDialog) AlertDialog(onDismissRequest = { showTagOptionsDialog = false }, title = { Text(stringResource(R.string.tag_options_title, selectedTagToManage)) }, text = { Text(stringResource(R.string.tag_options_subtitle)) }, confirmButton = { Button(onClick = { renameTagInput = selectedTagToManage; showTagOptionsDialog = false; showRenameTagDialog = true }) { Text(stringResource(R.string.rename)) } }, dismissButton = { Button(onClick = { showTagOptionsDialog = false; showDeleteTagDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.delete_folder).replace("le dossier", "")) } })
    if (showRenameTagDialog) AlertDialog(onDismissRequest = { showRenameTagDialog = false }, title = { Text(stringResource(R.string.rename)) }, text = { OutlinedTextField(value = renameTagInput, onValueChange = { renameTagInput = it }, singleLine = true) }, confirmButton = { Button(onClick = { if (renameTagInput.isNotBlank()) { viewModel.renameTag(collectionId, selectedTagToManage, renameTagInput.trim()); viewModel.updateTagFilter(renameTagInput.trim()); showRenameTagDialog = false } }) { Text(stringResource(R.string.btn_save)) } }, dismissButton = { TextButton(onClick = { showRenameTagDialog = false }) { Text(stringResource(R.string.cancel)) } })
    if (showDeleteTagDialog) AlertDialog(onDismissRequest = { showDeleteTagDialog = false }, title = { Text(stringResource(R.string.delete_tag_title)) }, text = { Text(stringResource(R.string.delete_tag_warning, selectedTagToManage)) }, confirmButton = { Button(onClick = { val tagToDelete = dbTags.find { it.name == selectedTagToManage }; tagToDelete?.let { viewModel.deleteTag(it) }; viewModel.updateTagFilter("Toutes"); showDeleteTagDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.create).replace("Créer", "Confirmer")) } }, dismissButton = { TextButton(onClick = { showDeleteTagDialog = false }) { Text(stringResource(R.string.cancel)) } })
    if (showRenameDialog) AlertDialog(onDismissRequest = { showRenameDialog = false }, title = { Text(stringResource(R.string.rename)) }, text = { OutlinedTextField(value = renameInput, onValueChange = { renameInput = it }, singleLine = true) }, confirmButton = { Button(onClick = { if (renameInput.isNotBlank()) { viewModel.renameCollection(collectionId, renameInput.trim()); showRenameDialog = false } }) { Text(stringResource(R.string.btn_save)) } }, dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text(stringResource(R.string.cancel)) } })
    if (showDeleteCollectionDialog) AlertDialog(onDismissRequest = { showDeleteCollectionDialog = false }, title = { Text(stringResource(R.string.delete_item_title).replace("l\'objet", "la collection")) }, text = { Text(stringResource(R.string.delete_folder_warning)) }, confirmButton = { Button(onClick = { viewModel.deleteCollection(collectionId); showDeleteCollectionDialog = false; onBackClick() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.delete_folder).replace(" la collection", "")) } }, dismissButton = { TextButton(onClick = { showDeleteCollectionDialog = false }) { Text(stringResource(R.string.cancel)) } })
}