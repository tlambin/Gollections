package com.pokyx.gollections.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokyx.gollections.data.Collection
import com.pokyx.gollections.data.repository.CollectionRepository
import com.pokyx.gollections.data.repository.ItemRepository
import com.pokyx.gollections.data.repository.TagRepository
import com.pokyx.gollections.data.tag.CollectionItemWithTags
import com.pokyx.gollections.data.tag.Tag
import com.pokyx.gollections.domain.usecase.GetCollectionDescendantsUseCase
import com.pokyx.gollections.domain.usecase.DeleteCollectionUseCase
import com.pokyx.gollections.domain.usecase.ProcessImageUseCase
import com.pokyx.gollections.domain.usecase.ScanBarcodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest

@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val itemRepository: ItemRepository,
    private val tagRepository: TagRepository,
    private val getCollectionDescendantsUseCase: GetCollectionDescendantsUseCase,
    private val deleteCollectionUseCase: DeleteCollectionUseCase,
    private val processImageUseCase: ProcessImageUseCase,
    private val scanBarcodeUseCase: ScanBarcodeUseCase
) : ViewModel() {

    val allCollections = collectionRepository.getAllCollections().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getSubCollections(parentId: Long): Flow<List<Collection>> = collectionRepository.getSubCollections(parentId)
    fun getItemsByCollectionWithTags(collectionId: Long): Flow<List<CollectionItemWithTags>> = itemRepository.getItemsByCollectionWithTags(collectionId)
    suspend fun getCollectionById(id: Long): Collection? = collectionRepository.getCollectionById(id)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTotalCount(collectionId: Long): Flow<Int> = allCollections.flatMapLatest { collections ->
        val descendantIds = getCollectionDescendantsUseCase(collectionId, collections)
        if (descendantIds.isEmpty()) flowOf(0) else itemRepository.getItemsCountByCollectionIds(descendantIds)
    }.flowOn(Dispatchers.Default)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTotalValue(collectionId: Long): Flow<Double> = allCollections.flatMapLatest { collections ->
        val descendantIds = getCollectionDescendantsUseCase(collectionId, collections)
        if (descendantIds.isEmpty()) flowOf(0.0) else itemRepository.getItemsTotalValueByCollectionIds(descendantIds)
    }.flowOn(Dispatchers.Default)

    fun insertCollection(name: String, cover: String = "", parentId: Long? = null) { viewModelScope.launch(Dispatchers.IO) { collectionRepository.insertCollection(Collection(name = name, cover = cover, parentId = parentId)) } }
    fun deleteCollection(collectionId: Long) { viewModelScope.launch(Dispatchers.IO) { deleteCollectionUseCase(collectionId, allCollections.value) } }
    fun renameCollection(id: Long, newName: String) { viewModelScope.launch(Dispatchers.IO) { collectionRepository.renameCollection(id, newName) } }
    fun updateCollectionParent(id: Long, newParentId: Long?) { viewModelScope.launch(Dispatchers.IO) { collectionRepository.updateParentId(id, newParentId) } }
    fun updateCollection(collection: Collection) { viewModelScope.launch(Dispatchers.IO) { collectionRepository.updateCollection(collection) } }

    // NOUVEAU : Fonction pour mettre à jour la Collection avec son nom ET son image/URL
    fun updateCollection(collectionId: Long, newName: String, newCover: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val collection = collectionRepository.getCollectionById(collectionId)
            if (collection != null) {
                collectionRepository.updateCollection(collection.copy(name = newName, cover = newCover))
            }
        }
    }

    fun getValidMoveDestinations(collectionId: Long, allCollections: List<Collection>): List<Collection> {
        val descendantIds = getCollectionDescendantsUseCase(collectionId, allCollections)
        return allCollections.filter { it.id !in descendantIds }
    }

    fun getTagsForCollections(collectionIds: List<Long>): Flow<List<Tag>> = if (collectionIds.isEmpty()) flowOf(emptyList()) else tagRepository.getTagsByCollectionIds(collectionIds)
    fun insertTag(name: String, collectionId: Long) { viewModelScope.launch(Dispatchers.IO) { tagRepository.insertTag(Tag(name = name, collectionId = collectionId)) } }
    fun deleteTag(tag: Tag) { viewModelScope.launch(Dispatchers.IO) { tagRepository.deleteTag(tag) } }
    fun renameTag(collectionId: Long, oldName: String, newName: String) { viewModelScope.launch(Dispatchers.IO) { tagRepository.renameTag(collectionId, oldName, newName) } }

    // Utilisation des UseCases
    fun processAndSaveImage(sourceUri: Uri, shouldCutout: Boolean, onResult: (String?) -> Unit) {
        viewModelScope.launch { onResult(processImageUseCase(sourceUri, shouldCutout)) }
    }

    fun fetchItemFromBarcode(barcode: String, onResult: (title: String?, imageUrl: String?, errorMsg: String?) -> Unit) {
        viewModelScope.launch {
            val result = scanBarcodeUseCase(barcode)
            onResult(result.title, result.imageUrl, result.errorMsg)
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    private val _tagFilter = MutableStateFlow("Toutes")
    val tagFilter = _tagFilter.asStateFlow()
    private val _sortOption = MutableStateFlow("NAME_ASC")
    val sortOption = _sortOption.asStateFlow()

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun updateTagFilter(tag: String) { _tagFilter.value = tag }
    fun updateSortOption(option: String) { _sortOption.value = option }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getPagedItems(collectionId: Long): Flow<PagingData<com.pokyx.gollections.data.tag.CollectionItemWithTags>> {
        return combine(_searchQuery, _tagFilter, _sortOption) { query, tag, sort -> Triple(query, tag, sort) }
            .flatMapLatest { (query, tag, sort) -> itemRepository.getPagedItemsWithFilters(collectionId, query, tag, sort) }
            .cachedIn(viewModelScope)
    }
}