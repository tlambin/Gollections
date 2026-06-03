package com.pokyx.gollections.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable // NOUVEAU
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pokyx.gollections.R
import com.pokyx.gollections.data.CollectionItem
import com.pokyx.gollections.data.ItemType
import com.pokyx.gollections.data.tag.Tag
import com.pokyx.gollections.ui.viewmodels.ItemViewModel
import com.pokyx.gollections.ui.viewmodels.PropertyKeys
import com.pokyx.gollections.utils.AddTagDialog
import com.pokyx.gollections.utils.getDynamicStatusOptions
import com.pokyx.gollections.utils.getLocalizedPropertyLabel
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemScreen(
    itemId: Int,
    onBackClick: () -> Unit,
    onSaveClick: (CollectionItem, List<Tag>, Map<String, String>) -> Unit,
    viewModel: ItemViewModel = hiltViewModel()
) {
    val context = LocalContext.current
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

    val finalSelectedId = state.selectedPath.lastOrNull()
    val finalSelectedName = collectionsList.find { it.id == finalSelectedId }?.name ?: ""
    val dbTags by viewModel.getTagsForCollections(state.selectedPath).collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddTagDialog by remember { mutableStateOf(false) }

    LaunchedEffect(finalSelectedId) {
        if (finalSelectedName.isNotEmpty() && hasLoadedItem) {
            val options = getDynamicStatusOptions(context, finalSelectedName)
            if (state.status !in options) {
                viewModel.updateForm { it.copy(status = options.first()) }
            }
        }
    }

    var showPurchaseDatePicker by remember { mutableStateOf(false) }
    val purchaseDatePickerState = rememberDatePickerState()
    var showLoanDatePicker by remember { mutableStateOf(false) }
    val loanDatePickerState = rememberDatePickerState()

    var showSourceDialog by remember { mutableStateOf(false) }
    var showDetourageConfirmation by remember { mutableStateOf(false) }
    var isProcessingImage by remember { mutableStateOf(false) }

    // CORRECTION ICI : Utilisation de rememberSaveable et conversion String <-> Uri
    var tempPhotoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val tempPhotoUri = tempPhotoUriString?.let { Uri.parse(it) }

    var pendingImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val pendingImageUri = pendingImageUriString?.let { Uri.parse(it) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            pendingImageUriString = uri.toString()
            showDetourageConfirmation = true
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempPhotoUriString != null) {
            pendingImageUriString = tempPhotoUriString
            showDetourageConfirmation = true
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(state.title.ifBlank { stringResource(R.string.title_edit_item) }, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.Close, contentDescription = null) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {

            Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).clickable(enabled = !isProcessingImage) { showSourceDialog = true }, contentAlignment = Alignment.Center) {
                if (isProcessingImage) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Spacer(modifier = Modifier.height(8.dp)); Text(stringResource(R.string.text_processing_cutout), fontSize = 12.sp) } }
                else if (state.imageUrl.isNotBlank()) AsyncImage(model = state.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                else { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("📸", fontSize = 40.sp); Spacer(modifier = Modifier.height(4.dp)); Text(stringResource(R.string.dialog_illustration_text), color = MaterialTheme.colorScheme.outline, fontSize = 13.sp) } }
            }

            Text(stringResource(R.string.title_item_type), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ItemType.values().forEach { type ->
                    FilterChip(
                        selected = state.itemType == type,
                        onClick = { viewModel.changeItemType(type) },
                        label = { Text("${type.emoji} ${type.label}") }
                    )
                }
            }

            OutlinedTextField(value = state.title, onValueChange = { text -> viewModel.updateForm { it.copy(title = text) } }, label = { Text(stringResource(R.string.label_title)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

            if (state.properties.isNotEmpty()) {
                Text(stringResource(R.string.title_specific_info), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                state.properties.forEach { (labelKey, value) ->
                    val isMultiLine = labelKey in listOf(PropertyKeys.SYNOPSIS, PropertyKeys.SUMMARY, PropertyKeys.DESCRIPTION)
                    OutlinedTextField(
                        value = value,
                        onValueChange = { newValue -> viewModel.updateProperty(labelKey, newValue) },
                        label = { Text(getLocalizedPropertyLabel(labelKey)) },
                        modifier = Modifier.fillMaxWidth().then(if (isMultiLine) Modifier.height(120.dp) else Modifier),
                        maxLines = if (isMultiLine) 5 else 1,
                        singleLine = !isMultiLine,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            if (collectionsList.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = stringResource(R.string.label_location), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))

                    var parentIdForNextLevel: Long? = null
                    for (i in 0..state.selectedPath.size) {
                        val options = collectionsList.filter { it.parentId == parentIdForNextLevel }
                        if (options.isNotEmpty()) {
                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                options.forEach { opt ->
                                    val isSelected = (i < state.selectedPath.size && state.selectedPath[i] == opt.id)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.updateForm { it.copy(selectedPath = state.selectedPath.take(i) + opt.id) } },
                                        label = { Text(opt.name) }
                                    )
                                }
                            }
                        }
                        if (i < state.selectedPath.size) parentIdForNextLevel = state.selectedPath[i] else break
                    }
                }
            }

            if (state.selectedPath.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.label_tags), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(onClick = { showAddTagDialog = true }) { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    }

                    if (dbTags.isEmpty()) {
                        Text(stringResource(R.string.no_tags_available), fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    } else {
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            dbTags.distinctBy { it.name }.forEach { tag ->
                                FilterChip(
                                    selected = state.selectedTags.any { it.name == tag.name },
                                    onClick = {
                                        val currentTags = state.selectedTags
                                        val newTags = if (currentTags.any { it.name == tag.name }) currentTags.filter { it.name != tag.name }.toSet() else currentTags + tag
                                        viewModel.updateForm { it.copy(selectedTags = newTags) }
                                    },
                                    label = { Text(tag.name) }
                                )
                            }
                        }
                    }
                }
            }

            if (finalSelectedId != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = stringResource(R.string.label_status), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                    val statusOptions = getDynamicStatusOptions(context, finalSelectedName)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        statusOptions.forEach { opt -> FilterChip(selected = state.status == opt, onClick = { viewModel.updateForm { it.copy(status = opt) } }, label = { Text(opt) }) }
                    }
                }
            }

            Text(text = stringResource(R.string.title_loan_management), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.loan_switch_text), fontWeight = FontWeight.Medium); Switch(checked = state.isLoaned, onCheckedChange = { value -> viewModel.updateForm { it.copy(isLoaned = value) } }) }
                    if (state.isLoaned) {
                        OutlinedTextField(value = state.loanTo, onValueChange = { text -> viewModel.updateForm { it.copy(loanTo = text) } }, label = { Text(stringResource(R.string.label_loan_to)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(value = state.loanDate, onValueChange = {}, readOnly = true, label = { Text(stringResource(R.string.label_loan_date)) }, modifier = Modifier.fillMaxWidth())
                            Box(modifier = Modifier.matchParentSize().clickable { showLoanDatePicker = true })
                        }
                    }
                }
            }

            Text(text = stringResource(R.string.title_acquisition_info), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = state.price, onValueChange = { input -> if (input.all { it.isDigit() || it == '.' || it == ',' } && input.count { it == '.' || it == ',' } <= 1) viewModel.updateForm { it.copy(price = input) } }, label = { Text(stringResource(R.string.label_price)) }, modifier = Modifier.weight(1.5f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
                Box(modifier = Modifier.weight(1.5f)) {
                    OutlinedTextField(value = state.purchaseDate, onValueChange = {}, readOnly = true, label = { Text(stringResource(R.string.label_purchase_date)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Box(modifier = Modifier.matchParentSize().clickable { showPurchaseDatePicker = true })
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
            Button(
                onClick = {
                    if (state.title.isNotBlank() && finalSelectedId != null && itemWithTags != null) {
                        val parsedPrice = state.price.trim().replace(",", ".").toDoubleOrNull() ?: 0.0

                        val updatedItem = itemWithTags!!.item.copy(
                            title = state.title.trim(),
                            collectionId = finalSelectedId,
                            purchaseDate = state.purchaseDate.trim(),
                            price = parsedPrice,
                            imageUrl = state.imageUrl,
                            status = state.status,
                            isLoaned = state.isLoaned,
                            loanTo = if (state.isLoaned) state.loanTo.trim() else "",
                            loanDate = if (state.isLoaned) state.loanDate else "",
                            itemType = state.itemType
                        )
                        onSaveClick(updatedItem, state.selectedTags.toList(), state.properties)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = state.title.isNotBlank() && finalSelectedId != null && itemWithTags != null
            ) { Text(stringResource(R.string.btn_save_edits), fontSize = 16.sp) }
        }

        if (showPurchaseDatePicker) DatePickerDialog(onDismissRequest = { showPurchaseDatePicker = false }, confirmButton = { TextButton(onClick = { purchaseDatePickerState.selectedDateMillis?.let { millis -> val dateStr = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); viewModel.updateForm { it.copy(purchaseDate = dateStr) } }; showPurchaseDatePicker = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { showPurchaseDatePicker = false }) { Text(stringResource(R.string.cancel)) } }) { DatePicker(state = purchaseDatePickerState) }
        if (showLoanDatePicker) DatePickerDialog(onDismissRequest = { showLoanDatePicker = false }, confirmButton = { TextButton(onClick = { loanDatePickerState.selectedDateMillis?.let { millis -> val dateStr = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); viewModel.updateForm { it.copy(loanDate = dateStr) } }; showLoanDatePicker = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { showLoanDatePicker = false }) { Text(stringResource(R.string.cancel)) } }) { DatePicker(state = loanDatePickerState) }

        if (showSourceDialog) AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text(stringResource(R.string.dialog_illustration_title)) },
            text = { Text(stringResource(R.string.dialog_illustration_text)) },
            confirmButton = {
                Button(onClick = {
                    showSourceDialog = false
                    val tempFile = File.createTempFile("cam_", ".jpg", context.cacheDir)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                    // CORRECTION ICI : On sauvegarde la String avant de lancer la caméra
                    tempPhotoUriString = uri.toString()
                    cameraLauncher.launch(uri)
                }) { Text(stringResource(R.string.source_camera)) }
            },
            dismissButton = {
                Button(onClick = {
                    showSourceDialog = false;
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text(stringResource(R.string.source_gallery)) }
            }
        )

        if (showDetourageConfirmation) AlertDialog(
            onDismissRequest = { showDetourageConfirmation = false },
            title = { Text(stringResource(R.string.dialog_cutout_title)) },
            text = { Text(stringResource(R.string.dialog_cutout_text)) },
            confirmButton = {
                Button(onClick = {
                    showDetourageConfirmation = false
                    pendingImageUri?.let { uri ->
                        isProcessingImage = true
                        viewModel.processAndSaveImage(uri, true) { finalUrl ->
                            isProcessingImage = false
                            if (finalUrl != null) viewModel.updateForm { it.copy(imageUrl = finalUrl) }
                            else Toast.makeText(context, R.string.toast_cutout_error, Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text(stringResource(R.string.btn_yes)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDetourageConfirmation = false
                    pendingImageUri?.let { uri ->
                        isProcessingImage = true
                        viewModel.processAndSaveImage(uri, false) { finalUrl ->
                            isProcessingImage = false
                            if (finalUrl != null) viewModel.updateForm { it.copy(imageUrl = finalUrl) }
                        }
                    }
                }) { Text(stringResource(R.string.btn_no)) }
            }
        )

        if (showAddTagDialog && finalSelectedId != null) {
            AddTagDialog(onDismiss = { showAddTagDialog = false }, onConfirm = { tagName -> viewModel.insertTag(tagName, finalSelectedId); showAddTagDialog = false })
        }
    }
}