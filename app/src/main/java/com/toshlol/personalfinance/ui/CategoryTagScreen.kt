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

private val classificationCharcoal: Color = Color(0xFF292B2F)
private val classificationPageGray: Color = Color(0xFFF1F2F4)
private val classificationLabelSeparator: String = "\u00b7"
private val classificationCardRadius: Dp = 20.dp
private val classificationRowRadius: Dp = 16.dp
private val classificationPageHorizontalPadding: Dp = 18.dp
private val classificationPageVerticalPadding: Dp = 16.dp
private val classificationPageItemSpacing: Dp = 12.dp
private val classificationErrorPadding: Dp = 12.dp
private val classificationHeaderPadding: Dp = 18.dp
private val classificationCardPadding: Dp = 16.dp
private val classificationFormItemSpacing: Dp = 10.dp
private val classificationStandardSpacing: Dp = 8.dp
private val classificationAccountActionSpacing: Dp = 4.dp
private val classificationRowHorizontalPadding: Dp = 16.dp
private val classificationRowVerticalPadding: Dp = 10.dp
private val classificationActionMinimumHeight: Dp = 48.dp
private val classificationRadioMinimumHeight: Dp = 52.dp

@Composable
@Suppress("ktlint:standard:no-unit-return")
fun CategoryTagScreen(
    state: TransactionsUiState,
    actions: OrganizerActions,
): Unit {
    var isAccountSectionVisible: Boolean by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(classificationPageGray)) {
        renderClassificationHeader(actions.close, isEnabled = !state.isSaving)
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().testTag("category-tag-list"),
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(horizontal = classificationPageHorizontalPadding, vertical = classificationPageVerticalPadding),
            verticalArrangement = Arrangement.spacedBy(classificationPageItemSpacing),
        ) {
            item {
                renderAccountManagementCard(state, actions.accounts, isAccountSectionVisible) {
                    isAccountSectionVisible = !isAccountSectionVisible
                }
            }
            item {
                renderCategoryEditorCard(state, actions.categories)
            }
            item {
                Text("Categories", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            }
            items(state.categories, key = { category: FinanceCategory -> "category-${category.id}" }) { category: FinanceCategory ->
                renderCategoryRow(category, actions.categories, isEnabled = !state.isSaving)
            }
            item {
                renderTagEditorCard(state, actions.tags)
            }
            item {
                Text("Tags", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            }
            items(state.tags, key = { tag: FinanceTag -> "tag-${tag.id}" }) { tag: FinanceTag ->
                renderTagRow(tag, state.categories, actions.tags, isEnabled = !state.isSaving)
            }
        }
        state.error?.let { error: String ->
            Text(
                error,
                Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(classificationErrorPadding)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun renderClassificationHeader(
    onBack: () -> Unit,
    isEnabled: Boolean,
): Unit {
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                classificationCharcoal,
                androidx.compose.foundation.shape
                    .RoundedCornerShape(classificationCardRadius),
            ).padding(classificationHeaderPadding),
        verticalArrangement = Arrangement.spacedBy(classificationStandardSpacing),
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
            modifier = Modifier.heightIn(min = classificationActionMinimumHeight).testTag("classification-back"),
        ) {
            Text("Back to overview")
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun renderAccountManagementCard(
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
                .RoundedCornerShape(classificationCardRadius),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(classificationCardPadding),
            verticalArrangement = Arrangement.spacedBy(classificationStandardSpacing),
        ) {
            Text("Accounts", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            OutlinedButton(onClick = onVisibilityChanged, modifier = Modifier.heightIn(min = classificationActionMinimumHeight)) {
                Text(if (isVisible) "Hide accounts" else "Manage accounts")
            }
            if (isVisible) {
                renderAccountManagementRows(state, actions)
                renderAccountDetailsForm(state, actions)
                renderAccountCreationForm(state, actions)
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun renderAccountManagementRows(
    state: TransactionsUiState,
    actions: AccountEditorActions,
): Unit {
    Column(verticalArrangement = Arrangement.spacedBy(classificationAccountActionSpacing)) {
        Text("Account closing balances for ${state.selectedMonth}", style = MaterialTheme.typography.titleMedium)
        state.accounts.forEach { account: Account -> renderAccountManagementRow(account, state, actions) }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun renderAccountManagementRow(
    account: Account,
    state: TransactionsUiState,
    actions: AccountEditorActions,
): Unit {
    Column {
        val closingBalanceMinor: Long = state.monthlyOverview.closingBalancesMinor[account.id] ?: 0L
        val archiveLabel: String = if (account.isArchived) " $classificationLabelSeparator Archived" else ""
        val accountBalance: String = formatMoney(closingBalanceMinor, state.currency)
        val accountSummary: String =
            "${account.name} $classificationLabelSeparator ${account.type.label} $classificationLabelSeparator ${state.currency} $accountBalance$archiveLabel"
        Text(accountSummary)
        if (!account.isArchived) {
            Row(horizontalArrangement = Arrangement.spacedBy(classificationAccountActionSpacing)) {
                val accountAction: String = if (state.editingAccountId == account.id) "Editing" else "Edit"
                TextButton(
                    onClick = { actions.select(account.id) },
                    modifier = Modifier.heightIn(min = classificationActionMinimumHeight),
                ) {
                    Text("$accountAction ${account.name}")
                }
                TextButton(
                    onClick = { actions.archive(account.id) },
                    modifier = Modifier.heightIn(min = classificationActionMinimumHeight),
                ) {
                    Text("Archive ${account.name}")
                }
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun renderAccountDetailsForm(
    state: TransactionsUiState,
    actions: AccountEditorActions,
): Unit {
    Column(verticalArrangement = Arrangement.spacedBy(classificationFormItemSpacing)) {
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
            modifier = Modifier.fillMaxWidth().heightIn(min = classificationActionMinimumHeight),
        ) {
            Text("Save account details")
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun renderAccountCreationForm(
    state: TransactionsUiState,
    actions: AccountEditorActions,
): Unit {
    Column(verticalArrangement = Arrangement.spacedBy(classificationFormItemSpacing)) {
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(classificationStandardSpacing)) {
            AccountType.entries.forEach { type: AccountType ->
                renderClassificationRadioChoice(
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
            modifier = Modifier.fillMaxWidth().heightIn(min = classificationActionMinimumHeight),
        ) {
            Text("Create account")
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun renderCategoryEditorCard(
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
                .RoundedCornerShape(classificationCardRadius),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(classificationCardPadding),
            verticalArrangement = Arrangement.spacedBy(classificationFormItemSpacing),
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(classificationStandardSpacing)) {
                TransactionDirection.entries.forEach { direction: TransactionDirection ->
                    renderClassificationRadioChoice(
                        label = direction.name.lowercase().replaceFirstChar(Char::uppercase),
                        isSelected = state.categoryDirection == direction,
                        onSelected = { actions.directionChanged(direction) },
                        enabled = !isEditing,
                        modifier = Modifier.weight(1f).testTag("category-direction-${direction.name}"),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(classificationStandardSpacing)) {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        actions.save()
                    },
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f).heightIn(min = classificationActionMinimumHeight),
                ) { Text(if (isEditing) "Save category" else "Add category") }
                if (isEditing) {
                    OutlinedButton(
                        onClick = {
                            keyboardController?.hide()
                            actions.cancelEditing()
                        },
                        enabled = !state.isSaving,
                        modifier = Modifier.weight(1f).heightIn(min = classificationActionMinimumHeight),
                    ) { Text("Cancel Category edit") }
                }
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun renderCategoryRow(
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
                .RoundedCornerShape(classificationRowRadius),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = classificationRowHorizontalPadding, vertical = classificationRowVerticalPadding),
        ) {
            Text(
                "${category.name} $classificationLabelSeparator ${category.direction.name.lowercase()}${if (category.archived) " $classificationLabelSeparator Archived" else ""}",
                style = MaterialTheme.typography.titleMedium,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = { actions.edit(category) },
                    enabled = isEnabled,
                    modifier = Modifier.heightIn(min = classificationActionMinimumHeight).testTag("edit-category-${category.id}"),
                ) {
                    Text(if (category.archived) "Rename archived" else "Rename")
                }
                if (!category.archived) {
                    TextButton(
                        onClick = { actions.archive(category.id) },
                        enabled = isEnabled,
                        modifier = Modifier.heightIn(min = classificationActionMinimumHeight).testTag("archive-category-${category.id}"),
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
private fun renderTagEditorCard(
    state: TransactionsUiState,
    actions: TagActions,
): Unit {
    val keyboardController: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current
    val isEditing: Boolean = state.editingTagId != null
    val availableCategories: List<FinanceCategory> =
        state.categories.filter { category: FinanceCategory -> !category.archived || category.id == state.tagCategoryId }
    Card(
        Modifier.fillMaxWidth().testTag("tag-editor"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape =
            androidx.compose.foundation.shape
                .RoundedCornerShape(classificationCardRadius),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(classificationCardPadding),
            verticalArrangement = Arrangement.spacedBy(classificationFormItemSpacing),
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
            renderClassificationRadioChoice(
                label = "Independent",
                isSelected = state.tagCategoryId == null,
                onSelected = { actions.categoryChanged(null) },
                modifier = Modifier.testTag("tag-association-independent"),
            )
            availableCategories.forEach { category: FinanceCategory ->
                renderClassificationRadioChoice(
                    label = "${category.name} Category${if (category.archived) " (Archived)" else ""}",
                    isSelected = state.tagCategoryId == category.id,
                    onSelected = { actions.categoryChanged(category.id) },
                    modifier = Modifier.testTag("tag-category-${category.id}"),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(classificationStandardSpacing)) {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        actions.save()
                    },
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f).heightIn(min = classificationActionMinimumHeight),
                ) { Text(if (isEditing) "Save tag" else "Add tag") }
                if (isEditing) {
                    OutlinedButton(
                        onClick = {
                            keyboardController?.hide()
                            actions.cancelEditing()
                        },
                        enabled = !state.isSaving,
                        modifier = Modifier.weight(1f).heightIn(min = classificationActionMinimumHeight),
                    ) { Text("Cancel Tag edit") }
                }
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun renderTagRow(
    tag: FinanceTag,
    categories: List<FinanceCategory>,
    actions: TagActions,
    isEnabled: Boolean,
): Unit {
    val category: FinanceCategory? = categories.firstOrNull { candidate: FinanceCategory -> candidate.id == tag.categoryId }
    val association: String =
        category?.let { parent: FinanceCategory ->
            "${parent.name} Category${if (parent.archived) " (Archived)" else ""}"
        } ?: if (tag.categoryId == null) "Independent" else "${tag.categoryName ?: "Unknown"} Category"
    Card(
        Modifier.fillMaxWidth().testTag("tag-row-${tag.id}").semantics {
            stateDescription = if (tag.archived) "Archived" else "Active"
        },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape =
            androidx.compose.foundation.shape
                .RoundedCornerShape(classificationRowRadius),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = classificationRowHorizontalPadding, vertical = classificationRowVerticalPadding),
        ) {
            Text(tag.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "$association${if (tag.archived) " $classificationLabelSeparator Archived" else ""}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = { actions.edit(tag) },
                    enabled = isEnabled,
                    modifier = Modifier.heightIn(min = classificationActionMinimumHeight).testTag("edit-tag-${tag.id}"),
                ) {
                    Text(if (tag.archived) "Rename archived" else "Rename")
                }
                if (!tag.archived) {
                    TextButton(
                        onClick = { actions.archive(tag.id) },
                        enabled = isEnabled,
                        modifier = Modifier.heightIn(min = classificationActionMinimumHeight).testTag("archive-tag-${tag.id}"),
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
private fun renderClassificationRadioChoice(
    label: String,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
): Unit {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = classificationRadioMinimumHeight)
            .selectable(selected = isSelected, enabled = enabled, role = Role.RadioButton, onClick = onSelected)
            .padding(horizontal = classificationStandardSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isSelected, onClick = null, enabled = enabled)
        Text(label, modifier = Modifier.padding(start = classificationStandardSpacing))
    }
}
