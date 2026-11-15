# Personal Finance Design

Status: ready-for-agent

## Problem Statement

The app's core finance features are implemented, but the screens do not consistently express the supplied visual design. Bottom navigation, standard forms, a combined Transaction list, and inconsistent month handling make historical finances difficult to review and make the app feel different from the references.

The user needs to choose which Accounts contribute to the main financial view and retain that choice across sessions. Balances, monthly totals, and activity must describe the selected calendar month. The existing one-Category-per-month Budget model cannot express the intended named, recurring spending limits across chosen Accounts, Categories, and Tags.

## Solution

Apply the supplied light-theme design throughout the native Android app, including destinations without individual references. Use charcoal headers, yellow accents, pale gray backgrounds, rounded white cards, clear typography, and red/green Expense and Income amounts. Provide the reference navigation drawer, Overview/Expenses/Income tabs, monthly grouped activity cards, and full-screen Income/Expense entry with an amount keypad. Categories remain text-only.

A right-side drawer, opened by swiping inward from the right edge, lists Accounts with their month-end Closing balances and individual pill switches. Selected Accounts determine the Overview balance, Income, Expenses, net, and recent activity, and the same selection applies to the Expenses and Income tabs. Initially no Accounts are selected. Every subsequent selection, including an empty selection, survives app restarts. Empty selection produces zero ledger totals without a selection prompt.

Users manually create named Budgets with a positive limit, at least one selected Account, and optional multiple Category and Tag restrictions. Each Budget repeats monthly without carryover, uses its own saved Account selection independently of the drawer, and can overlap other Budgets. Edits apply to all months, including the past. Preserve the local-first ledger, Transfer distinctions, archived history, and validated Backup behavior while extending persistence and Backups for this Budget model.

## User Stories

