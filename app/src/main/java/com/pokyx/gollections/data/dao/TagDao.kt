package com.pokyx.gollections.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pokyx.gollections.data.model.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM Tag WHERE collectionId IN (:collectionIds) ORDER BY name ASC")
    fun getTagsByCollectionIds(collectionIds: List<Long>): Flow<List<Tag>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag)

    @Delete
    suspend fun deleteTag(tag: Tag)

    @Query("UPDATE Tag SET name = :newName WHERE collectionId = :collectionId AND name = :oldName")
    suspend fun renameTag(collectionId: Long, oldName: String, newName: String)
}