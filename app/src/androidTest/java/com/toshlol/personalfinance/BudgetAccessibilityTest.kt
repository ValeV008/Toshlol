package com.toshlol.personalfinance

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.toshlol.personalfinance.data.RecurringBudgetRestrictions
import com.toshlol.personalfinance.data.RecurringBudgetTotals
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.ui.BudgetActions
import com.toshlol.personalfinance.ui.BudgetScopeActions
import com.toshlol.personalfinance.ui.BudgetsScreen
import com.toshlol.personalfinance.ui.theme.ToshlolTheme
import com.toshlol.personalfinance.ui.viewmodel.BudgetEditorScope
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
        composeRule.setContent { LargeFontBudgetEditor(createLargeFontBudgetState()) }
        assertBudgetProgressAndEditorVisible()
        assertLargeFontFilterControls()
        captureCategoryAndTagRestrictionSummaries()
        verifyAccountSelectionValidation()
    }

    @Test
    @Suppress("ktlint:standard:no-unit-return")
    fun showsRenamedCategoryAndTagNamesForSavedBudget(): Unit {
        val renamedState: TransactionsUiState =
            createLargeFontBudgetState().copy(
                categories =
                    listOf(
                        FinanceCategory("groceries", "Pantry", TransactionDirection.EXPENSE, true),
                        FinanceCategory("archived-dining", "Dining", TransactionDirection.EXPENSE, true),
                        FinanceCategory("income", "Salary", TransactionDirection.INCOME, false),
                    ),
                tags =
                    listOf(
                        FinanceTag("weekend", "Occasion", null, null, true),
                        FinanceTag("holiday", "holiday", null, null, true),
                    ),
            )
        composeRule.setContent { LargeFontBudgetEditor(renamedState) }
        composeRule.onNodeWithText("Categories: Pantry (Archived)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Tags: holiday (Archived), Occasion (Archived)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Selected: Pantry (Archived)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Selected: holiday (Archived), Occasion (Archived)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("budget-category-groceries").assertIsSelected()
        composeRule.onNodeWithTag("budget-tag-weekend").assertIsSelected()
    }

    @Suppress("ktlint:standard:no-unit-return")
    @Composable
    private fun LargeFontBudgetEditor(initialState: TransactionsUiState): Unit {
        val currentDensity: Density = LocalDensity.current
        val state: MutableState<TransactionsUiState> = remember { mutableStateOf(initialState) }
        CompositionLocalProvider(LocalDensity provides Density(currentDensity.density, 2f)) {
            ToshlolTheme {
                BudgetsScreen(state = state.value, actions = createLargeFontBudgetActions(state), onAddTransaction = {})
            }
        }
    }

    private fun createLargeFontBudgetState(): TransactionsUiState {
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
                RecurringBudgetTotals(20_000L, 22_500L),
                5L,
                6L,
                restrictions = RecurringBudgetRestrictions(setOf("groceries"), setOf("weekend", "holiday")),
            )
        return TransactionsUiState(
            isLoaded = true,
            currency = "EUR",
            selectedMonth = month.toString(),
            budgetMonth = month.toString(),
            accounts = accounts,
            categories = createLargeFontCategories(),
            tags = createLargeFontTags(),
            recurringBudgets = listOf(budget),
            editingBudgetId = budget.id,
            budgetName = budget.name,
            budgetAmount = "200.00",
            budgetScope =
                BudgetEditorScope(
                    accountIds = setOf("cash"),
                    restrictions = RecurringBudgetRestrictions(setOf("groceries"), setOf("weekend", "holiday")),
                ),
        )
    }

    private fun createLargeFontCategories(): List<FinanceCategory> =
        listOf(
            FinanceCategory("groceries", "Groceries", TransactionDirection.EXPENSE, false),
            FinanceCategory("archived-dining", "Dining", TransactionDirection.EXPENSE, true),
            FinanceCategory("income", "Salary", TransactionDirection.INCOME, false),
        )

    private fun createLargeFontTags(): List<FinanceTag> =
        listOf(FinanceTag("weekend", "weekend", null, null, false), FinanceTag("holiday", "holiday", null, null, true))

    private fun createLargeFontBudgetActions(state: MutableState<TransactionsUiState>): BudgetActions =
        BudgetActions(
            open = {},
            close = {},
            monthChanged = { value: String -> state.value = state.value.copy(budgetMonth = value, selectedMonth = value) },
            nameChanged = { value: String -> state.value = state.value.copy(budgetName = value) },
            amountChanged = { value: String -> state.value = state.value.copy(budgetAmount = value) },
            scope =
                BudgetScopeActions(
                    accountToggled = { id: String ->
                        state.value =
                            state.value.copy(
                                budgetScope =
                                    state.value.budgetScope.copy(
                                        accountIds = toggleSelectedId(state.value.budgetScope.accountIds, id),
                                    ),
                            )
                    },
                    categoryToggled = { id: String ->
                        state.value =
                            state.value.copy(
                                budgetScope =
                                    state.value.budgetScope.copy(
                                        restrictions =
                                            state.value.budgetScope.restrictions.copy(
                                                categoryIds = toggleSelectedId(state.value.budgetScope.restrictions.categoryIds, id),
                                            ),
                                    ),
                            )
                    },
                    tagToggled = { id: String ->
                        state.value =
                            state.value.copy(
                                budgetScope =
                                    state.value.budgetScope.copy(
                                        restrictions =
                                            state.value.budgetScope.restrictions.copy(
                                                tagIds = toggleSelectedId(state.value.budgetScope.restrictions.tagIds, id),
                                            ),
                                    ),
                            )
                    },
                ),
            create = {},
            edit = {},
            save = {
                state.value =
                    state.value.copy(
                        error =
                            if (state.value.budgetScope.accountIds
                                    .isEmpty()
                            ) {
                                "Choose at least one Account."
                            } else {
                                null
                            },
                    )
            },
        )

    private fun toggleSelectedId(
        selectedIds: Set<String>,
        id: String,
    ): Set<String> = if (id in selectedIds) selectedIds - id else selectedIds + id

    @Suppress("ktlint:standard:no-unit-return")
    private fun assertBudgetProgressAndEditorVisible(): Unit {
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
        composeRule.onNodeWithText("Dining \u00B7 Archived").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Salary").assertDoesNotExist()
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun assertLargeFontFilterControls(): Unit {
        composeRule.onNodeWithText("Selected: Groceries").performScrollTo().assertIsDisplayed()
        assertBudgetChoiceAccessible("budget-category-groceries")
        assertBudgetChoiceAccessible("budget-tag-weekend")
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun assertBudgetChoiceAccessible(tag: String): Unit {
        composeRule
            .onNodeWithTag(tag)
            .performScrollTo()
            .assertIsSelected()
            .assertHeightIsAtLeast(48.dp)
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun captureCategoryAndTagRestrictionSummaries(): Unit {
        saveScreenEvidence("large-font-combined.png")
        captureTagOnlySummary()
        captureCategoryOnlySummary()
        restoreCombinedRestrictionSummary()
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun captureTagOnlySummary(): Unit {
        composeRule.onNodeWithTag("budget-category-groceries").performClick().assertIsNotSelected()
        composeRule.onNodeWithText("Selected: Any Expense Category").performScrollTo().assertIsDisplayed()
        saveScreenEvidence("large-font-tag-only.png")
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun captureCategoryOnlySummary(): Unit {
        composeRule.onNodeWithTag("budget-category-groceries").performClick().assertIsSelected()
        composeRule.onNodeWithTag("budget-tag-weekend").performClick().assertIsNotSelected()
        composeRule
            .onNodeWithTag("budget-tag-holiday")
            .performScrollTo()
            .performClick()
            .assertIsNotSelected()
        composeRule.onNodeWithText("Selected: No Tag Restriction").performScrollTo().assertIsDisplayed()
        saveScreenEvidence("large-font-category-only.png")
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun restoreCombinedRestrictionSummary(): Unit {
        composeRule.onNodeWithTag("budget-tag-weekend").performClick().assertIsSelected()
        composeRule
            .onNodeWithTag("budget-tag-holiday")
            .performScrollTo()
            .performClick()
            .assertIsSelected()
        composeRule.onNodeWithText("Over budget").performScrollTo()
        saveScreenEvidence("large-font-over-budget.png")
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun verifyAccountSelectionValidation(): Unit {
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
