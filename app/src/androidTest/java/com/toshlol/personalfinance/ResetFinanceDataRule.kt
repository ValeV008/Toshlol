package com.toshlol.personalfinance

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.AppPreferences
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource

/** Clears the ledger before each test and after it unless the host verifies the seeded state after a process restart. */
class ResetFinanceDataRule(
    private val clearAfter: Boolean = true,
) : ExternalResource() {
    override fun before(): Unit = clearFinanceData()

    @Suppress("ktlint:standard:no-unit-return")
    override fun after(): Unit {
        if (clearAfter) clearFinanceData()
    }

    @Suppress("ktlint:standard:no-unit-return")
    private fun clearFinanceData(): Unit {
        val context: Context = ApplicationProvider.getApplicationContext()
        AppDatabase.getInstance(context).clearAllTables()
        runBlocking {
            AppPreferences(context).clearLegacySetup()
            AppPreferences(context).clearOverviewAccountIds()
            AppPreferences(context).clearLastTransactionAccountId()
        }
    }
}
