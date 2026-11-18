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
import com.toshlol.personalfinance.data.RecurringBudgetProgress

internal data class BudgetProgressCardState(
    val title: String,
    val accountIds: Set<String>,
    val spentMinor: Long,
    val limitMinor: Long,
    val isOverBudget: Boolean,
    val subtitle: String? = null,
    val titleTestTag: String? = null,
    val editTestTag: String? = null,
)

internal fun RecurringBudgetProgress.toBudgetProgressCardState(): BudgetProgressCardState =
    BudgetProgressCardState(
        title = name,
        accountIds = accountIds,
        spentMinor = spentMinor,
        limitMinor = limitMinor,
        isOverBudget = isOverBudget,
        titleTestTag = "budget-card-title-$id",
        editTestTag = "budget-edit-$id",
    )

internal fun CategoryBudget.toBudgetProgressCardState(): BudgetProgressCardState =
    BudgetProgressCardState(
        title = categoryName,
        accountIds = accountIds,
        spentMinor = spentMinor,
        limitMinor = limitMinor,
        isOverBudget = isOverBudget,
        subtitle = "Legacy monthly Budget",
    )

@Composable
internal fun BudgetProgressCard(
    state: BudgetProgressCardState,
    accounts: List<Account>,
    currency: String,
    onEdit: (() -> Unit)? = null,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            val titleModifier: Modifier =
                state.titleTestTag?.let { Modifier.testTag(it) } ?: Modifier
            Text(state.title, style = MaterialTheme.typography.titleMedium, modifier = titleModifier.semantics { heading() })
            state.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text("Accounts: ${formatBudgetAccountNames(state.accountIds, accounts)}", style = MaterialTheme.typography.bodyMedium)
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
