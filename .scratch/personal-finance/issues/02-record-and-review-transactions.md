# 02: Record and review Transactions

**What to build:** A user can record, edit, delete, and review Income and Expense Transactions associated with a mandatory Account.

**Blocked by:** 01: Launch app and create the first Account

**Status:** resolved

- [x] A Transaction requires an Account, positive amount, Income or Expense direction, and local calendar date.
- [x] The user can optionally add a note and leave the Transaction uncategorized.
- [x] The user can edit a Transaction and delete it only after confirmation.
- [x] Transactions persist locally with stable IDs and timestamps.
- [x] The Overview shows current-month income, expenses, net balance, and recent Transactions.
- [x] Transactions are browsable newest first.
- [x] Tests cover validation, date behavior, calculations, persistence, editing, deletion, and the critical Compose flow.

## Answer

Implemented local Transaction storage and migration, validated create/edit flows, confirmed deletion, and current-month Overview totals with newest-first history. Unit tests, lint, ktlint, and all 14 API 29 emulator tests pass.
