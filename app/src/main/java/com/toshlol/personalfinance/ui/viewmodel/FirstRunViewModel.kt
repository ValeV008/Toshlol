package com.toshlol.personalfinance.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toshlol.personalfinance.data.AccountRepository
import com.toshlol.personalfinance.data.AccountSetupValidator
import com.toshlol.personalfinance.data.AccountType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FirstRunUiState(
    val currency: String = "",
    val accountName: String = "",
    val selectedType: AccountType = AccountType.CASH,
    val error: String? = null,
    val hasAccount: Boolean = false,
    val isLoaded: Boolean = false
) {
    val isSetupComplete: Boolean
        get() = currency.isNotBlank() && hasAccount
}

class FirstRunViewModel(
    private val repository: AccountRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(FirstRunUiState())
    val uiState: StateFlow<FirstRunUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedCurrency = repository.loadCurrency() ?: ""
            val hasAccount = repository.hasAccount()
            _uiState.value = FirstRunUiState(
                currency = savedCurrency,
                hasAccount = hasAccount,
                isLoaded = true
            )
        }
    }

    fun updateCurrency(value: String) {
        _uiState.value = _uiState.value.copy(currency = value, error = null)
    }

    fun updateAccountName(value: String) {
        _uiState.value = _uiState.value.copy(accountName = value, error = null)
    }

    fun updateAccountType(value: AccountType) {
        _uiState.value = _uiState.value.copy(selectedType = value, error = null)
    }

    fun submit() {
        val currentState = _uiState.value
        val validation = AccountSetupValidator.validate(
            currency = currentState.currency,
            accountName = currentState.accountName,
            selectedType = currentState.selectedType
        )

        if (!validation.isValid) {
            _uiState.value = currentState.copy(error = validation.errorMessage)
            return
        }

        viewModelScope.launch {
            repository.saveCurrency(validation.currency)
            repository.createAccount(validation.accountName, validation.selectedType)
            _uiState.value = currentState.copy(
                currency = validation.currency,
                accountName = validation.accountName,
                selectedType = validation.selectedType,
                error = null,
                hasAccount = true,
                isLoaded = true
            )
        }
    }
}
