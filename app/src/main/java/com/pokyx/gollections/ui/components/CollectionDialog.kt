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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pokyx.gollections.R
import com.pokyx.gollections.ui.theme.GollectionsIcons
import com.pokyx.gollections.utils.getEmojiForCollection

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
                            title = { Text("Source de l'illustration", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                            text = {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {

                                    // URL (Planète)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showMenu = false; showUrlDialog = true }.padding(8.dp)) {
                                        Icon(imageVector = GollectionsIcons.Planet, contentDescription = "URL", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("URL", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }

                                    // Galerie (Montagnes)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showMenu = false; galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }.padding(8.dp)) {
                                        Icon(imageVector = GollectionsIcons.RoundedGallery, contentDescription = "Galerie", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Galerie", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }

                                    // Emoji (Smile)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showMenu = false; tempEmoji = if (!cover.startsWith("file") && !cover.startsWith("content") && !cover.startsWith("/") && !cover.startsWith("http")) cover else ""; showEmojiInput = true }.padding(8.dp)) {
                                        Icon(imageVector = GollectionsIcons.Smile, contentDescription = "Emoji", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
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