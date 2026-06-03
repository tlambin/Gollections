package com.pokyx.gollections.domain.usecase

import com.pokyx.gollections.data.Collection
import javax.inject.Inject

class GetCollectionDescendantsUseCase @Inject constructor() {
    /**
     * Calcule et retourne la liste contenant l'ID de la collection cible
     * ainsi que les IDs de tous ses enfants (et petits-enfants, etc.).
     */
    operator fun invoke(collectionId: Long, allCollections: List<Collection>): List<Long> {
        val descendantIds = mutableListOf(collectionId)
        var currentLevel = allCollections.filter { it.parentId == collectionId }.map { it.id }
        while (currentLevel.isNotEmpty()) {
            descendantIds.addAll(currentLevel)
            currentLevel = allCollections.filter { it.parentId in currentLevel }.map { it.id }
        }
        return descendantIds
    }
}