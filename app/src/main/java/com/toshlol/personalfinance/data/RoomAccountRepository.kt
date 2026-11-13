package com.toshlol.personalfinance.data

import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.room.withTransaction
import java.io.IOException
import java.time.LocalDate
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
                    AccountEntity(
                        nextIdentifier(),
                        setup.accountName,
                        setup.selectedType.name,
                        timestamp,
                        timestamp,
                        0L,
                        LocalDate.now().toEpochDay(),
                    ),
                )
                if (snapshot.currency.isEmpty()) database.settingsDao().insertSettings(AppSettings(currency = setup.currency))
                loadSnapshot()
            }
        }
    }

    override suspend fun renameAccount(
        id: String,
        name: String,
    ): FirstRunSnapshot =
        executeStorageOperation {
            database.withTransaction {
                val trimmedName: String = name.trim()
                require(trimmedName.isNotEmpty()) { "Enter an account name." }
                val accounts: List<AccountEntity> = database.accountDao().loadAccounts()
                val account: AccountEntity = requireNotNull(accounts.firstOrNull { it.id == id }) { "This account no longer exists." }
                require(accounts.none { it.id != id && it.name.equals(trimmedName, ignoreCase = true) }) {
                    "An account with this name already exists."
                }
                database.accountDao().saveAccount(account.copy(name = trimmedName, updatedAt = currentTime()))
                loadSnapshot()
            }
        }

    override suspend fun saveAccount(
        id: String,
        name: String,
        openingBalanceMinor: Long,
        openingBalanceDate: Long,
    ): FirstRunSnapshot =
        executeStorageOperation {
            database.withTransaction {
                val trimmedName: String = name.trim()
                require(trimmedName.isNotEmpty()) { "Enter an account name." }
                val accounts: List<AccountEntity> = database.accountDao().loadAccounts()
                val account: AccountEntity = requireNotNull(accounts.firstOrNull { it.id == id }) { "This account no longer exists." }
                require(
                    accounts.none { it.id != id && it.name.equals(trimmedName, ignoreCase = true) },
                ) { "An account with this name already exists." }
                database.accountDao().saveAccount(
                    account.copy(
                        name = trimmedName,
                        openingBalanceMinor = openingBalanceMinor,
                        openingBalanceDate = openingBalanceDate,
                        updatedAt = currentTime(),
                    ),
                )
                loadSnapshot()
            }
        }

    override suspend fun createAccount(
        name: String,
        type: AccountType,
        openingBalanceMinor: Long,
        openingBalanceDate: Long,
    ): FirstRunSnapshot =
        executeStorageOperation {
            database.withTransaction {
                val trimmedName: String = name.trim()
                require(trimmedName.isNotEmpty()) { "Enter an account name." }
                require(
                    database.accountDao().loadAccounts().none {
                        it.name.equals(trimmedName, ignoreCase = true)
                    },
                ) { "An account with this name already exists." }
                val timestamp: Long = currentTime()
                database.accountDao().insertAccount(
                    AccountEntity(nextIdentifier(), trimmedName, type.name, timestamp, timestamp, openingBalanceMinor, openingBalanceDate),
                )
                loadSnapshot()
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
            accounts = loadAccountsWithBalances(),
        )

    private suspend fun loadAccountsWithBalances(): List<Account> {
        val transactions: List<TransactionEntity> = database.transactionDao().loadAll()
        val transfers: List<TransferEntity> = database.transferDao().loadAll()
        return database.accountDao().loadAccounts().map { account ->
            val transactionBalance: Long =
                transactions
                    .asSequence()
                    .filter {
                        it.accountId == account.id &&
                            it.date >= account.openingBalanceDate
                    }.sumOf {
                        if (it.direction ==
                            TransactionDirection.INCOME.name
                        ) {
                            it.amountMinor
                        } else {
                            -it.amountMinor
                        }
                    }
            val transferBalance: Long =
                transfers.asSequence().filter { it.date >= account.openingBalanceDate }.sumOf { transfer ->
                    when (account.id) {
                        transfer.sourceAccountId -> -transfer.amountMinor
                        transfer.destinationAccountId -> transfer.amountMinor
                        else -> 0L
                    }
                }
            Account(
                account.id,
                account.name,
                AccountType.valueOf(account.type),
                account.createdAt,
                account.updatedAt,
                account.openingBalanceMinor,
                account.openingBalanceDate,
                account.openingBalanceMinor + transactionBalance + transferBalance,
            )
        }
    }

    private suspend fun <T> executeStorageOperation(operation: suspend () -> T): T =
        try {
            operation()
        } catch (exception: SQLiteException) {
            throw IOException("Unable to access local finance data.", exception)
        }
}
