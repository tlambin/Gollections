package com.pokyx.gollections.domain.usecase

import com.pokyx.gollections.data.CollectionItem
import com.pokyx.gollections.data.repository.ImageProcessorRepository
import com.pokyx.gollections.data.repository.ItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeleteItemUseCase @Inject constructor(
    private val itemRepository: ItemRepository,
    private val imageProcessor: ImageProcessorRepository
) {
    suspend operator fun invoke(item: CollectionItem) = withContext(Dispatchers.IO) {
        itemRepository.deleteItem(item)
        if (item.imageUrl.isNotBlank()) {
            imageProcessor.deleteImageFile(item.imageUrl)
        }
    }
}