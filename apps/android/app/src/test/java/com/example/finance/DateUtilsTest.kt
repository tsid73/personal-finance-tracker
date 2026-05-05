package com.example.finance

import com.example.finance.util.DateUtils
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
}
