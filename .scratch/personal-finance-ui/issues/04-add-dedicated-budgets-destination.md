# 04: Add a dedicated Budgets destination

**What to build:** A user can navigate directly to monthly spending limits for Expense Categories and understand each Budget's progress.

**Blocked by:** Personal Finance UI 03: Scope Categories and Tag associations by Transaction direction; Personal Finance 04: Set monthly Category Budgets; Personal Finance 05: Filter and navigate the ledger

**Status:** ready-for-agent

- [ ] The main navigation includes a dedicated Budgets destination.
- [ ] The Budgets screen shows monthly Budgets for the selected calendar month and allows the user to change the month.
- [ ] A Budget can be created only for an Expense Category, with at most one active Budget per Category per calendar month.
- [ ] Budget progress shows actual Expense spending, remaining amount, and over-budget state; Transfers and Income do not count as spending.
- [ ] Historical Budgets for archived Categories remain visible, while archived Categories cannot receive new Budgets.
- [ ] Tests verify navigation, Expense Category eligibility, monthly calculations, archived-category behavior, and the critical Compose journey through public behavior seams.
