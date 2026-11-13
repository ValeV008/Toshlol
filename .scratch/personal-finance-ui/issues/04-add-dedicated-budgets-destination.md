# 04: Add a dedicated Budgets destination

**What to build:** A user can navigate directly to monthly spending limits for Expense Categories and understand each Budget's progress.

**Blocked by:** Personal Finance UI 03: Scope Categories and Tag associations by Transaction direction; Personal Finance 04: Set monthly Category Budgets; Personal Finance 05: Filter and navigate the ledger

**Status:** resolved

## Parent-spec requirements

- [User stories 3–5 and 51–56](../spec.md#user-stories): Budgets has a dedicated main-navigation destination, opens on the current calendar month, lets the user browse other months, and shows each monthly Budget's progress. `TransactionFlowTest.navigateBudgetMonthsAndOfferOnlyExpenseCategories` verifies the destination, current month, and previous/next month behavior.
- [User stories 51–56](../spec.md#user-stories): Budgets can be created only for Expense Categories, one per Category and month; actuals include only Expense Transactions; archived Category Budgets remain visible but archived Categories are unavailable for new Budgets. `AppDatabaseTest.calculateMonthlyBudgetFromExpenseTransactionsAndKeepArchivedHistory` verifies calculations, uniqueness through updating the same Category/month, and archived behavior; `TransactionFlowTest.transferDoesNotContributeToBudgetActualSpending` verifies Transfers are excluded.
- [Implementation decisions](../spec.md#implementation-decisions): Budget amounts use precise minor units, apply to a calendar month without rollover, and exclude Transfers. Repository and Compose behavior is covered by the tests named above.
- [Testing decisions](../spec.md#testing-decisions): verify Budget rules through repository operations and the critical navigation, month-selection, creation, and progress journeys through accessible Compose semantics.

## Observable acceptance criteria

- [x] Main navigation exposes a dedicated Budgets destination; `navigateFilterTransactionsAndQuickAddFromEveryMainArea` verifies it can be opened.
- [x] Budgets opens on the current calendar month and previous/next controls change the selected month and visible Budgets; `navigateBudgetMonthsAndOfferOnlyExpenseCategories` verifies both directions.
- [x] Budget creation offers Expense Categories only, and repository validation rejects archived or non-Expense Categories; `navigateBudgetMonthsAndOfferOnlyExpenseCategories` and `calculateMonthlyBudgetFromExpenseTransactionsAndKeepArchivedHistory` verify eligibility.
- [x] Each Category has at most one Budget for a calendar month; saving the same Category/month updates its limit, covered by `calculateMonthlyBudgetFromExpenseTransactionsAndKeepArchivedHistory`.
- [x] Progress shows actual Expense spending, remaining amount, and over-budget state; Transfers and Income do not contribute, covered by `createBudgetAndSeeExpenseProgressOnBudgetsAndOverview`, `calculateMonthlyBudgetFromExpenseTransactionsAndKeepArchivedHistory`, and `transferDoesNotContributeToBudgetActualSpending`.
- [x] Historical Budgets remain visible for archived Categories, which cannot receive new Budgets; covered by `calculateMonthlyBudgetFromExpenseTransactionsAndKeepArchivedHistory`.
- [x] Compose coverage verifies navigation, month selection, Expense Category eligibility, and the critical Budget progress journey through accessible semantics.

- [x] The main navigation includes a dedicated Budgets destination.
- [x] The Budgets screen shows monthly Budgets for the selected calendar month and allows the user to change the month.
- [x] A Budget can be created only for an Expense Category, with at most one active Budget per Category per calendar month.
- [x] Budget progress shows actual Expense spending, remaining amount, and over-budget state; Transfers and Income do not count as spending.
- [x] Historical Budgets for archived Categories remain visible, while archived Categories cannot receive new Budgets.
- [x] Tests verify navigation, Expense Category eligibility, monthly calculations, archived-category behavior, and the critical Compose journey through public behavior seams.

## Answer

Added accessible previous/next month controls to the Budgets destination, keeping direct `YYYY-MM` entry available. Budget creation choices carry accessible test semantics, and Expense-only eligibility, monthly calculations, archived history, uniqueness, and Transfer exclusion are verified through repository and Compose journeys.

Verification (2026-11-13): all 12 JVM tests and 35 Android 10 instrumentation tests passed. Android lint and ktlint passed. The focused Android 10 month-navigation and category-eligibility journey passed before the full suite.
