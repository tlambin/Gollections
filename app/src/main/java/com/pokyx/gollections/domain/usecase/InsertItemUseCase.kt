package com.pokyx.gollections.domain.usecase

import com.pokyx.gollections.data.CollectionItem
import com.pokyx.gollections.data.ItemProperty
import com.pokyx.gollections.data.repository.ItemRepository
import com.pokyx.gollections.data.tag.CollectionItemTagCrossRef
import com.pokyx.gollections.data.tag.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class InsertItemUseCase @Inject constructor(
    private val itemRepository: ItemRepository
) {
    suspend operator fun invoke(
        item: CollectionItem,
        tags: List<Tag>,
        properties: Map<String, String>
    ) = withContext(Dispatchers.IO) {
        val itemId = itemRepository.insertItem(item).toInt()
        tags.forEach { tag ->
            itemRepository.insertItemTagCrossRef(CollectionItemTagCrossRef(itemId, tag.id))
        }

        // CORRIGÉ : Typage explicite
        val itemProperties: List<ItemProperty> = properties.map { (key, value) ->
            ItemProperty(itemId = itemId, label = key, value = value)
        }
        itemRepository.insertProperties(itemProperties)
    }
}