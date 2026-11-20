package com.toshlol.personalfinance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.toshlol.personalfinance.data.AccountType
import com.toshlol.personalfinance.data.BackupImportMode
import com.toshlol.personalfinance.data.FinanceCategory
import com.toshlol.personalfinance.data.FinanceTag
import com.toshlol.personalfinance.data.FinanceTransaction
import com.toshlol.personalfinance.data.FinanceTransfer
import com.toshlol.personalfinance.data.RecurringBudgetProgress
import com.toshlol.personalfinance.data.TransactionDirection
import com.toshlol.personalfinance.ui.viewmodel.FirstRunUiState
import com.toshlol.personalfinance.ui.viewmodel.TransactionsUiState
import kotlinx.coroutines.launch

private const val LARGE_FONT_SCALE: Float = 1.5f

data class LedgerNavigationActions(
    val selectArea: (String) -> Unit = {},
    val monthChanged: (String) -> Unit = {},
    val accountChanged: (String?) -> Unit = {},
    val categoryChanged: (String?) -> Unit = {},
    val tagChanged: (String?) -> Unit = {},
    val overviewAccountToggled: (String) -> Unit = {},
    val accountDrawerChanged: (Boolean) -> Unit = {},
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
    val form: TransferFormActions,
)

data class CategoryActions(
    val nameChanged: (String) -> Unit,
    val directionChanged: (TransactionDirection) -> Unit,
    val save: () -> Unit,
    val edit: (FinanceCategory) -> Unit,
    val archive: (String) -> Unit,
    val cancelEditing: () -> Unit,
)

data class TagActions(
    val nameChanged: (String) -> Unit,
    val categoryChanged: (String?) -> Unit,
    val save: () -> Unit,
    val edit: (FinanceTag) -> Unit,
    val archive: (String) -> Unit,
    val cancelEditing: () -> Unit,
)

