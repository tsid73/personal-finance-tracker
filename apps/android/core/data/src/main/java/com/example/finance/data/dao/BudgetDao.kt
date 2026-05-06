package com.example.finance.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.finance.data.entity.BudgetEntity
import com.example.finance.data.entity.MonthlyBudgetTargetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE user_id = :userId")
    fun getAllBudgets(userId: Int): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE user_id = :userId AND month = :month AND year = :year")
    fun getBudgetsByMonth(userId: Int, month: Int, year: Int): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudgetById(id: Int): BudgetEntity?

    @Query("SELECT * FROM monthly_budget_targets WHERE user_id = :userId AND month = :month AND year = :year LIMIT 1")
    fun getMonthlyTarget(userId: Int, month: Int, year: Int): Flow<MonthlyBudgetTargetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlyTarget(target: MonthlyBudgetTargetEntity)

    @Query("SELECT SUM(allocated_amount) FROM budgets WHERE user_id = :userId AND month = :month AND year = :year")
    suspend fun getTotalAllocated(userId: Int, month: Int, year: Int): Long?

    @Query("SELECT COUNT(*) FROM budgets WHERE category_id = :categoryId")
    suspend fun getBudgetCountByCategory(categoryId: Int): Int

    @Query("UPDATE budgets SET category_id = :newCategoryId WHERE category_id = :oldCategoryId")
    suspend fun reassignCategory(oldCategoryId: Int, newCategoryId: Int)
}
