package com.toshlol.personalfinance.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

@Database(
    entities = [
        AccountEntity::class,
        AppSettings::class,
        TransactionEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        TransactionTagCrossRef::class,
        BudgetEntity::class,
        LegacyBudgetAccountEntity::class,
        RecurringBudgetEntity::class,
        RecurringBudgetAccountEntity::class,
        RecurringBudgetCategoryEntity::class,
        RecurringBudgetTagEntity::class,
        TransferEntity::class,
    ],
    version = 9,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao

    abstract fun settingsDao(): AppSettingsDao

    abstract fun transactionDao(): TransactionDao

    abstract fun transferDao(): TransferDao

    abstract fun categoryTagDao(): CategoryTagDao

    abstract fun budgetDao(): BudgetDao

    abstract fun recurringBudgetDao(): RecurringBudgetDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null
        val MIGRATION_1_2: Migration =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE accounts_new (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, type TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)",
                    )
                    db.query("SELECT id, name, type, createdAt FROM accounts").use { cursor ->
                        while (cursor.moveToNext()) {
                            val identifier: String = UUID.randomUUID().toString()
                            db.execSQL(
                                "INSERT INTO accounts_new VALUES (?, ?, ?, ?, ?)",
                                arrayOf(identifier, cursor.getString(1), cursor.getString(2), cursor.getLong(3), cursor.getLong(3)),
                            )
                        }
                    }
                    db.execSQL("DROP TABLE accounts")
                    db.execSQL("ALTER TABLE accounts_new RENAME TO accounts")
                    db.execSQL("CREATE TABLE app_settings (id INTEGER NOT NULL PRIMARY KEY, currency TEXT NOT NULL)")
                }
            }

        val MIGRATION_2_3: Migration =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE transactions (id TEXT NOT NULL PRIMARY KEY, accountId TEXT NOT NULL, amountMinor INTEGER NOT NULL, direction TEXT NOT NULL, date INTEGER NOT NULL, note TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT)",
                    )
                    db.execSQL("CREATE INDEX index_transactions_accountId ON transactions(accountId)")
                    db.execSQL("CREATE INDEX index_transactions_date ON transactions(date)")
                }
            }

        val MIGRATION_3_4: Migration =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE categories (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, direction TEXT NOT NULL, archived INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)",
                    )
                    db.execSQL("CREATE INDEX index_categories_direction ON categories(direction)")
                    db.execSQL("CREATE INDEX index_categories_archived ON categories(archived)")
                    db.execSQL(
                        "CREATE TABLE tags (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, categoryId TEXT, archived INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT)",
                    )
                    db.execSQL("CREATE INDEX index_tags_categoryId ON tags(categoryId)")
                    db.execSQL("CREATE INDEX index_tags_archived ON tags(archived)")
                    db.execSQL("ALTER TABLE transactions ADD COLUMN categoryId TEXT REFERENCES categories(id) ON DELETE RESTRICT")
                    db.execSQL("CREATE INDEX index_transactions_categoryId ON transactions(categoryId)")
                    db.execSQL(
                        "CREATE TABLE transaction_tags (transactionId TEXT NOT NULL, tagId TEXT NOT NULL, PRIMARY KEY(transactionId, tagId), FOREIGN KEY(transactionId) REFERENCES transactions(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(tagId) REFERENCES tags(id) ON UPDATE NO ACTION ON DELETE RESTRICT)",
                    )
                    db.execSQL("CREATE INDEX index_transaction_tags_tagId ON transaction_tags(tagId)")
                }
            }

        val MIGRATION_4_5: Migration =
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE budgets (id TEXT NOT NULL PRIMARY KEY, categoryId TEXT NOT NULL, month TEXT NOT NULL, amountMinor INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT)",
                    )
                    db.execSQL("CREATE UNIQUE INDEX index_budgets_categoryId_month ON budgets(categoryId, month)")
                }
            }

        val MIGRATION_5_6: Migration =
            object : Migration(5, 6) {
                @Suppress("ktlint:standard:no-unit-return")
                override fun migrate(db: SupportSQLiteDatabase): Unit {
                    db.execSQL("ALTER TABLE accounts ADD COLUMN openingBalanceMinor INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE accounts ADD COLUMN openingBalanceDate INTEGER NOT NULL DEFAULT 0")
                    db.execSQL(
                        "UPDATE accounts SET openingBalanceDate = COALESCE((SELECT MIN(date) FROM transactions WHERE transactions.accountId = accounts.id), CAST(julianday(date(createdAt / 1000, 'unixepoch', 'localtime')) - 2440587.5 AS INTEGER))",
                    )
                    db.execSQL(
                        "CREATE TABLE transfers (id TEXT NOT NULL PRIMARY KEY, sourceAccountId TEXT NOT NULL, destinationAccountId TEXT NOT NULL, amountMinor INTEGER NOT NULL, date INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, FOREIGN KEY(sourceAccountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(destinationAccountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT)",
                    )
                    db.execSQL("CREATE INDEX index_transfers_sourceAccountId ON transfers(sourceAccountId)")
                    db.execSQL("CREATE INDEX index_transfers_destinationAccountId ON transfers(destinationAccountId)")
                    db.execSQL("CREATE INDEX index_transfers_date ON transfers(date)")
                }
            }

        val MIGRATION_6_7: Migration =
            object : Migration(6, 7) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE accounts ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
                }
            }

        val MIGRATION_7_8: Migration =
            object : Migration(7, 8) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE legacy_budget_accounts (budgetId TEXT NOT NULL, accountId TEXT NOT NULL, PRIMARY KEY(budgetId, accountId), FOREIGN KEY(budgetId) REFERENCES budgets(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT)",
                    )
                    db.execSQL("CREATE INDEX index_legacy_budget_accounts_accountId ON legacy_budget_accounts(accountId)")
                    db.execSQL(
                        "INSERT INTO legacy_budget_accounts (budgetId, accountId) SELECT budgets.id, accounts.id FROM budgets CROSS JOIN accounts",
                    )
                    db.execSQL(
                        "CREATE TABLE recurring_budgets (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, limitMinor INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)",
                    )
                    db.execSQL(
                        "CREATE TABLE recurring_budget_accounts (budgetId TEXT NOT NULL, accountId TEXT NOT NULL, PRIMARY KEY(budgetId, accountId), FOREIGN KEY(budgetId) REFERENCES recurring_budgets(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT)",
                    )
                    db.execSQL("CREATE INDEX index_recurring_budget_accounts_accountId ON recurring_budget_accounts(accountId)")
                }
            }

        val MIGRATION_8_9: Migration =
            object : Migration(8, 9) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE recurring_budget_categories (budgetId TEXT NOT NULL, categoryId TEXT NOT NULL, PRIMARY KEY(budgetId, categoryId), FOREIGN KEY(budgetId) REFERENCES recurring_budgets(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT)",
                    )
                    db.execSQL("CREATE INDEX index_recurring_budget_categories_categoryId ON recurring_budget_categories(categoryId)")
                    db.execSQL(
                        "CREATE TABLE recurring_budget_tags (budgetId TEXT NOT NULL, tagId TEXT NOT NULL, PRIMARY KEY(budgetId, tagId), FOREIGN KEY(budgetId) REFERENCES recurring_budgets(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(tagId) REFERENCES tags(id) ON UPDATE NO ACTION ON DELETE RESTRICT)",
                    )
                    db.execSQL("CREATE INDEX index_recurring_budget_tags_tagId ON recurring_budget_tags(tagId)")
                }
            }

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(context.applicationContext, AppDatabase::class.java, "personal_finance.db")
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .addMigrations(MIGRATION_5_6)
                    .addMigrations(MIGRATION_6_7)
                    .addMigrations(MIGRATION_7_8)
                    .addMigrations(MIGRATION_8_9)
                    .build()
                    .also { instance = it }
            }
    }
}
