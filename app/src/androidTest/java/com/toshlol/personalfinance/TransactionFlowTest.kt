package com.toshlol.personalfinance

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.RoomAccountRepository
import com.toshlol.personalfinance.domain.FirstRunSetup
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState
import com.toshlol.personalfinance.ui.viewmodel.TransactionsViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionFlowTest {
    private val composeRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> = createAndroidComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(ResetFinanceDataRule()).around(SeedAccountRule()).around(composeRule)

    @Test
    fun recordExpenseAndReviewItOnOverview() {
        waitForText("Overview")
        composeRule.onNodeWithText("Add expense").performClick()
        composeRule.onNodeWithText("Save").performClick()
        waitForText("Enter a positive amount and a valid date.")
        composeRule.onNodeWithText("Amount").performTextInput("12.50")
        composeRule.onNodeWithText("Date (YYYY-MM-DD)").performTextClearance()
        composeRule.onNodeWithText("Date (YYYY-MM-DD)").performTextInput("2026-02-30")
        composeRule.onNodeWithText("Save").performClick()
        waitForText("Enter a positive amount and a valid date.")
        composeRule.onNodeWithText("Date (YYYY-MM-DD)").performTextClearance()
        composeRule.onNodeWithText("Date (YYYY-MM-DD)").performTextInput(
            java.time.LocalDate
                .now()
                .toString(),
        )
        composeRule.onNodeWithText("Amount").performTextClearance()
        composeRule.onNodeWithText("Amount").performTextInput("12.50")
        composeRule.onNodeWithText("Save").performClick()
        waitForText("Expense \u00b7 EUR 12.50")
        dismissKeyboard()
        composeRule.onNodeWithText("Expense \u00b7 EUR 12.50").performScrollTo()
        composeRule.onNodeWithText("Expenses  EUR 12.50", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Net  EUR -12.50", substring = true).assertIsDisplayed()
        composeRule
            .onNodeWithText("Delete")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.waitUntil(15_000) { transactionsViewModelState().pendingDeleteId != null }
        waitForText("Delete transaction?")
        composeRule.onAllNodesWithText("Delete").get(1).performClick()
        waitForText("No transactions yet")
    }

    @Test
    fun createCategoryAndTagThenAssignThemToAnExpense() {
        waitForText("Overview")
        composeRule.onNodeWithText("Organize categories and tags").performClick()
        waitForText("Categories and tags")
        composeRule.onNodeWithText("Category name").performTextInput("Food")
        composeRule.onNodeWithTag("category-tag-list").performScrollToNode(hasText("Add category"))
        composeRule.onNodeWithText("Add category").assertIsDisplayed().performClick()
        composeRule.waitUntil(15_000) { !transactionsViewModelState().isSaving }
        val actualState: TransactionsUiState = transactionsViewModelState()
        assertTrue(
            "Category save state was ${actualState.error}; name was '${actualState.categoryName}'.",
            actualState.categories.any { it.name == "Food" },
        )
        composeRule.onNodeWithTag("category-tag-list").performScrollToNode(hasText("Food", substring = true))
        waitForText("Food")
        composeRule.onNodeWithTag("category-tag-list").performScrollToNode(hasText("Tag name"))
        waitForText("Tag name")
        composeRule.onNodeWithText("Tag name").performTextInput("Lunch")
        composeRule.onNodeWithTag("category-tag-list").performScrollToNode(hasText("Add tag"))
        composeRule.onNodeWithText("Add tag").assertIsDisplayed().performClick()
        composeRule.waitUntil(15_000) { !transactionsViewModelState().isSaving }
        val actualTagState: TransactionsUiState = transactionsViewModelState()
        assertTrue(
            "Tag save state was ${actualTagState.error}; name was '${actualTagState.tagName}'.",
            actualTagState.tags.any { it.name == "Lunch" },
        )
        composeRule.onNodeWithTag("category-tag-list").performScrollToNode(hasText("Lunch"))
        waitForText("Lunch")
        composeRule.onNodeWithText("Back to overview").performClick()
        composeRule.onNodeWithText("Organize categories and tags").performClick()
        composeRule.onNodeWithTag("category-tag-list").performScrollToNode(hasText("Lunch"))
        waitForText("Lunch")
        composeRule.onNodeWithText("Lunch").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Back to overview").performClick()
        composeRule.onNodeWithText("Add expense").performClick()
        composeRule.onNodeWithText("Amount").performTextInput("8.25")
        composeRule.onNodeWithTag("transaction-form").performScrollToNode(hasText("Food"))
        composeRule.onNodeWithText("Food").performClick()
        composeRule.onNodeWithTag("transaction-form").performScrollToNode(hasText("Lunch"))
        composeRule.onNodeWithText("Lunch").performClick()
        composeRule.onNodeWithText("Save").performClick()
        waitForText("Expense · EUR 8.25")
        composeRule.onNodeWithText("Food", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Lunch", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Organize categories and tags").performClick()
        composeRule.onNodeWithText("Account name").performTextClearance()
        composeRule.onNodeWithText("Account name").performTextInput("Wallet")
        composeRule
            .onNodeWithText("Save account name")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Back to overview").performClick()
        composeRule.onNodeWithText("Wallet", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun createBudgetAndSeeExpenseProgressInBudgetsScreen() {
        waitForText("Overview")
        composeRule.onNodeWithText("Organize categories and tags").performClick()
        waitForText("Categories and tags")
        composeRule.onNodeWithText("Category name").performTextInput("Food")
        composeRule.onNodeWithTag("category-tag-list").performScrollToNode(hasText("Add category"))
        composeRule.onNodeWithText("Add category").performClick()
        composeRule.waitUntil(15_000) { transactionsViewModelState().categories.any { it.name == "Food" } }
        composeRule.onNodeWithText("Back to overview").performClick()
        composeRule.onNodeWithText("Add expense").performClick()
        composeRule.onNodeWithText("Amount").performTextInput("12.50")
        composeRule.onNodeWithText("Category (optional)").performScrollTo()
        composeRule.onNodeWithText("Food").performClick()
        composeRule.onNodeWithText("Save").performClick()
        waitForText("Expense · EUR 12.50")
        dismissKeyboard()
        composeRule.onNodeWithText("Budgets").performClick()
        waitForText("Budgets")
        composeRule.onNodeWithText("Food").performClick()
        composeRule.waitUntil(15_000) { transactionsViewModelState().budgetCategoryId != null }
        composeRule.onNodeWithText("Budget amount").performScrollTo().performTextInput("10.00")
        dismissKeyboard()
        composeRule.onNodeWithText("Save budget").performScrollTo().performClick()
        composeRule.waitUntil(15_000) { transactionsViewModelState().budgets.isNotEmpty() || transactionsViewModelState().error != null }
        assertTrue(
            "Budget save state was ${transactionsViewModelState().error}; category was ${transactionsViewModelState().budgetCategoryId}.",
            transactionsViewModelState().budgets.isNotEmpty(),
        )
        waitForText("Spent  EUR 12.50")
        composeRule.onNodeWithText("Over budget  EUR 2.50").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Back to overview").performClick()
        waitForText("Budget progress")
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(15_000) { composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun transactionsViewModelState(): TransactionsUiState =
        ViewModelProvider(composeRule.activity)[TransactionsViewModel::class.java].uiState.value

    private fun dismissKeyboard() {
        composeRule.activity.runOnUiThread { composeRule.activity.currentFocus?.clearFocus() }
    }

    private class SeedAccountRule : ExternalResource() {
        override fun before() {
            val context: Context = ApplicationProvider.getApplicationContext()
            runBlocking {
                FirstRunSetup(RoomAccountRepository(AppDatabase.getInstance(context)))
                    .createFirstAccount("EUR", "Cash", AccountType.CASH)
            }
        }
    }
}
