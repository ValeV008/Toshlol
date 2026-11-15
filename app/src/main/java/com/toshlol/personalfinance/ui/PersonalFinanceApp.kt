package com.toshlol.personalfinance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.data.BackupImportMode
import com.toshlol.personalfinance.data.FinanceCategory
import com.toshlol.personalfinance.data.FinanceTag
import com.toshlol.personalfinance.data.FinanceTransaction
import com.toshlol.personalfinance.data.FinanceTransfer
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.ui.viewmodel.FirstRunUiState
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState

data class LedgerNavigationActions(
    val selectArea: (String) -> Unit = {},
    val monthChanged: (String) -> Unit = {},
    val accountChanged: (String?) -> Unit = {},
    val categoryChanged: (String?) -> Unit = {},
    val tagChanged: (String?) -> Unit = {},
)

data class SetupActions(
    val currencyChanged: (String) -> Unit,
    val accountNameChanged: (String) -> Unit,
    val accountTypeChanged: (AccountType) -> Unit,
    val submit: () -> Unit,
    val retry: () -> Unit,
)

data class TransactionActions(
    val create: (TransactionDirection) -> Unit,
    val edit: (FinanceTransaction) -> Unit,
    val delete: (String) -> Unit,
    val form: TransactionFormActions,
    val deleteConfirmation: TransactionDeleteActions,
)

data class TransferActions(
    val create: () -> Unit,
    val edit: (FinanceTransfer) -> Unit,
    val dialog: TransferDialogActions,
)

data class CategoryActions(
    val nameChanged: (String) -> Unit,
    val directionChanged: (TransactionDirection) -> Unit,
    val save: () -> Unit,
    val edit: (FinanceCategory) -> Unit,
    val archive: (String) -> Unit,
)

data class TagActions(
    val nameChanged: (String) -> Unit,
    val categoryChanged: (String?) -> Unit,
    val save: () -> Unit,
    val edit: (FinanceTag) -> Unit,
    val archive: (String) -> Unit,
)

data class AccountEditorActions(
    val nameChanged: (String) -> Unit,
    val select: (String) -> Unit,
    val save: () -> Unit,
    val openingBalanceChanged: (String) -> Unit,
    val openingDateChanged: (String) -> Unit,
    val newNameChanged: (String) -> Unit,
    val newTypeChanged: (AccountType) -> Unit,
    val newOpeningBalanceChanged: (String) -> Unit,
    val newOpeningDateChanged: (String) -> Unit,
    val create: () -> Unit,
    val archive: (String) -> Unit,
)

data class OrganizerActions(
    val open: () -> Unit,
    val close: () -> Unit,
    val categories: CategoryActions,
    val tags: TagActions,
    val accounts: AccountEditorActions,
)

data class BudgetActions(
    val open: () -> Unit,
    val close: () -> Unit,
    val monthChanged: (String) -> Unit,
    val categoryChanged: (String?) -> Unit,
    val amountChanged: (String) -> Unit,
    val save: () -> Unit,
)

data class AccountHistoryActions(
    val open: (String) -> Unit,
    val screen: AccountHistoryScreenActions,
)

data class PersonalFinanceActions(
    val setup: SetupActions,
    val transactions: TransactionActions,
    val transfers: TransferActions,
    val organizer: OrganizerActions,
    val budgets: BudgetActions,
    val accountHistory: AccountHistoryActions,
    val navigation: LedgerNavigationActions,
    val exportBackup: () -> Unit,
    val importBackup: (BackupImportMode) -> Unit,
    val resetData: () -> Unit,
    val backupStatus: String,
)

