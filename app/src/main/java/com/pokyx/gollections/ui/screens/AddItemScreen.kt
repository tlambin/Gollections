package com.pokyx.gollections.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
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
fun AddItemScreen(
    preSelectedCollectionId: Long? = null,
    scannedTitle: String? = null,
    scannedImageUrl: String? = null,
    onBackClick: () -> Unit,
    onSaveClick: (CollectionItem, List<Tag>, Map<String, String>) -> Unit,
    viewModel: ItemViewModel = hiltViewModel()
) {
    val context = LocalContext.current // NOUVEAU : Contexte nécessaire pour le Toast
    val collectionsList by viewModel.collections.collectAsStateWithLifecycle()
    val state by viewModel.formState.collectAsStateWithLifecycle()

    LaunchedEffect(preSelectedCollectionId, collectionsList, scannedTitle, scannedImageUrl) {
        if (collectionsList.isNotEmpty() && state.title.isEmpty() && state.imageUrl.isEmpty()) {
            viewModel.resetFormState(preSelectedCollectionId, collectionsList, scannedTitle, scannedImageUrl)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title.ifBlank { stringResource(R.string.title_new_item) }, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.Close, contentDescription = null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            ItemFormBody(
                viewModel = viewModel,
                buttonText = stringResource(R.string.btn_save),
                onSaveClick = { finalState ->
                    val finalSelectedId = finalState.selectedPath.lastOrNull()

                    // OPTIMISATION UX : Validation avec feedback visuel
                    if (finalState.title.isBlank()) {
                        Toast.makeText(context, "Veuillez entrer un titre", Toast.LENGTH_SHORT).show()
                        return@ItemFormBody
                    }
                    if (finalSelectedId == null) {
                        Toast.makeText(context, "Veuillez sélectionner un dossier", Toast.LENGTH_SHORT).show()
                        return@ItemFormBody
                    }

                    // Si on arrive ici, tout est valide !
                    val parsedPrice = finalState.price.trim().replace(",", ".").toDoubleOrNull() ?: 0.0
                    val newItem = CollectionItem(
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
                    onSaveClick(newItem, finalState.selectedTags.toList(), stringProperties)
                }
            )
        }
    }
}