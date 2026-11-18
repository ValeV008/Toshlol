# 04: Account management and monthly history

Status: resolved
Blocked by: 01

**What to build:** Review and manage Accounts in the shared visual style, with selected-month Closing balances and movement histories consistent with Overview and the Account drawer, while preserving signed Opening balances, renames, and archived read-only history.

## Parent-spec requirements

- [User stories](../spec.md#user-stories): Stories 1-6, 83-85: extend reference styling to Account destinations without a dedicated mockup, preserving accessibility and offline Android 10 use.
- [User stories](../spec.md#user-stories): Stories 11-12, 22, 24-28: shared month, consistent Closing balances, selected archived Accounts, signed values, and Opening/date boundaries.
- [User stories](../spec.md#user-stories): Stories 55, 75-76: keep archived history/read-only restrictions, resolve renamed references, and preserve existing finance records.
- [Implementation decisions](../spec.md#implementation-decisions): Consume ticket 01's public monthly projection and shared period rather than a separate all-time/current-balance formula. Main drawer selection scopes the three ledger tabs; Account management must still expose all Accounts for management and explicit history viewing.
- [Testing decisions](../spec.md#testing-decisions): use existing public finance use-case/repository and Backup interfaces, Compose semantics, and UI Automator for system UI; test visible outcomes rather than private helpers or tables. Named tests below are proposed or extensions of existing behavior tests, not claims of passing implementation.

## Observable acceptance criteria

- [x] The Accounts destination and Account creation/edit controls use the shared headers, cards, typography, money formatting, and accessible actions. Account type is selectable when creating an Account and retained while editing its name, signed Opening balance, and Opening date.
- [x] Account list/card/history balances agree with Overview/right-drawer values for the same Account and selected month. They support positive, zero, and negative values and exclude later movements and premature Opening balances.
- [x] Opening an Account history retains the selected month and shows that month's relevant Income, Expenses, and Transfers in newest-first deterministic order with clear counterpart Account names. History's Closing balance remains cumulative through month-end rather than just the monthly net.
- [x] Changing month from the available shared control refreshes Account balances/history consistently; navigating back does not silently return to the device's current month.
- [x] Create an Account, edit its signed Opening balance/date, and rename it through the public workflow. The persisted values and recalculated displays survive reopen, and renames resolve in already available historical views without rewriting movement identities.
- [x] Archive retains Account name, balance, and history and marks the Account read-only. The drawer can still select it, but new use and prohibited Transaction/Transfer edits remain unavailable.
- [x] Creating or managing an Account does not silently select it in the main drawer, and does not add it to any saved Budget Account scope. Preserve existing references and legacy finance data throughout. Account creation/edit/archive paths leave Overview selection and Budget records alone; Account-scoped Budgets are introduced by ticket 06.
- [x] Verified Account creation/edit/archive/history journeys and repository persistence/boundary behavior on Android 10, ran the relevant JVM/lint/format checks, and captured Account list/editor/history screenshots including negative balance and archived examples.

## Named behavior tests

| Test or journey | Observable result |
| --- | --- |
| `TransactionFlowTest.reviewEmptyAccountBeforeAndAfterArchiving` | Verify account types, formatted monthly balances, and explicit history navigation before and after archiving. |
| `TransactionFlowTest.createEditAndReopenAccountKeepsOverviewSelectionAndSignedOpeningValues` | Create a typed Account with a negative Opening balance, edit its name, balance, and date, then reopen the app Activity to verify persistence and unchanged Overview selection. |
| `TransactionFlowTest.accountManagementWrapsArchiveActionAndKeepsArchivedHistoryReadOnly` | Exercise long-name actions, archive, signed history activity, read-only state, and month retention back through Overview. |
| `TransactionFlowTest.createEditFilterAndDeleteTransferWithoutChangingIncomeOrExpenseTotals` | Create and edit Accounts, rename a counterpart in history, and verify signed Transfer history and unchanged ledger totals. |
| `AppDatabaseTest.saveOpeningBalanceAndDateRecalculatesPersistedBalance` | Verify pre-opening movements are excluded and persisted Opening values recalculate after reload. |
| `AppDatabaseTest.archiveAccountRetainsHistoryAndRejectsNewMovements` | Verify archival preserves records and rejects new or prohibited edits. |
| `OverviewAccountSelectionTest.selectArchivedAccountFromOverviewDrawer` | Verify archived Accounts remain selectable in Overview while movement actions stay unavailable. |
| `BackupImportUiTest.settingsLaunchesDocumentPickerForMergeAndCancellationReturnsToSettings` | Keep imported transfer history within the selected month and navigate back through Accounts to Overview. |

## Delivery boundaries

Keep all existing Account types and signed Opening balance semantics. Account history is an explicit Account view, not an implicit rewrite of the shared main-ledger Account selection. No bank integration, seeded Accounts, or new deletion policy.

Each slice must remain independently demoable and keep existing callers and relevant checks working. Include the feature's behavior and UI verification in this ticket rather than deferring them to the final integration ticket. Follow the parent spec's exclusions and existing ADRs.

## Answer

Implemented account management and selected-month Account history using the shared monthly projection. Account cards show month-end balances; creation offers accessible Account type selection, while editing preserves type and supports name, signed Opening balance, and Opening date changes. History shows signed Income, Expenses, and Transfers with current counterpart names, deterministic newest-first ordering, and a cumulative closing balance. Archived Accounts retain read-only history, including transfers to or from an archived counterpart. Account actions now wrap on narrow screens and long names so Archive remains reachable.

Verified the Account create/edit/rename/archive flows, persisted values after reopening the Activity, Opening-date boundaries, unchanged Overview selection, archived drawer selection, and month retention on Android 10 (API 29). The Transfer and Backup UI journeys were updated to expect signed history labels and current-month data. The Backup journey closes the Documents roots drawer before selecting its file, confirms return to the app, and checks the Account-history Back flow.

Screenshots: [negative Account list](../evidence/04/account-list-negative-balance.png), [signed Opening editor](../evidence/04/account-editor-negative.png), [Cash history](../evidence/04/account-history-cash.png), [history activity](../evidence/04/account-history-cash-activity.png), [Opening-date boundary](../evidence/04/account-history-pre-opening-boundary.png), [archived Accounts](../evidence/04/account-list-archived.png), and [archived read-only history](../evidence/04/account-history-archived.png).

Checks passed: `:app:testDebugUnitTest`, `:app:lintDebug`, and `:app:ktlintCheck`; the full `:app:connectedDebugAndroidTest` run passed on API 29 (60 tests, 0 failures, 0 errors, 0 skipped).
