@file:Suppress("ktlint:standard:filename", "ktlint:standard:no-unit-return")

package com.toshlol.personalfinance

import android.app.Instrumentation
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.AppPreferences
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.data.TransactionDraft
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class OverviewAccountSelectionTest {
    private val composeRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> = createAndroidComposeRule()
    private val ui: FinanceUiDriver = FinanceUiDriver(composeRule)

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(ResetFinanceDataRule())
            .around(SeedOverviewDataRule())
            .around(composeRule)
            .around(UiFailureArtifactsRule(composeRule))

    @Test
    fun showAndPersistEmptyAccountSelectionAndSelectAccountFromRightDrawer(): Unit {
        ui.showText("EUR 0.00")
        ui.showText("No activity for this month and Account selection.")
        saveScreenshot("overview.png")
        val displayWidth: Float = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).displayWidth.toFloat()
        composeRule.onNodeWithTag("overview-root").performTouchInput {
            swipe(start = center.copy(x = displayWidth - 2f), end = center.copy(x = displayWidth * 0.45f), durationMillis = 450)
        }
        composeRule.onNodeWithContentDescription("Cash Account selection").assertIsOff().assertIsDisplayed()
        ui.showText("EUR 1002.00")
        saveScreenshot("overview-account-drawer.png")
        composeRule.onNodeWithContentDescription("Cash Account selection").performClick().assertIsOn()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.onAllNodesWithText("EUR 1002.00").assertCountEquals(2)
        ui.showText("Income · EUR 2.00")
    }

    @Test
    fun retainSelectedMonthAcrossOverviewAndBudgetNavigation(): Unit {
        composeRule.waitUntil(15_000L) {
            composeRule.onAllNodesWithTag("previous-month").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("previous-month").performClick()
        val selectedMonth: YearMonth = YearMonth.now().minusMonths(1)
        ui.showText(
            selectedMonth.format(
                java.time.format.DateTimeFormatter
                    .ofPattern("MMMM yyyy"),
            ),
        )
        composeRule.onNodeWithTag("navigation-Budgets").performClick()
        ui.showText("Monthly expense limits")
        composeRule.onNodeWithTag("navigation-Overview").performClick()
        ui.showText(
            selectedMonth.format(
                java.time.format.DateTimeFormatter
                    .ofPattern("MMMM yyyy"),
            ),
        )
    }

    @Test
    fun persistSelectedAndExplicitlyEmptyAccountIdsInDataStore(): Unit {
        val context: Context = ApplicationProvider.getApplicationContext()
        val initialPreferences: AppPreferences = AppPreferences(context)
        runBlocking {
            initialPreferences.saveOverviewAccountIds(setOf("cash-id", "savings-id"))
            assertEquals(setOf("cash-id", "savings-id"), AppPreferences(context).loadOverviewAccountIds())
            initialPreferences.saveOverviewAccountIds(emptySet())
            assertEquals(emptySet<String>(), AppPreferences(context).loadOverviewAccountIds())
        }
    }

    @Test
    fun selectArchivedAccountFromOverviewDrawer(): Unit {
        val context: Context = ApplicationProvider.getApplicationContext()
        runBlocking {
            val accountId: String =
                requireNotNull(
                    AppDatabase
                        .getInstance(context)
                        .accountDao()
                        .loadAccounts()
                        .firstOrNull(),
                ).id
            FinanceTestFixtures(AppDatabase.getInstance(context)).accounts.archiveAccount(accountId)
        }
        val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        composeRule.waitUntil(15_000L) {
            composeRule.onAllNodesWithTag("overview-root").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("overview-root").performTouchInput {
            swipe(
                start = center.copy(x = device.displayWidth.toFloat() - 2f),
                end = center.copy(x = device.displayWidth * 0.45f),
                durationMillis = 450,
            )
        }
        composeRule.onNodeWithText("Archived").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Cash Account selection")
            .assertIsOff()
            .performClick()
            .assertIsOn()
        ui.showText("Income · EUR 2.00")
    }

    private fun saveScreenshot(fileName: String): Unit {
        val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
        val testOutputDirectory: String? = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val context: Context = ApplicationProvider.getApplicationContext()
        val outputDirectory: File = File(testOutputDirectory ?: context.getExternalFilesDir(null).toString(), "overview-evidence")
        check(outputDirectory.mkdirs() || outputDirectory.isDirectory)
        val screenshot: Bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(outputDirectory, fileName).outputStream().use { output: java.io.OutputStream ->
                check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
        } finally {
            screenshot.recycle()
        }
    }

    private class SeedOverviewDataRule : ExternalResource() {
        override fun before(): Unit {
            val context: Context = ApplicationProvider.getApplicationContext()
            runBlocking {
                val fixtures: FinanceTestFixtures = FinanceTestFixtures(AppDatabase.getInstance(context))
                val account: Account = fixtures.createFirstAccount()
                val currentMonth: YearMonth = YearMonth.now()
                fixtures.accounts.saveAccount(account.id, account.name, 100_000L, currentMonth.atDay(1).toEpochDay())
                fixtures.transactions.save(
                    TransactionDraft(
                        accountId = account.id,
                        amountMinor = 200L,
                        direction = TransactionDirection.INCOME,
                        date = LocalDate.now().toEpochDay(),
                        note = null,
                    ),
                )
                AppPreferences(context).saveOverviewAccountIds(emptySet())
            }
        }
    }
}
