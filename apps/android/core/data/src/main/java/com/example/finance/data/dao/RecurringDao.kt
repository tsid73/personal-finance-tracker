package com.example.finance.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.finance.data.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_transactions WHERE user_id = :userId ORDER BY is_active DESC, next_due_date ASC")
    fun getRecurringTransactions(userId: Int): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE user_id = :userId AND is_active = 1 AND next_due_date <= :today")
    suspend fun getDueRecurringTransactions(userId: Int, today: String): List<RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(recurring: RecurringTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringList(recurring: List<RecurringTransactionEntity>)

    @Update
    suspend fun updateRecurring(recurring: RecurringTransactionEntity)

    @Delete
    suspend fun deleteRecurring(recurring: RecurringTransactionEntity)

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getRecurringById(id: Int): RecurringTransactionEntity?

    @Query("SELECT COUNT(*) FROM recurring_transactions WHERE category_id = :categoryId")
    suspend fun getRecurringCountByCategory(categoryId: Int): Int

    @Query("UPDATE recurring_transactions SET category_id = :newCategoryId WHERE category_id = :oldCategoryId")
    suspend fun reassignCategory(oldCategoryId: Int, newCategoryId: Int)

    @Query("DELETE FROM recurring_transactions")
    suspend fun deleteAll()
}
