@file:Suppress("ktlint:standard:filename", "ktlint:standard:no-unit-return")

package com.toshlol.personalfinance

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@RunWith(AndroidJUnit4::class)
class IntegratedAndroid10Test {
    private val composeRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> = createAndroidComposeRule()
    private val ui: FinanceUiDriver = FinanceUiDriver(composeRule)

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(ResetFinanceDataRule()).around(composeRule).around(UiFailureArtifactsRule(composeRule))

    @Test
    fun operateRedesignedAppOnAndroid10(): Unit {
        createAccountsThroughSetupAndManagement()
        createCategoryAndTag()
        recordAndEditIncomeAndExpense()
        recordAndEditTransferAndReviewBothHistories()
        createAndEditOverlappingBudgets()
        verifyMonthAndAccountSelection()
        cancelExportThroughAndroidPicker()
    }

    private fun createAccountsThroughSetupAndManagement(): Unit {
        ui.showText("Set up your first account")
        composeRule.onNodeWithContentDescription("Currency").performClick()
        composeRule.onNodeWithText("EUR").performClick()
        ui.enterText("Account name", "Cash")
        composeRule.onNodeWithText("Create account").performScrollTo().performClick()
        ui.showOverview()
        ui.showText("No activity for this month and Account selection.")
        capture("overview-empty-selection.png")
        ui.selectOverviewAccount("Cash")
        navigateTo("Accounts")
        ui.clickText("Add Account")
        ui.enterText("Account name", "Reserve")
        ui.clickText("Create Account")
        ui.showText("Reserve")
        navigateTo("Overview")
        ui.selectOverviewAccount("Reserve")
    }

    private fun createCategoryAndTag(): Unit {
        navigateTo("Categories & Tags")
        ui.enterText("Category name", "Food", "category-tag-list")
        ui.clickText("Add category", "category-tag-list")
        ui.showText("Food · expense", "category-tag-list")
        ui.enterText("Tag name", "Weekend", "category-tag-list")
        ui.clickText("Add tag", "category-tag-list")
        capture("categories-tags-integrated.png")
        composeRule.onNodeWithTag("classification-back").performClick()
        ui.showOverview()
    }

    private fun recordAndEditIncomeAndExpense(): Unit {
        ui.clickText("Add income")
        ui.enterText("Amount", "30.00", "transaction-form")
        ui.enterText("Note (optional)", "Wages", "transaction-form")
        ui.clickText("Save")
        ui.showText("Income · EUR 30.00")
        ui.clickText("Income · EUR 30.00")
        ui.enterText("Note (optional)", "Wages edited", "transaction-form")
        ui.clickText("Save")
        navigateTo("Income")
        ui.showText("Wages edited", substring = true)
        ui.showText("EUR 30.00")
        navigateTo("Expenses")
        ui.clickText("Add expense")
        ui.enterText("Amount", "12.00", "transaction-form")
        ui.enterText("Note (optional)", "Market", "transaction-form")
        ui.clickText("Food", "transaction-form")
        composeRule
            .onNodeWithTag("transaction-tag-${tagId("Weekend")}")
            .performScrollTo()
            .performClick()
            .assertIsOn()
        ui.clickText("Save")
        navigateTo("Overview")
        ui.showText("Expense · EUR 12.00")
        ui.clickText("Expense · EUR 12.00")
        ui.showText("Selected account: Cash", "transaction-form")
        ui.enterText("Note (optional)", "Market edited", "transaction-form")
        ui.clickText("Save")
        navigateTo("Expenses")
        ui.showText("Market edited", substring = true)
        ui.showText("EUR 12.00")
        capture("expenses-integrated.png")
    }

    private fun recordAndEditTransferAndReviewBothHistories(): Unit {
        ui.clickText("Transfer money")
        ui.enterText("Amount", "5.00", "transfer-form")
        ui.clickText("Save")
        ui.showText("Choose two different Accounts.", "transfer-form")
        capture("transfer-validation.png")
        selectTransferAccounts()
        ui.clickText("Save")
        navigateTo("Overview")
        ui.showText("Transfer · EUR 5.00")
        ui.clickText("Transfer · EUR 5.00")
        ui.enterText("Amount", "6.00", "transfer-form")
        ui.clickText("Save")
        navigateTo("Expenses")
        ui.showText("-EUR 6.00 · Transfer out")
        ui.showText("EUR 12.00")
        capture("transfer-expenses-paired.png")
        navigateTo("Income")
        ui.showText("+EUR 6.00 · Transfer in")
        ui.showText("EUR 30.00")
        capture("transfer-income-paired.png")
        navigateTo("Accounts")
        ui.clickText("View Cash history")
        ui.showText("Balance  EUR 12.00")
        ui.showText("-EUR 6.00 · Transfer out")
        capture("transfer-cash-history.png")
        ui.clickText("Back")
        ui.clickText("View Reserve history")
        ui.showText("Balance  EUR 6.00")
        ui.showText("+EUR 6.00 · Transfer in")
        capture("transfer-reserve-history.png")
        ui.clickText("Back")
    }

