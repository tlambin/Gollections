package com.pokyx.gollections.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "collection_item_tag_cross_ref",
    primaryKeys = ["itemId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionItem::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tagId"])]
)
data class CollectionItemTagCrossRef(
    val itemId: Int,
    val tagId: Int
)