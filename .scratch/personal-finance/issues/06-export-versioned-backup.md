# 06: Export a versioned Backup

**What to build:** A user can export the complete local finance dataset as a versioned JSON Backup through Android's document picker.

**Blocked by:** 04: Set monthly Category Budgets

**Status:** ready-for-agent

- [ ] Export includes schema version, currency, settings, Accounts, Categories, Tags, Transactions, Budgets, stable IDs, and timestamps.
- [ ] Backup DTOs are separate from persistence entities and use the agreed serialization approach.
- [ ] The user selects the destination through Android's document picker, including Google Drive when available.
- [ ] The app does not require an account, backend, direct Google Drive API, or network permission.
- [ ] Export failures are reported without corrupting local data.
- [ ] Tests cover complete round-trip content, serialization, malformed output handling, and the document-provider boundary.
