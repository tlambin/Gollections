package com.pokyx.gollections.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pokyx.gollections.R
import com.pokyx.gollections.ui.viewmodels.ItemViewModel
import com.pokyx.gollections.utils.DetailRow
import com.pokyx.gollections.utils.getEmojiForCollection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    itemId: Int,
    onBackClick: () -> Unit,
    onEditClick: (Int) -> Unit,
    viewModel: ItemViewModel // <-- UTILISATION DU NOUVEAU VIEWMODEL ICI
) {
    val itemWithTags by viewModel.getItemByIdWithTags(itemId).collectAsStateWithLifecycle(initialValue = null)
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (itemWithTags == null) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    val currentItem = itemWithTags!!.item
    val tagsList = itemWithTags!!.tags

    val directParentName = "" // Optionnel: Tu peux récupérer le nom parent ici si besoin

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.title_details), fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) },
        floatingActionButton = {
            Row(modifier = Modifier.height(56.dp).wrapContentWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.fillMaxHeight().clickable { onEditClick(currentItem.id) }.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(10.dp)); Text(text = stringResource(R.string.rename).replace("Renommer", "Modifier"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 14.sp) }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.6f).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)))
                Box(modifier = Modifier.fillMaxHeight().aspectRatio(1f).clickable { showDeleteDialog = true }, contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState())) {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                if (currentItem.imageUrl.isNotBlank()) AsyncImage(model = currentItem.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Text(text = getEmojiForCollection(directParentName), fontSize = 80.sp)
            }

            Column(modifier = Modifier.padding(24.dp).padding(bottom = 72.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = currentItem.title, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold))

                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tagsList.forEach { tag ->
                        SuggestionChip(onClick = { }, label = { Text(tag.name) })
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                DetailRow(label = stringResource(R.string.label_purchase_date), value = currentItem.purchaseDate.ifBlank { stringResource(R.string.not_specified) })
                DetailRow(label = stringResource(R.string.label_price).replace(" (€)", ""), value = if (currentItem.price.isNotBlank()) "${currentItem.price} €" else stringResource(R.string.not_specified_price))
                DetailRow(label = stringResource(R.string.label_status), value = currentItem.status)

                Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), colors = CardDefaults.cardColors(containerColor = if (currentItem.isLoaned) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column { Text(stringResource(R.string.loan_status_title), fontWeight = FontWeight.Bold, color = if (currentItem.isLoaned) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer); if (currentItem.isLoaned) { Text(stringResource(R.string.loan_status_active_to, currentItem.loanTo), fontSize = 14.sp); Text(stringResource(R.string.loan_status_active_since, currentItem.loanDate), fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)) } else { Text(stringResource(R.string.loan_status_inactive), fontSize = 14.sp) } }
                        if (currentItem.isLoaned) Text(text = "⚠️", fontSize = 24.sp) else Text(text = "✅", fontSize = 24.sp)
                    }
                }
            }
        }
    }
    if (showDeleteDialog) { AlertDialog(onDismissRequest = { showDeleteDialog = false }, title = { Text(stringResource(R.string.delete_item_title)) }, text = { Text(stringResource(R.string.delete_item_warning, currentItem.title)) }, confirmButton = { TextButton(onClick = { viewModel.deleteItem(currentItem); showDeleteDialog = false; onBackClick() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.delete_folder).replace(" la collection", "")) } }, dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) } }) }
}