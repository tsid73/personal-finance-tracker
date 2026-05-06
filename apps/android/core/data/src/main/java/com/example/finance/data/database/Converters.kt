package com.example.finance.data.database

import androidx.room.TypeConverter
import com.example.finance.domain.model.AccountType
import com.example.finance.domain.model.BudgetMode
import com.example.finance.domain.model.Frequency
import com.example.finance.domain.model.TransactionKind

class Converters {
    @TypeConverter
    fun fromTransactionKind(value: TransactionKind): String = value.name

    @TypeConverter
    fun toTransactionKind(value: String): TransactionKind = TransactionKind.valueOf(value)

    @TypeConverter
    fun fromAccountType(value: AccountType): String = value.name

    @TypeConverter
    fun toAccountType(value: String): AccountType = AccountType.valueOf(value)

    @TypeConverter
    fun fromBudgetMode(value: BudgetMode): String = value.name

    @TypeConverter
    fun toBudgetMode(value: String): BudgetMode = BudgetMode.valueOf(value)

    @TypeConverter
    fun fromFrequency(value: Frequency): String = value.name

    @TypeConverter
    fun toFrequency(value: String): Frequency = Frequency.valueOf(value)
}
