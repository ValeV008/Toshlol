package com.toshlol.personalfinance.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.toshlol.personalfinance.data.FinanceCategory
import com.toshlol.personalfinance.data.FinanceTag
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState

data class TransactionFormActions(
    val close: () -> Unit,
    val amountChanged: (String) -> Unit,
    val dateChanged: (String) -> Unit,
    val noteChanged: (String) -> Unit,
    val directionChanged: (TransactionDirection) -> Unit,
    val save: () -> Unit,
    val categoryChanged: (String?) -> Unit,
    val tagToggled: (String) -> Unit,
)

data class TransactionDeleteActions(
    val cancelDelete: () -> Unit,
    val confirmDelete: () -> Unit,
)

data class TransactionDialogActions(
    val form: TransactionFormActions,
    val delete: TransactionDeleteActions,
)

@Composable
fun TransactionDialogs(
    state: TransactionsUiState,
    actions: TransactionDialogActions,
) {
    val keyboardController: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current
    LaunchedEffect(state.isFormVisible) {
        if (!state.isFormVisible) keyboardController?.hide()
    }
    if (state.isFormVisible) {
        AlertDialog(
            onDismissRequest = actions.form.close,
            title = { Text(if (state.editingId == null) "New transaction" else "Edit transaction") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).testTag("transaction-form"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row {
                        TransactionDirection.entries.forEach { direction: TransactionDirection ->
                            Row(Modifier.clickable { actions.form.directionChanged(direction) }) {
                                RadioButton(selected = state.direction == direction, onClick = { actions.form.directionChanged(direction) })
                                Text(direction.name.lowercase().replaceFirstChar(Char::uppercase))
                            }
                        }
                    }
                    OutlinedTextField(state.amount, actions.form.amountChanged, label = { Text("Amount") }, singleLine = true)
                    OutlinedTextField(state.date, actions.form.dateChanged, label = { Text("Date (YYYY-MM-DD)") }, singleLine = true)
                    OutlinedTextField(state.note, actions.form.noteChanged, label = { Text("Note (optional)") }, singleLine = true)
                    Text("Category (optional)", style = MaterialTheme.typography.titleSmall)
                    Row(
                        Modifier.heightIn(min = 48.dp).selectable(
                            selected = state.categoryId == null,
                            role = Role.RadioButton,
                            onClick = { actions.form.categoryChanged(null) },
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = state.categoryId == null, onClick = null)
                        Text("Uncategorized")
                    }
                    state.categories
                        .filter { !it.archived || it.id == state.categoryId }
                        .filter { it.direction == state.direction }
                        .forEach { category: FinanceCategory ->
                            Row(
                                Modifier.heightIn(min = 48.dp).selectable(
                                    selected = state.categoryId == category.id,
                                    role = Role.RadioButton,
                                    onClick = { actions.form.categoryChanged(category.id) },
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = state.categoryId == category.id, onClick = null)
                                Text(category.name + if (category.archived) " (archived)" else "")
                            }
                        }
                    Text("Tags (optional)", style = MaterialTheme.typography.titleSmall)
                    state.tags.filter { !it.archived || it.id in state.tagIds }.forEach { tag: FinanceTag ->
                        val canAssignTag: Boolean = tag.categoryId == null || tag.categoryId == state.categoryId
                        Row(
                            Modifier.heightIn(min = 48.dp).toggleable(
                                value = tag.id in state.tagIds,
                                enabled = canAssignTag,
                                role = Role.Checkbox,
                                onValueChange = { actions.form.tagToggled(tag.id) },
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.material3.Checkbox(
                                checked = tag.id in state.tagIds,
                                onCheckedChange = null,
                                enabled = canAssignTag,
                            )
                            Text(tag.name + tag.categoryName?.let { " · $it" }.orEmpty() + if (tag.archived) " (archived)" else "")
                        }
                    }
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { Button(onClick = actions.form.save, enabled = !state.isSaving) { Text("Save") } },
            dismissButton = { OutlinedButton(onClick = actions.form.close) { Text("Cancel") } },
        )
    }
    state.pendingDeleteId?.let {
        AlertDialog(
            onDismissRequest = actions.delete.cancelDelete,
            title = { Text("Delete transaction?") },
            text = { Text("This transaction will be permanently deleted.") },
            confirmButton = { Button(onClick = actions.delete.confirmDelete) { Text("Delete") } },
            dismissButton = { TextButton(onClick = actions.delete.cancelDelete) { Text("Cancel") } },
        )
    }
}
