package com.toshlol.personalfinance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.toshlol.personalfinance.data.AccountRepository
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.AppPreferences
import com.toshlol.personalfinance.ui.PersonalFinanceApp
import com.toshlol.personalfinance.ui.theme.ToshlolTheme
import com.toshlol.personalfinance.ui.viewmodel.FirstRunViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getInstance(applicationContext)
        val preferences = AppPreferences(applicationContext)
        val repository = AccountRepository(database, preferences)
        val viewModel = FirstRunViewModel(repository)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            ToshlolTheme {
                PersonalFinanceApp(uiState = uiState, onCurrencyChanged = viewModel::updateCurrency,
                    onAccountNameChanged = viewModel::updateAccountName,
                    onAccountTypeChanged = viewModel::updateAccountType,
                    onSubmit = viewModel::submit)
            }
        }
    }
}
