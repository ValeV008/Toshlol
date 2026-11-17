package com.toshlol.personalfinance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val accountsPageGray: Color = Color(0xFFF1F2F4)
private val accountsCharcoal: Color = Color(0xFF292B2F)
private val accountsYellow: Color = Color(0xFFFFD23F)
private val accountsPageHorizontalPadding: Dp = 18.dp
private val accountsPageVerticalPadding: Dp = 16.dp
private val accountsListItemSpacing: Dp = 12.dp
private val accountCardPadding: Dp = 16.dp
private val accountCardItemSpacing: Dp = 6.dp
private val accountCardCornerRadius: Dp = 18.dp
private val accountActionSpacing: Dp = 8.dp
private val accountEditorPadding: Dp = 18.dp
private val accountEditorItemSpacing: Dp = 12.dp
private val accountEditorCornerRadius: Dp = 20.dp
private val accountTypeItemSpacing: Dp = 4.dp
private val accountTypeLabelTopPadding: Dp = 12.dp
private val accountHeaderHorizontalPadding: Dp = 18.dp
private val accountHeaderTopPadding: Dp = 16.dp
private val accountHeaderBottomPadding: Dp = 10.dp
private val accountMonthLabelTopPadding: Dp = 12.dp

data class AccountsScreenActions(
    val back: () -> Unit,
    val monthChanged: (String) -> Unit,
    val openHistory: (String) -> Unit,
    val edit: (String) -> Unit,
    val archive: (String) -> Unit,
    val addAccount: () -> Unit,
    val closeEditor: () -> Unit,
    val accountEditor: AccountEditorActions,
)

@Composable
@Suppress("ktlint:standard:no-unit-return")
fun AccountsScreen(
    state: TransactionsUiState,
    actions: AccountsScreenActions,
): Unit {
    val selectedMonth: YearMonth = parseSelectedMonth(state.selectedMonth)
    Column(Modifier.fillMaxSize().background(accountsPageGray)) {
        ShowAccountMonthHeader(
            title = if (state.isAccountEditorVisible) getAccountEditorTitle(state) else "Accounts",
            month = selectedMonth,
            monthChanged = actions.monthChanged,
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(
                horizontal = accountsPageHorizontalPadding,
                vertical = accountsPageVerticalPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(accountsListItemSpacing),
        ) {
            OutlinedButton(onClick = if (state.isAccountEditorVisible) actions.closeEditor else actions.back) {
                Text(if (state.isAccountEditorVisible) "Back to Accounts" else "Back to overview")
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.isAccountEditorVisible) {
                ShowAccountEditor(state, actions)
            } else {
                Text("Closing balances for ${formatMonthLabel(selectedMonth)}")
                if (state.accounts.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(accountCardCornerRadius),
                    ) {
                        Text("No Accounts yet", Modifier.fillMaxWidth().padding(accountCardPadding))
                    }
                }
                state.accounts.forEach { account ->
                    ShowAccountManagementCard(
                        account = account,
                        balanceMinor = state.monthlyOverview.closingBalancesMinor[account.id] ?: 0L,
                        currency = state.currency,
                        actions = actions,
                    )
                }
                Button(onClick = actions.addAccount, modifier = Modifier.fillMaxWidth()) { Text("Add Account") }
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun ShowAccountManagementCard(
    account: Account,
    balanceMinor: Long,
    currency: String,
    actions: AccountsScreenActions,
): Unit {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(accountCardCornerRadius)) {
        Column(
            Modifier.fillMaxWidth().padding(accountCardPadding),
            verticalArrangement = Arrangement.spacedBy(accountCardItemSpacing),
        ) {
            Text(account.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text("Type \u00B7 ${account.type.label}")
            if (account.isArchived) Text("Archived")
            Text("Balance \u00B7 $currency ${formatMoney(balanceMinor, currency)}", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(accountActionSpacing)) {
                TextButton(onClick = { actions.openHistory(account.id) }) { Text("View ${account.name} history") }
                if (!account.isArchived) {
                    TextButton(onClick = { actions.edit(account.id) }) { Text("Edit ${account.name}") }
                    TextButton(onClick = { actions.archive(account.id) }) { Text("Archive ${account.name}") }
                }
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun ShowAccountEditor(
    state: TransactionsUiState,
    actions: AccountsScreenActions,
): Unit {
    val isCreating: Boolean = state.editingAccountId == null
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(accountEditorCornerRadius)) {
        Column(
            Modifier.fillMaxWidth().padding(accountEditorPadding),
            verticalArrangement = Arrangement.spacedBy(accountEditorItemSpacing),
        ) {
            Text(if (isCreating) "New Account" else "Account details", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = if (isCreating) state.newAccountName else state.accountNameDraft,
                onValueChange = if (isCreating) actions.accountEditor.newNameChanged else actions.accountEditor.nameChanged,
                label = { Text("Account name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = if (isCreating) state.newAccountOpeningBalance else state.openingBalanceAmount,
                onValueChange =
                    if (isCreating) actions.accountEditor.newOpeningBalanceChanged else actions.accountEditor.openingBalanceChanged,
                label = { Text("Opening balance (${state.currency})") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = if (isCreating) state.newAccountOpeningDate else state.openingBalanceDate,
                onValueChange = if (isCreating) actions.accountEditor.newOpeningDateChanged else actions.accountEditor.openingDateChanged,
                label = { Text("Opening date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (isCreating) {
                Column(verticalArrangement = Arrangement.spacedBy(accountTypeItemSpacing)) {
                    Text("Account type", style = MaterialTheme.typography.titleMedium)
                    AccountType.entries.forEach { type ->
                        Row(
                            Modifier.fillMaxWidth().semantics {
                                stateDescription = if (state.newAccountType == type) "Selected" else "Not selected"
                            },
                        ) {
                            RadioButton(
                                selected = state.newAccountType == type,
                                onClick = { actions.accountEditor.newTypeChanged(type) },
                            )
                            Text(type.label, modifier = Modifier.padding(top = accountTypeLabelTopPadding))
                        }
                    }
                }
            }
            Button(
                onClick = if (isCreating) actions.accountEditor.create else actions.accountEditor.save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isCreating) "Create Account" else "Save changes")
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
internal fun ShowAccountMonthHeader(
    title: String,
    month: YearMonth,
    monthChanged: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().background(accountsCharcoal).padding(
            start = accountHeaderHorizontalPadding,
            end = accountHeaderHorizontalPadding,
            top = accountHeaderTopPadding,
            bottom = accountHeaderBottomPadding,
        ),
    ) {
        Text(title, color = Color.White, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(
                onClick = { monthChanged(month.minusMonths(1).toString()) },
                modifier = Modifier.semantics { contentDescription = "Previous month" },
            ) { Text("‹", color = accountsYellow, style = MaterialTheme.typography.headlineMedium) }
            Text(
                formatMonthLabel(month),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = accountMonthLabelTopPadding),
            )
            TextButton(
                onClick = { monthChanged(month.plusMonths(1).toString()) },
                modifier = Modifier.semantics { contentDescription = "Next month" },
            ) { Text("›", color = accountsYellow, style = MaterialTheme.typography.headlineMedium) }
        }
    }
}

private fun getAccountEditorTitle(state: TransactionsUiState): String =
    if (state.editingAccountId ==
        null
    ) {
        "Add Account"
    } else {
        "Edit Account"
    }

internal fun parseSelectedMonth(value: String): YearMonth = runCatching { YearMonth.parse(value) }.getOrDefault(YearMonth.now())

internal fun formatMonthLabel(month: YearMonth): String = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
