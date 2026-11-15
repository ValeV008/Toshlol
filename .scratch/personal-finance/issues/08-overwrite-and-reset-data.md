# 08: Overwrite data and reset locally

**What to build:** A user can deliberately replace all local data from a validated Backup or delete all local data from Settings.

**Blocked by:** 06: Export a versioned Backup

**Status:** resolved

## Parent-spec requirements

- [User stories 37–38 and 41–43](../spec.md#user-stories): reject malformed Backups without mutation, allow the user to choose Merge or Overwrite, require deliberate confirmation before replacement, and preserve stable entity IDs.
- [User stories 33 and 42](../spec.md#user-stories): expose data management in Settings and provide a separate confirmed operation to delete all local data.
- [Implementation decisions](../spec.md#implementation-decisions): Overwrite uses the shared strict Backup module, replaces finance records and selected currency in one Room transaction, remains cancellable before that transaction, and validates before mutation.
- [Testing decisions](../spec.md#testing-decisions): exercise import through the public interface with Room, including Overwrite, cancellation, malformed input, rollback, persistence, and document-picker journeys.

## Cross-screen acceptance

After a confirmed Overwrite commits, Settings shows the imported currency and import counts, and Overview, Transactions, Accounts, and Budgets show the replacement dataset. Before commit, those screens retain the prior complete dataset. A rejected or canceled import leaves it unchanged. Confirmed local reset clears the local ledger and Backup-owned settings, returns the app to first-run setup, and is independent of Backup import.

## Acceptance tests

- `BackupImportTest.overwriteReplacesCompleteDatasetAndSettingsAndReportsInsertedCounts` checks whole-dataset replacement, currency replacement, and inserted counts.
- `BackupImportTest.canceledImportDoesNotMutateLedger`, `BackupImportTest.failedCommitRollsBackEveryEntityAndSetting`, and `BackupImportTest.invalidReferencesAndMalformedInputLeaveDataUnchanged` cover cancellation, rollback, and validation failure.
- `BackupImportTest.localResetClearsBackupDataAndAllowsFirstRunSetupAgain` checks the separate reset operation.
- `BackupImportUiTest.settingsLaunchesDocumentPickerForMergeAndCancellationReturnsToSettings` covers Merge, confirmed Overwrite, and reset confirmation through Settings and Android document picker.

- [x] The user can choose Overwrite during Backup import.
- [x] Overwrite uses the same Backup import module, strict decoding, bounded input, self-contained validation, and structured failures as Merge.
- [x] Overwrite requires explicit UI confirmation before invocation and atomically replaces the complete local dataset, including selected currency and other Backup-owned settings, in one non-cancellable Room transaction.
- [x] Screens observe the previous complete dataset until commit and never observe partial replacement.
- [x] Validation failure, storage failure, or cancellation before commit leaves the existing local dataset unchanged; retry remains an explicit user action.
- [x] Settings provides a separate explicit confirmation and application operation for deleting all local data; reset is not part of the Backup import module.
- [x] Successful Overwrite reports inserted counts per entity type without creating persistent import history.
- [x] Tests cover overwrite, pre-commit cancellation, rollback after injected transaction failure, malformed input, persistence, separate reset behavior, and critical Compose/document-picker journeys.

## Answer

Settings now offers Merge and confirmed Overwrite through the shared `RoomBackupImporter`, plus a separate confirmed reset operation. Overwrite atomically replaces all Backup-owned records and settings; successful imports refresh setup and ledger screens and report per-entity counts. Reset clears ledger records and settings in one Room transaction and returns the app to first-run setup. Confirmation dialogs describe the effects, and Settings scrolls so all data-management actions remain reachable.

Verification (2026-11-15): `testDebugUnitTest`, `lintDebug`, `ktlintCheck`, and all 44 `connectedDebugAndroidTest` cases passed on the API 29 emulator. `BackupImportTest` covers full replacement, counts, cancellation, rollback, malformed input, and reset. `BackupImportUiTest.settingsLaunchesDocumentPickerForMergeAndCancellationReturnsToSettings` covers Merge picker cancellation, confirmed Overwrite with a real document-provider Backup and refreshed currency/Account state, and confirmed reset to first-run setup.
