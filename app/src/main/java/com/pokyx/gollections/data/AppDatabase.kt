package com.pokyx.gollections.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pokyx.gollections.data.tag.Tag
import com.pokyx.gollections.data.tag.TagDao
import com.pokyx.gollections.data.tag.CollectionItemTagCrossRef

@Database(
    entities = [CollectionItem::class, Collection::class, Tag::class, CollectionItemTagCrossRef::class, ItemProperty::class],
    version = 13,
    exportSchema = true,
    autoMigrations = [] // Prêt pour les futures migrations
)
@TypeConverters(Converters::class) // <-- Ajouté ici
abstract class AppDatabase : RoomDatabase() {

    abstract fun collectionItemDao(): CollectionItemDao
    abstract fun collectionDao(): CollectionDao
    abstract fun tagDao(): TagDao

    abstract fun backupDao(): BackupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gollections_database"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}