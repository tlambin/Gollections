package com.pokyx.gollections.data.repository

import com.pokyx.gollections.data.tag.Tag
import com.pokyx.gollections.data.tag.TagDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    private val tagDao: TagDao
) {
    fun getTagsByCollectionIds(collectionIds: List<Long>): Flow<List<Tag>> = tagDao.getTagsByCollectionIds(collectionIds)
    suspend fun insertTag(tag: Tag) = tagDao.insertTag(tag)
    suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(tag)
    suspend fun renameTag(collectionId: Long, oldName: String, newName: String) = tagDao.renameTag(collectionId, oldName, newName)
}