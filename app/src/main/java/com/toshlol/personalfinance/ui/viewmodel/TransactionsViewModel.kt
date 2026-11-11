package com.toshlol.personalfinance.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toshlol.personalfinance.data.FinanceTransaction
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.data.TransactionDraft
import com.toshlol.personalfinance.data.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.util.Currency

data class TransactionsUiState(
    val currency: String = "EUR",
    val accountId: String = "",
    val transactions: List<FinanceTransaction> = emptyList(),
    val isFormVisible: Boolean = false,
    val editingId: String? = null,
    val amount: String = "",
    val direction: TransactionDirection = TransactionDirection.EXPENSE,
    val date: String = LocalDate.now().toString(),
    val note: String = "",
    val error: String? = null,
    val isSaving: Boolean = false,
    val pendingDeleteId: String? = null,
)

class TransactionsViewModel(
    private val repository: TransactionRepository,
    private val loadAccount: suspend () -> Pair<String, String>,
    private val loadCurrency: suspend () -> String,
) : ViewModel() {
    private val mutableUiState: MutableStateFlow<TransactionsUiState> = MutableStateFlow(TransactionsUiState())
    val uiState = mutableUiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            try {
                val account: Pair<String, String> = loadAccount()
                val currency: String = loadCurrency()
                val transactions: List<FinanceTransaction> = repository.loadTransactions()
                mutableUiState.value =
                    uiState.value.copy(accountId = account.first, currency = currency, transactions = transactions)
            } catch (
                exception: IOException,
            ) {
                mutableUiState.value =
                    uiState.value.copy(error = "Could not load transactions.")
            }
        }
    }

    fun beginCreate(direction: TransactionDirection) {
        mutableUiState.value =
            uiState.value.copy(
                isFormVisible = true,
                editingId = null,
                amount = "",
                direction = direction,
                date = LocalDate.now().toString(),
                note = "",
                error = null,
            )
    }

    fun beginEdit(transaction: FinanceTransaction) {
        val fractionDigits: Int = fractionDigits(uiState.value.currency)
        mutableUiState.value =
            uiState.value.copy(
                isFormVisible = true,
                editingId = transaction.id,
                amount =
                    java.math.BigDecimal
                        .valueOf(transaction.amountMinor, fractionDigits)
                        .toPlainString(),
                direction = transaction.direction,
                date = LocalDate.ofEpochDay(transaction.date).toString(),
                note = transaction.note.orEmpty(),
                error = null,
            )
    }

    fun closeForm() {
        mutableUiState.value = uiState.value.copy(isFormVisible = false, error = null)
    }

    fun updateAmount(value: String) {
        mutableUiState.value = uiState.value.copy(amount = value, error = null)
    }

    fun updateDate(value: String) {
        mutableUiState.value = uiState.value.copy(date = value, error = null)
    }

    fun updateNote(value: String) {
        mutableUiState.value = uiState.value.copy(note = value, error = null)
    }

    fun updateDirection(value: TransactionDirection) {
        mutableUiState.value = uiState.value.copy(direction = value, error = null)
    }

    fun requestDelete(id: String) {
        mutableUiState.value = uiState.value.copy(pendingDeleteId = id)
    }

    fun cancelDelete() {
        mutableUiState.value = uiState.value.copy(pendingDeleteId = null)
    }

    fun confirmDelete() {
        val id: String =
            uiState.value.pendingDeleteId ?: return
        viewModelScope.launch {
            try {
                mutableUiState.value =
                    uiState.value.copy(transactions = repository.delete(id), pendingDeleteId = null)
            } catch (
                exception: IOException,
            ) {
                mutableUiState.value =
                    uiState.value.copy(error = "Could not delete transaction.", pendingDeleteId = null)
            }
        }
    }

    fun save() {
        val state: TransactionsUiState = uiState.value
        val minorAmount: Long? =
            state.amount
                .toBigDecimalOrNull()
                ?.movePointRight(fractionDigits(state.currency))
                ?.longValueExactOrNull()
        val localDate: LocalDate? = runCatching { LocalDate.parse(state.date) }.getOrNull()
        if (minorAmount ==
            null ||
            minorAmount <= 0 ||
            localDate == null
        ) {
            mutableUiState.value = state.copy(error = "Enter a positive amount and a valid date.")
            return
        }
        mutableUiState.value =
            state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                val epochDay: Long = localDate.toEpochDay()
                val newState: List<FinanceTransaction> =
                    repository.save(
                        TransactionDraft(state.editingId, state.accountId, minorAmount, state.direction, epochDay, state.note),
                    )
                mutableUiState.value =
                    uiState.value.copy(transactions = newState, isFormVisible = false, isSaving = false)
            } catch (
                exception: Exception,
            ) {
                mutableUiState.value =
                    uiState.value.copy(isSaving = false, error = "Could not save transaction. Please try again.")
            }
        }
    }

    private fun fractionDigits(currency: String): Int =
        runCatching {
            Currency.getInstance(currency).defaultFractionDigits.coerceAtLeast(0)
        }.getOrDefault(2)

    private fun java.math.BigDecimal.longValueExactOrNull(): Long? = runCatching { longValueExact() }.getOrNull()
}
