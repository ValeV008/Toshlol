# 07: Merge an imported Backup

**What to build:** A user can import a validated Backup and merge it into local data without duplicate records or unexpected deletions.

**Blocked by:** 06: Export a versioned Backup

**Status:** resolved

## Parent-spec requirements

- [User stories 37–40 and 43–44](../spec.md#user-stories): reject malformed or unsupported Backups before changing the ledger; offer Merge; reconcile stable IDs using `updatedAt`; keep deletions local; preserve offline behavior.
- [Implementation decisions](../spec.md#implementation-decisions): one Backup import module owns strict decode, normalization, validation, reconciliation, and atomic writes of finance data and selected currency. Merge requires matching currency, replaces a complete record only for a newer timestamp, and rejects unresolved conflicts.
- [Testing decisions](../spec.md#testing-decisions): use the public import interface with Room's in-memory database; cover compatibility, validation, idempotency, timestamp conflicts, non-propagating deletions, final dataset conflicts, rollback, and document picker.

## Cross-screen acceptance

From Settings, the user can select a JSON Backup through Android's document picker and invoke Merge. A successful Merge refreshes Overview, Transactions, Accounts, Budgets, and Settings from the committed ledger; a failed or canceled import leaves their displayed data unchanged. The result reports inserted, updated, and unchanged counts for each entity type.

## Acceptance tests

- `BackupImportTest.mergeIsIdempotentAndAppliesNewerWholeRecords` asserts repeated import adds no duplicates and a newer record replaces all local fields.
- `BackupImportTest.rejectsCurrencyTimestampAndFinalDatasetConflictsAtomically` asserts conflicts reject the full merge.
- `BackupImportTest.invalidReferencesAndMalformedInputLeaveDataUnchanged` asserts complete validation precedes mutation.
- `BackupImportTest.failedCommitRollsBackEveryEntityAndSetting` asserts an injected transaction failure leaves the prior snapshot intact.
- `BackupImportTest.canceledImportDoesNotMutateLedger` and `BackupImportUiTest.settingsLaunchesDocumentPickerForMerge` cover cancellation and the Android document-provider journey.

- [x] The user can select a Backup through Android's document picker and choose Merge.
- [x] One Backup import module owns decoding, validation, and atomic mutation for both Merge and Overwrite; the document-provider adapter supplies bytes without leaking Android types into finance behavior.
- [x] File size, record counts, and reported validation failures are bounded, and supported older schemas normalize before strict decoding and semantic validation.
- [x] The complete Backup is self-contained and validates before mutation; the final reconciled dataset also validates before commit.
- [x] Merge rejects a Backup whose currency differs from the local ledger.
- [x] Stable IDs make repeated Merge operations idempotent.
- [x] Missing records are added, including locally deleted Transactions that remain in the Backup, and a newer imported `updatedAt` replaces the complete local record.
- [x] Equal-timestamp records must be identical; divergent content rejects the complete Merge.
- [x] Duplicate active names, duplicate monthly Budgets, or other cross-dataset conflicts reject the complete Merge rather than being repaired automatically.
- [x] Merge does not propagate deletions.
- [x] Import serializes against ledger writes, remains cancellable before commit, and uses one non-cancellable Room transaction for finance records and selected currency.
- [x] A failed import leaves the existing local dataset unchanged and returns deterministic structured failures without logging finance contents or document URIs.
- [x] A successful Merge reports inserted, updated, and unchanged counts per entity type without creating persistent import history.
- [x] Tests use Room's in-memory configuration through the public import interface and cover valid round trips, malformed JSON, unsupported schemas, missing references, currency mismatch, timestamp conflicts, idempotency, final-dataset conflicts, rollback after injected failures, and document-picker behavior.

## Answer

Implemented Merge from Settings through Android's document picker. The import module bounds and strictly decodes the input, normalizes supported schemas, validates the complete Backup and reconciled ledger, merges by stable ID and `updatedAt`, and atomically commits finance records and selected currency in Room. It preserves local deletions, rejects currency and content conflicts, and reports per-entity counts without exposing document URIs or finance contents in failures. The same module exposes validated Overwrite for issue 08.

Verification (2026-11-13): `testDebugUnitTest`, `lintDebug`, `ktlintCheck`, and all 41 Android instrumentation tests passed on API 29. The import tests cover idempotency, newer updates, currency and timestamp conflicts, duplicate active names, malformed input, invalid references and dates, imported-record counts, cancellation before the transaction, rollback, and picker cancellation.
