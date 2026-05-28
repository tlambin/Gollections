package com.pokyx.gollections.di

import android.content.Context
import com.pokyx.gollections.data.AppDatabase
import com.pokyx.gollections.data.CategoryDao
import com.pokyx.gollections.data.CollectionDao
import com.pokyx.gollections.data.SubCategoryDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideCollectionDao(database: AppDatabase): CollectionDao {
        return database.collectionDao()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    @Singleton
    fun provideSubCategoryDao(database: AppDatabase): SubCategoryDao {
        return database.subCategoryDao()
    }
}