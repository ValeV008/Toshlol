@file:Suppress("ktlint:standard:filename")

package com.toshlol.personalfinance.ui

import com.toshlol.personalfinance.data.FinanceCategory
import com.toshlol.personalfinance.data.FinanceTag
import com.toshlol.personalfinance.data.FinanceTransaction

internal data class ClassificationReferences(
    val categories: List<FinanceCategory>,
    val tags: List<FinanceTag>,
)

internal fun formatTransactionCategory(
    transaction: FinanceTransaction,
    categories: List<FinanceCategory>,
): String? {
    val category: FinanceCategory? = transaction.categoryId?.let { id -> categories.firstOrNull { it.id == id } }
    val name: String? = category?.name ?: transaction.categoryName
    return name?.let { value -> value + if (category?.archived == true) " (Archived)" else "" }
}

internal fun formatTransactionTags(
    transaction: FinanceTransaction,
    tags: List<FinanceTag>,
): List<String> =
    transaction.tagIds.mapIndexedNotNull { index, id ->
        val tag: FinanceTag? = tags.firstOrNull { it.id == id }
        val name: String? = tag?.name ?: transaction.tagNames.getOrNull(index)
        name?.let { value -> value + if (tag?.archived == true) " (Archived)" else "" }
    }
