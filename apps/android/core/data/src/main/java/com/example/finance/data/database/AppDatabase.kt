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
    version = 4,
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                mayAndJuneTransactionInserts.forEach(database::execSQL)
            }
        }

        private val mayAndJuneTransactionInserts = listOf(
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 4, 1, NULL, 'EXPENSE', 'Groceries', NULL, 'BB', 45000, '2026-05-30', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 4, 1, NULL, 'EXPENSE', 'Misc Groceries', NULL, 'Blinkit', 100000, '2026-05-26', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 4, 15, NULL, 'EXPENSE', 'Advance Tax', NULL, 'IT', 100000, '2026-05-25', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 1, 14, NULL, 'EXPENSE', 'ULIP', NULL, 'Tata AIA', 250000, '2026-05-25', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 5, 13, NULL, 'EXPENSE', 'Food', NULL, 'Gajanan Sweets', 30000, '2026-05-21', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 4, 11, NULL, 'EXPENSE', 'CC', NULL, 'Axis', 117700, '2026-05-20', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 4, 1, NULL, 'EXPENSE', 'Groceries', NULL, 'Mix', 110000, '2026-05-20', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 4, 3, NULL, 'EXPENSE', 'Net + DTH', NULL, 'Airtel', 93200, '2026-05-20', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 4, 13, NULL, 'EXPENSE', 'Food', NULL, 'Swiggy', 6000, '2026-05-19', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 4, 15, NULL, 'EXPENSE', 'Gpay lite', NULL, 'Gpay', 100000, '2026-05-16', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 1, 7, NULL, 'EXPENSE', 'Netflix', NULL, 'Netflix', 19900, '2026-05-14', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 5, 13, NULL, 'EXPENSE', 'Food', NULL, 'Misc', 12000, '2026-05-13', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 4, 11, NULL, 'EXPENSE', 'CC', NULL, 'Amazon', 676600, '2026-05-13', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 1, 3, NULL, 'EXPENSE', 'LinkedIn Subscription', NULL, 'LinkedIn', 50000, '2026-05-11', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 4, 15, NULL, 'EXPENSE', 'Apay', NULL, 'Amazon', 100000, '2026-05-09', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 4, 15, NULL, 'EXPENSE', 'Papa', NULL, 'Home', 310000, '2026-05-07', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 4, 1, NULL, 'EXPENSE', 'Groceries', NULL, 'Zepto', 56000, '2026-05-07', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 1, 12, NULL, 'EXPENSE', 'EMI', NULL, 'Axis Bank', 3100000, '2026-05-05', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 4, 15, NULL, 'EXPENSE', 'Repair', NULL, 'Urban Company', 100000, '2026-05-05', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 4, 1, NULL, 'EXPENSE', 'Groceries', NULL, 'Instamart', 70000, '2026-05-03', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 4, 11, NULL, 'EXPENSE', 'CC', NULL, 'Indusind', 210000, '2026-05-02', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 4, 11, NULL, 'EXPENSE', 'CC', NULL, 'Amazon', 650000, '2026-05-01', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 1, 14, NULL, 'EXPENSE', 'ULIP', NULL, 'Tata AIA', 249900, '2026-06-24', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 4, 3, NULL, 'EXPENSE', 'Net + DTH', NULL, 'Airtel', 93200, '2026-06-19', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 1, 12, NULL, 'EXPENSE', 'EMI', NULL, 'Axis Bank', 3100000, '2026-06-05', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 4, 15, NULL, 'EXPENSE', 'Home', NULL, 'Me+Mum', 300000, '2026-06-03', 1780594603429)",
            "INSERT OR IGNORE INTO transactions (user_id, account_id, category_id, recurring_transaction_id, kind, title, notes, merchant, amount, transaction_date, created_at) VALUES (1, 4, 14, NULL, 'EXPENSE', 'Investment', NULL, 'Mix', 4000000, '2026-06-02', 1780594603429)"
        )
    }
}
