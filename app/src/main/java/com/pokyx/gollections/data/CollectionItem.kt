package com.pokyx.gollections.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collection_items")
data class CollectionItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val year: String,
    val category: String,       // Blu-ray, Vinyles, Jeux Vidéo
    val subCategory: String,    // 4K, PS5, Switch, etc.
    val isLoaned: Boolean = false,
    val loanTo: String = ""
)