package com.example.finance.data.repository

import com.example.finance.data.entity.AccountEntity
import com.example.finance.data.entity.BudgetEntity
import com.example.finance.data.entity.CategoryEntity
import com.example.finance.data.entity.RecurringTransactionEntity
import com.example.finance.data.entity.TransactionEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class SyncResult(
    val healthy: Boolean,
    val pushedCategories: Int,
    val pushedTransactions: Int,
    val pushedRecurring: Int,
    val pushedBudgets: Int,
    val errors: List<String>
)

class RemoteSyncManager {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    suspend fun sync(baseUrl: String, snapshot: BackupSnapshot): SyncResult = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        val normalizedBaseUrl = baseUrl.trim().trimEnd('/')
        if (normalizedBaseUrl.isBlank()) {
            return@withContext SyncResult(false, 0, 0, 0, 0, listOf("Base URL is empty."))
        }

        val healthy = runCatching {
            val request = Request.Builder().url("$normalizedBaseUrl/api/health").get().build()
            client.newCall(request).execute().use { response -> response.isSuccessful }
        }.getOrElse {
            errors += "Health check failed: ${it.message}"
            false
        }

        if (!healthy) {
            return@withContext SyncResult(false, 0, 0, 0, 0, errors)
        }

        val remoteAccounts = fetchList<Map<String, Any?>>(normalizedBaseUrl, "/api/accounts")
        val accountMap = buildAccountMap(snapshot.accounts, remoteAccounts)

        val remoteCategories = fetchList<Map<String, Any?>>(normalizedBaseUrl, "/api/categories?includeArchived=1")
        val categoryMap = mutableMapOf<Int, Int>()
        var pushedCategories = 0
        snapshot.categories.forEach { category ->
            val existing = remoteCategories.firstOrNull {
                it["name"]?.toString()?.equals(category.name, true) == true &&
                    it["type"]?.toString()?.equals(category.type.name.lowercase(), true) == true
            }
            val remoteId = when {
                existing != null -> (existing["id"] as Number).toInt()
                else -> {
                    postJson(
                        normalizedBaseUrl,
                        "/api/categories",
                        mapOf(
                            "name" to category.name,
                            "type" to category.type.name.lowercase(),
                            "color" to category.color,
                            "icon" to category.icon,
                            "budgetMode" to category.budgetMode.name.lowercase(),
                            "changeNote" to "Synced from Android"
                        )
                    ).also { pushedCategories += 1 }
                }
            }
            categoryMap[category.id] = remoteId
        }

        val remoteRecurring = fetchList<Map<String, Any?>>(normalizedBaseUrl, "/api/recurring-transactions")
        var pushedRecurring = 0
        snapshot.recurring.forEach { recurring ->
            val remoteAccountId = accountMap[recurring.accountId] ?: return@forEach
            val remoteCategoryId = categoryMap[recurring.categoryId] ?: return@forEach
            val payload = recurringPayload(recurring, remoteAccountId, remoteCategoryId)
            val existing = remoteRecurring.firstOrNull {
                it["title"] == recurring.title &&
                    (it["amount"] as Number).toLong() == recurring.amount &&
                    (it["dayOfMonth"] as Number).toInt() == recurring.dayOfMonth &&
                    it["startDate"] == recurring.startDate
            }
            if (existing == null) {
                postJson(normalizedBaseUrl, "/api/recurring-transactions", payload)
                pushedRecurring += 1
            }
        }

