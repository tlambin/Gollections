package com.pokyx.gollections.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collection_items ORDER BY title ASC")
    fun getAllItems(): Flow<List<CollectionItem>>

    @Query("SELECT * FROM collection_items WHERE category = :categoryName ORDER BY title ASC")
    fun getItemsByCategory(categoryName: String): Flow<List<CollectionItem>>

    @Query("SELECT * FROM collection_items WHERE title LIKE '%' || :searchQuery || '%' ORDER BY title ASC")
    fun searchItems(searchQuery: String): Flow<List<CollectionItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: CollectionItem)

    @Delete
    suspend fun deleteItem(item: CollectionItem)

    @Query("SELECT * FROM collection_items WHERE id = :id")
    fun getItemById(id: Int): kotlinx.coroutines.flow.Flow<CollectionItem?>

    @androidx.room.Update
    suspend fun updateItem(item: CollectionItem)
}