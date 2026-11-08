package com.toshlol.personalfinance.data

import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.room.withTransaction
import java.io.IOException
import java.util.UUID

class RoomAccountRepository(
    private val database: AppDatabase,
    private val nextIdentifier: () -> String = { UUID.randomUUID().toString() },
    private val currentTime: () -> Long = System::currentTimeMillis,
    private val loadLegacyCurrency: suspend () -> String? = { null },
    private val clearLegacySetup: suspend () -> Unit = {},
) : AccountRepository {
    override suspend fun loadSetup(): FirstRunSnapshot =
        executeStorageOperation {
            val snapshot: FirstRunSnapshot = database.withTransaction { loadSnapshot() }
            if (snapshot.isSetupComplete || snapshot.accounts.isEmpty()) return@executeStorageOperation snapshot
            migrateLegacySetup(snapshot)
        }

    override suspend fun createFirstAccount(setup: AccountSetupValidationResult): FirstRunSnapshot {
        require(setup.isValid)
        return executeStorageOperation {
            database.withTransaction {
                val snapshot: FirstRunSnapshot = loadSnapshot()
                if (snapshot.isSetupComplete) return@withTransaction snapshot
                val timestamp: Long = currentTime()
                database.accountDao().insertAccount(
                    AccountEntity(nextIdentifier(), setup.accountName, setup.selectedType.name, timestamp, timestamp),
                )
                if (snapshot.currency.isEmpty()) database.settingsDao().insertSettings(AppSettings(currency = setup.currency))
                loadSnapshot()
            }
        }
    }

    private suspend fun migrateLegacySetup(snapshot: FirstRunSnapshot): FirstRunSnapshot {
        val legacyCurrency: String? = loadLegacyCurrency()
        if (legacyCurrency !in AccountSetupValidator.supportedCurrencies) return snapshot
        val migratedSnapshot: FirstRunSnapshot =
            database.withTransaction {
                if (database.settingsDao().loadCurrency() == null) {
                    database.settingsDao().insertSettings(AppSettings(currency = requireNotNull(legacyCurrency)))
                }
                loadSnapshot()
            }
        try {
            clearLegacySetup()
        } catch (exception: IOException) {
            Log.w("FinanceSetup", "Could not remove obsolete setup preferences.", exception)
        }
        return migratedSnapshot
    }

    private suspend fun loadSnapshot(): FirstRunSnapshot =
        FirstRunSnapshot(
            currency = database.settingsDao().loadCurrency().orEmpty(),
            accounts =
                database.accountDao().loadAccounts().map { account: AccountEntity ->
                    Account(account.id, account.name, AccountType.valueOf(account.type), account.createdAt, account.updatedAt)
                },
        )

    private suspend fun <T> executeStorageOperation(operation: suspend () -> T): T =
        try {
            operation()
        } catch (exception: SQLiteException) {
            throw IOException("Unable to access local finance data.", exception)
        }
}
