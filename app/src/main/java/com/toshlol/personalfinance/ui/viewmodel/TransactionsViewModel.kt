package com.toshlol.personalfinance.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.BudgetDraft
import com.toshlol.personalfinance.data.BudgetRepository
import com.toshlol.personalfinance.data.CategoryBudget
import com.toshlol.personalfinance.data.CategoryDraft
import com.toshlol.personalfinance.data.CategoryTagRepository
import com.toshlol.personalfinance.data.FinanceCategory
import com.toshlol.personalfinance.data.FinanceTag
import com.toshlol.personalfinance.data.FinanceTransaction
import com.toshlol.personalfinance.data.FinanceTransfer
import com.toshlol.personalfinance.data.TagDraft
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.data.TransactionDraft
import com.toshlol.personalfinance.data.TransactionRepository
import com.toshlol.personalfinance.data.TransferDraft
import com.toshlol.personalfinance.data.TransferRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth
import java.util.Currency

data class TransactionsUiState(
    val isLoaded: Boolean = false,
    val currency: String = "EUR",
    val accountId: String = "",
    val accountName: String = "",
    val accountNameDraft: String = "",
    val openingBalanceAmount: String = "0.00",
    val openingBalanceDate: String = LocalDate.now().toString(),
    val newAccountName: String = "",
    val newAccountType: com.toshlol.personalfinance.data.AccountType = com.toshlol.personalfinance.data.AccountType.BANK,
    val newAccountOpeningBalance: String = "0.00",
    val newAccountOpeningDate: String = LocalDate.now().toString(),
    val accounts: List<Account> = emptyList(),
    val transactions: List<FinanceTransaction> = emptyList(),
    val transfers: List<FinanceTransfer> = emptyList(),
    val isTransferFormVisible: Boolean = false,
    val editingTransferId: String? = null,
    val transferSourceAccountId: String? = null,
    val transferDestinationAccountId: String? = null,
    val pendingTransferDeleteId: String? = null,
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
    val selectedMonth: String = "",
    val selectedAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val selectedTagId: String? = null,
    val selectedArea: String = "Overview",
)

