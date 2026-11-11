package com.toshlol.personalfinance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.data.FinanceTransaction
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.ui.viewmodel.FirstRunUiState
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState

@Composable
fun PersonalFinanceApp(
    uiState: FirstRunUiState,
    onCurrencyChanged: (String) -> Unit,
    onAccountNameChanged: (String) -> Unit,
    onAccountTypeChanged: (AccountType) -> Unit,
    onSubmit: () -> Unit,
    onRetry: () -> Unit = {},
    transactionsState: TransactionsUiState = TransactionsUiState(),
    onCreateTransaction: (TransactionDirection) -> Unit = {},
    onEditTransaction: (FinanceTransaction) -> Unit = {},
    onDeleteTransaction: (String) -> Unit = {},
    onCancelDelete: () -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    onAmountChanged: (String) -> Unit = {},
    onDateChanged: (String) -> Unit = {},
    onNoteChanged: (String) -> Unit = {},
    onDirectionChanged: (TransactionDirection) -> Unit = {},
    onSaveTransaction: () -> Unit = {},
    onCloseTransactionForm: () -> Unit = {},
) {
    Surface(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        when {
            !uiState.isLoaded -> ShowLoading(uiState.hasLoadFailed, onRetry)
            uiState.isSetupComplete ->
                OverviewScreen(
                    transactionsState.copy(currency = uiState.currency),
                    onCreateTransaction,
                    onEditTransaction,
                    onDeleteTransaction,
                    onCancelDelete,
                    onConfirmDelete,
                    onAmountChanged,
                    onDateChanged,
                    onNoteChanged,
                    onDirectionChanged,
                    onSaveTransaction,
                    onCloseTransactionForm,
                )
            else -> SetupScreen(uiState, onCurrencyChanged, onAccountNameChanged, onAccountTypeChanged, onSubmit)
        }
    }
}

@Composable
private fun ShowLoading(
    hasFailed: Boolean,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (hasFailed) {
            Text("Could not load your ledger. Your saved data has not been changed.")
            Button(onClick = onRetry) { Text("Try again") }
        } else {
            CircularProgressIndicator()
            Text("Loading your ledger…")
        }
    }
}
