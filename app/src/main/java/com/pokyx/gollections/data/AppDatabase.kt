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
    entities = [CollectionItem::class, Category::class, SubCategory::class], // <-- Ajout de SubCategory
    version = 5, // <-- Passage à la version 5
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun collectionDao(): CollectionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun subCategoryDao(): SubCategoryDao // <-- Ajout

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
                    // Catégories par défaut
                    val catDao = database.categoryDao()
                    catDao.insertCategory(Category("Blu-ray"))
                    catDao.insertCategory(Category("Jeux Vidéo"))
                    catDao.insertCategory(Category("Vinyles"))

                    // Sous-catégories par défaut
                    val subCatDao = database.subCategoryDao()
                    // Pour les Blu-ray
                    subCatDao.insertSubCategory(SubCategory(name = "4K", categoryName = "Blu-ray"))
                    subCatDao.insertSubCategory(SubCategory(name = "3D", categoryName = "Blu-ray"))
                    subCatDao.insertSubCategory(SubCategory(name = "Standard", categoryName = "Blu-ray"))
                    // Pour les Jeux Vidéo
                    subCatDao.insertSubCategory(SubCategory(name = "Switch", categoryName = "Jeux Vidéo"))
                    subCatDao.insertSubCategory(SubCategory(name = "PC", categoryName = "Jeux Vidéo"))
                    subCatDao.insertSubCategory(SubCategory(name = "PS5", categoryName = "Jeux Vidéo"))
                    subCatDao.insertSubCategory(SubCategory(name = "Xbox", categoryName = "Jeux Vidéo"))
                }
            }
        }
    }
}