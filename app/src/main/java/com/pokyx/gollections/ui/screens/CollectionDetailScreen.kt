package com.pokyx.gollections.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.pokyx.gollections.R
import com.pokyx.gollections.ui.components.CollectionDialog
import com.pokyx.gollections.ui.components.CustomTagChip
import com.pokyx.gollections.ui.components.ExpandableActionFab
import com.pokyx.gollections.ui.components.FilterListIcon
import com.pokyx.gollections.ui.components.LabelIcon
import com.pokyx.gollections.ui.components.SubCollectionSmallCard
import com.pokyx.gollections.ui.viewmodels.CollectionDetailViewModel
import com.pokyx.gollections.ui.viewmodels.ScanEvent
import com.pokyx.gollections.utils.BarcodeScanner
import com.pokyx.gollections.utils.buildPathBottomUp
import com.pokyx.gollections.utils.getEmojiForCollection
import kotlinx.coroutines.launch
import java.text.NumberFormat

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
    val lifecycleOwner = LocalLifecycleOwner.current
    val allCollections by viewModel.allCollections.collectAsStateWithLifecycle()

    val currentCollection = allCollections.find { it.id == collectionId }
    val collectionName = currentCollection?.name ?: ""
    val collectionCover = currentCollection?.cover ?: ""

    val currentPathIds = remember(collectionId, allCollections) { buildPathBottomUp(collectionId, allCollections) }
    val pathCollections = currentPathIds.mapNotNull { id -> allCollections.find { it.id == id } }

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTagFilter by viewModel.tagFilter.collectAsStateWithLifecycle()

    var showTagsRow by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }

    var showEditCollectionDialog by remember { mutableStateOf(false) }
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

    val pagedItemsFlow = remember(collectionId) { viewModel.getPagedItems(collectionId) }
    val pagedItems = pagedItemsFlow.collectAsLazyPagingItems()

    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val msgSearching = stringResource(R.string.error_scan_searching)
    val msgLimit = stringResource(R.string.error_scan_limit)
    val msgNotFound = stringResource(R.string.error_scan_not_found)

    LaunchedEffect(viewModel.scanEvent, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.scanEvent.collect { event ->
                when (event) {
                    is ScanEvent.Searching -> {
                        scope.launch { snackbarHostState.showSnackbar(msgSearching, duration = SnackbarDuration.Short) }
                    }
                    is ScanEvent.Success -> {
                        onAddItemClick(event.title, event.imageUrl)
                    }
                    is ScanEvent.Error -> {
                        val msg = if (event.message == "error_scan_limit") msgLimit else msgNotFound
                        scope.launch { snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Long) }
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
                LargeTopAppBar(
                    title = {
                        val collapsedFraction = scrollBehavior.state.collapsedFraction
                        if (collapsedFraction <= 0.5f) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                if (collectionCover.startsWith("file") || collectionCover.startsWith("/") || collectionCover.startsWith("content") || collectionCover.startsWith("http")) {
                                    AsyncImage(model = collectionCover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)))
                                } else {
                                    val displayEmoji = if (collectionCover.isNotBlank()) collectionCover else getEmojiForCollection(collectionName)
                                    Text(text = displayEmoji, fontSize = 36.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = collectionName, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.padding(start = 8.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showEditSheet = true },
                            modifier = Modifier.padding(end = 8.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.background, scrolledContainerColor = MaterialTheme.colorScheme.background)
                )

                val collapsedFraction = scrollBehavior.state.collapsedFraction

                if (collapsedFraction > 0.5f) {
                    Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().height(64.dp), contentAlignment = Alignment.Center) {
                        Text(text = collectionName, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 72.dp))
                    }
                }
                else if (pathCollections.size > 1) {
                    val breadcrumbText = pathCollections.dropLast(1).joinToString(" > ") { it.name }
                    Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().height(64.dp), contentAlignment = Alignment.Center) {
                        Text(text = breadcrumbText, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 72.dp))
                    }
                }
            }
        },
        floatingActionButton = {
            ExpandableActionFab(
                isExpanded = isFabExpanded,
                createFolderText = stringResource(R.string.action_create_subfolder),
                onToggle = { isFabExpanded = !isFabExpanded },
                onScanClick = {
                    isFabExpanded = false
                    val barcodeScanner = BarcodeScanner(context)
                    barcodeScanner.startScan(onScanSuccess = { barcode -> viewModel.fetchItemFromBarcode(barcode) }, onScanFailure = { })
                },
                onCreateFolderClick = { isFabExpanded = false; showAddSubCollectionDialog = true },
                onAddItemClick = { isFabExpanded = false; onAddItemClick(null, null) }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                item {
                    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(bottom = 8.dp)) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text(stringResource(R.string.search_placeholder)) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                            shape = CircleShape,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 6.dp)) {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.updateSearchQuery("") }) { Icon(Icons.Default.Clear, contentDescription = null) }
                                    }
                                    Box(
                                        modifier = Modifier.size(40.dp).border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                                scope.launch { snackbarHostState.showSnackbar("Favoris bientôt disponibles", duration = SnackbarDuration.Short) }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Favorite, contentDescription = "Favoris", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                    }
                                }
                            },
                            colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent, focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant),
                            singleLine = true
                        )

                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape).clickable {
                                if (dbTags.isNotEmpty()) { showTagsRow = !showTagsRow }
                                else { scope.launch { snackbarHostState.showSnackbar("Aucune étiquette dans ce dossier", duration = SnackbarDuration.Short) } }
                            }, contentAlignment = Alignment.Center
                            ) { Icon(LabelIcon, contentDescription = "Tags", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp)) }

                            Box(modifier = Modifier.height(44.dp).width(90.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).border(2.5.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                                Text(text = totalCount.toString(), color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Box(modifier = Modifier.height(44.dp).width(90.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).border(2.5.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                                Text(text = formattedValue, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            Box {
                                Box(modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape).clickable { showSortMenu = true }, contentAlignment = Alignment.Center) {
                                    Icon(FilterListIcon, contentDescription = "Trier", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                                }
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

                if (subCollections.isNotEmpty()) {
                    item { Text(stringResource(R.string.title_folders), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) }
                    val chunkedSubCols = subCollections.chunked(3)
                    items(chunkedSubCols) { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            for (subCol in rowItems) {
                                val subCollectionItemCount by viewModel.getTotalCount(subCol.id).collectAsStateWithLifecycle(initialValue = 0)
                                SubCollectionSmallCard(subCol, subCollectionItemCount, Modifier.weight(1f), onCollectionClick)
                            }
                            repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                }

                if (pagedItems.itemCount > 0 || subCollections.isNotEmpty()) {
                    item { Text(stringResource(R.string.title_items), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp)) }
                }

                val isPagingLoading = pagedItems.loadState.refresh is LoadState.Loading

                if (pagedItems.itemCount == 0 && subCollections.isEmpty() && !isPagingLoading) {
                    item { Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) { Text(stringResource(R.string.empty_folder), color = MaterialTheme.colorScheme.outline) } }
                } else {
                    items(count = pagedItems.itemCount, key = { index -> pagedItems[index]?.item?.id ?: index }) { index ->
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

            if (isFabExpanded) {
                Spacer(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { isFabExpanded = false })
            }
        }
    }

    if (showEditSheet) {
        ModalBottomSheet(onDismissRequest = { showEditSheet = false }) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(stringResource(R.string.manage_folder, collectionName), modifier = Modifier.padding(start = 16.dp, bottom = 8.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                ListItem(headlineContent = { Text(stringResource(R.string.rename)) }, leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) }, modifier = Modifier.clickable { showEditSheet = false; showEditCollectionDialog = true })
                ListItem(headlineContent = { Text(stringResource(R.string.move_folder)) }, leadingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) }, modifier = Modifier.clickable { showEditSheet = false; showMoveDialog = true })
                ListItem(headlineContent = { Text(stringResource(R.string.delete_folder), color = MaterialTheme.colorScheme.error) }, leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }, modifier = Modifier.clickable { showEditSheet = false; showDeleteCollectionDialog = true })
            }
        }
    }

    if (showEditCollectionDialog) { CollectionDialog(title = stringResource(R.string.rename), initialName = collectionName, initialCover = collectionCover, onDismiss = { showEditCollectionDialog = false }, onConfirm = { name, cover -> viewModel.updateCollection(collectionId, name, cover); showEditCollectionDialog = false }, onProcessImage = { uri, cutout, callback -> viewModel.processAndSaveImage(uri, cutout, callback) }) }
    if (showAddSubCollectionDialog) { CollectionDialog(title = stringResource(R.string.new_subfolder_title), onDismiss = { showAddSubCollectionDialog = false }, onConfirm = { name, cover -> viewModel.insertCollection(name = name, cover = cover, parentId = collectionId); showAddSubCollectionDialog = false }, onProcessImage = { uri, cutout, callback -> viewModel.processAndSaveImage(uri, cutout, callback) }) }
    if (showMoveDialog) { val validDestinations = viewModel.getValidMoveDestinations(collectionId, allCollections); AlertDialog(onDismissRequest = { showMoveDialog = false }, title = { Text(stringResource(R.string.move_folder)) }, text = { LazyColumn(modifier = Modifier.fillMaxWidth()) { item { ListItem(headlineContent = { Text(stringResource(R.string.move_to_root), fontWeight = FontWeight.Bold) }, modifier = Modifier.clickable { viewModel.updateCollectionParent(collectionId, null); showMoveDialog = false }); HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }; items(validDestinations) { dest -> ListItem(headlineContent = { Text("📁 ${dest.name}") }, modifier = Modifier.clickable { viewModel.updateCollectionParent(collectionId, dest.id); showMoveDialog = false }) } } }, confirmButton = { TextButton(onClick = { showMoveDialog = false }) { Text(stringResource(R.string.cancel)) } }) }
    if (showTagOptionsDialog) AlertDialog(onDismissRequest = { showTagOptionsDialog = false }, title = { Text(stringResource(R.string.tag_options_title, selectedTagToManage)) }, text = { Text(stringResource(R.string.tag_options_subtitle)) }, confirmButton = { Button(onClick = { renameTagInput = selectedTagToManage; showTagOptionsDialog = false; showRenameTagDialog = true }) { Text(stringResource(R.string.rename)) } }, dismissButton = { Button(onClick = { showTagOptionsDialog = false; showDeleteTagDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.delete_folder).replace("le dossier", "")) } })
    if (showRenameTagDialog) AlertDialog(onDismissRequest = { showRenameTagDialog = false }, title = { Text(stringResource(R.string.rename)) }, text = { OutlinedTextField(value = renameTagInput, onValueChange = { renameTagInput = it }, singleLine = true) }, confirmButton = { Button(onClick = { if (renameTagInput.isNotBlank()) { viewModel.renameTag(collectionId, selectedTagToManage, renameTagInput.trim()); viewModel.updateTagFilter(renameTagInput.trim()); showRenameTagDialog = false } }) { Text(stringResource(R.string.btn_save)) } }, dismissButton = { TextButton(onClick = { showRenameTagDialog = false }) { Text(stringResource(R.string.cancel)) } })
    if (showDeleteTagDialog) AlertDialog(onDismissRequest = { showDeleteTagDialog = false }, title = { Text(stringResource(R.string.delete_tag_title)) }, text = { Text(stringResource(R.string.delete_tag_warning, selectedTagToManage)) }, confirmButton = { Button(onClick = { val tagToDelete = dbTags.find { it.name == selectedTagToManage }; tagToDelete?.let { viewModel.deleteTag(it) }; viewModel.updateTagFilter("Toutes"); showDeleteTagDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.create).replace("Créer", "Confirmer")) } }, dismissButton = { TextButton(onClick = { showDeleteTagDialog = false }) { Text(stringResource(R.string.cancel)) } })
    if (showDeleteCollectionDialog) AlertDialog(onDismissRequest = { showDeleteCollectionDialog = false }, title = { Text(stringResource(R.string.delete_item_title).replace("l\'objet", "la collection")) }, text = { Text(stringResource(R.string.delete_folder_warning)) }, confirmButton = { Button(onClick = { viewModel.deleteCollection(collectionId); showDeleteCollectionDialog = false; onBackClick() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.delete_folder).replace(" la collection", "")) } }, dismissButton = { TextButton(onClick = { showDeleteCollectionDialog = false }) { Text(stringResource(R.string.cancel)) } })
}