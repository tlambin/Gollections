package com.pokyx.gollections.ui

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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddObjectScreen(
    preSelectedCategory: String? = null,
    onBackClick: () -> Unit,
    onSaveClick: (title: String, category: String, subCategory: String, purchaseDate: String, price: String, imageUrl: String) -> Unit,
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }

    val todayDate = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }
    var purchaseDate by remember { mutableStateOf(todayDate) }
    var price by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val categoriesList by viewModel.allCategories.collectAsState(initial = emptyList())
    val categories = categoriesList.map { it.name }

    var selectedCategory by remember(preSelectedCategory) { mutableStateOf(preSelectedCategory ?: "") }
    var selectedSubCategory by remember { mutableStateOf("") }

    var imageUrl by remember { mutableStateOf("") }
    var showSourceDialog by remember { mutableStateOf(false) }
    var showDetourageConfirmation by remember { mutableStateOf(false) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(categories) {
        if (selectedCategory.isEmpty() && categories.isNotEmpty()) {
            selectedCategory = categories.first()
        }
    }

    val dbSubCategories by viewModel.getSubCategoriesByCategory(selectedCategory).collectAsState(initial = emptyList())
    val subCategories = dbSubCategories.map { it.name }

    LaunchedEffect(selectedCategory) {
        selectedSubCategory = ""
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                pendingImageUri = uri
                showDetourageConfirmation = true
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && tempPhotoUri != null) {
                pendingImageUri = tempPhotoUri
                showDetourageConfirmation = true
            }
        }
    )

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✖",
                    modifier = Modifier.clickable { onBackClick() }.padding(8.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "Nouvel Objet", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ZONE PHOTO
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .clickable(enabled = !isProcessingImage) { showSourceDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (isProcessingImage) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Détorage intelligent...", fontSize = 12.sp)
                    }
                } else if (imageUrl.isNotBlank()) {
                    AsyncImage(model = imageUrl, contentDescription = "Illustration", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📸", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Ajouter une photo (Optionnel)", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                    }
                }
            }

            // CHAMP TITRE
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre") },
                placeholder = { Text("Ex: Inception, Elden Ring...") },
                modifier = Modifier.fillMaxWidth()
            )

            // SECTION ACQUISITION
            Text(text = "Informations d'acquisition", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                // Champ Prix
                OutlinedTextField(
                    value = price,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' || it == ',' } && input.count { it == '.' || it == ',' } <= 1) {
                            price = input
                        }
                    },
                    label = { Text("Prix (€)") },
                    modifier = Modifier.weight(1.5f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Augmenter de 1€",
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable {
                                        val currentPrice = price.replace(',', '.').toDoubleOrNull() ?: 0.0
                                        val nextPrice = currentPrice + 1.0
                                        price = if (nextPrice % 1.0 == 0.0) nextPrice.toInt().toString() else nextPrice.toString()
                                    }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Diminuer de 1€",
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable {
                                        val currentPrice = price.replace(',', '.').toDoubleOrNull() ?: 0.0
                                        if (currentPrice >= 1.0) {
                                            val nextPrice = currentPrice - 1.0
                                            price = if (nextPrice % 1.0 == 0.0) nextPrice.toInt().toString() else nextPrice.toString()
                                        } else {
                                            price = ""
                                        }
                                    }
                            )
                        }
                    }
                )

                // Champ Date
                Box(modifier = Modifier.weight(1.5f)) {
                    OutlinedTextField(
                        value = purchaseDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date d'achat") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                }
            }

            // CORRECTION : "Catégorie" renommé en "Collection"
            if (preSelectedCategory == null && categories.isNotEmpty()) {
                Text(text = "Collection", fontWeight = FontWeight.Bold, fontSize = 16.sp) // <-- Mis à jour ici
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { category ->
                        FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category }, label = { Text(category) })
                    }
                }
            }

            // CORRECTION : "Format / Plateforme" renommé en "Catégorie"
            if (subCategories.isNotEmpty()) {
                Text(text = "Catégorie", fontWeight = FontWeight.Bold, fontSize = 16.sp) // <-- Mis à jour ici
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    subCategories.forEach { sub ->
                        FilterChip(selected = selectedSubCategory == sub, onClick = { selectedSubCategory = sub }, label = { Text(sub) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = { if (title.isNotBlank()) onSaveClick(title, selectedCategory, selectedSubCategory, purchaseDate.trim(), price.trim(), imageUrl) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = title.isNotBlank() && selectedCategory.isNotBlank()
            ) {
                Text(text = "Enregistrer dans ma collection", fontSize = 16.sp)
            }
        }

        // CALENDRIER
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            purchaseDate = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Annuler") } }
            ) { DatePicker(state = datePickerState) }
        }

        // SOURCES PHOTOS
        if (showSourceDialog) {
            AlertDialog(
                onDismissRequest = { showSourceDialog = false },
                title = { Text("Ajouter une illustration") },
                text = { Text("Choisissez comment vous souhaitez ajouter la photo de votre objet.") },
                confirmButton = {
                    Button(onClick = {
                        showSourceDialog = false
                        val tempFile = File.createTempFile("cam_", ".jpg", context.cacheDir)
                        tempPhotoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                        cameraLauncher.launch(tempPhotoUri!!)
                    }) { Text("📷 Appareil Photo") }
                },
                dismissButton = {
                    Button(onClick = {
                        showSourceDialog = false
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("📁 Galerie") }
                }
            )
        }

        if (showDetourageConfirmation) {
            AlertDialog(
                onDismissRequest = { showDetourageConfirmation = false },
                title = { Text("Détourage intelligent ✨") },
                text = { Text("Souhaitez-vous détourer automatiquement le fond de l'image pour n'isoler que votre objet ?") },
                confirmButton = {
                    Button(onClick = {
                        showDetourageConfirmation = false
                        val uri = pendingImageUri
                        if (uri != null) {
                            isProcessingImage = true
                            runSubjectSegmentation(context, uri, true) { finalUri ->
                                isProcessingImage = false
                                if (finalUri != null) imageUrl = finalUri.toString()
                            }
                        }
                    }) { Text("Oui, détourer") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDetourageConfirmation = false
                        val uri = pendingImageUri
                        if (uri != null) {
                            isProcessingImage = true
                            runSubjectSegmentation(context, uri, false) { finalUri ->
                                isProcessingImage = false
                                if (finalUri != null) imageUrl = finalUri.toString()
                            }
                        }
                    }) { Text("Non, garder entière") }
                }
            )
        }
    }
}

