package com.pokyx.gollections.utils

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pokyx.gollections.R
import com.pokyx.gollections.data.Collection

fun getDynamicStatusOptions(context: Context, collectionName: String): List<String> {
    return when (collectionName.lowercase().trim()) {
        "blu-ray", "films", "cinéma", "cinema" -> listOf(
            context.getString(R.string.status_to_see),
            context.getString(R.string.status_in_progress),
            context.getString(R.string.status_seen)
        )
        "livres", "mangas", "bd", "romans" -> listOf(
            context.getString(R.string.status_to_read),
            context.getString(R.string.status_in_progress),
            context.getString(R.string.status_read)
        )
        "jeux vidéo", "jeux", "jeux video", "gaming", "switch", "ps5" -> listOf(
            context.getString(R.string.status_to_do),
            context.getString(R.string.status_in_progress),
            context.getString(R.string.status_done),
            context.getString(R.string.status_platinum),
            context.getString(R.string.status_abandoned)
        )
        "vinyles", "musique", "cd", "disques" -> listOf(
            context.getString(R.string.status_to_listen),
            context.getString(R.string.status_listened)
        )
        else -> listOf(
            context.getString(R.string.status_new),
            context.getString(R.string.status_in_progress),
            context.getString(R.string.status_done)
        )
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

fun getUnitForCollection(context: Context, name: String, count: Int): String {
    return when (name.lowercase().trim()) {
        "blu-ray", "films", "cinéma", "cinema" -> if (count <= 1) context.getString(R.string.unit_film_single) else context.getString(R.string.unit_film_plural)
        "vinyles" -> if (count <= 1) context.getString(R.string.unit_album_single) else context.getString(R.string.unit_album_plural)
        "jeux vidéo", "jeux", "jeux video", "gaming", "switch", "ps5" -> if (count <= 1) context.getString(R.string.unit_game_single) else context.getString(R.string.unit_game_plural)
        "livres", "mangas", "bd", "romans" -> if (count <= 1) context.getString(R.string.unit_book_single) else context.getString(R.string.unit_book_plural)
        else -> if (count <= 1) context.getString(R.string.unit_object_single) else context.getString(R.string.unit_object_plural)
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
        else -> "📦"
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

@Composable
fun AddTagDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newTagName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_tag_title).replace("?", "")) },
        text = {
            OutlinedTextField(
                value = newTagName,
                onValueChange = { newTagName = it },
                label = { Text(stringResource(R.string.new_tag_label)) },
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
            ) { Text(stringResource(R.string.create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}