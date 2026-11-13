package com.toshlol.personalfinance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.toshlol.personalfinance.data.FinanceTransaction
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun TransactionsScreen(
    state: TransactionsUiState,
    onCreate: (TransactionDirection) -> Unit,
    onEdit: (FinanceTransaction) -> Unit,
    onDelete: (String) -> Unit,
    onMonthChanged: (String) -> Unit,
    onAccountChanged: (String?) -> Unit,
    onCategoryChanged: (String?) -> Unit,
    onTagChanged: (String?) -> Unit,
) {
    val month: YearMonth? = runCatching { YearMonth.parse(state.selectedMonth) }.getOrNull()
    val transactions: List<FinanceTransaction> =
        state.transactions
            .filter { state.selectedMonth.isBlank() || month != null && YearMonth.from(LocalDate.ofEpochDay(it.date)) == month }
            .filter { state.selectedAccountId == null || it.accountId == state.selectedAccountId }
            .filter { state.selectedCategoryId == null || it.categoryId == state.selectedCategoryId }
            .filter { state.selectedTagId == null || state.selectedTagId in it.tagIds }
            .sortedWith(compareByDescending<FinanceTransaction> { it.date }.thenByDescending { it.createdAt }.thenByDescending { it.id })
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Transactions", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        OutlinedTextField(
            value = state.selectedMonth,
            onValueChange = onMonthChanged,
            label = { Text("Month (YYYY-MM, blank for all)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        FilterSelector("Account", state.accounts.map { it.id to it.name }, state.selectedAccountId, onAccountChanged)
        FilterSelector("Category", state.categories.map { it.id to it.name }, state.selectedCategoryId, onCategoryChanged)
        FilterSelector("Tag", state.tags.map { it.id to it.name }, state.selectedTagId, onTagChanged)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onCreate(TransactionDirection.EXPENSE) }) { Text("Add expense") }
            Button(onClick = { onCreate(TransactionDirection.INCOME) }) { Text("Add income") }
        }
        if (transactions.isEmpty()) {
            Text("No transactions match these filters.", modifier = Modifier.semantics { heading() })
            Text("Try another month, Account, Category, or Tag.")
        } else {
            transactions.forEach { transaction: FinanceTransaction ->
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    TextButton(onClick = { onEdit(transaction) }) {
                        Text(
                            "${transaction.direction.name.lowercase().replaceFirstChar(
                                Char::uppercase,
                            )} · ${state.currency} ${formatMoney(transaction.amountMinor, state.currency)}",
                        )
                    }
                    Text(
                        "${LocalDate.ofEpochDay(transaction.date)} · ${transaction.accountName}" +
                            transaction.categoryName?.let { " · $it" }.orEmpty() +
                            transaction.tagNames
                                .joinToString(prefix = " · ")
                                .takeIf { transaction.tagNames.isNotEmpty() }
                                .orEmpty() +
                            transaction.note?.let { " · $it" }.orEmpty(),
                    )
                    TextButton(onClick = { onDelete(transaction.id) }) { Text("Delete") }
                }
            }
        }
    }
}

@Composable
private fun FilterSelector(
    label: String,
    options: List<Pair<String, String>>,
    selectedId: String?,
    onSelected: (String?) -> Unit,
) {
    var isExpanded: Boolean by remember { mutableStateOf(false) }
    val selectedName: String = options.firstOrNull { it.first == selectedId }?.second ?: "All $label"
    Column {
        TextButton(onClick = { isExpanded = true }) { Text("$label: $selectedName") }
        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            DropdownMenuItem(text = { Text("All $label") }, onClick = {
                onSelected(null)
                isExpanded = false
            })
            options.forEach { option: Pair<String, String> ->
                DropdownMenuItem(text = { Text(option.second) }, onClick = {
                    onSelected(option.first)
                    isExpanded = false
                })
            }
        }
    }
}
