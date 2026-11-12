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
    ],
    version = 5,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao

    abstract fun settingsDao(): AppSettingsDao

    abstract fun transactionDao(): TransactionDao

    abstract fun categoryTagDao(): CategoryTagDao

    abstract fun budgetDao(): BudgetDao

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

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(context.applicationContext, AppDatabase::class.java, "personal_finance.db")
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }
    }
}
