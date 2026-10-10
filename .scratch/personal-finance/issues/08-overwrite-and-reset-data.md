# 08: Overwrite data and reset locally

**What to build:** A user can deliberately replace all local data from a validated Backup or delete all local data from Settings.

**Blocked by:** 06: Export a versioned Backup

**Status:** ready-for-agent

- [ ] The user can choose Overwrite during Backup import.
- [ ] Overwrite validates the complete Backup before mutation.
- [ ] Overwrite requires explicit confirmation and atomically replaces the complete local dataset, including currency and settings.
- [ ] Settings provides a separate explicit confirmation for deleting all local data.
- [ ] Failed validation or cancellation leaves the existing local dataset unchanged.
- [ ] Tests cover overwrite, cancellation, malformed input, persistence, reset behavior, and critical Compose/document-picker journeys.
