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

    @Query("SELECT * FROM collection_items WHERE collectionId = :collectionId ORDER BY title ASC")
    fun getItemsByCollection(collectionId: Long): Flow<List<CollectionItem>>

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

    @Query("SELECT * FROM collection_items WHERE tags LIKE '%' || :tag || '%'")
    suspend fun getItemsWithTagSync(tag: String): List<CollectionItem>
}