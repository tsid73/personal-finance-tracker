package com.example.finance.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.finance.domain.model.TransactionKind

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = RecurringTransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurring_transaction_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["account_id"]),
        Index(value = ["category_id"]),
        Index(value = ["user_id", "transaction_date"]),
        Index(value = ["user_id", "category_id", "transaction_date"]),
        Index(value = ["recurring_transaction_id", "transaction_date"]),
        Index(value = ["user_id", "title", "amount", "transaction_date"], unique = true)
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_id") val userId: Int,
    @ColumnInfo(name = "account_id") val accountId: Int,
    @ColumnInfo(name = "category_id") val categoryId: Int,
    @ColumnInfo(name = "recurring_transaction_id") val recurringTransactionId: Int? = null,
    val kind: TransactionKind,
    val title: String,
    val notes: String? = null,
    val merchant: String? = null,
    val amount: Long, // in cents
    @ColumnInfo(name = "transaction_date") val transactionDate: String, // YYYY-MM-DD
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
