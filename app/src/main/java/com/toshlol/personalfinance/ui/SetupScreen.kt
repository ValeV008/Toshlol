package com.toshlol.personalfinance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.toshlol.personalfinance.data.AccountSetupValidator
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.ui.viewmodel.FirstRunUiState

@Composable
fun SetupScreen(
    state: FirstRunUiState,
    onCurrencyChanged: (String) -> Unit,
    onAccountNameChanged: (String) -> Unit,
    onAccountTypeChanged: (AccountType) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Set up your first account", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Text("Choose a currency and create an account to hold your income and expenses.")
        ChooseCurrency(state.currency, !state.isSaving, onCurrencyChanged)
        OutlinedTextField(
            value = state.accountName,
            onValueChange = onAccountNameChanged,
            label = { Text("Account name") },
            singleLine = true,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        ChooseAccountType(state.selectedType, !state.isSaving, onAccountTypeChanged)
        state.error?.let { message: String ->
            Text(
                message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Button(onClick = onSubmit, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.isSaving) "Saving account…" else "Create account")
        }
    }
}

@Composable
private fun ChooseCurrency(
    currency: String,
    isEnabled: Boolean,
    onCurrencyChanged: (String) -> Unit,
) {
    var isExpanded: Boolean by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(
            onClick = { isExpanded = true },
            enabled = isEnabled,
            modifier =
                Modifier.fillMaxWidth().semantics {
                    contentDescription = "Currency"
                    stateDescription = currency.ifEmpty { "Not selected" }
                },
        ) {
            Text("Currency: ${currency.ifEmpty { "Choose currency" }}")
        }
        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            AccountSetupValidator.supportedCurrencies.forEach { code: String ->
                DropdownMenuItem(text = { Text(code) }, onClick = {
                    onCurrencyChanged(code)
                    isExpanded = false
                })
            }
        }
    }
}

@Composable
private fun ChooseAccountType(
    selectedType: AccountType,
    isEnabled: Boolean,
    onTypeChanged: (AccountType) -> Unit,
) {
    Text("Account type", style = MaterialTheme.typography.titleMedium)
    Column(modifier = Modifier.selectableGroup()) {
        AccountType.entries.forEach { type: AccountType ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = selectedType == type,
                            enabled = isEnabled,
                            role = Role.RadioButton,
                            onClick = { onTypeChanged(type) },
                        ).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selectedType == type, onClick = null, enabled = isEnabled)
                Text(type.label, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}
