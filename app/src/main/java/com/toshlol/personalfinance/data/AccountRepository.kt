package com.toshlol.personalfinance.data

data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val createdAt: Long,
    val updatedAt: Long,
)

data class FirstRunSnapshot(
    val currency: String = "",
    val accounts: List<Account> = emptyList(),
) {
    val isSetupComplete: Boolean
        get() = currency.isNotBlank() && accounts.isNotEmpty()
}

interface AccountRepository {
    suspend fun loadSetup(): FirstRunSnapshot

    suspend fun createFirstAccount(setup: AccountSetupValidationResult): FirstRunSnapshot

    suspend fun renameAccount(
        id: String,
        name: String,
    ): FirstRunSnapshot
}
