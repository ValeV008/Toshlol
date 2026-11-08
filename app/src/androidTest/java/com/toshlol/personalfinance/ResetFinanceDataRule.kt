package com.toshlol.personalfinance

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.AppPreferences
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource

/** Runs outside the activity rule, so every UI test starts and ends with an empty ledger. */
class ResetFinanceDataRule : ExternalResource() {
    override fun before(): Unit = clearFinanceData()

    override fun after(): Unit = clearFinanceData()

    private fun clearFinanceData() {
        val context: Context = ApplicationProvider.getApplicationContext()
        AppDatabase.getInstance(context).clearAllTables()
        runBlocking { AppPreferences(context).clearLegacySetup() }
    }
}
