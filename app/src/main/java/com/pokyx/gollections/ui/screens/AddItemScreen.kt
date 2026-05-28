package com.pokyx.gollections.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.pokyx.gollections.data.CollectionItem
import com.pokyx.gollections.ui.viewmodels.CollectionViewModel
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    preSelectedCollection: String? = null,
    onBackClick: () -> Unit,
    onSaveClick: (CollectionItem) -> Unit, // Signature simplifiée
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }

    val todayDate = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }
    var purchaseDate by remember { mutableStateOf(todayDate) }
    var price by remember { mutableStateOf("") }

    var showPurchaseDatePicker by remember { mutableStateOf(false) }
    val purchaseDatePickerState = rememberDatePickerState()

    // --- Nouveaux états pour le statut et le prêt ---
    var status by remember { mutableStateOf("") }
    var isLoaned by remember { mutableStateOf(false) }
    var loanTo by remember { mutableStateOf("") }
    var loanDate by remember { mutableStateOf(todayDate) }
    var showLoanDatePicker by remember { mutableStateOf(false) }
    val loanDatePickerState = rememberDatePickerState()

    val collectionsList by viewModel.collections.collectAsState()
    val collections = collectionsList.map { it.name }
    var selectedCollection by remember(preSelectedCollection) { mutableStateOf(preSelectedCollection ?: "") }
    var selectedCategory by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    // Adaptation dynamique du statut quand on change de collection
    LaunchedEffect(selectedCollection) {
        selectedCategory = ""
        val options = getDynamicStatusOptions(selectedCollection)
        if (status !in options) status = options.first()
    }

    val dbCategories by viewModel.getCategoriesForCollection(selectedCollection).collectAsState(initial = emptyList())
    val categories = dbCategories.map { it.name }

    var showSourceDialog by remember { mutableStateOf(false) }
    var showDetourageConfirmation by remember { mutableStateOf(false) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) { pendingImageUri = uri; showDetourageConfirmation = true } }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success -> if (success && tempPhotoUri != null) { pendingImageUri = tempPhotoUri; showDetourageConfirmation = true } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title.ifBlank { "Nouvel Objet" }, fontWeight = FontWeight.Bold) }, // Ou "Éditer l'objet" pour EditItemScreen
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {

            // Photo
            Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).clickable(enabled = !isProcessingImage) { showSourceDialog = true }, contentAlignment = Alignment.Center) {
                if (isProcessingImage) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Spacer(modifier = Modifier.height(8.dp)); Text("Détourage...", fontSize = 12.sp) } }
                else if (imageUrl.isNotBlank()) AsyncImage(model = imageUrl, contentDescription = "Illustration", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                else { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("📸", fontSize = 40.sp); Spacer(modifier = Modifier.height(4.dp)); Text("Ajouter une photo", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp) } }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp) // <-- LA TOUCHE M3
            )

            // Collections et Catégories
            if (preSelectedCollection == null && collections.isNotEmpty()) { Text(text = "Collection", fontWeight = FontWeight.Bold, fontSize = 16.sp); Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { collections.forEach { collection -> FilterChip(selected = selectedCollection == collection, onClick = { selectedCollection = collection }, label = { Text(collection) }) } } }
            if (categories.isNotEmpty()) { Text(text = "Catégorie (Format)", fontWeight = FontWeight.Bold, fontSize = 16.sp); Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { categories.forEach { cat -> FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat) }) } } }

            // Statut dynamique
            if (selectedCollection.isNotBlank()) {
                Text(text = "Avancement", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                val statusOptions = getDynamicStatusOptions(selectedCollection)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    statusOptions.forEach { opt -> FilterChip(selected = status == opt, onClick = { status = opt }, label = { Text(opt) }) }
                }
            }

            // Prêt
            Text(text = "Gestion du Prêt", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Objet prêté à quelqu'un ?", fontWeight = FontWeight.Medium)
                        Switch(checked = isLoaned, onCheckedChange = { isLoaned = it })
                    }
                    if (isLoaned) {
                        OutlinedTextField(
                            value = loanTo,
                            onValueChange = { loanTo = it },
                            label = { Text("Prêté à (Nom)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp) // <-- LA TOUCHE M3
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(value = loanDate, onValueChange = {}, readOnly = true, label = { Text("Date du prêt") }, modifier = Modifier.fillMaxWidth())
                            Box(modifier = Modifier.matchParentSize().clickable { showLoanDatePicker = true })
                        }
                    }
                }
            }

            // Achat
            Text(text = "Informations d'acquisition", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = price, onValueChange = { input -> if (input.all { it.isDigit() || it == '.' || it == ',' } && input.count { it == '.' || it == ',' } <= 1) price = input }, label = { Text("Prix (€)") },
                    modifier = Modifier.weight(1.5f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp), // <-- LA TOUCHE M3
                    trailingIcon = { /* ton code avec les flèches */ }
                )
                Box(modifier = Modifier.weight(1.5f)) {
                    OutlinedTextField(
                        value = purchaseDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date d'achat") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp) // <-- LA TOUCHE M3
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { showPurchaseDatePicker = true })
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val newItem = CollectionItem(
                            title = title, collection = selectedCollection, category = selectedCategory,
                            purchaseDate = purchaseDate.trim(), price = price.trim(), imageUrl = imageUrl,
                            status = status, isLoaned = isLoaned, loanTo = if (isLoaned) loanTo.trim() else "", loanDate = if (isLoaned) loanDate else ""
                        )
                        onSaveClick(newItem)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp), enabled = title.isNotBlank() && selectedCollection.isNotBlank()
            ) { Text("Enregistrer dans ma collection", fontSize = 16.sp) }
        }

        // Dialogues DatePicker
        if (showPurchaseDatePicker) DatePickerDialog(onDismissRequest = { showPurchaseDatePicker = false }, confirmButton = { TextButton(onClick = { purchaseDatePickerState.selectedDateMillis?.let { millis -> purchaseDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }; showPurchaseDatePicker = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { showPurchaseDatePicker = false }) { Text("Annuler") } }) { DatePicker(state = purchaseDatePickerState) }
        if (showLoanDatePicker) DatePickerDialog(onDismissRequest = { showLoanDatePicker = false }, confirmButton = { TextButton(onClick = { loanDatePickerState.selectedDateMillis?.let { millis -> loanDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }; showLoanDatePicker = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { showLoanDatePicker = false }) { Text("Annuler") } }) { DatePicker(state = loanDatePickerState) }

        // Dialogues Photo
        if (showSourceDialog) AlertDialog(onDismissRequest = { showSourceDialog = false }, title = { Text("Illustration") }, text = { Text("Choisissez la source") }, confirmButton = { Button(onClick = { showSourceDialog = false; val tempFile = File.createTempFile("cam_", ".jpg", context.cacheDir); tempPhotoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile); cameraLauncher.launch(tempPhotoUri!!) }) { Text("📷 Appareil Photo") } }, dismissButton = { Button(onClick = { showSourceDialog = false; galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("📁 Galerie") } })
        if (showDetourageConfirmation) AlertDialog(onDismissRequest = { showDetourageConfirmation = false }, title = { Text("Détourage intelligent ✨") }, text = { Text("Détourer l'objet ?") }, confirmButton = { Button(onClick = { showDetourageConfirmation = false; pendingImageUri?.let { uri -> isProcessingImage = true; runSubjectSegmentation(context, uri, true) { finalUri -> isProcessingImage = false; if (finalUri != null) imageUrl = finalUri.toString() } } }) { Text("Oui") } }, dismissButton = { TextButton(onClick = { showDetourageConfirmation = false; pendingImageUri?.let { uri -> isProcessingImage = true; runSubjectSegmentation(context, uri, false) { finalUri -> isProcessingImage = false; if (finalUri != null) imageUrl = finalUri.toString() } } }) { Text("Non") } })
    }
}