1. As a user, I want the whole app to follow the supplied visual style, so that every destination feels consistent.
2. As a user, I want the interface to remain light-theme only, so that implementation matches the agreed references.
3. As a user, I want charcoal headers, yellow accents, pale gray backgrounds, and rounded white cards, so that important controls and figures are easy to scan.
4. As a user, I want readable typography and consistent amounts in my selected currency, so that financial values are understandable throughout the app.
5. As a user, I want direction labels, signs, and colors to distinguish Income and Expense, so that meaning does not depend on color alone.
6. As a user, I want Accounts, history, Transfers, Budgets, Categories & Tags, Settings, and setup styled consistently, so that secondary flows feel complete.
7. As a user, I want a left navigation drawer for Overview, Expenses, Income, Accounts, Categories & Tags, Budgets, Export, and Settings, so that all major tasks are discoverable.
8. As a user, I want Overview, Expenses, and Income tabs below the month header, so that I can change financial views quickly.
9. As a user, I want the current local calendar month selected initially, so that the starting period is relevant.
10. As a user, I want a readable month/year heading and previous/next controls, so that I can review historical or future recorded information.
11. As a user, I want navigation to retain the selected month, so that different screens describe the same period.
12. As a user, I want financial summaries and activity based on the selected month, so that historical reports do not mix old activity with today's balances.
13. As a user, I want to swipe inward from the right edge to open the Account drawer, so that I can change scope without adding a new header icon.
14. As a user, I want each drawer Account to show its name and Closing balance for the selected month, so that its contribution is clear.
15. As a user, I want an individual pill switch beside each Account, so that I can select several Accounts independently.
16. As a user, I want no Accounts selected initially, so that inclusion in my main financial view is deliberate.
17. As a user, I want Account selection preserved across app restarts, so that I return to the same scope.
18. As a user, I want an explicitly empty selection preserved across restarts, so that it is never interpreted as all Accounts.
19. As a user, I want zero ledger totals when no Accounts are selected, so that figures reflect my selection.
20. As a user, I want no Select Accounts prompt for an empty selection, so that the interface respects my chosen empty view.
21. As a user, I want Overview, Expenses, and Income to share Account selection, so that switching tabs does not change scope.
22. As a user, I want archived Accounts selectable and marked Archived, so that I can include their balances and historical activity.
23. As a user using accessibility services, I want accessible drawer open/close actions and labeled switches, so that an edge gesture is not the only usable interaction.
24. As a user, I want Overview's headline to sum selected Accounts' Closing balances, so that it represents their money at the selected month-end.
25. As a user, I want Closing balances to include signed Opening balances and applicable Income, Expenses, and Transfers through month-end, so that money from earlier months is retained correctly.
26. As a user, I want movements before an Account's Opening balance date excluded from balance calculation, so that the starting balance is not counted twice.
27. As a user, I want movements after the viewed month excluded from its Closing balance, so that later records do not alter earlier balances.
28. As a user, I want Account cards, drawer balances, Account lists, and history balances to use the same selected-month meaning, so that screens do not contradict one another.
29. As a user, I want Overview Income, Expenses, and net calculated from matching Transactions in the selected month and Accounts, so that flows remain distinct from balances.
30. As a user, I want recent activity limited to the selected month and Accounts and ordered newest first, so that it supports the displayed summary.
31. As a user, I want Overview Budget progress for the selected month, so that I can review limits alongside activity.
32. As a user, I want each Budget to retain its saved Account scope when I change the drawer selection, so that viewing preferences do not redefine Budgets.
33. As a user, I want separate monthly Expense and Income lists with direction-specific totals, so that each view is clear.
34. As a user, I want activity grouped by local calendar day in rounded cards, so that daily movements are easy to scan.
35. As a user, I want rows to expose amount, Account, and optional Category, Tags, and Note, so that I can identify a Transaction.
36. As a user, I want existing Category and Tag list filters retained, so that the redesign preserves investigation tools.
37. As a user, I want outgoing Transfers in Expenses when their source Account is selected and incoming Transfers in Income when their destination is selected, so that movements remain visible in context.
38. As a user, I want each Transfer shown once in Overview and once in each relevant directional list, so that its activity is not duplicated in one view.
39. As a user, I want Transfers excluded from Income, Expense, net, and Budget spending, so that internal movements do not inflate financial totals.
40. As a user, I want Category or Tag list filters to hide Transfers, so that filters do not imply unsupported classifications.
41. As a user, I want Add expense, Add income, and Transfer money as three labeled bottom actions on Overview, Expenses, and Income, so that all movement types are consistently accessible.
42. As a user, I want shared full-screen Income/Expense entry with direction selection, a prominent amount, optional Note, and keypad, so that entry is quick and familiar.
43. As a user, I want a positive amount, valid date, and mandatory active Account required for a Transaction, so that recorded movements remain valid.
44. As a user, I want explicit Account choices in Transaction entry, so that money is recorded against the intended Account.
45. As a user, I want the most recently used Account preselected across new Income and Expense entry, so that repeated entry takes fewer steps.
46. As a user, I want editing to load and preserve a Transaction's original Account unless I change it, so that another edit cannot silently move the Transaction.
47. As a user, I want the date to default to today's local date and remain editable, so that routine and backdated entry are supported.
48. As a user, I want the description field to represent the optional Note, so that there is only one context field.
49. As a user, I want Categories to remain optional, so that Pick a Category wording does not prevent saving an uncategorized Transaction.
50. As a user, I want text-only Category choices without icons or an icon picker, so that the app follows my chosen design.
51. As a user, I want direction-specific Categories and compatible optional Tags, so that changing direction or Category cannot leave invalid classifications.
52. As a user, I want existing edit, validation, and confirmation behavior preserved in the new forms, so that appearance changes do not reduce data safety.
53. As a user, I want Transfer entry to require explicit, distinct source and destination Accounts, so that direction is deliberate.
54. As a user, I want Transfers to retain amount/date details without Category, Tags, or Note, so that they remain distinct from Income and Expense.
55. As a user, I want archived Account history readable without prohibited edits, so that historical data remains trustworthy.
56. As a user, I want Budgets created manually, so that limits exist because I chose them rather than because a Category or month appeared.
57. As a user, I want a name for each Budget, so that I can distinguish limits such as Weekend spending and Household essentials.
58. As a user, I want a positive Budget limit in my selected currency, so that progress has a meaningful target.
59. As a user, I want at least one explicit Account selected for each Budget independently of the drawer, so that its saved scope is deliberate.
60. As a user, I want new Accounts excluded from existing Budgets until selected, so that Account creation cannot silently broaden spending scope.
61. As a user, I want multiple Expense Categories selectable for one Budget, so that a limit can cover related spending.
62. As a user, I want multiple Tags selectable for one Budget, so that a limit can cover cross-cutting contexts.
63. As a user, I want an empty Category selection to mean no Category restriction, so that Tag-only and overall spending Budgets are possible.
64. As a user, I want an empty Tag selection to mean no Tag restriction, so that untagged Expenses can count toward Category-only or overall Budgets.
65. As a user, I want any selected Category and any selected Tag to match within their groups while active groups apply together, so that a combined Budget narrows spending predictably.
66. As a user, I want an Expense counted once within a Budget even when it carries several matching Tags, so that tagging does not multiply spending.
67. As a user, I want overlapping Budgets to count the same Expense independently, so that one purchase can contribute to several useful limits.
68. As a user, I want the old one-Category-per-month uniqueness restriction removed, so that custom Budgets are not blocked by an existing limit.
69. As a user, I want a manually created Budget to repeat every calendar month with the same settings, so that I do not need to recreate it.
70. As a user, I want each month's spending calculated from that month's matching Expenses, so that a reset does not delete or rewrite Transactions.
71. As a user, I want neither unused amounts nor overspending carried forward, so that every month stands on its own.
72. As a user, I want to edit a Budget's name, limit, Accounts, Categories, and Tags, so that it can follow my needs.
73. As a user, I want Budget edits applied to past, present, and future months, so that every monthly view uses the updated definition.
74. As a user, I want Budget name, scope, spent amount, limit, remaining amount, and over-budget state displayed, so that progress is understandable.
75. As a user, I want renamed and archived references retained in history and Budget scopes, so that records remain intelligible.
76. As a user, I want the upgrade to preserve existing ledger data and Budgets, so that a design change does not discard financial data.
77. As a user, I want Backups to preserve custom Budget definitions, complete filter selections, IDs, and timestamps, so that importing restores the same limits and calculations.
78. As a user, I want older supported Backups to remain importable, so that the redesign does not strand earlier exports.
79. As a user, I want invalid Budget references and unsupported Backups rejected before mutation, so that importing cannot corrupt my ledger.
80. As a user, I want Merge, Overwrite, cancellation, and failures to retain atomic behavior, so that partial finance replacements are never visible.
81. As a new user, I want explicit currency and first-Account setup without seeded Accounts or Categories, so that the app begins with my own data.
82. As a user, I want Settings, Export, Import, and local reset retained with existing confirmations, so that data-management capabilities survive.
83. As a user, I want labeled controls, readable text, usable touch targets, and larger-font support, so that the app remains accessible.
84. As a user, I want these workflows offline on Android 10, so that the design adds no cloud dependency or device requirement.
85. As a maintainer, I want representative screenshots and executable behavior tests, so that visual fidelity and financial correctness can be reviewed separately.

