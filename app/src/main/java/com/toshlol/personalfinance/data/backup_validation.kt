@file:Suppress("ktlint:standard:filename")

package com.toshlol.personalfinance.data

import java.time.LocalDate
import java.time.YearMonth

internal object BackupValidator {
    fun validate(backup: FinanceBackup) {
        val recordCount: Int =
            backup.accounts.size + backup.categories.size + backup.tags.size + backup.transactions.size +
                backup.transfers.size + backup.budgets.size
        require(recordCount <= MAX_BACKUP_RECORDS) { "Backup contains too many records." }
        require(backup.settings.currency.matches(Regex("[A-Z]{3}"))) { "Backup currency is invalid." }
        val identifiers: List<String> = collectIdentifiers(backup)
        require(identifiers.all(String::isNotBlank) && identifiers.distinct().size == identifiers.size) {
            "Backup contains duplicate or empty IDs."
        }
        requireUniqueNames(backup.accounts.filterNot(BackupAccount::archived).map(BackupAccount::name), "Account")
        requireUniqueNames(backup.categories.filterNot(BackupCategory::archived).map { "${it.direction}:${it.name}" }, "Category")
        requireUniqueNames(backup.tags.filterNot(BackupTag::archived).map(BackupTag::name), "Tag")
        val accounts: Map<String, BackupAccount> = backup.accounts.associateBy(BackupAccount::id)
        val categories: Map<String, BackupCategory> = backup.categories.associateBy(BackupCategory::id)
        val tags: Map<String, BackupTag> = backup.tags.associateBy(BackupTag::id)
        validateAccounts(backup.accounts)
        validateCategories(backup.categories)
        validateTags(backup.tags, categories)
        validateTransactions(backup.transactions, accounts, categories, tags)
        validateTransfers(backup.transfers, accounts)
        validateBudgets(backup.budgets, categories)
    }

    private fun collectIdentifiers(backup: FinanceBackup): List<String> =
        backup.accounts.map(BackupAccount::id) + backup.categories.map(BackupCategory::id) +
            backup.tags.map(BackupTag::id) + backup.transactions.map(BackupTransaction::id) +
            backup.transfers.map(BackupTransfer::id) + backup.budgets.map(BackupBudget::id)

    private fun validateAccounts(accounts: List<BackupAccount>) {
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

    private fun validateCategories(categories: List<BackupCategory>) {
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
    ) {
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
        accounts: Map<String, BackupAccount>,
        categories: Map<String, BackupCategory>,
        tags: Map<String, BackupTag>,
    ) {
        transactions.forEach { transaction ->
            require(
                transaction.accountId in accounts &&
                    transaction.amountMinor > 0 &&
                    transaction.direction in TransactionDirection.entries.map(TransactionDirection::name) &&
                    transaction.updatedAt >= transaction.createdAt &&
                    isValidDate(transaction.date),
            ) { "Backup contains an invalid Transaction." }
            require(transaction.categoryId == null || categories[transaction.categoryId]?.direction == transaction.direction) {
                "Backup Transaction has a missing or incompatible Category."
            }
            require(
                transaction.tagIds.distinct().size == transaction.tagIds.size &&
                    transaction.tagIds.all { tagId ->
                        tags[tagId] != null &&
                            (tags[tagId]?.categoryId == null || tags[tagId]?.categoryId == transaction.categoryId)
                    },
            ) { "Backup Transaction has missing or incompatible Tags." }
        }
    }

    private fun validateTransfers(
        transfers: List<BackupTransfer>,
        accounts: Map<String, BackupAccount>,
    ) {
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
    ) {
        budgets.forEach { budget ->
            require(
                categories[budget.categoryId]?.direction == TransactionDirection.EXPENSE.name &&
                    budget.amountMinor > 0 &&
                    budget.updatedAt >= budget.createdAt &&
                    runCatching { YearMonth.parse(budget.month) }.isSuccess,
            ) { "Backup contains an invalid Budget." }
        }
        val monthlyBudgetKeys: List<String> = budgets.map { "${it.categoryId}:${it.month}" }
        require(monthlyBudgetKeys.distinct().size == budgets.size) { "Backup contains duplicate monthly Budgets." }
    }

    private fun requireUniqueNames(
        names: List<String>,
        entity: String,
    ) {
        require(names.all { it.isNotBlank() && it == it.trim() } && names.map(String::lowercase).distinct().size == names.size) {
            "Backup contains conflicting active $entity names."
        }
    }

    private fun isValidDate(epochDay: Long): Boolean = runCatching { LocalDate.ofEpochDay(epochDay) }.isSuccess
}
