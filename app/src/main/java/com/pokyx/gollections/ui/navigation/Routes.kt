package com.pokyx.gollections.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object DashboardRoute

@Serializable
data class CollectionListRoute(val collectionName: String)

@Serializable
data class ItemDetailRoute(val itemId: Int)

@Serializable
data class AddItemRoute(val preSelectedCollection: String? = null)

@Serializable
data class EditItemRoute(val itemId: Int)