package com.toshlol.personalfinance

import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.CategoryDraft
import com.toshlol.personalfinance.data.FinanceCategory
import com.toshlol.personalfinance.data.FinanceTag
import com.toshlol.personalfinance.data.TagDraft
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.data.TransactionDraft
import java.time.LocalDate

internal data class BudgetTestExpense(
    val account: Account,
    val amountMinor: Long,
    val date: LocalDate,
    val categoryId: String? = null,
    val tagIds: Set<String> = emptySet(),
)

internal suspend fun saveExpenseCategory(
    fixtures: FinanceTestFixtures,
    name: String,
): FinanceCategory =
    fixtures.organization
        .saveCategory(CategoryDraft(name = name, direction = TransactionDirection.EXPENSE))
        .categories
        .single { it.name == name }

internal suspend fun saveBudgetTag(
    fixtures: FinanceTestFixtures,
    name: String,
): FinanceTag =
    fixtures.organization
        .saveTag(TagDraft(name = name))
        .tags
        .single { it.name == name }

@Suppress("ktlint:standard:no-unit-return")
internal suspend fun saveExpense(
    fixtures: FinanceTestFixtures,
    expense: BudgetTestExpense,
): Unit {
    fixtures.transactions.save(
        TransactionDraft(
            id = null,
            accountId = expense.account.id,
            amountMinor = expense.amountMinor,
            direction = TransactionDirection.EXPENSE,
            date = expense.date.toEpochDay(),
            note = null,
            categoryId = expense.categoryId,
            tagIds = expense.tagIds,
        ),
    )
}
