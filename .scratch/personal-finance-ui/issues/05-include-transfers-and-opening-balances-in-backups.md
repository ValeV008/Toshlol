# 05: Include Transfers and Opening balances in Backups

**What to build:** A user can export and restore a complete finance dataset while preserving Transfers, Account balances, and compatibility with older Backups.

**Blocked by:** Personal Finance UI 01: Show Account balances and manage Opening balances; Personal Finance UI 02: Record and review Transfers; Personal Finance 06: Export a versioned Backup; Personal Finance 07: Merge an imported Backup; Personal Finance 08: Overwrite data and reset locally

**Status:** ready-for-agent

- [ ] New Backups include Transfers and each Account's signed Opening balance and as-of date, along with the existing finance dataset and schema version.
- [ ] Export reads the complete dataset consistently; Merge and Overwrite validate and mutate it atomically using the existing Backup import module.
- [ ] Transfer references and Account Opening balance fields are validated before any local mutation.
- [ ] Supported older Backups without Transfers import with no Transfers.
- [ ] Supported older Backups without Opening balance data import with a zero Opening balance and an as-of date equal to the oldest movement date for that Account, or the import date if it has no movements.
- [ ] Import failures leave the existing complete local dataset unchanged and report structured, deterministic outcomes.
- [ ] Tests verify export/import round trips, older-Backup normalization, references, Merge idempotency, Overwrite, rollback, and the document-picker journey through the public Backup interface.
