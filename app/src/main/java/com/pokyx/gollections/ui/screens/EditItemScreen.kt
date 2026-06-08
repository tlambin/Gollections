package com.pokyx.gollections.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pokyx.gollections.R
import com.pokyx.gollections.data.CollectionItem
import com.pokyx.gollections.data.tag.Tag
import com.pokyx.gollections.ui.components.ItemFormBody
import com.pokyx.gollections.ui.viewmodels.ItemViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemScreen(
    itemId: Int,
    onBackClick: () -> Unit,
    onSaveClick: (CollectionItem, List<Tag>, Map<String, String>) -> Unit,
    viewModel: ItemViewModel = hiltViewModel()
) {
    val context = LocalContext.current // NOUVEAU : Contexte pour les Toasts
    val itemWithTags by viewModel.getItemByIdWithTags(itemId).collectAsStateWithLifecycle(initialValue = null)
    val collectionsList by viewModel.collections.collectAsStateWithLifecycle()
    val state by viewModel.formState.collectAsStateWithLifecycle()

    var hasLoadedItem by remember { mutableStateOf(false) }

    LaunchedEffect(itemWithTags, collectionsList) {
        if (itemWithTags != null && collectionsList.isNotEmpty() && !hasLoadedItem) {
            viewModel.loadItemIntoForm(itemWithTags!!, collectionsList)
            hasLoadedItem = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title.ifBlank { stringResource(R.string.title_edit_item) }, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.Close, contentDescription = null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (itemWithTags != null && hasLoadedItem) {
                ItemFormBody(
                    viewModel = viewModel,
                    buttonText = stringResource(R.string.btn_save_edits),
                    onSaveClick = { finalState ->
                        val finalSelectedId = finalState.selectedPath.lastOrNull()

                        // OPTIMISATION UX : Validation explicite
                        if (finalState.title.isBlank()) {
                            Toast.makeText(context, "Veuillez entrer un titre", Toast.LENGTH_SHORT).show()
                            return@ItemFormBody
                        }
                        if (finalSelectedId == null) {
                            Toast.makeText(context, "Veuillez sélectionner un dossier", Toast.LENGTH_SHORT).show()
                            return@ItemFormBody
                        }

                        // Sauvegarde de l'objet mis à jour
                        val parsedPrice = finalState.price.trim().replace(",", ".").toDoubleOrNull() ?: 0.0
                        val updatedItem = itemWithTags!!.item.copy(
                            title = finalState.title.trim(),
                            collectionId = finalSelectedId,
                            purchaseDate = finalState.purchaseDate.trim(),
                            price = parsedPrice,
                            imageUrl = finalState.imageUrl,
                            status = finalState.status,
                            isLoaned = finalState.isLoaned,
                            loanTo = if (finalState.isLoaned) finalState.loanTo.trim() else "",
                            loanDate = if (finalState.isLoaned) finalState.loanDate else "",
                            itemType = finalState.itemType
                        )

                        val stringProperties = finalState.properties.mapKeys { it.key.value }
                        onSaveClick(updatedItem, finalState.selectedTags.toList(), stringProperties)
                    }
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}