// Outil intelligent pour les statuts
fun getDynamicStatusOptions(collectionName: String): List<String> {
    return when (collectionName.lowercase().trim()) {
        "blu-ray", "films", "cinéma", "cinema" -> listOf("À voir", "En cours", "Vu")
        "livres", "mangas", "bd", "romans" -> listOf("À lire", "En cours", "Lu")
        "jeux vidéo", "jeux", "jeux video" -> listOf("À faire", "En cours", "Terminé", "Platiné", "Abandonné")
        "vinyles", "musique", "cd", "disques" -> listOf("À écouter", "Écouté")
        else -> listOf("Nouveau", "En cours", "Terminé")
    }
}

// ML KIT
fun runSubjectSegmentation(context: Context, sourceUri: Uri, shouldCutout: Boolean, onComplete: (Uri?) -> Unit) {
    try {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, sourceUri)) { decoder, _, _ -> decoder.isMutableRequired = true } } else { @Suppress("DEPRECATION") MediaStore.Images.Media.getBitmap(context.contentResolver, sourceUri) }
        if (!shouldCutout) { onComplete(saveBitmapToInternalStorage(context, bitmap)); return }
        val options = SubjectSegmenterOptions.Builder().enableForegroundBitmap().build(); val segmenter = SubjectSegmentation.getClient(options)
        segmenter.process(InputImage.fromBitmap(bitmap, 0)).addOnSuccessListener { result -> val cutoutBitmap = result.foregroundBitmap; if (cutoutBitmap != null) onComplete(saveBitmapToInternalStorage(context, cutoutBitmap)) else { onComplete(saveBitmapToInternalStorage(context, bitmap)); Toast.makeText(context, "Sujet non détecté.", Toast.LENGTH_SHORT).show() } }.addOnFailureListener { onComplete(saveBitmapToInternalStorage(context, bitmap)) }
    } catch (e: Exception) { e.printStackTrace(); onComplete(null) }
}
fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): Uri { val file = File(context.filesDir, "gollections_img_${System.currentTimeMillis()}.png"); FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }; return Uri.fromFile(file) }