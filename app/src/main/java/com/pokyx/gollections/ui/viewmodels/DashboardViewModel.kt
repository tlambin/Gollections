package com.pokyx.gollections.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokyx.gollections.data.Collection
import com.pokyx.gollections.data.CollectionItemCount
import com.pokyx.gollections.data.repository.CollectionRepository
import com.pokyx.gollections.data.repository.ItemRepository
import com.pokyx.gollections.data.repository.ImageProcessorRepository // NOUVEL IMPORT
import com.pokyx.gollections.domain.usecase.GetCollectionDescendantsUseCase
import com.pokyx.gollections.domain.usecase.ProcessImageUseCase
import com.pokyx.gollections.domain.usecase.ScanBarcodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ScanEvent {
    data object Searching : ScanEvent()
    data class Success(val title: String?, val imageUrl: String?) : ScanEvent()
    data class Error(val message: String) : ScanEvent()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val itemRepository: ItemRepository,
    private val getCollectionDescendantsUseCase: GetCollectionDescendantsUseCase,
    private val processImageUseCase: ProcessImageUseCase,
    private val scanBarcodeUseCase: ScanBarcodeUseCase,
    private val imageProcessor: ImageProcessorRepository // AJOUT ICI
) : ViewModel() {

    val collections = collectionRepository.getAllCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rootCollections = collectionRepository.getRootCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalItemsCount = itemRepository.getTotalItemsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val loanedItemsCount = itemRepository.getLoanedItemsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val itemCountsPerCollection = itemRepository.getItemCountsPerCollection()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<CollectionItemCount>())

    val collectionItemCounts = combine(collections, itemCountsPerCollection) { allColls, countsList ->
        val flatCounts = countsList.associateBy({ it.collectionId }, { it.count })
        val counts = mutableMapOf<Long, Int>()
        for (collection in allColls) {
            val descendantIds = getCollectionDescendantsUseCase(collection.id, allColls)
            counts[collection.id] = descendantIds.sumOf { flatCounts[it] ?: 0 }
        }
        counts
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchedItemsWithTags = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) flowOf(emptyList()) else itemRepository.searchItemsWithTags(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _scanEvent = Channel<ScanEvent>()
    val scanEvent = _scanEvent.receiveAsFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun insertCollection(name: String, cover: String = "", parentId: Long? = null) {
        viewModelScope.launch {
            collectionRepository.insertCollection(Collection(name = name, cover = cover, parentId = parentId))
        }
    }

    // CORRECTION ICI : Chargement du Bitmap en amont
    fun processAndSaveImage(sourceUri: Uri, shouldCutout: Boolean, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val bitmap = imageProcessor.loadScaledBitmap(sourceUri)
            if (bitmap != null) {
                val result = processImageUseCase(bitmap, shouldCutout)
                onResult(result)
            } else {
                onResult(null)
            }
        }
    }

    fun fetchItemFromBarcode(barcode: String) {
        viewModelScope.launch {
            _scanEvent.send(ScanEvent.Searching)
            val result = scanBarcodeUseCase(barcode)
            if (result.title != null) {
                _scanEvent.send(ScanEvent.Success(result.title, result.imageUrl))
            } else {
                _scanEvent.send(ScanEvent.Error(result.errorMsg ?: "error_scan_not_found"))
            }
        }
    }
}