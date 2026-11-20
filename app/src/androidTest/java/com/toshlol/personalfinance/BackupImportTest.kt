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
import com.toshlol.personalfinance.data.BackupEntityType
import com.toshlol.personalfinance.data.BackupImportMode
import com.toshlol.personalfinance.data.BackupImportOutcome
import com.toshlol.personalfinance.data.BackupRecurringBudget
import com.toshlol.personalfinance.data.BackupSettings
import com.toshlol.personalfinance.data.BackupTag
import com.toshlol.personalfinance.data.BackupTransaction
import com.toshlol.personalfinance.data.BackupTransfer
import com.toshlol.personalfinance.data.FinanceBackup
import com.toshlol.personalfinance.data.FinanceBackupCodec
import com.toshlol.personalfinance.data.FinanceTransaction
import com.toshlol.personalfinance.data.FinanceTransfer
import com.toshlol.personalfinance.data.FirstRunSnapshot
import com.toshlol.personalfinance.data.MonthlyOverview
import com.toshlol.personalfinance.data.RecurringBudgetProgress
import com.toshlol.personalfinance.data.RoomAccountRepository
import com.toshlol.personalfinance.data.RoomBackupExporter
import com.toshlol.personalfinance.data.RoomBackupImporter
import com.toshlol.personalfinance.data.RoomLocalDataResetter
import com.toshlol.personalfinance.data.RoomRecurringBudgetRepository
import com.toshlol.personalfinance.data.RoomTransactionRepository
import com.toshlol.personalfinance.data.RoomTransferRepository
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.data.projectMonthlyOverview
import com.toshlol.personalfinance.domain.FirstRunSetup
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class BackupImportTest {
    @Test
    fun reconcileBudgetAsCompleteBackupRecordWithoutStaleAccountUnion(): Unit =
        runTest {
            val database: AppDatabase = createDatabase()
            try {
                val importer: RoomBackupImporter = RoomBackupImporter(database)
                val initial: FinanceBackup = createBackupWithRecurringBudget()
                val originalBudget: BackupRecurringBudget = initial.recurringBudgets.single()
                val scenario: BudgetMergeScenario = BudgetMergeScenario(importer, initial, originalBudget, database)
                assertTrue(importBackup(importer, initial) is BackupImportOutcome.Success)
                assertEquals(initial, captureBackup(database))
                assertReorderedBudgetScopeIsUnchanged(scenario)
                val updatedBackup: FinanceBackup = replaceBudgetScopeAndAssertMerge(scenario)
                assertBudgetConflictsAreRejected(importer, updatedBackup)
                assertEquals(updatedBackup, captureBackup(database))
            } finally {
                database.close()
            }
        }

    private fun createBackupWithRecurringBudget(): FinanceBackup {
        val secondAccount: BackupAccount = BackupAccount("savings", "Savings", "BANK", 1L, 2L, 0L, 0L, false)
        val budget: BackupRecurringBudget =
            BackupRecurringBudget(
                "budget",
                "Household",
                500L,
                listOf("account", "savings"),
                1L,
                2L,
                categoryIds = listOf("category", "dining"),
                tagIds = listOf("holiday", "weekend"),
            )
        val base: FinanceBackup = createBackup()
        return base.copy(
            accounts = base.accounts + secondAccount,
            tags =
                listOf(
                    BackupTag("holiday", "Holiday", null, false, 5L, 6L),
                    BackupTag("weekend", "Weekend", null, false, 3L, 4L),
                ),
            categories =
                listOf(BackupCategory("dining", "Dining", TransactionDirection.EXPENSE.name, false, 7L, 8L)) + base.categories,
            recurringBudgets = listOf(budget),
        )
    }

    private data class BudgetMergeScenario(
        val importer: RoomBackupImporter,
        val initial: FinanceBackup,
        val originalBudget: BackupRecurringBudget,
        val database: AppDatabase,
    )

    private suspend fun replaceBudgetScopeAndAssertMerge(scenario: BudgetMergeScenario): FinanceBackup {
        val updatedBudget: BackupRecurringBudget =
            scenario.originalBudget.copy(
                limitMinor = 700L,
                accountIds = listOf("savings"),
                categoryIds = emptyList(),
                tagIds = listOf("holiday"),
                updatedAt = 3L,
            )
        val updatedBackup: FinanceBackup = scenario.initial.copy(recurringBudgets = listOf(updatedBudget))
        val outcome: BackupImportOutcome.Success = importBackup(scenario.importer, updatedBackup) as BackupImportOutcome.Success
        val updated: Int =
            outcome.report.counts
                .getValue(BackupEntityType.BUDGET)
                .updated
        assertEquals(1, updated)
        val actualBudget: BackupRecurringBudget = captureBackup(scenario.database).recurringBudgets.single()
        assertEquals(listOf("savings"), actualBudget.accountIds)
        assertEquals(emptyList<String>(), actualBudget.categoryIds)
        assertEquals(listOf("holiday"), actualBudget.tagIds)
        assertBudgetImportIsIdempotent(scenario.importer, updatedBackup)
        return updatedBackup
    }

    @Suppress("ktlint:standard:no-unit-return")
    private suspend fun assertBudgetImportIsIdempotent(
        importer: RoomBackupImporter,
        backup: FinanceBackup,
    ): Unit {
        val outcome: BackupImportOutcome.Success = importBackup(importer, backup) as BackupImportOutcome.Success
        val unchanged: Int =
            outcome.report.counts
                .getValue(BackupEntityType.BUDGET)
                .unchanged
        assertEquals(1, unchanged)
    }

    @Suppress("ktlint:standard:no-unit-return")
    private suspend fun assertBudgetConflictsAreRejected(
        importer: RoomBackupImporter,
        updatedBackup: FinanceBackup,
    ): Unit {
        val updatedBudget: BackupRecurringBudget = updatedBackup.recurringBudgets.single()
        val divergentBudget: BackupRecurringBudget = updatedBudget.copy(name = "Different name")
        val divergentBackup: FinanceBackup = updatedBackup.copy(recurringBudgets = listOf(divergentBudget))
        assertTrue(importBackup(importer, divergentBackup) is BackupImportOutcome.Failure)
        val invalidBudget: BackupRecurringBudget = updatedBudget.copy(accountIds = listOf("missing"), updatedAt = 4L)
        val invalidBackup: FinanceBackup = updatedBackup.copy(recurringBudgets = listOf(invalidBudget))
        assertTrue(importBackup(importer, invalidBackup) is BackupImportOutcome.Failure)
        val missingCategoryBudget: BackupRecurringBudget = updatedBudget.copy(categoryIds = listOf("missing"), updatedAt = 4L)
        val missingCategoryBackup: FinanceBackup = updatedBackup.copy(recurringBudgets = listOf(missingCategoryBudget))
        assertTrue(importBackup(importer, missingCategoryBackup) is BackupImportOutcome.Failure)
        val missingTagBudget: BackupRecurringBudget = updatedBudget.copy(tagIds = listOf("missing"), updatedAt = 4L)
        val missingTagBackup: FinanceBackup = updatedBackup.copy(recurringBudgets = listOf(missingTagBudget))
        assertTrue(importBackup(importer, missingTagBackup) is BackupImportOutcome.Failure)
        val categoryConflict: BackupRecurringBudget = updatedBudget.copy(categoryIds = listOf("dining"))
        assertTrue(importBackup(importer, updatedBackup.copy(recurringBudgets = listOf(categoryConflict))) is BackupImportOutcome.Failure)
        val tagConflict: BackupRecurringBudget = updatedBudget.copy(tagIds = listOf("weekend"))
        assertTrue(importBackup(importer, updatedBackup.copy(recurringBudgets = listOf(tagConflict))) is BackupImportOutcome.Failure)
    }

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
    fun importerNormalizesV1AndRejectsUnsupportedSchemaWithoutMutation(): Unit =
        runTest {
            val database: AppDatabase = createDatabase()
            try {
                val importer: RoomBackupImporter = RoomBackupImporter(database)
                val legacyContents: String = createV1BackupJson()
                val legacyResult: BackupImportOutcome = importer.import(legacyContents.toByteArray())
                assertTrue(legacyResult is BackupImportOutcome.Success)
                val expected: FinanceBackup =
                    createBackup().copy(
                        accounts = listOf(createBackup().accounts.single().copy(openingBalanceDate = 1L)),
                    )
                assertEquals(expected, captureBackup(database))
                val unsupportedContents: ByteArray =
                    legacyContents.replace("\"schemaVersion\":1", "\"schemaVersion\":99").toByteArray()
                assertTrue(importer.import(unsupportedContents) is BackupImportOutcome.Failure)
                assertEquals(expected, captureBackup(database))
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
                val importer: RoomBackupImporter = RoomBackupImporter(database, beforeCommit = { error("Injected commit failure") })
                val replacement: FinanceBackup = initial.copy(settings = BackupSettings("USD"))
                val result: BackupImportOutcome = importBackup(importer, replacement, BackupImportMode.OVERWRITE)
                assertTrue(result is BackupImportOutcome.Failure)
                assertEquals(initial, captureBackup(database))
            } finally {
                database.close()
            }
        }

    @Test
    fun overwriteReplacesCompleteDatasetAndSettingsAndReportsInsertedCounts(): Unit =
        runTest {
            val database: AppDatabase = createDatabase()
            try {
                val importer: RoomBackupImporter = RoomBackupImporter(database)
                val previous: FinanceBackup = createBackup()
                importBackup(importer, previous)
                val replacement: FinanceBackup =
                    createBackup().copy(
                        settings = BackupSettings("USD"),
                        accounts = listOf(BackupAccount("replacement", "New account", "BANK", 4L, 5L, 100L, 3L, false)),
                        categories = emptyList(),
                        transactions = emptyList(),
                    )
                val outcome: BackupImportOutcome.Success =
                    importBackup(importer, replacement, BackupImportMode.OVERWRITE) as BackupImportOutcome.Success
                assertEquals(replacement, captureBackup(database))
                assertEquals(
                    1,
                    outcome.report.counts
                        .getValue(BackupEntityType.ACCOUNT)
                        .inserted,
                )
                assertEquals(
                    0,
                    outcome.report.counts
                        .getValue(BackupEntityType.TRANSACTION)
                        .inserted,
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun exportAndRestoreCompleteCustomBudgetDataset(): Unit =
        runTest {
            val databaseName: String = "backup-round-trip-${System.currentTimeMillis()}.db"
            context.deleteDatabase(databaseName)
            val sourceDatabase: AppDatabase = createDatabase()
            var restoredDatabase: AppDatabase? = null
            try {
                val sourceBackup: FinanceBackup = createCompleteRoundTripBackup()
                assertTrue(importBackup(RoomBackupImporter(sourceDatabase), sourceBackup) is BackupImportOutcome.Success)
                val exportedBackup: FinanceBackup = captureBackup(sourceDatabase)
                assertSameBackupRecordsIgnoringOrder(sourceBackup, exportedBackup)

                restoredDatabase =
                    Room
                        .databaseBuilder(context, AppDatabase::class.java, databaseName)
                        .build()
                assertTrue(
                    importBackup(
                        RoomBackupImporter(restoredDatabase),
                        exportedBackup,
                        BackupImportMode.OVERWRITE,
                    ) is BackupImportOutcome.Success,
                )
                assertEquals(exportedBackup, captureBackup(restoredDatabase))
                restoredDatabase.close()

                restoredDatabase =
                    Room
                        .databaseBuilder(context, AppDatabase::class.java, databaseName)
                        .build()
                assertRestoredRoundTripReads(restoredDatabase, exportedBackup)
            } finally {
                sourceDatabase.close()
                restoredDatabase?.close()
                context.deleteDatabase(databaseName)
            }
        }

    @Suppress("ktlint:standard:no-unit-return")
    private fun assertSameBackupRecordsIgnoringOrder(
        expected: FinanceBackup,
        actual: FinanceBackup,
    ): Unit {
        assertEquals(expected.schemaVersion, actual.schemaVersion)
        assertEquals(expected.settings, actual.settings)
        assertEquals(expected.accounts.toSet(), actual.accounts.toSet())
        assertEquals(expected.categories.toSet(), actual.categories.toSet())
        assertEquals(expected.tags.toSet(), actual.tags.toSet())
        assertEquals(expected.transactions.toSet(), actual.transactions.toSet())
        assertEquals(expected.transfers.toSet(), actual.transfers.toSet())
        assertEquals(expected.budgets.toSet(), actual.budgets.toSet())
        assertEquals(expected.recurringBudgets.toSet(), actual.recurringBudgets.toSet())
    }

    @Suppress("ktlint:standard:no-unit-return")
    private suspend fun assertRestoredRoundTripReads(
        database: AppDatabase,
        expectedBackup: FinanceBackup,
    ): Unit {
        assertEquals(expectedBackup, captureBackup(database))
        val setup: FirstRunSnapshot = RoomAccountRepository(database).loadSetup()
        val transactions: List<FinanceTransaction> = RoomTransactionRepository(database).loadTransactions()
        val transfers: List<FinanceTransfer> = RoomTransferRepository(database).loadTransfers()
        val month: YearMonth = YearMonth.of(2026, 11)
        assertRestoredLedgerProjection(setup, transactions, transfers, month)
        assertRestoredBudgetReads(database, month)
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun assertRestoredLedgerProjection(
        setup: FirstRunSnapshot,
        transactions: List<FinanceTransaction>,
        transfers: List<FinanceTransfer>,
        month: YearMonth,
    ): Unit {
        val overview: MonthlyOverview = projectMonthlyOverview(month, setup.accounts, transactions, transfers, setOf("cash", "savings"))
        assertEquals("EUR", setup.currency)
        assertEquals(mapOf("cash" to 4_650L, "savings" to 9_850L), overview.closingBalancesMinor)
        assertEquals(14_500L, overview.closingBalanceMinor)
        assertEquals(500L, overview.incomeMinor)
        assertEquals(1_000L, overview.expenseMinor)
        assertEquals(4, transactions.count { it.accountId == "cash" })
        assertEquals(1, transfers.count { it.sourceAccountId == "cash" || it.destinationAccountId == "cash" })
    }

    @Suppress("ktlint:standard:no-unit-return")
    private suspend fun assertRestoredBudgetReads(
        database: AppDatabase,
        month: YearMonth,
    ): Unit {
        val progress: Map<String, RecurringBudgetProgress> = RoomRecurringBudgetRepository(database).load(month).associateBy { it.id }
        assertEquals(1_000L, progress.getValue("unrestricted").totals.spentMinor)
        assertEquals(400L, progress.getValue("food-only").totals.spentMinor)
        assertEquals(100L, progress.getValue("food-weekend").totals.spentMinor)
        assertEquals(emptySet<String>(), progress.getValue("unrestricted").restrictions.categoryIds)
        assertEquals(setOf("food"), progress.getValue("food-only").restrictions.categoryIds)
        assertEquals(setOf("food"), progress.getValue("food-weekend").restrictions.categoryIds)
        assertEquals(setOf("weekend"), progress.getValue("food-weekend").restrictions.tagIds)
    }

    private fun createCompleteRoundTripBackup(): FinanceBackup {
        val openingDate: Long = LocalDate.of(2026, 11, 1).toEpochDay()
        return FinanceBackup(
            schemaVersion = 4,
            settings = BackupSettings("EUR"),
            accounts =
                listOf(
                    BackupAccount("cash", "Cash", "CASH", 1L, 2L, 5_000L, openingDate, false),
                    BackupAccount("savings", "Savings", "BANK", 3L, 4L, 10_000L, openingDate, false),
                ),
            categories =
                listOf(
                    BackupCategory("food", "Food", TransactionDirection.EXPENSE.name, false, 5L, 6L),
                    BackupCategory("dining", "Dining", TransactionDirection.EXPENSE.name, false, 7L, 8L),
                ),
            tags =
                listOf(
                    BackupTag("weekend", "Weekend", null, false, 9L, 10L),
                    BackupTag("market", "Market", "food", false, 11L, 12L),
                ),
            transactions =
                listOf(
                    createRoundTripTransaction(
                        RoundTripTransactionInput("cash-food-weekend", "cash", 100L, "EXPENSE", 2, "food", listOf("weekend")),
                    ),
                    createRoundTripTransaction(
                        RoundTripTransactionInput("cash-dining-weekend", "cash", 200L, "EXPENSE", 3, "dining", listOf("weekend")),
                    ),
                    createRoundTripTransaction(
                        RoundTripTransactionInput("cash-food-market", "cash", 300L, "EXPENSE", 4, "food", listOf("market")),
                    ),
                    createRoundTripTransaction(
                        RoundTripTransactionInput("savings-food-weekend", "savings", 400L, "EXPENSE", 5, "food", listOf("weekend")),
                    ),
                    createRoundTripTransaction(RoundTripTransactionInput("cash-income", "cash", 500L, "INCOME", 6, null, emptyList())),
                ),
            transfers =
                listOf(
                    BackupTransfer("cash-to-savings", "cash", "savings", 250L, LocalDate.of(2026, 11, 7).toEpochDay(), 13L, 14L),
                ),
            budgets =
                listOf(
                    BackupBudget("monthly-food", "food", "2026-11", 2_000L, 15L, 16L, listOf("cash", "savings")),
                ),
            recurringBudgets =
                listOf(
                    BackupRecurringBudget("unrestricted", "Unrestricted", 2_000L, listOf("cash", "savings"), 17L, 18L),
                    BackupRecurringBudget("food-only", "Food only", 1_000L, listOf("cash"), 19L, 20L, categoryIds = listOf("food")),
                    BackupRecurringBudget(
                        "food-weekend",
                        "Food on weekends",
                        500L,
                        listOf("cash"),
                        21L,
                        22L,
                        categoryIds = listOf("food"),
                        tagIds = listOf("weekend"),
                    ),
                ),
        )
    }

    private data class RoundTripTransactionInput(
        val id: String,
        val accountId: String,
        val amountMinor: Long,
        val direction: String,
        val dayOfMonth: Int,
        val categoryId: String?,
        val tagIds: List<String>,
    )

    private fun createRoundTripTransaction(input: RoundTripTransactionInput): BackupTransaction =
        BackupTransaction(
            input.id,
            input.accountId,
            input.amountMinor,
            input.direction,
            LocalDate.of(2026, 11, input.dayOfMonth).toEpochDay(),
            input.categoryId,
            input.tagIds,
            null,
            1L,
            2L,
        )

    @Test
    fun localResetClearsBackupDataAndAllowsFirstRunSetupAgain(): Unit =
        runTest {
            val database: AppDatabase = createDatabase()
            try {
                importBackup(RoomBackupImporter(database), createBackup())
                RoomLocalDataResetter(database).reset()
                val actualSetup: com.toshlol.personalfinance.data.FirstRunSnapshot =
                    FirstRunSetup(RoomAccountRepository(database)).loadSetup()
                assertTrue(actualSetup.currency.isEmpty())
                assertTrue(actualSetup.accounts.isEmpty())
                assertTrue(!actualSetup.isSetupComplete)
            } finally {
                database.close()
            }
        }

    @Test
    fun canceledImportDoesNotMutateLedger(): Unit =
        runTest {
            val database: AppDatabase = createDatabase()
            try {
                val initial: FinanceBackup = createBackup()
                val seeder: RoomBackupImporter = RoomBackupImporter(database)
                importBackup(seeder, initial)
                val replacement: FinanceBackup = initial.copy(settings = BackupSettings("USD"))
                val importer: RoomBackupImporter = RoomBackupImporter(database, beforeTransaction = { awaitCancellation() })
                val importJob: Job =
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        importBackup(importer, replacement, BackupImportMode.OVERWRITE)
                    }
                importJob.cancelAndJoin()
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

    @Suppress("ktlint:standard:no-unit-return")
    private suspend fun assertReorderedBudgetScopeIsUnchanged(scenario: BudgetMergeScenario): Unit {
        val reorderedBudget: BackupRecurringBudget =
            scenario.originalBudget.copy(
                accountIds = listOf("savings", "account"),
                categoryIds = scenario.originalBudget.categoryIds.reversed(),
                tagIds = scenario.originalBudget.tagIds.reversed(),
            )
        val reorderedBackup: FinanceBackup = scenario.initial.copy(recurringBudgets = listOf(reorderedBudget))
        val outcome: BackupImportOutcome.Success = importBackup(scenario.importer, reorderedBackup) as BackupImportOutcome.Success
        val unchanged: Int =
            outcome.report.counts
                .getValue(BackupEntityType.BUDGET)
                .unchanged
        assertEquals(1, unchanged)
        assertEquals(scenario.initial, captureBackup(scenario.database))
    }

    private suspend fun captureBackup(database: AppDatabase): FinanceBackup {
        var encodedBackup: String = ""
        val writer: BackupDocumentWriter = BackupDocumentWriter { _, contents -> encodedBackup = contents }
        RoomBackupExporter(database, writer).exportTo(Uri.parse("content://backup-provider/snapshot.json"))
        return FinanceBackupCodec.decode(encodedBackup)
    }

    private fun createDatabase(): AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

    private fun createBackup(): FinanceBackup =
        FinanceBackup(
            schemaVersion = 4,
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

    private fun createV1BackupJson(): String =
        """{"schemaVersion":1,"settings":{"currency":"EUR"},"accounts":[{"id":"account","name":"Cash","type":"CASH","createdAt":1,"updatedAt":2,"archived":false}],"categories":[{"id":"category","name":"Food","direction":"EXPENSE","archived":false,"createdAt":1,"updatedAt":2}],"tags":[],"transactions":[{"id":"transaction","accountId":"account","amountMinor":100,"direction":"EXPENSE","date":1,"categoryId":"category","tagIds":[],"note":null,"createdAt":1,"updatedAt":2}],"budgets":[]}"""

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()
}
