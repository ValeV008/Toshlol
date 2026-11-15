# 05: Include Transfers and Opening balances in Backups

**What to build:** A user can export and restore a complete finance dataset while preserving Transfers, Account balances, and compatibility with older Backups.

**Blocked by:** Personal Finance UI 01: Show Account balances and manage Opening balances; Personal Finance UI 02: Record and review Transfers; Personal Finance 06: Export a versioned Backup; Personal Finance 07: Merge an imported Backup; Personal Finance 08: Overwrite data and reset locally

**Status:** resolved

## Parent-spec requirements

- [Personal Finance UI story 58](../spec.md#user-stories): Export a versioned JSON Backup through Android's document picker.
- [Personal Finance UI stories 59–67](../spec.md#user-stories): Preserve complete data and schema version, normalize older Backups, reject malformed/conflicting data before mutation, support Merge and confirmed Overwrite, and keep the previous dataset visible until atomic commit.
- [Personal Finance UI decisions](../spec.md#implementation-decisions): Backups include Transfers and signed Opening balances/dates; legacy Backups normalize to zero and use the oldest movement date (or import date); imports use the single atomic Backup module.
- Restored Opening balances affect displayed Account balances; restored Transfers appear in both affected Account histories and the Income/Expense lists without changing totals.

## Acceptance criteria

- [x] New Backups include Transfers, signed Opening balances/dates, the complete finance dataset, and schema version; `BackupExportTest.exportWritesCompleteVersionedSnapshot` verifies the exported snapshot.
- [x] Export reads one consistent dataset snapshot; Merge and Overwrite validate and mutate atomically through the Backup module; `BackupImportTest.failedCommitRollsBackEveryEntityAndSetting` verifies rollback.
- [x] Transfer references and Opening balance dates validate before mutation; `BackupImportTest.invalidReferencesAndMalformedInputLeaveDataUnchanged` verifies the pre-mutation rejection.
- [x] Supported older Backups without Transfers normalize to no Transfers and zero Opening balances; dates derive from the oldest transaction movement or import date; `BackupExportTest.codecNormalizesVersionOneOpeningBalancesAndTransfers` and `BackupImportTest.importerNormalizesV1AndRejectsUnsupportedSchemaWithoutMutation` cover normalization.
- [x] Merge is idempotent and Overwrite replaces the complete dataset; covered by `BackupImportTest.mergeIsIdempotentAndAppliesNewerWholeRecords` and `BackupImportTest.overwriteReplacesCompleteDatasetAndSettingsAndReportsInsertedCounts`.
- [x] The document-picker journey verifies restored Account balance and Transfer history through UI; `BackupImportUiTest.settingsLaunchesDocumentPickerForMergeAndCancellationReturnsToSettings`.

## Answer

Backup schema version 2 persists Transfers and signed Opening balances/dates. The shared Backup importer validates the complete normalized dataset before one Room transaction, and the exporter snapshots all Backup-owned data inside one Room transaction. The picker journey now checks the restored balance and Transfer history. Older version 1 Backups normalize with no Transfers and zero Opening balances, using the earliest transaction date per Account or the import date when there are no movements.
