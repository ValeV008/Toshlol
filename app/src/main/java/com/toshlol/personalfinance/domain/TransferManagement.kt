package com.toshlol.personalfinance.domain

import com.toshlol.personalfinance.data.FinanceTransfer
import com.toshlol.personalfinance.data.TransferDraft
import com.toshlol.personalfinance.data.TransferRepository
import java.time.LocalDate

class TransferManagement(
    private val repository: TransferRepository,
) {
    suspend fun loadTransfers(): List<FinanceTransfer> = repository.loadTransfers()

    suspend fun saveTransfer(draft: TransferDraft): List<FinanceTransfer> {
        require(draft.amountMinor > 0) { "Enter a positive amount." }
        require(draft.date in LocalDate.MIN.toEpochDay()..LocalDate.MAX.toEpochDay()) { "Enter a valid date." }
        require(draft.sourceAccountId != draft.destinationAccountId) { "Choose two different Accounts." }
        return repository.save(draft)
    }

    suspend fun deleteTransfer(id: String): List<FinanceTransfer> = repository.delete(id)
}
