package com.toshlol.personalfinance

import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.AccountRepository
import com.toshlol.personalfinance.data.AccountSetupValidationResult
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.data.FirstRunSnapshot
import com.toshlol.personalfinance.domain.FirstRunSetup
import com.toshlol.personalfinance.domain.SetupOutcome
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstRunSetupTest {
    @Test
    fun `create first account normalizes currency and name and restores setup`(): Unit =
        runTest {
            val repository: InMemoryAccountRepository = InMemoryAccountRepository()
            val setup: FirstRunSetup = FirstRunSetup(repository)
            val actualOutcome: SetupOutcome = setup.createFirstAccount(" eur ", " Checking ", AccountType.BANK)
            assertTrue(actualOutcome is SetupOutcome.Completed)
            val actualSnapshot: FirstRunSnapshot = FirstRunSetup(repository).loadSetup()
            assertTrue(actualSnapshot.isSetupComplete)
            assertEquals("EUR", actualSnapshot.currency)
            assertEquals("Checking", actualSnapshot.accounts.single().name)
            assertEquals(AccountType.BANK, actualSnapshot.accounts.single().type)
        }

    @Test
    fun `invalid setup never persists currency or accounts`(): Unit =
        runTest {
            val repository: InMemoryAccountRepository = InMemoryAccountRepository()
            val setup: FirstRunSetup = FirstRunSetup(repository)
            val actualOutcomes: List<SetupOutcome> =
                listOf(
                    setup.createFirstAccount("", "Checking", AccountType.BANK),
                    setup.createFirstAccount("ZZZ", "Checking", AccountType.BANK),
                    setup.createFirstAccount("EUR", "  ", AccountType.BANK),
                    setup.createFirstAccount("EUR", "Checking", null),
                )
            assertTrue(actualOutcomes.all { it is SetupOutcome.Invalid })
            assertFalse(setup.loadSetup().isSetupComplete)
            assertEquals(0, repository.saveCount)
        }
}

internal class InMemoryAccountRepository : AccountRepository {
    var saveCount: Int = 0
        private set
    private var snapshot: FirstRunSnapshot = FirstRunSnapshot()

    override suspend fun loadSetup(): FirstRunSnapshot = snapshot

    override suspend fun createFirstAccount(setup: AccountSetupValidationResult): FirstRunSnapshot {
        if (snapshot.isSetupComplete) return snapshot
        saveCount += 1
        snapshot = FirstRunSnapshot(setup.currency, listOf(Account("test-account", setup.accountName, setup.selectedType, 1234L, 1234L)))
        return snapshot
    }
}
