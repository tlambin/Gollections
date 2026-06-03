package com.pokyx.gollections.domain.usecase

import com.pokyx.gollections.data.Collection
import com.pokyx.gollections.data.repository.CollectionRepository
import com.pokyx.gollections.data.repository.ImageProcessorRepository
import com.pokyx.gollections.data.repository.ItemRepository
import javax.inject.Inject

class DeleteCollectionUseCase @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val itemRepository: ItemRepository,
    private val imageProcessor: ImageProcessorRepository,
    private val getCollectionDescendantsUseCase: GetCollectionDescendantsUseCase
) {
    /**
     * Supprime une collection, toutes ses sous-collections,
     * et efface physiquement les images associées aux objets sur le téléphone.
     */
    suspend operator fun invoke(collectionId: Long, allCollections: List<Collection>) {
        // 1. Récupérer tous les IDs de la collection courante et de ses enfants
        val descendantIds = getCollectionDescendantsUseCase(collectionId, allCollections)

        // 2. Récupérer tous les objets liés pour traiter leurs images
        val itemsToDelete = itemRepository.getItemsByCollectionIdsSync(descendantIds)

        // 3. Supprimer les fichiers physiques du disque
        itemsToDelete.forEach {
            if (it.item.imageUrl.isNotBlank()) {
                imageProcessor.deleteImageFile(it.item.imageUrl)
            }
        }

        // 4. Supprimer la collection en base de données (Room gérera la suppression en cascade des entités)
        collectionRepository.deleteCollectionById(collectionId)
    }
}