package com.example.finance.core.common

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    private val displayMonthFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val displayDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

    fun getCurrentMonth(): String = LocalDate.now().format(monthFormatter)

    fun formatDisplayMonth(monthKey: String): String {
        return try {
            YearMonth.parse(monthKey, monthFormatter).format(displayMonthFormatter)
        } catch (e: Exception) {
            monthKey
        }
    }

    fun shiftMonth(monthKey: String, delta: Long): String {
        return YearMonth.parse(monthKey, monthFormatter).plusMonths(delta).format(monthFormatter)
    }

    fun getDaysInMonth(monthKey: String): Int {
        return YearMonth.parse(monthKey, monthFormatter).lengthOfMonth()
    }

    fun getRemainingDays(monthKey: String): Int {
        val now = LocalDate.now()
        val target = YearMonth.parse(monthKey, monthFormatter)
        return if (now.year == target.year && now.monthValue == target.monthValue) {
            target.lengthOfMonth() - now.dayOfMonth + 1
        } else if (now.isBefore(target.atDay(1))) {
            target.lengthOfMonth()
        } else {
            0
        }
    }

    fun today(): String = LocalDate.now().format(dateFormatter)

    fun formatDate(date: LocalDate): String = date.format(dateFormatter)

    fun formatDisplayDate(value: String): String {
        return parseDateOrNull(value)?.format(displayDateFormatter) ?: value
    }

    fun parseDateOrNull(value: String): LocalDate? {
        return try {
            LocalDate.parse(value, dateFormatter)
        } catch (_: Exception) {
            null
        }
    }

    fun coerceDate(value: String?, fallback: String = today()): String {
        return value?.takeIf { parseDateOrNull(it) != null } ?: fallback
    }

    fun getInitialTransactionDate(transactionDate: String?, selectedMonthKey: String): String {
        if (transactionDate != null && parseDateOrNull(transactionDate) != null) {
            return transactionDate
        }
        
        val today = LocalDate.now()
        val month = try {
            YearMonth.parse(selectedMonthKey, monthFormatter)
        } catch (e: Exception) {
            YearMonth.from(today)
        }
        
        val day = minOf(today.dayOfMonth, month.lengthOfMonth())
        return month.atDay(day).format(dateFormatter)
    }

    fun startOfMonth(monthKey: String): String {
        return YearMonth.parse(monthKey, monthFormatter).atDay(1).format(dateFormatter)
    }

    fun endOfMonth(monthKey: String): String {
        val month = YearMonth.parse(monthKey, monthFormatter)
        return month.atEndOfMonth().format(dateFormatter)
    }

    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60000 -> "just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            else -> "${diff / 86400000}d ago"
        }
    }

    fun getInitialNextDueDate(startDate: String, dayOfMonth: Int): String {
        val start = LocalDate.parse(startDate, dateFormatter)
        var nextDue = start.withDayOfMonth(minOf(dayOfMonth, start.lengthOfMonth()))
        if (nextDue.isBefore(start)) {
            nextDue = nextDue.plusMonths(1)
            nextDue = nextDue.withDayOfMonth(minOf(dayOfMonth, nextDue.lengthOfMonth()))
        }
        return nextDue.format(dateFormatter)
    }

    fun getNextMonthlyDueDate(currentDueDate: String, dayOfMonth: Int): String {
        val current = LocalDate.parse(currentDueDate, dateFormatter)
        var nextDue = current.plusMonths(1)
        nextDue = nextDue.withDayOfMonth(minOf(dayOfMonth, nextDue.lengthOfMonth()))
        return nextDue.format(dateFormatter)
    }
}
