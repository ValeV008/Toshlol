package com.toshlol.personalfinance.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "personal_finance_prefs")

/** Reads and removes setup settings written by the version 1 prototype. New finance settings belong to Room. */
class AppPreferences(
    private val context: Context,
) {
    private val currencyKey: Preferences.Key<String> = stringPreferencesKey("selected_currency")
    private val setupCompleteKey: Preferences.Key<Boolean> = booleanPreferencesKey("setup_complete")
    private val selectedOverviewAccountsKey: Preferences.Key<Set<String>> = stringSetPreferencesKey("overview_account_ids")
    private val lastTransactionAccountKey: Preferences.Key<String> = stringPreferencesKey("last_transaction_account_id")

    suspend fun loadLegacyCurrency(): String? = context.dataStore.data.first()[currencyKey]

    suspend fun clearLegacySetup() {
        context.dataStore.edit { preferences ->
            preferences.remove(currencyKey)
            preferences.remove(setupCompleteKey)
        }
    }

    suspend fun loadOverviewAccountIds(): Set<String> =
        context.dataStore.data
            .first()[selectedOverviewAccountsKey]
            .orEmpty()

    suspend fun saveOverviewAccountIds(accountIds: Set<String>) {
        context.dataStore.edit { preferences -> preferences[selectedOverviewAccountsKey] = accountIds }
    }

    suspend fun clearOverviewAccountIds() {
        context.dataStore.edit { preferences -> preferences.remove(selectedOverviewAccountsKey) }
    }

    suspend fun loadLastTransactionAccountId(): String? = context.dataStore.data.first()[lastTransactionAccountKey]

    suspend fun saveLastTransactionAccountId(accountId: String) {
        context.dataStore.edit { preferences -> preferences[lastTransactionAccountKey] = accountId }
    }

    suspend fun clearLastTransactionAccountId() {
        context.dataStore.edit { preferences -> preferences.remove(lastTransactionAccountKey) }
    }
}
