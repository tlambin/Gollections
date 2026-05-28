package com.pokyx.gollections.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokyx.gollections.data.Category
import com.pokyx.gollections.data.CategoryDao
import com.pokyx.gollections.data.Collection
import com.pokyx.gollections.data.CollectionDao
import com.pokyx.gollections.data.CollectionItem
import com.pokyx.gollections.data.CollectionItemDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionStats(
    val collectionName: String,
    val itemCount: Int,
    val totalInvestment: Double,
    val loanedCount: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val collectionItemDao: CollectionItemDao,
    private val collectionDao: CollectionDao,
    private val categoryDao: CategoryDao
) : ViewModel() {

    val allItems: StateFlow<List<CollectionItem>> = collectionItemDao.getAllItems()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val collections: StateFlow<List<Collection>> = collectionDao.getAllCollections()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchedItems: StateFlow<List<CollectionItem>> = _searchQuery
        .flatMapLatest { query -> if (query.isBlank()) collectionItemDao.getAllItems() else collectionItemDao.searchItems(query) }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val collectionStats: StateFlow<List<CollectionStats>> = allItems.map { items ->
        items.groupBy { it.collection }.map { (collectionName, collectionItems) ->
            val totalInvestment = collectionItems.sumOf { item -> item.price.replace(",", ".").toDoubleOrNull() ?: 0.0 }
            val loanedCount = collectionItems.count { it.isLoaned }
            CollectionStats(collectionName = collectionName, itemCount = collectionItems.size, totalInvestment = totalInvestment, loanedCount = loanedCount)
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    fun insertItem(item: CollectionItem) { viewModelScope.launch { collectionItemDao.insertItem(item) } }
    fun updateItem(item: CollectionItem) { viewModelScope.launch { collectionItemDao.updateItem(item) } }
    fun deleteItem(item: CollectionItem) { viewModelScope.launch { collectionItemDao.deleteItem(item) } }
    fun getItemById(id: Int): Flow<CollectionItem?> = collectionItemDao.getItemById(id)
    fun getItemsByCollection(collectionName: String): Flow<List<CollectionItem>> = collectionItemDao.getItemsByCollection(collectionName)

    fun insertCollection(name: String) { viewModelScope.launch { collectionDao.insertCollection(Collection(name = name)) } }
    fun deleteCollection(collection: Collection) {
        viewModelScope.launch {
            collectionItemDao.deleteItemsByCollection(collection.name)
            categoryDao.deleteCategoriesByCollection(collection.name)
            collectionDao.deleteCollection(collection)
        }
    }
    fun renameCollection(oldName: String, newName: String) {
        viewModelScope.launch {
            collectionDao.renameCollection(oldName, newName)
            categoryDao.updateCategoriesCollection(oldName, newName)
            collectionItemDao.updateItemsCollection(oldName, newName)
        }
    }

    fun insertCategory(name: String, collectionName: String) { viewModelScope.launch { categoryDao.insertCategory(Category(name = name, collectionName = collectionName)) } }
    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            collectionItemDao.clearItemsCategory(category.collectionName, category.name)
            categoryDao.deleteCategory(category)
        }
    }
    fun renameCategory(collectionName: String, oldName: String, newName: String) {
        viewModelScope.launch {
            categoryDao.renameCategory(collectionName, oldName, newName)
            collectionItemDao.updateItemsCategory(collectionName, oldName, newName)
        }
    }
    fun getCategoriesForCollection(collectionName: String): Flow<List<Category>> = categoryDao.getCategoriesByCollection(collectionName)
}