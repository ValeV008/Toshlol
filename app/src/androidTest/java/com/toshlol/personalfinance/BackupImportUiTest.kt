package com.toshlol.personalfinance

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupImportUiTest {
    private val composeRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> = createAndroidComposeRule()
    private val ui: FinanceUiDriver = FinanceUiDriver(composeRule)

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(ResetFinanceDataRule()).around(composeRule).around(UiFailureArtifactsRule(composeRule))

    @Test
    fun settingsLaunchesDocumentPickerForMergeAndCancellationReturnsToSettings() {
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithText("Set up your first account").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Currency").performClick()
        composeRule.onNodeWithText("EUR").performClick()
        ui.enterText("Account name", "Cash")
        ui.clickText("Create account")
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithText("EUR ledger is ready.").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Settings").performClick()
        val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        composeRule.onNodeWithText("Import Backup (Merge)").performClick()
        assertTrue("Android document picker did not open", device.wait(Until.hasObject(By.pkg("com.android.documentsui")), 10_000))
        device.pressBack()
        composeRule.onNodeWithText("Backup import canceled.").assertIsDisplayed()
    }
}