## Implementation Decisions

- Extend the implemented native Kotlin/Compose/Material 3 app. Reuse its single-module architecture, unidirectional state/events, public use cases, repository contracts, and manual dependency construction. A broad architecture rewrite is not required.
- Retain Android 10 (API 29), light theme, offline operation, no network permission, Room finance persistence, precise minor-unit money storage, and Kotlin serialization for versioned Backup DTOs.
- Replace the current bottom navigation and combined Transaction presentation with the agreed left drawer and Overview/Expenses/Income tabs, retaining all existing major destinations and tasks. Adapt screens without individual references using the same visual system.
- Maintain one selected calendar-month context for financial views, initialized to the current local month. Navigation must not silently switch the period. Month/year headings and previous/next controls follow the references.
- Keep the shared drawer Account selection, Transaction Account choice, and each Budget's saved Account selection distinct. Changing one does not redefine the others.
- Persist the drawer selection across app/process restarts. An empty initial or persisted selection is valid; never normalize it to all Accounts or a primary Account. Backup inclusion of this presentation preference was not requested and is not a new Backup requirement.
- Open the right drawer by an inward right-edge swipe, with equivalent accessible open/close actions. Do not add the proposed Accounts header icon that the user declined. Give each Account a labeled pill switch and selected-month Closing balance. Include selectable, clearly marked archived Accounts.
- Empty drawer selection yields zero balance, Income, Expense, and net and no matching main-ledger activity, without a Select Accounts prompt. Budget progress may remain nonzero because its saved Account selection is independent.
- Calculate an Account's Closing balance from its signed Opening balance plus Income, minus Expenses, minus outgoing Transfers, plus incoming Transfers through the viewed month's final day. Only movements on or after the Account's Opening balance date contribute. Do not apply a future Opening balance before its effective date.
- Sum selected Accounts' Closing balances for the Overview headline. Use the same selected-month balance meaning for Account cards, drawer balances, Account lists, and history balances. Keep monthly Income/Expense/net visibly distinct from the cumulative Closing balance.
- Monthly flow totals use Transaction dates and the selected Account set; the Opening balance cutoff affects balance calculation, not whether a recorded Transaction belongs to its calendar month's Income/Expense total. Preserve Category/Tag list filtering.
- Show newest-first, date-grouped activity cards with deterministic ordering for same-date movements. Display relevant Account, classification, Note, and amount details. Daily subtotals must not classify Transfers as earnings or spending.
- A Transfer appears in Expenses when its source Account is selected, in Income when its destination is selected, and once in Overview if either endpoint is selected. Both directional rows open the same Transfer. Category/Tag list filters hide Transfers. Transfers never contribute to Income, Expense, net, or Budget spending.
- Use the three labeled bottom actions Add expense, Add income, and Transfer money on Overview, Expenses, and Income instead of reproducing inconsistent floating plus controls.
- Use full-screen parallel Income/Expense entry with direction control, prominent amount, keypad, optional Note, text-only optional Categories, Account choices, editable date, compatible optional Tags, and clear save/cancel. Optional Category behavior overrides the mandatory-looking Pick a Category reference.
- Correct the existing Transaction Account defect as part of entry: expose Account selection; preselect the shared most-recently-used active Account for creation; load the original Account for editing. Saving another field must not move the Transaction to a primary Account.
- Preserve positive Transaction/Transfer amount validation, explicit distinct Transfer Accounts, signed Opening balances, archive restrictions, direction-specific Categories, Tag compatibility, and existing destructive confirmations. Remove invalid Category-associated Tags under the existing direction/Category-change rules.
- Category presentation is text-only: no Category icon picker, icon field, artwork, or icon-related Backup change. This does not remove ordinary navigation/action symbols from the reference design.
- A custom Budget has a stable identity, user-entered name, positive limit, nonempty explicit Account set, optional Expense Category set, optional Tag set, and creation/update timestamps. Store references by stable ID, not name. Remove the old Category/month uniqueness restriction for custom Budgets.
- Match any Account in the Budget's Account set. If its Category set is nonempty, match any selected Category; if its Tag set is nonempty, match at least one selected Tag. All active groups apply together. Count each qualifying Expense once per Budget, even if multiple selected Tags match.
- Empty Category or Tag sets remove only that restriction; an empty Account set is invalid. Support overall, Category-only, Tag-only, and combined-filter Budgets. An Expense may independently count toward multiple Budgets; do not present summed overlapping spending as unique overall spending.
- Manually created Budget definitions recur monthly. Calculate each viewed month's progress from its matching Expenses without requiring a background job or monthly duplication of the definition. Do not auto-create Budgets for Categories or add new Accounts to saved scopes.
- Retain settings across monthly resets. Remaining amount is limit minus spent, and over-budget means spent exceeds limit. Carry neither unused amount nor overspending forward.
- Editing a Budget updates the definition and recalculates past, present, and future months. Do not introduce effective-dated revisions that preserve old settings in historical reports. Preserve identity/creation time and update modification time.
- Resolve renamed entities by ID and retain archived historical references. Do not clear archived Budget filters silently: removing a Category/Tag restriction would broaden scope. Existing restrictions on assigning archived classifications to new Transactions remain in force.
- Extend Room with explicit tested migrations for revised Budgets and selections. Preserve ledger and legacy Budget data; never use destructive migration or collapse distinct old Budgets simply because they share a Category.
- Extend versioned Backup DTOs, validation, reconciliation, snapshot export, and atomic import for complete custom Budget definitions and selections. Retain previously supported Backup versions.
- Keep Backup-owned finance data/settings in Room and preferences excluded from Backups in DataStore, following the existing atomic-import ADR. Export a consistent snapshot; validate and commit the entire imported dataset atomically.
- Preserve stable-ID/newer-record Merge behavior, equal-timestamp conflict rejection, currency conflicts, no deletion propagation, full reference validation, cancellation, and rollback. Reconcile a Budget's definition and complete selections as one record, without unioning stale filter memberships.
- Screenshot sample names, email/profile data, seeded Categories, undefined percentages, and Updated just now are illustrative, not new product requirements. Do not introduce free-text search because a search symbol appears in a reference.

