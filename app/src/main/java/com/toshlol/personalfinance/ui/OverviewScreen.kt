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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.toshlol.personalfinance.data.CategoryBudget
import com.toshlol.personalfinance.data.FinanceTransaction
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState
import java.time.LocalDate
import java.time.YearMonth
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
    onOpenOrganizer: () -> Unit,
    onCategoryChanged: (String?) -> Unit,
    onTagToggled: (String) -> Unit,
    onOpenBudgets: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(state.isFormVisible) {
        if (!state.isFormVisible) keyboardController?.hide()
    }
    val currentMonth: List<FinanceTransaction> =
        state.transactions.filter {
            LocalDate.ofEpochDay(it.date).month == LocalDate.now().month &&
                LocalDate.ofEpochDay(it.date).year == LocalDate.now().year
        }
    val income: Long = currentMonth.filter { it.direction == TransactionDirection.INCOME }.sumOf { it.amountMinor }
    val expense: Long = currentMonth.filter { it.direction == TransactionDirection.EXPENSE }.sumOf { it.amountMinor }
    val currentBudgetMonth: YearMonth = YearMonth.now()
    val currentBudgets: List<CategoryBudget> = state.budgets.filter { it.month == currentBudgetMonth }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
        if (currentBudgets.isNotEmpty()) {
            Text("Budget progress", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            currentBudgets.forEach { budget: CategoryBudget ->
                Text(
                    "${budget.categoryName} · ${state.currency} ${formatMoney(
                        budget.spentMinor,
                        state.currency,
                    )} / ${formatMoney(
                        budget.limitMinor,
                        state.currency,
                    )} · ${if (budget.isOverBudget) "Over budget" else "Remaining ${formatMoney(budget.remainingMinor, state.currency)}"}",
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onOpenOrganizer, modifier = Modifier.weight(1f)) { Text("Organize categories and tags") }
            OutlinedButton(onClick = onOpenBudgets, modifier = Modifier.weight(1f).testTag("open-budgets")) { Text("Budgets") }
        }
        if (state.transactions.isEmpty()) {
            Text("No transactions yet")
            Text("Record income or an expense to start your history.")
        } else {
            Text("Recent transactions", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            state.transactions.forEach { transaction: FinanceTransaction ->
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
                        )} · ${transaction.accountName}${transaction.categoryName?.let {
                            " · $it"
                        }.orEmpty()}${transaction.tagNames.takeIf {
                            it
                                .isNotEmpty()
                        }?.joinToString(
                            prefix = " · ",
                        ).orEmpty()}${transaction.note?.let { " · $it" }.orEmpty()}",
                    )
                    TextButton(onClick = { onDelete(transaction.id) }) { Text("Delete") }
                }
            }
        }
    }
}

internal fun formatMoney(
    amountMinor: Long,
    currency: String,
): String {
    val fractionDigits: Int = runCatching { Currency.getInstance(currency).defaultFractionDigits.coerceAtLeast(0) }.getOrDefault(2)
    return java.math.BigDecimal
        .valueOf(amountMinor, fractionDigits)
        .toPlainString()
}
