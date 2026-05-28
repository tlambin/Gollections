package com.pokyx.gollections.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val collectionName: String // Lien vers la collection parente (ex: "Blu-ray")
)