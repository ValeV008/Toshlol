package com.toshlol.personalfinance

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.BudgetDraft
import com.toshlol.personalfinance.data.CategoryDraft
import com.toshlol.personalfinance.data.FinanceCategory
import com.toshlol.personalfinance.data.FinanceTag
import com.toshlol.personalfinance.data.RoomTransferRepository
import com.toshlol.personalfinance.data.TagDraft
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.data.TransactionDraft
import com.toshlol.personalfinance.data.TransferDraft
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class TransactionFlowTest {
    private val composeRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> = createAndroidComposeRule()
    private val ui: FinanceUiDriver = FinanceUiDriver(composeRule)
    private var filterTestDate: LocalDate = LocalDate.now()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(ResetFinanceDataRule())
            .around(SeedAccountRule())
            .around(SeedLedgerRule())
            .around(composeRule)
            .around(UiFailureArtifactsRule(composeRule))

    @Before
    fun selectCashForOverview() {
        ui.selectOverviewAccount("Cash")
    }

    @Test
    fun accountListShowsTypesBalancesAndHistoryNavigation() {
        navigateTo("Accounts")
        ui.showText("Add Account")
        ui.showText("Type · Cash")
        ui.showText("Balance · EUR 0.00")
        ui.clickText("View Cash history")
        ui.showText("Cash history")
        ui.showText("No account activity yet")
    }

    @Test
    fun archiveAccountKeepsReadOnlyBalanceAndHistory() {
        navigateTo("Accounts")
        ui.clickText("Archive Cash")
        ui.showText("Type · Cash")
        ui.showText("Archived")
        ui.clickText("View Cash history")
        ui.showText("Cash history")
        ui.showText("Balance  EUR 0.00")
        ui.showText("No account activity yet")
    }

    @Test
    fun editTransactionPreservesAccountAndReusesLastAccountAcrossDirections() {
        ui.clickText("Add expense")
        ui.enterText("Amount", "8.25", "transaction-form")
        val savingsAccountTag: String = "transaction-account-${accountId("Savings")}"
        composeRule.onNodeWithTag(savingsAccountTag).performClick()
        ui.clickText("Save")
        ui.selectOverviewAccount("Savings")
        ui.clickText("Expense \u00b7 EUR 8.25")
        ui.enterText("Note (optional)", "kept on Savings", "transaction-form")
        ui.clickText("Save")
        ui.clickText("Add income")
        ui.showText("Selected account: Savings", "transaction-form")
        ui.enterText("Amount", "1.00", "transaction-form")
        ui.clickText("Save")
        ui.showText("Income \u00b7 EUR 1.00")
    }

    @Test
    @Suppress("ktlint:standard:no-unit-return")
    fun createEditFilterAndDeleteTransferWithoutChangingIncomeOrExpenseTotals(): Unit {
        ui.clickText("Categories & Tags")
        ui.clickText("Manage accounts", "category-tag-list")
        ui.enterText("New account name", "Savings", "category-tag-list")
        ui.enterText("New account opening balance", "3.00", "category-tag-list")
        ui.clickText("Create account", "category-tag-list")
        ui.showText("Savings · Bank · EUR 3.00", "category-tag-list")
        ui.click(hasText("Edit Cash", substring = true), "category-tag-list")
        ui.click(hasText("Edit Savings", substring = true), "category-tag-list")
        ui.enterText("Account opening balance", "5.00", "category-tag-list")
        ui.enterText("Account opening date (YYYY-MM-DD)", "2026-01-01", "category-tag-list")
        ui.clickText("Save account details", "category-tag-list")
        ui.showText("Savings · Bank · EUR 5.00", "category-tag-list")
        ui.click(hasText("Edit Cash", substring = true), "category-tag-list")
        ui.click(hasText("Edit Savings", substring = true), "category-tag-list")
        ui.showText("2026-01-01", "category-tag-list")
        ui.clickText("Back to overview")
        ui.clickText("Add expense")
        ui.enterText("Amount", "1.00", "transaction-form")
        ui.enterText("Date (YYYY-MM-DD)", "2020-01-01", "transaction-form")
        ui.clickText("Save")
        navigateTo("Transactions")
        ui.enterText("Month (YYYY-MM, blank for all)", "")
        ui.showText("Expense \u00b7 EUR 1.00")
        ui.enterText("Month (YYYY-MM, blank for all)", YearMonth.now().toString())
        navigateTo("Overview")
        ui.selectOverviewAccount("Savings")
        composeRule.onNodeWithText("Expense \u00b7 EUR 1.00").assertDoesNotExist()
        ui.clickText("Transfer")
        ui.showText("New Transfer")
        composeRule.onNodeWithTag("transfer-source-${accountId("Cash")}").assertIsNotSelected()
        composeRule.onNodeWithTag("transfer-source-${accountId("Savings")}").assertIsNotSelected()
        composeRule.onNodeWithTag("transfer-destination-${accountId("Cash")}").assertIsNotSelected()
        composeRule.onNodeWithTag("transfer-destination-${accountId("Savings")}").assertIsNotSelected()
        ui.showText(LocalDate.now().toString(), "transfer-form")
        ui.enterText("Amount", "0", "transfer-form")
        ui.clickText("Save")
        ui.showText("Enter a positive amount", "transfer-form", substring = true)
        ui.enterText("Amount", "10.00", "transfer-form")
        ui.enterText("Date (YYYY-MM-DD)", LocalDate.now().toString(), "transfer-form")
        composeRule.onNodeWithTag("transfer-source-${accountId("Cash")}").performClick()
        composeRule.onNodeWithTag("transfer-destination-${accountId("Cash")}").performClick()
        ui.clickText("Save")
        ui.showText("Choose two different Accounts.", "transfer-form")
        composeRule.onNodeWithTag("transfer-destination-${accountId("Savings")}").performClick()
        ui.clickText("Save")
        ui.showText("Transfer · EUR 10.00")
        showOverviewAccount("Cash", "-10.00")
        showOverviewAccount("Savings", "15.00")
        ui.showOverview()
        composeRule.onAllNodesWithText("0.00").assertCountEquals(3)
        composeRule.onNodeWithText("Expense \u00b7 EUR 1.00").assertDoesNotExist()
        ui.clickText("Categories & Tags")
        ui.clickText("Manage accounts", "category-tag-list")
        ui.click(hasText("Editing Savings", substring = true), "category-tag-list")
        ui.enterText("Account name", "Reserve", "category-tag-list")
        ui.clickText("Save account details", "category-tag-list")
        ui.clickText("Back to overview")
        showOverviewAccount("Cash", "-10.00").performClick()
        ui.showText("Cash history")
        ui.showText("Transfer out · EUR 10.00")
        ui.showText("To Reserve", substring = true)
        ui.clickText("Back to overview")
        showOverviewAccount("Reserve", "15.00").performClick()
        ui.showText("Reserve history")
        ui.showText("Transfer in · EUR 10.00")
        ui.showText("From Cash", substring = true)
        ui.clickText("Back to overview")
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
        ui.showText("EUR 4.00", substring = true)
        navigateTo("Overview")
        showOverviewAccount("Cash", "-4.00")
        showOverviewAccount("Reserve", "9.00")
        navigateTo("Transactions")
        ui.click(hasText("Transfer out", substring = true))
        ui.clickText("Delete")
        ui.showText("Delete Transfer?")
        ui.click(hasText("Delete") and hasAnyAncestor(isDialog()))
        ui.showText("No transactions match these filters.")
        navigateTo("Overview")
        showOverviewAccount("Cash", "0.00")
        showOverviewAccount("Reserve", "5.00")
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
        ui.showText("Expenses")
        ui.showText("12.50")
        ui.showText("Net")
        ui.showText("-12.50")
        ui.clickText("Delete")
        ui.showText("Delete transaction?")
        ui.click(hasText("Delete") and hasAnyAncestor(isDialog()))
        ui.showText("No activity for this month and Account selection.")
    }

    @Test
    fun createCategoryAndTagThenAssignThemToAnExpense() {
        createFoodCategory()
        ui.enterText("Tag name", "Lunch", "category-tag-list")
        composeRule.onNode(hasText("Lunch") and hasSetTextAction()).performImeAction()
        ui.clickText("Add tag", "category-tag-list")
        ui.show(hasText("Lunch") and !hasSetTextAction(), "category-tag-list")
        ui.clickText("Back to overview")
        ui.clickText("Categories & Tags")
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
        ui.clickText("Categories & Tags")
        ui.clickText("Manage accounts", "category-tag-list")
        ui.enterText("Account name", "Wallet", "category-tag-list")
        ui.show(hasText("Wallet") and hasSetTextAction(), "category-tag-list")
        ui.clickText("Save account details", "category-tag-list")
        ui.showText("Wallet · Cash · EUR -8.25", "category-tag-list", substring = true)
        ui.clickText("Back to overview")
        showOverviewAccount("Wallet", "-8.25")
    }

    @Test
    fun changeTransactionDirectionScopesCategoriesAndRemovesAssociatedTags() {
        ui.clickText("Add expense")
        ui.enterText("Amount", "8.25", "transaction-form")
        ui.clickText("Food", "transaction-form")
        composeRule.onNodeWithTag("transaction-tag-${tagId("Lunch")}").performScrollTo().performClick()
        composeRule.onNodeWithTag("transaction-tag-${tagId("Reimbursable")}").performScrollTo().performClick()
        composeRule.onNodeWithTag("transaction-tag-${tagId("Lunch")}").assertIsOn()
        composeRule.onNodeWithTag("transaction-tag-${tagId("Reimbursable")}").assertIsOn()
        ui.clickText("Save")
        ui.clickText("Expense \u00b7 EUR 8.25")
        composeRule.onNodeWithTag("transaction-direction-INCOME").performClick()
        ui.showText("Uncategorized", "transaction-form").assertIsSelected()
        ui.showText("Salary", "transaction-form")
        composeRule.onNodeWithTag("transaction-tag-${tagId("Lunch")}").assertIsNotEnabled()
        ui.showText("Reimbursable", "transaction-form").assertIsOn()
        ui.clickText("Salary", "transaction-form")
        ui.clickText("Save")
        ui.showText("Income \u00b7 EUR 8.25")
        ui.showText("Salary", substring = true)
        ui.showText("Reimbursable", substring = true)
        composeRule.onNodeWithText("Food", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Lunch", substring = true).assertDoesNotExist()
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
        ui.showText("EUR 25.00 of 20.00")
        ui.showText("Over budget")
        navigateTo("Budgets")
        verifyOverBudgetOnBothScreens()
        deleteExpenseAndVerifyBudgetOnBothScreens()
    }

    @Test
    fun transferDoesNotContributeToBudgetActualSpending() {
        navigateTo("Budgets")
        ui.showText("Spent  EUR 0.00")
        ui.showText("Limit  EUR 10.00")
        ui.showText("Remaining  EUR 10.00")
        ui.clickText("Back to overview")
        ui.showText("Transfer \u00b7 EUR 5.00")
        ui.showText("Food")
        ui.showText("EUR 0.00 of 10.00")
        ui.showText("Remaining EUR 10.00")
    }

    @Test
    fun navigateBudgetMonthsAndOfferOnlyExpenseCategories() {
        navigateTo("Budgets")
        ui.showText("Month (YYYY-MM)").assert(hasSetTextAction())
        ui.showText("Limit  EUR 10.00")
        composeRule.onNodeWithTag("budget-category-${categoryId("Food")}").assertHasClickAction()
        composeRule.onNodeWithText("Pay").assertDoesNotExist()
        composeRule.onNodeWithTag("budget-previous-month").performClick()
        ui.showText("Limit  EUR 15.00")
        ui.showText("Month (YYYY-MM)").assert(hasText(YearMonth.now().minusMonths(1).toString()))
        composeRule.onNodeWithTag("budget-next-month").performClick()
        ui.showText("Limit  EUR 10.00")
        ui.showText("Month (YYYY-MM)").assert(hasText(YearMonth.now().toString()))
    }

    @Test
    fun navigateFilterTransactionsAndQuickAddFromEveryMainArea() {
        val currentDate: LocalDate = filterTestDate
        ui.showOverview()
        navigateTo("Transactions")
        ui.enterText("Month (YYYY-MM, blank for all)", "")
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
        showTransactionRow("Expense · EUR 8.25")
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
        ui.enterText("Month (YYYY-MM, blank for all)", YearMonth.now().toString())
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
        if (area == "Overview") {
            ui.showOverview()
            return
        }
        val screenText: String =
            when (area) {
                "Transactions" -> "Month (YYYY-MM, blank for all)"
                "Accounts" -> "Balance · EUR 0.00"
                "Budgets" -> "Monthly expense limits"
                else -> "Currency: EUR"
            }
        ui.showText(screenText)
    }

    private fun showOverviewAccount(
        name: String,
        amount: String,
    ): androidx.compose.ui.test.SemanticsNodeInteraction = ui.show(hasText(name) and hasText("EUR $amount"))

    private fun showTransactionRow(text: String) {
        ui.dismissKeyboard()
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    }

    private fun accountId(name: String): String {
        val context: Context = ApplicationProvider.getApplicationContext()
        return runBlocking {
            FinanceTestFixtures(
                AppDatabase.getInstance(context),
            ).accounts.loadSetup().accounts.single { it.name == name }.id
        }
    }

    private fun tagId(name: String): String {
        val context: Context = ApplicationProvider.getApplicationContext()
        return runBlocking {
            FinanceTestFixtures(
                AppDatabase.getInstance(context),
            ).organization.load().tags.single { it.name == name }.id
        }
    }

    private fun categoryId(name: String): String {
        val context: Context = ApplicationProvider.getApplicationContext()
        return runBlocking {
            FinanceTestFixtures(
                AppDatabase.getInstance(context),
            ).organization.load().categories.single { it.name == name }.id
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
        ui.showText("Food")
        ui.showText("EUR 12.50 of 20.00")
        ui.showText("Remaining EUR 7.50")
    }

    private fun verifyOverBudgetOnBothScreens() {
        ui.showText("Spent  EUR 25.00")
        ui.showText("Limit  EUR 20.00")
        ui.showText("Over budget  EUR 5.00")
        ui.clickText("Back to overview")
        ui.showText("Budget progress")
        ui.showText("Food")
        ui.showText("EUR 25.00 of 20.00")
        ui.showText("Over budget")
    }

    private fun deleteExpenseAndVerifyBudgetOnBothScreens() {
        ui.clickText("Delete")
        ui.showText("Delete transaction?")
        ui.click(hasText("Delete") and hasAnyAncestor(isDialog()))
        ui.showText("EUR 0.00 of 20.00")
        ui.showText("Remaining EUR 20.00")
        navigateTo("Budgets")
        ui.showText("Spent  EUR 0.00")
        ui.showText("Limit  EUR 20.00")
        ui.showText("Remaining  EUR 20.00")
        ui.clickText("Back to overview")
        ui.showText("EUR 0.00 of 20.00")
        ui.showText("Remaining EUR 20.00")
    }

    private fun createFoodCategory() {
        ui.clickText("Categories & Tags")
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

    private inner class SeedLedgerRule : TestWatcher() {
        override fun starting(description: Description) {
            if (description.methodName == "editTransactionPreservesAccountAndReusesLastAccountAcrossDirections") {
                val context: Context = ApplicationProvider.getApplicationContext()
                runBlocking {
                    FinanceTestFixtures(AppDatabase.getInstance(context)).createAdditionalAccount("Savings")
                }
                return
            }
            if (description.methodName == "changeTransactionDirectionScopesCategoriesAndRemovesAssociatedTags") {
                val context: Context = ApplicationProvider.getApplicationContext()
                runBlocking {
                    val fixtures: FinanceTestFixtures = FinanceTestFixtures(AppDatabase.getInstance(context))
                    val food: FinanceCategory =
                        fixtures.organization
                            .saveCategory(CategoryDraft(name = "Food", direction = TransactionDirection.EXPENSE))
                            .categories
                            .single()
                    fixtures.organization.saveCategory(CategoryDraft(name = "Salary", direction = TransactionDirection.INCOME))
                    fixtures.organization.saveTag(TagDraft(name = "Lunch", categoryId = food.id))
                    fixtures.organization.saveTag(TagDraft(name = "Reimbursable"))
                }
                return
            }
            if (description.methodName == "transferDoesNotContributeToBudgetActualSpending") {
                val context: Context = ApplicationProvider.getApplicationContext()
                runBlocking {
                    val fixtures: FinanceTestFixtures = FinanceTestFixtures(AppDatabase.getInstance(context))
                    val source: Account =
                        fixtures.accounts
                            .loadSetup()
                            .accounts
                            .single()
                    val destination: Account = fixtures.createAdditionalAccount("Savings")
                    val category: FinanceCategory =
                        fixtures.organization
                            .saveCategory(CategoryDraft(name = "Food", direction = TransactionDirection.EXPENSE))
                            .categories
                            .single()
                    fixtures.budgets.save(
                        BudgetDraft(category.id, YearMonth.now(), 1000L),
                    )
                    RoomTransferRepository(AppDatabase.getInstance(context)).save(
                        TransferDraft(
                            sourceAccountId = source.id,
                            destinationAccountId = destination.id,
                            amountMinor = 500L,
                            date = LocalDate.now().toEpochDay(),
                        ),
                    )
                }
                return
            }
            if (description.methodName == "navigateBudgetMonthsAndOfferOnlyExpenseCategories") {
                val context: Context = ApplicationProvider.getApplicationContext()
                runBlocking {
                    val fixtures: FinanceTestFixtures = FinanceTestFixtures(AppDatabase.getInstance(context))
                    val food: FinanceCategory =
                        fixtures.organization
                            .saveCategory(CategoryDraft(name = "Food", direction = TransactionDirection.EXPENSE))
                            .categories
                            .single()
                    fixtures.organization.saveCategory(CategoryDraft(name = "Pay", direction = TransactionDirection.INCOME))
                    fixtures.budgets.save(BudgetDraft(food.id, YearMonth.now(), 1000L))
                    fixtures.budgets.save(BudgetDraft(food.id, YearMonth.now().minusMonths(1), 1500L))
                }
                return
            }
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
            filterTestDate = LocalDate.now()
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
                        date = filterTestDate.toEpochDay(),
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
                        date = filterTestDate.toEpochDay(),
                        note = null,
                    ),
                )
                fixtures.transactions.save(
                    TransactionDraft(
                        accountId = account.id,
                        amountMinor = 500,
                        direction = TransactionDirection.EXPENSE,
                        date = filterTestDate.toEpochDay(),
                        note = null,
                        categoryId = transportCategory.id,
                    ),
                )
                fixtures.transactions.save(
                    TransactionDraft(
                        accountId = account.id,
                        amountMinor = 300,
                        direction = TransactionDirection.EXPENSE,
                        date = filterTestDate.toEpochDay(),
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
                        date = filterTestDate.minusMonths(1).toEpochDay(),
                        note = null,
                    ),
                )
            }
        }
    }
}
