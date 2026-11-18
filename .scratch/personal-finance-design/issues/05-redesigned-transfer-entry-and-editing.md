# 05: Redesigned Transfer entry and editing

Status: resolved
Blocked by: 02, 03

**What to build:** Create, edit, and delete Transfers in a full-screen flow that shares the entry visual language and appropriate amount/date controls, while keeping explicit source/destination selection and paired list/history updates correct.

## Parent-spec requirements

- [User stories](../spec.md#user-stories): Stories 1-6, 41, 83-85: extend the reference entry design and labeled quick-add into a usable, accessible Android 10 Transfer journey.
- [User stories](../spec.md#user-stories): Stories 37-40: preserve one movement behind paired directional rows, list filtering, and exclusion from earnings/spending totals.
- [User stories](../spec.md#user-stories): Stories 52-55: retain confirmation/validation, distinct explicitly selected Accounts, no classification/Note, and archived read-only restrictions.
- [Implementation decisions](../spec.md#implementation-decisions): Reuse ticket 03's entry presentation controls and ticket 02's directional navigation without representing a Transfer as Income plus Expense. Both Accounts must be deliberately selected for creation; do not derive them from the drawer, most-recently-used Transaction Account, or Budget selection.
- [Testing decisions](../spec.md#testing-decisions): use existing public finance use-case/repository and Backup interfaces, Compose semantics, and UI Automator for system UI; test visible outcomes rather than private helpers or tables. Named tests below are proposed or extensions of existing behavior tests, not claims of passing implementation.

## Observable acceptance criteria

- [x] Transfer money from each main ledger tab opens a visually consistent amount/date/source/destination flow with accessible save/cancel actions and no Category, Tag, or Note controls.
- [x] Each new Transfer requires explicit source and destination choices, both active and different. Positive precise amount and valid local date are required; today is the default date. An allowed overdraft is not blocked by an invented available-funds rule.
- [x] Saving persists one Transfer with stable identity and updates source/destination Closing balances and selected-month activity. The incoming and outgoing rows open the same movement; Overview contains one item and affected Account histories show the correct direction/counterpart.
- [x] Editing loads the saved movement, preserves identity/creation time, and updates both directional presentations and balances. Source/destination/month changes remove stale rows and place the movement in the correct current views.
- [x] Transfers remain excluded from Income, Expense, net, daily earnings/spending subtotals, and Budget actual spending. Source-only/destination-only/both/neither selection and Category/Tag list filters retain the rules delivered in ticket 02.
- [x] Cancellation and invalid/same-Account input do not mutate the ledger. Existing confirmed Transfer deletion updates both sides together; canceling confirmation preserves both sides.
- [x] Archived Accounts retain readable Transfer history and existing restrictions prevent prohibited editing. Presentation reuse does not introduce Transaction-only Tags, Categories, or Note semantics.
- [x] Run public Transfer/balance/total behavior tests and Android 10 create/edit/delete/filter journeys with lint and formatting; capture normal, validation, and larger-font Transfer screenshots.

## Named behavior tests

| Test or journey | Observable result |
| --- | --- |
| recordAndEditTransferThroughFullScreenEntry | Create, edit amount/Accounts/date, and reopen each paired row to assert one identity and consistent balances/history. |
| requireExplicitDifferentTransferAccounts | Assert new forms require both choices, reject same/missing Accounts and invalid amount/date, and allow an otherwise valid overdraft. |
| presentTransfersWithoutInflatingTotals | Re-run directional selection/filter cases with new entry-created data and verify unchanged Income/Expense/net/Budget totals. |
| confirmTransferDeletionUpdatesBothSides | Assert cancel preserves and confirm removes the one movement from both lists/histories while recalculating balances. |
| keepArchivedTransferHistoryReadOnly | Verify archived names/history remain intelligible and prohibited edits cannot be invoked. |
| operateTransferEntryAtLargeFont | Verify labels, Account choice states, amount/date controls, validation, and save/cancel remain usable. |

## Delivery boundaries

Block on ticket 02 for real paired directional views and ticket 03 for reusable full-screen entry controls. Do not add Transfer Category, Tags, Note, Income/Expense duplicates, insufficient-funds enforcement, or automatic Account preselection.

Each slice must remain independently demoable and keep existing callers and relevant checks working. Include the feature's behavior and UI verification in this ticket rather than deferring them to the final integration ticket. Follow the parent spec's exclusions and existing ADRs.

## Answer

Replaced the Transfer alert editor with a full-screen charcoal entry and editing flow that reuses the transaction amount keypad and date field. Source and destination Accounts remain unselected on new entries and are available only when active. The flow has labeled Save and Cancel actions, hides the keyboard on close, and keeps deletion behind confirmation. Transfer-specific validation rejects non-positive amounts, invalid dates, missing/same Accounts, and out-of-range epoch dates. Edits are blocked if either counterpart Account is archived.

Expanded the API 29 Compose journey to cover entry launch from Overview and both ledger tabs, Transfer creation from Overview, explicit Account selection, invalid input and allowed overdraft, paired rows and Account histories, filters and totals, moving an edit across Accounts and months, stable identity and timestamps, delete cancellation/confirmation, and archived counterpart restrictions. Added a larger-font journey. Captured screenshots:

- Normal entry: [transfer-normal.png](evidence/05/transfer-entry-evidence/transfer-normal.png)
- Validation: [transfer-validation.png](evidence/05/transfer-entry-evidence/transfer-validation.png)
- Larger font: [transfer-large-font.png](evidence/05/transfer-entry-evidence/transfer-large-font.png)

Verification on 2026-11-18:

- API 29 emulator `xperia_xz2_compact(AVD) - 10`: unfiltered connected Android suite passed, 59 tests with 0 failures, errors, or skips.
- `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:ktlintCheck`, `:app:assembleDebug`, and `:app:assembleDebugAndroidTest` passed.
- Python host-runner suite passed: 11 tests.
- `verify_overview_persistence.py` passed both selected and empty Account selection checks after host-driven process restarts. Evidence: `build/overview-persistence/20261118T133519187731Z`.