## Testing Decisions

- The user explicitly confirmed existing public finance use-case/repository seams for calculations, persistence, and Backup behavior, plus Android 10 Compose/UI Automator journeys. Assert public outputs, displayed amounts, selected states, validation results, and committed snapshots. Avoid private-helper tests, direct table assertions, implementation-mirroring tests, and a global coverage percentage.
- Put shared month-end projections and Budget matching behind public application/use-case behavior rather than duplicating calculations in composables. Extend existing boundaries as necessary without creating per-screen test-only interfaces. Prefer in-memory repository adapters with deterministic time and ID providers for JVM behavior tests.
- Verify Room persistence, migrations, archived references, stable identities, and reopen behavior through repository operations. Use Android integration tests when real Room behavior is necessary, rather than asserting database rows directly.
- Test decoding, validation, reconciliation results, export, rollback, cancellation, Merge, and Overwrite through existing public Backup interfaces and complete finance snapshots.
- Extend Compose journeys for both drawers, tabs, month changes, Account switches, text-only Categories, keypad, Account selection, Budget creation/editing, and visible progress through accessible semantics. Prove restart persistence by relaunch/recreation with persisted storage; composable-state restoration alone does not prove the app-restart requirement.
- Retain UI Automator for Android document-picker flows, including cancellation. Preserve Overwrite/reset confirmations. Android 10 (API 29) remains required; newer Android versions are optional.
- Capture representative Overview, Expenses, Income, both entry forms, and both drawers for comparison with the references. Also review adapted Accounts, history, Transfers, Budgets, Settings, and setup. Compare hierarchy, spacing, typography, cards, colors, and action placement while allowing agreed text-only Categories, consistent actions, and added Account switches.
- Verify labels, selected states, keypad actions, month navigation, save/cancel, touch targets, scrolling, larger fonts, and an accessible alternative to edge gestures. Screenshots complement behavior tests; manual TalkBack/Switch Access review remains useful.
- Reuse existing prior art: FirstRunSetupTest and FirstRunViewModelTest for public use-case/state tests; TransferManagementTest for application/repository behavior; AppDatabaseTest for balances, persistence, migration, archived history, and Budgets; TransactionFlowTest and FirstRunFlowTest for journeys/relaunch; AccessibilityTest for semantics/large fonts; BackupExportTest and BackupImportTest for versioning and atomic behavior; BackupImportUiTest for the picker; and shared FinanceUiDriver, fixtures, and UI failure artifacts.
- Update old tests that assume all Accounts selected, today's/all-time balances, combined Transaction navigation, or one Category/month per Budget to assert the new product behavior. Preserve their underlying validation, identity, finance, and atomicity coverage.
- Future implementation must run relevant JVM tests, lint, formatting checks, and Android 10 UI/integration tests and retain screenshot/log/report evidence. This documentation task does not claim new implementation or passing feature tests.

