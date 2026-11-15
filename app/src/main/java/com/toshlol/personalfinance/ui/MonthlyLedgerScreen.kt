package com.toshlol.personalfinance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.toshlol.personalfinance.data.FinanceTransaction
import com.toshlol.personalfinance.data.FinanceTransfer
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

data class MonthlyLedgerActions(
    val monthChanged: (String) -> Unit,
    val categoryChanged: (String?) -> Unit,
    val tagChanged: (String?) -> Unit,
    val create: (TransactionDirection) -> Unit,
    val edit: (FinanceTransaction) -> Unit,
    val delete: (String) -> Unit,
    val editTransfer: (FinanceTransfer) -> Unit,
    val deleteTransfer: (String) -> Unit,
    val createTransfer: () -> Unit,
    val overviewAccountToggled: (String) -> Unit,
    val accountDrawerChanged: (Boolean) -> Unit,
)

private data class LedgerDayActivity(
    val date: LocalDate,
    val activities: List<LedgerActivity>,
)

private data class LedgerActivity(
    val date: Long,
    val createdAt: Long,
    val id: String,
    val transaction: FinanceTransaction? = null,
    val transfer: FinanceTransfer? = null,
)

@Composable
@Suppress("ktlint:standard:no-unit-return")
fun MonthlyLedgerScreen(
    state: TransactionsUiState,
    direction: TransactionDirection,
    actions: MonthlyLedgerActions,
): Unit {
    val month: YearMonth = runCatching { YearMonth.parse(state.selectedMonth) }.getOrDefault(YearMonth.now())
    val isExpense: Boolean = direction == TransactionDirection.EXPENSE
    val heading: String = if (isExpense) "Expenses" else "Income"
    val selectedTransactions: List<FinanceTransaction> =
        state.monthlyOverview.transactions
            .filter { it.direction == direction }
            .filter { state.selectedCategoryId == null || it.categoryId == state.selectedCategoryId }
            .filter { state.selectedTagId == null || state.selectedTagId in it.tagIds }
            .sortedWith(compareByDescending<FinanceTransaction> { it.date }.thenByDescending { it.createdAt }.thenByDescending { it.id })
    val selectedTransfers: List<FinanceTransfer> =
        state.monthlyOverview.transfers
            .filter { transfer ->
                val relevantAccountId: String = if (isExpense) transfer.sourceAccountId else transfer.destinationAccountId
                relevantAccountId in state.selectedOverviewAccountIds && state.selectedCategoryId == null && state.selectedTagId == null
            }.sortedWith(compareByDescending<FinanceTransfer> { it.date }.thenByDescending { it.createdAt }.thenByDescending { it.id })
    val totalMinor: Long = selectedTransactions.sumOf { it.amountMinor }
    val days: List<LedgerDayActivity> = buildLedgerDays(selectedTransactions, selectedTransfers)
    Box(Modifier.fillMaxSize().background(Color(0xFFF1F2F4))) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.fillMaxWidth().background(Color(0xFF292B2F), RoundedCornerShape(18.dp)).padding(14.dp)) {
                    Text(
                        heading,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(
                            onClick = { actions.monthChanged(month.minusMonths(1).toString()) },
                        ) {
                            Text("<", color = Color(0xFFFFD23F))
                        }
                        Text(
                            month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        TextButton(
                            onClick = { actions.monthChanged(month.plusMonths(1).toString()) },
                        ) {
                            Text(">", color = Color(0xFFFFD23F))
                        }
                    }
                }
                OutlinedButton(onClick = { actions.accountDrawerChanged(true) }) {
                    Text("Accounts (${state.selectedOverviewAccountIds.size} selected)")
                }
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Text("MONTHLY $heading".uppercase(), color = Color(0xFF686B70), style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${state.currency} ${formatMoney(totalMinor, state.currency)}",
                            color = if (isExpense) Color(0xFFBE3B3B) else Color(0xFF24834A),
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Text("${state.selectedOverviewAccountIds.size} selected Accounts", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LedgerFilter("Category", state.categories.map { it.id to it.name }, state.selectedCategoryId, actions.categoryChanged)
                    LedgerFilter("Tag", state.tags.map { it.id to it.name }, state.selectedTagId, actions.tagChanged)
                }
                if (days.isEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Text("No $heading activity for this month and Account selection.", Modifier.fillMaxWidth().padding(18.dp))
                    }
                } else {
                    days.forEach { day -> LedgerDayCard(day, direction, state, actions) }
                }
            }
            Row(
                Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Button(onClick = { actions.create(TransactionDirection.EXPENSE) }, modifier = Modifier.weight(1f)) { Text("Add expense") }
                Button(onClick = { actions.create(TransactionDirection.INCOME) }, modifier = Modifier.weight(1f)) { Text("Add income") }
                OutlinedButton(onClick = actions.createTransfer, modifier = Modifier.weight(1f)) { Text("Transfer money") }
            }
        }
        if (state.isAccountDrawerOpen) {
            LedgerAccountSelectionDrawer(state, actions)
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun LedgerAccountSelectionDrawer(
    state: TransactionsUiState,
    actions: MonthlyLedgerActions,
): Unit {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(
                    0.86f,
                ).shadow(12.dp)
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Accounts", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                TextButton(onClick = { actions.accountDrawerChanged(false) }) { Text("Close") }
            }
            Text(state.selectedMonth, style = MaterialTheme.typography.titleMedium)
            state.accounts.forEach { account ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(account.name, style = MaterialTheme.typography.titleMedium)
                        if (account.isArchived) Text("Archived", style = MaterialTheme.typography.bodySmall)
                        val closingBalance: Long = state.monthlyOverview.closingBalancesMinor[account.id] ?: 0L
                        Text("${state.currency} ${formatMoney(closingBalance, state.currency)}")
                    }
                    Switch(
                        checked = account.id in state.selectedOverviewAccountIds,
                        onCheckedChange = { actions.overviewAccountToggled(account.id) },
                        modifier =
                            Modifier.semantics {
                                contentDescription = "${account.name} Account selection"
                                stateDescription = if (account.id in state.selectedOverviewAccountIds) "Selected" else "Not selected"
                            },
                    )
                }
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun LedgerDayCard(
    day: LedgerDayActivity,
    direction: TransactionDirection,
    state: TransactionsUiState,
    actions: MonthlyLedgerActions,
): Unit {
    val isExpense: Boolean = direction == TransactionDirection.EXPENSE
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                day.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            day.activities.forEach { activity ->
                val transaction: FinanceTransaction? = activity.transaction
                val transfer: FinanceTransfer? = activity.transfer
                if (transaction != null) LedgerTransactionRow(transaction, isExpense, state, actions)
                if (transfer != null) LedgerTransferRow(transfer, isExpense, state, actions)
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun LedgerTransactionRow(
    transaction: FinanceTransaction,
    isExpense: Boolean,
    state: TransactionsUiState,
    actions: MonthlyLedgerActions,
): Unit {
    val canEdit: Boolean = state.accounts.firstOrNull { it.id == transaction.accountId }?.isArchived == false
    Column {
        TextButton(onClick = { actions.edit(transaction) }, enabled = canEdit, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth()) {
                val sign: String = if (isExpense) "-" else "+"
                Text(
                    "$sign${state.currency} ${formatMoney(transaction.amountMinor, state.currency)}",
                    color = if (isExpense) Color(0xFFBE3B3B) else Color(0xFF24834A),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    listOfNotNull(
                        transaction.accountName,
                        transaction.categoryName,
                        transaction.tagNames.takeIf { it.isNotEmpty() }?.joinToString(),
                        transaction.note,
                    ).joinToString(" · "),
                )
            }
        }
        TextButton(onClick = { actions.delete(transaction.id) }, enabled = canEdit) { Text("Delete") }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun LedgerTransferRow(
    transfer: FinanceTransfer,
    isExpense: Boolean,
    state: TransactionsUiState,
    actions: MonthlyLedgerActions,
): Unit {
    val canEdit: Boolean =
        state.accounts
            .filter { it.id == transfer.sourceAccountId || it.id == transfer.destinationAccountId }
            .all { !it.isArchived }
    Column {
        TextButton(onClick = { actions.editTransfer(transfer) }, enabled = canEdit, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth()) {
                val label: String = if (isExpense) "Transfer out" else "Transfer in"
                val sign: String = if (isExpense) "-" else "+"
                Text(
                    "$sign${state.currency} ${formatMoney(transfer.amountMinor, state.currency)} · $label",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text("${transfer.sourceAccountName} to ${transfer.destinationAccountName}")
            }
        }
        TextButton(onClick = { actions.deleteTransfer(transfer.id) }, enabled = canEdit) { Text("Delete Transfer") }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun LedgerFilter(
    label: String,
    options: List<Pair<String, String>>,
    selectedId: String?,
    onSelected: (String?) -> Unit,
): Unit {
    var isExpanded: Boolean by remember { mutableStateOf(false) }
    val selectedName: String = options.firstOrNull { it.first == selectedId }?.second ?: "All $label"
    Column {
        OutlinedButton(onClick = { isExpanded = true }) { Text("$label: $selectedName") }
        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            DropdownMenuItem(text = { Text("All $label") }, onClick = {
                onSelected(null)
                isExpanded = false
            })
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option.second) }, onClick = {
                    onSelected(option.first)
                    isExpanded =
                        false
                })
            }
        }
    }
}

private fun buildLedgerDays(
    transactions: List<FinanceTransaction>,
    transfers: List<FinanceTransfer>,
): List<LedgerDayActivity> {
    val activities: List<LedgerActivity> =
        transactions.map { LedgerActivity(it.date, it.createdAt, it.id, transaction = it) } +
            transfers.map { LedgerActivity(it.date, it.createdAt, it.id, transfer = it) }
    return activities
        .groupBy { LocalDate.ofEpochDay(it.date) }
        .map { (date, dayActivities) ->
            val orderedActivities: List<LedgerActivity> =
                dayActivities.sortedWith(compareByDescending<LedgerActivity> { it.createdAt }.thenByDescending { it.id })
            LedgerDayActivity(date, orderedActivities)
        }.sortedByDescending(LedgerDayActivity::date)
}
