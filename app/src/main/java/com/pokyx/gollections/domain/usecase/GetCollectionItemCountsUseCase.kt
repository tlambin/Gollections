package com.pokyx.gollections.domain.usecase

import com.pokyx.gollections.data.repository.CollectionRepository
import com.pokyx.gollections.data.repository.ItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class GetCollectionItemCountsUseCase @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val itemRepository: ItemRepository,
    private val getCollectionDescendantsUseCase: GetCollectionDescendantsUseCase
) {
    operator fun invoke(): Flow<Map<Long, Int>> {
        return combine(
            collectionRepository.getAllCollections(),
            itemRepository.getItemCountsPerCollection()
        ) { allColls, countsList ->
            val flatCounts = countsList.associateBy({ it.collectionId }, { it.count })
            val counts = mutableMapOf<Long, Int>()
            for (collection in allColls) {
                val descendantIds = getCollectionDescendantsUseCase(collection.id, allColls)
                counts[collection.id] = descendantIds.sumOf { flatCounts[it] ?: 0 }
            }
            counts
        }.flowOn(Dispatchers.Default)
    }
}