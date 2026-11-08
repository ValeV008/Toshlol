package com.toshlol.personalfinance.domain

import com.toshlol.personalfinance.data.AccountRepository
import com.toshlol.personalfinance.data.AccountSetupValidationResult
import com.toshlol.personalfinance.data.AccountSetupValidator
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.data.FirstRunSnapshot

sealed interface SetupOutcome {
    data class Completed(
        val snapshot: FirstRunSnapshot,
    ) : SetupOutcome

    data class Invalid(
        val message: String,
    ) : SetupOutcome
}

class FirstRunSetup(
    private val repository: AccountRepository,
) {
    suspend fun loadSetup(): FirstRunSnapshot = repository.loadSetup()

    suspend fun createFirstAccount(
        currency: String,
        accountName: String,
        type: AccountType?,
    ): SetupOutcome {
        val validation: AccountSetupValidationResult = AccountSetupValidator.validate(currency, accountName, type)
        if (!validation.isValid) return SetupOutcome.Invalid(requireNotNull(validation.errorMessage))
        return SetupOutcome.Completed(repository.createFirstAccount(validation))
    }
}
