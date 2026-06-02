package com.pokyx.gollections.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pokyx.gollections.R
import com.pokyx.gollections.ui.viewmodels.ItemViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ItemDetailScreen(
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
    itemId: Int,
    onBackClick: () -> Unit,
    onEditClick: (Int) -> Unit,
    viewModel: ItemViewModel = hiltViewModel()
) {
    val itemWithTags by viewModel.getItemByIdWithTags(itemId).collectAsStateWithLifecycle(initialValue = null)
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (itemWithTags == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val item = itemWithTags!!.item
    val tags = itemWithTags!!.tags
    val properties = itemWithTags!!.properties

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.padding(start = 8.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
                },
                actions = {
                    IconButton(
                        onClick = { onEditClick(item.id) },
                        modifier = Modifier.padding(end = 8.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) { Icon(Icons.Default.Edit, contentDescription = "Modifier") }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.padding(end = 8.dp).background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                    ) { Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.onErrorContainer) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // --- LA MAGIE OPÈRE ICI SUR L'IMAGE ---
            with(sharedTransitionScope) {
                if (item.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .sharedElement(
                                rememberSharedContentState(key = "item_image_${item.id}"), // <-- Plus de "state = "
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ -> tween(durationMillis = 400) }
                            ),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .sharedElement(
                                rememberSharedContentState(key = "item_image_${item.id}"), // <-- Plus de "state = "
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ -> tween(durationMillis = 400) }
                            ),
                        contentAlignment = Alignment.Center
                    ) { Text(item.itemType.emoji, fontSize = 60.sp) }
                }
            }
            // ---------------------------------------

            // Titre et type
            Column {
                Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "${item.itemType.emoji} ${item.itemType.label}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    if (item.status.isNotBlank()) {
                        Text(text = " • ${item.status}", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            // Tags
            if (tags.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags.forEach { tag ->
                        Box(
                            modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
                        ) { Text(text = tag.name, color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 12.sp) }
                    }
                }
            }

            // Propriétés dynamiques
            if (properties.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.title_details), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))
                        properties.forEach { prop ->
                            if (prop.value.isNotBlank()) {
                                Column {
                                    Text(text = prop.label, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(text = prop.value, fontSize = 16.sp, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Informations Financières / Acquisition
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.label_price), fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Text(text = if (item.price.isNotBlank()) "${item.price} €" else stringResource(R.string.not_specified_price), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.label_purchase_date), fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Text(text = item.purchaseDate.ifBlank { stringResource(R.string.not_specified) }, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Prêt
            if (item.isLoaned) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.loan_status_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.loan_status_active_to, item.loanTo), color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Text(stringResource(R.string.loan_status_active_since, item.loanDate), fontSize = 12.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.delete_item_title)) },
                text = { Text(stringResource(R.string.delete_item_warning, item.title)) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteItem(item)
                            showDeleteDialog = false
                            onBackClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text(stringResource(R.string.delete_folder).replace("la collection", "").trim()) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
    }
}