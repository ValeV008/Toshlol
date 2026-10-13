package com.toshlol.personalfinance

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toshlol.personalfinance.data.AccountRepository
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.AppPreferences
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    @Test
    fun `database persists first account and currency`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = AccountRepository(database, AppPreferences(context))

        val accountId = repository.createAccount("Main checking", AccountType.BANK)
        repository.saveCurrency("EUR")

        assertTrue(accountId > 0)
        assertEquals(1, database.accountDao().countAccounts())
        assertEquals("EUR", repository.loadCurrency())

        database.close()
    }
}
