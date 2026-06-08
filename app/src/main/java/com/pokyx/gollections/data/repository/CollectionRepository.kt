package com.pokyx.gollections.data.repository

import com.pokyx.gollections.data.Collection
import com.pokyx.gollections.data.CollectionDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionRepository @Inject constructor(
    private val collectionDao: CollectionDao
) {
    fun getRootCollections(): Flow<List<Collection>> = collectionDao.getRootCollections()
    fun getAllCollections(): Flow<List<Collection>> = collectionDao.getAllCollections()
    fun getSubCollections(parentId: Long): Flow<List<Collection>> = collectionDao.getSubCollections(parentId)
    suspend fun getCollectionById(id: Long): Collection? = collectionDao.getCollectionById(id)

    suspend fun insertCollection(collection: Collection): Long = collectionDao.insertCollection(collection)
    suspend fun updateCollection(collection: Collection) = collectionDao.updateCollection(collection)
    suspend fun deleteCollection(collection: Collection) = collectionDao.deleteCollection(collection)
    suspend fun deleteCollectionById(id: Long) = collectionDao.deleteCollectionById(id)

    suspend fun updateCollectionDetails(id: Long, newName: String, newCover: String) =
        collectionDao.updateCollectionDetails(id, newName, newCover)

    suspend fun updateParentId(id: Long, newParentId: Long?) = collectionDao.updateParentId(id, newParentId)
}