package com.toshlol.personalfinance.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.withTransaction
import java.io.IOException
import java.util.UUID

@Entity(
    tableName = "transfers",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceAccountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["destinationAccountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("sourceAccountId"), Index("destinationAccountId"), Index("date")],
)
data class TransferEntity(
    @PrimaryKey val id: String,
    val sourceAccountId: String,
    val destinationAccountId: String,
    val amountMinor: Long,
    val date: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

data class FinanceTransfer(
    val id: String,
    val sourceAccountId: String,
    val sourceAccountName: String,
    val destinationAccountId: String,
    val destinationAccountName: String,
    val amountMinor: Long,
    val date: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

data class TransferDraft(
    val id: String? = null,
    val sourceAccountId: String,
    val destinationAccountId: String,
    val amountMinor: Long,
    val date: Long,
)

@Dao
interface TransferDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(transfer: TransferEntity)

    @Query("DELETE FROM transfers WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM transfers ORDER BY date DESC, createdAt DESC, id DESC")
    suspend fun loadAll(): List<TransferEntity>
}

interface TransferRepository {
    suspend fun loadTransfers(): List<FinanceTransfer>

    suspend fun save(draft: TransferDraft): List<FinanceTransfer>

    suspend fun delete(id: String): List<FinanceTransfer>
}

class RoomTransferRepository(
    private val database: AppDatabase,
    private val nextIdentifier: () -> String = { UUID.randomUUID().toString() },
    private val currentTime: () -> Long = System::currentTimeMillis,
) : TransferRepository {
    override suspend fun loadTransfers(): List<FinanceTransfer> =
        executeStorageOperation {
            database.withTransaction { loadInsideTransaction() }
        }

    override suspend fun save(draft: TransferDraft): List<FinanceTransfer> {
        require(draft.amountMinor > 0) { "Enter a positive amount." }
        require(draft.sourceAccountId != draft.destinationAccountId) { "Choose two different Accounts." }
        return executeStorageOperation {
            database.withTransaction {
                val accounts: Map<String, AccountEntity> = database.accountDao().loadAccounts().associateBy(AccountEntity::id)
                require(draft.sourceAccountId in accounts && draft.destinationAccountId in accounts) { "Choose two existing Accounts." }
                require(!accounts.getValue(draft.sourceAccountId).archived && !accounts.getValue(draft.destinationAccountId).archived) {
                    "Transfers require active Accounts."
                }
                val current: TransferEntity? = draft.id?.let { id -> database.transferDao().loadAll().firstOrNull { it.id == id } }
                val timestamp: Long = currentTime()
                database.transferDao().save(
                    TransferEntity(
                        current?.id ?: nextIdentifier(),
                        draft.sourceAccountId,
                        draft.destinationAccountId,
                        draft.amountMinor,
                        draft.date,
                        current?.createdAt ?: timestamp,
                        timestamp,
                    ),
                )
                loadInsideTransaction()
            }
        }
    }

    override suspend fun delete(id: String): List<FinanceTransfer> =
        executeStorageOperation {
            database.withTransaction {
                val transfer: TransferEntity? = database.transferDao().loadAll().firstOrNull { it.id == id }
                val archivedAccountIds: Set<String> =
                    database
                        .accountDao()
                        .loadAccounts()
                        .filter { it.archived }
                        .map(AccountEntity::id)
                        .toSet()
                require(
                    transfer == null ||
                        transfer.sourceAccountId !in archivedAccountIds &&
                        transfer.destinationAccountId !in archivedAccountIds,
                ) {
                    "Archived Account history is read-only."
                }
                database.transferDao().delete(id)
                loadInsideTransaction()
            }
        }

    private suspend fun loadInsideTransaction(): List<FinanceTransfer> {
        val accounts: Map<String, AccountEntity> = database.accountDao().loadAccounts().associateBy(AccountEntity::id)
        return database.transferDao().loadAll().mapNotNull { transfer ->
            val source: AccountEntity = accounts[transfer.sourceAccountId] ?: return@mapNotNull null
            val destination: AccountEntity = accounts[transfer.destinationAccountId] ?: return@mapNotNull null
            FinanceTransfer(
                transfer.id,
                source.id,
                source.name,
                destination.id,
                destination.name,
                transfer.amountMinor,
                transfer.date,
                transfer.createdAt,
                transfer.updatedAt,
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
