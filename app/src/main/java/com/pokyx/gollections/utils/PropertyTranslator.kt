package com.pokyx.gollections.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pokyx.gollections.R

@Composable
fun getLocalizedPropertyLabel(key: String): String {
    // Les références à l'enum supprimé ont été remplacées par leurs valeurs textuelles pour garder la rétrocompatibilité
    return when (key) {
        "prop_director" -> stringResource(R.string.prop_director)
        "prop_release_date" -> stringResource(R.string.prop_release_date)
        "prop_synopsis" -> stringResource(R.string.prop_synopsis)
        "prop_author" -> stringResource(R.string.prop_author)
        "prop_publication_date" -> stringResource(R.string.prop_publication_date)
        "prop_summary" -> stringResource(R.string.prop_summary)
        "prop_page_count" -> stringResource(R.string.prop_page_count)
        "prop_studio" -> stringResource(R.string.prop_studio)
        "prop_platform" -> stringResource(R.string.prop_platform)
        "prop_description" -> stringResource(R.string.prop_description)
        "prop_artist" -> stringResource(R.string.prop_artist)
        "prop_album" -> stringResource(R.string.prop_album)
        // Cas par défaut pour nos nouveaux templates dynamiques (ex: "Console") !
        else -> key
    }
}