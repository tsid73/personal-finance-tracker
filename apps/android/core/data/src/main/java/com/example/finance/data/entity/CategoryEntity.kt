package com.example.finance.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.finance.domain.model.BudgetMode
import com.example.finance.domain.model.TransactionKind

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["user_id", "type", "name"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_id") val userId: Int?, // NULL for default categories
    val name: String,
    val type: TransactionKind = TransactionKind.EXPENSE,
    val color: String = "#0f766e",
    val icon: String = "wallet",
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "budget_mode") val budgetMode: BudgetMode = BudgetMode.FLEXIBLE,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
