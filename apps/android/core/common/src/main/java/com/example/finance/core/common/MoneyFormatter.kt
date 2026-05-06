package com.example.finance.core.common

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object MoneyFormatter {
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        currency = Currency.getInstance("INR")
    }

    fun format(amountCents: Long): String {
        return currencyFormat.format(amountCents / 100.0)
    }

    fun parseToCents(amount: String): Long {
        return try {
            (amount.toDouble() * 100).toLong()
        } catch (e: Exception) {
            0L
        }
    }
}
