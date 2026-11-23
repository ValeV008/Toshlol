# 02: Active Account drawer and archived Account journeys

Status: ready-for-agent

Blocked by: 01

Approval: the user approved this breakdown and its blocking edges on 2026-11-23. Publication is documentation only; implementation has not started.

**What to build:** Users change month/active Account scope through the reference right drawer, archive Accounts without losing history, and manage active versus archived Accounts through the adapted Accounts destination.

## Parent-spec requirements

[User stories](../spec.md#user-stories) 12-29, 31, 87, 95-97; [Implementation Decisions](../spec.md#implementation-decisions) for shared month, persistent active-only selection, archive normalization, independent Budget scope, and blue management screens.

Also apply the parent specification's [shared stories](../spec.md#user-stories) 1-6 and 92-100 where relevant: visual fidelity, currency/direction clarity, readable large-font/empty/error states, accessible controls, existing-data/Backup preservation, native offline Android 10 behavior, and evidence separate from the prior effort. Follow its [Testing Decisions](../spec.md#testing-decisions), [Out of Scope](../spec.md#out-of-scope), and [reference roles](../spec.md#further-notes).

Prerequisite tickets: [01: Reference navigation shell and Overview](01-reference-navigation-shell-and-overview.md).

## Observable acceptance criteria

- [ ] The clock, right-edge gesture, and accessible action reach the same reference drawer from Overview, Expenses, Incomes, and Budgets. It has month selection, active Account Closing balances/switches, active-only All accounts, and no bottom actions or arbitrary date ranges.
- [ ] All accounts on selects the currently active Accounts; off clears. Partial selection appears off, new Accounts remain unselected, and initially empty/subset/explicitly empty selections survive actual app/process restarts.
- [ ] Archiving removes an Account from all main selections and the drawer, even in earlier-month views. Loading stale preferences or imported archived state cannot reselect it. Existing Account data, historical Closing balances, Budget scopes/spending, and supported Backup content remain intact.
- [ ] Accounts has Active/Archived views, reference-style Account management/editing, and retained read-only archived history. Signed Opening balances and opening-date rules remain unchanged. Archive is not permanent deletion.
- [ ] Account switches opened from Budgets update the scope used on other main screens without changing Budget header/content/progress; changing month updates its progress. Transaction Account choice and saved Budget scope remain separate.
- [ ] `shareMonthAndActiveAccountScopeAcrossDestinations`, `persistEmptyPartialAndBulkAccountSelection`, `archiveSelectedAccountAndKeepHistoricalData`, and `operateBudgetDrawerWithoutChangingBudgetScope` cover public values, restart behavior, and history access. Capture drawer/Account editor/Active/Archived/history evidence at normal and large fonts.

## Verification and completion

- [ ] Exercise the named critical journeys above through the confirmed public Transaction/application-repository, monthly-summary, Account/Budget, and Backup interfaces as applicable. Use Compose accessible semantics for in-app behavior and UI Automator for system document-picker flows. Assert displayed values, committed/reopened snapshots, selection, validation, and cancellation rather than private helpers or direct database tables.
- [ ] Match the owning reference as closely as possible at normal font size, including layout, spacing, typography, colors, generic icons, amounts, and control placement, with only agreed adaptations. Retain the normal/large-font screenshots above; do not defer this slice's fidelity or behavior coverage to integration.
- [ ] Pass the full repository [ticket completion checklist](../../../docs/research/ai-ui-testing.md#ticket-completion) against finished changes: all JVM and Python host-runner tests, Android lint/Kotlin formatting and required builds, both host-driven Android 10 Overview restart checks, and the full unfiltered Android 10 connected suite with no failures, errors, or skipped tests. Focused tests alone do not complete the ticket.
- [ ] Record device/API, check results and test counts, screenshots, reports, failure evidence, and limitations in the ticket's Answer. Leave unresolved if a required check is unavailable or failing. Do not claim manual TalkBack/Switch Access testing that the user declined.

## Delivery boundaries

Implement the approved slice end to end, preserving prerequisites and existing working finance behavior. Reuse existing public boundaries and do small necessary prefactoring before changing the live journey; avoid broad rewrites and screen-specific test interfaces. Later capabilities belong to their owning tickets, and incomplete controls must not be exposed. Publication is documentation only and does not authorize implementation, migrations, installation, or release. Keep the previously implemented effort and its evidence unchanged.
