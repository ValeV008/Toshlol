# 06: By category summaries and activity drill-down

Status: ready-for-agent

Blocked by: 05

Approval: the user approved this breakdown and its blocking edges on 2026-11-23. Publication is documentation only; implementation has not started.

**What to build:** Users switch between functioning By date/By category tabs, see descending Category totals, and open the matching classified activity without changing scope.

## Parent-spec requirements

[User stories](../spec.md#user-stories) 36-40; [Implementation Decisions](../spec.md#implementation-decisions) for Category grouping, Uncategorized, drill-down, Transfer exclusion, and per-part classification reads.

Also apply the parent specification's [shared stories](../spec.md#user-stories) 1-6 and 92-100 where relevant: visual fidelity, currency/direction clarity, readable large-font/empty/error states, accessible controls, existing-data/Backup preservation, native offline Android 10 behavior, and evidence separate from the prior effort. Follow its [Testing Decisions](../spec.md#testing-decisions), [Out of Scope](../spec.md#out-of-scope), and [reference roles](../spec.md#further-notes).

Prerequisite tickets: [05: Reference By date Expense and Income lists](05-reference-by-date-expense-and-income-lists.md).

## Observable acceptance criteria

- [ ] Both tabs function in Expenses and Incomes; By date remains the initial view. Switching preserves direction, selected month/Accounts, and applicable filters.
- [ ] By category shows matching totals largest first, including Uncategorized when applicable. Totals are based on movements, not displayed labels or duplicated card/group metadata; Transfers are absent.
- [ ] A Category opens its matching activity, whose rows open details and return without losing context. Existing Tag filtering remains usable.
- [ ] Use the existing public Transaction/summary outputs. If Transaction split support is already present, group/count matching parts individually; otherwise the later allocation ticket must verify that combined behavior when it lands.
- [ ] `switchCategorySummaryAndDrillDown` asserts actual totals, ordering, unclassified values, filtering, and Transfer absence. Capture the adapted normal/large-font Category summary and drill-down as part of this slice.

## Verification and completion

- [ ] Exercise the named critical journeys above through the confirmed public Transaction/application-repository, monthly-summary, Account/Budget, and Backup interfaces as applicable. Use Compose accessible semantics for in-app behavior and UI Automator for system document-picker flows. Assert displayed values, committed/reopened snapshots, selection, validation, and cancellation rather than private helpers or direct database tables.
- [ ] Match the owning reference as closely as possible at normal font size, including layout, spacing, typography, colors, generic icons, amounts, and control placement, with only agreed adaptations. Retain the normal/large-font screenshots above; do not defer this slice's fidelity or behavior coverage to integration.
- [ ] Pass the full repository [ticket completion checklist](../../../docs/research/ai-ui-testing.md#ticket-completion) against finished changes: all JVM and Python host-runner tests, Android lint/Kotlin formatting and required builds, both host-driven Android 10 Overview restart checks, and the full unfiltered Android 10 connected suite with no failures, errors, or skipped tests. Focused tests alone do not complete the ticket.
- [ ] Record device/API, check results and test counts, screenshots, reports, failure evidence, and limitations in the ticket's Answer. Leave unresolved if a required check is unavailable or failing. Do not claim manual TalkBack/Switch Access testing that the user declined.

## Delivery boundaries

Implement the approved slice end to end, preserving prerequisites and existing working finance behavior. Reuse existing public boundaries and do small necessary prefactoring before changing the live journey; avoid broad rewrites and screen-specific test interfaces. Later capabilities belong to their owning tickets, and incomplete controls must not be exposed. Publication is documentation only and does not authorize implementation, migrations, installation, or release. Keep the previously implemented effort and its evidence unchanged.
