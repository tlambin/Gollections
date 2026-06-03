package com.pokyx.gollections.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.pokyx.gollections.data.CollectionItem
import com.pokyx.gollections.data.CollectionItemDao
import com.pokyx.gollections.data.ItemProperty
import com.pokyx.gollections.data.tag.CollectionItemTagCrossRef
import com.pokyx.gollections.data.tag.CollectionItemWithTags
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository @Inject constructor(
    private val itemDao: CollectionItemDao
) {
    fun getAllItemsWithTags(): Flow<List<CollectionItemWithTags>> = itemDao.getAllItemsWithTags()

    fun getItemsByCollectionWithTags(collectionId: Long): Flow<List<CollectionItemWithTags>> = itemDao.getItemsByCollectionWithTags(collectionId)

    fun searchItemsWithTags(query: String): Flow<List<CollectionItemWithTags>> {
        val sanitizedQuery = query.replace(Regex("[^\\w\\sÀ-ÿ]"), "").trim()
        val ftsQuery = if (sanitizedQuery.isNotBlank()) "$sanitizedQuery*" else ""
        return itemDao.searchItemsWithTagsFts(ftsQuery)
    }

    fun getItemByIdWithTags(id: Int): Flow<CollectionItemWithTags?> = itemDao.getItemByIdWithTags(id)

    suspend fun insertItem(item: CollectionItem): Long = itemDao.insertItem(item)
    suspend fun updateItem(item: CollectionItem) = itemDao.updateItem(item)
    suspend fun deleteItem(item: CollectionItem) = itemDao.deleteItem(item)

    suspend fun clearTagsForItem(itemId: Int) = itemDao.clearTagsForItem(itemId)
    suspend fun clearPropertiesForItem(itemId: Int) = itemDao.clearPropertiesForItem(itemId)
    suspend fun insertItemTagCrossRef(crossRef: CollectionItemTagCrossRef) = itemDao.insertItemTagCrossRef(crossRef)

    // CORRIGÉ : Le DAO utilise insertProperties
    suspend fun insertProperties(properties: List<ItemProperty>) = itemDao.insertProperties(properties)

    suspend fun getItemsWithTagSync(tagName: String): List<CollectionItemWithTags> = itemDao.getItemsWithTagSync(tagName)

    fun getPagedItemsWithFilters(
        collectionId: Long,
        searchQuery: String,
        tagFilter: String,
        sortOption: String
    ): Flow<PagingData<CollectionItemWithTags>> {
        val sanitizedQuery = searchQuery.replace(Regex("[^\\w\\sÀ-ÿ]"), "").trim()
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { itemDao.getPagedItems(collectionId, sanitizedQuery, tagFilter, sortOption) }
        ).flow
    }
}