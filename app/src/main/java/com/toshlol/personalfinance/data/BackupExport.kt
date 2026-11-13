package com.toshlol.personalfinance.data

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.io.OutputStream
import java.time.LocalDate

const val CURRENT_BACKUP_SCHEMA_VERSION: Int = 2
private const val DEFAULT_OPENING_BALANCE_MINOR: Long = 0

@Serializable
data class FinanceBackup(
    val schemaVersion: Int,
    val settings: BackupSettings,
    val accounts: List<BackupAccount>,
    val categories: List<BackupCategory>,
    val tags: List<BackupTag>,
    val transactions: List<BackupTransaction>,
    val transfers: List<BackupTransfer>,
    val budgets: List<BackupBudget>,
)

@Serializable
data class BackupSettings(
    val currency: String,
)

@Serializable
data class BackupAccount(
    val id: String,
    val name: String,
    val type: String,
    val createdAt: Long,
    val updatedAt: Long,
    val openingBalanceMinor: Long,
    val openingBalanceDate: Long,
    val archived: Boolean,
)

@Serializable
data class BackupCategory(
    val id: String,
    val name: String,
    val direction: String,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupTag(
    val id: String,
    val name: String,
    val categoryId: String?,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupTransaction(
    val id: String,
    val accountId: String,
    val amountMinor: Long,
    val direction: String,
    val date: Long,
    val categoryId: String?,
    val tagIds: List<String>,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupTransfer(
    val id: String,
    val sourceAccountId: String,
    val destinationAccountId: String,
    val amountMinor: Long,
    val date: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupBudget(
    val id: String,
    val categoryId: String,
    val month: String,
    val amountMinor: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
private data class FinanceBackupV1(
    val schemaVersion: Int,
    val settings: BackupSettings,
    val accounts: List<BackupAccountV1>,
    val categories: List<BackupCategory>,
    val tags: List<BackupTag>,
    val transactions: List<BackupTransaction>,
    val budgets: List<BackupBudget>,
)

@Serializable
private data class BackupAccountV1(
    val id: String,
    val name: String,
    val type: String,
    val createdAt: Long,
    val updatedAt: Long,
    val archived: Boolean,
)

object FinanceBackupCodec {
    private val json: Json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = false
            isLenient = false
        }

    fun encode(backup: FinanceBackup): String {
        require(backup.schemaVersion == CURRENT_BACKUP_SCHEMA_VERSION) { "Unsupported Backup schema version." }
        return json.encodeToString(backup)
    }

    fun decode(contents: String): FinanceBackup {
        val root: JsonElement = json.parseToJsonElement(contents)
        val version: Int =
            root.jsonObject["schemaVersion"]?.jsonPrimitive?.int
                ?: throw SerializationException("Backup schema version is missing.")
        return when (version) {
            1 -> normalizeV1(json.decodeFromString<FinanceBackupV1>(contents))
            CURRENT_BACKUP_SCHEMA_VERSION -> json.decodeFromString<FinanceBackup>(contents)
            else -> throw SerializationException("Unsupported Backup schema version: $version")
        }
    }

    private fun normalizeV1(backup: FinanceBackupV1): FinanceBackup {
        val earliestMovementDate: Map<String, Long> =
            backup.transactions.groupBy(BackupTransaction::accountId).mapValues { entry: Map.Entry<String, List<BackupTransaction>> ->
                entry.value.minOf(BackupTransaction::date)
            }
        val fallbackDate: Long = LocalDate.now().toEpochDay()
        return FinanceBackup(
            CURRENT_BACKUP_SCHEMA_VERSION,
            backup.settings,
            backup.accounts.map { account: BackupAccountV1 ->
                BackupAccount(
                    account.id,
                    account.name,
                    account.type,
                    account.createdAt,
                    account.updatedAt,
                    DEFAULT_OPENING_BALANCE_MINOR,
                    earliestMovementDate[account.id] ?: fallbackDate,
                    account.archived,
                )
            },
            backup.categories,
            backup.tags,
            backup.transactions,
            emptyList(),
            backup.budgets,
        )
    }
}

class RoomBackupExporter(
    private val database: AppDatabase,
) {
    suspend fun exportTo(
        contentResolver: ContentResolver,
        destination: Uri,
    ): Unit =
        withContext(Dispatchers.IO) {
            val backup: FinanceBackup = database.withTransaction { createSnapshot() }
            val contents: String = FinanceBackupCodec.encode(backup)
            val output: OutputStream =
                contentResolver.openOutputStream(destination, "wt")
                    ?: throw IOException("The selected destination could not be opened.")
            output.bufferedWriter(Charsets.UTF_8).use { writer: java.io.BufferedWriter -> writer.write(contents) }
        }

    private suspend fun createSnapshot(): FinanceBackup {
        val settings: AppSettings =
            requireNotNull(database.settingsDao().loadAll().singleOrNull()) {
                "Finance settings are missing or invalid."
            }
        val tagsByTransaction: Map<String, List<String>> =
            database
                .transactionDao()
                .loadAllTransactionTags()
                .groupBy(TransactionTagCrossRef::transactionId)
                .mapValues { entry: Map.Entry<String, List<TransactionTagCrossRef>> ->
                    entry.value.map(TransactionTagCrossRef::tagId).sorted()
                }
        return FinanceBackup(
            CURRENT_BACKUP_SCHEMA_VERSION,
            BackupSettings(settings.currency),
            database.accountDao().loadAccounts().map { account: AccountEntity -> account.toBackup() },
            database.categoryTagDao().loadCategories().map { category: CategoryEntity -> category.toBackup() },
            database.categoryTagDao().loadTags().map { tag: TagEntity -> tag.toBackup() },
            database.transactionDao().loadAll().map { transaction: TransactionEntity ->
                transaction.toBackup(tagsByTransaction[transaction.id].orEmpty())
            },
            database.transferDao().loadAll().map { transfer: TransferEntity -> transfer.toBackup() },
            database.budgetDao().loadAll().map { budget: BudgetEntity -> budget.toBackup() },
        )
    }

    private fun AccountEntity.toBackup(): BackupAccount =
        BackupAccount(id, name, type, createdAt, updatedAt, openingBalanceMinor, openingBalanceDate, archived)

    private fun CategoryEntity.toBackup(): BackupCategory = BackupCategory(id, name, direction, archived, createdAt, updatedAt)

    private fun TagEntity.toBackup(): BackupTag = BackupTag(id, name, categoryId, archived, createdAt, updatedAt)

    private fun TransactionEntity.toBackup(tagIds: List<String>): BackupTransaction =
        BackupTransaction(id, accountId, amountMinor, direction, date, categoryId, tagIds, note, createdAt, updatedAt)

    private fun TransferEntity.toBackup(): BackupTransfer =
        BackupTransfer(id, sourceAccountId, destinationAccountId, amountMinor, date, createdAt, updatedAt)

    private fun BudgetEntity.toBackup(): BackupBudget = BackupBudget(id, categoryId, month, amountMinor, createdAt, updatedAt)
}
