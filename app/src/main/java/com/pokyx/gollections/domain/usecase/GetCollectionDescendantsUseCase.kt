package com.pokyx.gollections.domain.usecase

import com.pokyx.gollections.data.model.Collection
import javax.inject.Inject

class GetCollectionDescendantsUseCase @Inject constructor() {
    /**
     * Calcule et retourne la liste contenant l'ID de la collection cible
     * ainsi que les IDs de tous ses enfants (et petits-enfants, etc.).
     * Intègre une sécurité contre les boucles infinies.
     */
    operator fun invoke(collectionId: Long, allCollections: List<Collection>): List<Long> {
        // OPTIMISATION 1 : Création d'un dictionnaire pour un accès instantané aux enfants
        // Cela transforme une complexité algorithmique exponentielle en une complexité linéaire.
        val childrenByParent = allCollections.groupBy { it.parentId }

        val descendantIds = mutableListOf<Long>()
        val toVisit = ArrayDeque<Long>()
        toVisit.add(collectionId)

        // OPTIMISATION 2 : Registre des visites pour bloquer les références circulaires
        val visited = mutableSetOf<Long>()

        while (toVisit.isNotEmpty()) {
            val currentId = toVisit.removeFirst()

            // Sécurité absolue : si on a déjà traité cet ID, on l'ignore (anti-boucle infinie)
            if (!visited.add(currentId)) continue

            descendantIds.add(currentId)

            // On récupère instantanément les enfants sans recompter toute la liste
            val children = childrenByParent[currentId]?.map { it.id } ?: emptyList()
            toVisit.addAll(children)
        }

        return descendantIds
    }
}