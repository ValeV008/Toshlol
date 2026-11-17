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
import androidx.compose.ui.unit.dp
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val AccountsPageGray: Color = Color(0xFFF1F2F4)
private val AccountsCharcoal: Color = Color(0xFF292B2F)
private val AccountsYellow: Color = Color(0xFFFFD23F)

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
    val selectedMonth: YearMonth = runCatching { YearMonth.parse(state.selectedMonth) }.getOrDefault(YearMonth.now())
    Column(Modifier.fillMaxSize().background(AccountsPageGray)) {
        AccountMonthHeader(
            title = if (state.isAccountEditorVisible) accountEditorTitle(state) else "Accounts",
            month = selectedMonth,
            monthChanged = actions.monthChanged,
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = if (state.isAccountEditorVisible) actions.closeEditor else actions.back) {
                Text(if (state.isAccountEditorVisible) "Back to Accounts" else "Back to overview")
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.isAccountEditorVisible) {
                AccountEditor(state, actions)
            } else {
                Text("Closing balances for ${selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))}")
                if (state.accounts.isEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                        Text("No Accounts yet", Modifier.fillMaxWidth().padding(18.dp))
                    }
                }
                state.accounts.forEach { account ->
                    AccountManagementCard(
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
private fun AccountManagementCard(
    account: Account,
    balanceMinor: Long,
    currency: String,
    actions: AccountsScreenActions,
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(account.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text("Type \u00B7 ${account.type.label}")
            if (account.isArchived) Text("Archived")
            Text("Balance \u00B7 $currency ${formatMoney(balanceMinor, currency)}", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
private fun AccountEditor(
    state: TransactionsUiState,
    actions: AccountsScreenActions,
) {
    val isCreating: Boolean = state.editingAccountId == null
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                            Text(type.label, modifier = Modifier.padding(top = 12.dp))
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
internal fun AccountMonthHeader(
    title: String,
    month: YearMonth,
    monthChanged: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(AccountsCharcoal).padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 10.dp)) {
        Text(title, color = Color.White, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(
                onClick = { monthChanged(month.minusMonths(1).toString()) },
                modifier = Modifier.semantics { contentDescription = "Previous month" },
            ) { Text("‹", color = AccountsYellow, style = MaterialTheme.typography.headlineMedium) }
            Text(
                month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
            TextButton(
                onClick = { monthChanged(month.plusMonths(1).toString()) },
                modifier = Modifier.semantics { contentDescription = "Next month" },
            ) { Text("›", color = AccountsYellow, style = MaterialTheme.typography.headlineMedium) }
        }
    }
}

private fun accountEditorTitle(state: TransactionsUiState): String = if (state.editingAccountId == null) "Add Account" else "Edit Account"
