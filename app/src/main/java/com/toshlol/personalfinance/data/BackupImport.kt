package com.toshlol.personalfinance.data

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.time.YearMonth

enum class BackupImportMode { MERGE, OVERWRITE }

enum class BackupEntityType { ACCOUNT, CATEGORY, TAG, TRANSACTION, TRANSFER, BUDGET }

data class BackupEntityCounts(
    val inserted: Int = 0,
    val updated: Int = 0,
    val unchanged: Int = 0,
)

data class BackupImportReport(
    val counts: Map<BackupEntityType, BackupEntityCounts>,
)

data class BackupImportFailure(
    val code: String,
    val message: String,
)

sealed interface BackupImportOutcome {
    data class Success(
        val report: BackupImportReport,
    ) : BackupImportOutcome

    data class Failure(
        val failures: List<BackupImportFailure>,
    ) : BackupImportOutcome
}

fun interface BackupDocumentReader {
    suspend fun read(source: Uri): ByteArray
}

class ContentResolverBackupReader(
    private val contentResolver: ContentResolver,
) : BackupDocumentReader {
    override suspend fun read(source: Uri): ByteArray =
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            val input: InputStream = requireNotNull(contentResolver.openInputStream(source)) { "The selected Backup could not be opened." }
            input.use { stream ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8_192)
                var totalBytes = 0
                while (totalBytes < MAX_BACKUP_BYTES + 1) {
                    val bytesToRead = minOf(buffer.size, MAX_BACKUP_BYTES + 1 - totalBytes)
                    val bytesRead = stream.read(buffer, 0, bytesToRead)
                    if (bytesRead < 0) break
                    output.write(buffer, 0, bytesRead)
                    totalBytes += bytesRead
                }
                val bytes = output.toByteArray()
                require(bytes.size <= MAX_BACKUP_BYTES) { "Backup exceeds the maximum file size." }
                bytes
            }
        }
}

