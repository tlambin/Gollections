package com.pokyx.gollections.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokyx.gollections.data.Category
import com.pokyx.gollections.data.CategoryDao
import com.pokyx.gollections.data.CollectionDao
import com.pokyx.gollections.data.CollectionItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val dao: CollectionDao,
    private val categoryDao: CategoryDao
) : ViewModel() {

    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    val allItems: Flow<List<CollectionItem>> = dao.getAllItems()

    fun getItemsByCategory(category: String): Flow<List<CollectionItem>> {
        return dao.getItemsByCategory(category)
    }

    fun searchItems(query: String): Flow<List<CollectionItem>> {
        return dao.searchItems(query)
    }

    fun addItem(title: String, year: String, category: String, subCategory: String) {
        viewModelScope.launch {
            val newItem = CollectionItem(
                title = title,
                year = year,
                category = category,
                subCategory = subCategory
            )
            dao.insertItem(newItem)
        }
    }

    fun deleteItem(item: CollectionItem) {
        viewModelScope.launch {
            dao.deleteItem(item)
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            categoryDao.insertCategory(Category(name = name))
        }
    }
}
// PLUS DE FACTORY ICI ! C'EST ENTIÈREMENT GÉRÉ PAR HILT.