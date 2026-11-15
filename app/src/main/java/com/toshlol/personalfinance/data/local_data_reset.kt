@file:Suppress("ktlint:standard:filename", "ktlint:standard:no-unit-return")

package com.toshlol.personalfinance.data

import androidx.room.withTransaction

class RoomLocalDataResetter(
    private val database: AppDatabase,
) {
    suspend fun reset(): Unit =
        database.withTransaction {
            database.categoryTagDao().clearAllTransactionTags()
            database.budgetDao().clear()
            database.transferDao().clear()
            database.transactionDao().clear()
            database.categoryTagDao().clearTags()
            database.categoryTagDao().clearCategories()
            database.accountDao().clear()
            database.settingsDao().clear()
        }
}
