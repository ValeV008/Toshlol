package com.toshlol.personalfinance

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.toshlol.personalfinance.data.BackupAccount
import com.toshlol.personalfinance.data.BackupSettings
import com.toshlol.personalfinance.data.FinanceBackup
import com.toshlol.personalfinance.data.FinanceBackupCodec
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
        createOverwriteBackupDocument()
        val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        composeRule.onNodeWithText("Import Backup (Merge)").performScrollTo().performClick()
        assertTrue("Android document picker did not open", device.wait(Until.hasObject(By.pkg("com.android.documentsui")), 10_000))
        device.pressBack()
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithText("Backup import canceled.").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Import Backup (Overwrite)").performScrollTo().performClick()
        composeRule.onNodeWithText("Overwrite local data?").assertIsDisplayed()
        composeRule.onNodeWithText("Choose Backup").performClick()
        assertTrue(
            "Overwrite confirmation did not launch document picker",
            device.wait(Until.hasObject(By.pkg("com.android.documentsui")), 10_000),
        )
        val backupDocument = device.wait(Until.findObject(By.text("overwrite-backup.json")), 10_000)
        assertTrue("Replacement Backup was not visible in Android Documents", backupDocument != null)
        backupDocument!!.click()
        composeRule.waitUntil(15_000) {
            composeRule
                .onAllNodesWithText("Backup overwrite completed successfully.", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithText("Currency: USD").fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithText("Account: Imported account").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Currency: USD").assertIsDisplayed()
        composeRule.onNodeWithText("Account: Imported account").assertIsDisplayed()
        composeRule.onNodeWithText("Accounts").performClick()
        composeRule.onNodeWithText("Imported account").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Delete all local data").performScrollTo().performClick()
        composeRule.onNodeWithText("Delete all local data?").assertIsDisplayed()
        composeRule.onNodeWithText("Delete all data").performClick()
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithText("Set up your first account").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun createOverwriteBackupDocument() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val values: ContentValues =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "overwrite-backup.json")
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download")
            }
        val destination = requireNotNull(context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        val backup: FinanceBackup =
            FinanceBackup(
                schemaVersion = 2,
                settings = BackupSettings("USD"),
                accounts = listOf(BackupAccount("imported-account", "Imported account", "BANK", 1L, 2L, 0L, 0L, false)),
                categories = emptyList(),
                tags = emptyList(),
                transactions = emptyList(),
                transfers = emptyList(),
                budgets = emptyList(),
            )
        context.contentResolver.openOutputStream(destination).use { output ->
            requireNotNull(output).write(FinanceBackupCodec.encode(backup).toByteArray())
        }
    }
}
