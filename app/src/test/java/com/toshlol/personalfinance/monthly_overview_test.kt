@file:Suppress("ktlint:standard:filename", "ktlint:standard:no-unit-return")

package com.toshlol.personalfinance

import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.data.FinanceTransaction
import com.toshlol.personalfinance.data.FinanceTransfer
import com.toshlol.personalfinance.data.MonthlyOverview
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.data.projectMonthlyOverview
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthlyOverviewTest {
    @Test
    fun calculateClosingBalancesAtSelectedMonthEnd(): Unit {
        val january: YearMonth = YearMonth.of(2026, 1)
        val accountAId: String = "account-a"
        val accountBId: String = "account-b"
        val accountA: Account = createAccount(accountAId, 100_000L, january.atDay(1))
        val accountB: Account = createAccount(accountBId, 0L, january.atDay(1))
        val transactions: List<FinanceTransaction> =
            listOf(
                createTransaction("income-january", accountAId, 20_000L, TransactionDirection.INCOME, january.atDay(5)),
                createTransaction("expense-january", accountAId, 5_000L, TransactionDirection.EXPENSE, january.atDay(10)),
                createTransaction("expense-february", accountAId, 2_500L, TransactionDirection.EXPENSE, january.plusMonths(1).atDay(5)),
                createTransaction("income-march", accountAId, 10_000L, TransactionDirection.INCOME, january.plusMonths(2).atDay(5)),
            )
        val transfers: List<FinanceTransfer> =
            listOf(createTransfer("transfer-january", accountAId, accountBId, 10_000L, january.atDay(15)))
        val januaryOverview: MonthlyOverview =
            projectMonthlyOverview(january, listOf(accountA, accountB), transactions, transfers, setOf(accountAId, accountBId))
        val februaryOverview: MonthlyOverview =
            projectMonthlyOverview(
                january.plusMonths(1),
                listOf(accountA, accountB),
                transactions,
                transfers,
                setOf(accountAId, accountBId),
            )
        assertEquals(115_000L, januaryOverview.closingBalanceMinor)
        assertEquals(105_000L, januaryOverview.closingBalancesMinor[accountAId])
        assertEquals(10_000L, januaryOverview.closingBalancesMinor[accountBId])
        assertEquals(20_000L, januaryOverview.incomeMinor)
        assertEquals(5_000L, januaryOverview.expenseMinor)
        assertEquals(listOf("transfer-january"), januaryOverview.transfers.map(FinanceTransfer::id))
        assertEquals(112_500L, februaryOverview.closingBalanceMinor)
        assertEquals(-2_500L, februaryOverview.incomeMinor - februaryOverview.expenseMinor)
    }

    @Test
    fun respectOpeningDateAndCalendarBoundaries(): Unit {
        val month: YearMonth = YearMonth.of(2026, 1)
        val accountId: String = "account-a"
        val account: Account = createAccount(accountId, 50_000L, month.atDay(15))
        val futureAccount: Account = createAccount("future", 70_000L, month.plusMonths(1).atDay(1))
        val transactions: List<FinanceTransaction> =
            listOf(
                createTransaction("before-opening", accountId, 20_000L, TransactionDirection.INCOME, month.atDay(14)),
                createTransaction("on-opening", accountId, 10_000L, TransactionDirection.INCOME, month.atDay(15)),
                createTransaction("last-day", accountId, 5_000L, TransactionDirection.EXPENSE, month.atEndOfMonth()),
                createTransaction("future-opening", "future", 3_000L, TransactionDirection.INCOME, month.atDay(31)),
            )
        val actual: MonthlyOverview =
            projectMonthlyOverview(month, listOf(account, futureAccount), transactions, emptyList(), setOf(accountId, "future"))
        assertEquals(55_000L, actual.closingBalancesMinor[accountId])
        assertEquals(0L, actual.closingBalancesMinor["future"])
        assertEquals(33_000L, actual.incomeMinor)
        assertEquals(5_000L, actual.expenseMinor)
    }

    @Test
    fun filterOverviewActivityByMonthAndSelectedAccounts(): Unit {
        val month: YearMonth = YearMonth.of(2026, 4)
        val accountAId: String = "account-a"
        val accountBId: String = "account-b"
        val accountA: Account = createAccount(accountAId, 0L, month.atDay(1))
        val accountB: Account = createAccount(accountBId, 0L, month.atDay(1))
        val transactions: List<FinanceTransaction> =
            listOf(
                createTransaction("selected", accountAId, 1_000L, TransactionDirection.INCOME, month.atDay(2)),
                createTransaction("other-account", accountBId, 2_000L, TransactionDirection.INCOME, month.atDay(3)),
                createTransaction("other-month", accountAId, 3_000L, TransactionDirection.INCOME, month.minusMonths(1).atEndOfMonth()),
            )
        val transfers: List<FinanceTransfer> = listOf(createTransfer("once", accountAId, accountBId, 4_000L, month.atDay(4)))
        val actual: MonthlyOverview =
            projectMonthlyOverview(month, listOf(accountA, accountB), transactions, transfers, setOf(accountAId, "unknown"))
        assertEquals(listOf("selected"), actual.transactions.map(FinanceTransaction::id))
        assertEquals(listOf("once"), actual.transfers.map(FinanceTransfer::id))
        assertEquals(1_000L, actual.incomeMinor)
        assertEquals(-3_000L, actual.closingBalanceMinor)
        val emptySelection: MonthlyOverview = projectMonthlyOverview(month, listOf(accountA, accountB), transactions, transfers, emptySet())
        assertEquals(0L, emptySelection.closingBalanceMinor)
        assertEquals(0L, emptySelection.incomeMinor)
        assertEquals(0, emptySelection.transactions.size)
        assertEquals(0, emptySelection.transfers.size)
    }

    private fun createAccount(
        accountId: String,
        openingBalanceMinor: Long,
        openingDate: LocalDate,
    ): Account = Account(accountId, accountId, AccountType.BANK, 0L, 0L, openingBalanceMinor, openingDate.toEpochDay())

    private fun createTransaction(
        id: String,
        accountId: String,
        amountMinor: Long,
        direction: TransactionDirection,
        date: LocalDate,
    ): FinanceTransaction =
        FinanceTransaction(
            id,
            accountId,
            accountId,
            amountMinor,
            direction,
            date.toEpochDay(),
            null,
            null,
            emptyList(),
            emptyList(),
            null,
            0L,
            0L,
        )

    private fun createTransfer(
        id: String,
        sourceId: String,
        destinationId: String,
        amountMinor: Long,
        date: LocalDate,
    ): FinanceTransfer = FinanceTransfer(id, sourceId, sourceId, destinationId, destinationId, amountMinor, date.toEpochDay(), 0L, 0L)
}
