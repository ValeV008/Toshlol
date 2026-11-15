@file:Suppress("ktlint:standard:filename", "ktlint:standard:no-unit-return")

package com.toshlol.personalfinance

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
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.ui.OverviewScreen
import com.toshlol.personalfinance.ui.OverviewScreenActions
import com.toshlol.personalfinance.ui.theme.ToshlolTheme
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState
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
    fun operateOverviewDrawerAtLargeFont(): Unit {
        composeRule.setContent {
            val currentDensity: Density = LocalDensity.current
            val account: Account =
                Account("emergency", "Emergency savings", AccountType.BANK, 0L, 0L, 100_000L, LocalDate.now().toEpochDay())
            var state: TransactionsUiState by remember {
                mutableStateOf(TransactionsUiState(isLoaded = true, selectedMonth = YearMonth.now().toString(), accounts = listOf(account)))
            }
            CompositionLocalProvider(LocalDensity provides Density(currentDensity.density, 2f)) {
                ToshlolTheme {
                    OverviewScreen(
                        state,
                        OverviewScreenActions(
                            create = {},
                            edit = {},
                            delete = {},
                            openOrganizer = {},
                            openBudgets = {},
                            createTransfer = {},
                            editTransfer = {},
                            openAccountHistory = {},
                            monthChanged = { month: String -> state = state.copy(selectedMonth = month) },
                            overviewAccountToggled = { id: String ->
                                val selectedIds: Set<String> = state.selectedOverviewAccountIds
                                state =
                                    state.copy(selectedOverviewAccountIds = if (id in selectedIds) selectedIds - id else selectedIds + id)
                            },
                            accountDrawerChanged = { isOpen: Boolean -> state = state.copy(isAccountDrawerOpen = isOpen) },
                        ),
                    )
                }
            }
        }
        composeRule.onNodeWithTag("overview-root").assertIsDisplayed().performTouchInput {
            swipe(start = center.copy(x = center.x * 2f - 1f), end = center.copy(x = center.x * 0.8f), durationMillis = 450)
        }
        val accountSwitch: SemanticsNodeInteraction = composeRule.onNodeWithContentDescription("Emergency savings Account selection")
        accountSwitch
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
            .assertIsOn()
        composeRule.onNodeWithTag("next-month").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithContentDescription("Emergency savings Account selection").performClick()
        composeRule.onNodeWithContentDescription("Emergency savings Account selection").assertIsDisplayed()
        composeRule.onNodeWithTag("overview-root").assertIsDisplayed()
    }
}
