package com.pokyx.gollections.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.pokyx.gollections.ui.theme.GollectionsIcons
import com.pokyx.gollections.ui.viewmodels.ItemFormState
import com.pokyx.gollections.ui.viewmodels.ItemViewModel
import com.pokyx.gollections.utils.getEmojiForCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class DisplayFormat(val ratio: Float, val label: String) {
    PORTRAIT(3f / 4f, "Portrait"),
    LANDSCAPE(16f / 9f, "Paysage")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemFormBody(
    viewModel: ItemViewModel,
    onCollectionClick: () -> Unit,
    onTypeClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.formState.collectAsStateWithLifecycle()
    val collectionsList by viewModel.collections.collectAsStateWithLifecycle()

    val finalSelectedCollection = collectionsList.find { it.id == state.selectedPath.lastOrNull() }
    val finalSelectedName = finalSelectedCollection?.name ?: "Choisir un dossier"
    val finalSelectedCover = finalSelectedCollection?.cover ?: ""

    var showSourceDialog by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf("") }
    var isProcessingImage by remember { mutableStateOf(false) }
    var tempPhotoUriString by rememberSaveable { mutableStateOf<String?>(null) }

    var selectedImageFormat by rememberSaveable { mutableStateOf(DisplayFormat.PORTRAIT) }
    var attachments by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { viewModel.updateForm { it.copy(imageUrl = uri.toString()) } }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempPhotoUriString != null) { viewModel.updateForm { it.copy(imageUrl = tempPhotoUriString!!) } }
    }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { attachments = attachments + uri }
    }

    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val cardColor = MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ====================================================================
        // 1. BLOC IMAGE ET BULLE DE FORMAT
        // ====================================================================
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(selectedImageFormat.ratio)
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardColor)
                    .clickable { showSourceDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (isProcessingImage) {
                    CircularProgressIndicator()
                } else if (state.imageUrl.isNotBlank()) {
                    AsyncImage(model = state.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .clickable { selectedImageFormat = if (selectedImageFormat == DisplayFormat.PORTRAIT) DisplayFormat.LANDSCAPE else DisplayFormat.PORTRAIT }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Format", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = selectedImageFormat.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // ====================================================================
        // 2. TITRE PRINCIPAL
        // ====================================================================
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            TextField(
                value = state.title,
                onValueChange = { newTitle -> viewModel.updateForm { it.copy(title = newTitle) } },
                placeholder = { Text("Titre de l'objet", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                singleLine = true
            )
        }

        // ====================================================================
        // 3. CARTE D'IDENTITÉ (DOSSIER & TYPE)
        // ====================================================================
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onCollectionClick() }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (finalSelectedCollection != null) {
                        if (finalSelectedCover.startsWith("file") || finalSelectedCover.startsWith("/") || finalSelectedCover.startsWith("content") || finalSelectedCover.startsWith("http")) {
                            AsyncImage(model = finalSelectedCover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(24.dp).clip(CircleShape))
                        } else {
                            val emoji = if (finalSelectedCover.isNotBlank()) finalSelectedCover else getEmojiForCollection(finalSelectedName)
                            Text(text = emoji, fontSize = 18.sp)
                        }
                    } else {
                        Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Dossier", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Text(finalSelectedName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.rotate(-90f))
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = backgroundColor)

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onTypeClick() }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = state.itemType.emoji, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Type d'objet", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Text(state.itemType.label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.rotate(-90f))
                }
            }
        }

        // ====================================================================
        // 4. SECTIONS PROTON PASS
        // ====================================================================
        if (state.properties.isNotEmpty()) {
            ProtonPassSection(
                title = "Informations détaillées",
                onAddFieldClick = { /* Action ajout de champ */ }
            ) {
                state.properties.forEach { (propertyName, value) ->
                    ProtonPassFieldRow(
                        label = propertyName,
                        value = value,
                        onValueChange = { newValue -> viewModel.updateProperty(propertyName, newValue) },
                        onDeleteClick = { viewModel.removeProperty(propertyName) }
                    )
                    HorizontalDivider(color = backgroundColor, thickness = 1.dp, modifier = Modifier.padding(start = 16.dp))
                }
            }
        }

        ProtonPassSection(
            title = "Informations d'acquisition",
            onAddFieldClick = { /* Action ajout de champ */ }
        ) {
            ProtonPassFieldRow(
                label = "Prix d'achat",
                value = state.price,
                onValueChange = { newPrice -> viewModel.updateForm { form -> form.copy(price = newPrice) } },
                keyboardType = KeyboardType.Number
            )
            HorizontalDivider(color = backgroundColor, thickness = 1.dp, modifier = Modifier.padding(start = 16.dp))
            ProtonPassFieldRow(
                label = "Date d'achat",
                value = state.purchaseDate,
                onValueChange = {},
                readOnly = true
            )
        }

        TextButton(
            onClick = { /* Création de section dynamique */ },
            modifier = Modifier.padding(horizontal = 16.dp).align(Alignment.Start)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Créer une section", fontWeight = FontWeight.Medium)
        }

        // ====================================================================
        // 5. PIÈCES JOINTES
        // ====================================================================
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Column {
                Text(text = "Pièces jointes", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))

                attachments.forEach { uri ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Document joint", modifier = Modifier.weight(1f), maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                        IconButton(onClick = { attachments = attachments - uri }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    HorizontalDivider(color = backgroundColor, thickness = 1.dp, modifier = Modifier.padding(start = 48.dp))
                }

                TextButton(
                    onClick = { fileLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ajouter un fichier (Facture, Ticket...)")
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    // --- DIALOGUES ---
    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Source de l'illustration", fontWeight = FontWeight.Bold) },
            text = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showSourceDialog = false; showUrlDialog = true }.padding(8.dp)) {
                        Icon(imageVector = GollectionsIcons.Planet, contentDescription = "URL", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("URL", fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                        showSourceDialog = false
                        scope.launch(Dispatchers.IO) {
                            val tempFile = File.createTempFile("cam_", ".jpg", context.cacheDir)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                            withContext(Dispatchers.Main) {
                                tempPhotoUriString = uri.toString()
                                cameraLauncher.launch(uri)
                            }
                        }
                    }.padding(8.dp)) {
                        Icon(imageVector = GollectionsIcons.Camera, contentDescription = "Appareil", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Appareil", fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showSourceDialog = false; galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }.padding(8.dp)) {
                        Icon(imageVector = GollectionsIcons.RoundedGallery, contentDescription = "Galerie", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Galerie", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSourceDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("Lien de l'image (URL)", fontWeight = FontWeight.Bold) },
            text = { OutlinedTextField(value = urlInput, onValueChange = { urlInput = it }, label = { Text("Coller l'URL") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { Button(onClick = { if (urlInput.isNotBlank()) viewModel.updateForm { it.copy(imageUrl = urlInput.trim()) }; showUrlDialog = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showUrlDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
fun ProtonPassSection(title: String, onAddFieldClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
            content()
            TextButton(onClick = onAddFieldClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Ajouter un champ")
            }
        }
    }
}

@Composable
fun ProtonPassFieldRow(label: String, value: String, onValueChange: (String) -> Unit, readOnly: Boolean = false, keyboardType: KeyboardType = KeyboardType.Text, onDeleteClick: (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, modifier = Modifier.weight(0.35f), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        TextField(value = value, onValueChange = onValueChange, readOnly = readOnly, placeholder = { Text("Vide", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), fontSize = 14.sp) }, modifier = Modifier.weight(0.65f), keyboardOptions = KeyboardOptions(keyboardType = keyboardType), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface), textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium), trailingIcon = onDeleteClick?.let { { IconButton(onClick = it, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Clear, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(16.dp)) } } })
    }
}