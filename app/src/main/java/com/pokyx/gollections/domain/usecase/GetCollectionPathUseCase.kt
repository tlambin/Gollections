package com.pokyx.gollections.domain.usecase

import com.pokyx.gollections.data.Collection
import javax.inject.Inject

class GetCollectionPathUseCase @Inject constructor() {
    /**
     * Calcule le chemin hiérarchique (fil d'Ariane) d'une collection.
     * Retourne une liste d'IDs de la racine jusqu'à la collection cible.
     */
    operator fun invoke(targetCollectionId: Long?, allCollections: List<Collection>): List<Long> {
        val path = mutableListOf<Long>()
        if (targetCollectionId == null || allCollections.isEmpty()) return path

        var currentId: Long? = targetCollectionId
        while (currentId != null) {
            path.add(0, currentId)
            currentId = allCollections.find { it.id == currentId }?.parentId
        }
        return path
    }
}