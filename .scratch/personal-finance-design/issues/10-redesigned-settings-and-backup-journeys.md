# 10: Redesigned Settings and Backup journeys

Status: resolved
Blocked by: 02, 07

**What to build:** Use redesigned Settings and the Export destination to save and restore the complete finance dataset, including custom Budget filters, through Android's document picker with preserved confirmations, cancellation, validation, and atomic failure behavior.

## Parent-spec requirements

- [User stories](../spec.md#user-stories): Stories 1-7, 82-85: shared-style Settings and Export destinations, discoverable existing data-management tasks, accessible offline Android 10 journeys, and screenshots.
- [User stories](../spec.md#user-stories): Stories 75-80: preservation/restoration of all finance data and Budget references, older supported Backups, complete versioned definitions, validation, and atomic Merge/Overwrite.
- [User stories](../spec.md#user-stories): Story 82 also preserves the existing explicit confirmations and outcomes for Overwrite and local reset.
- [Implementation decisions](../spec.md#implementation-decisions): Use ticket 02's navigation destinations and ticket 07's complete, already working Budget Backup contracts. Restyle and exercise the real Android document-provider journeys without moving validation or reconciliation into the UI or changing the atomic-import ADR.
- [Testing decisions](../spec.md#testing-decisions): use existing public finance use-case/repository and Backup interfaces, Compose semantics, and UI Automator for system UI; test visible outcomes rather than private helpers or tables. Named tests below are proposed or extensions of existing behavior tests, not claims of passing implementation.

## Observable acceptance criteria

- [x] Settings shows existing currency/context and data-management capabilities in shared-style surfaces with clear labeled actions and status/error outcomes. The main drawer's Export destination launches the existing user-chosen document export workflow.
- [x] Export writes a consistent complete snapshot including Accounts and signed Opening balances/dates, classifications, Transactions, Transfers, custom Budget names/limits/all selections, currency, stable IDs, timestamps, and format version.
- [x] Import Merge and Import Overwrite use the Android document picker. Overwrite and local reset retain explicit destructive confirmations; canceling a confirmation or picker leaves finance data unchanged and returns to the correct usable screen.
- [x] After round-trip/Overwrite and app reopen, public reads and visible month-end totals, Account histories, and independent Budget progress match the exported dataset, including overlapping, unrestricted, and multiply restricted Budgets.
- [x] Merge remains idempotent and applies newer complete Budget records, without stale Account/Category/Tag membership union. Invalid/dangling references, malformed/unsupported format, currency conflicts, and divergent equal timestamps fail before mutation with readable outcomes.
- [x] Older supported Backups import through the reviewed lossless mapping established in ticket 06 and extended in ticket 07. Export/write failures, import cancellation, or failed commits preserve the prior complete dataset and settings; no partial state becomes visible.
- [x] Confirmed local reset clears finance data using the existing behavior and makes first-run setup available; canceling reset preserves it. This ticket does not add an unrequested Backup requirement for the local drawer presentation preference.
- [x] Run public full-snapshot Backup/export/import/reset tests and Android 10 Compose/UI Automator journeys for real system-picker launch, completion, cancellation, validation, and confirmation; run relevant JVM/lint/formatting checks and capture Settings/status/confirmation examples.

## Named behavior tests

| Test or journey | Observable result |
| --- | --- |
| exportAndRestoreCompleteCustomBudgetDataset | Round-trip via document/public Backup interfaces and assert all finance records, full Budget definitions, month-end values, and progress after reopen. |
| settingsLaunchesDocumentPickerForMergeAndCancellationReturnsToSettings | Extend existing UI Automator prior art with redesigned routing and unchanged data after cancel. |
| confirmOverwriteBeforeOpeningDocumentPicker | Assert cancellation preserves data and confirmed replacement restores the complete new dataset. |
| mergeCompleteBudgetFiltersWithoutDuplication | Assert idempotency, newer complete replacement, equal-timestamp/currency/reference rejection, and no stale set union. |
| retainCompleteDatasetAfterBackupFailureOrCancellation | Exercise document write/read failure, cancellation, invalid input, and failed commit through public Backup outcomes/snapshots. |
| importSupportedLegacyBackupsThroughReviewedMapping | Assert lossless legacy records and expected calculations after real import and reopen. |
| confirmResetAndReturnToFirstRun | Assert cancel/confirm outcomes using the public reset path and setup UI. |
| operateDataManagementAtLargeFont | Assert action labels, readable progress/error results, confirmations, navigation, and scrolling. |

## Delivery boundaries

This slice completes redesigned user-facing data management; earlier Budget slices must already serialize, validate, migrate, and round-trip their own fields. No direct cloud API, profile/login, background synchronization, encrypted Backup feature, or release distribution.

Each slice must remain independently demoable and keep existing callers and relevant checks working. Include the feature's behavior and UI verification in this ticket rather than deferring them to the final integration ticket. Follow the parent spec's exclusions and existing ADRs.

## Answer

Redesigned Settings into accessible, shared-style surfaces for the finance profile, quick actions, classifications, backups, and destructive data actions. Backup outcomes are announced from a shared live status notice. The drawer's Export destination now selects Settings before launching the existing Android document-create flow, so completion or cancellation returns to a usable screen with the outcome visible. Overwrite and local reset still require explicit confirmation.

Kept the existing versioned Backup and atomic import contracts intact. Added real Android document-picker coverage for Export cancellation, Merge cancellation, Overwrite confirmation/cancellation/completion, and reset confirmation/cancellation/completion. Added a persistent database round-trip test that exports and overwrites a complete custom dataset, reopens the database, and checks public Account, transaction, transfer, month-end, history, and independent unrestricted/category/tag-filtered Budget reads. Existing Backup tests continue to cover complete versioned snapshots, idempotent complete-record Merge, conflict/reference validation, legacy mappings, and rollback after failed writes or commits.

The Overwrite UI journey also recreates the Activity after import and verifies the restored Overview total, unrestricted and filtered Budget progress, Account balance, and Account history before confirming reset.

Verification on the Android 10/API 29 `xperia_xz2_compact(AVD) - 10` emulator:

- `:app:ktlintCheck`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, and `:app:assembleDebugAndroidTest` passed.
- The full unfiltered connected suite passed 95/95 tests with no failures, errors, or skips. After adding the dedicated round-trip regression, the focused `BackupImportTest` suite passed 10/10, including `exportAndRestoreCompleteCustomBudgetDataset`.
- The final focused `BackupImportTest`, `BackupImportUiTest`, and `SettingsAccessibilityTest` run passed 14/14 tests. The restored Overwrite UI journey passed after an Activity recreation, including visible Overview, Budget, and Account history checks.
- Settings, cancellation/status, larger-font, Overwrite confirmation, and readable error screenshots are in [`evidence/10/`](../evidence/10/). Run details are in [`evidence/10/verification.md`](../evidence/10/verification.md).
