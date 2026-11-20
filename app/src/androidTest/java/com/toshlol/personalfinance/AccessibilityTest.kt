@file:Suppress("ktlint:standard:no-unit-return")

package com.toshlol.personalfinance

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.ui.SetupScreen
import com.toshlol.personalfinance.ui.ShowLoading
import com.toshlol.personalfinance.ui.ShowPersonalFinanceTopNavigation
import com.toshlol.personalfinance.ui.theme.ToshlolTheme
import com.toshlol.personalfinance.ui.viewmodel.FirstRunUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class AccessibilityTest {
    private val composeRule: ComposeContentTestRule = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(composeRule).around(UiFailureArtifactsRule(composeRule))

    @Test
    fun exposeLabeledInputsSelectedTypesAndValidationAtLargeFont(): Unit {
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

    @Test
    fun exposeRetryActionForSetupLoadFailureAtLargeFont(): Unit {
        var retryCount: Int = 0
        composeRule.setContent {
            val currentDensity: Density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(currentDensity.density, 2f)) {
                ToshlolTheme {
                    ShowLoading(
                        hasFailed = true,
                        onRetry = { retryCount += 1 },
                        loadingMessage = "Loading your setup…",
                        failureMessage = "Could not load your setup. Your saved data has not been changed.",
                    )
                }
            }
        }
        composeRule
            .onNodeWithText("Could not load your setup. Your saved data has not been changed.")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        composeRule
            .onNodeWithText("Try again")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertHasClickAction()
            .performClick()
        assertEquals(1, retryCount)
        saveRetryScreenshot("first-run-retry-large-font.png")
    }

    @Test
    fun exposeSelectedNavigationAndMenuAtLargeFont(): Unit {
        var menuOpenCount: Int = 0
        composeRule.setContent {
            val currentDensity: Density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(currentDensity.density, 2f)) {
                ToshlolTheme {
                    ShowPersonalFinanceTopNavigation(
                        selectedArea = "Overview",
                        onOpenDrawer = { menuOpenCount += 1 },
                        onAreaSelected = {},
                    )
                }
            }
        }
        composeRule
            .onNodeWithTag("current-area")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Current tab"))
        composeRule.onAllNodesWithTag("tab-Overview").assertCountEquals(0)
        composeRule.onNodeWithTag("open-navigation-drawer").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, menuOpenCount)
    }

    @Test
    fun announceSetupLoadingProgressAtLargeFont(): Unit {
        composeRule.setContent {
            val currentDensity: Density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(currentDensity.density, 2f)) {
                ToshlolTheme {
                    ShowLoading(
                        hasFailed = false,
                        onRetry = {},
                        loadingMessage = "Loading your setup…",
                        failureMessage = "Could not load your setup. Your saved data has not been changed.",
                    )
                }
            }
        }
        composeRule
            .onNodeWithText("Loading your setup…")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).assertCountEquals(1)
    }

    @Test
    fun showAccountSavingProgressAtLargeFont(): Unit {
        composeRule.setContent {
            val currentDensity: Density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(currentDensity.density, 2f)) {
                ToshlolTheme {
                    SetupScreen(
                        FirstRunUiState(
                            currency = "EUR",
                            accountName = "Travel cash",
                            selectedType = AccountType.BANK,
                            isLoaded = true,
                            isSaving = true,
                        ),
                        onCurrencyChanged = {},
                        onAccountNameChanged = {},
                        onAccountTypeChanged = {},
                        onSubmit = {},
                    )
                }
            }
        }
        composeRule.onNodeWithText("Saving account…").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).assertCountEquals(1)
    }

    @Test
    fun captureRetryStateAtNormalFont(): Unit {
        composeRule.setContent {
            ToshlolTheme {
                ShowLoading(
                    hasFailed = true,
                    onRetry = {},
                    loadingMessage = "Loading your setup…",
                    failureMessage = "Could not load your setup. Your saved data has not been changed.",
                )
            }
        }
        composeRule.onNodeWithText("Try again").assertIsDisplayed()
        saveRetryScreenshot("first-run-retry.png")
    }

    private fun saveRetryScreenshot(fileName: String): Unit {
        val outputDirectory: File = requireNotNull(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null))
        val screenshot: Bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        FileOutputStream(File(outputDirectory, fileName)).use { output ->
            assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, SCREENSHOT_QUALITY, output))
        }
    }

    private companion object {
        const val SCREENSHOT_QUALITY: Int = 100
    }
}
