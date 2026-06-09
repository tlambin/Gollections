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
import androidx.compose.material.icons.filled.Delete
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val scope = rememberCoroutineScope()

    val state by viewModel.formState.collectAsStateWithLifecycle()
    val collectionsList by viewModel.collections.collectAsStateWithLifecycle()

    val finalSelectedId = state.selectedPath.lastOrNull()
    val finalSelectedName = collectionsList.find { it.id == finalSelectedId }?.name ?: ""

    val dbTags by remember(state.selectedPath) { viewModel.getTagsForCollections(state.selectedPath) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val uniqueTags = remember(dbTags) { dbTags.distinctBy { it.name } }

    var showAddTagDialog by remember { mutableStateOf(false) }
    var showPurchaseDatePicker by remember { mutableStateOf(false) }
    val purchaseDatePickerState = rememberDatePickerState()
    var showLoanDatePicker by remember { mutableStateOf(false) }
    val loanDatePickerState = rememberDatePickerState()

    var showSourceDialog by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf("") }
    var isProcessingImage by remember { mutableStateOf(false) }

    var tempPhotoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var loadedBitmapToCrop by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // --- GESTION DES CHIPS DYNAMIQUES DU DIALOGUE ---
    var showAddPropertyDialog by remember { mutableStateOf(false) }
    var newPropertyName by remember { mutableStateOf("") }
    var selectedDataType by remember { mutableStateOf("TEXT") } // TEXT, NUMBER, VALUE, DATE, FILE
    val dataTypesList = listOf("Texte" to "TEXT", "Chiffre" to "NUMBER", "Valeur" to "VALUE", "Calendrier" to "DATE", "Fichier" to "FILE")

    // --- SÉLECTEURS POUR LES PROPRIÉTÉS DYNAMIQUES DE TYPE DATE & FILE ---
    var targetPropertyForDatePicker by remember { mutableStateOf<String?>(null) }
    var showCustomPropertyDatePicker by remember { mutableStateOf(false) }
    val customPropertyDatePickerState = rememberDatePickerState()
    var targetPropertyForFilePicker by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) { pendingImageUriString = uri.toString() } }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success -> if (success && tempPhotoUriString != null) { pendingImageUriString = tempPhotoUriString } }

    // Lanceur universel pour n'importe quel fichier requis par le template
    val customFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && targetPropertyForFilePicker != null) {
            viewModel.updateProperty(targetPropertyForFilePicker!!, uri.toString())
            targetPropertyForFilePicker = null
        }
    }

    LaunchedEffect(pendingImageUriString) {
        pendingImageUriString?.let { uriStr ->
            isProcessingImage = true
            val bitmap = viewModel.loadBitmap(Uri.parse(uriStr))
            if (bitmap != null) { loadedBitmapToCrop = bitmap }
            else { Toast.makeText(context, "Erreur de chargement", Toast.LENGTH_SHORT).show() }
            isProcessingImage = false
            pendingImageUriString = null
        }
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
            ItemType.entries.forEach { type -> FilterChip(selected = state.itemType == type, onClick = { viewModel.changeItemType(type) }, label = { Text("${type.emoji} ${type.label}") }) }
        }

        // --- TITLE ---
        OutlinedTextField(value = state.title, onValueChange = { text -> viewModel.updateForm { it.copy(title = text) } }, label = { Text(stringResource(R.string.label_title)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

        // --- DYNAMIC PROPERTIES ---
        Text(stringResource(R.string.title_specific_info), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))

        state.properties.forEach { (propertyName, value) ->
            val propDataType = state.propertyTypes[propertyName] ?: "TEXT"
            val lowerName = propertyName.lowercase()
            val isMultiLine = propDataType == "TEXT" && (lowerName.contains("description") || lowerName.contains("synopsis") || lowerName.contains("résumé"))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = if (propDataType == "FILE" && value.startsWith("content")) "Fichier sélectionné 📎" else value,
                        onValueChange = { newValue -> viewModel.updateProperty(propertyName, newValue) },
                        label = { Text(propertyName + if (propDataType == "VALUE") " (€)" else "") },
                        modifier = Modifier.fillMaxWidth().then(if (isMultiLine) Modifier.height(120.dp) else Modifier),
                        maxLines = if (isMultiLine) 5 else 1,
                        singleLine = !isMultiLine,
                        readOnly = (propDataType == "DATE" || propDataType == "FILE"),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (propDataType == "NUMBER" || propDataType == "VALUE") KeyboardType.Number else KeyboardType.Text
                        ),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = { viewModel.deleteCustomPropertyTemplate(finalSelectedId ?: 0L, propertyName) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                            }
                        }
                    )

                    // Overlays d'interceptions pour les types non textuels
                    if (propDataType == "DATE") {
                        Box(modifier = Modifier.matchParentSize().padding(end = 48.dp).clickable { targetPropertyForDatePicker = propertyName; showCustomPropertyDatePicker = true })
                    } else if (propDataType == "FILE") {
                        Box(modifier = Modifier.matchParentSize().padding(end = 48.dp).clickable { targetPropertyForFilePicker = propertyName; customFileLauncher.launch("*/*") })
                    }
                }
            }
        }

        if (finalSelectedId != null) {
            TextButton(onClick = { showAddPropertyDialog = true }, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ajouter un champ personnalisé")
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
                                FilterChip(selected = isSelected, onClick = { viewModel.updateSelectedPath(state.selectedPath.take(i) + opt.id) }, label = { Text(opt.name) })
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
                if (uniqueTags.isEmpty()) {
                    Text(stringResource(R.string.no_tags_available), fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                } else {
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        uniqueTags.forEach { tag ->
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

        Spacer(modifier = Modifier.height(30.dp))
        Button(onClick = { onSaveClick(state) }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(buttonText, fontSize = 16.sp) }
    }

    // --- DIALOGUES DE DATE ET SÉLECTION ---

    if (showCustomPropertyDatePicker && targetPropertyForDatePicker != null) {
        DatePickerDialog(
            onDismissRequest = { showCustomPropertyDatePicker = false; targetPropertyForDatePicker = null },
            confirmButton = {
                TextButton(onClick = {
                    customPropertyDatePickerState.selectedDateMillis?.let { millis ->
                        val dateStr = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        viewModel.updateProperty(targetPropertyForDatePicker!!, dateStr)
                    }
                    showCustomPropertyDatePicker = false
                    targetPropertyForDatePicker = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showCustomPropertyDatePicker = false; targetPropertyForDatePicker = null }) { Text(stringResource(R.string.cancel)) } }
        ) { DatePicker(state = customPropertyDatePickerState) }
    }

    if (showAddPropertyDialog && finalSelectedId != null) {
        AlertDialog(
            onDismissRequest = { showAddPropertyDialog = false },
            title = { Text("Nouveau champ personnalisé", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = newPropertyName, onValueChange = { newPropertyName = it }, label = { Text("Nom du champ") }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    Text("Type de données :", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        dataTypesList.forEach { (label, typeKey) ->
                            FilterChip(selected = selectedDataType == typeKey, onClick = { selectedDataType = typeKey }, label = { Text(label) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newPropertyName.isNotBlank()) {
                        viewModel.addCustomPropertyTemplate(finalSelectedId, newPropertyName.trim(), selectedDataType)
                        showAddPropertyDialog = false
                        newPropertyName = ""
                        selectedDataType = "TEXT"
                    }
                }) { Text("Ajouter") }
            },
            dismissButton = { TextButton(onClick = { showAddPropertyDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (showPurchaseDatePicker) {
        DatePickerDialog(onDismissRequest = { showPurchaseDatePicker = false }, confirmButton = { TextButton(onClick = { purchaseDatePickerState.selectedDateMillis?.let { millis -> val dateStr = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); viewModel.updateForm { it.copy(purchaseDate = dateStr) } }; showPurchaseDatePicker = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { showPurchaseDatePicker = false }) { Text(stringResource(R.string.cancel)) } }) { DatePicker(state = purchaseDatePickerState) }
    }
    if (showLoanDatePicker) {
        DatePickerDialog(onDismissRequest = { showLoanDatePicker = false }, confirmButton = { TextButton(onClick = { loanDatePickerState.selectedDateMillis?.let { millis -> val dateStr = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); viewModel.updateForm { it.copy(loanDate = dateStr) } }; showLoanDatePicker = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { showLoanDatePicker = false }) { Text(stringResource(R.string.cancel)) } }) { DatePicker(state = loanDatePickerState) }
    }
    if (showSourceDialog) { AlertDialog(onDismissRequest = { showSourceDialog = false }, title = { Text("Source de l'illustration", fontSize = 18.sp, fontWeight = FontWeight.Bold) }, text = { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showSourceDialog = false; showUrlDialog = true }.padding(8.dp)) { Icon(imageVector = GollectionsIcons.Planet, contentDescription = "URL", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary); Spacer(modifier = Modifier.height(4.dp)); Text("URL", fontSize = 12.sp, fontWeight = FontWeight.Medium) }; Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showSourceDialog = false; scope.launch(Dispatchers.IO) { val tempFile = File.createTempFile("cam_", ".jpg", context.cacheDir); val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile); withContext(Dispatchers.Main) { tempPhotoUriString = uri.toString(); cameraLauncher.launch(uri) } } }.padding(8.dp)) { Icon(imageVector = GollectionsIcons.Camera, contentDescription = "Appareil", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary); Spacer(modifier = Modifier.height(4.dp)); Text("Appareil", fontSize = 12.sp, fontWeight = FontWeight.Medium) }; Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showSourceDialog = false; galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }.padding(8.dp)) { Icon(imageVector = GollectionsIcons.RoundedGallery, contentDescription = "Galerie", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary); Spacer(modifier = Modifier.height(4.dp)); Text("Galerie", fontSize = 12.sp, fontWeight = FontWeight.Medium) } } }, confirmButton = { TextButton(onClick = { showSourceDialog = false }) { Text(stringResource(R.string.cancel)) } }) }
    if (showUrlDialog) { AlertDialog(onDismissRequest = { showUrlDialog = false }, title = { Text("Lien de l'image (URL)", fontWeight = FontWeight.Bold) }, text = { OutlinedTextField(value = urlInput, onValueChange = { urlInput = it }, label = { Text("Coller l'URL de l'image ici") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }, confirmButton = { Button(onClick = { if (urlInput.isNotBlank()) viewModel.updateForm { it.copy(imageUrl = urlInput.trim()) }; showUrlDialog = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { showUrlDialog = false }) { Text(stringResource(R.string.cancel)) } }) }
    if (loadedBitmapToCrop != null) { CropImageDialog(bitmap = loadedBitmapToCrop!!, overlayShape = CropOverlayShape.ROUNDED_SQUARE, onDismiss = { loadedBitmapToCrop = null }, onConfirm = { croppedBitmap, smartCutout -> isProcessingImage = true; loadedBitmapToCrop = null; viewModel.processAndSaveBitmap(croppedBitmap, smartCutout) { finalUrl -> isProcessingImage = false; if (finalUrl != null) { viewModel.updateForm { it.copy(imageUrl = finalUrl) } } else { Toast.makeText(context, "Erreur de traitement", Toast.LENGTH_SHORT).show() } } }) }
    if (showAddTagDialog && finalSelectedId != null) { AddTagDialog(onDismiss = { showAddTagDialog = false }, onConfirm = { tagName -> viewModel.insertTag(tagName, finalSelectedId); showAddTagDialog = false }) }
}