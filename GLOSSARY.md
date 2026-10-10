# Personal Finance

This glossary defines the language for the personal finance ledger, budgeting, and backup workflows.

## Ledger

**Transaction**:
A recorded income or expense with a positive amount, a date, and a mandatory Account; Category and Tags are optional.
_Avoid_: Entry, record

**Account**:
A user-created source or destination for money, such as a bank account, cash holding, or credit card, that every Transaction belongs to.
_Avoid_: Wallet, balance

**Category**:
A user-created classification for Transactions and monthly Budgets. Categories are optional on Transactions and may be archived.
_Avoid_: Type, bucket

**Tag**:
A reusable label that can be assigned to Transactions and can optionally be associated with one Category.
_Avoid_: Label, keyword

**Budget**:
A monthly spending limit for one Category.
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
The lifecycle state that prevents an Account, Category, or Tag from new use while preserving its historical references and displayed name.
_Avoid_: Delete, remove
