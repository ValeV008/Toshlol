package com.toshlol.personalfinance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.data.FinanceCategory
import com.toshlol.personalfinance.data.FinanceTag
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState

@Composable
@Suppress("ktlint:standard:no-unit-return")
fun CategoryTagScreen(
    state: TransactionsUiState,
    actions: OrganizerActions,
): Unit {
    val keyboardController = LocalSoftwareKeyboardController.current
    var isAccountSectionVisible: Boolean by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Categories and tags", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        OutlinedButton(onClick = actions.close) { Text("Back to overview") }
        LazyColumn(Modifier.testTag("category-tag-list"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("Account", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                OutlinedButton(onClick = { isAccountSectionVisible = !isAccountSectionVisible }) {
                    Text(if (isAccountSectionVisible) "Hide accounts" else "Manage accounts")
                }
                if (isAccountSectionVisible) {
                    Text("Account closing balances for ${state.selectedMonth}", style = MaterialTheme.typography.titleMedium)
                    state.accounts.forEach { account ->
                        Text(
                            "${account.name} · ${account.type.label} · ${state.currency} ${formatMoney(
                                state.monthlyOverview.closingBalancesMinor[account.id] ?: 0L,
                                state.currency,
                            )}${if (account.isArchived) " · Archived" else ""}",
                        )
                        if (!account.isArchived) {
                            TextButton(onClick = { actions.accounts.select(account.id) }) {
                                val accountAction: String = if (state.editingAccountId == account.id) "Editing" else "Edit"
                                Text("$accountAction ${account.name}")
                            }
                            TextButton(onClick = { actions.accounts.archive(account.id) }) { Text("Archive ${account.name}") }
                        }
                    }
                    OutlinedTextField(
                        state.accountNameDraft,
                        actions.accounts.nameChanged,
                        label = { Text("Account name") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        state.openingBalanceAmount,
                        actions.accounts.openingBalanceChanged,
                        label = { Text("Account opening balance") },
                        singleLine = true,
                    )
                    OutlinedTextField(state.openingBalanceDate, actions.accounts.openingDateChanged, label = {
                        Text("Account opening date (YYYY-MM-DD)")
                    }, singleLine = true)
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            actions.accounts.save()
                        },
                        enabled = !state.isSaving,
                    ) { Text("Save account details") }
                    Text("Add Account", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = state.newAccountName,
                        onValueChange = actions.accounts.newNameChanged,
                        label = { Text("New account name") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.newAccountOpeningBalance,
                        onValueChange = actions.accounts.newOpeningBalanceChanged,
                        label = { Text("New account opening balance") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.newAccountOpeningDate,
                        onValueChange = actions.accounts.newOpeningDateChanged,
                        label = { Text("New account opening date (YYYY-MM-DD)") },
                        singleLine = true,
                    )
                    Row {
                        AccountType.entries.forEach { type ->
                            Row(
                                Modifier
                                    .selectable(
                                        selected = state.newAccountType == type,
                                        role = androidx.compose.ui.semantics.Role.RadioButton,
                                        onClick = { actions.accounts.newTypeChanged(type) },
                                    ),
                            ) {
                                RadioButton(selected = state.newAccountType == type, onClick = null)
                                Text(type.name.lowercase().replaceFirstChar(Char::uppercase))
                            }
                        }
                    }
                    Button(onClick = actions.accounts.create, enabled = !state.isSaving) { Text("Create account") }
                }
            }
            item {
                Text("Categories", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                OutlinedTextField(state.categoryName, actions.categories.nameChanged, label = { Text("Category name") }, singleLine = true)
                Row {
                    TransactionDirection.entries.forEach { direction: TransactionDirection ->
                        Row {
                            RadioButton(
                                selected = state.categoryDirection == direction,
                                onClick = { actions.categories.directionChanged(direction) },
                                enabled = state.editingCategoryId == null,
                            )
                            Text(direction.name.lowercase().replaceFirstChar(Char::uppercase))
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        actions.categories.save()
                    },
                    enabled = !state.isSaving,
                ) { Text(if (state.editingCategoryId == null) "Add category" else "Save category") }
            }
            item {
                Text("Tags", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                OutlinedTextField(
                    state.tagName,
                    actions.tags.nameChanged,
                    label = { Text("Tag name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                )
                Row {
                    RadioButton(selected = state.tagCategoryId == null, onClick = { actions.tags.categoryChanged(null) })
                    Text("Independent")
                }
                state.categories.filterNot(FinanceCategory::archived).forEach { category: FinanceCategory ->
                    Row {
                        RadioButton(selected = state.tagCategoryId == category.id, onClick = { actions.tags.categoryChanged(category.id) })
                        Text("${category.name} tag")
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        actions.tags.save()
                    },
                    enabled = !state.isSaving,
                ) { Text(if (state.editingTagId == null) "Add tag" else "Save tag") }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            items(state.categories, key = { "category-${it.id}" }) { category: FinanceCategory ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${category.name} · ${category.direction.name.lowercase()}${if (category.archived) " · archived" else ""}")
                    if (!category.archived) {
                        TextButton(onClick = { actions.categories.edit(category) }) { Text("Rename") }
                        TextButton(onClick = { actions.categories.archive(category.id) }) { Text("Archive") }
                    }
                }
            }
            items(state.tags, key = { "tag-${it.id}" }) { tag: FinanceTag ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${tag.name}${tag.categoryName?.let { " · $it" }.orEmpty()}${if (tag.archived) " · archived" else ""}")
                    if (!tag.archived) {
                        TextButton(onClick = { actions.tags.edit(tag) }) { Text("Rename") }
                        TextButton(onClick = { actions.tags.archive(tag.id) }) { Text("Archive") }
                    }
                }
            }
        }
    }
}
