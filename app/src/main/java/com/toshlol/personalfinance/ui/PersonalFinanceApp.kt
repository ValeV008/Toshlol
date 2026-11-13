package com.toshlol.personalfinance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.data.FinanceCategory
import com.toshlol.personalfinance.data.FinanceTag
import com.toshlol.personalfinance.data.FinanceTransaction
import com.toshlol.personalfinance.data.FinanceTransfer
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.ui.viewmodel.FirstRunUiState
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState

data class LedgerNavigationActions(
    val selectArea: (String) -> Unit = {},
    val monthChanged: (String) -> Unit = {},
    val accountChanged: (String?) -> Unit = {},
    val categoryChanged: (String?) -> Unit = {},
    val tagChanged: (String?) -> Unit = {},
)

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
    onCreateTransfer: () -> Unit = {},
    onEditTransfer: (FinanceTransfer) -> Unit = {},
    onCloseTransfer: () -> Unit = {},
    onSaveTransfer: () -> Unit = {},
    onTransferSourceChanged: (String) -> Unit = {},
    onTransferDestinationChanged: (String) -> Unit = {},
    onDeleteTransfer: (String) -> Unit = {},
    onCancelDeleteTransfer: () -> Unit = {},
    onConfirmDeleteTransfer: () -> Unit = {},
    onCloseTransactionForm: () -> Unit = {},
    onOpenOrganizer: () -> Unit = {},
    onOrganizerAccountNameChanged: (String) -> Unit = {},
    onSaveAccountName: () -> Unit = {},
    onOpeningBalanceAmountChanged: (String) -> Unit = {},
    onOpeningBalanceDateChanged: (String) -> Unit = {},
    onNewAccountNameChanged: (String) -> Unit = {},
    onNewAccountTypeChanged: (com.toshlol.personalfinance.data.AccountType) -> Unit = {},
    onNewAccountOpeningBalanceChanged: (String) -> Unit = {},
    onNewAccountOpeningDateChanged: (String) -> Unit = {},
    onCreateAccount: () -> Unit = {},
    onCloseOrganizer: () -> Unit = {},
    onCategoryChanged: (String?) -> Unit = {},
    onTagToggled: (String) -> Unit = {},
    onCategoryNameChanged: (String) -> Unit = {},
    onCategoryDirectionChanged: (TransactionDirection) -> Unit = {},
    onSaveCategory: () -> Unit = {},
    onEditCategory: (FinanceCategory) -> Unit = {},
    onArchiveCategory: (String) -> Unit = {},
    onTagNameChanged: (String) -> Unit = {},
    onTagCategoryChanged: (String?) -> Unit = {},
    onSaveTag: () -> Unit = {},
    onEditTag: (FinanceTag) -> Unit = {},
    onArchiveTag: (String) -> Unit = {},
    onOpenBudgets: () -> Unit = {},
    onCloseBudgets: () -> Unit = {},
    onBudgetMonthChanged: (String) -> Unit = {},
    onBudgetCategoryChanged: (String?) -> Unit = {},
    onBudgetAmountChanged: (String) -> Unit = {},
    onSaveBudget: () -> Unit = {},
    navigationActions: LedgerNavigationActions = LedgerNavigationActions(),
) {
    Surface(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                when {
                    !uiState.isLoaded -> ShowLoading(uiState.hasLoadFailed, onRetry)
                    uiState.isSetupComplete && !transactionsState.isLoaded -> ShowLoading(false) {}
                    uiState.isSetupComplete && transactionsState.isOrganizerVisible ->
                        CategoryTagScreen(
                            transactionsState,
                            onCloseOrganizer,
                            onCategoryNameChanged,
                            onCategoryDirectionChanged,
                            onSaveCategory,
                            onEditCategory,
                            onArchiveCategory,
                            onTagNameChanged,
                            onTagCategoryChanged,
                            onSaveTag,
                            onEditTag,
                            onArchiveTag,
                            onOrganizerAccountNameChanged,
                            onSaveAccountName,
                            onOpeningBalanceAmountChanged,
                            onOpeningBalanceDateChanged,
                            onNewAccountNameChanged,
                            onNewAccountTypeChanged,
                            onNewAccountOpeningBalanceChanged,
                            onNewAccountOpeningDateChanged,
                            onCreateAccount,
                        )
                    uiState.isSetupComplete && transactionsState.selectedArea == "Budgets" ->
                        BudgetsScreen(
                            transactionsState.copy(currency = uiState.currency),
                            onCloseBudgets,
                            onBudgetMonthChanged,
                            onBudgetCategoryChanged,
                            onBudgetAmountChanged,
                            onSaveBudget,
                            { onCreateTransaction(TransactionDirection.EXPENSE) },
                            onCreateTransfer,
                        )
                    uiState.isSetupComplete && transactionsState.selectedArea == "Transactions" ->
                        TransactionsScreen(
                            transactionsState.copy(currency = uiState.currency),
                            TransactionsScreenActions(
                                create = onCreateTransaction,
                                edit = onEditTransaction,
                                delete = onDeleteTransaction,
                                monthChanged = navigationActions.monthChanged,
                                accountChanged = navigationActions.accountChanged,
                                categoryChanged = navigationActions.categoryChanged,
                                tagChanged = navigationActions.tagChanged,
                                createTransfer = onCreateTransfer,
                                editTransfer = onEditTransfer,
                                deleteTransfer = onDeleteTransfer,
                            ),
                        )
                    uiState.isSetupComplete && transactionsState.selectedArea == "Settings" ->
                        SettingsScreen(
                            transactionsState,
                            onOpenOrganizer,
                            { onCreateTransaction(TransactionDirection.EXPENSE) },
                            onCreateTransfer,
                        )
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
                            onOpenOrganizer,
                            onCategoryChanged,
                            onTagToggled,
                            onOpenBudgets,
                            onCreateTransfer,
                            onEditTransfer,
                        )
                    else -> SetupScreen(uiState, onCurrencyChanged, onAccountNameChanged, onAccountTypeChanged, onSubmit)
                }
                if (uiState.isSetupComplete && transactionsState.isLoaded) {
                    TransactionDialogs(
                        transactionsState,
                        TransactionDialogActions(
                            form =
                                TransactionFormActions(
                                    close = onCloseTransactionForm,
                                    amountChanged = onAmountChanged,
                                    dateChanged = onDateChanged,
                                    noteChanged = onNoteChanged,
                                    directionChanged = onDirectionChanged,
                                    save = onSaveTransaction,
                                    categoryChanged = onCategoryChanged,
                                    tagToggled = onTagToggled,
                                ),
                            delete = TransactionDeleteActions(onCancelDelete, onConfirmDelete),
                        ),
                    )
                    TransferDialog(
                        transactionsState,
                        onCloseTransfer,
                        onSaveTransfer,
                        onAmountChanged,
                        onDateChanged,
                        onTransferSourceChanged,
                        onTransferDestinationChanged,
                        onDeleteTransfer,
                        onCancelDeleteTransfer,
                        onConfirmDeleteTransfer,
                    )
                }
            }
            if (uiState.isSetupComplete && transactionsState.isLoaded && !transactionsState.isOrganizerVisible) {
                NavigationBar {
                    Row(Modifier.fillMaxWidth()) {
                        listOf("Overview", "Transactions", "Budgets", "Settings").forEach { area: String ->
                            Button(
                                onClick = { navigationActions.selectArea(area) },
                                modifier =
                                    Modifier.weight(1f).testTag("navigation-$area").semantics {
                                        stateDescription = if (transactionsState.selectedArea == area) "Current section" else "Section"
                                    },
                            ) {
                                Text(area)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    state: TransactionsUiState,
    onOpenOrganizer: () -> Unit,
    onCreateTransaction: () -> Unit,
    onCreateTransfer: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Text("Currency: ${state.currency}")
        Text("Account: ${state.accountName}")
        Button(onClick = onCreateTransaction) { Text("Add transaction") }
        Button(onClick = onCreateTransfer) { Text("Transfer") }
        Text("Data management", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Button(onClick = onOpenOrganizer) { Text("Manage categories and tags") }
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
