package com.pokyx.gollections.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokyx.gollections.data.Collection
import com.pokyx.gollections.data.repository.CollectionRepository
import com.pokyx.gollections.data.repository.ImageProcessorRepository
import com.pokyx.gollections.data.tag.CollectionItemWithTags
import com.pokyx.gollections.data.tag.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.pokyx.gollections.data.repository.BarcodeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    private val repository: CollectionRepository,
    private val imageProcessor: ImageProcessorRepository,
    private val barcodeRepository: BarcodeRepository
) : ViewModel() {

    val allCollections = repository.getAllCollections().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allItemsWithTags = repository.getAllItemsWithTags().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getSubCollections(parentId: Long): Flow<List<Collection>> = repository.getSubCollections(parentId)
    fun getItemsByCollectionWithTags(collectionId: Long): Flow<List<CollectionItemWithTags>> = repository.getItemsByCollectionWithTags(collectionId)
    suspend fun getCollectionById(id: Long): Collection? = repository.getCollectionById(id)

    // NOUVEAU : Calculs déportés en arrière-plan (Dispatchers.Default)
    fun getTotalCount(collectionId: Long): Flow<Int> = combine(allCollections, allItemsWithTags) { collections, items ->
        val descendantIds = mutableListOf(collectionId)
        var currentLevel = collections.filter { it.parentId == collectionId }.map { it.id }
        while (currentLevel.isNotEmpty()) { descendantIds.addAll(currentLevel); currentLevel = collections.filter { it.parentId in currentLevel }.map { it.id } }
        items.count { it.item.collectionId in descendantIds }
    }.flowOn(Dispatchers.Default)

    fun getTotalValue(collectionId: Long): Flow<Double> = combine(allCollections, allItemsWithTags) { collections, items ->
        val descendantIds = mutableListOf(collectionId)
        var currentLevel = collections.filter { it.parentId == collectionId }.map { it.id }
        while (currentLevel.isNotEmpty()) {
            descendantIds.addAll(currentLevel)
            currentLevel = collections.filter { it.parentId in currentLevel }.map { it.id }
        }
        items
            .filter { it.item.collectionId in descendantIds }
            .mapNotNull { it.item.price.replace(",", ".").toDoubleOrNull() }
            .sum()
    }.flowOn(Dispatchers.Default)

    fun insertCollection(name: String, cover: String = "", parentId: Long? = null) { viewModelScope.launch { repository.insertCollection(Collection(name = name, cover = cover, parentId = parentId)) } }

    // MODIFIE : Pour supprimer aussi les images associées si on supprime toute la collection
    fun deleteCollection(collectionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            // Purger les fichiers images avant de détruire la base
            val descendantIds = mutableListOf(collectionId)
            var currentLevel = allCollections.value.filter { it.parentId == collectionId }.map { it.id }
            while (currentLevel.isNotEmpty()) { descendantIds.addAll(currentLevel); currentLevel = allCollections.value.filter { it.parentId in currentLevel }.map { it.id } }

            val itemsToDelete = allItemsWithTags.value.filter { it.item.collectionId in descendantIds }
            itemsToDelete.forEach {
                if (it.item.imageUrl.isNotBlank()) imageProcessor.deleteImageFile(it.item.imageUrl)
            }

            repository.deleteCollectionById(collectionId)
        }
    }

    fun renameCollection(id: Long, newName: String) { viewModelScope.launch { repository.renameCollection(id, newName) } }
    fun updateCollectionParent(id: Long, newParentId: Long?) { viewModelScope.launch { repository.updateParentId(id, newParentId) } }
    fun updateCollection(collection: Collection) { viewModelScope.launch { repository.updateCollection(collection) } }

    fun getValidMoveDestinations(collectionId: Long, allCollections: List<Collection>): List<Collection> {
        val descendantIds = mutableListOf(collectionId)
        var currentLevel = allCollections.filter { it.parentId == collectionId }.map { it.id }
        while (currentLevel.isNotEmpty()) { descendantIds.addAll(currentLevel); currentLevel = allCollections.filter { it.parentId in currentLevel }.map { it.id } }
        return allCollections.filter { it.id !in descendantIds }
    }

    fun getTagsForCollections(collectionIds: List<Long>): Flow<List<Tag>> = if (collectionIds.isEmpty()) flowOf(emptyList()) else repository.getTagsByCollectionIds(collectionIds)
    fun insertTag(name: String, collectionId: Long) { viewModelScope.launch { repository.insertTag(Tag(name = name, collectionId = collectionId)) } }
    fun deleteTag(tag: Tag) { viewModelScope.launch { repository.deleteTag(tag) } }
    fun renameTag(collectionId: Long, oldName: String, newName: String) { viewModelScope.launch { repository.renameTag(collectionId, oldName, newName) } }

    fun processAndSaveImage(sourceUri: Uri, shouldCutout: Boolean, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val resultUri = imageProcessor.processImage(sourceUri, shouldCutout)
            onResult(resultUri?.toString())
        }
    }

    fun fetchItemFromBarcode(barcode: String, onResult: (title: String?, imageUrl: String?, errorMsg: String?) -> Unit) {
        viewModelScope.launch {
            val result = barcodeRepository.getInfoFromBarcode(barcode)
            if (result.isSuccess) {
                val info = result.getOrNull()
                onResult(info?.title, info?.imageUrl, null)
            } else {
                val exceptionMsg = result.exceptionOrNull()?.message
                onResult(null, null, exceptionMsg)
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
    fun getPagedItems(collectionId: Long): Flow<PagingData<com.pokyx.gollections.data.tag.CollectionItemWithTags>> {
        return combine(_searchQuery, _tagFilter, _sortOption) { query, tag, sort ->
            Triple(query, tag, sort)
        }.flatMapLatest { (query, tag, sort) ->
            repository.getPagedItemsWithFilters(collectionId, query, tag, sort)
        }.cachedIn(viewModelScope)
    }
}