    private fun selectTransferAccounts(): Unit {
        val cashId: String = accountId("Cash")
        val reserveId: String = accountId("Reserve")
        composeRule
            .onNodeWithTag("transfer-source-$cashId")
            .performScrollTo()
            .performClick()
            .assertIsSelected()
        composeRule
            .onNodeWithTag("transfer-destination-$reserveId")
            .performScrollTo()
            .performClick()
            .assertIsSelected()
    }

    private fun createAndEditOverlappingBudgets(): Unit {
        navigateTo("Budgets")
        ui.enterText("Budget name", "Food limit")
        ui.enterText("Monthly limit (EUR)", "10.00")
        composeRule.onNodeWithTag("budget-account-${accountId("Cash")}").performScrollTo().performClick()
        composeRule.onNodeWithTag("budget-category-${categoryId("Food")}").performScrollTo().performClick()
        ui.clickText("Save Budget")
        ui.showText("Spent  EUR 12.00")
        ui.showText("Over budget")
        capture("budget-over-limit-integrated.png")
        ui.clickText("Edit Budget")
        ui.enterText("Monthly limit (EUR)", "20.00")
        ui.clickText("Save changes")
        ui.showText("Remaining  EUR 8.00")
        ui.enterText("Budget name", "Weekend spending")
        ui.enterText("Monthly limit (EUR)", "15.00")
        composeRule.onNodeWithTag("budget-account-${accountId("Cash")}").performScrollTo().performClick()
        composeRule.onNodeWithTag("budget-tag-${tagId("Weekend")}").performScrollTo().performClick()
        ui.clickText("Save Budget")
        ui.showText("Weekend spending")
        assertBothBudgetCardsShowSpending("12.00")
    }

    private fun verifyMonthAndAccountSelection(): Unit {
        navigateTo("Overview")
        ui.showText("Food limit")
        assertBothBudgetCardsShowSpending("12.00")
        composeRule.onNodeWithTag("previous-month").performClick()
        val previousMonth: String = YearMonth.now().minusMonths(1).format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        ui.showText(previousMonth)
        assertBothBudgetCardsShowSpending("0.00")
        composeRule.onNodeWithTag("next-month").performClick()
        assertBothBudgetCardsShowSpending("12.00")
        navigateTo("Expenses")
        ui.clickText("Accounts (2 selected)")
        composeRule.onNodeWithContentDescription("Cash Account selection").performClick().assertIsOff()
        composeRule.onNodeWithContentDescription("Reserve Account selection").performClick().assertIsOff()
        capture("drawer-empty-selection-integrated.png")
        ui.clickText("Close")
        navigateTo("Overview")
        ui.showText("No activity for this month and Account selection.")
        assertBothBudgetCardsShowSpending("12.00")
        capture("overview-empty-selection-with-budget.png")
    }

    private fun cancelExportThroughAndroidPicker(): Unit {
        val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        composeRule.onNodeWithTag("open-navigation-drawer").performClick()
        composeRule.onNodeWithTag("drawer-Export").performClick()
        assertTrue(device.wait(Until.hasObject(By.pkg("com.android.documentsui")), 10_000L))
        device.pressBack()
        ui.showText("Backup export canceled.")
        capture("settings-export-canceled.png")
    }

    private fun navigateTo(destination: String): Unit {
        composeRule.onNodeWithTag("open-navigation-drawer").performClick()
        composeRule.onNodeWithTag("drawer-$destination").performClick()
    }

    private fun assertBothBudgetCardsShowSpending(amount: String): Unit {
        val expectedText: String = "Spent  EUR $amount"
        composeRule.waitUntil(15_000L) { composeRule.onAllNodesWithText(expectedText).fetchSemanticsNodes().size == 2 }
        composeRule.onAllNodesWithText(expectedText).assertCountEquals(2)
    }

    private fun capture(fileName: String): Unit = saveTestScreenshot(TestScreenshotEvidence("integrated-android-10", fileName))

    private fun accountId(name: String): String =
        kotlinx.coroutines.runBlocking {
            FinanceTestFixtures(
                com.toshlol.personalfinance.data.AppDatabase
                    .getInstance(composeRule.activity),
            ).accounts.loadSetup().accounts.single { it.name == name }.id
        }

    private fun categoryId(name: String): String =
        kotlinx.coroutines.runBlocking {
            FinanceTestFixtures(
                com.toshlol.personalfinance.data.AppDatabase
                    .getInstance(composeRule.activity),
            ).organization.load().categories.single { it.name == name }.id
        }

    private fun tagId(name: String): String =
        kotlinx.coroutines.runBlocking {
            FinanceTestFixtures(
                com.toshlol.personalfinance.data.AppDatabase
                    .getInstance(composeRule.activity),
            ).organization.load().tags.single { it.name == name }.id
        }
}