private fun runSubjectSegmentation(context: Context, sourceUri: Uri, shouldCutout: Boolean, onComplete: (Uri?) -> Unit) {
    try {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, sourceUri)) { decoder, _, _ -> decoder.isMutableRequired = true }
        } else {
            @Suppress("DEPRECATION") MediaStore.Images.Media.getBitmap(context.contentResolver, sourceUri)
        }
        if (!shouldCutout) {
            onComplete(saveBitmapToInternalStorage(context, bitmap))
            return
        }
        val options = SubjectSegmenterOptions.Builder().enableForegroundBitmap().build()
        val segmenter = SubjectSegmentation.getClient(options)
        segmenter.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { result ->
                val cutoutBitmap = result.foregroundBitmap
                if (cutoutBitmap != null) onComplete(saveBitmapToInternalStorage(context, cutoutBitmap))
                else {
                    onComplete(saveBitmapToInternalStorage(context, bitmap))
                    Toast.makeText(context, "Aucun sujet net détecté.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { onComplete(saveBitmapToInternalStorage(context, bitmap)) }
    } catch (e: Exception) {
        e.printStackTrace()
        onComplete(null)
    }
}

private fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): Uri {
    val file = File(context.filesDir, "gollections_img_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
    return Uri.fromFile(file)
}