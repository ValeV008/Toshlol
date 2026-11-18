package com.toshlol.personalfinance

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.data.AccountEntity
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.AppSettings
import com.toshlol.personalfinance.data.BackupAccount
import com.toshlol.personalfinance.data.BackupBudget
import com.toshlol.personalfinance.data.BackupCategory
import com.toshlol.personalfinance.data.BackupDocumentWriter
import com.toshlol.personalfinance.data.BackupRecurringBudget
import com.toshlol.personalfinance.data.BackupSettings
import com.toshlol.personalfinance.data.BackupTag
import com.toshlol.personalfinance.data.BackupTransaction
import com.toshlol.personalfinance.data.BackupTransfer
import com.toshlol.personalfinance.data.BudgetEntity
import com.toshlol.personalfinance.data.CategoryEntity
import com.toshlol.personalfinance.data.FinanceBackup
import com.toshlol.personalfinance.data.FinanceBackupCodec
import com.toshlol.personalfinance.data.LegacyBudgetAccountEntity
import com.toshlol.personalfinance.data.RecurringBudgetAccountEntity
import com.toshlol.personalfinance.data.RecurringBudgetCategoryEntity
import com.toshlol.personalfinance.data.RecurringBudgetEntity
import com.toshlol.personalfinance.data.RecurringBudgetTagEntity
import com.toshlol.personalfinance.data.RoomBackupExporter
import com.toshlol.personalfinance.data.TagEntity
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.data.TransactionEntity
import com.toshlol.personalfinance.data.TransactionTagCrossRef
import com.toshlol.personalfinance.data.TransferEntity
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class BackupExportTest {
    @Test
    fun exportWritesCompleteVersionedSnapshot(): Unit =
        runTest {
            val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                val expectedBackup: FinanceBackup = saveFinanceSnapshot(database)
                val writer: RecordingBackupWriter = RecordingBackupWriter()
                val destination: Uri = Uri.parse("content://backup-provider/export.json")
                RoomBackupExporter(database, writer).exportTo(destination)
                val actualBackup: FinanceBackup = FinanceBackupCodec.decode(writer.contents)
                assertEquals(destination, writer.destination)
                assertEquals(expectedBackup, actualBackup)
            } finally {
                database.close()
            }
        }

    @Test
    fun codecRoundTripsAndRejectsMalformedUnknownAndUnsupportedInput() {
        val inputBackup: FinanceBackup = createExpectedBackup()
        val encodedBackup: String = FinanceBackupCodec.encode(inputBackup)
        assertEquals(inputBackup, FinanceBackupCodec.decode(encodedBackup))
        assertThrows(SerializationException::class.java) { FinanceBackupCodec.decode("{") }
        assertThrows(SerializationException::class.java) { FinanceBackupCodec.decode(encodedBackup.dropLast(1) + ",\"unexpected\":true}") }
        assertThrows(SerializationException::class.java) {
            FinanceBackupCodec.decode(encodedBackup.replace("\"schemaVersion\":4", "\"schemaVersion\":99"))
        }
    }

    @Test
    fun codecNormalizesVersionOneOpeningBalancesAndTransfers() {
        val versionOneBackup: String =
            """
            {"schemaVersion":1,"settings":{"currency":"EUR"},"accounts":[{"id":"account","name":"Cash","type":"CASH","createdAt":1,"updatedAt":1,"archived":false}],"categories":[],"tags":[],"transactions":[{"id":"transaction","accountId":"account","amountMinor":100,"direction":"EXPENSE","date":20,"categoryId":null,"tagIds":[],"note":null,"createdAt":1,"updatedAt":1}],"budgets":[]}
            """.trimIndent()
        val actualBackup: FinanceBackup = FinanceBackupCodec.decode(versionOneBackup)
        assertEquals(4, actualBackup.schemaVersion)
        assertEquals(0L, actualBackup.accounts.single().openingBalanceMinor)
        assertEquals(20L, actualBackup.accounts.single().openingBalanceDate)
        assertEquals(emptyList<BackupTransfer>(), actualBackup.transfers)
    }

    @Test
    fun failedDocumentWriteLeavesFinanceSnapshotUnchanged(): Unit =
        runTest {
            val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                saveFinanceSnapshot(database)
                val failingWriter: BackupDocumentWriter = BackupDocumentWriter { _, _ -> throw IOException("Provider failed") }
                assertThrows(IOException::class.java) {
                    kotlinx.coroutines.runBlocking {
                        RoomBackupExporter(database, failingWriter).exportTo(Uri.parse("content://backup-provider/fail.json"))
                    }
                }
                assertEquals("EUR", database.settingsDao().loadCurrency())
                assertEquals(2, database.accountDao().loadAccounts().size)
                assertEquals(1, database.transactionDao().loadAll().size)
            } finally {
                database.close()
            }
        }

    private suspend fun saveFinanceSnapshot(database: AppDatabase): FinanceBackup {
        val account: AccountEntity = AccountEntity("account", "Cash", "CASH", 1L, 2L, -500L, 10L, false)
        val destinationAccount: AccountEntity = AccountEntity("savings", "Savings", "BANK", 3L, 4L, 1000L, 11L, true)
        val category: CategoryEntity = CategoryEntity("category", "Food", TransactionDirection.EXPENSE.name, false, 5L, 6L)
        val tag: TagEntity = TagEntity("tag", "Groceries", category.id, true, 7L, 8L)
        val transaction: TransactionEntity =
            TransactionEntity("transaction", account.id, 1250L, "EXPENSE", 20L, category.id, "Market", 9L, 10L)
        val transfer: TransferEntity = TransferEntity("transfer", account.id, destinationAccount.id, 400L, 21L, 11L, 12L)
        val budget: BudgetEntity = BudgetEntity("budget", category.id, "2026-11", 5000L, 13L, 14L)
        database.settingsDao().insertSettings(AppSettings(currency = "EUR"))
        database.accountDao().insertAccount(account)
        database.accountDao().insertAccount(destinationAccount)
        database.categoryTagDao().saveCategory(category)
        database.categoryTagDao().saveTag(tag)
        database.transactionDao().save(transaction)
        database.categoryTagDao().addTransactionTag(TransactionTagCrossRef(transaction.id, tag.id))
        database.transferDao().save(transfer)
        database.budgetDao().save(budget)
        listOf(account.id, destinationAccount.id).forEach { accountId ->
            database.budgetDao().saveAccount(LegacyBudgetAccountEntity(budget.id, accountId))
        }
        val recurringBudget: RecurringBudgetEntity = RecurringBudgetEntity("recurring-budget", "Household", 7_000L, 15L, 16L)
        database.recurringBudgetDao().save(recurringBudget)
        database.recurringBudgetDao().saveAccount(RecurringBudgetAccountEntity(recurringBudget.id, account.id))
        database.recurringBudgetDao().saveCategory(RecurringBudgetCategoryEntity(recurringBudget.id, category.id))
        database.recurringBudgetDao().saveTag(RecurringBudgetTagEntity(recurringBudget.id, tag.id))
        return createExpectedBackup()
    }

    private fun createExpectedBackup(): FinanceBackup =
        FinanceBackup(
            schemaVersion = 4,
            settings = BackupSettings("EUR"),
            accounts =
                listOf(
                    BackupAccount("account", "Cash", "CASH", 1L, 2L, -500L, 10L, false),
                    BackupAccount("savings", "Savings", "BANK", 3L, 4L, 1000L, 11L, true),
                ),
            categories = listOf(BackupCategory("category", "Food", "EXPENSE", false, 5L, 6L)),
            tags = listOf(BackupTag("tag", "Groceries", "category", true, 7L, 8L)),
            transactions =
                listOf(
                    BackupTransaction("transaction", "account", 1250L, "EXPENSE", 20L, "category", listOf("tag"), "Market", 9L, 10L),
                ),
            transfers = listOf(BackupTransfer("transfer", "account", "savings", 400L, 21L, 11L, 12L)),
            budgets = listOf(BackupBudget("budget", "category", "2026-11", 5000L, 13L, 14L, listOf("account", "savings"))),
            recurringBudgets =
                listOf(
                    BackupRecurringBudget(
                        "recurring-budget",
                        "Household",
                        7_000L,
                        listOf("account"),
                        15L,
                        16L,
                        listOf("category"),
                        listOf("tag"),
                    ),
                ),
        )

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()
}

private class RecordingBackupWriter : BackupDocumentWriter {
    var destination: Uri? = null
        private set
    var contents: String = ""
        private set

    override suspend fun write(
        destination: Uri,
        contents: String,
    ) {
        this.destination = destination
        this.contents = contents
    }
}
