# Local-First Finance Data

The app owns finance data locally and does not require an account, backend, or `INTERNET` permission. Backups are explicit, versioned JSON files selected through Android's document picker, which can expose Google Drive; imports offer validated Merge or Overwrite because local financial history should remain usable offline and cloud synchronization would add unnecessary ownership and conflict complexity.

## Consequences

- The app must provide reliable export and import validation.
- Merge uses stable IDs and timestamps, while deletions do not propagate during merge.
- Overwrite is the explicit recovery path when the backup should become authoritative.
