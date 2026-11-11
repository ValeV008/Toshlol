package com.toshlol.personalfinance.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.toshlol.personalfinance.data.FinanceTransaction
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState
import java.time.LocalDate
import java.util.Currency

@Composable
fun OverviewScreen(
    state: TransactionsUiState,
    onCreate: (TransactionDirection) -> Unit,
    onEdit: (FinanceTransaction) -> Unit,
    onDelete: (String) -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onAmountChanged: (String) -> Unit,
    onDateChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onDirectionChanged: (TransactionDirection) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    val currentMonth: List<FinanceTransaction> =
        state.transactions.filter {
            LocalDate.ofEpochDay(it.date).month == LocalDate.now().month &&
                LocalDate.ofEpochDay(it.date).year == LocalDate.now().year
        }
    val income: Long = currentMonth.filter { it.direction == TransactionDirection.INCOME }.sumOf { it.amountMinor }
    val expense: Long = currentMonth.filter { it.direction == TransactionDirection.EXPENSE }.sumOf { it.amountMinor }
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Overview", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Text("${state.currency} ledger is ready.", style = MaterialTheme.typography.bodyLarge)
        Text("This month · ${state.currency}", style = MaterialTheme.typography.titleMedium)
        Text(
            "Income  ${state.currency} ${formatMoney(
                income,
                state.currency,
            )}     Expenses  ${state.currency} ${formatMoney(expense, state.currency)}",
        )
        Text("Net  ${state.currency} ${formatMoney(income - expense, state.currency)}", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onCreate(TransactionDirection.INCOME) }) { Text("Add income") }
            Button(onClick = { onCreate(TransactionDirection.EXPENSE) }) { Text("Add expense") }
        }
        if (state.transactions.isEmpty()) {
            Text("No transactions yet")
            Text("Record income or an expense to start your history.")
        } else {
            Text("Recent transactions", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.transactions, key = { it.id }) { transaction: FinanceTransaction ->
                    Column(Modifier.fillMaxWidth().padding(8.dp)) {
                        TextButton(onClick = { onEdit(transaction) }) {
                            Text(
                                "${transaction.direction.name.lowercase().replaceFirstChar(
                                    Char::uppercase,
                                )} · ${state.currency} ${formatMoney(transaction.amountMinor, state.currency)}",
                            )
                        }
                        Text(
                            "${LocalDate.ofEpochDay(
                                transaction.date,
                            )} · ${transaction.accountName}${transaction.note?.let { " · $it" }.orEmpty()}",
                        )
                        TextButton(onClick = { onDelete(transaction.id) }) { Text("Delete") }
                    }
                }
            }
        }
    }
    if (state.isFormVisible) {
        AlertDialog(onDismissRequest = onClose, title = {
            Text(
                if (state.editingId ==
                    null
                ) {
                    "New transaction"
                } else {
                    "Edit transaction"
                },
            )
        }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row {
                    TransactionDirection.entries.forEach { direction: TransactionDirection ->
                        Row(Modifier.clickable { onDirectionChanged(direction) }) {
                            RadioButton(
                                selected =
                                    state.direction == direction,
                                onClick = { onDirectionChanged(direction) },
                            )
                            ; Text(direction.name.lowercase().replaceFirstChar(Char::uppercase))
                        }
                    }
                }
                OutlinedTextField(state.amount, onAmountChanged, label = { Text("Amount") }, singleLine = true)
                OutlinedTextField(state.date, onDateChanged, label = { Text("Date (YYYY-MM-DD)") }, singleLine = true)
                OutlinedTextField(state.note, onNoteChanged, label = { Text("Note (optional)") }, singleLine = true)
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = {
            Button(onClick = onSave, enabled = !state.isSaving) {
                Text("Save")
            }
        }, dismissButton = { OutlinedButton(onClick = onClose) { Text("Cancel") } })
    }
    state.pendingDeleteId?.let {
        AlertDialog(onDismissRequest = onCancelDelete, title = {
            Text("Delete transaction?")
        }, text = {
            Text("This transaction will be permanently deleted.")
        }, confirmButton = {
            Button(onClick = onConfirmDelete) { Text("Delete") }
        }, dismissButton = { TextButton(onClick = onCancelDelete) { Text("Cancel") } })
    }
}

private fun formatMoney(
    amountMinor: Long,
    currency: String,
): String {
    val fractionDigits: Int = runCatching { Currency.getInstance(currency).defaultFractionDigits.coerceAtLeast(0) }.getOrDefault(2)
    return java.math.BigDecimal
        .valueOf(amountMinor, fractionDigits)
        .toPlainString()
}
