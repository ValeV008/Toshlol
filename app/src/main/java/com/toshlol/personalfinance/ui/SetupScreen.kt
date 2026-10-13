package com.toshlol.personalfinance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.ui.viewmodel.FirstRunUiState

@Composable
fun SetupScreen(
    state: FirstRunUiState,
    onCurrencyChanged: (String) -> Unit,
    onAccountNameChanged: (String) -> Unit,
    onAccountTypeChanged: (AccountType) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Set up your first account",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Choose a base currency and create the account you will use to track spending.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = state.currency,
            onValueChange = onCurrencyChanged,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Currency" },
            label = { Text("Currency") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Account type",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AccountType.entries.forEach { accountType ->
                OutlinedButton(
                    onClick = { onAccountTypeChanged(accountType) },
                    modifier = Modifier
                        .weight(1f)
                        .selectable(
                            selected = state.selectedType == accountType,
                            onClick = { onAccountTypeChanged(accountType) }
                        ),
                    enabled = true
                ) {
                    Text(accountType.label)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedTextField(
            value = state.accountName,
            onValueChange = onAccountNameChanged,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Account name" },
            label = { Text("Account name") },
            singleLine = true
        )

        if (state.error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = state.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Create account" }
        ) {
            Text("Create account")
        }
    }
}
