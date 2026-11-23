# 05: Reference By date Expense and Income lists

Status: ready-for-agent

Blocked by: 02, 03

Approval: the user approved this breakdown and its blocking edges on 2026-11-23. Publication is documentation only; implementation has not started.

**What to build:** Both directional lists show the selected month's correct activity in the red/green reference layout, with day grouping, filters, Transfer rules, and the three bottom actions.

## Parent-spec requirements

[User stories](../spec.md#user-stories) 5-6, 10, 12, 21-22, 33-35, 39-41; [Implementation Decisions](../spec.md#implementation-decisions) for By date ordering, hierarchy, main scope, filters, and distinct Transfers.

Also apply the parent specification's [shared stories](../spec.md#user-stories) 1-6 and 92-100 where relevant: visual fidelity, currency/direction clarity, readable large-font/empty/error states, accessible controls, existing-data/Backup preservation, native offline Android 10 behavior, and evidence separate from the prior effort. Follow its [Testing Decisions](../spec.md#testing-decisions), [Out of Scope](../spec.md#out-of-scope), and [reference roles](../spec.md#further-notes).

Prerequisite tickets: [02: Active Account drawer and archived Account journeys](02-active-account-drawer-and-archived-account-journeys.md), [03: Transaction details and immutable direction](03-transaction-details-and-immutable-direction.md).

## Observable acceptance criteria

- [ ] Reproduce the appropriate header, Account/month summary, gray date/subtotal bands, white rows, Category/Tags/optional Note hierarchy, amount alignment, and red/green meaning. Omit search and floating plus; retain all three labeled bottom actions.
- [ ] By date is the initial list mode. Day/local-month boundaries, newest-first deterministic order, Category/Tag filters, zero selection, and contextual empty states use the existing correct finance data.
- [ ] Outgoing/incoming Transfers follow selected source/destination membership, open the existing Transfer, and remain excluded from daily/monthly flows, net, and Budget spending. Category/Tag filters hide Transfers.
- [ ] Ordinary Transaction rows open the details from 03, retain month/scope on return, and expose Account context without breaking the reference hierarchy.
- [ ] Preserve a working By date route while By category is delivered separately; do not expose a nonfunctional second tab. Keep the mode-state extension local to presentation and existing public summary behavior.
- [ ] `preserveSelectedMonthFinanceAndTransferRules` and directional portions of `shareMonthAndActiveAccountScopeAcrossDestinations` assert values, membership, filtering, and navigation. Capture normal/large-font/empty/filtered Expense and Income examples against their references.

## Verification and completion

- [ ] Exercise the named critical journeys above through the confirmed public Transaction/application-repository, monthly-summary, Account/Budget, and Backup interfaces as applicable. Use Compose accessible semantics for in-app behavior and UI Automator for system document-picker flows. Assert displayed values, committed/reopened snapshots, selection, validation, and cancellation rather than private helpers or direct database tables.
- [ ] Match the owning reference as closely as possible at normal font size, including layout, spacing, typography, colors, generic icons, amounts, and control placement, with only agreed adaptations. Retain the normal/large-font screenshots above; do not defer this slice's fidelity or behavior coverage to integration.
- [ ] Pass the full repository [ticket completion checklist](../../../docs/research/ai-ui-testing.md#ticket-completion) against finished changes: all JVM and Python host-runner tests, Android lint/Kotlin formatting and required builds, both host-driven Android 10 Overview restart checks, and the full unfiltered Android 10 connected suite with no failures, errors, or skipped tests. Focused tests alone do not complete the ticket.
- [ ] Record device/API, check results and test counts, screenshots, reports, failure evidence, and limitations in the ticket's Answer. Leave unresolved if a required check is unavailable or failing. Do not claim manual TalkBack/Switch Access testing that the user declined.

## Delivery boundaries

Implement the approved slice end to end, preserving prerequisites and existing working finance behavior. Reuse existing public boundaries and do small necessary prefactoring before changing the live journey; avoid broad rewrites and screen-specific test interfaces. Later capabilities belong to their owning tickets, and incomplete controls must not be exposed. Publication is documentation only and does not authorize implementation, migrations, installation, or release. Keep the previously implemented effort and its evidence unchanged.
