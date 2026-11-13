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
import com.toshlol.personalfinance.data.FinanceTransfer
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState
import java.time.LocalDate
import java.time.YearMonth

data class TransactionsScreenActions(
    val create: (TransactionDirection) -> Unit,
    val edit: (FinanceTransaction) -> Unit,
    val delete: (String) -> Unit,
    val monthChanged: (String) -> Unit,
    val accountChanged: (String?) -> Unit,
    val categoryChanged: (String?) -> Unit,
    val tagChanged: (String?) -> Unit,
    val createTransfer: () -> Unit,
    val editTransfer: (FinanceTransfer) -> Unit,
    val deleteTransfer: (String) -> Unit,
)

@Composable
@Suppress("ktlint:standard:no-unit-return")
fun TransactionsScreen(
    state: TransactionsUiState,
    actions: TransactionsScreenActions,
): Unit {
    val month: YearMonth? = runCatching { YearMonth.parse(state.selectedMonth) }.getOrNull()
    val transactions: List<FinanceTransaction> =
        state.transactions
            .filter { state.selectedMonth.isBlank() || month != null && YearMonth.from(LocalDate.ofEpochDay(it.date)) == month }
            .filter { state.selectedAccountId == null || it.accountId == state.selectedAccountId }
            .filter { state.selectedCategoryId == null || it.categoryId == state.selectedCategoryId }
            .filter { state.selectedTagId == null || state.selectedTagId in it.tagIds }
            .sortedWith(compareByDescending<FinanceTransaction> { it.date }.thenByDescending { it.createdAt }.thenByDescending { it.id })
    val transfers: List<FinanceTransfer> =
        state.transfers.filter { transfer ->
            (state.selectedMonth.isBlank() || month != null && YearMonth.from(LocalDate.ofEpochDay(transfer.date)) == month) &&
                (
                    state.selectedAccountId == null ||
                        transfer.sourceAccountId == state.selectedAccountId ||
                        transfer.destinationAccountId == state.selectedAccountId
                ) &&
                state.selectedCategoryId == null &&
                state.selectedTagId == null
        }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Transactions", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        OutlinedTextField(
            value = state.selectedMonth,
            onValueChange = actions.monthChanged,
            label = { Text("Month (YYYY-MM, blank for all)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        FilterSelector("Account", state.accounts.map { it.id to it.name }, state.selectedAccountId, actions.accountChanged)
        FilterSelector("Category", state.categories.map { it.id to it.name }, state.selectedCategoryId, actions.categoryChanged)
        FilterSelector("Tag", state.tags.map { it.id to it.name }, state.selectedTagId, actions.tagChanged)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { actions.create(TransactionDirection.EXPENSE) }) { Text("Add expense") }
            Button(onClick = { actions.create(TransactionDirection.INCOME) }) { Text("Add income") }
            Button(onClick = actions.createTransfer) { Text("Transfer") }
        }
        if (transactions.isEmpty() && transfers.isEmpty()) {
            Text("No transactions match these filters.", modifier = Modifier.semantics { heading() })
            Text("Try another month, Account, Category, or Tag.")
        } else {
            transactions.forEach { transaction: FinanceTransaction ->
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    val isAccountActive: Boolean = state.accounts.firstOrNull { it.id == transaction.accountId }?.isArchived == false
                    TextButton(onClick = { actions.edit(transaction) }, enabled = isAccountActive) {
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
                    TextButton(onClick = { actions.delete(transaction.id) }, enabled = isAccountActive) { Text("Delete") }
                }
            }
            transfers.forEach { transfer ->
                val rows: List<Pair<String, String>> =
                    when (state.selectedAccountId) {
                        transfer.sourceAccountId -> listOf("Expense" to "Transfer out")
                        transfer.destinationAccountId -> listOf("Income" to "Transfer in")
                        else -> listOf("Expense" to "Transfer out", "Income" to "Transfer in")
                    }
                rows.forEach { (direction, context) ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        val isTransferEditable: Boolean =
                            state.accounts
                                .filter { it.id == transfer.sourceAccountId || it.id == transfer.destinationAccountId }
                                .all { !it.isArchived }
                        TextButton(onClick = {
                            actions.editTransfer(transfer)
                        }, enabled = isTransferEditable) {
                            Text(
                                "$direction · ${state.currency} ${formatMoney(transfer.amountMinor, state.currency)} · $context",
                            )
                        }
                        Text("${LocalDate.ofEpochDay(transfer.date)} · ${transfer.sourceAccountName} → ${transfer.destinationAccountName}")
                        TextButton(
                            onClick = { actions.deleteTransfer(transfer.id) },
                            enabled = isTransferEditable,
                        ) { Text("Delete Transfer") }
                    }
                }
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun FilterSelector(
    label: String,
    options: List<Pair<String, String>>,
    selectedId: String?,
    onSelected: (String?) -> Unit,
): Unit {
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