data class AccountEditorActions(
    val nameChanged: (String) -> Unit,
    val select: (String) -> Unit,
    val openEditor: (String) -> Unit,
    val beginCreate: () -> Unit,
    val closeEditor: () -> Unit,
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

data class BudgetScopeActions(
    val accountToggled: (String) -> Unit,
    val categoryToggled: (String) -> Unit,
    val tagToggled: (String) -> Unit,
)

data class BudgetActions(
    val open: () -> Unit,
    val close: () -> Unit,
    val monthChanged: (String) -> Unit,
    val nameChanged: (String) -> Unit,
    val amountChanged: (String) -> Unit,
    val scope: BudgetScopeActions,
    val create: () -> Unit,
    val edit: (RecurringBudgetProgress) -> Unit,
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
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = uiState.isSetupComplete,
        drawerContent = {
            ModalDrawerSheet {
                if (uiState.isSetupComplete) {
                    Text("Navigate", Modifier.padding(20.dp), style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                    listOf(
                        "Overview",
                        "Expenses",
                        "Income",
                        "Accounts",
                        "Categories & Tags",
                        "Budgets",
                        "Export",
                        "Settings",
                    ).forEach { destination ->
                        TextButton(
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                when (destination) {
                                    "Categories & Tags" -> actions.organizer.open()
                                    "Export" -> actions.exportBackup()
                                    else -> actions.navigation.selectArea(destination)
                                }
                            },
                            modifier =
                                Modifier.fillMaxWidth().testTag("drawer-$destination").semantics {
                                    stateDescription = if (transactionsState.selectedArea == destination) "Current section" else "Section"
                                },
                        ) { Text(destination, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) }
                    }
                }
            }
        },
    ) {
        Surface(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (uiState.isSetupComplete &&
                    transactionsState.isLoaded &&
                    transactionsState.accountHistoryAccountId == null &&
                    !transactionsState.isOrganizerVisible
                ) {
                    ShowPersonalFinanceTopNavigation(
                        transactionsState.selectedArea,
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                        onAreaSelected = actions.navigation.selectArea,
                    )
                }
                androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                    when {
                        !uiState.isLoaded ->
                            ShowLoading(
                                hasFailed = uiState.hasLoadFailed,
                                onRetry = actions.setup.retry,
                                loadingMessage = "Loading your setup…",
                                failureMessage = "Could not load your setup. Your saved data has not been changed.",
                            )
                        uiState.isSetupComplete && !transactionsState.isLoaded -> ShowLoading(false, {})
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
                                state = transactionsState.copy(currency = uiState.currency),
                                actions = actions.budgets,
                                onAddTransaction = { actions.transactions.create(TransactionDirection.EXPENSE) },
                            )
                        uiState.isSetupComplete && transactionsState.selectedArea == "Accounts" ->
                            AccountsScreen(
                                state = transactionsState.copy(currency = uiState.currency),
                                actions =
                                    AccountsScreenActions(
                                        back = { actions.navigation.selectArea("Overview") },
                                        monthChanged = actions.navigation.monthChanged,
                                        openHistory = actions.accountHistory.open,
                                        edit = actions.organizer.accounts.openEditor,
                                        archive = actions.organizer.accounts.archive,
                                        addAccount = actions.organizer.accounts.beginCreate,
                                        closeEditor = actions.organizer.accounts.closeEditor,
                                        accountEditor = actions.organizer.accounts,
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
                                    deleteTransfer = actions.transfers.form.delete,
                                ),
                            )
                        uiState.isSetupComplete && transactionsState.selectedArea == "Expenses" ->
                            MonthlyLedgerScreen(
                                transactionsState.copy(currency = uiState.currency),
                                TransactionDirection.EXPENSE,
                                MonthlyLedgerActions(
                                    actions.navigation.monthChanged,
                                    actions.navigation.categoryChanged,
                                    actions.navigation.tagChanged,
                                    actions.transactions.create,
                                    actions.transactions.edit,
                                    actions.transactions.delete,
                                    actions.transfers.edit,
                                    actions.transfers.form.delete,
                                    actions.transfers.create,
                                    actions.navigation.overviewAccountToggled,
                                    actions.navigation.accountDrawerChanged,
                                ),
                            )
                        uiState.isSetupComplete && transactionsState.selectedArea == "Income" ->
                            MonthlyLedgerScreen(
                                transactionsState.copy(currency = uiState.currency),
                                TransactionDirection.INCOME,
                                MonthlyLedgerActions(
                                    actions.navigation.monthChanged,
                                    actions.navigation.categoryChanged,
                                    actions.navigation.tagChanged,
                                    actions.transactions.create,
                                    actions.transactions.edit,
                                    actions.transactions.delete,
                                    actions.transfers.edit,
                                    actions.transfers.form.delete,
                                    actions.transfers.create,
                                    actions.navigation.overviewAccountToggled,
                                    actions.navigation.accountDrawerChanged,
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
                                        monthChanged = actions.navigation.monthChanged,
                                        overviewAccountToggled = actions.navigation.overviewAccountToggled,
                                        accountDrawerChanged = actions.navigation.accountDrawerChanged,
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
                                        accountChanged = actions.transactions.form.accountChanged,
                                    ),
                                delete =
                                    TransactionDeleteActions(
                                        actions.transactions.deleteConfirmation.cancelDelete,
                                        actions.transactions.deleteConfirmation.confirmDelete,
                                    ),
                            ),
                        )
                        TransferEntryDialogs(
                            transactionsState,
                            actions.transfers.form,
                        )
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
internal fun ShowPersonalFinanceTopNavigation(
    selectedArea: String,
    onOpenDrawer: () -> Unit,
    onAreaSelected: (String) -> Unit,
): Unit {
    val useLargeFontNavigation: Boolean = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        TextButton(onClick = onOpenDrawer, modifier = Modifier.heightIn(min = 48.dp).testTag("open-navigation-drawer")) {
            Text("Menu")
        }
        if (selectedArea in setOf("Overview", "Expenses", "Income")) {
            if (useLargeFontNavigation) {
                Text(
                    selectedArea,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .semantics {
                                stateDescription = "Current tab"
                            }.testTag("current-area"),
                )
            } else {
                listOf("Overview", "Expenses", "Income").forEach { area ->
                    TextButton(
                        onClick = { onAreaSelected(area) },
                        modifier =
                            Modifier.weight(1f).heightIn(min = 48.dp).testTag("tab-$area").semantics {
                                stateDescription = if (selectedArea == area) "Current tab" else "Tab"
                            },
                    ) { Text(area, maxLines = 1, softWrap = false) }
                }
            }
        }
    }
}

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
internal fun ShowLoading(
    hasFailed: Boolean,
    onRetry: () -> Unit,
    loadingMessage: String = "Loading your ledger…",
    failureMessage: String = "Could not load your ledger. Your saved data has not been changed.",
): Unit {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (hasFailed) {
                    Text(
                        failureMessage,
                        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Try again") }
                } else {
                    CircularProgressIndicator()
                    Text(
                        loadingMessage,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}
