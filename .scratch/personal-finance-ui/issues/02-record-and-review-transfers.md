# 02: Record and review Transfers

**What to build:** A user can record one Transfer between two different Accounts and find it consistently in the relevant Account and ledger views without counting it as Income or Expense.

**Blocked by:** Personal Finance UI 01: Show Account balances and manage Opening balances; Personal Finance 02: Record and review Transactions; Personal Finance 05: Filter and navigate the ledger

**Status:** ready-for-agent

- [ ] Quick-add offers Transfer as a third movement flow.
- [ ] A Transfer requires a positive amount, editable local calendar date defaulting to today, and explicitly selected, distinct source and destination Accounts.
- [ ] A Transfer has no Category, Tags, or Note and is allowed to overdraw its source Account.
- [ ] Saving, editing, or deleting a Transfer updates both Account balances and both Account histories as one movement.
- [ ] The Expense list shows the outgoing side and the Income list shows the incoming side; both rows open the same Transfer.
- [ ] Overview recent activity includes Transfers. Transfer rows are labeled as Transfers and excluded from Income totals, Expense totals, monthly net, and Budget actual spending.
- [ ] Month and relevant Account filters include Transfer rows; Category and Tag filters hide them.
- [ ] Tests verify validation, both balance effects, paired views, filtering, totals, editing/deletion consistency, persistence, and the critical Compose journey through public behavior seams.
