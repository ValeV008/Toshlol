package com.toshlol.personalfinance.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "personal_finance_prefs")

/** Reads and removes setup settings written by the version 1 prototype. New finance settings belong to Room. */
class AppPreferences(
    private val context: Context,
) {
    private val currencyKey: Preferences.Key<String> = stringPreferencesKey("selected_currency")
    private val setupCompleteKey: Preferences.Key<Boolean> = booleanPreferencesKey("setup_complete")

    suspend fun loadLegacyCurrency(): String? = context.dataStore.data.first()[currencyKey]

    suspend fun clearLegacySetup() {
        context.dataStore.edit { preferences ->
            preferences.remove(currencyKey)
            preferences.remove(setupCompleteKey)
        }
    }
}
