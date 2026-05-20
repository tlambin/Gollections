package com.pokyx.gollections.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pokyx.gollections.data.AppDatabase
import com.pokyx.gollections.data.CollectionItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class CollectionViewModel(application: Application) : AndroidViewModel(application) {

    // On récupère notre DAO via l'instance de notre Base de données
    private val dao = AppDatabase.getDatabase(application).collectionDao()

    // 1. Flux (Flow) contenant tous les objets pour la recherche globale
    val allItems: Flow<List<CollectionItem>> = dao.getAllItems()

    // 2. Récupérer les objets d'une catégorie spécifique
    fun getItemsByCategory(category: String): Flow<List<CollectionItem>> {
        return dao.getItemsByCategory(category)
    }

    // 3. Chercher des objets par texte
    fun searchItems(query: String): Flow<List<CollectionItem>> {
        return dao.searchItems(query)
    }

    // 4. Action d'ajout dans la base (exécutée en tâche de fond via un Scope Coroutine)
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

    // 5. Action de suppression
    fun deleteItem(item: CollectionItem) {
        viewModelScope.launch {
            dao.deleteItem(item)
        }
    }
}

// Le "moule" (Factory) obligatoire pour qu'Android Studio sache comment instancier notre ViewModel
class CollectionViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CollectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CollectionViewModel(application) as T
        }
        throw IllegalArgumentException("ViewModel inconnu")
    }
}