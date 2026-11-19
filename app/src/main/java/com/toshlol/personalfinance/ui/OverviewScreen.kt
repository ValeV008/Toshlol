package com.toshlol.personalfinance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.CategoryBudget
import com.toshlol.personalfinance.data.FinanceTransaction
import com.toshlol.personalfinance.data.FinanceTransfer
import com.toshlol.personalfinance.data.MonthlyOverview
import com.toshlol.personalfinance.data.RecurringBudgetProgress
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState
import java.time.LocalDate
import java.time.YearMonth
import java.util.Currency

private val Charcoal: Color = Color(0xFF292B2F)
private val OverviewYellow: Color = Color(0xFFFFD23F)
private val PageGray: Color = Color(0xFFF1F2F4)
private val IncomeGreen: Color = Color(0xFF24834A)
private val ExpenseRed: Color = Color(0xFFBE3B3B)
private val EDGE_GESTURE_WIDTH = 32.dp
private val EDGE_GESTURE_DISTANCE = 48.dp
private const val DRAWER_WIDTH_FRACTION: Float = 0.86f
private const val DRAWER_OVERLAY_ALPHA: Float = 0.35f

data class OverviewScreenActions(
    val create: (TransactionDirection) -> Unit,
    val edit: (FinanceTransaction) -> Unit,
    val delete: (String) -> Unit,
    val openOrganizer: () -> Unit,
    val openBudgets: () -> Unit,
    val createTransfer: () -> Unit,
    val editTransfer: (FinanceTransfer) -> Unit,
    val openAccountHistory: (String) -> Unit,
    val monthChanged: (String) -> Unit,
    val overviewAccountToggled: (String) -> Unit,
    val accountDrawerChanged: (Boolean) -> Unit,
)

