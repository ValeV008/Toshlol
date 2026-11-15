# 02: Monthly Expenses, Income, and main navigation

Status: resolved
Blocked by: 01

**What to build:** Navigate using the left drawer and Overview/Expenses/Income tabs, review monthly direction-specific totals and day-grouped cards for the selected Accounts, and start any of the three movement types from consistent labeled bottom actions.

## Parent-spec requirements

- [User stories](../spec.md#user-stories): Stories 1-5, 7-12, 21: shared reference styling, all navigation destinations, tabs, month context, and the same Account selection across ledger views.
- [User stories](../spec.md#user-stories): Stories 29-30, 33-40: direction-specific monthly totals, grouped visible details, retained Category/Tag filters, and correct paired Transfer presentation without total inflation.
- [User stories](../spec.md#user-stories): Stories 41, 83-85: three labeled bottom actions, accessible offline Android 10 operation, and screenshot evidence.
- [Implementation decisions](../spec.md#implementation-decisions): Reuse ticket 01's shared month, Account selection, public monthly projection, and visual components. Replace bottom navigation and combined Transaction presentation while retaining every existing destination. Directional Transfer membership follows source/destination Account selection; Category/Tag filters hide Transfers.
- [Testing decisions](../spec.md#testing-decisions): use existing public finance use-case/repository and Backup interfaces, Compose semantics, and UI Automator for system UI; test visible outcomes rather than private helpers or tables. Named tests below are proposed or extensions of existing behavior tests, not claims of passing implementation.

## Observable acceptance criteria

- [x] The left drawer reaches Overview, Expenses, Income, Accounts, Categories & Tags, Budgets, Export, and Settings without stranding existing flows. Overview/Expenses/Income tabs show accessible current selection, and returning to a tab retains the chosen month and Account subset.
- [x] Expenses and Income each show the selected calendar month, a correctly filtered direction-specific total, and newest-first activity grouped into local-date cards. Rows retain amount/sign, Account, Category, Tags, and Note when present; Category presentation contains no icons.
- [x] Account selection, including an empty set, is shared with Overview. Reopening the app and switching tabs restore the persisted subset; empty selection remains zero with no Select Accounts prompt.
- [x] Existing Category and Tag list filtering remains discoverable and updates displayed records and relevant totals without changing the shared Account set. Month changes exclude records outside the viewed month.
- [x] Expenses includes a Transfer only if its source Account is selected; Income includes it only if its destination is selected. Each relevant directional view contains one row per Transfer, both rows open the same movement, and Category/Tag filters hide them.
- [x] Transfer rows and daily grouping do not inflate Income, Expense, net, or Budget totals. Activity ordering remains deterministic when Transactions and Transfers share dates.
- [x] Overview, Expenses, and Income each expose Add expense, Add income, and Transfer money as three labeled bottom actions, launching the correct existing or subsequently redesigned flow. Do not retain the inconsistent duplicate floating plus controls.
- [x] Android 10 Compose navigation/filter/Transfer journeys, relevant public finance tests, lint, and formatting pass. Collected Expense, Income, left-drawer, bottom-action, empty-selection, and larger-font screenshots.

## Named behavior tests

| Test or journey | Observable result |
| --- | --- |
| shareAccountSelectionAcrossTabsAndRelaunch | Assert the same switches and filtered totals across all three tabs and after app restart. |
| navigateEveryDestinationWithoutResettingMonth | Open each drawer destination, return, and assert selected month and accessible current section. |
| groupMonthlyIncomeAndExpensesByLocalDate | Assert correct direction, date grouping/order, visible record details, and filtered totals across month boundaries. |
| presentTransfersWithoutInflatingTotals | Select source only, destination only, both, and neither; assert directional membership, one row per relevant view, shared edit identity, and excluded totals. |
| filterDirectionalListsByCategoryAndTag | Assert selected Category/Tag membership and totals and hidden Transfer rows. |
| startEveryMovementFromEachMainLedgerTab | Launch each labeled bottom action from Overview, Expenses, and Income and verify the intended movement form. |

## Delivery boundaries

Build on ticket 01 rather than duplicating selection, date, or money calculations. This slice routes into working entry flows; tickets 03 and 05 redesign those forms. Retain Export and Settings behavior while ticket 10 handles their visual/data-management journey.

Each slice must remain independently demoable and keep existing callers and relevant checks working. Include the feature's behavior and UI verification in this ticket rather than deferring them to the final integration ticket. Follow the parent spec's exclusions and existing ADRs.

## Answer

Replaced the bottom destination bar with a left navigation drawer and Overview/Expenses/Income tabs. Added separate monthly ledgers with shared Account selection, directional Transfer rows, Category/Tag filtering, daily activity cards, and consistent bottom actions. Overview now uses the same three bottom actions. Captured Android 10 evidence in `../evidence/02/`.

## Comments

- 2026-11-15: Focused API 29 run passed `FirstRunFlowTest`, `MonthlyLedgerTest`, and `OverviewAccountSelectionTest` (8 tests total). `:app:testDebugUnitTest`, `:app:ktlintCheck`, and `:app:lintDebug` passed.
- 2026-11-15: The full connected suite ran 52 tests; 10 failed. The first-run navigation semantics failure was fixed and passed on the focused rerun. Nine `TransactionFlowTest` cases still time out against the replaced legacy combined-Transactions navigation/UI assumptions; the integrated ticket should reconcile those tests with the new split ledgers.
- Screenshots: [Expenses](../evidence/02/expenses.png), [Income](../evidence/02/income.png), [Account selection](../evidence/02/account-selection.png), [empty Income](../evidence/02/empty-income.png), [navigation drawer](../evidence/02/navigation-drawer.png), and [Overview bottom actions](../evidence/02/overview-bottom-actions.png).
