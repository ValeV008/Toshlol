@file:Suppress("ktlint:standard:filename", "ktlint:standard:no-unit-return")

package com.toshlol.personalfinance.data

import java.time.LocalDate
import java.time.YearMonth

private data class BackupReferences(
    val accounts: Map<String, BackupAccount>,
    val categories: Map<String, BackupCategory>,
    val tags: Map<String, BackupTag>,
)

internal object BackupValidator {
    fun validate(backup: FinanceBackup): Unit {
        val recordCount: Int =
            backup.accounts.size + backup.categories.size + backup.tags.size + backup.transactions.size +
                backup.transfers.size + backup.budgets.size + backup.recurringBudgets.size
        require(recordCount <= MAX_BACKUP_RECORDS) { "Backup contains too many records." }
        require(backup.settings.currency.matches(Regex("[A-Z]{3}"))) { "Backup currency is invalid." }
        val identifiers: List<String> = collectIdentifiers(backup)
        require(identifiers.all(String::isNotBlank) && identifiers.distinct().size == identifiers.size) {
            "Backup contains duplicate or empty IDs."
        }
        requireUniqueNames(backup.accounts.filterNot(BackupAccount::archived).map(BackupAccount::name), "Account")
        requireUniqueNames(backup.categories.filterNot(BackupCategory::archived).map { "${it.direction}:${it.name}" }, "Category")
        requireUniqueNames(backup.tags.filterNot(BackupTag::archived).map(BackupTag::name), "Tag")
        val references: BackupReferences =
            BackupReferences(
                backup.accounts.associateBy(BackupAccount::id),
                backup.categories.associateBy(BackupCategory::id),
                backup.tags.associateBy(BackupTag::id),
            )
        validateAccounts(backup.accounts)
        validateCategories(backup.categories)
        validateTags(backup.tags, references.categories)
        validateTransactions(backup.transactions, references)
        validateTransfers(backup.transfers, references.accounts)
        validateBudgets(backup.budgets, references.categories, references.accounts)
        validateRecurringBudgets(backup.recurringBudgets, references.accounts)
    }

    private fun collectIdentifiers(backup: FinanceBackup): List<String> =
        backup.accounts.map(BackupAccount::id) + backup.categories.map(BackupCategory::id) +
            backup.tags.map(BackupTag::id) + backup.transactions.map(BackupTransaction::id) +
            backup.transfers.map(BackupTransfer::id) + backup.budgets.map(BackupBudget::id) +
            backup.recurringBudgets.map(BackupRecurringBudget::id)

    private fun validateAccounts(accounts: List<BackupAccount>): Unit {
        accounts.forEach { account ->
            require(
                account.name.isNotBlank() &&
                    account.name == account.name.trim() &&
                    account.type in AccountType.entries.map(AccountType::name) &&
                    account.updatedAt >= account.createdAt &&
                    isValidDate(account.openingBalanceDate),
            ) { "Backup contains an invalid Account." }
        }
    }

    private fun validateCategories(categories: List<BackupCategory>): Unit {
        categories.forEach { category ->
            require(
                category.name.isNotBlank() &&
                    category.name == category.name.trim() &&
                    category.direction in TransactionDirection.entries.map(TransactionDirection::name) &&
                    category.updatedAt >= category.createdAt,
            ) { "Backup contains an invalid Category." }
        }
    }

    private fun validateTags(
        tags: List<BackupTag>,
        categories: Map<String, BackupCategory>,
    ): Unit {
        tags.forEach { tag ->
            require(
                tag.name.isNotBlank() &&
                    tag.name == tag.name.trim() &&
                    (tag.categoryId == null || tag.categoryId in categories) &&
                    tag.updatedAt >= tag.createdAt,
            ) { "Backup contains an invalid Tag." }
        }
    }

    private fun validateTransactions(
        transactions: List<BackupTransaction>,
        references: BackupReferences,
    ): Unit {
        transactions.forEach { transaction ->
            require(
                transaction.accountId in references.accounts &&
                    transaction.amountMinor > 0 &&
                    transaction.direction in TransactionDirection.entries.map(TransactionDirection::name) &&
                    transaction.updatedAt >= transaction.createdAt &&
                    isValidDate(transaction.date),
            ) { "Backup contains an invalid Transaction." }
            require(transaction.categoryId == null || references.categories[transaction.categoryId]?.direction == transaction.direction) {
                "Backup Transaction has a missing or incompatible Category."
            }
            require(
                transaction.tagIds.distinct().size == transaction.tagIds.size &&
                    transaction.tagIds.all { tagId ->
                        references.tags[tagId] != null &&
                            (references.tags[tagId]?.categoryId == null || references.tags[tagId]?.categoryId == transaction.categoryId)
                    },
            ) { "Backup Transaction has missing or incompatible Tags." }
        }
    }

    private fun validateTransfers(
        transfers: List<BackupTransfer>,
        accounts: Map<String, BackupAccount>,
    ): Unit {
        transfers.forEach { transfer ->
            require(
                transfer.sourceAccountId in accounts &&
                    transfer.destinationAccountId in accounts &&
                    transfer.sourceAccountId != transfer.destinationAccountId &&
                    transfer.amountMinor > 0 &&
                    transfer.updatedAt >= transfer.createdAt &&
                    isValidDate(transfer.date),
            ) { "Backup contains an invalid Transfer." }
        }
    }

    private fun validateBudgets(
        budgets: List<BackupBudget>,
        categories: Map<String, BackupCategory>,
        accounts: Map<String, BackupAccount>,
    ): Unit {
        budgets.forEach { budget ->
            require(
                categories[budget.categoryId]?.direction == TransactionDirection.EXPENSE.name &&
                    budget.amountMinor > 0 &&
                    budget.updatedAt >= budget.createdAt &&
                    runCatching { YearMonth.parse(budget.month) }.isSuccess &&
                    budget.accountIds.distinct().size == budget.accountIds.size &&
                    budget.accountIds.all { it in accounts },
            ) { "Backup contains an invalid Budget." }
        }
        val monthlyBudgetKeys: List<String> = budgets.map { "${it.categoryId}:${it.month}" }
        require(monthlyBudgetKeys.distinct().size == budgets.size) { "Backup contains duplicate monthly Budgets." }
    }

    private fun validateRecurringBudgets(
        budgets: List<BackupRecurringBudget>,
        accounts: Map<String, BackupAccount>,
    ): Unit {
        budgets.forEach { budget ->
            require(
                budget.name.isNotBlank() &&
                    budget.name == budget.name.trim() &&
                    budget.limitMinor > 0 &&
                    budget.updatedAt >= budget.createdAt &&
                    budget.accountIds.isNotEmpty() &&
                    budget.accountIds.distinct().size == budget.accountIds.size &&
                    budget.accountIds.all { it in accounts },
            ) { "Backup contains an invalid Budget." }
        }
    }

    private fun requireUniqueNames(
        names: List<String>,
        entity: String,
    ): Unit {
        require(names.all { it.isNotBlank() && it == it.trim() } && names.map(String::lowercase).distinct().size == names.size) {
            "Backup contains conflicting active $entity names."
        }
    }

    private fun isValidDate(epochDay: Long): Boolean = runCatching { LocalDate.ofEpochDay(epochDay) }.isSuccess
}