        val months = snapshot.transactions.map { it.transactionDate.take(7) }.distinct()
        var pushedTransactions = 0
        months.forEach { month ->
            val remoteTransactionsResponse = getMap(normalizedBaseUrl, "/api/transactions?month=$month&page=1&perPage=500")
            val remoteTransactions = (remoteTransactionsResponse["items"] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
            snapshot.transactions.filter { it.transactionDate.startsWith(month) }.forEach transactionLoop@{ transaction ->
                val remoteAccountId = accountMap[transaction.accountId] ?: return@transactionLoop
                val remoteCategoryId = categoryMap[transaction.categoryId] ?: return@transactionLoop
                val payload = transactionPayload(transaction, remoteAccountId, remoteCategoryId)
                val existing = remoteTransactions.firstOrNull {
                    it["title"] == transaction.title &&
                        it["kind"] == transaction.kind.name.lowercase() &&
                        (it["amount"] as Number).toLong() == transaction.amount &&
                        it["transactionDate"] == transaction.transactionDate
                }
                if (existing == null) {
                    postJson(normalizedBaseUrl, "/api/transactions", payload)
                    pushedTransactions += 1
                }
            }
        }

        val budgetMonths = snapshot.budgets.map { "${it.year}-${it.month.toString().padStart(2, '0')}" }.distinct()
        var pushedBudgets = 0
        snapshot.monthlyTargets.forEach { target ->
            putJson(
                normalizedBaseUrl,
                "/api/monthly-budget",
                mapOf("month" to target.month, "year" to target.year, "totalBudget" to target.totalBudget)
            )
        }
        budgetMonths.forEach { month ->
            val remoteBudgets = fetchList<Map<String, Any?>>(normalizedBaseUrl, "/api/budgets?month=$month")
            snapshot.budgets.filter { "${it.year}-${it.month.toString().padStart(2, '0')}" == month }.forEach budgetLoop@{ budget ->
                val remoteCategoryId = categoryMap[budget.categoryId] ?: return@budgetLoop
                val payload = mapOf(
                    "categoryId" to remoteCategoryId,
                    "month" to budget.month,
                    "year" to budget.year,
                    "allocatedAmount" to budget.allocatedAmount
                )
                val existing = remoteBudgets.firstOrNull { (it["categoryId"] as Number).toInt() == remoteCategoryId }
                if (existing == null) {
                    postJson(normalizedBaseUrl, "/api/budgets", payload)
                } else {
                    putJson(normalizedBaseUrl, "/api/budgets/${(existing["id"] as Number).toInt()}", payload)
                }
                pushedBudgets += 1
            }
        }

        SyncResult(true, pushedCategories, pushedTransactions, pushedRecurring, pushedBudgets, errors)
    }

    private fun buildAccountMap(localAccounts: List<AccountEntity>, remoteAccounts: List<Map<String, Any?>>): Map<Int, Int> {
        val remoteByLabel = remoteAccounts.associateBy { it["label"]?.toString()?.lowercase() ?: "" }
        return localAccounts.mapNotNull { account ->
            remoteByLabel[account.name.lowercase()]?.get("id")?.let { account.id to (it as Number).toInt() }
        }.toMap()
    }

    private fun transactionPayload(transaction: TransactionEntity, remoteAccountId: Int, remoteCategoryId: Int) = mapOf(
        "title" to transaction.title,
        "kind" to transaction.kind.name.lowercase(),
        "amount" to transaction.amount,
        "notes" to transaction.notes,
        "merchant" to transaction.merchant,
        "transactionDate" to transaction.transactionDate,
        "accountId" to remoteAccountId,
        "categoryId" to remoteCategoryId
    )

    private fun recurringPayload(recurring: RecurringTransactionEntity, remoteAccountId: Int, remoteCategoryId: Int) = mapOf(
        "title" to recurring.title,
        "kind" to recurring.kind.name.lowercase(),
        "amount" to recurring.amount,
        "notes" to recurring.notes,
        "merchant" to recurring.merchant,
        "accountId" to remoteAccountId,
        "categoryId" to remoteCategoryId,
        "dayOfMonth" to recurring.dayOfMonth,
        "startDate" to recurring.startDate,
        "autoCreate" to recurring.autoCreate,
        "isActive" to recurring.isActive
    )

    private inline fun <reified T> fetchList(baseUrl: String, path: String): List<T> {
        val request = Request.Builder().url("$baseUrl$path").get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("GET $path failed with ${response.code}")
            val json = response.body?.string().orEmpty()
            return gson.fromJson(json, object : TypeToken<List<T>>() {}.type)
        }
    }

    private fun getMap(baseUrl: String, path: String): Map<String, Any?> {
        val request = Request.Builder().url("$baseUrl$path").get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("GET $path failed with ${response.code}")
            val json = response.body?.string().orEmpty()
            return gson.fromJson(json, object : TypeToken<Map<String, Any?>>() {}.type)
        }
    }

    private fun postJson(baseUrl: String, path: String, payload: Map<String, Any?>): Int {
        val request = Request.Builder()
            .url("$baseUrl$path")
            .post(gson.toJson(payload).toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("POST $path failed with ${response.code}")
            val json = response.body?.string().orEmpty()
            val body = gson.fromJson<Map<String, Any?>>(json, object : TypeToken<Map<String, Any?>>() {}.type)
            return (body["id"] as? Number)?.toInt() ?: 0
        }
    }

    private fun putJson(baseUrl: String, path: String, payload: Map<String, Any?>) {
        val request = Request.Builder()
            .url("$baseUrl$path")
            .put(gson.toJson(payload).toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("PUT $path failed with ${response.code}")
        }
    }
}
