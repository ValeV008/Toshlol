package com.toshlol.personalfinance.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.withTransaction
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Entity(tableName = "recurring_budgets")
data class RecurringBudgetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val limitMinor: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "recurring_budget_accounts",
    primaryKeys = ["budgetId", "accountId"],
    foreignKeys = [
        ForeignKey(
            entity = RecurringBudgetEntity::class,
            parentColumns = ["id"],
            childColumns = ["budgetId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("accountId")],
)
data class RecurringBudgetAccountEntity(
    val budgetId: String,
    val accountId: String,
)

@Entity(
    tableName = "recurring_budget_categories",
    primaryKeys = ["budgetId", "categoryId"],
    foreignKeys = [
        ForeignKey(
            entity = RecurringBudgetEntity::class,
            parentColumns = ["id"],
            childColumns = ["budgetId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("categoryId")],
)
data class RecurringBudgetCategoryEntity(
    val budgetId: String,
    val categoryId: String,
)

@Entity(
    tableName = "recurring_budget_tags",
    primaryKeys = ["budgetId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = RecurringBudgetEntity::class,
            parentColumns = ["id"],
            childColumns = ["budgetId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("tagId")],
)
data class RecurringBudgetTagEntity(
    val budgetId: String,
    val tagId: String,
)

@Dao
interface RecurringBudgetDao {
    @Query("SELECT * FROM recurring_budgets ORDER BY name COLLATE NOCASE, id")
    suspend fun loadAll(): List<RecurringBudgetEntity>

    @Query("SELECT * FROM recurring_budgets WHERE id = :id LIMIT 1")
    suspend fun load(id: String): RecurringBudgetEntity?

    @Query("SELECT accountId FROM recurring_budget_accounts WHERE budgetId = :budgetId ORDER BY accountId")
    suspend fun loadAccountIds(budgetId: String): List<String>

    @Query("SELECT categoryId FROM recurring_budget_categories WHERE budgetId = :budgetId ORDER BY categoryId")
    suspend fun loadCategoryIds(budgetId: String): List<String>

    @Query("SELECT tagId FROM recurring_budget_tags WHERE budgetId = :budgetId ORDER BY tagId")
    suspend fun loadTagIds(budgetId: String): List<String>

    @Upsert
    suspend fun save(budget: RecurringBudgetEntity)

    @Query("DELETE FROM recurring_budget_accounts WHERE budgetId = :budgetId")
    suspend fun clearAccountIds(budgetId: String)

    @Upsert
    suspend fun saveAccount(account: RecurringBudgetAccountEntity)

    @Query("DELETE FROM recurring_budget_categories WHERE budgetId = :budgetId")
    suspend fun clearCategoryIds(budgetId: String)

    @Upsert
    suspend fun saveCategory(category: RecurringBudgetCategoryEntity)

    @Query("DELETE FROM recurring_budget_tags WHERE budgetId = :budgetId")
    suspend fun clearTagIds(budgetId: String)

    @Upsert
    suspend fun saveTag(tag: RecurringBudgetTagEntity)

    @Query("DELETE FROM recurring_budgets")
    suspend fun clear()
}

data class RecurringBudgetDraft(
    val id: String?,
    val name: String,
    val limitMinor: Long,
    val accountIds: Set<String>,
    val categoryIds: Set<String> = emptySet(),
    val tagIds: Set<String> = emptySet(),
) {
    init {
        require(name.isNotBlank()) { "Enter a Budget name." }
        require(limitMinor > 0) { "Enter a positive Budget limit." }
        require(accountIds.isNotEmpty()) { "Choose at least one Account." }
    }
}

data class RecurringBudgetProgress(
    val id: String,
    val name: String,
    val month: YearMonth,
    val accountIds: Set<String>,
    val limitMinor: Long,
    val spentMinor: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val categoryIds: Set<String> = emptySet(),
    val tagIds: Set<String> = emptySet(),
) {
    val remainingMinor: Long get() = limitMinor - spentMinor
    val isOverBudget: Boolean get() = spentMinor > limitMinor
}

interface RecurringBudgetRepository {
    suspend fun load(month: YearMonth): List<RecurringBudgetProgress>

    suspend fun save(
        draft: RecurringBudgetDraft,
        month: YearMonth,
    ): List<RecurringBudgetProgress>
}

class RoomRecurringBudgetRepository(
    private val database: AppDatabase,
    private val nextIdentifier: () -> String = { UUID.randomUUID().toString() },
    private val currentTime: () -> Long = System::currentTimeMillis,
) : RecurringBudgetRepository {
    override suspend fun load(month: YearMonth): List<RecurringBudgetProgress> =
        executeStorageOperation {
            database.withTransaction { loadInsideTransaction(month) }
        }

    override suspend fun save(
        draft: RecurringBudgetDraft,
        month: YearMonth,
    ): List<RecurringBudgetProgress> =
        executeStorageOperation {
            database.withTransaction {
                val accountIds: List<String> = database.accountDao().loadAccounts().map(AccountEntity::id)
                require(accountIds.containsAll(draft.accountIds)) { "Choose Accounts that still exist." }
                val categoriesById: Map<String, CategoryEntity> = database.categoryTagDao().loadCategories().associateBy(CategoryEntity::id)
                require(
                    draft.categoryIds.all { categoryId -> categoriesById[categoryId]?.direction == TransactionDirection.EXPENSE.name },
                ) { "Choose existing Expense Categories." }
                val tagsById: Map<String, TagEntity> = database.categoryTagDao().loadTags().associateBy(TagEntity::id)
                require(draft.tagIds.all { tagId -> tagId in tagsById }) { "Choose Tags that still exist." }
                val existing: RecurringBudgetEntity? = draft.id?.let { database.recurringBudgetDao().load(it) }
                require(draft.id == null || existing != null) { "The Budget could not be found." }
                val identifier: String = existing?.id ?: nextIdentifier()
                val timestamp: Long = currentTime()
                database.recurringBudgetDao().save(
                    RecurringBudgetEntity(
                        identifier,
                        draft.name.trim(),
                        draft.limitMinor,
                        existing?.createdAt ?: timestamp,
                        timestamp,
                    ),
                )
                database.recurringBudgetDao().clearAccountIds(identifier)
                draft.accountIds.sorted().forEach { accountId ->
                    database.recurringBudgetDao().saveAccount(RecurringBudgetAccountEntity(identifier, accountId))
                }
                database.recurringBudgetDao().clearCategoryIds(identifier)
                draft.categoryIds.sorted().forEach { categoryId ->
                    database.recurringBudgetDao().saveCategory(RecurringBudgetCategoryEntity(identifier, categoryId))
                }
                database.recurringBudgetDao().clearTagIds(identifier)
                draft.tagIds.sorted().forEach { tagId ->
                    database.recurringBudgetDao().saveTag(RecurringBudgetTagEntity(identifier, tagId))
                }
                loadInsideTransaction(month)
            }
        }

    private suspend fun loadInsideTransaction(month: YearMonth): List<RecurringBudgetProgress> {
        val budgets: List<RecurringBudgetEntity> = database.recurringBudgetDao().loadAll()
        val accountIdsByBudget: Map<String, Set<String>> =
            budgets.associate { budget ->
                budget.id to database.recurringBudgetDao().loadAccountIds(budget.id).toSet()
            }
        val categoryIdsByBudget: Map<String, Set<String>> =
            budgets.associate { budget ->
                budget.id to database.recurringBudgetDao().loadCategoryIds(budget.id).toSet()
            }
        val tagIdsByBudget: Map<String, Set<String>> =
            budgets.associate { budget ->
                budget.id to database.recurringBudgetDao().loadTagIds(budget.id).toSet()
            }
        val tagIdsByTransaction: Map<String, Set<String>> =
            database.transactionDao().loadAllTransactionTags().groupBy(TransactionTagCrossRef::transactionId).mapValues { entry ->
                entry.value.map(TransactionTagCrossRef::tagId).toSet()
            }
        val expenses: List<TransactionEntity> =
            database.transactionDao().loadAll().filter { transaction ->
                transaction.direction == TransactionDirection.EXPENSE.name &&
                    YearMonth.from(LocalDate.ofEpochDay(transaction.date)) == month
            }
        return budgets.map { budget ->
            val accountIds: Set<String> = accountIdsByBudget.getValue(budget.id)
            val categoryIds: Set<String> = categoryIdsByBudget.getValue(budget.id)
            val tagIds: Set<String> = tagIdsByBudget.getValue(budget.id)
            val spentMinor: Long =
                expenses
                    .filter { transaction ->
                        transaction.accountId in accountIds &&
                            (categoryIds.isEmpty() || transaction.categoryId in categoryIds) &&
                            (tagIds.isEmpty() || tagIdsByTransaction[transaction.id].orEmpty().any(tagIds::contains))
                    }.sumOf(TransactionEntity::amountMinor)
            RecurringBudgetProgress(
                id = budget.id,
                name = budget.name,
                month = month,
                accountIds = accountIds,
                limitMinor = budget.limitMinor,
                spentMinor = spentMinor,
                createdAt = budget.createdAt,
                updatedAt = budget.updatedAt,
                categoryIds = categoryIds,
                tagIds = tagIds,
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
