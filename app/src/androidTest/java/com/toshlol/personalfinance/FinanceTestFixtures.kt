package com.toshlol.personalfinance

import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.RoomAccountRepository
import com.toshlol.personalfinance.data.RoomBudgetRepository
import com.toshlol.personalfinance.data.RoomCategoryTagRepository
import com.toshlol.personalfinance.data.RoomTransactionRepository
import com.toshlol.personalfinance.domain.FirstRunSetup
import com.toshlol.personalfinance.domain.SetupOutcome
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/** Create one factory per test/database. IDs are unique across all of its repositories. */
class FinanceTestFixtures(
    database: AppDatabase,
    timeMillis: Long = FIXED_TIME_MILLIS,
) {
    private val identifierSequence: AtomicLong = AtomicLong()
    val accounts: RoomAccountRepository = RoomAccountRepository(database, ::nextIdentifier, { timeMillis })
    val organization: RoomCategoryTagRepository = RoomCategoryTagRepository(database, ::nextIdentifier, { timeMillis })
    val transactions: RoomTransactionRepository = RoomTransactionRepository(database, ::nextIdentifier, { timeMillis })
    val budgets: RoomBudgetRepository = RoomBudgetRepository(database, ::nextIdentifier, { timeMillis })

    suspend fun createFirstAccount(
        name: String = "Cash",
        currency: String = "EUR",
        type: AccountType = AccountType.CASH,
    ): Account = (FirstRunSetup(accounts).createFirstAccount(currency, name, type) as SetupOutcome.Completed).snapshot.accounts.single()

    private fun nextIdentifier(): String = UUID(0L, identifierSequence.incrementAndGet()).toString()

    companion object {
        const val FIXED_TIME_MILLIS: Long = 1_768_435_200_000L
    }
}