class TransactionsViewModel(
    private val repository: TransactionRepository,
    private val loadAccount: suspend () -> Pair<String, String>,
    private val saveAccount: suspend (String, String, Long, Long) -> Account,
    private val createAccount: suspend (String, com.toshlol.personalfinance.data.AccountType, Long, Long) -> Account,
    private val loadCurrency: suspend () -> String,
    private val categoryTagRepository: CategoryTagRepository,
    private val budgetRepository: BudgetRepository,
    private val loadAccounts: suspend () -> List<Account>,
    private val transferRepository: TransferRepository,
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
                val accounts: List<Account> = loadAccounts()
                val transfers: List<FinanceTransfer> = transferRepository.loadTransfers()
                mutableUiState.value =
                    uiState.value.copy(
                        accountId = account.first,
                        accountName = account.second,
                        accountNameDraft = account.second,
                        openingBalanceAmount = formatAmount(accounts.firstOrNull()?.openingBalanceMinor ?: 0L, currency),
                        openingBalanceDate =
                            accounts.firstOrNull()?.openingBalanceDate?.let { LocalDate.ofEpochDay(it).toString() }
                                ?: LocalDate.now().toString(),
                        isLoaded = true,
                        currency = currency,
                        transactions = transactions,
                        categories = organization.categories,
                        tags = organization.tags,
                        budgets = budgets,
                        accounts = accounts,
                        transfers = transfers,
                    )
            } catch (
                exception: IOException,
            ) {
                mutableUiState.value =
                    uiState.value.copy(isLoaded = true, error = "Could not load transactions.")
            }
        }
    }

    fun selectArea(area: String) {
        mutableUiState.value = uiState.value.copy(selectedArea = area)
    }

    fun updateSelectedMonth(value: String) {
        mutableUiState.value = uiState.value.copy(selectedMonth = value)
    }

    fun updateSelectedAccount(id: String?) {
        mutableUiState.value = uiState.value.copy(selectedAccountId = id)
    }

    fun updateSelectedCategory(id: String?) {
        mutableUiState.value = uiState.value.copy(selectedCategoryId = id)
    }

    fun updateSelectedTag(id: String?) {
        mutableUiState.value = uiState.value.copy(selectedTagId = id)
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

    fun beginTransfer() {
        val accounts: List<Account> = uiState.value.accounts
        mutableUiState.value =
            uiState.value.copy(
                isTransferFormVisible = true,
                editingTransferId = null,
                amount = "",
                date = LocalDate.now().toString(),
                transferSourceAccountId = accounts.firstOrNull()?.id,
                transferDestinationAccountId = accounts.getOrNull(1)?.id,
                error = null,
            )
    }

    fun beginEditTransfer(transfer: FinanceTransfer) {
        val fractionDigits: Int = fractionDigits(uiState.value.currency)
        mutableUiState.value =
            uiState.value.copy(
                isTransferFormVisible = true,
                editingTransferId = transfer.id,
                amount =
                    java.math.BigDecimal
                        .valueOf(transfer.amountMinor, fractionDigits)
                        .toPlainString(),
                date = LocalDate.ofEpochDay(transfer.date).toString(),
                transferSourceAccountId = transfer.sourceAccountId,
                transferDestinationAccountId = transfer.destinationAccountId,
                error = null,
            )
    }

    fun closeTransferForm() {
        mutableUiState.value = uiState.value.copy(isTransferFormVisible = false, error = null)
    }

    fun selectTransferSource(id: String) {
        mutableUiState.value = uiState.value.copy(transferSourceAccountId = id, error = null)
    }

    fun selectTransferDestination(id: String) {
        mutableUiState.value = uiState.value.copy(transferDestinationAccountId = id, error = null)
    }

    fun saveTransfer() {
        val state: TransactionsUiState = uiState.value
        val amountMinor: Long? =
            state.amount
                .toBigDecimalOrNull()
                ?.movePointRight(fractionDigits(state.currency))
                ?.longValueExactOrNull()
        val localDate: LocalDate? = runCatching { LocalDate.parse(state.date) }.getOrNull()
        val sourceId: String? = state.transferSourceAccountId
        val destinationId: String? = state.transferDestinationAccountId
        if (amountMinor == null || amountMinor <= 0 || localDate == null) {
            mutableUiState.value = state.copy(error = "Enter a positive amount and a valid date.")
            return
        }
        if (sourceId == null || destinationId == null || sourceId == destinationId) {
            mutableUiState.value = state.copy(error = "Choose two different Accounts.")
            return
        }
        viewModelScope.launch {
            try {
                val transfers: List<FinanceTransfer> =
                    transferRepository.save(
                        TransferDraft(state.editingTransferId, sourceId, destinationId, amountMinor, localDate.toEpochDay()),
                    )
                mutableUiState.value =
                    uiState.value.copy(transfers = transfers, accounts = loadAccounts(), isTransferFormVisible = false, error = null)
            } catch (exception: Exception) {
                mutableUiState.value = uiState.value.copy(error = exception.message ?: "Could not save Transfer.")
            }
        }
    }

    fun requestDeleteTransfer(id: String) {
        mutableUiState.value = uiState.value.copy(pendingTransferDeleteId = id)
    }

    fun cancelDeleteTransfer() {
        mutableUiState.value = uiState.value.copy(pendingTransferDeleteId = null)
    }

    fun confirmDeleteTransfer() {
        val id: String = uiState.value.pendingTransferDeleteId ?: return
        viewModelScope.launch {
            try {
                val transfers: List<FinanceTransfer> = transferRepository.delete(id)
                mutableUiState.value =
                    uiState.value.copy(
                        transfers = transfers,
                        accounts = loadAccounts(),
                        pendingTransferDeleteId = null,
                        isTransferFormVisible = false,
                    )
            } catch (exception: IOException) {
                mutableUiState.value = uiState.value.copy(pendingTransferDeleteId = null, error = "Could not delete Transfer.")
            }
        }
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
                val openingMinor: Long? = parseAmount(state.openingBalanceAmount, state.currency)
                val openingDate: LocalDate? = runCatching { LocalDate.parse(state.openingBalanceDate) }.getOrNull()
                require(openingMinor != null && openingDate != null) { "Enter a valid opening balance and date." }
                val account: Account = saveAccount(state.accountId, state.accountNameDraft, openingMinor, openingDate.toEpochDay())
                val transactions: List<FinanceTransaction> = repository.loadTransactions()
                val accounts: List<Account> = loadAccounts()
                mutableUiState.value =
                    uiState.value.copy(
                        accountName = account.name,
                        accountNameDraft = account.name,
                        openingBalanceAmount = formatAmount(account.openingBalanceMinor, state.currency),
                        openingBalanceDate = LocalDate.ofEpochDay(account.openingBalanceDate).toString(),
                        accounts = accounts,
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

    fun updateOpeningBalanceAmount(value: String) {
        mutableUiState.value = uiState.value.copy(openingBalanceAmount = value, error = null)
    }

    fun updateOpeningBalanceDate(value: String) {
        mutableUiState.value = uiState.value.copy(openingBalanceDate = value, error = null)
    }

    fun updateNewAccountName(value: String) {
        mutableUiState.value = uiState.value.copy(newAccountName = value, error = null)
    }

    fun updateNewAccountType(value: com.toshlol.personalfinance.data.AccountType) {
        mutableUiState.value =
            uiState.value.copy(newAccountType = value, error = null)
    }

    fun updateNewAccountOpeningBalance(value: String) {
        mutableUiState.value =
            uiState.value.copy(newAccountOpeningBalance = value, error = null)
    }

    fun updateNewAccountOpeningDate(value: String) {
        mutableUiState.value = uiState.value.copy(newAccountOpeningDate = value, error = null)
    }

    fun createAccount() {
        val state: TransactionsUiState = uiState.value
        val amountMinor: Long? = parseAmount(state.newAccountOpeningBalance, state.currency)
        val date: LocalDate? = runCatching { LocalDate.parse(state.newAccountOpeningDate) }.getOrNull()
        if (amountMinor == null || date == null || state.newAccountName.isBlank()) {
            mutableUiState.value = state.copy(error = "Enter an account name, signed opening balance, and valid date.")
            return
        }
        viewModelScope.launch {
            try {
                createAccount(state.newAccountName, state.newAccountType, amountMinor, date.toEpochDay())
                mutableUiState.value =
                    uiState.value.copy(
                        accounts = loadAccounts(),
                        newAccountName = "",
                        newAccountOpeningBalance = "0.00",
                        newAccountOpeningDate = LocalDate.now().toString(),
                        error = null,
                    )
            } catch (exception: Exception) {
                mutableUiState.value = uiState.value.copy(error = exception.message ?: "Could not create Account.")
            }
        }
    }

    private fun parseAmount(
        value: String,
        currency: String,
    ): Long? = value.toBigDecimalOrNull()?.movePointRight(fractionDigits(currency))?.longValueExactOrNull()

    private fun formatAmount(
        amountMinor: Long,
        currency: String,
    ): String =
        java.math.BigDecimal
            .valueOf(amountMinor, fractionDigits(currency))
            .toPlainString()

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
        mutableUiState.value = uiState.value.copy(selectedArea = "Budgets")
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
        mutableUiState.value = uiState.value.copy(isBudgetsVisible = false, selectedArea = "Overview", error = null)
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
        val budgetMonth: YearMonth? = runCatching { YearMonth.parse(state.budgetMonth) }.getOrNull()
        if (amountMinor == null || amountMinor <= 0 || budgetMonth == null) {
            mutableUiState.value = state.copy(error = "Enter a positive amount and a valid month.")
            return
        }
        viewModelScope.launch {
            try {
                val budgets: List<CategoryBudget> = budgetRepository.save(BudgetDraft(categoryId, budgetMonth, amountMinor))
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
                val transactions: List<FinanceTransaction> = repository.delete(id)
                val budgets: List<CategoryBudget> = budgetRepository.load()
                val accounts: List<Account> = loadAccounts()
                mutableUiState.value =
                    uiState.value.copy(transactions = transactions, budgets = budgets, accounts = accounts, pendingDeleteId = null)
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
                val budgets: List<CategoryBudget> = budgetRepository.load()
                val accounts: List<Account> = loadAccounts()
                mutableUiState.value =
                    uiState.value.copy(
                        transactions = newState,
                        budgets = budgets,
                        accounts = accounts,
                        isFormVisible = false,
                        isSaving = false,
                    )
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
