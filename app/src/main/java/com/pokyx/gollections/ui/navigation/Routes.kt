package com.pokyx.gollections.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object DashboardRoute

@Serializable
data class CollectionListRoute(val collectionId: Long)

@Serializable
data class ItemDetailRoute(val itemId: Int)

@Serializable
data class AddItemRoute(val preSelectedCollectionId: Long? = null)

@Serializable
data class EditItemRoute(val itemId: Int)