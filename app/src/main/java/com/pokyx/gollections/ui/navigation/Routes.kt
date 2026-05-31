package com.pokyx.gollections.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object DashboardRoute

@Serializable
data class CollectionDetailRoute(val collectionId: Long)

@Serializable
data class ItemDetailRoute(val itemId: Int) // <-- RESTAURÉ ICI

@Serializable
data class AddItemRoute(val preSelectedCollectionId: Long? = null,
                        val scannedTitle: String? = null,
                        val scannedImageUrl: String? = null)

@Serializable
data class EditItemRoute(val itemId: Int)