package com.toshlol.personalfinance

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.FinanceCategory
import com.toshlol.personalfinance.data.FinanceTag
import com.toshlol.personalfinance.data.RecurringBudgetDraft
import com.toshlol.personalfinance.data.RecurringBudgetProgress
import com.toshlol.personalfinance.data.RoomRecurringBudgetRepository
import com.toshlol.personalfinance.data.RoomTransferRepository
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.data.TransactionDraft
import com.toshlol.personalfinance.data.TransferDraft
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class RecurringBudgetMatchingTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun matchBudgetAcrossAccountCategoryAndTagSelections(): Unit =
        runTest {
            val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                val setup: BudgetMatchingSetup = createBudgetMatchingSetup(database)
                saveBudgetMatchingTransactions(setup)
                val actual: RecurringBudgetProgress = loadFilteredBudgetProgress(database, setup)
                assertFilteredBudgetProgress(actual, setup)
            } finally {
                database.close()
            }
        }

    private data class BudgetMatchingSetup(
        val database: AppDatabase,
        val fixtures: FinanceTestFixtures,
        val selectedAccount: Account,
        val otherAccount: Account,
        val groceryCategoryId: String,
        val diningCategoryId: String,
        val otherCategoryId: String,
        val weekendTagId: String,
        val holidayTagId: String,
    )

    private suspend fun createBudgetMatchingSetup(database: AppDatabase): BudgetMatchingSetup {
        val fixtures: FinanceTestFixtures = FinanceTestFixtures(database)
        val selectedAccount: Account = fixtures.createFirstAccount("Cash")
        val otherAccount: Account = fixtures.createAdditionalAccount("Savings")
        val categoryIds: BudgetMatchingCategoryIds = createBudgetMatchingCategories(fixtures)
        val tagIds: BudgetMatchingTagIds = createBudgetMatchingTags(fixtures)
        return BudgetMatchingSetup(
            database,
            fixtures,
            selectedAccount,
            otherAccount,
            categoryIds.grocery,
            categoryIds.dining,
            categoryIds.other,
            tagIds.weekend,
            tagIds.holiday,
        )
    }

    private data class BudgetMatchingCategoryIds(
        val grocery: String,
        val dining: String,
        val other: String,
    )

    private suspend fun createBudgetMatchingCategories(fixtures: FinanceTestFixtures): BudgetMatchingCategoryIds {
        val grocery: FinanceCategory = saveExpenseCategory(fixtures, "Groceries")
        val dining: FinanceCategory = saveExpenseCategory(fixtures, "Dining")
        val other: FinanceCategory = saveExpenseCategory(fixtures, "Transport")
        return BudgetMatchingCategoryIds(grocery.id, dining.id, other.id)
    }

    private data class BudgetMatchingTagIds(
        val weekend: String,
        val holiday: String,
    )

    private suspend fun createBudgetMatchingTags(fixtures: FinanceTestFixtures): BudgetMatchingTagIds {
        val weekend: FinanceTag = saveBudgetTag(fixtures, "weekend")
        val holiday: FinanceTag = saveBudgetTag(fixtures, "holiday")
        return BudgetMatchingTagIds(weekend.id, holiday.id)
    }

    @Suppress("ktlint:standard:no-unit-return")
    private suspend fun saveBudgetMatchingTransactions(setup: BudgetMatchingSetup): Unit {
        saveMatchingExpenses(setup)
        saveExcludedIncomeAndTransfer(setup)
    }

    @Suppress("ktlint:standard:no-unit-return")
    private suspend fun saveMatchingExpenses(setup: BudgetMatchingSetup): Unit {
        val fixtures: FinanceTestFixtures = setup.fixtures
        val selectedAccount: Account = setup.selectedAccount
        val otherAccount: Account = setup.otherAccount
        saveExpense(
            fixtures,
            BudgetTestExpense(selectedAccount, 20L, LocalDate.of(2026, 5, 2), setup.groceryCategoryId, setOf(setup.weekendTagId)),
        )
        saveExpense(
            fixtures,
            BudgetTestExpense(selectedAccount, 30L, LocalDate.of(2026, 5, 3), setup.diningCategoryId, setOf(setup.holidayTagId)),
        )
        saveExpense(
            fixtures,
            BudgetTestExpense(
                selectedAccount,
                10L,
                LocalDate.of(2026, 5, 4),
                setup.groceryCategoryId,
                setOf(setup.weekendTagId, setup.holidayTagId),
            ),
        )
        saveExpense(fixtures, BudgetTestExpense(selectedAccount, 15L, LocalDate.of(2026, 5, 5), setup.groceryCategoryId))
        saveExpense(
            fixtures,
            BudgetTestExpense(selectedAccount, 17L, LocalDate.of(2026, 5, 6), setup.otherCategoryId, setOf(setup.weekendTagId)),
        )
        saveExpense(
            fixtures,
            BudgetTestExpense(otherAccount, 99L, LocalDate.of(2026, 5, 7), setup.groceryCategoryId, setOf(setup.weekendTagId)),
        )
        saveExpense(
            fixtures,
            BudgetTestExpense(selectedAccount, 101L, LocalDate.of(2026, 6, 1), setup.groceryCategoryId, setOf(setup.weekendTagId)),
        )
    }

    @Suppress("ktlint:standard:no-unit-return")
    private suspend fun saveExcludedIncomeAndTransfer(setup: BudgetMatchingSetup): Unit {
        setup.fixtures.transactions.save(
            TransactionDraft(
                accountId = setup.selectedAccount.id,
                amountMinor = 200L,
                direction = TransactionDirection.INCOME,
                date = LocalDate.of(2026, 5, 8).toEpochDay(),
                note = null,
                categoryId = null,
                tagIds = setOf(setup.weekendTagId),
            ),
        )
        RoomTransferRepository(setup.database).save(
            TransferDraft(
                sourceAccountId = setup.selectedAccount.id,
                destinationAccountId = setup.otherAccount.id,
                amountMinor = 300L,
                date = LocalDate.of(2026, 5, 9).toEpochDay(),
            ),
        )
    }

    private suspend fun loadFilteredBudgetProgress(
        database: AppDatabase,
        setup: BudgetMatchingSetup,
    ): RecurringBudgetProgress {
        val repository: RoomRecurringBudgetRepository = RoomRecurringBudgetRepository(database, { "filtered-budget" }, { 500L })
        return repository
            .save(
                RecurringBudgetDraft(
                    null,
                    "Food weekends",
                    50L,
                    setOf(setup.selectedAccount.id),
                    setOf(setup.groceryCategoryId, setup.diningCategoryId),
                    setOf(setup.weekendTagId, setup.holidayTagId),
                ),
                YearMonth.of(2026, 5),
            ).single()
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun assertFilteredBudgetProgress(
        actual: RecurringBudgetProgress,
        setup: BudgetMatchingSetup,
    ): Unit {
        assertEquals(60L, actual.totals.spentMinor)
        assertEquals(-10L, actual.totals.remainingMinor)
        assertTrue(actual.totals.isOverBudget)
        assertEquals(setOf(setup.groceryCategoryId, setup.diningCategoryId), actual.restrictions.categoryIds)
        assertEquals(setOf(setup.weekendTagId, setup.holidayTagId), actual.restrictions.tagIds)
    }

    @Test
    fun supportUnrestrictedBudgetGroups(): Unit =
        runTest {
            val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                val scenario: UnrestrictedBudgetScenario = createUnrestrictedBudgetScenario(database)
                val actualByName: Map<String, Long> = saveAndLoadUnrestrictedBudgets(scenario)
                assertUnrestrictedBudgetTotals(actualByName)
                assertRejectedDraft(
                    scenario.repository,
                    { RecurringBudgetDraft(null, "Missing account", 1_000L, emptySet()) },
                    YearMonth.of(2026, 7),
                )
            } finally {
                database.close()
            }
        }

    private data class UnrestrictedBudgetScenario(
        val repository: RoomRecurringBudgetRepository,
        val account: Account,
        val categoryId: String,
        val tagId: String,
    )

    private suspend fun createUnrestrictedBudgetScenario(database: AppDatabase): UnrestrictedBudgetScenario {
        val fixtures: FinanceTestFixtures = FinanceTestFixtures(database)
        val account: Account = fixtures.createFirstAccount("Cash")
        val categoryId: String = saveExpenseCategory(fixtures, "Groceries").id
        val tagId: String = saveBudgetTag(fixtures, "weekend").id
        saveUnrestrictedBudgetExpenses(fixtures, account, categoryId, tagId)
        var budgetNumber: Int = 0
        val repository: RoomRecurringBudgetRepository =
            RoomRecurringBudgetRepository(database, { "unrestricted-${++budgetNumber}" }, { 500L })
        return UnrestrictedBudgetScenario(repository, account, categoryId, tagId)
    }

    @Suppress("ktlint:standard:no-unit-return")
    private suspend fun saveUnrestrictedBudgetExpenses(
        fixtures: FinanceTestFixtures,
        account: Account,
        categoryId: String,
        tagId: String,
    ): Unit {
        saveExpense(fixtures, BudgetTestExpense(account, 100L, LocalDate.of(2026, 7, 1), categoryId))
        saveExpense(fixtures, BudgetTestExpense(account, 200L, LocalDate.of(2026, 7, 2), categoryId, setOf(tagId)))
        saveExpense(fixtures, BudgetTestExpense(account, 300L, LocalDate.of(2026, 7, 3), tagIds = setOf(tagId)))
        saveExpense(fixtures, BudgetTestExpense(account, 400L, LocalDate.of(2026, 7, 4)))
    }

    private suspend fun saveAndLoadUnrestrictedBudgets(scenario: UnrestrictedBudgetScenario): Map<String, Long> {
        val month: YearMonth = YearMonth.of(2026, 7)
        scenario.repository.save(
            RecurringBudgetDraft(null, "Category only", 1_000L, setOf(scenario.account.id), setOf(scenario.categoryId)),
            month,
        )
        scenario.repository.save(
            RecurringBudgetDraft(null, "Tag only", 1_000L, setOf(scenario.account.id), tagIds = setOf(scenario.tagId)),
            month,
        )
        scenario.repository.save(RecurringBudgetDraft(null, "Unrestricted", 1_000L, setOf(scenario.account.id)), month)
        return scenario.repository.load(month).associate { it.name to it.totals.spentMinor }
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun assertUnrestrictedBudgetTotals(actualByName: Map<String, Long>): Unit {
        assertEquals(300L, actualByName.getValue("Category only"))
        assertEquals(500L, actualByName.getValue("Tag only"))
        assertEquals(1_000L, actualByName.getValue("Unrestricted"))
    }

    @Suppress("ktlint:standard:no-unit-return")
    private suspend fun assertRejectedDraft(
        repository: RoomRecurringBudgetRepository,
        createDraft: () -> RecurringBudgetDraft,
        month: YearMonth,
    ): Unit {
        try {
            repository.save(createDraft(), month)
            fail("Expected invalid Budget draft to be rejected")
        } catch (expectedException: IllegalArgumentException) {
            assertTrue(expectedException.message.orEmpty().isNotBlank())
        }
    }
}