@Composable
@Suppress("ktlint:standard:no-unit-return")
fun OverviewScreen(
    state: TransactionsUiState,
    actions: OverviewScreenActions,
): Unit {
    val keyboardController = LocalSoftwareKeyboardController.current
    androidx.compose.runtime.LaunchedEffect(state.isFormVisible) {
        if (!state.isFormVisible) keyboardController?.hide()
    }
    val selectedMonth: YearMonth = runCatching { YearMonth.parse(state.selectedMonth) }.getOrDefault(YearMonth.now())
    val projection: MonthlyOverview = state.monthlyOverview
    val budgets: List<CategoryBudget> = state.budgets.filter { it.month == selectedMonth }
    val recurringBudgets: List<RecurringBudgetProgress> = state.recurringBudgets.filter { it.month == selectedMonth }
    val activity: List<OverviewActivityItem> = buildOverviewActivityItems(projection.transactions, projection.transfers)
    Box(
        modifier =
            Modifier.fillMaxSize().background(PageGray).testTag("overview-root").pointerInput(state.isAccountDrawerOpen) {
                var dragStartX: Float = 0f
                var dragAmount: Float = 0f
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragStartX = offset.x
                        dragAmount = 0f
                    },
                    onHorizontalDrag = { change, amount ->
                        dragAmount += amount
                        change.consume()
                    },
                    onDragEnd = {
                        if (dragStartX > size.width - EDGE_GESTURE_WIDTH.toPx() &&
                            dragAmount < -EDGE_GESTURE_DISTANCE.toPx()
                        ) {
                            actions.accountDrawerChanged(true)
                        }
                        if (state.isAccountDrawerOpen && dragAmount > EDGE_GESTURE_DISTANCE.toPx()) actions.accountDrawerChanged(false)
                    },
                )
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).semantics {
                        customActions =
                            listOf(
                                CustomAccessibilityAction("Open Account selection") {
                                    actions.accountDrawerChanged(true)
                                    true
                                },
                            )
                    },
            ) {
                OverviewMonthHeader(selectedMonth, actions.monthChanged)
                Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("CLOSING BALANCE", color = Color(0xFF686B70), style = MaterialTheme.typography.labelLarge)
                            Text(
                                "${state.currency} ${formatMoney(projection.closingBalanceMinor, state.currency)}",
                                style = MaterialTheme.typography.headlineLarge,
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SummaryAmount("Income", projection.incomeMinor, state.currency, IncomeGreen, Modifier.weight(1f))
                                SummaryAmount("Expenses", projection.expenseMinor, state.currency, ExpenseRed, Modifier.weight(1f))
                                SummaryAmount(
                                    "Net",
                                    projection.incomeMinor - projection.expenseMinor,
                                    state.currency,
                                    Charcoal,
                                    Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    if (budgets.isNotEmpty() || recurringBudgets.isNotEmpty()) {
                        Text("Budget progress", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                        recurringBudgets.forEach { budget ->
                            BudgetProgressCard(
                                budget.toBudgetProgressCardState(state.categories, state.tags),
                                state.accounts,
                                state.currency,
                            )
                        }
                        budgets.forEach { budget ->
                            BudgetProgressCard(
                                budget.toBudgetProgressCardState(),
                                state.accounts,
                                state.currency,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = actions.openOrganizer, modifier = Modifier.weight(1f)) { Text("Categories & Tags") }
                        OutlinedButton(
                            onClick = actions.openBudgets,
                            modifier = Modifier.weight(1f).testTag("open-budgets"),
                        ) { Text("Budgets") }
                    }
                    Text("Accounts", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                    state.accounts.filter { it.id in state.selectedOverviewAccountIds }.forEach { account ->
                        AccountCard(account, projection.closingBalancesMinor[account.id] ?: 0L, state.currency, actions.openAccountHistory)
                    }
                    Text("Recent activity", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                    if (activity.isEmpty()) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Text("No activity for this month and Account selection.", Modifier.padding(18.dp))
                        }
                    } else {
                        activity.forEach { item -> ActivityCard(item, state, actions) }
                    }
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
            AccountSelectionDrawer(state, projection, actions)
        }
    }
}

@Composable
private fun OverviewMonthHeader(
    month: YearMonth,
    monthChanged: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(Charcoal).padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 18.dp)) {
        Text("Overview", color = Color.White, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(
                onClick = { monthChanged(month.minusMonths(1).toString()) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("previous-month").semantics { contentDescription = "Previous month" },
            ) { Text("‹", color = OverviewYellow, style = MaterialTheme.typography.headlineMedium) }
            Text(
                month.format(
                    java.time.format.DateTimeFormatter
                        .ofPattern("MMMM yyyy"),
                ),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
            )
            TextButton(
                onClick = { monthChanged(month.plusMonths(1).toString()) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("next-month").semantics { contentDescription = "Next month" },
            ) { Text("›", color = OverviewYellow, style = MaterialTheme.typography.headlineMedium) }
        }
    }
}

@Composable
private fun SummaryAmount(
    label: String,
    amountMinor: Long,
    currency: String,
    color: Color,
    modifier: Modifier,
) {
    Column(modifier) {
        Text(label, color = Color(0xFF686B70), style = MaterialTheme.typography.labelMedium)
        Text(formatMoney(amountMinor, currency), color = color, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun AccountCard(
    account: Account,
    balanceMinor: Long,
    currency: String,
    openHistory: (String) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), onClick = {
        openHistory(account.id)
    }) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(account.name, style = MaterialTheme.typography.titleMedium)
                Text(account.type.label + if (account.isArchived) " · Archived" else "", style = MaterialTheme.typography.bodySmall)
            }
            Text("$currency ${formatMoney(balanceMinor, currency)}", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun AccountSelectionDrawer(
    state: TransactionsUiState,
    projection: MonthlyOverview,
    actions: OverviewScreenActions,
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = DRAWER_OVERLAY_ALPHA)), contentAlignment = Alignment.CenterEnd) {
        Column(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(
                    DRAWER_WIDTH_FRACTION,
                ).shadow(12.dp)
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Accounts", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                TextButton(
                    onClick = { actions.accountDrawerChanged(false) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("account-drawer-close"),
                ) { Text("Close") }
            }
            Text(
                projection.month.format(
                    java.time.format.DateTimeFormatter
                        .ofPattern("MMMM yyyy"),
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            state.accounts.forEach { account ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(account.name, style = MaterialTheme.typography.titleMedium)
                        if (account.isArchived) Text("Archived", style = MaterialTheme.typography.bodySmall)
                        Text("${state.currency} ${formatMoney(projection.closingBalancesMinor[account.id] ?: 0L, state.currency)}")
                    }
                    Switch(
                        checked = account.id in state.selectedOverviewAccountIds,
                        onCheckedChange = { actions.overviewAccountToggled(account.id) },
                        modifier =
                            Modifier.heightIn(min = 48.dp).semantics {
                                contentDescription = "${account.name} Account selection"
                                stateDescription = if (account.id in state.selectedOverviewAccountIds) "Selected" else "Not selected"
                            },
                    )
                }
            }
        }
    }
}

