package com.toshlol.personalfinance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun OverviewScreen(currency: String) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Overview", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Text("$currency ledger is ready.", style = MaterialTheme.typography.bodyLarge)
        Text("No transactions yet", style = MaterialTheme.typography.titleLarge)
        Text("Your account is ready. Start with your next income or expense.")
        Text("Use categories and tags to organize transactions when you need them.")
        Text("Monthly totals and budget progress will appear as you record your finances.")
    }
}
