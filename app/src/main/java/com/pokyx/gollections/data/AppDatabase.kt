package com.pokyx.gollections.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pokyx.gollections.data.tag.Tag
import com.pokyx.gollections.data.tag.TagDao
import com.pokyx.gollections.data.tag.CollectionItemTagCrossRef

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
    autoMigrations = [
        AutoMigration(from = 15, to = 16),
        AutoMigration(from = 16, to = 17),
        AutoMigration(from = 17, to = 18) // NOUVELLE MIGRATION AJOUTÉE ICI
    ],
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