package com.example.finance.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class DateUtilsTest {
    @Test
    fun testInitialNextDueDate() {
        // Start date 2026-04-01, day 15 -> 2026-04-15
        assertEquals("2026-04-15", DateUtils.getInitialNextDueDate("2026-04-01", 15))
        
        // Start date 2026-04-20, day 15 -> 2026-05-15
        assertEquals("2026-05-15", DateUtils.getInitialNextDueDate("2026-04-20", 15))
        
        // Leap year check: Feb 29
        assertEquals("2024-02-29", DateUtils.getInitialNextDueDate("2024-02-01", 31))
    }

    @Test
    fun testNextMonthlyDueDate() {
        assertEquals("2026-05-15", DateUtils.getNextMonthlyDueDate("2026-04-15", 15))
        assertEquals("2026-03-31", DateUtils.getNextMonthlyDueDate("2026-02-28", 31))
    }

    @Test
    fun testGetInitialTransactionDate() {
        // Today is 2026-05-06 in the context of previous tools
        // But the function uses LocalDate.now(), which might vary.
        // Let's test with a month that doesn't have 31 days if today was 31st.
        
        // Mocking LocalDate.now() is hard without extra libs, but we can test the logic
        // with the selectedMonthKey.
        
        // If today is 6th, and we select 2026-02, it should return 2026-02-06
        // (Assuming today's day is 6)
        // If we select a month where today's day doesn't exist (e.g. today is 31st, select Feb),
        // it should return the last day of that month.
        
        // Since we can't easily mock now(), we'll check consistency with today's day of month
        val today = java.time.LocalDate.now()
        val result = DateUtils.getInitialTransactionDate(null, "2026-02")
        val expectedDay = minOf(today.dayOfMonth, 28)
        assertEquals("2026-02-${expectedDay.toString().padStart(2, '0')}", result)
    }

    @Test
    fun testCoerceDate() {
        assertEquals("2026-05-06", DateUtils.coerceDate("2026-05-06"))
        assertEquals(DateUtils.today(), DateUtils.coerceDate("invalid-date"))
        assertEquals(DateUtils.today(), DateUtils.coerceDate(null))
    }
}
