@file:Suppress("ktlint:standard:filename")

package com.toshlol.personalfinance.data

internal class BackupReconciler {
    fun merge(
        local: FinanceBackup,
        imported: FinanceBackup,
    ): FinanceBackup =
        FinanceBackup(
            CURRENT_BACKUP_SCHEMA_VERSION,
            local.settings,
            reconcile(local.accounts, imported.accounts),
            reconcile(local.categories, imported.categories),
            reconcile(local.tags, imported.tags),
            reconcile(local.transactions, imported.transactions),
            reconcile(local.transfers, imported.transfers),
            reconcile(local.budgets, imported.budgets),
            reconcile(local.recurringBudgets, imported.recurringBudgets),
        )

    fun createReport(
        current: FinanceBackup?,
        imported: FinanceBackup,
        mode: BackupImportMode,
    ): BackupImportReport =
        if (mode == BackupImportMode.OVERWRITE) {
            BackupImportReport(
                mapOf(
                    BackupEntityType.ACCOUNT to BackupEntityCounts(inserted = imported.accounts.size),
                    BackupEntityType.CATEGORY to BackupEntityCounts(inserted = imported.categories.size),
                    BackupEntityType.TAG to BackupEntityCounts(inserted = imported.tags.size),
                    BackupEntityType.TRANSACTION to BackupEntityCounts(inserted = imported.transactions.size),
                    BackupEntityType.TRANSFER to BackupEntityCounts(inserted = imported.transfers.size),
                    BackupEntityType.BUDGET to BackupEntityCounts(inserted = imported.recurringBudgets.size),
                    BackupEntityType.LEGACY_BUDGET to BackupEntityCounts(inserted = imported.budgets.size),
                ),
            )
        } else {
            createMergeReport(current, imported)
        }

    private fun createMergeReport(
        current: FinanceBackup?,
        imported: FinanceBackup,
    ): BackupImportReport {
        val previous: FinanceBackup? = current
        return BackupImportReport(
            mapOf(
                BackupEntityType.ACCOUNT to countImported(previous?.accounts.orEmpty(), imported.accounts),
                BackupEntityType.CATEGORY to countImported(previous?.categories.orEmpty(), imported.categories),
                BackupEntityType.TAG to countImported(previous?.tags.orEmpty(), imported.tags),
                BackupEntityType.TRANSACTION to countImported(previous?.transactions.orEmpty(), imported.transactions),
                BackupEntityType.TRANSFER to countImported(previous?.transfers.orEmpty(), imported.transfers),
                BackupEntityType.BUDGET to countImported(previous?.recurringBudgets.orEmpty(), imported.recurringBudgets),
                BackupEntityType.LEGACY_BUDGET to countImported(previous?.budgets.orEmpty(), imported.budgets),
            ),
        )
    }

    private fun <T : BackupEntityRecord> reconcile(
        local: List<T>,
        imported: List<T>,
    ): List<T> {
        val records: MutableMap<String, T> = local.associateBy(BackupEntityRecord::id).toMutableMap()
        imported.forEach { incoming ->
            val existing: T? = records[incoming.id]
            if (existing == null || incoming.updatedAt > existing.updatedAt) {
                records[incoming.id] = incoming
            } else if (incoming.updatedAt == existing.updatedAt && incoming != existing) {
                throw BackupImportRejected("timestamp_conflict", "Records with equal update times have different content.")
            }
        }
        return records.values.toList()
    }

    private fun <T : BackupEntityRecord> countImported(
        local: List<T>,
        imported: List<T>,
    ): BackupEntityCounts {
        val localById: Map<String, T> = local.associateBy(BackupEntityRecord::id)
        val inserted: Int = imported.count { it.id !in localById }
        val updated: Int = imported.count { incoming -> localById[incoming.id]?.let { incoming.updatedAt > it.updatedAt } == true }
        val unchanged: Int = imported.size - inserted - updated
        return BackupEntityCounts(inserted, updated, unchanged)
    }
}
