# 06: Export a versioned Backup

**What to build:** A user can export the complete local finance dataset as a versioned JSON Backup through Android's document picker.

**Blocked by:** 04: Set monthly Category Budgets

**Status:** claimed

## Parent-spec requirements

- [User stories 34–36 and 43–44](../spec.md#user-stories): export a restorable versioned Backup through Android's document picker, including currency, Backup-owned settings, Accounts and opening balances, Categories, Tags, Transactions, Transfers, Budgets, stable IDs, timestamps, while retaining offline local-first behavior.
- [Implementation decisions](../spec.md#implementation-decisions): Kotlin serialization DTOs separate from Room entities; one Room transaction produces a consistent snapshot; export remains manual and uses Android's document picker without network or direct Drive APIs.
- [Testing decisions](../spec.md#testing-decisions): strict format compatibility and round-trip behavior, snapshot consistency, malformed output handling, and the document-provider boundary.

## Cross-screen acceptance

From Settings, the user can choose an export destination using the Android document picker and receives a clear success or failure result. The exported JSON contains every Backup-owned setting and finance record from one Room snapshot, including archived records, transfers, and opening balance dates. Export never mutates local finance data.

## Acceptance tests

- `BackupExportTest.exportWritesCompleteVersionedSnapshot` asserts the encoded DTO round-trips all entity fields and Tag associations, strict decoding rejects malformed, unknown-field, and unsupported-version input, and the Room snapshot is consistent.
- `SettingsFlowTest.exportLaunchesDocumentPickerAndShowsResult` asserts Settings launches the document-provider flow and reports successful and failed writes accessibly.

- [x] Export includes schema version, currency, settings, Accounts, Categories, Tags, Transactions, Budgets, stable IDs, and timestamps.
- [x] Backup DTOs are separate from persistence entities and use the agreed serialization approach.
- [x] Export reads finance records and Backup-owned settings from one Room transaction so the Backup is a consistent snapshot.
- [x] Supported older schema versions normalize to the current model, while unknown fields and unsupported versions are rejected rather than silently discarded.
- [x] The user selects the destination through Android's document picker, including Google Drive when available.
- [x] The app does not require an account, backend, direct Google Drive API, or network permission.
- [x] Export failures are reported without corrupting local data.
- [ ] Tests cover complete round-trip content, strict serialization, consistent snapshots, malformed output handling, and the document-provider seam.
