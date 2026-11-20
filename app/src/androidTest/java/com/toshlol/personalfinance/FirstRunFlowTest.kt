@file:Suppress("ktlint:standard:no-unit-return")

package com.toshlol.personalfinance

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.FirstRunSnapshot
import com.toshlol.personalfinance.data.RoomAccountRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class FirstRunFlowTest {
    private val composeRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> = createAndroidComposeRule()

    private val ui: FinanceUiDriver = FinanceUiDriver(composeRule)

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(ResetFinanceDataRule()).around(composeRule).around(UiFailureArtifactsRule(composeRule))

    @Test
    fun createFirstAccountAndKeepOverviewAcrossRelaunch(): Unit {
        createFirstBankAccount()
        assertCurrentNavigationArea()
        val createdAccountId: String = assertCompletedSetupAndGetAccountId()
        assertCreatedAccountIsSelectableForTransaction(createdAccountId)
        recreateSetupAndVerifyEmptyOverview()
        retainSelectedAndClearedOverviewSelectionAcrossRelaunches()
    }

    private fun createFirstBankAccount(): Unit {
        waitForText("Set up your first account")
        composeRule.onNodeWithContentDescription("Currency").performClick()
        composeRule.onNodeWithText("EUR").performClick()
        composeRule.onNodeWithText("Account name").performTextInput(" Checking ")
        dismissKeyboard()
        composeRule
            .onNodeWithContentDescription("Bank account type")
            .performScrollTo()
            .performClick()
            .assertIsSelected()
        ui.clickText("Create account")
        waitForText("Overview")
        assertInitialEmptyOverview()
    }

    private fun assertCompletedSetupAndGetAccountId(): String {
        ui.showText("EUR 0.00")
        ui.showText("No activity for this month and Account selection.")
        val actualSnapshot: FirstRunSnapshot =
            runBlocking {
                RoomAccountRepository(AppDatabase.getInstance(composeRule.activity)).loadSetup()
            }
        assertEquals("Checking", actualSnapshot.accounts.single().name)
        assertEquals(AccountType.BANK, actualSnapshot.accounts.single().type)
        return actualSnapshot.accounts.single().id
    }

    private fun assertCreatedAccountIsSelectableForTransaction(accountId: String): Unit {
        composeRule.onNodeWithTag("open-navigation-drawer").performClick()
        composeRule.onNodeWithTag("drawer-Expenses").performClick()
        ui.clickText("Add expense")
        composeRule.onNodeWithTag("transaction-account-$accountId").performClick().assertIsSelected()
        ui.clickText("Cancel")
        composeRule.onNodeWithTag("open-navigation-drawer").performClick()
        composeRule.onNodeWithTag("drawer-Overview").performClick()
        waitForText("Overview")
        assertInitialEmptyOverview()
    }

    private fun recreateSetupAndVerifyEmptyOverview(): Unit {
        composeRule.activityRule.scenario.recreate()
        waitForText("Overview")
        composeRule.activityRule.scenario.close()
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForText("Overview")
            assertInitialEmptyOverview()
            ui.showText("EUR 0.00")
            ui.showText("No activity for this month and Account selection.")
            setOverviewAccountSelection("Checking", isSelected = true)
            composeRule.onNodeWithText("Set up your first account").assertDoesNotExist()
        }
    }

    private fun retainSelectedAndClearedOverviewSelectionAcrossRelaunches(): Unit {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForText("Overview")
            assertOverviewAccountSelection("Checking", isSelected = true)
            setOverviewAccountSelection("Checking", isSelected = false)
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForText("Overview")
            assertInitialEmptyOverview()
        }
    }

    @Test
    fun requireCurrencyAndAccountNameBeforeContinuing(): Unit {
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
    fun retainUnsubmittedFormAcrossActivityRecreation(): Unit {
        waitForText("Set up your first account")
        composeRule.onNodeWithContentDescription("Currency").performClick()
        composeRule.onNodeWithText("GBP").performClick()
        composeRule.onNodeWithText("Account name").performTextInput("Travel cash")
        dismissKeyboard()
        composeRule
            .onNodeWithContentDescription("Bank account type")
            .performScrollTo()
            .performClick()
            .assertIsSelected()
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithText("Travel cash").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Currency: GBP").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Bank account type").performScrollTo().assertIsSelected()
    }

    @Test
    fun captureFirstRunDesignExamples(): Unit {
        waitForText("Set up your first account")
        composeRule.waitUntil(WAIT_FOR_SETUP_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText("Loading your setup…").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithText("Currency").assertIsDisplayed()
        captureScreenshot("first-run-setup.png")
        composeRule.onNodeWithText("Create account").performScrollTo().performClick()
        ui.showText("Pick a supported currency before continuing.")
        captureScreenshot("first-run-validation.png")
        composeRule.onNodeWithContentDescription("Currency").performScrollTo().performClick()
        composeRule.onNodeWithText("EUR").performClick()
        composeRule.onNodeWithText("Account name").performTextInput("Everyday")
        dismissKeyboard()
        composeRule.onNodeWithText("Create account").performScrollTo().performClick()
        waitForText("Overview")
        assertInitialEmptyOverview("Everyday")
        captureScreenshot("first-run-empty-overview.png")
    }

    private fun waitForText(text: String): Unit {
        if (text == "Overview") {
            ui.showOverview()
        } else {
            ui.showText(text)
        }
    }

    private fun assertCurrentNavigationArea(): Unit {
        if (composeRule.onAllNodesWithTag("tab-Overview").fetchSemanticsNodes().isEmpty()) {
            composeRule.onNodeWithTag("current-area").assert(
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Current tab"),
            )
        } else {
            composeRule.onNodeWithTag("tab-Overview").assert(
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Current tab"),
            )
        }
    }

    private fun dismissKeyboard(): Unit {
        ui.dismissKeyboard()
    }

    private fun assertInitialEmptyOverview(accountName: String = "Checking"): Unit {
        val currentMonth: String =
            YearMonth.now().format(
                DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()),
            )
        ui.showText(currentMonth)
        openOverviewDrawer()
        composeRule.onNodeWithContentDescription("$accountName Account selection").assertIsOff()
        closeOverviewDrawer()
    }

    private fun assertOverviewAccountSelection(
        accountName: String,
        isSelected: Boolean,
    ): Unit {
        openOverviewDrawer()
        if (isSelected) {
            composeRule.onNodeWithContentDescription("$accountName Account selection").assertIsOn()
        } else {
            composeRule.onNodeWithContentDescription("$accountName Account selection").assertIsOff()
        }
        closeOverviewDrawer()
    }

    private fun setOverviewAccountSelection(
        accountName: String,
        isSelected: Boolean,
    ): Unit {
        openOverviewDrawer()
        val accountSelection: SemanticsNodeInteraction =
            composeRule.onNodeWithContentDescription("$accountName Account selection")
        if (isSelected) {
            accountSelection.assertIsOff().performClick().assertIsOn()
        } else {
            accountSelection.assertIsOn().performClick().assertIsOff()
        }
        closeOverviewDrawer()
    }

    private fun openOverviewDrawer(): Unit {
        composeRule.onNodeWithTag("overview-root").performTouchInput {
            swipe(
                start = center.copy(x = center.x * DRAWER_SWIPE_START_X_SCALE - DRAWER_SWIPE_EDGE_OFFSET),
                end = center.copy(x = center.x * DRAWER_SWIPE_END_X_SCALE),
                durationMillis = DRAWER_SWIPE_DURATION_MILLIS,
            )
        }
    }

    private fun closeOverviewDrawer(): Unit {
        composeRule.onNodeWithText("Close").performClick()
        composeRule.waitUntil(WAIT_FOR_DRAWER_TIMEOUT_MILLIS) { composeRule.onAllNodesWithText("Close").fetchSemanticsNodes().isEmpty() }
    }

    private fun captureScreenshot(filename: String): Unit {
        composeRule.waitForIdle()
        Thread.sleep(SCREENSHOT_SETTLE_DELAY_MILLIS)
        val outputDirectory: File =
            InstrumentationRegistry
                .getArguments()
                .getString("additionalTestOutputDir")
                ?.let { path: String -> File(path, "first-run") }
                ?: File(requireNotNull(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)), "first-run")
        check(outputDirectory.mkdirs() || outputDirectory.isDirectory)
        val outputFile: File = File(outputDirectory, filename)
        val screenshot: Bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        FileOutputStream(outputFile).use { output ->
            assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, SCREENSHOT_QUALITY, output))
        }
    }

    private companion object {
        const val WAIT_FOR_SETUP_TIMEOUT_MILLIS: Long = 5_000L
        const val WAIT_FOR_DRAWER_TIMEOUT_MILLIS: Long = 5_000L
        const val DRAWER_SWIPE_DURATION_MILLIS: Long = 450L
        const val DRAWER_SWIPE_START_X_SCALE: Float = 2f
        const val DRAWER_SWIPE_END_X_SCALE: Float = 0.8f
        const val DRAWER_SWIPE_EDGE_OFFSET: Float = 1f
        const val SCREENSHOT_SETTLE_DELAY_MILLIS: Long = 300L
        const val SCREENSHOT_QUALITY: Int = 100
    }
}
