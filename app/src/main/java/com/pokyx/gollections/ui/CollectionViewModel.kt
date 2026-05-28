package com.pokyx.gollections.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokyx.gollections.data.Category
import com.pokyx.gollections.data.CategoryDao
import com.pokyx.gollections.data.CollectionDao
import com.pokyx.gollections.data.CollectionItem
import com.pokyx.gollections.data.SubCategory
import com.pokyx.gollections.data.SubCategoryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val dao: CollectionDao,
    private val categoryDao: CategoryDao,
    private val subCategoryDao: SubCategoryDao
) : ViewModel() {

    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    val allItems: Flow<List<CollectionItem>> = dao.getAllItems()

    fun getItemsByCategory(category: String): Flow<List<CollectionItem>> {
        return dao.getItemsByCategory(category)
    }

    fun searchItems(query: String): Flow<List<CollectionItem>> {
        return dao.searchItems(query)
    }

    fun addItem(title: String, category: String, subCategory: String, purchaseDate: String, price: String,
                imageUrl: String) {
        viewModelScope.launch {
            val newItem = CollectionItem(
                title = title,
                category = category,
                subCategory = subCategory,
                purchaseDate = purchaseDate,
                price = price,
                imageUrl = imageUrl,
                status = "", // Valeurs par défaut pour un nouvel objet
                comment = "",
                isLoaned = false,
                loanTo = ""
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

    fun getSubCategoriesByCategory(categoryName: String): Flow<List<SubCategory>> {
        return subCategoryDao.getSubCategoriesByCategory(categoryName)
    }

    fun addSubCategory(name: String, categoryName: String) {
        viewModelScope.launch {
            subCategoryDao.insertSubCategory(SubCategory(name = name, categoryName = categoryName))
        }
    }

    fun getItemById(id: Int): Flow<CollectionItem?> {
        return dao.getItemById(id)
    }

    fun updateItem(item: CollectionItem) {
        viewModelScope.launch {
            dao.updateItem(item)
        }
    }

    fun renameCollection(oldName: String, newName: String) {
        viewModelScope.launch {
            categoryDao.renameCategory(oldName, newName)
            dao.updateItemsCategory(oldName, newName)
            subCategoryDao.updateSubCategoriesCategory(oldName, newName)
        }
    }

    fun deleteCollection(categoryName: String) {
        viewModelScope.launch {
            categoryDao.deleteCategoryByName(categoryName)
            dao.deleteItemsByCategory(categoryName)
            subCategoryDao.deleteSubCategoriesByCategory(categoryName)
        }
    }

    fun renameSubCollection(categoryName: String, oldName: String, newName: String) {
        viewModelScope.launch {
            subCategoryDao.renameSubCategory(categoryName, oldName, newName)
            dao.updateItemsSubCategory(categoryName, oldName, newName)
        }
    }

    fun deleteSubCollection(categoryName: String, subName: String) {
        viewModelScope.launch {
            subCategoryDao.deleteSubCategoryByName(categoryName, subName)
            dao.clearItemsSubCategory(categoryName, subName)
        }
    }
}
// PLUS DE FACTORY ICI ! C'EST ENTIÈREMENT GÉRÉ PAR HILT.