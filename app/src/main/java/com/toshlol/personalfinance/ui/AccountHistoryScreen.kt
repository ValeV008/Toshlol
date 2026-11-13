package com.toshlol.personalfinance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.FinanceTransaction
import com.toshlol.personalfinance.data.FinanceTransfer
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState
import java.time.LocalDate

data class AccountHistoryScreenActions(
    val back: () -> Unit,
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
    val activities: List<AccountHistoryItem> =
        account
            ?.let { account ->
                buildAccountHistoryItems(state.transactions, state.transfers, account)
            }.orEmpty()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "${account?.name ?: "Account"} history",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        OutlinedButton(onClick = actions.back) { Text("Back to overview") }
        if (account == null) {
            Text("This Account is no longer available.")
        } else {
            Text("Balance  ${state.currency} ${formatMoney(account.balanceMinor, state.currency)}")
            if (activities.isEmpty()) {
                Text("No account activity yet")
            } else {
                activities.forEach { item ->
                    AccountHistoryActivity(item, account, state.currency, actions)
                }
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun AccountHistoryActivity(
    item: AccountHistoryItem,
    account: Account,
    currency: String,
    actions: AccountHistoryScreenActions,
): Unit {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        val transaction: FinanceTransaction? = item.transaction
        val transfer: FinanceTransfer? = item.transfer
        if (transaction != null) {
            TextButton(onClick = { actions.editTransaction(transaction) }) {
                Text(
                    "${transaction.direction.name.lowercase().replaceFirstChar(
                        Char::uppercase,
                    )} · $currency ${formatMoney(transaction.amountMinor, currency)}",
                )
            }
            Text(
                "${LocalDate.ofEpochDay(
                    transaction.date,
                )} · ${transaction.categoryName ?: "Transaction"}${transaction.note?.let { " · $it" }.orEmpty()}",
            )
            TextButton(onClick = { actions.deleteTransaction(transaction.id) }) { Text("Delete") }
        } else if (transfer != null) {
            val isOutgoing: Boolean = transfer.sourceAccountId == account.id
            val context: String = if (isOutgoing) "Transfer out" else "Transfer in"
            val otherAccountName: String = if (isOutgoing) transfer.destinationAccountName else transfer.sourceAccountName
            TextButton(onClick = { actions.editTransfer(transfer) }) {
                Text("$context · $currency ${formatMoney(transfer.amountMinor, currency)}")
            }
            Text("${LocalDate.ofEpochDay(transfer.date)} · ${if (isOutgoing) "To" else "From"} $otherAccountName")
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
): List<AccountHistoryItem> =
    (
        transactions
            .filter { it.accountId == account.id }
            .map { AccountHistoryItem(it.id, it.date, it.createdAt, transaction = it) } +
            transfers
                .filter { it.sourceAccountId == account.id || it.destinationAccountId == account.id }
                .map { AccountHistoryItem(it.id, it.date, it.createdAt, transfer = it) }
    ).sortedWith(compareByDescending<AccountHistoryItem> { it.date }.thenByDescending { it.createdAt }.thenByDescending { it.id })
