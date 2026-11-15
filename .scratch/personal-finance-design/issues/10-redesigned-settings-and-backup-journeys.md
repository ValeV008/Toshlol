# 10: Redesigned Settings and Backup journeys

Status: ready-for-agent
Blocked by: 02, 07

**What to build:** Use redesigned Settings and the Export destination to save and restore the complete finance dataset, including custom Budget filters, through Android's document picker with preserved confirmations, cancellation, validation, and atomic failure behavior.

## Parent-spec requirements

- [User stories](../spec.md#user-stories): Stories 1-7, 82-85: shared-style Settings and Export destinations, discoverable existing data-management tasks, accessible offline Android 10 journeys, and screenshots.
- [User stories](../spec.md#user-stories): Stories 75-80: preservation/restoration of all finance data and Budget references, older supported Backups, complete versioned definitions, validation, and atomic Merge/Overwrite.
- [User stories](../spec.md#user-stories): Story 82 also preserves the existing explicit confirmations and outcomes for Overwrite and local reset.
- [Implementation decisions](../spec.md#implementation-decisions): Use ticket 02's navigation destinations and ticket 07's complete, already working Budget Backup contracts. Restyle and exercise the real Android document-provider journeys without moving validation or reconciliation into the UI or changing the atomic-import ADR.
- [Testing decisions](../spec.md#testing-decisions): use existing public finance use-case/repository and Backup interfaces, Compose semantics, and UI Automator for system UI; test visible outcomes rather than private helpers or tables. Named tests below are proposed or extensions of existing behavior tests, not claims of passing implementation.

## Observable acceptance criteria

- [ ] Settings shows existing currency/context and data-management capabilities in shared-style surfaces with clear labeled actions and status/error outcomes. The main drawer's Export destination launches the existing user-chosen document export workflow.
- [ ] Export writes a consistent complete snapshot including Accounts and signed Opening balances/dates, classifications, Transactions, Transfers, custom Budget names/limits/all selections, currency, stable IDs, timestamps, and format version.
- [ ] Import Merge and Import Overwrite use the Android document picker. Overwrite and local reset retain explicit destructive confirmations; canceling a confirmation or picker leaves finance data unchanged and returns to the correct usable screen.
- [ ] After round-trip/Overwrite and app reopen, public reads and visible month-end totals, Account histories, and independent Budget progress match the exported dataset, including overlapping, unrestricted, and multiply restricted Budgets.
- [ ] Merge remains idempotent and applies newer complete Budget records, without stale Account/Category/Tag membership union. Invalid/dangling references, malformed/unsupported format, currency conflicts, and divergent equal timestamps fail before mutation with readable outcomes.
- [ ] Older supported Backups import through the reviewed lossless mapping established in ticket 06 and extended in ticket 07. Export/write failures, import cancellation, or failed commits preserve the prior complete dataset and settings; no partial state becomes visible.
- [ ] Confirmed local reset clears finance data using the existing behavior and makes first-run setup available; canceling reset preserves it. This ticket does not add an unrequested Backup requirement for the local drawer presentation preference.
- [ ] Run public full-snapshot Backup/export/import/reset tests and Android 10 Compose/UI Automator journeys for real system-picker launch, completion, cancellation, validation, and confirmation; run relevant JVM/lint/formatting checks and capture Settings/status/confirmation examples.

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
