package com.pokyx.gollections.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE collectionName = :collectionName ORDER BY name ASC")
    fun getCategoriesByCollection(collectionName: String): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("UPDATE categories SET collectionName = :newCollection WHERE collectionName = :oldCollection")
    suspend fun updateCategoriesCollection(oldCollection: String, newCollection: String)

    @Query("DELETE FROM categories WHERE collectionName = :collectionName")
    suspend fun deleteCategoriesByCollection(collectionName: String)

    @Query("UPDATE categories SET name = :newName WHERE name = :oldName AND collectionName = :collectionName")
    suspend fun renameCategory(collectionName: String, oldName: String, newName: String)

    @Query("DELETE FROM categories WHERE name = :name AND collectionName = :collectionName")
    suspend fun deleteCategoryByName(collectionName: String, name: String)
}