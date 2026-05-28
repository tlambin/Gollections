package com.pokyx.gollections.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sub_categories")
data class SubCategory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val categoryName: String // Lien vers la catégorie parente (ex: "Blu-ray")
)