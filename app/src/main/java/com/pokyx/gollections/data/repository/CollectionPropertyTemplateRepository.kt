package com.pokyx.gollections.data.repository

import com.pokyx.gollections.data.model.CollectionPropertyTemplate
import com.pokyx.gollections.data.dao.CollectionPropertyTemplateDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionPropertyTemplateRepository @Inject constructor(
    private val templateDao: CollectionPropertyTemplateDao
) {
    fun getTemplatesForCollection(collectionId: Long): Flow<List<CollectionPropertyTemplate>> =
        templateDao.getTemplatesForCollection(collectionId)

    suspend fun insertTemplate(template: CollectionPropertyTemplate): Long =
        templateDao.insertTemplate(template)

    suspend fun insertTemplates(templates: List<CollectionPropertyTemplate>) =
        templateDao.insertTemplates(templates)

    suspend fun deleteTemplate(template: CollectionPropertyTemplate) =
        templateDao.deleteTemplate(template)
}