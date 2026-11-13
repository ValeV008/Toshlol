# 06: Export a versioned Backup

**What to build:** A user can export the complete local finance dataset as a versioned JSON Backup through Android's document picker.

**Blocked by:** 04: Set monthly Category Budgets

**Status:** resolved

## Parent-spec requirements

- [User stories 34–36 and 43–44](../spec.md#user-stories): export a restorable versioned Backup through Android's document picker, including currency, Backup-owned settings, Accounts and opening balances, Categories, Tags, Transactions, Transfers, Budgets, stable IDs, timestamps, while retaining offline local-first behavior.
- [Implementation decisions](../spec.md#implementation-decisions): Kotlin serialization DTOs separate from Room entities; one Room transaction produces a consistent snapshot; export remains manual and uses Android's document picker without network or direct Drive APIs.
- [Testing decisions](../spec.md#testing-decisions): strict format compatibility and round-trip behavior, snapshot consistency, malformed output handling, and the document-provider boundary.

## Cross-screen acceptance

From Settings, the user can choose an export destination using the Android document picker and receives a clear success or failure result. The exported JSON contains every Backup-owned setting and finance record from one Room snapshot, including archived records, transfers, and opening balance dates. Export never mutates local finance data.

## Acceptance tests

- `BackupExportTest.exportWritesCompleteVersionedSnapshot` asserts all entities, archived records, opening balances, timestamps, and Tag associations round-trip through the document-writer seam.
- `BackupExportTest.codecRoundTripsAndRejectsMalformedUnknownAndUnsupportedInput` and `BackupExportTest.codecNormalizesVersionOneOpeningBalancesAndTransfers` assert strict decoding and supported-version normalization.
- `BackupExportTest.failedDocumentWriteLeavesFinanceSnapshotUnchanged` asserts provider failures do not mutate local finance data.

- [x] Export includes schema version, currency, settings, Accounts, Categories, Tags, Transactions, Budgets, stable IDs, and timestamps.
- [x] Backup DTOs are separate from persistence entities and use the agreed serialization approach.
- [x] Export reads finance records and Backup-owned settings from one Room transaction so the Backup is a consistent snapshot.
- [x] Supported older schema versions normalize to the current model, while unknown fields and unsupported versions are rejected rather than silently discarded.
- [x] The user selects the destination through Android's document picker, including Google Drive when available.
- [x] The app does not require an account, backend, direct Google Drive API, or network permission.
- [x] Export failures are reported without corrupting local data.
- [x] Tests cover complete round-trip content, strict serialization, consistent snapshots, malformed output handling, and the document-provider seam.

## Answer

Implemented versioned JSON Backup export through Android's document picker. The exporter reads settings and every finance entity in one Room transaction, serializes Backup DTOs separately from Room entities, rejects malformed/unknown/unsupported JSON, and normalizes schema version 1. Export writes through a document-writer interface and reports provider failures without changing the ledger.

Verification (2026-11-13): `testDebugUnitTest`, `lintDebug`, `ktlintCheck`, and Android test compilation passed. All four `BackupExportTest` cases passed on the Android 10 emulator. The full Android instrumentation run also exposed two existing `TransactionFlowTest` timeouts in Budget and navigation flows; the Backup tests passed in that run and again in isolation.
