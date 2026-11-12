package com.toshlol.personalfinance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.toshlol.personalfinance.data.FinanceCategory
import com.toshlol.personalfinance.data.FinanceTag
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState

@Composable
fun CategoryTagScreen(
    state: TransactionsUiState,
    onClose: () -> Unit,
    onCategoryNameChanged: (String) -> Unit,
    onCategoryDirectionChanged: (TransactionDirection) -> Unit,
    onSaveCategory: () -> Unit,
    onEditCategory: (FinanceCategory) -> Unit,
    onArchiveCategory: (String) -> Unit,
    onTagNameChanged: (String) -> Unit,
    onTagCategoryChanged: (String?) -> Unit,
    onSaveTag: () -> Unit,
    onEditTag: (FinanceTag) -> Unit,
    onArchiveTag: (String) -> Unit,
    onOrganizerAccountNameChanged: (String) -> Unit,
    onSaveAccountName: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Categories and tags", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        OutlinedButton(onClick = onClose) { Text("Back to overview") }
        LazyColumn(Modifier.testTag("category-tag-list"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("Account", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                OutlinedTextField(
                    state.accountNameDraft,
                    onOrganizerAccountNameChanged,
                    label = { Text("Account name") },
                    singleLine = true,
                )
                Button(
                    onClick = {
                        keyboardController?.hide()
                        onSaveAccountName()
                    },
                    enabled = !state.isSaving,
                ) { Text("Save account name") }
            }
            item {
                Text("Categories", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                OutlinedTextField(state.categoryName, onCategoryNameChanged, label = { Text("Category name") }, singleLine = true)
                Row {
                    TransactionDirection.entries.forEach { direction: TransactionDirection ->
                        Row {
                            RadioButton(
                                selected = state.categoryDirection == direction,
                                onClick = { onCategoryDirectionChanged(direction) },
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
                        onSaveCategory()
                    },
                    enabled = !state.isSaving,
                ) { Text(if (state.editingCategoryId == null) "Add category" else "Save category") }
            }
            item {
                Text("Tags", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                OutlinedTextField(state.tagName, onTagNameChanged, label = { Text("Tag name") }, singleLine = true)
                Row {
                    RadioButton(selected = state.tagCategoryId == null, onClick = { onTagCategoryChanged(null) })
                    Text("Independent")
                }
                state.categories.filterNot(FinanceCategory::archived).forEach { category: FinanceCategory ->
                    Row {
                        RadioButton(selected = state.tagCategoryId == category.id, onClick = { onTagCategoryChanged(category.id) })
                        Text("${category.name} tag")
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        onSaveTag()
                    },
                    enabled = !state.isSaving,
                ) { Text(if (state.editingTagId == null) "Add tag" else "Save tag") }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            items(state.categories, key = { "category-${it.id}" }) { category: FinanceCategory ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${category.name} · ${category.direction.name.lowercase()}${if (category.archived) " · archived" else ""}")
                    if (!category.archived) {
                        TextButton(onClick = { onEditCategory(category) }) { Text("Rename") }
                        TextButton(onClick = { onArchiveCategory(category.id) }) { Text("Archive") }
                    }
                }
            }
            items(state.tags, key = { "tag-${it.id}" }) { tag: FinanceTag ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${tag.name}${tag.categoryName?.let { " · $it" }.orEmpty()}${if (tag.archived) " · archived" else ""}")
                    if (!tag.archived) {
                        TextButton(onClick = { onEditTag(tag) }) { Text("Rename") }
                        TextButton(onClick = { onArchiveTag(tag.id) }) { Text("Archive") }
                    }
                }
            }
        }
    }
}
