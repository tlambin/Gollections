package com.pokyx.gollections.domain.usecase

import com.pokyx.gollections.data.CollectionItem
import com.pokyx.gollections.data.ItemProperty
import com.pokyx.gollections.data.repository.CollectionRepository
import com.pokyx.gollections.data.repository.ImageProcessorRepository
import com.pokyx.gollections.data.tag.CollectionItemTagCrossRef
import com.pokyx.gollections.data.tag.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdateItemUseCase @Inject constructor(
    private val repository: CollectionRepository,
    private val imageProcessor: ImageProcessorRepository
) {
    /**
     * Orchestre la mise à jour complète d'un item, incluant la gestion de son ancienne image,
     * de ses tags et de ses propriétés.
     */
    suspend operator fun invoke(
        item: CollectionItem,
        tags: List<Tag>,
        properties: Map<String, String>
    ) = withContext(Dispatchers.IO) {

        // 1. Gestion de l'image : on supprime l'ancienne si elle a été modifiée
        val oldItem = repository.getItemByIdWithTags(item.id).firstOrNull()?.item
        if (oldItem != null && oldItem.imageUrl != item.imageUrl && oldItem.imageUrl.isNotBlank()) {
            imageProcessor.deleteImageFile(oldItem.imageUrl)
        }

        // 2. Mise à jour des informations principales de l'objet
        repository.updateItem(item)

        // 3. Nettoyage des anciennes relations (Tags et Propriétés)
        repository.clearTagsForItem(item.id)
        repository.clearPropertiesForItem(item.id)

        // 4. Insertion des nouveaux Tags
        tags.forEach { tag ->
            repository.insertItemTagCrossRef(CollectionItemTagCrossRef(item.id, tag.id))
        }

        // 5. Insertion des nouvelles Propriétés
        val itemProperties = properties.map { (key, value) ->
            ItemProperty(itemId = item.id, label = key, value = value)
        }
        repository.insertItemProperties(itemProperties)
    }
}