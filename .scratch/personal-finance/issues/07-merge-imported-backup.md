# 07: Merge an imported Backup

**What to build:** A user can import a validated Backup and merge it into local data without duplicate records or unexpected deletions.

**Blocked by:** 06: Export a versioned Backup

**Status:** ready-for-agent

- [ ] The user can select a Backup through Android's document picker and choose Merge.
- [ ] One Backup import module owns decoding, validation, and atomic mutation for both Merge and Overwrite; the document-provider adapter supplies bytes without leaking Android types into finance behavior.
- [ ] File size, record counts, and reported validation failures are bounded, and supported older schemas normalize before strict decoding and semantic validation.
- [ ] The complete Backup is self-contained and validates before mutation; the final reconciled dataset also validates before commit.
- [ ] Merge rejects a Backup whose currency differs from the local ledger.
- [ ] Stable IDs make repeated Merge operations idempotent.
- [ ] Missing records are added, including locally deleted Transactions that remain in the Backup, and a newer imported `updatedAt` replaces the complete local record.
- [ ] Equal-timestamp records must be identical; divergent content rejects the complete Merge.
- [ ] Duplicate active names, duplicate monthly Budgets, or other cross-dataset conflicts reject the complete Merge rather than being repaired automatically.
- [ ] Merge does not propagate deletions.
- [ ] Import serializes against ledger writes, remains cancellable before commit, and uses one non-cancellable Room transaction for finance records and selected currency.
- [ ] A failed import leaves the existing local dataset unchanged and returns deterministic structured failures without logging finance contents or document URIs.
- [ ] A successful Merge reports inserted, updated, and unchanged counts per entity type without creating persistent import history.
- [ ] Tests use Room's in-memory configuration through the public import interface and cover valid round trips, malformed JSON, unsupported schemas, missing references, currency mismatch, timestamp conflicts, idempotency, final-dataset conflicts, rollback after injected failures, and document-picker behavior.
