package com.toshlol.personalfinance

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.CategoryDraft
import com.toshlol.personalfinance.data.FinanceTransaction
import com.toshlol.personalfinance.data.RecurringBudgetDraft
import com.toshlol.personalfinance.data.RecurringBudgetProgress
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
    fun matchBudgetAcrossAccountCategoryAndTagSelections(): Unit =
        runTest {
            val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                val fixtures: FinanceTestFixtures = FinanceTestFixtures(database)
                val selectedAccount: Account = fixtures.createFirstAccount("Cash")
                val otherAccount: Account = fixtures.createAdditionalAccount("Savings")
                val groceryCategory =
                    fixtures.organization
                        .saveCategory(
                            CategoryDraft(name = "Groceries", direction = TransactionDirection.EXPENSE),
                        ).categories
                        .single { it.name == "Groceries" }
                val diningCategory =
                    fixtures.organization
                        .saveCategory(CategoryDraft(name = "Dining", direction = TransactionDirection.EXPENSE))
                        .categories
                        .single { it.name == "Dining" }
                val otherCategory =
                    fixtures.organization
                        .saveCategory(
                            CategoryDraft(name = "Transport", direction = TransactionDirection.EXPENSE),
                        ).categories
                        .single { it.name == "Transport" }
                val weekendTag =
                    fixtures.organization
                        .saveTag(TagDraft(name = "weekend"))
                        .tags
                        .single { it.name == "weekend" }
                val holidayTag =
                    fixtures.organization
                        .saveTag(TagDraft(name = "holiday"))
                        .tags
                        .single { it.name == "holiday" }
                saveExpense(fixtures, selectedAccount, 20L, LocalDate.of(2026, 5, 2), groceryCategory.id, setOf(weekendTag.id))
                saveExpense(fixtures, selectedAccount, 30L, LocalDate.of(2026, 5, 3), diningCategory.id, setOf(holidayTag.id))
                saveExpense(
                    fixtures,
                    selectedAccount,
                    10L,
                    LocalDate.of(2026, 5, 4),
                    groceryCategory.id,
                    setOf(weekendTag.id, holidayTag.id),
                )
                saveExpense(fixtures, selectedAccount, 15L, LocalDate.of(2026, 5, 5), groceryCategory.id)
                saveExpense(fixtures, selectedAccount, 17L, LocalDate.of(2026, 5, 6), otherCategory.id, setOf(weekendTag.id))
                saveExpense(fixtures, otherAccount, 99L, LocalDate.of(2026, 5, 7), groceryCategory.id, setOf(weekendTag.id))
                saveExpense(fixtures, selectedAccount, 101L, LocalDate.of(2026, 6, 1), groceryCategory.id, setOf(weekendTag.id))
                fixtures.transactions.save(
                    TransactionDraft(
                        accountId = selectedAccount.id,
                        amountMinor = 200L,
                        direction = TransactionDirection.INCOME,
                        date = LocalDate.of(2026, 5, 8).toEpochDay(),
                        note = null,
                        categoryId = null,
                        tagIds = setOf(weekendTag.id),
                    ),
                )
                RoomTransferRepository(database).save(
                    TransferDraft(
                        sourceAccountId = selectedAccount.id,
                        destinationAccountId = otherAccount.id,
                        amountMinor = 300L,
                        date = LocalDate.of(2026, 5, 9).toEpochDay(),
                    ),
                )
                val repository: RoomRecurringBudgetRepository = RoomRecurringBudgetRepository(database, { "filtered-budget" }, { 500L })
                val actual: RecurringBudgetProgress =
                    repository
                        .save(
                            RecurringBudgetDraft(
                                null,
                                "Food weekends",
                                50L,
                                setOf(selectedAccount.id),
                                setOf(groceryCategory.id, diningCategory.id),
                                setOf(weekendTag.id, holidayTag.id),
                            ),
                            YearMonth.of(2026, 5),
                        ).single()
                assertEquals(60L, actual.spentMinor)
                assertEquals(-10L, actual.remainingMinor)
                assertTrue(actual.isOverBudget)
                assertEquals(setOf(groceryCategory.id, diningCategory.id), actual.categoryIds)
                assertEquals(setOf(weekendTag.id, holidayTag.id), actual.tagIds)
            } finally {
                database.close()
            }
        }

    @Test
    fun supportUnrestrictedBudgetGroups(): Unit =
        runTest {
            val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                val fixtures: FinanceTestFixtures = FinanceTestFixtures(database)
                val account: Account = fixtures.createFirstAccount("Cash")
                val category =
                    fixtures.organization
                        .saveCategory(
                            CategoryDraft(name = "Groceries", direction = TransactionDirection.EXPENSE),
                        ).categories
                        .single { it.name == "Groceries" }
                val tag =
                    fixtures.organization
                        .saveTag(TagDraft(name = "weekend"))
                        .tags
                        .single { it.name == "weekend" }
                saveExpense(fixtures, account, 100L, LocalDate.of(2026, 7, 1), category.id)
                saveExpense(fixtures, account, 200L, LocalDate.of(2026, 7, 2), category.id, setOf(tag.id))
                saveExpense(fixtures, account, 300L, LocalDate.of(2026, 7, 3), tagIds = setOf(tag.id))
                saveExpense(fixtures, account, 400L, LocalDate.of(2026, 7, 4))
                var budgetNumber: Int = 0
                val repository: RoomRecurringBudgetRepository =
                    RoomRecurringBudgetRepository(database, { "unrestricted-${++budgetNumber}" }, { 500L })
                repository.save(
                    RecurringBudgetDraft(null, "Category only", 1_000L, setOf(account.id), setOf(category.id)),
                    YearMonth.of(2026, 7),
                )
                repository.save(
                    RecurringBudgetDraft(null, "Tag only", 1_000L, setOf(account.id), tagIds = setOf(tag.id)),
                    YearMonth.of(2026, 7),
                )
                repository.save(RecurringBudgetDraft(null, "Unrestricted", 1_000L, setOf(account.id)), YearMonth.of(2026, 7))
                val actualByName: Map<String, Long> = repository.load(YearMonth.of(2026, 7)).associate { it.name to it.spentMinor }
                assertEquals(300L, actualByName.getValue("Category only"))
                assertEquals(500L, actualByName.getValue("Tag only"))
                assertEquals(1_000L, actualByName.getValue("Unrestricted"))
                assertRejectedDraft(
                    repository,
                    { RecurringBudgetDraft(null, "Missing account", 1_000L, emptySet()) },
                    YearMonth.of(2026, 7),
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun preserveFiltersThroughRenamesAndArchives(): Unit =
        runTest {
            val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                val fixtures: FinanceTestFixtures = FinanceTestFixtures(database)
                val account: Account = fixtures.createFirstAccount("Cash")
                val category: com.toshlol.personalfinance.data.FinanceCategory =
                    fixtures.organization
                        .saveCategory(
                            CategoryDraft(name = "Groceries", direction = TransactionDirection.EXPENSE),
                        ).categories
                        .single()
                val tag: com.toshlol.personalfinance.data.FinanceTag =
                    fixtures.organization
                        .saveTag(TagDraft(name = "Weekend"))
                        .tags
                        .single()
                saveExpense(fixtures, account, 250L, LocalDate.of(2026, 9, 2), category.id, setOf(tag.id))
                val repository: RoomRecurringBudgetRepository = RoomRecurringBudgetRepository(database, { "archive-budget" }, { 500L })
                repository.save(
                    RecurringBudgetDraft(null, "Food", 200L, setOf(account.id), setOf(category.id), setOf(tag.id)),
                    YearMonth.of(2026, 9),
                )
                fixtures.organization.archiveCategory(category.id)
                fixtures.organization.archiveTag(tag.id)
                fixtures.organization.saveCategory(CategoryDraft(category.id, "Pantry", TransactionDirection.EXPENSE))
                fixtures.organization.saveTag(TagDraft(tag.id, "Occasion", null))
                val actual: RecurringBudgetProgress = repository.load(YearMonth.of(2026, 9)).single()
                val currentOrganization = fixtures.organization.load()
                assertEquals(setOf(category.id), actual.categoryIds)
                assertEquals(setOf(tag.id), actual.tagIds)
                assertEquals("Pantry", currentOrganization.categories.single { it.id == category.id }.name)
                assertTrue(currentOrganization.categories.single { it.id == category.id }.archived)
                assertEquals("Occasion", currentOrganization.tags.single { it.id == tag.id }.name)
                assertTrue(currentOrganization.tags.single { it.id == tag.id }.archived)
                assertEquals(250L, actual.spentMinor)
            } finally {
                database.close()
            }
        }

    @Test
    fun rejectMissingOrIncomeCategoryFiltersWithoutReplacingSavedBudget(): Unit =
        runTest {
            val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                val fixtures: FinanceTestFixtures = FinanceTestFixtures(database)
                val account: Account = fixtures.createFirstAccount("Cash")
                val expenseCategory =
                    fixtures.organization
                        .saveCategory(
                            CategoryDraft(name = "Groceries", direction = TransactionDirection.EXPENSE),
                        ).categories
                        .single { it.name == "Groceries" }
                val incomeCategory =
                    fixtures.organization
                        .saveCategory(CategoryDraft(name = "Salary", direction = TransactionDirection.INCOME))
                        .categories
                        .single { it.name == "Salary" }
                val tag =
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
                assertEquals(setOf(expenseCategory.id), unchanged.categoryIds)
                assertEquals(setOf(tag.id), unchanged.tagIds)
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
    fun savedNamedBudgetAndAccountScopeSurviveDatabaseReopen(): Unit =
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

    private fun openPersistentDatabase(name: String): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, name).build()

    private suspend fun createPersistentBudget(databaseName: String): RecurringBudgetProgress {
        val database: AppDatabase = openPersistentDatabase(databaseName)
        try {
            val fixtures: FinanceTestFixtures = FinanceTestFixtures(database)
            val selectedAccount: Account = fixtures.createFirstAccount("Cash")
            val excludedAccount: Account = fixtures.createAdditionalAccount("Savings")
            saveExpense(fixtures, selectedAccount, 250L, LocalDate.of(2026, 1, 4))
            saveExpense(fixtures, excludedAccount, 400L, LocalDate.of(2026, 1, 5))
            val repository: RoomRecurringBudgetRepository = RoomRecurringBudgetRepository(database, { "persistent-budget" }, { 500L })
            val expectedBudget: RecurringBudgetProgress =
                RecurringBudgetProgress(
                    "persistent-budget",
                    "Household",
                    YearMonth.of(2026, 1),
                    setOf(selectedAccount.id),
                    1_000L,
                    250L,
                    500L,
                    500L,
                )
            assertEquals(
                listOf(expectedBudget),
                repository.save(
                    RecurringBudgetDraft(null, "Household", 1_000L, setOf(selectedAccount.id)),
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

    private suspend fun saveExpense(
        fixtures: FinanceTestFixtures,
        account: Account,
        amountMinor: Long,
        date: LocalDate,
        categoryId: String? = null,
        tagIds: Set<String> = emptySet(),
    ) {
        fixtures.transactions.save(
            TransactionDraft(
                id = null,
                accountId = account.id,
                amountMinor = amountMinor,
                direction = TransactionDirection.EXPENSE,
                date = date.toEpochDay(),
                note = null,
                categoryId = categoryId,
                tagIds = tagIds,
            ),
        )
    }
}
