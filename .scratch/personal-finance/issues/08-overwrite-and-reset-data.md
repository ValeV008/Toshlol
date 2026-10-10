# 08: Overwrite data and reset locally

**What to build:** A user can deliberately replace all local data from a validated Backup or delete all local data from Settings.

**Blocked by:** 06: Export a versioned Backup

**Status:** ready-for-agent

- [ ] The user can choose Overwrite during Backup import.
- [ ] Overwrite uses the same Backup import module, strict decoding, bounded input, self-contained validation, and structured failures as Merge.
- [ ] Overwrite requires explicit UI confirmation before invocation and atomically replaces the complete local dataset, including selected currency and other Backup-owned settings, in one non-cancellable Room transaction.
- [ ] Screens observe the previous complete dataset until commit and never observe partial replacement.
- [ ] Validation failure, storage failure, or cancellation before commit leaves the existing local dataset unchanged; retry remains an explicit user action.
- [ ] Settings provides a separate explicit confirmation and application operation for deleting all local data; reset is not part of the Backup import module.
- [ ] Successful Overwrite reports inserted counts per entity type without creating persistent import history.
- [ ] Tests cover overwrite, pre-commit cancellation, rollback after injected transaction failure, malformed input, persistence, separate reset behavior, and critical Compose/document-picker journeys.
