package com.example.finance.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.finance.domain.model.Frequency
import com.example.finance.domain.model.TransactionKind

@Entity(
    tableName = "recurring_transactions",
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
        )
    ],
    indices = [
        Index(value = ["account_id"]),
        Index(value = ["category_id"]),
        Index(value = ["user_id", "is_active", "next_due_date"])
    ]
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_id") val userId: Int,
    @ColumnInfo(name = "account_id") val accountId: Int,
    @ColumnInfo(name = "category_id") val categoryId: Int,
    val kind: TransactionKind,
    val title: String,
    val notes: String? = null,
    val merchant: String? = null,
    val amount: Long, // in cents
    val frequency: Frequency = Frequency.MONTHLY,
    @ColumnInfo(name = "day_of_month") val dayOfMonth: Int,
    @ColumnInfo(name = "start_date") val startDate: String, // YYYY-MM-DD
    @ColumnInfo(name = "next_due_date") val nextDueDate: String, // YYYY-MM-DD
    @ColumnInfo(name = "auto_create") val autoCreate: Boolean = true,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
