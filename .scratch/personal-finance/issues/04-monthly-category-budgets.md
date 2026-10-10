# 04: Set monthly Category Budgets

**What to build:** A user can create monthly spending limits for Categories and see whether spending is within or beyond each limit.

**Blocked by:** 03: Organize Transactions with Categories and Tags

**Status:** ready-for-agent

- [ ] A Budget belongs to one Category and one calendar month.
- [ ] Only one active Budget can exist for a Category in a month.
- [ ] Budget amounts must be greater than zero and use precise currency values.
- [ ] The app shows actual spending, remaining amount, and over-budget state.
- [ ] Existing Budgets for archived Categories remain visible, while new Budgets cannot use archived Categories.
- [ ] Budgets do not roll over and do not represent income limits.
- [ ] Tests cover monthly boundaries, calculations, uniqueness, archived Categories, persistence, and the critical Compose flow.
