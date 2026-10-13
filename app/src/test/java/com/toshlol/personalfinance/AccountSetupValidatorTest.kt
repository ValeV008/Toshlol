package com.toshlol.personalfinance

import com.toshlol.personalfinance.data.AccountSetupValidator
import com.toshlol.personalfinance.data.AccountType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountSetupValidatorTest {
    @Test
    fun `validate accepts supported currency and type`() {
        val result = AccountSetupValidator.validate("usd", "Main checking", AccountType.BANK)

        assertTrue(result.isValid)
        assertTrue(result.currency == "USD")
        assertTrue(result.accountName == "Main checking")
    }

    @Test
    fun `validate rejects empty currency`() {
        val result = AccountSetupValidator.validate("", "Main checking", AccountType.BANK)

        assertFalse(result.isValid)
    }

    @Test
    fun `validate rejects empty account name`() {
        val result = AccountSetupValidator.validate("EUR", "  ", AccountType.CASH)

        assertFalse(result.isValid)
    }
}
