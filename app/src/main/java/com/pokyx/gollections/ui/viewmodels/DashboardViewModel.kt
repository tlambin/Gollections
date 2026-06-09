package com.pokyx.gollections.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokyx.gollections.data.Collection
import com.pokyx.gollections.data.repository.CollectionRepository
import com.pokyx.gollections.data.repository.ItemRepository
import com.pokyx.gollections.data.repository.ImageProcessorRepository
import com.pokyx.gollections.domain.usecase.GetCollectionItemCountsUseCase
import com.pokyx.gollections.domain.usecase.InsertCollectionUseCase
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
    sealed class Error : ScanEvent() {
        data object LimitReached : Error()
        data object NotFound : Error()
        data class Unknown(val message: String) : Error()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val itemRepository: ItemRepository,
    private val getCollectionItemCountsUseCase: GetCollectionItemCountsUseCase,
    private val insertCollectionUseCase: InsertCollectionUseCase, // NOUVEL INJECT ICI
    private val processImageUseCase: ProcessImageUseCase,
    private val scanBarcodeUseCase: ScanBarcodeUseCase,
    private val imageProcessor: ImageProcessorRepository
) : ViewModel() {

    val collections = collectionRepository.getAllCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rootCollections = collectionRepository.getRootCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalItemsCount = itemRepository.getTotalItemsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val loanedItemsCount = itemRepository.getLoanedItemsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val collectionItemCounts = getCollectionItemCountsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

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

    // MISE À JOUR ICI : On délègue au UseCase pour bénéficier des champs automatiques
    fun insertCollection(name: String, cover: String = "", parentId: Long? = null) {
        viewModelScope.launch {
            insertCollectionUseCase(name = name, cover = cover, parentId = parentId)
        }
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
}