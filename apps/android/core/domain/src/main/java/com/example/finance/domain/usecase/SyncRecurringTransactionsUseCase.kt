package com.example.finance.domain.usecase

import com.example.finance.data.database.AppDatabase
import com.example.finance.data.entity.TransactionEntity
import com.example.finance.core.common.DateUtils
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncRecurringTransactionsUseCase(private val db: AppDatabase) {
    private val demoUserId = 1

    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        db.withTransaction {
            val today = DateUtils.today()
            val dueSchedules = db.recurringDao().getDueRecurringTransactions(demoUserId, today)
            
            var createdCount = 0
            var updatedSchedules = 0

            for (schedule in dueSchedules) {
                var nextDue = schedule.nextDueDate
                while (nextDue <= today) {
                    val exists = db.transactionDao().existsRecurringTransaction(demoUserId, schedule.id, nextDue)

                    if (schedule.autoCreate && !exists) {
                        db.transactionDao().insertTransaction(TransactionEntity(
                            userId = demoUserId,
                            accountId = schedule.accountId,
                            categoryId = schedule.categoryId,
                            recurringTransactionId = schedule.id,
                            kind = schedule.kind,
                            title = schedule.title,
                            notes = schedule.notes,
                            merchant = schedule.merchant,
                            amount = schedule.amount,
                            transactionDate = nextDue
                        ))
                        createdCount++
                    }

                    nextDue = DateUtils.getNextMonthlyDueDate(nextDue, schedule.dayOfMonth)
                }

                db.recurringDao().updateRecurring(schedule.copy(nextDueDate = nextDue))
                updatedSchedules++
            }

            Pair(createdCount, updatedSchedules)
        }
    }
}
