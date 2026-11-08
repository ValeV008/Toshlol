package com.toshlol.personalfinance.data

data class AccountSetupValidationResult(
    val isValid: Boolean,
    val currency: String = "",
    val accountName: String = "",
    val selectedType: AccountType = AccountType.CASH,
    val errorMessage: String? = null,
) {
    init {
        if (isValid) {
            require(currency in AccountSetupValidator.supportedCurrencies)
            require(accountName.isNotBlank() && accountName == accountName.trim())
            require(errorMessage == null)
        }
    }
}

object AccountSetupValidator {
    val supportedCurrencies: Set<String> =
        setOf(
            "USD",
            "EUR",
            "GBP",
            "JPY",
            "CAD",
            "AUD",
            "CHF",
            "SEK",
            "NOK",
            "DKK",
        )

    fun validate(
        currency: String,
        accountName: String,
        selectedType: AccountType?,
    ): AccountSetupValidationResult {
        val trimmedCurrency: String = currency.trim().uppercase(java.util.Locale.ROOT)
        if (trimmedCurrency !in supportedCurrencies) {
            return AccountSetupValidationResult(
                isValid = false,
                errorMessage = "Pick a supported currency before continuing.",
            )
        }
        val trimmedName: String = accountName.trim()
        if (trimmedName.isEmpty()) {
            return AccountSetupValidationResult(
                isValid = false,
                errorMessage = "Enter an account name.",
            )
        }
        if (selectedType == null) {
            return AccountSetupValidationResult(
                isValid = false,
                errorMessage = "Choose an account type.",
            )
        }
        return AccountSetupValidationResult(
            isValid = true,
            currency = trimmedCurrency,
            accountName = trimmedName,
            selectedType = selectedType,
        )
    }
}
