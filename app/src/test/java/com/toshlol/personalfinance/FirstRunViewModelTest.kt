package com.toshlol.personalfinance

import com.toshlol.personalfinance.data.AccountRepository
import com.toshlol.personalfinance.data.AccountSetupValidationResult
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.data.FirstRunSnapshot
import com.toshlol.personalfinance.domain.FirstRunSetup
import com.toshlol.personalfinance.ui.viewmodel.FirstRunViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class FirstRunViewModelTest {
    @Test
    fun `setup stays unavailable until saved data has loaded`(): Unit =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val viewModel: FirstRunViewModel = FirstRunViewModel(FirstRunSetup(InMemoryAccountRepository()))
                viewModel.updateAccountName("Ignored while loading")
                viewModel.submit()
                assertFalse(viewModel.uiState.value.isLoaded)
                runCurrent()
                assertTrue(viewModel.uiState.value.isLoaded)
                assertFalse(viewModel.uiState.value.isSetupComplete)
                assertEquals("", viewModel.uiState.value.accountName)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `saving blocks duplicate submissions and edits then shows persisted overview`(): Unit =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val saveGate: CompletableDeferred<Unit> = CompletableDeferred()
                val savedRepository: InMemoryAccountRepository = InMemoryAccountRepository()
                var saveAttempts: Int = 0
                val repository: AccountRepository =
                    object : AccountRepository by savedRepository {
                        override suspend fun createFirstAccount(setup: AccountSetupValidationResult): FirstRunSnapshot {
                            saveAttempts += 1
                            saveGate.await()
                            return savedRepository.createFirstAccount(setup)
                        }
                    }
                val viewModel: FirstRunViewModel = FirstRunViewModel(FirstRunSetup(repository))
                runCurrent()
                viewModel.updateCurrency("EUR")
                viewModel.updateAccountName("Checking")
                viewModel.updateAccountType(AccountType.BANK)
                viewModel.submit()
                viewModel.submit()
                viewModel.updateAccountName("Ignored while saving")
                runCurrent()
                assertTrue(viewModel.uiState.value.isSaving)
                assertFalse(viewModel.uiState.value.isSetupComplete)
                saveGate.complete(Unit)
                runCurrent()
                assertTrue(viewModel.uiState.value.isSetupComplete)
                assertFalse(viewModel.uiState.value.isSaving)
                assertEquals(1, saveAttempts)
                assertEquals(
                    "Checking",
                    FirstRunSetup(savedRepository)
                        .loadSetup()
                        .accounts
                        .single()
                        .name,
                )
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `storage failure preserves form and allows retry`(): Unit =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val savedRepository: InMemoryAccountRepository = InMemoryAccountRepository()
                var shouldFail: Boolean = true
                val repository: AccountRepository =
                    object : AccountRepository by savedRepository {
                        override suspend fun createFirstAccount(setup: AccountSetupValidationResult): FirstRunSnapshot {
                            if (shouldFail) throw IOException("Disk full")
                            return savedRepository.createFirstAccount(setup)
                        }
                    }
                val viewModel: FirstRunViewModel = FirstRunViewModel(FirstRunSetup(repository))
                runCurrent()
                viewModel.updateCurrency("EUR")
                viewModel.updateAccountName("Cash")
                viewModel.submit()
                runCurrent()
                assertFalse(viewModel.uiState.value.isSetupComplete)
                assertFalse(viewModel.uiState.value.isSaving)
                assertEquals("Cash", viewModel.uiState.value.accountName)
                assertEquals("Could not save your account. Please try again.", viewModel.uiState.value.error)
                shouldFail = false
                viewModel.submit()
                runCurrent()
                assertTrue(viewModel.uiState.value.isSetupComplete)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `load failure offers retry without exposing setup`(): Unit =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val savedRepository: InMemoryAccountRepository = InMemoryAccountRepository()
                var shouldFail: Boolean = true
                val repository: AccountRepository =
                    object : AccountRepository by savedRepository {
                        override suspend fun loadSetup(): FirstRunSnapshot {
                            if (shouldFail) throw IOException("Read failure")
                            return savedRepository.loadSetup()
                        }
                    }
                val viewModel: FirstRunViewModel = FirstRunViewModel(FirstRunSetup(repository))
                runCurrent()
                assertTrue(viewModel.uiState.value.hasLoadFailed)
                assertFalse(viewModel.uiState.value.isLoaded)
                shouldFail = false
                viewModel.retryLoad()
                runCurrent()
                assertTrue(viewModel.uiState.value.isLoaded)
                assertFalse(viewModel.uiState.value.hasLoadFailed)
            } finally {
                Dispatchers.resetMain()
            }
        }
}