class RoomBackupImporter(
    private val database: AppDatabase,
    private val beforeCommit: suspend () -> Unit = {},
) {
    suspend fun import(
        contents: ByteArray,
        mode: BackupImportMode = BackupImportMode.MERGE,
    ): BackupImportOutcome {
        if (contents.size > MAX_BACKUP_BYTES) return failure("file_too_large", "Backup exceeds the maximum file size.")
        val backup: FinanceBackup =
            try {
                val decoder =
                    StandardCharsets.UTF_8
                        .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                FinanceBackupCodec.decode(decoder.decode(ByteBuffer.wrap(contents)).toString()).also(::validateBackup)
            } catch (_: Exception) {
                return failure("invalid_backup", "Backup is malformed, unsupported, or invalid.")
            }
        return try {
            currentCoroutineContext().ensureActive()
            withContext(NonCancellable) {
                database.withTransaction {
                    val current: FinanceBackup? = loadCurrentBackup()
                    if (mode == BackupImportMode.MERGE && current != null && current.settings.currency != backup.settings.currency) {
                        throw ImportRejected("currency_mismatch", "Backup currency does not match the local ledger.")
                    }
                    val merged: FinanceBackup =
                        if (mode == BackupImportMode.OVERWRITE ||
                            current == null
                        ) {
                            backup
                        } else {
                            merge(current, backup)
                        }
                    validateBackup(merged)
                    writeBackup(merged, mode)
                    beforeCommit()
                    report(current, merged, mode)
                }
            }.let(BackupImportOutcome::Success)
        } catch (exception: ImportRejected) {
            failure(exception.code, exception.message ?: "Backup could not be imported.")
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            failure("storage_failure", "Backup could not be imported. Local data was not changed.")
        }
    }

    private suspend fun loadCurrentBackup(): FinanceBackup? {
        val settingsRows = database.settingsDao().loadAll()
        if (settingsRows.isEmpty()) {
            val hasLocalData =
                database.accountDao().loadAccounts().isNotEmpty() ||
                    database.categoryTagDao().loadCategories().isNotEmpty() ||
                    database.categoryTagDao().loadTags().isNotEmpty() ||
                    database.transactionDao().loadAll().isNotEmpty() ||
                    database.transferDao().loadAll().isNotEmpty() ||
                    database.budgetDao().loadAll().isNotEmpty()
            if (hasLocalData) throw ImportRejected("local_data_invalid", "Local ledger settings are missing.")
            return null
        }
        val settings = settingsRows.singleOrNull() ?: throw ImportRejected("local_data_invalid", "Local ledger settings are invalid.")
        val links = database.transactionDao().loadAllTransactionTags().groupBy(TransactionTagCrossRef::transactionId)
        return FinanceBackup(
            CURRENT_BACKUP_SCHEMA_VERSION,
            BackupSettings(settings.currency),
            database.accountDao().loadAccounts().map {
                BackupAccount(
                    it.id,
                    it.name,
                    it.type,
                    it.createdAt,
                    it.updatedAt,
                    it.openingBalanceMinor,
                    it.openingBalanceDate,
                    it.archived,
                )
            },
            database.categoryTagDao().loadCategories().map {
                BackupCategory(
                    it.id,
                    it.name,
                    it.direction,
                    it.archived,
                    it.createdAt,
                    it.updatedAt,
                )
            },
            database.categoryTagDao().loadTags().map { BackupTag(it.id, it.name, it.categoryId, it.archived, it.createdAt, it.updatedAt) },
            database.transactionDao().loadAll().map {
                BackupTransaction(
                    it.id,
                    it.accountId,
                    it.amountMinor,
                    it.direction,
                    it.date,
                    it.categoryId,
                    links[it.id].orEmpty().map(TransactionTagCrossRef::tagId).sorted(),
                    it.note,
                    it.createdAt,
                    it.updatedAt,
                )
            },
            database.transferDao().loadAll().map {
                BackupTransfer(it.id, it.sourceAccountId, it.destinationAccountId, it.amountMinor, it.date, it.createdAt, it.updatedAt)
            },
            database.budgetDao().loadAll().map { BackupBudget(it.id, it.categoryId, it.month, it.amountMinor, it.createdAt, it.updatedAt) },
        )
    }

    private fun merge(
        local: FinanceBackup,
        imported: FinanceBackup,
    ): FinanceBackup {
        fun <T> reconcile(
            localItems: List<T>,
            importedItems: List<T>,
            id: (T) -> String,
            updated: (T) -> Long,
        ): List<T> {
            val records = localItems.associateBy(id).toMutableMap()
            importedItems.forEach { incoming ->
                val existing = records[id(incoming)]
                if (existing == null || updated(incoming) > updated(existing)) {
                    records[id(incoming)] = incoming
                } else if (updated(incoming) == updated(existing) && incoming != existing) {
                    throw ImportRejected("timestamp_conflict", "Records with equal update times have different content.")
                }
            }
            return records.values.toList()
        }
        return FinanceBackup(
            CURRENT_BACKUP_SCHEMA_VERSION,
            local.settings,
            reconcile(local.accounts, imported.accounts, BackupAccount::id, BackupAccount::updatedAt),
            reconcile(local.categories, imported.categories, BackupCategory::id, BackupCategory::updatedAt),
            reconcile(local.tags, imported.tags, BackupTag::id, BackupTag::updatedAt),
            reconcile(local.transactions, imported.transactions, BackupTransaction::id, BackupTransaction::updatedAt),
            reconcile(local.transfers, imported.transfers, BackupTransfer::id, BackupTransfer::updatedAt),
            reconcile(local.budgets, imported.budgets, BackupBudget::id, BackupBudget::updatedAt),
        )
    }

    private fun validateBackup(backup: FinanceBackup) {
        val records: Int =
            backup.accounts.size + backup.categories.size + backup.tags.size + backup.transactions.size + backup.transfers.size +
                backup.budgets.size
        require(records <= MAX_BACKUP_RECORDS) { "Backup contains too many records." }
        require(backup.settings.currency.matches(Regex("[A-Z]{3}"))) { "Backup currency is invalid." }
        val all =
            backup.accounts.map(BackupAccount::id) + backup.categories.map(BackupCategory::id) + backup.tags.map(BackupTag::id) +
                backup.transactions.map(BackupTransaction::id) +
                backup.transfers.map(BackupTransfer::id) +
                backup.budgets.map(BackupBudget::id)
        require(all.all(String::isNotBlank) && all.distinct().size == all.size) { "Backup contains duplicate or empty IDs." }
        requireUniqueNames(backup.accounts.filterNot(BackupAccount::archived).map(BackupAccount::name), "Account")
        requireUniqueNames(backup.categories.filterNot(BackupCategory::archived).map { "${it.direction}:${it.name}" }, "Category")
        requireUniqueNames(backup.tags.filterNot(BackupTag::archived).map(BackupTag::name), "Tag")
        val accounts = backup.accounts.associateBy(BackupAccount::id)
        val categories = backup.categories.associateBy(BackupCategory::id)
        val tags = backup.tags.associateBy(BackupTag::id)
        backup.accounts.forEach {
            require(
                it.name.isNotBlank() && it.type in AccountType.entries.map(AccountType::name) && it.updatedAt >= it.createdAt,
            ) { "Backup contains an invalid Account." }
        }
        backup.categories.forEach {
            require(
                it.name.isNotBlank() &&
                    it.direction in TransactionDirection.entries.map(TransactionDirection::name) &&
                    it.updatedAt >= it.createdAt,
            ) { "Backup contains an invalid Category." }
        }
        backup.tags.forEach {
            require(
                it.name.isNotBlank() && (it.categoryId == null || it.categoryId in categories) && it.updatedAt >= it.createdAt,
            ) { "Backup contains an invalid Tag." }
        }
        backup.transactions.forEach { transaction ->
            require(
                transaction.accountId in accounts &&
                    transaction.amountMinor > 0 &&
                    transaction.direction in TransactionDirection.entries.map(TransactionDirection::name) &&
                    transaction.updatedAt >= transaction.createdAt,
            ) { "Backup contains an invalid Transaction." }
            require(transaction.categoryId == null || categories[transaction.categoryId]?.direction == transaction.direction) {
                "Backup Transaction has a missing or incompatible Category."
            }
            require(
                transaction.tagIds.distinct().size == transaction.tagIds.size &&
                    transaction.tagIds.all { tagId ->
                        tags[tagId] != null &&
                            (tags[tagId]?.categoryId == null || tags[tagId]?.categoryId == transaction.categoryId)
                    },
            ) { "Backup Transaction has missing or incompatible Tags." }
        }
        backup.transfers.forEach {
            require(
                it.sourceAccountId in accounts &&
                    it.destinationAccountId in accounts &&
                    it.sourceAccountId != it.destinationAccountId &&
                    it.amountMinor > 0 &&
                    it.updatedAt >= it.createdAt,
            ) { "Backup contains an invalid Transfer." }
        }
        backup.budgets.forEach {
            require(
                categories[it.categoryId]?.direction == TransactionDirection.EXPENSE.name &&
                    it.amountMinor > 0 &&
                    it.updatedAt >= it.createdAt &&
                    runCatching { YearMonth.parse(it.month) }.isSuccess,
            ) { "Backup contains an invalid Budget." }
        }
        require(
            backup.budgets
                .map {
                    "${it.categoryId}:${it.month}"
                }.distinct()
                .size == backup.budgets.size,
        ) { "Backup contains duplicate monthly Budgets." }
    }

    private fun requireUniqueNames(
        names: List<String>,
        entity: String,
    ) {
        require(
            names.all { it.isNotBlank() && it == it.trim() } &&
                names.map(String::lowercase).distinct().size == names.size,
        ) {
            "Backup contains conflicting active $entity names."
        }
    }

    private suspend fun writeBackup(
        backup: FinanceBackup,
        mode: BackupImportMode,
    ) {
        if (backup.schemaVersion != CURRENT_BACKUP_SCHEMA_VERSION) error("Unsupported Backup schema.")
        if (mode == BackupImportMode.OVERWRITE) {
            database.categoryTagDao().clearAllTransactionTags()
            database.budgetDao().clear()
            database.transferDao().clear()
            database.transactionDao().clear()
            database.categoryTagDao().clearTags()
            database.categoryTagDao().clearCategories()
            database.accountDao().clear()
        }
        database.settingsDao().saveSettings(AppSettings(currency = backup.settings.currency))
        backup.accounts.forEach {
            database.accountDao().saveAccount(
                AccountEntity(
                    it.id,
                    it.name,
                    it.type,
                    it.createdAt,
                    it.updatedAt,
                    it.openingBalanceMinor,
                    it.openingBalanceDate,
                    it.archived,
                ),
            )
        }
        backup.categories.forEach {
            database.categoryTagDao().saveCategory(CategoryEntity(it.id, it.name, it.direction, it.archived, it.createdAt, it.updatedAt))
        }
        backup.tags.forEach {
            database.categoryTagDao().saveTag(
                TagEntity(it.id, it.name, it.categoryId, it.archived, it.createdAt, it.updatedAt),
            )
        }
        backup.transactions.forEach { transaction ->
            database.transactionDao().save(
                TransactionEntity(
                    transaction.id,
                    transaction.accountId,
                    transaction.amountMinor,
                    transaction.direction,
                    transaction.date,
                    transaction.categoryId,
                    transaction.note,
                    transaction.createdAt,
                    transaction.updatedAt,
                ),
            )
            database.categoryTagDao().clearTransactionTags(transaction.id)
            transaction.tagIds.forEach { database.categoryTagDao().addTransactionTag(TransactionTagCrossRef(transaction.id, it)) }
        }
        backup.transfers.forEach {
            database.transferDao().save(
                TransferEntity(it.id, it.sourceAccountId, it.destinationAccountId, it.amountMinor, it.date, it.createdAt, it.updatedAt),
            )
        }
        backup.budgets.forEach {
            database.budgetDao().save(BudgetEntity(it.id, it.categoryId, it.month, it.amountMinor, it.createdAt, it.updatedAt))
        }
    }

    private fun report(
        current: FinanceBackup?,
        result: FinanceBackup,
        mode: BackupImportMode,
    ): BackupImportReport {
        fun <T : Any> counts(
            old: List<T>,
            new: List<T>,
        ): BackupEntityCounts {
            val previous = old.associateBy { recordId(it) }
            val inserted = new.count { recordId(it) !in previous }
            val updated = new.count { previous[recordId(it)] != null && previous[recordId(it)] != it }
            return BackupEntityCounts(inserted, updated, new.size - inserted - updated)
        }
        val previous =
            current
                ?: FinanceBackup(
                    CURRENT_BACKUP_SCHEMA_VERSION,
                    result.settings,
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                )
        if (mode == BackupImportMode.OVERWRITE) {
            return BackupImportReport(
                mapOf(
                    BackupEntityType.ACCOUNT to BackupEntityCounts(inserted = result.accounts.size),
                    BackupEntityType.CATEGORY to BackupEntityCounts(inserted = result.categories.size),
                    BackupEntityType.TAG to BackupEntityCounts(inserted = result.tags.size),
                    BackupEntityType.TRANSACTION to BackupEntityCounts(inserted = result.transactions.size),
                    BackupEntityType.TRANSFER to BackupEntityCounts(inserted = result.transfers.size),
                    BackupEntityType.BUDGET to BackupEntityCounts(inserted = result.budgets.size),
                ),
            )
        }
        return BackupImportReport(
            mapOf(
                BackupEntityType.ACCOUNT to counts(previous.accounts, result.accounts),
                BackupEntityType.CATEGORY to counts(previous.categories, result.categories),
                BackupEntityType.TAG to counts(previous.tags, result.tags),
                BackupEntityType.TRANSACTION to counts(previous.transactions, result.transactions),
                BackupEntityType.TRANSFER to counts(previous.transfers, result.transfers),
                BackupEntityType.BUDGET to counts(previous.budgets, result.budgets),
            ),
        )
    }

    private fun recordId(record: Any): String =
        when (record) {
            is BackupAccount -> record.id
            is BackupCategory -> record.id
            is BackupTag -> record.id
            is BackupTransaction -> record.id
            is BackupTransfer -> record.id
            is BackupBudget -> record.id
            else -> error("Unsupported Backup record.")
        }

    private fun failure(
        code: String,
        message: String,
    ) = BackupImportOutcome.Failure(listOf(BackupImportFailure(code, message)))

    private class ImportRejected(
        val code: String,
        override val message: String,
    ) : IllegalArgumentException(message)
}
