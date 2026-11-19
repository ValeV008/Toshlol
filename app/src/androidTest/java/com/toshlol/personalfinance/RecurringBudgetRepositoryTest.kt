package com.toshlol.personalfinance

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.CategoryDraft
import com.toshlol.personalfinance.data.CategoryTagSnapshot
import com.toshlol.personalfinance.data.FinanceCategory
import com.toshlol.personalfinance.data.FinanceTag
import com.toshlol.personalfinance.data.FinanceTransaction
import com.toshlol.personalfinance.data.RecurringBudgetDraft
import com.toshlol.personalfinance.data.RecurringBudgetProgress
import com.toshlol.personalfinance.data.RecurringBudgetRestrictions
import com.toshlol.personalfinance.data.RecurringBudgetTotals
import com.toshlol.personalfinance.data.RoomRecurringBudgetRepository
import com.toshlol.personalfinance.data.RoomTransferRepository
import com.toshlol.personalfinance.data.TagDraft
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
    fun preserveFiltersThroughRenamesAndArchives(): Unit =
        runTest {
            val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                assertArchivedBudgetPreserved(createArchivedBudgetResult(database))
            } finally {
                database.close()
            }
        }

    private data class ArchivedBudgetResult(
        val categoryId: String,
        val tagId: String,
        val progress: RecurringBudgetProgress,
        val organization: CategoryTagSnapshot,
    )

    private suspend fun createArchivedBudgetResult(database: AppDatabase): ArchivedBudgetResult {
        val fixtures: FinanceTestFixtures = FinanceTestFixtures(database)
        val account: Account = fixtures.createFirstAccount("Cash")
        val category: FinanceCategory = saveExpenseCategory(fixtures, "Groceries")
        val tag: FinanceTag = saveBudgetTag(fixtures, "Weekend")
        saveExpense(fixtures, BudgetTestExpense(account, 250L, LocalDate.of(2026, 9, 2), category.id, setOf(tag.id)))
        val repository: RoomRecurringBudgetRepository = RoomRecurringBudgetRepository(database, { "archive-budget" }, { 500L })
        repository.save(
            RecurringBudgetDraft(null, "Food", 200L, setOf(account.id), setOf(category.id), setOf(tag.id)),
            YearMonth.of(2026, 9),
        )
        fixtures.organization.archiveCategory(category.id)
        fixtures.organization.archiveTag(tag.id)
        fixtures.organization.saveCategory(CategoryDraft(category.id, "Pantry", TransactionDirection.EXPENSE))
        fixtures.organization.saveTag(TagDraft(tag.id, "Occasion", null))
        return ArchivedBudgetResult(
            category.id,
            tag.id,
            repository.load(YearMonth.of(2026, 9)).single(),
            fixtures.organization.load(),
        )
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun assertArchivedBudgetPreserved(result: ArchivedBudgetResult): Unit {
        assertEquals(setOf(result.categoryId), result.progress.restrictions.categoryIds)
        assertEquals(setOf(result.tagId), result.progress.restrictions.tagIds)
        assertEquals(
            "Pantry",
            result.organization.categories
                .single { it.id == result.categoryId }
                .name,
        )
        assertTrue(
            result.organization.categories
                .single { it.id == result.categoryId }
                .archived,
        )
        assertEquals(
            "Occasion",
            result.organization.tags
                .single { it.id == result.tagId }
                .name,
        )
        assertTrue(
            result.organization.tags
                .single { it.id == result.tagId }
                .archived,
        )
        assertEquals(250L, result.progress.totals.spentMinor)
    }

    @Test
    fun editCategoryAndTagFiltersRecalculatesPastCurrentAndFutureMonths(): Unit =
        runTest {
            val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                val fixtures: FinanceTestFixtures = FinanceTestFixtures(database)
                val setup: BudgetMonthFilterSetup = createBudgetMonthFilterSetup(database, fixtures)
                saveBudgetMonthFilterTransactions(setup, fixtures)
                val edit: BudgetMonthFilterEdit = saveEditedBudgetMonthFilter(setup)
                assertBudgetMonthFilterProgress(setup, edit)
            } finally {
                database.close()
            }
        }

    private data class BudgetMonthFilterSetup(
        val repository: RoomRecurringBudgetRepository,
        val account: Account,
        val groceryCategoryId: String,
        val diningCategoryId: String,
        val weekendTagId: String,
        val holidayTagId: String,
        val pastMonth: YearMonth,
        val currentMonth: YearMonth,
        val futureMonth: YearMonth,
    )

    private data class BudgetMonthFilterEdit(
        val original: RecurringBudgetProgress,
        val updated: RecurringBudgetProgress,
    )

    private data class BudgetMonthFilterExpense(
        val setup: BudgetMonthFilterSetup,
        val amountMinor: Long,
        val month: YearMonth,
        val categoryId: String,
        val tagId: String,
    )

    private suspend fun createBudgetMonthFilterSetup(
        database: AppDatabase,
        fixtures: FinanceTestFixtures,
    ): BudgetMonthFilterSetup {
        val account: Account = fixtures.createFirstAccount("Cash")
        val groceryCategoryId: String = saveExpenseCategory(fixtures, "Groceries").id
        val diningCategoryId: String = saveExpenseCategory(fixtures, "Dining").id
        val weekendTagId: String = saveBudgetTag(fixtures, "Weekend").id
        val holidayTagId: String = saveBudgetTag(fixtures, "Holiday").id
        val currentMonth: YearMonth = YearMonth.now()
        return BudgetMonthFilterSetup(
            RoomRecurringBudgetRepository(database, { "edited-budget" }, { 500L }),
            account,
            groceryCategoryId,
            diningCategoryId,
            weekendTagId,
            holidayTagId,
            currentMonth.minusMonths(1),
            currentMonth,
            currentMonth.plusMonths(1),
        )
    }

    @Suppress("ktlint:standard:no-unit-return")
    private suspend fun saveBudgetMonthFilterTransactions(
        setup: BudgetMonthFilterSetup,
        fixtures: FinanceTestFixtures,
    ): Unit {
        saveMonthlyCategoryTagExpense(
            fixtures,
            BudgetMonthFilterExpense(setup, 100L, setup.pastMonth, setup.groceryCategoryId, setup.weekendTagId),
        )
        saveMonthlyCategoryTagExpense(
            fixtures,
            BudgetMonthFilterExpense(setup, 200L, setup.currentMonth, setup.groceryCategoryId, setup.weekendTagId),
        )
        saveMonthlyCategoryTagExpense(
            fixtures,
            BudgetMonthFilterExpense(setup, 300L, setup.futureMonth, setup.groceryCategoryId, setup.weekendTagId),
        )
        saveMonthlyCategoryTagExpense(
            fixtures,
            BudgetMonthFilterExpense(setup, 400L, setup.pastMonth, setup.diningCategoryId, setup.holidayTagId),
        )
        saveMonthlyCategoryTagExpense(
            fixtures,
            BudgetMonthFilterExpense(setup, 500L, setup.currentMonth, setup.diningCategoryId, setup.holidayTagId),
        )
        saveMonthlyCategoryTagExpense(
            fixtures,
            BudgetMonthFilterExpense(setup, 600L, setup.futureMonth, setup.diningCategoryId, setup.holidayTagId),
        )
    }

    @Suppress("ktlint:standard:no-unit-return")
    private suspend fun saveMonthlyCategoryTagExpense(
        fixtures: FinanceTestFixtures,
        expense: BudgetMonthFilterExpense,
    ): Unit =
        saveExpense(
            fixtures,
            BudgetTestExpense(
                expense.setup.account,
                expense.amountMinor,
                expense.month.atDay(2),
                expense.categoryId,
                setOf(expense.tagId),
            ),
        )

    private suspend fun saveEditedBudgetMonthFilter(setup: BudgetMonthFilterSetup): BudgetMonthFilterEdit {
        val original: RecurringBudgetProgress =
            setup.repository
                .save(
                    RecurringBudgetDraft(
                        null,
                        "Household",
                        1_000L,
                        setOf(setup.account.id),
                        setOf(setup.groceryCategoryId),
                        setOf(setup.weekendTagId),
                    ),
                    setup.currentMonth,
                ).single()
        val updated: RecurringBudgetProgress =
            setup.repository
                .save(
                    RecurringBudgetDraft(
                        original.id,
                        original.name,
                        original.totals.limitMinor,
                        original.accountIds,
                        setOf(setup.diningCategoryId),
                        setOf(setup.holidayTagId),
                    ),
                    setup.currentMonth,
                ).single()
        return BudgetMonthFilterEdit(original, updated)
    }

    @Suppress("ktlint:standard:no-unit-return")
    private suspend fun assertBudgetMonthFilterProgress(
        setup: BudgetMonthFilterSetup,
        edit: BudgetMonthFilterEdit,
    ): Unit {
        val pastBudget: RecurringBudgetProgress = setup.repository.load(setup.pastMonth).single()
        val currentBudget: RecurringBudgetProgress = setup.repository.load(setup.currentMonth).single()
        val futureBudget: RecurringBudgetProgress = setup.repository.load(setup.futureMonth).single()
        assertEquals(edit.original.id, edit.updated.id)
        assertEquals(500L, edit.updated.totals.spentMinor)
        assertEquals(400L, pastBudget.totals.spentMinor)
        assertEquals(500L, currentBudget.totals.spentMinor)
        assertEquals(600L, futureBudget.totals.spentMinor)
        assertEquals(setOf(setup.diningCategoryId), pastBudget.restrictions.categoryIds)
        assertEquals(setOf(setup.holidayTagId), futureBudget.restrictions.tagIds)
    }

    @Test
    fun rejectMissingOrIncomeCategoryFiltersWithoutReplacingSavedBudget(): Unit =
        runTest {
            val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                val fixtures: FinanceTestFixtures = FinanceTestFixtures(database)
                val account: Account = fixtures.createFirstAccount("Cash")
                val expenseCategory: FinanceCategory =
                    fixtures.organization
                        .saveCategory(
                            CategoryDraft(name = "Groceries", direction = TransactionDirection.EXPENSE),
                        ).categories
                        .single { it.name == "Groceries" }
                val incomeCategory: FinanceCategory =
                    fixtures.organization
                        .saveCategory(CategoryDraft(name = "Salary", direction = TransactionDirection.INCOME))
                        .categories
                        .single { it.name == "Salary" }
                val tag: FinanceTag =
                    fixtures.organization
                        .saveTag(TagDraft(name = "Weekend"))
                        .tags
                        .single { it.name == "Weekend" }
                val month: YearMonth = YearMonth.of(2026, 10)
                val repository: RoomRecurringBudgetRepository = RoomRecurringBudgetRepository(database, { "validated-budget" }, { 500L })
                repository.save(
                    RecurringBudgetDraft(null, "Food", 200L, setOf(account.id), setOf(expenseCategory.id), setOf(tag.id)),
                    month,
                )
                assertRejectedDraft(
                    repository,
                    { RecurringBudgetDraft("validated-budget", "Food", 200L, setOf(account.id), setOf(incomeCategory.id), setOf(tag.id)) },
                    month,
                )
                assertRejectedDraft(
                    repository,
                    { RecurringBudgetDraft("validated-budget", "Food", 200L, setOf(account.id), setOf("missing"), setOf(tag.id)) },
                    month,
                )
                assertRejectedDraft(
                    repository,
                    {
                        RecurringBudgetDraft(
                            "validated-budget",
                            "Food",
                            200L,
                            setOf(account.id),
                            setOf(expenseCategory.id),
                            setOf("missing"),
                        )
                    },
                    month,
                )
                val unchanged: RecurringBudgetProgress = repository.load(month).single()
                assertEquals(setOf(expenseCategory.id), unchanged.restrictions.categoryIds)
                assertEquals(setOf(tag.id), unchanged.restrictions.tagIds)
            } finally {
                database.close()
            }
        }

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
                        RecurringBudgetTotals(1_000L, 250L),
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
    fun preserveNamedBudgetRestrictionsAcrossDatabaseReopen(): Unit =
        runTest {
            val databaseName: String = "recurring-budget-persistence-${System.nanoTime()}.db"
            context.deleteDatabase(databaseName)
            try {
                val expectedBudget: RecurringBudgetProgress = createPersistentBudget(databaseName)
                assertPersistentBudgetReopens(databaseName, expectedBudget)
            } finally {
                context.deleteDatabase(databaseName)
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
                saveExpense(fixtures, BudgetTestExpense(firstAccount, 600L, LocalDate.of(2026, 1, 4)))
                saveExpense(fixtures, BudgetTestExpense(secondAccount, 500L, LocalDate.of(2026, 1, 5)))
                saveExpense(fixtures, BudgetTestExpense(firstAccount, 300L, LocalDate.of(2026, 2, 2)))
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
                assertEquals(700L, actualJanuary.totals.limitMinor)
                assertEquals(500L, actualJanuary.totals.spentMinor)
                assertEquals(200L, actualJanuary.totals.remainingMinor)
                assertFalse(actualJanuary.totals.isOverBudget)
                assertEquals(500L, actualJanuary.createdAt)
                assertEquals(900L, actualJanuary.updatedAt)
                assertEquals(0L, actualFebruary.totals.spentMinor)
                assertEquals(setOf(secondAccount.id), actualFebruary.accountIds)
                assertTrue(
                    repository
                        .load(YearMonth.of(2027, 1))
                        .single()
                        .totals.spentMinor == 0L,
                )
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
                saveExpense(fixtures, BudgetTestExpense(firstAccount, 300L, LocalDate.of(2026, 3, 4)))
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
                saveExpense(fixtures, BudgetTestExpense(secondAccount, 500L, LocalDate.of(2026, 3, 6)))
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
                saveExpense(fixtures, BudgetTestExpense(newAccount, 1_000L, LocalDate.of(2026, 3, 8)))
                val actualBudgets: List<RecurringBudgetProgress> = repository.load(YearMonth.of(2026, 3))
                val actualCashBudget: RecurringBudgetProgress = actualBudgets.single { it.id == "budget-1" }
                val actualAllSpendingBudget: RecurringBudgetProgress = actualBudgets.single { it.id == "budget-2" }
                assertEquals(300L, actualCashBudget.totals.spentMinor)
                assertTrue(actualCashBudget.totals.isOverBudget)
                assertEquals(800L, actualAllSpendingBudget.totals.spentMinor)
                assertEquals(setOf(firstAccount.id, secondAccount.id), actualAllSpendingBudget.accountIds)
                assertEquals(1_200L, actualAllSpendingBudget.totals.remainingMinor)
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
                saveExpense(fixtures, BudgetTestExpense(account, 250L, LocalDate.of(2026, 4, 4)))
                val transaction: FinanceTransaction = fixtures.transactions.loadTransactions().single()
                val repository: RoomRecurringBudgetRepository = RoomRecurringBudgetRepository(database, { "budget-1" }, { 500L })
                repository.save(RecurringBudgetDraft(null, "Household", 1_000L, setOf(account.id)), YearMonth.of(2026, 4))
                assertEquals(
                    250L,
                    repository
                        .load(YearMonth.of(2026, 4))
                        .single()
                        .totals.spentMinor,
                )
                fixtures.transactions.delete(transaction.id)
                assertEquals(
                    0L,
                    repository
                        .load(YearMonth.of(2026, 4))
                        .single()
                        .totals.spentMinor,
                )
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
                assertEquals(1_000L, actualBudgets.single().totals.limitMinor)
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

    private fun openPersistentDatabase(name: String): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, name).build()

    private suspend fun createPersistentBudget(databaseName: String): RecurringBudgetProgress {
        val database: AppDatabase = openPersistentDatabase(databaseName)
        try {
            val fixtures: FinanceTestFixtures = FinanceTestFixtures(database)
            val selectedAccount: Account = fixtures.createFirstAccount("Cash")
            val excludedAccount: Account = fixtures.createAdditionalAccount("Savings")
            val category: FinanceCategory = saveExpenseCategory(fixtures, "Groceries")
            val tag: FinanceTag = saveBudgetTag(fixtures, "weekend")
            saveExpense(fixtures, BudgetTestExpense(selectedAccount, 250L, LocalDate.of(2026, 1, 4), category.id, setOf(tag.id)))
            saveExpense(fixtures, BudgetTestExpense(excludedAccount, 400L, LocalDate.of(2026, 1, 5)))
            val repository: RoomRecurringBudgetRepository = RoomRecurringBudgetRepository(database, { "persistent-budget" }, { 500L })
            val expectedBudget: RecurringBudgetProgress =
                RecurringBudgetProgress(
                    id = "persistent-budget",
                    name = "Household",
                    month = YearMonth.of(2026, 1),
                    accountIds = setOf(selectedAccount.id),
                    totals = RecurringBudgetTotals(1_000L, 250L),
                    createdAt = 500L,
                    updatedAt = 500L,
                    restrictions = RecurringBudgetRestrictions(setOf(category.id), setOf(tag.id)),
                )
            assertEquals(
                listOf(expectedBudget),
                repository.save(
                    RecurringBudgetDraft(
                        null,
                        "Household",
                        1_000L,
                        setOf(selectedAccount.id),
                        categoryIds = setOf(category.id),
                        tagIds = setOf(tag.id),
                    ),
                    YearMonth.of(2026, 1),
                ),
            )
            return expectedBudget
        } finally {
            database.close()
        }
    }

    @Suppress("ktlint:standard:no-unit-return")
    private suspend fun assertPersistentBudgetReopens(
        databaseName: String,
        expectedBudget: RecurringBudgetProgress,
    ): Unit {
        val reopenedDatabase: AppDatabase = openPersistentDatabase(databaseName)
        try {
            val repository: RoomRecurringBudgetRepository =
                RoomRecurringBudgetRepository(reopenedDatabase, { "unused-budget-id" }, { 900L })
            assertEquals(listOf(expectedBudget), repository.load(YearMonth.of(2026, 1)))
        } finally {
            reopenedDatabase.close()
        }
    }
}
