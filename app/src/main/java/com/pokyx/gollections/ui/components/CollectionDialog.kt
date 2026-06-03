package com.pokyx.gollections.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pokyx.gollections.R
import com.pokyx.gollections.utils.getEmojiForCollection

// --- NOS ICÔNES MAISON PERSONNALISÉES EN VECTORIEL ---

// Icône Planète (Internet / URL)
val CustomPlanetIcon: ImageVector
    get() = ImageVector.Builder(
        name = "CustomPlanet",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        curveToRelative(0f, 5.52f, 4.48f, 10f, 10f, 10f)
        curveToRelative(5.52f, 0f, 10f, -4.48f, 10f, -10f)
        curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
        close()
        // Contour extérieur / méridiens
        moveTo(12f, 4f)
        curveToRelative(1.66f, 0f, 3.45f, 2.83f, 3.92f, 6f)
        horizontalLineTo(8.08f)
        curveTo(8.55f, 6.83f, 10.34f, 4f, 12f, 4f)
        close()
        moveTo(12f, 20f)
        curveToRelative(-1.66f, 0f, -3.45f, -2.83f, -3.92f, -6f)
        horizontalLineToRelative(7.84f)
        curveToRelative(-0.47f, 3.17f, -2.26f, 6f, -1.92f, 6f)
        close()
        // Lignes horizontales (équateur)
        moveTo(4.17f, 12f)
        curveToRelative(0.12f, -2.71f, 1.48f, -5.11f, 3.49f, -6.61f)
        curveTo(6.46f, 7.15f, 5.86f, 9.47f, 5.8f, 12f)
        horizontalLineTo(4.17f)
        close()
        moveTo(19.83f, 12f)
        horizontalLineToRelative(-1.63f)
        curveToRelative(-0.06f, -2.53f, -0.66f, -4.85f, -1.86f, -6.61f)
        curveToRelative(2.01f, 1.5f, 3.37f, 3.9f, 3.49f, 6.61f)
        close()
        // Hémisphère sud horizontal
        moveTo(7.66f, 18.61f)
        curveToRelative(-2.01f, -1.5f, -3.37f, -3.9f, -3.49f, -6.61f)
        horizontalLineToRelative(1.63f)
        curveToRelative(0.06f, 2.53f, 0.66f, 4.85f, 1.86f, 6.61f)
        close()
        moveTo(16.34f, 18.61f)
        curveToRelative(1.2f, -1.76f, 1.8f, -4.08f, 1.86f, -6.61f)
        horizontalLineToRelative(1.63f)
        curveToRelative(-0.12f, 2.71f, -1.48f, 5.11f, -3.49f, 6.61f)
        close()
    }.build()

// Icône Photo aux bords arrondis avec montagnes (Galerie)
val CustomRoundedGalleryIcon: ImageVector
    get() = ImageVector.Builder(
        name = "CustomRoundedGallery",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
        // Cadre extérieur arrondi
        moveTo(19f, 3f)
        horizontalLineTo(5f)
        curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
        verticalLineToRelative(14f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(5f)
        curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f)
        close()
        // Zone de dessin interne de la photo
        moveTo(19f, 17f)
        horizontalLineTo(5f)
        verticalLineTo(5f)
        horizontalLineToRelative(14f)
        verticalLineToRelative(12f)
        close()
        // Les montagnes à l'intérieur
        moveTo(14f, 11.4f)
        lineToRelative(-3.5f, 4.5f)
        lineToRelative(-2.5f, -3f)
        lineTo(5f, 17f)
        horizontalLineToRelative(14f)
        lineToRelative(-5f, -5.6f)
        close()
    }.build()

