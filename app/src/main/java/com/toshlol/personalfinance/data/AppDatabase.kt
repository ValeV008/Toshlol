package com.toshlol.personalfinance.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

@Database(entities = [AccountEntity::class, AppSettings::class, TransactionEntity::class], version = 3, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao

    abstract fun settingsDao(): AppSettingsDao

    abstract fun transactionDao(): TransactionDao

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

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(context.applicationContext, AppDatabase::class.java, "personal_finance.db")
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}
