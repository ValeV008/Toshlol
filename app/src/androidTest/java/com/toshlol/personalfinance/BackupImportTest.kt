package com.toshlol.personalfinance

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.BackupAccount
import com.toshlol.personalfinance.data.BackupCategory
import com.toshlol.personalfinance.data.BackupDocumentWriter
import com.toshlol.personalfinance.data.BackupEntityType
import com.toshlol.personalfinance.data.BackupImportMode
import com.toshlol.personalfinance.data.BackupImportOutcome
import com.toshlol.personalfinance.data.BackupSettings
import com.toshlol.personalfinance.data.BackupTransaction
import com.toshlol.personalfinance.data.FinanceBackup
import com.toshlol.personalfinance.data.FinanceBackupCodec
import com.toshlol.personalfinance.data.RoomBackupExporter
import com.toshlol.personalfinance.data.RoomBackupImporter
import com.toshlol.personalfinance.data.TransactionDirection
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupImportTest {
    @Test
    fun mergeIsIdempotentAndAppliesNewerWholeRecords(): Unit =
        runTest {
            val database: AppDatabase = createDatabase()
            try {
                val importer: RoomBackupImporter = RoomBackupImporter(database)
                val initial: FinanceBackup = createBackup()
                assertTrue(importBackup(importer, initial) is BackupImportOutcome.Success)
                val changed: FinanceBackup =
                    initial.copy(
                        accounts = listOf(initial.accounts.single().copy(name = "New name", updatedAt = 3L)),
                    )
                val updateOutcome: BackupImportOutcome.Success = importBackup(importer, changed) as BackupImportOutcome.Success
                assertEquals(
                    1,
                    updateOutcome.report.counts
                        .getValue(BackupEntityType.ACCOUNT)
                        .updated,
                )
                assertEquals("New name", captureBackup(database).accounts.single().name)
                val repeatedOutcome: BackupImportOutcome.Success = importBackup(importer, changed) as BackupImportOutcome.Success
                assertEquals(
                    1,
                    repeatedOutcome.report.counts
                        .getValue(BackupEntityType.ACCOUNT)
                        .unchanged,
                )
                val localOnlyAccount: BackupAccount = BackupAccount("local", "Local", "CASH", 1L, 1L, 0L, 0L, false)
                importBackup(importer, initial.copy(accounts = initial.accounts + localOnlyAccount))
                val localRecordOutcome: BackupImportOutcome.Success = importBackup(importer, changed) as BackupImportOutcome.Success
                assertEquals(
                    1,
                    localRecordOutcome.report.counts
                        .getValue(BackupEntityType.ACCOUNT)
                        .unchanged,
                )
                assertEquals(2, captureBackup(database).accounts.size)
                assertEquals(1, captureBackup(database).transactions.size)
            } finally {
                database.close()
            }
        }

    @Test
    fun rejectsCurrencyTimestampAndFinalDatasetConflictsAtomically(): Unit =
        runTest {
            val database: AppDatabase = createDatabase()
            try {
                val importer: RoomBackupImporter = RoomBackupImporter(database)
                val initial: FinanceBackup = createBackup()
                importBackup(importer, initial)
                val currencyMismatch: FinanceBackup = initial.copy(settings = BackupSettings("USD"))
                assertTrue(importBackup(importer, currencyMismatch) is BackupImportOutcome.Failure)
                val divergent: FinanceBackup = initial.copy(accounts = listOf(initial.accounts.single().copy(name = "Conflict")))
                assertTrue(importBackup(importer, divergent) is BackupImportOutcome.Failure)
                val withLocalAccount: FinanceBackup =
                    initial.copy(
                        accounts = initial.accounts + BackupAccount("local", "Local", "CASH", 1L, 1L, 0L, 0L, false),
                    )
                importBackup(importer, withLocalAccount)
                val duplicateName: FinanceBackup =
                    initial.copy(
                        accounts = listOf(initial.accounts.single().copy(name = "Local", updatedAt = 3L)),
                    )
                assertTrue(importBackup(importer, duplicateName) is BackupImportOutcome.Failure)
                assertEquals(withLocalAccount, captureBackup(database))
            } finally {
                database.close()
            }
        }

    @Test
    fun invalidReferencesAndMalformedInputLeaveDataUnchanged(): Unit =
        runTest {
            val database: AppDatabase = createDatabase()
            try {
                val importer: RoomBackupImporter = RoomBackupImporter(database)
                val initial: FinanceBackup = createBackup()
                importBackup(importer, initial)
                assertTrue(importer.import("{".toByteArray()) is BackupImportOutcome.Failure)
                val invalidTransaction: BackupTransaction = initial.transactions.single().copy(accountId = "missing")
                val invalid: FinanceBackup = initial.copy(transactions = listOf(invalidTransaction))
                assertTrue(importBackup(importer, invalid) is BackupImportOutcome.Failure)
                val invalidDate: FinanceBackup =
                    initial.copy(
                        accounts = listOf(initial.accounts.single().copy(openingBalanceDate = Long.MAX_VALUE)),
                        transactions = listOf(initial.transactions.single().copy(date = Long.MAX_VALUE)),
                    )
                assertTrue(importBackup(importer, invalidDate) is BackupImportOutcome.Failure)
                assertEquals(initial, captureBackup(database))
            } finally {
                database.close()
            }
        }

    @Test
    fun failedCommitRollsBackEveryEntityAndSetting(): Unit =
        runTest {
            val database: AppDatabase = createDatabase()
            try {
                val initial: FinanceBackup = createBackup()
                val seeder: RoomBackupImporter = RoomBackupImporter(database)
                importBackup(seeder, initial)
                val importer: RoomBackupImporter = RoomBackupImporter(database) { error("Injected commit failure") }
                val replacement: FinanceBackup = initial.copy(settings = BackupSettings("USD"))
                val result: BackupImportOutcome = importBackup(importer, replacement, BackupImportMode.OVERWRITE)
                assertTrue(result is BackupImportOutcome.Failure)
                assertEquals(initial, captureBackup(database))
            } finally {
                database.close()
            }
        }

    private suspend fun importBackup(
        importer: RoomBackupImporter,
        backup: FinanceBackup,
        mode: BackupImportMode = BackupImportMode.MERGE,
    ): BackupImportOutcome = importer.import(FinanceBackupCodec.encode(backup).toByteArray(), mode)

    private suspend fun captureBackup(database: AppDatabase): FinanceBackup {
        var encodedBackup: String = ""
        val writer: BackupDocumentWriter = BackupDocumentWriter { _, contents -> encodedBackup = contents }
        RoomBackupExporter(database, writer).exportTo(Uri.parse("content://backup-provider/snapshot.json"))
        return FinanceBackupCodec.decode(encodedBackup)
    }

    private fun createDatabase(): AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

    private fun createBackup(): FinanceBackup =
        FinanceBackup(
            schemaVersion = 2,
            settings = BackupSettings("EUR"),
            accounts = listOf(BackupAccount("account", "Cash", "CASH", 1L, 2L, 0L, 0L, false)),
            categories = listOf(BackupCategory("category", "Food", TransactionDirection.EXPENSE.name, false, 1L, 2L)),
            tags = emptyList(),
            transactions =
                listOf(
                    BackupTransaction(
                        "transaction",
                        "account",
                        100L,
                        TransactionDirection.EXPENSE.name,
                        1L,
                        "category",
                        emptyList(),
                        null,
                        1L,
                        2L,
                    ),
                ),
            transfers = emptyList(),
            budgets = emptyList(),
        )

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()
}
