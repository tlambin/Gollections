package com.pokyx.gollections.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pokyx.gollections.data.tag.Tag
import com.pokyx.gollections.data.tag.TagDao
import com.pokyx.gollections.data.tag.CollectionItemTagCrossRef

@Database(
    entities = [CollectionItem::class, Collection::class, Tag::class, CollectionItemTagCrossRef::class],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun collectionItemDao(): CollectionItemDao
    abstract fun collectionDao(): CollectionDao
    abstract fun tagDao(): TagDao

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
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}