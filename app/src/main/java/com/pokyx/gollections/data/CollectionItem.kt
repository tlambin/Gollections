package com.pokyx.gollections.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "collection_items",
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
data class CollectionItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val collectionId: Long,

    val isLoaned: Boolean = false,
    val loanTo: String = "",
    val loanDate: String = "",

    val purchaseDate: String = "",
    val price: String = "",
    val status: String = "Non commencé",
    val comment: String = "",
    val imageUrl: String = ""
)