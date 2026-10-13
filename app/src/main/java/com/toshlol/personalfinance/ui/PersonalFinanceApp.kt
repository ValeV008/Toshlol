package com.toshlol.personalfinance.ui

import androidx.compose.runtime.Composable
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.ui.viewmodel.FirstRunUiState

@Composable
fun PersonalFinanceApp(
    uiState: FirstRunUiState,
    onCurrencyChanged: (String) -> Unit,
    onAccountNameChanged: (String) -> Unit,
    onAccountTypeChanged: (AccountType) -> Unit,
    onSubmit: () -> Unit
) {
    if (uiState.isSetupComplete) {
        OverviewScreen(currency = uiState.currency)
    } else {
        SetupScreen(
            state = uiState,
            onCurrencyChanged = onCurrencyChanged,
            onAccountNameChanged = onAccountNameChanged,
            onAccountTypeChanged = onAccountTypeChanged,
            onSubmit = onSubmit
        )
    }
}
