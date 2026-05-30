package com.pokyx.gollections.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokyx.gollections.data.Tag
import com.pokyx.gollections.data.TagDao
import com.pokyx.gollections.data.Collection
import com.pokyx.gollections.data.CollectionDao
import com.pokyx.gollections.data.CollectionItem
import com.pokyx.gollections.data.CollectionItemDao
import com.pokyx.gollections.data.repository.ImageProcessorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val collectionItemDao: CollectionItemDao,
    private val collectionDao: CollectionDao,
    private val tagDao: TagDao,
    private val imageProcessor: ImageProcessorRepository // Ajout du Repository
) : ViewModel() {

    val allItems: StateFlow<List<CollectionItem>> = collectionItemDao.getAllItems().stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())
    val collections: StateFlow<List<Collection>> = collectionDao.getAllCollections().stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())
    val rootCollections: StateFlow<List<Collection>> = collectionDao.getRootCollections().stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val searchedItems: StateFlow<List<CollectionItem>> = _searchQuery.flatMapLatest { query -> if (query.isBlank()) collectionItemDao.getAllItems() else collectionItemDao.searchItems(query) }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    fun insertItem(item: CollectionItem) { viewModelScope.launch { collectionItemDao.insertItem(item) } }
    fun updateItem(item: CollectionItem) { viewModelScope.launch { collectionItemDao.updateItem(item) } }
    fun deleteItem(item: CollectionItem) { viewModelScope.launch { collectionItemDao.deleteItem(item) } }
    fun getItemById(id: Int): Flow<CollectionItem?> = collectionItemDao.getItemById(id)
    fun getItemsByCollection(collectionId: Long): Flow<List<CollectionItem>> = collectionItemDao.getItemsByCollection(collectionId)

    fun getSubCollections(parentId: Long): Flow<List<Collection>> = collectionDao.getSubCollections(parentId)
    suspend fun getCollectionById(id: Long): Collection? = collectionDao.getCollectionById(id)

    fun insertCollection(name: String, parentId: Long? = null) { viewModelScope.launch { collectionDao.insertCollection(Collection(name = name, parentId = parentId)) } }
    fun deleteCollection(collectionId: Long) { viewModelScope.launch { collectionDao.deleteCollectionById(collectionId) } }
    fun renameCollection(id: Long, newName: String) { viewModelScope.launch { collectionDao.renameCollection(id, newName) } }
    fun updateCollectionParent(id: Long, newParentId: Long?) { viewModelScope.launch { collectionDao.updateParentId(id, newParentId) } }

    fun getRecursiveItemCount(collectionId: Long, allCollections: List<Collection>, allItems: List<CollectionItem>): Int {
        val descendantIds = mutableListOf(collectionId)
        var currentLevel = allCollections.filter { it.parentId == collectionId }.map { it.id }
        while (currentLevel.isNotEmpty()) { descendantIds.addAll(currentLevel); currentLevel = allCollections.filter { it.parentId in currentLevel }.map { it.id } }
        return allItems.count { it.collectionId in descendantIds }
    }

    fun getValidMoveDestinations(collectionId: Long, allCollections: List<Collection>): List<Collection> {
        val descendantIds = mutableListOf(collectionId)
        var currentLevel = allCollections.filter { it.parentId == collectionId }.map { it.id }
        while (currentLevel.isNotEmpty()) { descendantIds.addAll(currentLevel); currentLevel = allCollections.filter { it.parentId in currentLevel }.map { it.id } }
        return allCollections.filter { it.id !in descendantIds }
    }

    // --- LOGIQUE DES TAGS ---
    fun getTagsForCollections(collectionIds: List<Long>): Flow<List<Tag>> {
        return if (collectionIds.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList()) else tagDao.getTagsByCollectionIds(collectionIds)
    }

    fun insertTag(name: String, collectionId: Long) { viewModelScope.launch { tagDao.insertTag(Tag(name = name, collectionId = collectionId)) } }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            val items = collectionItemDao.getItemsWithTagSync(tag.name)
            items.forEach { item ->
                val newTags = item.tags.split(",").filter { it != tag.name && it.isNotBlank() }.joinToString(",")
                collectionItemDao.updateItem(item.copy(tags = newTags))
            }
            tagDao.deleteTag(tag)
        }
    }

    fun renameTag(collectionId: Long, oldName: String, newName: String) {
        viewModelScope.launch {
            tagDao.renameTag(collectionId, oldName, newName)
            val items = collectionItemDao.getItemsWithTagSync(oldName)
            items.forEach { item ->
                val newTags = item.tags.split(",").map { if (it == oldName) newName else it }.joinToString(",")
                collectionItemDao.updateItem(item.copy(tags = newTags))
            }
        }
    }

    // --- NOUVELLE FONCTION : TRAITEMENT D'IMAGE VIA LE REPOSITORY ---
    fun processAndSaveImage(sourceUri: Uri, shouldCutout: Boolean, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val resultUri = imageProcessor.processImage(sourceUri, shouldCutout)
            onResult(resultUri?.toString())
        }
    }
}