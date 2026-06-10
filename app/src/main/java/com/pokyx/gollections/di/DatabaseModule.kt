package com.pokyx.gollections.di

import android.content.Context
import androidx.room.Room
import com.pokyx.gollections.data.AppDatabase
import com.pokyx.gollections.data.dao.BackupDao
import com.pokyx.gollections.data.dao.CollectionDao
import com.pokyx.gollections.data.dao.CollectionItemDao
import com.pokyx.gollections.data.dao.CollectionPropertyTemplateDao
import com.pokyx.gollections.data.dao.ItemAttachmentDao
import com.pokyx.gollections.data.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "gollections_database"
        )
            .fallbackToDestructiveMigration() // Réinitialise proprement la base en cas de changement de version en Dev
            .build()
    }

    @Provides
    fun provideCollectionDao(database: AppDatabase): CollectionDao = database.collectionDao()

    @Provides
    fun provideCollectionItemDao(database: AppDatabase): CollectionItemDao = database.collectionItemDao()

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao = database.tagDao()

    @Provides
    fun provideCollectionPropertyTemplateDao(database: AppDatabase): CollectionPropertyTemplateDao =
        database.collectionPropertyTemplateDao()

    @Provides
    fun provideBackupDao(database: AppDatabase): BackupDao = database.backupDao()

    @Provides
    fun provideItemAttachmentDao(database: AppDatabase): ItemAttachmentDao = database.itemAttachmentDao()
}