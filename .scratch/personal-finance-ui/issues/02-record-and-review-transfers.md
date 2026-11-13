# 02: Record and review Transfers

**What to build:** A user can record one Transfer between two different Accounts and find it consistently in the relevant Account and ledger views without counting it as Income or Expense.

**Blocked by:** Personal Finance UI 01: Show Account balances and manage Opening balances; Personal Finance 02: Record and review Transactions; Personal Finance 05: Filter and navigate the ledger

**Status:** resolved

## Parent-spec requirements

- [Spec user stories 10, 27–36](../spec.md#user-stories): quick-add provides a distinct Transfer flow; require a positive amount, editable local date, and explicit distinct Accounts; show one Transfer as outgoing in Expenses and incoming in Income, both opening the same record; include it in Account histories and Overview activity while excluding it from Income, Expense, net, and Budget totals. Compose coverage: `createEditFilterAndDeleteTransferWithoutChangingIncomeOrExpenseTotals` verifies validation, explicit Account selection, both balances and histories, paired list rows, filters, editing, deletion, and unchanged totals; `transferDoesNotContributeToBudgetActualSpending` verifies Budget exclusion.
- [Spec implementation decisions](../spec.md#implementation-decisions): keep Transfer as one persisted domain record, update it atomically, and derive signed Account balance effects from its source and destination. JVM coverage: `TransferManagementTest` rejects non-positive amounts and same-Account drafts; Android persistence and journey coverage: `AppDatabaseTest` and `createEditFilterAndDeleteTransferWithoutChangingIncomeOrExpenseTotals`.
- [Spec testing decisions](../spec.md#testing-decisions): exercise public finance behavior and accessible Compose semantics, including persistence. Coverage: `TransferManagementTest`, `AppDatabaseTest`, and `createEditFilterAndDeleteTransferWithoutChangingIncomeOrExpenseTotals`.

- [x] Quick-add offers Transfer as a third movement flow.
- [x] A Transfer requires a positive amount, editable local calendar date defaulting to today, and explicitly selected, distinct source and destination Accounts.
- [x] A Transfer has no Category, Tags, or Note and is allowed to overdraw its source Account.
- [x] Saving, editing, or deleting a Transfer updates both Account balances and both Account histories as one movement.
- [x] The Expense list shows the outgoing side and the Income list shows the incoming side; both rows open the same Transfer.
- [x] Overview recent activity includes Transfers. Transfer rows are labeled as Transfers and excluded from Income totals, Expense totals, monthly net, and Budget actual spending.
- [x] Month and relevant Account filters include Transfer rows; Category and Tag filters hide them.
- [x] Tests verify validation, both balance effects, paired views, filtering, totals, editing/deletion consistency, persistence, and the critical Compose journey through public behavior seams.

## Answer

Transfer entry now leaves both Account choices empty until the user selects them. The transfer journey and Budget exclusion are covered by Compose tests; finance validation and persistence are covered by the existing JVM and database tests.

Verification: `testDebugUnitTest`, `ktlintCheck`, `compileDebugAndroidTestKotlin`, `createEditFilterAndDeleteTransferWithoutChangingIncomeOrExpenseTotals`, and `transferDoesNotContributeToBudgetActualSpending` passed.
