package com.toshlol.personalfinance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    val accountChanged: (String) -> Unit,
)

data class TransactionDeleteActions(
    val cancelDelete: () -> Unit,
    val confirmDelete: () -> Unit,
)

data class TransactionDialogActions(
    val form: TransactionFormActions,
    val delete: TransactionDeleteActions,
)

data class TransferFormActions(
    val close: () -> Unit,
    val save: () -> Unit,
    val amountChanged: (String) -> Unit,
    val dateChanged: (String) -> Unit,
    val sourceChanged: (String) -> Unit,
    val destinationChanged: (String) -> Unit,
    val delete: (String) -> Unit,
    val cancelDelete: () -> Unit,
    val confirmDelete: () -> Unit,
)

@Composable
@Suppress("ktlint:standard:no-unit-return")
fun TransactionDialogs(
    state: TransactionsUiState,
    actions: TransactionDialogActions,
): Unit {
    val keyboardController: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current
    LaunchedEffect(state.isFormVisible) { if (!state.isFormVisible) keyboardController?.hide() }
    if (state.isFormVisible) TransactionEntryDialog(state, actions.form)
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

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun TransactionEntryDialog(
    state: TransactionsUiState,
    actions: TransactionFormActions,
): Unit {
    val charcoal: Color = Color(0xFF292B2F)
    Dialog(onDismissRequest = actions.close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            color = charcoal,
        ) {
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                        .testTag("transaction-form"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(if (state.editingId == null) "New transaction" else "Edit transaction", color = Color.White)
                    }
                    Row(Modifier.fillMaxWidth()) {
                        TransactionDirection.entries.forEach { direction ->
                            TextButton(
                                onClick = { actions.directionChanged(direction) },
                                Modifier
                                    .weight(1f)
                                    .semantics {
                                        role = Role.RadioButton
                                        selected = state.direction == direction
                                    }.testTag("transaction-direction-${direction.name}"),
                            ) {
                                Text(
                                    direction.name.lowercase().replaceFirstChar(Char::uppercase),
                                    color =
                                        if (state.direction ==
                                            direction
                                        ) {
                                            Color(0xFFFFD23F)
                                        } else {
                                            Color.White
                                        },
                                )
                            }
                        }
                    }
                    Text("Account", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Selected account: ${state.accounts
                            .firstOrNull {
                                it.id == state.transactionAccountId
                            }?.name ?: "Choose an Account"}",
                        color = Color.White,
                    )
                    state.accounts.filterNot { it.isArchived }.forEach { account ->
                        TextButton(
                            onClick = { actions.accountChanged(account.id) },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .semantics {
                                        role = Role.RadioButton
                                        selected = state.transactionAccountId == account.id
                                    }.testTag("transaction-account-${account.id}"),
                        ) {
                            RadioButton(selected = state.transactionAccountId == account.id, onClick = null)
                            Text(account.name, color = Color.White)
                        }
                    }
                    Text("Amount", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${state.currency} ${state.amount.ifBlank { "0.00" }}",
                        Modifier.fillMaxWidth().semantics { contentDescription = "Amount" }.testTag("transaction-amount"),
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    OutlinedTextField(state.note, actions.noteChanged, label = {
                        Text("Note (optional)")
                    }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    TransactionDateField(state.date, actions.dateChanged)
                    Text("Category (optional)", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    CategoryChoice("Uncategorized", state.categoryId == null) { actions.categoryChanged(null) }
                    state.categories
                        .filter { (!it.archived || it.id == state.categoryId) && it.direction == state.direction }
                        .forEach { category ->
                            CategoryChoice(category.name + if (category.archived) " (archived)" else "", state.categoryId == category.id) {
                                actions.categoryChanged(category.id)
                            }
                        }
                    Text("Tags (optional)", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    state.tags.filter { !it.archived || it.id in state.tagIds }.forEach { tag ->
                        val canAssignTag: Boolean = tag.categoryId == null || tag.categoryId == state.categoryId
                        val isCategoryArchived: Boolean = state.categories.firstOrNull { it.id == tag.categoryId }?.archived == true
                        val categoryLabel: String =
                            tag.categoryName?.let { " · $it" }.orEmpty() + if (isCategoryArchived) " (archived Category)" else ""
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .toggleable(
                                    value = tag.id in state.tagIds,
                                    enabled = canAssignTag,
                                    role = Role.Checkbox,
                                    onValueChange = { actions.tagToggled(tag.id) },
                                ).testTag("transaction-tag-${tag.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = tag.id in state.tagIds, onCheckedChange = null, enabled = canAssignTag)
                            Text(
                                tag.name + categoryLabel + if (tag.archived) " (archived)" else "",
                                color = Color.White,
                            )
                        }
                    }
                    state.error?.let { Text(it, color = Color(0xFFFF8A80)) }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(onClick = actions.close, modifier = Modifier.weight(1f).testTag("transaction-cancel")) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = actions.save,
                        enabled = !state.isSaving,
                        modifier = Modifier.weight(1f).testTag("transaction-save"),
                    ) {
                        Text("Save")
                    }
                }
                AmountKeypad(state.amount, state.currency, actions.amountChanged)
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun CategoryChoice(
    label: String,
    isSelected: Boolean,
    onSelected: () -> Unit,
): Unit {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
            selected = isSelected,
            role = Role.RadioButton,
            onClick = onSelected,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Text(label, color = Color.White)
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
fun AmountKeypad(
    amount: String,
    currency: String,
    onAmountChanged: (String) -> Unit,
): Unit {
    val fractionDigits: Int =
        runCatching {
            java.util.Currency
                .getInstance(
                    currency,
                ).defaultFractionDigits
                .coerceAtLeast(0)
        }.getOrDefault(2)
    val currentMinor: Long = amount.toBigDecimalOrNull()?.movePointRight(fractionDigits)?.toLong() ?: 0L
    val keys: List<String> = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "C")
    Column(
        Modifier.fillMaxWidth().background(Color(0xFF292B2F)).padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        keys.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    Card(
                        Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clickable {
                                val nextMinor: Long =
                                    when (key) {
                                        "C" -> 0L
                                        "⌫" -> currentMinor / 10L
                                        else ->
                                            if (currentMinor >
                                                (Long.MAX_VALUE - key.toLong()) / 10L
                                            ) {
                                                currentMinor
                                            } else {
                                                currentMinor * 10L + key.toLong()
                                            }
                                    }
                                onAmountChanged(
                                    java.math.BigDecimal
                                        .valueOf(nextMinor, fractionDigits)
                                        .toPlainString(),
                                )
                            }.testTag("amount-key-$key"),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF414348)),
                    ) {
                        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(key, color = Color.White, fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
fun TransactionDateField(
    value: String,
    onValueChange: (String) -> Unit,
): Unit {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Date (YYYY-MM-DD)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFFFFD23F),
                focusedBorderColor = Color(0xFFFFD23F),
                unfocusedBorderColor = Color(0xFFCFD1D5),
                focusedLabelColor = Color(0xFFFFD23F),
                unfocusedLabelColor = Color(0xFFCFD1D5),
            ),
    )
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
fun TransferEntryDialogs(
    state: TransactionsUiState,
    actions: TransferFormActions,
): Unit {
    val keyboardController: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current
    LaunchedEffect(state.isTransferFormVisible) { if (!state.isTransferFormVisible) keyboardController?.hide() }
    if (state.isTransferFormVisible) TransferEntryDialog(state, actions)
    if (state.pendingTransferDeleteId != null) {
        AlertDialog(
            onDismissRequest = actions.cancelDelete,
            title = { Text("Delete Transfer?") },
            text = { Text("This Transfer will be permanently deleted.") },
            confirmButton = {
                Button(onClick = actions.confirmDelete, modifier = Modifier.testTag("confirm-transfer-delete")) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = actions.cancelDelete, modifier = Modifier.testTag("cancel-transfer-delete")) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun TransferEntryDialog(
    state: TransactionsUiState,
    actions: TransferFormActions,
): Unit {
    val charcoal: Color = Color(0xFF292B2F)
    Dialog(onDismissRequest = actions.close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            color = charcoal,
        ) {
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                        .testTag("transfer-form"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (state.editingTransferId == null) "New Transfer" else "Edit Transfer",
                            Modifier.weight(1f),
                            color = Color.White,
                        )
                        state.editingTransferId?.let { id ->
                            TextButton(onClick = { actions.delete(id) }, modifier = Modifier.testTag("transfer-delete")) {
                                Text("Delete", color = Color.White)
                            }
                        }
                    }
                    Text("Amount", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${state.currency} ${state.amount.ifBlank { "0.00" }}",
                        Modifier.fillMaxWidth().semantics { contentDescription = "Amount" }.testTag("transfer-amount"),
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    TransactionDateField(state.date, actions.dateChanged)
                    Text("Source Account", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    state.accounts.filterNot { it.isArchived }.forEach { account ->
                        TransferAccountChoice(
                            accountName = account.name,
                            isSelected = state.transferSourceAccountId == account.id,
                            testTag = "transfer-source-${account.id}",
                            onSelected = { actions.sourceChanged(account.id) },
                        )
                    }
                    Text("Destination Account", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    state.accounts.filterNot { it.isArchived }.forEach { account ->
                        TransferAccountChoice(
                            accountName = account.name,
                            isSelected = state.transferDestinationAccountId == account.id,
                            testTag = "transfer-destination-${account.id}",
                            onSelected = { actions.destinationChanged(account.id) },
                        )
                    }
                    state.error?.let { Text(it, color = Color(0xFFFF8A80)) }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(onClick = actions.close, modifier = Modifier.weight(1f).testTag("transfer-cancel")) {
                        Text("Cancel")
                    }
                    Button(onClick = actions.save, modifier = Modifier.weight(1f).testTag("transfer-save")) {
                        Text("Save")
                    }
                }
                AmountKeypad(state.amount, state.currency, actions.amountChanged)
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun TransferAccountChoice(
    accountName: String,
    isSelected: Boolean,
    testTag: String,
    onSelected: () -> Unit,
): Unit {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onSelected)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Text(accountName, color = Color.White)
    }
}