The following names describe proposed tests and required observable results, not tests already implemented:

| Proposed test or journey | Required assertions |
| --- | --- |
| showAndPersistEmptyAccountSelection | Existing Accounts do not cause initial selection; ledger totals are zero; no Select Accounts prompt appears; relaunch preserves empty selection. |
| shareAccountSelectionAcrossTabsAndRelaunch | Toggle a subset, navigate Overview/Expenses/Income, and relaunch; the same subset and appropriate totals appear. |
| selectArchivedAccountWithoutEnablingEdits | Archived Account remains labeled/selectable with historical balance/activity; prohibited edits remain unavailable. |
| calculateClosingBalancesAtSelectedMonthEnd | A opens at 1,000; January Income 200, Expense 50, and Transfer 100 to B give A 1,050, B 100, combined 1,150, Income 200, Expense 50, net 150. A February Expense 25 gives combined February balance 1,125 and February net -25. March Income does not alter January/February Closing balances. |
| respectOpeningDateAndCalendarBoundaries | Cover before/on/after Opening balance date, last/first days of months, signed/zero balances, and Opening balances effective after the viewed month. Never apply a future opening amount early. Monthly flow totals still follow Transaction dates. |
| presentTransfersWithoutInflatingTotals | Select source, destination, both, and neither; assert directional membership, one Overview activity item, shared edit identity, unchanged financial totals, and hiding under Category/Tag filters. |
| editTransactionWithoutUnexpectedAccountChange | Create against a non-primary Account; edit Note/amount without changing Account; relaunch and assert identity, Account, balances, and totals. Explicit Account changes update both affected balances; new entry uses the most recently used Account. |
| saveUncategorizedTransactionUsingTextOnlyEntry | Use keypad/Account/date to save without Category/Note; no Category gate/icons appear. Direction/Category changes retain only compatible Tags. |
| matchBudgetAcrossAccountCategoryAndTagSelections | For Account A, Categories Groceries/Dining and Tags weekend/holiday, include A/Groceries/weekend 20, A/Dining/holiday 30, and A/Groceries/both Tags 10 once: spent 60. Exclude no-Tag grocery, Transport/weekend, Account B, other-month Expense, Income, and Transfer examples. At limit 50, remaining is -10 and over-budget is true. |
| supportUnrestrictedBudgetGroups | Empty Tags permits untagged matching Expenses; empty Categories permits any Expense Category; both empty permits unclassified spending in selected Accounts; empty Accounts cannot save. |
| keepBudgetScopesIndependentAndAllowOverlap | One Expense contributes once to each matching Budget. Drawer selection, including empty selection, does not alter progress. New Accounts do not join saved Budget scopes. |
| repeatBudgetMonthlyAndRecalculateHistory | Review months with different spending, including none; each uses its own Expenses and same limit. Edit limit/filters and assert recalculated past/current/future results, stable identity, no carryover, and no monthly duplicated definitions. |
| preserveFiltersThroughRenamesAndArchives | Historical names/references remain readable and archived selections do not silently disappear or become unrestricted filters. |
| migrateAndRestoreBudgetsWithoutDataLoss | Upgrade existing data and import every previously supported Backup version; assert the documented legacy mapping through public reads, original finance records, stable references, and correct calculations. Round-trip complete new Budget definitions. |
| reconcileBudgetAsOneAtomicBackupRecord | Apply a newer complete definition and repeat import; no duplication or stale filter union. Invalid references and equal-timestamp divergences fail without mutation. Overwrite, cancellation, and failed commits retain atomicity. |
| operateRedesignedAppOnAndroid10 | Reach all destinations, change month, operate both drawers accessibly, use all three actions, create/edit Budgets, and complete/cancel picker flows with displayed-value and large-font assertions. |

