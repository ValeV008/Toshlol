package com.toshlol.personalfinance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState

data class AccountsScreenActions(
    val back: () -> Unit,
    val openHistory: (String) -> Unit,
    val edit: (String) -> Unit,
    val archive: (String) -> Unit,
    val addAccount: () -> Unit,
)

@Composable
@Suppress("ktlint:standard:no-unit-return")
fun AccountsScreen(
    state: TransactionsUiState,
    actions: AccountsScreenActions,
): Unit {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Accounts", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        OutlinedButton(onClick = actions.back) { Text("Back to overview") }
        if (state.accounts.isEmpty()) Text("No Accounts yet")
        state.accounts.forEach { account ->
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium)
                Text("Type · ${account.type.label}")
                if (account.isArchived) Text("Archived")
                Text("Balance · ${state.currency} ${formatMoney(account.balanceMinor, state.currency)}")
                TextButton(onClick = { actions.openHistory(account.id) }) { Text("View ${account.name} history") }
                if (!account.isArchived) {
                    TextButton(onClick = { actions.edit(account.id) }) { Text("Edit ${account.name}") }
                    TextButton(onClick = { actions.archive(account.id) }) { Text("Archive ${account.name}") }
                }
            }
        }
        Button(onClick = actions.addAccount) { Text("Add Account") }
    }
}
