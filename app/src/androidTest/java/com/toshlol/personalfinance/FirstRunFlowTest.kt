package com.toshlol.personalfinance

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.FirstRunSnapshot
import com.toshlol.personalfinance.data.RoomAccountRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirstRunFlowTest {
    private val composeRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> = createAndroidComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(ResetFinanceDataRule()).around(composeRule)

    @Test
    fun createFirstAccountAndKeepOverviewAcrossRelaunch() {
        waitForText("Set up your first account")
        composeRule.onNodeWithContentDescription("Currency").performClick()
        composeRule.onNodeWithText("EUR").performClick()
        composeRule.onNodeWithText("Account name").performTextInput(" Checking ")
        composeRule.onNodeWithText("Bank").performScrollTo().performClick()
        dismissKeyboard()
        composeRule.onNodeWithText("Create account").performScrollTo().performClick()
        waitForText("Overview")
        composeRule.onNodeWithText("EUR ledger is ready.").assertIsDisplayed()
        composeRule.onNodeWithText("No transactions yet").assertIsDisplayed()
        val actualSnapshot: FirstRunSnapshot =
            runBlocking {
                RoomAccountRepository(AppDatabase.getInstance(composeRule.activity)).loadSetup()
            }
        assertEquals("Checking", actualSnapshot.accounts.single().name)
        assertEquals(AccountType.BANK, actualSnapshot.accounts.single().type)
        composeRule.activityRule.scenario.recreate()
        waitForText("Overview")
        composeRule.activityRule.scenario.close()
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForText("Overview")
            composeRule.onNodeWithText("EUR ledger is ready.").assertIsDisplayed()
            composeRule.onNodeWithText("Set up your first account").assertDoesNotExist()
        }
    }

    @Test
    fun requireCurrencyAndAccountNameBeforeContinuing() {
        waitForText("Set up your first account")
        composeRule.onNodeWithText("Create account").performScrollTo().performClick()
        waitForText("Pick a supported currency before continuing.")
        composeRule.onNodeWithText("Pick a supported currency before continuing.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Currency").performScrollTo().performClick()
        composeRule.onNodeWithText("USD").performClick()
        composeRule.onNodeWithText("Create account").performScrollTo().performClick()
        waitForText("Enter an account name.")
        composeRule.onNodeWithText("Enter an account name.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Overview").assertDoesNotExist()
    }

    @Test
    fun retainUnsubmittedFormAcrossActivityRecreation() {
        waitForText("Set up your first account")
        composeRule.onNodeWithContentDescription("Currency").performClick()
        composeRule.onNodeWithText("GBP").performClick()
        composeRule.onNodeWithText("Account name").performTextInput("Travel cash")
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithText("Travel cash").assertIsDisplayed()
        composeRule.onNodeWithText("Currency: GBP").assertIsDisplayed()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(15_000) { composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }

    private fun dismissKeyboard() {
        composeRule.activity.runOnUiThread { composeRule.activity.currentFocus?.clearFocus() }
    }
}
