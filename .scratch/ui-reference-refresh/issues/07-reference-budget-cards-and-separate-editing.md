# 07: Reference Budget cards and separate editing

Status: ready-for-agent

Blocked by: 02

Approval: the user approved this breakdown and its blocking edges on 2026-11-23. Publication is documentation only; implementation has not started.

**What to build:** Users review one reference-style card per monthly Budget and create/edit definitions on a separate screen while their existing saved scopes remain independent of main Account selection.

## Parent-spec requirements

[User stories](../spec.md#user-stories) 11, 24-26, 29, 82-86, 95; [Implementation Decisions](../spec.md#implementation-decisions) for retained Budget model, separate editing, independent header/scope, and progress presentation.

Also apply the parent specification's [shared stories](../spec.md#user-stories) 1-6 and 92-100 where relevant: visual fidelity, currency/direction clarity, readable large-font/empty/error states, accessible controls, existing-data/Backup preservation, native offline Android 10 behavior, and evidence separate from the prior effort. Follow its [Testing Decisions](../spec.md#testing-decisions), [Out of Scope](../spec.md#out-of-scope), and [reference roles](../spec.md#further-notes).

Prerequisite tickets: [02: Active Account drawer and archived Account journeys](02-active-account-drawer-and-archived-account-journeys.md).

## Observable acceptance criteria

- [ ] Use blue header/plus, All budgets / selected month, and one white card per Budget containing name, month, saved scope, spent/limit, remaining/overspent, and conventional spent/limit progress. Omit unexplained black markers and Non-monthly grouping.
- [ ] Plus opens separate creation; tapping a card opens editing. Remove the always-visible inline editor while preserving all existing fields, filters, nonempty explicit Account scope, positive amount validation, and stable saved identity.
- [ ] Recurrence, no carryover, independent overlap, OR/AND restriction matching, all-month edits, and preserved archived scopes/history remain correct. Drawer Account changes do not alter the header, cards, or definitions.
- [ ] Do not redo the already-settled legacy Budget conversion or add Budget deletion/stopping/archival. Existing Backups remain valid.
- [ ] `preserveBudgetModelAndSeparateEditor` and `operateBudgetDrawerWithoutChangingBudgetScope` assert public progress and displayed values. Capture list, creation/editing, empty/over-budget, and large-font evidence. Verify part-aware spending when allocation support is present or lands later.

## Verification and completion

- [ ] Exercise the named critical journeys above through the confirmed public Transaction/application-repository, monthly-summary, Account/Budget, and Backup interfaces as applicable. Use Compose accessible semantics for in-app behavior and UI Automator for system document-picker flows. Assert displayed values, committed/reopened snapshots, selection, validation, and cancellation rather than private helpers or direct database tables.
- [ ] Match the owning reference as closely as possible at normal font size, including layout, spacing, typography, colors, generic icons, amounts, and control placement, with only agreed adaptations. Retain the normal/large-font screenshots above; do not defer this slice's fidelity or behavior coverage to integration.
- [ ] Pass the full repository [ticket completion checklist](../../../docs/research/ai-ui-testing.md#ticket-completion) against finished changes: all JVM and Python host-runner tests, Android lint/Kotlin formatting and required builds, both host-driven Android 10 Overview restart checks, and the full unfiltered Android 10 connected suite with no failures, errors, or skipped tests. Focused tests alone do not complete the ticket.
- [ ] Record device/API, check results and test counts, screenshots, reports, failure evidence, and limitations in the ticket's Answer. Leave unresolved if a required check is unavailable or failing. Do not claim manual TalkBack/Switch Access testing that the user declined.

## Delivery boundaries

Implement the approved slice end to end, preserving prerequisites and existing working finance behavior. Reuse existing public boundaries and do small necessary prefactoring before changing the live journey; avoid broad rewrites and screen-specific test interfaces. Later capabilities belong to their owning tickets, and incomplete controls must not be exposed. Publication is documentation only and does not authorize implementation, migrations, installation, or release. Keep the previously implemented effort and its evidence unchanged.
