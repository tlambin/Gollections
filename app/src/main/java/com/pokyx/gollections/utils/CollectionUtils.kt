package com.pokyx.gollections.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pokyx.gollections.data.Collection

fun getDynamicStatusOptions(collectionName: String): List<String> {
    return when (collectionName.lowercase().trim()) {
        "blu-ray", "films", "cinéma", "cinema" -> listOf("À voir", "En cours", "Vu")
        "livres", "mangas", "bd", "romans" -> listOf("À lire", "En cours", "Lu")
        "jeux vidéo", "jeux", "jeux video", "gaming", "switch", "ps5" -> listOf("À faire", "En cours", "Terminé", "Platiné", "Abandonné")
        "vinyles", "musique", "cd", "disques" -> listOf("À écouter", "Écouté")
        else -> listOf("Nouveau", "En cours", "Terminé")
    }
}

fun buildPathBottomUp(targetId: Long, allCols: List<Collection>): List<Long> {
    val path = mutableListOf<Long>()
    var curr: Long? = targetId
    while (curr != null) {
        val found = allCols.find { it.id == curr }
        if (found != null) {
            path.add(0, curr)
            curr = found.parentId
        } else {
            break
        }
    }
    return path
}

fun getUnitForCollection(name: String, count: Int): String {
    return when (name.lowercase().trim()) {
        "blu-ray", "films", "cinéma", "cinema" -> if (count <= 1) "film" else "films"
        "vinyles" -> if (count <= 1) "album" else "albums"
        "jeux vidéo", "jeux", "jeux video", "gaming", "switch", "ps5" -> if (count <= 1) "jeu" else "jeux"
        "livres", "mangas", "bd", "romans" -> if (count <= 1) "livre" else "livres"
        else -> if (count <= 1) "objet" else "objets"
    }
}

fun getEmojiForCollection(collectionName: String): String {
    return when (collectionName.lowercase().trim()) {
        "blu-ray", "bluray", "film", "films", "cinéma", "cinema" -> "🎬"
        "vinyles", "vinyle", "musique", "disques", "disque", "cd" -> "🎵"
        "jeux vidéo", "jeux", "jeux video", "gaming", "switch", "ps5" -> "🎮"
        "livres", "livre", "mangas", "manga", "bd", "romans" -> "📚"
        "figurines", "figurine", "pop" -> "🧸"
        "jeux de société", "jeux de societe", "cartes" -> "🎲"
        else -> "🗃️"
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.outline, fontSize = 14.sp)
        Text(text = value, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

// --- NOUVEAU : Dialogue d'ajout d'étiquette réutilisable ---
@Composable
fun AddTagDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newTagName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle Étiquette") },
        text = {
            OutlinedTextField(
                value = newTagName,
                onValueChange = { newTagName = it },
                label = { Text("Nom (ex: 4K, Collector, PS5...)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newTagName.isNotBlank()) {
                        onConfirm(newTagName.trim())
                    }
                }
            ) { Text("Créer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}