# 06: Named recurring Budgets with selected Accounts

Status: ready-for-agent
Blocked by: 01

**What to build:** Manually create and edit named recurring Budgets for explicit selected Accounts, review independent monthly progress on Budgets and Overview, and preserve them through upgrade, restart, and Backup export/import. The initial custom Budget covers all Expense classifications in its selected Accounts; ticket 07 adds the optional Category/Tag restrictions.

## Parent-spec requirements

- [User stories](../spec.md#user-stories): Stories 1-6, 9-12, 31-32, 83-85: shared styling/month context, understandable progress on Budgets and Overview, independent scope, and tested accessible Android 10 behavior.
- [User stories](../spec.md#user-stories): Stories 39, 56-60, 63-64, 67-74: manual named positive-limit Budgets with nonempty saved Account sets, initially unrestricted classifications, permitted overlap, monthly recurrence/reset, and edits affecting all months.
- [User stories](../spec.md#user-stories): Stories 75-80: historical references, lossless upgrades, complete versioned Budget Backup records, legacy compatibility, validation, and atomic Merge/Overwrite.
- [Implementation decisions](../spec.md#implementation-decisions): Use a stable recurring definition instead of one Category/month record or monthly background duplication. Budget Account scopes are explicit, independent of the drawer, and do not auto-expand. Use tested Room migrations and update the public Budget and Backup paths together; keep application snapshots and import atomicity under the existing ADRs.
- [Testing decisions](../spec.md#testing-decisions): use existing public finance use-case/repository and Backup interfaces, Compose semantics, and UI Automator for system UI; test visible outcomes rather than private helpers or tables. Named tests below are proposed or extensions of existing behavior tests, not claims of passing implementation.

## Observable acceptance criteria

- [ ] The shared-style Budgets destination allows manual creation and editing of a name, positive precise limit, and at least one selected Account. Save/reopen preserves the complete definition and selections. Empty Account scope or invalid limit fails without mutation; the drawer selection is not silently substituted.
- [ ] A manually created definition appears for every viewed calendar month and uses only that month's matching Expenses from its saved Accounts. Display name, Account scope, spent, limit, remaining, and over-budget state on Budgets and Overview using the shared selected month.
- [ ] Editing name, limit, or Accounts preserves stable identity/creation time, updates modification time, and recalculates past, present, and future views. No unused amount or overspending carries forward; recurrence neither rewrites Transactions nor creates a new definition each month.
- [ ] Different named definitions can overlap, including scopes containing the same Accounts, and track progress independently. There is no old Category/month uniqueness restriction. Initially no Category/Tag restriction means all Expenses, including unclassified/untagged Expenses, in selected Accounts.
- [ ] Income and Transfers do not count as spending. Changing or emptying the main drawer selection does not change Budget progress. Creating a new Account does not add it to saved Budget scopes. Renamed/archived references remain readable without silently broadening or clearing the saved scope.
- [ ] Before schema/data mutation, document a deterministic lossless legacy Budget mapping preserving original IDs, Category, original-month provenance, amount, timestamps, and finance history. The parent spec has not selected missing Account scopes: resolve any remaining product-dependent mapping decision explicitly instead of guessing, collapsing records, or discarding data.
- [ ] Implement the reviewed mapping with explicit tested Room migrations and compatible public callers. Existing Accounts, Transactions, Transfers, classifications, and settings remain intact across upgrade/reopen, including distinct legacy Budgets sharing a Category. Use an additive transition if required to keep the intermediate app and tests working.
- [ ] Export and import already round-trip the new Budget name/limit/Account definition with stable references and timestamps; do not defer this first working Backup path to ticket 10. Previously supported Backups remain importable under the documented mapping.
- [ ] Merge reconciles a complete Budget record idempotently by stable ID/update time without a stale Account-set union; equal-timestamp divergence, invalid references, and unsupported/currency-conflicting input are rejected under the existing rules. Overwrite, cancellation, and failed commits preserve atomicity.
- [ ] Run public Budget calculation/validation tests, repository migration/reopen tests, Backup compatibility/atomicity tests, Android 10 create/edit/month/progress journeys, lint, and formatting. Capture Budget editor/progress and independent Overview examples.

## Named behavior tests

| Test or journey | Observable result |
| --- | --- |
| createAndPersistNamedAccountScopedBudget | Create a named positive-limit Budget with chosen Accounts; reopen and assert complete definition, explicit scope, and invalid empty-account/nonpositive-limit rejection. |
| repeatBudgetMonthlyAndRecalculateHistory | Compare months with different or zero spending, edit limit/Accounts, and assert past/current/future recalculation, stable identity, no carryover, and no duplicated definitions. |
| keepBudgetScopesIndependentAndAllowOverlap | Create overlapping scopes; assert independent progress, unchanged results after drawer changes, and exclusion of newly created Accounts. |
| excludeIncomeAndTransfersFromRecurringBudget | Assert Expense-only spending, correct remaining/over-budget values, and the same result on Overview and Budgets. |
| migrateAndRestoreBudgetsWithoutDataLoss | Exercise the reviewed legacy mapping through public repository/Backup reads, preserving old identity/provenance/amounts and unrelated finance data across upgrade/reopen. |
| roundTripNamedBudgetAccountScope | Export/import/reopen and compare complete new Budget definitions and computed monthly progress. |
| reconcileBudgetAsOneAtomicBackupRecord | Assert newer complete-record replacement, idempotency, no stale Account union, conflict/reference rejection, and rollback/cancellation. |
| operateNamedBudgetEditorAtLargeFont | Assert labeled Account selections, name/limit validation, monthly progress, and readable independent scope. |

## Delivery boundaries

This is an end-to-end unclassified spending Budget slice, not a schema-only ticket. Optional Category/Tag selection arrives in ticket 07; retain existing legacy Category data through the reviewed mapping. Do not invent Budget deletion/stopping/archival behavior, name uniqueness, effective-dated revisions, destructive migrations, or automatic scopes. Legacy compatibility is a required delivery gate within this ticket, not a silently resolved product decision.

Each slice must remain independently demoable and keep existing callers and relevant checks working. Include the feature's behavior and UI verification in this ticket rather than deferring them to the final integration ticket. Follow the parent spec's exclusions and existing ADRs.
