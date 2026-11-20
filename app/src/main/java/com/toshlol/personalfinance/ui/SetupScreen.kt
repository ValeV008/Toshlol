package com.toshlol.personalfinance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
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
@Suppress("ktlint:standard:no-unit-return")
fun SetupScreen(
    state: FirstRunUiState,
    onCurrencyChanged: (String) -> Unit,
    onAccountNameChanged: (String) -> Unit,
    onAccountTypeChanged: (AccountType) -> Unit,
    onSubmit: () -> Unit,
): Unit {
    val keyboardController: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(24.dp)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("GET STARTED", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Text(
                "Set up your first account",
                color = MaterialTheme.colorScheme.onSecondary,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                "Choose a currency and create an account to hold your income and expenses.",
                color = MaterialTheme.colorScheme.onSecondary,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Currency", style = MaterialTheme.typography.titleMedium)
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
                Button(
                    onClick = {
                        keyboardController?.hide()
                        onSubmit()
                    },
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        }
                        Text(if (state.isSaving) "Saving account…" else "Create account")
                    }
                }
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun ChooseCurrency(
    currency: String,
    isEnabled: Boolean,
    onCurrencyChanged: (String) -> Unit,
): Unit {
    var isExpanded: Boolean by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { isExpanded = true },
            enabled = isEnabled,
            modifier =
                Modifier.fillMaxWidth().heightIn(min = 52.dp).semantics {
                    contentDescription = "Currency"
                    stateDescription = currency.ifEmpty { "Not selected" }
                },
        ) {
            Text("Currency: ${currency.ifEmpty { "Choose currency" }}")
        }
        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            AccountSetupValidator.supportedCurrencies.forEach { code: String ->
                DropdownMenuItem(
                    text = { Text(code) },
                    onClick = {
                        onCurrencyChanged(code)
                        isExpanded = false
                    },
                )
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun ChooseAccountType(
    selectedType: AccountType,
    isEnabled: Boolean,
    onTypeChanged: (AccountType) -> Unit,
): Unit {
    Text("Account type", style = MaterialTheme.typography.titleMedium)
    Column(modifier = Modifier.selectableGroup()) {
        AccountType.entries.forEach { type: AccountType ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .selectable(
                            selected = selectedType == type,
                            enabled = isEnabled,
                            role = Role.RadioButton,
                            onClick = { onTypeChanged(type) },
                        ).semantics {
                            contentDescription = "${type.label} account type"
                            stateDescription = if (selectedType == type) "Selected" else "Not selected"
                        }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selectedType == type, onClick = null, enabled = isEnabled)
                Text(type.label, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}
