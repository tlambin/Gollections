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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pokyx.gollections.R
import com.pokyx.gollections.ui.viewmodels.CollectionViewModel
import com.pokyx.gollections.utils.getEmojiForCollection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDialog(
    title: String,
    initialName: String = "",
    initialCover: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String, cover: String) -> Unit,
    viewModel: CollectionViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var name by remember { mutableStateOf(initialName) }
    var cover by remember { mutableStateOf(initialCover) }

    var showMenu by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    // Outils pour forcer l'ouverture du clavier sur le sélecteur d'émojis
    val focusRequester = remember { FocusRequester() }
    var hiddenInputText by remember { mutableStateOf("") }

    // Launcher pour ouvrir la galerie photo
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            isProcessing = true
            viewModel.processAndSaveImage(uri, false) { savedUrl ->
                isProcessing = false
                if (savedUrl != null) {
                    cover = savedUrl
                } else {
                    Toast.makeText(context, R.string.toast_cutout_error, Toast.LENGTH_SHORT).show()
                }
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
                // ZONE DE L'ICÔNE (Cercle avec badge)
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
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
                                .offset(x = 4.dp, y = 4.dp)
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable { showMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                        }
                    }

                    // Menu des options de couverture
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.choose_gallery)) },
                            onClick = {
                                showMenu = false
                                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.choose_emoji)) },
                            onClick = {
                                showMenu = false
                                coroutineScope.launch {
                                    // Demande le focus sur le champ invisible et affiche le clavier
                                    focusRequester.requestFocus()
                                    delay(100) // Laisse le temps au système de s'ajuster
                                    keyboardController?.show()
                                }
                            }
                        )
                        if (cover.isNotBlank()) {
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.reset_cover), color = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; cover = "" }
                            )
                        }
                    }
                }

                // CHAMP DE TEXTE INVISIBLE POUR CAPTURER L'EMOJI DU CLAVIER
                // On utilise un BasicTextField presque invisible (taille 1dp, transparent)
                BasicTextField(
                    value = hiddenInputText,
                    onValueChange = { input ->
                        if (input.isNotEmpty()) {
                            // On extrait le dernier caractère/émoji tapé
                            cover = input
                            hiddenInputText = "" // Réinitialise pour les prochaines saisies
                            // Optionnel : masque le clavier après sélection
                            keyboardController?.hide()
                        }
                    },
                    modifier = Modifier
                        .size(1.dp)
                        .focusRequester(focusRequester)
                        .alpha(0f),
                    // Indique au système d'ouvrir de préférence le panneau émoji si disponible
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        autoCorrectEnabled = false
                    )
                )

                // CHAMP DE TEXTE NORMAL POUR LE NOM
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