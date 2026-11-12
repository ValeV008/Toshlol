package com.toshlol.personalfinance.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toshlol.personalfinance.data.BudgetDraft
import com.toshlol.personalfinance.data.BudgetRepository
import com.toshlol.personalfinance.data.CategoryBudget
import com.toshlol.personalfinance.data.CategoryDraft
import com.toshlol.personalfinance.data.CategoryTagRepository
import com.toshlol.personalfinance.data.FinanceCategory
import com.toshlol.personalfinance.data.FinanceTag
import com.toshlol.personalfinance.data.FinanceTransaction
import com.toshlol.personalfinance.data.TagDraft
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
    val isLoaded: Boolean = false,
    val currency: String = "EUR",
    val accountId: String = "",
    val accountName: String = "",
    val accountNameDraft: String = "",
    val transactions: List<FinanceTransaction> = emptyList(),
    val categories: List<FinanceCategory> = emptyList(),
    val tags: List<FinanceTag> = emptyList(),
    val isFormVisible: Boolean = false,
    val editingId: String? = null,
    val amount: String = "",
    val direction: TransactionDirection = TransactionDirection.EXPENSE,
    val date: String = LocalDate.now().toString(),
    val note: String = "",
    val categoryId: String? = null,
    val tagIds: Set<String> = emptySet(),
    val error: String? = null,
    val isSaving: Boolean = false,
    val pendingDeleteId: String? = null,
    val isOrganizerVisible: Boolean = false,
    val categoryName: String = "",
    val categoryDirection: TransactionDirection = TransactionDirection.EXPENSE,
    val editingCategoryId: String? = null,
    val tagName: String = "",
    val tagCategoryId: String? = null,
    val editingTagId: String? = null,
    val isBudgetsVisible: Boolean = false,
    val budgets: List<CategoryBudget> = emptyList(),
    val budgetMonth: String =
        java.time.YearMonth
            .now()
            .toString(),
    val budgetCategoryId: String? = null,
    val budgetAmount: String = "",
)

