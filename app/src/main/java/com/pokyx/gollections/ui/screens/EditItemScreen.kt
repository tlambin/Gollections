package com.pokyx.gollections.ui.screens

import android.net.Uri
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.pokyx.gollections.data.CollectionItem
import com.pokyx.gollections.ui.viewmodels.CollectionViewModel
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemScreen(
    itemId: Int,
    onBackClick: () -> Unit,
    onSaveClick: (CollectionItem) -> Unit,
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val itemToEdit by viewModel.getItemById(itemId).collectAsState(initial = null)
    var isInitialized by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    val todayDate = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }
    var purchaseDate by remember { mutableStateOf(todayDate) }
    var price by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var isLoaned by remember { mutableStateOf(false) }
    var loanTo by remember { mutableStateOf("") }
    var loanDate by remember { mutableStateOf(todayDate) }
    var imageUrl by remember { mutableStateOf("") }

    val collectionsList by viewModel.collections.collectAsState()
    var selectedPath by remember { mutableStateOf(listOf<Long>()) }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(itemToEdit, collectionsList) {
        if (itemToEdit != null && collectionsList.isNotEmpty() && !isInitialized) {
            val item = itemToEdit!!
            title = item.title; purchaseDate = item.purchaseDate.ifBlank { todayDate }; price = item.price
            status = item.status; isLoaned = item.isLoaned; loanTo = item.loanTo; loanDate = item.loanDate.ifBlank { todayDate }
            imageUrl = item.imageUrl

            val path = mutableListOf<Long>()
            var curr: Long? = item.collectionId
            while(curr != null) { path.add(0, curr); curr = collectionsList.find { it.id == curr }?.parentId }
            selectedPath = path
            selectedTags = item.tags.split(",").filter { it.isNotBlank() }.toSet()
            isInitialized = true
        }
    }

    val finalSelectedId = selectedPath.lastOrNull()
    val finalSelectedName = collectionsList.find { it.id == finalSelectedId }?.name ?: ""
    val dbTags by viewModel.getTagsForCollections(selectedPath).collectAsState(initial = emptyList())
    val availableTags = dbTags.map { it.name }.distinct()

    LaunchedEffect(finalSelectedId) {
        if (isInitialized) {
            val options = getDynamicStatusOptions(finalSelectedName)
            if (status !in options) status = options.first()
        }
    }

    var showPurchaseDatePicker by remember { mutableStateOf(false) }
    val purchaseDatePickerState = rememberDatePickerState()
    var showLoanDatePicker by remember { mutableStateOf(false) }
    val loanDatePickerState = rememberDatePickerState()

    var showSourceDialog by remember { mutableStateOf(false) }
    var showDetourageConfirmation by remember { mutableStateOf(false) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) { pendingImageUri = uri; showDetourageConfirmation = true } }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success -> if (success && tempPhotoUri != null) { pendingImageUri = tempPhotoUri; showDetourageConfirmation = true } }

    if (!isInitialized) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }

    Scaffold(
        topBar = { TopAppBar(title = { Text(title.ifBlank { "Éditer l'objet" }, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.Close, contentDescription = "Fermer") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).clickable(enabled = !isProcessingImage) { showSourceDialog = true }, contentAlignment = Alignment.Center) {
                if (isProcessingImage) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Spacer(modifier = Modifier.height(8.dp)); Text("Détourage...", fontSize = 12.sp) } }
                else if (imageUrl.isNotBlank()) AsyncImage(model = imageUrl, contentDescription = "Illustration", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                else { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("📸", fontSize = 40.sp); Spacer(modifier = Modifier.height(4.dp)); Text("Modifier la photo", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp) } }
            }
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titre") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

            // --- SÉLECTEUR DE DOSSIERS COMPACT ---
            if (collectionsList.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Emplacement", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))

                    var parentIdForNextLevel: Long? = null
                    for (i in 0..selectedPath.size) {
                        val options = collectionsList.filter { it.parentId == parentIdForNextLevel }
                        if (options.isNotEmpty()) {
                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                options.forEach { opt ->
                                    val isSelected = (i < selectedPath.size && selectedPath[i] == opt.id)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedPath = selectedPath.take(i) + opt.id },
                                        label = { Text(opt.name) }
                                    )
                                }
                            }
                        }
                        if (i < selectedPath.size) parentIdForNextLevel = selectedPath[i] else break
                    }
                }
            }

            // --- ÉTIQUETTES ---
            if (availableTags.isNotEmpty() || selectedPath.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Étiquettes", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                    if (availableTags.isEmpty()) {
                        Text("Aucune étiquette.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    } else {
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            availableTags.forEach { tag ->
                                FilterChip(
                                    selected = selectedTags.contains(tag),
                                    onClick = { if (selectedTags.contains(tag)) selectedTags -= tag else selectedTags += tag },
                                    label = { Text(tag) }
                                )
                            }
                        }
                    }
                }
            }

            if (finalSelectedId != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Avancement", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                    val statusOptions = getDynamicStatusOptions(finalSelectedName)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        statusOptions.forEach { opt -> FilterChip(selected = status == opt, onClick = { status = opt }, label = { Text(opt) }) }
                    }
                }
            }

            Text(text = "Gestion du Prêt", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Objet prêté ?", fontWeight = FontWeight.Medium); Switch(checked = isLoaned, onCheckedChange = { isLoaned = it }) }
                    if (isLoaned) {
                        OutlinedTextField(value = loanTo, onValueChange = { loanTo = it }, label = { Text("Prêté à (Nom)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(value = loanDate, onValueChange = {}, readOnly = true, label = { Text("Date du prêt") }, modifier = Modifier.fillMaxWidth())
                            Box(modifier = Modifier.matchParentSize().clickable { showLoanDatePicker = true })
                        }
                    }
                }
            }

            Text(text = "Informations", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = price, onValueChange = { input -> if (input.all { it.isDigit() || it == '.' || it == ',' } && input.count { it == '.' || it == ',' } <= 1) price = input }, label = { Text("Prix (€)") }, modifier = Modifier.weight(1.5f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
                Box(modifier = Modifier.weight(1.5f)) {
                    OutlinedTextField(value = purchaseDate, onValueChange = {}, readOnly = true, label = { Text("Date d'achat") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Box(modifier = Modifier.matchParentSize().clickable { showPurchaseDatePicker = true })
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
            Button(onClick = { if (title.isNotBlank() && finalSelectedId != null) { onSaveClick(itemToEdit!!.copy(title = title, collectionId = finalSelectedId, tags = selectedTags.joinToString(","), purchaseDate = purchaseDate.trim(), price = price.trim(), imageUrl = imageUrl, status = status, isLoaned = isLoaned, loanTo = if (isLoaned) loanTo.trim() else "", loanDate = if (isLoaned) loanDate else "")) } }, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = title.isNotBlank() && finalSelectedId != null) { Text("Mettre à jour", fontSize = 16.sp) }
        }

        if (showPurchaseDatePicker) DatePickerDialog(onDismissRequest = { showPurchaseDatePicker = false }, confirmButton = { TextButton(onClick = { purchaseDatePickerState.selectedDateMillis?.let { millis -> purchaseDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }; showPurchaseDatePicker = false }) { Text("OK") } }) { DatePicker(state = purchaseDatePickerState) }
        if (showLoanDatePicker) DatePickerDialog(onDismissRequest = { showLoanDatePicker = false }, confirmButton = { TextButton(onClick = { loanDatePickerState.selectedDateMillis?.let { millis -> loanDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }; showLoanDatePicker = false }) { Text("OK") } }) { DatePicker(state = loanDatePickerState) }
        if (showSourceDialog) AlertDialog(onDismissRequest = { showSourceDialog = false }, title = { Text("Modifier") }, text = { Text("Source ?") }, confirmButton = { Button(onClick = { showSourceDialog = false; val tempFile = File.createTempFile("cam_", ".jpg", context.cacheDir); tempPhotoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile); cameraLauncher.launch(tempPhotoUri!!) }) { Text("📷 Photo") } }, dismissButton = { Button(onClick = { showSourceDialog = false; galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("📁 Galerie") } })
        if (showDetourageConfirmation) AlertDialog(onDismissRequest = { showDetourageConfirmation = false }, title = { Text("Détourage") }, text = { Text("Détourer ?") }, confirmButton = { Button(onClick = { showDetourageConfirmation = false; pendingImageUri?.let { uri -> isProcessingImage = true; runSubjectSegmentation(context, uri, true) { finalUri -> isProcessingImage = false; if (finalUri != null) imageUrl = finalUri.toString() } } }) { Text("Oui") } }, dismissButton = { TextButton(onClick = { showDetourageConfirmation = false; pendingImageUri?.let { uri -> isProcessingImage = true; runSubjectSegmentation(context, uri, false) { finalUri -> isProcessingImage = false; if (finalUri != null) imageUrl = finalUri.toString() } } }) { Text("Non") } })
    }
}