package com.toshlol.personalfinance.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "personal_finance_prefs")

class AppPreferences(private val context: Context) {
    private val currencyKey = stringPreferencesKey("selected_currency")
    private val setupCompleteKey = booleanPreferencesKey("setup_complete")

    val currency: Flow<String?>
        get() = context.dataStore.data.map { preferences -> preferences[currencyKey] }

    suspend fun saveCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[currencyKey] = currency.trim()
        }
    }

    suspend fun getCurrency(): String? = context.dataStore.data.map { it[currencyKey] }.firstOrNull()

    suspend fun markSetupComplete() {
        context.dataStore.edit { preferences ->
            preferences[setupCompleteKey] = true
        }
    }

    fun hasCompletedSetup(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[setupCompleteKey] ?: false
    }
}
