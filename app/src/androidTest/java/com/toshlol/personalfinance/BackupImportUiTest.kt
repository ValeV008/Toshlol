package com.toshlol.personalfinance

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
import com.toshlol.personalfinance.data.BackupCategory
import com.toshlol.personalfinance.data.BackupRecurringBudget
import com.toshlol.personalfinance.data.BackupSettings
import com.toshlol.personalfinance.data.BackupTag
import com.toshlol.personalfinance.data.BackupTransaction
import com.toshlol.personalfinance.data.BackupTransfer
import com.toshlol.personalfinance.data.FinanceBackup
import com.toshlol.personalfinance.data.FinanceBackupCodec
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class BackupImportUiTest {
    private companion object {
        const val BACKUP_DOCUMENT_NAME_TIMESTAMP_MODULUS: Long = 100_000L
        const val DOCUMENT_PICKER_WAIT_TIMEOUT_MILLIS: Long = 10_000L
        const val DOCUMENT_PICKER_RESULT_WAIT_TIMEOUT_MILLIS: Long = 2_000L
        const val FINANCE_APPLICATION_PACKAGE_NAME: String = "com.toshlol.finance"
    }

    private val composeRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> = createAndroidComposeRule()
    private val ui: FinanceUiDriver = FinanceUiDriver(composeRule)
    private val backupDocumentName: String = "backup-${System.currentTimeMillis() % BACKUP_DOCUMENT_NAME_TIMESTAMP_MODULUS}.json"

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(ResetFinanceDataRule()).around(composeRule).around(UiFailureArtifactsRule(composeRule))

    @Test
    @Suppress("ktlint:standard:no-unit-return")
    fun settingsLaunchesDocumentPickerForMergeAndCancellationReturnsToSettings(): Unit {
        createCashAccount()
        openSettingsAndCapture()
        createOverwriteBackupDocument()
        val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        cancelMergeAndVerifyLocalProfile(device)
        cancelOverwriteAndVerifyLocalProfile(device)
        completeOverwriteFromDownloads(device)
        assertRestoredOverviewAndBudgetProgress()
        openSettings()
        assertRestoredAccountHistory()
        openSettings()
        confirmResetPreservingCancellation()
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun createCashAccount(): Unit {
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithText("Set up your first account").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Currency").performClick()
        composeRule.onNodeWithText("EUR").performClick()
        ui.enterText("Account name", "Cash")
        composeRule.onNodeWithText("Create account").performScrollTo().performClick()
        ui.showOverview()
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun openSettingsAndCapture(): Unit {
        composeRule.onNodeWithTag("open-navigation-drawer").performClick()
        composeRule.onNodeWithTag("drawer-Settings").performClick()
        composeRule.onNodeWithTag("settings-title").assertIsDisplayed()
        composeRule.onNodeWithText("Finance profile").assertIsDisplayed()
        composeRule.waitForIdle()
        saveTestScreenshot(TestScreenshotEvidence("backup-import-evidence", "settings.png"))
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun cancelMergeAndVerifyLocalProfile(device: UiDevice): Unit {
        composeRule.onNodeWithText("Import Backup (Merge)").performScrollTo().performClick()
        assertDocumentPickerOpen(device, "Android document picker did not open")
        device.pressBack()
        waitForBackupStatus("Backup import canceled.")
        composeRule.waitForIdle()
        saveTestScreenshot(TestScreenshotEvidence("backup-import-evidence", "settings-import-canceled.png"))
        assertLocalProfile("EUR", "Cash")
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun cancelOverwriteAndVerifyLocalProfile(device: UiDevice): Unit {
        composeRule.onNodeWithText("Import Backup (Overwrite)").performScrollTo().performClick()
        composeRule.onNodeWithText("Overwrite local data?").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        assertLocalProfile("EUR", "Cash")
        composeRule.onNodeWithText("Import Backup (Overwrite)").performScrollTo().performClick()
        composeRule.onNodeWithText("Overwrite local data?").assertIsDisplayed()
        composeRule.onNodeWithText("Choose Backup").performClick()
        assertDocumentPickerOpen(device, "Overwrite confirmation did not launch document picker")
        device.pressBack()
        waitForBackupStatus("Backup import canceled.")
        assertLocalProfile("EUR", "Cash")
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun completeOverwriteFromDownloads(device: UiDevice): Unit {
        composeRule.onNodeWithText("Import Backup (Overwrite)").performScrollTo().performClick()
        composeRule.onNodeWithText("Overwrite local data?").assertIsDisplayed()
        composeRule.onNodeWithText("Choose Backup").performClick()
        assertDocumentPickerOpen(device, "Confirmed Overwrite did not open the Android document picker")
        selectBackupFromDownloads(device)
        returnFromBackupPicker(device)
        waitForBackupStatus("Backup overwrite completed successfully.", substring = true)
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun selectBackupFromDownloads(device: UiDevice): Unit {
        val rootsButton: UiObject2? = device.wait(Until.findObject(By.desc("Show roots")), DOCUMENT_PICKER_WAIT_TIMEOUT_MILLIS)
        assertTrue("Android document picker did not show storage locations", rootsButton != null)
        rootsButton!!.click()
        val downloadsLocation: UiObject2? = device.wait(Until.findObject(By.text("Downloads")), DOCUMENT_PICKER_WAIT_TIMEOUT_MILLIS)
        assertTrue("Downloads was not available in Android Documents", downloadsLocation != null)
        downloadsLocation!!.click()
        device.pressBack()
        assertTrue(
            "Android document picker navigation drawer did not close",
            device.wait(Until.gone(By.text("Open from")), DOCUMENT_PICKER_WAIT_TIMEOUT_MILLIS),
        )
        val backupDocument: UiObject2? = device.wait(Until.findObject(By.text(backupDocumentName)), DOCUMENT_PICKER_WAIT_TIMEOUT_MILLIS)
        assertTrue("Replacement Backup was not visible in Android Documents", backupDocument != null)
        backupDocument!!.click()
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun assertRestoredOverviewAndBudgetProgress(): Unit {
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag("open-navigation-drawer").performClick()
        composeRule.onNodeWithTag("drawer-Overview").performClick()
        ui.showOverview()
        ui.selectOverviewAccount("Imported account")
        composeRule.onAllNodesWithText("USD 65.00").assertCountEquals(2)
        composeRule.onAllNodesWithText("USD 65.00").get(0).assertIsDisplayed()
        ui.showText("Every expense")
        ui.showText("Spent  USD 15.00")
        ui.showText("Food on weekends")
        ui.showText("Spent  USD 10.00")
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun openSettings(): Unit {
        composeRule.onNodeWithTag("open-navigation-drawer").performClick()
        composeRule.onNodeWithTag("drawer-Settings").performClick()
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithText("Currency: USD").fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithText("Account: Destination account").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun assertRestoredAccountHistory(): Unit {
        composeRule.onNodeWithTag("open-navigation-drawer").performClick()
        composeRule.onNodeWithTag("drawer-Accounts").performClick()
        composeRule.onNodeWithText("Imported account").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("65.00", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("View Imported account history").performScrollTo().performClick()
        composeRule.onNodeWithText("Balance  USD 65.00").assertIsDisplayed()
        composeRule.onNodeWithText("Expense · -USD 10.00").assertIsDisplayed()
        composeRule.onNodeWithText("-USD 25.00 · Transfer out").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("25.00", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Back").performScrollTo().performClick()
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithTag("open-navigation-drawer").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun confirmResetPreservingCancellation(): Unit {
        composeRule.onNodeWithText("Delete all local data").performScrollTo().performClick()
        composeRule.onNodeWithText("Delete all local data?").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        assertLocalProfile("USD", "Destination account")
        composeRule.onNodeWithText("Delete all local data").performScrollTo().performClick()
        composeRule.onNodeWithText("Delete all local data?").assertIsDisplayed()
        composeRule.onNodeWithText("Delete all data").performClick()
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithText("Set up your first account").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun assertLocalProfile(
        currency: String,
        accountName: String,
    ): Unit {
        composeRule.onNodeWithText("Currency: $currency").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Account: $accountName").performScrollTo().assertIsDisplayed()
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun assertDocumentPickerOpen(
        device: UiDevice,
        failureMessage: String,
    ): Unit {
        assertTrue(
            failureMessage,
            device.wait(Until.hasObject(By.pkg("com.android.documentsui")), DOCUMENT_PICKER_WAIT_TIMEOUT_MILLIS),
        )
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun waitForBackupStatus(
        message: String,
        substring: Boolean = false,
    ): Unit {
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithText(message, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    @Suppress("ktlint:standard:no-unit-return")
    fun exportDestinationLaunchesDocumentPickerAndShowsCancellationOnSettings(): Unit {
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithText("Set up your first account").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Currency").performClick()
        composeRule.onNodeWithText("EUR").performClick()
        ui.enterText("Account name", "Cash")
        composeRule.onNodeWithText("Create account").performScrollTo().performClick()
        ui.showOverview()
        val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        composeRule.onNodeWithTag("open-navigation-drawer").performClick()
        composeRule.onNodeWithTag("drawer-Export").performClick()
        assertTrue(
            "Android document picker did not open for the Export destination",
            device.wait(Until.hasObject(By.pkg("com.android.documentsui")), DOCUMENT_PICKER_WAIT_TIMEOUT_MILLIS),
        )
        device.pressBack()
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithTag("backup-status").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings-title").assertIsDisplayed()
        composeRule.onNodeWithText("Backup export canceled.").assertIsDisplayed()
        composeRule.onNodeWithText("Currency: EUR").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Account: Cash").performScrollTo().assertIsDisplayed()
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun createOverwriteBackupDocument(): Unit {
        val context: Context = ApplicationProvider.getApplicationContext()
        val values: ContentValues =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, backupDocumentName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download")
            }
        val destination: Uri = requireNotNull(context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        val backup: FinanceBackup =
            FinanceBackup(
                schemaVersion = 4,
                settings = BackupSettings("USD"),
                accounts =
                    listOf(
                        BackupAccount("imported-account", "Imported account", "BANK", 1L, 2L, 10_000L, 0L, false),
                        BackupAccount("destination-account", "Destination account", "CASH", 1L, 2L, -500L, 0L, false),
                    ),
                categories = listOf(BackupCategory("food", "Food", "EXPENSE", false, 3L, 4L)),
                tags = listOf(BackupTag("weekend", "Weekend", "food", false, 5L, 6L)),
                transactions =
                    listOf(
                        BackupTransaction(
                            "imported-expense",
                            "imported-account",
                            1_000L,
                            "EXPENSE",
                            YearMonth.now().atDay(2).toEpochDay(),
                            "food",
                            listOf("weekend"),
                            "Market",
                            7L,
                            8L,
                        ),
                        BackupTransaction(
                            "destination-expense",
                            "destination-account",
                            500L,
                            "EXPENSE",
                            YearMonth.now().atDay(3).toEpochDay(),
                            null,
                            emptyList(),
                            null,
                            9L,
                            10L,
                        ),
                    ),
                transfers =
                    listOf(
                        BackupTransfer(
                            "imported-transfer",
                            "imported-account",
                            "destination-account",
                            2_500L,
                            YearMonth.now().atDay(1).toEpochDay(),
                            1L,
                            2L,
                        ),
                    ),
                budgets = emptyList(),
                recurringBudgets =
                    listOf(
                        BackupRecurringBudget(
                            "unrestricted",
                            "Every expense",
                            2_000L,
                            listOf("imported-account", "destination-account"),
                            11L,
                            12L,
                        ),
                        BackupRecurringBudget(
                            "food-weekend",
                            "Food on weekends",
                            1_500L,
                            listOf("imported-account"),
                            13L,
                            14L,
                            categoryIds = listOf("food"),
                            tagIds = listOf("weekend"),
                        ),
                    ),
            )
        context.contentResolver.openOutputStream(destination).use { output ->
            requireNotNull(output).write(FinanceBackupCodec.encode(backup).toByteArray())
        }
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun returnFromBackupPicker(device: UiDevice): Unit {
        val returnedToApp: Boolean =
            device.wait(
                Until.hasObject(By.pkg(FINANCE_APPLICATION_PACKAGE_NAME)),
                DOCUMENT_PICKER_RESULT_WAIT_TIMEOUT_MILLIS,
            )
        if (!returnedToApp) {
            val openButton: UiObject2? =
                device.wait(
                    Until.findObject(By.text("Open")),
                    DOCUMENT_PICKER_WAIT_TIMEOUT_MILLIS,
                )
            if (openButton != null) {
                openButton.click()
            }
        }
        assertTrue(
            "Choosing the backup did not return to the app",
            device.wait(Until.hasObject(By.pkg(FINANCE_APPLICATION_PACKAGE_NAME)), DOCUMENT_PICKER_WAIT_TIMEOUT_MILLIS),
        )
    }
}
