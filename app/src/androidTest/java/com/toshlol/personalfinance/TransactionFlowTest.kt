package com.toshlol.personalfinance

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.RoomAccountRepository
import com.toshlol.personalfinance.domain.FirstRunSetup
import kotlinx.coroutines.runBlocking
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
        composeRule.onNodeWithText("Expenses  EUR 12.50", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Net  EUR -12.50", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Cash", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Expense \u00b7 EUR 12.50", substring = true).performClick()
        composeRule.onNodeWithText("Amount").performTextClearance()
        composeRule.onNodeWithText("Amount").performTextInput("13.50")
        composeRule.onNodeWithText("Save").performClick()
        waitForText("Expense \u00b7 EUR 13.50")
        composeRule.onNodeWithText("Delete").performScrollTo().performClick()
        waitForText("Delete transaction?")
        composeRule.onAllNodesWithText("Delete").get(1).performClick()
        waitForText("No transactions yet")
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(15_000) { composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText(text).assertIsDisplayed()
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
