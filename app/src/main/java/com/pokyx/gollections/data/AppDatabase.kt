package com.pokyx.gollections.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pokyx.gollections.data.dao.BackupDao
import com.pokyx.gollections.data.dao.CollectionDao
import com.pokyx.gollections.data.dao.CollectionItemDao
import com.pokyx.gollections.data.dao.CollectionPropertyTemplateDao
import com.pokyx.gollections.data.dao.ItemAttachmentDao
import com.pokyx.gollections.data.dao.TagDao
import com.pokyx.gollections.data.model.Collection
import com.pokyx.gollections.data.model.CollectionItem
import com.pokyx.gollections.data.model.CollectionItemFts
import com.pokyx.gollections.data.model.CollectionItemTagCrossRef
import com.pokyx.gollections.data.model.CollectionPropertyTemplate
import com.pokyx.gollections.data.model.Converters
import com.pokyx.gollections.data.model.ItemAttachment
import com.pokyx.gollections.data.model.ItemProperty
import com.pokyx.gollections.data.model.Tag

@Database(
    entities = [
        Collection::class,
        CollectionItem::class,
        CollectionItemTagCrossRef::class,
        Tag::class,
        CollectionPropertyTemplate::class,
        ItemProperty::class,
        CollectionItemFts::class,
        ItemAttachment::class
    ],
    version = 20,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao
    abstract fun collectionItemDao(): CollectionItemDao
    abstract fun tagDao(): TagDao
    abstract fun collectionPropertyTemplateDao(): CollectionPropertyTemplateDao
    abstract fun backupDao(): BackupDao
    abstract fun itemAttachmentDao(): ItemAttachmentDao
}