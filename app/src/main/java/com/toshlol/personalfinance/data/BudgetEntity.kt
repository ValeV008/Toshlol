package com.toshlol.personalfinance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.YearMonth

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["categoryId", "month"], unique = true)],
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val month: String,
    val amountMinor: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "legacy_budget_accounts",
    primaryKeys = ["budgetId", "accountId"],
    foreignKeys = [
        ForeignKey(
            entity = BudgetEntity::class,
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
data class LegacyBudgetAccountEntity(
    val budgetId: String,
    val accountId: String,
)

data class CategoryBudget(
    val id: String,
    val categoryId: String,
    val categoryName: String,
    val isCategoryArchived: Boolean,
    val month: YearMonth,
    val limitMinor: Long,
    val spentMinor: Long,
    val accountIds: Set<String> = emptySet(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    val remainingMinor: Long get() = limitMinor - spentMinor
    val isOverBudget: Boolean get() = spentMinor > limitMinor
}

data class BudgetDraft(
    val categoryId: String,
    val month: YearMonth,
    val amountMinor: Long,
)
