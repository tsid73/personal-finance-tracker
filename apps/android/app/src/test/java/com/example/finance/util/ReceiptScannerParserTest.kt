package com.example.finance.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ReceiptScannerParserTest {

    @Test
    fun `parseText should extract amount and merchant from simple text`() {
        val text = """
            STARBUCKS COFFEE
            Store #12345
            Date: 2023-10-24
            
            CAFE LATTE      $4.50
            TAX             $0.36
            TOTAL           $4.86
            THANK YOU
        """.trimIndent()

        val scanned = ReceiptScanner.internalParseText(text)
        assertEquals("STARBUCKS COFFEE", scanned.merchant)
        assertEquals(486L, scanned.amount)
        assertEquals(LocalDate.of(2023, 10, 24), scanned.date)
    }

    @Test
    fun `parseText should handle different date formats`() {
        val text1 = "Date: 24/10/2023\nTOTAL $10.00"
        val scanned1 = ReceiptScanner.internalParseText(text1)
        assertEquals(LocalDate.of(2023, 10, 24), scanned1.date)

        val text2 = "Date: 10/24/23\nTOTAL $10.00"
        val scanned2 = ReceiptScanner.internalParseText(text2)
        assertEquals(LocalDate.of(2023, 10, 24), scanned2.date)
    }

    @Test
    fun `parseText should pick largest amount near TOTAL keyword`() {
        val text = """
            GROCERY STORE
            SUBTOTAL: 15.00
            TAX: 1.20
            TOTAL: 16.20
            CASH: 20.00
            CHANGE: 3.80
        """.trimIndent()
        
        val scanned = ReceiptScanner.internalParseText(text)
        assertEquals(1620L, scanned.amount)
    }
}
