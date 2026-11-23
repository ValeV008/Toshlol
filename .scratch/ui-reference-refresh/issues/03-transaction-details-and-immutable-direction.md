# 03: Transaction details and immutable direction

Status: ready-for-agent

Blocked by: 01

Approval: the user approved this breakdown and its blocking edges on 2026-11-23. Publication is documentation only; implementation has not started.

**What to build:** Ordinary Transaction rows open reference-style details before editing, with correct metadata, existing delete confirmation, and fixed Income/Expense direction through both UI and public editing behavior.

## Parent-spec requirements

[User stories](../spec.md#user-stories) 42-46, 59; [Implementation Decisions](../spec.md#implementation-decisions) for details-first navigation, immutable direction, preserved Account/identity, and destructive confirmation.

Also apply the parent specification's [shared stories](../spec.md#user-stories) 1-6 and 92-100 where relevant: visual fidelity, currency/direction clarity, readable large-font/empty/error states, accessible controls, existing-data/Backup preservation, native offline Android 10 behavior, and evidence separate from the prior effort. Follow its [Testing Decisions](../spec.md#testing-decisions), [Out of Scope](../spec.md#out-of-scope), and [reference roles](../spec.md#further-notes).

Prerequisite tickets: [01: Reference navigation shell and Overview](01-reference-navigation-shell-and-overview.md).

## Observable acceptance criteria

- [ ] Transaction taps from Overview and both directional lists open amount/metadata/optional Note details with back/edit/delete. Omit empty optional metadata and unsupported Make transfer conversion.
- [ ] Edit opens the current valid editing flow, retaining identity and original Account. Delete requires confirmation; cancellation and validation failure leave the Transaction unchanged.
- [ ] Creation direction follows Add expense/Add income. No direction selector remains and public save/edit rejects conversion of an existing Transaction, preserving all other data on failure.
- [ ] Keep Categories appropriate to the immutable direction and existing compatible-Tag/archive restrictions. Do only the minimal public-boundary/state cleanup needed for this behavior; do not create another finance module.
- [ ] Split/Unsplit are not dead controls before their owning capability tickets. The details interface must accommodate their later addition without changing ordinary values or navigation.
- [ ] `inspectDetailsThenEditWithFixedDirection` asserts displayed detail values, confirmation, Account/identity preservation, and rejection through the public edit boundary. Capture both details directions against their new references.

## Verification and completion

- [ ] Exercise the named critical journeys above through the confirmed public Transaction/application-repository, monthly-summary, Account/Budget, and Backup interfaces as applicable. Use Compose accessible semantics for in-app behavior and UI Automator for system document-picker flows. Assert displayed values, committed/reopened snapshots, selection, validation, and cancellation rather than private helpers or direct database tables.
- [ ] Match the owning reference as closely as possible at normal font size, including layout, spacing, typography, colors, generic icons, amounts, and control placement, with only agreed adaptations. Retain the normal/large-font screenshots above; do not defer this slice's fidelity or behavior coverage to integration.
- [ ] Pass the full repository [ticket completion checklist](../../../docs/research/ai-ui-testing.md#ticket-completion) against finished changes: all JVM and Python host-runner tests, Android lint/Kotlin formatting and required builds, both host-driven Android 10 Overview restart checks, and the full unfiltered Android 10 connected suite with no failures, errors, or skipped tests. Focused tests alone do not complete the ticket.
- [ ] Record device/API, check results and test counts, screenshots, reports, failure evidence, and limitations in the ticket's Answer. Leave unresolved if a required check is unavailable or failing. Do not claim manual TalkBack/Switch Access testing that the user declined.

## Delivery boundaries

Implement the approved slice end to end, preserving prerequisites and existing working finance behavior. Reuse existing public boundaries and do small necessary prefactoring before changing the live journey; avoid broad rewrites and screen-specific test interfaces. Later capabilities belong to their owning tickets, and incomplete controls must not be exposed. Publication is documentation only and does not authorize implementation, migrations, installation, or release. Keep the previously implemented effort and its evidence unchanged.
