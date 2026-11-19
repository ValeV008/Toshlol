package com.toshlol.personalfinance

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.BackupAccount
import com.toshlol.personalfinance.data.BackupBudget
import com.toshlol.personalfinance.data.BackupCategory
import com.toshlol.personalfinance.data.BackupDocumentWriter
import com.toshlol.personalfinance.data.BackupImportMode
import com.toshlol.personalfinance.data.BackupImportOutcome
import com.toshlol.personalfinance.data.BackupRecurringBudget
import com.toshlol.personalfinance.data.BackupSettings
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
class BudgetBackupCompatibilityTest {
    @Test
    fun restoresVersionThreeBudgetsWithoutOptionalCategoryOrTagRestrictions() {
        val versionThreeBackup: String =
            """{"schemaVersion":3,"settings":{"currency":"EUR"},"accounts":[{"id":"cash","name":"Cash","type":"CASH","createdAt":1,"updatedAt":2,"openingBalanceMinor":0,"openingBalanceDate":0,"archived":false}],"categories":[{"id":"food","name":"Food","direction":"EXPENSE","archived":false,"createdAt":5,"updatedAt":6}],"tags":[],"transactions":[],"transfers":[],"budgets":[],"recurringBudgets":[{"id":"household","name":"Household","limitMinor":1000,"accountIds":["cash"],"createdAt":7,"updatedAt":8}]}"""
        val actual: FinanceBackup = FinanceBackupCodec.decode(versionThreeBackup)
        val budget: BackupRecurringBudget = actual.recurringBudgets.single()
        assertEquals(4, actual.schemaVersion)
        assertEquals(emptyList<String>(), budget.categoryIds)
        assertEquals(emptyList<String>(), budget.tagIds)
    }

    @Test
    fun restoresVersionTwoBudgetsWithTheirOriginalAllAccountScope() {
        val versionTwoBackup: String =
            """{"schemaVersion":2,"settings":{"currency":"EUR"},"accounts":[{"id":"cash","name":"Cash","type":"CASH","createdAt":1,"updatedAt":2,"openingBalanceMinor":0,"openingBalanceDate":0,"archived":false},{"id":"savings","name":"Savings","type":"BANK","createdAt":3,"updatedAt":4,"openingBalanceMinor":0,"openingBalanceDate":0,"archived":false}],"categories":[{"id":"food","name":"Food","direction":"EXPENSE","archived":false,"createdAt":5,"updatedAt":6}],"tags":[],"transactions":[],"transfers":[],"budgets":[{"id":"old-budget","categoryId":"food","month":"2025-12","amountMinor":9000,"createdAt":7,"updatedAt":8}]}"""
        val actualBackup: FinanceBackup = FinanceBackupCodec.decode(versionTwoBackup)
        assertEquals(4, actualBackup.schemaVersion)
        assertEquals(
            listOf(BackupBudget("old-budget", "food", "2025-12", 9000L, 7L, 8L, listOf("cash", "savings"))),
            actualBackup.budgets,
        )
        assertTrue(
            actualBackup.budgets
                .single()
                .accountIds
                .isNotEmpty(),
        )
    }

    @Test
    fun rejectIncomeCategoryAsExpenseBudgetFilterBeforeMutation(): Unit =
        runTest {
            val database: AppDatabase =
                Room
                    .inMemoryDatabaseBuilder(
                        ApplicationProvider.getApplicationContext<Context>(),
                        AppDatabase::class.java,
                    ).build()
            try {
                val baselineBackup: FinanceBackup =
                    FinanceBackup(
                        schemaVersion = 4,
                        settings = BackupSettings("EUR"),
                        accounts = listOf(BackupAccount("account", "Cash", "CASH", 1L, 2L, 0L, 0L, false)),
                        categories = emptyList(),
                        tags = emptyList(),
                        transactions = emptyList(),
                        transfers = emptyList(),
                        budgets = emptyList(),
                    )
                val importer: RoomBackupImporter = RoomBackupImporter(database)
                assertTrue(importer.import(FinanceBackupCodec.encode(baselineBackup).toByteArray()) is BackupImportOutcome.Success)
                val invalidBackup: FinanceBackup =
                    FinanceBackup(
                        schemaVersion = 4,
                        settings = BackupSettings("EUR"),
                        accounts = listOf(BackupAccount("account", "Cash", "CASH", 1L, 2L, 0L, 0L, false)),
                        categories =
                            listOf(BackupCategory("income", "Salary", TransactionDirection.INCOME.name, false, 3L, 4L)),
                        tags = emptyList(),
                        transactions = emptyList(),
                        transfers = emptyList(),
                        budgets = emptyList(),
                        recurringBudgets =
                            listOf(
                                BackupRecurringBudget(
                                    "budget",
                                    "Income filter",
                                    500L,
                                    listOf("account"),
                                    5L,
                                    6L,
                                    categoryIds = listOf("income"),
                                ),
                            ),
                    )
                val before: FinanceBackup = exportBackup(database)
                val result: BackupImportOutcome =
                    importer.import(
                        FinanceBackupCodec.encode(invalidBackup).toByteArray(),
                        BackupImportMode.OVERWRITE,
                    )
                assertTrue(result is BackupImportOutcome.Failure)
                assertEquals(before, exportBackup(database))
            } finally {
                database.close()
            }
        }

    private suspend fun exportBackup(database: AppDatabase): FinanceBackup {
        var encodedBackup: String = ""
        val writer: BackupDocumentWriter = BackupDocumentWriter { _, contents -> encodedBackup = contents }
        RoomBackupExporter(database, writer).exportTo(Uri.parse("content://backup-provider/snapshot.json"))
        return FinanceBackupCodec.decode(encodedBackup)
    }
}
