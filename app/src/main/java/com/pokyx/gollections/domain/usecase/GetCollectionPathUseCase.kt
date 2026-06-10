package com.pokyx.gollections.domain.usecase

import com.pokyx.gollections.data.model.Collection
import javax.inject.Inject

class GetCollectionPathUseCase @Inject constructor() {

    operator fun invoke(targetCollectionId: Long?, allCollections: List<Collection>): List<Long> {
        if (targetCollectionId == null || allCollections.isEmpty()) return emptyList()

        // Création d'un dictionnaire (ID -> Collection) pour éviter de parcourir la liste en boucle
        val collectionMap = allCollections.associateBy { it.id }

        val path = mutableListOf<Long>()
        var currentId: Long? = targetCollectionId

        while (currentId != null) {
            path.add(0, currentId) // On insère toujours au début pour avoir l'ordre [Racine -> ... -> Cible]
            currentId = collectionMap[currentId]?.parentId
        }

        return path
    }
}