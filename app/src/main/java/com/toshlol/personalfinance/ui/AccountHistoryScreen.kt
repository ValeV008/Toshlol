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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.FinanceTransaction
import com.toshlol.personalfinance.data.FinanceTransfer
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState
import java.time.LocalDate
import java.time.YearMonth

private val historyPageGray: Color = Color(0xFFF1F2F4)
private val historyIncomeGreen: Color = Color(0xFF24834A)
private val historyExpenseRed: Color = Color(0xFFBE3B3B)
private val historyPageHorizontalPadding: Dp = 18.dp
private val historyPageVerticalPadding: Dp = 16.dp
private val historySectionSpacing: Dp = 12.dp
private val historyCardCornerRadius: Dp = 18.dp
private val historyBalanceCardCornerRadius: Dp = 20.dp
private val historySmallCardPadding: Dp = 18.dp
private val historyBalanceCardPadding: Dp = 18.dp
private val historyBalanceItemSpacing: Dp = 6.dp
private val historyMovementCardPadding: Dp = 14.dp
private val historyMovementItemSpacing: Dp = 4.dp

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
    val selectedMonth: YearMonth = parseSelectedMonth(state.selectedMonth)
    val activities: List<AccountHistoryItem> =
        account
            ?.let { selectedAccount: Account ->
                buildAccountHistoryItems(state.transactions, state.transfers, selectedAccount, selectedMonth)
            }.orEmpty()
    val closingBalanceMinor: Long =
        account?.let { selectedAccount: Account ->
            state.monthlyOverview.closingBalancesMinor[selectedAccount.id]
        } ?: 0L
    Column(Modifier.fillMaxSize().background(historyPageGray)) {
        ShowAccountMonthHeader(
            title = "${account?.name ?: "Account"} history",
            month = selectedMonth,
            monthChanged = actions.monthChanged,
        )
        ShowAccountHistoryContent(
            content =
                AccountHistoryContentState(
                    account = account,
                    accounts = state.accounts,
                    classifications = ClassificationReferences(state.categories, state.tags),
                    currency = state.currency,
                    month = selectedMonth,
                    closingBalanceMinor = closingBalanceMinor,
                    activities = activities,
                    actions = actions,
                ),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun ShowAccountHistoryContent(
    content: AccountHistoryContentState,
    modifier: Modifier,
): Unit {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(
            horizontal = historyPageHorizontalPadding,
            vertical = historyPageVerticalPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(historySectionSpacing),
    ) {
        OutlinedButton(onClick = content.actions.back) { Text("Back") }
        if (content.account == null) {
            ShowAccountHistoryMessage("This Account is no longer available.")
        } else {
            ShowAccountHistorySummary(content.account, content.currency, content.month, content.closingBalanceMinor)
            Text(
                "Activity for ${formatMonthLabel(content.month)}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            if (content.activities.isEmpty()) ShowAccountHistoryMessage("No account activity yet")
            content.activities.forEach { item: AccountHistoryItem ->
                AccountHistoryActivity(item, content)
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun ShowAccountHistoryMessage(message: String): Unit {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(historyCardCornerRadius)) {
        Text(message, Modifier.fillMaxWidth().padding(historySmallCardPadding))
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun ShowAccountHistorySummary(
    account: Account,
    currency: String,
    month: YearMonth,
    closingBalanceMinor: Long,
): Unit {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(historyBalanceCardCornerRadius)) {
        Column(
            Modifier.fillMaxWidth().padding(historyBalanceCardPadding),
            verticalArrangement = Arrangement.spacedBy(historyBalanceItemSpacing),
        ) {
            Text("${formatMonthLabel(month)} closing balance", style = MaterialTheme.typography.titleMedium)
            Text("Balance  $currency ${formatMoney(closingBalanceMinor, currency)}", style = MaterialTheme.typography.headlineMedium)
            if (account.isArchived) {
                Text("Archived")
                Text("Read-only history")
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun AccountHistoryActivity(
    item: AccountHistoryItem,
    content: AccountHistoryContentState,
): Unit {
    val account: Account = checkNotNull(content.account)
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(historyCardCornerRadius)) {
        Column(
            Modifier.fillMaxWidth().padding(historyMovementCardPadding),
            verticalArrangement = Arrangement.spacedBy(historyMovementItemSpacing),
        ) {
            item.transaction?.let { transaction: FinanceTransaction ->
                ShowTransactionHistoryActivity(transaction, content)
            }
            item.transfer?.let { transfer: FinanceTransfer ->
                ShowTransferHistoryActivity(transfer, content)
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun ShowTransactionHistoryActivity(
    transaction: FinanceTransaction,
    content: AccountHistoryContentState,
): Unit {
    val account: Account = checkNotNull(content.account)
    val isIncome: Boolean = transaction.direction == TransactionDirection.INCOME
    val sign: String = if (isIncome) "+" else "-"
    val label: String = if (isIncome) "Income" else "Expense"
    val color: Color = if (isIncome) historyIncomeGreen else historyExpenseRed
    val title: String = "$label \u00B7 $sign${content.currency} ${formatMoney(transaction.amountMinor, content.currency)}"
    if (account.isArchived) {
        Text(title, color = color, style = MaterialTheme.typography.titleMedium)
    } else {
        TextButton(onClick = { content.actions.editTransaction(transaction) }) {
            Text(title, color = color, style = MaterialTheme.typography.titleMedium)
        }
    }
    val categoryName: String = formatTransactionCategory(transaction, content.classifications.categories) ?: "No Category"
    val tagNames: List<String> = formatTransactionTags(transaction, content.classifications.tags)
    Text(
        "${LocalDate.ofEpochDay(transaction.date)} \u00B7 $categoryName" +
            tagNames.joinToString(prefix = " \u00B7 ").takeIf { tagNames.isNotEmpty() }.orEmpty(),
    )
    transaction.note?.let { note: String -> Text(note) }
    if (!account.isArchived) TextButton(onClick = { content.actions.deleteTransaction(transaction.id) }) { Text("Delete") }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun ShowTransferHistoryActivity(
    transfer: FinanceTransfer,
    content: AccountHistoryContentState,
): Unit {
    val account: Account = checkNotNull(content.account)
    val isOutgoing: Boolean = transfer.sourceAccountId == account.id
    val otherAccountId: String = if (isOutgoing) transfer.destinationAccountId else transfer.sourceAccountId
    val otherAccount: Account? = content.accounts.firstOrNull { candidate: Account -> candidate.id == otherAccountId }
    val sign: String = if (isOutgoing) "-" else "+"
    val label: String = if (isOutgoing) "Transfer out" else "Transfer in"
    val otherAccountName: String = if (isOutgoing) transfer.destinationAccountName else transfer.sourceAccountName
    val title: String = "$sign${content.currency} ${formatMoney(transfer.amountMinor, content.currency)} \u00B7 $label"
    val canEdit: Boolean = !account.isArchived && otherAccount?.isArchived == false
    if (canEdit) {
        TextButton(onClick = { content.actions.editTransfer(transfer) }) { Text(title, style = MaterialTheme.typography.titleMedium) }
    } else {
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
    Text("${LocalDate.ofEpochDay(transfer.date)} \u00B7 ${if (isOutgoing) "To" else "From"} $otherAccountName")
}

private data class AccountHistoryItem(
    val id: String,
    val date: Long,
    val createdAt: Long,
    val transaction: FinanceTransaction? = null,
    val transfer: FinanceTransfer? = null,
)

private data class AccountHistoryContentState(
    val account: Account?,
    val accounts: List<Account>,
    val classifications: ClassificationReferences,
    val currency: String,
    val month: YearMonth,
    val closingBalanceMinor: Long,
    val activities: List<AccountHistoryItem>,
    val actions: AccountHistoryScreenActions,
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
            .filter { it.accountId == account.id && it.date in monthStart..monthEnd }
            .map { AccountHistoryItem(it.id, it.date, it.createdAt, transaction = it) } +
            transfers
                .filter {
                    (it.sourceAccountId == account.id || it.destinationAccountId == account.id) &&
                        it.date in monthStart..monthEnd
                }.map { AccountHistoryItem(it.id, it.date, it.createdAt, transfer = it) }
    ).sortedWith(compareByDescending<AccountHistoryItem> { it.date }.thenByDescending { it.createdAt }.thenByDescending { it.id })
}
