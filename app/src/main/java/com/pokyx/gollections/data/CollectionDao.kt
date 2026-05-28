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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCollection(collection: Collection)

    @Delete
    suspend fun deleteCollection(collection: Collection)

    @Query("UPDATE collections SET name = :newName WHERE name = :oldName")
    suspend fun renameCollection(oldName: String, newName: String)

    @Query("DELETE FROM collections WHERE name = :name")
    suspend fun deleteCollectionByName(name: String)
}