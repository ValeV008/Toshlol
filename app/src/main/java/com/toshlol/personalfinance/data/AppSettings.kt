package com.toshlol.personalfinance.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = SINGLE_SETTINGS_ID,
    val currency: String,
) {
    companion object {
        const val SINGLE_SETTINGS_ID: Int = 1
    }
}

@Dao
interface AppSettingsDao {
    @Query("SELECT currency FROM app_settings WHERE id = 1")
    suspend fun loadCurrency(): String?

    @Insert
    suspend fun insertSettings(settings: AppSettings): Long
}
