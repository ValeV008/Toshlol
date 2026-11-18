package com.toshlol.personalfinance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.CategoryBudget
import com.toshlol.personalfinance.data.RecurringBudgetProgress
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun BudgetsScreen(
    state: TransactionsUiState,
    onBack: () -> Unit,
    onMonthChanged: (String) -> Unit,
    onNameChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onAccountToggled: (String) -> Unit,
    onCreate: () -> Unit,
    onEdit: (RecurringBudgetProgress) -> Unit,
    onSave: () -> Unit,
    onAddTransaction: () -> Unit = {},
) {
    val selectedMonth: YearMonth? = runCatching { YearMonth.parse(state.budgetMonth) }.getOrNull()
    val legacyBudgets: List<CategoryBudget> = state.budgets.filter { it.month.toString() == state.budgetMonth }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Budgets", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(
                onClick = { selectedMonth?.minusMonths(1)?.let { onMonthChanged(it.toString()) } },
                enabled = selectedMonth != null,
                modifier = Modifier.heightIn(min = 48.dp).testTag("budget-previous-month"),
            ) { Text("Previous month") }
            Text(
                selectedMonth?.format(DateTimeFormatter.ofPattern("MMMM yyyy")).orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            OutlinedButton(
                onClick = { selectedMonth?.plusMonths(1)?.let { onMonthChanged(it.toString()) } },
                enabled = selectedMonth != null,
                modifier = Modifier.heightIn(min = 48.dp).testTag("budget-next-month"),
            ) { Text("Next month") }
        }
        Text("Monthly expense limits", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        state.recurringBudgets.forEach { budget -> RecurringBudgetCard(budget, state.accounts, state.currency, onEdit) }
        legacyBudgets.forEach { budget -> LegacyBudgetCard(budget, state.accounts, state.currency) }
        if (state.recurringBudgets.isEmpty() && legacyBudgets.isEmpty()) Text("No Budgets for this month.")
        OutlinedButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back to overview") }
        OutlinedButton(onClick = onAddTransaction, modifier = Modifier.heightIn(min = 48.dp)) { Text("Add transaction") }
        Button(onClick = onCreate, modifier = Modifier.heightIn(min = 48.dp).testTag("budget-create")) { Text("Create Budget") }
        Text(
            if (state.editingBudgetId == null) "New recurring Budget" else "Edit recurring Budget",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() }.testTag("budget-editor"),
        )
        OutlinedTextField(
            value = state.budgetName,
            onValueChange = onNameChanged,
            label = { Text("Budget name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("budget-name"),
        )
        OutlinedTextField(
            value = state.budgetAmount,
            onValueChange = onAmountChanged,
            label = { Text("Monthly limit (${state.currency})") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("budget-limit"),
        )
        Text("Accounts", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        state.accounts.forEach { account -> BudgetAccountChoice(account, account.id in state.budgetAccountIds, onAccountToggled) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("budget-save"),
        ) { Text(if (state.editingBudgetId == null) "Save Budget" else "Save changes") }
        Text("Budgets repeat each month. Only Expenses in selected Accounts count.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun RecurringBudgetCard(
    budget: RecurringBudgetProgress,
    accounts: List<Account>,
    currency: String,
    onEdit: (RecurringBudgetProgress) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(budget.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("budget-card-title-${budget.id}"))
            Text("Accounts: ${formatAccountNames(budget.accountIds, accounts)}", style = MaterialTheme.typography.bodyMedium)
            Text("Spent  $currency ${formatMoney(budget.spentMinor, currency)}")
            Text("Limit  $currency ${formatMoney(budget.limitMinor, currency)}")
            Text("Remaining  $currency ${formatMoney(budget.remainingMinor, currency)}")
            if (budget.isOverBudget) Text("Over budget", color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { onEdit(budget) }, modifier = Modifier.heightIn(min = 48.dp).testTag("budget-edit-${budget.id}")) {
                Text("Edit Budget")
            }
        }
    }
}

@Composable
private fun LegacyBudgetCard(
    budget: CategoryBudget,
    accounts: List<Account>,
    currency: String,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(budget.categoryName, style = MaterialTheme.typography.titleMedium)
            Text("Legacy monthly Budget", style = MaterialTheme.typography.bodySmall)
            Text("Accounts: ${formatAccountNames(budget.accountIds, accounts)}")
            Text("Spent  $currency ${formatMoney(budget.spentMinor, currency)}")
            Text("Limit  $currency ${formatMoney(budget.limitMinor, currency)}")
            Text("Remaining  $currency ${formatMoney(budget.remainingMinor, currency)}")
            if (budget.isOverBudget) Text("Over budget", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun BudgetAccountChoice(
    account: Account,
    isSelected: Boolean,
    onToggled: (String) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag("budget-account-${account.id}")
            .selectable(selected = isSelected, role = Role.Checkbox, onClick = { onToggled(account.id) })
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = isSelected, onCheckedChange = null)
        Text(account.name + if (account.isArchived) " · Archived" else "", modifier = Modifier.padding(start = 8.dp))
    }
}

private fun formatAccountNames(
    accountIds: Set<String>,
    accounts: List<Account>,
): String =
    accountIds
        .sorted()
        .mapNotNull { accountId -> accounts.firstOrNull { it.id == accountId } }
        .joinToString(", ") { account -> account.name + if (account.isArchived) " (Archived)" else "" }
        .ifEmpty { "No Accounts" }
