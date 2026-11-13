package com.toshlol.personalfinance.domain

import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.AccountRepository
import com.toshlol.personalfinance.data.AccountType

data class AccountDetailsDraft(
    val id: String,
    val name: String,
    val openingBalanceMinor: Long,
    val openingBalanceDate: Long,
)

data class NewAccountDraft(
    val name: String,
    val type: AccountType,
    val openingBalanceMinor: Long,
    val openingBalanceDate: Long,
)

class AccountManagement(
    private val repository: AccountRepository,
) {
    suspend fun loadAccounts(): List<Account> = repository.loadSetup().accounts

    suspend fun saveDetails(draft: AccountDetailsDraft): Account {
        require(draft.name.isNotBlank()) { "Enter an account name." }
        return repository.saveAccount(draft.id, draft.name, draft.openingBalanceMinor, draft.openingBalanceDate)
    }

    suspend fun createAccount(draft: NewAccountDraft): Account {
        require(draft.name.isNotBlank()) { "Enter an account name." }
        return repository.createAccount(draft.name, draft.type, draft.openingBalanceMinor, draft.openingBalanceDate)
    }
}
