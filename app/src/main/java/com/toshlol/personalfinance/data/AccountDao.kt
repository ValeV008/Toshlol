package com.toshlol.personalfinance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AccountDao {
    @Insert
    suspend fun insertAccount(account: AccountEntity): Long

    @Query("SELECT * FROM accounts ORDER BY createdAt, id")
    suspend fun loadAccounts(): List<AccountEntity>
}
