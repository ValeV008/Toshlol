# Stable Entity Identities

Accounts, Categories, Tags, Transactions, Budgets, and backup records use stable unique IDs rather than timestamp-only identity. Transactions reference Accounts, Categories, and Tags by ID, so renaming an entity updates its displayed name throughout history while archiving preserves historical references without allowing new use.

## Consequences

- UUID generation and time are separate injectable concerns in tests.
- Merge can be idempotent and resolve conflicting versions with `updatedAt`.
- Deleting referenced entities is replaced by archiving them.
