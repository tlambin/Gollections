package com.pokyx.gollections.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [CollectionItem::class, Category::class], version = 2, exportSchema = false) // <-- Version passe à 2, ajout de Category
abstract class AppDatabase : RoomDatabase() {

    abstract fun collectionDao(): CollectionDao
    abstract fun categoryDao(): CategoryDao // <-- AJOUT

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
                    .fallbackToDestructiveMigration(dropAllTables = true) // Réinitialise proprement la BDD en dev si la structure change
                    .addCallback(AppDatabaseCallback()) // <-- AJOUT : Alimentation initiale
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    // Callback pour insérer les catégories par défaut lors de la toute première création de la BDD
    private class AppDatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = database.categoryDao()
                    dao.insertCategory(Category("Blu-ray"))
                    dao.insertCategory(Category("Jeux Vidéo"))
                    dao.insertCategory(Category("Vinyles"))
                }
            }
        }
    }
}