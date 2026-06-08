package com.pokyx.gollections.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import com.pokyx.gollections.utils.getEmojiForCollection
import com.pokyx.gollections.ui.theme.Dimens

// OPTIMISATION CRITIQUE : Suppression des "get()" pour stocker le vecteur en cache
// au lieu de le recalculer à chaque frame de l'interface.
val LabelIcon: ImageVector = ImageVector.Builder(name = "Label", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply { path(fill = SolidColor(Color.Black)) { moveTo(17.63f, 5.84f); curveTo(17.27f, 5.33f, 16.67f, 5.0f, 16.0f, 5.0f); lineTo(5.01f, 5.0f); curveTo(3.9f, 5.0f, 3.0f, 5.9f, 3.0f, 7.0f); lineTo(3.0f, 17.0f); curveTo(3.0f, 18.1f, 3.9f, 19.0f, 5.01f, 19.0f); lineTo(16.0f, 19.0f); curveTo(16.67f, 19.0f, 17.27f, 18.66f, 17.63f, 18.15f); lineTo(22.0f, 12.0f); lineTo(17.63f, 5.84f); close() } }.build()
val FilterListIcon: ImageVector = ImageVector.Builder(name = "FilterList", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply { path(fill = SolidColor(Color.Black)) { moveTo(10.0f, 18.0f); horizontalLineToRelative(4.0f); verticalLineToRelative(-2.0f); horizontalLineToRelative(-4.0f); verticalLineToRelative(2.0f); close(); moveTo(3.0f, 6.0f); verticalLineToRelative(2.0f); horizontalLineToRelative(18.0f); verticalLineToRelative(-2.0f); horizontalLineTo(3.0f); close(); moveTo(6.0f, 13.0f); horizontalLineToRelative(12.0f); verticalLineToRelative(-2.0f); horizontalLineTo(6.0f); verticalLineToRelative(2.0f); close() } }.build()
val CameraIcon: ImageVector = ImageVector.Builder(name = "Camera", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply { path(fill = SolidColor(Color.Black)) { moveTo(9.0f, 2.0f); lineTo(7.17f, 4.0f); horizontalLineTo(4.0f); curveTo(2.9f, 4.0f, 2.0f, 4.9f, 2.0f, 6.0f); verticalLineToRelative(12.0f); curveTo(2.0f, 19.1f, 2.9f, 20.0f, 4.0f, 20.0f); horizontalLineToRelative(16.0f); curveTo(21.1f, 20.0f, 22.0f, 19.1f, 22.0f, 18.0f); verticalLineTo(6.0f); curveTo(22.0f, 4.9f, 21.1f, 4.0f, 20.0f, 4.0f); horizontalLineToRelative(-3.17f); lineTo(15.0f, 2.0f); horizontalLineTo(9.0f); close(); moveTo(12.0f, 17.0f); curveTo(9.24f, 17.0f, 7.0f, 14.76f, 7.0f, 12.0f); curveTo(7.0f, 9.24f, 9.24f, 7.0f, 12.0f, 7.0f); curveTo(14.76f, 7.0f, 17.0f, 9.24f, 17.0f, 12.0f); curveTo(17.0f, 14.76f, 14.76f, 17.0f, 12.0f, 17.0f); close(); moveTo(12.0f, 9.0f); curveTo(10.34f, 9.0f, 9.0f, 10.34f, 9.0f, 12.0f); curveTo(9.0f, 13.66f, 10.34f, 15.0f, 12.0f, 15.0f); curveTo(13.66f, 15.0f, 15.0f, 13.66f, 15.0f, 12.0f); curveTo(15.0f, 10.34f, 13.66f, 9.0f, 12.0f, 9.0f); close() } }.build()
val FolderIcon: ImageVector = ImageVector.Builder(name = "Folder", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply { path(fill = SolidColor(Color.Black)) { moveTo(10.0f, 4.0f); horizontalLineTo(4.0f); curveTo(2.9f, 4.0f, 2.01f, 4.9f, 2.01f, 6.0f); lineTo(2.0f, 18.0f); curveTo(2.0f, 19.1f, 2.9f, 20.0f, 4.0f, 20.0f); horizontalLineToRelative(16.0f); curveTo(21.1f, 20.0f, 22.0f, 19.1f, 22.0f, 18.0f); verticalLineTo(8.0f); curveTo(22.0f, 6.9f, 21.1f, 6.0f, 20.0f, 6.0f); horizontalLineToRelative(-8.0f); lineToRelative(-2.0f, -2.0f); close() } }.build()

// --- Boutons flottants du menu + ---
@Composable
fun MultiFabItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.padding(end = Dimens.spacingSmall)
    ) {
        Surface(
            shape = RoundedCornerShape(Dimens.cornerSmall),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 2.dp,
            modifier = Modifier
                .padding(end = Dimens.spacingMedium)
                .clickable { onClick() }
        ) {
            Text(
                text = text,
                fontSize = Dimens.textBody,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = Dimens.spacingMedium, vertical = Dimens.spacingTiny)
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Icon(icon, contentDescription = text, modifier = Modifier.size(Dimens.iconSmall))
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
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier = Modifier.padding(vertical = Dimens.spacingTiny),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(horizontal = Dimens.spacingMedium, vertical = Dimens.spacingSmall),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = contentColor,
                fontSize = Dimens.textBody,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// --- Petite carte pour les sous-dossiers ---
@Composable
fun SubCollectionSmallCard(
    collection: Collection,
    itemCount: Int,
    modifier: Modifier = Modifier,
    onCollectionClick: (Long) -> Unit
) {
    // Box globale permettant de faire dépasser la bulle de notification (Badge) en haut à droite
    Box(
        modifier = modifier
            .padding(top = Dimens.spacingSmall, end = Dimens.spacingSmall)
    ) {
        // 1. La Carte Principale
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.cardHeight)
                .clickable { onCollectionClick(collection.id) },
            shape = RoundedCornerShape(Dimens.cornerLarge),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimens.spacingMedium),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier.wrapContentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (collection.cover.startsWith("file") || collection.cover.startsWith("/") || collection.cover.startsWith("content") || collection.cover.startsWith("http")) {
                        AsyncImage(
                            model = collection.cover,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        val displayEmoji = if (collection.cover.isNotBlank()) collection.cover else getEmojiForCollection(collection.name)
                        Text(
                            text = displayEmoji,
                            fontSize = Dimens.textEmojiLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = collection.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = Dimens.textCaption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 2. La bulle d'objet sortante en haut à droite
        if (itemCount > 0) {
            Box(
                modifier = Modifier
                    .size(Dimens.iconMedium)
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = itemCount.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = Dimens.textBadge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}