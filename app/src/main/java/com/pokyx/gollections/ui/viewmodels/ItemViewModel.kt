package com.pokyx.gollections.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
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

enum class ItemPropertyKey(val value: String, val isMultiLine: Boolean = false) {
    DIRECTOR("prop_director"),
    RELEASE_DATE("prop_release_date"),
    SYNOPSIS("prop_synopsis", isMultiLine = true),
    AUTHOR("prop_author"),
    PUBLICATION_DATE("prop_publication_date"),
    SUMMARY("prop_summary", isMultiLine = true),
    PAGE_COUNT("prop_page_count"),
    STUDIO("prop_studio"),
    PLATFORM("prop_platform"),
    DESCRIPTION("prop_description", isMultiLine = true),
    ARTIST("prop_artist"),
    ALBUM("prop_album");

    companion object {
        fun fromValue(value: String): ItemPropertyKey? = values().find { it.value == value }
    }
}

data class ItemFormState(
    val title: String = "",
    val itemType: ItemType = ItemType.OTHER,
    val selectedPath: List<Long> = emptyList(),
    val purchaseDate: String = "",
    val price: String = "",
    val imageUrl: String = "",
    val isLoaned: Boolean = false,
    val loanTo: String = "",
    val loanDate: String = "",
    val status: String = "Non commencé",
    val selectedTags: Set<Tag> = emptySet(),
    val properties: Map<ItemPropertyKey, String> = emptyMap()
)

@HiltViewModel
class ItemViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val collectionRepository: CollectionRepository,
    private val itemRepository: ItemRepository,
    private val tagRepository: TagRepository,
    private val imageProcessor: ImageProcessorRepository,
    private val insertItemUseCase: InsertItemUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val getCollectionPathUseCase: GetCollectionPathUseCase
) : ViewModel() {

    private val gson = Gson()

    val collections = collectionRepository.getAllCollections().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _formState = MutableStateFlow(
        savedStateHandle.get<String>("form_state_json")?.let {
            try {
                gson.fromJson(it, ItemFormState::class.java)
            } catch (e: Exception) {
                ItemFormState()
            }
        } ?: ItemFormState()
    )
    val formState: StateFlow<ItemFormState> = _formState.asStateFlow()

    fun updateForm(transform: (ItemFormState) -> ItemFormState) {
        _formState.update { oldState ->
            val newState = transform(oldState)
            savedStateHandle["form_state_json"] = gson.toJson(newState)
            newState
        }
    }

    fun resetFormState(
        preSelectedCollectionId: Long? = null,
        collectionsList: List<Collection> = emptyList(),
        scannedTitle: String? = null,
        scannedImageUrl: String? = null
    ) {
        val initialPath = getCollectionPathUseCase(preSelectedCollectionId, collectionsList)
        val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

        updateForm {
            ItemFormState(
                selectedPath = initialPath,
                title = scannedTitle ?: "",
                imageUrl = scannedImageUrl ?: "",
                purchaseDate = currentDate,
                loanDate = currentDate
            )
        }
    }

    fun loadItemIntoForm(itemWithTags: CollectionItemWithTags, collectionsList: List<Collection>) {
        val item = itemWithTags.item
        val priceString = if (item.price > 0.0) item.price.toString().replace(".", ",") else ""
        val path = com.pokyx.gollections.utils.buildPathBottomUp(item.collectionId, collectionsList)

        val mappedProperties = itemWithTags.properties.mapNotNull { prop ->
            ItemPropertyKey.fromValue(prop.label)?.let { it to prop.value }
        }.toMap()

        updateForm { oldState ->
            oldState.copy(
                title = item.title,
                price = priceString,
                purchaseDate = item.purchaseDate,
                imageUrl = item.imageUrl,
                status = item.status,
                isLoaned = item.isLoaned,
                loanTo = item.loanTo,
                loanDate = item.loanDate,
                itemType = item.itemType,
                selectedPath = path,
                selectedTags = itemWithTags.tags.toSet(),
                properties = mappedProperties
            )
        }
    }

    fun changeItemType(newType: ItemType) {
        val defaultProps = when (newType) {
            ItemType.MOVIE -> mapOf(ItemPropertyKey.DIRECTOR to "", ItemPropertyKey.RELEASE_DATE to "", ItemPropertyKey.SYNOPSIS to "")
            ItemType.BOOK -> mapOf(ItemPropertyKey.AUTHOR to "", ItemPropertyKey.PUBLICATION_DATE to "", ItemPropertyKey.SUMMARY to "", ItemPropertyKey.PAGE_COUNT to "")
            ItemType.GAME -> mapOf(ItemPropertyKey.STUDIO to "", ItemPropertyKey.PLATFORM to "", ItemPropertyKey.RELEASE_DATE to "", ItemPropertyKey.DESCRIPTION to "")
            ItemType.MUSIC -> mapOf(ItemPropertyKey.ARTIST to "", ItemPropertyKey.ALBUM to "", ItemPropertyKey.RELEASE_DATE to "")
            ItemType.OTHER -> emptyMap()
        }
        updateForm { it.copy(itemType = newType, properties = defaultProps) }
    }

    fun updateProperty(key: ItemPropertyKey, value: String) {
        updateForm { it.copy(properties = it.properties + (key to value)) }
    }

    fun insertItemWithTags(item: CollectionItem, tags: List<Tag>, properties: Map<String, String>) {
        viewModelScope.launch { insertItemUseCase(item, tags, properties) }
    }

    fun updateItemWithTags(item: CollectionItem, tags: List<Tag>, properties: Map<String, String>) {
        viewModelScope.launch { updateItemUseCase(item, tags, properties) }
    }

    fun deleteItem(item: CollectionItem) { viewModelScope.launch { deleteItemUseCase(item) } }

    fun getItemByIdWithTags(id: Int): Flow<CollectionItemWithTags?> = itemRepository.getItemByIdWithTags(id)

    fun getTagsForCollections(collectionIds: List<Long>): Flow<List<Tag>> = if (collectionIds.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList()) else tagRepository.getTagsByCollectionIds(collectionIds)

    fun insertTag(name: String, collectionId: Long) { viewModelScope.launch(Dispatchers.IO) { tagRepository.insertTag(Tag(name = name, collectionId = collectionId)) } }

    fun processAndSaveImage(sourceUri: Uri, shouldCutout: Boolean, onResult: (String?) -> Unit) { viewModelScope.launch { val resultUri = imageProcessor.processImage(sourceUri, shouldCutout); onResult(resultUri?.toString()) } }
}