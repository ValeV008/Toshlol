package com.toshlol.personalfinance

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.FinanceTransaction
import com.toshlol.personalfinance.data.RecurringBudgetDraft
import com.toshlol.personalfinance.data.RecurringBudgetProgress
import com.toshlol.personalfinance.data.RoomRecurringBudgetRepository
import com.toshlol.personalfinance.data.RoomTransferRepository
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.data.TransactionDraft
import com.toshlol.personalfinance.data.TransferDraft
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class RecurringBudgetRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private var currentTime: Long = 500L

    @Test
    fun saveAndReloadNamedBudgetUsesOnlyItsSelectedAccounts(): Unit =
        runTest {
            val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                val fixtures: FinanceTestFixtures = FinanceTestFixtures(database)
                val selectedAccount: Account = fixtures.createFirstAccount("Cash")
                val otherAccount: Account = fixtures.createAdditionalAccount("Savings")
                fixtures.transactions.save(
                    TransactionDraft(
                        id = null,
                        accountId = selectedAccount.id,
                        amountMinor = 250L,
                        direction = TransactionDirection.EXPENSE,
                        date = LocalDate.of(2026, 1, 4).toEpochDay(),
                        note = null,
                        categoryId = null,
                    ),
                )
                fixtures.transactions.save(
                    TransactionDraft(
                        id = null,
                        accountId = otherAccount.id,
                        amountMinor = 400L,
                        direction = TransactionDirection.EXPENSE,
                        date = LocalDate.of(2026, 1, 5).toEpochDay(),
                        note = null,
                        categoryId = null,
                    ),
                )
                val repository: RoomRecurringBudgetRepository = RoomRecurringBudgetRepository(database, { "budget-1" }, { 500L })
                val actualBudgets: List<RecurringBudgetProgress> =
                    repository.save(
                        RecurringBudgetDraft(null, "Household", 1_000L, setOf(selectedAccount.id)),
                        YearMonth.of(2026, 1),
                    )
                val expectedBudget: RecurringBudgetProgress =
                    RecurringBudgetProgress(
                        "budget-1",
                        "Household",
                        YearMonth.of(2026, 1),
                        setOf(selectedAccount.id),
                        1_000L,
                        250L,
                        500L,
                        500L,
                    )
                assertEquals(listOf(expectedBudget), actualBudgets)
                assertEquals(actualBudgets, repository.load(YearMonth.of(2026, 1)))
            } finally {
                database.close()
            }
        }

    @Test
    fun editNamedBudgetChangesAllMonthsAndReplacesItsAccountScope(): Unit =
        runTest {
            val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                val fixtures: FinanceTestFixtures = FinanceTestFixtures(database)
                val firstAccount: Account = fixtures.createFirstAccount("Cash")
                val secondAccount: Account = fixtures.createAdditionalAccount("Savings")
                saveExpense(fixtures, firstAccount, 600L, LocalDate.of(2026, 1, 4))
                saveExpense(fixtures, secondAccount, 500L, LocalDate.of(2026, 1, 5))
                saveExpense(fixtures, firstAccount, 300L, LocalDate.of(2026, 2, 2))
                currentTime = 500L
                val repository: RoomRecurringBudgetRepository = RoomRecurringBudgetRepository(database, { "budget-1" }, { currentTime })
                repository.save(RecurringBudgetDraft(null, "Household", 1_000L, setOf(firstAccount.id)), YearMonth.of(2026, 1))
                currentTime = 900L
                val actualBudgets: List<RecurringBudgetProgress> =
                    repository.save(
                        RecurringBudgetDraft("budget-1", "Weekend spending", 700L, setOf(secondAccount.id)),
                        YearMonth.of(2026, 1),
                    )
                val actualJanuary: RecurringBudgetProgress = actualBudgets.single()
                val actualFebruary: RecurringBudgetProgress = repository.load(YearMonth.of(2026, 2)).single()
                assertEquals("budget-1", actualJanuary.id)
                assertEquals("Weekend spending", actualJanuary.name)
                assertEquals(setOf(secondAccount.id), actualJanuary.accountIds)
                assertEquals(700L, actualJanuary.limitMinor)
                assertEquals(500L, actualJanuary.spentMinor)
                assertEquals(200L, actualJanuary.remainingMinor)
                assertFalse(actualJanuary.isOverBudget)
                assertEquals(500L, actualJanuary.createdAt)
                assertEquals(900L, actualJanuary.updatedAt)
                assertEquals(0L, actualFebruary.spentMinor)
                assertEquals(setOf(secondAccount.id), actualFebruary.accountIds)
                assertTrue(repository.load(YearMonth.of(2027, 1)).single().spentMinor == 0L)
            } finally {
                database.close()
            }
        }

    @Test
    fun overlappingBudgetsCountExpensesIndependentlyAndIgnoreIncomeTransfersAndNewAccounts(): Unit =
        runTest {
            val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                val fixtures: FinanceTestFixtures = FinanceTestFixtures(database)
                val firstAccount: Account = fixtures.createFirstAccount("Cash")
                val secondAccount: Account = fixtures.createAdditionalAccount("Savings")
                saveExpense(fixtures, firstAccount, 300L, LocalDate.of(2026, 3, 4))
                fixtures.transactions.save(
                    TransactionDraft(
                        id = null,
                        accountId = firstAccount.id,
                        amountMinor = 700L,
                        direction = TransactionDirection.INCOME,
                        date = LocalDate.of(2026, 3, 5).toEpochDay(),
                        note = null,
                        categoryId = null,
                    ),
                )
                saveExpense(fixtures, secondAccount, 500L, LocalDate.of(2026, 3, 6))
                RoomTransferRepository(database).save(
                    TransferDraft(
                        sourceAccountId = firstAccount.id,
                        destinationAccountId = secondAccount.id,
                        amountMinor = 900L,
                        date = LocalDate.of(2026, 3, 7).toEpochDay(),
                    ),
                )
                var nextBudgetId: Int = 0
                val repository: RoomRecurringBudgetRepository =
                    RoomRecurringBudgetRepository(database, {
                        nextBudgetId++
                        "budget-$nextBudgetId"
                    }, { 500L })
                repository.save(RecurringBudgetDraft(null, "Cash", 250L, setOf(firstAccount.id)), YearMonth.of(2026, 3))
                repository.save(
                    RecurringBudgetDraft(null, "All spending", 2_000L, setOf(firstAccount.id, secondAccount.id)),
                    YearMonth.of(2026, 3),
                )
                val newAccount: Account = fixtures.createAdditionalAccount("New account")
                saveExpense(fixtures, newAccount, 1_000L, LocalDate.of(2026, 3, 8))
                val actualBudgets: List<RecurringBudgetProgress> = repository.load(YearMonth.of(2026, 3))
                val actualCashBudget: RecurringBudgetProgress = actualBudgets.single { it.id == "budget-1" }
                val actualAllSpendingBudget: RecurringBudgetProgress = actualBudgets.single { it.id == "budget-2" }
                assertEquals(300L, actualCashBudget.spentMinor)
                assertTrue(actualCashBudget.isOverBudget)
                assertEquals(800L, actualAllSpendingBudget.spentMinor)
                assertEquals(setOf(firstAccount.id, secondAccount.id), actualAllSpendingBudget.accountIds)
                assertEquals(1_200L, actualAllSpendingBudget.remainingMinor)
            } finally {
                database.close()
            }
        }

    @Test
    fun deletingExpenseRecalculatesBudgetProgress(): Unit =
        runTest {
            val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                val fixtures: FinanceTestFixtures = FinanceTestFixtures(database)
                val account: Account = fixtures.createFirstAccount("Cash")
                saveExpense(fixtures, account, 250L, LocalDate.of(2026, 4, 4))
                val transaction: FinanceTransaction = fixtures.transactions.loadTransactions().single()
                val repository: RoomRecurringBudgetRepository = RoomRecurringBudgetRepository(database, { "budget-1" }, { 500L })
                repository.save(RecurringBudgetDraft(null, "Household", 1_000L, setOf(account.id)), YearMonth.of(2026, 4))
                assertEquals(250L, repository.load(YearMonth.of(2026, 4)).single().spentMinor)
                fixtures.transactions.delete(transaction.id)
                assertEquals(0L, repository.load(YearMonth.of(2026, 4)).single().spentMinor)
            } finally {
                database.close()
            }
        }

    @Test
    fun invalidBudgetDraftDoesNotMutateSavedDefinition(): Unit =
        runTest {
            val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                val fixtures: FinanceTestFixtures = FinanceTestFixtures(database)
                val account: Account = fixtures.createFirstAccount()
                val repository: RoomRecurringBudgetRepository = RoomRecurringBudgetRepository(database, { "budget-1" }, { 500L })
                val month: YearMonth = YearMonth.of(2026, 1)
                repository.save(RecurringBudgetDraft(null, "Household", 1_000L, setOf(account.id)), month)
                assertRejectedDraft(repository, { RecurringBudgetDraft("budget-1", "Household", 0L, setOf(account.id)) }, month)
                assertRejectedDraft(repository, { RecurringBudgetDraft("budget-1", "Household", 1_000L, emptySet()) }, month)
                assertRejectedDraft(repository, { RecurringBudgetDraft("budget-1", " ", 1_000L, setOf(account.id)) }, month)
                val actualBudgets: List<RecurringBudgetProgress> = repository.load(month)
                assertEquals(1, actualBudgets.size)
                assertEquals("Household", actualBudgets.single().name)
                assertEquals(1_000L, actualBudgets.single().limitMinor)
                assertEquals(setOf(account.id), actualBudgets.single().accountIds)
            } finally {
                database.close()
            }
        }

    private suspend fun assertRejectedDraft(
        repository: RoomRecurringBudgetRepository,
        createDraft: () -> RecurringBudgetDraft,
        month: YearMonth,
    ) {
        try {
            repository.save(createDraft(), month)
            fail("Expected invalid Budget draft to be rejected")
        } catch (expectedException: IllegalArgumentException) {
            assertTrue(expectedException.message.orEmpty().isNotBlank())
        }
    }

    private suspend fun saveExpense(
        fixtures: FinanceTestFixtures,
        account: Account,
        amountMinor: Long,
        date: LocalDate,
    ) {
        fixtures.transactions.save(
            TransactionDraft(
                id = null,
                accountId = account.id,
                amountMinor = amountMinor,
                direction = TransactionDirection.EXPENSE,
                date = date.toEpochDay(),
                note = null,
                categoryId = null,
            ),
        )
    }
}
