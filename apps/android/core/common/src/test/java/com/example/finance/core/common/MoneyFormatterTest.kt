package com.example.finance.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterTest {
    @Test
    fun testParseToCents() {
        assertEquals(10000L, MoneyFormatter.parseToCents("100"))
        assertEquals(10050L, MoneyFormatter.parseToCents("100.50"))
        assertEquals(100L, MoneyFormatter.parseToCents("1"))
    }

    @Test
    fun testFormat() {
        // This might depend on locale in tests, but let's check basic logic
        // MoneyFormatter.format(10050L) -> should contain "100.50"
        val formatted = MoneyFormatter.format(10050L)
        assert(formatted.contains("100.50"))
    }
}
