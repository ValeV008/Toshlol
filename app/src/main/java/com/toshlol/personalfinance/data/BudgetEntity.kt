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

data class CategoryBudget(
    val id: String,
    val categoryId: String,
    val categoryName: String,
    val isCategoryArchived: Boolean,
    val month: YearMonth,
    val limitMinor: Long,
    val spentMinor: Long,
) {
    val remainingMinor: Long get() = limitMinor - spentMinor
    val isOverBudget: Boolean get() = spentMinor > limitMinor
}

data class BudgetDraft(
    val categoryId: String,
    val month: YearMonth,
    val amountMinor: Long,
)
