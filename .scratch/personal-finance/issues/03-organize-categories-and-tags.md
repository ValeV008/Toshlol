# 03: Organize Transactions with Categories and Tags

**What to build:** A user can create and manage Categories and Tags, then apply them to Transactions without damaging historical references.

**Blocked by:** 02: Record and review Transactions

**Status:** ready-for-agent

- [ ] Users can create, rename, and archive Categories and Tags.
- [ ] Names are trimmed and case-insensitively unique among active entities in their scope.
- [ ] A Tag can be independent or associated with one Category.
- [ ] A Transaction can have an optional Category and multiple Tags.
- [ ] Renaming an Account, Category, or Tag updates its displayed name on historical Transactions.
- [ ] Archiving preserves history and prevents new use.
- [ ] Tests cover lifecycle rules, associations, historical references, and the critical Compose flow.
