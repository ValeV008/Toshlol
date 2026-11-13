package com.toshlol.personalfinance

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.CategoryDraft
import com.toshlol.personalfinance.data.FinanceCategory
import com.toshlol.personalfinance.data.FinanceTag
import com.toshlol.personalfinance.data.TagDraft
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.data.TransactionDraft
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TransactionFlowTest {
    private val composeRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> = createAndroidComposeRule()
    private val ui: FinanceUiDriver = FinanceUiDriver(composeRule)

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(ResetFinanceDataRule())
            .around(SeedAccountRule())
            .around(SeedLedgerRule())
            .around(composeRule)
            .around(UiFailureArtifactsRule(composeRule))

    @Test
    fun createEditFilterAndDeleteTransferWithoutChangingIncomeOrExpenseTotals() {
        ui.clickText("Organize categories and tags")
        ui.clickText("Manage accounts", "category-tag-list")
        ui.enterText("New account name", "Savings", "category-tag-list")
        ui.enterText("New account opening balance", "3.00", "category-tag-list")
        ui.clickText("Create account", "category-tag-list")
        ui.showText("Savings · EUR 3.00", "category-tag-list")
        ui.clickText("Back to overview")
        ui.clickText("Transfer")
        ui.showText("New Transfer")
        ui.enterText("Amount", "0", "transfer-form")
        ui.clickText("Save")
        ui.showText("Enter a positive amount", "transfer-form", substring = true)
        ui.enterText("Amount", "10.00", "transfer-form")
        composeRule.onNodeWithTag("transfer-destination-${accountId("Cash")}").performClick()
        ui.clickText("Save")
        ui.showText("Choose two different Accounts.", "transfer-form")
        composeRule.onNodeWithTag("transfer-destination-${accountId("Savings")}").performClick()
        ui.clickText("Save")
        ui.showText("Transfer · EUR 10.00")
        ui.showText("Cash  EUR -10.00")
        ui.showText("Savings  EUR 13.00")
        ui.showText("Income  EUR 0.00", substring = true)
        ui.showText("Expenses  EUR 0.00", substring = true)
        navigateTo("Transactions")
        ui.enterText("Month (YYYY-MM, blank for all)", LocalDate.now().toString().take(7))
        ui.showText("Transfer out", substring = true)
        assertTrue(
            composeRule
                .onAllNodes(hasText("Transfer in", substring = true))
                .fetchSemanticsNodes()
                .isNotEmpty(),
        )
        ui.clickText("Account: All Account")
        ui.clickText("Cash")
        ui.showText("Transfer out", substring = true)
        ui.clickText("Category: All Category")
        ui.clickText("Food")
        ui.showText("No transactions match these filters.")
        ui.clickText("Category: Food")
        ui.clickText("All Category")
        ui.clickText("Tag: All Tag")
        ui.clickText("Lunch")
        ui.showText("No transactions match these filters.")
        ui.clickText("Tag: Lunch")
        ui.clickText("All Tag")
        ui.showText("Transfer out", substring = true)
        ui.click(hasText("Transfer out", substring = true))
        ui.enterText("Amount", "4.00", "transfer-form")
        ui.clickText("Save")
        ui.showText("Transfer out", substring = true)
        navigateTo("Overview")
        ui.showText("Cash  EUR -4.00")
        ui.showText("Savings  EUR 7.00")
        navigateTo("Transactions")
        ui.click(hasText("Transfer out", substring = true))
        ui.clickText("Delete")
        ui.showText("Delete Transfer?")
        ui.click(hasText("Delete") and hasAnyAncestor(isDialog()))
        ui.showText("No transactions match these filters.")
        navigateTo("Overview")
        ui.showText("Cash  EUR 0.00")
        ui.showText("Savings  EUR 3.00")
    }

    @Test
    fun recordExpenseAndReviewItOnOverview() {
        ui.clickText("Add expense")
        ui.clickText("Save")
        ui.showText("Enter a positive amount and a valid date.", "transaction-form")
        ui.enterText("Amount", "12.50", "transaction-form")
        ui.enterText("Date (YYYY-MM-DD)", "2026-02-30", "transaction-form")
        ui.clickText("Save")
        ui.showText("Enter a positive amount and a valid date.", "transaction-form")
        ui.enterText("Date (YYYY-MM-DD)", LocalDate.now().toString(), "transaction-form")
        ui.clickText("Save")
        ui.showText("Expense \u00b7 EUR 12.50")
        ui.showText("Expenses  EUR 12.50", substring = true)
        ui.showText("Net  EUR -12.50")
        ui.clickText("Delete")
        ui.showText("Delete transaction?")
        ui.click(hasText("Delete") and hasAnyAncestor(isDialog()))
        ui.showText("No transactions yet")
    }

    @Test
    fun createCategoryAndTagThenAssignThemToAnExpense() {
        createFoodCategory()
        ui.enterText("Tag name", "Lunch", "category-tag-list")
        ui.clickText("Add tag", "category-tag-list")
        ui.show(hasText("Lunch") and !hasSetTextAction(), "category-tag-list")
        ui.clickText("Back to overview")
        ui.clickText("Organize categories and tags")
        ui.show(hasText("Lunch") and !hasSetTextAction(), "category-tag-list")
        ui.clickText("Back to overview")
        ui.clickText("Add expense")
        ui.enterText("Amount", "8.25", "transaction-form")
        ui.clickText("Food", "transaction-form")
        ui.showText("Food", "transaction-form").assertIsSelected()
        ui.clickText("Lunch", "transaction-form")
        ui.showText("Lunch", "transaction-form").assertIsOn()
        ui.clickText("Save")
        ui.showText("Expense \u00b7 EUR 8.25")
        ui.showText("Food", substring = true)
        ui.showText("Lunch", substring = true)
        ui.clickText("Organize categories and tags")
        ui.clickText("Manage accounts", "category-tag-list")
        ui.enterText("Account name", "Wallet", "category-tag-list")
        ui.clickText("Save account name", "category-tag-list")
        ui.showText("Wallet · EUR -8.25", "category-tag-list", substring = true)
        ui.clickText("Back to overview")
        ui.showText("Wallet  EUR -8.25", substring = true)
    }

    @Test
    fun createBudgetAndSeeExpenseProgressOnBudgetsAndOverview() {
        createFoodCategory()
        ui.clickText("Back to overview")
        recordFoodExpense()
        composeRule.onNodeWithTag("open-budgets").performClick()
        ui.clickText("Food")
        ui.showText("Food").assertIsSelected()
        ui.enterText("Budget amount", "20.00")
        ui.clickText("Save budget")
        verifyRemainingBudgetOnBothScreens()
        ui.clickText("Expense \u00b7 EUR 12.50")
        ui.enterText("Amount", "25.00", "transaction-form")
        ui.clickText("Save")
        ui.showText("Food \u00b7 EUR 25.00 / 20.00 \u00b7 Over budget")
        navigateTo("Budgets")
        verifyOverBudgetOnBothScreens()
        deleteExpenseAndVerifyBudgetOnBothScreens()
    }

    @Test
    fun navigateFilterTransactionsAndQuickAddFromEveryMainArea() {
        val currentDate: LocalDate = LocalDate.now()
        ui.showText("EUR ledger is ready.")
        navigateTo("Transactions")
        ui.showText("Month (YYYY-MM, blank for all)").assert(hasSetTextAction())
        ui.showText("Account: All Account").assertHasClickAction()
        ui.showText("Category: All Category").assertHasClickAction()
        ui.showText("Tag: All Tag").assertHasClickAction()
        ui.showText("Add expense").assertHasClickAction()
        ui.showText("Expense · EUR 8.25")
        ui.showText("Expense · EUR 4.00")
        val newerPosition: Float =
            composeRule
                .onNodeWithText("Expense · EUR 8.25")
                .fetchSemanticsNode()
                .boundsInRoot.top
        val olderPosition: Float =
            composeRule
                .onNodeWithText("Expense · EUR 4.00")
                .fetchSemanticsNode()
                .boundsInRoot.top
        assertTrue(newerPosition < olderPosition)
        ui.enterText("Month (YYYY-MM, blank for all)", currentDate.toString().take(7))
        ui.showText("Expense · EUR 8.25")
        composeRule.onNodeWithText("Expense · EUR 4.00").assertDoesNotExist()
        ui.enterText("Month (YYYY-MM, blank for all)", currentDate.minusMonths(1).toString().take(7))
        ui.showText("Expense · EUR 4.00")
        composeRule.onNodeWithText("Expense · EUR 8.25").assertDoesNotExist()
        ui.enterText("Month (YYYY-MM, blank for all)", currentDate.minusYears(5).toString().take(7))
        ui.showText("No transactions match these filters.")
        ui.enterText("Month (YYYY-MM, blank for all)", currentDate.toString().take(7))
        ui.clickText("Account: All Account")
        ui.clickText("Cash")
        composeRule.onNodeWithText("EUR 6.00", substring = true).assertDoesNotExist()
        ui.clickText("Account: Cash")
        ui.clickText("All Account")
        ui.showText("EUR 6.00", substring = true)
        ui.showText("Expense · EUR 8.25")
        ui.clickText("Category: All Category")
        ui.clickText("Food")
        composeRule.onNodeWithText("EUR 5.00", substring = true).assertDoesNotExist()
        ui.clickText("Category: Food")
        ui.clickText("All Category")
        ui.showText("EUR 5.00", substring = true)
        ui.showText("Expense · EUR 8.25")
        ui.clickText("Tag: All Tag")
        ui.clickText("Lunch")
        composeRule.onNodeWithText("EUR 3.00", substring = true).assertDoesNotExist()
        ui.clickText("Tag: Lunch")
        ui.clickText("All Tag")
        ui.showText("EUR 3.00", substring = true)
        ui.showText("Expense · EUR 8.25")
        ui.enterText("Month (YYYY-MM, blank for all)", "")
        ui.showText("EUR 4.00", substring = true)
        navigateTo("Overview")
        ui.showText("Add expense").assertHasClickAction()
        ui.clickText("Add expense")
        ui.showText("New transaction")
        ui.clickText("Cancel")
        navigateTo("Transactions")
        ui.showText("Add expense").assertHasClickAction()
        ui.clickText("Add expense")
        ui.showText("New transaction")
        ui.clickText("Cancel")
        navigateTo("Budgets")
        ui.showText("Add transaction").assertHasClickAction()
        ui.clickText("Add transaction")
        ui.showText("New transaction")
        ui.clickText("Cancel")
        navigateTo("Settings")
        ui.showText("Currency: EUR")
        ui.showText("Manage categories and tags").assertHasClickAction()
        ui.clickText("Manage categories and tags")
        ui.showText("Category name", "category-tag-list")
        ui.clickText("Back to overview")
        navigateTo("Settings")
        ui.clickText("Add transaction")
        ui.showText("New transaction")
    }

    private fun navigateTo(area: String) {
        val navigationTag: String = "navigation-$area"
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithTag(navigationTag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(navigationTag, useUnmergedTree = true).performClick()
        composeRule.waitUntil(15_000) {
            runCatching {
                composeRule
                    .onNodeWithTag(navigationTag, useUnmergedTree = true)
                    .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Current section"))
            }.isSuccess
        }
        val screenText: String =
            when (area) {
                "Overview" -> "EUR ledger is ready."
                "Transactions" -> "Month (YYYY-MM, blank for all)"
                "Budgets" -> "Monthly expense limits"
                else -> "Currency: EUR"
            }
        ui.showText(screenText)
    }

    private fun accountId(name: String): String {
        val context: Context = ApplicationProvider.getApplicationContext()
        return runBlocking {
            FinanceTestFixtures(
                AppDatabase.getInstance(context),
            ).accounts.loadSetup().accounts.single { it.name == name }.id
        }
    }

    private fun recordFoodExpense() {
        ui.clickText("Add expense")
        ui.enterText("Amount", "12.50", "transaction-form")
        ui.clickText("Food", "transaction-form")
        ui.showText("Food", "transaction-form").assertIsSelected()
        ui.clickText("Save")
        ui.showText("Expense \u00b7 EUR 12.50")
    }

    private fun verifyRemainingBudgetOnBothScreens() {
        ui.showText("Spent  EUR 12.50")
        ui.showText("Limit  EUR 20.00")
        ui.showText("Remaining  EUR 7.50")
        ui.clickText("Back to overview")
        ui.showText("Budget progress")
        ui.showText("Food \u00b7 EUR 12.50 / 20.00 \u00b7 Remaining 7.50")
    }

    private fun verifyOverBudgetOnBothScreens() {
        ui.showText("Spent  EUR 25.00")
        ui.showText("Limit  EUR 20.00")
        ui.showText("Over budget  EUR 5.00")
        ui.clickText("Back to overview")
        ui.showText("Budget progress")
        ui.showText("Food \u00b7 EUR 25.00 / 20.00 \u00b7 Over budget")
    }

    private fun deleteExpenseAndVerifyBudgetOnBothScreens() {
        ui.clickText("Delete")
        ui.showText("Delete transaction?")
        ui.click(hasText("Delete") and hasAnyAncestor(isDialog()))
        ui.showText("Food \u00b7 EUR 0.00 / 20.00 \u00b7 Remaining 20.00")
        navigateTo("Budgets")
        ui.showText("Spent  EUR 0.00")
        ui.showText("Limit  EUR 20.00")
        ui.showText("Remaining  EUR 20.00")
        ui.clickText("Back to overview")
        ui.showText("Food \u00b7 EUR 0.00 / 20.00 \u00b7 Remaining 20.00")
    }

    private fun createFoodCategory() {
        ui.clickText("Organize categories and tags")
        ui.enterText("Category name", "Food", "category-tag-list")
        ui.clickText("Add category", "category-tag-list")
        ui.showText("Food \u00b7 expense", "category-tag-list")
    }

    private class SeedAccountRule : ExternalResource() {
        override fun before() {
            val context: Context = ApplicationProvider.getApplicationContext()
            runBlocking { FinanceTestFixtures(AppDatabase.getInstance(context)).createFirstAccount() }
        }
    }

    private class SeedLedgerRule : TestWatcher() {
        override fun starting(description: Description) {
            if (description.methodName == "createEditFilterAndDeleteTransferWithoutChangingIncomeOrExpenseTotals") {
                val context: Context = ApplicationProvider.getApplicationContext()
                runBlocking {
                    val fixtures: FinanceTestFixtures = FinanceTestFixtures(AppDatabase.getInstance(context))
                    val category: FinanceCategory =
                        fixtures.organization
                            .saveCategory(
                                CategoryDraft(name = "Food", direction = TransactionDirection.EXPENSE),
                            ).categories
                            .single()
                    fixtures.organization.saveTag(TagDraft(name = "Lunch", categoryId = category.id))
                }
                return
            }
            if (description.methodName != "navigateFilterTransactionsAndQuickAddFromEveryMainArea") return
            val context: Context = ApplicationProvider.getApplicationContext()
            runBlocking {
                val fixtures: FinanceTestFixtures = FinanceTestFixtures(AppDatabase.getInstance(context))
                val account: Account =
                    fixtures.accounts
                        .loadSetup()
                        .accounts
                        .single()
                val savingsAccount: Account = fixtures.createAdditionalAccount("Savings")
                val category: FinanceCategory =
                    fixtures.organization
                        .saveCategory(CategoryDraft(name = "Food", direction = TransactionDirection.EXPENSE))
                        .categories
                        .single()
                val tag: FinanceTag =
                    fixtures.organization
                        .saveTag(TagDraft(name = "Lunch", categoryId = category.id))
                        .tags
                        .single()
                val transportCategory: FinanceCategory =
                    fixtures.organization
                        .saveCategory(CategoryDraft(name = "Transport", direction = TransactionDirection.EXPENSE))
                        .categories
                        .single { it.name == "Transport" }
                val dinnerTag: FinanceTag =
                    fixtures.organization
                        .saveTag(TagDraft(name = "Dinner", categoryId = category.id))
                        .tags
                        .single { it.name == "Dinner" }
                fixtures.transactions.save(
                    TransactionDraft(
                        accountId = account.id,
                        amountMinor = 825,
                        direction = TransactionDirection.EXPENSE,
                        date = LocalDate.now().toEpochDay(),
                        note = null,
                        categoryId = category.id,
                        tagIds = setOf(tag.id),
                    ),
                )
                fixtures.transactions.save(
                    TransactionDraft(
                        accountId = savingsAccount.id,
                        amountMinor = 600,
                        direction = TransactionDirection.EXPENSE,
                        date = LocalDate.now().toEpochDay(),
                        note = null,
                    ),
                )
                fixtures.transactions.save(
                    TransactionDraft(
                        accountId = account.id,
                        amountMinor = 500,
                        direction = TransactionDirection.EXPENSE,
                        date = LocalDate.now().toEpochDay(),
                        note = null,
                        categoryId = transportCategory.id,
                    ),
                )
                fixtures.transactions.save(
                    TransactionDraft(
                        accountId = account.id,
                        amountMinor = 300,
                        direction = TransactionDirection.EXPENSE,
                        date = LocalDate.now().toEpochDay(),
                        note = null,
                        categoryId = category.id,
                        tagIds = setOf(dinnerTag.id),
                    ),
                )
                fixtures.transactions.save(
                    TransactionDraft(
                        accountId = account.id,
                        amountMinor = 400,
                        direction = TransactionDirection.EXPENSE,
                        date = LocalDate.now().minusMonths(1).toEpochDay(),
                        note = null,
                    ),
                )
            }
        }
    }
}
