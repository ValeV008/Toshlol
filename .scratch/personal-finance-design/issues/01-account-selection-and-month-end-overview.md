# 01: Account selection and month-end Overview

Status: ready-for-agent
Blocked by: None (can start immediately)

**What to build:** Choose Accounts in a persistent right-side drawer and review their combined month-end Closing balance, monthly Income, Expenses, net, and recent activity on a redesigned Overview. This first working slice establishes the shared visual components and monthly projection used by later screens.

## Parent-spec requirements

- [User stories](../spec.md#user-stories): Stories 1-5, 83-85: the shared light visual system, readable amounts, accessible actions, offline Android 10 behavior, and visual evidence.
- [User stories](../spec.md#user-stories): Stories 9-20, 22-30: shared month context, right-edge Account drawer, selectable archived Accounts, empty initial/persisted selection, and correct monthly balances and flows.
- [User stories](../spec.md#user-stories): Stories 31-32, 39, 75: preserve independently scoped Budget progress, Transfer exclusion, and historical references. Story 28's consistency across additional Account views is completed in ticket 04.
- [Implementation decisions](../spec.md#implementation-decisions): Separate drawer selection from Transaction Account and Budget Account selections; use one public monthly projection rather than composable-specific balance math; never treat an empty Account set as all Accounts. Reuse existing public finance use-case/repository seams and preserve the established storage/Backup ownership split.
- [Testing decisions](../spec.md#testing-decisions): use existing public finance use-case/repository and Backup interfaces, Compose semantics, and UI Automator for system UI; test visible outcomes rather than private helpers or tables. Named tests below are proposed or extensions of existing behavior tests, not claims of passing implementation.

## Observable acceptance criteria

- [ ] The Overview uses the reference charcoal month header, yellow accents, light gray background, rounded white cards, readable selected-currency amounts, and distinct Income/Expense/net labels. Existing routes and finance actions remain usable while later navigation changes are pending.
- [ ] Current local month is selected initially; previous/next controls update the month heading, headline Closing balance, period flows, and recent activity together. Navigation to an already available finance view does not silently reset the shared month.
- [ ] The Account drawer opens by an inward swipe from the right edge and exposes accessible open/close actions without adding the declined Accounts header icon. Every Account has a name, selected-month Closing balance, and accessible pill switch; archived Accounts are selectable and marked Archived.
- [ ] Fresh display preferences select no Accounts, even when the ledger already contains Accounts. A saved subset and an explicitly saved empty set survive a genuine app/process relaunch. Empty selection shows zero ledger balance/Income/Expense/net and no matching activity, with no Select Accounts prompt.
- [ ] The headline sums only selected Accounts' signed Closing balances through the selected month's final day. Opening-date cutoffs, Transfers, negative/zero balances, later recorded movements, and future-effective Opening balances are handled consistently. Monthly flow totals use Transaction dates, not the Opening balance cutoff.
- [ ] Overview activity is limited to the selected month and Accounts, newest first with deterministic same-date ordering. Each relevant Transfer appears once even when both Accounts are selected; Transfers do not inflate Income, Expense, or net.
- [ ] Existing Budget progress remains available and independent of the drawer selection. Do not introduce recurring-Budget schema changes here; tickets 06-07 supply those capabilities and their full monthly integration.
- [ ] Shared month/projection/state behavior is introduced through existing public boundaries without a separate broad refactor. Relevant JVM behavior tests, Android 10 Overview/drawer journeys, lint, formatting, and Overview/right-drawer screenshots pass or provide reviewable evidence.

## Named behavior tests

| Test or journey | Observable result |
| --- | --- |
| showAndPersistEmptyAccountSelection | Assert zero initial totals, absent selection prompt, and persistence of the empty set after app restart. |
| persistSelectedAccountsAcrossAppRestart | Toggle a subset, restart, and assert restored switches, Closing balance, and period totals. |
| calculateClosingBalancesAtSelectedMonthEnd | A opens at 1,000; January Income 200, Expense 50, Transfer 100 to B yields A 1,050, B 100, total 1,150, Income 200, Expense 50, net 150. February Expense 25 yields total 1,125 and February net -25. March Income changes neither earlier month. |
| respectOpeningDateAndCalendarBoundaries | Cover dates before/on/after the Opening balance date, month boundaries, negative/zero balances, and Opening balances effective after the viewed month; flow totals still follow their Transaction dates. |
| selectArchivedAccountWithoutEnablingEdits | Include archived balance/activity via a labeled switch without enabling prohibited edits. |
| filterOverviewActivityByMonthAndSelectedAccounts | Assert month/subset filtering, ordering, one Transfer item, and unchanged independently scoped Budget progress. |
| operateOverviewDrawerAtLargeFont | Assert accessible drawer actions, switch labels/states, month controls, scrolling, and readable amounts at larger font settings. |

## Delivery boundaries

Own the shared visual foundation, month context, and Overview/right-drawer journey. Preserve existing screens instead of removing them before their replacement tickets land. Ticket 02 extends the scope to tabs/directional lists; ticket 04 completes Account/history presentation. Do not introduce Category icons, sample profile data, search, fabricated percentages, or seeded data.

Each slice must remain independently demoable and keep existing callers and relevant checks working. Include the feature's behavior and UI verification in this ticket rather than deferring them to the final integration ticket. Follow the parent spec's exclusions and existing ADRs.
