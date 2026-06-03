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

// NOUVELLE DATA CLASS POUR COMPTER RAPIDEMENT
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

    // REQUÊTES D'OPTIMISATION MÉMOIRE (Étape 1 & 2)
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

    // NOUVELLE REQUÊTE POUR LE DASHBOARD (GROUP BY)
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
    ): androidx.paging.PagingSource<Int, CollectionItemWithTags>
}