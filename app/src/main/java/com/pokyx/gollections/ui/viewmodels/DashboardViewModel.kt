package com.pokyx.gollections.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokyx.gollections.data.repository.CollectionRepository
import com.pokyx.gollections.data.repository.ItemRepository
import com.pokyx.gollections.data.repository.ImageProcessorRepository
import com.pokyx.gollections.domain.usecase.CollectionCategoryType
import com.pokyx.gollections.domain.usecase.GetCollectionItemCountsUseCase
import com.pokyx.gollections.domain.usecase.InsertCollectionUseCase
import com.pokyx.gollections.domain.usecase.ProcessImageUseCase
import com.pokyx.gollections.domain.usecase.ScanBarcodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val itemRepository: ItemRepository,
    private val getCollectionItemCountsUseCase: GetCollectionItemCountsUseCase,
    private val insertCollectionUseCase: InsertCollectionUseCase,
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

    // OPTIMISATION : Application du Debounce pour soulager la base de données
    val searchedItemsWithTags = _searchQuery
        .debounce(300) // Attend 300ms après la dernière frappe
        .distinctUntilChanged() // Ignore si le texte est identique au précédent
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList()) else itemRepository.searchItemsWithTags(query)
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _scanEvent = Channel<ScanEvent>()
    val scanEvent = _scanEvent.receiveAsFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // MISE À JOUR : On transfère le UseCase vers le nouveau système typé.
    // L'analyse textuelle est maintenue ici uniquement comme solution de repli (Fallback)
    // en attendant que l'UI intègre un sélecteur de catégorie.
    fun insertCollection(name: String, cover: String = "", parentId: Long? = null) {
        viewModelScope.launch {
            val lowerName = name.lowercase()
            val inferredCategory = when {
                lowerName.contains("jeu") || lowerName.contains("console") || lowerName.contains("playstation") -> CollectionCategoryType.GAMES
                lowerName.contains("film") || lowerName.contains("cinéma") || lowerName.contains("dvd") -> CollectionCategoryType.FILMS
                lowerName.contains("musique") || lowerName.contains("vinyle") || lowerName.contains("cd") -> CollectionCategoryType.MUSIC
                lowerName.contains("vêtement") || lowerName.contains("habit") || lowerName.contains("sneaker") -> CollectionCategoryType.CLOTHING
                else -> CollectionCategoryType.CUSTOM
            }

            insertCollectionUseCase(name = name, categoryType = inferredCategory, cover = cover, parentId = parentId)
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