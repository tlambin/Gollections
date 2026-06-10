package com.pokyx.gollections.domain.usecase

import androidx.room.withTransaction
import com.pokyx.gollections.data.AppDatabase
import com.pokyx.gollections.data.model.CollectionItem
import com.pokyx.gollections.data.model.CollectionItemTagCrossRef
import com.pokyx.gollections.data.model.ItemAttachment
import com.pokyx.gollections.data.model.ItemProperty
import com.pokyx.gollections.data.model.Tag
import javax.inject.Inject

class InsertItemUseCase @Inject constructor(
    private val database: AppDatabase
) {
    /**
     * Insère un objet de collection de manière atomique avec toutes ses dépendances :
     * tags, propriétés personnalisées et pièces jointes (factures/tickets).
     */
    suspend operator fun invoke(
        item: CollectionItem,
        tags: List<Tag>,
        properties: List<ItemProperty>,
        attachments: List<ItemAttachment>
    ): Long {
        return database.withTransaction {
            // 1. Insertion de l'item principal pour générer son ID unique
            val itemDao = database.collectionItemDao()
            val generatedItemId = itemDao.insertItem(item)
            val itemIdInt = generatedItemId.toInt()

            // 2. Gestion et liaison des Tags
            if (tags.isNotEmpty()) {
                val tagDao = database.tagDao()
                tags.forEach { tag ->
                    // On insère le tag s'il n'existe pas déjà
                    tagDao.insertTag(tag)

                    // ✅ CORRECTION 1 & 2 : Utilisation de itemId
                    val crossRef = CollectionItemTagCrossRef(
                        itemId = itemIdInt,
                        tagId = tag.id
                    )
                    // ✅ CORRECTION 3 : Utilisation du bon nom de méthode du DAO
                    itemDao.insertItemTagCrossRef(crossRef)
                }
            }

            // 3. Liaison et insertion des champs personnalisés (ItemProperties)
            if (properties.isNotEmpty()) {
                val updatedProperties = properties.map { property ->
                    property.copy(itemId = itemIdInt)
                }
                // ✅ CORRECTION 4 : Utilisation du bon nom de méthode du DAO
                itemDao.insertProperties(updatedProperties)
            }

            // 4. Liaison et insertion des pièces jointes (ItemAttachments)
            if (attachments.isNotEmpty()) {
                val updatedAttachments = attachments.map { attachment ->
                    attachment.copy(itemId = itemIdInt)
                }
                val attachmentDao = database.itemAttachmentDao()
                attachmentDao.insertAttachments(updatedAttachments)
            }

            generatedItemId
        }
    }
}