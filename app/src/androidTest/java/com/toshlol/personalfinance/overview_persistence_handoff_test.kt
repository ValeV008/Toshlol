@file:Suppress("ktlint:standard:filename", "ktlint:standard:no-unit-return")

package com.toshlol.personalfinance

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.AppPreferences
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.data.TransactionDraft
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

/** Seeds states for scripts/verify_overview_persistence.py; the host owns restart and UI assertions. */
@RunWith(AndroidJUnit4::class)
class OverviewPersistenceHandoffTest {
    @get:Rule
    val resetRule: ResetFinanceDataRule = ResetFinanceDataRule(clearAfter = false)

    @Test
    fun prepareSelectedOverviewForHostProcessRestart(): Unit {
        seedOverviewForHostProcessRestart(isSelected = true)
    }

    @Test
    fun prepareEmptyOverviewForHostProcessRestart(): Unit {
        seedOverviewForHostProcessRestart(isSelected = false)
    }

    private fun seedOverviewForHostProcessRestart(isSelected: Boolean): Unit {
        val context: Context = ApplicationProvider.getApplicationContext()
        runBlocking {
            val fixtures: FinanceTestFixtures = FinanceTestFixtures(AppDatabase.getInstance(context))
            val today: LocalDate = LocalDate.now()
            val month: YearMonth = YearMonth.from(today)
            val cash: Account = fixtures.createFirstAccount()
            val savings: Account = fixtures.createAdditionalAccount("Savings")
            fixtures.accounts.saveAccount(cash.id, cash.name, 100_000L, month.atDay(1).toEpochDay())
            fixtures.accounts.saveAccount(savings.id, savings.name, 500_000L, month.atDay(1).toEpochDay())
            fixtures.transactions.save(
                TransactionDraft(
                    accountId = cash.id,
                    amountMinor = 200L,
                    direction = TransactionDirection.INCOME,
                    date = today.toEpochDay(),
                    note = null,
                ),
            )
            fixtures.transactions.save(
                TransactionDraft(
                    accountId = savings.id,
                    amountMinor = 700L,
                    direction = TransactionDirection.INCOME,
                    date = today.toEpochDay(),
                    note = null,
                ),
            )
            val expectedAccountIds: Set<String> = if (isSelected) setOf(cash.id) else emptySet()
            AppPreferences(context).saveOverviewAccountIds(expectedAccountIds)
            assertEquals(expectedAccountIds, AppPreferences(context).loadOverviewAccountIds())
        }
    }
}
