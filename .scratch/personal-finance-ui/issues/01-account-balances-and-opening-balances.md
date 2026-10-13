# 01: Show Account balances and manage Opening balances

**What to build:** A user can see each Account's signed current balance and history, including money already held before the recorded ledger begins.

**Blocked by:** Personal Finance 01: Launch app and create the first Account; Personal Finance 02: Record and review Transactions; Personal Finance 03: Organize Transactions with Categories and Tags

**Status:** ready-for-agent

- [ ] Each Account has a signed Opening balance, defaulting to zero, and an as-of date, defaulting to today.
- [ ] The Opening balance represents the Account amount at the start of the as-of date; only Income, Expense, and Transfer movements dated on or after that date affect the current balance.
- [ ] Income increases the balance, Expense decreases it, and later Transfers decrease the source and increase the destination.
- [ ] Positive, zero, and negative balances are allowed for every Account type; the UI displays the sign clearly.
- [ ] A user can edit an Account's Opening balance and as-of date, and the displayed balance recalculates.
- [ ] The Accounts destination shows Account names, types, and current balances; each Account's history shows the movements that affect it.
- [ ] Archived Accounts retain their displayed balance and history and cannot receive new movements.
- [ ] Tests verify balance calculations, as-of-day boundaries, editing, persistence, and the Account list/history journey through public behavior seams.
