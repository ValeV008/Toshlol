package com.toshlol.personalfinance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.withTransaction
import java.io.IOException
import java.util.UUID

@Dao
interface CategoryTagDao {
    @Query("SELECT * FROM categories ORDER BY direction, name COLLATE NOCASE, id")
    suspend fun loadCategories(): List<CategoryEntity>

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE, id")
    suspend fun loadTags(): List<TagEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun loadCategory(id: String): CategoryEntity?

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun loadTag(id: String): TagEntity?

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countTransactionsForCategory(categoryId: String): Int

    @Query("SELECT COUNT(*) FROM transaction_tags WHERE tagId = :tagId")
    suspend fun countTransactionsForTag(tagId: String): Int

    @Upsert
    suspend fun saveCategory(category: CategoryEntity)

    @Upsert
    suspend fun saveTag(tag: TagEntity)

    @Query("SELECT * FROM transaction_tags WHERE transactionId = :transactionId")
    suspend fun loadTransactionTags(transactionId: String): List<TransactionTagCrossRef>

    @Query("DELETE FROM transaction_tags WHERE transactionId = :transactionId")
    suspend fun clearTransactionTags(transactionId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTransactionTag(reference: TransactionTagCrossRef)
}

interface CategoryTagRepository {
    suspend fun load(): CategoryTagSnapshot

    suspend fun saveCategory(draft: CategoryDraft): CategoryTagSnapshot

    suspend fun saveTag(draft: TagDraft): CategoryTagSnapshot

    suspend fun archiveCategory(id: String): CategoryTagSnapshot

    suspend fun archiveTag(id: String): CategoryTagSnapshot
}

class RoomCategoryTagRepository(
    private val database: AppDatabase,
    private val nextIdentifier: () -> String = { UUID.randomUUID().toString() },
    private val currentTime: () -> Long = System::currentTimeMillis,
) : CategoryTagRepository {
    override suspend fun load(): CategoryTagSnapshot = executeStorageOperation { database.withTransaction { loadInsideTransaction() } }

    override suspend fun saveCategory(draft: CategoryDraft): CategoryTagSnapshot =
        executeStorageOperation {
            database.withTransaction {
                val name: String = validateName(draft.name)
                val categories: List<CategoryEntity> = database.categoryTagDao().loadCategories()
                val existing: CategoryEntity? = draft.id?.let { id -> categories.firstOrNull { it.id == id } }
                require(draft.id == null || existing != null) { "This category no longer exists." }
                require(
                    existing == null ||
                        existing.direction == draft.direction.name ||
                        database.categoryTagDao().countTransactionsForCategory(existing.id) == 0,
                ) {
                    "A category used by transactions cannot change its direction."
                }
                require(
                    categories.none {
                        it.id != draft.id &&
                            !it.archived &&
                            it.direction == draft.direction.name &&
                            it.name.equals(name, true)
                    },
                ) {
                    "An active category with this name already exists."
                }
                val timestamp: Long = currentTime()
                database.categoryTagDao().saveCategory(
                    CategoryEntity(
                        draft.id ?: nextIdentifier(),
                        name,
                        draft.direction.name,
                        existing?.archived ?: false,
                        existing?.createdAt ?: timestamp,
                        timestamp,
                    ),
                )
                loadInsideTransaction()
            }
        }

    override suspend fun saveTag(draft: TagDraft): CategoryTagSnapshot =
        executeStorageOperation {
            database.withTransaction {
                val name: String = validateName(draft.name)
                val tags: List<TagEntity> = database.categoryTagDao().loadTags()
                val existing: TagEntity? = draft.id?.let { id -> tags.firstOrNull { it.id == id } }
                require(draft.id == null || existing != null) { "This tag no longer exists." }
                require(
                    existing == null ||
                        existing.categoryId == draft.categoryId ||
                        database.categoryTagDao().countTransactionsForTag(existing.id) == 0,
                ) {
                    "A tag used by transactions cannot change its category."
                }
                require(tags.none { it.id != draft.id && !it.archived && it.name.equals(name, true) }) {
                    "An active tag with this name already exists."
                }
                val category: CategoryEntity? = draft.categoryId?.let { database.categoryTagDao().loadCategory(it) }
                require(draft.categoryId == null || category?.archived == false) { "Choose an active category for this tag." }
                val timestamp: Long = currentTime()
                database.categoryTagDao().saveTag(
                    TagEntity(
                        draft.id ?: nextIdentifier(),
                        name,
                        draft.categoryId,
                        existing?.archived ?: false,
                        existing?.createdAt ?: timestamp,
                        timestamp,
                    ),
                )
                loadInsideTransaction()
            }
        }

    override suspend fun archiveCategory(id: String): CategoryTagSnapshot =
        executeStorageOperation {
            database.withTransaction {
                val category: CategoryEntity = requireNotNull(database.categoryTagDao().loadCategory(id))
                database.categoryTagDao().saveCategory(category.copy(archived = true, updatedAt = currentTime()))
                loadInsideTransaction()
            }
        }

    override suspend fun archiveTag(id: String): CategoryTagSnapshot =
        executeStorageOperation {
            database.withTransaction {
                val tag: TagEntity = requireNotNull(database.categoryTagDao().loadTag(id))
                database.categoryTagDao().saveTag(tag.copy(archived = true, updatedAt = currentTime()))
                loadInsideTransaction()
            }
        }

    private suspend fun loadInsideTransaction(): CategoryTagSnapshot {
        val categories: List<CategoryEntity> = database.categoryTagDao().loadCategories()
        val namesById: Map<String, String> = categories.associate { it.id to it.name }
        return CategoryTagSnapshot(
            categories.map { FinanceCategory(it.id, it.name, TransactionDirection.valueOf(it.direction), it.archived) },
            database.categoryTagDao().loadTags().map {
                FinanceTag(
                    it.id,
                    it.name,
                    it.categoryId,
                    it.categoryId?.let(namesById::get),
                    it.archived,
                )
            },
        )
    }

    private fun validateName(rawName: String): String {
        val name: String = rawName.trim()
        require(name.isNotEmpty()) { "Enter a name." }
        return name
    }

    private suspend fun <T> executeStorageOperation(operation: suspend () -> T): T =
        try {
            operation()
        } catch (exception: android.database.sqlite.SQLiteException) {
            throw IOException("Unable to access local finance data.", exception)
        }
}
