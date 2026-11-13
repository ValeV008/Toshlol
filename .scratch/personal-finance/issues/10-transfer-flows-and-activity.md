# 10: Transfer flows and activity

**What to build:** A user can move money between two Accounts with a Transfer and find that movement in summaries, account history, and transaction lists.

**Blocked by:** 01: Launch and first Account, 02: Record and review Transactions, 03: Organize Transactions with Categories and Tags

**Status:** resolved

## Parent-spec requirements

- User stories 52–58: preserve Account opening balances and signed balances; record Transfers between distinct Accounts; exclude Transfers from Income, Expense, and Budget totals; show Transfer activity in Overview, both Account histories, and outgoing/incoming transaction lists; filter Transfers by month and relevant Account while Category and Tag filters hide them; expose Transfer quick-add.
- User stories 49 and 51: cover finance rules at the application seam and critical Compose journeys through observable behavior.

## Observable acceptance criteria

- A Transfer has a positive amount, local date, stable ID, distinct source and destination Accounts, and timestamps; it has no Category, Tag, or Note.
- Users can create, edit, and delete Transfers through the app's existing transaction entry points.
- Account balances subtract a Transfer from the source and add it to the destination, respecting each Account's opening-balance date.
- Transfers do not affect Income totals, Expense totals, or Budget progress.
- Overview and Account history show Transfer activity; the Expense list shows the outgoing source row and the Income list shows the incoming destination row, and both rows open the same Transfer.
- The Transactions list filters Transfer rows by month and the corresponding Account; selecting a Category or Tag excludes Transfer rows.
- Compose tests cover transfer creation, validation, balance effects, list presentation, filters, editing, and deletion through public behavior.

- [x] Transfer data and balance rules are persisted and tested through repository operations and Room migrations.
- [x] Transfer quick-add and entry are accessible and covered by Compose tests.
- [x] Transfer rows appear in Overview, Account history, and the Income/Expense lists with the correct source/destination context.
- [x] Month and relevant Account filters include Transfer rows; Category and Tag filters exclude them.
- [x] Income, Expense, and Budget totals exclude Transfers.

## Answer

Implemented version 6 Room storage for Transfers and Account opening balances, with signed balance calculation that respects each Account's opening date. Added Account creation and per-Account name/opening balance/date management so a fresh user can create two Accounts before moving money. Transfers can be created, edited, and deleted from quick-add; Overview shows Transfer activity and signed Account balances. The Transactions list shows outgoing and incoming rows that open the same Transfer and applies month and Account filters while Category and Tag filters hide those rows.

Behavior coverage includes `transfersPersistValidateAndChangeAccountBalancesByOpeningDate`, `migrateVersionThreeTransactionsWithoutLosingHistory`, and `createEditFilterAndDeleteTransferWithoutChangingIncomeOrExpenseTotals`. The repository test verifies Transfer balance and Budget exclusion; the Compose test covers creating Accounts, selecting and editing a secondary Account's name/opening balance/date, amount and distinct-Account validation, list context, filters, Transfer editing/deletion, and Income/Expense totals.

Verification passed: JVM tests, lint, ktlint, all 24 Android 10 instrumentation tests, and `assembleDebug`.
