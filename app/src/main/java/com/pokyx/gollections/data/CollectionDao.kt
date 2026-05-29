package com.pokyx.gollections.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collections ORDER BY name ASC")
    fun getAllCollections(): Flow<List<Collection>>

    @Query("SELECT * FROM collections WHERE parentId IS NULL ORDER BY name ASC")
    fun getRootCollections(): Flow<List<Collection>>

    @Query("SELECT * FROM collections WHERE parentId = :parentId ORDER BY name ASC")
    fun getSubCollections(parentId: Long): Flow<List<Collection>>

    @Query("SELECT * FROM collections WHERE id = :id LIMIT 1")
    suspend fun getCollectionById(id: Long): Collection?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCollection(collection: Collection)

    @Delete
    suspend fun deleteCollection(collection: Collection)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollectionById(id: Long)

    @Query("UPDATE collections SET name = :newName WHERE id = :id")
    suspend fun renameCollection(id: Long, newName: String)

    // NOUVEAU : Met à jour le parent pour déplacer la collection
    @Query("UPDATE collections SET parentId = :newParentId WHERE id = :id")
    suspend fun updateParentId(id: Long, newParentId: Long?)
}