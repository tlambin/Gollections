package com.pokyx.gollections.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pokyx.gollections.R

@Composable
fun ExpandableActionFab(
    isExpanded: Boolean,
    createFolderText: String,
    onToggle: () -> Unit,
    onScanClick: () -> Unit,
    onCreateFolderClick: () -> Unit,
    onAddItemClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.End) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { 50 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { 50 })
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                MultiFabItem(
                    text = stringResource(R.string.action_scan),
                    icon = CameraIcon, // Composant externe présumé
                    onClick = onScanClick
                )
                MultiFabItem(
                    text = createFolderText,
                    icon = FolderIcon, // Composant externe présumé
                    onClick = onCreateFolderClick
                )
                MultiFabItem(
                    text = stringResource(R.string.action_add_item),
                    icon = Icons.Default.Add,
                    onClick = onAddItemClick
                )
            }
        }

        FloatingActionButton(
            onClick = onToggle,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            val rotation by animateFloatAsState(targetValue = if (isExpanded) 45f else 0f, label = "fab_rotation")
            Icon(
                Icons.Default.Add,
                contentDescription = if (isExpanded) "Fermer le menu" else "Ouvrir le menu actions", // Micro-optimisation d'accessibilité
                modifier = Modifier.size(28.dp).rotate(rotation)
            )
        }
    }
}