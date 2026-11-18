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

@Dao
interface RecurringBudgetDao {
    @Query("SELECT * FROM recurring_budgets ORDER BY name COLLATE NOCASE, id")
    suspend fun loadAll(): List<RecurringBudgetEntity>

    @Query("SELECT * FROM recurring_budgets WHERE id = :id LIMIT 1")
    suspend fun load(id: String): RecurringBudgetEntity?

    @Query("SELECT accountId FROM recurring_budget_accounts WHERE budgetId = :budgetId ORDER BY accountId")
    suspend fun loadAccountIds(budgetId: String): List<String>

    @Upsert
    suspend fun save(budget: RecurringBudgetEntity)

    @Query("DELETE FROM recurring_budget_accounts WHERE budgetId = :budgetId")
    suspend fun clearAccountIds(budgetId: String)

    @Upsert
    suspend fun saveAccount(account: RecurringBudgetAccountEntity)

    @Query("DELETE FROM recurring_budgets")
    suspend fun clear()
}

data class RecurringBudgetDraft(
    val id: String?,
    val name: String,
    val limitMinor: Long,
    val accountIds: Set<String>,
)

data class RecurringBudgetProgress(
    val id: String,
    val name: String,
    val month: YearMonth,
    val accountIds: Set<String>,
    val limitMinor: Long,
    val spentMinor: Long,
    val createdAt: Long,
    val updatedAt: Long,
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
    ): List<RecurringBudgetProgress> {
        require(draft.name.isNotBlank()) { "Enter a Budget name." }
        require(draft.limitMinor > 0) { "Enter a positive Budget limit." }
        require(draft.accountIds.isNotEmpty()) { "Choose at least one Account." }
        return executeStorageOperation {
            database.withTransaction {
                val accountIds: List<String> = database.accountDao().loadAccounts().map(AccountEntity::id)
                require(accountIds.containsAll(draft.accountIds)) { "Choose Accounts that still exist." }
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
                loadInsideTransaction(month)
            }
        }
    }

    private suspend fun loadInsideTransaction(month: YearMonth): List<RecurringBudgetProgress> {
        val accountIdsByBudget: Map<String, Set<String>> =
            database.recurringBudgetDao().loadAll().associate { budget ->
                budget.id to database.recurringBudgetDao().loadAccountIds(budget.id).toSet()
            }
        val expenses: List<TransactionEntity> =
            database.transactionDao().loadAll().filter { transaction ->
                transaction.direction == TransactionDirection.EXPENSE.name &&
                    YearMonth.from(LocalDate.ofEpochDay(transaction.date)) == month
            }
        return database.recurringBudgetDao().loadAll().map { budget ->
            val accountIds: Set<String> = accountIdsByBudget.getValue(budget.id)
            val spentMinor: Long = expenses.filter { it.accountId in accountIds }.sumOf(TransactionEntity::amountMinor)
            RecurringBudgetProgress(
                budget.id,
                budget.name,
                month,
                accountIds,
                budget.limitMinor,
                spentMinor,
                budget.createdAt,
                budget.updatedAt,
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
