package com.pokyx.gollections.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import com.pokyx.gollections.ui.components.CollectionDialog
import com.pokyx.gollections.ui.viewmodels.DashboardViewModel
import com.pokyx.gollections.utils.BarcodeScanner
import com.pokyx.gollections.utils.getEmojiForCollection
import com.pokyx.gollections.utils.getUnitForCollection
import com.pokyx.gollections.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onCollectionClick: (Long) -> Unit,
    onAddItemClick: () -> Unit,
    viewModel: DashboardViewModel
) {
    val context = LocalContext.current

    val rootCollections by viewModel.rootCollections.collectAsStateWithLifecycle()
    val allCollections by viewModel.collections.collectAsStateWithLifecycle()
    val allItemsWithTags by viewModel.allItemsWithTags.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResultsWithTags by viewModel.searchedItemsWithTags.collectAsStateWithLifecycle()

    var showAddCollectionDialog by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 50 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { 50 })
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        MultiFabItem(
                            text = "Scanner",
                            icon = CameraIcon,
                            onClick = {
                                isFabExpanded = false
                                // Instanciation locale sans fuite de mémoire
                                val barcodeScanner = BarcodeScanner(context)
                                barcodeScanner.startScan(
                                    onScanSuccess = { barcode ->
                                        Toast.makeText(context, "Code détecté : $barcode", Toast.LENGTH_LONG).show()
                                    },
                                    onScanFailure = { exception ->
                                        android.util.Log.e("BarcodeScan", "Erreur lors du scan : ${exception.message}")
                                    }
                                )
                            }
                        )
                        MultiFabItem(
                            text = "Créer une collection",
                            icon = FolderIcon,
                            onClick = { isFabExpanded = false; showAddCollectionDialog = true }
                        )
                        MultiFabItem(
                            text = "Ajouter un objet",
                            icon = Icons.Default.Add,
                            onClick = { isFabExpanded = false; onAddItemClick() }
                        )
                    }
                }

                FloatingActionButton(
                    onClick = { isFabExpanded = !isFabExpanded },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    val rotation by animateFloatAsState(targetValue = if (isFabExpanded) 45f else 0f, label = "fab_rotation")
                    Icon(Icons.Default.Add, contentDescription = "Menu Actions", modifier = Modifier.size(28.dp).rotate(rotation))
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                item {
                    TextField(
                        value = searchQuery, onValueChange = { viewModel.updateSearchQuery(it) }, placeholder = { Text(stringResource(R.string.search_placeholder)) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), shape = CircleShape, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, trailingIcon = { if (searchQuery.isNotEmpty()) { IconButton(onClick = { viewModel.updateSearchQuery("") }) { Icon(Icons.Default.Clear, contentDescription = null) } } },
                        colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent, focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant), singleLine = true
                    )
                }

                if (searchQuery.isEmpty()) {
                    item { StatsSlider(totalItems = allItemsWithTags.size, totalCollections = rootCollections.size, loanedItems = allItemsWithTags.count { it.item.isLoaned }) }
                    item { Text(text = stringResource(R.string.my_collections), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 24.dp)) }
                    item { CollectionsGrid(collections = rootCollections, allCollections = allCollections, items = allItemsWithTags, onCollectionClick = onCollectionClick, onAddCollectionClick = { showAddCollectionDialog = true }, viewModel = viewModel) }
                } else {
                    item { Text(text = "${stringResource(R.string.title_items)} (${searchResultsWithTags.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 24.dp)) }
                    if (searchResultsWithTags.isEmpty()) { item { Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_object_found), color = Color.Gray) } } }
                    else {
                        items(searchResultsWithTags) { itemWithTags ->
                            val item = itemWithTags.item
                            val parentCol = allCollections.find { it.id == item.collectionId }
                            val colName = parentCol?.name ?: ""
                            val tagsStr = itemWithTags.tags.joinToString(" • ") { it.name }
                            ListItem(
                                headlineContent = { Text(item.title, fontWeight = FontWeight.SemiBold) },
                                supportingContent = { if (tagsStr.isNotEmpty()) Text("$colName • $tagsStr") else Text(colName) },
                                leadingContent = { Text(getEmojiForCollection(colName), fontSize = 24.sp) },
                                modifier = Modifier.padding(horizontal = 24.dp).clickable { onCollectionClick(item.collectionId) }
                            )
                        }
                    }
                }
            }

            if (isFabExpanded) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { isFabExpanded = false }
                )
            }
        }
    }

    if (showAddCollectionDialog) {
        CollectionDialog(
            title = stringResource(R.string.new_collection),
            onDismiss = { showAddCollectionDialog = false },
            onConfirm = { name, cover ->
                viewModel.insertCollection(name = name, cover = cover, parentId = null)
                showAddCollectionDialog = false
            },
            onProcessImage = { uri, cutout, callback -> viewModel.processAndSaveImage(uri, cutout, callback) }
        )
    }
}

