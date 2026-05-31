package com.pokyx.gollections.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokyx.gollections.data.Collection
import com.pokyx.gollections.data.repository.CollectionRepository
import com.pokyx.gollections.data.repository.ImageProcessorRepository
import com.pokyx.gollections.data.tag.CollectionItemWithTags
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.pokyx.gollections.data.repository.BarcodeRepository

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: CollectionRepository,
    private val imageProcessor: ImageProcessorRepository,
    private val barcodeRepository: BarcodeRepository
) : ViewModel() {

    val allItemsWithTags = repository.getAllItemsWithTags().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val collections = repository.getAllCollections().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val rootCollections = repository.getRootCollections().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchedItemsWithTags = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) repository.getAllItemsWithTags() else repository.searchItemsWithTags(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    fun insertCollection(name: String, cover: String = "", parentId: Long? = null) {
        viewModelScope.launch { repository.insertCollection(Collection(name = name, cover = cover, parentId = parentId)) }
    }

    fun getRecursiveItemCount(collectionId: Long, allCollections: List<Collection>, allItems: List<CollectionItemWithTags>): Int {
        val descendantIds = mutableListOf(collectionId)
        var currentLevel = allCollections.filter { it.parentId == collectionId }.map { it.id }
        while (currentLevel.isNotEmpty()) { descendantIds.addAll(currentLevel); currentLevel = allCollections.filter { it.parentId in currentLevel }.map { it.id } }
        return allItems.count { it.item.collectionId in descendantIds }
    }

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