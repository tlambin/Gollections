package com.pokyx.gollections.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokyx.gollections.data.Collection
import com.pokyx.gollections.data.CollectionItem
import com.pokyx.gollections.data.ItemProperty
import com.pokyx.gollections.data.repository.CollectionRepository
import com.pokyx.gollections.data.repository.ImageProcessorRepository
import com.pokyx.gollections.data.tag.CollectionItemTagCrossRef
import com.pokyx.gollections.data.tag.CollectionItemWithTags
import com.pokyx.gollections.data.tag.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ItemFormState(
    val title: String = "",
    val purchaseDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
    val price: String = "",
    val status: String = "",
    val isLoaned: Boolean = false,
    val loanTo: String = "",
    val loanDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
    val imageUrl: String = "",
    val selectedPath: List<Long> = emptyList(),
    val selectedTags: Set<Tag> = emptySet(),
    val itemType: String = "OTHER",
    val properties: Map<String, String> = emptyMap()
)

@HiltViewModel
class ItemViewModel @Inject constructor(
    private val repository: CollectionRepository,
    private val imageProcessor: ImageProcessorRepository
) : ViewModel() {

    val collections = repository.getAllCollections().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _formState = MutableStateFlow(ItemFormState())
    val formState: StateFlow<ItemFormState> = _formState.asStateFlow()

    fun resetFormState(
        preSelectedCollectionId: Long? = null,
        collectionsList: List<Collection> = emptyList(),
        scannedTitle: String? = null,
        scannedImageUrl: String? = null
    ) {
        val initialPath = mutableListOf<Long>()
        if (preSelectedCollectionId != null && collectionsList.isNotEmpty()) {
            var curr: Long? = preSelectedCollectionId
            while(curr != null) {
                initialPath.add(0, curr)
                curr = collectionsList.find { it.id == curr }?.parentId
            }
        }
        _formState.value = ItemFormState(
            selectedPath = initialPath,
            title = scannedTitle ?: "",
            imageUrl = scannedImageUrl ?: ""
        )
    }

    fun loadItemIntoForm(itemWithTags: CollectionItemWithTags, collectionsList: List<Collection>) {
        val currentItem = itemWithTags.item
        val path = mutableListOf<Long>()
        var curr: Long? = currentItem.collectionId
        while (curr != null) {
            path.add(0, curr)
            curr = collectionsList.find { it.id == curr }?.parentId
        }

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

    fun changeItemType(newType: String) {
        val defaultProps = when (newType) {
            "MOVIE" -> mapOf("Réalisateur" to "", "Date de sortie" to "", "Synopsis" to "")
            "BOOK" -> mapOf("Auteur" to "", "Date de publication" to "", "Résumé" to "", "Nombre de pages" to "")
            "GAME" -> mapOf("Studio" to "", "Plateforme" to "", "Date de sortie" to "", "Description" to "")
            "MUSIC" -> mapOf("Artiste" to "", "Album" to "", "Date de sortie" to "")
            else -> emptyMap()
        }
        updateForm { it.copy(itemType = newType, properties = defaultProps) }
    }

    fun updateProperty(label: String, value: String) {
        updateForm { it.copy(properties = it.properties + (label to value)) }
    }

    fun insertItemWithTags(item: CollectionItem, tags: List<Tag>, properties: Map<String, String>) {
        viewModelScope.launch {
            val itemId = repository.insertItem(item).toInt()
            tags.forEach { tag ->
                repository.insertItemTagCrossRef(CollectionItemTagCrossRef(itemId, tag.id))
            }
            val itemProperties = properties.map { (key, value) ->
                ItemProperty(itemId = itemId, label = key, value = value)
            }
            repository.insertItemProperties(itemProperties)
        }
    }

    fun updateItemWithTags(item: CollectionItem, tags: List<Tag>, properties: Map<String, String>) {
        viewModelScope.launch(Dispatchers.IO) {
            // NOUVEAU : Suppression de l'ancienne image si elle a été modifiée
            val oldItem = repository.getItemByIdWithTags(item.id).firstOrNull()?.item
            if (oldItem != null && oldItem.imageUrl != item.imageUrl && oldItem.imageUrl.isNotBlank()) {
                imageProcessor.deleteImageFile(oldItem.imageUrl)
            }

            repository.updateItem(item)
            repository.clearTagsForItem(item.id)
            repository.clearPropertiesForItem(item.id)

            tags.forEach { tag ->
                repository.insertItemTagCrossRef(CollectionItemTagCrossRef(item.id, tag.id))
            }
            val itemProperties = properties.map { (key, value) ->
                ItemProperty(itemId = item.id, label = key, value = value)
            }
            repository.insertItemProperties(itemProperties)
        }
    }

    // NOUVEAU : Suppression de l'image liée lors de la suppression de l'objet
    fun deleteItem(item: CollectionItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteItem(item)
            if (item.imageUrl.isNotBlank()) {
                imageProcessor.deleteImageFile(item.imageUrl)
            }
        }
    }

    fun getItemByIdWithTags(id: Int): Flow<CollectionItemWithTags?> = repository.getItemByIdWithTags(id)
    fun getTagsForCollections(collectionIds: List<Long>): Flow<List<Tag>> = if (collectionIds.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList()) else repository.getTagsByCollectionIds(collectionIds)
    fun insertTag(name: String, collectionId: Long) { viewModelScope.launch { repository.insertTag(Tag(name = name, collectionId = collectionId)) } }

    fun processAndSaveImage(sourceUri: Uri, shouldCutout: Boolean, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val resultUri = imageProcessor.processImage(sourceUri, shouldCutout)
            onResult(resultUri?.toString())
        }
    }
}