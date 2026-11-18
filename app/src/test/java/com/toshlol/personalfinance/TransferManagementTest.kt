package com.toshlol.personalfinance

import com.toshlol.personalfinance.data.FinanceTransfer
import com.toshlol.personalfinance.data.TransferDraft
import com.toshlol.personalfinance.data.TransferRepository
import com.toshlol.personalfinance.domain.TransferManagement
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TransferManagementTest {
    @Test
    fun `save transfer delegates valid draft`(): Unit =
        runTest {
            val repository: InMemoryTransferRepository = InMemoryTransferRepository()
            val transferManagement: TransferManagement = TransferManagement(repository)
            val draft: TransferDraft =
                TransferDraft(sourceAccountId = "cash", destinationAccountId = "savings", amountMinor = 1200L, date = 10L)
            val actualTransfers: List<FinanceTransfer> = transferManagement.saveTransfer(draft)
            assertEquals(draft, repository.savedDraft)
            assertEquals(1, actualTransfers.size)
        }

    @Test
    fun `save transfer rejects invalid drafts before repository write`(): Unit =
        runTest {
            val repository: InMemoryTransferRepository = InMemoryTransferRepository()
            val transferManagement: TransferManagement = TransferManagement(repository)
            val nonPositiveDraft: TransferDraft =
                TransferDraft(sourceAccountId = "cash", destinationAccountId = "savings", amountMinor = 0L, date = 10L)
            val sameAccountDraft: TransferDraft =
                TransferDraft(sourceAccountId = "cash", destinationAccountId = "cash", amountMinor = 1200L, date = 10L)
            val invalidDateDraft: TransferDraft =
                TransferDraft(sourceAccountId = "cash", destinationAccountId = "savings", amountMinor = 1200L, date = Long.MAX_VALUE)
            assertThrows(
                IllegalArgumentException::class.java,
            ) { kotlinx.coroutines.runBlocking { transferManagement.saveTransfer(nonPositiveDraft) } }
            assertThrows(
                IllegalArgumentException::class.java,
            ) { kotlinx.coroutines.runBlocking { transferManagement.saveTransfer(sameAccountDraft) } }
            assertThrows(
                IllegalArgumentException::class.java,
            ) { kotlinx.coroutines.runBlocking { transferManagement.saveTransfer(invalidDateDraft) } }
            assertEquals(null, repository.savedDraft)
        }
}

private class InMemoryTransferRepository : TransferRepository {
    var savedDraft: TransferDraft? = null
        private set

    override suspend fun loadTransfers(): List<FinanceTransfer> = emptyList()

    override suspend fun save(draft: TransferDraft): List<FinanceTransfer> {
        savedDraft = draft
        return listOf(
            FinanceTransfer(
                "transfer",
                draft.sourceAccountId,
                "Cash",
                draft.destinationAccountId,
                "Savings",
                draft.amountMinor,
                draft.date,
                1L,
                1L,
            ),
        )
    }

    override suspend fun delete(id: String): List<FinanceTransfer> = emptyList()
}
