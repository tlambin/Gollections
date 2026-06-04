package com.pokyx.gollections.ui.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pokyx.gollections.R
import com.pokyx.gollections.data.ItemType
import com.pokyx.gollections.ui.theme.GollectionsIcons
import com.pokyx.gollections.ui.viewmodels.ItemFormState
import com.pokyx.gollections.ui.viewmodels.ItemViewModel
import com.pokyx.gollections.utils.AddTagDialog
import com.pokyx.gollections.utils.getDynamicStatusOptions
import com.pokyx.gollections.utils.getLocalizedPropertyLabel
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemFormBody(
    viewModel: ItemViewModel,
    buttonText: String,
    onSaveClick: (ItemFormState) -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.formState.collectAsStateWithLifecycle()
    val collectionsList by viewModel.collections.collectAsStateWithLifecycle()

    val finalSelectedId = state.selectedPath.lastOrNull()
    val finalSelectedName = collectionsList.find { it.id == finalSelectedId }?.name ?: ""
    val dbTags by viewModel.getTagsForCollections(state.selectedPath).collectAsStateWithLifecycle(initialValue = emptyList())

    var showAddTagDialog by remember { mutableStateOf(false) }
    var showPurchaseDatePicker by remember { mutableStateOf(false) }
    val purchaseDatePickerState = rememberDatePickerState()
    var showLoanDatePicker by remember { mutableStateOf(false) }
    val loanDatePickerState = rememberDatePickerState()

    var showSourceDialog by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf("") }
    var showDetourageConfirmation by remember { mutableStateOf(false) }
    var isProcessingImage by remember { mutableStateOf(false) }

    var tempPhotoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val pendingImageUri = pendingImageUriString?.let { Uri.parse(it) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { pendingImageUriString = uri.toString(); showDetourageConfirmation = true }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempPhotoUriString != null) { pendingImageUriString = tempPhotoUriString; showDetourageConfirmation = true }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- IMAGE HEADER ---
        Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).clickable(enabled = !isProcessingImage) { showSourceDialog = true }, contentAlignment = Alignment.Center) {
            if (isProcessingImage) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Spacer(modifier = Modifier.height(8.dp)); Text(stringResource(R.string.text_processing_cutout), fontSize = 12.sp) } }
            else if (state.imageUrl.isNotBlank()) AsyncImage(model = state.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            else { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("📸", fontSize = 40.sp); Spacer(modifier = Modifier.height(4.dp)); Text(stringResource(R.string.dialog_illustration_text), color = MaterialTheme.colorScheme.outline, fontSize = 13.sp) } }
        }

        // --- ITEM TYPE ---
        Text(stringResource(R.string.title_item_type), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ItemType.values().forEach { type -> FilterChip(selected = state.itemType == type, onClick = { viewModel.changeItemType(type) }, label = { Text("${type.emoji} ${type.label}") }) }
        }

        // --- TITLE ---
        OutlinedTextField(value = state.title, onValueChange = { text -> viewModel.updateForm { it.copy(title = text) } }, label = { Text(stringResource(R.string.label_title)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

        // --- DYNAMIC PROPERTIES ---
        if (state.properties.isNotEmpty()) {
            Text(stringResource(R.string.title_specific_info), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
            state.properties.forEach { (keyEnum, value) ->
                val isMultiLine = keyEnum.isMultiLine
                OutlinedTextField(
                    value = value, onValueChange = { newValue -> viewModel.updateProperty(keyEnum, newValue) },
                    label = { Text(getLocalizedPropertyLabel(keyEnum.value)) },
                    modifier = Modifier.fillMaxWidth().then(if (isMultiLine) Modifier.height(120.dp) else Modifier),
                    maxLines = if (isMultiLine) 5 else 1, singleLine = !isMultiLine, shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // --- LOCATION (PATH) ---
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
                                FilterChip(selected = isSelected, onClick = { viewModel.updateForm { it.copy(selectedPath = state.selectedPath.take(i) + opt.id) } }, label = { Text(opt.name) })
                            }
                        }
                    }
                    if (i < state.selectedPath.size) parentIdForNextLevel = state.selectedPath[i] else break
                }
            }
        }

        // --- TAGS ---
        if (state.selectedPath.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(text = stringResource(R.string.label_tags), fontWeight = FontWeight.Bold, fontSize = 16.sp); IconButton(onClick = { showAddTagDialog = true }) { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) } }
                if (dbTags.isEmpty()) { Text(stringResource(R.string.no_tags_available), fontSize = 12.sp, color = MaterialTheme.colorScheme.outline) } else {
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        dbTags.distinctBy { it.name }.forEach { tag -> FilterChip(selected = state.selectedTags.any { it.name == tag.name }, onClick = { val currentTags = state.selectedTags; val newTags = if (currentTags.any { it.name == tag.name }) currentTags.filter { it.name != tag.name }.toSet() else currentTags + tag; viewModel.updateForm { it.copy(selectedTags = newTags) } }, label = { Text(tag.name) }) }
                    }
                }
            }
        }

        // --- STATUS ---
        if (finalSelectedId != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.label_status), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                val statusOptions = getDynamicStatusOptions(context, finalSelectedName)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { statusOptions.forEach { opt -> FilterChip(selected = state.status == opt, onClick = { viewModel.updateForm { it.copy(status = opt) } }, label = { Text(opt) }) } }
            }
        }

        // --- LOAN MANAGEMENT ---
        Text(text = stringResource(R.string.title_loan_management), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.loan_switch_text), fontWeight = FontWeight.Medium); Switch(checked = state.isLoaned, onCheckedChange = { value -> viewModel.updateForm { it.copy(isLoaned = value) } }) }
                if (state.isLoaned) {
                    OutlinedTextField(value = state.loanTo, onValueChange = { text -> viewModel.updateForm { it.copy(loanTo = text) } }, label = { Text(stringResource(R.string.label_loan_to)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                    Box(modifier = Modifier.fillMaxWidth()) { OutlinedTextField(value = state.loanDate, onValueChange = {}, readOnly = true, label = { Text(stringResource(R.string.label_loan_date)) }, modifier = Modifier.fillMaxWidth()); Box(modifier = Modifier.matchParentSize().clickable { showLoanDatePicker = true }) }
                }
            }
        }

        // --- ACQUISITION INFO ---
        Text(text = stringResource(R.string.title_acquisition_info), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(value = state.price, onValueChange = { input -> if (input.all { it.isDigit() || it == '.' || it == ',' } && input.count { it == '.' || it == ',' } <= 1) viewModel.updateForm { it.copy(price = input) } }, label = { Text(stringResource(R.string.label_price)) }, modifier = Modifier.weight(1.5f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
            Box(modifier = Modifier.weight(1.5f)) { OutlinedTextField(value = state.purchaseDate, onValueChange = {}, readOnly = true, label = { Text(stringResource(R.string.label_purchase_date)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)); Box(modifier = Modifier.matchParentSize().clickable { showPurchaseDatePicker = true }) }
        }

        // --- SAVE BUTTON ---
        Spacer(modifier = Modifier.height(30.dp))
        Button(
            onClick = { onSaveClick(state) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = state.title.isNotBlank() && finalSelectedId != null
        ) { Text(buttonText, fontSize = 16.sp) }
    }

    // --- DIALOGS ---
    if (showPurchaseDatePicker) DatePickerDialog(onDismissRequest = { showPurchaseDatePicker = false }, confirmButton = { TextButton(onClick = { purchaseDatePickerState.selectedDateMillis?.let { millis -> val dateStr = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); viewModel.updateForm { it.copy(purchaseDate = dateStr) } }; showPurchaseDatePicker = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { showPurchaseDatePicker = false }) { Text(stringResource(R.string.cancel)) } }) { DatePicker(state = purchaseDatePickerState) }
    if (showLoanDatePicker) DatePickerDialog(onDismissRequest = { showLoanDatePicker = false }, confirmButton = { TextButton(onClick = { loanDatePickerState.selectedDateMillis?.let { millis -> val dateStr = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); viewModel.updateForm { it.copy(loanDate = dateStr) } }; showLoanDatePicker = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { showLoanDatePicker = false }) { Text(stringResource(R.string.cancel)) } }) { DatePicker(state = loanDatePickerState) }
    if (showSourceDialog) AlertDialog(onDismissRequest = { showSourceDialog = false }, title = { Text("Source de l'illustration", fontSize = 18.sp, fontWeight = FontWeight.Bold) }, text = { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showSourceDialog = false; showUrlDialog = true }.padding(8.dp)) { Icon(imageVector = GollectionsIcons.Planet, contentDescription = "URL", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary); Spacer(modifier = Modifier.height(4.dp)); Text("URL", fontSize = 12.sp, fontWeight = FontWeight.Medium) }; Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showSourceDialog = false; val tempFile = File.createTempFile("cam_", ".jpg", context.cacheDir); val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile); tempPhotoUriString = uri.toString(); cameraLauncher.launch(uri) }.padding(8.dp)) { Icon(imageVector = GollectionsIcons.Camera, contentDescription = "Appareil", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary); Spacer(modifier = Modifier.height(4.dp)); Text("Appareil", fontSize = 12.sp, fontWeight = FontWeight.Medium) }; Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showSourceDialog = false; galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }.padding(8.dp)) { Icon(imageVector = GollectionsIcons.RoundedGallery, contentDescription = "Galerie", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary); Spacer(modifier = Modifier.height(4.dp)); Text("Galerie", fontSize = 12.sp, fontWeight = FontWeight.Medium) } } }, confirmButton = { TextButton(onClick = { showSourceDialog = false }) { Text(stringResource(R.string.cancel)) } })
    if (showUrlDialog) AlertDialog(onDismissRequest = { showUrlDialog = false }, title = { Text("Lien de l'image (URL)", fontWeight = FontWeight.Bold) }, text = { OutlinedTextField(value = urlInput, onValueChange = { urlInput = it }, label = { Text("Coller l'URL de l'image ici") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }, confirmButton = { Button(onClick = { if (urlInput.isNotBlank()) viewModel.updateForm { it.copy(imageUrl = urlInput.trim()) }; showUrlDialog = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { showUrlDialog = false }) { Text(stringResource(R.string.cancel)) } })
    if (showDetourageConfirmation) AlertDialog(onDismissRequest = { showDetourageConfirmation = false }, title = { Text(stringResource(R.string.dialog_cutout_title)) }, text = { Text(stringResource(R.string.dialog_cutout_text)) }, confirmButton = { Button(onClick = { showDetourageConfirmation = false; pendingImageUri?.let { uri -> isProcessingImage = true; viewModel.processAndSaveImage(uri, true) { finalUrl -> isProcessingImage = false; if (finalUrl != null) viewModel.updateForm { it.copy(imageUrl = finalUrl) } else Toast.makeText(context, R.string.toast_cutout_error, Toast.LENGTH_SHORT).show() } } }) { Text(stringResource(R.string.btn_yes)) } }, dismissButton = { TextButton(onClick = { showDetourageConfirmation = false; pendingImageUri?.let { uri -> isProcessingImage = true; viewModel.processAndSaveImage(uri, false) { finalUrl -> isProcessingImage = false; if (finalUrl != null) viewModel.updateForm { it.copy(imageUrl = finalUrl) } } } }) { Text(stringResource(R.string.btn_no)) } })
    if (showAddTagDialog && finalSelectedId != null) AddTagDialog(onDismiss = { showAddTagDialog = false }, onConfirm = { tagName -> viewModel.insertTag(tagName, finalSelectedId); showAddTagDialog = false })
}