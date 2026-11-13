# 04: Set monthly Category Budgets

**What to build:** A user can create monthly spending limits for Categories and see whether spending is within or beyond each limit.

**Blocked by:** 03: Organize Transactions with Categories and Tags

**Status:** resolved

- [x] A Budget belongs to one Category and one calendar month.
- [x] Only one active Budget can exist for a Category in a month.
- [x] Budget amounts must be greater than zero and use precise currency values.
- [x] The app shows actual spending, remaining amount, and over-budget state.
- [x] Overview shows current-month Category Budget progress with the same spending and limit amounts as Budgets, plus the remaining amount or over-budget state; changes to an Expense update both screens.
- [x] Existing Budgets for archived Categories remain visible, while new Budgets cannot use archived Categories.
- [x] Budgets do not roll over and do not represent income limits.
- [x] Tests cover monthly boundaries, calculations, uniqueness, archived Categories, persistence, and the critical Compose flow.

## Parent-spec requirements

- [User stories 24–27 and 61](../spec.md#user-stories): monthly Category limits, uniqueness, actual spending and remaining/over-budget state, archived history, and a separate Budgets destination.
- [User stories 28–29](../spec.md#user-stories): Overview defaults to the current month and includes Budget progress. This display is part of this ticket alongside the Budgets screen.
- [Implementation decisions](../spec.md#implementation-decisions): positive precise amounts, Expense-only Categories, calendar-month limits, and no rollover.
- [Testing decisions](../spec.md#testing-decisions): repository coverage for monthly calculations and persistence, and Compose coverage for Budget navigation and display.

## Cross-screen acceptance test

[`TransactionFlowTest.createBudgetAndSeeExpenseProgressOnBudgetsAndOverview`](../../../app/src/androidTest/java/com/toshlol/personalfinance/TransactionFlowTest.kt) creates a current-month Food Expense of EUR 12.50 and a EUR 20.00 Budget. It asserts Spent EUR 12.50, Limit EUR 20.00, and Remaining EUR 7.50 in Budgets, then the same spending, limit, and remaining amount in Overview. After editing the Expense to EUR 25.00, it asserts the updated spending and limit on both screens, Over budget EUR 5.00 in Budgets, and the Over budget state in Overview. Deleting the Expense restores spending to EUR 0.00 and remaining to EUR 20.00 on both screens.

## Answer

Implemented monthly Category Budgets with precise minor-unit values, Room persistence and a database version 4 to 5 migration, monthly Expense-only calculations, duplicate prevention, archived Category history, and a dedicated Budgets screen linked from Overview. Current-month Budget progress also appears on Overview. Added Room behavior coverage and a Compose journey for creating a Budget and viewing over-budget progress.

Cross-screen acceptance coverage also verifies remaining funds and updates after editing and deleting an Expense. Transaction saves and deletions reload Budget calculations before publishing the updated screen state.

Verification (2026-11-13): all 9 JVM tests and 21 Android 10 tests passed, along with Android lint and ktlint. The extended cross-screen Budget journey also passed in isolation. One focused run encountered a transient Add category setup failure; the focused retry and full suite passed. Failure evidence and verification logs are preserved under `build/retro/`.
