package com.example.finance.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.finance.data.dao.AccountDao
import com.example.finance.data.dao.ActivityDao
import com.example.finance.data.dao.BudgetDao
import com.example.finance.data.dao.CategoryDao
import com.example.finance.data.dao.RecurringDao
import com.example.finance.data.dao.TransactionDao
import com.example.finance.data.dao.UserDao
import com.example.finance.data.entity.AccountEntity
import com.example.finance.data.entity.ActivityLogEntity
import com.example.finance.data.entity.BudgetEntity
import com.example.finance.data.entity.CategoryEntity
import com.example.finance.data.entity.MonthlyBudgetTargetEntity
import com.example.finance.data.entity.RecurringTransactionEntity
import com.example.finance.data.entity.TransactionEntity
import com.example.finance.data.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        RecurringTransactionEntity::class,
        BudgetEntity::class,
        MonthlyBudgetTargetEntity::class,
        ActivityLogEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringDao(): RecurringDao
    abstract fun budgetDao(): BudgetDao
    abstract fun activityDao(): ActivityDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE activity_logs ADD COLUMN source TEXT NOT NULL DEFAULT 'local'")
            }
        }
    }
}
