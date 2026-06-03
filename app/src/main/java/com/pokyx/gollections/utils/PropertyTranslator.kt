package com.pokyx.gollections.utils

import com.pokyx.gollections.ui.viewmodels.ItemPropertyKey

fun getLocalizedPropertyLabel(key: String): String {
    return when (key) {
        ItemPropertyKey.DIRECTOR.value -> "Réalisateur"
        ItemPropertyKey.RELEASE_DATE.value -> "Date de sortie"
        ItemPropertyKey.SYNOPSIS.value -> "Synopsis"
        ItemPropertyKey.AUTHOR.value -> "Auteur"
        ItemPropertyKey.PUBLICATION_DATE.value -> "Date de publication"
        ItemPropertyKey.SUMMARY.value -> "Résumé"
        ItemPropertyKey.PAGE_COUNT.value -> "Nombre de pages"
        ItemPropertyKey.STUDIO.value -> "Studio"
        ItemPropertyKey.PLATFORM.value -> "Plateforme"
        ItemPropertyKey.DESCRIPTION.value -> "Description"
        ItemPropertyKey.ARTIST.value -> "Artiste"
        ItemPropertyKey.ALBUM.value -> "Album"
        // Rétrocompatibilité : Si la clé en base de données est déjà l'ancien texte français ("Réalisateur"), on l'affiche tel quel.
        else -> key
    }
}