@file:Suppress("ktlint:standard:filename", "ktlint:standard:no-unit-return")

package com.toshlol.personalfinance.data

internal class RoomBackupStore(
    private val database: AppDatabase,
) {
    suspend fun load(): FinanceBackup? {
        val settingsRows: List<AppSettings> = database.settingsDao().loadAll()
        if (settingsRows.isEmpty()) {
            requireEmptyLedger()
            return null
        }
        val settings: AppSettings =
            settingsRows.singleOrNull()
                ?: throw BackupImportRejected("local_data_invalid", "Local ledger settings are invalid.")
        return createSnapshot(settings)
    }

    suspend fun save(
        backup: FinanceBackup,
        mode: BackupImportMode,
    ): Unit {
        if (mode == BackupImportMode.OVERWRITE) clearLedger()
        database.settingsDao().saveSettings(AppSettings(currency = backup.settings.currency))
        saveAccounts(backup.accounts)
        saveCategories(backup.categories)
        saveTags(backup.tags)
        saveTransactions(backup.transactions)
        saveTransfers(backup.transfers)
        saveBudgets(backup.budgets)
    }

    private suspend fun requireEmptyLedger(): Unit {
        val hasLocalData: Boolean =
            database.accountDao().loadAccounts().isNotEmpty() ||
                database.categoryTagDao().loadCategories().isNotEmpty() ||
                database.categoryTagDao().loadTags().isNotEmpty() ||
                database.transactionDao().loadAll().isNotEmpty() ||
                database.transferDao().loadAll().isNotEmpty() ||
                database.budgetDao().loadAll().isNotEmpty()
        if (hasLocalData) throw BackupImportRejected("local_data_invalid", "Local ledger settings are missing.")
    }

    private suspend fun createSnapshot(settings: AppSettings): FinanceBackup {
        val transactionTagAssociationsById: Map<String, List<TransactionTagCrossRef>> =
            database.transactionDao().loadAllTransactionTags().groupBy(TransactionTagCrossRef::transactionId)
        return FinanceBackup(
            CURRENT_BACKUP_SCHEMA_VERSION,
            BackupSettings(settings.currency),
            database.accountDao().loadAccounts().map(::createBackupAccount),
            database.categoryTagDao().loadCategories().map(::createBackupCategory),
            database.categoryTagDao().loadTags().map(::createBackupTag),
            database.transactionDao().loadAll().map { transaction -> createBackupTransaction(transaction, transactionTagAssociationsById) },
            database.transferDao().loadAll().map(::createBackupTransfer),
            database.budgetDao().loadAll().map(::createBackupBudget),
        )
    }

    private fun createBackupAccount(account: AccountEntity): BackupAccount =
        BackupAccount(
            account.id,
            account.name,
            account.type,
            account.createdAt,
            account.updatedAt,
            account.openingBalanceMinor,
            account.openingBalanceDate,
            account.archived,
        )

    private fun createBackupCategory(category: CategoryEntity): BackupCategory =
        BackupCategory(category.id, category.name, category.direction, category.archived, category.createdAt, category.updatedAt)

    private fun createBackupTag(tag: TagEntity): BackupTag =
        BackupTag(tag.id, tag.name, tag.categoryId, tag.archived, tag.createdAt, tag.updatedAt)

    private fun createBackupTransaction(
        transaction: TransactionEntity,
        transactionTagAssociationsById: Map<String, List<TransactionTagCrossRef>>,
    ): BackupTransaction =
        BackupTransaction(
            transaction.id,
            transaction.accountId,
            transaction.amountMinor,
            transaction.direction,
            transaction.date,
            transaction.categoryId,
            transactionTagAssociationsById[transaction.id].orEmpty().map(TransactionTagCrossRef::tagId).sorted(),
            transaction.note,
            transaction.createdAt,
            transaction.updatedAt,
        )

    private fun createBackupTransfer(transfer: TransferEntity): BackupTransfer =
        BackupTransfer(
            transfer.id,
            transfer.sourceAccountId,
            transfer.destinationAccountId,
            transfer.amountMinor,
            transfer.date,
            transfer.createdAt,
            transfer.updatedAt,
        )

    private fun createBackupBudget(budget: BudgetEntity): BackupBudget =
        BackupBudget(budget.id, budget.categoryId, budget.month, budget.amountMinor, budget.createdAt, budget.updatedAt)

    private suspend fun clearLedger(): Unit {
        database.categoryTagDao().clearAllTransactionTags()
        database.budgetDao().clear()
        database.transferDao().clear()
        database.transactionDao().clear()
        database.categoryTagDao().clearTags()
        database.categoryTagDao().clearCategories()
        database.accountDao().clear()
    }

    private suspend fun saveAccounts(accounts: List<BackupAccount>): Unit {
        accounts.forEach { account ->
            database.accountDao().saveAccount(
                AccountEntity(
                    account.id,
                    account.name,
                    account.type,
                    account.createdAt,
                    account.updatedAt,
                    account.openingBalanceMinor,
                    account.openingBalanceDate,
                    account.archived,
                ),
            )
        }
    }

    private suspend fun saveCategories(categories: List<BackupCategory>): Unit {
        categories.forEach { category ->
            database.categoryTagDao().saveCategory(
                CategoryEntity(category.id, category.name, category.direction, category.archived, category.createdAt, category.updatedAt),
            )
        }
    }

    private suspend fun saveTags(tags: List<BackupTag>): Unit {
        tags.forEach { tag ->
            database.categoryTagDao().saveTag(TagEntity(tag.id, tag.name, tag.categoryId, tag.archived, tag.createdAt, tag.updatedAt))
        }
    }

    private suspend fun saveTransactions(transactions: List<BackupTransaction>): Unit {
        transactions.forEach { transaction ->
            database.transactionDao().save(
                TransactionEntity(
                    transaction.id,
                    transaction.accountId,
                    transaction.amountMinor,
                    transaction.direction,
                    transaction.date,
                    transaction.categoryId,
                    transaction.note,
                    transaction.createdAt,
                    transaction.updatedAt,
                ),
            )
            database.categoryTagDao().clearTransactionTags(transaction.id)
            transaction.tagIds.forEach { tagId ->
                database.categoryTagDao().addTransactionTag(TransactionTagCrossRef(transaction.id, tagId))
            }
        }
    }

    private suspend fun saveTransfers(transfers: List<BackupTransfer>): Unit {
        transfers.forEach { transfer ->
            database.transferDao().save(
                TransferEntity(
                    transfer.id,
                    transfer.sourceAccountId,
                    transfer.destinationAccountId,
                    transfer.amountMinor,
                    transfer.date,
                    transfer.createdAt,
                    transfer.updatedAt,
                ),
            )
        }
    }

    private suspend fun saveBudgets(budgets: List<BackupBudget>): Unit {
        budgets.forEach { budget ->
            database.budgetDao().save(
                BudgetEntity(budget.id, budget.categoryId, budget.month, budget.amountMinor, budget.createdAt, budget.updatedAt),
            )
        }
    }
}
