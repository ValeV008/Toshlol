# 04: Set monthly Category Budgets

**What to build:** A user can create monthly spending limits for Categories and see whether spending is within or beyond each limit.

**Blocked by:** 03: Organize Transactions with Categories and Tags

**Status:** resolved

- [x] A Budget belongs to one Category and one calendar month.
- [x] Only one active Budget can exist for a Category in a month.
- [x] Budget amounts must be greater than zero and use precise currency values.
- [x] The app shows actual spending, remaining amount, and over-budget state.
- [x] Existing Budgets for archived Categories remain visible, while new Budgets cannot use archived Categories.
- [x] Budgets do not roll over and do not represent income limits.
- [x] Tests cover monthly boundaries, calculations, uniqueness, archived Categories, persistence, and the critical Compose flow.

## Answer

Implemented monthly Category Budgets with precise minor-unit values, Room persistence and a database version 4 to 5 migration, monthly Expense-only calculations, duplicate prevention, archived Category history, and a dedicated Budgets screen linked from Overview. Added Room behavior coverage and a Compose journey for creating a Budget and viewing over-budget progress.

Verification: unit tests, lint, the focused Budget repository and Compose tests passed. The focused Budget repository and Compose tests pass on the Android 10 emulator. The full Android 10 suite has intermittent Compose interaction failures in TransactionFlowTest, including setup assertions; they are recorded in the test output.
