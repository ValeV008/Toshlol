package com.toshlol.personalfinance

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.ui.SetupScreen
import com.toshlol.personalfinance.ui.theme.ToshlolTheme
import com.toshlol.personalfinance.ui.viewmodel.FirstRunUiState
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityTest {
    private val composeRule: ComposeContentTestRule = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(composeRule).around(UiFailureArtifactsRule(composeRule))

    @Test
    fun exposeLabeledInputsSelectedTypesAndValidationAtLargeFont() {
        composeRule.setContent {
            val currentDensity: Density = LocalDensity.current
            var state: FirstRunUiState by remember {
                mutableStateOf(FirstRunUiState(isLoaded = true, error = "Enter an account name."))
            }
            CompositionLocalProvider(LocalDensity provides Density(currentDensity.density, 2f)) {
                ToshlolTheme {
                    SetupScreen(
                        state,
                        onCurrencyChanged = { state = state.copy(currency = it) },
                        onAccountNameChanged = { state = state.copy(accountName = it) },
                        onAccountTypeChanged = { state = state.copy(selectedType = it) },
                        onSubmit = {},
                    )
                }
            }
        }
        composeRule
            .onNodeWithText("Set up your first account")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        composeRule.onNodeWithContentDescription("Currency").performScrollTo().assertHasClickAction()
        composeRule.onNodeWithText("Account name").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Cash").performScrollTo().assertIsSelected()
        composeRule
            .onNodeWithText("Credit card")
            .performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertHeightIsAtLeast(48.dp)
            .performClick()
            .assertIsSelected()
        composeRule.onNodeWithText("Cash").assertIsNotSelected()
        composeRule
            .onNodeWithText("Enter an account name.")
            .performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        composeRule
            .onNodeWithText("Create account")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasClickAction()
    }
}
