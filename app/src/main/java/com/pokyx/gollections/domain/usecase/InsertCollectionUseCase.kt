package com.pokyx.gollections.domain.usecase

import com.pokyx.gollections.data.model.Collection
import com.pokyx.gollections.data.model.CollectionPropertyTemplate
import com.pokyx.gollections.data.repository.CollectionPropertyTemplateRepository
import com.pokyx.gollections.data.repository.CollectionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Énumération définissant les grands types de collections supportés par l'application.
 */
enum class CollectionCategoryType {
    GAMES, FILMS, MUSIC, CLOTHING, CUSTOM
}

class InsertCollectionUseCase @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val templateRepository: CollectionPropertyTemplateRepository
) {
    suspend operator fun invoke(
        name: String,
        categoryType: CollectionCategoryType,
        cover: String = "",
        parentId: Long? = null
    ) {
        withContext(Dispatchers.IO) {
            // 1. On crée la collection vierge
            val newCollection = Collection(name = name, cover = cover, parentId = parentId)
            val collectionId = collectionRepository.insertCollection(newCollection)

            // 2. On prépare nos champs par défaut en fonction du type strict
            val defaultTemplates = when (categoryType) {
                CollectionCategoryType.GAMES -> listOf(
                    CollectionPropertyTemplate(collectionId = collectionId, propertyName = "Plateforme"),
                    CollectionPropertyTemplate(collectionId = collectionId, propertyName = "Éditeur"),
                    CollectionPropertyTemplate(collectionId = collectionId, propertyName = "Année de sortie")
                )
                CollectionCategoryType.FILMS -> listOf(
                    CollectionPropertyTemplate(collectionId = collectionId, propertyName = "Réalisateur"),
                    CollectionPropertyTemplate(collectionId = collectionId, propertyName = "Format (DVD, Blu-Ray, Démat)"),
                    CollectionPropertyTemplate(collectionId = collectionId, propertyName = "Durée (min)")
                )
                CollectionCategoryType.MUSIC -> listOf(
                    CollectionPropertyTemplate(collectionId = collectionId, propertyName = "Artiste"),
                    CollectionPropertyTemplate(collectionId = collectionId, propertyName = "Format (Vinyle, CD, Cassette)"),
                    CollectionPropertyTemplate(collectionId = collectionId, propertyName = "Durée (min)")
                )
                CollectionCategoryType.CLOTHING -> listOf(
                    CollectionPropertyTemplate(collectionId = collectionId, propertyName = "Marque"),
                    CollectionPropertyTemplate(collectionId = collectionId, propertyName = "Taille"),
                    CollectionPropertyTemplate(collectionId = collectionId, propertyName = "Couleur principale")
                )
                CollectionCategoryType.CUSTOM -> emptyList()
            }

            // 3. On injecte les modèles dans la base de données
            if (defaultTemplates.isNotEmpty()) {
                templateRepository.insertTemplates(defaultTemplates)
            }
        }
    }
}