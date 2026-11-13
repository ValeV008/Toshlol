package com.toshlol.personalfinance

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.data.AccountEntity
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.AppSettings
import com.toshlol.personalfinance.data.BackupAccount
import com.toshlol.personalfinance.data.BackupCategory
import com.toshlol.personalfinance.data.BackupImportMode
import com.toshlol.personalfinance.data.BackupImportOutcome
import com.toshlol.personalfinance.data.BackupSettings
import com.toshlol.personalfinance.data.BackupTransaction
import com.toshlol.personalfinance.data.FinanceBackup
import com.toshlol.personalfinance.data.FinanceBackupCodec
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
            val database = createDatabase()
            try {
                val importer = RoomBackupImporter(database)
                val initial = createBackup()
                assertTrue(importer.import(FinanceBackupCodec.encode(initial).toByteArray()) is BackupImportOutcome.Success)
                val changed = initial.copy(accounts = listOf(initial.accounts.single().copy(name = "New name", updatedAt = 3L)))
                val merged = importer.import(FinanceBackupCodec.encode(changed).toByteArray())
                assertTrue(merged is BackupImportOutcome.Success)
                assertEquals(
                    "New name",
                    database
                        .accountDao()
                        .loadAccounts()
                        .single()
                        .name,
                )
                val repeated = importer.import(FinanceBackupCodec.encode(changed).toByteArray()) as BackupImportOutcome.Success
                assertEquals(
                    0,
                    repeated.report.counts
                        .getValue(com.toshlol.personalfinance.data.BackupEntityType.ACCOUNT)
                        .inserted,
                )
                assertEquals(
                    1,
                    repeated.report.counts
                        .getValue(com.toshlol.personalfinance.data.BackupEntityType.ACCOUNT)
                        .unchanged,
                )
                assertEquals(1, database.transactionDao().loadAll().size)
            } finally {
                database.close()
            }
        }

    @Test
    fun rejectsCurrencyTimestampAndFinalDatasetConflictsAtomically(): Unit =
        runTest {
            val database = createDatabase()
            try {
                database.settingsDao().insertSettings(AppSettings(currency = "EUR"))
                database.accountDao().insertAccount(AccountEntity("local", "Local", "CASH", 1L, 1L))
                val importer = RoomBackupImporter(database)
                val currencyMismatch = createBackup().copy(settings = BackupSettings("USD"))
                assertTrue(importer.import(FinanceBackupCodec.encode(currencyMismatch).toByteArray()) is BackupImportOutcome.Failure)
                val initial = createBackup()
                importer.import(FinanceBackupCodec.encode(initial).toByteArray())
                val divergent = initial.copy(accounts = listOf(initial.accounts.single().copy(name = "Conflict")))
                assertTrue(importer.import(FinanceBackupCodec.encode(divergent).toByteArray()) is BackupImportOutcome.Failure)
                val duplicateName = initial.copy(accounts = listOf(initial.accounts.single().copy(name = "Local", updatedAt = 3L)))
                assertTrue(importer.import(FinanceBackupCodec.encode(duplicateName).toByteArray()) is BackupImportOutcome.Failure)
                assertEquals(
                    "Cash",
                    database
                        .accountDao()
                        .loadAccounts()
                        .first { it.id == "account" }
                        .name,
                )
                assertTrue(database.accountDao().loadAccounts().any { it.id == "local" })
            } finally {
                database.close()
            }
        }

    @Test
    fun invalidReferencesAndMalformedInputLeaveDataUnchanged(): Unit =
        runTest {
            val database = createDatabase()
            try {
                database.settingsDao().insertSettings(AppSettings(currency = "EUR"))
                database.accountDao().insertAccount(AccountEntity("local", "Local", "CASH", 1L, 1L))
                val importer = RoomBackupImporter(database)
                val malformed = importer.import("{".toByteArray())
                assertTrue(malformed is BackupImportOutcome.Failure)
                val invalidTransaction = createBackup().transactions.single().copy(accountId = "missing")
                val invalid = createBackup().copy(transactions = listOf(invalidTransaction))
                assertTrue(importer.import(FinanceBackupCodec.encode(invalid).toByteArray()) is BackupImportOutcome.Failure)
                assertEquals("EUR", database.settingsDao().loadCurrency())
                assertEquals(listOf("local"), database.accountDao().loadAccounts().map(AccountEntity::id))
            } finally {
                database.close()
            }
        }

    @Test
    fun failedCommitRollsBackEveryEntityAndSetting(): Unit =
        runTest {
            val database = createDatabase()
            try {
                database.settingsDao().insertSettings(AppSettings(currency = "EUR"))
                database.accountDao().insertAccount(AccountEntity("local", "Local", "CASH", 1L, 1L))
                val importer = RoomBackupImporter(database) { error("Injected commit failure") }
                val result = importer.import(FinanceBackupCodec.encode(createBackup()).toByteArray(), BackupImportMode.OVERWRITE)
                assertTrue(result is BackupImportOutcome.Failure)
                assertEquals("EUR", database.settingsDao().loadCurrency())
                assertEquals(listOf("local"), database.accountDao().loadAccounts().map(AccountEntity::id))
            } finally {
                database.close()
            }
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
