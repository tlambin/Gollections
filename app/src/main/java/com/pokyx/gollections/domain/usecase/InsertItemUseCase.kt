package com.pokyx.gollections.domain.usecase

import com.pokyx.gollections.data.CollectionItem
import com.pokyx.gollections.data.repository.ItemRepository
import com.pokyx.gollections.data.tag.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class InsertItemUseCase @Inject constructor(
    private val itemRepository: ItemRepository
) {
    // OPTIMISATION : Ajout du type de retour ": Long" pour rendre le contrat clair pour le ViewModel
    suspend operator fun invoke(
        item: CollectionItem,
        tags: List<Tag>,
        properties: Map<String, String>
    ): Long = withContext(Dispatchers.IO) {
        // On délègue tout à l'opération sécurisée par Transaction du DAO
        itemRepository.insertItemComplete(item, tags, properties)
    }
}