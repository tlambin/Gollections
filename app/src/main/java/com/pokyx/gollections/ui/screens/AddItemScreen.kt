package com.pokyx.gollections.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pokyx.gollections.R
import com.pokyx.gollections.data.model.Collection
import com.pokyx.gollections.data.model.CollectionItem
import com.pokyx.gollections.data.model.ItemType
import com.pokyx.gollections.data.model.Tag
import com.pokyx.gollections.ui.components.ItemFormBody
import com.pokyx.gollections.ui.viewmodels.ItemViewModel
import com.pokyx.gollections.utils.buildPathBottomUp
import com.pokyx.gollections.utils.getEmojiForCollection

private fun getVisibleCollections(
    allCollections: List<Collection>,
    parentId: Long?,
    depth: Int,
    expandedIds: Set<Long>
): List<Pair<Collection, Int>> {
    val result = mutableListOf<Pair<Collection, Int>>()
    val children = allCollections.filter { it.parentId == parentId }.sortedBy { it.name }
    for (child in children) {
        result.add(child to depth)
        if (expandedIds.contains(child.id)) {
            result.addAll(getVisibleCollections(allCollections, child.id, depth + 1, expandedIds))
        }
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    preSelectedCollectionId: Long? = null,
    scannedTitle: String? = null,
    scannedImageUrl: String? = null,
    onBackClick: () -> Unit,
    onSaveClick: (CollectionItem, List<Tag>, Map<String, String>) -> Unit,
    viewModel: ItemViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val collectionsList by viewModel.collections.collectAsStateWithLifecycle()
    val state by viewModel.formState.collectAsStateWithLifecycle()

    var showCollectionSheet by remember { mutableStateOf(false) }
    var showTypeSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var expandedCollectionIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // États pour les boîtes de dialogue de création rapide
    var showNewCollectionDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }
    var showNewTypeDialog by remember { mutableStateOf(false) }
    var newTypeName by remember { mutableStateOf("") }

    LaunchedEffect(preSelectedCollectionId, collectionsList, scannedTitle, scannedImageUrl) {
        if (collectionsList.isNotEmpty() && state.title.isEmpty() && state.imageUrl.isEmpty()) {
            viewModel.resetFormState(preSelectedCollectionId, collectionsList, scannedTitle, scannedImageUrl)
        }
    }

    LaunchedEffect(showCollectionSheet) {
        if (showCollectionSheet && state.selectedPath.isNotEmpty()) {
            expandedCollectionIds = state.selectedPath.dropLast(1).toSet()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Default.Close, contentDescription = "Fermer") }
                },
                title = { },
                actions = {
                    Button(
                        onClick = {
                            val finalSelectedIdSave = state.selectedPath.lastOrNull()
                            if (state.title.isBlank()) { Toast.makeText(context, "Veuillez entrer un titre", Toast.LENGTH_SHORT).show(); return@Button }
                            if (finalSelectedIdSave == null) { Toast.makeText(context, "Veuillez sélectionner un dossier", Toast.LENGTH_SHORT).show(); return@Button }

                            val parsedPrice = state.price.trim().replace(",", ".").toDoubleOrNull() ?: 0.0
                            val newItem = CollectionItem(
                                title = state.title.trim(),
                                collectionId = finalSelectedIdSave,
                                purchaseDate = state.purchaseDate.trim(),
                                price = parsedPrice,
                                imageUrl = state.imageUrl,
                                status = state.status,
                                isLoaned = state.isLoaned,
                                loanTo = if (state.isLoaned) state.loanTo.trim() else "",
                                loanDate = if (state.isLoaned) state.loanDate else "",
                                itemType = state.itemType
                            )
                            onSaveClick(newItem, state.selectedTags.toList(), state.properties)
                        },
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        modifier = Modifier.padding(end = 8.dp).height(36.dp)
                    ) {
                        Text(stringResource(R.string.btn_save), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            ItemFormBody(
                viewModel = viewModel,
                onCollectionClick = { showCollectionSheet = true },
                onTypeClick = { showTypeSheet = true }
            )
        }

        // --- DIALOGUES DE CRÉATION RAPIDE ---
        if (showNewCollectionDialog) {
            AlertDialog(
                onDismissRequest = { showNewCollectionDialog = false },
                title = { Text("Nouveau dossier", fontWeight = FontWeight.Bold) },
                text = { OutlinedTextField(value = newCollectionName, onValueChange = { newCollectionName = it }, label = { Text("Nom du dossier") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
                confirmButton = { Button(onClick = { Toast.makeText(context, "Création bientôt dispo avec la BDD !", Toast.LENGTH_SHORT).show(); showNewCollectionDialog = false; newCollectionName = "" }) { Text("Créer") } },
                dismissButton = { TextButton(onClick = { showNewCollectionDialog = false }) { Text("Annuler") } }
            )
        }

        if (showNewTypeDialog) {
            AlertDialog(
                onDismissRequest = { showNewTypeDialog = false },
                title = { Text("Nouveau type d'objet", fontWeight = FontWeight.Bold) },
                text = { OutlinedTextField(value = newTypeName, onValueChange = { newTypeName = it }, label = { Text("Ex: Timbre, Vinyle...") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
                confirmButton = { Button(onClick = { Toast.makeText(context, "Il faut d'abord modifier Room !", Toast.LENGTH_SHORT).show(); showNewTypeDialog = false; newTypeName = "" }) { Text("Créer") } },
                dismissButton = { TextButton(onClick = { showNewTypeDialog = false }) { Text("Annuler") } }
            )
        }

        // --- BOTTOM SHEET 1 : COLLECTIONS ---
        if (showCollectionSheet) {
            ModalBottomSheet(onDismissRequest = { showCollectionSheet = false }, sheetState = sheetState) {
                Column(modifier = Modifier.padding(bottom = 24.dp).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Choisir un dossier", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(
                            onClick = { showNewCollectionDialog = true },
                            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape).size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Nouveau dossier", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    HorizontalDivider()

                    val visibleCollections = remember(collectionsList, expandedCollectionIds) { getVisibleCollections(collectionsList, null, 0, expandedCollectionIds) }

                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(visibleCollections, key = { it.first.id }) { (collection, depth) ->
                            val hasChildren = collectionsList.any { it.parentId == collection.id }
                            val isExpanded = expandedCollectionIds.contains(collection.id)
                            val isSelected = state.selectedPath.lastOrNull() == collection.id

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = (depth * 24).dp).padding(end = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (hasChildren) {
                                    IconButton(
                                        onClick = { expandedCollectionIds = if (isExpanded) expandedCollectionIds - collection.id else expandedCollectionIds + collection.id },
                                        modifier = Modifier.size(36.dp)
                                    ) { Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Étendre", modifier = Modifier.rotate(if (isExpanded) 0f else -90f)) }
                                } else { Spacer(modifier = Modifier.width(36.dp)) }

                                Row(
                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable { viewModel.updateSelectedPath(buildPathBottomUp(collection.id, collectionsList)); showCollectionSheet = false }
                                        .padding(vertical = 12.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val cover = collection.cover
                                    if (cover.startsWith("file") || cover.startsWith("/") || cover.startsWith("content") || cover.startsWith("http")) {
                                        AsyncImage(model = cover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(20.dp).clip(CircleShape))
                                    } else {
                                        val emoji = if (cover.isNotBlank()) cover else getEmojiForCollection(collection.name)
                                        Text(text = emoji, fontSize = 16.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(collection.name, fontSize = 16.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- BOTTOM SHEET 2 : TYPES D'OBJET ---
        if (showTypeSheet) {
            ModalBottomSheet(onDismissRequest = { showTypeSheet = false }, sheetState = sheetState) {
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Type d'objet", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(
                            onClick = { showNewTypeDialog = true },
                            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape).size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Nouveau type", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    HorizontalDivider()

                    ItemType.entries.forEach { type ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.changeItemType(type); showTypeSheet = false }.padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(type.emoji, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(type.label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}