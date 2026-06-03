package com.pokyx.gollections.domain.usecase

import com.pokyx.gollections.data.CollectionItem
import com.pokyx.gollections.data.repository.ItemRepository
import com.pokyx.gollections.data.tag.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdateItemUseCase @Inject constructor(
    private val itemRepository: ItemRepository
) {
    suspend operator fun invoke(
        item: CollectionItem,
        tags: List<Tag>,
        properties: Map<String, String>
    ) = withContext(Dispatchers.IO) {
        // CORRIGÉ : On délègue tout à l'opération sécurisée par Transaction
        itemRepository.updateItemComplete(item, tags, properties)
    }
}