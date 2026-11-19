# 07: Category and Tag restrictions for Budgets

Status: resolved
Blocked by: 06

**What to build:** Choose optional multiple Expense Categories and Tags in each named Budget and see precisely matched monthly spending on Budgets and Overview. Persist, edit, migrate, export, and import complete restrictions without losing overlap, recurrence, or historical recalculation.

## Parent-spec requirements

- [User stories](../spec.md#user-stories): Stories 61-68: multiple optional Category/Tag selections, unrestricted empty groups, any match within groups, all active groups together, once-per-Budget counting, and independent overlapping limits.
- [User stories](../spec.md#user-stories): Stories 31-32, 39, 56-60, 69-75: independently saved Account scopes, Expense-only monthly recurrence, all-month edits, progress, and preserved renamed/archived references.
- [User stories](../spec.md#user-stories): Stories 1-6, 76-80, 83-85: integrated visual presentation, complete Backup/migration compatibility, atomicity, and accessible offline Android 10 journeys.
- [Implementation decisions](../spec.md#implementation-decisions): Extend ticket 06's recurring definition and complete-record Backup behavior. Account set is required; Category and Tag sets are optional independent restrictions. Use stable references and the same matching/projection behavior on Overview, Budgets, and restored datasets instead of per-screen filters.
- [Testing decisions](../spec.md#testing-decisions): use existing public finance use-case/repository and Backup interfaces, Compose semantics, and UI Automator for system UI; test visible outcomes rather than private helpers or tables. Named tests below are proposed or extensions of existing behavior tests, not claims of passing implementation.

## Observable acceptance criteria

- [x] The Budget editor offers multiple Expense Category and multiple Tag choices with accessible selected states and readable scope summaries. Category choices are text-only; references must exist, and Income Categories cannot be used as Expense filters.
- [x] An Expense matches if its Account is selected, its Category matches any selected Category when that set is nonempty, and it has at least one selected Tag when the Tag set is nonempty. All active groups must match, and multiple matching Tags count the Expense only once.
- [x] Empty Categories means no Category restriction; empty Tags means no Tag restriction. Category-only, Tag-only, overall, and combined restrictions work, including unclassified/untagged Expenses where groups are unrestricted. Empty Account scope remains invalid.
- [x] For Account A, Categories Groceries/Dining and Tags weekend/holiday, include amounts 20 for Groceries/weekend, 30 for Dining/holiday, and 10 for Groceries/both Tags once, for spent 60. Exclude other Accounts, mismatched Category, missing required Tag, other months, Income, and Transfers; at limit 50 remaining is -10 and over-budget is true.
- [x] Overlap remains allowed: one Expense can contribute independently to two matching Budgets. Drawer changes do not modify saved scopes or progress; newly created Accounts do not join them.
- [x] Editing any restriction persists complete replacement selections, preserves Budget identity, and recalculates past/current/future months on both Budget and Overview views. Switching months carries no spending/remaining balance forward and does not duplicate definitions.
- [x] Renaming selected entities updates displayed scope names by identity. Archived selections/history remain intelligible; do not silently remove a selected Category/Tag and thereby broaden the Budget. Preserve Transaction-specific classification restrictions without assigning Budget filters to Transactions.
- [x] Public repository reads after migration/reopen and versioned Backup round-trips preserve the full name/limit/Account/Category/Tag definition. Budgets created by ticket 06 remain unrestricted rather than becoming invalid when optional groups are introduced.
- [x] Backup validation rejects invalid/dangling/incorrect-direction references before mutation. Merge replaces the complete newer definition and all filter sets without stale union or partial membership; equal-timestamp conflicts, idempotency, Overwrite, cancellation, rollback, and older supported Backup mappings retain their guarantees.
- [x] Run public matching/recurrence/overlap tests, Room migration/reopen tests, complete-record Backup tests, and Android 10 create/edit/review journeys, plus lint and formatting. Capture Category-only, Tag-only, combined, and over-budget examples with larger-font coverage.

## Named behavior tests

| Test or journey | Observable result |
| --- | --- |
| `RecurringBudgetMatchingTest.matchBudgetAcrossAccountCategoryAndTagSelections` | Verifies the 60-spent/50-limit fixture, positive and negative matches, and one count when two selected Tags match. |
| `RecurringBudgetMatchingTest.supportUnrestrictedBudgetGroups` | Verifies Category-only, Tag-only, unrestricted, untagged/uncategorized cases, and invalid empty Account scope. |
| `RecurringBudgetRepositoryTest.overlappingBudgetsCountExpensesIndependentlyAndIgnoreIncomeTransfersAndNewAccounts` | Verifies independent overlap, Expense-only counting, and no automatic inclusion of new Accounts. |
| `RecurringBudgetRepositoryTest.editCategoryAndTagFiltersRecalculatesPastCurrentAndFutureMonths` and `TransactionFlowTest.editBudgetCategoryAndTagFiltersRecalculatesPastCurrentAndFutureMonthsOnBothScreens` | Verify persisted replacement filters and recalculation across past/current/future months on repository and UI paths. |
| `RecurringBudgetRepositoryTest.preserveFiltersThroughRenamesAndArchives` and `BudgetAccessibilityTest.showsRenamedCategoryAndTagNamesForSavedBudget` | Verify stable identity and readable renamed/archived scope labels. |
| `RecurringBudgetRepositoryTest.preserveNamedBudgetRestrictionsAcrossDatabaseReopen`, `BudgetMigrationTest.migrateVersionEightBudgetKeepsEmptyOptionalGroupsUnrestrictedAfterReopen`, and `BudgetMigrationTest.migrateLegacyBudgetPreservesIdentityProvenanceAndAllExistingAccountsAcrossReopen` | Verify reopen persistence and migration compatibility, including older unrestricted definitions. |
| `BackupExportTest.exportWritesCompleteVersionedSnapshot`, `BackupImportTest.reconcileBudgetAsCompleteBackupRecordWithoutStaleAccountUnion`, `BackupImportTest.mergeIsIdempotentAndAppliesNewerWholeRecords`, `BackupImportTest.rejectsCurrencyTimestampAndFinalDatasetConflictsAtomically`, `BackupImportTest.invalidReferencesAndMalformedInputLeaveDataUnchanged`, `BackupImportTest.failedCommitRollsBackEveryEntityAndSetting`, `BackupImportTest.overwriteReplacesCompleteDatasetAndSettingsAndReportsInsertedCounts`, `BackupImportTest.canceledImportDoesNotMutateLedger`, `BudgetBackupCompatibilityTest.restoresVersionThreeBudgetsWithoutOptionalCategoryOrTagRestrictions`, `BudgetBackupCompatibilityTest.restoresVersionTwoBudgetsWithTheirOriginalAllAccountScope`, and `BudgetBackupCompatibilityTest.rejectIncomeCategoryAsExpenseBudgetFilterBeforeMutation` | Verify complete export/import, no stale scope union, idempotency, conflict/reference rejection, rollback, Overwrite/cancellation behavior, older v2/v3 compatibility, and rejection of an Income Category filter before mutation. |
| `BudgetAccessibilityTest.operateNamedBudgetEditorAtLargeFont` and `TransactionFlowTest.createBudgetAndSeeExpenseProgressOnBudgetsAndOverview` | Verify larger-font editor operation and visible Budget progress on Budgets and Overview. |

## Delivery boundaries

Deliver the optional restrictions through UI, public behavior, persistence, migrations, and Backups in this slice; ticket 10 styles the document-management journey rather than supplying missing serialization. Do not impose one Category per Budget, require every selected Tag, double-count an Expense, or add unagreed Budget lifecycle behavior.

Each slice must remain independently demoable and keep existing callers and relevant checks working. Include the feature's behavior and UI verification in this ticket rather than deferring them to the final integration ticket. Follow the parent spec's exclusions and existing ADRs.

## Answer

Implemented optional multi-select Expense Category and Tag restrictions on named recurring Budgets. Matching requires the selected Account plus a match in every nonempty filter group; selections are OR-ed within each group, and an Expense contributes once per Budget. The editor, Budget progress card, and Overview use the shared persisted definition and resolve renamed or archived selections by stable identity. Database migration and complete-record Backup export/import preserve the restrictions, while older v2/v3 Backups retain their original unrestricted mappings and invalid Income Category filters are rejected before mutation.

Verification completed on 2026-11-19 using the API 29 `xperia_xz2_compact(AVD) - 10` emulator:

- `python -m unittest discover -s scripts -p 'test_*.py'` — 15 passed.
- `.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:ktlintCheck :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` — passed; JVM report: 15 tests, 0 failures/errors/skips.
- `python scripts/verify_overview_persistence.py --serial emulator-5554 --skip-build` — selected and empty Account states passed. Evidence: `build/overview-persistence/20261119T111721533634Z/`.
- `.\gradlew.bat :app:connectedDebugAndroidTest --console=plain` — full unfiltered suite passed: 79 tests, 0 failures/errors/skips.
- Screenshots for Category-only, Tag-only, combined, and over-budget large-font states are in [`evidence/07/budget-restriction-evidence/`](evidence/07/budget-restriction-evidence/). Budget editor, progress, and Overview progress screenshots are in [`evidence/07/budget-evidence/`](evidence/07/budget-evidence/).
