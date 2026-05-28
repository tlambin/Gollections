package com.pokyx.gollections.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionItemDao {

    @Query("SELECT * FROM collection_items ORDER BY title ASC")
    fun getAllItems(): Flow<List<CollectionItem>>

    @Query("SELECT * FROM collection_items WHERE collection = :collectionName ORDER BY title ASC")
    fun getItemsByCollection(collectionName: String): Flow<List<CollectionItem>>

    @Query("SELECT * FROM collection_items WHERE title LIKE '%' || :searchQuery || '%' ORDER BY title ASC")
    fun searchItems(searchQuery: String): Flow<List<CollectionItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: CollectionItem)

    @Delete
    suspend fun deleteItem(item: CollectionItem)

    @Query("SELECT * FROM collection_items WHERE id = :id")
    fun getItemById(id: Int): Flow<CollectionItem?>

    @Update
    suspend fun updateItem(item: CollectionItem)

    @Query("UPDATE collection_items SET collection = :newCollection WHERE collection = :oldCollection")
    suspend fun updateItemsCollection(oldCollection: String, newCollection: String)

    @Query("DELETE FROM collection_items WHERE collection = :collectionName")
    suspend fun deleteItemsByCollection(collectionName: String)

    @Query("UPDATE collection_items SET category = :newCategory WHERE collection = :collectionName AND category = :oldCategory")
    suspend fun updateItemsCategory(collectionName: String, oldCategory: String, newCategory: String)

    @Query("UPDATE collection_items SET category = '' WHERE collection = :collectionName AND category = :oldCategory")
    suspend fun clearItemsCategory(collectionName: String, oldCategory: String)
}