// Icône Smiley standard (Emoji)
val CustomSmileIcon: ImageVector
    get() = ImageVector.Builder(
        name = "CustomSmile",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        curveToRelative(0f, 5.52f, 4.48f, 10f, 10f, 10f)
        curveToRelative(5.52f, 0f, 10f, -4.48f, 10f, -10f)
        curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
        close()
        moveTo(12f, 20f)
        curveToRelative(-4.41f, 0f, -8f, -3.59f, -8f, -8f)
        curveToRelative(0f, -4.41f, 3.59f, -8f, 8f, -8f)
        curveToRelative(4.41f, 0f, 8f, 3.59f, 8f, 8f)
        curveToRelative(0f, 4.41f, -3.59f, 8f, -8f, 8f)
        close()
        moveTo(15.5f, 11f)
        curveToRelative(0.83f, 0f, 1.5f, -0.67f, 1.5f, -1.5f)
        curveTo(17f, 8.67f, 16.33f, 8f, 15.5f, 8f)
        curveTo(14.67f, 8f, 14f, 8.67f, 14f, 9.5f)
        curveToRelative(0f, 0.83f, 0.67f, 1.5f, 1.5f, 1.5f)
        close()
        moveTo(8.5f, 11f)
        curveToRelative(0.83f, 0f, 1.5f, -0.67f, 1.5f, -1.5f)
        curveTo(10f, 8.67f, 9.33f, 8f, 8.5f, 8f)
        curveTo(7.67f, 8f, 7f, 8.67f, 7f, 9.5f)
        curveToRelative(0f, 0.83f, 0.67f, 1.5f, 1.5f, 1.5f)
        close()
        moveTo(12f, 17.5f)
        curveToRelative(2.33f, 0f, 4.31f, -1.46f, 5.11f, -3.5f)
        horizontalLineTo(6.89f)
        curveToRelative(0.8f, 2.04f, 2.78f, 3.5f, 5.21f, 3.5f)
        close()
    }.build()


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDialog(
    title: String,
    initialName: String = "",
    initialCover: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String, cover: String) -> Unit,
    onProcessImage: (Uri, Boolean, (String?) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialName) }
    var cover by remember { mutableStateOf(initialCover) }

    var showMenu by remember { mutableStateOf(false) }
    var showEmojiInput by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var tempEmoji by remember { mutableStateOf("") }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            isProcessing = true
            onProcessImage(uri, false) { savedUrl ->
                isProcessing = false
                if (savedUrl != null) cover = savedUrl else Toast.makeText(context, R.string.toast_cutout_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(enabled = !isProcessing) { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else if (cover.startsWith("file") || cover.startsWith("content") || cover.startsWith("http") || cover.startsWith("/")) {
                            AsyncImage(
                                model = cover,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val displayEmoji = if (cover.isNotBlank()) cover else getEmojiForCollection(name.ifBlank { "dossier" })
                            Text(text = displayEmoji, fontSize = 40.sp)
                        }
                    }

                    if (!isProcessing) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 8.dp, y = 8.dp)
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable { showMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                        }
                    }

                    if (showMenu) {
                        AlertDialog(
                            onDismissRequest = { showMenu = false },
                            // MODIFIÉ : Titre beaucoup plus propre et professionnel
                            title = { Text("Importer depuis", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                            text = {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {

                                    // 1. Action URL avec notre icône planète maison
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showMenu = false; showUrlDialog = true }.padding(8.dp)) {
                                        Icon(imageVector = CustomPlanetIcon, contentDescription = "URL", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("URL", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }

                                    // 2. Action GALERIE avec notre icône de photo montagne arrondie maison
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showMenu = false; galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }.padding(8.dp)) {
                                        Icon(imageVector = CustomRoundedGalleryIcon, contentDescription = "Galerie", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Galerie", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }

                                    // 3. Action EMOJI
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showMenu = false; tempEmoji = if (!cover.startsWith("file") && !cover.startsWith("content") && !cover.startsWith("/") && !cover.startsWith("http")) cover else ""; showEmojiInput = true }.padding(8.dp)) {
                                        Icon(imageVector = CustomSmileIcon, contentDescription = "Emoji", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Emoji", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            },
                            confirmButton = {
                                if (cover.isNotBlank()) {
                                    TextButton(onClick = { showMenu = false; cover = "" }) { Text(stringResource(R.string.reset_cover), color = MaterialTheme.colorScheme.error) }
                                }
                                TextButton(onClick = { showMenu = false }) { Text(stringResource(R.string.cancel)) }
                            }
                        )
                    }
                }

                if (showUrlDialog) {
                    AlertDialog(
                        onDismissRequest = { showUrlDialog = false },
                        title = { Text("Lien de l'image (URL)", fontWeight = FontWeight.Bold) },
                        text = {
                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                label = { Text("Coller l'URL de l'image ici") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                if (urlInput.isNotBlank()) cover = urlInput.trim()
                                showUrlDialog = false
                            }) { Text("OK") }
                        },
                        dismissButton = { TextButton(onClick = { showUrlDialog = false }) { Text(stringResource(R.string.cancel)) } }
                    )
                }

                if (showEmojiInput) {
                    OutlinedTextField(
                        value = tempEmoji,
                        onValueChange = { input ->
                            if (input.length <= 4) {
                                tempEmoji = input
                                if (input.isNotBlank()) cover = input
                            }
                        },
                        label = { Text(stringResource(R.string.type_emoji)) },
                        singleLine = true,
                        trailingIcon = { TextButton(onClick = { showEmojiInput = false }) { Text("OK") } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.new_subfolder_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), cover) },
                enabled = name.isNotBlank() && !isProcessing
            ) { Text(stringResource(R.string.btn_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}