// Les fonctions StatsSlider, StatCardContent, CollectionsGrid, CollectionItemCard, CollectionCard et AddCollectionCard
// restent inchangées, assure-toi juste qu'elles prennent 'viewModel: DashboardViewModel' si elles l'utilisaient.
@Composable
fun StatsSlider(totalItems: Int, totalCollections: Int, loanedItems: Int) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(state = pagerState, contentPadding = PaddingValues(horizontal = 32.dp), pageSpacing = 16.dp) { page ->
            Card(modifier = Modifier.fillMaxWidth().height(130.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    when (page) {
                        0 -> StatCardContent(title = stringResource(R.string.stat_total_items), value = totalItems.toString(), sub = stringResource(R.string.stat_total_items_sub))
                        1 -> StatCardContent(title = stringResource(R.string.stat_root_collections), value = totalCollections.toString(), sub = stringResource(R.string.stat_root_collections_sub))
                        2 -> StatCardContent(title = stringResource(R.string.stat_loaned_items), value = loanedItems.toString(), sub = stringResource(R.string.stat_loaned_items_sub))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(Modifier.wrapContentHeight().fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(pagerState.pageCount) { iteration -> Box(modifier = Modifier.padding(2.dp).clip(CircleShape).background(if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.LightGray).size(8.dp)) }
        }
    }
}

@Composable
fun StatCardContent(title: String, value: String, sub: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(text = title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)); Text(text = value, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer); Text(text = sub, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)) } }

@Composable
fun CollectionsGrid(collections: List<DBCollection>, allCollections: List<DBCollection>, items: List<com.pokyx.gollections.data.tag.CollectionItemWithTags>, onCollectionClick: (Long) -> Unit, onAddCollectionClick: () -> Unit, viewModel: DashboardViewModel) {
    val totalSlots = collections.size + 1
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        for (i in 0 until totalSlots step 2) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (i < collections.size) CollectionItemCard(collections[i], allCollections, items, onCollectionClick, Modifier.weight(1f), viewModel) else if (i == collections.size) AddCollectionCard(onClick = onAddCollectionClick, modifier = Modifier.weight(1f))
                if (i + 1 < collections.size) CollectionItemCard(collections[i + 1], allCollections, items, onCollectionClick, Modifier.weight(1f), viewModel) else if (i + 1 == collections.size) AddCollectionCard(onClick = onAddCollectionClick, modifier = Modifier.weight(1f)) else Box(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun CollectionItemCard(collection: DBCollection, allCollections: List<DBCollection>, items: List<com.pokyx.gollections.data.tag.CollectionItemWithTags>, onCollectionClick: (Long) -> Unit, modifier: Modifier = Modifier, viewModel: DashboardViewModel) {
    val context = LocalContext.current
    val count = viewModel.getRecursiveItemCount(collection.id, allCollections, items)
    val unit = getUnitForCollection(context, collection.name, count)
    CollectionCard(title = collection.name, count = "$count $unit", cover = collection.cover, fallbackEmoji = getEmojiForCollection(collection.name), onClick = { onCollectionClick(collection.id) }, modifier = modifier)
}

@Composable
fun CollectionCard(title: String, count: String, cover: String, fallbackEmoji: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(120.dp).clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            if (cover.startsWith("file") || cover.startsWith("/") || cover.startsWith("content") || cover.startsWith("http")) {
                AsyncImage(model = cover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(36.dp).clip(CircleShape))
            } else {
                val displayEmoji = if (cover.isNotBlank()) cover else fallbackEmoji
                Text(text = displayEmoji, fontSize = 28.sp)
            }
            Column { Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1); Text(text = count, fontSize = 12.sp, color = Color.Gray) }
        }
    }
}

@Composable
fun AddCollectionCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(120.dp).clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)); Spacer(modifier = Modifier.height(8.dp)); Text(text = stringResource(R.string.rename).replace("Renommer", "Nouvelle"), fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary) }
    }
}