package com.pokyx.gollections.data.tag

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pokyx.gollections.data.Collection

@Entity(
    tableName = "Tag",
    foreignKeys = [
        ForeignKey(
            entity = Collection::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["collectionId"]),
        Index(value = ["name", "collectionId"], unique = true)
    ]
)
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val collectionId: Long
)