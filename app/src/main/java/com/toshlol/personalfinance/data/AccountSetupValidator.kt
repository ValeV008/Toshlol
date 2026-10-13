package com.toshlol.personalfinance.data

data class AccountSetupValidationResult(
    val isValid: Boolean,
    val currency: String = "",
    val accountName: String = "",
    val selectedType: AccountType = AccountType.CASH,
    val errorMessage: String? = null
)

object AccountSetupValidator {
    private val supportedCurrencies = setOf(
        "USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "SEK", "NOK", "DKK"
    )

    fun validate(
        currency: String,
        accountName: String,
        selectedType: AccountType?
    ): AccountSetupValidationResult {
        val trimmedCurrency = currency.trim().uppercase()
        if (trimmedCurrency.isEmpty() || trimmedCurrency.length != 3 || !supportedCurrencies.contains(trimmedCurrency)) {
            return AccountSetupValidationResult(
                isValid = false,
                errorMessage = "Pick a supported currency before continuing."
            )
        }

        val trimmedName = accountName.trim()
        if (trimmedName.isEmpty()) {
            return AccountSetupValidationResult(
                isValid = false,
                errorMessage = "Enter an account name."
            )
        }

        if (selectedType == null) {
            return AccountSetupValidationResult(
                isValid = false,
                errorMessage = "Choose an account type."
            )
        }

        return AccountSetupValidationResult(
            isValid = true,
            currency = trimmedCurrency,
            accountName = trimmedName,
            selectedType = selectedType
        )
    }
}
