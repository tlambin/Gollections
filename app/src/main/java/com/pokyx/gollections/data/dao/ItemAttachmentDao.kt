package com.pokyx.gollections.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pokyx.gollections.data.model.ItemAttachment
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemAttachmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<ItemAttachment>)

    @Query("SELECT * FROM item_attachments WHERE item_id = :itemId")
    fun getAttachmentsForItem(itemId: Int): Flow<List<ItemAttachment>>

    @Query("SELECT * FROM item_attachments WHERE item_id = :itemId")
    suspend fun getAttachmentsForItemDirect(itemId: Int): List<ItemAttachment>

    @Query("DELETE FROM item_attachments WHERE item_id = :itemId")
    suspend fun deleteAttachmentsForItem(itemId: Int)
}