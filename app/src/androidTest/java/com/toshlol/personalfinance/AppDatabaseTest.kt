package com.toshlol.personalfinance

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.BudgetDraft
import com.toshlol.personalfinance.data.CategoryBudget
import com.toshlol.personalfinance.data.CategoryDraft
import com.toshlol.personalfinance.data.FinanceTransaction
import com.toshlol.personalfinance.data.FirstRunSnapshot
import com.toshlol.personalfinance.data.RoomAccountRepository
import com.toshlol.personalfinance.data.RoomBudgetRepository
import com.toshlol.personalfinance.data.RoomCategoryTagRepository
import com.toshlol.personalfinance.data.RoomTransactionRepository
import com.toshlol.personalfinance.data.TagDraft
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
    fun calculateMonthlyBudgetFromExpenseTransactionsAndKeepArchivedHistory(): Unit =
        runTest {
            val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                val fixtures: FinanceTestFixtures = FinanceTestFixtures(database)
                val account: Account = fixtures.createFirstAccount()
                val categoryRepository: RoomCategoryTagRepository = fixtures.organization
                val categoryId: String =
                    categoryRepository
                        .saveCategory(
                            CategoryDraft(name = "Food", direction = TransactionDirection.EXPENSE),
                        ).categories
                        .single {
                            it.name ==
                                "Food"
                        }.id
                val incomeCategoryId: String =
                    categoryRepository
                        .saveCategory(
                            CategoryDraft(name = "Pay", direction = TransactionDirection.INCOME),
                        ).categories
                        .single {
                            it.name ==
                                "Pay"
                        }.id
                val transactionRepository: RoomTransactionRepository = fixtures.transactions
                transactionRepository.save(
                    TransactionDraft(
                        null,
                        account.id,
                        1200,
                        TransactionDirection.EXPENSE,
                        java.time.LocalDate
                            .parse("2026-01-31")
                            .toEpochDay(),
                        null,
                        categoryId,
                    ),
                )
                transactionRepository.save(
                    TransactionDraft(
                        null,
                        account.id,
                        8000,
                        TransactionDirection.INCOME,
                        java.time.LocalDate
                            .parse("2026-01-31")
                            .toEpochDay(),
                        null,
                        incomeCategoryId,
                    ),
                )
                transactionRepository.save(
                    TransactionDraft(
                        null,
                        account.id,
                        500,
                        TransactionDirection.EXPENSE,
                        java.time.LocalDate
                            .parse("2026-02-01")
                            .toEpochDay(),
                        null,
                        categoryId,
                    ),
                )
                val savedExpenses: List<FinanceTransaction> =
                    transactionRepository.loadTransactions().filter { it.direction == TransactionDirection.EXPENSE }
                assertEquals(2, savedExpenses.size)
                assertEquals(2, savedExpenses.map(FinanceTransaction::id).toSet().size)
                assertEquals(categoryId, savedExpenses.first().categoryId)
                assertTrue(
                    "Unexpected expense transactions: $savedExpenses",
                    savedExpenses.any {
                        it.amountMinor == 1200L &&
                            java.time.YearMonth
                                .from(java.time.LocalDate.ofEpochDay(it.date))
                                .toString() == "2026-01"
                    },
                )
                val budgetRepository: RoomBudgetRepository = fixtures.budgets
                var actual: List<CategoryBudget> = budgetRepository.save(BudgetDraft(categoryId, java.time.YearMonth.of(2026, 1), 1000))
                assertEquals(1, actual.size)
                assertEquals(1200L, actual.single().spentMinor)
                assertEquals(-200L, actual.single().remainingMinor)
                assertTrue(actual.single().isOverBudget)
                actual = budgetRepository.save(BudgetDraft(categoryId, java.time.YearMonth.of(2026, 1), 1500))
                assertEquals(1, actual.size)
                assertEquals(1500L, actual.single().limitMinor)
                assertEquals(1200L, actual.single().spentMinor)
                categoryRepository.archiveCategory(categoryId)
                actual = budgetRepository.load()
                assertTrue(actual.single().isCategoryArchived)
                try {
                    budgetRepository.save(BudgetDraft(categoryId, java.time.YearMonth.of(2026, 2), 1000))
                    fail("Expected archived categories to reject new budgets")
                } catch (expectedException: IllegalArgumentException) {
                    assertTrue(expectedException.message!!.contains("active expense category"))
                }
            } finally {
                database.close()
            }
        }

    @Test
    fun persistDistinctFixtureRecordsAndEditOnlyTheRequestedTransaction(): Unit =
        runTest {
            val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                val fixtures: FinanceTestFixtures = FinanceTestFixtures(database)
                val account: Account = fixtures.createFirstAccount()
                fixtures.organization.saveCategory(CategoryDraft(name = "Food", direction = TransactionDirection.EXPENSE))
                val categories: List<com.toshlol.personalfinance.data.FinanceCategory> =
                    fixtures.organization.saveCategory(CategoryDraft(name = "Travel", direction = TransactionDirection.EXPENSE)).categories
                val foodId: String = categories.single { it.name == "Food" }.id
                val travelId: String = categories.single { it.name == "Travel" }.id
                fixtures.organization.saveTag(TagDraft(name = "Lunch", categoryId = foodId))
                val tags: List<com.toshlol.personalfinance.data.FinanceTag> =
                    fixtures.organization.saveTag(TagDraft(name = "Train", categoryId = travelId)).tags
                fixtures.transactions.save(TransactionDraft(null, account.id, 500L, TransactionDirection.EXPENSE, 20600L, null, foodId))
                val original: List<FinanceTransaction> =
                    fixtures.transactions.save(
                        TransactionDraft(null, account.id, 800L, TransactionDirection.EXPENSE, 20600L, null, travelId),
                    )
                fixtures.budgets.save(BudgetDraft(foodId, java.time.YearMonth.of(2026, 5), 1000L))
                val budgets: List<CategoryBudget> = fixtures.budgets.save(BudgetDraft(travelId, java.time.YearMonth.of(2026, 5), 2000L))
                val identifiers: List<String> =
                    listOf(account.id) + categories.map { it.id } + tags.map { it.id } +
                        original.map { it.id } + budgets.map { it.id }
                assertEquals(9, identifiers.toSet().size)
                assertEquals(listOf(500L, 800L), original.map { it.amountMinor }.sorted())
                assertTrue(original.all { it.createdAt == FinanceTestFixtures.FIXED_TIME_MILLIS })
                val editedId: String = original.single { it.categoryId == foodId }.id
                val edited: List<FinanceTransaction> =
                    fixtures.transactions.save(
                        TransactionDraft(editedId, account.id, 600L, TransactionDirection.EXPENSE, 20600L, null, foodId),
                    )
                assertEquals(original.map { it.id }.toSet(), edited.map { it.id }.toSet())
                assertEquals(600L, edited.single { it.id == editedId }.amountMinor)
                assertEquals(original.single { it.categoryId == travelId }, edited.single { it.categoryId == travelId })
            } finally {
                database.close()
            }
        }

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
                        TransactionDraft(null, account.id, 1250, TransactionDirection.EXPENSE, 20600, "Lunch"),
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
    fun organizeCategoriesAndTagsWithoutDamagingHistoricalTransactions(): Unit =
        runTest {
            val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                val fixtures: FinanceTestFixtures = FinanceTestFixtures(database)
                val account: Account = fixtures.createFirstAccount()
                val organizationRepository: RoomCategoryTagRepository = fixtures.organization
                var organization =
                    organizationRepository.saveCategory(
                        CategoryDraft(name = "  Food ", direction = TransactionDirection.EXPENSE),
                    )
                val foodId: String = organization.categories.single().id
                try {
                    organizationRepository.saveCategory(CategoryDraft(name = "food", direction = TransactionDirection.EXPENSE))
                    fail("Expected active category names to be unique without case sensitivity")
                } catch (expectedException: IllegalArgumentException) {
                    assertTrue(expectedException.message!!.contains("already exists"))
                }
                organization = organizationRepository.saveCategory(CategoryDraft(name = "Travel", direction = TransactionDirection.EXPENSE))
                val travelId: String = organization.categories.single { it.name == "Travel" }.id
                organization = organizationRepository.saveTag(TagDraft(name = " Lunch ", categoryId = foodId))
                val lunchId: String = organization.tags.single { it.name == "Lunch" }.id
                organization = organizationRepository.saveTag(TagDraft(name = "Reimbursable"))
                val independentId: String = organization.tags.single { it.name == "Reimbursable" }.id
                try {
                    organizationRepository.saveTag(TagDraft(name = " reimbursable "))
                    fail("Expected active tag names to be unique without case sensitivity")
                } catch (expectedException: IllegalArgumentException) {
                    assertTrue(expectedException.message!!.contains("already exists"))
                }
                val transactionRepository: RoomTransactionRepository = fixtures.transactions
                transactionRepository.save(
                    TransactionDraft(
                        null,
                        account.id,
                        1250,
                        TransactionDirection.EXPENSE,
                        20600,
                        "Lunch",
                        foodId,
                        setOf(lunchId, independentId),
                    ),
                )
                val transactionId: String = transactionRepository.loadTransactions().single().id
                organizationRepository.saveCategory(CategoryDraft(foodId, "Dining", TransactionDirection.EXPENSE))
                organizationRepository.saveTag(TagDraft(lunchId, "Meal", foodId))
                var actual: com.toshlol.personalfinance.data.FinanceTransaction = transactionRepository.loadTransactions().single()
                assertEquals("Dining", actual.categoryName)
                assertEquals(listOf("Meal", "Reimbursable"), actual.tagNames.sorted())
                try {
                    organizationRepository.saveCategory(CategoryDraft(foodId, "Dining", TransactionDirection.INCOME))
                    fail("Expected a category used by transactions to keep its direction")
                } catch (expectedException: IllegalArgumentException) {
                    assertTrue(expectedException.message!!.contains("cannot change its direction"))
                }
                try {
                    organizationRepository.saveTag(TagDraft(lunchId, "Meal", travelId))
                    fail("Expected a tag used by transactions to keep its category")
                } catch (expectedException: IllegalArgumentException) {
                    assertTrue(expectedException.message!!.contains("cannot change its category"))
                }
                transactionRepository.save(
                    TransactionDraft(
                        transactionId,
                        account.id,
                        1250,
                        TransactionDirection.EXPENSE,
                        20600,
                        "Lunch",
                        travelId,
                        setOf(lunchId, independentId),
                    ),
                )
                val renamedAccountSnapshot: FirstRunSnapshot =
                    RoomAccountRepository(database, currentTime = {
                        350L
                    }).renameAccount(account.id, "  Everyday cash  ")
                assertEquals("Everyday cash", renamedAccountSnapshot.accounts.single().name)
                assertEquals("Everyday cash", transactionRepository.loadTransactions().single().accountName)
                actual = transactionRepository.loadTransactions().single()
                assertEquals("Travel", actual.categoryName)
                assertEquals(listOf("Reimbursable"), actual.tagNames)
                organizationRepository.archiveCategory(travelId)
                actual = transactionRepository.loadTransactions().single()
                assertEquals("Travel", actual.categoryName)
                try {
                    transactionRepository.save(TransactionDraft(null, account.id, 100, TransactionDirection.EXPENSE, 20601, null, travelId))
                    fail("Expected an archived category to be unavailable for a new transaction")
                } catch (expectedException: IllegalArgumentException) {
                    assertTrue(expectedException.message!!.contains("active category"))
                }
                organizationRepository.archiveTag(independentId)
                assertEquals(listOf("Reimbursable"), transactionRepository.loadTransactions().single().tagNames)
                try {
                    transactionRepository.save(
                        TransactionDraft(
                            null,
                            account.id,
                            100,
                            TransactionDirection.EXPENSE,
                            20601,
                            null,
                            tagIds = setOf(independentId),
                        ),
                    )
                    fail("Expected an archived tag to be unavailable for a new transaction")
                } catch (expectedException: IllegalArgumentException) {
                    assertTrue(expectedException.message!!.contains("active tags"))
                }
            } finally {
                database.close()
            }
        }

    @Test
    fun migrateVersionThreeTransactionsWithoutLosingHistory(): Unit =
        runTest {
            val databaseName: String = "categories-migration-${UUID.randomUUID()}.db"
            createVersionThreeDatabase(databaseName)
            val database: AppDatabase = openDatabase(databaseName)
            try {
                val actual: com.toshlol.personalfinance.data.FinanceTransaction =
                    RoomTransactionRepository(database).loadTransactions().single()
                assertEquals("legacy-transaction", actual.id)
                assertEquals("Legacy cash", actual.accountName)
                assertEquals(1250L, actual.amountMinor)
                assertEquals(TransactionDirection.EXPENSE, actual.direction)
                assertEquals(null, actual.categoryId)
                assertTrue(actual.tagIds.isEmpty())
                assertTrue(actual.tagNames.isEmpty())
            } finally {
                database.close()
                context.deleteDatabase(databaseName)
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

    private fun createVersionThreeDatabase(databaseName: String) {
        context.getDatabasePath(databaseName).parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(databaseName), null).use { legacyDatabase: SQLiteDatabase ->
            legacyDatabase.execSQL(
                "CREATE TABLE accounts (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, type TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)",
            )
            legacyDatabase.execSQL("INSERT INTO accounts VALUES ('legacy-account', 'Legacy cash', 'CASH', 100, 100)")
            legacyDatabase.execSQL("CREATE TABLE app_settings (id INTEGER NOT NULL PRIMARY KEY, currency TEXT NOT NULL)")
            legacyDatabase.execSQL("INSERT INTO app_settings VALUES (1, 'EUR')")
            legacyDatabase.execSQL(
                "CREATE TABLE transactions (id TEXT NOT NULL PRIMARY KEY, accountId TEXT NOT NULL, amountMinor INTEGER NOT NULL, direction TEXT NOT NULL, date INTEGER NOT NULL, note TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT)",
            )
            legacyDatabase.execSQL("CREATE INDEX index_transactions_accountId ON transactions(accountId)")
            legacyDatabase.execSQL("CREATE INDEX index_transactions_date ON transactions(date)")
            legacyDatabase.execSQL(
                "INSERT INTO transactions VALUES ('legacy-transaction', 'legacy-account', 1250, 'EXPENSE', 20600, 'Before categories', 200, 200)",
            )
            legacyDatabase.version = 3
        }
    }

    private fun openDatabase(name: String): AppDatabase =
        Room
            .databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .addMigrations(AppDatabase.MIGRATION_3_4)
            .addMigrations(AppDatabase.MIGRATION_4_5)
            .build()
}
