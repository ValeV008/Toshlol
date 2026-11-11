package com.toshlol.personalfinance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.withTransaction
import java.io.IOException
import java.util.UUID

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC, id DESC")
    suspend fun loadAll(): List<TransactionEntity>

    @Query("SELECT * FROM accounts ORDER BY createdAt, id")
    suspend fun loadAccounts(): List<AccountEntity>
}

interface TransactionRepository {
    suspend fun loadTransactions(): List<FinanceTransaction>

    suspend fun save(draft: TransactionDraft): List<FinanceTransaction>

    suspend fun delete(id: String): List<FinanceTransaction>
}

class RoomTransactionRepository(
    private val database: AppDatabase,
    private val nextIdentifier: () -> String = { UUID.randomUUID().toString() },
    private val currentTime: () -> Long = System::currentTimeMillis,
) : TransactionRepository {
    override suspend fun loadTransactions(): List<FinanceTransaction> = load()

    override suspend fun save(draft: TransactionDraft): List<FinanceTransaction> {
        require(draft.amountMinor > 0)
        return executeStorageOperation {
            database.withTransaction {
                val existing: TransactionEntity? =
                    draft.id?.let { identifier: String ->
                        database.transactionDao().loadAll().firstOrNull {
                            it.id ==
                                identifier
                        }
                    }
                val timestamp: Long = currentTime()
                database.transactionDao().save(
                    TransactionEntity(
                        id = existing?.id ?: nextIdentifier(),
                        accountId = draft.accountId,
                        amountMinor = draft.amountMinor,
                        direction = draft.direction.name,
                        date = draft.date,
                        note = draft.note?.trim()?.takeIf(String::isNotEmpty),
                        createdAt = existing?.createdAt ?: timestamp,
                        updatedAt = timestamp,
                    ),
                )
                loadInsideTransaction()
            }
        }
    }

    override suspend fun delete(id: String): List<FinanceTransaction> =
        executeStorageOperation {
            database.withTransaction {
                database.transactionDao().delete(id)
                loadInsideTransaction()
            }
        }

    private suspend fun load(): List<FinanceTransaction> = executeStorageOperation { database.withTransaction { loadInsideTransaction() } }

    private suspend fun loadInsideTransaction(): List<FinanceTransaction> {
        val accountNames: Map<String, String> = database.transactionDao().loadAccounts().associate { it.id to it.name }
        return database.transactionDao().loadAll().map { entity: TransactionEntity ->
            FinanceTransaction(
                entity.id,
                entity.accountId,
                accountNames[entity.accountId].orEmpty(),
                entity.amountMinor,
                TransactionDirection.valueOf(entity.direction),
                entity.date,
                entity.note,
                entity.createdAt,
                entity.updatedAt,
            )
        }
    }

    private suspend fun <T> executeStorageOperation(operation: suspend () -> T): T =
        try {
            operation()
        } catch (exception: android.database.sqlite.SQLiteException) {
            throw IOException("Unable to access local finance data.", exception)
        }
}
