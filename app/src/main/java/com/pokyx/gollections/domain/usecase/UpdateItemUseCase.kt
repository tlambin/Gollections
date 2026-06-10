package com.pokyx.gollections.domain.usecase

import androidx.room.withTransaction
import com.pokyx.gollections.data.AppDatabase
import com.pokyx.gollections.data.model.CollectionItem
import com.pokyx.gollections.data.model.CollectionItemTagCrossRef
import com.pokyx.gollections.data.model.ItemAttachment
import com.pokyx.gollections.data.model.ItemProperty
import com.pokyx.gollections.data.model.Tag
import javax.inject.Inject

class UpdateItemUseCase @Inject constructor(
    private val database: AppDatabase
) {
    /**
     * Met à jour un objet de collection et synchronise l'intégralité de ses dépendances
     * (tags, champs personnalisés et pièces jointes) de manière sécurisée.
     */
    suspend operator fun invoke(
        item: CollectionItem,
        tags: List<Tag>,
        properties: List<ItemProperty>,
        attachments: List<ItemAttachment>
    ) {
        database.withTransaction {
            val itemDao = database.collectionItemDao()
            val tagDao = database.tagDao()
            val attachmentDao = database.itemAttachmentDao()
            val itemId = item.id

            // 1. Mise à jour de l'entité principale (titre, format d'image, etc.)
            itemDao.updateItem(item)

            // 2. Synchronisation des Tags
            itemDao.clearTagsForItem(itemId)
            tags.forEach { tag ->
                tagDao.insertTag(tag)
                val crossRef = CollectionItemTagCrossRef(
                    itemId = itemId, // ✅ CORRECTION : Le bon nom de paramètre
                    tagId = tag.id
                )
                itemDao.insertItemTagCrossRef(crossRef) // Utilisation du bon nom de méthode du DAO
            }

            // 3. Synchronisation des champs personnalisés (Properties)
            itemDao.clearPropertiesForItem(itemId)
            if (properties.isNotEmpty()) {
                val updatedProperties = properties.map { property ->
                    property.copy(itemId = itemId)
                }
                itemDao.insertProperties(updatedProperties) // Utilisation du bon nom de méthode
            }

            // 4. Synchronisation des pièces jointes (Attachments)
            attachmentDao.deleteAttachmentsForItem(itemId)
            if (attachments.isNotEmpty()) {
                val updatedAttachments = attachments.map { attachment ->
                    attachment.copy(itemId = itemId)
                }
                attachmentDao.insertAttachments(updatedAttachments)
            }
        }
    }
}