package com.pokyx.gollections.domain.usecase

import com.pokyx.gollections.data.Collection
import com.pokyx.gollections.data.CollectionPropertyTemplate
import com.pokyx.gollections.data.repository.CollectionRepository
import com.pokyx.gollections.data.repository.CollectionPropertyTemplateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class InsertCollectionUseCase @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val templateRepository: CollectionPropertyTemplateRepository
) {
    suspend operator fun invoke(name: String, cover: String = "", parentId: Long? = null) {
        withContext(Dispatchers.IO) {
            // 1. On crée la collection vierge
            val newCollection = Collection(name = name, cover = cover, parentId = parentId)
            val collectionId = collectionRepository.insertCollection(newCollection)

            // 2. On prépare nos champs par défaut
            val defaultTemplates = mutableListOf<CollectionPropertyTemplate>()
            val lowerName = name.lowercase()

            // 3. Détection intelligente des types de collection
            if (lowerName.contains("jeu") || lowerName.contains("console") || lowerName.contains("playstation") || lowerName.contains("nintendo")) {
                defaultTemplates.add(CollectionPropertyTemplate(collectionId = collectionId, propertyName = "Plateforme"))
                defaultTemplates.add(CollectionPropertyTemplate(collectionId = collectionId, propertyName = "Éditeur"))
                defaultTemplates.add(CollectionPropertyTemplate(collectionId = collectionId, propertyName = "Année de sortie"))
            }
            else if (lowerName.contains("film") || lowerName.contains("cinéma") || lowerName.contains("dvd") || lowerName.contains("bluray")) {
                defaultTemplates.add(CollectionPropertyTemplate(collectionId = collectionId, propertyName = "Réalisateur"))
                defaultTemplates.add(CollectionPropertyTemplate(collectionId = collectionId, propertyName = "Format (DVD, Blu-Ray, Démat)"))
                defaultTemplates.add(CollectionPropertyTemplate(collectionId = collectionId, propertyName = "Durée (min)"))
            }
            else if (lowerName.contains("musique") || lowerName.contains("vinyle") || lowerName.contains("cd") || lowerName.contains("album")) {
                defaultTemplates.add(CollectionPropertyTemplate(collectionId = collectionId, propertyName = "Artiste"))
                defaultTemplates.add(CollectionPropertyTemplate(collectionId = collectionId, propertyName = "Format (Vinyle, CD, Cassette)"))
                defaultTemplates.add(CollectionPropertyTemplate(collectionId = collectionId, propertyName = "Durée (min)"))
            }
            else if (lowerName.contains("vêtement") || lowerName.contains("habit") || lowerName.contains("sneaker")) {
                defaultTemplates.add(CollectionPropertyTemplate(collectionId = collectionId, propertyName = "Marque"))
                defaultTemplates.add(CollectionPropertyTemplate(collectionId = collectionId, propertyName = "Taille"))
                defaultTemplates.add(CollectionPropertyTemplate(collectionId = collectionId, propertyName = "Couleur principale"))
            }

            // 4. On injecte les modèles dans la base de données
            if (defaultTemplates.isNotEmpty()) {
                templateRepository.insertTemplates(defaultTemplates)
            }
        }
    }
}