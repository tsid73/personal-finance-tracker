package com.example.finance

import android.app.Application
import androidx.room.Room
import com.example.finance.data.database.AppDatabase
import com.example.finance.data.repository.FinanceRepository
import com.example.finance.data.repository.PreferenceManager

class FinanceApplication : Application() {
    val database by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "finance_db")
            .fallbackToDestructiveMigration()
            .build()
    }
    val repository by lazy { FinanceRepository(database) }
    val preferenceManager by lazy { PreferenceManager(this) }

    override fun onCreate() {
        super.onCreate()
        // Initialize seeds in a background thread or let the first VM do it
    }
}
