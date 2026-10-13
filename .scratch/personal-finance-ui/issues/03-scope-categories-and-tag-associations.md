# 03: Scope Categories and Tag associations by Transaction direction

**What to build:** A user can classify Income and Expense Transactions without applying a Category or Category-associated Tag to the wrong kind of Transaction.

**Blocked by:** Personal Finance 02: Record and review Transactions; Personal Finance 03: Organize Transactions with Categories and Tags

**Status:** ready-for-agent

- [ ] Income and Expense Categories are separate sets; a Category belongs to only one direction.
- [ ] A Transaction can use only a Category belonging to its Income or Expense direction; Category remains optional.
- [ ] A Category-associated Tag can be assigned only when its associated Category is selected on the Transaction; unassociated Tags remain available to either direction.
- [ ] When a Transaction's Category changes or is cleared, Tags associated with the previous Category are removed automatically without a notice.
- [ ] Historical Transactions retain their Category and Tag references and display renamed entity names correctly.
- [ ] Tests verify direction validation, optional Categories, Tag association rules, automatic Tag removal, persistence, and the relevant entry/edit journeys through public behavior seams.
