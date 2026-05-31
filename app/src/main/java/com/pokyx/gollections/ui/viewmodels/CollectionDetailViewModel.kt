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

    fun insertCollection(name: String, cover: String = "", parentId: Long? = null) { viewModelScope.launch { repository.insertCollection(Collection(name = name, cover = cover, parentId = parentId)) } }
    fun deleteCollection(collectionId: Long) { viewModelScope.launch { repository.deleteCollectionById(collectionId) } }
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

    fun fetchItemFromBarcode(barcode: String, onResult: (title: String?, imageUrl: String?) -> Unit) {
        viewModelScope.launch {
            val result = barcodeRepository.getInfoFromBarcode(barcode)
            val info = result.getOrNull()
            onResult(info?.title, info?.imageUrl)
        }
    }
}