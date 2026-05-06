package com.example.finance.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.finance.data.entity.TransactionEntity
import com.example.finance.domain.model.TransactionKind
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE user_id = :userId ORDER BY transaction_date DESC, id DESC")
    fun getAllTransactions(userId: Int): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE user_id = :userId 
        AND transaction_date LIKE :month || '-%'
        ORDER BY transaction_date DESC, id DESC
    """)
    fun getTransactionsByMonth(userId: Int, month: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE user_id = :userId 
        AND transaction_date LIKE :month || '-%'
        ORDER BY transaction_date DESC, id DESC
        LIMIT 6
    """)
    fun getRecentTransactions(userId: Int, month: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): TransactionEntity?

    @Query("SELECT SUM(amount) FROM transactions WHERE user_id = :userId AND kind = :kind AND transaction_date LIKE :month || '-%'")
    suspend fun getMonthlyTotal(userId: Int, month: String, kind: TransactionKind): Long?

    @Query("SELECT COUNT(*) FROM transactions WHERE category_id = :categoryId")
    suspend fun getTransactionCountByCategory(categoryId: Int): Int
    
    @Query("UPDATE transactions SET category_id = :newCategoryId WHERE category_id = :oldCategoryId")
    suspend fun reassignCategory(oldCategoryId: Int, newCategoryId: Int)

    @Query("DELETE FROM transactions WHERE id IN (:ids) AND user_id = :userId")
    suspend fun bulkDelete(ids: List<Int>, userId: Int)

    @Query("UPDATE transactions SET category_id = :categoryId WHERE id IN (:ids) AND user_id = :userId")
    suspend fun bulkRecategorize(ids: List<Int>, categoryId: Int, userId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE user_id = :userId AND recurring_transaction_id = :recurringId AND transaction_date = :date LIMIT 1)")
    suspend fun existsRecurringTransaction(userId: Int, recurringId: Int, date: String): Boolean
}
