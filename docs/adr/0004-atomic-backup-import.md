# Atomic Backup Import

Backup import uses one deep application module for Merge and Overwrite, and commits all finance data and Backup-owned settings in one Room transaction after complete validation. Selected currency therefore lives in Room with the ledger, while DataStore is limited to preferences that are not part of a Backup, such as theme; this replaces the planned split ownership because Room and DataStore cannot share a crash-safe transaction.

## Consequences

- The Android document-provider adapter supplies Backup bytes but does not own validation, reconciliation, or mutation.
- Merge and Overwrite share decoding, validation, exclusive mutation, structured outcomes, and failure handling; local reset remains a separate operation.
- Merge rejects currency mismatches, equal-timestamp divergent records, invalid final datasets, and conflicts that would require invented user intent.
- Export reads all Backup-owned state in one Room transaction so every Backup is a consistent snapshot.
- A cross-store recovery protocol and persistent import history are deliberately excluded.
