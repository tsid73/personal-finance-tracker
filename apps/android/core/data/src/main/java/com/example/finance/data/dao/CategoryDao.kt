package com.example.finance.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.finance.data.entity.CategoryEntity
import com.example.finance.domain.model.TransactionKind
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("""
        SELECT * FROM categories 
        WHERE (user_id IS NULL OR user_id = :userId) 
        AND (:includeArchived = 1 OR is_archived = 0)
        ORDER BY type ASC, name ASC
    """)
    fun getCategories(userId: Int, includeArchived: Boolean): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Int): CategoryEntity?

    @Query("SELECT * FROM categories WHERE LOWER(name) = LOWER(:name) AND type = :type AND is_archived = 0 AND (user_id IS NULL OR user_id = :userId) LIMIT 1")
    suspend fun findCategoryByName(name: String, type: TransactionKind, userId: Int): CategoryEntity?

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
