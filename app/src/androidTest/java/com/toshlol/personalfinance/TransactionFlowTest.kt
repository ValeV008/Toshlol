package com.toshlol.personalfinance

import android.content.Context
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.data.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
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
            .around(composeRule)
            .around(UiFailureArtifactsRule(composeRule))

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
        ui.enterText("Account name", "Wallet", "category-tag-list")
        ui.clickText("Save account name", "category-tag-list")
        ui.clickText("Back to overview")
        ui.showText("Wallet", substring = true)
    }

    @Test
    fun createBudgetAndSeeExpenseProgressOnBudgetsAndOverview() {
        createFoodCategory()
        ui.clickText("Back to overview")
        recordFoodExpense()
        ui.clickText("Budgets")
        ui.clickText("Food")
        ui.showText("Food").assertIsSelected()
        ui.enterText("Budget amount", "20.00")
        ui.clickText("Save budget")
        verifyRemainingBudgetOnBothScreens()
        ui.clickText("Expense \u00b7 EUR 12.50")
        ui.enterText("Amount", "25.00", "transaction-form")
        ui.clickText("Save")
        ui.showText("Food \u00b7 EUR 25.00 / 20.00 \u00b7 Over budget")
        ui.clickText("Budgets")
        verifyOverBudgetOnBothScreens()
        deleteExpenseAndVerifyBudgetOnBothScreens()
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
        ui.clickText("Budgets")
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
}