@Composable
@Suppress("ktlint:standard:no-unit-return")
fun PersonalFinanceApp(
    uiState: FirstRunUiState,
    transactionsState: TransactionsUiState,
    actions: PersonalFinanceActions,
): Unit {
    Surface(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                when {
                    !uiState.isLoaded -> ShowLoading(uiState.hasLoadFailed, actions.setup.retry)
                    uiState.isSetupComplete && !transactionsState.isLoaded -> ShowLoading(false) {}
                    uiState.isSetupComplete && transactionsState.accountHistoryAccountId != null ->
                        AccountHistoryScreen(
                            state = transactionsState.copy(currency = uiState.currency),
                            actions = actions.accountHistory.screen,
                        )
                    uiState.isSetupComplete && transactionsState.isOrganizerVisible ->
                        CategoryTagScreen(
                            state = transactionsState,
                            actions = actions.organizer,
                        )
                    uiState.isSetupComplete && transactionsState.selectedArea == "Budgets" ->
                        BudgetsScreen(
                            transactionsState.copy(currency = uiState.currency),
                            actions.budgets.close,
                            actions.budgets.monthChanged,
                            actions.budgets.categoryChanged,
                            actions.budgets.amountChanged,
                            actions.budgets.save,
                            { actions.transactions.create(TransactionDirection.EXPENSE) },
                            actions.transfers.create,
                        )
                    uiState.isSetupComplete && transactionsState.selectedArea == "Accounts" ->
                        AccountsScreen(
                            state = transactionsState.copy(currency = uiState.currency),
                            actions =
                                AccountsScreenActions(
                                    back = { actions.navigation.selectArea("Overview") },
                                    openHistory = actions.accountHistory.open,
                                    edit = { id ->
                                        actions.organizer.accounts.select(id)
                                        actions.organizer.open()
                                    },
                                    archive = actions.organizer.accounts.archive,
                                    addAccount = {
                                        actions.organizer.accounts.select(transactionsState.editingAccountId ?: transactionsState.accountId)
                                        actions.organizer.open()
                                    },
                                ),
                        )
                    uiState.isSetupComplete && transactionsState.selectedArea == "Transactions" ->
                        TransactionsScreen(
                            transactionsState.copy(currency = uiState.currency),
                            TransactionsScreenActions(
                                create = actions.transactions.create,
                                edit = actions.transactions.edit,
                                delete = actions.transactions.delete,
                                monthChanged = actions.navigation.monthChanged,
                                accountChanged = actions.navigation.accountChanged,
                                categoryChanged = actions.navigation.categoryChanged,
                                tagChanged = actions.navigation.tagChanged,
                                createTransfer = actions.transfers.create,
                                editTransfer = actions.transfers.edit,
                                deleteTransfer = actions.transfers.dialog.delete,
                            ),
                        )
                    uiState.isSetupComplete && transactionsState.selectedArea == "Settings" ->
                        SettingsScreen(
                            state = transactionsState,
                            actions =
                                SettingsActions(
                                    openOrganizer = actions.organizer.open,
                                    createTransaction = { actions.transactions.create(TransactionDirection.EXPENSE) },
                                    createTransfer = actions.transfers.create,
                                    exportBackup = actions.exportBackup,
                                    importBackup = actions.importBackup,
                                    resetData = actions.resetData,
                                    backupStatus = actions.backupStatus,
                                ),
                        )
                    uiState.isSetupComplete ->
                        OverviewScreen(
                            state = transactionsState.copy(currency = uiState.currency),
                            actions =
                                OverviewScreenActions(
                                    create = actions.transactions.create,
                                    edit = actions.transactions.edit,
                                    delete = actions.transactions.delete,
                                    openOrganizer = actions.organizer.open,
                                    openBudgets = actions.budgets.open,
                                    createTransfer = actions.transfers.create,
                                    editTransfer = actions.transfers.edit,
                                    openAccountHistory = actions.accountHistory.open,
                                ),
                        )
                    else ->
                        SetupScreen(
                            uiState,
                            actions.setup.currencyChanged,
                            actions.setup.accountNameChanged,
                            actions.setup.accountTypeChanged,
                            actions.setup.submit,
                        )
                }
                if (uiState.isSetupComplete && transactionsState.isLoaded) {
                    TransactionDialogs(
                        transactionsState,
                        TransactionDialogActions(
                            form =
                                TransactionFormActions(
                                    close = actions.transactions.form.close,
                                    amountChanged = actions.transactions.form.amountChanged,
                                    dateChanged = actions.transactions.form.dateChanged,
                                    noteChanged = actions.transactions.form.noteChanged,
                                    directionChanged = actions.transactions.form.directionChanged,
                                    save = actions.transactions.form.save,
                                    categoryChanged = actions.transactions.form.categoryChanged,
                                    tagToggled = actions.transactions.form.tagToggled,
                                ),
                            delete =
                                TransactionDeleteActions(
                                    actions.transactions.deleteConfirmation.cancelDelete,
                                    actions.transactions.deleteConfirmation.confirmDelete,
                                ),
                        ),
                    )
                    TransferDialog(
                        transactionsState,
                        actions.transfers.dialog,
                    )
                }
            }
            if (uiState.isSetupComplete &&
                transactionsState.isLoaded &&
                !transactionsState.isOrganizerVisible &&
                transactionsState.accountHistoryAccountId == null
            ) {
                NavigationBar {
                    Row(Modifier.fillMaxWidth()) {
                        listOf("Overview", "Transactions", "Accounts", "Budgets", "Settings").forEach { area: String ->
                            Button(
                                onClick = { actions.navigation.selectArea(area) },
                                modifier =
                                    Modifier.weight(1f).testTag("navigation-$area").semantics {
                                        stateDescription = if (transactionsState.selectedArea == area) "Current section" else "Section"
                                    },
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                            ) {
                                Text(area, maxLines = 1, softWrap = false, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class SettingsActions(
    val openOrganizer: () -> Unit,
    val createTransaction: () -> Unit,
    val createTransfer: () -> Unit,
    val exportBackup: () -> Unit,
    val importBackup: (BackupImportMode) -> Unit,
    val resetData: () -> Unit,
    val backupStatus: String,
)

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun SettingsScreen(
    state: TransactionsUiState,
    actions: SettingsActions,
): Unit {
    var confirmation: String by rememberSaveable { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Text("Currency: ${state.currency}")
        Text("Account: ${state.accountName}")
        Button(onClick = actions.createTransaction) { Text("Add transaction") }
        Button(onClick = actions.createTransfer) { Text("Transfer") }
        Text("Data management", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Button(onClick = actions.openOrganizer) { Text("Manage categories and tags") }
        Button(onClick = actions.exportBackup) { Text("Export Backup") }
        Button(onClick = { actions.importBackup(BackupImportMode.MERGE) }) { Text("Import Backup (Merge)") }
        Button(onClick = { confirmation = "overwrite" }) { Text("Import Backup (Overwrite)") }
        Button(onClick = { confirmation = "reset" }) { Text("Delete all local data") }
        if (actions.backupStatus.isNotEmpty()) Text(actions.backupStatus)
    }
    if (confirmation.isNotEmpty()) {
        val isOverwrite: Boolean = confirmation == "overwrite"
        AlertDialog(
            onDismissRequest = { confirmation = "" },
            title = { Text(if (isOverwrite) "Overwrite local data?" else "Delete all local data?") },
            text = {
                Text(
                    if (isOverwrite) {
                        "The selected Backup will replace all local data, including currency. This cannot be undone."
                    } else {
                        "All accounts, transactions, transfers, budgets, categories, tags, and settings will be deleted. This cannot be undone."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmation = ""
                        if (isOverwrite) actions.importBackup(BackupImportMode.OVERWRITE) else actions.resetData()
                    },
                ) { Text(if (isOverwrite) "Choose Backup" else "Delete all data") }
            },
            dismissButton = { TextButton(onClick = { confirmation = "" }) { Text("Cancel") } },
        )
    }
}

@Composable
@Suppress("ktlint:standard:no-unit-return")
private fun ShowLoading(
    hasFailed: Boolean,
    onRetry: () -> Unit,
): Unit {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (hasFailed) {
            Text("Could not load your ledger. Your saved data has not been changed.")
            Button(onClick = onRetry) { Text("Try again") }
        } else {
            CircularProgressIndicator()
            Text("Loading your ledger…")
        }
    }
}
