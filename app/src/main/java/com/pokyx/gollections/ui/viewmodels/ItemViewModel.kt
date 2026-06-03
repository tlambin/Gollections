package com.pokyx.gollections.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokyx.gollections.data.Collection
import com.pokyx.gollections.data.CollectionItem
import com.pokyx.gollections.data.ItemType
import com.pokyx.gollections.data.repository.CollectionRepository
import com.pokyx.gollections.data.repository.ImageProcessorRepository
import com.pokyx.gollections.data.repository.ItemRepository
import com.pokyx.gollections.data.repository.TagRepository
import com.pokyx.gollections.data.tag.CollectionItemWithTags
import com.pokyx.gollections.data.tag.Tag
import com.pokyx.gollections.domain.usecase.DeleteItemUseCase
import com.pokyx.gollections.domain.usecase.GetCollectionPathUseCase
import com.pokyx.gollections.domain.usecase.InsertItemUseCase
import com.pokyx.gollections.domain.usecase.UpdateItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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

object PropertyKeys {
    const val DIRECTOR = "prop_director"
    const val RELEASE_DATE = "prop_release_date"
    const val SYNOPSIS = "prop_synopsis"
    const val AUTHOR = "prop_author"
    const val PUBLICATION_DATE = "prop_publication_date"
    const val SUMMARY = "prop_summary"
    const val PAGE_COUNT = "prop_page_count"
    const val STUDIO = "prop_studio"
    const val PLATFORM = "prop_platform"
    const val DESCRIPTION = "prop_description"
    const val ARTIST = "prop_artist"
    const val ALBUM = "prop_album"
}

// CORRECTION : Les dates ne sont plus figées à l'instanciation de la classe
data class ItemFormState(
    val title: String = "",
    val purchaseDate: String = "",
    val price: String = "",
    val status: String = "",
    val isLoaned: Boolean = false,
    val loanTo: String = "",
    val loanDate: String = "",
    val imageUrl: String = "",
    val selectedPath: List<Long> = emptyList(),
    val selectedTags: Set<Tag> = emptySet(),
    val itemType: ItemType = ItemType.OTHER,
    val properties: Map<String, String> = emptyMap()
)

@HiltViewModel
class ItemViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val itemRepository: ItemRepository,
    private val tagRepository: TagRepository,
    private val imageProcessor: ImageProcessorRepository,
    private val insertItemUseCase: InsertItemUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val getCollectionPathUseCase: GetCollectionPathUseCase // NOUVELLE INJECTION
) : ViewModel() {

    val collections = collectionRepository.getAllCollections().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _formState = MutableStateFlow(ItemFormState())
    val formState: StateFlow<ItemFormState> = _formState.asStateFlow()

    fun resetFormState(
        preSelectedCollectionId: Long? = null,
        collectionsList: List<Collection> = emptyList(),
        scannedTitle: String? = null,
        scannedImageUrl: String? = null
    ) {
        // CORRECTION : Délégation de l'algorithme de calcul du chemin au Use Case
        val initialPath = getCollectionPathUseCase(preSelectedCollectionId, collectionsList)

        // CORRECTION : La date est calculée à l'instant T où le formulaire est réinitialisé
        val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

        _formState.value = ItemFormState(
            selectedPath = initialPath,
            title = scannedTitle ?: "",
            imageUrl = scannedImageUrl ?: "",
            purchaseDate = currentDate,
            loanDate = currentDate
        )
    }

    fun loadItemIntoForm(itemWithTags: CollectionItemWithTags, collectionsList: List<Collection>) {
        val currentItem = itemWithTags.item

        // CORRECTION : Délégation de l'algorithme au Use Case
        val path = getCollectionPathUseCase(currentItem.collectionId, collectionsList)

        val propsMap = itemWithTags.properties.associate { it.label to it.value }

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
            selectedTags = itemWithTags.tags.toSet(),
            itemType = currentItem.itemType,
            properties = propsMap
        )
    }

    fun updateForm(transform: (ItemFormState) -> ItemFormState) {
        _formState.update(transform)
    }

    fun changeItemType(newType: ItemType) {
        val defaultProps = when (newType) {
            ItemType.MOVIE -> mapOf(PropertyKeys.DIRECTOR to "", PropertyKeys.RELEASE_DATE to "", PropertyKeys.SYNOPSIS to "")
            ItemType.BOOK -> mapOf(PropertyKeys.AUTHOR to "", PropertyKeys.PUBLICATION_DATE to "", PropertyKeys.SUMMARY to "", PropertyKeys.PAGE_COUNT to "")
            ItemType.GAME -> mapOf(PropertyKeys.STUDIO to "", PropertyKeys.PLATFORM to "", PropertyKeys.RELEASE_DATE to "", PropertyKeys.DESCRIPTION to "")
            ItemType.MUSIC -> mapOf(PropertyKeys.ARTIST to "", PropertyKeys.ALBUM to "", PropertyKeys.RELEASE_DATE to "")
            ItemType.OTHER -> emptyMap()
        }
        updateForm { it.copy(itemType = newType, properties = defaultProps) }
    }

    fun updateProperty(label: String, value: String) {
        updateForm { it.copy(properties = it.properties + (label to value)) }
    }

    fun insertItemWithTags(item: CollectionItem, tags: List<Tag>, properties: Map<String, String>) {
        viewModelScope.launch {
            insertItemUseCase(item, tags, properties)
        }
    }

    fun updateItemWithTags(item: CollectionItem, tags: List<Tag>, properties: Map<String, String>) {
        viewModelScope.launch {
            updateItemUseCase(item, tags, properties)
        }
    }

    fun deleteItem(item: CollectionItem) {
        viewModelScope.launch {
            deleteItemUseCase(item)
        }
    }

    fun getItemByIdWithTags(id: Int): Flow<CollectionItemWithTags?> = itemRepository.getItemByIdWithTags(id)

    fun getTagsForCollections(collectionIds: List<Long>): Flow<List<Tag>> = if (collectionIds.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList()) else tagRepository.getTagsByCollectionIds(collectionIds)

    fun insertTag(name: String, collectionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            tagRepository.insertTag(Tag(name = name, collectionId = collectionId))
        }
    }

    fun processAndSaveImage(sourceUri: Uri, shouldCutout: Boolean, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val resultUri = imageProcessor.processImage(sourceUri, shouldCutout)
            onResult(resultUri?.toString())
        }
    }
}