private data class OverviewActivityItem(
    val id: String,
    val date: Long,
    val createdAt: Long,
    val transaction: FinanceTransaction? = null,
    val transfer: FinanceTransfer? = null,
)

@Composable
private fun ActivityCard(
    item: OverviewActivityItem,
    state: TransactionsUiState,
    actions: OverviewScreenActions,
) {
    val transaction: FinanceTransaction? = item.transaction
    val transfer: FinanceTransfer? = item.transfer
    val date: LocalDate = LocalDate.ofEpochDay(item.date)
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            if (transaction != null) {
                val isEditable: Boolean = state.accounts.firstOrNull { it.id == transaction.accountId }?.isArchived == false
                TextButton(onClick = { actions.edit(transaction) }, enabled = isEditable) {
                    Text(
                        "${transaction.direction.name.lowercase().replaceFirstChar(
                            Char::uppercase,
                        )} · ${state.currency} ${formatMoney(transaction.amountMinor, state.currency)}",
                    )
                }
                Text(
                    "$date · ${transaction.accountName}${formatTransactionCategory(transaction, state.categories)?.let {
                        " · $it"
                    }.orEmpty()}${formatTransactionTags(transaction, state.tags).takeIf { it.isNotEmpty() }?.joinToString(
                        prefix = " · ",
                    ).orEmpty()}${transaction.note?.let { " · $it" }.orEmpty()}",
                )
                TextButton(onClick = { actions.delete(transaction.id) }, enabled = isEditable) { Text("Delete") }
            } else if (transfer != null) {
                val canEdit: Boolean =
                    state.accounts
                        .filter {
                            it.id == transfer.sourceAccountId || it.id == transfer.destinationAccountId
                        }.all { !it.isArchived }
                TextButton(onClick = {
                    actions.editTransfer(transfer)
                }, enabled = canEdit) { Text("Transfer · ${state.currency} ${formatMoney(transfer.amountMinor, state.currency)}") }
                Text("$date · ${transfer.sourceAccountName} → ${transfer.destinationAccountName}")
            }
        }
    }
}

private fun buildOverviewActivityItems(
    transactions: List<FinanceTransaction>,
    transfers: List<FinanceTransfer>,
): List<OverviewActivityItem> =
    (
        transactions.map { OverviewActivityItem(it.id, it.date, it.createdAt, transaction = it) } +
            transfers.map { OverviewActivityItem(it.id, it.date, it.createdAt, transfer = it) }
    ).sortedWith(compareByDescending<OverviewActivityItem> { it.date }.thenByDescending { it.createdAt }.thenByDescending { it.id })

internal fun formatMoney(
    amountMinor: Long,
    currency: String,
): String {
    val fractionDigits: Int = runCatching { Currency.getInstance(currency).defaultFractionDigits.coerceAtLeast(0) }.getOrDefault(2)
    return java.math.BigDecimal
        .valueOf(amountMinor, fractionDigits)
        .toPlainString()
}
