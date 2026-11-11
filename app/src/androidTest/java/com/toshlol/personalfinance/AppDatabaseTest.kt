package com.toshlol.personalfinance

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.FirstRunSnapshot
import com.toshlol.personalfinance.data.RoomAccountRepository
import com.toshlol.personalfinance.data.RoomTransactionRepository
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.data.TransactionDraft
import com.toshlol.personalfinance.domain.FirstRunSetup
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun persistTransactionAndPreserveIdentityAndCreationTimeWhenEdited(): Unit =
        runTest {
            val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                val account: com.toshlol.personalfinance.data.Account =
                    FirstRunSetup(RoomAccountRepository(database, { "account-id" }, { 100L }))
                        .createFirstAccount("EUR", "Cash", AccountType.CASH)
                        .let { (it as com.toshlol.personalfinance.domain.SetupOutcome.Completed).snapshot.accounts.single() }
                val repository: RoomTransactionRepository = RoomTransactionRepository(database, { "transaction-id" }, { 200L })
                val initial: List<com.toshlol.personalfinance.data.FinanceTransaction> =
                    repository.save(
                        TransactionDraft(account.id, account.id, 1250, TransactionDirection.EXPENSE, 20600, "Lunch"),
                    )
                val edited: List<com.toshlol.personalfinance.data.FinanceTransaction> =
                    repository.save(
                        TransactionDraft("transaction-id", account.id, 1500, TransactionDirection.EXPENSE, 20601, null),
                    )
                assertEquals(1, initial.size)
                assertEquals(1, edited.size)
                assertEquals("transaction-id", edited.single().id)
                assertEquals(200L, edited.single().createdAt)
                assertEquals(200L, edited.single().updatedAt)
                assertEquals(1500L, edited.single().amountMinor)
                assertEquals(TransactionDirection.EXPENSE, edited.single().direction)
                assertEquals(20601L, edited.single().date)
                assertEquals(null, edited.single().note)
            } finally {
                database.close()
            }
        }

    @Test
    fun loadFreshDatabaseWithoutSeededAccountsOrCurrency(): Unit =
        runTest {
            val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                val actualSnapshot: FirstRunSnapshot = RoomAccountRepository(database).loadSetup()
                assertFalse(actualSnapshot.isSetupComplete)
                assertEquals("", actualSnapshot.currency)
                assertTrue(actualSnapshot.accounts.isEmpty())
            } finally {
                database.close()
            }
        }

    @Test
    fun persistFirstAccountAndCurrencyAcrossDatabaseReopen(): Unit =
        runTest {
            val databaseName: String = "first-run-${UUID.randomUUID()}.db"
            val database: AppDatabase = openDatabase(databaseName)
            val expectedIdentifier: String = "00000000-0000-0000-0000-000000000001"
            try {
                FirstRunSetup(RoomAccountRepository(database, { expectedIdentifier }, { 1234L }))
                    .createFirstAccount(" eur ", " Checking ", AccountType.BANK)
            } finally {
                database.close()
            }
            val reopenedDatabase: AppDatabase = openDatabase(databaseName)
            try {
                val actualSnapshot: FirstRunSnapshot = RoomAccountRepository(reopenedDatabase).loadSetup()
                assertTrue(actualSnapshot.isSetupComplete)
                assertEquals("EUR", actualSnapshot.currency)
                assertEquals(expectedIdentifier, actualSnapshot.accounts.single().id)
                assertEquals("Checking", actualSnapshot.accounts.single().name)
                assertEquals(AccountType.BANK, actualSnapshot.accounts.single().type)
                assertEquals(1234L, actualSnapshot.accounts.single().createdAt)
                assertEquals(1234L, actualSnapshot.accounts.single().updatedAt)
            } finally {
                reopenedDatabase.close()
                context.deleteDatabase(databaseName)
            }
        }

    @Test
    fun completeConcurrentSubmissionsWithExactlyOneAccount(): Unit =
        runTest {
            val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                val setup: FirstRunSetup = FirstRunSetup(RoomAccountRepository(database))
                List(4) { async { setup.createFirstAccount("EUR", "Cash", AccountType.CASH) } }.awaitAll()
                val actualSnapshot: FirstRunSnapshot = setup.loadSetup()
                assertEquals(1, actualSnapshot.accounts.size)
                assertEquals("EUR", actualSnapshot.currency)
            } finally {
                database.close()
            }
        }

    @Test
    fun rollBackAccountWhenSavingCurrencyFails(): Unit =
        runTest {
            val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                database.openHelper.writableDatabase.execSQL(
                    "CREATE TRIGGER fail_settings BEFORE INSERT ON app_settings BEGIN SELECT RAISE(ABORT, 'Injected write failure'); END",
                )
                val setup: FirstRunSetup = FirstRunSetup(RoomAccountRepository(database))
                try {
                    setup.createFirstAccount("EUR", "Cash", AccountType.CASH)
                    fail("Expected a storage failure")
                } catch (expectedException: IOException) {
                    assertFalse(setup.loadSetup().isSetupComplete)
                    assertTrue(setup.loadSetup().accounts.isEmpty())
                    assertEquals("", setup.loadSetup().currency)
                }
            } finally {
                database.close()
            }
        }

    @Test
    fun migratePrototypeAccountAndLegacyCurrencyWithoutDataLoss(): Unit =
        runTest {
            val databaseName: String = "migration-${UUID.randomUUID()}.db"
            createLegacyDatabase(databaseName)
            val database: AppDatabase = openDatabase(databaseName)
            var hasClearedLegacySetup: Boolean = false
            try {
                val repository: RoomAccountRepository =
                    RoomAccountRepository(
                        database,
                        loadLegacyCurrency = { "EUR" },
                        clearLegacySetup = { hasClearedLegacySetup = true },
                    )
                val actualSnapshot: FirstRunSnapshot = repository.loadSetup()
                assertTrue(actualSnapshot.isSetupComplete)
                assertEquals("EUR", actualSnapshot.currency)
                assertEquals("Old bank", actualSnapshot.accounts.single().name)
                assertEquals(AccountType.BANK, actualSnapshot.accounts.single().type)
                UUID.fromString(actualSnapshot.accounts.single().id)
                assertTrue(hasClearedLegacySetup)
            } finally {
                database.close()
            }
            val reopenedDatabase: AppDatabase = openDatabase(databaseName)
            try {
                assertTrue(RoomAccountRepository(reopenedDatabase).loadSetup().isSetupComplete)
            } finally {
                reopenedDatabase.close()
                context.deleteDatabase(databaseName)
            }
        }

    @Test
    fun loadCompletedSetupWithoutConsultingLegacyPreferences(): Unit =
        runTest {
            val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                FirstRunSetup(RoomAccountRepository(database)).createFirstAccount("EUR", "Cash", AccountType.CASH)
                val repository: RoomAccountRepository =
                    RoomAccountRepository(database, loadLegacyCurrency = { throw IOException("Corrupt legacy preferences") })
                assertTrue(repository.loadSetup().isSetupComplete)
            } finally {
                database.close()
            }
        }

    @Test
    fun migrateSeparateInstallationsWithUniqueStableIdentifiers(): Unit =
        runTest {
            val databaseNames: List<String> = List(2) { "unique-migration-${UUID.randomUUID()}.db" }
            val actualIdentifiers: List<String> =
                databaseNames.map { name: String ->
                    createLegacyDatabase(name)
                    val database: AppDatabase = openDatabase(name)
                    try {
                        RoomAccountRepository(database)
                            .loadSetup()
                            .accounts
                            .single()
                            .id
                    } finally {
                        database.close()
                    }
                }
            assertEquals(2, actualIdentifiers.toSet().size)
            databaseNames.zip(actualIdentifiers).forEach { entry: Pair<String, String> ->
                val database: AppDatabase = openDatabase(entry.first)
                try {
                    assertEquals(
                        entry.second,
                        RoomAccountRepository(database)
                            .loadSetup()
                            .accounts
                            .single()
                            .id,
                    )
                } finally {
                    database.close()
                    context.deleteDatabase(entry.first)
                }
            }
        }

    @Test
    fun keepMigratedSetupUsableWhenLegacyCleanupFails(): Unit =
        runTest {
            val databaseName: String = "cleanup-migration-${UUID.randomUUID()}.db"
            createLegacyDatabase(databaseName)
            val database: AppDatabase = openDatabase(databaseName)
            try {
                val repository: RoomAccountRepository =
                    RoomAccountRepository(
                        database,
                        loadLegacyCurrency = { "EUR" },
                        clearLegacySetup = { throw IOException("Read-only legacy file") },
                    )
                assertTrue(repository.loadSetup().isSetupComplete)
                assertTrue(RoomAccountRepository(database).loadSetup().isSetupComplete)
            } finally {
                database.close()
                context.deleteDatabase(databaseName)
            }
        }

    private fun createLegacyDatabase(databaseName: String) {
        context.getDatabasePath(databaseName).parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(databaseName), null).use { legacyDatabase: SQLiteDatabase ->
            legacyDatabase.execSQL(
                "CREATE TABLE accounts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL, createdAt INTEGER NOT NULL)",
            )
            legacyDatabase.execSQL("INSERT INTO accounts (id, name, type, createdAt) VALUES (7, 'Old bank', 'BANK', 1234)")
            legacyDatabase.version = 1
        }
    }

    private fun openDatabase(name: String): AppDatabase =
        Room
            .databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .build()
}
