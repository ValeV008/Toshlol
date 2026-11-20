@file:Suppress("ktlint:standard:filename", "ktlint:standard:no-unit-return")

package com.toshlol.personalfinance

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.data.BackupImportMode
import com.toshlol.personalfinance.ui.BackupStatusNotice
import com.toshlol.personalfinance.ui.SettingsActions
import com.toshlol.personalfinance.ui.SettingsScreen
import com.toshlol.personalfinance.ui.theme.ToshlolTheme
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsAccessibilityTest {
    private val composeRule: ComposeContentTestRule = createComposeRule()

    private data class SettingsActionState(
        var importMode: BackupImportMode? = null,
        var resetCount: Int = 0,
        var organizerCount: Int = 0,
    )

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(composeRule)
            .around(UiFailureArtifactsRule(composeRule))

    @Test
    fun operateDataManagementAtLargeFont(): Unit {
        val actions: SettingsActionState = SettingsActionState()
        setLargeFontSettings(actions)
        assertLargeFontProfile()
        saveLargeFontSettingsScreenshot()
        exerciseOrganizerAndMerge(actions)
        assertDestructiveSettingsActionsAccessible()
        exerciseOverwriteConfirmation(actions)
        exerciseResetConfirmation(actions)
    }

    private fun setLargeFontSettings(actions: SettingsActionState): Unit {
        composeRule.setContent {
            val currentDensity: Density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(currentDensity.density, 1.7f)) {
                ToshlolTheme {
                    SettingsScreen(
                        state = TransactionsUiState(currency = "EUR", accountName = "Cash", selectedMonth = "2026-11"),
                        actions =
                            SettingsActions(
                                openOrganizer = { actions.organizerCount += 1 },
                                createTransaction = {},
                                createTransfer = {},
                                exportBackup = {},
                                importBackup = { mode: BackupImportMode -> actions.importMode = mode },
                                resetData = { actions.resetCount += 1 },
                            ),
                    )
                }
            }
        }
    }

    private fun assertLargeFontProfile(): Unit {
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Currency: EUR").assertIsDisplayed()
        composeRule.onNodeWithText("Account: Cash").assertIsDisplayed()
        composeRule.onNodeWithText("Viewing month: 2026-11").assertIsDisplayed()
    }

    private fun saveLargeFontSettingsScreenshot(): Unit {
        composeRule.waitForIdle()
        saveTestScreenshot(TestScreenshotEvidence("settings-evidence", "settings-large-font.png"))
    }

    private fun exerciseOrganizerAndMerge(actions: SettingsActionState): Unit {
        composeRule
            .onNodeWithText("Manage categories and tags")
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertEquals(1, actions.organizerCount)
        composeRule
            .onNodeWithText("Import Backup (Merge)")
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertEquals(BackupImportMode.MERGE, actions.importMode)
    }

    private fun assertDestructiveSettingsActionsAccessible(): Unit {
        composeRule.onNodeWithText("Import Backup (Overwrite)").performScrollTo().assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Delete all local data").performScrollTo().assertHeightIsAtLeast(48.dp)
    }

    private fun exerciseOverwriteConfirmation(actions: SettingsActionState): Unit {
        composeRule.onNodeWithText("Import Backup (Overwrite)").performScrollTo().performClick()
        composeRule.onNodeWithText("Overwrite local data?").assertIsDisplayed()
        composeRule.waitForIdle()
        saveTestScreenshot(TestScreenshotEvidence("settings-evidence", "settings-overwrite-confirmation.png"))
        composeRule.onNodeWithText("Cancel").performClick()
        assertEquals(BackupImportMode.MERGE, actions.importMode)
        composeRule.onNodeWithText("Import Backup (Overwrite)").performScrollTo().performClick()
        composeRule.onNodeWithText("Choose Backup").performClick()
        assertEquals(BackupImportMode.OVERWRITE, actions.importMode)
    }

    private fun exerciseResetConfirmation(actions: SettingsActionState): Unit {
        composeRule.onNodeWithText("Delete all local data").performScrollTo().performClick()
        composeRule.onNodeWithText("Delete all local data?").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        assertEquals(0, actions.resetCount)
        composeRule.onNodeWithText("Delete all local data").performScrollTo().performClick()
        composeRule.onNodeWithText("Delete all data").performClick()
        assertEquals(1, actions.resetCount)
    }

    @Test
    fun announceBackupFailureAsLiveStatus(): Unit {
        val statusMessage: String = "Backup import failed: Backup currency does not match the local ledger."
        composeRule.setContent {
            ToshlolTheme {
                androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize()) {
                    BackupStatusNotice(statusMessage)
                }
            }
        }
        composeRule
            .onNodeWithTag("backup-status")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        composeRule.onNodeWithText("Data management result").assertIsDisplayed()
        composeRule.onNodeWithText(statusMessage).assertIsDisplayed()
        composeRule.waitForIdle()
        val screenshot: Bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        saveTestScreenshot(TestScreenshotEvidence("settings-evidence", "settings-backup-status-error.png", screenshot))
    }
}
