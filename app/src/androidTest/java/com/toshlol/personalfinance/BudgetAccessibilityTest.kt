package com.toshlol.personalfinance

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.data.FinanceCategory
import com.toshlol.personalfinance.data.FinanceTag
import com.toshlol.personalfinance.data.RecurringBudgetProgress
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.ui.BudgetActions
import com.toshlol.personalfinance.ui.BudgetsScreen
import com.toshlol.personalfinance.ui.theme.ToshlolTheme
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File
import java.io.OutputStream
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class BudgetAccessibilityTest {
    private val composeRule = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(composeRule).around(UiFailureArtifactsRule(composeRule))

    @Test
    @Suppress("ktlint:standard:no-unit-return")
    fun operateNamedBudgetEditorAtLargeFont(): Unit {
        composeRule.setContent {
            val currentDensity: Density = LocalDensity.current
            val month: YearMonth = YearMonth.now()
            val accounts: List<Account> =
                listOf(
                    Account("cash", "Cash", AccountType.CASH, 1L, 2L),
                    Account("savings", "Long term savings", AccountType.BANK, 3L, 4L, isArchived = true),
                )
            val budget: RecurringBudgetProgress =
                RecurringBudgetProgress(
                    "household",
                    "Household essentials",
                    month,
                    setOf("cash"),
                    20_000L,
                    22_500L,
                    5L,
                    6L,
                    setOf("groceries"),
                    setOf("weekend", "holiday"),
                )
            var state: TransactionsUiState by
                remember {
                    mutableStateOf(
                        TransactionsUiState(
                            isLoaded = true,
                            currency = "EUR",
                            selectedMonth = month.toString(),
                            budgetMonth = month.toString(),
                            accounts = accounts,
                            categories =
                                listOf(
                                    FinanceCategory("groceries", "Groceries", TransactionDirection.EXPENSE, false),
                                    FinanceCategory("archived-dining", "Dining", TransactionDirection.EXPENSE, true),
                                    FinanceCategory("income", "Salary", TransactionDirection.INCOME, false),
                                ),
                            tags =
                                listOf(
                                    FinanceTag("weekend", "weekend", null, null, false),
                                    FinanceTag("holiday", "holiday", null, null, true),
                                ),
                            recurringBudgets = listOf(budget),
                            editingBudgetId = budget.id,
                            budgetName = budget.name,
                            budgetAmount = "200.00",
                            budgetAccountIds = setOf("cash"),
                            budgetCategoryIds = setOf("groceries"),
                            budgetTagIds = setOf("weekend", "holiday"),
                        ),
                    )
                }
            CompositionLocalProvider(LocalDensity provides Density(currentDensity.density, 2f)) {
                ToshlolTheme {
                    BudgetsScreen(
                        state = state,
                        actions =
                            BudgetActions(
                                open = {},
                                close = {},
                                monthChanged = { value -> state = state.copy(budgetMonth = value, selectedMonth = value) },
                                nameChanged = { value -> state = state.copy(budgetName = value) },
                                amountChanged = { value -> state = state.copy(budgetAmount = value) },
                                accountToggled = { id ->
                                    val accountIds: Set<String> = state.budgetAccountIds
                                    state = state.copy(budgetAccountIds = if (id in accountIds) accountIds - id else accountIds + id)
                                },
                                categoryToggled = { id ->
                                    val categoryIds: Set<String> = state.budgetCategoryIds
                                    state = state.copy(budgetCategoryIds = if (id in categoryIds) categoryIds - id else categoryIds + id)
                                },
                                tagToggled = { id ->
                                    val tagIds: Set<String> = state.budgetTagIds
                                    state = state.copy(budgetTagIds = if (id in tagIds) tagIds - id else tagIds + id)
                                },
                                create = {},
                                edit = {},
                                save = {
                                    state =
                                        state.copy(
                                            error = if (state.budgetAccountIds.isEmpty()) "Choose at least one Account." else null,
                                        )
                                },
                            ),
                        onAddTransaction = {},
                    )
                }
            }
        }
        composeRule.onNodeWithTag("budget-card-title-household").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Accounts: Cash").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Categories: Groceries").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Tags: holiday (Archived), weekend").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Spent  EUR 225.00").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Remaining  EUR -25.00").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Over budget").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Budget name").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Monthly limit (EUR)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Expense Categories").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Selected: Groceries").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Dining · Archived").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Salary").assertDoesNotExist()
        composeRule
            .onNodeWithTag("budget-category-groceries")
            .performScrollTo()
            .assertIsSelected()
            .assertHeightIsAtLeast(48.dp)
        composeRule
            .onNodeWithTag("budget-tag-weekend")
            .performScrollTo()
            .assertIsSelected()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Selected: Groceries").performScrollTo()
        saveScreenEvidence("large-font-combined.png")
        composeRule.onNodeWithTag("budget-category-groceries").performClick().assertIsNotSelected()
        composeRule.onNodeWithText("Selected: Any Expense Category").performScrollTo().assertIsDisplayed()
        saveScreenEvidence("large-font-tag-only.png")
        composeRule.onNodeWithTag("budget-category-groceries").performClick().assertIsSelected()
        composeRule.onNodeWithTag("budget-tag-weekend").performClick().assertIsNotSelected()
        composeRule
            .onNodeWithTag("budget-tag-holiday")
            .performScrollTo()
            .performClick()
            .assertIsNotSelected()
        composeRule.onNodeWithText("Selected: No Tag Restriction").performScrollTo().assertIsDisplayed()
        saveScreenEvidence("large-font-category-only.png")
        composeRule.onNodeWithTag("budget-tag-weekend").performClick().assertIsSelected()
        composeRule
            .onNodeWithTag("budget-tag-holiday")
            .performScrollTo()
            .performClick()
            .assertIsSelected()
        composeRule.onNodeWithText("Over budget").performScrollTo()
        saveScreenEvidence("large-font-over-budget.png")
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

    @Suppress("ktlint:standard:no-unit-return")
    private fun saveScreenEvidence(name: String): Unit {
        val outputRoot: File =
            InstrumentationRegistry
                .getArguments()
                .getString("additionalTestOutputDir")
                ?.takeIf(String::isNotBlank)
                ?.let(::File)
                ?: androidx.test.core.app.ApplicationProvider
                    .getApplicationContext<android.content.Context>()
                    .cacheDir
        val outputDirectory: File = File(outputRoot, "budget-restriction-evidence")
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
}
