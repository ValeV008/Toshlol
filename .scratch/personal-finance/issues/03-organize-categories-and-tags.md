# 03: Organize Transactions with Categories and Tags

**What to build:** A user can create and manage Categories and Tags, then apply them to Transactions without damaging historical references.

**Blocked by:** 02: Record and review Transactions

**Status:** resolved

- [x] Users can create, rename, and archive Categories and Tags.
- [x] Names are trimmed and case-insensitively unique among active entities in their scope.
- [x] A Tag can be independent or associated with one Category.
- [x] A Transaction can have an optional Category and multiple Tags.
- [x] Renaming an Account, Category, or Tag updates its displayed name on historical Transactions.
- [x] Archiving preserves history and prevents new use.
- [x] Tests cover lifecycle rules, associations, historical references, and the critical Compose flow.

## Answer

Implemented Category and Tag lifecycle management, Category/Tag assignments on Transactions, history-preserving renames and archives, and Account renaming. Added the Room 3→4 migration and tests for persistence, lifecycle rules, historical references, and the Compose flow.

Verification passed: JVM tests, all 17 Android 10 instrumentation tests, lint, and ktlint.
