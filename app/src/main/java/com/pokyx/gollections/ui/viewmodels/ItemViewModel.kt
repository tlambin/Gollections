package com.pokyx.gollections.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokyx.gollections.data.Collection
import com.pokyx.gollections.data.CollectionItem
import com.pokyx.gollections.data.repository.CollectionRepository
import com.pokyx.gollections.data.repository.ImageProcessorRepository
import com.pokyx.gollections.data.tag.CollectionItemTagCrossRef
import com.pokyx.gollections.data.tag.CollectionItemWithTags
import com.pokyx.gollections.data.tag.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ItemFormState(
    val title: String = "",
    val purchaseDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
    val price: String = "",
    val status: String = "",
    val isLoaned: Boolean = false,
    val loanTo: String = "",
    val loanDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
    val imageUrl: String = "",
    val selectedPath: List<Long> = emptyList(),
    val selectedTags: Set<Tag> = emptySet()
)

@HiltViewModel
class ItemViewModel @Inject constructor(
    private val repository: CollectionRepository,
    private val imageProcessor: ImageProcessorRepository
) : ViewModel() {

    val collections = repository.getAllCollections().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _formState = MutableStateFlow(ItemFormState())
    val formState: StateFlow<ItemFormState> = _formState.asStateFlow()

    fun resetFormState(preSelectedCollectionId: Long? = null, collectionsList: List<Collection> = emptyList()) {
        val initialPath = mutableListOf<Long>()
        if (preSelectedCollectionId != null && collectionsList.isNotEmpty()) {
            var curr: Long? = preSelectedCollectionId
            while(curr != null) {
                initialPath.add(0, curr)
                curr = collectionsList.find { it.id == curr }?.parentId
            }
        }
        _formState.value = ItemFormState(selectedPath = initialPath)
    }

    fun loadItemIntoForm(itemWithTags: CollectionItemWithTags, collectionsList: List<Collection>) {
        val currentItem = itemWithTags.item
        val path = mutableListOf<Long>()
        var curr: Long? = currentItem.collectionId
        while (curr != null) {
            path.add(0, curr)
            curr = collectionsList.find { it.id == curr }?.parentId
        }
        _formState.value = ItemFormState(
            title = currentItem.title,
            purchaseDate = currentItem.purchaseDate,
            price = currentItem.price,
            status = currentItem.status,
            isLoaned = currentItem.isLoaned,
            loanTo = currentItem.loanTo,
            loanDate = currentItem.loanDate,
            imageUrl = currentItem.imageUrl,
            selectedPath = path,
            selectedTags = itemWithTags.tags.toSet()
        )
    }

    fun updateForm(transform: (ItemFormState) -> ItemFormState) {
        _formState.update(transform)
    }

    fun insertItemWithTags(item: CollectionItem, tags: List<Tag>) {
        viewModelScope.launch {
            val itemId = repository.insertItem(item).toInt()
            tags.forEach { tag ->
                repository.insertItemTagCrossRef(CollectionItemTagCrossRef(itemId, tag.id))
            }
        }
    }

    fun updateItemWithTags(item: CollectionItem, tags: List<Tag>) {
        viewModelScope.launch {
            repository.updateItem(item)
            repository.clearTagsForItem(item.id)
            tags.forEach { tag ->
                repository.insertItemTagCrossRef(CollectionItemTagCrossRef(item.id, tag.id))
            }
        }
    }

    fun deleteItem(item: CollectionItem) { viewModelScope.launch { repository.deleteItem(item) } }
    fun getItemByIdWithTags(id: Int): Flow<CollectionItemWithTags?> = repository.getItemByIdWithTags(id)

    fun getTagsForCollections(collectionIds: List<Long>): Flow<List<Tag>> = if (collectionIds.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList()) else repository.getTagsByCollectionIds(collectionIds)
    fun insertTag(name: String, collectionId: Long) { viewModelScope.launch { repository.insertTag(Tag(name = name, collectionId = collectionId)) } }

    fun processAndSaveImage(sourceUri: Uri, shouldCutout: Boolean, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val resultUri = imageProcessor.processImage(sourceUri, shouldCutout)
            onResult(resultUri?.toString())
        }
    }
}