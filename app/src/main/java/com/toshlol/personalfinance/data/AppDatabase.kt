package com.toshlol.personalfinance.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

@Database(entities = [AccountEntity::class, AppSettings::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao

    abstract fun settingsDao(): AppSettingsDao

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

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(context.applicationContext, AppDatabase::class.java, "personal_finance.db")
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
