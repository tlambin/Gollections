package com.pokyx.gollections.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pokyx.gollections.R
import com.pokyx.gollections.ui.viewmodels.PropertyKeys

@Composable
fun getLocalizedPropertyLabel(key: String): String {
    return when (key) {
        PropertyKeys.DIRECTOR -> stringResource(R.string.prop_director)
        PropertyKeys.RELEASE_DATE -> stringResource(R.string.prop_release_date)
        PropertyKeys.SYNOPSIS -> stringResource(R.string.prop_synopsis)
        PropertyKeys.AUTHOR -> stringResource(R.string.prop_author)
        PropertyKeys.PUBLICATION_DATE -> stringResource(R.string.prop_publication_date)
        PropertyKeys.SUMMARY -> stringResource(R.string.prop_summary)
        PropertyKeys.PAGE_COUNT -> stringResource(R.string.prop_page_count)
        PropertyKeys.STUDIO -> stringResource(R.string.prop_studio)
        PropertyKeys.PLATFORM -> stringResource(R.string.prop_platform)
        PropertyKeys.DESCRIPTION -> stringResource(R.string.prop_description)
        PropertyKeys.ARTIST -> stringResource(R.string.prop_artist)
        PropertyKeys.ALBUM -> stringResource(R.string.prop_album)
        // Rétrocompatibilité : Si la clé en base de données est déjà l'ancien texte français ("Réalisateur"), on l'affiche tel quel.
        else -> key
    }
}