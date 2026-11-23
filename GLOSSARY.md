# Personal Finance

This glossary defines the language for the personal finance ledger, budgeting, and backup workflows.

## Ledger

**Transaction**:
A recorded income or expense with a positive amount, a date, and a mandatory Account; Category and Tags are optional. Its Income or Expense direction is fixed, and its amount can be allocated across Transaction split parts.
_Avoid_: Entry, record

**Transaction split**:
The allocation of one Transaction into two or more related Transaction split parts that share an Account, date, and Income or Expense direction. Creating a Transaction split preserves the original amount; its current total is the sum of its parts.
_Avoid_: Shared bill, transfer

**Transaction split part**:
A separately displayed portion of a Transaction split with its own amount and optional Category, Tags, and Note. Its amount and classification can change independently, Account and date remain shared, and its Income or Expense direction is fixed.
_Avoid_: Separate payment

**Unsplit**:
The recombination of all parts of a Transaction split into one Transaction for their current combined amount, with a chosen Category, Tags, and Note.
_Avoid_: Refund, reversal

**Note**:
Optional free text that preserves context about a Transaction or Transaction split part.
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

**Closing balance**:
The signed amount in an Account at the end of a selected calendar month, including its Opening balance and all applicable movements through that month's last day.
_Avoid_: Monthly net

**Category**:
A user-created classification for either Income or Expense Transactions and their Transaction split parts, never both; Expense Categories may also have monthly Budgets. Categories are optional and may be archived.
_Avoid_: Type, bucket

**Tag**:
A reusable label for Transactions or Transaction split parts; a Tag associated with a Category can only be assigned where that Category is used.
_Avoid_: Label, keyword

**Budget**:
A named, user-created spending limit for a nonempty selection of Accounts, optionally restricted by Expense Categories and Tags, that repeats each calendar month. Spending is counted separately for each month, with no unused amount or overspending carried over. Budgets may overlap and track their limits independently.
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
A soft-deletion state that prevents an Account, Category, or Tag from new use while preserving its historical references and displayed name. Archived Accounts leave the main Account selection but retain read-only history and existing Budget references.
_Avoid_: Permanent deletion
