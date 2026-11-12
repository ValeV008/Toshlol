package com.toshlol.personalfinance.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.toshlol.personalfinance.data.CategoryBudget
import com.toshlol.personalfinance.data.FinanceCategory
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState

@Composable
fun BudgetsScreen(
    state: TransactionsUiState,
    onBack: () -> Unit,
    onMonthChanged: (String) -> Unit,
    onCategoryChanged: (String?) -> Unit,
    onAmountChanged: (String) -> Unit,
    onSave: () -> Unit,
) {
    val monthBudgets: List<CategoryBudget> =
        state.budgets.filter { it.month.toString() == state.budgetMonth }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Budgets", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        OutlinedButton(onClick = onBack) { Text("Back to overview") }
        OutlinedTextField(state.budgetMonth, onMonthChanged, label = { Text("Month (YYYY-MM)") }, singleLine = true)
        Text("Monthly expense limits", style = MaterialTheme.typography.titleMedium)
        monthBudgets.forEach { budget: CategoryBudget ->
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    budget.categoryName + if (budget.isCategoryArchived) " · archived" else "",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text("Spent  ${state.currency} ${formatMoney(budget.spentMinor, state.currency)}")
                Text("Limit  ${state.currency} ${formatMoney(budget.limitMinor, state.currency)}")
                Text(
                    "${if (budget.isOverBudget) "Over budget" else "Remaining"}  ${state.currency} ${formatMoney(
                        if (budget.isOverBudget) -budget.remainingMinor else budget.remainingMinor,
                        state.currency,
                    )}",
                )
            }
        }
        if (monthBudgets.isEmpty()) Text("No budgets for this month.")
        Text("Set a budget", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        state.categories.filter { it.direction == TransactionDirection.EXPENSE && !it.archived }.forEach { category: FinanceCategory ->
            Row(Modifier.clickable { onCategoryChanged(category.id) }) {
                RadioButton(selected = state.budgetCategoryId == category.id, onClick = { onCategoryChanged(category.id) })
                Text(category.name)
            }
        }
        OutlinedTextField(state.budgetAmount, onAmountChanged, label = { Text("Budget amount") }, singleLine = true)
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = onSave) { Text("Save budget") }
        Text("Budgets do not roll over. Income is not budgeted.", style = MaterialTheme.typography.bodySmall)
    }
}
