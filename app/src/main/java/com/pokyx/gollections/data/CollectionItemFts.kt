package com.pokyx.gollections.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = CollectionItem::class)
@Entity(tableName = "collection_items_fts")
data class CollectionItemFts(
    @ColumnInfo(name = "title") val title: String
)