# 07: Merge an imported Backup

**What to build:** A user can import a validated Backup and merge it into local data without duplicate records or unexpected deletions.

**Blocked by:** 06: Export a versioned Backup

**Status:** ready-for-agent

- [ ] The user can select a Backup through Android's document picker and choose Merge.
- [ ] The complete Backup validates before any local mutation occurs.
- [ ] Stable IDs make repeated Merge operations idempotent.
- [ ] Missing records are added and conflicting records use the newer `updatedAt` version.
- [ ] Merge does not propagate deletions.
- [ ] A failed import leaves the existing local dataset unchanged.
- [ ] Tests cover valid round trips, malformed JSON, unsupported schema versions, missing mandatory Accounts, timestamp conflicts, idempotency, and document-picker behavior.
