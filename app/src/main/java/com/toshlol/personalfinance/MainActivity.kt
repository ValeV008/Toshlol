package com.toshlol.personalfinance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.toshlol.personalfinance.ui.PersonalFinanceApp
import com.toshlol.personalfinance.ui.theme.ToshlolTheme
import com.toshlol.personalfinance.ui.viewmodel.FirstRunUiState
import com.toshlol.personalfinance.ui.viewmodel.FirstRunViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: FirstRunViewModel by viewModels {
        viewModelFactory {
            initializer { FirstRunViewModel((application as PersonalFinanceApplication).firstRunSetup) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val uiState: FirstRunUiState by viewModel.uiState.collectAsStateWithLifecycle()
            ToshlolTheme {
                PersonalFinanceApp(
                    uiState = uiState,
                    onCurrencyChanged = viewModel::updateCurrency,
                    onAccountNameChanged = viewModel::updateAccountName,
                    onAccountTypeChanged = viewModel::updateAccountType,
                    onSubmit = viewModel::submit,
                    onRetry = viewModel::retryLoad,
                )
            }
        }
    }
}
