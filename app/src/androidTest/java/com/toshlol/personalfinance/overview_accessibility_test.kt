@file:Suppress("ktlint:standard:filename", "ktlint:standard:no-unit-return")

package com.toshlol.personalfinance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.data.CategoryBudget
import com.toshlol.personalfinance.data.FinanceTransaction
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.ui.OverviewScreen
import com.toshlol.personalfinance.ui.OverviewScreenActions
import com.toshlol.personalfinance.ui.theme.ToshlolTheme
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class OverviewAccessibilityTest {
    private val composeRule: ComposeContentTestRule = createComposeRule()

    @get:Rule
    val rules: org.junit.rules.RuleChain =
        org.junit.rules.RuleChain
            .outerRule(composeRule)
            .around(UiFailureArtifactsRule(composeRule))

    @Test
    fun largeFontOverviewControlsRemainAccessible(): Unit {
        renderLargeFontOverview()
        assertLargeFontHeaderAndSummary()
        assertLargeFontBudgetAndActions()
    }

    @Test
    fun largeFontDrawerSelectionPreservesActivityOrder(): Unit {
        renderLargeFontOverview()
        openAccountDrawer()
        selectDrawerAccount()
        closeAccountDrawer()
        assertBudgetContentDisplayed()
        assertActivityOrder()
    }

    private fun renderLargeFontOverview(): Unit {
        composeRule.setContent { LargeFontOverviewFixture() }
    }

    private fun assertLargeFontHeaderAndSummary(): Unit {
        composeRule
            .onNodeWithTag("next-month")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        composeRule
            .onNodeWithTag("previous-month")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Expenses").performScrollTo().assertIsDisplayed()
    }

    private fun assertLargeFontBudgetAndActions(): Unit {
        composeRule.onNodeWithText("Add expense").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Add income").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Transfer money").performScrollTo().assertIsDisplayed()
        assertBudgetContentDisplayed()
    }

    private fun assertBudgetContentDisplayed(): Unit {
        composeRule.onNodeWithText("Food").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Spent  EUR 70.00").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Limit  EUR 250.00").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Remaining  EUR 180.00").performScrollTo().assertIsDisplayed()
    }

    private fun openAccountDrawer(): Unit {
        composeRule.onNodeWithTag("overview-root").assertIsDisplayed().performTouchInput {
            swipe(start = center.copy(x = center.x * 2f - 1f), end = center.copy(x = center.x * 0.8f), durationMillis = 450)
        }
    }

    private fun selectDrawerAccount(): Unit {
        val accountSwitch: SemanticsNodeInteraction = composeRule.onNodeWithContentDescription("Emergency savings 1 Account selection")
        accountSwitch
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
            .assertIsOn()
        composeRule.onNodeWithText("Emergency savings 5").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Emergency savings 5 Account selection").assertHeightIsAtLeast(48.dp)
    }

    private fun closeAccountDrawer(): Unit {
        composeRule.onNodeWithText("Close").performScrollTo().performClick()
        composeRule.onNodeWithTag("overview-root").assertIsDisplayed()
    }

    private fun assertActivityOrder(): Unit {
        val separator: String = "\u00b7"
        composeRule.onNodeWithText("$olderDate $separator Emergency savings 1 $separator older-date").performScrollTo()
        val sameDateZTop: Float = activityItemTop("$monthDate $separator Emergency savings 1 $separator same-date-z")
        val sameDateATop: Float = activityItemTop("$monthDate $separator Emergency savings 1 $separator same-date-a")
        val olderDateTop: Float = activityItemTop("$olderDate $separator Emergency savings 1 $separator older-date")
        assertTrue("same-date id descending tie-break", sameDateZTop < sameDateATop)
        assertTrue("newer dates precede older dates", sameDateATop < olderDateTop)
    }

    private fun activityItemTop(label: String): Float =
        composeRule
            .onNodeWithText(label)
            .fetchSemanticsNode()
            .boundsInRoot.top

    @Composable
    private fun LargeFontOverviewFixture(): Unit {
        val currentDensity: Density = LocalDensity.current
        val accounts: List<Account> = remember { createOverviewAccounts() }
        var state: TransactionsUiState by remember { mutableStateOf(createOverviewState(accounts)) }
        CompositionLocalProvider(LocalDensity provides Density(currentDensity.density, 2f)) {
            ToshlolTheme {
                OverviewScreen(state, createOverviewActions(state) { updatedState: TransactionsUiState -> state = updatedState })
            }
        }
    }

    private fun createOverviewAccounts(): List<Account> =
        (1..5).map { accountNumber: Int ->
            Account(
                "emergency-$accountNumber",
                "Emergency savings $accountNumber",
                AccountType.BANK,
                0L,
                0L,
                100_000L,
                LocalDate.now().toEpochDay(),
            )
        }

    private fun createOverviewState(accounts: List<Account>): TransactionsUiState {
        val month: YearMonth = YearMonth.now()
        val accountId: String = accounts.first().id
        return TransactionsUiState(
            isLoaded = true,
            selectedMonth = month.toString(),
            accounts = accounts,
            budgets = listOf(CategoryBudget("food-budget", "food", "Food", false, month, 25_000L, 7_000L)),
            transactions =
                listOf(
                    createTransaction("same-z", accountId, month.atDay(20), 300L, 3_000L, "same-date-z"),
                    createTransaction("same-a", accountId, month.atDay(20), 300L, 2_000L, "same-date-a"),
                    createTransaction("older-date", accountId, month.atDay(19), 900L, 9_000L, "older-date"),
                ),
        )
    }

    private fun createOverviewActions(
        state: TransactionsUiState,
        onStateChanged: (TransactionsUiState) -> Unit,
    ): OverviewScreenActions =
        OverviewScreenActions(
            create = {},
            edit = {},
            delete = {},
            openOrganizer = {},
            openBudgets = {},
            createTransfer = {},
            editTransfer = {},
            openAccountHistory = {},
            monthChanged = { month: String -> onStateChanged(state.copy(selectedMonth = month)) },
            overviewAccountToggled = { id: String ->
                val selectedIds: Set<String> = state.selectedOverviewAccountIds
                onStateChanged(state.copy(selectedOverviewAccountIds = if (id in selectedIds) selectedIds - id else selectedIds + id))
            },
            accountDrawerChanged = { isOpen: Boolean -> onStateChanged(state.copy(isAccountDrawerOpen = isOpen)) },
        )

    private val monthDate: String = LocalDate.now().withDayOfMonth(20).toString()
    private val olderDate: String = LocalDate.now().withDayOfMonth(19).toString()

    private fun createTransaction(
        id: String,
        accountId: String,
        date: LocalDate,
        amountMinor: Long,
        createdAt: Long,
        note: String,
    ): FinanceTransaction =
        FinanceTransaction(
            id,
            accountId,
            "Emergency savings 1",
            amountMinor,
            TransactionDirection.EXPENSE,
            date.toEpochDay(),
            null,
            null,
            emptyList(),
            emptyList(),
            note,
            createdAt,
            createdAt,
        )
}
