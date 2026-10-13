# Personal Finance

This glossary defines the language for the personal finance ledger, budgeting, and backup workflows.

## Ledger

**Transaction**:
A recorded income or expense with a positive amount, a date, and a mandatory Account; Category and Tags are optional.
_Avoid_: Entry, record

**Note**:
Optional free text that preserves context about a Transaction.
_Avoid_: Description

**Transfer**:
The movement of a positive amount between two different Accounts owned by the same person; it is neither income nor expense.
_Avoid_: Internal transaction

**Account**:
A user-created place to hold money, such as a bank account, cash holding, or credit card, that a Transaction belongs to or a Transfer moves money between.
_Avoid_: Wallet, balance

**Opening balance**:
The signed amount in an Account at the start of a chosen date, before that date's recorded movements.
_Avoid_: Starting amount

**Category**:
A user-created classification for either Income or Expense Transactions, never both; Expense Categories may also have monthly Budgets. Categories are optional on Transactions and may be archived.
_Avoid_: Type, bucket

**Tag**:
A reusable label for Transactions; a Tag associated with a Category can only be assigned to Transactions using that Category.
_Avoid_: Label, keyword

**Budget**:
A monthly spending limit for one Expense Category.
_Avoid_: Goal, allowance

## Backup

**Backup**:
A versioned JSON export containing the local finance dataset and settings.
_Avoid_: Sync, cloud copy

**Merge**:
An import operation that adds missing records and applies newer updates without propagating deletions.
_Avoid_: Restore

**Overwrite**:
An import operation that replaces the complete local dataset with the validated backup.
_Avoid_: Reset, merge

**Archive**:
The lifecycle state that prevents an Account, Category, or Tag from new use while preserving its historical references and displayed name; archived Accounts retain their final balance and history.
_Avoid_: Delete, remove
