package com.pokyx.gollections.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.pokyx.gollections.data.Collection
import com.pokyx.gollections.data.repository.CollectionRepository
import com.pokyx.gollections.data.repository.ItemRepository
import com.pokyx.gollections.data.repository.TagRepository
import com.pokyx.gollections.data.repository.ImageProcessorRepository
import com.pokyx.gollections.data.tag.CollectionItemWithTags
import com.pokyx.gollections.data.tag.Tag
import com.pokyx.gollections.domain.usecase.DeleteCollectionUseCase
import com.pokyx.gollections.domain.usecase.GetCollectionDescendantsUseCase
import com.pokyx.gollections.domain.usecase.InsertCollectionUseCase
import com.pokyx.gollections.domain.usecase.ProcessImageUseCase
import com.pokyx.gollections.domain.usecase.ScanBarcodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val itemRepository: ItemRepository,
    private val tagRepository: TagRepository,
    private val getCollectionDescendantsUseCase: GetCollectionDescendantsUseCase,
    private val deleteCollectionUseCase: DeleteCollectionUseCase,
    private val insertCollectionUseCase: InsertCollectionUseCase, // NOUVEL INJECT ICI
    private val processImageUseCase: ProcessImageUseCase,
    private val scanBarcodeUseCase: ScanBarcodeUseCase,
    private val imageProcessor: ImageProcessorRepository
) : ViewModel() {

    val allCollections = collectionRepository.getAllCollections().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getSubCollections(parentId: Long): Flow<List<Collection>> = collectionRepository.getSubCollections(parentId)
    fun getItemsByCollectionWithTags(collectionId: Long): Flow<List<CollectionItemWithTags>> = itemRepository.getItemsByCollectionWithTags(collectionId)
    suspend fun getCollectionById(id: Long): Collection? = collectionRepository.getCollectionById(id)

    fun getTotalCount(collectionId: Long): Flow<Int> {
        return itemRepository.getTotalCountRecursive(collectionId).flowOn(Dispatchers.IO)
    }

    fun getTotalValue(collectionId: Long): Flow<Double> {
        return itemRepository.getTotalValueRecursive(collectionId)
            .map { it ?: 0.0 }
            .flowOn(Dispatchers.IO)
    }

    // MISE À JOUR ICI : On délègue au UseCase
    fun insertCollection(name: String, cover: String = "", parentId: Long? = null) {
        viewModelScope.launch {
            insertCollectionUseCase(name = name, cover = cover, parentId = parentId)
        }
    }

    fun deleteCollection(collectionId: Long) {
        viewModelScope.launch { deleteCollectionUseCase(collectionId, allCollections.value) }
    }

    fun updateCollectionParent(id: Long, newParentId: Long?) {
        viewModelScope.launch { collectionRepository.updateParentId(id, newParentId) }
    }

    fun updateCollection(collection: Collection) {
        viewModelScope.launch { collectionRepository.updateCollection(collection) }
    }

    fun updateCollection(collectionId: Long, newName: String, newCover: String) {
        viewModelScope.launch {
            collectionRepository.updateCollectionDetails(collectionId, newName, newCover)
        }
    }

    fun getValidMoveDestinations(collectionId: Long, allCollections: List<Collection>): List<Collection> {
        val descendantIds = getCollectionDescendantsUseCase(collectionId, allCollections)
        return allCollections.filter { it.id !in descendantIds }
    }

    fun getTagsForCollections(collectionIds: List<Long>): Flow<List<Tag>> = if (collectionIds.isEmpty()) flowOf(emptyList()) else tagRepository.getTagsByCollectionIds(collectionIds)

    fun insertTag(name: String, collectionId: Long) {
        viewModelScope.launch { tagRepository.insertTag(Tag(name = name, collectionId = collectionId)) }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch { tagRepository.deleteTag(tag) }
    }

    fun renameTag(collectionId: Long, oldName: String, newName: String) {
        viewModelScope.launch { tagRepository.renameTag(collectionId, oldName, newName) }
    }

    suspend fun loadBitmap(uri: Uri): android.graphics.Bitmap? {
        return imageProcessor.loadScaledBitmap(uri)
    }

    fun processAndSaveBitmap(bitmap: android.graphics.Bitmap, shouldCutout: Boolean, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val resultUri = imageProcessor.processAndSaveBitmap(bitmap, shouldCutout)
            onResult(resultUri?.toString())
        }
    }

    private val _scanEvent = Channel<ScanEvent>()
    val scanEvent = _scanEvent.receiveAsFlow()

    fun fetchItemFromBarcode(barcode: String) {
        viewModelScope.launch {
            _scanEvent.send(ScanEvent.Searching)
            val result = scanBarcodeUseCase(barcode)
            if (result.title != null) {
                _scanEvent.send(ScanEvent.Success(result.title, result.imageUrl))
            } else {
                val errorEvent = when (result.errorMsg) {
                    "error_scan_limit" -> ScanEvent.Error.LimitReached
                    "error_scan_not_found" -> ScanEvent.Error.NotFound
                    else -> ScanEvent.Error.Unknown(result.errorMsg ?: "Erreur inconnue")
                }
                _scanEvent.send(errorEvent)
            }
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
    fun getPagedItems(collectionId: Long): Flow<PagingData<CollectionItemWithTags>> {
        return combine(_searchQuery, _tagFilter, _sortOption) { query, tag, sort -> Triple(query, tag, sort) }
            .flatMapLatest { (query, tag, sort) -> itemRepository.getPagedItemsWithFilters(collectionId, query, tag, sort) }
            .cachedIn(viewModelScope)
    }
}