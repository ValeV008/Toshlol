# 07: Category and Tag restrictions for Budgets

Status: ready-for-agent
Blocked by: 06

**What to build:** Choose optional multiple Expense Categories and Tags in each named Budget and see precisely matched monthly spending on Budgets and Overview. Persist, edit, migrate, export, and import complete restrictions without losing overlap, recurrence, or historical recalculation.

## Parent-spec requirements

- [User stories](../spec.md#user-stories): Stories 61-68: multiple optional Category/Tag selections, unrestricted empty groups, any match within groups, all active groups together, once-per-Budget counting, and independent overlapping limits.
- [User stories](../spec.md#user-stories): Stories 31-32, 39, 56-60, 69-75: independently saved Account scopes, Expense-only monthly recurrence, all-month edits, progress, and preserved renamed/archived references.
- [User stories](../spec.md#user-stories): Stories 1-6, 76-80, 83-85: integrated visual presentation, complete Backup/migration compatibility, atomicity, and accessible offline Android 10 journeys.
- [Implementation decisions](../spec.md#implementation-decisions): Extend ticket 06's recurring definition and complete-record Backup behavior. Account set is required; Category and Tag sets are optional independent restrictions. Use stable references and the same matching/projection behavior on Overview, Budgets, and restored datasets instead of per-screen filters.
- [Testing decisions](../spec.md#testing-decisions): use existing public finance use-case/repository and Backup interfaces, Compose semantics, and UI Automator for system UI; test visible outcomes rather than private helpers or tables. Named tests below are proposed or extensions of existing behavior tests, not claims of passing implementation.

## Observable acceptance criteria

- [ ] The Budget editor offers multiple Expense Category and multiple Tag choices with accessible selected states and readable scope summaries. Category choices are text-only; references must exist, and Income Categories cannot be used as Expense filters.
- [ ] An Expense matches if its Account is selected, its Category matches any selected Category when that set is nonempty, and it has at least one selected Tag when the Tag set is nonempty. All active groups must match, and multiple matching Tags count the Expense only once.
- [ ] Empty Categories means no Category restriction; empty Tags means no Tag restriction. Category-only, Tag-only, overall, and combined restrictions work, including unclassified/untagged Expenses where groups are unrestricted. Empty Account scope remains invalid.
- [ ] For Account A, Categories Groceries/Dining and Tags weekend/holiday, include amounts 20 for Groceries/weekend, 30 for Dining/holiday, and 10 for Groceries/both Tags once, for spent 60. Exclude other Accounts, mismatched Category, missing required Tag, other months, Income, and Transfers; at limit 50 remaining is -10 and over-budget is true.
- [ ] Overlap remains allowed: one Expense can contribute independently to two matching Budgets. Drawer changes do not modify saved scopes or progress; newly created Accounts do not join them.
- [ ] Editing any restriction persists complete replacement selections, preserves Budget identity, and recalculates past/current/future months on both Budget and Overview views. Switching months carries no spending/remaining balance forward and does not duplicate definitions.
- [ ] Renaming selected entities updates displayed scope names by identity. Archived selections/history remain intelligible; do not silently remove a selected Category/Tag and thereby broaden the Budget. Preserve Transaction-specific classification restrictions without assigning Budget filters to Transactions.
- [ ] Public repository reads after migration/reopen and versioned Backup round-trips preserve the full name/limit/Account/Category/Tag definition. Budgets created by ticket 06 remain unrestricted rather than becoming invalid when optional groups are introduced.
- [ ] Backup validation rejects invalid/dangling/incorrect-direction references before mutation. Merge replaces the complete newer definition and all filter sets without stale union or partial membership; equal-timestamp conflicts, idempotency, Overwrite, cancellation, rollback, and older supported Backup mappings retain their guarantees.
- [ ] Run public matching/recurrence/overlap tests, Room migration/reopen tests, complete-record Backup tests, and Android 10 create/edit/review journeys, plus lint and formatting. Capture Category-only, Tag-only, combined, and over-budget examples with larger-font coverage.

## Named behavior tests

| Test or journey | Observable result |
| --- | --- |
| matchBudgetAcrossAccountCategoryAndTagSelections | Assert the 60-spent/50-limit fixture, all positive and negative matches, and one count despite two selected matching Tags. |
| supportUnrestrictedBudgetGroups | Assert Category-only, Tag-only, unrestricted, untagged/uncategorized cases and invalid empty Account set. |
| keepBudgetScopesIndependentAndAllowOverlap | Assert independent overlapping progress, unchanged results under drawer selection changes, and no auto-added Accounts. |
| editBudgetFiltersRecalculatesEveryViewedMonth | Replace Categories/Tags/Accounts and assert persisted all-month results on Overview and Budgets with stable identity. |
| preserveFiltersThroughRenamesAndArchives | Assert renamed scope labels and preserved archived restrictions rather than silent broadening. |
| roundTripCompleteBudgetRestrictions | Migrate/reopen and export/import complete filters while keeping earlier unrestricted recurring definitions valid. |
| reconcileBudgetAsOneAtomicBackupRecord | Assert complete-record membership replacement, conflict/reference rejection, idempotency, atomic Overwrite, cancellation, and rollback. |
| operateBudgetRestrictionsAtLargeFont | Exercise accessible multiple choices, empty-group meaning, scope summaries, and over-budget progress. |

## Delivery boundaries

Deliver the optional restrictions through UI, public behavior, persistence, migrations, and Backups in this slice; ticket 10 styles the document-management journey rather than supplying missing serialization. Do not impose one Category per Budget, require every selected Tag, double-count an Expense, or add unagreed Budget lifecycle behavior.

Each slice must remain independently demoable and keep existing callers and relevant checks working. Include the feature's behavior and UI verification in this ticket rather than deferring them to the final integration ticket. Follow the parent spec's exclusions and existing ADRs.
