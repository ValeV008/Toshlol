package com.toshlol.personalfinance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.CategoryBudget
import com.toshlol.personalfinance.data.FinanceCategory
import com.toshlol.personalfinance.data.FinanceTag
import com.toshlol.personalfinance.data.RecurringBudgetProgress

internal data class BudgetScopeSummary(
    val categories: String? = null,
    val tags: String? = null,
)

internal data class BudgetProgressCardState(
    val title: String,
    val accountIds: Set<String>,
    val spentMinor: Long,
    val limitMinor: Long,
    val isOverBudget: Boolean,
    val subtitle: String? = null,
    val titleTestTag: String? = null,
    val editTestTag: String? = null,
    val scopeSummary: BudgetScopeSummary? = null,
)

internal fun RecurringBudgetProgress.toBudgetProgressCardState(
    categories: List<FinanceCategory>,
    tags: List<FinanceTag>,
): BudgetProgressCardState =
    BudgetProgressCardState(
        title = name,
        accountIds = accountIds,
        spentMinor = totals.spentMinor,
        limitMinor = totals.limitMinor,
        isOverBudget = totals.isOverBudget,
        titleTestTag = "budget-card-title-$id",
        editTestTag = "budget-edit-$id",
        scopeSummary =
            BudgetScopeSummary(
                categories = formatBudgetCategoryNames(restrictions.categoryIds, categories),
                tags = formatBudgetTagNames(restrictions.tagIds, tags),
            ),
    )

internal fun CategoryBudget.toBudgetProgressCardState(): BudgetProgressCardState =
    BudgetProgressCardState(
        title = categoryName,
        accountIds = accountIds,
        spentMinor = spentMinor,
        limitMinor = limitMinor,
        isOverBudget = isOverBudget,
        subtitle = "Legacy monthly Budget",
        scopeSummary = BudgetScopeSummary(categories = categoryName + if (isCategoryArchived) " (Archived)" else ""),
    )

@Composable
@Suppress("ktlint:standard:no-unit-return")
internal fun BudgetProgressCard(
    state: BudgetProgressCardState,
    accounts: List<Account>,
    currency: String,
    onEdit: (() -> Unit)? = null,
): Unit {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            val titleModifier: Modifier =
                state.titleTestTag?.let { Modifier.testTag(it) } ?: Modifier
            Text(state.title, style = MaterialTheme.typography.titleMedium, modifier = titleModifier.semantics { heading() })
            state.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text("Accounts: ${formatBudgetAccountNames(state.accountIds, accounts)}", style = MaterialTheme.typography.bodyMedium)
            state.scopeSummary?.categories?.let { Text("Categories: $it", style = MaterialTheme.typography.bodyMedium) }
            state.scopeSummary?.tags?.let { Text("Tags: $it", style = MaterialTheme.typography.bodyMedium) }
            Text("Spent  $currency ${formatMoney(state.spentMinor, currency)}")
            Text("Limit  $currency ${formatMoney(state.limitMinor, currency)}")
            Text("Remaining  $currency ${formatMoney(state.limitMinor - state.spentMinor, currency)}")
            if (state.isOverBudget) Text("Over budget", color = MaterialTheme.colorScheme.error)
            onEdit?.let { edit ->
                OutlinedButton(onClick = edit, modifier = Modifier.heightIn(min = 48.dp).testTag(state.editTestTag ?: "budget-edit")) {
                    Text("Edit Budget")
                }
            }
        }
    }
}

private fun formatBudgetAccountNames(
    accountIds: Set<String>,
    accounts: List<Account>,
): String =
    accountIds
        .sorted()
        .mapNotNull { accountId -> accounts.firstOrNull { it.id == accountId } }
        .joinToString(", ") { account -> account.name + if (account.isArchived) " (Archived)" else "" }
        .ifEmpty { "No Accounts" }

internal fun formatBudgetCategoryNames(
    categoryIds: Set<String>,
    categories: List<FinanceCategory>,
): String =
    categoryIds
        .map { categoryId -> categories.firstOrNull { it.id == categoryId } }
        .sortedBy { category -> category?.name?.lowercase().orEmpty() }
        .map { category ->
            category?.name?.let { name -> name + if (category.archived) " (Archived)" else "" } ?: "Unknown Category"
        }.joinToString(", ")
        .ifEmpty { "Any Expense Category" }

internal fun formatBudgetTagNames(
    tagIds: Set<String>,
    tags: List<FinanceTag>,
): String =
    tagIds
        .map { tagId -> tags.firstOrNull { it.id == tagId } }
        .sortedBy { tag -> tag?.name?.lowercase().orEmpty() }
        .map { tag ->
            tag?.name?.let { name -> name + if (tag.archived) " (Archived)" else "" } ?: "Unknown Tag"
        }.joinToString(", ")
        .ifEmpty { "No Tag Restriction" }
