package com.toshlol.personalfinance

import android.app.Application
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.AppPreferences
import com.toshlol.personalfinance.data.RoomAccountRepository
import com.toshlol.personalfinance.data.RoomTransactionRepository
import com.toshlol.personalfinance.domain.FirstRunSetup

class PersonalFinanceApplication : Application() {
    val transactionRepository: RoomTransactionRepository by lazy { RoomTransactionRepository(AppDatabase.getInstance(this)) }
    val firstRunSetup: FirstRunSetup by lazy {
        val preferences: AppPreferences = AppPreferences(this)
        FirstRunSetup(
            RoomAccountRepository(
                database = AppDatabase.getInstance(this),
                loadLegacyCurrency = preferences::loadLegacyCurrency,
                clearLegacySetup = preferences::clearLegacySetup,
            ),
        )
    }
}
