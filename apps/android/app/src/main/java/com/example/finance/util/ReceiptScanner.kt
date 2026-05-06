package com.example.finance.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.example.finance.core.common.DateUtils
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.util.regex.Pattern

data class ScannedReceipt(
    val merchant: String?,
    val amount: Long?, // in cents
    val date: LocalDate?
)

object ReceiptScanner {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun scan(context: Context, uri: Uri): ScannedReceipt? {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val result = recognizer.process(image).await()
            internalParseText(result.text)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Internal parsing logic separated for unit testing.
     */
    fun internalParseText(text: String): ScannedReceipt {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        
        // 1. Merchant Detection
        // Usually the first few lines, skipping generic headers
        val genericHeaders = listOf("TAX INVOICE", "WELCOME", "RECEIPT", "OFFICIAL", "DUPLICATE")
        val merchant = lines.firstOrNull { line ->
            genericHeaders.none { header -> line.contains(header, ignoreCase = true) }
        }

        // 2. Amount Extraction (Cents)
        // Heuristic: Look for keywords like "TOTAL", "AMOUNT DUE", "BALANCE"
        // and pick the largest amount that follows such a keyword or is the largest overall.
        val totalKeywords = listOf("TOTAL", "AMOUNT DUE", "BALANCE", "NET", "PAYABLE", "DUE")
        val allAmounts = extractAllAmounts(text)
        
        var bestAmount = allAmounts.maxOrNull()
        
        // Refine: find line with "total" and look for an amount in that line or the next line
        val totalLineIndex = lines.indexOfFirst { line ->
            totalKeywords.any { kw -> line.contains(kw, ignoreCase = true) }
        }
        
        if (totalLineIndex != -1) {
            val searchLines = lines.subList(totalLineIndex, minOf(totalLineIndex + 3, lines.size))
            val lineAmounts = searchLines.flatMap { extractAllAmounts(it) }
            if (lineAmounts.isNotEmpty()) {
                bestAmount = lineAmounts.maxOrNull()
            }
        }

        // 3. Date Extraction
        var date: LocalDate? = null
        val datePatterns = listOf(
            Pattern.compile("(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})"), // YYYY-MM-DD
            Pattern.compile("(\\d{1,2})[/.](\\d{1,2})[/.](\\d{4})"), // DD/MM/YYYY
            Pattern.compile("(\\d{1,2})[/.](\\d{1,2})[/.](\\d{2})")  // DD/MM/YY or MM/DD/YY
        )

        for (pattern in datePatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val d1 = matcher.group(1)?.toIntOrNull() ?: 0
                val d2 = matcher.group(2)?.toIntOrNull() ?: 0
                val d3 = matcher.group(3)?.toIntOrNull() ?: 0
                
                date = tryParseDate(d1, d2, d3)
                if (date != null) break
            }
        }

        return ScannedReceipt(merchant, bestAmount, date)
    }

    private fun extractAllAmounts(input: String): List<Long> {
        val amounts = mutableListOf<Long>()
        val matcher = Pattern.compile("(\\d+[.,]\\d{2})").matcher(input)
        while (matcher.find()) {
            val clean = matcher.group(1)?.replace(",", ".") ?: continue
            clean.toDoubleOrNull()?.let {
                amounts.add((it * 100).toLong())
            }
        }
        return amounts
    }

    private fun tryParseDate(a: Int, b: Int, c: Int): LocalDate? {
        // Try YYYY (if a > 1000), MM, DD
        if (a > 1000) runCatching { return LocalDate.of(a, b, c) }
        
        // Try DD, MM, YYYY (if c > 1000)
        if (c > 1000) runCatching { return LocalDate.of(c, b, a) }
        
        // Try MM, DD, YYYY (if c > 1000)
        if (c > 1000) runCatching { return LocalDate.of(c, a, b) }
        
        // Try YY (assume 20xx), MM, DD
        if (a < 100) runCatching { return LocalDate.of(2000 + a, b, c) }
        
        // Try DD, MM, YY
        if (c < 100) runCatching { return LocalDate.of(2000 + c, b, a) }

        // Try MM, DD, YY
        if (c < 100) runCatching { return LocalDate.of(2000 + c, a, b) }
        
        return null
    }
}
