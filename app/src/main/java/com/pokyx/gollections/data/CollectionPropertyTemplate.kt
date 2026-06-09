package com.pokyx.gollections.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "CollectionPropertyTemplate",
    foreignKeys = [
        ForeignKey(
            entity = Collection::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["collectionId"])]
)
data class CollectionPropertyTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val collectionId: Long,
    val propertyName: String,
    val propertyType: String = "TEXT" // Préparé pour le futur (NUMBER, DATE, BOOLEAN...)
)