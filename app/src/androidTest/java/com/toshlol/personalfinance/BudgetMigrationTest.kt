package com.toshlol.personalfinance

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.CategoryBudget
import com.toshlol.personalfinance.data.RoomBudgetRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class BudgetMigrationTest {
    @get:Rule
    val migrationHelper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun migrateLegacyBudgetPreservesIdentityProvenanceAndAllExistingAccountsAcrossReopen(): Unit =
        runTest {
            val databaseName: String = "budget-migration-${UUID.randomUUID()}.db"
            try {
                migrationHelper.createDatabase(databaseName, 7).use(::seedLegacyBudget)
                migrationHelper.runMigrationsAndValidate(databaseName, 8, true, AppDatabase.MIGRATION_7_8).close()
                val migratedDatabase: AppDatabase = openDatabase(databaseName)
                try {
                    assertMigratedLegacyBudget(migratedDatabase)
                } finally {
                    migratedDatabase.close()
                }
                val reopenedDatabase: AppDatabase = openDatabase(databaseName)
                try {
                    assertReopenedLegacyBudget(reopenedDatabase)
                } finally {
                    reopenedDatabase.close()
                }
            } finally {
                context.deleteDatabase(databaseName)
            }
        }

    @Suppress("ktlint:standard:no-unit-return")
    private suspend fun assertMigratedLegacyBudget(database: AppDatabase): Unit {
        val actualBudget: CategoryBudget = RoomBudgetRepository(database).load().single()
        assertEquals("legacy-budget", actualBudget.id)
        assertEquals("legacy-food", actualBudget.categoryId)
        assertEquals(YearMonth.of(2025, 12), actualBudget.month)
        assertEquals(10_000L, actualBudget.limitMinor)
        assertEquals(750L, actualBudget.spentMinor)
        assertEquals(100L, actualBudget.createdAt)
        assertEquals(200L, actualBudget.updatedAt)
        assertEquals(setOf("cash", "savings"), actualBudget.accountIds)
    }

    @Suppress("ktlint:standard:no-unit-return")
    private suspend fun assertReopenedLegacyBudget(database: AppDatabase): Unit {
        assertMigratedLegacyBudget(database)
        assertEquals(2, database.transactionDao().loadAll().size)
        assertEquals("EUR", database.settingsDao().loadCurrency())
    }

    private fun seedLegacyBudget(database: SupportSQLiteDatabase) {
        database.execSQL("INSERT INTO accounts VALUES ('cash', 'Cash', 'CASH', 1, 2, 0, 20450, 0)")
        database.execSQL("INSERT INTO accounts VALUES ('savings', 'Savings', 'BANK', 3, 4, 0, 20450, 0)")
        database.execSQL("INSERT INTO app_settings VALUES (1, 'EUR')")
        database.execSQL("INSERT INTO categories VALUES ('legacy-food', 'Food', 'EXPENSE', 0, 5, 6)")
        database.execSQL(
            "INSERT INTO transactions VALUES ('cash-expense', 'cash', 300, 'EXPENSE', ${LocalDate.of(
                2025,
                12,
                4,
            ).toEpochDay()}, 'legacy-food', NULL, 10, 11)",
        )
        database.execSQL(
            "INSERT INTO transactions VALUES ('savings-expense', 'savings', 450, 'EXPENSE', ${LocalDate.of(
                2025,
                12,
                5,
            ).toEpochDay()}, 'legacy-food', NULL, 12, 13)",
        )
        database.execSQL("INSERT INTO budgets VALUES ('legacy-budget', 'legacy-food', '2025-12', 10000, 100, 200)")
    }

    private fun openDatabase(name: String): AppDatabase =
        Room
            .databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .addMigrations(AppDatabase.MIGRATION_3_4)
            .addMigrations(AppDatabase.MIGRATION_4_5)
            .addMigrations(AppDatabase.MIGRATION_5_6)
            .addMigrations(AppDatabase.MIGRATION_6_7)
            .addMigrations(AppDatabase.MIGRATION_7_8)
            .build()
}
