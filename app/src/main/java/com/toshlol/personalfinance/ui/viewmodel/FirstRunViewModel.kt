package com.toshlol.personalfinance.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.data.FirstRunSnapshot
import com.toshlol.personalfinance.domain.FirstRunSetup
import com.toshlol.personalfinance.domain.SetupOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

data class FirstRunUiState(
    val currency: String = "",
    val accountName: String = "",
    val selectedType: AccountType = AccountType.CASH,
    val error: String? = null,
    val isSetupComplete: Boolean = false,
    val isLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val hasLoadFailed: Boolean = false,
)

class FirstRunViewModel(
    private val setup: FirstRunSetup,
) : ViewModel() {
    private val mutableUiState: MutableStateFlow<FirstRunUiState> = MutableStateFlow(FirstRunUiState())
    val uiState: StateFlow<FirstRunUiState> = mutableUiState.asStateFlow()

    init {
        loadSetup()
    }

    fun updateCurrency(value: String) {
        if (canEdit()) mutableUiState.value = uiState.value.copy(currency = value, error = null)
    }

    fun updateAccountName(value: String) {
        if (canEdit()) mutableUiState.value = uiState.value.copy(accountName = value, error = null)
    }

    fun updateAccountType(value: AccountType) {
        if (canEdit()) mutableUiState.value = uiState.value.copy(selectedType = value, error = null)
    }

    fun retryLoad() {
        if (uiState.value.hasLoadFailed) loadSetup()
    }

    fun submit() {
        if (!canEdit()) return
        val currentState: FirstRunUiState = uiState.value
        mutableUiState.value = currentState.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                when (
                    val outcome: SetupOutcome =
                        setup.createFirstAccount(
                            currentState.currency,
                            currentState.accountName,
                            currentState.selectedType,
                        )
                ) {
                    is SetupOutcome.Completed -> showSnapshot(outcome.snapshot)
                    is SetupOutcome.Invalid -> mutableUiState.value = currentState.copy(error = outcome.message)
                }
            } catch (exception: IOException) {
                mutableUiState.value = currentState.copy(error = "Could not save your account. Please try again.")
            }
        }
    }

    private fun canEdit(): Boolean = uiState.value.isLoaded && !uiState.value.isSaving && !uiState.value.isSetupComplete

    private fun loadSetup() {
        mutableUiState.value = FirstRunUiState()
        viewModelScope.launch {
            try {
                showSnapshot(setup.loadSetup())
            } catch (exception: IOException) {
                mutableUiState.value = FirstRunUiState(hasLoadFailed = true)
            }
        }
    }

    private fun showSnapshot(snapshot: FirstRunSnapshot) {
        mutableUiState.value =
            uiState.value.copy(
                currency = snapshot.currency,
                isSetupComplete = snapshot.isSetupComplete,
                isLoaded = true,
                isSaving = false,
                error = null,
            )
    }
}
