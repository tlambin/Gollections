package com.pokyx.gollections.domain.usecase

import com.pokyx.gollections.data.CollectionItem
import com.pokyx.gollections.data.repository.CollectionRepository
import com.pokyx.gollections.data.repository.ImageProcessorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeleteItemUseCase @Inject constructor(
    private val repository: CollectionRepository,
    private val imageProcessor: ImageProcessorRepository
) {
    suspend operator fun invoke(item: CollectionItem) = withContext(Dispatchers.IO) {
        // 1. Suppression de l'objet en base de données
        // Note: Grâce aux Foreign Keys de Room avec "CASCADE", les tags et propriétés associés seront nettoyés automatiquement par SQLite.
        repository.deleteItem(item)

        // 2. Suppression de l'image physique associée pour libérer l'espace de stockage
        if (item.imageUrl.isNotBlank()) {
            imageProcessor.deleteImageFile(item.imageUrl)
        }
    }
}