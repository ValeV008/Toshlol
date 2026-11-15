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
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

/** Seeds states for host-side ADB force-stop and relaunch verification. */
@RunWith(AndroidJUnit4::class)
class OverviewPersistenceHandoffTest {
    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(ResetFinanceDataRule(clearAfter = false))
            .around(PersistenceSeedRule())

    @Test
    fun prepareSelectedOverviewForHostProcessRestart(): Unit {
        val context: Context = ApplicationProvider.getApplicationContext()
        val accountIds: Set<String> = runBlocking { AppPreferences(context).loadOverviewAccountIds() }
        val expectedAccountId: String =
            runBlocking {
                requireNotNull(
                    AppDatabase
                        .getInstance(context)
                        .accountDao()
                        .loadAccounts()
                        .firstOrNull(),
                ).id
            }
        assertEquals(setOf(expectedAccountId), accountIds)
    }

    @Test
    fun prepareEmptyOverviewForHostProcessRestart(): Unit {
        val context: Context = ApplicationProvider.getApplicationContext()
        runBlocking {
            AppPreferences(context).saveOverviewAccountIds(emptySet())
            assertEquals(emptySet<String>(), AppPreferences(context).loadOverviewAccountIds())
        }
    }

    private class PersistenceSeedRule : ExternalResource() {
        override fun before(): Unit {
            val context: Context = ApplicationProvider.getApplicationContext()
            runBlocking {
                val fixtures: FinanceTestFixtures = FinanceTestFixtures(AppDatabase.getInstance(context))
                val account: Account = fixtures.createFirstAccount()
                val month: YearMonth = YearMonth.now()
                fixtures.accounts.saveAccount(account.id, account.name, 100_000L, month.atDay(1).toEpochDay())
                fixtures.transactions.save(
                    TransactionDraft(
                        accountId = account.id,
                        amountMinor = 200L,
                        direction = TransactionDirection.INCOME,
                        date = LocalDate.now().toEpochDay(),
                        note = null,
                    ),
                )
                AppPreferences(context).saveOverviewAccountIds(setOf(account.id))
            }
        }
    }
}
