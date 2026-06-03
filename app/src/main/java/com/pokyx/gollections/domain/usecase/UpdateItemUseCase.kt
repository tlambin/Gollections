package com.pokyx.gollections.domain.usecase

import com.pokyx.gollections.data.CollectionItem
import com.pokyx.gollections.data.ItemProperty
import com.pokyx.gollections.data.repository.ImageProcessorRepository
import com.pokyx.gollections.data.repository.ItemRepository
import com.pokyx.gollections.data.tag.CollectionItemTagCrossRef
import com.pokyx.gollections.data.tag.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdateItemUseCase @Inject constructor(
    private val itemRepository: ItemRepository,
    private val imageProcessor: ImageProcessorRepository
) {
    suspend operator fun invoke(
        item: CollectionItem,
        tags: List<Tag>,
        properties: Map<String, String>
    ) = withContext(Dispatchers.IO) {
        val oldItem = itemRepository.getItemByIdWithTags(item.id).firstOrNull()?.item
        if (oldItem != null && oldItem.imageUrl != item.imageUrl && oldItem.imageUrl.isNotBlank()) {
            imageProcessor.deleteImageFile(oldItem.imageUrl)
        }

        itemRepository.updateItem(item)
        itemRepository.clearTagsForItem(item.id)
        itemRepository.clearPropertiesForItem(item.id)

        tags.forEach { tag ->
            itemRepository.insertItemTagCrossRef(CollectionItemTagCrossRef(item.id, tag.id))
        }

        // CORRIGÉ : Typage explicite
        val itemProperties: List<ItemProperty> = properties.map { (key, value) ->
            ItemProperty(itemId = item.id, label = key, value = value)
        }
        itemRepository.insertProperties(itemProperties)
    }
}