package com.toshlol.personalfinance

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.toshlol.personalfinance.data.AppDatabase
import com.toshlol.personalfinance.data.BackupImportMode
import com.toshlol.personalfinance.data.BackupImportOutcome
import com.toshlol.personalfinance.data.ContentResolverBackupReader
import com.toshlol.personalfinance.data.ContentResolverBackupWriter
import com.toshlol.personalfinance.data.RoomBackupExporter
import com.toshlol.personalfinance.data.RoomBackupImporter
import com.toshlol.personalfinance.data.RoomLocalDataResetter
import com.toshlol.personalfinance.ui.AccountEditorActions
import com.toshlol.personalfinance.ui.AccountHistoryActions
import com.toshlol.personalfinance.ui.AccountHistoryScreenActions
import com.toshlol.personalfinance.ui.BudgetActions
import com.toshlol.personalfinance.ui.CategoryActions
import com.toshlol.personalfinance.ui.LedgerNavigationActions
import com.toshlol.personalfinance.ui.OrganizerActions
import com.toshlol.personalfinance.ui.PersonalFinanceActions
import com.toshlol.personalfinance.ui.PersonalFinanceApp
import com.toshlol.personalfinance.ui.SetupActions
import com.toshlol.personalfinance.ui.TagActions
import com.toshlol.personalfinance.ui.TransactionActions
import com.toshlol.personalfinance.ui.TransactionDeleteActions
import com.toshlol.personalfinance.ui.TransactionFormActions
import com.toshlol.personalfinance.ui.TransferActions
import com.toshlol.personalfinance.ui.TransferFormActions
import com.toshlol.personalfinance.ui.theme.ToshlolTheme
import com.toshlol.personalfinance.ui.viewmodel.FirstRunUiState
import com.toshlol.personalfinance.ui.viewmodel.FirstRunViewModel
import com.toshlol.personalfinance.ui.viewmodel.TransactionsViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val backupExporter: RoomBackupExporter by lazy {
        RoomBackupExporter(AppDatabase.getInstance(this), ContentResolverBackupWriter(contentResolver))
    }
    private val backupImporter: RoomBackupImporter by lazy { RoomBackupImporter(AppDatabase.getInstance(this)) }
    private val localDataResetter: RoomLocalDataResetter by lazy { RoomLocalDataResetter(AppDatabase.getInstance(this)) }
    private val viewModel: FirstRunViewModel by viewModels {
        viewModelFactory {
            initializer { FirstRunViewModel((application as PersonalFinanceApplication).firstRunSetup) }
        }
    }
    private val transactionsViewModel: TransactionsViewModel by viewModels {
        viewModelFactory {
            initializer {
                val application: PersonalFinanceApplication = application as PersonalFinanceApplication
                TransactionsViewModel(
                    application.transactionRepository,
                    application.firstRunSetup,
                    application.accountManagement,
                    application.categoryTagRepository,
                    application.budgetRepository,
                    application.transferManagement,
                    application.appPreferences,
                )
            }
        }
    }

    @Suppress("ktlint:standard:no-unit-return")
    override fun onCreate(savedInstanceState: Bundle?): Unit {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            var backupStatus: String by rememberSaveable { mutableStateOf("") }
            var importMode: BackupImportMode by rememberSaveable { mutableStateOf(BackupImportMode.MERGE) }
            val exportBackup: ManagedActivityResultLauncher<String, Uri?> =
                rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { destination: Uri? ->
                    if (destination == null) {
                        backupStatus = "Backup export canceled."
                    } else {
                        lifecycleScope.launch {
                            backupStatus =
                                try {
                                    backupExporter.exportTo(destination)
                                    "Backup exported successfully."
                                } catch (exception: Exception) {
                                    "Backup export failed: ${exception.message ?: "Unable to write the selected file."}"
                                }
                        }
                    }
                }
            val importBackup: ManagedActivityResultLauncher<Array<String>, Uri?> =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { source: Uri? ->
                    if (source == null) {
                        backupStatus = "Backup import canceled."
                    } else {
                        lifecycleScope.launch {
                            backupStatus =
                                try {
                                    val contents: ByteArray = ContentResolverBackupReader(contentResolver).read(source)
                                    when (val outcome = backupImporter.import(contents, importMode)) {
                                        is BackupImportOutcome.Success -> {
                                            viewModel.refresh()
                                            transactionsViewModel.refresh()
                                            val totals: String =
                                                outcome.report.counts.entries.joinToString { (entity, count) ->
                                                    "${entity.name.lowercase()}: ${count.inserted} added, ${count.updated} updated, ${count.unchanged} unchanged"
                                                }
                                            "Backup ${importMode.name.lowercase()} completed successfully. $totals"
                                        }
                                        is BackupImportOutcome.Failure -> "Backup import failed: ${outcome.failures.joinToString {
                                            it.message
                                        }}"
                                    }
                                } catch (_: Exception) {
                                    "Backup import failed. The selected file could not be read."
                                }
                        }
                    }
                }
            val importSelectedBackup: (BackupImportMode) -> Unit = { mode: BackupImportMode ->
                importMode = mode
                importBackup.launch(arrayOf("application/json", "text/json", "application/octet-stream"))
            }
            val resetLocalData: () -> Unit = {
                lifecycleScope.launch {
                    backupStatus =
                        try {
                            localDataResetter.reset()
                            viewModel.refresh()
                            transactionsViewModel.refresh()
                            "All local data was deleted. Set up a new account to continue."
                        } catch (_: Exception) {
                            "Local data could not be deleted. Saved data was not changed."
                        }
                }
            }
            val uiState: FirstRunUiState by viewModel.uiState.collectAsStateWithLifecycle()
            val transactionsState by transactionsViewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(uiState.isSetupComplete) { if (uiState.isSetupComplete) transactionsViewModel.refresh() }
            ToshlolTheme {
                PersonalFinanceApp(
                    uiState = uiState,
                    transactionsState = transactionsState,
                    actions =
                        PersonalFinanceActions(
                            setup =
                                SetupActions(
                                    currencyChanged = viewModel::updateCurrency,
                                    accountNameChanged = viewModel::updateAccountName,
                                    accountTypeChanged = viewModel::updateAccountType,
                                    submit = viewModel::submit,
                                    retry = viewModel::retryLoad,
                                ),
                            transactions =
                                TransactionActions(
                                    create = transactionsViewModel::beginCreate,
                                    edit = transactionsViewModel::beginEdit,
                                    delete = transactionsViewModel::requestDelete,
                                    form =
                                        TransactionFormActions(
                                            close = transactionsViewModel::closeForm,
                                            amountChanged = transactionsViewModel::updateAmount,
                                            dateChanged = transactionsViewModel::updateDate,
                                            noteChanged = transactionsViewModel::updateNote,
                                            directionChanged = transactionsViewModel::updateDirection,
                                            save = transactionsViewModel::save,
                                            categoryChanged = transactionsViewModel::updateCategory,
                                            tagToggled = transactionsViewModel::toggleTag,
                                            accountChanged = transactionsViewModel::updateTransactionAccount,
                                        ),
                                    deleteConfirmation =
                                        TransactionDeleteActions(
                                            cancelDelete = transactionsViewModel::cancelDelete,
                                            confirmDelete = transactionsViewModel::confirmDelete,
                                        ),
                                ),
                            transfers =
                                TransferActions(
                                    create = transactionsViewModel::beginTransfer,
                                    edit = transactionsViewModel::beginEditTransfer,
                                    form =
                                        TransferFormActions(
                                            close = transactionsViewModel::closeTransferForm,
                                            save = transactionsViewModel::saveTransfer,
                                            amountChanged = transactionsViewModel::updateAmount,
                                            dateChanged = transactionsViewModel::updateDate,
                                            sourceChanged = transactionsViewModel::selectTransferSource,
                                            destinationChanged = transactionsViewModel::selectTransferDestination,
                                            delete = transactionsViewModel::requestDeleteTransfer,
                                            cancelDelete = transactionsViewModel::cancelDeleteTransfer,
                                            confirmDelete = transactionsViewModel::confirmDeleteTransfer,
                                        ),
                                ),
                            organizer =
                                OrganizerActions(
                                    open = transactionsViewModel::showOrganizer,
                                    close = transactionsViewModel::closeOrganizer,
                                    categories =
                                        CategoryActions(
                                            nameChanged = transactionsViewModel::updateCategoryName,
                                            directionChanged = transactionsViewModel::updateCategoryDirection,
                                            save = transactionsViewModel::saveCategory,
                                            edit = transactionsViewModel::beginCategoryEdit,
                                            archive = transactionsViewModel::archiveCategory,
                                        ),
                                    tags =
                                        TagActions(
                                            nameChanged = transactionsViewModel::updateTagName,
                                            categoryChanged = transactionsViewModel::updateTagCategory,
                                            save = transactionsViewModel::saveTag,
                                            edit = transactionsViewModel::beginTagEdit,
                                            archive = transactionsViewModel::archiveTag,
                                        ),
                                    accounts =
                                        AccountEditorActions(
                                            nameChanged = transactionsViewModel::updateAccountName,
                                            select = transactionsViewModel::selectAccountForEditing,
                                            openEditor = transactionsViewModel::showAccountEditor,
                                            beginCreate = transactionsViewModel::beginCreateAccount,
                                            closeEditor = transactionsViewModel::closeAccountEditor,
                                            save = transactionsViewModel::saveAccountDetails,
                                            openingBalanceChanged = transactionsViewModel::updateOpeningBalanceAmount,
                                            openingDateChanged = transactionsViewModel::updateOpeningBalanceDate,
                                            newNameChanged = transactionsViewModel::updateNewAccountName,
                                            newTypeChanged = transactionsViewModel::updateNewAccountType,
                                            newOpeningBalanceChanged = transactionsViewModel::updateNewAccountOpeningBalance,
                                            newOpeningDateChanged = transactionsViewModel::updateNewAccountOpeningDate,
                                            create = transactionsViewModel::createAccount,
                                            archive = transactionsViewModel::archiveAccount,
                                        ),
                                ),
                            budgets =
                                BudgetActions(
                                    open = transactionsViewModel::showBudgets,
                                    close = transactionsViewModel::closeBudgets,
                                    monthChanged = transactionsViewModel::updateBudgetMonth,
                                    categoryChanged = transactionsViewModel::updateBudgetCategory,
                                    amountChanged = transactionsViewModel::updateBudgetAmount,
                                    save = transactionsViewModel::saveBudget,
                                ),
                            accountHistory =
                                AccountHistoryActions(
                                    open = transactionsViewModel::openAccountHistory,
                                    screen =
                                        AccountHistoryScreenActions(
                                            back = transactionsViewModel::closeAccountHistory,
                                            monthChanged = transactionsViewModel::updateSelectedMonth,
                                            editTransaction = transactionsViewModel::beginEdit,
                                            deleteTransaction = transactionsViewModel::requestDelete,
                                            editTransfer = transactionsViewModel::beginEditTransfer,
                                        ),
                                ),
                            navigation =
                                LedgerNavigationActions(
                                    selectArea = transactionsViewModel::selectArea,
                                    monthChanged = transactionsViewModel::updateSelectedMonth,
                                    accountChanged = transactionsViewModel::updateSelectedAccount,
                                    categoryChanged = transactionsViewModel::updateSelectedCategory,
                                    tagChanged = transactionsViewModel::updateSelectedTag,
                                    overviewAccountToggled = transactionsViewModel::toggleOverviewAccount,
                                    accountDrawerChanged = transactionsViewModel::setAccountDrawerOpen,
                                ),
                            exportBackup = { exportBackup.launch("Toshlol-Backup.json") },
                            importBackup = importSelectedBackup,
                            resetData = resetLocalData,
                            backupStatus = backupStatus,
                        ),
                )
            }
        }
    }
}
