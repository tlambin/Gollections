package com.pokyx.gollections.di

import android.content.Context
import com.pokyx.gollections.data.AppDatabase
import com.pokyx.gollections.data.CollectionDao
import com.pokyx.gollections.data.CollectionItemDao
import com.pokyx.gollections.data.CollectionPropertyTemplateDao
import com.pokyx.gollections.data.tag.TagDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase = AppDatabase.getDatabase(context)

    @Provides
    fun provideCollectionItemDao(database: AppDatabase): CollectionItemDao = database.collectionItemDao()

    @Provides
    fun provideCollectionDao(database: AppDatabase): CollectionDao = database.collectionDao()

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao = database.tagDao()

    @Provides
    fun provideCollectionPropertyTemplateDao(database: AppDatabase): CollectionPropertyTemplateDao {
        return database.collectionPropertyTemplateDao()
    }

}