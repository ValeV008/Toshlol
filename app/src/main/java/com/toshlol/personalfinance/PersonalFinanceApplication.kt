package com.toshlol.personalfinance

import android.app.Application
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.AppPreferences
import com.toshlol.personalfinance.data.RoomAccountRepository
import com.toshlol.personalfinance.data.RoomBudgetRepository
import com.toshlol.personalfinance.data.RoomCategoryTagRepository
import com.toshlol.personalfinance.data.RoomTransactionRepository
import com.toshlol.personalfinance.domain.AccountManagement
import com.toshlol.personalfinance.domain.FirstRunSetup
import com.toshlol.personalfinance.domain.TransferManagement

class PersonalFinanceApplication : Application() {
    val transactionRepository: RoomTransactionRepository by lazy { RoomTransactionRepository(AppDatabase.getInstance(this)) }
    val transferRepository: com.toshlol.personalfinance.data.RoomTransferRepository by lazy {
        com.toshlol.personalfinance.data
            .RoomTransferRepository(AppDatabase.getInstance(this))
    }
    val categoryTagRepository: RoomCategoryTagRepository by lazy { RoomCategoryTagRepository(AppDatabase.getInstance(this)) }
    val budgetRepository: RoomBudgetRepository by lazy { RoomBudgetRepository(AppDatabase.getInstance(this)) }
    val accountRepository: RoomAccountRepository by lazy {
        val preferences: AppPreferences = AppPreferences(this)
        RoomAccountRepository(
            database = AppDatabase.getInstance(this),
            loadLegacyCurrency = preferences::loadLegacyCurrency,
            clearLegacySetup = preferences::clearLegacySetup,
        )
    }
    val firstRunSetup: FirstRunSetup by lazy { FirstRunSetup(accountRepository) }
    val appPreferences: AppPreferences by lazy { AppPreferences(this) }
    val accountManagement: AccountManagement by lazy { AccountManagement(accountRepository) }
    val transferManagement: TransferManagement by lazy { TransferManagement(transferRepository) }
}
