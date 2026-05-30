package com.pokyx.gollections.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.pokyx.gollections.data.tag.CollectionItemTagCrossRef
import com.pokyx.gollections.data.tag.CollectionItemWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionItemDao {

    @Transaction
    @Query("SELECT * FROM collection_items ORDER BY title ASC")
    fun getAllItemsWithTags(): Flow<List<CollectionItemWithTags>>

    @Transaction
    @Query("SELECT * FROM collection_items WHERE collectionId = :collectionId ORDER BY title ASC")
    fun getItemsByCollectionWithTags(collectionId: Long): Flow<List<CollectionItemWithTags>>

    @Transaction
    @Query("SELECT * FROM collection_items WHERE title LIKE '%' || :searchQuery || '%' ORDER BY title ASC")
    fun searchItemsWithTags(searchQuery: String): Flow<List<CollectionItemWithTags>>

    @Transaction
    @Query("SELECT * FROM collection_items WHERE id = :id")
    fun getItemByIdWithTags(id: Int): Flow<CollectionItemWithTags?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: CollectionItem): Long

    @Update
    suspend fun updateItem(item: CollectionItem)

    @Delete
    suspend fun deleteItem(item: CollectionItem)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItemTagCrossRef(crossRef: CollectionItemTagCrossRef)

    @Query("DELETE FROM collection_item_tag_cross_ref WHERE itemId = :itemId")
    suspend fun clearTagsForItem(itemId: Int)

    @Transaction
    @Query("""
        SELECT collection_items.* FROM collection_items 
        INNER JOIN collection_item_tag_cross_ref ON collection_items.id = collection_item_tag_cross_ref.itemId
        INNER JOIN Tag ON collection_item_tag_cross_ref.tagId = Tag.id
        WHERE Tag.name = :tagName
    """)
    suspend fun getItemsWithTagSync(tagName: String): List<CollectionItemWithTags>
}