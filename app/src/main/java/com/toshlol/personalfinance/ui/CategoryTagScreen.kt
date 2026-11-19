package com.toshlol.personalfinance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toshlol.personalfinance.data.Account
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.data.FinanceCategory
import com.toshlol.personalfinance.data.FinanceTag
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState

private val ClassificationCharcoal: Color = Color(0xFF292B2F)
private val ClassificationPageGray: Color = Color(0xFFF1F2F4)
private val ClassificationCardRadius: Dp = 20.dp
private val ClassificationRowRadius: Dp = 16.dp
private val ClassificationPageHorizontalPadding: Dp = 18.dp
private val ClassificationPageVerticalPadding: Dp = 16.dp
private val ClassificationPageItemSpacing: Dp = 12.dp
private val ClassificationErrorPadding: Dp = 12.dp
private val ClassificationHeaderPadding: Dp = 18.dp
private val ClassificationCardPadding: Dp = 16.dp
private val ClassificationFormItemSpacing: Dp = 10.dp
private val ClassificationStandardSpacing: Dp = 8.dp
private val ClassificationAccountActionSpacing: Dp = 4.dp
private val ClassificationRowHorizontalPadding: Dp = 16.dp
private val ClassificationRowVerticalPadding: Dp = 10.dp
private val ClassificationActionMinimumHeight: Dp = 48.dp
private val ClassificationRadioMinimumHeight: Dp = 52.dp

