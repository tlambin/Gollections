package com.pokyx.gollections.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "item_properties",
    foreignKeys = [
        ForeignKey(
            entity = CollectionItem::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["itemId"])]
)
data class ItemProperty(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemId: Int,
    val label: String,
    val value: String,
    @ColumnInfo(name = "section_name", defaultValue = "Informations générales")
    val sectionName: String = "Informations générales",
    @ColumnInfo(name = "type", defaultValue = "TEXT") // ✅ NOUVEAU: Type de donnée
    val type: String = "TEXT"
)