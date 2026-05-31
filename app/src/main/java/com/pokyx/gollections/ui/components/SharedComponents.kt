package com.pokyx.gollections.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pokyx.gollections.data.Collection

// --- Définition des icônes manquantes (Vecteurs originaux réintégrés) ---
val LabelIcon: ImageVector get() = ImageVector.Builder(name = "Label", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply { path(fill = SolidColor(Color.Black)) { moveTo(17.63f, 5.84f); curveTo(17.27f, 5.33f, 16.67f, 5.0f, 16.0f, 5.0f); lineTo(5.01f, 5.0f); curveTo(3.9f, 5.0f, 3.0f, 5.9f, 3.0f, 7.0f); lineTo(3.0f, 17.0f); curveTo(3.0f, 18.1f, 3.9f, 19.0f, 5.01f, 19.0f); lineTo(16.0f, 19.0f); curveTo(16.67f, 19.0f, 17.27f, 18.66f, 17.63f, 18.15f); lineTo(22.0f, 12.0f); lineTo(17.63f, 5.84f); close() } }.build()
val FilterListIcon: ImageVector get() = ImageVector.Builder(name = "FilterList", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply { path(fill = SolidColor(Color.Black)) { moveTo(10.0f, 18.0f); horizontalLineToRelative(4.0f); verticalLineToRelative(-2.0f); horizontalLineToRelative(-4.0f); verticalLineToRelative(2.0f); close(); moveTo(3.0f, 6.0f); verticalLineToRelative(2.0f); horizontalLineToRelative(18.0f); verticalLineToRelative(-2.0f); horizontalLineTo(3.0f); close(); moveTo(6.0f, 13.0f); horizontalLineToRelative(12.0f); verticalLineToRelative(-2.0f); horizontalLineTo(6.0f); verticalLineToRelative(2.0f); close() } }.build()
val CameraIcon: ImageVector get() = ImageVector.Builder(name = "Camera", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply { path(fill = SolidColor(Color.Black)) { moveTo(9.0f, 2.0f); lineTo(7.17f, 4.0f); horizontalLineTo(4.0f); curveTo(2.9f, 4.0f, 2.0f, 4.9f, 2.0f, 6.0f); verticalLineToRelative(12.0f); curveTo(2.0f, 19.1f, 2.9f, 20.0f, 4.0f, 20.0f); horizontalLineToRelative(16.0f); curveTo(21.1f, 20.0f, 22.0f, 19.1f, 22.0f, 18.0f); verticalLineTo(6.0f); curveTo(22.0f, 4.9f, 21.1f, 4.0f, 20.0f, 4.0f); horizontalLineToRelative(-3.17f); lineTo(15.0f, 2.0f); horizontalLineTo(9.0f); close(); moveTo(12.0f, 17.0f); curveTo(9.24f, 17.0f, 7.0f, 14.76f, 7.0f, 12.0f); curveTo(7.0f, 9.24f, 9.24f, 7.0f, 12.0f, 7.0f); curveTo(14.76f, 7.0f, 17.0f, 9.24f, 17.0f, 12.0f); curveTo(17.0f, 14.76f, 14.76f, 17.0f, 12.0f, 17.0f); close(); moveTo(12.0f, 9.0f); curveTo(10.34f, 9.0f, 9.0f, 10.34f, 9.0f, 12.0f); curveTo(9.0f, 13.66f, 10.34f, 15.0f, 12.0f, 15.0f); curveTo(13.66f, 15.0f, 15.0f, 13.66f, 15.0f, 12.0f); curveTo(15.0f, 10.34f, 13.66f, 9.0f, 12.0f, 9.0f); close() } }.build()
val FolderIcon: ImageVector get() = ImageVector.Builder(name = "Folder", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply { path(fill = SolidColor(Color.Black)) { moveTo(10.0f, 4.0f); horizontalLineTo(4.0f); curveTo(2.9f, 4.0f, 2.01f, 4.9f, 2.01f, 6.0f); lineTo(2.0f, 18.0f); curveTo(2.0f, 19.1f, 2.9f, 20.0f, 4.0f, 20.0f); horizontalLineToRelative(16.0f); curveTo(21.1f, 20.0f, 22.0f, 19.1f, 22.0f, 18.0f); verticalLineTo(8.0f); curveTo(22.0f, 6.9f, 21.1f, 6.0f, 20.0f, 6.0f); horizontalLineToRelative(-8.0f); lineToRelative(-2.0f, -2.0f); close() } }.build()

// --- Boutons flottants du menu + ---
@Composable
fun MultiFabItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 2.dp,
            modifier = Modifier
                .padding(end = 12.dp)
                .clickable { onClick() }
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Icon(icon, contentDescription = text, modifier = Modifier.size(20.dp))
        }
    }
}

// --- Étiquettes personnalisées (Tags) ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomTagChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// --- Petite carte pour les sous-dossiers ---
@Composable
fun SubCollectionSmallCard(
    collection: Collection,
    itemCount: Int,
    modifier: Modifier = Modifier,
    onClick: (Long) -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(1.1f)
            .clickable { onClick(collection.id) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (collection.cover.isNotBlank() && (collection.cover.startsWith("file") || collection.cover.startsWith("content") || collection.cover.startsWith("/"))) {
                AsyncImage(
                    model = collection.cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                )
            } else {
                val displayEmoji = if (collection.cover.isNotBlank()) collection.cover else "📁"
                Text(text = displayEmoji, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = collection.name,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$itemCount obj",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1
            )
        }
    }
}