## Out of Scope

- App implementation, migration execution, release packaging, installation, or deployment during this documentation-only skill invocation.
- Framework replacement, backend/cloud profiles, online accounts, bank connections, synchronization, or network permission.
- Dark theme, biometric/app lock, encrypted Backups, Play Store publishing, or a newer-Android testing requirement.
- Category icons, Category artwork/pickers, or icon-related persistence and Backup fields.
- Free-text Transaction search, seeded finance data, screenshot sample profiles/email, fabricated update timestamps, or undefined percentage changes.
- Auto-generated Budgets for Categories/new Accounts or a monthly background job that duplicates recurring definitions.
- Budget carryover or effective-dated Budget edits that preserve previous settings for past months.
- Category/Tag/Note assignment to Transfers, counting Transfers as earnings/spending, recurring Transactions, forecasting, or advanced reporting.
- A new Budget deletion, stopping, or archival policy: confirmed scope covers creation, editing, recurrence, and historical recalculation. Extra lifecycle behavior needs a separate product decision.
- Splitting this spec into implementation tickets or resolving previous implementation tickets during specification authoring.

## Further Notes

- The user confirmed the completed design, then explicitly clarified that the session must remain documentation-only. Testing seams were confirmed separately during this to-spec invocation. Ready-for-agent describes the specification's triage state and does not authorize implementation in this session.
- This spec supersedes earlier one-Expense-Category-per-calendar-month Budget requirements and current/all-time balance displays. It also overrides screenshot implications of Category icons, required Categories, duplicate floating plus controls, profile data, and nonempty Account selection.
- Existing local-first, native Android, stable-identity, atomic-Backup, and distinct-Transfer ADRs remain applicable. The new display/Budget behavior does not authorize a different atomicity model or treating Transfers as Transactions.
- Source and tests already exist. Older specs' statements that the checkout lacks them are historical.
- Known entry defect: the current form lacks Transaction Account selection, editing does not load the original Account, and saving uses the primary Account ID. Correct this behavior during the redesign.
- Legacy Budget compatibility is mandatory, but the discussion did not choose an exact conversion from old Category/month Budgets into named recurring multi-filter Budgets. Before schema work, implementation planning must document and test a deterministic, lossless mapping retaining original identity, Category, month provenance, amount, timestamps, and finance history. Do not silently collapse records, discard Budgets, invent Account scopes without a documented mapping, or remove older Backup support. Surface any remaining product-dependent conversion choice before mutation.
- Reference images establish visual direction rather than requiring copied system status bars, phone frames, sample Transactions, or branded profile identities. The user supplied sidemenu_right.jpg during the interview; the missing pill switches must be added.
- Source specifications: [Personal Finance MVP](../personal-finance/spec.md) and [Personal Finance UI](../personal-finance-ui/spec.md). Use the root [glossary](../../GLOSSARY.md), existing ADRs, Android coding rules, and Android testing research.

| Reference | Role and agreed adaptation |
| --- | --- |
| [overview.jpg](../../UI_pics/overview.jpg) | Palette, balance/Account cards, recent activity, three actions; sum selected Accounts' month-end balances. |
| [expenses.jpg](../../UI_pics/expenses.jpg) | Monthly Expense summary, day-grouped cards, hierarchy, tabs; consistent labeled actions and no Category icons. |
| [incomes.jpg](../../UI_pics/incomes.jpg) | Parallel Income layout and green amounts; consistent actions and no Category icons. |
| [add_expense.jpg](../../UI_pics/add_expense.jpg) | Direction/amount/Note/keypad layout; optional text-only Categories plus Account/date/Tag choices. |
| [add_income.jpg](../../UI_pics/add_income.jpg) | Parallel Income entry with the same optional classification and Account behavior. |
| [sidemenu.jpg](../../UI_pics/sidemenu.jpg) | Left drawer, retaining Budgets and all agreed destinations without cloud-profile details. |
| [sidemenu_right.jpg](../../UI_pics/sidemenu_right.jpg) | Right drawer with added switches, archived labels, month-end balances, and empty persistent initial selection. |