@Composable
@Suppress("ktlint:standard:no-unit-return")
fun CategoryTagScreen(
    state: TransactionsUiState,
    actions: OrganizerActions,
): Unit {
    var isAccountSectionVisible: Boolean by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(ClassificationPageGray)) {
        ClassificationHeader(actions.close, isEnabled = !state.isSaving)
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().testTag("category-tag-list"),
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(horizontal = ClassificationPageHorizontalPadding, vertical = ClassificationPageVerticalPadding),
            verticalArrangement = Arrangement.spacedBy(ClassificationPageItemSpacing),
        ) {
            item {
                AccountManagementCard(state, actions.accounts, isAccountSectionVisible) {
                    isAccountSectionVisible = !isAccountSectionVisible
                }
            }
            item {
                CategoryEditorCard(state, actions.categories)
            }
            item {
                Text("Categories", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            }
            items(state.categories, key = { category -> "category-${category.id}" }) { category ->
                CategoryRow(category, actions.categories, isEnabled = !state.isSaving)
            }
            item {
                TagEditorCard(state, actions.tags)
            }
            item {
                Text("Tags", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            }
            items(state.tags, key = { tag -> "tag-${tag.id}" }) { tag ->
                TagRow(tag, state.categories, actions.tags, isEnabled = !state.isSaving)
            }
        }
        state.error?.let { error ->
            Text(
                error,
                Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(ClassificationErrorPadding)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun ClassificationHeader(
    onBack: () -> Unit,
    isEnabled: Boolean,
): Unit {
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                ClassificationCharcoal,
                androidx.compose.foundation.shape
                    .RoundedCornerShape(ClassificationCardRadius),
            ).padding(ClassificationHeaderPadding),
        verticalArrangement = Arrangement.spacedBy(ClassificationStandardSpacing),
    ) {
        Text(
            "Categories & Tags",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "Use text labels to organize Income and Expense Transactions.",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(
            onClick = onBack,
            enabled = isEnabled,
            modifier = Modifier.heightIn(min = ClassificationActionMinimumHeight).testTag("classification-back"),
        ) {
            Text("Back to overview")
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun AccountManagementCard(
    state: TransactionsUiState,
    actions: AccountEditorActions,
    isVisible: Boolean,
    onVisibilityChanged: () -> Unit,
): Unit {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape =
            androidx.compose.foundation.shape
                .RoundedCornerShape(ClassificationCardRadius),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(ClassificationCardPadding),
            verticalArrangement = Arrangement.spacedBy(ClassificationStandardSpacing),
        ) {
            Text("Accounts", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            OutlinedButton(onClick = onVisibilityChanged, modifier = Modifier.heightIn(min = ClassificationActionMinimumHeight)) {
                Text(if (isVisible) "Hide accounts" else "Manage accounts")
            }
            if (isVisible) {
                AccountManagementRows(state, actions)
                AccountDetailsForm(state, actions)
                AccountCreationForm(state, actions)
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun AccountManagementRows(
    state: TransactionsUiState,
    actions: AccountEditorActions,
): Unit {
    Column(verticalArrangement = Arrangement.spacedBy(ClassificationAccountActionSpacing)) {
        Text("Account closing balances for ${state.selectedMonth}", style = MaterialTheme.typography.titleMedium)
        state.accounts.forEach { account: Account -> AccountManagementRow(account, state, actions) }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun AccountManagementRow(
    account: Account,
    state: TransactionsUiState,
    actions: AccountEditorActions,
): Unit {
    Column {
        val closingBalanceMinor: Long = state.monthlyOverview.closingBalancesMinor[account.id] ?: 0L
        val archiveLabel: String = if (account.isArchived) " · Archived" else ""
        Text(
            "${account.name} · ${account.type.label} · ${state.currency} ${formatMoney(
                closingBalanceMinor,
                state.currency,
            )}$archiveLabel",
        )
        if (!account.isArchived) {
            Row(horizontalArrangement = Arrangement.spacedBy(ClassificationAccountActionSpacing)) {
                val accountAction: String = if (state.editingAccountId == account.id) "Editing" else "Edit"
                TextButton(
                    onClick = { actions.select(account.id) },
                    modifier = Modifier.heightIn(min = ClassificationActionMinimumHeight),
                ) {
                    Text("$accountAction ${account.name}")
                }
                TextButton(
                    onClick = { actions.archive(account.id) },
                    modifier = Modifier.heightIn(min = ClassificationActionMinimumHeight),
                ) {
                    Text("Archive ${account.name}")
                }
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun AccountDetailsForm(
    state: TransactionsUiState,
    actions: AccountEditorActions,
): Unit {
    Column(verticalArrangement = Arrangement.spacedBy(ClassificationFormItemSpacing)) {
        OutlinedTextField(
            value = state.accountNameDraft,
            onValueChange = actions.nameChanged,
            label = { Text("Account name") },
            singleLine = true,
        )
        OutlinedTextField(value = state.openingBalanceAmount, onValueChange = actions.openingBalanceChanged, label = {
            Text("Account opening balance")
        }, singleLine = true)
        OutlinedTextField(value = state.openingBalanceDate, onValueChange = actions.openingDateChanged, label = {
            Text("Account opening date (YYYY-MM-DD)")
        }, singleLine = true)
        Button(
            onClick = actions.save,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth().heightIn(min = ClassificationActionMinimumHeight),
        ) {
            Text("Save account details")
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun AccountCreationForm(
    state: TransactionsUiState,
    actions: AccountEditorActions,
): Unit {
    Column(verticalArrangement = Arrangement.spacedBy(ClassificationFormItemSpacing)) {
        Text("Add Account", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = state.newAccountName, onValueChange = actions.newNameChanged, label = {
            Text("New account name")
        }, singleLine = true)
        OutlinedTextField(value = state.newAccountOpeningBalance, onValueChange = actions.newOpeningBalanceChanged, label = {
            Text("New account opening balance")
        }, singleLine = true)
        OutlinedTextField(
            value = state.newAccountOpeningDate,
            onValueChange = actions.newOpeningDateChanged,
            label = { Text("New account opening date (YYYY-MM-DD)") },
            singleLine = true,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ClassificationStandardSpacing)) {
            AccountType.entries.forEach { type: AccountType ->
                ClassificationRadioChoice(
                    label = type.name.lowercase().replaceFirstChar(Char::uppercase),
                    isSelected = state.newAccountType == type,
                    onSelected = { actions.newTypeChanged(type) },
                    modifier = Modifier.weight(1f).testTag("account-type-${type.name}"),
                )
            }
        }
        Button(
            onClick = actions.create,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth().heightIn(min = ClassificationActionMinimumHeight),
        ) {
            Text("Create account")
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun CategoryEditorCard(
    state: TransactionsUiState,
    actions: CategoryActions,
): Unit {
    val keyboardController: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current
    val isEditing: Boolean = state.editingCategoryId != null
    Card(
        Modifier.fillMaxWidth().testTag("category-editor"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape =
            androidx.compose.foundation.shape
                .RoundedCornerShape(ClassificationCardRadius),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(ClassificationCardPadding),
            verticalArrangement = Arrangement.spacedBy(ClassificationFormItemSpacing),
        ) {
            Text(
                if (isEditing) "Edit Category" else "New Category",
                style = MaterialTheme.typography.titleLarge,
                modifier =
                    Modifier
                        .semantics {
                            heading()
                        },
            )
            OutlinedTextField(
                value = state.categoryName,
                onValueChange = actions.nameChanged,
                label = { Text("Category name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Direction", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ClassificationStandardSpacing)) {
                TransactionDirection.entries.forEach { direction ->
                    ClassificationRadioChoice(
                        label = direction.name.lowercase().replaceFirstChar(Char::uppercase),
                        isSelected = state.categoryDirection == direction,
                        onSelected = { actions.directionChanged(direction) },
                        enabled = !isEditing,
                        modifier = Modifier.weight(1f).testTag("category-direction-${direction.name}"),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ClassificationStandardSpacing)) {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        actions.save()
                    },
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f).heightIn(min = ClassificationActionMinimumHeight),
                ) { Text(if (isEditing) "Save category" else "Add category") }
                if (isEditing) {
                    OutlinedButton(
                        onClick = {
                            keyboardController?.hide()
                            actions.cancelEditing()
                        },
                        enabled = !state.isSaving,
                        modifier = Modifier.weight(1f).heightIn(min = ClassificationActionMinimumHeight),
                    ) { Text("Cancel Category edit") }
                }
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun CategoryRow(
    category: FinanceCategory,
    actions: CategoryActions,
    isEnabled: Boolean,
): Unit {
    Card(
        Modifier.fillMaxWidth().testTag("category-row-${category.id}").semantics {
            stateDescription = if (category.archived) "Archived" else "Active"
        },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape =
            androidx.compose.foundation.shape
                .RoundedCornerShape(ClassificationRowRadius),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = ClassificationRowHorizontalPadding, vertical = ClassificationRowVerticalPadding),
        ) {
            Text(
                "${category.name} · ${category.direction.name.lowercase()}${if (category.archived) " · Archived" else ""}",
                style = MaterialTheme.typography.titleMedium,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = { actions.edit(category) },
                    enabled = isEnabled,
                    modifier = Modifier.heightIn(min = ClassificationActionMinimumHeight).testTag("edit-category-${category.id}"),
                ) {
                    Text(if (category.archived) "Rename archived" else "Rename")
                }
                if (!category.archived) {
                    TextButton(
                        onClick = { actions.archive(category.id) },
                        enabled = isEnabled,
                        modifier = Modifier.heightIn(min = ClassificationActionMinimumHeight).testTag("archive-category-${category.id}"),
                    ) {
                        Text("Archive")
                    }
                }
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun TagEditorCard(
    state: TransactionsUiState,
    actions: TagActions,
): Unit {
    val keyboardController: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current
    val isEditing: Boolean = state.editingTagId != null
    val availableCategories: List<FinanceCategory> =
        state.categories.filter { category -> !category.archived || category.id == state.tagCategoryId }
    Card(
        Modifier.fillMaxWidth().testTag("tag-editor"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape =
            androidx.compose.foundation.shape
                .RoundedCornerShape(ClassificationCardRadius),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(ClassificationCardPadding),
            verticalArrangement = Arrangement.spacedBy(ClassificationFormItemSpacing),
        ) {
            Text(
                if (isEditing) "Edit Tag" else "New Tag",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            OutlinedTextField(
                value = state.tagName,
                onValueChange = actions.nameChanged,
                label = { Text("Tag name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Category association", style = MaterialTheme.typography.titleMedium)
            ClassificationRadioChoice(
                label = "Independent",
                isSelected = state.tagCategoryId == null,
                onSelected = { actions.categoryChanged(null) },
                modifier = Modifier.testTag("tag-association-independent"),
            )
            availableCategories.forEach { category ->
                ClassificationRadioChoice(
                    label = "${category.name} Category${if (category.archived) " (Archived)" else ""}",
                    isSelected = state.tagCategoryId == category.id,
                    onSelected = { actions.categoryChanged(category.id) },
                    modifier = Modifier.testTag("tag-category-${category.id}"),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ClassificationStandardSpacing)) {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        actions.save()
                    },
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f).heightIn(min = ClassificationActionMinimumHeight),
                ) { Text(if (isEditing) "Save tag" else "Add tag") }
                if (isEditing) {
                    OutlinedButton(
                        onClick = {
                            keyboardController?.hide()
                            actions.cancelEditing()
                        },
                        enabled = !state.isSaving,
                        modifier = Modifier.weight(1f).heightIn(min = ClassificationActionMinimumHeight),
                    ) { Text("Cancel Tag edit") }
                }
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun TagRow(
    tag: FinanceTag,
    categories: List<FinanceCategory>,
    actions: TagActions,
    isEnabled: Boolean,
): Unit {
    val category: FinanceCategory? = categories.firstOrNull { category -> category.id == tag.categoryId }
    val association: String =
        category?.let { parent ->
            "${parent.name} Category${if (parent.archived) " (Archived)" else ""}"
        } ?: if (tag.categoryId == null) "Independent" else "${tag.categoryName ?: "Unknown"} Category"
    Card(
        Modifier.fillMaxWidth().testTag("tag-row-${tag.id}").semantics {
            stateDescription = if (tag.archived) "Archived" else "Active"
        },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape =
            androidx.compose.foundation.shape
                .RoundedCornerShape(ClassificationRowRadius),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = ClassificationRowHorizontalPadding, vertical = ClassificationRowVerticalPadding),
        ) {
            Text(tag.name, style = MaterialTheme.typography.titleMedium)
            Text("$association${if (tag.archived) " · Archived" else ""}", style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = { actions.edit(tag) },
                    enabled = isEnabled,
                    modifier = Modifier.heightIn(min = ClassificationActionMinimumHeight).testTag("edit-tag-${tag.id}"),
                ) {
                    Text(if (tag.archived) "Rename archived" else "Rename")
                }
                if (!tag.archived) {
                    TextButton(
                        onClick = { actions.archive(tag.id) },
                        enabled = isEnabled,
                        modifier = Modifier.heightIn(min = ClassificationActionMinimumHeight).testTag("archive-tag-${tag.id}"),
                    ) {
                        Text("Archive")
                    }
                }
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun ClassificationRadioChoice(
    label: String,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
): Unit {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = ClassificationRadioMinimumHeight)
            .selectable(selected = isSelected, enabled = enabled, role = Role.RadioButton, onClick = onSelected)
            .padding(horizontal = ClassificationStandardSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isSelected, onClick = null, enabled = enabled)
        Text(label, modifier = Modifier.padding(start = ClassificationStandardSpacing))
    }
}
