package com.pokyx.gollections.domain.usecase

import com.pokyx.gollections.data.CollectionItem
import com.pokyx.gollections.data.ItemProperty
import com.pokyx.gollections.data.repository.CollectionRepository
import com.pokyx.gollections.data.tag.CollectionItemTagCrossRef
import com.pokyx.gollections.data.tag.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class InsertItemUseCase @Inject constructor(
    private val repository: CollectionRepository
) {
    suspend operator fun invoke(
        item: CollectionItem,
        tags: List<Tag>,
        properties: Map<String, String>
    ) = withContext(Dispatchers.IO) {

        // 1. Insertion de l'item et récupération de son ID auto-généré
        val itemId = repository.insertItem(item).toInt()

        // 2. Insertion des relations (Tags)
        tags.forEach { tag ->
            repository.insertItemTagCrossRef(CollectionItemTagCrossRef(itemId, tag.id))
        }

        // 3. Insertion des propriétés dynamiques
        val itemProperties = properties.map { (key, value) ->
            ItemProperty(itemId = itemId, label = key, value = value)
        }
        repository.insertItemProperties(itemProperties)
    }
}