package com.pokyx.gollections.data.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class CollectionItemWithTags(
    @Embedded
    val item: CollectionItem,

    // Relation Many-to-Many avec table de jointure (Junction)
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = CollectionItemTagCrossRef::class,
            parentColumn = "itemId",
            entityColumn = "tagId"
        )
    )
    val tags: List<Tag>,

    // Relation One-to-Many standard
    @Relation(
        parentColumn = "id",
        entityColumn = "itemId"
    )
    val properties: List<ItemProperty> = emptyList()
)