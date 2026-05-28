package com.pokyx.gollections.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [CollectionItem::class, Collection::class, Category::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun collectionItemDao(): CollectionItemDao
    abstract fun collectionDao(): CollectionDao
    abstract fun categoryDao(): CategoryDao

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
                    .addCallback(AppDatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val colDao = database.collectionDao()
                    colDao.insertCollection(Collection("Blu-ray"))
                    colDao.insertCollection(Collection("Jeux Vidéo"))
                    colDao.insertCollection(Collection("Vinyles"))

                    val catDao = database.categoryDao()
                    catDao.insertCategory(Category(name = "4K", collectionName = "Blu-ray"))
                    catDao.insertCategory(Category(name = "3D", collectionName = "Blu-ray"))
                    catDao.insertCategory(Category(name = "Standard", collectionName = "Blu-ray"))

                    catDao.insertCategory(Category(name = "Switch", collectionName = "Jeux Vidéo"))
                    catDao.insertCategory(Category(name = "PC", collectionName = "Jeux Vidéo"))
                    catDao.insertCategory(Category(name = "PS5", collectionName = "Jeux Vidéo"))
                    catDao.insertCategory(Category(name = "Xbox", collectionName = "Jeux Vidéo"))
                }
            }
        }
    }
}