package com.pokyx.gollections.data.tag

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.pokyx.gollections.data.CollectionItem

data class CollectionItemWithTags(
    @Embedded val item: CollectionItem,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = CollectionItemTagCrossRef::class,
            parentColumn = "itemId",
            entityColumn = "tagId"
        )
    )
    val tags: List<Tag>
)