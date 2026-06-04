package com.example.finance.data.repository

import com.example.finance.data.entity.RecurringTransactionEntity
import com.example.finance.data.entity.TransactionEntity
import com.example.finance.domain.model.TransactionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteSyncManagerTest {
    private val manager = RemoteSyncManager()

    @Test
    fun convertsCentsToApiAmountWithStableScale() {
        assertEquals(123.45, manager.centsToApiAmount(12_345), 0.0)
        assertEquals(0.29, manager.centsToApiAmount(29), 0.0)
    }

    @Test
    fun convertsApiAmountBackToCents() {
        assertEquals(12_345L, manager.apiAmountToCents(123.45))
        assertEquals(12_345L, manager.apiAmountToCents("123.45"))
        assertEquals(29L, manager.apiAmountToCents(0.29))
    }

    @Test
    fun matchesTransactionUsingFullRemoteIdentity() {
        val local = TransactionEntity(
            id = 1,
            userId = 1,
            accountId = 2,
            categoryId = 3,
            kind = TransactionKind.EXPENSE,
            title = "Lunch",
            amount = 12_345,
            transactionDate = "2026-06-04",
            notes = "team",
            merchant = "Cafe"
        )

        val remote = mapOf<String, Any?>(
            "title" to "Lunch",
            "kind" to "expense",
            "amount" to 123.45,
            "transactionDate" to "2026-06-04",
            "accountId" to 22,
            "categoryId" to 33,
            "notes" to "team",
            "merchant" to "Cafe"
        )

        assertTrue(manager.matchesTransaction(remote, local, remoteAccountId = 22, remoteCategoryId = 33))
        assertFalse(manager.matchesTransaction(remote + ("merchant" to "Other"), local, remoteAccountId = 22, remoteCategoryId = 33))
    }

    @Test
    fun matchesRecurringUsingFullRemoteIdentity() {
        val local = RecurringTransactionEntity(
            id = 1,
            userId = 1,
            accountId = 2,
            categoryId = 3,
            kind = TransactionKind.EXPENSE,
            title = "Rent",
            amount = 50_000,
            dayOfMonth = 5,
            startDate = "2026-06-05",
            nextDueDate = "2026-07-05",
            autoCreate = true,
            isActive = true,
            notes = "monthly",
            merchant = "Landlord"
        )

        val remote = mapOf<String, Any?>(
            "title" to "Rent",
            "kind" to "expense",
            "amount" to 500.0,
            "accountId" to 22,
            "categoryId" to 33,
            "dayOfMonth" to 5,
            "startDate" to "2026-06-05",
            "autoCreate" to true,
            "isActive" to true,
            "notes" to "monthly",
            "merchant" to "Landlord"
        )

        assertTrue(manager.matchesRecurring(remote, local, remoteAccountId = 22, remoteCategoryId = 33))
        assertFalse(manager.matchesRecurring(remote + ("autoCreate" to false), local, remoteAccountId = 22, remoteCategoryId = 33))
    }
}
