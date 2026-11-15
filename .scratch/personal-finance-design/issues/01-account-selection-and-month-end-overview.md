# 01: Account selection and month-end Overview

Status: resolved
Blocked by: None (can start immediately)

**What to build:** Choose Accounts in a persistent right-side drawer and review their combined month-end Closing balance, monthly Income, Expenses, net, and recent activity on a redesigned Overview. This first working slice establishes the shared visual components and monthly projection used by later screens.

## Parent-spec requirements

- [User stories](../spec.md#user-stories): Stories 1-5, 83-85: the shared light visual system, readable amounts, accessible actions, offline Android 10 behavior, and visual evidence.
- [User stories](../spec.md#user-stories): Stories 9-20, 22-30: shared month context, right-edge Account drawer, selectable archived Accounts, empty initial/persisted selection, and correct monthly balances and flows.
- [User stories](../spec.md#user-stories): Stories 31-32, 39, 75: preserve independently scoped Budget progress, Transfer exclusion, and historical references. Story 28's consistency across additional Account views is completed in ticket 04.
- [Implementation decisions](../spec.md#implementation-decisions): Separate drawer selection from Transaction Account and Budget Account selections; use one public monthly projection rather than composable-specific balance math; never treat an empty Account set as all Accounts. Reuse existing public finance use-case/repository seams and preserve the established storage/Backup ownership split.
- [Testing decisions](../spec.md#testing-decisions): use existing public finance use-case/repository and Backup interfaces, Compose semantics, and UI Automator for system UI; test visible outcomes rather than private helpers or tables. Named tests below are proposed or extensions of existing behavior tests, not claims of passing implementation.

## Observable acceptance criteria

- [x] The Overview uses the reference charcoal month header, yellow accents, light gray background, rounded white cards, readable selected-currency amounts, and distinct Income/Expense/net labels. Existing routes and finance actions remain usable while later navigation changes are pending.
- [x] Current local month is selected initially; previous/next controls update the month heading, headline Closing balance, period flows, and recent activity together. Navigation to an already available finance view does not silently reset the shared month.
- [x] The Account drawer opens by an inward swipe from the right edge and exposes accessible open/close actions without adding the declined Accounts header icon. Every Account has a name, selected-month Closing balance, and accessible pill switch; archived Accounts are selectable and marked Archived.
- [x] Fresh display preferences select no Accounts, even when the ledger already contains Accounts. A saved subset and an explicitly saved empty set survive a genuine app/process relaunch. Empty selection shows zero ledger balance/Income/Expense/net and no matching activity, with no Select Accounts prompt.
- [x] The headline sums only selected Accounts' signed Closing balances through the selected month's final day. Opening-date cutoffs, Transfers, negative/zero balances, later recorded movements, and future-effective Opening balances are handled consistently. Monthly flow totals use Transaction dates, not the Opening balance cutoff.
- [x] Overview activity is limited to the selected month and Accounts, newest first with deterministic same-date ordering. Each relevant Transfer appears once even when both Accounts are selected; Transfers do not inflate Income, Expense, or net.
- [x] Existing Budget progress remains available and independent of the drawer selection. Do not introduce recurring-Budget schema changes here; tickets 06-07 supply those capabilities and their full monthly integration.
- [x] Shared month/projection/state behavior is introduced through existing public boundaries without a separate broad refactor. Relevant JVM behavior tests, Android 10 Overview/drawer journeys, lint, formatting, and Overview/right-drawer screenshots pass or provide reviewable evidence.

## Named behavior tests

| Test or journey | Observable result |
| --- | --- |
| showAndPersistEmptyAccountSelection | Assert zero initial totals, absent selection prompt, and persistence of the empty set after app restart. |
| persistSelectedAccountsAcrossAppRestart | Toggle a subset, restart, and assert restored switches, Closing balance, and period totals. |
| calculateClosingBalancesAtSelectedMonthEnd | A opens at 1,000; January Income 200, Expense 50, Transfer 100 to B yields A 1,050, B 100, total 1,150, Income 200, Expense 50, net 150. February Expense 25 yields total 1,125 and February net -25. March Income changes neither earlier month. |
| respectOpeningDateAndCalendarBoundaries | Cover dates before/on/after the Opening balance date, month boundaries, negative/zero balances, and Opening balances effective after the viewed month; flow totals still follow their Transaction dates. |
| selectArchivedAccountWithoutEnablingEdits | Include archived balance/activity via a labeled switch without enabling prohibited edits. |
| filterOverviewActivityByMonthAndSelectedAccounts | Assert month/subset filtering, ordering, one Transfer item, and unchanged independently scoped Budget progress. |
| operateOverviewDrawerAtLargeFont | Assert accessible drawer actions, switch labels/states, month controls, scrolling, retained Budget values, ordered activity, and readable amounts at larger font settings. |

## Delivery boundaries

Own the shared visual foundation, month context, and Overview/right-drawer journey. Preserve existing screens instead of removing them before their replacement tickets land. Ticket 02 extends the scope to tabs/directional lists; ticket 04 completes Account/history presentation. Do not introduce Category icons, sample profile data, search, fabricated percentages, or seeded data.

Each slice must remain independently demoable and keep existing callers and relevant checks working. Include the feature's behavior and UI verification in this ticket rather than deferring them to the final integration ticket. Follow the parent spec's exclusions and existing ADRs.

## Comments

- 2026-11-15: Implementation committed as `ea9c3f6` (`Implement account-scoped monthly overview`). The month-end projection, persistent Account selection, right-edge drawer, month navigation, and Overview styling are implemented. Kotlin compilation, the existing JVM test task, ktlint, and Android lint passed through the commit hook. The named Overview/drawer behavior tests and Android 10 UI/screenshot evidence are still outstanding, so this ticket remains claimed rather than resolved.
- 2026-11-15: Follow-up verification added `MonthlyOverviewTest` coverage for month-end balances, opening-date/calendar boundaries, selected-account activity, Transfer exclusion/deduplication, and empty selection. `OverviewAccountSelectionTest` covers the empty initial state, right-edge drawer selection, shared month navigation with Budgets, archived-account selection, and stored selected/empty preference values. `OverviewAccessibilityTest` exercises the drawer and switch at 2x font scale. All five targeted Compose/UI tests passed on the Android 10 API 29 `xperia_xz2_compact` emulator; `:app:testDebugUnitTest`, `:app:ktlintCheck`, and `:app:lintDebug` passed. Reviewed screenshots: [Overview](evidence/01/overview.png) and [Account drawer](evidence/01/overview-account-drawer.png).
- 2026-11-15: Verified saved subset and explicit-empty persistence across real host-driven process restarts on the same API 29 emulator. `OverviewPersistenceHandoffTest.prepareSelectedOverviewForHostProcessRestart` and `.prepareEmptyOverviewForHostProcessRestart` seed each state; after each standalone instrumentation invocation, host ADB ran `am force-stop` and relaunched `MainActivity`. The selected screenshot/UI hierarchy shows EUR 1,002 closing balance and EUR 2 income; the empty screenshot/UI hierarchy shows EUR 0, no activity, and no selection prompt. Evidence: [selected after restart](evidence/01/overview-after-selected-relaunch.png), [empty after restart](evidence/01/overview-after-empty-relaunch.png), [selected hierarchy](evidence/01/overview-after-selected-relaunch.xml), [empty hierarchy](evidence/01/overview-after-empty-relaunch.xml).
- 2026-11-15: Completed the remaining visible-behavior assertions. At 2x font scale, `OverviewAccessibilityTest` verifies the retained Food Budget spent/limit/remaining values after changing drawer selection, newest-first activity ordering, deterministic descending-ID tie-breaking for same-date/same-createdAt entries, scrolling to the fifth Account and Close action. `OverviewAccountSelectionTest` verifies archived activity cannot be edited. The selected and empty preference states were separately verified after host-driven force-stop/relaunch as recorded above. Final checks passed: `:app:testDebugUnitTest`, `:app:ktlintCheck`, `:app:lintDebug`, one `OverviewAccessibilityTest`, and four `OverviewAccountSelectionTest` journeys on API 29.

- 2026-11-15: Replaced the manual host-restart procedure with [`scripts/verify_overview_persistence.py`](../../../scripts/verify_overview_persistence.py). The runner builds/installs the debug APKs, seeds each state in separate instrumentation runs, verifies process termination, relaunches the app, and asserts displayed totals, activity, and both drawer switches. The fixture now includes an unselected Savings Account so selecting all Accounts cannot accidentally pass. Selected and explicitly empty restart checks passed on API 29; ten emulator-free host assertion regressions, JVM tests, ktlint, and Android lint passed. CI now runs the host check before the connected suite and uploads its XML/PNG, seed, command, and logcat evidence even if another test subsequently fails. Local run details are in [the Android testing guide](../../../docs/research/ai-ui-testing.md#overview-selection-across-process-restarts).

- 2026-11-15: The subsequent full `connectedDebugAndroidTest` run completed 51 tests: 42 passed and 9 failed, with no skips. Both persistence seed methods and all five Overview/account-selection/accessibility journeys passed. The failures are in unchanged `BackupImportUiTest`, `FirstRunFlowTest`, and seven `TransactionFlowTest` journeys. The first two wait for the removed `EUR ledger is ready.` text; the Transaction journeys also time out on existing UI assumptions. No production code was changed by this automation follow-up. Full output is in `build/overview-connected-suite.log`, with the HTML report at `app/build/reports/androidTests/connected/debug/index.html`; the separate host restart result and evidence are under `build/overview-persistence/20261115T202200083838Z/`.

## Answer

Implemented and verified account-scoped monthly Overview, persistent Account selection, month-end balances and flows, activity filtering/order, independent Budget progress, archived Account behavior, and accessible right-drawer interaction. The API 29 emulator evidence is linked above. This ticket is resolved; dependent tickets 02, 03, 04, 06, 08, and 09 can proceed.
