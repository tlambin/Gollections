package com.pokyx.gollections.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pokyx.gollections.R
import com.pokyx.gollections.ui.viewmodels.ItemPropertyKey

@Composable
fun getLocalizedPropertyLabel(key: String): String {
    return when (key) {
        ItemPropertyKey.DIRECTOR.value -> stringResource(R.string.prop_director)
        ItemPropertyKey.RELEASE_DATE.value -> stringResource(R.string.prop_release_date)
        ItemPropertyKey.SYNOPSIS.value -> stringResource(R.string.prop_synopsis)
        ItemPropertyKey.AUTHOR.value -> stringResource(R.string.prop_author)
        ItemPropertyKey.PUBLICATION_DATE.value -> stringResource(R.string.prop_publication_date)
        ItemPropertyKey.SUMMARY.value -> stringResource(R.string.prop_summary)
        ItemPropertyKey.PAGE_COUNT.value -> stringResource(R.string.prop_page_count)
        ItemPropertyKey.STUDIO.value -> stringResource(R.string.prop_studio)
        ItemPropertyKey.PLATFORM.value -> stringResource(R.string.prop_platform)
        ItemPropertyKey.DESCRIPTION.value -> stringResource(R.string.prop_description)
        ItemPropertyKey.ARTIST.value -> stringResource(R.string.prop_artist)
        ItemPropertyKey.ALBUM.value -> stringResource(R.string.prop_album)
        // Rétrocompatibilité : Si la clé en base de données est déjà l'ancien texte français ("Réalisateur"), on l'affiche tel quel.
        else -> key
    }
}