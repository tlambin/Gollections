package com.pokyx.gollections.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SubCategoryDao {

    @Query("SELECT * FROM sub_categories WHERE categoryName = :categoryName ORDER BY name ASC")
    fun getSubCategoriesByCategory(categoryName: String): Flow<List<SubCategory>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubCategory(subCategory: SubCategory)

    @Delete
    suspend fun deleteSubCategory(subCategory: SubCategory)
}