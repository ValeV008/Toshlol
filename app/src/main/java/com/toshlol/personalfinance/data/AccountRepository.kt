package com.toshlol.personalfinance.data

import kotlinx.coroutines.flow.Flow

class AccountRepository(
    private val database: AppDatabase,
    private val appPreferences: AppPreferences
) {
    suspend fun saveCurrency(currency: String) {
        appPreferences.saveCurrency(currency)
    }

    suspend fun loadCurrency(): String? {
        return appPreferences.getCurrency()
    }

    suspend fun createAccount(name: String, type: AccountType): Long {
        val trimmedName = name.trim()
        val created = AccountEntity(
            name = trimmedName,
            type = type.name
        )
        return database.accountDao().insert(created)
    }

    suspend fun hasAccount(): Boolean = database.accountDao().countAccounts() > 0

    fun observeAccounts(): Flow<List<AccountEntity>> = database.accountDao().observeAccounts()
}
