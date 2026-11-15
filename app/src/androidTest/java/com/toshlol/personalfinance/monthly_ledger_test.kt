@file:Suppress("ktlint:standard:filename", "ktlint:standard:no-unit-return")

package com.toshlol.personalfinance

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.AppPreferences
import com.toshlol.personalfinance.data.CategoryDraft
import com.toshlol.personalfinance.data.FinanceCategory
import com.toshlol.personalfinance.data.RoomTransferRepository
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.data.TransactionDraft
import com.toshlol.personalfinance.data.TransferDraft
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File
import java.io.OutputStream
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MonthlyLedgerTest {
    private val composeRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> = createAndroidComposeRule()
    private val ui: FinanceUiDriver = FinanceUiDriver(composeRule)

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(ResetFinanceDataRule())
            .around(SeedMonthlyLedgerRule())
            .around(composeRule)
            .around(UiFailureArtifactsRule(composeRule))

    @Test
    fun showDirectionTotalsAndPairedTransferInDayGroupedLedgers(): Unit {
        ui.showOverview()
        saveEvidenceScreenshot("overview-bottom-actions")
        composeRule.onNodeWithTag("open-navigation-drawer").performClick()
        composeRule.waitUntil(15_000L) {
            composeRule.onAllNodesWithTag("drawer-Accounts").fetchSemanticsNodes().size == 1
        }
        saveEvidenceScreenshot("navigation-drawer")
        composeRule.onNodeWithTag("drawer-Overview").performClick()
        composeRule.onNodeWithTag("tab-Expenses").performClick()
        composeRule.waitUntil(15_000L) {
            composeRule.onAllNodesWithText("EUR 5.00").fetchSemanticsNodes().size == 1
        }
        saveEvidenceScreenshot("expenses")
        ui.showText("EUR 5.00")
        ui.showText("-EUR 3.00 · Transfer out")
        ui.showText("Cash · Food · Lunch")
        composeRule.onNodeWithText("Category: All Category").performClick()
        composeRule.onNodeWithText("Food").performClick()
        composeRule.onAllNodesWithText("-EUR 3.00 · Transfer out").assertCountEquals(0)
        composeRule.onNodeWithText("Category: Food").performClick()
        composeRule.onNodeWithText("All Category").performClick()
        composeRule.onNodeWithTag("tab-Income").performClick()
        composeRule.waitUntil(15_000L) {
            composeRule.onAllNodesWithText("EUR 7.00").fetchSemanticsNodes().size == 1
        }
        saveEvidenceScreenshot("income")
        ui.showText("EUR 7.00")
        ui.showText("+EUR 3.00 · Transfer in")
        ui.showText("Savings · Salary")
        ui.showText("Accounts (2 selected)")
        composeRule.onNodeWithText("Accounts (2 selected)").performClick()
        composeRule.waitUntil(15_000L) {
            composeRule.onAllNodesWithText("Close").fetchSemanticsNodes().size == 1
        }
        saveEvidenceScreenshot("account-selection")
        composeRule.onNodeWithContentDescription("Savings Account selection").performClick()
        composeRule.onNodeWithText("Close").performClick()
        ui.showText("No Income activity for this month and Account selection.")
        saveEvidenceScreenshot("empty-income")
        composeRule.onNodeWithTag("tab-Expenses").performClick()
        ui.showText("EUR 5.00")
        ui.showText("-EUR 3.00 · Transfer out")
        composeRule.onNodeWithTag("open-navigation-drawer").performClick()
        composeRule.onNodeWithTag("drawer-Budgets").assertIsDisplayed()
        composeRule.onNodeWithTag("drawer-Expenses").performClick()
        ui.showText("EUR 5.00")
        composeRule.onAllNodesWithText("-EUR 3.00 · Transfer out").assertCountEquals(1)
    }

    private fun saveEvidenceScreenshot(name: String): Unit {
        val context: Context = ApplicationProvider.getApplicationContext()
        val outputDirectory: File = File(context.getExternalFilesDir(null), "monthly-ledger-evidence")
        check(outputDirectory.mkdirs() || outputDirectory.isDirectory)
        val screenshot: Bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        try {
            File(outputDirectory, "$name.png").outputStream().use { output: OutputStream ->
                check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
        } finally {
            screenshot.recycle()
        }
    }

    private class SeedMonthlyLedgerRule : ExternalResource() {
        override fun before(): Unit {
            val context: Context = ApplicationProvider.getApplicationContext()
            runBlocking {
                val database: AppDatabase = AppDatabase.getInstance(context)
                val fixtures: FinanceTestFixtures = FinanceTestFixtures(database)
                val cash: Account = fixtures.createFirstAccount()
                val savings: Account = fixtures.createAdditionalAccount("Savings")
                val category: FinanceCategory =
                    fixtures.organization
                        .saveCategory(CategoryDraft(null, "Food", TransactionDirection.EXPENSE))
                        .categories
                        .single()
                fixtures.transactions.save(
                    TransactionDraft(
                        accountId = cash.id,
                        amountMinor = 500L,
                        direction = TransactionDirection.EXPENSE,
                        date = LocalDate.now().toEpochDay(),
                        note = "Lunch",
                        categoryId = category.id,
                    ),
                )
                fixtures.transactions.save(
                    TransactionDraft(
                        accountId = savings.id,
                        amountMinor = 700L,
                        direction = TransactionDirection.INCOME,
                        date = LocalDate.now().toEpochDay(),
                        note = "Salary",
                    ),
                )
                val transferRepository: RoomTransferRepository = RoomTransferRepository(database, { "transfer-ledger" }, { 1L })
                transferRepository.save(
                    TransferDraft(
                        sourceAccountId = cash.id,
                        destinationAccountId = savings.id,
                        amountMinor = 300L,
                        date = LocalDate.now().toEpochDay(),
                    ),
                )
                AppPreferences(context).saveOverviewAccountIds(setOf(cash.id, savings.id))
            }
        }
    }
}
