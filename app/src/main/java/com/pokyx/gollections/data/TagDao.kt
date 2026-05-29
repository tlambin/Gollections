package com.pokyx.gollections.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    // Permet de récupérer les tags de la collection actuelle ET de ses dossiers parents !
    @Query("SELECT * FROM tags WHERE collectionId IN (:collectionIds) ORDER BY name ASC")
    fun getTagsByCollectionIds(collectionIds: List<Long>): Flow<List<Tag>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: Tag)

    @Delete
    suspend fun deleteTag(tag: Tag)

    @Query("UPDATE tags SET name = :newName WHERE collectionId = :collectionId AND name = :oldName")
    suspend fun renameTag(collectionId: Long, oldName: String, newName: String)
}