package com.pokyx.gollections.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pokyx.gollections.data.dao.BackupDao
import com.pokyx.gollections.data.dao.CollectionDao
import com.pokyx.gollections.data.dao.CollectionItemDao
import com.pokyx.gollections.data.dao.CollectionPropertyTemplateDao
import com.pokyx.gollections.data.model.Tag
import com.pokyx.gollections.data.dao.TagDao
import com.pokyx.gollections.data.model.Collection
import com.pokyx.gollections.data.model.CollectionItem
import com.pokyx.gollections.data.model.CollectionItemFts
import com.pokyx.gollections.data.model.CollectionPropertyTemplate
import com.pokyx.gollections.data.model.Converters
import com.pokyx.gollections.data.model.ItemProperty
import com.pokyx.gollections.data.model.CollectionItemTagCrossRef

@Database(
    entities = [
        CollectionItem::class,
        Collection::class,
        Tag::class,
        CollectionItemTagCrossRef::class,
        ItemProperty::class,
        CollectionItemFts::class,
        CollectionPropertyTemplate::class // NOUVELLE ENTITÉ AJOUTÉE ICI
    ],
    version = 18, // PASSAGE À LA VERSION 18
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun collectionItemDao(): CollectionItemDao
    abstract fun collectionDao(): CollectionDao
    abstract fun tagDao(): TagDao
    abstract fun backupDao(): BackupDao
    abstract fun collectionPropertyTemplateDao(): CollectionPropertyTemplateDao // NOUVEAU DAO AJOUTÉ ICI

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gollections_database"
                )
                    // OPTIMISATION CRITIQUE : Suppression de .fallbackToDestructiveMigration(true)
                    // La base de données ne sera plus jamais effacée accidentellement !
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}