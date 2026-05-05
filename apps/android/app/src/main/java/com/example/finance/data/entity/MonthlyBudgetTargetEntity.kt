package com.example.finance.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "monthly_budget_targets",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id", "month", "year"], unique = true),
        Index(value = ["user_id", "year", "month"])
    ]
)
data class MonthlyBudgetTargetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_id") val userId: Int,
    val month: Int,
    val year: Int,
    @ColumnInfo(name = "total_budget") val totalBudget: Long, // in cents
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