class TransactionsViewModel(
    private val repository: TransactionRepository,
    private val loadAccount: suspend () -> Pair<String, String>,
    private val renameAccount: suspend (String, String) -> Pair<String, String>,
    private val loadCurrency: suspend () -> String,
    private val categoryTagRepository: CategoryTagRepository,
    private val budgetRepository: BudgetRepository,
) : ViewModel() {
    private val mutableUiState: MutableStateFlow<TransactionsUiState> = MutableStateFlow(TransactionsUiState())
    val uiState = mutableUiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            try {
                val account: Pair<String, String> = loadAccount()
                val currency: String = loadCurrency()
                val transactions: List<FinanceTransaction> = repository.loadTransactions()
                val organization: com.toshlol.personalfinance.data.CategoryTagSnapshot = categoryTagRepository.load()
                val budgets: List<CategoryBudget> = budgetRepository.load()
                mutableUiState.value =
                    uiState.value.copy(
                        accountId = account.first,
                        accountName = account.second,
                        accountNameDraft = account.second,
                        isLoaded = true,
                        currency = currency,
                        transactions = transactions,
                        categories = organization.categories,
                        tags = organization.tags,
                        budgets = budgets,
                    )
            } catch (
                exception: IOException,
            ) {
                mutableUiState.value =
                    uiState.value.copy(isLoaded = true, error = "Could not load transactions.")
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
                categoryId = null,
                tagIds = emptySet(),
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
                categoryId = transaction.categoryId,
                tagIds = transaction.tagIds.toSet(),
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

    fun updateCategory(value: String?) {
        mutableUiState.value =
            uiState.value.copy(
                categoryId = value,
                tagIds =
                    uiState.value.tagIds.filterTo(mutableSetOf()) { tagId ->
                        uiState.value.tags
                            .firstOrNull { it.id == tagId }
                            ?.categoryId
                            .let { it == null || it == value }
                    },
                error = null,
            )
    }

    fun toggleTag(id: String) {
        val selected: Set<String> = uiState.value.tagIds
        mutableUiState.value = uiState.value.copy(tagIds = if (id in selected) selected - id else selected + id, error = null)
    }

    fun showOrganizer() {
        mutableUiState.value = uiState.value.copy(isOrganizerVisible = true, error = null)
    }

    fun updateAccountName(value: String) {
        mutableUiState.value = uiState.value.copy(accountNameDraft = value, error = null)
    }

    fun saveAccountName() {
        val state: TransactionsUiState = uiState.value
        mutableUiState.value = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                val account: Pair<String, String> = renameAccount(state.accountId, state.accountNameDraft)
                val transactions: List<FinanceTransaction> = repository.loadTransactions()
                mutableUiState.value =
                    uiState.value.copy(
                        accountName = account.second,
                        accountNameDraft = account.second,
                        transactions = transactions,
                        isSaving = false,
                        error = null,
                    )
            } catch (exception: IllegalArgumentException) {
                mutableUiState.value = uiState.value.copy(isSaving = false, error = exception.message ?: "Could not rename account.")
            } catch (exception: IOException) {
                mutableUiState.value = uiState.value.copy(isSaving = false, error = "Could not rename account.")
            }
        }
    }

    fun closeOrganizer() {
        mutableUiState.value = uiState.value.copy(isOrganizerVisible = false, error = null)
        viewModelScope.launch {
            try {
                val organization: com.toshlol.personalfinance.data.CategoryTagSnapshot = categoryTagRepository.load()
                mutableUiState.value = uiState.value.copy(categories = organization.categories, tags = organization.tags)
            } catch (exception: IOException) {
                mutableUiState.value = uiState.value.copy(error = "Could not load categories and tags.")
            }
        }
    }

    fun showBudgets() {
        viewModelScope.launch {
            try {
                mutableUiState.value =
                    uiState.value.copy(
                        isBudgetsVisible = true,
                        budgets = budgetRepository.load(),
                        budgetMonth =
                            java.time.YearMonth
                                .now()
                                .toString(),
                        budgetCategoryId = null,
                        budgetAmount = "",
                        error = null,
                    )
            } catch (exception: IOException) {
                mutableUiState.value = uiState.value.copy(error = "Could not load budgets.")
            }
        }
    }

    fun closeBudgets() {
        mutableUiState.value = uiState.value.copy(isBudgetsVisible = false, error = null)
    }

    fun updateBudgetMonth(value: String) {
        mutableUiState.value = uiState.value.copy(budgetMonth = value, error = null)
    }

    fun updateBudgetCategory(value: String?) {
        mutableUiState.value = uiState.value.copy(budgetCategoryId = value, error = null)
    }

    fun updateBudgetAmount(value: String) {
        mutableUiState.value = uiState.value.copy(budgetAmount = value, error = null)
    }

    fun saveBudget() {
        val state: TransactionsUiState = uiState.value
        val categoryId: String =
            state.budgetCategoryId ?: run {
                mutableUiState.value = state.copy(error = "Choose an expense category.")
                return
            }
        val amountMinor: Long? =
            state.budgetAmount
                .toBigDecimalOrNull()
                ?.movePointRight(
                    fractionDigits(state.currency),
                )?.longValueExactOrNull()
        if (amountMinor == null || amountMinor <= 0 || runCatching { java.time.YearMonth.parse(state.budgetMonth) }.isFailure) {
            mutableUiState.value = state.copy(error = "Enter a positive amount and a valid month.")
            return
        }
        viewModelScope.launch {
            try {
                val budgets: List<CategoryBudget> = budgetRepository.save(BudgetDraft(categoryId, state.budgetMonth, amountMinor))
                mutableUiState.value = uiState.value.copy(budgets = budgets, budgetAmount = "", budgetCategoryId = null, error = null)
            } catch (exception: Exception) {
                mutableUiState.value = uiState.value.copy(error = exception.message ?: "Could not save budget.")
            }
        }
    }

    fun updateCategoryName(value: String) {
        mutableUiState.value = uiState.value.copy(categoryName = value, error = null)
    }

    fun updateCategoryDirection(value: TransactionDirection) {
        mutableUiState.value = uiState.value.copy(categoryDirection = value, error = null)
    }

    fun beginCategoryEdit(category: FinanceCategory) {
        mutableUiState.value =
            uiState.value.copy(
                editingCategoryId = category.id,
                categoryName = category.name,
                categoryDirection = category.direction,
                error = null,
            )
    }

    fun saveCategory() {
        val state: TransactionsUiState = uiState.value
        mutableUiState.value = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                val result: com.toshlol.personalfinance.data.CategoryTagSnapshot =
                    categoryTagRepository.saveCategory(
                        CategoryDraft(state.editingCategoryId, state.categoryName, state.categoryDirection),
                    )
                mutableUiState.value =
                    uiState.value.copy(
                        categories = result.categories,
                        tags = result.tags,
                        categoryName = "",
                        editingCategoryId = null,
                        isSaving = false,
                        error = null,
                    )
            } catch (exception: IllegalArgumentException) {
                mutableUiState.value = uiState.value.copy(isSaving = false, error = exception.message ?: "Could not save category.")
            } catch (exception: IOException) {
                mutableUiState.value = uiState.value.copy(isSaving = false, error = "Could not save category.")
            }
        }
    }

    fun archiveCategory(id: String) {
        viewModelScope.launch {
            try {
                val result: com.toshlol.personalfinance.data.CategoryTagSnapshot = categoryTagRepository.archiveCategory(id)
                mutableUiState.value = uiState.value.copy(categories = result.categories, tags = result.tags, error = null)
            } catch (exception: IllegalArgumentException) {
                mutableUiState.value = uiState.value.copy(error = "Could not archive category.")
            } catch (exception: IOException) {
                mutableUiState.value = uiState.value.copy(error = "Could not archive category.")
            }
        }
    }

    fun updateTagName(value: String) {
        mutableUiState.value = uiState.value.copy(tagName = value, error = null)
    }

    fun updateTagCategory(value: String?) {
        mutableUiState.value = uiState.value.copy(tagCategoryId = value, error = null)
    }

    fun beginTagEdit(tag: FinanceTag) {
        mutableUiState.value = uiState.value.copy(editingTagId = tag.id, tagName = tag.name, tagCategoryId = tag.categoryId, error = null)
    }

    fun saveTag() {
        val state: TransactionsUiState = uiState.value
        mutableUiState.value = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                val result: com.toshlol.personalfinance.data.CategoryTagSnapshot =
                    categoryTagRepository.saveTag(
                        TagDraft(state.editingTagId, state.tagName, state.tagCategoryId),
                    )
                mutableUiState.value =
                    uiState.value.copy(
                        categories = result.categories,
                        tags = result.tags,
                        tagName = "",
                        tagCategoryId = null,
                        editingTagId = null,
                        isSaving = false,
                        error = null,
                    )
            } catch (exception: IllegalArgumentException) {
                mutableUiState.value = uiState.value.copy(isSaving = false, error = exception.message ?: "Could not save tag.")
            } catch (exception: IOException) {
                mutableUiState.value = uiState.value.copy(isSaving = false, error = "Could not save tag.")
            }
        }
    }

    fun archiveTag(id: String) {
        viewModelScope.launch {
            try {
                val result: com.toshlol.personalfinance.data.CategoryTagSnapshot = categoryTagRepository.archiveTag(id)
                mutableUiState.value = uiState.value.copy(categories = result.categories, tags = result.tags, error = null)
            } catch (exception: IllegalArgumentException) {
                mutableUiState.value = uiState.value.copy(error = "Could not archive tag.")
            } catch (exception: IOException) {
                mutableUiState.value = uiState.value.copy(error = "Could not archive tag.")
            }
        }
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
                        TransactionDraft(
                            state.editingId,
                            state.accountId,
                            minorAmount,
                            state.direction,
                            epochDay,
                            state.note,
                            state.categoryId,
                            state.tagIds,
                        ),
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
