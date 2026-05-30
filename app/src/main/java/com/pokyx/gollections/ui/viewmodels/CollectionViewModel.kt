package com.pokyx.gollections.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokyx.gollections.data.Collection
import com.pokyx.gollections.data.CollectionDao
import com.pokyx.gollections.data.CollectionItem
import com.pokyx.gollections.data.CollectionItemDao
import com.pokyx.gollections.data.repository.ImageProcessorRepository
import com.pokyx.gollections.data.tag.CollectionItemTagCrossRef
import com.pokyx.gollections.data.tag.CollectionItemWithTags
import com.pokyx.gollections.data.tag.Tag
import com.pokyx.gollections.data.tag.TagDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// --- STRUCTURE DE L'ÉTAT DU FORMULAIRE (UDF) ---
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
    val selectedTags: Set<Tag> = emptySet()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val collectionItemDao: CollectionItemDao,
    private val collectionDao: CollectionDao,
    private val tagDao: TagDao,
    private val imageProcessor: ImageProcessorRepository
) : ViewModel() {

    val allItemsWithTags: StateFlow<List<CollectionItemWithTags>> = collectionItemDao.getAllItemsWithTags().stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())
    val collections: StateFlow<List<Collection>> = collectionDao.getAllCollections().stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())
    val rootCollections: StateFlow<List<Collection>> = collectionDao.getRootCollections().stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val searchedItemsWithTags: StateFlow<List<CollectionItemWithTags>> = _searchQuery.flatMapLatest { query -> if (query.isBlank()) collectionItemDao.getAllItemsWithTags() else collectionItemDao.searchItemsWithTags(query) }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    // Flux d'état pour le formulaire
    private val _formState = MutableStateFlow(ItemFormState())
    val formState: StateFlow<ItemFormState> = _formState.asStateFlow()

    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    // --- GESTION DE L'ÉTAT DU FORMULAIRE ---
    fun resetFormState(preSelectedCollectionId: Long? = null, collectionsList: List<Collection> = emptyList()) {
        val initialPath = mutableListOf<Long>()
        if (preSelectedCollectionId != null && collectionsList.isNotEmpty()) {
            var curr: Long? = preSelectedCollectionId
            while(curr != null) {
                initialPath.add(0, curr)
                curr = collectionsList.find { it.id == curr }?.parentId
            }
        }
        _formState.value = ItemFormState(selectedPath = initialPath)
    }

    fun loadItemIntoForm(itemWithTags: CollectionItemWithTags, collectionsList: List<Collection>) {
        val currentItem = itemWithTags.item
        val path = mutableListOf<Long>()
        var curr: Long? = currentItem.collectionId
        while (curr != null) {
            path.add(0, curr)
            curr = collectionsList.find { it.id == curr }?.parentId
        }
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
            selectedTags = itemWithTags.tags.toSet()
        )
    }

    fun updateForm(transform: (ItemFormState) -> ItemFormState) {
        _formState.update(transform)
    }

    // --- LOGIQUE BD ---
    fun insertItemWithTags(item: CollectionItem, tags: List<Tag>) {
        viewModelScope.launch {
            val itemId = collectionItemDao.insertItem(item).toInt()
            tags.forEach { tag ->
                collectionItemDao.insertItemTagCrossRef(CollectionItemTagCrossRef(itemId, tag.id))
            }
        }
    }

    fun updateItemWithTags(item: CollectionItem, tags: List<Tag>) {
        viewModelScope.launch {
            collectionItemDao.updateItem(item)
            collectionItemDao.clearTagsForItem(item.id)
            tags.forEach { tag ->
                collectionItemDao.insertItemTagCrossRef(CollectionItemTagCrossRef(item.id, tag.id))
            }
        }
    }

    fun deleteItem(item: CollectionItem) { viewModelScope.launch { collectionItemDao.deleteItem(item) } }
    fun getItemByIdWithTags(id: Int): Flow<CollectionItemWithTags?> = collectionItemDao.getItemByIdWithTags(id)
    fun getItemsByCollectionWithTags(collectionId: Long): Flow<List<CollectionItemWithTags>> = collectionItemDao.getItemsByCollectionWithTags(collectionId)

    fun getSubCollections(parentId: Long): Flow<List<Collection>> = collectionDao.getSubCollections(parentId)
    suspend fun getCollectionById(id: Long): Collection? = collectionDao.getCollectionById(id)

    fun insertCollection(name: String, cover: String = "", parentId: Long? = null) { viewModelScope.launch { collectionDao.insertCollection(Collection(name = name, cover = cover, parentId = parentId)) } }
    fun deleteCollection(collectionId: Long) { viewModelScope.launch { collectionDao.deleteCollectionById(collectionId) } }
    fun renameCollection(id: Long, newName: String) { viewModelScope.launch { collectionDao.renameCollection(id, newName) } }
    fun updateCollectionParent(id: Long, newParentId: Long?) { viewModelScope.launch { collectionDao.updateParentId(id, newParentId) } }

    fun updateCollection(collection: Collection) {
        viewModelScope.launch {
            // Note: assurez-vous d'avoir une fonction @Update suspend fun updateCollection(collection: Collection) dans votre CollectionDao
            collectionDao.updateCollection(collection)
        }
    }

    fun getRecursiveItemCount(collectionId: Long, allCollections: List<Collection>, allItems: List<CollectionItemWithTags>): Int {
        val descendantIds = mutableListOf(collectionId)
        var currentLevel = allCollections.filter { it.parentId == collectionId }.map { it.id }
        while (currentLevel.isNotEmpty()) { descendantIds.addAll(currentLevel); currentLevel = allCollections.filter { it.parentId in currentLevel }.map { it.id } }
        return allItems.count { it.item.collectionId in descendantIds }
    }

    fun getValidMoveDestinations(collectionId: Long, allCollections: List<Collection>): List<Collection> {
        val descendantIds = mutableListOf(collectionId)
        var currentLevel = allCollections.filter { it.parentId == collectionId }.map { it.id }
        while (currentLevel.isNotEmpty()) { descendantIds.addAll(currentLevel); currentLevel = allCollections.filter { it.parentId in currentLevel }.map { it.id } }
        return allCollections.filter { it.id !in descendantIds }
    }

    fun getTagsForCollections(collectionIds: List<Long>): Flow<List<Tag>> {
        return if (collectionIds.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList()) else tagDao.getTagsByCollectionIds(collectionIds)
    }

    fun insertTag(name: String, collectionId: Long) { viewModelScope.launch { tagDao.insertTag(Tag(name = name, collectionId = collectionId)) } }
    fun deleteTag(tag: Tag) { viewModelScope.launch { tagDao.deleteTag(tag) } }
    fun renameTag(collectionId: Long, oldName: String, newName: String) { viewModelScope.launch { tagDao.renameTag(collectionId, oldName, newName) } }

    fun processAndSaveImage(sourceUri: Uri, shouldCutout: Boolean, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val resultUri = imageProcessor.processImage(sourceUri, shouldCutout)
            onResult(resultUri?.toString())
        }
    }
}