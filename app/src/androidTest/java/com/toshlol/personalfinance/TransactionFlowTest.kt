package com.toshlol.personalfinance

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.CategoryDraft
import com.toshlol.personalfinance.data.FinanceCategory
import com.toshlol.personalfinance.data.FinanceTag
import com.toshlol.personalfinance.data.RecurringBudgetDraft
import com.toshlol.personalfinance.data.RoomRecurringBudgetRepository
import com.toshlol.personalfinance.data.RoomTransferRepository
import com.toshlol.personalfinance.data.TagDraft
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.data.TransactionDraft
import com.toshlol.personalfinance.data.TransferDraft
import com.toshlol.personalfinance.data.projectMonthlyOverview
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import java.io.File
import java.io.OutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val ACCOUNT_SELECTION_SWIPE_EDGE_INSET_PX: Float = 1f
private const val ACCOUNT_SELECTION_SWIPE_END_RATIO: Float = 0.8f
private const val ACCOUNT_SELECTION_SWIPE_DURATION_MILLIS: Long = 450L

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

    @Before
    fun selectCashForOverview() {
        ui.selectOverviewAccount("Cash")
    }

    @Test
    @Suppress("ktlint:standard:no-unit-return")
    fun reviewEmptyAccountBeforeAndAfterArchiving(): Unit {
        navigateTo("Accounts")
        ui.showText("Add Account")
        ui.showText("Type · Cash")
        ui.showText("Balance · EUR 0.00")
        ui.clickText("View Cash history")
        ui.showText("Cash history")
        ui.showText("No account activity yet")
        ui.clickText("Back")
        ui.clickText("Archive Cash")
        ui.showText("Type · Cash")
        ui.showText("Archived")
        ui.clickText("View Cash history")
        ui.showText("Cash history")
        ui.showText("Balance  EUR 0.00")
        ui.showText("No account activity yet")
    }

    @Test
    @Suppress("ktlint:standard:no-unit-return")
    fun createEditAndReopenAccountKeepsOverviewSelectionAndSignedOpeningValues(): Unit {
        val openingDate: LocalDate = LocalDate.now().minusDays(1)
        val editedOpeningDate: LocalDate = openingDate.minusDays(1)
        createReserveAccount(openingDate)
        editReserveAccount(editedOpeningDate)
        reopenAndVerifyTravelAccount(editedOpeningDate)
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun createReserveAccount(openingDate: LocalDate): Unit {
        composeRule.onNodeWithTag("open-navigation-drawer").performClick()
        composeRule.onNodeWithTag("drawer-Accounts").performClick()
        ui.clickText("Add Account")
        ui.enterText("Account name", "Reserve")
        ui.enterText("Opening balance (EUR)", "-2.50")
        ui.enterText("Opening date (YYYY-MM-DD)", openingDate.toString())
        composeRule
            .onNodeWithContentDescription("Account type Credit card")
            .performScrollTo()
            .performClick()
            .assertIsSelected()
        ui.clickText("Create Account")
        ui.showText("Type · Credit card")
        ui.showText("Balance · EUR -2.50")
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun editReserveAccount(openingDate: LocalDate): Unit {
        ui.clickText("Edit Reserve")
        ui.enterText("Account name", "Travel")
        ui.enterText("Opening balance (EUR)", "-4.25")
        ui.enterText("Opening date (YYYY-MM-DD)", openingDate.toString())
        ui.clickText("Save changes")
        ui.showText("Type · Credit card")
        ui.showText("Balance · EUR -4.25")
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun reopenAndVerifyTravelAccount(openingDate: LocalDate): Unit {
        ui.clickText("Back to overview")
        assertOverviewAccountSelection("Travel", isSelected = false)
        composeRule.onNodeWithContentDescription("Cash Account selection").assertIsOn()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.activityRule.scenario.close()
        ActivityScenario.launch(MainActivity::class.java).use {
            ui.showOverview()
            composeRule.onNodeWithTag("open-navigation-drawer").performClick()
            composeRule.onNodeWithTag("drawer-Accounts").performClick()
            ui.showText("Travel")
            ui.showText("Type · Credit card")
            ui.showText("Balance · EUR -4.25")
            ui.clickText("Edit Travel")
            ui.showText(openingDate.toString())
            ui.clickText("Back to Accounts")
            ui.clickText("View Travel history")
            ui.showText("Balance  EUR -4.25")
        }
    }

    @Test
    @Suppress("ktlint:standard:no-unit-return")
    fun accountManagementWrapsArchiveActionAndKeepsArchivedHistoryReadOnly(): Unit {
        archiveReserveAndReviewHistory()
        verifyArchivedCounterpartTransferIsReadOnly()
        verifyArchivedHistoryMonthRetention()
        verifyActiveLedgerCannotEditArchivedCounterpartTransfer()
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun archiveReserveAndReviewHistory(): Unit {
        composeRule.onNodeWithTag("open-navigation-drawer").performClick()
        composeRule.onNodeWithTag("drawer-Accounts").performClick()
        ui.showText("Archive Reserve")
        ui.clickText("Archive Reserve")
        ui.showText("Archived")
        composeRule.onAllNodesWithText("Edit Reserve").assertCountEquals(0)
        ui.clickText("View Reserve history")
        ui.showText("Reserve history")
        ui.showText("Balance  EUR 3.50")
        ui.showText("Archived")
        ui.showText("Read-only history")
        ui.showText("Income · +EUR 2.50")
        ui.showText("+EUR 1.00 · Transfer in")
        ui.showText("From Cash", substring = true)
        ui.clickText("Back")
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun verifyArchivedCounterpartTransferIsReadOnly(): Unit {
        ui.clickText("View Cash history")
        val outgoingTransfer: String = "-EUR 1.00 · Transfer out"
        composeRule.onNodeWithText(outgoingTransfer).assertHasNoClickAction()
        ui.clickText("Back")
    }

    private fun verifyActiveLedgerCannotEditArchivedCounterpartTransfer() {
        val outgoingTransfer: String = "-EUR 1.00 · Transfer out"
        navigateTo("Expenses")
        composeRule.waitUntil(15_000) {
            runCatching { !composeRule.onNodeWithText("Navigate").isDisplayed() }.getOrDefault(false)
        }
        ui.clickText(">")
        ui.showText(outgoingTransfer).assertIsNotEnabled()
        composeRule.onNodeWithTag("transfer-form").assertDoesNotExist()
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun verifyArchivedHistoryMonthRetention(): Unit {
        ui.clickText("View Reserve history")
        val previousMonthLabel: String =
            YearMonth
                .now()
                .minusMonths(1)
                .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
        composeRule.onNodeWithContentDescription("Previous month").performClick()
        ui.showText(previousMonthLabel)
        ui.clickText("Back")
        ui.showText("Closing balances for $previousMonthLabel")
        ui.clickText("Back to overview")
        ui.showText(previousMonthLabel)
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
    fun editTransactionKeepsIdentityAndRecalculatesBothAccountBalances() {
        ui.clickText("Add expense")
        ui.enterText("Amount", "8.25", "transaction-form")
        ui.enterText("Note (optional)", "original note", "transaction-form")
        ui.clickText("Food", "transaction-form")
        composeRule
            .onNodeWithTag("transaction-tag-${tagId("Lunch")}")
            .performScrollTo()
            .performClick()
            .assertIsOn()
        ui.clickText("Save")
        ui.clickText("Expense \u00b7 EUR 8.25")
        composeRule.onNodeWithTag("transaction-amount").assert(hasText("EUR 8.25"))
        composeRule.onNodeWithTag("transaction-direction-EXPENSE").assertIsSelected()
        ui.showText(LocalDate.now().toString(), "transaction-form")
        ui.showText("original note", "transaction-form")
        ui.showText("Food", "transaction-form").assertIsSelected()
        composeRule.onNodeWithTag("transaction-tag-${tagId("Lunch")}").assertIsOn()
        val context: Context = ApplicationProvider.getApplicationContext()
        val repository: com.toshlol.personalfinance.data.RoomTransactionRepository =
            com.toshlol.personalfinance.data
                .RoomTransactionRepository(AppDatabase.getInstance(context))
        val original: com.toshlol.personalfinance.data.FinanceTransaction = runBlocking { repository.loadTransactions().single() }
        ui.enterText("Note (optional)", "edited after save", "transaction-form")
        ui.enterText("Amount", "9.00", "transaction-form")
        ui.clickText("Save")
        composeRule.activityRule.scenario.recreate()
        ui.showText("Expense \u00b7 EUR 9.00")
        val edited: com.toshlol.personalfinance.data.FinanceTransaction = runBlocking { repository.loadTransactions().single() }
        assertEquals(original.id, edited.id)
        assertEquals(original.createdAt, edited.createdAt)
        assertEquals(original.accountId, edited.accountId)
        assertEquals("edited after save", edited.note)
        assertNotNull(edited.categoryId)
        assertEquals(listOf("Lunch"), edited.tagNames)
        val accountsAfterAmountEdit: List<Account> =
            runBlocking { FinanceTestFixtures(AppDatabase.getInstance(context)).accounts.loadSetup().accounts }
        assertEquals(-900L, accountsAfterAmountEdit.single { it.name == "Cash" }.balanceMinor)
        val monthlyAfterAmountEdit: com.toshlol.personalfinance.data.MonthlyOverview =
            projectMonthlyOverview(
                YearMonth.now(),
                accountsAfterAmountEdit,
                runBlocking {
                    repository.loadTransactions()
                },
                emptyList(),
                accountsAfterAmountEdit.map { it.id }.toSet(),
            )
        assertEquals(900L, monthlyAfterAmountEdit.expenseMinor)
        ui.clickText("Expense \u00b7 EUR 9.00")
        composeRule.onNodeWithTag("transaction-account-${accountId("Savings")}").performScrollTo().performClick()
        ui.clickText("Save")
        val moved: com.toshlol.personalfinance.data.FinanceTransaction = runBlocking { repository.loadTransactions().single() }
        val accounts: List<Account> = runBlocking { FinanceTestFixtures(AppDatabase.getInstance(context)).accounts.loadSetup().accounts }
        assertEquals(original.id, moved.id)
        assertEquals(original.createdAt, moved.createdAt)
        assertEquals(original.categoryId, moved.categoryId)
        assertEquals(original.tagIds, moved.tagIds)
        assertEquals(0L, accounts.single { it.name == "Cash" }.balanceMinor)
        assertEquals(-900L, accounts.single { it.name == "Savings" }.balanceMinor)
        val monthlyAfterAccountChange: com.toshlol.personalfinance.data.MonthlyOverview =
            projectMonthlyOverview(
                YearMonth.now(),
                accounts,
                runBlocking {
                    repository.loadTransactions()
                },
                emptyList(),
                accounts.map { it.id }.toSet(),
            )
        assertEquals(900L, monthlyAfterAccountChange.expenseMinor)
        ui.showOverview()
    }

    @Test
    fun validatesAmountAndDateWithoutMutationAndSavesWithoutOptionalFields() {
        ui.clickText("Add expense")
        ui.showText(LocalDate.now().toString(), "transaction-form")
        ui.clickText("Save")
        ui.showText("Enter a positive amount and a valid date.", "transaction-form")
        ui.enterText("Amount", "0", "transaction-form")
        ui.clickText("Save")
        ui.showText("Enter a positive amount and a valid date.", "transaction-form")
        assertEquals(
            0,
            runBlocking {
                FinanceTestFixtures(
                    AppDatabase.getInstance(ApplicationProvider.getApplicationContext()),
                ).transactions.loadTransactions().size
            },
        )
        composeRule.onNodeWithTag("amount-key-C").performClick()
        composeRule.onNodeWithTag("amount-key-9").performClick()
        composeRule.onNodeWithTag("amount-key-\u232b").performClick()
        listOf("1", "2", "3", "4", "5").forEach { digit -> composeRule.onNodeWithTag("amount-key-$digit").performClick() }
        composeRule.onNodeWithTag("amount-key-\u232b").performClick()
        ui.enterText("Date (YYYY-MM-DD)", "2026-02-30", "transaction-form")
        ui.clickText("Save")
        ui.showText("Enter a positive amount and a valid date.", "transaction-form")
        assertEquals(
            0,
            runBlocking {
                FinanceTestFixtures(
                    AppDatabase.getInstance(ApplicationProvider.getApplicationContext()),
                ).transactions.loadTransactions().size
            },
        )
        ui.enterText("Date (YYYY-MM-DD)", "2020-01-02", "transaction-form")
        ui.showText("Uncategorized", "transaction-form").assertIsSelected()
        composeRule.onNodeWithTag("transaction-save").performClick()
        val saved: com.toshlol.personalfinance.data.FinanceTransaction =
            runBlocking {
                FinanceTestFixtures(
                    AppDatabase.getInstance(ApplicationProvider.getApplicationContext()),
                ).transactions.loadTransactions().single()
            }
        assertEquals(1234L, saved.amountMinor)
        assertEquals(LocalDate.parse("2020-01-02").toEpochDay(), saved.date)
        assertNull(saved.categoryId)
        assertTrue(saved.tagIds.isEmpty())
        assertNull(saved.note)
        ui.showOverview()
        ui.showText("No activity for this month and Account selection.")
        ui.clickText("Add income")
        ui.enterText("Amount", "7.50", "transaction-form")
        ui.clickText("Cancel")
        assertEquals(
            1,
            runBlocking {
                FinanceTestFixtures(
                    AppDatabase.getInstance(ApplicationProvider.getApplicationContext()),
                ).transactions.loadTransactions().size
            },
        )
    }

    @Test
    fun cancelAndConfirmTransactionDeletionUpdateLedger() {
        ui.clickText("Add expense")
        ui.enterText("Amount", "4.00", "transaction-form")
        ui.clickText("Save")
        ui.clickText("Expense \u00b7 EUR 4.00")
        ui.clickText("Delete")
        ui.showText("Delete transaction?")
        composeRule.onAllNodesWithText("Cancel").get(1).performClick()
        assertEquals(2, runBlocking { transactionRepository().loadTransactions().size })
        ui.clickText("Delete")
        ui.click(hasText("Delete") and hasAnyAncestor(isDialog()))
        ui.showText("No activity for this month and Account selection.")
        val remainingTransactions: List<com.toshlol.personalfinance.data.FinanceTransaction> =
            runBlocking { transactionRepository().loadTransactions() }
        assertEquals(1, remainingTransactions.size)
        assertEquals("unrelated record", remainingTransactions.single().note)
        assertEquals(
            0L,
            runBlocking {
                FinanceTestFixtures(
                    AppDatabase.getInstance(ApplicationProvider.getApplicationContext()),
                ).accounts.loadSetup().accounts.single { it.name == "Cash" }.balanceMinor
            },
        )
        assertEquals(
            100L,
            runBlocking {
                FinanceTestFixtures(
                    AppDatabase.getInstance(ApplicationProvider.getApplicationContext()),
                ).accounts.loadSetup().accounts.single { it.name == "Savings" }.balanceMinor
            },
        )
    }

    @Test
    fun requiresAnActiveAccountBeforeSavingTransaction() {
        ui.clickText("Add expense")
        ui.enterText("Amount", "2.00", "transaction-form")
        ui.clickText("Save")
        ui.showText("Choose an active Account.", "transaction-form")
        assertTrue(
            runBlocking {
                FinanceTestFixtures(
                    AppDatabase.getInstance(ApplicationProvider.getApplicationContext()),
                ).transactions.loadTransactions().isEmpty()
            },
        )
    }

    @Test
    fun transactionEntryRemainsUsableAtLargeFontScale() {
        ui.clickText("Add income")
        composeRule.onNodeWithTag("transaction-direction-INCOME").assertIsSelected()
        composeRule.onNodeWithTag("transaction-account-${accountId("Cash")}").assertIsDisplayed()
        ui.showText("Amount", "transaction-form")
        ui.showText("Date (YYYY-MM-DD)", "transaction-form")
        ui.showText("Note (optional)", "transaction-form")
        ui.showText("Large text tag", "transaction-form")
        ui.showText("Save").assertHasClickAction()
        ui.showText("Cancel").assertHasClickAction()
        ui.clickText("Cancel")
        ui.showOverview()
    }

    @Test
    @Suppress("ktlint:standard:no-unit-return")
    fun recordAndEditTransferThroughFullScreenEntry(): Unit {
        val previousMonth: YearMonth = YearMonth.now().minusMonths(1)
        openOrganizer()
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
        ui.enterText("Account opening date (YYYY-MM-DD)", LocalDate.now().toString(), "category-tag-list")
        ui.clickText("Save account details", "category-tag-list")
        ui.click(hasText("Edit Savings", substring = true), "category-tag-list")
        ui.showText("2026-01-01", "category-tag-list")
        ui.clickText("Back to overview")
        ui.clickText("Add expense")
        ui.enterText("Amount", "1.00", "transaction-form")
        ui.enterText("Date (YYYY-MM-DD)", previousMonth.atEndOfMonth().toString(), "transaction-form")
        ui.clickText("Save")
        navigateTo("Expenses")
        ui.clickText("<")
        ui.showText("-EUR 1.00")
        ui.clickText(">")
        ui.showText("No Expenses activity for this month and Account selection.")
        navigateTo("Overview")
        ui.selectOverviewAccount("Savings")
        composeRule.onNodeWithText("Expense \u00b7 EUR 1.00").assertDoesNotExist()
        ui.clickText("Transfer money")
        ui.showText("New Transfer")
        composeRule.onNodeWithTag("transfer-source-${accountId("Cash")}").assertIsNotSelected()
        composeRule.onNodeWithTag("transfer-source-${accountId("Savings")}").assertIsNotSelected()
        composeRule.onNodeWithTag("transfer-destination-${accountId("Cash")}").assertIsNotSelected()
        composeRule.onNodeWithTag("transfer-destination-${accountId("Savings")}").assertIsNotSelected()
        ui.showText(LocalDate.now().toString(), "transfer-form")
        composeRule.onNodeWithTag("transfer-amount").assertIsDisplayed()
        composeRule.onNodeWithTag("amount-key-1").assertIsDisplayed()
        composeRule.onNodeWithTag("transfer-save").assertHasClickAction()
        composeRule.onNodeWithTag("transfer-cancel").assertHasClickAction()
        composeRule.onNodeWithText("Note (optional)").assertDoesNotExist()
        composeRule.onNodeWithText("Category (optional)").assertDoesNotExist()
        composeRule.onNodeWithText("Tags (optional)").assertDoesNotExist()
        saveTransferEvidenceScreenshot("transfer-normal")
        ui.enterText("Amount", "0", "transfer-form")
        ui.clickText("Save")
        ui.showText("Enter a positive amount", "transfer-form", substring = true)
        saveTransferEvidenceScreenshot("transfer-validation")
        assertTrue(isTransferLedgerEmpty())
        ui.enterText("Amount", "10.00", "transfer-form")
        ui.enterText("Date (YYYY-MM-DD)", "2026-99-99", "transfer-form")
        ui.clickText("Save")
        ui.showText("Enter a positive amount and a valid date.", "transfer-form")
        assertTrue(isTransferLedgerEmpty())
        ui.enterText("Date (YYYY-MM-DD)", LocalDate.now().toString(), "transfer-form")
        ui.clickText("Save")
        ui.showText("Choose two different Accounts.", "transfer-form")
        assertTrue(isTransferLedgerEmpty())
        composeRule.onNodeWithTag("transfer-source-${accountId("Cash")}").performScrollTo().performClick()
        composeRule.onNodeWithTag("transfer-destination-${accountId("Cash")}").performScrollTo().performClick()
        ui.clickText("Save")
        ui.showText("Choose two different Accounts.", "transfer-form")
        composeRule.onNodeWithTag("transfer-destination-${accountId("Savings")}").performScrollTo().performClick()
        ui.clickText("Save")
        ui.showText("Transfer · EUR 10.00")
        showOverviewAccount("Cash", "-10.00")
        showOverviewAccount("Savings", "15.00")
        ui.showOverview()
        composeRule.onAllNodesWithText("0.00").assertCountEquals(3)
        composeRule.onNodeWithText("Expense \u00b7 EUR 1.00").assertDoesNotExist()
        openOrganizer()
        ui.clickText("Manage accounts", "category-tag-list")
        ui.click(hasText("Editing Savings", substring = true), "category-tag-list")
        ui.enterText("Account name", "Reserve", "category-tag-list")
        ui.clickText("Save account details", "category-tag-list")
        ui.showText("Reserve \u00b7 Bank \u00b7 EUR 15.00", "category-tag-list")
        ui.clickText("Back to overview")
        showOverviewAccount("Cash", "-10.00").performClick()
        ui.showText("Cash history")
        ui.showText("-EUR 10.00 · Transfer out")
        ui.showText("To Reserve", substring = true)
        ui.clickText("Back")
        showOverviewAccount("Reserve", "15.00").performClick()
        ui.showText("Reserve history")
        ui.showText("+EUR 10.00 · Transfer in")
        ui.showText("From Cash", substring = true)
        ui.clickText("Back")
        navigateTo("Income")
        ui.showText("+EUR 10.00 \u00b7 Transfer in")
        composeRule.onAllNodesWithText("+EUR 10.00 \u00b7 Transfer in").assertCountEquals(1)
        ui.showText("EUR 0.00")
        toggleLedgerAccount("Reserve", 2)
        ui.showText("No Income activity for this month and Account selection.")
        toggleLedgerAccount("Reserve", 1)
        ui.clickText("+EUR 10.00 \u00b7 Transfer in")
        composeRule.onNodeWithTag("transfer-amount").assert(hasText("EUR 10.00"))
        composeRule.onNodeWithTag("transfer-source-${accountId("Cash")}").assertIsSelected()
        composeRule.onNodeWithTag("transfer-destination-${accountId("Reserve")}").assertIsSelected()
        ui.clickText("Cancel")
        navigateTo("Expenses")
        ui.showText("-EUR 10.00 \u00b7 Transfer out")
        composeRule.onAllNodesWithText("-EUR 10.00 \u00b7 Transfer out").assertCountEquals(1)
        ui.showText("EUR 0.00")
        toggleLedgerAccount("Cash", 2)
        ui.showText("No Expenses activity for this month and Account selection.")
        toggleLedgerAccount("Cash", 1)
        ui.clickText("Category: All Category")
        ui.clickText("Food")
        ui.showText("No Expenses activity for this month and Account selection.")
        ui.clickText("Category: Food")
        ui.clickText("All Category")
        ui.clickText("Tag: All Tag")
        ui.clickText("Lunch")
        ui.showText("No Expenses activity for this month and Account selection.")
        ui.clickText("Tag: Lunch")
        ui.clickText("All Tag")
        ui.clickText("-EUR 10.00 \u00b7 Transfer out")
        ui.enterText("Amount", "4.00", "transfer-form")
        val previousMonthTransferDate: LocalDate = previousMonth.atDay(15)
        ui.enterText("Date (YYYY-MM-DD)", previousMonthTransferDate.toString(), "transfer-form")
        composeRule.onNodeWithTag("transfer-source-${accountId("Reserve")}").performScrollTo().performClick()
        composeRule.onNodeWithTag("transfer-destination-${accountId("Cash")}").performScrollTo().performClick()
        ui.clickText("Save")
        ui.showText("No Expenses activity for this month and Account selection.")
        ui.showText("EUR 0.00")
        navigateTo("Income")
        ui.showText("No Income activity for this month and Account selection.")
        ui.clickText("<")
        ui.showText("+EUR 4.00 \u00b7 Transfer in")
        navigateTo("Expenses")
        ui.showText("-EUR 4.00 \u00b7 Transfer out")
        ui.clickText("-EUR 4.00 \u00b7 Transfer out")
        composeRule.onNodeWithTag("transfer-source-${accountId("Reserve")}").assertIsSelected()
        composeRule.onNodeWithTag("transfer-destination-${accountId("Cash")}").assertIsSelected()
        ui.showText(previousMonthTransferDate.toString(), "transfer-form")
        ui.clickText("Cancel")
        navigateTo("Income")
        ui.clickText("+EUR 4.00 \u00b7 Transfer in")
        ui.enterText("Amount", "3.00", "transfer-form")
        ui.enterText("Date (YYYY-MM-DD)", LocalDate.now().toString(), "transfer-form")
        ui.clickText("Save")
        ui.showText("No Income activity for this month and Account selection.")
        ui.clickText(">")
        ui.showText("+EUR 3.00 \u00b7 Transfer in")
        navigateTo("Expenses")
        ui.showText("-EUR 3.00 \u00b7 Transfer out")
        navigateTo("Overview")
        showOverviewAccount("Cash", "3.00")
        showOverviewAccount("Reserve", "2.00")
        navigateTo("Income")
        ui.clickText("+EUR 3.00 \u00b7 Transfer in")
        composeRule.onNodeWithTag("transfer-delete").performClick()
        ui.showText("Delete Transfer?")
        composeRule.onNodeWithTag("cancel-transfer-delete").performClick()
        ui.clickText("Cancel")
        ui.showText("+EUR 3.00 \u00b7 Transfer in")
        navigateTo("Expenses")
        ui.showText("-EUR 3.00 \u00b7 Transfer out")
        navigateTo("Income")
        ui.clickText("+EUR 3.00 \u00b7 Transfer in")
        composeRule.onNodeWithTag("transfer-delete").performClick()
        ui.showText("Delete Transfer?")
        composeRule.onNodeWithTag("confirm-transfer-delete").performClick()
        ui.showText("No Income activity for this month and Account selection.")
        navigateTo("Expenses")
        composeRule.onNodeWithText("-EUR 3.00 \u00b7 Transfer out").assertDoesNotExist()
        navigateTo("Overview")
        showOverviewAccount("Cash", "0.00")
        showOverviewAccount("Reserve", "5.00")
    }

    @Test
    fun transferEntryRemainsUsableAtLargeFontScale() {
        ui.clickText("Transfer money")
        ui.showText("New Transfer")
        composeRule.onNodeWithTag("transfer-source-${accountId("Cash")}").assertIsNotSelected()
        composeRule.onNodeWithTag("transfer-source-${accountId("Savings")}").assertIsNotSelected()
        composeRule.onNodeWithTag("transfer-destination-${accountId("Cash")}").assertIsNotSelected()
        composeRule.onNodeWithTag("transfer-destination-${accountId("Savings")}").assertIsNotSelected()
        ui.showText("Date (YYYY-MM-DD)", "transfer-form")
        composeRule.onNodeWithTag("amount-key-1").assertIsDisplayed()
        composeRule.onNodeWithTag("transfer-cancel").assertHasClickAction()
        composeRule.onNodeWithTag("transfer-save").assertHasClickAction()
        saveTransferEvidenceScreenshot("transfer-large-font")
        ui.enterText("Amount", "0", "transfer-form")
        ui.clickText("Save")
        ui.showText("Enter a positive amount", "transfer-form", substring = true)
        ui.clickText("Cancel")
        assertTrue(isTransferLedgerEmpty())
    }

    @Test
    fun recordExpenseAndReviewItOnOverview() {
        ui.clickText("Add expense")
        ui.enterText("Amount", "12.50", "transaction-form")
        ui.clickText("Save")
        ui.showText("Expense \u00b7 EUR 12.50")
        ui.show(hasText("Expenses") and hasAnySibling(hasText("12.50")))
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
        openOrganizer()
        ui.show(hasText("Lunch") and !hasSetTextAction(), "category-tag-list")
        ui.clickText("Back to overview")
        ui.clickText("Add expense")
        ui.enterText("Amount", "8.25", "transaction-form")
        ui.clickText("Food", "transaction-form")
        ui.showText("Food", "transaction-form").assertIsSelected()
        composeRule
            .onNodeWithTag("transaction-tag-${tagId("Lunch")}")
            .performScrollTo()
            .performClick()
            .assertIsOn()
        ui.clickText("Save")
        ui.showText("Expense \u00b7 EUR 8.25")
        ui.showText("Food", substring = true)
        ui.showText("Lunch", substring = true)
        openOrganizer()
        ui.clickText("Manage accounts", "category-tag-list")
        val accountNameField: SemanticsNodeInteraction = ui.showText("Account name", "category-tag-list")
        accountNameField.performTextReplacement("Wallet")
        accountNameField.assert(hasText("Wallet"))
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
        ui.enterText("Tag name", "Lunch", "category-tag-list")
        composeRule.onNode(hasText("Lunch") and hasSetTextAction()).performImeAction()
        ui.clickText("Add tag", "category-tag-list")
        ui.clickText("Back to overview")
        recordFoodExpense()
        navigateTo("Overview")
        composeRule.onNodeWithTag("open-budgets").performClick()
        ui.enterText("Budget name", "Household")
        ui.enterText("Monthly limit (EUR)", "20.00")
        composeRule.onNodeWithTag("budget-account-${accountId("Cash")}").performScrollTo().performClick()
        composeRule.onNodeWithTag("budget-account-${accountId("Cash")}").assertIsSelected()
        composeRule
            .onNodeWithTag("budget-category-${categoryId("Food")}")
            .performScrollTo()
            .performClick()
            .assertIsSelected()
        composeRule
            .onNodeWithTag("budget-tag-${tagId("Lunch")}")
            .performScrollTo()
            .performClick()
            .assertIsSelected()
        ui.clickText("Save Budget")
        ui.showText("Household")
        ui.showText("Accounts: Cash")
        ui.showText("Categories: Food")
        ui.showText("Tags: Lunch")
        saveBudgetEvidenceScreenshot("budget-progress.png")
        ui.clickText("Edit Budget")
        composeRule.onNodeWithTag("budget-name").assert(hasText("Household"))
        composeRule.onNodeWithTag("budget-limit").assert(hasText("20.00"))
        composeRule.onNodeWithTag("budget-category-${categoryId("Food")}").performScrollTo().assertIsSelected()
        composeRule
            .onNodeWithTag("budget-tag-${tagId("Lunch")}")
            .performScrollTo()
            .performClick()
            .assertIsNotSelected()
        composeRule.onNodeWithTag("budget-limit").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("budgets-screen").performTouchInput {
            swipe(start = center.copy(y = center.y * 0.8f), end = center.copy(y = center.y * 0.2f), durationMillis = 450)
        }
        composeRule.onNodeWithTag("budget-name").assertIsDisplayed()
        composeRule.onNodeWithTag("budget-limit").assertIsDisplayed()
        saveBudgetEvidenceScreenshot("budget-editor.png")
        composeRule.onNodeWithText("Save changes").performScrollTo().performClick()
        ui.showText("Categories: Food")
        ui.showText("Tags: Any Tag")
        verifyRemainingBudgetOnBothScreens()
        composeRule.onNodeWithTag("overview-root").performTouchInput {
            swipe(start = center.copy(x = center.x * 2f - 1f), end = center.copy(x = center.x * 0.8f), durationMillis = 450)
        }
        composeRule.onNodeWithContentDescription("Cash Account selection").performClick().assertIsOff()
        composeRule.onNodeWithTag("account-drawer-close").performClick()
        ui.showText("Household")
        ui.showText("Accounts: Cash")
        ui.showText("Categories: Food")
        ui.showText("Tags: Any Tag")
        ui.showText("Spent  EUR 12.50")
        ui.showText("Remaining  EUR 7.50")
        saveBudgetEvidenceScreenshot("overview-budget-progress.png")
        ui.selectOverviewAccount("Cash")
        ui.clickText("Expense \u00b7 EUR 12.50")
        ui.enterText("Amount", "25.00", "transaction-form")
        ui.clickText("Save")
        ui.showText("Remaining  EUR -5.00")
        ui.showText("Over budget")
        navigateTo("Budgets")
        verifyOverBudgetOnBothScreens()
    }

    @Test
    fun transferDoesNotContributeToBudgetActualSpending() {
        navigateTo("Budgets")
        ui.showText("Spent  EUR 0.00")
        ui.showText("Limit  EUR 10.00")
        ui.showText("Remaining  EUR 10.00")
        ui.clickText("Back to overview")
        ui.showText("Transfer \u00b7 EUR 5.00")
        ui.showText("Household")
        ui.showText("Spent  EUR 0.00")
        ui.showText("Remaining  EUR 10.00")
    }

    @Test
    fun recurringBudgetUsesSharedMonthAndResetsWithoutCarryover() {
        navigateTo("Budgets")
        ui.showText(YearMonth.now().format(DateTimeFormatter.ofPattern("MMMM yyyy")))
        ui.showText("Limit  EUR 10.00")
        ui.showText("Spent  EUR 0.00")
        composeRule.onNodeWithTag("budget-previous-month").performClick()
        ui.showText("Limit  EUR 10.00")
        ui.showText("Remaining  EUR 10.00")
        composeRule.onNodeWithTag("budget-next-month").performClick()
        ui.showText("Limit  EUR 10.00")
        ui.showText("Remaining  EUR 10.00")
    }

    @Test
    fun navigateFilterTransactionsAndQuickAddFromEveryMainArea() {
        navigateTo("Expenses")
        verifyLedgerMonthsAndOrdering()
        verifyLedgerAccountFiltering()
        verifyLedgerCategoryFiltering()
        verifyLedgerTagFiltering()
        navigateTo("Income")
        ui.showText("No Income activity for this month and Account selection.")
        ui.showText("EUR 0.00")
        listOf("Overview", "Expenses", "Income").forEach { area -> verifyMainLedgerActions(area) }
        verifySecondaryAreaEntry()
    }

    private fun verifyLedgerMonthsAndOrdering() {
        ui.showText("EUR 16.25")
        showTransactionRow("-EUR 8.25")
        showTransactionRow("-EUR 3.00")
        val newerPosition: Float =
            composeRule
                .onNodeWithText("-EUR 8.25")
                .fetchSemanticsNode()
                .boundsInRoot.top
        val olderPosition: Float =
            composeRule
                .onNodeWithText("-EUR 3.00")
                .fetchSemanticsNode()
                .boundsInRoot.top
        assertTrue(newerPosition < olderPosition)
        composeRule.onNodeWithText("-EUR 4.00").assertDoesNotExist()
        composeRule.onNodeWithText("-EUR 6.00").assertDoesNotExist()
        ui.clickText("<")
        ui.showText("EUR 4.00")
        showTransactionRow("-EUR 4.00")
        composeRule.onNodeWithText("-EUR 8.25").assertDoesNotExist()
        ui.clickText("<")
        ui.showText("No Expenses activity for this month and Account selection.")
        ui.clickText(">")
        ui.clickText(">")
        ui.showText("EUR 16.25")
    }

    private fun verifyLedgerAccountFiltering() {
        toggleLedgerAccount("Savings", 1)
        ui.showText("EUR 22.25")
        showTransactionRow("-EUR 6.00")
        toggleLedgerAccount("Savings", 2)
        ui.showText("EUR 16.25")
        composeRule.onNodeWithText("-EUR 6.00").assertDoesNotExist()
    }

    private fun verifyLedgerCategoryFiltering() {
        ui.clickText("Category: All Category")
        ui.clickText("Food")
        ui.showText("EUR 11.25")
        composeRule.onNodeWithText("-EUR 5.00").assertDoesNotExist()
        ui.clickText("Category: Food")
        ui.clickText("All Category")
        ui.showText("EUR 16.25")
        showTransactionRow("-EUR 5.00")
    }

    private fun verifyLedgerTagFiltering() {
        ui.clickText("Tag: All Tag")
        ui.clickText("Lunch")
        ui.showText("EUR 8.25")
        showTransactionRow("-EUR 8.25")
        composeRule.onNodeWithText("-EUR 3.00").assertDoesNotExist()
        ui.clickText("Tag: Lunch")
        ui.clickText("All Tag")
        ui.showText("EUR 16.25")
        showTransactionRow("-EUR 3.00")
    }

    private fun verifySecondaryAreaEntry() {
        listOf("Budgets", "Settings").forEach { area ->
            navigateTo(area)
            ui.clickText("Add transaction")
            composeRule.onNodeWithTag("transaction-direction-EXPENSE").assertIsSelected()
            ui.clickText("Cancel")
        }
        ui.clickText("Manage categories and tags")
        ui.showText("Category name", "category-tag-list")
        ui.clickText("Back to overview")
        navigateTo("Overview")
    }

    private fun verifyMainLedgerActions(area: String) {
        navigateTo(area)
        listOf("Add expense", "Add income").forEach { action ->
            ui.clickText(action)
            val direction: String = if (action == "Add expense") "EXPENSE" else "INCOME"
            composeRule.onNodeWithTag("transaction-direction-$direction").assertIsSelected()
            ui.clickText("Cancel")
        }
        ui.clickText("Transfer money")
        ui.showText("New Transfer")
        ui.clickText("Cancel")
    }

    private fun toggleLedgerAccount(
        name: String,
        selectedCount: Int,
    ) {
        ui.clickText("Accounts ($selectedCount selected)")
        composeRule.onNodeWithContentDescription("$name Account selection").performClick()
        ui.clickText("Close")
    }

    private fun navigateTo(area: String) {
        val navigationTag: String = "drawer-$area"
        composeRule.onNodeWithTag("open-navigation-drawer").performClick()
        composeRule.onNodeWithTag(navigationTag).performClick()
        composeRule.waitUntil(15_000) {
            runCatching {
                composeRule
                    .onNodeWithTag(navigationTag)
                    .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Current section"))
            }.isSuccess
        }
        if (area == "Overview") {
            ui.showOverview()
            return
        }
        val screenText: String =
            when (area) {
                "Expenses" -> "Add expense"
                "Income" -> "Add income"
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

    private fun isTransferLedgerEmpty(): Boolean {
        val context: Context = ApplicationProvider.getApplicationContext()
        return runBlocking { RoomTransferRepository(AppDatabase.getInstance(context)).loadTransfers().isEmpty() }
    }

    private fun saveTransferEvidenceScreenshot(name: String) {
        val context: Context = ApplicationProvider.getApplicationContext()
        val outputDirectory: File = File(context.getExternalFilesDir(null), "transfer-entry-evidence")
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

    private fun saveBudgetEvidenceScreenshot(name: String) {
        val context: Context = ApplicationProvider.getApplicationContext()
        val outputRoot: File =
            InstrumentationRegistry
                .getArguments()
                .getString("additionalTestOutputDir")
                ?.takeIf(String::isNotBlank)
                ?.let(::File)
                ?: context.cacheDir
        val outputDirectory: File = File(outputRoot, "budget-evidence")
        check(outputDirectory.mkdirs() || outputDirectory.isDirectory)
        val screenshot: Bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        try {
            File(outputDirectory, name).outputStream().use { output: OutputStream ->
                check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
        } finally {
            screenshot.recycle()
        }
    }

    private fun accountId(name: String): String {
        val context: Context = ApplicationProvider.getApplicationContext()
        return runBlocking {
            FinanceTestFixtures(
                AppDatabase.getInstance(context),
            ).accounts.loadSetup().accounts.single { it.name == name }.id
        }
    }

    private fun transactionRepository(): com.toshlol.personalfinance.data.RoomTransactionRepository {
        val context: Context = ApplicationProvider.getApplicationContext()
        return FinanceTestFixtures(AppDatabase.getInstance(context)).transactions
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
        navigateTo("Expenses")
        ui.clickText("Add expense")
        ui.enterText("Amount", "12.50", "transaction-form")
        ui.clickText("Food", "transaction-form")
        ui.showText("Food", "transaction-form").assertIsSelected()
        composeRule
            .onNodeWithTag("transaction-tag-${tagId("Lunch")}")
            .performScrollTo()
            .performClick()
            .assertIsOn()
        ui.clickText("Save")
        assertEquals(1250L, runBlocking { transactionRepository().loadTransactions().single().amountMinor })
    }

    private fun verifyRemainingBudgetOnBothScreens() {
        ui.showText("Spent  EUR 12.50")
        ui.showText("Limit  EUR 20.00")
        ui.showText("Remaining  EUR 7.50")
        ui.clickText("Back to overview")
        ui.showText("Budget progress")
        ui.showText("Household")
        ui.showText("Accounts: Cash")
        ui.showText("Spent  EUR 12.50")
        ui.showText("Remaining  EUR 7.50")
    }

    private fun verifyOverBudgetOnBothScreens() {
        ui.showText("Spent  EUR 25.00")
        ui.showText("Limit  EUR 20.00")
        ui.showText("Remaining  EUR -5.00")
        ui.clickText("Back to overview")
        ui.showText("Budget progress")
        ui.showText("Household")
        ui.showText("Remaining  EUR -5.00")
        ui.showText("Over budget")
    }

    private fun createFoodCategory() {
        openOrganizer()
        ui.enterText("Category name", "Food", "category-tag-list")
        ui.clickText("Add category", "category-tag-list")
        ui.showText("Food \u00b7 expense", "category-tag-list")
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun assertOverviewAccountSelection(
        name: String,
        isSelected: Boolean,
    ): Unit {
        composeRule.onNodeWithTag("overview-root").performTouchInput {
            swipe(
                start = center.copy(x = center.x + center.x - ACCOUNT_SELECTION_SWIPE_EDGE_INSET_PX),
                end = center.copy(x = center.x * ACCOUNT_SELECTION_SWIPE_END_RATIO),
                durationMillis = ACCOUNT_SELECTION_SWIPE_DURATION_MILLIS,
            )
        }
        val accountSelection: SemanticsNodeInteraction = composeRule.onNodeWithContentDescription("$name Account selection")
        if (isSelected) accountSelection.assertIsOn() else accountSelection.assertIsOff()
    }

    private fun openOrganizer() {
        navigateTo("Overview")
        composeRule.onNodeWithTag("open-navigation-drawer").performClick()
        composeRule.onNodeWithTag("drawer-Categories & Tags").performClick()
        ui.showText("Category name", "category-tag-list")
    }

    private class SeedAccountRule : ExternalResource() {
        override fun before() {
            val context: Context = ApplicationProvider.getApplicationContext()
            runBlocking { FinanceTestFixtures(AppDatabase.getInstance(context)).createFirstAccount() }
        }
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun seedArchivedReserveHistory(): Unit {
        val context: Context = ApplicationProvider.getApplicationContext()
        runBlocking {
            val database: AppDatabase = AppDatabase.getInstance(context)
            val fixtures: FinanceTestFixtures = FinanceTestFixtures(database)
            val cash: Account =
                fixtures.accounts
                    .loadSetup()
                    .accounts
                    .single { account: Account -> account.name == "Cash" }
            val reserve: Account = fixtures.createAdditionalAccount("Reserve")
            val date: Long = LocalDate.now().toEpochDay()
            fixtures.transactions.save(
                TransactionDraft(
                    accountId = reserve.id,
                    amountMinor = 250L,
                    direction = TransactionDirection.INCOME,
                    date = date,
                    note = null,
                ),
            )
            RoomTransferRepository(database).save(
                TransferDraft(
                    sourceAccountId = cash.id,
                    destinationAccountId = reserve.id,
                    amountMinor = 100L,
                    date = date,
                ),
            )
        }
    }

    private inner class SeedLedgerRule : TestWatcher() {
        override fun starting(description: Description) {
            if (description.methodName == "accountManagementWrapsArchiveActionAndKeepsArchivedHistoryReadOnly") {
                seedArchivedReserveHistory()
                return
            }
            if (description.methodName == "cancelAndConfirmTransactionDeletionUpdateLedger") {
                val context: Context = ApplicationProvider.getApplicationContext()
                runBlocking {
                    val fixtures: FinanceTestFixtures = FinanceTestFixtures(AppDatabase.getInstance(context))
                    val unrelatedAccount: Account = fixtures.createAdditionalAccount("Savings")
                    fixtures.transactions.save(
                        TransactionDraft(
                            accountId = unrelatedAccount.id,
                            amountMinor = 100L,
                            direction = TransactionDirection.INCOME,
                            date = LocalDate.now().toEpochDay(),
                            note = "unrelated record",
                        ),
                    )
                }
                return
            }
            if (description.methodName == "editTransactionKeepsIdentityAndRecalculatesBothAccountBalances") {
                val context: Context = ApplicationProvider.getApplicationContext()
                runBlocking {
                    val fixtures: FinanceTestFixtures = FinanceTestFixtures(AppDatabase.getInstance(context))
                    fixtures.createAdditionalAccount("Savings")
                    val category: FinanceCategory =
                        fixtures.organization
                            .saveCategory(CategoryDraft(name = "Food", direction = TransactionDirection.EXPENSE))
                            .categories
                            .single()
                    fixtures.organization.saveTag(TagDraft(name = "Lunch", categoryId = category.id))
                }
                return
            }
            if (description.methodName == "requiresAnActiveAccountBeforeSavingTransaction") {
                val context: Context = ApplicationProvider.getApplicationContext()
                runBlocking {
                    val fixtures: FinanceTestFixtures = FinanceTestFixtures(AppDatabase.getInstance(context))
                    val onlyAccount: Account =
                        fixtures.accounts
                            .loadSetup()
                            .accounts
                            .single()
                    fixtures.accounts.archiveAccount(onlyAccount.id)
                }
                return
            }
            if (description.methodName == "transactionEntryRemainsUsableAtLargeFontScale") {
                val context: Context = ApplicationProvider.getApplicationContext()
                runBlocking {
                    FinanceTestFixtures(
                        AppDatabase.getInstance(context),
                    ).organization.saveTag(TagDraft(name = "Large text tag"))
                }
                return
            }
            if (description.methodName == "transferEntryRemainsUsableAtLargeFontScale") {
                val context: Context = ApplicationProvider.getApplicationContext()
                runBlocking { FinanceTestFixtures(AppDatabase.getInstance(context)).createAdditionalAccount("Savings") }
                return
            }
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
                    RoomRecurringBudgetRepository(AppDatabase.getInstance(context)).save(
                        RecurringBudgetDraft(null, "Household", 1_000L, setOf(source.id)),
                        YearMonth.now(),
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
            if (description.methodName == "recurringBudgetUsesSharedMonthAndResetsWithoutCarryover") {
                val context: Context = ApplicationProvider.getApplicationContext()
                runBlocking {
                    val fixtures: FinanceTestFixtures = FinanceTestFixtures(AppDatabase.getInstance(context))
                    val account: Account =
                        fixtures.accounts
                            .loadSetup()
                            .accounts
                            .single()
                    RoomRecurringBudgetRepository(AppDatabase.getInstance(context)).save(
                        RecurringBudgetDraft(null, "Household", 1_000L, setOf(account.id)),
                        YearMonth.now(),
                    )
                }
                return
            }
            if (description.methodName == "recordAndEditTransferThroughFullScreenEntry") {
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
                        date = YearMonth.now().atEndOfMonth().toEpochDay(),
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
                        date = YearMonth.now().atDay(1).toEpochDay(),
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
