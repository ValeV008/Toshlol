package com.toshlol.personalfinance

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.data.RecurringBudgetProgress
import com.toshlol.personalfinance.ui.BudgetsScreen
import com.toshlol.personalfinance.ui.theme.ToshlolTheme
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class BudgetAccessibilityTest {
    private val composeRule = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(composeRule).around(UiFailureArtifactsRule(composeRule))

    @Test
    fun operateNamedBudgetEditorAtLargeFont() {
        composeRule.setContent {
            val currentDensity: Density = LocalDensity.current
            val month: YearMonth = YearMonth.now()
            val accounts: List<Account> =
                listOf(
                    Account("cash", "Cash", AccountType.CASH, 1L, 2L),
                    Account("savings", "Long term savings", AccountType.BANK, 3L, 4L, isArchived = true),
                )
            val budget: RecurringBudgetProgress =
                RecurringBudgetProgress("household", "Household essentials", month, setOf("cash"), 20_000L, 22_500L, 5L, 6L)
            var state: TransactionsUiState by
                remember {
                    mutableStateOf(
                        TransactionsUiState(
                            isLoaded = true,
                            currency = "EUR",
                            selectedMonth = month.toString(),
                            budgetMonth = month.toString(),
                            accounts = accounts,
                            recurringBudgets = listOf(budget),
                            editingBudgetId = budget.id,
                            budgetName = budget.name,
                            budgetAmount = "200.00",
                            budgetAccountIds = setOf("cash"),
                        ),
                    )
                }
            CompositionLocalProvider(LocalDensity provides Density(currentDensity.density, 2f)) {
                ToshlolTheme {
                    BudgetsScreen(
                        state = state,
                        onBack = {},
                        onMonthChanged = { value -> state = state.copy(budgetMonth = value, selectedMonth = value) },
                        onNameChanged = { value -> state = state.copy(budgetName = value) },
                        onAmountChanged = { value -> state = state.copy(budgetAmount = value) },
                        onAccountToggled = { id ->
                            val accountIds: Set<String> = state.budgetAccountIds
                            state = state.copy(budgetAccountIds = if (id in accountIds) accountIds - id else accountIds + id)
                        },
                        onCreate = {},
                        onEdit = {},
                        onSave = {
                            state =
                                state.copy(
                                    error = if (state.budgetAccountIds.isEmpty()) "Choose at least one Account." else null,
                                )
                        },
                        onAddTransaction = {},
                    )
                }
            }
        }
        composeRule.onNodeWithTag("budget-card-title-household").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Accounts: Cash").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Spent  EUR 225.00").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Remaining  EUR -25.00").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Over budget").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Budget name").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Monthly limit (EUR)").performScrollTo().assertIsDisplayed()
        composeRule
            .onNodeWithTag("budget-account-cash")
            .performScrollTo()
            .assertIsSelected()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithText("Save changes").performScrollTo().performClick()
        composeRule.onNodeWithText("Choose at least one Account.").performScrollTo().assertIsDisplayed()
        composeRule
            .onNodeWithTag("budget-account-savings")
            .performScrollTo()
            .assertHasClickAction()
            .performClick()
            .assertIsSelected()
        composeRule
            .onNodeWithText("Save changes")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        composeRule.onNodeWithText("Choose at least one Account.").assertDoesNotExist()
    }
}
