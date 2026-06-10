package com.pokyx.gollections.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.pokyx.gollections.data.model.CollectionItem
import com.pokyx.gollections.data.model.ItemProperty
import com.pokyx.gollections.data.model.CollectionItemTagCrossRef
import com.pokyx.gollections.data.model.CollectionItemWithTags
import com.pokyx.gollections.data.model.Tag
import kotlinx.coroutines.flow.Flow

data class CollectionItemCount(
    val collectionId: Long,
    val count: Int
)

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperties(properties: List<ItemProperty>)

    @Query("DELETE FROM item_properties WHERE itemId = :itemId")
    suspend fun clearPropertiesForItem(itemId: Int)

    @Query("SELECT COUNT(*) FROM collection_items WHERE collectionId IN (:collectionIds)")
    fun getItemsCountByCollectionIds(collectionIds: List<Long>): Flow<Int>

    @Query("SELECT COALESCE(SUM(price), 0.0) FROM collection_items WHERE collectionId IN (:collectionIds)")
    fun getItemsTotalValueByCollectionIds(collectionIds: List<Long>): Flow<Double>

    @Transaction
    @Query("SELECT * FROM collection_items WHERE collectionId IN (:collectionIds)")
    suspend fun getItemsByCollectionIdsSync(collectionIds: List<Long>): List<CollectionItemWithTags>

    @Query("SELECT COUNT(*) FROM collection_items")
    fun getTotalItemsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM collection_items WHERE isLoaned = 1")
    fun getLoanedItemsCount(): Flow<Int>

    @Query("SELECT collectionId, COUNT(id) as count FROM collection_items GROUP BY collectionId")
    fun getItemCountsPerCollection(): Flow<List<CollectionItemCount>>

    @Transaction
    @Query("""
        SELECT collection_items.* FROM collection_items 
        JOIN collection_items_fts ON collection_items.id = collection_items_fts.rowid 
        WHERE collection_items_fts MATCH :searchQuery
    """)
    fun searchItemsWithTagsFts(searchQuery: String): Flow<List<CollectionItemWithTags>>

    @Transaction
    @Query("""
        SELECT ci.* FROM collection_items ci
        LEFT JOIN collection_items_fts fts ON ci.id = fts.rowid
        WHERE ci.collectionId = :collectionId
        AND (:searchQuery = '' OR fts.title MATCH :searchQuery || '*')
        AND (:tagFilter = 'Toutes' OR ci.id IN (
            SELECT itemId FROM collection_item_tag_cross_ref 
            INNER JOIN Tag ON collection_item_tag_cross_ref.tagId = Tag.id 
            WHERE Tag.name = :tagFilter
        ))
        ORDER BY 
            CASE WHEN :sortOption = 'NAME_ASC' THEN ci.title END ASC,
            CASE WHEN :sortOption = 'NAME_DESC' THEN ci.title END DESC,
            CASE WHEN :sortOption = 'PRICE_ASC' THEN ci.price END ASC,
            CASE WHEN :sortOption = 'PRICE_DESC' THEN ci.price END DESC,
            CASE WHEN :sortOption = 'DATE_ASC' THEN substr(ci.purchaseDate, 7, 4) || substr(ci.purchaseDate, 4, 2) || substr(ci.purchaseDate, 1, 2) END ASC,
            CASE WHEN :sortOption = 'DATE_DESC' THEN substr(ci.purchaseDate, 7, 4) || substr(ci.purchaseDate, 4, 2) || substr(ci.purchaseDate, 1, 2) END DESC
    """)
    fun getPagedItems(
        collectionId: Long,
        searchQuery: String,
        tagFilter: String,
        sortOption: String
    ): PagingSource<Int, CollectionItemWithTags>

    // --- TRANSACTIONS SÉCURISÉES ---

    @Transaction
    suspend fun insertItemComplete(
        item: CollectionItem,
        tags: List<Tag>,
        properties: Map<String, String>
    ): Long {
        val itemId = insertItem(item).toInt()

        tags.forEach { tag ->
            insertItemTagCrossRef(CollectionItemTagCrossRef(itemId, tag.id))
        }

        val itemProperties = properties.map { (key, value) ->
            ItemProperty(itemId = itemId, label = key, value = value)
        }
        if (itemProperties.isNotEmpty()) {
            insertProperties(itemProperties)
        }

        return itemId.toLong()
    }

    @Transaction
    suspend fun updateItemComplete(
        item: CollectionItem,
        tags: List<Tag>,
        properties: Map<String, String>
    ) {
        updateItem(item)
        val itemId = item.id

        clearTagsForItem(itemId)
        tags.forEach { tag ->
            insertItemTagCrossRef(CollectionItemTagCrossRef(itemId, tag.id))
        }

        clearPropertiesForItem(itemId)
        val itemProperties = properties.map { (key, value) ->
            ItemProperty(itemId = itemId, label = key, value = value)
        }
        if (itemProperties.isNotEmpty()) {
            insertProperties(itemProperties)
        }
    }

    // --- REQUÊTES OPTIMISÉES (CTE RÉCURSIVES) ---

    @Query("""
        WITH RECURSIVE CollectionTree AS (
            SELECT id FROM collections WHERE id = :collectionId
            UNION ALL
            SELECT c.id FROM collections c INNER JOIN CollectionTree ct ON c.parentId = ct.id
        )
        SELECT COUNT(id) FROM collection_items WHERE collectionId IN CollectionTree
    """)
    fun getTotalCountRecursive(collectionId: Long): Flow<Int>

    // OPTIMISATION : Ajout de COALESCE pour garantir un Double non-nullable
    @Query("""
        WITH RECURSIVE CollectionTree AS (
            SELECT id FROM collections WHERE id = :collectionId
            UNION ALL
            SELECT c.id FROM collections c INNER JOIN CollectionTree ct ON c.parentId = ct.id
        )
        SELECT COALESCE(SUM(price), 0.0) FROM collection_items WHERE collectionId IN CollectionTree
    """)
    fun getTotalValueRecursive(collectionId: Long): Flow<Double>
}