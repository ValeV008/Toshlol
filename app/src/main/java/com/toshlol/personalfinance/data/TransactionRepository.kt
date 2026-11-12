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

    @Query("SELECT * FROM categories")
    suspend fun loadCategories(): List<CategoryEntity>

    @Query("SELECT * FROM tags")
    suspend fun loadTags(): List<TagEntity>

    @Query("SELECT * FROM transaction_tags")
    suspend fun loadAllTransactionTags(): List<TransactionTagCrossRef>
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
                val category: CategoryEntity? = draft.categoryId?.let { id -> database.categoryTagDao().loadCategory(id) }
                val transactionId: String = existing?.id ?: nextIdentifier()
                val oldTags: Set<String> =
                    existing
                        ?.let {
                            database
                                .categoryTagDao()
                                .loadTransactionTags(it.id)
                                .map(TransactionTagCrossRef::tagId)
                                .toSet()
                        }.orEmpty()
                require(
                    draft.categoryId == null ||
                        category?.direction == draft.direction.name &&
                        (!category.archived || category.id == existing?.let { it.categoryId }),
                ) {
                    "Choose an active category that matches the transaction direction."
                }
                val categories: List<CategoryEntity> = database.categoryTagDao().loadCategories()
                val tagsById: Map<String, TagEntity> = database.categoryTagDao().loadTags().associateBy(TagEntity::id)
                require(
                    draft.tagIds.all { tagId ->
                        tagsById[tagId] != null && (!tagsById.getValue(tagId).archived || tagId in oldTags)
                    },
                ) {
                    "Choose active tags."
                }
                val compatibleTagIds: Set<String> =
                    draft.tagIds
                        .filter { tagId ->
                            val tag: TagEntity? = tagsById[tagId]
                            tag?.categoryId == null || tag?.categoryId == draft.categoryId
                        }.filter { tagId ->
                            val tag: TagEntity = tagsById.getValue(tagId)
                            tag.categoryId == null ||
                                categories.firstOrNull { it.id == tag.categoryId }?.archived == false ||
                                tagId in oldTags
                        }.toSet()
                database.transactionDao().save(
                    TransactionEntity(
                        id = transactionId,
                        accountId = draft.accountId,
                        amountMinor = draft.amountMinor,
                        direction = draft.direction.name,
                        date = draft.date,
                        categoryId = draft.categoryId,
                        note = draft.note?.trim()?.takeIf(String::isNotEmpty),
                        createdAt = existing?.createdAt ?: timestamp,
                        updatedAt = timestamp,
                    ),
                )
                database.categoryTagDao().clearTransactionTags(transactionId)
                compatibleTagIds.forEach { tagId ->
                    database.categoryTagDao().addTransactionTag(TransactionTagCrossRef(transactionId, tagId))
                }
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
        val categoriesById: Map<String, CategoryEntity> = database.categoryTagDao().loadCategories().associateBy(CategoryEntity::id)
        val tagsById: Map<String, TagEntity> = database.categoryTagDao().loadTags().associateBy(TagEntity::id)
        val transactionTags: Map<String, List<String>> =
            database
                .transactionDao()
                .loadAllTransactionTags()
                .groupBy(
                    TransactionTagCrossRef::transactionId,
                ).mapValues { entry ->
                    entry.value.map(TransactionTagCrossRef::tagId)
                }
        return database.transactionDao().loadAll().map { entity: TransactionEntity ->
            val tagIds: List<String> = transactionTags[entity.id].orEmpty()
            FinanceTransaction(
                entity.id,
                entity.accountId,
                accountNames[entity.accountId].orEmpty(),
                entity.amountMinor,
                TransactionDirection.valueOf(entity.direction),
                entity.date,
                entity.categoryId,
                entity.categoryId?.let { categoriesById[it]?.name },
                tagIds,
                tagIds.mapNotNull { tagsById[it]?.name },
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
