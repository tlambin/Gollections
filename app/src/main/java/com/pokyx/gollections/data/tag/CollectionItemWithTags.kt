package com.pokyx.gollections.data.tag

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.pokyx.gollections.data.CollectionItem
import com.pokyx.gollections.data.ItemProperty

data class CollectionItemWithTags(
    @Embedded val item: CollectionItem,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        // C'est ici qu'il manquait la précision des colonnes pour la table de jointure :
        associateBy = Junction(
            value = CollectionItemTagCrossRef::class,
            parentColumn = "itemId",
            entityColumn = "tagId"
        )
    )
    val tags: List<Tag>,

    @Relation(
        parentColumn = "id",
        entityColumn = "itemId"
    )
    val properties: List<ItemProperty> = emptyList()
)