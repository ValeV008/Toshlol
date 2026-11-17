package com.toshlol.personalfinance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.FinanceTransaction
import com.toshlol.personalfinance.data.FinanceTransfer
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HistoryPageGray: Color = Color(0xFFF1F2F4)
private val HistoryIncomeGreen: Color = Color(0xFF24834A)
private val HistoryExpenseRed: Color = Color(0xFFBE3B3B)

data class AccountHistoryScreenActions(
    val back: () -> Unit,
    val monthChanged: (String) -> Unit,
    val editTransaction: (FinanceTransaction) -> Unit,
    val deleteTransaction: (String) -> Unit,
    val editTransfer: (FinanceTransfer) -> Unit,
)

@Composable
@Suppress("ktlint:standard:no-unit-return")
fun AccountHistoryScreen(
    state: TransactionsUiState,
    actions: AccountHistoryScreenActions,
): Unit {
    val account: Account? = state.accounts.firstOrNull { it.id == state.accountHistoryAccountId }
    val selectedMonth: YearMonth = runCatching { YearMonth.parse(state.selectedMonth) }.getOrDefault(YearMonth.now())
    val activities: List<AccountHistoryItem> =
        account?.let { buildAccountHistoryItems(state.transactions, state.transfers, it, selectedMonth) }.orEmpty()
    val closingBalanceMinor: Long = account?.let { state.monthlyOverview.closingBalancesMinor[it.id] } ?: 0L
    Column(Modifier.fillMaxSize().background(HistoryPageGray)) {
        AccountMonthHeader(
            title = "${account?.name ?: "Account"} history",
            month = selectedMonth,
            monthChanged = actions.monthChanged,
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = actions.back) { Text("Back") }
            if (account == null) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                    Text("This Account is no longer available.", Modifier.fillMaxWidth().padding(18.dp))
                }
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${monthLabel(selectedMonth)} closing balance", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Balance  ${state.currency} ${formatMoney(closingBalanceMinor, state.currency)}",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        if (account.isArchived) {
                            Text("Archived")
                            Text("Read-only history")
                        }
                    }
                }
                Text(
                    "Activity for ${monthLabel(selectedMonth)}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                if (activities.isEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                        Text("No account activity yet", Modifier.fillMaxWidth().padding(18.dp))
                    }
                } else {
                    activities.forEach { item ->
                        AccountHistoryActivity(item, account, state.currency, actions)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountHistoryActivity(
    item: AccountHistoryItem,
    account: Account,
    currency: String,
    actions: AccountHistoryScreenActions,
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val transaction: FinanceTransaction? = item.transaction
            val transfer: FinanceTransfer? = item.transfer
            if (transaction != null) {
                val isIncome: Boolean = transaction.direction.name == "INCOME"
                val label: String = if (isIncome) "Income" else "Expense"
                val color: Color = if (isIncome) HistoryIncomeGreen else HistoryExpenseRed
                val title: String = "$label · $currency ${formatMoney(transaction.amountMinor, currency)}"
                if (account.isArchived) {
                    Text(title, color = color, style = MaterialTheme.typography.titleMedium)
                } else {
                    TextButton(onClick = { actions.editTransaction(transaction) }) {
                        Text(title, color = color, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Text("${LocalDate.ofEpochDay(transaction.date)} · ${transaction.categoryName ?: "No Category"}")
                transaction.note?.let { Text(it) }
                if (!account.isArchived) TextButton(onClick = { actions.deleteTransaction(transaction.id) }) { Text("Delete") }
            }
            if (transfer != null) {
                val isOutgoing: Boolean = transfer.sourceAccountId == account.id
                val label: String = if (isOutgoing) "Transfer out" else "Transfer in"
                val otherAccountName: String = if (isOutgoing) transfer.destinationAccountName else transfer.sourceAccountName
                val title: String = "$label · $currency ${formatMoney(transfer.amountMinor, currency)}"
                if (account.isArchived) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                } else {
                    TextButton(onClick = { actions.editTransfer(transfer) }) { Text(title, style = MaterialTheme.typography.titleMedium) }
                }
                Text("${LocalDate.ofEpochDay(transfer.date)} · ${if (isOutgoing) "To" else "From"} $otherAccountName")
            }
        }
    }
}

private data class AccountHistoryItem(
    val id: String,
    val date: Long,
    val createdAt: Long,
    val transaction: FinanceTransaction? = null,
    val transfer: FinanceTransfer? = null,
)

private fun buildAccountHistoryItems(
    transactions: List<FinanceTransaction>,
    transfers: List<FinanceTransfer>,
    account: Account,
    month: YearMonth,
): List<AccountHistoryItem> {
    val monthStart: Long = month.atDay(1).toEpochDay()
    val monthEnd: Long = month.atEndOfMonth().toEpochDay()
    return (
        transactions
            .filter { it.accountId == account.id && it.date >= account.openingBalanceDate && it.date in monthStart..monthEnd }
            .map { AccountHistoryItem(it.id, it.date, it.createdAt, transaction = it) } +
            transfers
                .filter {
                    (it.sourceAccountId == account.id || it.destinationAccountId == account.id) &&
                        it.date >= account.openingBalanceDate &&
                        it.date in monthStart..monthEnd
                }.map { AccountHistoryItem(it.id, it.date, it.createdAt, transfer = it) }
    ).sortedWith(compareByDescending<AccountHistoryItem> { it.date }.thenByDescending { it.createdAt }.thenByDescending { it.id })
}

private fun monthLabel(month: YearMonth): String = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
