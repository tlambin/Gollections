package com.pokyx.gollections.ui.navigation

import kotlinx.serialization.Serializable

// Écran principal (Dashboard)
@Serializable
object DashboardRoute

// Écran de liste, qui prend obligatoirement un argument de type String
@Serializable
data class CollectionListRoute(val categoryName: String)

// Écran d'ajout d'un objet
@Serializable
object AddObjectRoute