package com.pokyx.gollections.data.repository

import com.pokyx.gollections.data.Collection
import com.pokyx.gollections.data.CollectionDao
import com.pokyx.gollections.data.CollectionItem
import com.pokyx.gollections.data.CollectionItemDao
import com.pokyx.gollections.data.tag.CollectionItemTagCrossRef
import com.pokyx.gollections.data.tag.CollectionItemWithTags
import com.pokyx.gollections.data.tag.Tag
import com.pokyx.gollections.data.tag.TagDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionRepository @Inject constructor(
    private val collectionDao: CollectionDao,
    private val collectionItemDao: CollectionItemDao,
    private val tagDao: TagDao
) {
    // --- Collections ---
    fun getRootCollections(): Flow<List<Collection>> = collectionDao.getRootCollections()
    fun getAllCollections(): Flow<List<Collection>> = collectionDao.getAllCollections()
    fun getSubCollections(parentId: Long): Flow<List<Collection>> = collectionDao.getSubCollections(parentId)
    suspend fun getCollectionById(id: Long): Collection? = collectionDao.getCollectionById(id)
    suspend fun insertCollection(collection: Collection): Long = collectionDao.insertCollection(collection)
    suspend fun updateCollection(collection: Collection) = collectionDao.updateCollection(collection)
    suspend fun deleteCollection(collection: Collection) = collectionDao.deleteCollection(collection)
    suspend fun deleteCollectionById(id: Long) = collectionDao.deleteCollectionById(id)
    suspend fun renameCollection(id: Long, newName: String) = collectionDao.renameCollection(id, newName)
    suspend fun updateParentId(id: Long, newParentId: Long?) = collectionDao.updateParentId(id, newParentId)

    // --- Items ---
    fun getAllItemsWithTags(): Flow<List<CollectionItemWithTags>> = collectionItemDao.getAllItemsWithTags()
    fun getItemsByCollectionWithTags(collectionId: Long): Flow<List<CollectionItemWithTags>> = collectionItemDao.getItemsByCollectionWithTags(collectionId)
    fun searchItemsWithTags(searchQuery: String): Flow<List<CollectionItemWithTags>> = collectionItemDao.searchItemsWithTags(searchQuery)
    fun getItemByIdWithTags(id: Int): Flow<CollectionItemWithTags?> = collectionItemDao.getItemByIdWithTags(id)
    suspend fun insertItem(item: CollectionItem): Long = collectionItemDao.insertItem(item)
    suspend fun updateItem(item: CollectionItem) = collectionItemDao.updateItem(item)
    suspend fun deleteItem(item: CollectionItem) = collectionItemDao.deleteItem(item)
    suspend fun insertItemTagCrossRef(crossRef: CollectionItemTagCrossRef) = collectionItemDao.insertItemTagCrossRef(crossRef)
    suspend fun clearTagsForItem(itemId: Int) = collectionItemDao.clearTagsForItem(itemId)
    suspend fun getItemsWithTagSync(tagName: String): List<CollectionItemWithTags> = collectionItemDao.getItemsWithTagSync(tagName)

    // --- Tags ---
    fun getTagsByCollectionIds(collectionIds: List<Long>): Flow<List<Tag>> = tagDao.getTagsByCollectionIds(collectionIds)
    suspend fun insertTag(tag: Tag) = tagDao.insertTag(tag)
    suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(tag)
    suspend fun renameTag(collectionId: Long, oldName: String, newName: String) = tagDao.renameTag(collectionId, oldName, newName)
}