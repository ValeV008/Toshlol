package com.toshlol.personalfinance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.toshlol.personalfinance.ui.PersonalFinanceApp
import com.toshlol.personalfinance.ui.theme.ToshlolTheme
import com.toshlol.personalfinance.ui.viewmodel.FirstRunUiState
import com.toshlol.personalfinance.ui.viewmodel.FirstRunViewModel
import com.toshlol.personalfinance.ui.viewmodel.TransactionsViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: FirstRunViewModel by viewModels {
        viewModelFactory {
            initializer { FirstRunViewModel((application as PersonalFinanceApplication).firstRunSetup) }
        }
    }
    private val transactionsViewModel: TransactionsViewModel by viewModels {
        viewModelFactory {
            initializer {
                val application: PersonalFinanceApplication = application as PersonalFinanceApplication
                TransactionsViewModel(application.transactionRepository, {
                    application.firstRunSetup.loadSetup().accounts.firstOrNull()?.let {
                        it.id to
                            it.name
                    }
                        ?: ("" to "")
                }, { application.firstRunSetup.loadSetup().currency })
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val uiState: FirstRunUiState by viewModel.uiState.collectAsStateWithLifecycle()
            val transactionsState by transactionsViewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(uiState.isSetupComplete) { if (uiState.isSetupComplete) transactionsViewModel.refresh() }
            ToshlolTheme {
                PersonalFinanceApp(
                    uiState = uiState,
                    onCurrencyChanged = viewModel::updateCurrency,
                    onAccountNameChanged = viewModel::updateAccountName,
                    onAccountTypeChanged = viewModel::updateAccountType,
                    onSubmit = viewModel::submit,
                    onRetry = viewModel::retryLoad,
                    transactionsState = transactionsState,
                    onCreateTransaction = transactionsViewModel::beginCreate,
                    onEditTransaction = transactionsViewModel::beginEdit,
                    onDeleteTransaction = transactionsViewModel::requestDelete,
                    onCancelDelete = transactionsViewModel::cancelDelete,
                    onConfirmDelete = transactionsViewModel::confirmDelete,
                    onAmountChanged = transactionsViewModel::updateAmount,
                    onDateChanged = transactionsViewModel::updateDate,
                    onNoteChanged = transactionsViewModel::updateNote,
                    onDirectionChanged = transactionsViewModel::updateDirection,
                    onSaveTransaction = transactionsViewModel::save,
                    onCloseTransactionForm = transactionsViewModel::closeForm,
                )
            }
        }
    }
}
