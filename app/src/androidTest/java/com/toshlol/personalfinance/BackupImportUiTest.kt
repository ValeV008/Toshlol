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
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.toshlol.personalfinance.data.BackupAccount
import com.toshlol.personalfinance.data.BackupSettings
import com.toshlol.personalfinance.data.BackupTransfer
import com.toshlol.personalfinance.data.FinanceBackup
import com.toshlol.personalfinance.data.FinanceBackupCodec
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupImportUiTest {
    private companion object {
        const val BACKUP_DOCUMENT_NAME_TIMESTAMP_MODULUS: Long = 100_000L
        const val DOCUMENT_PICKER_WAIT_TIMEOUT_MILLIS: Long = 10_000L
    }

    private val composeRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> = createAndroidComposeRule()
    private val ui: FinanceUiDriver = FinanceUiDriver(composeRule)
    private val backupDocumentName: String = "backup-${System.currentTimeMillis() % BACKUP_DOCUMENT_NAME_TIMESTAMP_MODULUS}.json"

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
        composeRule.onNodeWithText("Create account").performScrollTo().performClick()
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithText("EUR ledger is ready.").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Settings").performClick()
        createOverwriteBackupDocument()
        val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        composeRule.onNodeWithText("Import Backup (Merge)").performScrollTo().performClick()
        assertTrue(
            "Android document picker did not open",
            device.wait(Until.hasObject(By.pkg("com.android.documentsui")), DOCUMENT_PICKER_WAIT_TIMEOUT_MILLIS),
        )
        device.pressBack()
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithText("Backup import canceled.").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Import Backup (Overwrite)").performScrollTo().performClick()
        composeRule.onNodeWithText("Overwrite local data?").assertIsDisplayed()
        composeRule.onNodeWithText("Choose Backup").performClick()
        assertTrue(
            "Overwrite confirmation did not launch document picker",
            device.wait(Until.hasObject(By.pkg("com.android.documentsui")), DOCUMENT_PICKER_WAIT_TIMEOUT_MILLIS),
        )
        val rootsButton: UiObject2? = device.wait(Until.findObject(By.desc("Show roots")), DOCUMENT_PICKER_WAIT_TIMEOUT_MILLIS)
        assertTrue("Android document picker did not show storage locations", rootsButton != null)
        rootsButton!!.click()
        val downloadsLocation: UiObject2? = device.wait(Until.findObject(By.text("Downloads")), DOCUMENT_PICKER_WAIT_TIMEOUT_MILLIS)
        assertTrue("Downloads was not available in Android Documents", downloadsLocation != null)
        downloadsLocation!!.click()
        device.pressBack()
        val backupDocument: UiObject2? = device.wait(Until.findObject(By.text(backupDocumentName)), DOCUMENT_PICKER_WAIT_TIMEOUT_MILLIS)
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
                composeRule.onAllNodesWithText("Account: Destination account").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Currency: USD").assertIsDisplayed()
        composeRule.onNodeWithText("Account: Destination account").assertIsDisplayed()
        composeRule.onNodeWithText("Accounts").performClick()
        composeRule.onNodeWithText("Imported account").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("75.00", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("View Imported account history").performScrollTo().performClick()
        composeRule.onNodeWithText("25.00", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Back to overview").performClick()
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
                put(MediaStore.MediaColumns.DISPLAY_NAME, backupDocumentName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download")
            }
        val destination = requireNotNull(context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        val backup: FinanceBackup =
            FinanceBackup(
                schemaVersion = 2,
                settings = BackupSettings("USD"),
                accounts =
                    listOf(
                        BackupAccount("imported-account", "Imported account", "BANK", 1L, 2L, 10_000L, 0L, false),
                        BackupAccount("destination-account", "Destination account", "CASH", 1L, 2L, -500L, 0L, false),
                    ),
                categories = emptyList(),
                tags = emptyList(),
                transactions = emptyList(),
                transfers = listOf(BackupTransfer("imported-transfer", "imported-account", "destination-account", 2_500L, 1L, 1L, 2L)),
                budgets = emptyList(),
            )
        context.contentResolver.openOutputStream(destination).use { output ->
            requireNotNull(output).write(FinanceBackupCodec.encode(backup).toByteArray())
        }
    }
}
