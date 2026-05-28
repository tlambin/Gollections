package com.pokyx.gollections.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collection_items")
data class CollectionItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val collection: String,     // Ex: Blu-ray, Vinyles, Jeux Vidéo (remplace category)
    val category: String,       // Ex: 4K, PS5, Switch (remplace subCategory)

    val isLoaned: Boolean = false,
    val loanTo: String = "",
    val loanDate: String = "",

    val purchaseDate: String = "",
    val price: String = "",
    val status: String = "Non commencé",
    val comment: String = "",
    val imageUrl: String = ""
)