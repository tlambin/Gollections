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

    @Query("UPDATE sub_categories SET categoryName = :newCategory WHERE categoryName = :oldCategory")
    suspend fun updateSubCategoriesCategory(oldCategory: String, newCategory: String)

    @Query("DELETE FROM sub_categories WHERE categoryName = :categoryName")
    suspend fun deleteSubCategoriesByCategory(categoryName: String)

    @Query("UPDATE sub_categories SET name = :newName WHERE name = :oldName AND categoryName = :categoryName")
    suspend fun renameSubCategory(categoryName: String, oldName: String, newName: String)

    @Query("DELETE FROM sub_categories WHERE name = :name AND categoryName = :categoryName")
    suspend fun deleteSubCategoryByName(categoryName: String, name: String)
}