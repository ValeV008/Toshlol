package com.toshlol.personalfinance

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.CategoryDraft
import com.toshlol.personalfinance.data.RecurringBudgetDraft
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
            val expected = createClassificationsAndReferences(databaseName)
            try {
                val reopenedDatabase: AppDatabase = openDatabase(databaseName)
                try {
                    val classifications: RoomCategoryTagRepository = RoomCategoryTagRepository(reopenedDatabase)
                    val snapshot = classifications.load()
                    val transaction: com.toshlol.personalfinance.data.FinanceTransaction =
                        RoomTransactionRepository(reopenedDatabase).loadTransactions().single()
                    val budget: com.toshlol.personalfinance.data.RecurringBudgetProgress =
                        RoomRecurringBudgetRepository(reopenedDatabase).load(expected.month).single()
                    val archivedCategory = snapshot.categories.single()
                    val archivedTag = snapshot.tags.single()
                    assertEquals(expected.categoryId, archivedCategory.id)
                    assertEquals("Groceries", archivedCategory.name)
                    assertTrue(archivedCategory.archived)
                    assertEquals(expected.tagId, archivedTag.id)
                    assertEquals("Dinner", archivedTag.name)
                    assertTrue(archivedTag.archived)
                    assertEquals(expected.transactionId, transaction.id)
                    assertEquals(1_250L, transaction.amountMinor)
                    assertEquals(expected.categoryId, transaction.categoryId)
                    assertEquals(listOf(expected.tagId), transaction.tagIds)
                    assertEquals(setOf(expected.categoryId), budget.restrictions.categoryIds)
                    assertEquals(setOf(expected.tagId), budget.restrictions.tagIds)
                    assertEquals(1_250L, budget.totals.spentMinor)
                } finally {
                    reopenedDatabase.close()
                }
            } finally {
                context.deleteDatabase(databaseName)
            }
        }

    private suspend fun createClassificationsAndReferences(databaseName: String): PersistentClassificationState {
        val database: AppDatabase = openDatabase(databaseName)
        try {
            val fixtures: FinanceTestFixtures = FinanceTestFixtures(database)
            val account = fixtures.createFirstAccount("Cash")
            val organization: RoomCategoryTagRepository = fixtures.organization
            val category =
                organization
                    .saveCategory(
                        CategoryDraft(name = "Food", direction = TransactionDirection.EXPENSE),
                    ).categories
                    .single()
            val tag = organization.saveTag(TagDraft(name = "Lunch", categoryId = category.id)).tags.single()
            val transaction =
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
            val month: YearMonth = YearMonth.from(LocalDate.now())
            RoomRecurringBudgetRepository(database).save(
                RecurringBudgetDraft(null, "Food limit", 5_000L, setOf(account.id), setOf(category.id), setOf(tag.id)),
                month,
            )
            organization.saveCategory(CategoryDraft(category.id, "Groceries", TransactionDirection.EXPENSE))
            organization.saveTag(TagDraft(tag.id, "Dinner", category.id))
            organization.archiveCategory(category.id)
            organization.archiveTag(tag.id)
            return PersistentClassificationState(category.id, tag.id, transaction.id, month)
        } finally {
            database.close()
        }
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
