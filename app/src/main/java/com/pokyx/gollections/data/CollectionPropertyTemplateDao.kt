package com.pokyx.gollections.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionPropertyTemplateDao {
    @Query("SELECT * FROM CollectionPropertyTemplate WHERE collectionId = :collectionId")
    fun getTemplatesForCollection(collectionId: Long): Flow<List<CollectionPropertyTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: CollectionPropertyTemplate): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<CollectionPropertyTemplate>)

    @Delete
    suspend fun deleteTemplate(template: CollectionPropertyTemplate)
}