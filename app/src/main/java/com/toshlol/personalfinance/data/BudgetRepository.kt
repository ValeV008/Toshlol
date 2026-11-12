package com.toshlol.personalfinance.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.withTransaction
import java.io.IOException
import java.time.YearMonth
import java.util.UUID

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY month DESC, categoryId")
    suspend fun loadAll(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND month = :month LIMIT 1")
    suspend fun load(
        categoryId: String,
        month: String,
    ): BudgetEntity?

    @Upsert
    suspend fun save(budget: BudgetEntity)
}

interface BudgetRepository {
    suspend fun load(): List<CategoryBudget>

    suspend fun save(draft: BudgetDraft): List<CategoryBudget>
}

class RoomBudgetRepository(
    private val database: AppDatabase,
    private val nextIdentifier: () -> String = { UUID.randomUUID().toString() },
    private val currentTime: () -> Long = System::currentTimeMillis,
) : BudgetRepository {
    override suspend fun load(): List<CategoryBudget> =
        executeStorageOperation {
            database.withTransaction { loadInsideTransaction() }
        }

    override suspend fun save(draft: BudgetDraft): List<CategoryBudget> {
        require(draft.amountMinor > 0) { "Enter a positive budget amount." }
        val monthKey: String = draft.month.toString()
        return executeStorageOperation {
            database.withTransaction {
                val category: CategoryEntity? = database.categoryTagDao().loadCategory(draft.categoryId)
                require(category != null && category.direction == TransactionDirection.EXPENSE.name && !category.archived) {
                    "Choose an active expense category."
                }
                val existing: BudgetEntity? = database.budgetDao().load(draft.categoryId, monthKey)
                val timestamp: Long = currentTime()
                database.budgetDao().save(
                    BudgetEntity(
                        existing?.id ?: nextIdentifier(),
                        draft.categoryId,
                        monthKey,
                        draft.amountMinor,
                        existing?.createdAt ?: timestamp,
                        timestamp,
                    ),
                )
                loadInsideTransaction()
            }
        }
    }

    private suspend fun loadInsideTransaction(): List<CategoryBudget> {
        val categories: Map<String, CategoryEntity> = database.categoryTagDao().loadCategories().associateBy(CategoryEntity::id)
        val expenses: List<TransactionEntity> =
            database.transactionDao().loadAll().filter {
                it.direction ==
                    TransactionDirection.EXPENSE.name
            }
        return database.budgetDao().loadAll().mapNotNull { budget: BudgetEntity ->
            val category: CategoryEntity = categories[budget.categoryId] ?: return@mapNotNull null
            val month: YearMonth = YearMonth.parse(budget.month)
            val spent: Long =
                expenses
                    .asSequence()
                    .filter { it.categoryId == budget.categoryId }
                    .filter { YearMonth.from(java.time.LocalDate.ofEpochDay(it.date)) == month }
                    .sumOf(TransactionEntity::amountMinor)
            CategoryBudget(budget.id, category.id, category.name, category.archived, month, budget.amountMinor, spent)
        }
    }

    private suspend fun <T> executeStorageOperation(operation: suspend () -> T): T =
        try {
            operation()
        } catch (exception: android.database.sqlite.SQLiteException) {
            throw IOException("Unable to access local finance data.", exception)
        }
}
