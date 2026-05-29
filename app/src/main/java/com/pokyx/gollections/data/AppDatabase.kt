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
    entities = [CollectionItem::class, Collection::class, Tag::class],
    version = 9,
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

                    val idBluRay = 1L
                    val idJeuxVideo = 2L
                    val idVinyles = 3L

                    colDao.insertCollection(Collection(id = idBluRay, name = "Blu-ray"))
                    colDao.insertCollection(Collection(id = idJeuxVideo, name = "Jeux Vidéo"))
                    colDao.insertCollection(Collection(id = idVinyles, name = "Vinyles"))

                    val tagDao = database.tagDao()
                    tagDao.insertTag(Tag(name = "4K", collectionId = idBluRay))
                    tagDao.insertTag(Tag(name = "3D", collectionId = idBluRay))
                    tagDao.insertTag(Tag(name = "Steelbook", collectionId = idBluRay))

                    tagDao.insertTag(Tag(name = "Switch", collectionId = idJeuxVideo))
                    tagDao.insertTag(Tag(name = "PC", collectionId = idJeuxVideo))
                    tagDao.insertTag(Tag(name = "PS5", collectionId = idJeuxVideo))
                    tagDao.insertTag(Tag(name = "Xbox", collectionId = idJeuxVideo))
                }
            }
        }
    }
}