package com.toshlol.personalfinance.data

import java.time.YearMonth

data class MonthlyOverview(
    val month: YearMonth,
    val closingBalancesMinor: Map<String, Long>,
    val selectedAccountIds: Set<String>,
    val incomeMinor: Long,
    val expenseMinor: Long,
    val transactions: List<FinanceTransaction>,
    val transfers: List<FinanceTransfer>,
) {
    val closingBalanceMinor: Long
        get() = closingBalancesMinor.filterKeys { it in selectedAccountIds }.values.sum()
}

fun projectMonthlyOverview(
    month: YearMonth,
    accounts: List<Account>,
    transactions: List<FinanceTransaction>,
    transfers: List<FinanceTransfer>,
    selectedAccountIds: Set<String>,
): MonthlyOverview {
    val monthStart: Long = month.atDay(1).toEpochDay()
    val monthEnd: Long = month.atEndOfMonth().toEpochDay()
    val selectedIds: Set<String> = selectedAccountIds.intersect(accounts.map(Account::id).toSet())
    val balances: Map<String, Long> =
        accounts.associate { account ->
            val openingBalance: Long = account.openingBalanceMinor.takeIf { account.openingBalanceDate <= monthEnd } ?: 0L
            val transactionBalance: Long =
                transactions
                    .asSequence()
                    .filter { it.accountId == account.id && it.date in account.openingBalanceDate..monthEnd }
                    .sumOf { if (it.direction == TransactionDirection.INCOME) it.amountMinor else -it.amountMinor }
            val transferBalance: Long =
                transfers
                    .asSequence()
                    .filter { it.date in account.openingBalanceDate..monthEnd }
                    .sumOf { transfer ->
                        when (account.id) {
                            transfer.sourceAccountId -> -transfer.amountMinor
                            transfer.destinationAccountId -> transfer.amountMinor
                            else -> 0L
                        }
                    }
            account.id to (openingBalance + transactionBalance + transferBalance)
        }
    val monthlyTransactions: List<FinanceTransaction> =
        transactions.filter {
            it.accountId in selectedIds && it.date in monthStart..monthEnd
        }
    return MonthlyOverview(
        month = month,
        closingBalancesMinor = balances,
        selectedAccountIds = selectedAccountIds,
        incomeMinor = monthlyTransactions.filter { it.direction == TransactionDirection.INCOME }.sumOf(FinanceTransaction::amountMinor),
        expenseMinor = monthlyTransactions.filter { it.direction == TransactionDirection.EXPENSE }.sumOf(FinanceTransaction::amountMinor),
        transactions = monthlyTransactions,
        transfers =
            transfers.filter {
                it.date in monthStart..monthEnd &&
                    (it.sourceAccountId in selectedIds || it.destinationAccountId in selectedIds)
            },
    )
}
