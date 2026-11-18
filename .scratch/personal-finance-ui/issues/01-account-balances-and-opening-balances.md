# 01: Show Account balances and manage Opening balances

**What to build:** A user can see each Account's signed current balance and history, including money already held before the recorded ledger begins.

**Blocked by:** Personal Finance 01: Launch app and create the first Account; Personal Finance 02: Record and review Transactions; Personal Finance 03: Organize Transactions with Categories and Tags

**Status:** resolved

## Parent-spec requirements

- [User story 37](../spec.md#user-stories): the Accounts destination shows every Account's name, type, and signed current balance.
- [User stories 39–44](../spec.md#user-stories): Accounts have a signed Opening balance and as-of date; movements on or after that date affect balances; Income adds, Expense subtracts, and Transfers move value between Accounts; any sign is valid for every Account type.
- [User stories 45–46](../spec.md#user-stories): Account history explains its balance; archived Accounts retain their name, balance, and history while rejecting new movements.
- [Implementation decisions](../spec.md#implementation-decisions): Opening balance is the amount at the start of its local date; Room is the source of truth; retain local-first behavior and use explicit tested migrations.
- [Testing decisions](../spec.md#testing-decisions): cover balance boundaries and signs through repository/use-case behavior, persistence and migrations through Room, and the Account list/history journey through Compose semantics.

Observable acceptance criteria and regression coverage:

- The Accounts destination lists active and archived Accounts with type and signed current balance; `TransactionFlowTest.reviewEmptyAccountBeforeAndAfterArchiving` verifies values and navigation.
- Saving a changed Opening balance or as-of date persists it and recalculates the displayed current balance; `AppDatabaseTest.saveOpeningBalanceAndDateRecalculatesPersistedBalance` verifies editing and persistence.
- Only Income, Expense, and Transfer movements dated on or after the Opening balance date affect the balance; `AppDatabaseTest.saveOpeningBalanceAndDateRecalculatesPersistedBalance` verifies signs and the inclusive day boundary.
- Archived Accounts remain visible with read-only history, and Transaction/Transfer saves reject them as movement endpoints; `AppDatabaseTest.archiveAccountRetainsHistoryAndRejectsNewMovements` and `TransactionFlowTest.reviewEmptyAccountBeforeAndAfterArchiving` verify persistence and journey behavior.

- [x] Each Account has a signed Opening balance, defaulting to zero, and an as-of date, defaulting to today.
- [x] The Opening balance represents the Account amount at the start of the as-of date; only Income, Expense, and Transfer movements dated on or after that date affect the current balance.
- [x] Income increases the balance, Expense decreases it, and later Transfers decrease the source and increase the destination.
- [x] Positive, zero, and negative balances are allowed for every Account type; the UI displays the sign clearly.
- [x] A user can edit an Account's Opening balance and as-of date, and the displayed balance recalculates.
- [x] The Accounts destination shows Account names, types, and current balances; each Account's history shows the movements that affect it.
- [x] Archived Accounts retain their displayed balance and history and cannot receive new movements.
- [x] Tests verify balance calculations, as-of-day boundaries, editing, persistence, and the Account list/history journey through public behavior seams.

## Answer

Added a dedicated Accounts destination with signed balances, Account type, editable Opening balance and date, movement history, and archive state. Archived Accounts retain their history and balance and reject new or edited movements. Added the Room 6→7 migration and coverage for date boundaries, balance recalculation, archive persistence, and UI navigation. JVM tests and all 28 Android API 29 instrumentation tests pass.
