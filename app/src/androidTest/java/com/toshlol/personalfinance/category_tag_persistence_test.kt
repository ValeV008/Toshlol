@file:Suppress("ktlint:standard:filename", "ktlint:standard:no-unit-return")

package com.toshlol.personalfinance

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.CategoryDraft
import com.toshlol.personalfinance.data.CategoryTagSnapshot
import com.toshlol.personalfinance.data.FinanceCategory
import com.toshlol.personalfinance.data.FinanceTag
import com.toshlol.personalfinance.data.FinanceTransaction
import com.toshlol.personalfinance.data.RecurringBudgetDraft
import com.toshlol.personalfinance.data.RecurringBudgetProgress
import com.toshlol.personalfinance.data.RoomCategoryTagRepository
import com.toshlol.personalfinance.data.RoomRecurringBudgetRepository
import com.toshlol.personalfinance.data.RoomTransactionRepository
import com.toshlol.personalfinance.data.TagDraft
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.data.TransactionDraft
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class CategoryTagPersistenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun classificationNamesArchivesAndBudgetReferencesPersistAfterDatabaseReopen(): Unit =
        runBlocking {
            val databaseName: String = "classification-persistence-${System.nanoTime()}.db"
            context.deleteDatabase(databaseName)
            val expected: PersistentClassificationState = createClassificationsAndReferences(databaseName)
            try {
                assertPersistedClassificationState(databaseName, expected)
            } finally {
                context.deleteDatabase(databaseName)
            }
        }

    private fun assertPersistedClassificationState(
        databaseName: String,
        expected: PersistentClassificationState,
    ): Unit {
        runBlocking {
            val reopenedDatabase: AppDatabase = openDatabase(databaseName)
            try {
                val snapshot: CategoryTagSnapshot = RoomCategoryTagRepository(reopenedDatabase).load()
                assertPersistedClassificationLabels(snapshot, expected)
                assertPersistedTransactionReferences(reopenedDatabase, expected)
                assertPersistedBudgetReferences(reopenedDatabase, expected)
            } finally {
                reopenedDatabase.close()
            }
        }
    }

    private fun assertPersistedClassificationLabels(
        snapshot: CategoryTagSnapshot,
        expected: PersistentClassificationState,
    ): Unit {
        val archivedCategory: FinanceCategory = snapshot.categories.single()
        val archivedTag: FinanceTag = snapshot.tags.single()
        assertEquals(expected.categoryId, archivedCategory.id)
        assertEquals("Groceries", archivedCategory.name)
        assertTrue(archivedCategory.archived)
        assertEquals(expected.tagId, archivedTag.id)
        assertEquals("Dinner", archivedTag.name)
        assertTrue(archivedTag.archived)
    }

    private suspend fun assertPersistedTransactionReferences(
        database: AppDatabase,
        expected: PersistentClassificationState,
    ): Unit {
        val transaction: FinanceTransaction = RoomTransactionRepository(database).loadTransactions().single()
        assertEquals(expected.transactionId, transaction.id)
        assertEquals(1_250L, transaction.amountMinor)
        assertEquals(expected.categoryId, transaction.categoryId)
        assertEquals(listOf(expected.tagId), transaction.tagIds)
    }

    private suspend fun assertPersistedBudgetReferences(
        database: AppDatabase,
        expected: PersistentClassificationState,
    ): Unit {
        val budget: RecurringBudgetProgress = RoomRecurringBudgetRepository(database).load(expected.month).single()
        assertEquals(setOf(expected.categoryId), budget.restrictions.categoryIds)
        assertEquals(setOf(expected.tagId), budget.restrictions.tagIds)
        assertEquals(1_250L, budget.totals.spentMinor)
    }

    private suspend fun createClassificationsAndReferences(databaseName: String): PersistentClassificationState {
        val database: AppDatabase = openDatabase(databaseName)
        try {
            val fixtures: FinanceTestFixtures = FinanceTestFixtures(database)
            val account: Account = fixtures.createFirstAccount("Cash")
            val organization: RoomCategoryTagRepository = fixtures.organization
            val expected: PersistentClassificationState = createClassificationRecords(fixtures, account, organization)
            saveClassificationBudget(database, account, expected)
            renameAndArchiveClassifications(organization, expected)
            return expected
        } finally {
            database.close()
        }
    }

    private suspend fun createClassificationRecords(
        fixtures: FinanceTestFixtures,
        account: Account,
        organization: RoomCategoryTagRepository,
    ): PersistentClassificationState {
        val category: FinanceCategory =
            organization.saveCategory(CategoryDraft(name = "Food", direction = TransactionDirection.EXPENSE)).categories.single()
        val tag: FinanceTag = organization.saveTag(TagDraft(name = "Lunch", categoryId = category.id)).tags.single()
        val transaction: FinanceTransaction =
            fixtures.transactions
                .save(
                    TransactionDraft(
                        accountId = account.id,
                        amountMinor = 1_250L,
                        direction = TransactionDirection.EXPENSE,
                        date = LocalDate.now().toEpochDay(),
                        note = null,
                        categoryId = category.id,
                        tagIds = setOf(tag.id),
                    ),
                ).single()
        return PersistentClassificationState(category.id, tag.id, transaction.id, YearMonth.from(LocalDate.now()))
    }

    private suspend fun saveClassificationBudget(
        database: AppDatabase,
        account: Account,
        expected: PersistentClassificationState,
    ): Unit {
        RoomRecurringBudgetRepository(database).save(
            RecurringBudgetDraft(null, "Food limit", 5_000L, setOf(account.id), setOf(expected.categoryId), setOf(expected.tagId)),
            expected.month,
        )
    }

    private suspend fun renameAndArchiveClassifications(
        organization: RoomCategoryTagRepository,
        expected: PersistentClassificationState,
    ): Unit {
        organization.saveCategory(CategoryDraft(expected.categoryId, "Groceries", TransactionDirection.EXPENSE))
        organization.saveTag(TagDraft(expected.tagId, "Dinner", expected.categoryId))
        organization.archiveCategory(expected.categoryId)
        organization.archiveTag(expected.tagId)
    }

    private fun openDatabase(databaseName: String): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()

    private data class PersistentClassificationState(
        val categoryId: String,
        val tagId: String,
        val transactionId: String,
        val month: YearMonth,
    )
}
